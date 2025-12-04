package com.botts.impl.service.oscar.stats;

import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.api.resource.ResourceKey;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.utils.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class StatisticsOutput extends AbstractSensorOutput<OSCARSystem> {

    Logger logger = LoggerFactory.getLogger(StatisticsOutput.class);

    public static final String NAME = "siteStatistics";

    public static String gammaNeutronAlarmCQL = "gammaAlarm = true AND neutronAlarm = true";
    public static String gammaAlarmCQL = "gammaAlarm = true AND neutronAlarm = false";
    public static String neutronAlarmCQL = "gammaAlarm = false AND neutronAlarm = true";
    public static String faultCQL = "alarmState = 'Fault - Neutron High' OR alarmState = 'Fault - Gamma Low' OR alarmState = 'Fault - Gamma High'";
    public static String gammaFaultCQL = "alarmState = 'Fault - Gamma Low' OR alarmState = 'Fault - Gamma High'";
    public static String gammaHighFaultCQL = "alarmState = 'Fault - Gamma High'";
    public static String gammaLowFaultCQL = "alarmState = 'Fault - Gamma Low'";
    public static String neutronFaultCQL = "alarmState = 'Fault - Neutron High'";
    public static String tamperCQL = "tamperStatus = true";

    private final DataComponent recordDescription;
    private final DataEncoding recommendedEncoding;
    private static final int SAMPLING_PERIOD = 60;

    private ScheduledExecutorService service;
    private final IObsSystemDatabase database;
    private Map<String, Set<BigId>> observedPropertyToDataStreamIds;

    public StatisticsOutput(OSCARSystem parentSensor, IObsSystemDatabase database) {
        super(NAME, parentSensor);
        this.database = database;
        RADHelper fac = new RADHelper();
        recordDescription = fac.createSiteStatistics();
        recommendedEncoding = new TextEncodingImpl();
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
                RADHelper.DEF_TAMPER
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
        service.scheduleAtFixedRate(this::publishLatestStatistics, 0L, SAMPLING_PERIOD, TimeUnit.MINUTES);
    }

    public void stop() {
        service.shutdown();
        service = null;
    }

    public void publishLatestStatistics() {
        initializeDataStreamMapping();
        logger.info("Starting stats counting...");
        long currentTime = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? recordDescription.createDataBlock() : latestRecord.renew();

        int i = 0;
        dataBlock.setLongValue(i++, currentTime/1000);

        // Get total counts from all observations
        i = populateDataBlock(dataBlock, i, null, null);

        // Get monthly counts
        int currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        var monthStart = Instant.now().minus(currentDayOfMonth-1, TimeUnit.DAYS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);
        var lastDayOfMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        var monthEnd = Instant.now().plus(lastDayOfMonth-currentDayOfMonth+1, TimeUnit.DAYS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);

        i = populateDataBlock(dataBlock, i, monthStart, monthEnd);

        // Get weekly counts
        int currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        var weekStart = Instant.now().minus(currentDayOfWeek-1, TimeUnit.DAYS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);
        var lastDayOfWeek = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_WEEK);
        var weekEnd = Instant.now().plus(lastDayOfWeek-currentDayOfWeek+1, TimeUnit.DAYS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);

        i = populateDataBlock(dataBlock, i, weekStart, weekEnd);

        // Get today's counts
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        var dayStart = Instant.now().minus(currentHour-1, TimeUnit.HOURS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);
        var lastHour = Calendar.getInstance().getActualMaximum(Calendar.HOUR_OF_DAY);
        var dayEnd = Instant.now().plus(lastHour-currentHour+1, TimeUnit.HOURS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);

        populateDataBlock(dataBlock, i, dayStart, dayEnd);

        latestRecord = dataBlock;
        latestRecordTime = currentTime;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
        logger.info("Finished stats counting in {}s", (System.currentTimeMillis() - currentTime) / 1000);
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
     * Populates the data block with statistics for the given time range.
     */
    public int populateDataBlock(DataBlock dataBlock, int i, Instant start, Instant end) {
        Statistics stats = getStats(start, end);
        return populateDataBlockWithStats(dataBlock, i, stats);
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
        long numOccupancies = countObservations(database, null, start, end, RADHelper.DEF_OCCUPANCY);
        long numGammaAlarms = countObservations(database, gammaAlarmCQL, start, end, RADHelper.DEF_OCCUPANCY);
        long numNeutronAlarms = countObservations(database, neutronAlarmCQL, start, end, RADHelper.DEF_OCCUPANCY);
        long numGammaNeutronAlarms = countObservations(database, gammaNeutronAlarmCQL, start, end, RADHelper.DEF_OCCUPANCY);
        long numGammaFaults = countObservations(database, gammaFaultCQL, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        long numNeutronFaults = countObservations(database, neutronFaultCQL, start, end, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);
        long numTampers = countObservations(database, tamperCQL, start, end, RADHelper.DEF_TAMPER);
        long numFaults = countObservations(database, faultCQL, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM) + numTampers;

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
    private long countObservations(IObsSystemDatabase database, String cqlValue, Instant begin, Instant end, String... observedProperty) {
        var dsFilterBuilder = new DataStreamFilter.Builder()
                .withObservedProperties(observedProperty);

        if (begin != null && end != null)
            dsFilterBuilder = dsFilterBuilder.withValidTimeDuring(begin, end);

        var dsFilter = dsFilterBuilder.build();

        var obsBuilder = new ObsFilter.Builder().withDataStreams(dsFilter);

        if (cqlValue != null &&  !cqlValue.isBlank()) {
            obsBuilder.withCQLFilter(cqlValue);
        }

        return database.getObservationStore().countMatchingEntries(obsBuilder.build());
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
        return SAMPLING_PERIOD * SAMPLING_PERIOD;
    }
}