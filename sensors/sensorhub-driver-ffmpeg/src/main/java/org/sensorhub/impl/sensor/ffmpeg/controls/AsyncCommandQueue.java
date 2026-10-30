package org.sensorhub.impl.sensor.ffmpeg.controls;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/** A non-blocking FIFO for asynchronous control operations. */
final class AsyncCommandQueue<T> {
    private static final int DEFAULT_MAX_PENDING = 64;
    private final Object lock = new Object();
    private final int maxPending;
    private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
    private int pending;

    AsyncCommandQueue() {
        this(DEFAULT_MAX_PENDING);
    }

    AsyncCommandQueue(int maxPending) {
        if (maxPending <= 0)
            throw new IllegalArgumentException("maxPending must be positive");
        this.maxPending = maxPending;
    }

    CompletableFuture<T> submit(Supplier<CompletableFuture<T>> operation) {
        Objects.requireNonNull(operation, "operation");
        CompletableFuture<T> exposed = new CompletableFuture<>();
        CompletableFuture<Void> operationDone = new CompletableFuture<>();
        CompletableFuture<Void> predecessor;
        synchronized (lock) {
            if (pending >= maxPending) {
                exposed.completeExceptionally(new RejectedExecutionException(
                        "Asynchronous command queue is full"));
                return exposed;
            }
            predecessor = tail;
            // A caller may cancel its exposed future. The queue nevertheless waits for the real
            // operationDone boundary before releasing the next camera command.
            tail = operationDone.handle((ignored, failure) -> null);
            pending++;
        }

        operationDone.whenComplete((ignored, failure) -> {
            synchronized (lock) {
                pending--;
            }
        });

        predecessor.whenComplete((ignored, predecessorFailure) -> {
            final CompletableFuture<T> submitted;
            try {
                submitted = Objects.requireNonNull(operation.get(),
                        "Queued operation returned a null future");
            } catch (Throwable failure) {
                exposed.completeExceptionally(failure);
                operationDone.complete(null);
                return;
            }

            submitted.whenComplete((value, failure) -> {
                if (failure == null)
                    exposed.complete(value);
                else
                    exposed.completeExceptionally(unwrap(failure));
                operationDone.complete(null);
            });
        });
        return exposed;
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException && current.getCause() != null)
            current = current.getCause();
        return current;
    }
}
