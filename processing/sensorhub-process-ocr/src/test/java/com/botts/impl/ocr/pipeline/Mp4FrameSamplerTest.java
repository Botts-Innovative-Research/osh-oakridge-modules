package com.botts.impl.ocr.pipeline;

import com.botts.impl.ocr.onnx.OnnxOcrEngine;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Exercises the sampler against a real recorded MP4 (fixture: 4 s, 24 fps,
 * keyframe interval 15 — matching the lane camera config — showing the
 * container number CSQU 3054383).
 */
public class Mp4FrameSamplerTest {

    static String fixturePath;

    @BeforeClass
    public static void extractFixture() throws IOException {
        try (InputStream in = Mp4FrameSamplerTest.class.getResourceAsStream("/ocr/container.mp4")) {
            assertNotNull("fixture /ocr/container.mp4 missing from test resources", in);
            File temp = File.createTempFile("ocr-fixture", ".mp4");
            temp.deleteOnExit();
            Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            fixturePath = temp.getAbsolutePath();
        }
    }

    @Test
    public void samplesEvenlySpreadDecodedKeyframes() throws Exception {
        List<Mp4FrameSampler.Frame> frames = new Mp4FrameSampler().sample(fixturePath, 4);

        assertFalse("no frames decoded", frames.isEmpty());
        assertTrue("expected at most 4 frames, got " + frames.size(), frames.size() <= 4);
        for (Mp4FrameSampler.Frame frame : frames) {
            assertEquals(896, frame.image.getWidth());
            assertEquals(224, frame.image.getHeight());
        }
        for (int i = 1; i < frames.size(); i++)
            assertTrue("frame PTS not ascending", frames.get(i).ptsSeconds >= frames.get(i - 1).ptsSeconds);
    }

    @Test
    public void capsFrameCountAtRequestedMax() throws Exception {
        List<Mp4FrameSampler.Frame> two = new Mp4FrameSampler().sample(fixturePath, 2);
        assertTrue(two.size() <= 2);
    }

    @Test
    public void failsCleanlyOnTruncatedFile() throws Exception {
        File truncated = File.createTempFile("ocr-truncated", ".mp4");
        truncated.deleteOnExit();
        byte[] all = Files.readAllBytes(Paths.get(fixturePath));
        Files.write(truncated.toPath(), java.util.Arrays.copyOf(all, 100));

        try {
            // no retries so the test doesn't sit through the backoff schedule
            new Mp4FrameSampler(new long[0]).sample(truncated.getAbsolutePath(), 4);
            fail("expected IOException on truncated file");
        } catch (IOException expected) {
        }
    }

    /** Full offline pipeline: recorded clip -> frames -> ONNX OCR -> validated candidate. */
    @Test
    public void pipelineReadsContainerNumberFromRecordedClip() throws Exception {
        Path modelDir = Paths.get(System.getProperty("ocrModelDir", "../../../../dist/models/ocr"));
        assumeTrue("OCR models not present, skipping pipeline test",
                Files.isReadable(modelDir.resolve(OnnxOcrEngine.DET_MODEL)));

        OcrSettings settings = new OcrSettings();
        List<CandidateAggregator.FrameRead> frameReads = new ArrayList<>();

        try (OnnxOcrEngine engine = new OnnxOcrEngine()) {
            engine.init(modelDir);
            for (Mp4FrameSampler.Frame frame : new Mp4FrameSampler().sample(fixturePath, settings.framesPerVideo)) {
                for (var read : engine.read(frame.image))
                    frameReads.add(new CandidateAggregator.FrameRead(read, frame.image, "test-cam", Instant.now()));
            }
        }

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings).aggregate(frameReads);

        assertTrue("no valid container candidate from clip; reads=" + frameReads.size(),
                candidates.stream().anyMatch(c ->
                        VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType)
                                && "CSQU3054383".equals(c.normalizedValue)
                                && c.checksumValid));

        CandidateAggregator.Candidate best = candidates.get(0);
        assertTrue("expected multi-frame agreement, readCount=" + best.readCount, best.readCount >= 2);
        assertNotNull(best.evidenceFrame);
        assertNotNull(best.evidenceBox);
    }
}
