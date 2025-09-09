package com.botts.impl.system.database.types;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class RDSReport extends Report {
    String reportTitle = "RDS Site Report";

    String[] alarmOccupancyHeaders =  {
            "Neutron", "Gamma", "Gamma & Neutron", "EML Suppressed", "Total Occupancies",  "Daily Occupancy Average", "Speed (Avg)", "Alarm Rate", "EML Alarm Rate"
    };

    String[] faultHeaders =  {
            "Gamma-High", "Gamma-Low", "Neutron-High", "Tamper", "Extended Occupancy", "Comm", "Camera"
    };


    Document document;

    public RDSReport(Document document) {
        super(document);
    }

    @Override
    public void generate(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Site ID: " + "TODO: site id").setFontSize(12));
        document.add(new Paragraph("\n"));


        document.add(new Paragraph("Alarm & Occupancy Statistics").setFontSize(12));
//        addTableToPdf(alarmOccupancyHeaders, null);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Fault Statistics").setFontSize(12));
//        addTableToPdf(faultHeaders, null);
        document.add(new Paragraph("\n"));



//        TODO: Drive storage availability
        document.add(new Paragraph("Drive Storage Availability"));
    }

}
