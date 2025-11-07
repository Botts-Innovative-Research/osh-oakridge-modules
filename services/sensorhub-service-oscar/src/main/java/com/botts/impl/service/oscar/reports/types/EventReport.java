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
import org.sensorhub.api.data.IObsData;
import org.sensorhub.impl.utils.rad.RADHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class EventReport extends Report {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    Document document;
    PdfDocument pdfDocument;

    TableGenerator tableGenerator;
    ChartGenerator chartGenerator;

    String laneUIDs;
    EventReportType eventType;

    private record DatasetResult(DefaultCategoryDataset dataset, Map<String, Map<String, String>> tableData) {}

    public EventReport(OutputStream outputStream, Instant startTime, Instant endTime, EventReportType eventType, String laneUIDs, OSCARServiceModule module) {
        super(outputStream, startTime, endTime, module);

        pdfDocument = new PdfDocument(new PdfWriter(outputStream));
        document = new Document(pdfDocument);

        this.eventType = eventType;
        this.laneUIDs = laneUIDs;
        this.tableGenerator = new TableGenerator();
        this.chartGenerator = new ChartGenerator(module);

//        ZoneId systemZone = ZoneId.systemDefault();
//
//        ZonedDateTime localStart = start.atZone(systemZone).truncatedTo(ChronoUnit.DAYS);
//        ZonedDateTime localEnd = end.atZone(systemZone);
//
//        if (localEnd.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
//            localEnd = localEnd.plusDays(1).minusNanos(1);
//        }

        this.start = startTime;
        this.end = endTime;
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
        document.add(new Paragraph("Event Report ").setFontSize(16).simulateBold());
        document.add(new Paragraph("Event Type: " + eventType).setFontSize(12));
        document.add(new Paragraph("Requested Time: " + start + "-" + end).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addAlarmStatisticsByDay() {
        document.add(new Paragraph("Alarm Statistics").setFontSize(12));

        DatasetResult result = buildAlarmingDatasetAndTable();
        DefaultCategoryDataset dataset = result.dataset();
        Map<String, Map<String, String>> tableData = result.tableData();

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

            if (chart == null) {
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

            Table table = tableGenerator.addTableByDate(tableData);
            document.add(table);
            document.add(new Paragraph("\n"));
        } catch (IOException e) {
            module.getLogger().error("Error creating Alarm chart", e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

    private void addFaultStatisticsByDay(){

        Map<Instant, Long> gammaHighDaily = Utils.countObservationsByDay(laneUIDs, module, Utils.gammaHighPredicate, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        Map<Instant, Long> gammaLowDaily = Utils.countObservationsByDay(laneUIDs, module, Utils.gammaLowPredicate, start, end, RADHelper.DEF_GAMMA, RADHelper.DEF_ALARM);
        Map<Instant, Long> neutronHighDaily = Utils.countObservationsByDay(laneUIDs, module, Utils.neutronHighPredicate, start, end, RADHelper.DEF_NEUTRON, RADHelper.DEF_ALARM);
        Map<Instant, Long> tamperDaily = Utils.countObservationsByDay(laneUIDs, module, Utils.tamperPredicate, start, end, RADHelper.DEF_TAMPER);
//        Map<Instant, Long> extendedOccupancyDaily = Utils.countObservationsByDay(module, Utils.extendedOccPredicate, start, end, RADHelper.DEF_OCCUPANCY);
//        Map<Instant, Long> commDaily = Utils.countObservationsByDay(module, Utils.commsPredicate, start, end, RADHelper.DEF_OCCUPANCY);
//        Map<Instant, Long> cameraDaily = Utils.countObservationsByDay(module, Utils.cameraPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        Map<String, Map<String, String>> tableDataByDate = new LinkedHashMap<>();

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for(Map.Entry<Instant, Long> entry : gammaHighDaily.entrySet()){
            String date = formatter.format(entry.getKey());
            dataset.addValue(entry.getValue(), "Gamma High", date);
            tableDataByDate.computeIfAbsent(date, k -> new LinkedHashMap<>()).put("Gamma High", entry.getValue().toString());
        }

        for(Map.Entry<Instant, Long> entry : gammaLowDaily.entrySet()){
            String date = formatter.format(entry.getKey());
            dataset.addValue(entry.getValue(), "Gamma Low", date);
            tableDataByDate.computeIfAbsent(date, k -> new LinkedHashMap<>()).put("Gamma Low", entry.getValue().toString());
        }

        for(Map.Entry<Instant, Long> entry : neutronHighDaily.entrySet()){
            String date = formatter.format(entry.getKey());
            dataset.addValue(entry.getValue(), "Neutron High", date);
            tableDataByDate.computeIfAbsent(date, k -> new LinkedHashMap<>()).put("Neutron High", entry.getValue().toString());
        }

        for(Map.Entry<Instant, Long> entry : tamperDaily.entrySet()){
            String date = formatter.format(entry.getKey());
            dataset.addValue(entry.getValue(), "Tamper", date);
            tableDataByDate.computeIfAbsent(date, k -> new LinkedHashMap<>()).put("Tamper", entry.getValue().toString());
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
            Table table = tableGenerator.addTableByDate(tableDataByDate);
            document.add(table);
            document.add(new Paragraph("\n"));

        } catch (IOException e) {
            module.getLogger().error("Error creating SOH chart", e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

    private void addAlarmOccStatisticsByDay(){

        Map<Instant, Long> totalOccupancyDaily = Utils.countObservationsByDay(laneUIDs, module, Utils.occupancyTotalPredicate, start, end, RADHelper.DEF_OCCUPANCY);

        DatasetResult result = buildAlarmingDatasetAndTable();
        DefaultCategoryDataset dataset = result.dataset();
        Map<String, Map<String, String>> tableData = result.tableData();


        // this will be a linegraph on top of the bar chart
        DefaultCategoryDataset occDataset = new DefaultCategoryDataset();

        for(Map.Entry<Instant, Long> entry : totalOccupancyDaily.entrySet()){
            String date = formatter.format(entry.getKey());

            occDataset.addValue(entry.getValue(), "TotalOccupancy", date);
            tableData.computeIfAbsent(date, k -> new LinkedHashMap<>()).put("Total Occupancy", entry.getValue().toString());
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

            Table table = tableGenerator.addTableByDate(tableData);
            document.add(table);
            document.add(new Paragraph("\n"));
        } catch (IOException e) {
            module.getLogger().error("Error creating Alarm-Occupancy chart", e);
            return;
        }

        document.add(new Paragraph("\n"));
    }


//        Map<Instant, Long> gammaDaily = new LinkedHashMap<>();
//        Map<Instant, Long> neutronDaily  = new LinkedHashMap<>();
//        Map<Instant, Long> gammaNeutronDaily  = new LinkedHashMap<>();
//        Map<Instant, Long> emlSuppressedDaily = new LinkedHashMap<>();
//
//        var startTime = start;
//        var endTime = end;
//
//        while (startTime.isBefore(endTime)) {
//            Instant currentDay = startTime;
//            Instant endOfCurrentDay = currentDay.plus(1, ChronoUnit.DAYS);
//
//            if(endOfCurrentDay.isAfter(endTime)){
//                endOfCurrentDay = endTime;
//            }
//
//            var query = Utils.getQuery(module, laneUIDs, currentDay, endOfCurrentDay, RADHelper.DEF_OCCUPANCY);
//
//            long gammaCount = 0;
//            long neutronCount = 0;
//            long emlSuppressedCount = 0;
//            long gammaNeutronCount = 0;
//
//            while (query.hasNext()) {
//                var entry = query.next();
//                var result = entry.getResult();
//
//                var gamma = result.getBooleanValue(4);
//                var neutron = result.getBooleanValue(5);
//
//                if (gamma && neutron) {
//                    gammaNeutronCount++;
//                } else if (gamma && !neutron) {
//                    gammaCount++;
//                } else if (!gamma && neutron) {
//                    neutronCount++;
//                }
//            }
//
//            var query2 = Utils.getQuery(module, laneUIDs, currentDay, endOfCurrentDay, RADHelper.DEF_EML_ANALYSIS);
//
//            while (query2.hasNext()) {
//                var entry = query2.next();
//                var result = entry.getResult();
//
//                if (result.getStringValue(0).equals("RELEASE")) {
//                    emlSuppressedCount++;
//                }
//            }
//
//            gammaDaily.put(currentDay, gammaCount);
//            neutronDaily.put(currentDay, neutronCount);
//            emlSuppressedDaily.put(currentDay, emlSuppressedCount);
//            gammaNeutronDaily.put(currentDay, gammaNeutronCount);
//
//            startTime = endOfCurrentDay;
//        }



    private DatasetResult buildAlarmingDatasetAndTable() {

        Map<Instant, Long> gammaDaily = Utils.countObservationsByDay(laneUIDs, module, Utils.gammaPredicate, start, end, RADHelper.DEF_OCCUPANCY);
        Map<Instant, Long> neutronDaily = Utils.countObservationsByDay(laneUIDs, module,  Utils.neutronPredicate, start, end, RADHelper.DEF_OCCUPANCY);
        Map<Instant, Long> gammaNeutronDaily = Utils.countObservationsByDay(laneUIDs, module,  Utils.gammaNeutronPredicate, start, end, RADHelper.DEF_OCCUPANCY);
        Map<Instant, Long> emlSuppressedDaily = Utils.countObservationsByDay(laneUIDs, module,  Utils.emlSuppressedPredicate, start, end, RADHelper.DEF_EML_ANALYSIS);

        
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, Map<String, String>> tableDataByDate = new LinkedHashMap<>();

        for (Map.Entry<Instant, Long> entry : gammaDaily.entrySet()) {
            String date = formatter.format(entry.getKey());

            dataset.addValue(entry.getValue(), "Gamma", date);
            tableDataByDate.computeIfAbsent(date, k -> new LinkedHashMap<>()).put("Gamma", entry.getValue().toString());
        }

        for (Map.Entry<Instant, Long> entry : gammaNeutronDaily.entrySet()) {
            String date = formatter.format(entry.getKey());

            dataset.addValue(entry.getValue(), "Gamma-Neutron", date);
            tableDataByDate.computeIfAbsent(date, k -> new LinkedHashMap<>()).put("Gamma-Neutron", entry.getValue().toString());
        }

        for (Map.Entry<Instant, Long> entry : neutronDaily.entrySet()) {
            String date = formatter.format(entry.getKey());

            dataset.addValue(entry.getValue(), "Neutron", date);
            tableDataByDate.computeIfAbsent(date, k -> new LinkedHashMap<>()).put("Neutron", entry.getValue().toString());
        }

        for (Map.Entry<Instant, Long> entry : emlSuppressedDaily.entrySet()) {
            String date = formatter.format(entry.getKey());

            dataset.addValue(entry.getValue(), "EML-Suppressed", date);
            tableDataByDate.computeIfAbsent(date, k -> new LinkedHashMap<>()).put("EML-Suppressed", entry.getValue().toString());
        }

        return new DatasetResult(dataset, tableDataByDate);
    }
}