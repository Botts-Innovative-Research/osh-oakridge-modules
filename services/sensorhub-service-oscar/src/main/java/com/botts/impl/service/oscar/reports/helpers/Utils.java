package com.botts.impl.service.oscar.reports.helpers;

import com.botts.impl.service.oscar.OSCARServiceModule;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;

import java.util.stream.Stream;

import static com.botts.impl.service.oscar.reports.helpers.Constants.DEF_ALARM;
import static com.botts.impl.service.oscar.reports.helpers.Constants.DEF_NEUTRON;

public class Utils {

    public static double calcEMLAlarmRate(double emlSuppCount, double alarmCount){
        if (emlSuppCount + alarmCount == 0) return 0.0;

        return emlSuppCount / (emlSuppCount + alarmCount);
    }

    public static double calculateAlarmingOccRate(double alarmCount, double occupancyCount){
        if(occupancyCount == 0) return 0.0;

        return alarmCount / occupancyCount;
    }

    public static Object queryDatabase(String observedProperties, OSCARServiceModule module){
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(observedProperties)
                        .build())
                .build());
    }

}
