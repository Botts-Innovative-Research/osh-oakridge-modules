package com.botts.impl.service.oscar.reports.types;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class RDSReport extends Report {
    String reportTitle = "RDS Site Report";

    String siteId = "";

    String[] alarmOccupancyHeaders =  {
            "Neutron", "Gamma", "Gamma & Neutron", "EML Suppressed", "Total Occupancies",  "Daily Occupancy Average", "Speed (Avg)", "Alarm Rate", "EML Alarm Rate"
    };

    String[] faultHeaders =  {
            "Gamma-High", "Gamma-Low", "Neutron-High", "Tamper", "Extended Occupancy", "Comm", "Camera"
    };


    Document document;

    public RDSReport(Document document, String startTime, String endTime) {
        super(document, startTime, endTime);
    }

    @Override
    public void generate(){
        addHeader();

        addAlarmStatistics();
        addFaultStatistics();
        addStatistics();
        addOccupancyStatistics();
        addDriveStorageAvailability();

    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Site ID: " + siteId).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addAlarmStatistics(){
        document.add(new Paragraph("Alarm Statistics"));
        //        addTableToPdf(faultHeaders, null);

        document.add(new Paragraph("\n"));
    }

    private void addFaultStatistics(){
        document.add(new Paragraph("Fault Statistics"));
        //        addTableToPdf(faultHeaders, null);

        document.add(new Paragraph("\n"));
    }

    private void addOccupancyStatistics(){
        document.add(new Paragraph("Occupancy Statistics"));
        //        addTableToPdf(faultHeaders, null);

        document.add(new Paragraph("\n"));
    }

    private void addStatistics() {
        document.add(new Paragraph("Statistics").setFontSize(12));
        //        addTableToPdf(faultHeaders, null);

        document.add(new Paragraph("\n"));
    }

    private void addDriveStorageAvailability(){
        document.add(new Paragraph("Drive Storage Availability"));
    }
}
