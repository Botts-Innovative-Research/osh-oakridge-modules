package com.botts.impl.system.database.types;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class AlarmEventReport extends Report {
    String reportTitle = "Alarm EventReport Report";

    Document document;

    public AlarmEventReport(Document document){
        super(document);
    }

    @Override
    public void generate() {
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Lane Name: " + "TODO: lane name").setFontSize(12));
        document.add(new Paragraph("Occupancy ID: " + "TODO: occupancy ID").setFontSize(12));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Images").setFontSize(12));

        document.add(new Paragraph("Alarm Chart").setFontSize(12));

        document.add(new Paragraph("Adjudication Notes:").setFontSize(12));

        document.add(new Paragraph("Attached Files:").setFontSize(12));
    }
}
