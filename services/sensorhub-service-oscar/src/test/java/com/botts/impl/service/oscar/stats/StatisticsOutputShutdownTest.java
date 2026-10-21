package com.botts.impl.service.oscar.stats;

import com.botts.impl.service.oscar.OSCARSystem;
import org.junit.Test;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.ICancellableDataStore;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class StatisticsOutputShutdownTest {

    @Test
    public void workerFinishingAtStopBoundaryCannotEscapeCancellationTracking() throws Exception {
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        CountDownLatch scopeClosed = new CountDownLatch(1);
        CountDownLatch stopOwnsWorkMonitor = new CountDownLatch(1);
        CountDownLatch allowCancellationMark = new CountDownLatch(1);
        CountDownLatch cancellationStarted = new CountDownLatch(1);
        CountDownLatch releaseCancellation = new CountDownLatch(1);

        BoundaryStatisticsOutput output = new BoundaryStatisticsOutput(
                workerStarted, releaseWorker, scopeClosed, stopOwnsWorkMonitor,
                allowCancellationMark, cancellationStarted, releaseCancellation);
        Thread requester = new Thread(output::requestStop, "fixture-statistics-stop-request");
        try {
            output.start();
            assertTrue(workerStarted.await(2, TimeUnit.SECONDS));
            requester.start();
            assertTrue("Stop did not reach the active-work marking boundary",
                    stopOwnsWorkMonitor.await(2, TimeUnit.SECONDS));

            // Let the worker close its datastore scope while requestStop still owns the work
            // monitor. It will then block immediately before workerFinished can remove the work.
            releaseWorker.countDown();
            assertTrue(scopeClosed.await(2, TimeUnit.SECONDS));
            allowCancellationMark.countDown();
            requester.join(2_000L);
            assertFalse("The non-blocking request phase did not return", requester.isAlive());
            assertTrue(cancellationStarted.await(2, TimeUnit.SECONDS));

            assertThrows("Successful stop must wait for the already-marked cancellation helper",
                    IllegalStateException.class,
                    () -> output.awaitStop(100, TimeUnit.MILLISECONDS));

            releaseCancellation.countDown();
            output.awaitStop(2, TimeUnit.SECONDS);
        } finally {
            releaseWorker.countDown();
            allowCancellationMark.countDown();
            releaseCancellation.countDown();
            requester.join(2_000L);
            try {
                output.requestStop();
                output.awaitStop(2, TimeUnit.SECONDS);
            } catch (RuntimeException ignored) {
                // Preserve the primary assertion if fixture setup failed.
            }
        }
    }

    private static final class BoundaryStatisticsOutput extends StatisticsOutput {
        private final CountDownLatch workerStarted;
        private final CountDownLatch releaseWorker;
        private final CountDownLatch scopeClosed;
        private final CountDownLatch stopOwnsWorkMonitor;
        private final CountDownLatch allowCancellationMark;
        private final CountDownLatch cancellationStarted;
        private final CountDownLatch releaseCancellation;

        BoundaryStatisticsOutput(CountDownLatch workerStarted,
                                 CountDownLatch releaseWorker,
                                 CountDownLatch scopeClosed,
                                 CountDownLatch stopOwnsWorkMonitor,
                                 CountDownLatch allowCancellationMark,
                                 CountDownLatch cancellationStarted,
                                 CountDownLatch releaseCancellation) {
            super(new TestSystem(), databaseProxy(), 60);
            this.workerStarted = workerStarted;
            this.releaseWorker = releaseWorker;
            this.scopeClosed = scopeClosed;
            this.stopOwnsWorkMonitor = stopOwnsWorkMonitor;
            this.allowCancellationMark = allowCancellationMark;
            this.cancellationStarted = cancellationStarted;
            this.releaseCancellation = releaseCancellation;
        }

        @Override
        protected void publishLatestStatisticsWithinScope() {
            workerStarted.countDown();
            awaitIgnoringInterrupts(releaseWorker);
        }

        @Override
        protected ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
            return new ICancellableDataStore.CancellableOperation() {
                private volatile boolean cancelled;

                @Override
                public void cancel() {
                    cancelled = true;
                    cancellationStarted.countDown();
                    awaitIgnoringInterrupts(releaseCancellation);
                }

                @Override
                public boolean isCancellationRequested() {
                    return cancelled;
                }

                @Override
                public void close() {
                    scopeClosed.countDown();
                }
            };
        }

        @Override
        void requestActiveWorkCancellationLocked() {
            stopOwnsWorkMonitor.countDown();
            awaitIgnoringInterrupts(allowCancellationMark);
            super.requestActiveWorkCancellationLocked();
        }
    }

    private static final class TestSystem extends OSCARSystem {
        TestSystem() {
            super("statistics-shutdown-test");
        }
    }

    private static IObsSystemDatabase databaseProxy() {
        return (IObsSystemDatabase) Proxy.newProxyInstance(
                IObsSystemDatabase.class.getClassLoader(),
                new Class<?>[]{IObsSystemDatabase.class},
                (proxy, method, arguments) -> null);
    }

    private static void awaitIgnoringInterrupts(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }
        if (interrupted)
            Thread.interrupted();
    }
}
