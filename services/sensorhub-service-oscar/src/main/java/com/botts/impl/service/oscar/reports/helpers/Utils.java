package com.botts.impl.service.oscar.reports.helpers;

import com.botts.impl.service.oscar.OSCARServiceModule;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;

import java.time.Instant;
import java.util.function.Predicate;


public class Utils {

    public static long calcEMLAlarmRate(long emlSuppCount, long alarmCount){
        if (emlSuppCount + alarmCount == 0) return 0;

        return emlSuppCount / (emlSuppCount + alarmCount);
    }

    public static long calculateAlarmingOccRate(long alarmCount, long occupancyCount){
        if(occupancyCount == 0) return 0;

        return alarmCount / occupancyCount;
    }

    public static long countObservations(String[] observedProperties, OSCARServiceModule module, Predicate<IObsData> valuePredicate, Instant begin, Instant end){
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(observedProperties)
                        .withValidTimeDuring(begin, end)
                        .build())
                        .withValuePredicate(valuePredicate)
                .build()).count();
    }

    public static long countObservationsFromLane(String laneUID, String[] observedProperties, OSCARServiceModule module, Predicate<IObsData> valuePredicate,Instant begin, Instant end){
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withSystems(new SystemFilter.Builder()
                        .withUniqueIDs(laneUID)
                        .build())
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(observedProperties)
                        .withValidTimeDuring(begin, end)
                        .build())
                .withValuePredicate(valuePredicate)
                .build())
                .count();
    }

}
