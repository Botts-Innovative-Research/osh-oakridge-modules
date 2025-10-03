
package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.sensorhub.impl.utils.rad.RADHelper;
import java.io.File;
import java.io.OutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class RDSReport extends Report {

    Document document;
    PdfDocument pdfDocument;

    String siteId;
    Instant begin;
    Instant end;

    TableGenerator tableGenerator;
    OSCARServiceModule module;

    public RDSReport(OutputStream out, Instant startTime, Instant endTime, OSCARServiceModule module) {
        pdfDocument = new PdfDocument(new PdfWriter(out));
        document = new Document(pdfDocument);

        this.module = module;
        this.siteId = module.getConfiguration().nodeId;
        this.begin = startTime;
        this.end = endTime;
        this.tableGenerator = new TableGenerator();
    }

    @Override
    public void generate(){
        addHeader();
        addAlarmStatistics();
        addFaultStatistics();
        addDriveStorageAvailability();

        document.close();
        tableGenerator = null;
    }

    private void addHeader(){
        document.add(new Paragraph("RDS Site Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Site ID: " + siteId).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addAlarmStatistics(){
        document.add(new Paragraph("Alarm Statistics"));

        Map<String, String> alarmOccCounts = new LinkedHashMap<>();

        long gammaNeutronAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, Utils.gammaNeutronPredicate, begin, end);
        long gammaAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, Utils.gammaPredicate, begin, end);
        long neutronAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, Utils.neutronPredicate, begin, end);
        long totalOccupancyCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, Utils.occupancyTotalPredicate, begin, end);

        long rpmEmlAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_EML_SCAN}, module, Utils.emlRpmAlarmPredicate, begin, end);
        long emlAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_EML_ANALYSIS}, module, Utils.emlAlarmPredicate, begin, end);

        long totalAlarmingCount = gammaAlarmCount + neutronAlarmCount + gammaNeutronAlarmCount;
        long alarmOccupancyAverage = Utils.calculateAlarmingOccRate(totalAlarmingCount, totalOccupancyCount);

        long emlSuppressedCount = 0;
        long emlSuppressedAverage = Utils.calcEMLAlarmRate(emlSuppressedCount, totalAlarmingCount);

        alarmOccCounts.put("Gamma Alarm", String.valueOf(gammaAlarmCount));
        alarmOccCounts.put("Neutron Alarm", String.valueOf(neutronAlarmCount));
        alarmOccCounts.put("Gamma-Neutron Alarm", String.valueOf(gammaNeutronAlarmCount));
        alarmOccCounts.put("Total Occupancies", String.valueOf(totalOccupancyCount));
        alarmOccCounts.put("Alarm Occupancy Rate", String.valueOf(alarmOccupancyAverage));
        alarmOccCounts.put("EML Suppressed", String.valueOf(emlSuppressedCount));
        alarmOccCounts.put("EML Alarm Rate", String.valueOf(emlSuppressedAverage));

        var table = tableGenerator.addTable(alarmOccCounts);
        if (table == null) {
            document.add(new Paragraph("Failed to add Alarm statistics"));
            return;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addFaultStatistics(){
        document.add(new Paragraph("Fault Statistics"));

        HashMap<String, String> faultCounts = new LinkedHashMap<>();

        long tamperCount = Utils.countObservations(new String[]{RADHelper.DEF_TAMPER}, module, Utils.tamperPredicate, begin, end);
        long gammaHighFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM}, module, Utils.gammaHighPredicate, begin, end);
        long gammaLowFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM}, module, Utils.gammaLowPredicate, begin, end);
        long neutronHighFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM}, module, Utils.neutronHighPredicate, begin, end);
        long extendedOccupancyCount = Utils.countObservations(new String[]{RADHelper.DEF_TAMPER}, module, Utils.extendedOccPredicate, begin, end);
        long commsCount = Utils.countObservations(new String[]{RADHelper.DEF_COMM}, module, Utils.commsPredicate, begin, end);
        long camCount = Utils.countObservations(new String[]{RADHelper.DEF_VIDEO}, module, Utils.cameraPredicate, begin, end);

        faultCounts.put("Tamper", String.valueOf(tamperCount));
        faultCounts.put("Gamma-High", String.valueOf(gammaHighFaultCount));
        faultCounts.put("Gamma-Low", String.valueOf(gammaLowFaultCount));
        faultCounts.put("Neutron-High", String.valueOf(neutronHighFaultCount));
        faultCounts.put("Extended Occupancy", String.valueOf(extendedOccupancyCount));
        faultCounts.put("Comm", String.valueOf(commsCount));
        faultCounts.put("Camera", String.valueOf(camCount));

        var table = tableGenerator.addTable(faultCounts);
        if (table == null) {
            document.add(new Paragraph("Failed to create Fault Table"));
            return;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addDriveStorageAvailability(){
        document.add(new Paragraph("Drive Storage Availability"));

        Map<String, String> driveStorageAvailability = new LinkedHashMap<>();
        var roots = File.listRoots();

        for(var drive: roots){
            document.add(new Paragraph(drive.getName()));
            long free = drive.getFreeSpace() / (1024 * 1024 * 1024);
            long total = drive.getTotalSpace() / (1024 * 1024 * 1024);
            long usable = drive.getUsableSpace() / (1024 * 1024 * 1024);

            driveStorageAvailability.put("Free", free + " GB");
            driveStorageAvailability.put("Usable", usable + " GB");
            driveStorageAvailability.put("Total", total + " GB");

            var table = tableGenerator.addTable(driveStorageAvailability);
            if (table == null) {
                document.add(new Paragraph("Failed to create Drive Storage Availability"));
                return;
            }

            document.add(table);
            document.add(new Paragraph("\n"));
        }
    }
}
