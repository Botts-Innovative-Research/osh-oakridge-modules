package org.sensorhub.impl.sensor.ffmpeg.controls;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AsyncCommandQueueTest {

    @Test
    public void callerCancellationCannotReleaseNextRealOperationEarly() {
        AsyncCommandQueue<String> queue = new AsyncCommandQueue<>();
        CompletableFuture<String> firstReal = new CompletableFuture<>();
        AtomicBoolean secondStarted = new AtomicBoolean();
        CompletableFuture<String> first = queue.submit(() -> firstReal);
        CompletableFuture<String> second = queue.submit(() -> {
            secondStarted.set(true);
            return CompletableFuture.completedFuture("second");
        });

        assertTrue(first.cancel(true));
        assertFalse(secondStarted.get());
        firstReal.complete("first");

        assertEquals("second", second.join());
        assertTrue(secondStarted.get());
    }

    @Test
    public void realFailureReleasesNextOperationInFifoOrder() {
        AsyncCommandQueue<String> queue = new AsyncCommandQueue<>();
        CompletableFuture<String> firstReal = new CompletableFuture<>();
        AtomicBoolean secondStarted = new AtomicBoolean();
        CompletableFuture<String> first = queue.submit(() -> firstReal);
        CompletableFuture<String> second = queue.submit(() -> {
            secondStarted.set(true);
            return CompletableFuture.completedFuture("second");
        });

        firstReal.completeExceptionally(new IllegalStateException("failed"));
        assertTrue(first.isCompletedExceptionally());
        assertEquals("second", second.join());
        assertTrue(secondStarted.get());
    }

    @Test
    public void hungOperationCannotGrowQueuePastConfiguredBound() {
        AsyncCommandQueue<String> queue = new AsyncCommandQueue<>(2);
        CompletableFuture<String> hung = new CompletableFuture<>();
        queue.submit(() -> hung);
        AtomicBoolean secondStarted = new AtomicBoolean();
        queue.submit(() -> {
            secondStarted.set(true);
            return CompletableFuture.completedFuture("second");
        });

        CompletableFuture<String> rejected = queue.submit(
                () -> CompletableFuture.completedFuture("third"));
        assertTrue(rejected.isCompletedExceptionally());
        assertFalse(secondStarted.get());

        hung.complete("first");
        assertTrue(secondStarted.get());
    }
}
