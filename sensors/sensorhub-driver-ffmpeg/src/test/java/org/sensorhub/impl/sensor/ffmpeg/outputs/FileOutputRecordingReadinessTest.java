package org.sensorhub.impl.sensor.ffmpeg.outputs;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileOutputRecordingReadinessTest {

    @Test
    public void firstMuxedPacketReleasesCurrentGeneration() throws Exception {
        FileOutput.RecordingReadiness readiness = new FileOutput.RecordingReadiness();
        long generation = readiness.start();

        CompletableFuture<Boolean> waiting = CompletableFuture.supplyAsync(() -> {
            try {
                return readiness.await(generation, Duration.ofSeconds(1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        });

        assertFalse("Readiness was reported before a packet was muxed", waiting.isDone());
        readiness.packetWritten(generation);
        assertTrue(waiting.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void stoppedGenerationWakesWaiterWithoutReadiness() throws Exception {
        FileOutput.RecordingReadiness readiness = new FileOutput.RecordingReadiness();
        long generation = readiness.start();
        CompletableFuture<Boolean> waiting = CompletableFuture.supplyAsync(() -> {
            try {
                return readiness.await(generation, Duration.ofSeconds(2));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        });

        readiness.stop(generation);
        assertFalse(waiting.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void waitTimesOutWhenNoPacketArrives() throws Exception {
        FileOutput.RecordingReadiness readiness = new FileOutput.RecordingReadiness();
        long generation = readiness.start();
        long started = System.nanoTime();

        assertFalse(readiness.await(generation, Duration.ofMillis(40)));
        assertTrue("Bounded readiness wait returned before its timeout",
                System.nanoTime() - started >= TimeUnit.MILLISECONDS.toNanos(20));
    }

    @Test
    public void stalePacketCannotReleaseNextGeneration() throws Exception {
        FileOutput.RecordingReadiness readiness = new FileOutput.RecordingReadiness();
        long firstGeneration = readiness.start();
        readiness.packetWritten(firstGeneration);
        assertTrue(readiness.await(firstGeneration, Duration.ofMillis(10)));

        long secondGeneration = readiness.start();
        readiness.packetWritten(firstGeneration);
        assertFalse("A stale packet released a later recording generation",
                readiness.await(secondGeneration, Duration.ofMillis(40)));

        readiness.packetWritten(secondGeneration);
        assertTrue(readiness.await(secondGeneration, Duration.ofMillis(10)));
    }
}
