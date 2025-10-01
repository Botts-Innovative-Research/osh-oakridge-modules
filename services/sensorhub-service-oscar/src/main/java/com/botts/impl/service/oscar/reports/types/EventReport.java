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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Predicate;

public class EventReport extends Report {
    private static final Logger log = LoggerFactory.getLogger(EventReport.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    String reportTitle = "Event Report";

    Document document;
    PdfDocument pdfDocument;
    String pdfFileName;

    OSCARServiceModule module;
    TableGenerator tableGenerator;
    ChartGenerator chartGenerator;

    String laneUID;
    EventReportType eventType;
    Instant start;
    Instant end;

    public EventReport(OutputStream outputStream, Instant startTime, Instant endTime, EventReportType eventType, String laneUID, OSCARServiceModule module) {
        pdfDocument = new PdfDocument(new PdfWriter(outputStream));
        document = new Document(pdfDocument);

        this.eventType = eventType;
        this.laneUID = laneUID;
        this.module = module;
        this.start = startTime;
        this.end = endTime;
        this.tableGenerator = new TableGenerator();
        this.chartGenerator = new ChartGenerator();
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
        pdfDocument.close();
        chartGenerator = null;
        tableGenerator = null;
    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Event Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Event Type:" + eventType).setFontSize(12));
        document.add(new Paragraph("Lane ID:" + laneUID).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addAlarmStatisticsByDay(){
        document.add(new Paragraph("Alarm Statistics").setFontSize(12));

        String[] observedProperties = new String[]{RADHelper.DEF_OCCUPANCY};

        Predicate<IObsData> gammaNeutronPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> gammaPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> neutronPredicate = (obsData) -> !obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> emlSuppressedPredicate = (obsData) -> !obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);

        Map<Instant, Long> gammaDaily = Utils.countObservationsByDay(observedProperties, module, gammaPredicate, start, end);
        Map<Instant, Long> neutronDaily = Utils.countObservationsByDay(observedProperties, module, neutronPredicate, start, end);
        Map<Instant, Long> gammaNeutronDaily = Utils.countObservationsByDay(observedProperties, module, gammaNeutronPredicate, start, end);
        Map<Instant, Long> emlSuppressedDaily = Utils.countObservationsByDay(observedProperties, module, emlSuppressedPredicate, start, end);


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
            String chartPath = chartGenerator.createStackedBarChart(
                    title,
                    xAxis,
                    yAxis, dataset,
                    outFileName
            );

            if(chartPath == null){
                document.add(new Paragraph("Alarm Chart failed to create"));
                return;
            }

            Image image = new Image(ImageDataFactory.create(chartPath)).setAutoScale(true);
            document.add(image);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

    private void addFaultStatisticsByDay(){

        String[] observedProperties = new String[]{RADHelper.DEF_OCCUPANCY};

        Predicate<IObsData> gammaHighPredicate = (obsData) -> true;
        Predicate<IObsData> gammaLowPredicate = (obsData) -> true;
        Predicate<IObsData> neutronHighPredicate = (obsData) -> true;
        Predicate<IObsData> extendedOccupancyPredicate = (obsData) -> true;
        Predicate<IObsData> tamperPredicate = (obsData) -> true;
        Predicate<IObsData> commPredicate = (obsData) -> true;
        Predicate<IObsData> cameraPredicate = (obsData) -> true;

        Map<Instant, Long> gammaHighDaily = Utils.countObservationsByDay(observedProperties, module, gammaHighPredicate, start, end);
        Map<Instant, Long> gammaLowDaily = Utils.countObservationsByDay(observedProperties, module, gammaLowPredicate, start, end);
        Map<Instant, Long> neutronHighDaily = Utils.countObservationsByDay(observedProperties, module, neutronHighPredicate, start, end);
        Map<Instant, Long> extendedOccupancyDaily = Utils.countObservationsByDay(observedProperties, module, extendedOccupancyPredicate, start, end);
        Map<Instant, Long> tamperDaily = Utils.countObservationsByDay(observedProperties, module, tamperPredicate, start, end);
        Map<Instant, Long> commDaily = Utils.countObservationsByDay(observedProperties, module, commPredicate, start, end);
        Map<Instant, Long> cameraDaily = Utils.countObservationsByDay(observedProperties, module, cameraPredicate, start, end);


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
            String chartPath = chartGenerator.createStackedBarChart(
                    title,
                    xAxis,
                    yAxis,
                    dataset,
                    outFileName
            );

            if(chartPath == null){
                document.add(new Paragraph("SOH Chart failed to create"));
                return;
            }

            Image image = new Image(ImageDataFactory.create(chartPath)).setAutoScale(true);
            document.add(image);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

    private void addAlarmOccStatisticsByDay(){

        String[] observedProperties = new String[]{RADHelper.DEF_OCCUPANCY};

        Predicate<IObsData> gammaNeutronPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> gammaPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> neutronPredicate = (obsData) -> !obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> emlSuppressedPredicate = (obsData) -> !obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);
        Predicate<IObsData> occupancyTotalPredicate = (obsData) -> true;

        Map<Instant, Long> gammaDaily = Utils.countObservationsByDay(observedProperties, module, gammaPredicate, start, end);
        Map<Instant, Long> neutronDaily = Utils.countObservationsByDay(observedProperties, module, neutronPredicate, start, end);
        Map<Instant, Long> gammaNeutronDaily = Utils.countObservationsByDay(observedProperties, module, gammaNeutronPredicate, start, end);
        Map<Instant, Long> totalOccupancyDaily = Utils.countObservationsByDay(observedProperties, module, occupancyTotalPredicate, start, end);
        Map<Instant, Long> emlSuppressedDaily = Utils.countObservationsByDay(observedProperties, module, emlSuppressedPredicate, start, end);

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

        // this will be a linagraph on top of the bar chart
        DefaultCategoryDataset occDataset = new DefaultCategoryDataset();

        for(Map.Entry<Instant, Long> entry : totalOccupancyDaily.entrySet()){
            occDataset.addValue(entry.getValue(), "TotalOccupancy",  formatter.format(entry.getKey()));
        }

        String title = "Alarms and Occupancies";
        String xAxis = "Date";
        String yAxis = "Count";
        String outFileName = "alarm_occupancy_chart.png";

        try{
            String chartPath = chartGenerator.createStackedBarLineOverlayChart(
                    title,
                    xAxis,
                    yAxis,
                    dataset,
                    occDataset,
                    outFileName
            );

            if(chartPath == null){
                document.add(new Paragraph("Alarm-Occupancy Chart failed to create"));
                return;
            }

            Image image = new Image(ImageDataFactory.create(chartPath)).setAutoScale(true);
            document.add(image);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        document.add(new Paragraph("\n"));
    }

}