package com.botts.impl.service.oscar.stats;

import com.botts.impl.service.oscar.OSCARSystem;
import com.google.common.collect.Range;
import net.opengis.swe.v20.DataBlock;
import org.junit.Test;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.ICancellableDataStore;
import org.sensorhub.impl.utils.rad.RADHelper;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class StatisticsOutputShutdownTest {

    @Test
    public void legacyEpochSecondsTimestampIsDecodedWithoutChangingModernInstants() {
        DataBlock legacy = new RADHelper().createSiteStatistics().createDataBlock();
        Instant intendedLegacyTime = Instant.parse("2026-10-23T12:34:56Z");
        legacy.setLongValue(0, intendedLegacyTime.getEpochSecond());

        assertEquals(intendedLegacyTime,
                StatisticsOutput.decodeStoredStatisticsTimestamp(legacy));

        DataBlock modern = new RADHelper().createSiteStatistics().createDataBlock();
        Instant modernTime = Instant.parse("2026-10-23T12:34:56.123Z");
        modern.setTimeStamp(0, modernTime);

        assertEquals(modernTime,
                StatisticsOutput.decodeStoredStatisticsTimestamp(modern));
    }

    @Test
    public void publicationTimesMatchSweMillisecondsAndRemainStrictlyIncreasing() {
        Instant candidate = Instant.parse("2026-10-23T12:34:56.123456789Z");
        assertEquals(Instant.parse("2026-10-23T12:34:56.123Z"),
                StatisticsOutput.normalizePublicationTime(candidate));

        Instant prior = Instant.parse("2026-10-23T12:34:56.123900Z");
        assertEquals(Instant.parse("2026-10-23T12:34:56.124Z"),
                StatisticsOutput.publicationTimeAfter(candidate, prior));
    }

    @Test
    public void incrementalRangeExcludesPriorCutoffAndIncludesNewCutoff() {
        Instant prior = Instant.parse("2026-10-23T12:34:56.123456Z");
        Instant current = Instant.parse("2026-10-23T12:35:56.654321Z");
        Range<Instant> range = StatisticsOutput.timeRange(prior, current, true);

        assertFalse(range.contains(prior));
        assertTrue(range.contains(prior.plusNanos(1L)));
        assertTrue(range.contains(current));
        assertFalse(range.contains(current.plusNanos(1L)));
    }

    @Test
    public void fullPublicationsNeverRunConcurrently() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        SerializedStatisticsOutput output = new SerializedStatisticsOutput(
                firstStarted, releaseFirst, secondEntered);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();

        Thread first = publicationThread(output, firstFailure, "fixture-statistics-first");
        Thread second = publicationThread(output, secondFailure, "fixture-statistics-second");
        try {
            first.start();
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            second.start();

            assertFalse("A queued full publication entered while the first still held the lock",
                    secondEntered.await(150, TimeUnit.MILLISECONDS));
            releaseFirst.countDown();
            assertTrue(secondEntered.await(2, TimeUnit.SECONDS));

            first.join(2_000L);
            second.join(2_000L);
            assertFalse(first.isAlive());
            assertFalse(second.isAlive());
            assertNull(firstFailure.get());
            assertNull(secondFailure.get());
            assertEquals(2, output.invocations.get());
            assertEquals("At most one full statistics publication may run", 1,
                    output.maxConcurrency.get());
        } finally {
            releaseFirst.countDown();
            first.join(2_000L);
            second.join(2_000L);
        }
    }

    @Test
    public void queuedPublicationCannotStartAfterStop() throws Exception {
        CountDownLatch ownerStarted = new CountDownLatch(1);
        CountDownLatch cancellationReleasedOwner = new CountDownLatch(1);
        StopWaiterStatisticsOutput output = new StopWaiterStatisticsOutput(
                ownerStarted, cancellationReleasedOwner);
        AtomicReference<Throwable> ownerFailure = new AtomicReference<>();
        AtomicReference<Throwable> waiterFailure = new AtomicReference<>();
        CountDownLatch waiterAttempted = new CountDownLatch(1);

        Thread owner = publicationThread(output, ownerFailure, "fixture-statistics-owner");
        Thread waiter = new Thread(() -> {
            waiterAttempted.countDown();
            try {
                output.publishLatestStatistics();
            } catch (Throwable error) {
                waiterFailure.set(error);
            }
        }, "fixture-statistics-waiter");
        try {
            owner.start();
            assertTrue(ownerStarted.await(2, TimeUnit.SECONDS));
            waiter.start();
            assertTrue(waiterAttempted.await(2, TimeUnit.SECONDS));

            output.requestStop();
            output.awaitStop(2, TimeUnit.SECONDS);
            assertTrue(cancellationReleasedOwner.await(2, TimeUnit.SECONDS));

            owner.join(2_000L);
            waiter.join(2_000L);
            assertFalse(owner.isAlive());
            assertFalse(waiter.isAlive());
            assertNull(ownerFailure.get());
            assertTrue("The queued publication must be rejected after stop",
                    waiterFailure.get() instanceof CancellationException);
            assertEquals("Only the lock owner may enter publication work", 1,
                    output.invocations.get());
        } finally {
            output.requestStop();
            owner.join(2_000L);
            waiter.join(2_000L);
        }
    }

    @Test
    public void queuedPublicationFromStoppedLifecycleCannotRunAfterRestart() throws Exception {
        CountDownLatch restartedPublicationStarted = new CountDownLatch(1);
        RestartStatisticsOutput output = new RestartStatisticsOutput(restartedPublicationStarted);
        AtomicReference<Throwable> staleFailure = new AtomicReference<>();
        Thread staleWaiter = publicationThread(
                output, staleFailure, "fixture-statistics-stale-waiter");
        ReentrantLock publicationLock = publicationLock(output);

        publicationLock.lock();
        try {
            staleWaiter.start();
            assertTrue("Pre-stop publisher did not queue on the serialization lock",
                    awaitQueued(publicationLock, staleWaiter, 2, TimeUnit.SECONDS));

            output.requestStop();
            output.awaitStop(2, TimeUnit.SECONDS);
            output.start();
        } finally {
            publicationLock.unlock();
        }

        try {
            staleWaiter.join(2_000L);
            assertFalse(staleWaiter.isAlive());
            assertTrue("A publisher queued before stop must be invalid after restart",
                    staleFailure.get() instanceof CancellationException);
            assertTrue("The restarted lifecycle did not run its own scheduled publication",
                    restartedPublicationStarted.await(2, TimeUnit.SECONDS));
            assertEquals("Only the restarted lifecycle may publish", 1,
                    output.invocations.get());
        } finally {
            output.requestStop();
            output.awaitStop(2, TimeUnit.SECONDS);
            staleWaiter.join(2_000L);
        }
    }

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

    private static final class SerializedStatisticsOutput extends StatisticsOutput {
        private final CountDownLatch firstStarted;
        private final CountDownLatch releaseFirst;
        private final CountDownLatch secondEntered;
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger invocations = new AtomicInteger();
        private final AtomicInteger maxConcurrency = new AtomicInteger();

        SerializedStatisticsOutput(CountDownLatch firstStarted,
                                   CountDownLatch releaseFirst,
                                   CountDownLatch secondEntered) {
            super(new TestSystem(), databaseProxy(), 60);
            this.firstStarted = firstStarted;
            this.releaseFirst = releaseFirst;
            this.secondEntered = secondEntered;
        }

        @Override
        protected void publishLatestStatisticsWithinScope() {
            int invocation = invocations.incrementAndGet();
            int concurrency = active.incrementAndGet();
            maxConcurrency.accumulateAndGet(concurrency, Math::max);
            try {
                if (invocation == 1) {
                    firstStarted.countDown();
                    awaitIgnoringInterrupts(releaseFirst);
                } else {
                    secondEntered.countDown();
                }
            } finally {
                active.decrementAndGet();
            }
        }
    }

    private static final class StopWaiterStatisticsOutput extends StatisticsOutput {
        private final CountDownLatch ownerStarted;
        private final CountDownLatch cancellationReleasedOwner;
        private final AtomicInteger invocations = new AtomicInteger();

        StopWaiterStatisticsOutput(CountDownLatch ownerStarted,
                                   CountDownLatch cancellationReleasedOwner) {
            super(new TestSystem(), databaseProxy(), 60);
            this.ownerStarted = ownerStarted;
            this.cancellationReleasedOwner = cancellationReleasedOwner;
        }

        @Override
        protected void publishLatestStatisticsWithinScope() {
            invocations.incrementAndGet();
            ownerStarted.countDown();
            awaitIgnoringInterrupts(cancellationReleasedOwner);
        }

        @Override
        protected ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
            return new ICancellableDataStore.CancellableOperation() {
                private volatile boolean cancelled;

                @Override
                public void cancel() {
                    cancelled = true;
                    cancellationReleasedOwner.countDown();
                }

                @Override
                public boolean isCancellationRequested() {
                    return cancelled;
                }

                @Override
                public void close() {
                }
            };
        }
    }

    private static final class RestartStatisticsOutput extends StatisticsOutput {
        private final CountDownLatch publicationStarted;
        private final AtomicInteger invocations = new AtomicInteger();

        RestartStatisticsOutput(CountDownLatch publicationStarted) {
            super(new TestSystem(), databaseProxy(), 60);
            this.publicationStarted = publicationStarted;
        }

        @Override
        protected void publishLatestStatisticsWithinScope() {
            invocations.incrementAndGet();
            publicationStarted.countDown();
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

    private static Thread publicationThread(StatisticsOutput output,
                                            AtomicReference<Throwable> failure,
                                            String name) {
        return new Thread(() -> {
            try {
                output.publishLatestStatistics();
            } catch (Throwable error) {
                failure.set(error);
            }
        }, name);
    }

    private static ReentrantLock publicationLock(StatisticsOutput output) throws Exception {
        var field = StatisticsOutput.class.getDeclaredField("publicationLock");
        field.setAccessible(true);
        return (ReentrantLock) field.get(output);
    }

    private static boolean awaitQueued(ReentrantLock lock, Thread thread,
                                       long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (lock.hasQueuedThread(thread))
                return true;
            Thread.sleep(5L);
        }
        return lock.hasQueuedThread(thread);
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
