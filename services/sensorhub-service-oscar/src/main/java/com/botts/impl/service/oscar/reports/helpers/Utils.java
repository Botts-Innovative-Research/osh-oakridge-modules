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
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;


public class Utils {

    // ALARMS
    public static Predicate<IObsData> gammaNeutronPredicate = (obsData) -> obsData.getResult().getBooleanValue(4) && obsData.getResult().getBooleanValue(5);
    public static Predicate<IObsData> gammaPredicate = (obsData) -> obsData.getResult().getBooleanValue(4) && !obsData.getResult().getBooleanValue(5);
    public static Predicate<IObsData> neutronPredicate = (obsData) -> !obsData.getResult().getBooleanValue(4) && obsData.getResult().getBooleanValue(5);
    public static Predicate<IObsData> occupancyTotalPredicate = (obsData) -> true;

    // EML
    public static Predicate<IObsData> emlSuppressedPredicate = (obsData) -> obsData.getResult().getStringValue(0).equals("RELEASE");

    // FAULTS
    public static Predicate<IObsData> tamperPredicate = (obsData) -> obsData.getResult().getBooleanValue(1);
    public static Predicate<IObsData> gammaHighPredicate = (obsData) -> obsData.getResult().getStringValue(1).equals("Fault - Gamma High");
    public static Predicate<IObsData> gammaLowPredicate = (obsData) -> obsData.getResult().getStringValue(1).equals("Fault - Gamma Low");
    public static Predicate<IObsData> neutronHighPredicate = (obsData) -> obsData.getResult().getStringValue(1).equals("Fault - Neutron High");
//    public static Predicate<IObsData> commsPredicate = (obsData) -> obsData.getResult().getBooleanValue(1); //TODO
//    public static Predicate<IObsData> cameraPredicate = (obsData) -> obsData.getResult().getBooleanValue(1); //TODO
//    public static Predicate<IObsData> extendedOccPredicate = (obsData) -> obsData.getResult().getBooleanValue(1); //TODO


    public static long calcEMLAlarmRate(long emlSuppCount, long alarmCount){
        if (emlSuppCount + alarmCount == 0) return 0;

        return emlSuppCount / (emlSuppCount + alarmCount);
    }

    public static long calculateAlarmingOccRate(long alarmCount, long occupancyCount){
        if(occupancyCount == 0) return 0;

        return alarmCount / occupancyCount;
    }

    public static long countObservations(OSCARServiceModule module, Predicate<IObsData> valuePredicate, Instant begin, Instant end, String... observedProperties){
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(observedProperties)
                        .withValidTimeDuring(begin, end)
                        .build())
                .withValuePredicate(valuePredicate)
                .build()).count();
    }


    public static long countStatusResults(String laneUID, OSCARServiceModule module, Predicate<ICommandStatus> valuePredicate, Instant begin, Instant end){
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getCommandStatusStore().select(new CommandStatusFilter.Builder()
                        .withStatus(ICommandStatus.CommandStatusCode.COMPLETED)
                        .withReportTimeDuring(begin, end)
                        .withValuePredicate(valuePredicate)
                        .withCommands(new CommandFilter.Builder().withSystems(new SystemFilter.Builder().withUniqueIDs(laneUID).build()).build())
                        .build()).count();
    }

    //  suppressed : RPM Gamma Alert = true  but the EML Gamma Alert = false (release)
    // so count how many times the RPM had a true alarm but the EML decided it was false

    public static long countObservationsFromLane(String laneUID, OSCARServiceModule module, Predicate<IObsData> valuePredicate, Instant begin, Instant end, String... observedProperties){
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                        .withSystems(new SystemFilter.Builder()
                                .withUniqueIDs(laneUID)
                                .build())
                        .withDataStreams(new DataStreamFilter.Builder()
                                .withObservedProperties(observedProperties)
                                .withValidTimeDuring(begin, end)
                                .build())
                        .withValuePredicate(valuePredicate)
                        .build()).count();
    }

    public static Map<Instant, Long> countObservationsByDay(String laneUIDs, OSCARServiceModule module, Predicate<IObsData> predicate, Instant startDate, Instant endDate, String... observedProperties){
        Map<Instant, Long> result = new LinkedHashMap<>();

        while (startDate.isBefore(endDate)) {
            Instant currentDay = startDate;
            Instant endOfCurrentDay = currentDay.plus(1, ChronoUnit.DAYS);

            if(endOfCurrentDay.isAfter(endDate)){
                endOfCurrentDay = endDate;
            }

            long count = Utils.countObservationsFromLane(laneUIDs, module,  predicate, currentDay, endOfCurrentDay, observedProperties);
            if (count > 0) {
                result.put(currentDay, count);
            }


            startDate = endOfCurrentDay;
        }
        return result;
    }

    public static Iterator<IObsData> getQuery(OSCARServiceModule module, String laneUID, Instant begin, Instant end, String... observedProperties){
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withSystems(new SystemFilter.Builder()
                        .withUniqueIDs(laneUID)
                        .build())
                .withDataStreams(new DataStreamFilter.Builder()
                        .withValidTimeDuring(begin, end)
                        .withObservedProperties(observedProperties)
                        .build())
                .build()).iterator();
    }

    public static Iterator<ICommandStatus> queryCommandStatus(OSCARServiceModule module, String laneUID, Instant begin, Instant end){
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getCommandStatusStore().select(new CommandStatusFilter.Builder()
                .withStatus(ICommandStatus.CommandStatusCode.COMPLETED)
                .withReportTimeDuring(begin, end)
                .withCommands(new CommandFilter.Builder()
                        .withSystems(new SystemFilter.Builder()
                        .withUniqueIDs(laneUID)
                        .build())
                .build()).build()).iterator();
    }
}
