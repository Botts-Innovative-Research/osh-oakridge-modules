package com.botts.impl.service.oscar.reports.helpers;

import com.botts.impl.service.oscar.OSCARServiceConfig;
import com.botts.impl.service.oscar.OSCARServiceModule;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.utils.rad.RADHelper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;


public class Utils {

    // ALARMS
    public static Predicate<IObsData> gammaNeutronPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
    public static Predicate<IObsData> gammaPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);
    public static Predicate<IObsData> neutronPredicate = (obsData) -> !obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
    public static Predicate<IObsData> occupancyTotalPredicate = (obsData) -> true;

    // EML
    public static Predicate<IObsData> emlRpmAlarmPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) || obsData.getResult().getBooleanValue(6);
    public static Predicate<IObsData> emlAlarmPredicate = (obsData) -> obsData.getResult().getBooleanValue(3) || obsData.getResult().getBooleanValue(4);


    public static Predicate<IObsData> emlRpmGammaPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);
    public static Predicate<IObsData> emlRpmNeutronPredicate = (obsData) -> !obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
    public static Predicate<IObsData> emlRpmGammaNeutronPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);

    // when eml is false but the rpm is true / total number of alarms
    public static Predicate<IObsData> emlGammaPredicate = (obsData) -> obsData.getResult().getBooleanValue(3) && !obsData.getResult().getBooleanValue(4);
    public static Predicate<IObsData> emlNeutronPredicate = (obsData) -> !obsData.getResult().getBooleanValue(3) && obsData.getResult().getBooleanValue(4);
    public static Predicate<IObsData> emlGammaNeutronPredicate = (obsData) -> obsData.getResult().getBooleanValue(3) && obsData.getResult().getBooleanValue(4);

    // FAULTS
    public static Predicate<IObsData> tamperPredicate = (obsData) -> obsData.getResult().getBooleanValue(1);
    public static Predicate<IObsData> gammaHighPredicate = (obsData) -> obsData.getResult().getStringValue(1).equals("Fault - Gamma High");
    public static Predicate<IObsData> gammaLowPredicate = (obsData) -> obsData.getResult().getStringValue(1).equals("Fault - Gamma Low");
    public static Predicate<IObsData> neutronHighPredicate = (obsData) -> obsData.getResult().getStringValue(1).equals("Fault - Neutron High");
    public static Predicate<IObsData> commsPredicate = (obsData) -> obsData.getResult().getBooleanValue(1);
    public static Predicate<IObsData> cameraPredicate = (obsData) -> obsData.getResult().getBooleanValue(1);
    public static Predicate<IObsData> extendedOccPredicate = (obsData) -> obsData.getResult().getBooleanValue(1);

    // adj
    public static Predicate<IObsData> realAlarmOtherPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("Real Alarm - Other");
    public static Predicate<IObsData> falseAlarmOtherPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("False Alarm - Other");
    public static Predicate<IObsData> phyInsPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("Physical Inspection");
    public static Predicate<IObsData> incMedPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("Innocent Alarm - Medical Isotope Found");
    public static Predicate<IObsData> incRadioPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("Innocent Alarm - Declared Shipment of Radioactive Material");
    public static Predicate<IObsData> noDisPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("No Disposition");
    public static Predicate<IObsData> falseAlarmRIIDPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("False Alarm - RIID/ASP Indicates Background Only");
    public static Predicate<IObsData> realAlarmContraPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("Real Alarm - Contraband Found");
    public static Predicate<IObsData> tamperFaultPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("Tamper/Fault - Unauthorized Activity");
    public static Predicate<IObsData> alarmTamperFaultPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity");
    public static Predicate<IObsData> alarmNORMPredicate = (obsData) -> obsData.getResult().getStringValue(3).contains("Alarm - Naturally Occurring Radioactive Material (NORM) Found");


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


    //  suppressed : RPM Gamma Alert = true  but the EML Gamma Alert = false
    // so count how many times the RPM had a true alarm but the EML descided it was false
    //
    public static long countEMLSuppressed(String laneUID, String[] obsProp, OSCARServiceModule module, Instant begin, Instant end){

        var query = module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withSystems(new SystemFilter.Builder()
                        .withUniqueIDs(laneUID)
                        .build())
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(obsProp)
                        .withValidTimeDuring(begin, end)
                        .build())
                .build())
                .iterator();


        var emlSuppressedCount = 0;

        while (query.hasNext()) {
            var entry  = query.next();

            var result = entry.getResult();
//
            var emlGammaResult = result.getBooleanValue();
            var emlNeutronResult = result.getBooleanValue();

            var rpmGammaResult = result.getBooleanValue();
            var rpmNeutronResult = result.getBooleanValue();

        }
        return emlSuppressedCount;

        //        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
//                .withSystems(new SystemFilter.Builder()
//                        .withUniqueIDs(laneUID)
//                        .build())
//                .withDataStreams(new DataStreamFilter.Builder()
//                        .withObservedProperties(observedProperties)
//                        .withValidTimeDuring(begin, end)
//                        .build())
//                .withValuePredicate(valuePredicate1)
//                .withValuePredicate(valuePredicate2)
//                .build()).count();


    }

    public static long countObservationsFromLane(String laneUID, String[] observedProperties, OSCARServiceModule module, Predicate<IObsData> valuePredicate, Instant begin, Instant end){
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

    public static Map<Instant, Long> countObservationsByDay(String[] observedProperties, OSCARServiceModule module, Predicate<IObsData> predicate,  Instant startDate, Instant endDate){
        Map<Instant, Long> result = new HashMap<>();

        while (startDate.isBefore(endDate)) {
            Instant currentDay = startDate;
            Instant endOfCurrentDay = currentDay.plus(1, ChronoUnit.DAYS);

            if(endOfCurrentDay.isAfter(endDate)){
                endOfCurrentDay = endDate;
            }

            long count = Utils.countObservations(observedProperties, module,  predicate, currentDay, endOfCurrentDay);
            result.put(currentDay, count);

            startDate = endOfCurrentDay;
        }
        return result;
    }

    public static void getAdjudicationDetails(OSCARServiceModule module, String laneUID, String observedProperties){
        module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withSystems(new SystemFilter.Builder()
                        .withUniqueIDs(laneUID)
                        .build())
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(observedProperties)
                        .build())
                .build());
    }
}