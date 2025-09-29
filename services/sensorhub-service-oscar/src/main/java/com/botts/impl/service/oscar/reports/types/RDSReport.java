package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;


public class RDSReport extends Report {

    private static final Logger log = LoggerFactory.getLogger(RDSReport.class);
    String reportTitle = "RDS Site Report";

    Document document;
    PdfDocument pdfDocument;
    String pdfFileName;

    String siteId;
    Instant begin;
    Instant end;

    TableGenerator tableGenerator;
    OSCARServiceModule module;

    public RDSReport(String filePath, Instant startTime, Instant endTime, OSCARServiceModule module) {
        try {
            pdfFileName = ReportCmdType.RDS_SITE.name()+ "_" + startTime + "_"+ endTime + ".pdf";
            File file = new File("files/reports/" + pdfFileName);
            file.getParentFile().mkdirs();

            pdfDocument = new PdfDocument(new PdfWriter(file));
            document = new Document(pdfDocument);

        } catch (IOException e) {
            document.close();
            pdfDocument.close();
            log.error(e.getMessage(), e);
            return;
        }
        this.module = module;

        this.siteId = module.getConfiguration().nodeId;

        this.begin = startTime;
        this.end = endTime;
        this.tableGenerator = new TableGenerator();
    }

    @Override
    public String generate(){
        addHeader();
        addAlarmStatistics();
        addFaultStatistics();
        addDriveStorageAvailability();

        document.close();
        pdfDocument.close();

        tableGenerator =null;

        return pdfFileName;
    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Site ID: " + siteId).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addAlarmStatistics(){

        document.add(new Paragraph("Alarm Statistics"));

        Map<String, String> alarmOccCounts = new HashMap<>();

        Predicate<IObsData> gammaNeutronPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);};

        Predicate<IObsData> gammaPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);};

        Predicate<IObsData> neutronPredicate = (obsData) -> {return !obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);};;

        Predicate<IObsData> occupancyTotalPredicate = (obsData) -> {return true;};

        Predicate<IObsData> occupancyNonAlarmingPredicate = (obsData) -> {return !obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);};
//        Predicate<IObsData> emlSuppPredicate = (obsData) -> {return obsData.getResult();};


        long gammaNeutronAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, gammaNeutronPredicate, begin, end);
        long gammaAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module ,gammaPredicate, begin, end);
        long neutronAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, neutronPredicate, begin, end);
        long totalOccupancyCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, occupancyTotalPredicate, begin, end);
//        long nonAlarmingOccupancyCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, occupancyNonAlarmingPredicate, begin, end);

//        long emlSuppressedCount= Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, emlSuppPredicate);


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


        Predicate<IObsData> tamperPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(1);};
        Predicate<IObsData> gammaHighPredicate = (obsData) -> {return obsData.getResult().getStringValue(1).equals("Fault - Gamma High");};
        Predicate<IObsData> gammaLowPredicate = (obsData) -> {return obsData.getResult().getStringValue(1).equals("Fault - Gamma Low");};
        Predicate<IObsData> neutronHighPredicate = (obsData) -> {return obsData.getResult().getStringValue(1).equals("Fault - Neutron High");};
//        Predicate<IObsData> commsPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(1);};
//        Predicate<IObsData> cameraPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(1);};
//        Predicate<IObsData> extendedOccPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(1);};


        long tamperCount = Utils.countObservations(new String[]{RADHelper.DEF_TAMPER}, module, tamperPredicate, begin, end);
        long gammaHighFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM}, module,gammaHighPredicate, begin, end);
        long gammaLowFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM}, module,gammaLowPredicate, begin, end);
        long neutronHighFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM}, module,neutronHighPredicate, begin, end);

//        long extendedOccupancyCount = Utils.countObservations(RADHelper.DEF_TAMPER, module,extendedOccPredicate, begin, end);


        faultCounts.put("Tamper", String.valueOf(tamperCount));
//        faultCounts.put("Extended Occupancy", extendedOccupancyCount);
//        faultCounts.put("Comm", commsCount);
//        faultCounts.put("Camera", camCount);
        faultCounts.put("Gamma-High", String.valueOf(gammaHighFaultCount));
        faultCounts.put("Gamma-Low", String.valueOf(gammaLowFaultCount));
        faultCounts.put("Neutron-High", String.valueOf(neutronHighFaultCount));

        var table = tableGenerator.addTable(faultCounts);
        document.add(table);

        document.add(new Paragraph("\n"));
    }

    private void addDriveStorageAvailability(){
        document.add(new Paragraph("Drive Storage Availability"));

        Map<String, String> driveStorageAvailability = new HashMap<>();
        var roots = File.listRoots();

        for(var drive: roots){
            document.add(new Paragraph(drive.getName()));
            long free = drive.getFreeSpace() / (1024 * 1024 * 1024);
            long total = drive.getTotalSpace() / (1024 * 1024 * 1024);
            long usable = drive.getUsableSpace() / (1024 * 1024 * 1024);

            driveStorageAvailability.put("Free", free + " GB");
            driveStorageAvailability.put("Total", total + " GB");
            driveStorageAvailability.put("Usable", usable + " GB");

            var table = tableGenerator.addTable(driveStorageAvailability);
            document.add(table);

            document.add(new Paragraph("\n"));
        }
    }

}
