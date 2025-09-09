package com.botts.impl.system.database.types;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class EventReport extends Report {
    String reportTitle = "Event Report";

    Document document;

    public EventReport(Document document) {
        super(document);
    }


    @Override
    public void generate() {
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Site ID: " + "TODO: site id").setFontSize(12));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Chart by Lane").setFontSize(12));
//        document.add(chartGenerator.createChart("Site ID: ", "Time", "CPS", null, "line"));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("SOH").setFontSize(12));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Occupancy").setFontSize(12));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Alarms").setFontSize(12));
        document.add(new Paragraph("\n"));


        document.add(new Paragraph("Alarms & Occupancies").setFontSize(12));
        document.add(new Paragraph("\n"));
    }
}
