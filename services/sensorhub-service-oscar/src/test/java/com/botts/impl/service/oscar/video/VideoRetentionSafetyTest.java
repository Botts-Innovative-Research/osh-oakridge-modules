package com.botts.impl.service.oscar.video;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sensorhub.mpegts.MpegTsProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_free;

public class VideoRetentionSafetyTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void unknownFrameRateIsNotMisclassifiedAsAlreadyDecimated() {
        assertFalse(VideoRetention.isAlreadyDecimated(0, 0));
        assertFalse(VideoRetention.isAlreadyDecimated(0, 1));
        assertFalse(VideoRetention.isAlreadyDecimated(30, 1));
        assertTrue(VideoRetention.isAlreadyDecimated(1, 2));
    }

    @Test
    public void processingBudgetConvertsSecondsToMilliseconds() {
        // 900,000 ticks at 1/90,000 is 10 seconds; with the 5x safety factor this is 50 seconds.
        assertEquals(50_000L,
                VideoRetention.maximumProcessingDurationMillis(900_000L, 1, 90_000));
    }

    @Test
    public void replacementFailurePreservesOriginalBytes() throws Exception {
        Path directory = temporaryFolder.newFolder("replace-failure").toPath();
        Path original = directory.resolve("clip.mp4");
        byte[] originalBytes = "original-video".getBytes(StandardCharsets.UTF_8);
        Files.write(original, originalBytes);

        Path missingTemporary = directory.resolve("missing.mp4");
        try {
            VideoRetention.replaceOriginalWithValidatedTemp(original, missingTemporary);
            fail("Missing replacement must fail");
        } catch (IOException expected) {
            assertArrayEquals(originalBytes, Files.readAllBytes(original));
        }
    }

    @Test
    public void successfulReplacementHasCheckedOutcome() throws Exception {
        Path directory = temporaryFolder.newFolder("replace-success").toPath();
        Path original = directory.resolve("clip.mp4");
        Path temporary = directory.resolve("clip.decimated.mp4");
        Files.write(original, "original-video".getBytes(StandardCharsets.UTF_8));
        byte[] replacementBytes = "validated-decimated-video".getBytes(StandardCharsets.UTF_8);
        Files.write(temporary, replacementBytes);

        VideoRetention.replaceOriginalWithValidatedTemp(original, temporary);

        assertArrayEquals(replacementBytes, Files.readAllBytes(original));
        assertFalse(Files.exists(temporary));
    }

    @Test
    public void corruptTemporaryFileDoesNotPassFfmpegValidation() throws Exception {
        Path corrupt = temporaryFolder.newFile("corrupt.mp4").toPath();
        Files.write(corrupt, "not-an-mp4".getBytes(StandardCharsets.UTF_8));

        assertFalse(VideoRetention.validateDecimatedFile(corrupt));
    }

    @Test
    public void inlineDecimatorFinalizesAReadableH264File() throws Exception {
        assertReadableNativeDecimation();
    }

    @Test
    public void trulyUnknownDurationPreservesSourceInsteadOfGuessing() throws Exception {
        Path fixture = bundledH264Fixture();
        long sourceSize = Files.size(fixture);
        Path output = temporaryFolder.newFolder("unknown-duration").toPath().resolve("decimated.mp4");
        MpegTsProcessor processor = new MpegTsProcessor(fixture.toString());
        MpegTsProcessor.VideoStreamSnapshot snapshot = null;
        try {
            processor.setInjectExtradata(false);
            assertTrue(processor.openStream());
            processor.queryEmbeddedStreams();
            snapshot = processor.getVideoStreamSnapshot();
            assertNotNull(snapshot);
            snapshot = new MpegTsProcessor.VideoStreamSnapshot(
                    snapshot.codecParameters(),
                    snapshot.timeBaseNumerator(),
                    snapshot.timeBaseDenominator(),
                    0,
                    snapshot.averageFrameRateNumerator(),
                    snapshot.averageFrameRateDenominator());

            try {
                new VideoKeyframeDecimator(output.toString(), 3, snapshot);
                fail("A truly unknown duration must not be guessed");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("duration"));
            }
            assertEquals("Source video must remain untouched", sourceSize, Files.size(fixture));
            assertFalse("Rejected output must not be retained", Files.exists(output));
        } finally {
            processor.closeStream();
            if (snapshot != null && snapshot.codecParameters() != null) {
                avcodec_parameters_free(snapshot.codecParameters());
            }
        }
    }

    private void assertReadableNativeDecimation() throws Exception {
        Path fixture = bundledH264Fixture();

        Path output = temporaryFolder.newFolder("native-decimator").toPath().resolve("decimated.mp4");
        MpegTsProcessor processor = new MpegTsProcessor(fixture.toString());
        MpegTsProcessor.VideoStreamSnapshot snapshot = null;
        VideoKeyframeDecimator decimator = null;
        try {
            processor.setInjectExtradata(false);
            assertTrue(processor.openStream());
            processor.queryEmbeddedStreams();
            snapshot = processor.getVideoStreamSnapshot();
            assertNotNull(snapshot);

            decimator = new VideoKeyframeDecimator(output.toString(), 3, snapshot);
            decimator.setFileCloseCallback(processor::stopProcessingStream);
            processor.addVideoDataBufferListener(decimator, false);
            processor.processStream();
            processor.join(15_000L);
            assertFalse("Inline decimation must finish before finalization", processor.isAlive());
            decimator.closeFile();
            decimator = null;

            assertTrue("Completed decimated MP4 must reopen and contain a video packet",
                    VideoRetention.validateDecimatedFile(output));
        } finally {
            processor.stopProcessingStream();
            processor.join(5_000L);
            processor.closeStream();
            if (decimator != null) {
                try {
                    decimator.closeFile();
                } catch (IOException ignored) {
                    // The assertion above reports the primary failure.
                }
            }
            if (snapshot != null && snapshot.codecParameters() != null) {
                avcodec_parameters_free(snapshot.codecParameters());
            }
        }
    }

    private static Path bundledH264Fixture() {
        String configuredFixture = System.getenv("H264_RETENTION_TEST_FILE");
        if (configuredFixture != null && Files.isRegularFile(Path.of(configuredFixture))) {
            return Path.of(configuredFixture);
        }

        Path parentBuildFixture = Path.of(
                "include/osh-oakridge-modules/sensors/sensorhub-driver-ffmpeg/src/test/resources/"
                        + "org/sensorhub/impl/sensor/ffmpeg/sample-stream.ts").toAbsolutePath();
        if (Files.isRegularFile(parentBuildFixture)) {
            return parentBuildFixture;
        }

        Path moduleBuildFixture = Path.of(
                "sensors/sensorhub-driver-ffmpeg/src/test/resources/"
                        + "org/sensorhub/impl/sensor/ffmpeg/sample-stream.ts").toAbsolutePath();
        assertTrue("Bundled H.264 retention fixture is missing", Files.isRegularFile(moduleBuildFixture));
        return moduleBuildFixture;
    }
}
