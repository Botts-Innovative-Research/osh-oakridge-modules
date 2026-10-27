package com.botts.impl.service.oscar.stats;

import com.botts.impl.service.oscar.OSCARSystem;
import com.google.common.collect.Range;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.ICancellableDataStore;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.resource.ResourceKey;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.utils.Async;
import org.vast.data.TextEncodingImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StatisticsOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "siteStatistics";
    private static final long DEFAULT_STOP_TIMEOUT_MILLIS = 5_000L;
    private static final Instant LEGACY_STATS_EPOCH_SECONDS_MIN =
            Instant.parse("2020-01-01T00:00:00Z");
    private static final Instant LEGACY_STATS_EPOCH_SECONDS_MAX =
            Instant.parse("2100-01-01T00:00:00Z");

    public static String gammaNeutronAlarmCQL = "gammaAlarm = true AND neutronAlarm = true";
    public static String gammaAlarmCQL = "gammaAlarm = true AND neutronAlarm = false";
    public static String neutronAlarmCQL = "gammaAlarm = false AND neutronAlarm = true";
    public static String faultCQL = "alarmState = 'Fault - Neutron High' OR alarmState = 'Fault - Gamma Low' OR alarmState = 'Fault - Gamma High'";
    public static String gammaFaultCQL = "alarmState = 'Fault - Gamma Low' OR alarmState = 'Fault - Gamma High'";
    public static String gammaHighFaultCQL = "alarmState = 'Fault - Gamma High'";
    public static String gammaLowFaultCQL = "alarmState = 'Fault - Gamma Low'";
    public static String neutronFaultCQL = "alarmState = 'Fault - Neutron High'";
    public static String tamperCQL = "tamperStatus = true";

    // RS350 alarm CQL filters (AlarmOutput uses alarmCategoryCode field)
    public static String rs350GammaAlarmCQL = "alarmCategoryCode = 'Gamma'";
    public static String rs350NeutronAlarmCQL = "alarmCategoryCode = 'Neutron'";

    private static final String DEF_ALARM_CATEGORY = RADHelper.getRadUri("AlarmCategoryCode");

    private final DataComponent recordDescription;
    private final DataEncoding recommendedEncoding;
    private final int samplingPeriod;

    private final Object lifecycleMonitor = new Object();
    private final Object workMonitor = new Object();
    private final ReentrantLock publicationLock = new ReentrantLock(true);
    private final Set<StatisticsWork> activeWork = new java.util.HashSet<>();
    private final ThreadLocal<Boolean> operationScope =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private final AtomicReference<RuntimeException> unsafeStopFailure = new AtomicReference<>();
    private volatile boolean stopRequested;
    private long lifecycleGeneration;
    private ScheduledExecutorService service;
    private final IObsSystemDatabase database;
    private Map<String, Set<BigId>> observedPropertyToDataStreamIds;

    private record LastStatisticObservationData(Instant timestamp, Statistics total) {

    }

    public StatisticsOutput(OSCARSystem parentSensor, IObsSystemDatabase database, int samplingPeriod) {
        super(NAME, parentSensor);
        this.database = database;
        RADHelper fac = new RADHelper();
        recordDescription = fac.createSiteStatistics();
        recommendedEncoding = new TextEncodingImpl();
        this.samplingPeriod = samplingPeriod;
    }

    /**
     * Initializes the mapping between observed properties and their datastream IDs.
     */
    private void initializeDataStreamMapping() {
        if (observedPropertyToDataStreamIds == null)
            observedPropertyToDataStreamIds = new HashMap<>();

        // Query for each observed property type
        String[] observedProperties = {
                RADHelper.DEF_OCCUPANCY,
                RADHelper.DEF_GAMMA,
                RADHelper.DEF_NEUTRON,
                RADHelper.DEF_ALARM,
                RADHelper.DEF_TAMPER,
                DEF_ALARM_CATEGORY
        };

        for (String property : observedProperties) {
            Set<BigId> dataStreamIds = database.getDataStreamStore()
                    .selectKeys(new DataStreamFilter.Builder()
                            .withObservedProperties(property)
                            .build())
                    .map(ResourceKey::getInternalID)
                    .collect(Collectors.toSet());

            observedPropertyToDataStreamIds.put(property, dataStreamIds);
        }
    }

    public void start() {
        synchronized (lifecycleMonitor) {
            if (service != null && !service.isTerminated())
                return;
            RuntimeException priorFailure = unsafeStopFailure.get();
            if (priorFailure != null)
                throw new IllegalStateException(
                        "Statistics output cannot restart after unsafe datastore cancellation", priorFailure);
            synchronized (workMonitor) {
                if (!activeWork.isEmpty())
                    throw new IllegalStateException(
                            "Statistics output still has active database work");
                stopRequested = false;
            }
            service = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "OSCAR-statistics");
                thread.setDaemon(true);
                return thread;
            });
            service.scheduleWithFixedDelay(this::publishLatestStatistics, 0L,
                    samplingPeriod, TimeUnit.MINUTES);
        }
    }

    public void stop() {
        stop(DEFAULT_STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void stop(long timeout, TimeUnit unit) {
        java.util.Objects.requireNonNull(unit, "unit");
        if (timeout <= 0)
            throw new IllegalArgumentException("Statistics stop timeout must be positive");

        long budgetNanos = Math.max(1L, unit.toNanos(timeout));
        long deadline = System.nanoTime() + budgetNanos;
        requestStop();
        awaitStop(Math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
    }

    /**
     * Interrupts the scheduler and asynchronously cancels every scoped datastore operation.
     * Driver cancellation is never run on the module-registry thread because it may itself block.
     */
    public void requestStop() {
        synchronized (lifecycleMonitor) {
            synchronized (workMonitor) {
                stopRequested = true;
                lifecycleGeneration++;
                requestActiveWorkCancellationLocked();
            }
            if (service != null)
                service.shutdownNow();
        }
    }

    /**
     * Marks every currently active operation as cancellation-pending while {@link #workMonitor}
     * is still held. This closes the finish-versus-cancel race: a worker cannot remove itself
     * between discovery and marking, so a successful await also proves its cancellation helper
     * completed.
     */
    void requestActiveWorkCancellationLocked() {
        if (!Thread.holdsLock(workMonitor))
            throw new IllegalStateException("Statistics work monitor must be held");
        new java.util.ArrayList<>(activeWork)
                .forEach(StatisticsWork::requestCancellation);
    }

    /**
     * Waits for scheduler termination, database-scope closure, and cancellation completion.
     * A successful return therefore guarantees that no statistics worker can publish later.
     */
    public void awaitStop(long timeout, TimeUnit unit) {
        java.util.Objects.requireNonNull(unit, "unit");
        if (timeout <= 0)
            throw new IllegalArgumentException("Statistics stop timeout must be positive");
        long budgetNanos = Math.max(1L, unit.toNanos(timeout));
        long deadline = System.nanoTime() + budgetNanos;
        ScheduledExecutorService stopping;
        synchronized (lifecycleMonitor) {
            stopping = service;
        }
        try {
            while (true) {
                boolean schedulerTerminated = stopping == null || stopping.isTerminated();
                boolean workTerminated;
                synchronized (workMonitor) {
                    workTerminated = activeWork.isEmpty();
                }
                if (schedulerTerminated && workTerminated)
                    break;
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0)
                    throw new IllegalStateException(
                            "Statistics output worker did not terminate within the stop deadline");
                long slice = Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(50));
                if (!schedulerTerminated) {
                    stopping.awaitTermination(slice, TimeUnit.NANOSECONDS);
                } else {
                    synchronized (workMonitor) {
                        if (!activeWork.isEmpty())
                            TimeUnit.NANOSECONDS.timedWait(workMonitor, slice);
                    }
                }
            }
        } catch (InterruptedException error) {
            if (stopping != null)
                stopping.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting for statistics output to stop", error);
        }

        RuntimeException unsafeFailure = unsafeStopFailure.get();
        if (unsafeFailure != null)
            throw new IllegalStateException(
                    "Statistics output stopped, but datastore cancellation or scope cleanup failed",
                    unsafeFailure);
        synchronized (lifecycleMonitor) {
            if (service == stopping)
                service = null;
        }
    }

    public final void publishLatestStatistics() {
        runSerializedPublication(() -> runCancellableStatistics(() -> {
            publishLatestStatisticsWithinScope();
            return null;
        }));
    }

    /**
     * Publishes a complete site-statistics record and returns the ID assigned to that
     * exact publication.  The baseline lookup, publication, and fresh-ID check are
     * serialized with scheduled publications so a caller can never be handed an older
     * observation merely because one already exists in the datastore.
     */
    public final BigId publishLatestStatisticsAndGetObservationId() {
        return runSerializedPublication(() -> runCancellableStatistics(() -> {
            BigId previousObservationId = getLatestObservationIdWithinScope();
            publishLatestStatisticsWithinScope();
            return waitForLatestObservationIdWithinScope(previousObservationId);
        }));
    }

    /**
     * Full publications are intentionally single-filed.  Scoped/custom counts do not
     * use this lock and remain independently cancellable.  Acquiring the lock before
     * opening the datastore operation prevents a queued publication from advertising
     * database work that has not started and preserves truthful shutdown accounting.
     */
    private <T> T runSerializedPublication(Supplier<T> publication) {
        long requestedGeneration;
        synchronized (workMonitor) {
            if (stopRequested)
                throw new CancellationException("Statistics output is stopping");
            requestedGeneration = lifecycleGeneration;
        }

        boolean locked = false;
        try {
            publicationLock.lockInterruptibly();
            locked = true;
            synchronized (workMonitor) {
                if (stopRequested || lifecycleGeneration != requestedGeneration)
                    throw new CancellationException(
                            "Statistics publication belongs to a stopped lifecycle");
            }
            return publication.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            CancellationException cancelled =
                    new CancellationException("Interrupted while waiting to publish statistics");
            cancelled.initCause(error);
            throw cancelled;
        } finally {
            if (locked)
                publicationLock.unlock();
        }
    }

    protected void publishLatestStatisticsWithinScope() {
        initializeDataStreamMapping();
        getLogger().info("Starting stats counting...");
        long calculationStarted = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? recordDescription.createDataBlock() : latestRecord.renew();
        Instant now = normalizePublicationTime(Instant.now());

        // Get total counts from all observations
        LastStatisticObservationData lastStatisticObservationData = getLastObservation();
        if (lastStatisticObservationData != null) {
            // A statistics observation is keyed by its sampling time. Preserve ordering even
            // when two empty/fast publications occur within the SWE field's millisecond resolution.
            now = publicationTimeAfter(now, lastStatisticObservationData.timestamp());
        }

        int i = 0;
        dataBlock.setTimeStamp(i++, now);

        if (lastStatisticObservationData != null) {
            Instant lastTimestamp = lastStatisticObservationData.timestamp();
            Statistics previousTotal = lastStatisticObservationData.total();
            Statistics incrementalStats = getStatsAfter(lastTimestamp, now, Set.of());

            Statistics newTotal = previousTotal.add(incrementalStats);

            i = populateDataBlockWithStats(dataBlock, i, newTotal);
        } else {
            getLogger().debug("No previous stats observation found, calculating from epoch to {}", now);
            i = populateDataBlock(dataBlock, i, Instant.EPOCH, now);
        }

        // last 30 days
        Instant monthStart = now.minus(30, ChronoUnit.DAYS);
        i = populateDataBlock(dataBlock, i, monthStart, now);

        // last 7 days
        Instant weekStart = now.minus(7, ChronoUnit.DAYS);
        i = populateDataBlock(dataBlock, i, weekStart, now);

        // last 24 hours
        Instant dayStart = now.minus(24, ChronoUnit.HOURS);
        populateDataBlock(dataBlock, i, dayStart, now);

        latestRecord = dataBlock;
        latestRecordTime = now.toEpochMilli();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
        getLogger().info("Finished stats counting in {}s",
                (System.currentTimeMillis() - calculationStarted) / 1000);
    }

    private BigId getLatestObservationIdWithinScope() {
        var keys = database.getObservationStore().selectKeys(new ObsFilter.Builder()
                .withLatestResult()
                .withSystems().withUniqueIDs(parentSensor.getUniqueIdentifier())
                .done()
                .withDataStreams().withOutputNames(NAME)
                .done()
                .withLimit(1)
                .build()).toList();
        return keys.size() == 1 ? keys.get(0) : null;
    }

    private BigId waitForLatestObservationIdWithinScope(BigId previousObservationId) {
        try {
            final List<BigId> result = new ArrayList<>();

            Async.waitForCondition(() -> {
                BigId latestObservationId = getLatestObservationIdWithinScope();
                if (latestObservationId != null
                        && (previousObservationId == null
                        || !previousObservationId.equals(latestObservationId))) {
                    result.clear();
                    result.add(latestObservationId);
                    return true;
                }
                return false;
            }, 5000);
            return result.isEmpty() ? null : result.get(0);
        } catch (TimeoutException e) {
            getLogger().error("Could not find latest observation ID");
            return null;
        }
    }

    /**
     * Populates the data block with statistics for the given time range.
     */
    public int populateDataBlock(DataBlock dataBlock, int i, Instant start, Instant end) {
        return populateDataBlock(dataBlock, i, start, end, Set.of());
    }

    public int populateDataBlock(DataBlock dataBlock, int i, Instant start, Instant end, Collection<String> laneUIDs) {
        return runCancellableStatistics(() -> {
            Statistics stats = getStats(start, end, laneUIDs);
            return populateDataBlockWithStats(dataBlock, i, stats);
        });
    }

    private int populateDataBlockWithStats(DataBlock dataBlock, int i, Statistics stats) {
        dataBlock.setLongValue(i++, stats.getNumOccupancies());
        dataBlock.setLongValue(i++, stats.getNumGammaAlarms());
        dataBlock.setLongValue(i++, stats.getNumNeutronAlarms());
        dataBlock.setLongValue(i++, stats.getNumGammaNeutronAlarms());
        dataBlock.setLongValue(i++, stats.getNumFaults());
        dataBlock.setLongValue(i++, stats.getNumGammaFaults());
        dataBlock.setLongValue(i++, stats.getNumNeutronFaults());
        dataBlock.setLongValue(i++, stats.getNumTampers());
        return i;
    }

    /**
     * Gets statistics by fetching from database
     */
    protected Statistics getStats(Instant start, Instant end) {
        return getStats(start, end, Set.of());
    }

    protected Statistics getStats(Instant start, Instant end, Collection<String> laneUIDs) {
        return getStats(timeRange(start, end, false), laneUIDs);
    }

    /** Counts only observations strictly newer than the previous publication cutoff. */
    protected Statistics getStatsAfter(Instant exclusiveStart, Instant inclusiveEnd,
                                       Collection<String> laneUIDs) {
        return getStats(timeRange(exclusiveStart, inclusiveEnd, true), laneUIDs);
    }

    private Statistics getStats(Range<Instant> timeRange, Collection<String> laneUIDs) {
        long numOccupancies = countObservations(database, null, timeRange, laneUIDs, RADHelper.DEF_OCCUPANCY);
        long numGammaAlarms = countObservations(database, gammaAlarmCQL, timeRange, laneUIDs, RADHelper.DEF_OCCUPANCY);
        long numNeutronAlarms = countObservations(database, neutronAlarmCQL, timeRange, laneUIDs, RADHelper.DEF_OCCUPANCY);
        long numGammaNeutronAlarms = countObservations(database, gammaNeutronAlarmCQL, timeRange, laneUIDs, RADHelper.DEF_OCCUPANCY);
        long numGammaFaults = countObservations(database, gammaFaultCQL, timeRange, laneUIDs, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        long numNeutronFaults = countObservations(database, neutronFaultCQL, timeRange, laneUIDs, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);
        long numTampers = countObservations(database, tamperCQL, timeRange, laneUIDs, RADHelper.DEF_TAMPER);
        long numFaults = countObservations(database, faultCQL, timeRange, laneUIDs, RADHelper.DEF_GAMMA, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM) + numTampers;

        // RS350 alarm counts from raw AlarmOutput (uses alarmCategoryCode observed property)
        long numRS350GammaAlarms = countObservations(database, rs350GammaAlarmCQL, timeRange, laneUIDs, DEF_ALARM_CATEGORY);
        long numRS350NeutronAlarms = countObservations(database, rs350NeutronAlarmCQL, timeRange, laneUIDs, DEF_ALARM_CATEGORY);
        numGammaAlarms += numRS350GammaAlarms;
        numNeutronAlarms += numRS350NeutronAlarms;
        numOccupancies += numRS350GammaAlarms + numRS350NeutronAlarms;

        return new Statistics.Builder()
                .numOccupancies(numOccupancies)
                .numGammaAlarms(numGammaAlarms)
                .numNeutronAlarms(numNeutronAlarms)
                .numGammaNeutronAlarms(numGammaNeutronAlarms)
                .numFaults(numFaults)
                .numGammaFaults(numGammaFaults)
                .numNeutronFaults(numNeutronFaults)
                .numTampers(numTampers)
                .build();
    }

    /**
     * Counts observations from database (used by external callers like submitCommand).
     */
    private long countObservations(IObsSystemDatabase database, String cqlValue, Range<Instant> timeRange,
                                   Collection<String> laneUIDs, String... observedProperty) {
        var dsFilterBuilder = new DataStreamFilter.Builder()
                .withObservedProperties(observedProperty);

        if (laneUIDs != null && !laneUIDs.isEmpty()) {
            dsFilterBuilder.withSystems()
                    .withUniqueIDs(laneUIDs.toArray(String[]::new))
                    .includeMembers(true)
                    .done();
        }

        var dsFilter = dsFilterBuilder.build();

        var obsBuilder = new ObsFilter.Builder().withDataStreams(dsFilter);

        if (timeRange != null)
            obsBuilder.withPhenomenonTime().withRange(timeRange).done();

        if (cqlValue != null &&  !cqlValue.isBlank()) {
            obsBuilder.withCQLFilter(cqlValue);
        }

        return database.getObservationStore().countMatchingEntries(obsBuilder.build());
    }

    /**
     * get last observation
     */
    private LastStatisticObservationData getLastObservation() {
        var query = database.getObservationStore().select(new ObsFilter.Builder()
                .withSystems().withUniqueIDs(parentSensor.getUniqueIdentifier()).done()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withOutputNames(NAME)
                        .build())
                .withLatestResult()
                .build());

        var observations = query.toList();

        if (observations.isEmpty())
            return null;

        var result = observations.get(0).getResult();

        Instant timestamp = decodeStoredStatisticsTimestamp(result);

        Statistics allTime = getLastObservationCount(result, 0);

        return new LastStatisticObservationData(timestamp, allTime);
    }

    static Range<Instant> timeRange(Instant begin, Instant end, boolean beginExclusive) {
        if (begin == null || end == null)
            return null;
        return beginExclusive ? Range.openClosed(begin, end) : Range.closed(begin, end);
    }

    /**
     * Before OSCAR 3.11.0, statistics wrote epoch seconds with {@code setLongValue}
     * into an INSTANT field. That produced an apparent January 1970 timestamp, while
     * {@code getLongValue} still exposed the intended epoch-second number. Restrict
     * compatibility decoding to the bounded lifetime of plausible OSCAR records so a
     * genuinely modern INSTANT is never reinterpreted by magnitude alone.
     */
    static Instant decodeStoredStatisticsTimestamp(DataBlock result) {
        Instant directTimestamp = result.getTimeStamp(0);
        if (directTimestamp == null)
            return null;

        long legacyEpochSeconds = result.getLongValue(0);
        if (directTimestamp.isBefore(LEGACY_STATS_EPOCH_SECONDS_MIN)
                && legacyEpochSeconds >= LEGACY_STATS_EPOCH_SECONDS_MIN.getEpochSecond()
                && legacyEpochSeconds < LEGACY_STATS_EPOCH_SECONDS_MAX.getEpochSecond()) {
            return Instant.ofEpochSecond(legacyEpochSeconds);
        }
        return directTimestamp;
    }

    /**
     * The persisted SWE Time field is backed by epoch milliseconds. PostgreSQL and
     * the cold archive can retain finer precision, but using an exact millisecond
     * here keeps the result value and observation key identical across every tier.
     */
    static Instant normalizePublicationTime(Instant timestamp) {
        return timestamp.truncatedTo(ChronoUnit.MILLIS);
    }

    /** Returns an exact-millisecond timestamp strictly newer than the prior publication. */
    static Instant publicationTimeAfter(Instant candidate, Instant priorPublication) {
        Instant normalizedCandidate = normalizePublicationTime(candidate);
        if (priorPublication == null || normalizedCandidate.isAfter(priorPublication))
            return normalizedCandidate;
        return normalizePublicationTime(priorPublication).plus(1, ChronoUnit.MILLIS);
    }

    private <T> T runCancellableStatistics(Supplier<T> operation) {
        if (Boolean.TRUE.equals(operationScope.get()))
            return operation.get();

        StatisticsWork work;
        synchronized (workMonitor) {
            if (stopRequested)
                throw new CancellationException("Statistics output is stopping");
            work = new StatisticsWork();
            activeWork.add(work);
        }

        ICancellableDataStore.CancellableOperation datastoreOperation = null;
        boolean operationAttached = false;
        boolean scopeClosed = true;
        Throwable primaryFailure = null;
        Throwable closeFailure = null;
        try {
            try {
                datastoreOperation = beginCancellableDataStoreOperation();
            } catch (RuntimeException | Error error) {
                latchUnsafeStopFailure(asRuntimeFailure(error,
                        "Datastore operation-scope creation failed"));
                throw error;
            }
            work.attachOperation(datastoreOperation);
            operationAttached = true;
            operationScope.set(Boolean.TRUE);
            if (stopRequested) {
                work.requestCancellation();
                throw new CancellationException("Statistics output is stopping");
            }
            return operation.get();
        } catch (RuntimeException | Error error) {
            primaryFailure = error;
            throw error;
        } finally {
            operationScope.remove();
            if (!operationAttached)
                work.attachOperation(null);
            if (datastoreOperation != null) {
                try {
                    datastoreOperation.close();
                } catch (RuntimeException | Error error) {
                    scopeClosed = false;
                    closeFailure = error;
                    latchUnsafeStopFailure(asRuntimeFailure(error,
                            "Datastore operation-scope cleanup failed"));
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

    protected ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
        var observationStore = database.getObservationStore();
        return observationStore instanceof ICancellableDataStore cancellable
                ? cancellable.beginCancellableOperation() : null;
    }

    private void latchUnsafeStopFailure(RuntimeException failure) {
        unsafeStopFailure.compareAndSet(null, failure);
    }

    private static RuntimeException asRuntimeFailure(Throwable failure, String message) {
        return failure instanceof RuntimeException runtime
                ? runtime : new IllegalStateException(message, failure);
    }

    private final class StatisticsWork {
        private ICancellableDataStore.CancellableOperation operation;
        private boolean operationAttached;
        private boolean cancellationRequested;
        private boolean cancellationComplete = true;
        private boolean cancellationStarted;
        private boolean workerComplete;
        private boolean scopeClosed;

        void attachOperation(ICancellableDataStore.CancellableOperation attachedOperation) {
            synchronized (workMonitor) {
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
            synchronized (workMonitor) {
                cancellationRequested = true;
                if (operationAttached) {
                    if (operation != null)
                        startCancellationLocked();
                    else
                        cancellationComplete = true;
                } else {
                    cancellationComplete = false;
                }
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
                    latchUnsafeStopFailure(asRuntimeFailure(error,
                            "Datastore cancellation failed"));
                } finally {
                    synchronized (workMonitor) {
                        cancellationComplete = true;
                        removeIfCompleteLocked();
                    }
                }
            }, "OSCAR-statistics-cancel");
            cancellationThread.setDaemon(true);
            cancellationThread.start();
        }

        void workerFinished(boolean closedCleanly) {
            synchronized (workMonitor) {
                workerComplete = true;
                scopeClosed = closedCleanly;
                removeIfCompleteLocked();
            }
        }

        private void removeIfCompleteLocked() {
            if (workerComplete && scopeClosed
                    && (!cancellationRequested || cancellationComplete)) {
                activeWork.remove(this);
                workMonitor.notifyAll();
            }
        }
    }

    private Statistics getLastObservationCount(DataBlock dataBlock, int index) {
        return new Statistics.Builder()
                .numOccupancies(dataBlock.getLongValue(index + 1))
                .numGammaAlarms(dataBlock.getLongValue(index + 2))
                .numNeutronAlarms(dataBlock.getLongValue(index + 3))
                .numGammaNeutronAlarms(dataBlock.getLongValue(index + 4))
                .numFaults(dataBlock.getLongValue(index + 5))
                .numGammaFaults(dataBlock.getLongValue(index + 6))
                .numNeutronFaults(dataBlock.getLongValue(index + 7))
                .numTampers(dataBlock.getLongValue(index + 8))
                .build();
    }

    @Override
    public DataComponent getRecordDescription() {
        return recordDescription;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return recommendedEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return samplingPeriod * 60;
    }
}
