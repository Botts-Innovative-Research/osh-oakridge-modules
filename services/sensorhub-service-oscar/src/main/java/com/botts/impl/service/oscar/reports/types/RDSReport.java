package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.reports.helpers.ReportType;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class RDSReport extends Report {
    String reportTitle = "RDS Site Report";


    String[] alarmOccupancyHeaders =  {
            "Neutron", "Gamma", "Gamma & Neutron", "EML Suppressed", "Total Occupancies",  "Daily Occupancy Average", "Speed (Avg)", "Alarm Rate", "EML Alarm Rate"
    };

    String[] faultHeaders =  {
            "Gamma-High", "Gamma-Low", "Neutron-High", "Tamper", "Extended Occupancy", "Comm", "Camera"
    };


    Document document;
    PdfWriter pdfWriter;
    PdfDocument pdfDocument;
    String pdfFileName;

    String siteId;

    public RDSReport(Instant startTime, Instant endTime) {
        try {
            pdfFileName = ReportType.RDS_SITE.name()+ "_" + startTime + "_"+ endTime + ".pdf";
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
    public String generate(){

        addHeader();

        addAlarmStatistics();
        addFaultStatistics();
        addStatistics();
        addOccupancyStatistics();
        addDriveStorageAvailability();

        document.close();

        pdfDocument.close();

        return pdfFileName;

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
