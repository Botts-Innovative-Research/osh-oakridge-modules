package com.botts.impl.system.database;


import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.sensorhub.api.database.IObsSystemDatabase;

import java.io.File;
import java.io.IOException;


/**
 **
 * @author Kalyn Stricklin
 * @since Sept 05, 2025
 */
public class ReportGenerator {

    private ChartGenerator chartGenerator;
    private IObsSystemDatabase database;
    private Report report;

    String[] alarmOccupancyHeaders =  {
            "Neutron", "Gamma", "Gamma & Neutron", "EML Suppressed", "Total Occupancies",  "Daily Occupancy Average", "Speed (Avg)", "Alarm Rate", "EML Alarm Rate"
    };

    String[] faultHeaders =  {
            "Gamma-High", "Gamma-Low", "Neutron-High", "Tamper", "Extended Occupancy", "Comm", "Camera"
    };

    String[] dispositionHeaders =  {
            "Real Alarm - Other", "False Alarm - Other", "Physical Inspection Negative", "Innocent Alarm - Medical Isotope Found", "Innocent Alarm - Declared Shipment of Radioactive Material", "No Disposition", "False Alarm - RIID/ASP Indicates Background Only", "Real Alarm - Contraband Found", "Tamper/Fault - Unauthorized Activity", "Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity", "Alarm - Naturally Occurring Radioactive Material (NORM) Found"
    };

    String[] secInsHeaders = {
//            "Primary Date", "Primary Time", "Event Record ID", "Primary N.Sigma", "SI Result", "Cargo", "Disposition #"
    };

    public static final String destination = "/files/pdfTest.pdf";

    Document document;

    public ReportGenerator(IObsSystemDatabase database) {
        this.database = database;
        chartGenerator = new ChartGenerator();
    }

    public double calcEMLAlarmRate(int emlSupp, int alarm){
        return emlSupp / (emlSupp + alarm);
    }

    public double calculateAlarmingOccupancyPercentage(int alarm, int occupancyCount){
        return alarm / occupancyCount;
    }

    public void generateReport(Report report) throws IOException {

        File file = new File(destination);
        file.getParentFile().mkdirs();

        PdfDocument pdf = new PdfDocument(new PdfWriter(file));

        document = new Document(pdf);

       generateHeader();

       // add details
        switch(report.getReportType()){
            case RDS_SITE_REPORT:
                generateRdsSiteReport();
                break;
            case LANE_REPORT:
                generateLaneReport();
                break;
            case ALARM_EVENT_REPORT:
                generateAlarmEventReport();
                break;
            case EVENT_REPORT:
                generateEventReport();
                break;
            case OPERATIONS_REPORT:
                generateOperationsReport();
                break;
        }

       generateFooter();


        System.out.println("pdf created");
    }


    protected void addTableToPdf(String[] headers, Object dataset){
        Table table = new Table(UnitValue.createPercentArray(8)).useAllAvailableWidth();

        for(var header : headers){
            table.addHeaderCell(header);
        }

        for(int i = 0; i < 10; i++){
            table.addCell("cell example");
        }

        document.add(table);
    }

    protected void addImageToPdf(String imgPath){

        try{
            File file = new File(imgPath);
            if(file.exists()){
                Image image = new Image(ImageDataFactory.create(imgPath));
                image.scaleToFit(500, 300);
                document.add(image);
                file.delete();
            }
        }catch(Exception e){
            System.out.println("Error while adding image to pdf");
        }
    }


    private void generateRdsSiteReport(){
        document.add(new Paragraph("RDS Site Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Site ID: " + "TODO: site id").setFontSize(12));
        document.add(new Paragraph("\n"));


        document.add(new Paragraph("Alarm & Occupancy Statistics").setFontSize(12));
        addTableToPdf(alarmOccupancyHeaders, null);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Fault Statistics").setFontSize(12));
        addTableToPdf(faultHeaders, null);
        document.add(new Paragraph("\n"));



//        TODO: Drive storage availability
        document.add(new Paragraph("Drive Storage Availability"));
    }

    private void generateLaneReport(){
        document.add(new Paragraph("Lane Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane Name: " + "TODO: lane name").setFontSize(12));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Alarm & Occupancy Statistics").setFontSize(12));
        addTableToPdf(alarmOccupancyHeaders, null);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Fault Statistics").setFontSize(12));
        addTableToPdf(faultHeaders, null);
        document.add(new Paragraph("\n"));

    }

    private void generateAlarmEventReport(){
        document.add(new Paragraph("Alarm Event Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Lane Name: " + "TODO: lane name").setFontSize(12));
        document.add(new Paragraph("Occupancy ID: " + "TODO: occupancy ID").setFontSize(12));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Images").setFontSize(12));

        document.add(new Paragraph("Alarm Chart").setFontSize(12));

        document.add(new Paragraph("Adjudication Notes:").setFontSize(12));

        document.add(new Paragraph("Attached Files:").setFontSize(12));
    }

    private void generateEventReport(){
        document.add(new Paragraph("Event Report").setFontSize(16).simulateBold());
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


    private void generateOperationsReport(){
        document.add(new Paragraph("Operations Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Disposition").setFontSize(12));
        // chart
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Secondary Inspection Results").setFontSize(12));
        // add chart
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Secondary Inspection Details").setFontSize(12));
        addTableToPdf(secInsHeaders, null);
        document.add(new Paragraph("\n"));

    }


    private void generateHeader(){
        document.add(new Paragraph("-- Start of Report --")
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void generateFooter(){
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("-- End of Report --")
                .setTextAlignment(TextAlignment.CENTER));

        document.close();
    }

}



