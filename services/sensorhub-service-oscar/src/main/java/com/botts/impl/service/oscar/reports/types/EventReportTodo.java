package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.sensorhub.api.database.IDatabaseRegistry;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class EventReportTodo extends Report {

    String reportTitle = "Event Report";

    Document document;
    PdfWriter pdfWriter;
    PdfDocument pdfDocument;
    String pdfFileName;

    OSCARServiceModule module;
    TableGenerator tableGenerator;

    public EventReportTodo(Instant startTime, Instant endTime, String eventID, String laneId, OSCARServiceModule module) {
        try {

            pdfFileName = ReportCmdType.EVENT.name()+ "_" + laneId + "_" + eventID + "_" + startTime + "_"+ endTime + ".pdf";
            File file = new File( "files/reports/" + pdfFileName);
            file.getParentFile().mkdirs();

            pdfWriter  = new PdfWriter(file);
            pdfDocument = new PdfDocument(pdfWriter);
            document = new Document(pdfDocument);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.module = module;
        tableGenerator = new TableGenerator(document);
    }

    @Override
    public String generate() {
        addHeader();

        // check for data before calling these
//        addAlarmStatistics();
//        addFaultStatistics();
//        addStatistics();
//        addOccupancyStatistics();

        document.close();
        pdfDocument.close();

        return pdfFileName;
    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Event Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane ID:" + "TODO: lane name").setFontSize(12));
        document.add(new Paragraph("Event ID:" + "TODO: lane name").setFontSize(12));
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

    private void addStatistics(){
        document.add(new Paragraph("Statistics").setFontSize(12));
        //        addTableToPdf(alarmOccupancyHeaders, null);

    }
}
