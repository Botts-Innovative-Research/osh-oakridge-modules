package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.junit.Assert.*;

public class MpegTsProcessorLifecycleTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void probeBudgetIsBoundedByConnectTimeout() {
        var processor = new MpegTsProcessor("unused");
        processor.setTimeout(5_000_000L);
        assertEquals(5_000_000L, processor.getAnalyzeDurationMicros());
        assertEquals(5_000_000L, processor.getProbeSizeBytes(processor.getAnalyzeDurationMicros()));

        processor.setTimeout(60_000_000L);
        assertEquals(15_000_000L, processor.getAnalyzeDurationMicros());
        assertEquals(10_000_000L, processor.getProbeSizeBytes(processor.getAnalyzeDurationMicros()));
    }

    @Test
    public void incompleteInputIsClosedAndMetadataCannotEscape() throws Exception {
        File emptyInput = temporaryFolder.newFile("incomplete.mp4");
        var processor = new MpegTsProcessor(emptyInput.getAbsolutePath());

        assertFalse(processor.openStream());
        assertFalse(processor.isStreamOpen());
        assertNull(processor.getVideoStreamSnapshot());
        processor.closeStream();
    }

    @Test
    public void encodedDeliveryPacketIsFreedWithoutFreeingReusableDemuxContainer() {
        AVPacket demuxPacket = av_packet_alloc();
        AVPacket encodedPacket = av_packet_alloc();
        assertNotNull(demuxPacket);
        assertNotNull(encodedPacket);
        assertEquals(0, av_new_packet(demuxPacket, 8));
        assertEquals(0, av_new_packet(encodedPacket, 8));

        MpegTsProcessor.releaseIterationPackets(demuxPacket, encodedPacket);

        assertFalse("Demux packet container must remain reusable", demuxPacket.isNull());
        assertTrue("Demux packet payload must be unreferenced",
                demuxPacket.data() == null || demuxPacket.data().isNull());
        assertTrue("Distinct encoder packet must be freed", encodedPacket.isNull());
        av_packet_free(demuxPacket);
    }

    @Test
    public void legacyPresentationTimestampFallsBackToDtsWithoutInventingPts() {
        assertEquals(1.5, MpegTsProcessor.presentationTimestamp(
                AV_NOPTS_VALUE, 135_000L, 1.0 / 90_000.0), 0.000_001);
        assertTrue(Double.isNaN(MpegTsProcessor.presentationTimestamp(
                AV_NOPTS_VALUE, AV_NOPTS_VALUE, 1.0 / 90_000.0)));
    }

    @Test
    public void missingStreamDurationFallsBackToExactContainerDuration() {
        assertEquals(900_000L,
                MpegTsProcessor.resolveVideoDuration(0, 10_000_000L, 1, 90_000));
        assertEquals(123_456L,
                MpegTsProcessor.resolveVideoDuration(123_456L, 10_000_000L, 1, 90_000));
        assertEquals(0L,
                MpegTsProcessor.resolveVideoDuration(0, 0, 1, 90_000));
    }

    @Test
    public void nonH264TranscoderStopsAndDeliversOwnedPackets() throws Exception {
        String fixture = System.getenv("MJPEG_TEST_FILE");
        org.junit.Assume.assumeTrue("Set MJPEG_TEST_FILE for the native non-H264 regression",
                fixture != null && new File(fixture).isFile());

        var processor = new MpegTsProcessor(fixture, 5, false, false);
        var packets = new AtomicInteger();
        var threadFailure = new AtomicReference<Throwable>();
        processor.setUncaughtExceptionHandler((thread, failure) -> threadFailure.set(failure));
        processor.addVideoDataBufferListener(new DataBufferListener() {
            @Override public void onDataBuffer(DataBufferRecord record) { packets.incrementAndGet(); }
            @Override public boolean isWriting() { return true; }
            @Override public String getName() { return "NON-H264-OWNERSHIP-TEST"; }
        }, false);

        try {
            assertTrue(processor.openStream());
            processor.queryEmbeddedStreams();
            assertNotEquals("Fixture must exercise transcoding", AV_CODEC_ID_H264, processor.getCodecId());
            processor.processStream();
            processor.join(15_000L);
            assertFalse("Processor/coder teardown must not hang", processor.isAlive());
            assertNull("Transcoder worker must not fail", threadFailure.get());
            assertTrue("Transcoder must deliver at least one owned encoded packet", packets.get() > 0);
        } finally {
            processor.stopProcessingStream();
            processor.join(5_000L);
            processor.closeStream();
        }
    }
}
