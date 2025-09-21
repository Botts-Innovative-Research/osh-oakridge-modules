package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ChartGenerator;
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

public class AdjudicationReport extends Report {
    String reportTitle = "Adjudication Report";
    Document document;
    PdfWriter pdfWriter;
    PdfDocument pdfDocument;
    String pdfFileName;

    OSCARServiceModule module;
    TableGenerator tableGenerator;
    ChartGenerator chartGenerator;

//    String[] dispositionHeaders =  {
//            "Real Alarm - Other", "False Alarm - Other", "Physical Inspection Negative", "Innocent Alarm - Medical Isotope Found", "Innocent Alarm - Declared Shipment of Radioactive Material", "No Disposition", "False Alarm - RIID/ASP Indicates Background Only", "Real Alarm - Contraband Found", "Tamper/Fault - Unauthorized Activity", "Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity", "Alarm - Naturally Occurring Radioactive Material (NORM) Found"
//    };
//
//    String[] secInsHeaders = {
//            "Primary Date", "Primary Time", "EventReport Record ID", "Primary N.Sigma", "SI Result", "Cargo", "Disposition #"
//    };

    public AdjudicationReport(Instant startTime, Instant endTime, String eventId, String laneId, OSCARServiceModule module) {
        try {
            pdfFileName = ReportCmdType.ADJUDICATION.name() + "_" + startTime + "_"+ endTime + ".pdf";
            File file = new File("files/reports/" + pdfFileName);
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
        addDisposition();
        addSecondaryInspectionResults();
        addSecondaryInspectionDetails();

        document.close();
        pdfDocument.close();

        return pdfFileName;
    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("\n"));
    }

    private void addDisposition(){
        document.add(new Paragraph("Disposition").setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addSecondaryInspectionResults(){
        document.add(new Paragraph("Secondary Inspection Results").setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addSecondaryInspectionDetails(){
        document.add(new Paragraph("Secondary Inspection Details").setFontSize(12));
        document.add(new Paragraph("\n"));
    }
}
