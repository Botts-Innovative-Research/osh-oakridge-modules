package org.sensorhub.mpegts;

import org.junit.Assume;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class MpegTsProcessorTranscodeTimingTest {

    @Test
    public void ntscFrameRateKeepsItsExactRational() {
        assertArrayEquals(new int[] {30_000, 1_001},
                MpegTsProcessor.selectTranscoderFrameRate(30_000, 1_001, 30));
    }

    @Test
    public void mjpegTranscodePublishesConstantFrameRateTimestampsAndStops() throws Exception {
        String fixture = System.getenv("MJPEG_TRANSCODE_TEST_FILE");
        Assume.assumeTrue("Set MJPEG_TRANSCODE_TEST_FILE for native transcode regression",
                fixture != null && Files.isRegularFile(Path.of(fixture)));

        MpegTsProcessor processor = new MpegTsProcessor(fixture, 5, false, false);
        List<DataBufferRecord> records = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        processor.setUncaughtExceptionHandler((thread, throwable) -> failure.set(throwable));
        processor.addVideoDataBufferListener(new DataBufferListener() {
            @Override
            public void onDataBuffer(DataBufferRecord record) {
                records.add(record);
            }

            @Override
            public boolean isWriting() {
                return true;
            }

            @Override
            public String getName() {
                return "MJPEG-TIMING-TEST";
            }
        }, false);

        try {
            assertTrue("MJPEG fixture did not open", processor.openStream());
            processor.queryEmbeddedStreams();
            processor.processStream();
            processor.join(20_000L);

            assertFalse("MJPEG processor did not stop at EOF", processor.isAlive());
            assertNull("MJPEG transcoder failed", failure.get());
            assertTrue("MJPEG transcoder published too few H.264 packets", records.size() >= 5);

            double previous = Double.NEGATIVE_INFINITY;
            for (DataBufferRecord record : records) {
                assertTrue("Transcoded packet timestamp is not finite",
                        Double.isFinite(record.getPresentationTimestamp()));
                assertTrue("Transcoded packet timestamps are not strictly increasing",
                        record.getPresentationTimestamp() > previous);
                assertTrue("Transcoded packet is missing packet-level timing", record.hasPacketTiming());
                previous = record.getPresentationTimestamp();
            }

            double observedDuration = records.get(records.size() - 1).getPresentationTimestamp()
                    - records.get(0).getPresentationTimestamp();
            double expectedDuration = (records.size() - 1) / 5.0;
            assertEquals("Encoded timeline does not match the 5 FPS fixture",
                    expectedDuration, observedDuration, 0.05);
        } finally {
            processor.stopProcessingStream();
            processor.join(5_000L);
            processor.closeStream();
        }
    }
}
