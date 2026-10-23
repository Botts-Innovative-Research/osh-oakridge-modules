package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;

import java.io.File;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.av_make_q;
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
    public void outputMetadataSnapshotDoesNotWaitForBlockingInputContext() throws Exception {
        String fixture = Paths.get(FFMPEGSensor.class.getResource("sample-stream.ts").toURI())
                .toString();
        var processor = new MpegTsProcessor(fixture);
        assertTrue(processor.openStream());

        ExecutorService snapshotExecutor = Executors.newSingleThreadExecutor();
        MpegTsProcessor.VideoStreamSnapshot snapshot = null;
        try {
            // Models av_read_frame() holding the input-context lock. Recording startup must obtain
            // its owned codec metadata without waiting for that blocking read to finish.
            synchronized (processor.contextLock) {
                Future<MpegTsProcessor.VideoStreamSnapshot> snapshotFuture =
                        snapshotExecutor.submit(processor::getVideoStreamSnapshot);
                snapshot = snapshotFuture.get(5, TimeUnit.SECONDS);
            }

            assertNotNull(snapshot);
            assertTrue(snapshot.codecParameters().codec_id() != AV_CODEC_ID_NONE);
            assertTrue(snapshot.codecParameters().width() > 0);
            assertTrue(snapshot.codecParameters().height() > 0);
            assertTrue(snapshot.timeBaseNumerator() > 0);
            assertTrue(snapshot.timeBaseDenominator() > 0);

            avcodec_parameters_free(snapshot.codecParameters());
            snapshot = null;
            processor.closeStream();
            assertNull("Closed streams must not expose cached output metadata",
                    processor.getVideoStreamSnapshot());
        } finally {
            if (snapshot != null && snapshot.codecParameters() != null
                    && !snapshot.codecParameters().isNull()) {
                avcodec_parameters_free(snapshot.codecParameters());
            }
            snapshotExecutor.shutdownNow();
            processor.closeStream();
            assertFalse(processor.isStreamOpen());
            assertNull(processor.getVideoStreamSnapshot());
        }
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
    public void closeBoundaryDrainsAcceptedCallbacksAndRejectsStalePackets() throws Exception {
        var processor = new MpegTsProcessor("unused");
        var writing = new AtomicBoolean(true);
        var delivered = new ConcurrentLinkedQueue<Integer>();
        var firstCallbackEntered = new CountDownLatch(1);
        var releaseFirstCallback = new CountDownLatch(1);
        var closeActionEntered = new CountDownLatch(1);
        var releaseCloseAction = new CountDownLatch(1);
        var newGenerationDelivered = new CountDownLatch(1);
        var listener = new DataBufferListener() {
            @Override
            public void onDataBuffer(DataBufferRecord record) {
                if (!writing.get()) {
                    return;
                }
                int marker = record.getDataBuffer()[0];
                delivered.add(marker);
                if (marker == 1) {
                    firstCallbackEntered.countDown();
                    try {
                        assertTrue(releaseFirstCallback.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                } else if (marker == 4) {
                    newGenerationDelivered.countDown();
                }
            }

            @Override public boolean isWriting() { return writing.get(); }
            @Override public String getName() { return "ORDERED-FILE-OUTPUT-TEST"; }
            @Override public DeliveryMode getDeliveryMode() { return DeliveryMode.BUFFERED; }
        };
        var closeExecutor = Executors.newSingleThreadExecutor();

        processor.addVideoDataBufferListener(listener);
        try {
            processor.notifyVideoDataBufferListeners(record(1), EnumSet.of(DeliveryMode.BUFFERED));
            assertTrue(firstCallbackEntered.await(5, TimeUnit.SECONDS));
            processor.notifyVideoDataBufferListeners(record(2), EnumSet.of(DeliveryMode.BUFFERED));

            var closeResult = closeExecutor.submit(() -> processor.executeAfterPendingVideoData(listener, () -> {
                closeActionEntered.countDown();
                assertTrue(releaseCloseAction.await(5, TimeUnit.SECONDS));
                writing.set(false);
                return List.copyOf(delivered);
            }));

            releaseFirstCallback.countDown();
            assertTrue(closeActionEntered.await(5, TimeUnit.SECONDS));

            // This callback is accepted while close is at the executor boundary, but it belongs to
            // the old generation and must not be allowed into the next recording.
            processor.notifyVideoDataBufferListeners(record(3), EnumSet.of(DeliveryMode.BUFFERED));
            releaseCloseAction.countDown();
            assertEquals(List.of(1, 2), closeResult.get(5, TimeUnit.SECONDS));

            writing.set(true);
            processor.notifyVideoDataBufferListeners(record(4), EnumSet.of(DeliveryMode.BUFFERED));
            assertTrue(newGenerationDelivered.await(5, TimeUnit.SECONDS));
            assertEquals(List.of(1, 2, 4), List.copyOf(delivered));
        } finally {
            releaseFirstCallback.countDown();
            releaseCloseAction.countDown();
            closeExecutor.shutdownNow();
            processor.clearVideoDataBufferListeners();
        }
    }

    @Test
    public void closeBoundaryDrainsFinitePreRollBeforeClosing() throws Exception {
        var processor = new MpegTsProcessor("unused");
        var writing = new AtomicBoolean(false);
        var events = new CopyOnWriteArrayList<Integer>();
        var firstCallbackEntered = new CountDownLatch(1);
        var releaseFirstCallback = new CountDownLatch(1);
        var listener = new DataBufferListener() {
            @Override
            public void onDataBuffer(DataBufferRecord record) {
                if (!writing.get()) {
                    return;
                }
                int marker = record.getDataBuffer()[0];
                events.add(marker);
                if (marker == 1) {
                    firstCallbackEntered.countDown();
                    try {
                        assertTrue(releaseFirstCallback.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }
            }

            @Override public boolean isWriting() { return writing.get(); }
            @Override public String getName() { return "BUFFERED-CLOSE-TEST"; }
            @Override public DeliveryMode getDeliveryMode() { return DeliveryMode.BUFFERED; }
        };
        var closeExecutor = Executors.newSingleThreadExecutor();

        processor.setFrameBuffer(4);
        processor.addVideoDataBufferListener(listener);
        try {
            for (int marker = 1; marker <= 3; marker++) {
                queueMarker(processor, marker);
            }

            writing.set(true);
            var closeResult = closeExecutor.submit(() -> processor.executeAfterPendingVideoData(listener, () -> {
                events.add(-1);
                writing.set(false);
                return List.copyOf(events);
            }));

            assertTrue(firstCallbackEntered.await(5, TimeUnit.SECONDS));
            assertFalse("Close must wait behind every detached pre-roll callback", closeResult.isDone());

            // These packets arrive after the cutoff. They must be retained in the rotated queue,
            // not extend this close indefinitely or leak into the clip being finalized.
            queueMarker(processor, 4);
            queueMarker(processor, 5);
            releaseFirstCallback.countDown();

            assertEquals(List.of(1, 2, 3, -1), closeResult.get(5, TimeUnit.SECONDS));

            writing.set(true);
            var nextCloseResult = closeExecutor.submit(() -> processor.executeAfterPendingVideoData(listener, () -> {
                events.add(-2);
                writing.set(false);
                return List.copyOf(events);
            }));
            assertEquals(List.of(1, 2, 3, -1, 4, 5, -2),
                    nextCloseResult.get(5, TimeUnit.SECONDS));
        } finally {
            releaseFirstCallback.countDown();
            closeExecutor.shutdownNow();
            processor.shutdownExecutors();
        }
    }

    private static DataBufferRecord record(int marker) {
        return new DataBufferRecord(0.0, new byte[] {(byte) marker}, true);
    }

    private static AVPacket markerPacket(int marker) {
        AVPacket packet = av_packet_alloc();
        assertNotNull(packet);
        assertEquals(0, av_new_packet(packet, 1));
        packet.data().position(0).put(new byte[] {(byte) marker}, 0, 1);
        packet.pts(marker);
        packet.dts(marker);
        packet.duration(1);
        packet.time_base(av_make_q(1, 1_000));
        return packet;
    }

    private static void queueMarker(MpegTsProcessor processor, int marker) {
        AVPacket packet = markerPacket(marker);
        try {
            processor.submitPacketToFrameQueue(packet);
        } finally {
            av_packet_free(packet);
        }
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
