package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.sensorhub.impl.utils.rad.RADHelper;
import java.io.OutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class LaneReport extends Report {

    Document document;
    PdfDocument pdfDocument;

    String laneUIDs;
    Instant begin;
    Instant end;
    OSCARServiceModule module;
    TableGenerator tableGenerator;

    public LaneReport(OutputStream out, Instant startTime, Instant endTime, String laneUIDs, OSCARServiceModule module) {
        pdfDocument = new PdfDocument(new PdfWriter(out));
        document = new Document(pdfDocument);

        this.laneUIDs = laneUIDs;
        this.module = module;
        this.begin = startTime;
        this.end = endTime;
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

    private void addHeader(){
        document.add(new Paragraph("Lane Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane UIDs: " + laneUIDs).setFontSize(12));
        document.add(new Paragraph("\n"));
    }


    private void addAlarmStatistics(){
        document.add(new Paragraph("Alarm Statistics"));

        Map<String, Map<String, String>> countsLane = new LinkedHashMap<>();

        for (var laneUID : laneUIDs.split(",")){
            var counts = calculateAlarmCounts(laneUID);

            countsLane.put(laneUID, counts);
        }

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

    private Map<String, String> calculateAlarmCounts(String laneUID) {
        Map<String, String> alarmOccCounts = new LinkedHashMap<>();

        long gammaNeutronAlarmCount = Utils.countObservationsFromLane(laneUID, module, Utils.gammaNeutronPredicate, begin, end, RADHelper.DEF_OCCUPANCY);
        long gammaAlarmCount = Utils.countObservationsFromLane(laneUID, module, Utils.gammaPredicate, begin, end, RADHelper.DEF_OCCUPANCY);
        long neutronAlarmCount = Utils.countObservationsFromLane(laneUID, module, Utils.neutronPredicate, begin, end, RADHelper.DEF_OCCUPANCY);
        long totalOccupancyCount = Utils.countObservationsFromLane(laneUID, module, Utils.occupancyTotalPredicate, begin, end, RADHelper.DEF_OCCUPANCY);

        long emlSuppressedCount = Utils.countObservationsFromLane(laneUID, module, Utils.emlSuppressedPredicate, begin, end, RADHelper.DEF_EML_ANALYSIS);


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

        long tamperCount = Utils.countObservationsFromLane(laneUID, module, Utils.tamperPredicate, begin, end, RADHelper.DEF_TAMPER);
        long gammaHighFaultCount = Utils.countObservationsFromLane(laneUID, module, Utils.gammaHighPredicate, begin, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        long gammaLowFaultCount = Utils.countObservationsFromLane(laneUID, module, Utils.gammaLowPredicate, begin, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        long neutronHighFaultCount = Utils.countObservationsFromLane(laneUID, module, Utils.neutronHighPredicate, begin, end, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);
//        long extendedOccupancyCount = Utils.countObservationsFromLane(laneUID, module, Utils.extendedOccPredicate, begin, end, RADHelper.DEF_OCCUPANCY);

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