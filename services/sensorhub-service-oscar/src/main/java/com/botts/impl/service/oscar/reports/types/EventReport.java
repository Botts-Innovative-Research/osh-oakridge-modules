package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.*;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
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
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Predicate;

public class EventReport extends Report {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    Document document;
    PdfDocument pdfDocument;

    OSCARServiceModule module;
    TableGenerator tableGenerator;
    ChartGenerator chartGenerator;

    String laneUID;
    EventReportType eventType;
    Instant start;
    Instant end;
    String[] observedProperties = new String[]{RADHelper.DEF_OCCUPANCY};

    public EventReport(OutputStream outputStream, Instant startTime, Instant endTime, EventReportType eventType, String laneUID, OSCARServiceModule module) {
        pdfDocument = new PdfDocument(new PdfWriter(outputStream));
        document = new Document(pdfDocument);

        this.eventType = eventType;
        this.laneUID = laneUID;
        this.module = module;
        this.start = startTime;
        this.end = endTime;
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

    private void addHeader(){
        document.add(new Paragraph("Event Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Event Type:" + eventType).setFontSize(12));
        document.add(new Paragraph("Lane UIDs:" + laneUID).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addAlarmStatisticsByDay(){
        document.add(new Paragraph("Alarm Statistics").setFontSize(12));

        Map<Instant, Long> gammaDaily = Utils.countObservationsByDay(observedProperties, module, Utils.gammaPredicate, start, end);
        Map<Instant, Long> neutronDaily = Utils.countObservationsByDay(observedProperties, module, Utils.neutronPredicate, start, end);
        Map<Instant, Long> gammaNeutronDaily = Utils.countObservationsByDay(observedProperties, module, Utils.gammaNeutronPredicate, start, end);
        Map<Instant, Long> emlSuppressedDaily = Utils.countObservationsByDay(observedProperties, module, Utils.emlSuppressedPredicate, start, end);


        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for(Map.Entry<Instant, Long> entry : gammaDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Gamma",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : gammaNeutronDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Gamma-Neutron",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : neutronDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Neutron",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : emlSuppressedDaily.entrySet()){
            dataset.addValue(entry.getValue(), "EML-Suppressed",  formatter.format(entry.getKey()));
        }

        String title = "Alarms";
        String xAxis = "Dates";
        String yAxis = "Counts";
        String outFileName = "alarm_chart.png";

        try{
            var chart = chartGenerator.createStackedBarChart(
                    title,
                    xAxis,
                    yAxis, dataset,
                    outFileName
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

        } catch (IOException e) {
            module.getLogger().error("Error creating Alarm chart", e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

    private void addFaultStatisticsByDay(){

        Map<Instant, Long> gammaHighDaily = Utils.countObservationsByDay(observedProperties, module, Utils.gammaHighPredicate, start, end);
        Map<Instant, Long> gammaLowDaily = Utils.countObservationsByDay(observedProperties, module, Utils.gammaLowPredicate, start, end);
        Map<Instant, Long> neutronHighDaily = Utils.countObservationsByDay(observedProperties, module, Utils.neutronHighPredicate, start, end);
        Map<Instant, Long> extendedOccupancyDaily = Utils.countObservationsByDay(observedProperties, module, Utils.extendedOccPredicate, start, end);
        Map<Instant, Long> tamperDaily = Utils.countObservationsByDay(observedProperties, module, Utils.tamperPredicate, start, end);
        Map<Instant, Long> commDaily = Utils.countObservationsByDay(observedProperties, module, Utils.commsPredicate, start, end);
        Map<Instant, Long> cameraDaily = Utils.countObservationsByDay(observedProperties, module, Utils.cameraPredicate, start, end);


        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for(Map.Entry<Instant, Long> entry : gammaHighDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Gamma High",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : gammaLowDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Gamma Low",  formatter.format(entry.getKey()));
        }
        for(Map.Entry<Instant, Long> entry : neutronHighDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Neutron High",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : tamperDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Tamper",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : extendedOccupancyDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Extended Occupancy",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : cameraDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Camera",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : commDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Comm",  formatter.format(entry.getKey()));
        }

        String title = "SOH";
        String xAxis = "Date";
        String yAxis = "Count";
        String outFileName = "soh_chart.png";

        try{
            var chart = chartGenerator.createStackedBarChart(
                    title,
                    xAxis,
                    yAxis,
                    dataset,
                    outFileName
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

        } catch (IOException e) {
            module.getLogger().error("Error creating SOH chart", e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

    private void addAlarmOccStatisticsByDay(){

        Map<Instant, Long> gammaDaily = Utils.countObservationsByDay(observedProperties, module, Utils.gammaPredicate, start, end);
        Map<Instant, Long> neutronDaily = Utils.countObservationsByDay(observedProperties, module, Utils.neutronPredicate, start, end);
        Map<Instant, Long> gammaNeutronDaily = Utils.countObservationsByDay(observedProperties, module, Utils.gammaNeutronPredicate, start, end);
        Map<Instant, Long> totalOccupancyDaily = Utils.countObservationsByDay(observedProperties, module, Utils.occupancyTotalPredicate, start, end);
        Map<Instant, Long> emlSuppressedDaily = Utils.countObservationsByDay(observedProperties, module, Utils.emlSuppressedPredicate, start, end);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for(Map.Entry<Instant, Long> entry : gammaDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Gamma",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : gammaNeutronDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Gamma-Neutron",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : neutronDaily.entrySet()){
            dataset.addValue(entry.getValue(), "Neutron",  formatter.format(entry.getKey()));
        }

        for(Map.Entry<Instant, Long> entry : emlSuppressedDaily.entrySet()){
            dataset.addValue(entry.getValue(), "EML-Suppressed",  formatter.format(entry.getKey()));
        }

        // this will be a linegraph on top of the bar chart
        DefaultCategoryDataset occDataset = new DefaultCategoryDataset();

        for(Map.Entry<Instant, Long> entry : totalOccupancyDaily.entrySet()){
            occDataset.addValue(entry.getValue(), "TotalOccupancy",  formatter.format(entry.getKey()));
        }

        String title = "Alarms and Occupancies";
        String xAxis = "Date";
        String yAxis = "Count";
        String outFileName = "alarm_occupancy_chart.png";

        try{
            var chart = chartGenerator.createStackedBarLineOverlayChart(
                    title,
                    xAxis,
                    yAxis,
                    dataset,
                    occDataset,
                    outFileName
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

        } catch (IOException e) {
            module.getLogger().error("Error creating Alarm-Occupancy chart", e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

}