package com.botts.impl.service.oscar.stats;

import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class StatisticsOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "siteStatistics";

    // Mirrors LaneSystem.LANE_SYSTEM_PREFIX (kept private there). All lane
    // systems registered via the lane module use this UID prefix, so a trailing
    // '*' wildcard in withUniqueIDs() selects every lane on the hub.
    public static final String LANE_UID_PREFIX = "urn:osh:system:lane:";

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

    private ScheduledExecutorService service;
    private final IObsSystemDatabase database;
    private final IdEncoder obsIdEncoder;
    private Map<String, Set<BigId>> observedPropertyToDataStreamIds;

    private record LastStatisticObservationData(Instant timestamp, Statistics total) {

    }

    public StatisticsOutput(OSCARSystem parentSensor, IObsSystemDatabase database, IdEncoder obsIdEncoder, int samplingPeriod) {
        super(NAME, parentSensor);
        this.database = database;
        this.obsIdEncoder = obsIdEncoder;
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
        if (service == null || service.isShutdown())
            service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this::publishLatestStatistics, 0L, samplingPeriod, TimeUnit.MINUTES);
    }

    public void stop() {
        if (service != null) {
            service.shutdown();
            service = null;
        }
    }

    public void publishLatestStatistics() {
        try {
            publishLatestStatisticsInternal();
        } catch (Throwable t) {
            // scheduleAtFixedRate silently suppresses exceptions and stops
            // re-scheduling — surface them so they don't disappear.
            getLogger().error("publishLatestStatistics failed", t);
            throw t;
        }
    }

    private void publishLatestStatisticsInternal() {
        initializeDataStreamMapping();
        getLogger().info("Starting stats counting...");
        long currentTime = System.currentTimeMillis();
        Instant now = Instant.now();

        // Compute all four buckets up front so we know each per-lane array
        // size before allocating the data block (variable-size SWE arrays
        // can't grow once createDataBlock() returns).
        Statistics allTimeStats;
        LastStatisticObservationData lastObs = getLastObservation();
        if (lastObs != null) {
            // Preserve the existing all-time incremental optimization for the
            // node-wide counts; recompute the per-lane breakdown fresh so it
            // reflects current lane membership.
            Statistics incrementalStats = getStats(lastObs.timestamp().plusNanos(1), now);
            Statistics newTotals = lastObs.total().add(incrementalStats);
            List<Statistics.LaneStatistics> freshPerLane = getStats(Instant.EPOCH, now).getPerLane();
            allTimeStats = new Statistics.Builder()
                    .numOccupancies(newTotals.getNumOccupancies())
                    .numGammaAlarms(newTotals.getNumGammaAlarms())
                    .numNeutronAlarms(newTotals.getNumNeutronAlarms())
                    .numGammaNeutronAlarms(newTotals.getNumGammaNeutronAlarms())
                    .numFaults(newTotals.getNumFaults())
                    .numGammaFaults(newTotals.getNumGammaFaults())
                    .numNeutronFaults(newTotals.getNumNeutronFaults())
                    .numTampers(newTotals.getNumTampers())
                    .perLane(freshPerLane)
                    .build();
        } else {
            getLogger().debug("No previous stats observation found, calculating from epoch to {}", now);
            allTimeStats = getStats(Instant.EPOCH, now);
        }

        Statistics monthlyStats = getStats(now.minus(30, ChronoUnit.DAYS), now);
        Statistics weeklyStats  = getStats(now.minus(7,  ChronoUnit.DAYS), now);
        Statistics dailyStats   = getStats(now.minus(24, ChronoUnit.HOURS), now);

        // Resize each bucket's "byLane" array to match the lanes we found.
        sizeBucketArray("allTime", allTimeStats.getPerLane().size());
        sizeBucketArray("monthly", monthlyStats.getPerLane().size());
        sizeBucketArray("weekly",  weeklyStats.getPerLane().size());
        sizeBucketArray("daily",   dailyStats.getPerLane().size());

        // Always allocate fresh; latestRecord.renew() can't grow var-size arrays.
        // createDataBlock() honours the per-lane sizes we just set above, so we
        // skip the recursive setData() consistency check (which compares each
        // array's count-component value against its allocated atom count — that
        // value is still 0 in the freshly-allocated block until we write it
        // below).
        DataBlock dataBlock = recordDescription.createDataBlock();

        int i = 0;
        dataBlock.setLongValue(i++, currentTime / 1000);
        i = populateBucket(dataBlock, i, allTimeStats);
        i = populateBucket(dataBlock, i, monthlyStats);
        i = populateBucket(dataBlock, i, weeklyStats);
        populateBucket(dataBlock, i, dailyStats);

        latestRecord = dataBlock;
        latestRecordTime = currentTime;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
        getLogger().info("Finished stats counting in {}s", (System.currentTimeMillis() - currentTime) / 1000);
    }

    protected BigId waitForLatestObservationId() {
        try {
            final List<BigId> result = new ArrayList<>();

            Async.waitForCondition(() -> {
                var keysQuery = database.getObservationStore().selectKeys(new ObsFilter.Builder()
                        .withLatestResult()
                        .withSystems().withUniqueIDs(parentSensor.getUniqueIdentifier())
                        .done()
                        .withDataStreams().withOutputNames(NAME)
                        .done()
                        .withLimit(1)
                        .build());
                var keys = keysQuery.toList();
                if (keys.size() == 1) {
                    result.clear();
                    result.add(keys.get(0));
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
     * Populates a single bucket sub-record (allTime/monthly/weekly/daily)
     * starting at flat atom index {@code i}. Used by {@link StatisticsControl}
     * to write custom-range responses into a standalone bucket data block.
     */
    public int populateBucket(DataBlock dataBlock, int i, Statistics stats) {
        // 8 base counts (must match the order in RADHelper.createCountStatistics)
        dataBlock.setLongValue(i++, stats.getNumOccupancies());
        dataBlock.setLongValue(i++, stats.getNumGammaAlarms());
        dataBlock.setLongValue(i++, stats.getNumNeutronAlarms());
        dataBlock.setLongValue(i++, stats.getNumGammaNeutronAlarms());
        dataBlock.setLongValue(i++, stats.getNumFaults());
        dataBlock.setLongValue(i++, stats.getNumGammaFaults());
        dataBlock.setLongValue(i++, stats.getNumNeutronFaults());
        dataBlock.setLongValue(i++, stats.getNumTampers());

        // numLanes + byLane array
        var perLane = stats.getPerLane();
        dataBlock.setIntValue(i++, perLane.size());
        for (var lane : perLane) {
            dataBlock.setStringValue(i++, lane.getLaneUID());
            dataBlock.setLongValue(i++, lane.getNumOccupancies());
            dataBlock.setLongValue(i++, lane.getNumGammaAlarms());
            dataBlock.setLongValue(i++, lane.getNumNeutronAlarms());
            dataBlock.setLongValue(i++, lane.getNumGammaNeutronAlarms());
            dataBlock.setLongValue(i++, lane.getNumFaults());
            dataBlock.setLongValue(i++, lane.getNumGammaFaults());
            dataBlock.setLongValue(i++, lane.getNumNeutronFaults());
            dataBlock.setLongValue(i++, lane.getNumTampers());
            dataBlock.setLongValue(i++, lane.getNumAdjudicated());
            dataBlock.setDoubleValue(i++, lane.getAvgTimeToAdjudicateSec());
        }
        return i;
    }

    /**
     * Convenience entry used by {@link StatisticsControl}: compute stats for
     * the given window and populate into the provided bucket data block.
     */
    public int populateBucket(DataBlock dataBlock, int i, Instant start, Instant end) {
        return populateBucket(dataBlock, i, getStats(start, end));
    }

    /**
     * Resizes one bucket's per-lane SWE array so {@link #recordDescription}
     * .createDataBlock() allocates the right number of trailing atoms.
     */
    public void sizeBucketArray(String bucketName, int numLanes) {
        ((DataArray) recordDescription.getComponent(bucketName).getComponent("byLane")).updateSize(numLanes);
    }

    /**
     * Public entry point for one-off range queries (used by
     * {@link StatisticsControl}'s custom-range command).
     */
    public Statistics getStatsForRange(Instant start, Instant end) {
        return getStats(start, end);
    }

    /**
     * Gets statistics by fetching from database
     */
    protected Statistics getStats(Instant start, Instant end) {
        long numOccupancies = countObservations(database, null, start, end, RADHelper.DEF_OCCUPANCY);
        long numGammaAlarms = countObservations(database, gammaAlarmCQL, start, end, RADHelper.DEF_OCCUPANCY);
        long numNeutronAlarms = countObservations(database, neutronAlarmCQL, start, end, RADHelper.DEF_OCCUPANCY);
        long numGammaNeutronAlarms = countObservations(database, gammaNeutronAlarmCQL, start, end, RADHelper.DEF_OCCUPANCY);
        long numGammaFaults = countObservations(database, gammaFaultCQL, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        long numNeutronFaults = countObservations(database, neutronFaultCQL, start, end, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);
        long numTampers = countObservations(database, tamperCQL, start, end, RADHelper.DEF_TAMPER);
        long numFaults = countObservations(database, faultCQL, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM) + numTampers;

        // RS350 alarm counts from raw AlarmOutput (uses alarmCategoryCode observed property)
        long numRS350GammaAlarms = countObservations(database, rs350GammaAlarmCQL, start, end, DEF_ALARM_CATEGORY);
        long numRS350NeutronAlarms = countObservations(database, rs350NeutronAlarmCQL, start, end, DEF_ALARM_CATEGORY);
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
                .perLane(getPerLaneStats(start, end))
                .build();
    }

    /**
     * Builds one {@link Statistics.LaneStatistics} per registered lane system.
     * Returns an empty list when no lane systems are present (e.g. during
     * fresh test bring-up before any LaneSystem is loaded).
     */
    private List<Statistics.LaneStatistics> getPerLaneStats(Instant start, Instant end) {
        // Lane systems are stored as multiple validity-time versions of the same
        // UID — select(...) returns one per version, so dedupe on UID.
        var laneUIDs = database.getSystemDescStore()
                .select(new SystemFilter.Builder()
                        .withUniqueIDs(LANE_UID_PREFIX + "*")
                        .build())
                .map(sys -> sys.getUniqueIdentifier())
                .distinct()
                .collect(Collectors.toList());

        var result = new ArrayList<Statistics.LaneStatistics>(laneUIDs.size());
        for (String laneUID : laneUIDs) {
            long occ           = countLaneObservations(laneUID, null, start, end, RADHelper.DEF_OCCUPANCY);
            long gAlarm        = countLaneObservations(laneUID, gammaAlarmCQL, start, end, RADHelper.DEF_OCCUPANCY);
            long nAlarm        = countLaneObservations(laneUID, neutronAlarmCQL, start, end, RADHelper.DEF_OCCUPANCY);
            long gnAlarm       = countLaneObservations(laneUID, gammaNeutronAlarmCQL, start, end, RADHelper.DEF_OCCUPANCY);
            long gFault        = countLaneObservations(laneUID, gammaFaultCQL, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
            long nFault        = countLaneObservations(laneUID, neutronFaultCQL, start, end, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);
            long tampers       = countLaneObservations(laneUID, tamperCQL, start, end, RADHelper.DEF_TAMPER);
            long faults        = countLaneObservations(laneUID, faultCQL, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM) + tampers;

            // RS350 alarm counts roll into the same lane buckets as the
            // node-level totals do above.
            long rs350Gamma   = countLaneObservations(laneUID, rs350GammaAlarmCQL, start, end, DEF_ALARM_CATEGORY);
            long rs350Neutron = countLaneObservations(laneUID, rs350NeutronAlarmCQL, start, end, DEF_ALARM_CATEGORY);
            gAlarm += rs350Gamma;
            nAlarm += rs350Neutron;
            occ    += rs350Gamma + rs350Neutron;

            var adj = computeAdjudicationMetrics(laneUID, start, end);

            result.add(new Statistics.LaneStatistics.Builder()
                    .laneUID(laneUID)
                    .numOccupancies(occ)
                    .numGammaAlarms(gAlarm)
                    .numNeutronAlarms(nAlarm)
                    .numGammaNeutronAlarms(gnAlarm)
                    .numFaults(faults)
                    .numGammaFaults(gFault)
                    .numNeutronFaults(nFault)
                    .numTampers(tampers)
                    .numAdjudicated(adj.count)
                    .avgTimeToAdjudicateSec(adj.avgSeconds)
                    .build());
        }
        return result;
    }

    private record AdjudicationMetrics(long count, double avgSeconds) {}

    /**
     * Walks the lane's COMPLETED adjudication command statuses in the window
     * and returns the count plus the mean time between the linked occupancy's
     * phenomenon time and the adjudication's report time (seconds).
     * Statuses whose linked occupancy can't be resolved are skipped from the
     * average but still counted (matches what AdjudicationLog shows the user).
     */
    private AdjudicationMetrics computeAdjudicationMetrics(String laneUID, Instant start, Instant end) {
        var statusStore = database.getCommandStatusStore();
        var iter = statusStore.select(new CommandStatusFilter.Builder()
                .withStatus(ICommandStatus.CommandStatusCode.COMPLETED)
                .withReportTimeDuring(start, end)
                .withCommands(new CommandFilter.Builder()
                        .withSystems().withUniqueIDs(laneUID).done()
                        .build())
                .build()).iterator();

        long count = 0;
        double sumSeconds = 0.0;
        long timedCount = 0;
        while (iter.hasNext()) {
            var status = iter.next();
            count++;

            try {
                var records = status.getResult().getInlineRecords().stream().toList();
                if (records.isEmpty())
                    continue;
                var record = records.get(0);

                // Adjudication result layout (see AdjudicationControl.createResultData):
                //   0: username, 1: feedback, 2: code, 3: isotopeCount,
                //   4..N: isotopes, N+1: secondaryInspection, N+2: filePathCount,
                //   N+3..M: filePaths, M+1: occupancyObsId, M+2: vehicleId
                int idx = 3;
                int isotopeCount = record.getIntValue(idx++);
                idx += isotopeCount;       // skip isotopes
                idx++;                     // skip secondaryInspectionStatus
                int filePathCount = record.getIntValue(idx++);
                idx += filePathCount;      // skip file paths
                String occupancyObsId = record.getStringValue(idx);

                if (occupancyObsId == null || occupancyObsId.isBlank())
                    continue;

                BigId decoded = obsIdEncoder.decodeID(occupancyObsId);
                if (decoded == null)
                    continue;

                var occupancyObs = database.getObservationStore().get(decoded);
                if (occupancyObs == null)
                    continue;

                double seconds = (status.getReportTime().toEpochMilli()
                        - occupancyObs.getPhenomenonTime().toEpochMilli()) / 1000.0;
                if (seconds < 0)
                    continue;

                sumSeconds += seconds;
                timedCount++;
            } catch (Exception e) {
                getLogger().debug("Skipping adjudication latency entry for lane {}: {}", laneUID, e.getMessage());
            }
        }

        double avg = timedCount == 0 ? 0.0 : sumSeconds / timedCount;
        return new AdjudicationMetrics(count, avg);
    }

    /**
     * Counts observations from database (used by external callers like submitCommand).
     */
    private long countObservations(IObsSystemDatabase database, String cqlValue, Instant begin, Instant end, String... observedProperty) {
        var dsFilterBuilder = new DataStreamFilter.Builder()
                .withObservedProperties(observedProperty);

        var dsFilter = dsFilterBuilder.build();

        var obsBuilder = new ObsFilter.Builder().withDataStreams(dsFilter);

        if (begin != null && end != null) {
            obsBuilder.withPhenomenonTime().withRange(begin, end).done();
        }

        if (cqlValue != null &&  !cqlValue.isBlank()) {
            obsBuilder.withCQLFilter(cqlValue);
        }

        return database.getObservationStore().countMatchingEntries(obsBuilder.build());
    }

    /**
     * Lane-scoped version of {@link #countObservations}: restricts the
     * datastream filter to those owned by (or nested under) the given lane UID.
     * Mirrors reports/helpers/Utils#countObservationsFromLane.
     */
    private long countLaneObservations(String laneUID, String cqlValue, Instant begin, Instant end, String... observedProperty) {
        var dsFilter = new DataStreamFilter.Builder()
                .withSystems().withUniqueIDs(laneUID).includeMembers(true).done()
                .withObservedProperties(observedProperty)
                .build();

        var obsBuilder = new ObsFilter.Builder().withDataStreams(dsFilter);

        if (begin != null && end != null) {
            obsBuilder.withPhenomenonTime().withRange(begin, end).done();
        }

        if (cqlValue != null && !cqlValue.isBlank()) {
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

        long storedTimestampSeconds = result.getLongValue(0);
        Instant timestamp = Instant.ofEpochSecond(storedTimestampSeconds);

        Statistics allTime = getLastObservationCount(result, 0);

        return new LastStatisticObservationData(timestamp, allTime);
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
