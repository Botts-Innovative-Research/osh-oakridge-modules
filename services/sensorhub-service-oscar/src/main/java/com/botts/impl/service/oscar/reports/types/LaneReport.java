package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.utils.rad.RADHelper;
import java.io.OutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;


public class LaneReport extends Report {

    Document document;
    PdfDocument pdfDocument;

    String laneUIDs;
    TableGenerator tableGenerator;

    public LaneReport(OutputStream out, Instant startTime, Instant endTime, String laneUIDs, OSCARServiceModule module) {
        super(out, startTime, endTime, module);
        pdfDocument = new PdfDocument(new PdfWriter(out));
        document = new Document(pdfDocument);

        this.laneUIDs = laneUIDs;
        this.tableGenerator = new TableGenerator();
    }

    @Override
    public void generate() {
        addHeader();
        addAlarmStatistics();
        addFaultStatistics();

        document.close();
        tableGenerator = null;
    }

    @Override
    public String getReportType() {
        return ReportCmdType.LANE.name();
    }

    private void addHeader(){
        document.add(new Paragraph("Lane Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane UIDs: " + laneUIDs).setFontSize(12));
        document.add(new Paragraph("\n"));
    }


    private void addAlarmStatistics(){
        document.add(new Paragraph("Alarm Statistics"));

        Map<String, Map<String, String>> countsLane = getLaneCounts(laneUIDs.split(","));

//        for (var laneUID : laneUIDs.split(",")) {
//            var counts = calculateAlarmCounts(laneUID);
//            countsLane.put(laneUID, counts);
//        }

        var table = tableGenerator.addLanesTable(countsLane);
        if (table == null) {
            document.add(new Paragraph("Failed to add Alarm Stats table to pdf"));
            return;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addFaultStatistics(){
        document.add(new Paragraph("Fault Statistics"));

        Map<String, Map<String, String>> countsLane = new LinkedHashMap<>();

        for (var laneUID : laneUIDs.split(",")){
            var counts = calculateFaultCounts(laneUID);
            countsLane.put(laneUID, counts);
        }

        var table = tableGenerator.addLanesTable(countsLane);
        if (table == null) {
            document.add(new Paragraph("Failed to add Fault Stats table to pdf"));
            return;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private Map<String, Map<String, String>> getLaneCounts(String... laneUids) {
        var db = module.getParentHub().getDatabaseRegistry().getFederatedDatabase();

        Map<BigId, String> dsIdToLaneUID = new HashMap<>();
        var laneDsIterator = db.getDataStreamStore().selectEntries(new DataStreamFilter.Builder()
                .withSystems().withUniqueIDs(laneUids).includeMembers(true).done()
                .withObservedProperties(RADHelper.DEF_OCCUPANCY)
                .build())
                .iterator();

        while (laneDsIterator.hasNext()) {
            var entry = laneDsIterator.next();
            var dsId = entry.getKey();
            var ds = entry.getValue();
            dsIdToLaneUID.put(dsId.getInternalID(), ds.getSystemID().getUniqueID());
        }

        var allObsIterator = db.getObservationStore().select(new ObsFilter.Builder()
                .withSystems().withUniqueIDs(laneUids).includeMembers(true).done()
                .withDataStreams().withObservedProperties(RADHelper.DEF_OCCUPANCY).done()
                .withResultTimeDuring(start, end)
                        .build())
                .iterator();

        Map<String, Map<String, Long>> laneCounts = new LinkedHashMap<>();
        for (String laneUID : dsIdToLaneUID.values()) {
            laneCounts.put(laneUID, new LinkedHashMap<>());
            Map<String, Long> counts = laneCounts.get(laneUID);
            counts.put("Gamma Alarm", 0L);
            counts.put("Neutron Alarm", 0L);
            counts.put("Gamma-Neutron Alarm", 0L);
            counts.put("Total Occupancies", 0L);
        }

        while (allObsIterator.hasNext()) {
            var obs = allObsIterator.next();
            BigId dsId = obs.getDataStreamID();
            String laneUID = dsIdToLaneUID.get(dsId);
            if (laneUID == null) continue;

            Map<String, Long> counts = laneCounts.get(laneUID);

            //  todo: update because shouldnt use test and cannot.
//            if (Utils.gammaNeutronCql.test(obs)) {
//                counts.put("Gamma-Neutron Alarm", counts.get("Gamma-Neutron Alarm") + 1);
//            }
//            if (Utils.gammaCql.test(obs)) {
//                counts.put("Gamma Alarm", counts.get("Gamma Alarm") + 1);
//            }
//            if (Utils.neutronCql.test(obs)) {
//                counts.put("Neutron Alarm", counts.get("Neutron Alarm") + 1);
//            }
//            if (Utils.occupancyCql.test(obs)) {
//                counts.put("Total Occupancies", counts.get("Total Occupancies") + 1);
//            }
        }

        Map<String, Map<String, String>> results = new LinkedHashMap<>();
        for (var entry : laneCounts.entrySet()) {
            String laneUID = entry.getKey();
            Map<String, Long> counts = entry.getValue();
            Map<String, String> stringCounts = new LinkedHashMap<>();
            counts.forEach((k, v) -> stringCounts.put(k, String.valueOf(v)));
            results.put(laneUID, stringCounts);
        }

        return results;
    }

    private Map<String, String> calculateAlarmCounts(String laneUID) {
        Map<String, String> alarmOccCounts = new LinkedHashMap<>();

        long gammaNeutronAlarmCount = Utils.countObservationsFromLane(laneUID, module, Utils.gammaNeutronCql, start, end, RADHelper.DEF_OCCUPANCY);
        long gammaAlarmCount = Utils.countObservationsFromLane(laneUID, module, Utils.gammaCql, start, end, RADHelper.DEF_OCCUPANCY);
        long neutronAlarmCount = Utils.countObservationsFromLane(laneUID, module, Utils.neutronCql, start, end, RADHelper.DEF_OCCUPANCY);
        long totalOccupancyCount = Utils.countObservationsFromLane(laneUID, module,null, start, end, RADHelper.DEF_OCCUPANCY);

        long emlSuppressedCount = Utils.countObservationsFromLane(laneUID, module, Utils.emlSuppressedCql, start, end, RADHelper.DEF_EML_ANALYSIS);


        long totalAlarmingCount = gammaAlarmCount + neutronAlarmCount + gammaNeutronAlarmCount;
        long alarmOccupancyAverage = Utils.calculateAlarmingOccRate(totalAlarmingCount, totalOccupancyCount);
        long emlSuppressedAverage = Utils.calcEMLAlarmRate(emlSuppressedCount, totalAlarmingCount);

        alarmOccCounts.put("Gamma Alarm", String.valueOf(gammaAlarmCount));
        alarmOccCounts.put("Neutron Alarm", String.valueOf(neutronAlarmCount));
        alarmOccCounts.put("Gamma-Neutron Alarm", String.valueOf(gammaNeutronAlarmCount));
        alarmOccCounts.put("EML Suppressed", String.valueOf(emlSuppressedCount));
        alarmOccCounts.put("Total Occupancies", String.valueOf(totalOccupancyCount));
        alarmOccCounts.put("Alarm Occupancy Rate", String.valueOf(alarmOccupancyAverage));
        alarmOccCounts.put("EML Alarm Rate", String.valueOf(emlSuppressedAverage));

        return alarmOccCounts;
    }

    private Map<String, String> calculateFaultCounts(String laneUID){
        HashMap<String, String> faultCounts = new LinkedHashMap<>();

        long tamperCount = Utils.countObservationsFromLane(laneUID, module, Utils.tamperCql, start, end, RADHelper.DEF_TAMPER);
        long gammaHighFaultCount = Utils.countObservationsFromLane(laneUID, module, Utils.gammaHighFaultCql, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        long gammaLowFaultCount = Utils.countObservationsFromLane(laneUID, module, Utils.gammaLowFaultCql, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        long neutronHighFaultCount = Utils.countObservationsFromLane(laneUID, module, Utils.neutronFaultCql, start, end, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);
//        long extendedOccupancyCount = Utils.countObservationsFromLane(laneUID, module, Utils.extendedOccPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        faultCounts.put("Tamper", String.valueOf(tamperCount));
        faultCounts.put("Gamma-High", String.valueOf(gammaHighFaultCount));
        faultCounts.put("Gamma-Low", String.valueOf(gammaLowFaultCount));
        faultCounts.put("Neutron-High", String.valueOf(neutronHighFaultCount));
//        faultCounts.put("Extended Occupancy", String.valueOf(extendedOccupancyCount));
//        faultCounts.put("Comm", commsCount);
//        faultCounts.put("Camera", camCount);

        return faultCounts;
    }

}