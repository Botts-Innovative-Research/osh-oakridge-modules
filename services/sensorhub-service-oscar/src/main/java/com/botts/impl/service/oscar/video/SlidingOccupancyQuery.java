package com.botts.impl.service.oscar.video;

import com.botts.impl.service.oscar.media.MediaIdentityResolver;
import com.botts.impl.service.oscar.media.MediaSetStore;
import com.botts.impl.service.oscar.media.RetentionScanCheckpoint;
import com.botts.impl.service.oscar.media.RetentionProcessedObservation;
import com.botts.impl.service.oscar.media.RetentionReconciliationCheckpoint;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.ICancellableDataStore;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Incrementally scans occupancy observations as they cross the media-retention cutoff.
 *
 * <p>The scan moves forward from a durable high-water mark. Each scheduled cycle covers at most
 * {@code maximumCatchUp}; each individual query covers at most {@code batchWindowLength}. A window
 * is checkpointed only after every selected occupancy has been handed to the durable media job
 * runner. Closed query boundaries deliberately replay a boundary timestamp, relying on media job
 * identity for idempotence rather than risking a precision-dependent gap.</p>
 */
public final class SlidingOccupancyQuery {
    private static final Logger logger = LoggerFactory.getLogger(SlidingOccupancyQuery.class);
    private static final Instant RETENTION_EPOCH = Instant.EPOCH;
    private static final long DEFAULT_STOP_TIMEOUT_MILLIS = 5_000L;

    private final Duration batchWindowLength;
    private final Duration retentionOffset;
    private final Duration maximumCatchUp;
    private final Duration lateArrivalOverlap;
    private final Duration reconciliationCatchUp;
    private final QueryAction afterQueryAction;
    private final CycleAction beforeQueryAction;
    private final ISensorHub hub;
    private final MediaIdentityResolver identityResolver;
    private final CursorStore cursorStore;
    private final Clock clock;

    private final Object cycleLock = new Object();
    private final Object operationLock = new Object();
    private volatile boolean stopRequested;
    private ICancellableDataStore.CancellableOperation activeOperation;
    private boolean cancellationStarted;
    private boolean cancellationComplete = true;
    private RuntimeException operationFailure;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    public SlidingOccupancyQuery(ISensorHub hub, TemporalAmount batchWindowLength,
                                 QueryAction afterQueryAction, TemporalAmount retentionOffset,
                                 TemporalAmount maximumCatchUp, MediaSetStore mediaStore,
                                 CycleAction beforeQueryAction,
                                 TemporalAmount lateArrivalOverlap,
                                 TemporalAmount reconciliationCatchUp) {
        this(hub, duration(batchWindowLength, "video query period"), afterQueryAction,
                duration(retentionOffset, "video retention offset"),
                duration(maximumCatchUp, "maximum retention catch-up"),
                new MediaCursorStore(mediaStore), beforeQueryAction, Clock.systemUTC(),
                duration(lateArrivalOverlap, "late-arrival overlap"),
                duration(reconciliationCatchUp, "reconciliation catch-up"));
    }

    SlidingOccupancyQuery(ISensorHub hub, Duration batchWindowLength,
                          QueryAction afterQueryAction, Duration retentionOffset,
                          Duration maximumCatchUp, CursorStore cursorStore,
                          CycleAction beforeQueryAction, Clock clock,
                          Duration lateArrivalOverlap, Duration reconciliationCatchUp) {
        if (batchWindowLength.isZero() || batchWindowLength.isNegative())
            throw new IllegalArgumentException("Video query period must be positive");
        if (retentionOffset.isNegative())
            throw new IllegalArgumentException("Video retention offset must not be negative");
        if (maximumCatchUp.isZero() || maximumCatchUp.isNegative())
            throw new IllegalArgumentException("Maximum retention catch-up must be positive");
        if (lateArrivalOverlap.isNegative())
            throw new IllegalArgumentException("Late-arrival overlap must not be negative");
        if (reconciliationCatchUp.isZero() || reconciliationCatchUp.isNegative())
            throw new IllegalArgumentException("Reconciliation catch-up must be positive");
        this.hub = java.util.Objects.requireNonNull(hub, "hub");
        this.batchWindowLength = batchWindowLength;
        this.afterQueryAction = java.util.Objects.requireNonNull(afterQueryAction, "afterQueryAction");
        this.retentionOffset = retentionOffset;
        this.maximumCatchUp = maximumCatchUp;
        this.lateArrivalOverlap = lateArrivalOverlap;
        this.reconciliationCatchUp = reconciliationCatchUp;
        this.cursorStore = java.util.Objects.requireNonNull(cursorStore, "cursorStore");
        this.beforeQueryAction = beforeQueryAction == null ? () -> { } : beforeQueryAction;
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.identityResolver = new MediaIdentityResolver(hub);
    }

    private static ScheduledExecutorService newExecutor() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "OSCAR-video-retention");
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadScheduledExecutor(factory);
    }

    public synchronized void start() {
        if (future != null && !future.isDone())
            return;
        if (executor != null && !executor.isTerminated())
            throw new IllegalStateException(
                    "Video-retention worker from the previous generation has not terminated");
        synchronized (operationLock) {
            if (activeOperation != null || (cancellationStarted && !cancellationComplete))
                throw new IllegalStateException(
                        "Video-retention datastore cancellation from the previous generation "
                                + "has not completed");
            if (operationFailure != null)
                throw new IllegalStateException(
                        "Video-retention datastore operation did not close cleanly; recreate the module",
                        operationFailure);
            cancellationStarted = false;
            cancellationComplete = true;
        }
        executor = null;
        future = null;
        stopRequested = false;
        executor = newExecutor();
        long delayMillis = Math.max(1_000L, batchWindowLength.toMillis());
        // Fixed delay prevents a slow catch-up cycle from building an immediate fixed-rate backlog.
        future = executor.scheduleWithFixedDelay(this::runCycle, 0, delayMillis,
                TimeUnit.MILLISECONDS);
    }

    void runCycle() {
        // A timed-out stop may leave native decoding in flight. A restarted scheduler uses a new
        // executor, but this lock still prevents old and new generations from overlapping.
        synchronized (cycleLock) {
            runCycleLocked();
        }
    }

    private void runCycleLocked() {
        if (shouldStop())
            return;
        try {
            beforeQueryAction.run();
        } catch (Exception e) {
            if (shouldStop())
                logger.debug("Retained-media maintenance stopped during module shutdown");
            else
                logger.warn("Retained-media maintenance cycle failed; occupancy scanning will continue", e);
        }

        if (shouldStop())
            return;
        try {
            scanEligibleOccupanciesCancellable();
        } catch (Exception e) {
            // The current window is intentionally not checkpointed. A later cycle repeats it.
            if (shouldStop())
                logger.debug("Occupancy media retention query cancelled during module shutdown");
            else
                logger.warn("Exception while querying occupancy media retention window", e);
        }
    }

    /**
     * A PostGIS query can be waiting in the driver when the scheduler is interrupted. Bind the
     * whole federated scan to the datastore's thread-local cancellation scope so stop() can abort
     * only this worker's statements before PostgreSQL is stopped.
     */
    private void scanEligibleOccupanciesCancellable() throws IOException {
        ICancellableDataStore.CancellableOperation operation =
                beginCancellableDataStoreOperation();
        RuntimeException closeFailure = null;
        synchronized (operationLock) {
            activeOperation = operation;
            cancellationStarted = false;
            cancellationComplete = true;
            if (stopRequested && operation != null)
                startOperationCancellationLocked(operation);
        }
        try {
            if (shouldStop())
                return;
            scanEligibleOccupancies();
        } finally {
            if (operation != null) {
                try {
                    operation.close();
                } catch (RuntimeException failure) {
                    closeFailure = failure;
                }
            }
            synchronized (operationLock) {
                if (activeOperation == operation)
                    activeOperation = null;
                if (closeFailure != null && operationFailure == null)
                    operationFailure = closeFailure;
                operationLock.notifyAll();
            }
            if (closeFailure != null)
                throw closeFailure;
        }
    }

    ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
        // PostGIS cancellation scopes are thread-bound across its linked stores. Opening one on
        // the configured registry is therefore sufficient for the federated query on this thread.
        for (var database : hub.getDatabaseRegistry().getObsSystemDatabases()) {
            var observationStore = database.getObservationStore();
            if (observationStore instanceof ICancellableDataStore cancellable)
                return cancellable.beginCancellableOperation();
        }
        return null;
    }

    private void scanEligibleOccupancies() throws IOException {
        if (shouldStop())
            return;
        Instant now = clock.instant();
        Instant eligibleThrough = subtractClamped(now, retentionOffset);
        RetentionScanCheckpoint checkpoint = cursorStore.read()
                .orElseGet(() -> initializeCheckpoint(eligibleThrough, now));
        // Revisit one bounded recent interval every cycle. Durable processed-observation IDs make
        // this cheap while still discovering records committed after their event-time window was
        // first scanned.
        Instant overlapStart = overlapStart(checkpoint.processedThrough(), lateArrivalOverlap);
        if (overlapStart.isBefore(checkpoint.processedThrough())
                && !processWindow(overlapStart, checkpoint.processedThrough(), true))
            return;

        ScanPlan plan = plan(checkpoint.processedThrough(), eligibleThrough, maximumCatchUp);
        Instant processedThrough = checkpoint.processedThrough();
        if (plan.hasWork()) {
            Instant windowStart = plan.startInclusive();
            while (!shouldStop() && windowStart.isBefore(plan.endInclusive())) {
                Instant windowEnd = minimum(addClamped(windowStart, batchWindowLength),
                        plan.endInclusive());
                if (!processWindow(windowStart, windowEnd, true))
                    return;
                Instant checkpointTime = clock.instant();
                cursorStore.write(RetentionScanCheckpoint.at(windowEnd, checkpointTime));
                processedThrough = windowEnd;
                windowStart = windowEnd;
            }
        }

        if (!shouldStop())
            reconcileHistoricalOccupancies(processedThrough, eligibleThrough);
    }

    private RetentionScanCheckpoint initializeCheckpoint(Instant eligibleThrough, Instant now) {
        try {
            IObsStore observationStore = hub.getDatabaseRegistry().getFederatedDatabase()
                    .getObservationStore();
            Optional<Instant> earliest = findEarliestEligibleTime(observationStore, eligibleThrough);
            Instant seed;
            if (earliest.isPresent()) {
                Instant first = maximum(RETENTION_EPOCH, earliest.get());
                seed = first.equals(RETENTION_EPOCH) ? RETENTION_EPOCH : first.minusNanos(1);
            } else if (hasEligibleOccupancy(observationStore, eligibleThrough)) {
                // Some custom stores cannot expose dynamic datastream ranges. Epoch is safe, and
                // maximumCatchUp still strictly bounds every cycle. Persist before doing any scan
                // so a restart never repeats initialization work or resets progress.
                seed = RETENTION_EPOCH;
                logger.warn("Occupancy datastream ranges are unavailable; bounded retention " +
                        "backfill will begin at the Unix epoch");
            } else {
                // A fresh/empty install checks one normal query window before the current cutoff.
                // This closes the initialization race with a concurrent first observation without
                // creating an unbounded historical scan.
                seed = maximum(RETENTION_EPOCH,
                        subtractClamped(eligibleThrough, batchWindowLength));
            }
            RetentionScanCheckpoint initialized = RetentionScanCheckpoint.at(seed, now);
            cursorStore.write(initialized);
            logger.info("Initialized retained-media scan cursor at {} (eligible through {})",
                    seed, eligibleThrough);
            return initialized;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to durably initialize retention scan cursor", e);
        }
    }

    private Optional<Instant> findEarliestEligibleTime(IObsStore observationStore,
                                                        Instant eligibleThrough) {
        DataStreamFilter occupancyStreams = occupancyStreams();
        try (Stream<org.sensorhub.api.data.IDataStreamInfo> streams =
                     observationStore.getDataStreams().select(occupancyStreams)) {
            return streams.map(info -> info.getResultTimeRange())
                    .filter(java.util.Objects::nonNull)
                    .map(range -> range.begin())
                    .filter(java.util.Objects::nonNull)
                    .filter(begin -> !begin.isAfter(eligibleThrough))
                    .min(Comparator.naturalOrder());
        }
    }

    private boolean hasEligibleOccupancy(IObsStore observationStore, Instant eligibleThrough) {
        if (eligibleThrough.isBefore(RETENTION_EPOCH))
            return false;
        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(occupancyStreams())
                .withResultTimeDuring(RETENTION_EPOCH, eligibleThrough)
                .withLimit(1)
                .build();
        try (Stream<Map.Entry<BigId, IObsData>> entries = observationStore.selectEntries(filter)) {
            return entries.findAny().isPresent();
        }
    }

    private boolean processWindow(Instant startInclusive, Instant endInclusive,
                                  boolean skipProcessed) throws IOException {
        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(occupancyStreams())
                .withResultTimeDuring(startInclusive, endInclusive)
                .build();
        try (Stream<Map.Entry<BigId, IObsData>> entries = hub.getDatabaseRegistry()
                .getFederatedDatabase().getObservationStore().selectEntries(filter)) {
            // Ordering is not part of the checkpoint contract: the window advances only after
            // every entry succeeds, and durable observation markers make replay idempotent. Stream
            // entries instead of materializing/sorting an unbounded event-time window in memory.
            var iterator = entries.iterator();
            while (!shouldStop() && iterator.hasNext()) {
                Map.Entry<BigId, IObsData> entry = iterator.next();
                IObsData observation = entry.getValue();
                String observationId = identityResolver.publicObservationId(entry.getKey());
                if (skipProcessed && cursorStore.isProcessed(
                        observationId, observation.getResultTime()))
                    continue;
                OccupancyRecord record = new OccupancyRecord(
                        observationId,
                        identityResolver.laneUid(observation), observation,
                        Occupancy.toOccupancy(observation.getResult()));
                if (!afterQueryAction.call(record))
                    return false;
                // Historical reconciliation intentionally ignores processed markers. Rewriting a
                // marker there would recreate old tiny files immediately after safe reclamation;
                // only the forward/late-arrival scan needs this bounded deduplication state.
                if (skipProcessed)
                    cursorStore.markProcessed(RetentionProcessedObservation.at(observationId,
                            observation.getResultTime(), clock.instant()));
            }
        }
        return !shouldStop();
    }

    private void reconcileHistoricalOccupancies(Instant forwardProcessedThrough,
                                                Instant eligibleThrough) throws IOException {
        if (shouldStop())
            return;
        Instant reconcileBefore = overlapStart(forwardProcessedThrough, lateArrivalOverlap);
        if (!RETENTION_EPOCH.isBefore(reconcileBefore))
            return;

        IObsStore observationStore = hub.getDatabaseRegistry().getFederatedDatabase()
                .getObservationStore();
        RetentionReconciliationCheckpoint checkpoint = cursorStore.readReconciliation()
                .orElseGet(() -> initializeReconciliationCheckpoint(
                        observationStore, eligibleThrough, reconcileBefore));
        Instant start = checkpoint.processedThrough();
        if (!start.isBefore(reconcileBefore)) {
            start = reconciliationSeed(observationStore, eligibleThrough, reconcileBefore);
            cursorStore.writeReconciliation(
                    RetentionReconciliationCheckpoint.at(start, clock.instant()));
        }

        Instant end = minimum(addClamped(start, reconciliationCatchUp), reconcileBefore);
        if (!start.isBefore(end))
            return;
        // Reconciliation deliberately revalidates even a previously processed observation. This
        // slowly repairs media whose ledger/catalog was lost, without an unbounded restart scan.
        if (!processWindow(start, end, false))
            return;
        cursorStore.writeReconciliation(
                RetentionReconciliationCheckpoint.at(end, clock.instant()));
    }

    private RetentionReconciliationCheckpoint initializeReconciliationCheckpoint(
            IObsStore observationStore, Instant eligibleThrough, Instant reconcileBefore) {
        try {
            Instant seed = reconciliationSeed(observationStore, eligibleThrough, reconcileBefore);
            RetentionReconciliationCheckpoint checkpoint =
                    RetentionReconciliationCheckpoint.at(seed, clock.instant());
            // Persist before scanning so every restart resumes the same bounded sweep.
            cursorStore.writeReconciliation(checkpoint);
            return checkpoint;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to durably initialize retention reconciliation cursor", e);
        }
    }

    private Instant reconciliationSeed(IObsStore observationStore, Instant eligibleThrough,
                                       Instant reconcileBefore) {
        Optional<Instant> earliest = findEarliestEligibleTime(observationStore,
                minimum(eligibleThrough, reconcileBefore));
        if (earliest.isPresent()) {
            Instant first = maximum(RETENTION_EPOCH, earliest.get());
            return first.equals(RETENTION_EPOCH) ? RETENTION_EPOCH : first.minusNanos(1);
        }
        if (hasEligibleOccupancy(observationStore, minimum(eligibleThrough, reconcileBefore)))
            return RETENTION_EPOCH;
        return reconcileBefore;
    }

    private static DataStreamFilter occupancyStreams() {
        return new DataStreamFilter.Builder().withOutputNames(OccupancyOutput.NAME).build();
    }

    static ScanPlan plan(Instant processedThrough, Instant eligibleThrough, Duration maximumCatchUp) {
        java.util.Objects.requireNonNull(processedThrough, "processedThrough");
        java.util.Objects.requireNonNull(eligibleThrough, "eligibleThrough");
        java.util.Objects.requireNonNull(maximumCatchUp, "maximumCatchUp");
        if (maximumCatchUp.isZero() || maximumCatchUp.isNegative())
            throw new IllegalArgumentException("maximumCatchUp must be positive");
        if (!processedThrough.isBefore(eligibleThrough))
            return new ScanPlan(processedThrough, processedThrough);
        return new ScanPlan(processedThrough,
                minimum(addClamped(processedThrough, maximumCatchUp), eligibleThrough));
    }

    static Instant overlapStart(Instant processedThrough, Duration overlap) {
        return maximum(RETENTION_EPOCH, subtractClamped(processedThrough, overlap));
    }

    public void stop() {
        stop(DEFAULT_STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop(long timeout, TimeUnit unit) {
        java.util.Objects.requireNonNull(unit, "unit");
        if (timeout <= 0)
            throw new IllegalArgumentException("Video-retention stop timeout must be positive");
        long budgetNanos = Math.max(1L, unit.toNanos(timeout));
        long deadline = System.nanoTime() + budgetNanos;
        requestStop();
        awaitStop(Math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
    }

    /**
     * Signals the scheduler and its datastore operation without waiting. Driver cancellation is
     * delegated to a daemon helper because a JDBC cancellation request can itself block.
     */
    public synchronized void requestStop() {
        stopRequested = true;
        ICancellableDataStore.CancellableOperation operation;
        synchronized (operationLock) {
            operation = activeOperation;
            if (operation != null)
                startOperationCancellationLocked(operation);
        }
        if (future != null)
            future.cancel(true);
        ScheduledExecutorService stopping = executor;
        if (stopping == null)
            future = null;
        else
            stopping.shutdownNow();
    }

    /** Waits truthfully for a stop that has already been requested. */
    public synchronized void awaitStop(long timeout, TimeUnit unit) {
        java.util.Objects.requireNonNull(unit, "unit");
        if (timeout <= 0)
            throw new IllegalArgumentException("Video-retention stop timeout must be positive");
        long budgetNanos = Math.max(1L, unit.toNanos(timeout));
        long deadline = System.nanoTime() + budgetNanos;
        ScheduledExecutorService stopping = executor;
        boolean terminated;
        try {
            if (stopping == null || stopping.isTerminated()) {
                terminated = true;
            } else {
                long remaining = Math.max(1L, deadline - System.nanoTime());
                terminated = stopping.awaitTermination(remaining, TimeUnit.NANOSECONDS);
            }
            if (terminated) {
                synchronized (operationLock) {
                    while (cancellationStarted && !cancellationComplete) {
                        long remaining = deadline - System.nanoTime();
                        if (remaining <= 0) {
                            terminated = false;
                            break;
                        }
                        TimeUnit.NANOSECONDS.timedWait(operationLock, remaining);
                    }
                }
            }
        } catch (InterruptedException e) {
            if (stopping != null)
                stopping.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting for occupancy retention thread to stop", e);
        }
        if (!terminated)
            throw new IllegalStateException(
                    "Occupancy retention worker did not terminate within the stop deadline");
        RuntimeException unsafeFailure;
        synchronized (operationLock) {
            unsafeFailure = operationFailure;
        }
        if (unsafeFailure != null)
            throw new IllegalStateException(
                    "Occupancy retention stopped, but datastore cancellation or scope cleanup failed",
                    unsafeFailure);
        if (executor == stopping)
            executor = null;
        future = null;
    }

    private void startOperationCancellationLocked(
            ICancellableDataStore.CancellableOperation operation) {
        if (cancellationStarted)
            return;
        cancellationStarted = true;
        cancellationComplete = false;
        Thread cancellationThread = new Thread(() -> {
            try {
                operation.cancel();
            } catch (Throwable failure) {
                RuntimeException runtimeFailure = failure instanceof RuntimeException runtime
                        ? runtime : new IllegalStateException(
                                "Occupancy-retention datastore cancellation failed", failure);
                synchronized (operationLock) {
                    if (operationFailure == null)
                        operationFailure = runtimeFailure;
                }
            } finally {
                synchronized (operationLock) {
                    cancellationComplete = true;
                    operationLock.notifyAll();
                }
            }
        }, "OSCAR-video-retention-cancel");
        cancellationThread.setDaemon(true);
        cancellationThread.start();
    }

    private boolean shouldStop() {
        return stopRequested || Thread.currentThread().isInterrupted();
    }

    private static Duration duration(TemporalAmount amount, String field) {
        try {
            return Duration.from(java.util.Objects.requireNonNull(amount, field));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(field + " must be a fixed duration", e);
        }
    }

    private static Instant addClamped(Instant instant, Duration duration) {
        try {
            return instant.plus(duration);
        } catch (java.time.DateTimeException | ArithmeticException e) {
            return Instant.MAX;
        }
    }

    private static Instant subtractClamped(Instant instant, Duration duration) {
        try {
            return instant.minus(duration);
        } catch (java.time.DateTimeException | ArithmeticException e) {
            return Instant.MIN;
        }
    }

    private static Instant minimum(Instant left, Instant right) {
        return left.compareTo(right) <= 0 ? left : right;
    }

    private static Instant maximum(Instant left, Instant right) {
        return left.compareTo(right) >= 0 ? left : right;
    }

    record ScanPlan(Instant startInclusive, Instant endInclusive) {
        boolean hasWork() {
            return startInclusive.isBefore(endInclusive);
        }
    }

    public record OccupancyRecord(String eventId, String laneUid, IObsData observation,
                                  Occupancy occupancy) {}

    @FunctionalInterface
    public interface QueryAction {
        boolean call(OccupancyRecord queryResult);
    }

    @FunctionalInterface
    public interface CycleAction {
        void run() throws Exception;
    }

    interface CursorStore {
        Optional<RetentionScanCheckpoint> read() throws IOException;

        void write(RetentionScanCheckpoint checkpoint) throws IOException;

        Optional<RetentionReconciliationCheckpoint> readReconciliation() throws IOException;

        void writeReconciliation(RetentionReconciliationCheckpoint checkpoint) throws IOException;

        boolean isProcessed(String observationId, Instant resultTime) throws IOException;

        void markProcessed(RetentionProcessedObservation marker) throws IOException;
    }

    private record MediaCursorStore(MediaSetStore mediaStore) implements CursorStore {
        private MediaCursorStore {
            java.util.Objects.requireNonNull(mediaStore, "mediaStore");
        }

        @Override
        public Optional<RetentionScanCheckpoint> read() throws IOException {
            return mediaStore.readRetentionScanCheckpoint();
        }

        @Override
        public void write(RetentionScanCheckpoint checkpoint) throws IOException {
            mediaStore.writeRetentionScanCheckpoint(checkpoint);
        }

        @Override
        public Optional<RetentionReconciliationCheckpoint> readReconciliation() throws IOException {
            return mediaStore.readReconciliationCheckpoint();
        }

        @Override
        public void writeReconciliation(RetentionReconciliationCheckpoint checkpoint)
                throws IOException {
            mediaStore.writeReconciliationCheckpoint(checkpoint);
        }

        @Override
        public boolean isProcessed(String observationId, Instant resultTime) throws IOException {
            return mediaStore.isObservationProcessed(observationId, resultTime);
        }

        @Override
        public void markProcessed(RetentionProcessedObservation marker) throws IOException {
            mediaStore.markObservationProcessed(marker);
        }
    }
}
