package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ReportType;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;

import static com.botts.impl.service.oscar.reports.helpers.Constants.*;
import static com.botts.impl.service.oscar.reports.helpers.Constants.DEF_ALARM;
import static com.botts.impl.service.oscar.reports.helpers.Constants.DEF_GAMMA;
import static com.botts.impl.service.oscar.reports.helpers.Constants.DEF_NEUTRON;

public class LaneReport extends Report {

    String reportTitle = "Lane Report";
    String laneId;
    Document document;
    PdfWriter pdfWriter;
    PdfDocument pdfDocument;
    String pdfFileName;

    OSCARServiceModule module;
    TableGenerator tableGenerator;

    public LaneReport(Instant startTime, Instant endTime, String laneId, OSCARServiceModule module) {
        try {
            pdfFileName = ReportType.LANE.name() + "_" + laneId + "_" + startTime + "_"+ endTime + ".pdf";
            File file = new File("files/reports/" + pdfFileName);
            file.getParentFile().mkdirs();

            pdfWriter  = new PdfWriter(file);

            pdfDocument = new PdfDocument(pdfWriter);
            document = new Document(pdfDocument);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.laneId = laneId;
        this.module = module;
        tableGenerator = new TableGenerator(document);
    }

    @Override
    public String generate() {

        addHeader();
        addAlarmStatistics();
        addFaultStatistics();

        document.close();
        pdfDocument.close();

        return pdfFileName;

    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane Name: " + "TODO: lane name").setFontSize(12));
        document.add(new Paragraph("\n"));
    }


    private void addAlarmStatistics(){
        document.add(new Paragraph("Alarm Statistics"));

        HashMap<String, Double> alarmOccCounts = new HashMap<>();

        double gammaAlarmCount = 0;
        double neutronAlarmCount = 0;
        double gammaNeutronAlarmCount = 0;
        double emlSuppressedCount = 0;
        double totalOccupancyCount = 0;

        double alarmOccupancyAverage = 0;
        double emlSuppressedAverage = 0;

        // TODO: also need to sort out from specific LANE

        var query = module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(DEF_OCCUPANCY)
                        .build())
                .build()).iterator();

        while (query.hasNext()) {
            var entry = query.next();

            var result = entry.getResult();

            var gammaAlarm = result.getBooleanValue(5);
            var neutronAlarm = result.getBooleanValue(6);

            if (gammaAlarm && neutronAlarm) {
                gammaNeutronAlarmCount++;

            }  else if (gammaAlarm && !neutronAlarm) {
                gammaAlarmCount++;
            }   else if (!gammaAlarm && neutronAlarm) {
                neutronAlarmCount++;
            }

        }


        double totalAlarmingCount = gammaAlarmCount + neutronAlarmCount + gammaNeutronAlarmCount;
        alarmOccupancyAverage = Utils.calculateAlarmingOccRate(totalAlarmingCount, totalOccupancyCount);

        emlSuppressedAverage = Utils.calcEMLAlarmRate(emlSuppressedCount, totalAlarmingCount);


        alarmOccCounts.put("Gamma Alarm", gammaAlarmCount);
        alarmOccCounts.put("Neutron Alarm", neutronAlarmCount);
        alarmOccCounts.put("Gamma-Neutron Alarm", gammaNeutronAlarmCount);
        alarmOccCounts.put("EML Suppressed", emlSuppressedCount);
        alarmOccCounts.put("Total Occupancies", totalOccupancyCount);
        alarmOccCounts.put("Alarm Occupancy Rate", alarmOccupancyAverage);
        alarmOccCounts.put("EML Suppressed Rate", emlSuppressedAverage);

        tableGenerator.addTable(alarmOccCounts);

        document.add(new Paragraph("\n"));
    }

    private void addFaultStatistics(){
        document.add(new Paragraph("Fault Statistics"));

        HashMap<String, Double> faultCounts = new HashMap<>();

        double tamperCount = 0;
        double extendedOccupancyCount = 0;
        double commsCount = 0;
        double camCount = 0;
        double gammaHighFaultCount = 0;
        double gammaLowFaultCount = 0;
        double neutronHighFaultCount = 0;


        var query = module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withSystems(new SystemFilter.Builder().withUniqueIDs("urn:osh:system:" + laneId).build())
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(DEF_TAMPER)
                        .build())
                .build()).iterator();

        while (query.hasNext()) {
            var entry = query.next();

            var result = entry.getResult();

            var tamper = result.getBooleanValue(1);
            if(tamper){
                tamperCount++;
            }
        }

        var query2 = module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withSystems(new SystemFilter.Builder().withUniqueIDs("urn:osh:system:" + laneId).build())
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(DEF_ALARM, DEF_GAMMA)
                        .build())
                .build()).iterator();

        while (query2.hasNext()) {
            var entry = query2.next();
            var result = entry.getResult();
            var alarmState = result.getStringValue(1);

            if(alarmState == "Fault - Gamma High"){
                gammaHighFaultCount++;
            }
            else if(alarmState == "Fault - Gamma Low"){
                gammaLowFaultCount++;
            }
        }

        var query3 = module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withSystems(new SystemFilter.Builder().withUniqueIDs("urn:osh:system:" + laneId).build())
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(DEF_ALARM, DEF_NEUTRON)
                        .build())
                .build()).iterator();

        while (query3.hasNext()) {
            var entry = query2.next();
            var result = entry.getResult();
            var alarmState = result.getStringValue(1);

            if(alarmState == "Fault - Neutron High"){
                neutronHighFaultCount++;
            }
        }


        faultCounts.put("Tamper", tamperCount);
        faultCounts.put("Extended Occupancy", extendedOccupancyCount);
        faultCounts.put("Comm", commsCount);
        faultCounts.put("Camera", camCount);
        faultCounts.put("Gamma-High", gammaHighFaultCount);
        faultCounts.put("Gamma-Low", gammaLowFaultCount);
        faultCounts.put("Neutron-High", neutronHighFaultCount);

        tableGenerator.addTable(faultCounts);

        document.add(new Paragraph("\n"));
    }
}
