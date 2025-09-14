package com.botts.impl.service.oscar.reports.types;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class LaneReport extends Report {

    String reportTitle = "Lane Report";

    Document document;

    public LaneReport(Document document, String startTime, String endTime) {
        super(document, startTime, endTime);
    }

    @Override
    public void generate() {
        addHeader();

        // check for data before calling these
        addAlarmStatistics();
        addFaultStatistics();
        addStatisics();
        addOccupancyStatistics();

    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane Name: " + "TODO: lane name").setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addOccupancyStatistics(){
        document.add(new Paragraph("Occupancy Statistics").setFontSize(12));
//        addTableToPdf(alarmOccupancyHeaders, null);

    }

    private void addAlarmStatistics(){
        document.add(new Paragraph("Alarm Statistics").setFontSize(12));
        //        addTableToPdf(alarmOccupancyHeaders, null);

    }

    private void addFaultStatistics(){
        document.add(new Paragraph("Fault Statistics").setFontSize(12));
        //        addTableToPdf(alarmOccupancyHeaders, null);

    }

    private void addStatisics(){
        document.add(new Paragraph("Statisics").setFontSize(12));
        //        addTableToPdf(alarmOccupancyHeaders, null);

    }
}
