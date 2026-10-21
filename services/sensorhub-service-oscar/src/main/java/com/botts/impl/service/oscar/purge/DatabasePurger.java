package com.botts.impl.service.oscar.purge;

import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.ICancellableDataStore;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.resource.ResourceKey;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.dailyfile.IDailyFileJournalService;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.TimeExtent;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.google.common.collect.Range;
import org.sensorhub.api.datastore.TemporalFilter;

public class DatabasePurger {
    private static final Logger log = LoggerFactory.getLogger(DatabasePurger.class);

    private final IObsSystemDatabase database;
    private final IBucketStore bucketStore;
    private final IDailyFileJournalService journalService;
    private final ZoneId dailyFileZone;
    private final Clock clock;
    private final boolean observationLifecycleDelegated;
    private final int dailyFilePurgeWorkLimit;
    private volatile ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> hourlyPurgeTask;
    private volatile ScheduledFuture<?> dailyExportTask;
    private final Object maintenanceMonitor = new Object();
    private final Set<MaintenanceWork> activeMaintenance = new HashSet<>();
    private final ThreadLocal<Boolean> maintenanceScope =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private final AtomicReference<RuntimeException> unsafeStopFailure = new AtomicReference<>();
    private volatile boolean stopRequested;

    private final int occupancyBufferSeconds;

    private static final String DAILY_FILES_BUCKET = "dailyfiles";

    private static final int CONNECTION_STATUS_RETENTION_HOURS = 1;
    static final int DELETE_BATCH_SIZE = 5_000;
    static final int DEFAULT_DAILY_FILE_PURGE_WORK_LIMIT = 50;
    static final long STOP_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(5);

    // Data stream output names
    private static final String OUTPUT_DAILY_FILE = "dailyFile";
    private static final String OUTPUT_SPEED = "speed";
    private static final String OUTPUT_CONNECTION_STATUS = "connectionStatus";
    private static final String OUTPUT_GAMMA_COUNTS = "gammaCounts";
    private static final String OUTPUT_NEUTRON_COUNTS = "neutronCounts";
    private static final String OUTPUT_GAMMA_THRESHOLD = "gammaThreshold";
    private static final String OUTPUT_OCCUPANCY = OccupancyOutput.NAME;

    // RS350 data stream output names
    private static final String OUTPUT_BACKGROUND_REPORT = "backgroundReport";
    private static final String OUTPUT_FOREGROUND_REPORT = "foregroundReport";
    private static final String OUTPUT_STATUS = "status";

    public DatabasePurger(IObsSystemDatabase database, IBucketStore bucketStore, int occupancyBufferSeconds) {
        this(database, bucketStore, occupancyBufferSeconds, null, ZoneId.of("UTC"), Clock.systemUTC(),
                false, DEFAULT_DAILY_FILE_PURGE_WORK_LIMIT);
    }

    public DatabasePurger(IObsSystemDatabase database, IBucketStore bucketStore, int occupancyBufferSeconds,
                          IDailyFileJournalService journalService) {
        this(database, bucketStore, occupancyBufferSeconds, journalService,
                journalService != null ? journalService.getJournalZoneId() : ZoneId.of("UTC"),
                Clock.systemUTC(), false, DEFAULT_DAILY_FILE_PURGE_WORK_LIMIT);
    }

    public DatabasePurger(IObsSystemDatabase database, IBucketStore bucketStore, int occupancyBufferSeconds,
                          IDailyFileJournalService journalService, boolean observationLifecycleDelegated) {
        this(database, bucketStore, occupancyBufferSeconds, journalService,
                observationLifecycleDelegated, DEFAULT_DAILY_FILE_PURGE_WORK_LIMIT);
    }

    public DatabasePurger(IObsSystemDatabase database, IBucketStore bucketStore, int occupancyBufferSeconds,
                          IDailyFileJournalService journalService, boolean observationLifecycleDelegated,
                          int dailyFilePurgeWorkLimit) {
        this(database, bucketStore, occupancyBufferSeconds, journalService,
                journalService != null ? journalService.getJournalZoneId() : ZoneId.of("UTC"),
                Clock.systemUTC(), observationLifecycleDelegated, dailyFilePurgeWorkLimit);
    }

    DatabasePurger(IObsSystemDatabase database, IBucketStore bucketStore, int occupancyBufferSeconds,
                   IDailyFileJournalService journalService, ZoneId dailyFileZone, Clock clock) {
        this(database, bucketStore, occupancyBufferSeconds, journalService, dailyFileZone, clock,
                false, DEFAULT_DAILY_FILE_PURGE_WORK_LIMIT);
    }

    DatabasePurger(IObsSystemDatabase database, IBucketStore bucketStore, int occupancyBufferSeconds,
                   IDailyFileJournalService journalService, ZoneId dailyFileZone, Clock clock,
                   boolean observationLifecycleDelegated) {
        this(database, bucketStore, occupancyBufferSeconds, journalService, dailyFileZone, clock,
                observationLifecycleDelegated, DEFAULT_DAILY_FILE_PURGE_WORK_LIMIT);
    }

    DatabasePurger(IObsSystemDatabase database, IBucketStore bucketStore, int occupancyBufferSeconds,
                   IDailyFileJournalService journalService, ZoneId dailyFileZone, Clock clock,
                   boolean observationLifecycleDelegated, int dailyFilePurgeWorkLimit) {
        this.database = database;
        this.bucketStore = bucketStore;
        this.occupancyBufferSeconds = occupancyBufferSeconds;
        this.journalService = journalService;
        this.dailyFileZone = Objects.requireNonNull(dailyFileZone, "dailyFileZone");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.observationLifecycleDelegated = observationLifecycleDelegated;
        if (dailyFilePurgeWorkLimit <= 0)
            throw new IllegalArgumentException("dailyFilePurgeWorkLimit must be positive");
        this.dailyFilePurgeWorkLimit = dailyFilePurgeWorkLimit;
    }

    public void start() throws SensorHubException {
        if (unsafeStopFailure.get() != null)
            throw new SensorHubException(
                    "Database purger cannot restart after unsafe datastore cleanup; recreate the module",
                    unsafeStopFailure.get());
        ScheduledExecutorService previousScheduler = scheduler;
        if (previousScheduler != null && !previousScheduler.isTerminated())
            throw new SensorHubException("Database purger is already running or has not terminated");

        synchronized (maintenanceMonitor) {
            if (!activeMaintenance.isEmpty())
                throw new SensorHubException("Database purger still has active maintenance work");
            stopRequested = false;
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OSCAR-database-purger");
            t.setDaemon(true);
            return t;
        });

        try {
            if (!bucketStore.bucketExists(DAILY_FILES_BUCKET)) {
                bucketStore.createBucket(DAILY_FILES_BUCKET);
                log.info("Created bucket for daily file exports: {}", DAILY_FILES_BUCKET);
            }

            hourlyPurgeTask = scheduler.scheduleAtFixedRate(
                this::executeHourlyPurge,
                0,
                1,
                TimeUnit.HOURS
            );
            log.info("Scheduled hourly purge task (every 1 hour)");

            scheduleNextDailyExport();

        } catch (DataStoreException e) {
            scheduler.shutdownNow();
            throw new SensorHubException("Failed to create daily files bucket", e);
        }
    }

    public void stop() throws SensorHubException {
        stop(STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void stop(long timeout, TimeUnit unit) throws SensorHubException {
        Objects.requireNonNull(unit, "unit");
        if (timeout <= 0)
            throw new IllegalArgumentException("Database purger stop timeout must be positive");

        long requestedNanos = unit.toNanos(timeout);
        long maximumNanos = TimeUnit.MILLISECONDS.toNanos(STOP_TIMEOUT_MILLIS);
        long budgetNanos = Math.min(requestedNanos, maximumNanos);
        if (budgetNanos <= 0)
            budgetNanos = 1;
        long deadline = System.nanoTime() + budgetNanos;

        requestStop();
        awaitStop(Math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
    }

    /**
     * Signals every purger task and active datastore scope without waiting for termination.
     * This phase is intentionally separate so the parent OSCAR module can broadcast shutdown to
     * all of its independent maintenance workers before any one slow JDBC cancellation consumes
     * the shared module-registry deadline.
     */
    public void requestStop() {

        log.info("Stopping database purger...");
        List<MaintenanceWork> workToCancel;
        synchronized (maintenanceMonitor) {
            stopRequested = true;
            workToCancel = new ArrayList<>(activeMaintenance);
        }
        // Driver cancellation can block while a cancel request is delivered. Initiate it on a
        // daemon helper so this module-registry thread retains one bounded stop deadline.
        workToCancel.forEach(MaintenanceWork::requestCancellation);

        if (hourlyPurgeTask != null)
            hourlyPurgeTask.cancel(true);
        if (dailyExportTask != null)
            dailyExportTask.cancel(true);

        ScheduledExecutorService activeScheduler = scheduler;
        if (activeScheduler != null && !activeScheduler.isTerminated())
            activeScheduler.shutdownNow();
    }

    /**
     * Waits for a previously requested stop. Success means both the scheduler and every
     * cancellable maintenance scope have actually terminated.
     */
    public void awaitStop(long timeout, TimeUnit unit) throws SensorHubException {
        Objects.requireNonNull(unit, "unit");
        if (timeout <= 0)
            throw new IllegalArgumentException("Database purger stop timeout must be positive");

        long requestedNanos = unit.toNanos(timeout);
        long maximumNanos = TimeUnit.MILLISECONDS.toNanos(STOP_TIMEOUT_MILLIS);
        long budgetNanos = Math.min(requestedNanos, maximumNanos);
        if (budgetNanos <= 0)
            budgetNanos = 1;
        long deadline = System.nanoTime() + budgetNanos;
        ScheduledExecutorService activeScheduler = scheduler;
        boolean terminated;
        try {
            terminated = awaitMaintenanceShutdown(activeScheduler, deadline);
        } catch (InterruptedException e) {
            requestCancellationForActiveMaintenance();
            if (activeScheduler != null)
                activeScheduler.shutdownNow();
            Thread.currentThread().interrupt();
            throw new SensorHubException("Interrupted while stopping database purger", e);
        }

        if (!terminated) {
            requestCancellationForActiveMaintenance();
            int activeCount;
            synchronized (maintenanceMonitor) {
                activeCount = activeMaintenance.size();
            }
            SensorHubException failure = new SensorHubException(
                    "Database purger did not stop within its single " +
                            TimeUnit.NANOSECONDS.toMillis(budgetNanos) +
                            " ms deadline (active maintenance=" + activeCount +
                            ", schedulerTerminated=" +
                            (activeScheduler == null || activeScheduler.isTerminated()) +
                            "); refusing to report a successful stop");
            RuntimeException unsafeFailure = unsafeStopFailure.get();
            if (unsafeFailure != null)
                failure.addSuppressed(unsafeFailure);
            throw failure;
        }

        clearSchedulerReferences(activeScheduler);
        throwIfUnsafeStop();
        log.info("Database purger stopped");
    }

    private boolean awaitMaintenanceShutdown(ScheduledExecutorService activeScheduler,
                                             long deadline) throws InterruptedException {
        while (true) {
            boolean schedulerTerminated = activeScheduler == null || activeScheduler.isTerminated();
            boolean maintenanceTerminated;
            synchronized (maintenanceMonitor) {
                maintenanceTerminated = activeMaintenance.isEmpty();
            }
            if (schedulerTerminated && maintenanceTerminated)
                return true;

            long remaining = deadline - System.nanoTime();
            if (remaining <= 0)
                return false;
            long slice = Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(50));
            if (!schedulerTerminated) {
                activeScheduler.awaitTermination(slice, TimeUnit.NANOSECONDS);
            } else {
                synchronized (maintenanceMonitor) {
                    if (!activeMaintenance.isEmpty())
                        TimeUnit.NANOSECONDS.timedWait(maintenanceMonitor, slice);
                }
            }
        }
    }

    private void throwIfUnsafeStop() throws SensorHubException {
        RuntimeException failure = unsafeStopFailure.get();
        if (failure != null)
            throw new SensorHubException(
                    "Database purger workers terminated, but datastore cancellation or scope cleanup failed; " +
                            "refusing to report a successful stop", failure);
    }

    private void requestCancellationForActiveMaintenance() {
        List<MaintenanceWork> snapshot;
        synchronized (maintenanceMonitor) {
            snapshot = new ArrayList<>(activeMaintenance);
        }
        snapshot.forEach(MaintenanceWork::requestCancellation);
    }

    private void latchUnsafeStopFailure(RuntimeException failure, String description) {
        unsafeStopFailure.compareAndSet(null, failure);
        log.warn(description, failure);
    }

    private void clearSchedulerReferences(ScheduledExecutorService stoppedScheduler) {
        if (scheduler == stoppedScheduler)
            scheduler = null;
        hourlyPurgeTask = null;
        dailyExportTask = null;
    }

    void executeHourlyPurge() {
        runCancellableMaintenance(this::executeHourlyPurgeWithinScope);
    }

    private void executeHourlyPurgeWithinScope() {
        if (shouldStopPurge())
            return;
        log.info("Starting hourly purge task...");

        if (!runHourlyPurgePhase("connection-status purge", this::purgeOldConnectionStatus))
            return;

        if (observationLifecycleDelegated) {
            log.debug("Skipping legacy occupancy-gated high-volume purge because lifecycle is delegated to the database");
        } else {
            if (!runHourlyPurgePhase("legacy occupancy-gated purge", this::purgeNonOccupancyData))
                return;
        }

        // The exact-midnight attempt can run before the journal finishes publishing every
        // lane. Reuse the same bounded, manifest-gated catch-up on each hourly cycle so a
        // skipped day is retried after publication and an outage backlog converges. Keep the
        // legacy no-journal exporter on its once-daily schedule to avoid duplicate exports.
        if (journalService != null) {
            if (!runHourlyPurgePhase("Daily File journal catch-up", this::exportAndPurgeDailyFileData))
                return;
        }

        if (!shouldStopPurge())
            log.info("Hourly purge task completed");
    }

    private boolean runHourlyPurgePhase(String description, Runnable phase) {
        if (shouldStopPurge())
            return false;
        try {
            phase.run();
        } catch (Exception e) {
            // Isolate phases so an unrelated retention failure cannot starve journal catch-up,
            // and contain every exception so ScheduledExecutorService keeps the recurring task.
            if (shouldStopPurge())
                log.debug("Cancelled hourly {} during database-purger stop", description);
            else
                log.error("Error during hourly {}", description, e);
        }
        return !shouldStopPurge();
    }

    private void executeDailyExportAndPurge() {
        try {
            runCancellableMaintenance(() -> {
                if (shouldStopPurge())
                    return;
                log.info("Starting daily export and purge task...");
                exportAndPurgeDailyFileData();
                if (!shouldStopPurge())
                    log.info("Daily export and purge task completed successfully");
            });
        } catch (Exception e) {
            if (shouldStopPurge())
                log.debug("Cancelled daily export and purge task during database-purger stop");
            else
                log.error("Error during daily export and purge task", e);
        } finally {
            if (!shouldStopPurge() && scheduler != null && !scheduler.isShutdown())
                scheduleNextDailyExport();
        }
    }

    private void runCancellableMaintenance(Runnable maintenance) {
        if (Boolean.TRUE.equals(maintenanceScope.get())) {
            maintenance.run();
            return;
        }
        if (shouldStopPurge())
            return;

        MaintenanceWork work;
        synchronized (maintenanceMonitor) {
            if (stopRequested || unsafeStopFailure.get() != null)
                return;
            work = new MaintenanceWork();
            activeMaintenance.add(work);
        }

        ICancellableDataStore.CancellableOperation operation = null;
        boolean operationAttached = false;
        boolean scopeClosed = true;
        Throwable primaryFailure = null;
        Throwable closeFailure = null;
        try {
            try {
                operation = beginCancellableDataStoreOperation();
            } catch (RuntimeException | Error error) {
                latchUnsafeStopFailure(asRuntimeFailure(error,
                                "Datastore operation-scope creation failed"),
                        "Could not begin a database-purge operation scope cleanly");
                throw error;
            }
            work.attachOperation(operation);
            operationAttached = true;
            maintenanceScope.set(Boolean.TRUE);
            if (shouldStopPurge()) {
                work.requestCancellation();
                return;
            }
            maintenance.run();
        } catch (RuntimeException | Error error) {
            primaryFailure = error;
            throw error;
        } finally {
            maintenanceScope.remove();
            if (!operationAttached)
                work.attachOperation(null);
            if (operation != null) {
                try {
                    operation.close();
                } catch (RuntimeException | Error error) {
                    scopeClosed = false;
                    closeFailure = error;
                    latchUnsafeStopFailure(asRuntimeFailure(error,
                                    "Datastore operation-scope cleanup failed"),
                            "Could not close a database-purge operation scope cleanly");
                }
            }
            work.workerFinished(scopeClosed);
            if (closeFailure != null) {
                if (primaryFailure != null)
                    primaryFailure.addSuppressed(closeFailure);
                else if (closeFailure instanceof RuntimeException runtime)
                    throw runtime;
                else
                    throw (Error) closeFailure;
            }
        }
    }

    private final class MaintenanceWork {
        private ICancellableDataStore.CancellableOperation operation;
        private boolean operationAttached;
        private boolean cancellationRequested;
        private boolean cancellationComplete = true;
        private boolean cancellationStarted;
        private boolean workerComplete;
        private boolean scopeClosed;

        void attachOperation(ICancellableDataStore.CancellableOperation attachedOperation) {
            synchronized (maintenanceMonitor) {
                if (operationAttached)
                    return;
                operation = attachedOperation;
                operationAttached = true;
                if (cancellationRequested && operation != null)
                    startCancellationLocked();
                else if (cancellationRequested)
                    cancellationComplete = true;
                removeIfCompleteLocked();
            }
        }

        void requestCancellation() {
            synchronized (maintenanceMonitor) {
                cancellationRequested = true;
                if (operationAttached) {
                    if (operation != null)
                        startCancellationLocked();
                    else
                        cancellationComplete = true;
                } else
                    cancellationComplete = false;
                removeIfCompleteLocked();
            }
        }

        private void startCancellationLocked() {
            if (cancellationStarted)
                return;
            cancellationStarted = true;
            cancellationComplete = false;
            Thread cancellationThread = new Thread(() -> {
                try {
                    operation.cancel();
                } catch (Throwable error) {
                    RuntimeException failure = asRuntimeFailure(error,
                            "Datastore cancellation failed");
                    latchUnsafeStopFailure(failure,
                            "Could not cancel an active database-purge operation cleanly");
                } finally {
                    synchronized (maintenanceMonitor) {
                        cancellationComplete = true;
                        removeIfCompleteLocked();
                    }
                }
            }, "OSCAR-database-purger-cancel");
            cancellationThread.setDaemon(true);
            cancellationThread.start();
        }

        void workerFinished(boolean closedCleanly) {
            synchronized (maintenanceMonitor) {
                workerComplete = true;
                scopeClosed = closedCleanly;
                removeIfCompleteLocked();
            }
        }

        private void removeIfCompleteLocked() {
            if (workerComplete && scopeClosed
                    && (!cancellationRequested || cancellationComplete)) {
                activeMaintenance.remove(this);
                maintenanceMonitor.notifyAll();
            }
        }
    }

    private static RuntimeException asRuntimeFailure(Throwable failure, String message) {
        return failure instanceof RuntimeException runtime
                ? runtime : new IllegalStateException(message, failure);
    }

    ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
        var observationStore = database.getObservationStore();
        return observationStore instanceof ICancellableDataStore cancellable
                ? cancellable.beginCancellableOperation() : null;
    }

    private boolean shouldStopPurge() {
        return stopRequested || unsafeStopFailure.get() != null
                || Thread.currentThread().isInterrupted();
    }

    private void scheduleNextDailyExport() {
        long delayMillis = calculateDelayUntilMidnightMillis(clock.instant());
        dailyExportTask = scheduler.schedule(this::executeDailyExportAndPurge,
                delayMillis, TimeUnit.MILLISECONDS);
        log.info("Scheduled daily export task for the next {} midnight (in {} ms)",
                dailyFileZone, delayMillis);
    }

    // Utility methods

    private Set<BigId> getDataStreamIdsByOutputNames(String... outputNames) {
        var filter = new DataStreamFilter.Builder()
            .withOutputNames(outputNames)
            .build();

        return database.getDataStreamStore()
            .selectKeys(filter)
            .map(ResourceKey::getInternalID)
            .collect(Collectors.toSet());
    }

    private Map<BigId, IDataStreamInfo> getDataStreamEntriesByOutputNames(String... outputNames) {
        var filter = new DataStreamFilter.Builder()
            .withOutputNames(outputNames)
            .build();

        Map<BigId, IDataStreamInfo> result = new HashMap<>();
        database.getDataStreamStore()
            .selectEntries(filter)
            .forEach(entry -> {
                DataStreamKey key = entry.getKey();
                IDataStreamInfo value = entry.getValue();
                result.put(key.getInternalID(), value);
            });
        return result;
    }

    private Map<String, List<TimeExtent>> getOccupancyWindowsByLane() {
        Map<BigId, IDataStreamInfo> occupancyStreams = getDataStreamEntriesByOutputNames(OUTPUT_OCCUPANCY);
        if (occupancyStreams.isEmpty())
            return Map.of();

        Map<BigId, String> laneByDataStream = new HashMap<>();
        occupancyStreams.forEach((dataStreamId, info) ->
                laneByDataStream.put(dataStreamId, laneKeyForSystem(info)));

        var filter = new ObsFilter.Builder()
                .withDataStreams(occupancyStreams.keySet())
                .build();
        Map<String, List<TimeExtent>> windowsByLane = new HashMap<>();
        database.getObservationStore().select(filter).forEach(obs -> {
            String lane = laneByDataStream.get(obs.getDataStreamID());
            if (lane == null)
                return;
            try {
                Occupancy occupancy = Occupancy.toOccupancy(obs.getResult());
                Instant start = instantFromEpochSeconds(occupancy.getStartTime());
                Instant end = instantFromEpochSeconds(occupancy.getEndTime());
                if (!start.isBefore(end)) {
                    log.warn("Ignoring invalid occupancy window on lane {}: [{}, {})", lane, start, end);
                    return;
                }
                windowsByLane.computeIfAbsent(lane, ignored -> new ArrayList<>())
                        .add(TimeExtent.period(start, end));
            } catch (Exception e) {
                log.warn("Failed to parse occupancy record for lane {}: {}", lane, e.getMessage());
            }
        });
        windowsByLane.values().forEach(windows -> windows.sort(Comparator.comparing(TimeExtent::begin)));
        return windowsByLane;
    }

    private Map<String, Set<BigId>> getDataStreamIdsByLane(String... outputNames) {
        Map<String, Set<BigId>> result = new HashMap<>();
        getDataStreamEntriesByOutputNames(outputNames).forEach((id, info) ->
                result.computeIfAbsent(laneKeyForSystem(info), ignored -> new HashSet<>()).add(id));
        return result;
    }

    /**
     * Uses the immediate parent system as the lane boundary. Sensor and process
     * subsystems in one lane therefore share windows, while peer lanes cannot
     * retain each other's high-volume observations.
     */
    String laneKeyForSystem(IDataStreamInfo dataStreamInfo) {
        BigId systemId = dataStreamInfo.getSystemID().getInternalID();
        BigId parentId = database.getSystemDescStore().getParent(systemId);
        if (parentId != null) {
            var parent = database.getSystemDescStore().getCurrentVersion(parentId);
            if (parent != null && parent.getUniqueIdentifier() != null)
                return parent.getUniqueIdentifier();
        }
        return dataStreamInfo.getSystemID().getUniqueID();
    }

    private static Instant instantFromEpochSeconds(double value) {
        long seconds = (long) Math.floor(value);
        long nanos = Math.round((value - seconds) * 1_000_000_000d);
        if (nanos == 1_000_000_000L) {
            seconds++;
            nanos = 0;
        }
        return Instant.ofEpochSecond(seconds, nanos);
    }

    private List<TimeExtent> mergeAndBufferWindows(List<TimeExtent> windows, Duration buffer) {
        if (windows.isEmpty()) {
            return List.of();
        }

        // First apply buffer to each window
        List<TimeExtent> bufferedWindows = windows.stream()
            .map(w -> TimeExtent.period(w.begin().minus(buffer), w.end().plus(buffer)))
            .sorted(Comparator.comparing(TimeExtent::begin))
            .toList();

        // Merge overlapping windows
        List<TimeExtent> merged = new ArrayList<>();
        TimeExtent current = bufferedWindows.get(0);

        for (int i = 1; i < bufferedWindows.size(); i++) {
            TimeExtent next = bufferedWindows.get(i);
            if (current.intersects(next) || !current.end().isBefore(next.begin())) {
                // Windows overlap or are adjacent, merge them using span
                current = TimeExtent.span(current, next);
            } else {
                // No overlap, add current and move to next
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }

    public void purgeOldConnectionStatus() {
        runCancellableMaintenance(this::purgeOldConnectionStatusWithinScope);
    }

    void purgeOldConnectionStatusWithinScope() {
        if (shouldStopPurge())
            return;
        Set<BigId> ids = getDataStreamIdsByOutputNames(OUTPUT_CONNECTION_STATUS);

        if (ids.isEmpty()) {
            log.debug("No Connection Status dataStreams found");
            return;
        }

        Instant cutoff = clock.instant().minus(Duration.ofHours(CONNECTION_STATUS_RETENTION_HOURS));
        long deleted = deleteObservationsBeforeTime(ids, cutoff);
        if (shouldStopPurge())
            log.debug("Connection Status purge cancelled after deleting {} observations", deleted);
        else
            log.info("Purged {} Connection Status observations older than {}", deleted, cutoff);
    }

    /**
     * Catches up completed daily-file windows in oldest-first order.  The journal owns the
     * per-feed high-water mark, so a restart never has to scan from epoch and a long outage is
     * recovered over bounded cycles.  The synchronized boundary also prevents an administrative
     * invocation from overlapping the scheduled single-threaded task.
     */
    public synchronized void exportAndPurgeDailyFileData() {
        runCancellableMaintenance(this::exportAndPurgeDailyFileDataWithinScope);
    }

    private void exportAndPurgeDailyFileDataWithinScope() {
        if (journalService == null) {
            // Compatibility fallback for installations that have not enabled the durable journal.
            // Such installs retain the historical one-day behavior because they have nowhere safe
            // to store a restart checkpoint.
            exportAndPurgeDailyFileDataWithinScope(
                    previousCompletedDayWindow(clock.instant(), dailyFileZone));
            return;
        }

        if (!journalService.isJournalReady()) {
            // Module startup is concurrent: OSCAR can discover the journal capability before
            // its durable store has opened.  This is not a feed failure and no data is eligible
            // for deletion yet.  The hourly task will retry the same manifest-gated work.
            log.info("Daily File journal catch-up deferred until the durable journal is ready");
            return;
        }

        LocalDate latestEligibleDay = LocalDate.ofInstant(clock.instant(), dailyFileZone).minusDays(1);
        Map<BigId, IDataStreamInfo> streams = getDataStreamEntriesByOutputNames(OUTPUT_DAILY_FILE);
        if (streams.isEmpty()) {
            log.debug("No Daily File data streams found");
            return;
        }

        PriorityQueue<DailyFileCatchUpFeed> work = new PriorityQueue<>(Comparator
                .comparing((DailyFileCatchUpFeed feed) -> feed.nextDay)
                .thenComparing(feed -> feed.feedId));
        for (var entry : streams.entrySet()) {
            try {
                createDailyFileCatchUpFeed(entry.getKey(), entry.getValue(), latestEligibleDay)
                        .ifPresent(work::add);
            } catch (IOException | RuntimeException e) {
                log.error("Unable to initialize Daily File purge progress for {}: {}",
                        entry.getValue().getFullName(), e.getMessage(), e);
            }
        }

        int attempted = 0;
        long deleted = 0;
        while (!shouldStopPurge() && !work.isEmpty() && attempted < dailyFilePurgeWorkLimit) {
            DailyFileCatchUpFeed feed = work.remove();
            LocalDate day = feed.nextDay;
            attempted++;
            try {
                DayWindow window = dayWindow(day, dailyFileZone);
                PurgeDayResult result = purgeDailyFileFeed(feed.dataStreamId, feed.info,
                        feed.laneId, window);
                if (!result.complete)
                    continue; // Do not skip over an unpublished or failed day for this feed.

                deleted += result.deleted;
                if (feed.processedThrough == null || day.isAfter(feed.processedThrough)) {
                    journalService.writeObservationPurgeCheckpoint(feed.feedId, day);
                    feed.processedThrough = day;
                }

                // A late observation can put nextDay behind the durable high-water mark.  After
                // repairing that day, resume immediately after the high-water mark instead of
                // wasting the bounded budget replaying already-empty days.
                LocalDate next = feed.processedThrough != null
                        ? feed.processedThrough.plusDays(1) : day.plusDays(1);
                if (!next.isAfter(latestEligibleDay)) {
                    feed.nextDay = next;
                    work.add(feed);
                }
            } catch (IOException | RuntimeException e) {
                if (shouldStopPurge())
                    break;
                log.error("Daily File purge stopped for feed {} on {} without advancing its checkpoint: {}",
                        feed.feedId, day, e.getMessage(), e);
            }
        }

        log.info("Daily File catch-up processed {} of at most {} lane/day window(s), purged {} observation(s), and left {} feed(s) ready for a later cycle",
                attempted, dailyFilePurgeWorkLimit, deleted, work.size());
    }

    synchronized void exportAndPurgeDailyFileData(DayWindow window) {
        runCancellableMaintenance(() -> exportAndPurgeDailyFileDataWithinScope(window));
    }

    private void exportAndPurgeDailyFileDataWithinScope(DayWindow window) {
        log.info("Exporting and purging Daily File data for immutable window [{}, {})",
                window.startInclusive, window.endExclusive);
        Map<BigId, IDataStreamInfo> streams = getDataStreamEntriesByOutputNames(OUTPUT_DAILY_FILE);
        if (streams.isEmpty()) {
            log.debug("No Daily File data streams found");
            return;
        }

        long deleted = 0;
        for (var entry : streams.entrySet()) {
            if (shouldStopPurge())
                break;
            BigId dataStreamId = entry.getKey();
            IDataStreamInfo info = entry.getValue();
            PurgeDayResult result = purgeDailyFileFeed(dataStreamId, info,
                    laneKeyForSystem(info), window);
            if (result.complete)
                deleted += result.deleted;
        }
        log.info("Purged {} Daily File observations from [{}, {}) after verified publication",
                deleted, window.startInclusive, window.endExclusive);
    }

    private Optional<DailyFileCatchUpFeed> createDailyFileCatchUpFeed(
            BigId dataStreamId, IDataStreamInfo info, LocalDate latestEligibleDay) throws IOException {
        String feedId = info.getFullName();
        String laneId = laneKeyForSystem(info);
        Optional<LocalDate> checkpoint = journalService.readObservationPurgeCheckpoint(feedId);
        Optional<LocalDate> databaseFirst = firstObservationDay(info);

        LocalDate nextDay;
        if (checkpoint.isPresent()) {
            LocalDate afterCheckpoint = checkpoint.get().plusDays(1);
            // Revisit a late committed record behind the high-water mark.  The half-open deletion
            // and immutable publication gate make this replay idempotent.
            nextDay = databaseFirst.filter(day -> day.isBefore(afterCheckpoint))
                    .orElse(afterCheckpoint);
        } else {
            Optional<LocalDate> journalFirst = journalService.findEarliestJournalDay(laneId);
            nextDay = earlier(databaseFirst, journalFirst).orElse(null);
        }

        if (nextDay == null || nextDay.isAfter(latestEligibleDay))
            return Optional.empty();
        return Optional.of(new DailyFileCatchUpFeed(dataStreamId, info, feedId, laneId,
                checkpoint.orElse(null), nextDay));
    }

    private Optional<LocalDate> firstObservationDay(IDataStreamInfo info) {
        TimeExtent range = info.getPhenomenonTimeRange();
        if (range == null || range.begin() == null)
            return Optional.empty();
        return Optional.of(LocalDate.ofInstant(range.begin(), dailyFileZone));
    }

    private static Optional<LocalDate> earlier(Optional<LocalDate> first,
                                                Optional<LocalDate> second) {
        if (first.isEmpty())
            return second;
        if (second.isEmpty())
            return first;
        return Optional.of(first.get().isBefore(second.get()) ? first.get() : second.get());
    }

    private PurgeDayResult purgeDailyFileFeed(BigId dataStreamId, IDataStreamInfo info,
                                               String laneId, DayWindow window) {
        if (!hasObservationsInTimeRange(dataStreamId, window.startInclusive, window.endExclusive)) {
            // Preserve the legacy exporter contract for installations that have not enabled
            // the durable journal: each discovered dailyFile stream still receives a dated,
            // header-only CSV for an empty completed day.  Journal-backed installations do not
            // synthesize an empty source file because their signed manifest is authoritative.
            if (journalService == null) {
                String objectKey = legacyObjectKey(info, window.day);
                boolean published = exportDataStreamToCSV(dataStreamId, objectKey,
                        window.startInclusive, window.endExclusive);
                return new PurgeDayResult(published, 0);
            }
            return new PurgeDayResult(true, 0);
        }

        boolean safelyPublished;
        if (journalService != null) {
            safelyPublished = journalService.isPublished(laneId,
                    window.startInclusive, window.endExclusive);
            if (!safelyPublished) {
                log.warn("Skipping Daily File purge for lane {} because the journal manifest for [{}, {}) is not published",
                        laneId, window.startInclusive, window.endExclusive);
            }
        } else {
            String objectKey = legacyObjectKey(info, window.day);
            safelyPublished = exportDataStreamToCSV(dataStreamId, objectKey,
                    window.startInclusive, window.endExclusive);
        }
        if (!safelyPublished)
            return new PurgeDayResult(false, 0);

        long deleted = deleteObservationsInTimeRange(Set.of(dataStreamId),
                window.startInclusive, window.endExclusive);
        return new PurgeDayResult(true, deleted);
    }

    private boolean hasObservationsInTimeRange(BigId dataStreamId, Instant start, Instant end) {
        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(dataStreamId)
                .withPhenomenonTime(halfOpenTimeRange(start, end))
                .withLimit(1)
                .build();
        try (var observations = database.getObservationStore().select(filter)) {
            return observations.anyMatch(obs -> isInsideHalfOpenWindow(
                    obs.getPhenomenonTime(), start, end));
        }
    }

    public void purgeNonOccupancyData() {
        runCancellableMaintenance(this::purgeNonOccupancyDataWithinScope);
    }

    private void purgeNonOccupancyDataWithinScope() {
        log.info("Purging data outside occupancy windows...");

        Map<String, List<TimeExtent>> windowsByLane = getOccupancyWindowsByLane();
        if (windowsByLane.isEmpty()) {
            log.warn("No occupancy windows found - skipping non-occupancy purge to avoid data loss");
            return;
        }

        Map<String, Set<BigId>> streamsByLane = getDataStreamIdsByLane(
            OUTPUT_GAMMA_COUNTS,
            OUTPUT_NEUTRON_COUNTS,
            OUTPUT_GAMMA_THRESHOLD,
            OUTPUT_SPEED,
            OUTPUT_BACKGROUND_REPORT,
            OUTPUT_FOREGROUND_REPORT,
            OUTPUT_STATUS
        );

        if (streamsByLane.isEmpty()) {
            log.debug("No high-volume dataStreams found");
            return;
        }

        long totalDeleted = 0;
        Duration buffer = Duration.ofSeconds(occupancyBufferSeconds);
        for (var entry : streamsByLane.entrySet()) {
            if (shouldStopPurge())
                break;
            String laneId = entry.getKey();
            List<TimeExtent> laneWindows = windowsByLane.get(laneId);
            if (laneWindows == null || laneWindows.isEmpty()) {
                log.warn("No occupancy windows found for lane {}; leaving its {} high-volume streams untouched",
                        laneId, entry.getValue().size());
                continue;
            }
            List<TimeExtent> merged = mergeAndBufferWindows(laneWindows, buffer);
            log.info("Processing {} dataStreams for lane {} against {} merged occupancy windows",
                    entry.getValue().size(), laneId, merged.size());
            totalDeleted += purgeGapsBetweenWindows(entry.getValue(), merged);
        }
        log.info("Purged {} observations outside occupancy windows", totalDeleted);
    }

    private long purgeGapsBetweenWindows(Set<BigId> dataStreamIds, List<TimeExtent> windows) {
        long totalDeleted = 0;

        // Delete data before the first occupancy
        if (!windows.isEmpty()) {
            TimeExtent firstWindow = windows.get(0);
            // Only delete data from a reasonable past (e.g., 30 days ago) to avoid deleting historical data
            Instant earliestPurge = clock.instant().minus(Duration.ofDays(30));
            if (firstWindow.begin().isAfter(earliestPurge)) {
                long deleted = deleteObservationsInTimeRange(dataStreamIds, earliestPurge, firstWindow.begin());
                totalDeleted += deleted;
                log.debug("Deleted {} observations before first occupancy window", deleted);
            }
        }

        // Delete data in gaps between occupancy windows
        Instant previousEnd = windows.isEmpty() ? Instant.EPOCH : windows.get(0).end();
        for (int i = 1; i < windows.size(); i++) {
            if (shouldStopPurge())
                return totalDeleted;
            TimeExtent window = windows.get(i);
            if (previousEnd.isBefore(window.begin())) {
                // There's a gap between windows - delete data in this gap
                long deleted = deleteObservationsInTimeRange(dataStreamIds, previousEnd, window.begin());
                totalDeleted += deleted;
                log.debug("Deleted {} observations in gap between {} and {}", deleted, previousEnd, window.begin());
            }
            previousEnd = window.end();
        }

        // Delete data after the last occupancy up to current time minus a safety margin
        // Keep recent data (last hour) in case new occupancies are being recorded
        if (!shouldStopPurge() && !windows.isEmpty()) {
            TimeExtent lastWindow = windows.get(windows.size() - 1);
            Instant safetyMargin = clock.instant().minus(Duration.ofHours(1));
            if (lastWindow.end().isBefore(safetyMargin)) {
                long deleted = deleteObservationsInTimeRange(dataStreamIds, lastWindow.end(), safetyMargin);
                totalDeleted += deleted;
                log.debug("Deleted {} observations after last occupancy window", deleted);
            }
        }

        return totalDeleted;
    }

    private long deleteObservationsBeforeTime(Set<BigId> dataStreamIds, Instant cutoff) {
        if (dataStreamIds.isEmpty()) {
            return 0;
        }

        // Use a reasonable earliest time to avoid querying from epoch
        Instant earliestTime = clock.instant().minus(Duration.ofDays(365));
        return deleteObservationsInTimeRange(dataStreamIds, earliestTime, cutoff);
    }

    private long deleteObservationsInTimeRange(Set<BigId> dataStreamIds, Instant start, Instant end) {
        if (dataStreamIds.isEmpty() || !start.isBefore(end)) {
            return 0;
        }

        long totalDeleted = 0;
        while (!shouldStopPurge()) {
            var candidates = new ObsFilter.Builder()
                    .withDataStreams(dataStreamIds)
                    .withPhenomenonTime(halfOpenTimeRange(start, end))
                    .withLimit(DELETE_BATCH_SIZE)
                    .build();

            List<BigId> observationIds;
            try (var entries = database.getObservationStore().selectEntries(candidates)) {
                observationIds = entries
                        .filter(entry -> isInsideHalfOpenWindow(
                                entry.getValue().getPhenomenonTime(), start, end))
                        .map(Map.Entry::getKey)
                        .toList();
            }
            if (shouldStopPurge())
                return totalDeleted;
            if (observationIds.isEmpty())
                return totalDeleted;

            // Selection and deletion are separate datastore calls.  Reapply every eligibility
            // predicate in the delete itself so a concurrent update cannot move a selected row
            // outside the retention window (or to another stream) and still have it removed by
            // ID.  Stores that translate filters to SQL perform this as one predicate-guarded
            // DELETE; the H2 store applies the same filter while holding its mutation lock.
            long deleted = database.getObservationStore().removeEntries(
                    new ObsFilter.Builder()
                            .withInternalIDs(observationIds)
                            .withDataStreams(dataStreamIds)
                            .withPhenomenonTime(halfOpenTimeRange(start, end))
                            // Revalidate in the datastore after selection. Some stores optimize
                            // internal-ID removal and otherwise bypass secondary filter fields;
                            // this bounded predicate protects rows concurrently moved out of the
                            // immutable retention window. The ID list caps the candidate set, so
                            // it does not cause the unbounded multi-stream scan avoided above.
                            .withValuePredicate(obs -> dataStreamIds.contains(
                                    obs.getDataStreamID()) && isInsideHalfOpenWindow(
                                    obs.getPhenomenonTime(), start, end))
                            .build());
            if (deleted == 0) {
                // Every candidate may legitimately have become ineligible after selection.  Stop
                // this pass rather than spinning on a datastore that cannot currently progress;
                // the next scheduled pass will discover any still-eligible rows.
                log.debug("Selected observation batch became ineligible before guarded deletion");
                return totalDeleted;
            }
            if (deleted > observationIds.size())
                throw new IllegalStateException("Observation store deleted more rows than the selected batch");
            totalDeleted += deleted;
        }
        return totalDeleted;
    }

    private String legacyObjectKey(IDataStreamInfo dataStreamInfo, LocalDate day) {
        String systemId = dataStreamInfo.getSystemID().getUniqueID();
        String laneId = systemId.contains(":")
                ? systemId.substring(systemId.lastIndexOf(':') + 1) : systemId;
        return laneId.replaceAll("[^A-Za-z0-9._-]", "_") + "_"
                + day.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
    }

    private boolean exportDataStreamToCSV(BigId dataStreamId, String objectKey,
                                          Instant startTime, Instant endTime) {
        var filter = new ObsFilter.Builder()
            .withDataStreams(Set.of(dataStreamId))
            .withPhenomenonTime(halfOpenTimeRange(startTime, endTime))
            .build();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", "text/csv");

        try (OutputStream outputStream = bucketStore.putObject(DAILY_FILES_BUCKET, objectKey, metadata);
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

            // Write CSV header
            writer.write("message,timestamp\n");

            long counter = 0;
            try (var observations = database.getObservationStore().select(filter)) {
                var iterator = observations.iterator();
                while (!shouldStopPurge() && iterator.hasNext()) {
                    var obs = iterator.next();
                    String timestamp = obs.getPhenomenonTime().toString();
                    String message = "";
                    if (obs.getResult() != null && obs.getResult().getAtomCount() > 1) {
                        message = obs.getResult().getStringValue(1);
                        message = "\"" + message.replace("\"", "\"\"") + "\"";
                    }
                    writer.write(String.format("%s,%s\n", message, timestamp));
                    counter++;
                }
            }
            writer.flush();
            log.info("Exported {} DailyFileOutput records to bucket {}/{}", counter, DAILY_FILES_BUCKET, objectKey);
            return true;
        } catch (IOException | DataStoreException e) {
            log.error("Failed to export DailyFileOutput to bucket {}/{}: {}", DAILY_FILES_BUCKET, objectKey, e.getMessage(), e);
            return false;
        }
    }

    static TemporalFilter halfOpenTimeRange(Instant startInclusive, Instant endExclusive) {
        return new TemporalFilter.Builder()
                .withRange(Range.closedOpen(startInclusive, endExclusive))
                .build();
    }

    private static boolean isInsideHalfOpenWindow(Instant value,
                                                   Instant startInclusive,
                                                   Instant endExclusive) {
        return value != null && !value.isBefore(startInclusive) && value.isBefore(endExclusive);
    }

    long calculateDelayUntilMidnightMillis(Instant now) {
        Instant nextMidnight = LocalDate.ofInstant(now, dailyFileZone).plusDays(1)
                .atStartOfDay(dailyFileZone).toInstant();
        Duration delay = Duration.between(now, nextMidnight);
        long delayMillis = delay.toMillis();
        // Duration#toMillis truncates. Round up so a sub-millisecond
        // remainder can never schedule cleanup before the day is complete.
        if (delay.compareTo(Duration.ofMillis(delayMillis)) > 0)
            delayMillis++;
        return Math.max(1L, delayMillis);
    }

    static DayWindow previousCompletedDayWindow(Instant capturedNow, ZoneId zoneId) {
        LocalDate day = LocalDate.ofInstant(capturedNow, zoneId).minusDays(1);
        return dayWindow(day, zoneId);
    }

    static DayWindow dayWindow(LocalDate day, ZoneId zoneId) {
        return new DayWindow(day, day.atStartOfDay(zoneId).toInstant(),
                day.plusDays(1).atStartOfDay(zoneId).toInstant());
    }

    record DayWindow(LocalDate day, Instant startInclusive, Instant endExclusive) {
    }

    private record PurgeDayResult(boolean complete, long deleted) {
    }

    private static final class DailyFileCatchUpFeed {
        private final BigId dataStreamId;
        private final IDataStreamInfo info;
        private final String feedId;
        private final String laneId;
        private LocalDate processedThrough;
        private LocalDate nextDay;

        private DailyFileCatchUpFeed(BigId dataStreamId, IDataStreamInfo info, String feedId,
                                     String laneId, LocalDate processedThrough, LocalDate nextDay) {
            this.dataStreamId = dataStreamId;
            this.info = info;
            this.feedId = feedId;
            this.laneId = laneId;
            this.processedThrough = processedThrough;
            this.nextDay = nextDay;
        }
    }
}
