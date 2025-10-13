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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class StatisticsOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "siteStatistics";

    private final DataComponent recordDescription;
    private final DataEncoding recommendedEncoding;
    private static final int SAMPLING_PERIOD = 60;

    private ScheduledExecutorService service;
    private final IObsSystemDatabase database;

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

    public synchronized void start() {
        if (service == null || service.isShutdown())
            service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this::publishLatestStatistics, 0L, SAMPLING_PERIOD, TimeUnit.MINUTES);
    }

    public synchronized void stop() {
        service.shutdown();
        service = null;
    }

    public synchronized void publishLatestStatistics() {
        long currentTime = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? recordDescription.createDataBlock() : latestRecord.renew();

        int i = 0;
        dataBlock.setLongValue(i++, currentTime/1000);

        // Get total counts from DB
        i = populateDataBlock(dataBlock, i, null, null);

        // Get monthly counts from DB
        // Modify start and end time for current month-to-date
        int currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        var monthStart = Instant.now().minus(currentDayOfMonth-1, TimeUnit.DAYS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);
        var lastDayOfMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        var monthEnd = Instant.now().plus(lastDayOfMonth-currentDayOfMonth+1, TimeUnit.DAYS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);

        i = populateDataBlock(dataBlock, i, monthStart, monthEnd);

        // Get weekly counts from DB
        int currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        var weekStart = Instant.now().minus(currentDayOfWeek-1, TimeUnit.DAYS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);
        var lastDayOfWeek = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_WEEK);
        var weekEnd = Instant.now().plus(lastDayOfWeek-currentDayOfWeek+1, TimeUnit.DAYS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);

        i = populateDataBlock(dataBlock, i, weekStart, weekEnd);

        // Get today's counts from DB
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        var dayStart = Instant.now().minus(currentHour-1, TimeUnit.HOURS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);
        var lastHour = Calendar.getInstance().getActualMaximum(Calendar.HOUR_OF_DAY);
        var dayEnd = Instant.now().plus(lastHour-currentHour+1, TimeUnit.HOURS.toChronoUnit()).truncatedTo(ChronoUnit.DAYS);

        populateDataBlock(dataBlock, i, dayStart, dayEnd);

        latestRecord = dataBlock;
        latestRecordTime = currentTime;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
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

    protected int populateDataBlock(DataBlock dataBlock, int i, Instant start, Instant end) {
        Statistics stats = getStats(start, end);
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

    protected Statistics getStats(Instant start, Instant end) {
        Predicate<IObsData> occupancyPredicate = (obs) -> true;
        long numOccupancies = countObservations(database, occupancyPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        Predicate<IObsData> gammaAlarmPredicate = (obs) -> obs.getResult().getBooleanValue(5) && !obs.getResult().getBooleanValue(6);
        long numGammaAlarms = countObservations(database, gammaAlarmPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        Predicate<IObsData> neutronAlarmPredicate = (obs) -> !obs.getResult().getBooleanValue(5) && obs.getResult().getBooleanValue(6);
        long numNeutronAlarms = countObservations(database, neutronAlarmPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        Predicate<IObsData> gammaNeutronAlarmPredicate = (obs) -> obs.getResult().getBooleanValue(5) && obs.getResult().getBooleanValue(6);
        long numGammaNeutronAlarms = countObservations(database, gammaNeutronAlarmPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        Predicate<IObsData> gammaFaultPredicate = (obs) -> obs.getResult().getStringValue(1).contains("Fault - Gamma");
        long numGammaFaults = countObservations(database, gammaFaultPredicate, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);

        Predicate<IObsData> neutronFaultPredicate = (obs) -> obs.getResult().getStringValue(1).contains("Fault - Neutron");
        long numNeutronFaults = countObservations(database, neutronFaultPredicate, start, end, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);

        Predicate<IObsData> tamperPredicate = (obs) -> obs.getResult().getBooleanValue(1);
        long numTampers = countObservations(database, tamperPredicate, start, end, RADHelper.DEF_TAMPER);

        Predicate<IObsData> faultPredicate = (obs) -> obs.getResult().getStringValue(1).contains("Fault");
        long numFaults = countObservations(database, faultPredicate, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM) + numTampers;

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

    private long countObservations(IObsSystemDatabase database, Predicate<IObsData> valuePredicate, Instant begin, Instant end, String... observedProperty){
        var dsFilterBuilder = new DataStreamFilter.Builder()
                .withObservedProperties(observedProperty);

        if (begin != null && end != null)
            dsFilterBuilder = dsFilterBuilder.withValidTimeDuring(begin, end);

        var dsFilter = dsFilterBuilder.build();

        return database.getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(dsFilter)
                .withValuePredicate(valuePredicate)
                .build())
                .count();
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
