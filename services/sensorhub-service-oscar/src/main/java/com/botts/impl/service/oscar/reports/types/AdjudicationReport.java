package com.botts.impl.service.oscar.reports.types;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class AdjudicationReport extends Report {
    String reportTitle = "Adjudication Report";

    Document document;

    String[] dispositionHeaders =  {
            "Real Alarm - Other", "False Alarm - Other", "Physical Inspection Negative", "Innocent Alarm - Medical Isotope Found", "Innocent Alarm - Declared Shipment of Radioactive Material", "No Disposition", "False Alarm - RIID/ASP Indicates Background Only", "Real Alarm - Contraband Found", "Tamper/Fault - Unauthorized Activity", "Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity", "Alarm - Naturally Occurring Radioactive Material (NORM) Found"
    };

    String[] secInsHeaders = {
//            "Primary Date", "Primary Time", "EventReport Record ID", "Primary N.Sigma", "SI Result", "Cargo", "Disposition #"
    };

    public AdjudicationReport(Document document, String startTime, String endTime) {
        super(document, startTime, endTime);
    }

    @Override
    public void generate() {
        addHeader();
        addDisposition();
        addSecondaryInspectionResults();
        addSecondaryInspectionDetails();
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
