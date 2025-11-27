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
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.utils.Async;
import org.vast.data.TextEncodingImpl;
import org.vast.util.TimeExtent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class StatisticsOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "siteStatistics";

    private final DataComponent recordDescription;
    private final DataEncoding recommendedEncoding;
    private static final int SAMPLING_PERIOD = 60;

    private ScheduledExecutorService service;
    private final IObsSystemDatabase database;

    private static class TimeRanges {
        final Instant monthStart, monthEnd;
        final Instant weekStart, weekEnd;
        final Instant dayStart, dayEnd;

        TimeRanges(Instant monthStart, Instant monthEnd,
                   Instant weekStart, Instant weekEnd,
                   Instant dayStart, Instant dayEnd) {
            this.monthStart = monthStart;
            this.monthEnd = monthEnd;
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
            this.dayStart = dayStart;
            this.dayEnd = dayEnd;
        }
    }

    private static class AllStatistics {
        final Statistics all;
        final Statistics monthly;
        final Statistics weekly;
        final Statistics daily;

        AllStatistics(Statistics all, Statistics monthly,
                      Statistics weekly, Statistics daily) {
            this.all = all;
            this.monthly = monthly;
            this.weekly = weekly;
            this.daily = daily;
        }
    }

    /*
    Use cases:

    - Go to national view page, see current total, monthly, weekly, daily
    - Request for specific time range (control)
    - Request for fresh data (control)
     */

    public StatisticsOutput(OSCARSystem parentSensor, IObsSystemDatabase database) {
        super(NAME, parentSensor);
        this.database = database;
        RADHelper fac = new RADHelper();
        recordDescription = fac.createSiteStatistics();
        recommendedEncoding = new TextEncodingImpl();
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
        long currentTime = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ?
                recordDescription.createDataBlock() : latestRecord.renew();

        int i = 0;
        dataBlock.setLongValue(i++, currentTime / 1000);

        // Calculate time ranges
        TimeRanges timeRanges = calculateTimeRanges();

        // Fetch ALL observations ONCE and compute statistics for all time periods
        AllStatistics allStats = computeAllStatistics(timeRanges);

        // Populate data block with all time periods
        i = populateDataBlock(dataBlock, i, allStats.all);
        i = populateDataBlock(dataBlock, i, allStats.monthly);
        i = populateDataBlock(dataBlock, i, allStats.weekly);
        populateDataBlock(dataBlock, i, allStats.daily);

        latestRecord = dataBlock;
        latestRecordTime = currentTime;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }

    /**
     * Calculate all time ranges once
     */
    private TimeRanges calculateTimeRanges() {
        Calendar cal = Calendar.getInstance();
        Instant now = Instant.now();

        // Monthly range (start of month to end of month)
        int currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        Instant monthStart = now.minus(currentDayOfMonth - 1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS);
        int lastDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Instant monthEnd = now.plus(lastDayOfMonth - currentDayOfMonth + 1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS);

        // Weekly range (start of week to end of week)
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        Instant weekStart = now.minus(currentDayOfWeek - 1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS);
        int lastDayOfWeek = cal.getActualMaximum(Calendar.DAY_OF_WEEK);
        Instant weekEnd = now.plus(lastDayOfWeek - currentDayOfWeek + 1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS);

        // Daily range (start of today to end of today)
        Instant dayStart = now.truncatedTo(ChronoUnit.DAYS);
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

        return new TimeRanges(monthStart, monthEnd, weekStart, weekEnd, dayStart, dayEnd);
    }

    /**
     * Fetch all observations once and compute statistics for all time ranges
     */
    private AllStatistics computeAllStatistics(TimeRanges timeRanges) {
        List<IObsData> occupancyObs = fetchObservations(RADHelper.DEF_OCCUPANCY);

        List<IObsData> gammaObs = fetchObservations(RADHelper.DEF_GAMMA);

        List<IObsData> neutronObs = fetchObservations(RADHelper.DEF_NEUTRON);

        List<IObsData> alarmObs = fetchObservations(RADHelper.DEF_ALARM);

        List<IObsData> tamperObs = fetchObservations(RADHelper.DEF_TAMPER);

        // Compute statistics for all time periods
        Statistics allTime = computeStatistics(
                occupancyObs, gammaObs, neutronObs, alarmObs, tamperObs,
                null, null);

        Statistics monthly = computeStatistics(
                occupancyObs, gammaObs, neutronObs, alarmObs, tamperObs,
                timeRanges.monthStart, timeRanges.monthEnd);

        Statistics weekly = computeStatistics(
                occupancyObs, gammaObs, neutronObs, alarmObs, tamperObs,
                timeRanges.weekStart, timeRanges.weekEnd);

        Statistics daily = computeStatistics(
                occupancyObs, gammaObs, neutronObs, alarmObs, tamperObs,
                timeRanges.dayStart, timeRanges.dayEnd);

        return new AllStatistics(allTime, monthly, weekly, daily);
    }

    protected List<IObsData> fetchObservations(String observedProperty, Instant start, Instant end) {
        ObsFilter.Builder filterBuilder = new ObsFilter.Builder()
                .withSystems().withUniqueIDs(parentSensor.getUniqueIdentifier()).done()
                .withDataStreams().withObservedProperties(observedProperty).done();

        if (start != null && end != null)
            filterBuilder.withPhenomenonTimeDuring(start, end);

        ObsFilter filter = filterBuilder.build();

        List<IObsData> observations = new ArrayList<>();
        try (Stream<IObsData> stream = database.getObservationStore().select(filter)) {
            stream.forEach(observations::add);
        }

        return observations;
    }

    private List<IObsData> fetchObservations(String observedProperty) {
        return fetchObservations(observedProperty, null, null);
    }

    protected Statistics computeStatistics(
            List<IObsData> occupancyObs,
            List<IObsData> gammaObs,
            List<IObsData> neutronObs,
            List<IObsData> alarmObs,
            List<IObsData> tamperObs,
            Instant start,
            Instant end) {

        java.util.function.Predicate<Instant> inRange = time -> {
            if (start == null && end == null) return true;
            if (start != null && time.isBefore(start)) return false;
            if (end != null && time.isAfter(end)) return false;
            return true;
        };

        // Count occupancies (all within range)
        long numOccupancies = occupancyObs.stream()
                .filter(obs -> inRange.test(obs.getPhenomenonTime()))
                .count();

        // Count alarms by type
        long numGammaAlarms = occupancyObs.stream()
                .filter(obs -> inRange.test(obs.getPhenomenonTime()))
                .filter(obs -> obs.getResult().getBooleanValue(5) && !obs.getResult().getBooleanValue(6))
                .count();

        long numNeutronAlarms = occupancyObs.stream()
                .filter(obs -> inRange.test(obs.getPhenomenonTime()))
                .filter(obs -> !obs.getResult().getBooleanValue(5) && obs.getResult().getBooleanValue(6))
                .count();

        long numGammaNeutronAlarms = occupancyObs.stream()
                .filter(obs -> inRange.test(obs.getPhenomenonTime()))
                .filter(obs -> obs.getResult().getBooleanValue(5) && obs.getResult().getBooleanValue(6))
                .count();

        // Count faults
        long numGammaFaults = Stream.concat(gammaObs.stream(), alarmObs.stream())
                .filter(obs -> inRange.test(obs.getPhenomenonTime()))
                .filter(obs -> obs.getResult().getStringValue(1).contains("Fault - Gamma"))
                .count();

        long numNeutronFaults = Stream.concat(neutronObs.stream(), alarmObs.stream())
                .filter(obs -> inRange.test(obs.getPhenomenonTime()))
                .filter(obs -> obs.getResult().getStringValue(1).contains("Fault - Neutron"))
                .count();

        // Count tampers
        long numTampers = tamperObs.stream()
                .filter(obs -> inRange.test(obs.getPhenomenonTime()))
                .filter(obs -> obs.getResult().getBooleanValue(1))
                .count();

        // Total faults
        long numFaults = Stream.of(gammaObs.stream(), neutronObs.stream(), alarmObs.stream())
                .flatMap(s -> s)
                .filter(obs -> inRange.test(obs.getPhenomenonTime()))
                .filter(obs -> obs.getResult().getStringValue(1).contains("Fault"))
                .count() + numTampers;

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

    protected int populateDataBlock(DataBlock dataBlock, int i, Statistics stats) {
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
        return SAMPLING_PERIOD*SAMPLING_PERIOD;
    }
}
