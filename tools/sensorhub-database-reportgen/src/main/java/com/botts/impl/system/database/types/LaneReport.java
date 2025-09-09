package com.botts.impl.system.database.types;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class LaneReport extends Report {

    String reportTitle = "Lane Report";

    Document document;

    public LaneReport(Document document) {
       super(document);
    }

    @Override
    public void generate() {
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane Name: " + "TODO: lane name").setFontSize(12));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Alarm & Occupancy Statistics").setFontSize(12));
//        addTableToPdf(alarmOccupancyHeaders, null);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Fault Statistics").setFontSize(12));
//        addTableToPdf(faultHeaders, null);
        document.add(new Paragraph("\n"));
    }

}
