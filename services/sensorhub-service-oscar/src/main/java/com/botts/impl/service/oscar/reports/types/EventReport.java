package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.*;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.sensorhub.impl.utils.rad.RADHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventReport extends Report {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    Document document;
    PdfDocument pdfDocument;

    TableGenerator tableGenerator;
    ChartGenerator chartGenerator;

    String laneUID;
    EventReportType eventType;

    private record DatasetResult(DefaultCategoryDataset dataset, Map<String, String> tableData) {}

    public EventReport(OutputStream outputStream, Instant startTime, Instant endTime, EventReportType eventType, String laneUID, OSCARServiceModule module) {
        super(outputStream, startTime, endTime, module);

        pdfDocument = new PdfDocument(new PdfWriter(outputStream));
        document = new Document(pdfDocument);

        this.eventType = eventType;
        this.laneUID = laneUID;
        this.tableGenerator = new TableGenerator();
        this.chartGenerator = new ChartGenerator(module);
    }

    @Override
    public void generate() {
        addHeader();

        if (eventType.equals(EventReportType.ALARMS_OCCUPANCIES))
            addAlarmOccStatisticsByDay();

        else if (eventType.equals(EventReportType.ALARMS))
            addAlarmStatisticsByDay();

        else if (eventType.equals(EventReportType.SOH))
            addFaultStatisticsByDay();

        document.close();
        chartGenerator = null;
        tableGenerator = null;
    }

    @Override
    public String getReportType() {
        return ReportCmdType.EVENT.name();
    }
    private void addHeader(){
        document.add(new Paragraph("Event Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Event Type:" + eventType).setFontSize(12));
        document.add(new Paragraph("Lane UIDs:" + laneUID).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addAlarmStatisticsByDay(){
        document.add(new Paragraph("Alarm Statistics").setFontSize(12));

        DatasetResult result = buildAlarmingDatasetAndTable();
        DefaultCategoryDataset dataset = result.dataset();
        Map<String, String> tableData = result.tableData();

        String title = "Alarms";
        String xAxis = "Dates";
        String yAxis = "Counts";

        try{
            var chart = chartGenerator.createStackedBarChart(
                    title,
                    xAxis,
                    yAxis,
                    dataset
            );

            if(chart == null){
                document.add(new Paragraph("Alarm Chart failed to create"));
                return;
            }

            BufferedImage bufferedImage = chart.createBufferedImage(1200, 600);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            Image image = new Image(ImageDataFactory.create(imageBytes)).setAutoScale(true);

            document.add(image);
            document.add(new Paragraph("\n"));

            Table table = tableGenerator.addTable(tableData);
            document.add(table);
            document.add(new Paragraph("\n"));
        } catch (IOException e) {
            module.getLogger().error("Error creating Alarm chart", e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

    private void addFaultStatisticsByDay(){

        Map<Instant, Long> gammaHighDaily = Utils.countObservationsByDay(module, Utils.gammaHighPredicate, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        Map<Instant, Long> gammaLowDaily = Utils.countObservationsByDay(module, Utils.gammaLowPredicate, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        Map<Instant, Long> neutronHighDaily = Utils.countObservationsByDay(module, Utils.neutronHighPredicate, start, end, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);
//        Map<Instant, Long> extendedOccupancyDaily = Utils.countObservationsByDay(module, Utils.extendedOccPredicate, start, end, RADHelper.DEF_OCCUPANCY);
        Map<Instant, Long> tamperDaily = Utils.countObservationsByDay(module, Utils.tamperPredicate, start, end, RADHelper.DEF_TAMPER);
//        Map<Instant, Long> commDaily = Utils.countObservationsByDay(module, Utils.commsPredicate, start, end, RADHelper.DEF_OCCUPANCY);
//        Map<Instant, Long> cameraDaily = Utils.countObservationsByDay(module, Utils.cameraPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        Map<String, String> tableData = new LinkedHashMap<>();

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for(Map.Entry<Instant, Long> entry : gammaHighDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Gamma High",  formatter.format(entry.getKey()));
            tableData.put("Gamma High", entry.getValue().toString());
        }

        for(Map.Entry<Instant, Long> entry : gammaLowDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Gamma Low",  formatter.format(entry.getKey()));
            tableData.put("Gamma Low", entry.getValue().toString());
        }
        for(Map.Entry<Instant, Long> entry : neutronHighDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Neutron High",  formatter.format(entry.getKey()));
            tableData.put("Neutron High", entry.getValue().toString());
        }

        for(Map.Entry<Instant, Long> entry : tamperDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Tamper",  formatter.format(entry.getKey()));
            tableData.put("Tamper", entry.getValue().toString());
        }

//        for(Map.Entry<Instant, Long> entry : extendedOccupancyDaily.entrySet()){
//            dataset.addValue(entry.getValue(), "Extended Occupancy",  formatter.format(entry.getKey()));
//        }
//
//        for(Map.Entry<Instant, Long> entry : cameraDaily.entrySet()){
//            dataset.addValue(entry.getValue(), "Camera",  formatter.format(entry.getKey()));
//        }
//
//        for(Map.Entry<Instant, Long> entry : commDaily.entrySet()){
//            dataset.addValue(entry.getValue(), "Comm",  formatter.format(entry.getKey()));
//        }

        String title = "SOH";
        String xAxis = "Date";
        String yAxis = "Count";

        try{
            var chart = chartGenerator.createStackedBarChart(
                    title,
                    xAxis,
                    yAxis,
                    dataset
            );

            if(chart == null){
                document.add(new Paragraph("SOH Chart failed to create"));
                return;
            }


            BufferedImage bufferedImage = chart.createBufferedImage(1200, 600);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            Image image = new Image(ImageDataFactory.create(imageBytes)).setAutoScale(true);

            document.add(image);
            document.add(new Paragraph("\n"));
            Table table = tableGenerator.addTable(tableData);
            document.add(table);
            document.add(new Paragraph("\n"));

        } catch (IOException e) {
            module.getLogger().error("Error creating SOH chart", e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

    private void addAlarmOccStatisticsByDay(){

        Map<Instant, Long> totalOccupancyDaily = Utils.countObservationsByDay(module, Utils.occupancyTotalPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        DatasetResult result = buildAlarmingDatasetAndTable();
        DefaultCategoryDataset dataset = result.dataset();
        Map<String, String> tableData = result.tableData();


        // this will be a linegraph on top of the bar chart
        DefaultCategoryDataset occDataset = new DefaultCategoryDataset();

        for(Map.Entry<Instant, Long> entry : totalOccupancyDaily.entrySet()){
            occDataset.addValue(entry.getValue(), "TotalOccupancy",  formatter.format(entry.getKey()));
            tableData.put("Total Occupancy", entry.getValue().toString());
        }

        String title = "Alarms and Occupancies";
        String xAxis = "Date";
        String yAxis = "Count";

        try{
            var chart = chartGenerator.createStackedBarLineOverlayChart(
                    title,
                    xAxis,
                    yAxis,
                    dataset,
                    occDataset
            );

            if(chart == null){
                document.add(new Paragraph("Alarm-Occupancy Chart failed to create"));
                module.getLogger().error("Chart failed to create");
                return;
            }


            BufferedImage bufferedImage = chart.createBufferedImage(1200, 600);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            Image image = new Image(ImageDataFactory.create(imageBytes)).setAutoScale(true);

            document.add(image);
            document.add(new Paragraph("\n"));

            Table table = tableGenerator.addTable(tableData);
            document.add(table);
            document.add(new Paragraph("\n"));
        } catch (IOException e) {
            module.getLogger().error("Error creating Alarm-Occupancy chart", e);
            return;
        }

        document.add(new Paragraph("\n"));
    }


    private DatasetResult buildAlarmingDatasetAndTable() {
        Map<Instant, Long> gammaDaily = Utils.countObservationsByDay(module, Utils.gammaPredicate, start, end, RADHelper.DEF_OCCUPANCY);
        Map<Instant, Long> neutronDaily = Utils.countObservationsByDay(module, Utils.neutronPredicate, start, end, RADHelper.DEF_OCCUPANCY);
        Map<Instant, Long> gammaNeutronDaily = Utils.countObservationsByDay(module, Utils.gammaNeutronPredicate, start, end, RADHelper.DEF_OCCUPANCY);
        Map<Instant, Long> emlSuppressedDaily = Utils.countObservationsByDay(module, Utils.emlSuppressedPredicate, start, end, RADHelper.DEF_EML_ANALYSIS);


        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, String> tableData = new LinkedHashMap<>();

        for (Map.Entry<Instant, Long> entry : gammaDaily.entrySet()) {
            dataset.addValue(entry.getValue(), "Gamma", formatter.format(entry.getKey()));
            tableData.put("Gamma", entry.getValue().toString());
        }

        for (Map.Entry<Instant, Long> entry : gammaNeutronDaily.entrySet()) {
            dataset.addValue(entry.getValue(), "Gamma-Neutron", formatter.format(entry.getKey()));
            tableData.put("Gamma-Neutron", entry.getValue().toString());
        }

        for (Map.Entry<Instant, Long> entry : neutronDaily.entrySet()) {
            dataset.addValue(entry.getValue(), "Neutron", formatter.format(entry.getKey()));
            tableData.put("Neutron", entry.getValue().toString());
        }

        for (Map.Entry<Instant, Long> entry : emlSuppressedDaily.entrySet()) {
            dataset.addValue(entry.getValue(), "EML-Suppressed", formatter.format(entry.getKey()));
            tableData.put("EML-Suppressed", entry.getValue().toString());
        }

        return new DatasetResult(dataset, tableData);
    }
}