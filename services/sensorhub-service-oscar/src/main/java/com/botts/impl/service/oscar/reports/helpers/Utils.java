package com.botts.impl.service.oscar.reports.helpers;

import com.botts.impl.service.oscar.OSCARServiceModule;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;


public class Utils {

    public static String gammaNeutronCql = "gammaAlarm = true AND neutronAlarm = true";
    public static String gammaCql = "gammaAlarm = true AND neutronAlarm = false";
    public static String neutronCql = "gammaAlarm = false AND neutronAlarm = true";
    public static String faultCql = "neutronAlarm = Fault - Neutron High OR gammaAlarm = Fault - Gamma Low OR gammaAlarm = Fault - Gamma High";
    public static String gammaFaultCql = "gammaAlarm = Fault - Gamma Low OR gammaAlarm = Fault - Gamma High";
    public static String gammaHighFaultCql = "gammaAlarm = Fault - Gamma High";
    public static String gammaLowFaultCql = "gammaAlarm = Fault - Gamma Low";
    public static String neutronFaultCql = "neutronAlarm = Fault - Neutron High";
    public static String tamperCql = "tamperStatus = true";

    public static String emlSuppressedCql = "result = RELEASE";


    public static long calcEMLAlarmRate(long emlSuppCount, long alarmCount){
        if (emlSuppCount + alarmCount == 0) return 0;

        return emlSuppCount / (emlSuppCount + alarmCount);
    }

    public static long calculateAlarmingOccRate(long alarmCount, long occupancyCount){
        if(occupancyCount == 0) return 0;

        return alarmCount / occupancyCount;
    }

    public static long countObservations(OSCARServiceModule module, String cqlValue, Instant begin, Instant end, String... observedProperties){

        ObsFilter.Builder builder = new ObsFilter.Builder()
                .withResultTimeDuring(begin, end)
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(observedProperties)
                        .build()
                );

        if (cqlValue != null && !cqlValue.isBlank()) {
            builder.withCQLFilter(cqlValue);
        }

        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(builder.build()).count();
    }


    public static long countStatusResults(String laneUID, OSCARServiceModule module, String cqlValue, Instant begin, Instant end){
        CommandStatusFilter.Builder builder = new CommandStatusFilter.Builder()
                        .withStatus(ICommandStatus.CommandStatusCode.COMPLETED)
                        .withReportTimeDuring(begin, end)
                        .withCommands(new CommandFilter.Builder()
                                        .withSystems()
                                        .withUniqueIDs(laneUID)
                                        .done()
                                        .build()
                        );

        if (cqlValue != null && !cqlValue.isBlank()) {
            builder.withCQLFilter(cqlValue);
        }

        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getCommandStatusStore().select(builder.build()).count();

    }

    //  suppressed : RPM Gamma Alert = true  but the EML Gamma Alert = false (release)
    // so count how many times the RPM had a true alarm but the EML decided it was false

    public static long countObservationsFromLane(String laneUID, OSCARServiceModule module, String cqlValue, Instant begin, Instant end, String... observedProperties){
        ObsFilter.Builder builder = new ObsFilter.Builder()
                .withSystems().withUniqueIDs(laneUID)
                .done()
                .withResultTimeDuring(begin, end)
                .withDataStreams(new DataStreamFilter.Builder()
                .withObservedProperties(observedProperties)
                        .build()
                );

        if (cqlValue != null && !cqlValue.isBlank()) {
            builder.withCQLFilter(cqlValue);
        }

        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(builder.build()).count();
    }

    public static Map<Instant, Long> countObservationsByDay(String laneUIDs, OSCARServiceModule module,String cqlValue, Instant startDate, Instant endDate, String... observedProperties){
        Map<Instant, Long> result = new LinkedHashMap<>();

        var start = startDate;
        var end = endDate;

        while (start.isBefore(end)) {
            Instant currentDay = start;
            Instant endOfCurrentDay = currentDay.plus(1, ChronoUnit.DAYS);

            if(endOfCurrentDay.isAfter(end)){
                endOfCurrentDay = end;
            }

            long count = countObservationsFromLane(laneUIDs, module, cqlValue, currentDay, endOfCurrentDay, observedProperties);

            result.put(currentDay, count);

            start = endOfCurrentDay;
        }

        return result;
    }
    
    public static Iterator<ICommandStatus> queryCommandStatus(OSCARServiceModule module, String laneUID, Instant begin, Instant end){
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getCommandStatusStore().select(new CommandStatusFilter.Builder()
                .withStatus(ICommandStatus.CommandStatusCode.COMPLETED)
                .withReportTimeDuring(begin, end)
                .withCommands(new CommandFilter.Builder().withSystems().withUniqueIDs(laneUID).done()
                .build()).build()).iterator();
    }
}
