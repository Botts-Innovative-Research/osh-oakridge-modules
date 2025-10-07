package com.botts.impl.service.oscar.stats;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class StatisticsOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "siteStatistics";
    public static final String LABEL = "Site Statistics";
    public static final String DESCRIPTION = "Statistics for this node's RPMs/lanes";

    private DataComponent recordDescription;
    private DataEncoding recommendedEncoding;
    private final int SAMPLING_PERIOD = 60;

    private final ScheduledExecutorService service;
    private final IObsSystemDatabase database;

    /*
    Use cases:

    - Go to national view page, see current total, monthly, weekly, daily
    - Request for specific time range
     */

    public StatisticsOutput(OSCARSystem parentSensor, IObsSystemDatabase database) {
        super(NAME, parentSensor);
        this.database = database;
        service = Executors.newSingleThreadScheduledExecutor();
        RADHelper fac = new RADHelper();
        recordDescription = fac.createSiteStatistics();
        recommendedEncoding = new TextEncodingImpl();
    }

    public void start() {
        service.scheduleAtFixedRate(this::calculateLatestStatistics, 0L, SAMPLING_PERIOD, TimeUnit.MINUTES);
    }

    public void stop() {
        service.shutdown();
    }

    public synchronized DataBlock calculateLatestStatistics() {
        return calculateStatistics(null, null);
    }

    public synchronized DataBlock calculateStatistics(Instant start, Instant end) {
        long currentTime = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? recordDescription.createDataBlock() : latestRecord.renew();

        if (start == null || end == null) {
            // TODO: Query for all time from db
        }

        int i = 0;
        dataBlock.setLongValue(i++, currentTime/1000);

        // TODO: Get total counts from DB
        Statistics totalStats = getStats(null, null);

        dataBlock.setLongValue(i++, totalOccupancies);
        dataBlock.setLongValue(i++, totalGammaAlarms);
        dataBlock.setLongValue(i++, totalNeutronAlarms);
        dataBlock.setLongValue(i++, totalFaults);
        dataBlock.setLongValue(i++, totalTampers);

        // TODO: Get monthly counts from DB
        // TODO: Modify start and end time for current month-to-date
        Statistics monthlyStats = getStats(start, end);

        dataBlock.setLongValue(i++, monthlyStats.getNumOccupancies());
        dataBlock.setLongValue(i++, monthlyStats.getNumGammaAlarms());
        dataBlock.setLongValue(i++, monthlyStats.getNumNeutronAlarms());
        dataBlock.setLongValue(i++, monthlyStats.getNumFaults());
        dataBlock.setLongValue(i++, monthlyStats.getNumTampers());

        // TODO: Get weekly counts from DB
        Statistics weeklyStats = getStats(start, end);

        dataBlock.setLongValue(i++, weeklyOccupancies);
        dataBlock.setLongValue(i++, weeklyGammaAlarms);
        dataBlock.setLongValue(i++, weeklyNeutronAlarms);
        dataBlock.setLongValue(i++, weeklyFaults);
        dataBlock.setLongValue(i++, weeklyTampers);

        // TODO: Get daily counts from DB
        int dailyOccupancies = 0;
        int dailyGammaAlarms = 0;
        int dailyNeutronAlarms = 0;
        int dailyFaults = 0;
        int dailyTampers = 0;

        dataBlock.setIntValue(i++, dailyOccupancies);
        dataBlock.setIntValue(i++, dailyGammaAlarms);
        dataBlock.setIntValue(i++, dailyNeutronAlarms);
        dataBlock.setIntValue(i++, dailyFaults);
        dataBlock.setIntValue(i++, dailyTampers);


        latestRecord = dataBlock;
        latestRecordTime = currentTime;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));

        return dataBlock;
    }

    private Statistics getStats(Instant start, Instant end) {
        Predicate<IObsData> occupancyPredicate = (obs) -> true;
        long numOccupancies = countObservations(database, occupancyPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        Predicate<IObsData> gammaAlarmPredicate = (obs) -> obs.getResult().getBooleanValue(5);
        long numGammaAlarms = countObservations(database, gammaAlarmPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        Predicate<IObsData> neutronAlarmPredicate = (obs) -> obs.getResult().getBooleanValue(6);
        long numNeutronAlarms = countObservations(database, neutronAlarmPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        Predicate<IObsData> faultPredicate = (obs) -> obs.getResult().getStringValue(1).contains("Fault");
        long numFaults = countObservations(database, faultPredicate, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);

        Predicate<IObsData> tamperPredicate = (obs) -> obs.getResult().getBooleanValue(1);
        long numTampers = countObservations(database, tamperPredicate, start, end, RADHelper.DEF_TAMPER);

        return new Statistics.Builder()
                .numOccupancies(numOccupancies)
                .numGammaAlarms(numGammaAlarms)
                .numNeutronAlarms(numNeutronAlarms)
                .numFaults(numFaults)
                .numTampers(numTampers)
                .build();
    }

    // TODO Change this to use Kalyn's stuff
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
        return SAMPLING_PERIOD;
    }
}
