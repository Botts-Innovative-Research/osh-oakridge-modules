package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;


public class LaneReport extends Report {

    private static final Logger log = LoggerFactory.getLogger(LaneReport.class);
    String reportTitle = "Lane Report";

    Document document;
    PdfDocument pdfDocument;
    String pdfFileName;

    String laneUID;
    Instant begin;
    Instant end;
    OSCARServiceModule module;
    TableGenerator tableGenerator;

    public LaneReport(String filePath, Instant startTime, Instant endTime, String laneUID, OSCARServiceModule module) {
        try {
            pdfFileName = filePath;
            File file = new File(pdfFileName);
            file.getParentFile().mkdirs();

            pdfDocument = new PdfDocument(new PdfWriter(file));
            document = new Document(pdfDocument);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        this.laneUID = laneUID;
        this.module = module;
        this.begin = startTime;
        this.end = endTime;
        this.tableGenerator = new TableGenerator();
    }

    @Override
    public String generate() {

        addHeader();
        addAlarmStatistics();
        addFaultStatistics();

        document.close();
        pdfDocument.close();

        tableGenerator = null;
        return pdfFileName;
    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane Name: " + "TODO: lane name").setFontSize(12));
        document.add(new Paragraph("\n"));
    }


    private void addAlarmStatistics(){

        document.add(new Paragraph("Alarm Statistics"));

        Map<String, String> alarmOccCounts = new HashMap<>();

        Predicate<IObsData> gammaNeutronPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> gammaPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> neutronPredicate = (obsData) -> !obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> occupancyTotalPredicate = (obsData) -> true;
//        Predicate<IObsData> occupancyNonAlarmingPredicate = (obsData) -> !obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);
//        Predicate<IObsData> emlSuppPredicate = (obsData) -> {return obsData.getResult();};


        long gammaNeutronAlarmCount = Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_OCCUPANCY}, module, gammaNeutronPredicate, begin, end);
        long gammaAlarmCount = Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_OCCUPANCY}, module ,gammaPredicate, begin, end);
        long neutronAlarmCount = Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_OCCUPANCY}, module, neutronPredicate, begin, end);
        long totalOccupancyCount = Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_OCCUPANCY}, module, occupancyTotalPredicate, begin, end);
//        long nonAlarmingOccupancyCount = Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_OCCUPANCY}, module, occupancyNonAlarmingPredicate, begin, end);
//        long emlSuppressedCount= Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_OCCUPANCY}, module, emlSuppPredicate);

        long totalAlarmingCount = gammaAlarmCount + neutronAlarmCount + gammaNeutronAlarmCount;

        long alarmOccupancyAverage = Utils.calculateAlarmingOccRate(totalAlarmingCount, totalOccupancyCount);
//        long emlSuppressedAverage = Utils.calcEMLAlarmRate(emlSuppressedCount, totalAlarmingCount);


        alarmOccCounts.put("Gamma Alarm", String.valueOf(gammaAlarmCount));
        alarmOccCounts.put("Neutron Alarm", String.valueOf(neutronAlarmCount));
        alarmOccCounts.put("Gamma-Neutron Alarm", String.valueOf(gammaNeutronAlarmCount));
//        alarmOccCounts.put("Non-Alarming Occupancies", String.valueOf(nonAlarmingOccupancyCount));
        alarmOccCounts.put("Total Occupancies", String.valueOf(totalOccupancyCount));
        alarmOccCounts.put("Alarm Occupancy Rate", String.valueOf(alarmOccupancyAverage));

        //        alarmOccCounts.put("EML Suppressed", emlSuppressedCount);
//        alarmOccCounts.put("EML Alarm Rate", emlSuppressedAverage);

        var table = tableGenerator.addTable(alarmOccCounts);
        document.add(table);

        document.add(new Paragraph("\n"));
    }

    private void addFaultStatistics(){
        document.add(new Paragraph("Fault Statistics"));

        HashMap<String, String> faultCounts = new HashMap<>();


        Predicate<IObsData> tamperPredicate = (obsData) -> obsData.getResult().getBooleanValue(1);
        Predicate<IObsData> gammaHighPredicate = (obsData) -> obsData.getResult().getStringValue(1).equals("Fault - Gamma High");
        Predicate<IObsData> gammaLowPredicate = (obsData) -> obsData.getResult().getStringValue(1).equals("Fault - Gamma Low");
        Predicate<IObsData> neutronHighPredicate = (obsData) -> obsData.getResult().getStringValue(1).equals("Fault - Neutron High");
//        Predicate<IObsData> commsPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(1);};
//        Predicate<IObsData> cameraPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(1);};
//        Predicate<IObsData> extendedOccPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(1);};


        long tamperCount = Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_TAMPER}, module, tamperPredicate, begin, end);
        long gammaHighFaultCount = Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM}, module,gammaHighPredicate, begin, end);
        long gammaLowFaultCount = Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM}, module,gammaLowPredicate, begin, end);
        long neutronHighFaultCount = Utils.countObservationsFromLane(laneUID, new String[]{RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM}, module,neutronHighPredicate, begin, end);

//        long extendedOccupancyCount = Utils.countObservationsFromLane(laneUID, RADHelper.DEF_TAMPER, module,extendedOccPredicate, begin, end);


        faultCounts.put("Tamper", String.valueOf(tamperCount));
//        faultCounts.put("Extended Occupancy", extendedOccupancyCount);
//        faultCounts.put("Comm", commsCount);
//        faultCounts.put("Camera", camCount);
        faultCounts.put("Gamma-High", String.valueOf(gammaHighFaultCount));
        faultCounts.put("Gamma-Low", String.valueOf(gammaLowFaultCount));
        faultCounts.put("Neutron-High", String.valueOf(neutronHighFaultCount));

        tableGenerator.addTable(faultCounts);

        document.add(new Paragraph("\n"));
    }

}
