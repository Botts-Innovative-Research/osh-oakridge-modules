package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.reports.helpers.ReportType;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class LaneReport extends Report {

    String reportTitle = "Lane Report";
    Document document;
    PdfWriter pdfWriter;
    PdfDocument pdfDocument;
    String pdfFileName;

    public LaneReport(Instant startTime, Instant endTime, String laneId) {
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
    }

    @Override
    public String generate() {

        addHeader();

        // check for data before calling these
        addAlarmStatistics();
        addFaultStatistics();
        addStatisics();
        addOccupancyStatistics();

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
