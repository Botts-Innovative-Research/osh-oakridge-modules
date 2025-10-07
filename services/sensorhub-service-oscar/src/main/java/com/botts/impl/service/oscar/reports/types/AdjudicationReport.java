package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.*;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.impl.utils.rad.RADHelper;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class AdjudicationReport extends Report {

    Document document;
    PdfDocument pdfDocument;

    OSCARServiceModule module;
    TableGenerator tableGenerator;
    ChartGenerator chartGenerator;

    String laneUIDs;
    Instant begin;
    Instant end;

    public AdjudicationReport(OutputStream out, Instant startTime, Instant endTime, String laneUIDs, OSCARServiceModule module) {
        pdfDocument = new PdfDocument(new PdfWriter(out));
        document = new Document(pdfDocument);

        this.module = module;
        this.laneUIDs = laneUIDs;
        this.begin = startTime;
        this.end = endTime;
        this.tableGenerator = new TableGenerator();
        this.chartGenerator = new ChartGenerator(module);
    }

    @Override
    public void generate() {
        addHeader();
        addDisposition();
        addSecondaryInspectionIsotopeResults();
//        addSecondaryInspectionDetails();
//        addAdjudicationDetails();

        document.close();
        chartGenerator = null;
        tableGenerator = null;
    }

    private void addHeader() {
        document.add(new Paragraph("Adjudication Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane UID: " + laneUIDs).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addDisposition() {
        document.add(new Paragraph("Disposition").setFontSize(12));

        String title = "Disposition";
        String yLabel = "% of Total Number of Records";

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        Map<Integer, String> dispositions = Map.ofEntries(
                Map.entry(0, "Real Alarm - Other"),
                Map.entry(1, "False Alarm - Other"),
                Map.entry(2, "Physical Inspection Negative"),
                Map.entry(3, "Innocent Alarm - Medical Isotope Found"),
                Map.entry(4, "Innocent Alarm - Declared Shipment of Radioactive Material"),
                Map.entry(5, "No Disposition"),
                Map.entry(6, "False Alarm - RIID/ASP Indicates Background Only"),
                Map.entry(7, "Real Alarm - Contraband Found"),
                Map.entry(8, "Tamper/Fault - Unauthorized Activity"),
                Map.entry(9, "Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity"),
                Map.entry(10, "Alarm - Naturally Occurring Radioactive Material (NORM) Found")
        );

        long totalRecords = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, obsData -> true, begin, end);

        if (totalRecords == 0) {
            document.add(new Paragraph("No data available for the selected time period"));
            return;
        }


        for (Map.Entry<Integer, String> entry : dispositions.entrySet()) {
             String name = entry.getValue();

            Predicate<IObsData> predicate = (obsData) ->
                    obsData.getResult().getStringValue(3).contains(name);

            long count = Utils.countObservations(
                    new String[]{RADHelper.DEF_ADJUDICATION},
                    module,
                    predicate,
                    begin,
                    end
            );
            double percentage = (count / (double) totalRecords) * 100.0;

            dataset.addValue(percentage, name, name);
        }

        try {
            var chart = chartGenerator.createChart(
                    title,
                    null,
                    yLabel,
                    dataset,
                    "bar",
                    PlotOrientation.HORIZONTAL
            );

            if (chart == null) {
                document.add(new Paragraph("Disposition chart failed to create"));
                return;
            }

            BufferedImage bufferedImage = chart.createBufferedImage(1200, 600);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            Image image = new Image(ImageDataFactory.create(imageBytes)).setAutoScale(true);

            document.add(image);
            document.add(new Paragraph("\n"));
        } catch (IOException e) {
            module.getLogger().error("Error adding chart to report", e);
        }
    }

    private void addSecondaryInspectionIsotopeResults() {
        document.add(new Paragraph("Secondary Inspection Results").setFontSize(12));

        String title = "Secondary Inspection Results";
        String yLabel = "Count";
        String xLabel = "Isotope";

        // List of isotope mappings: Key = Symbol, Value = Search Term
        Map<String, String> isotopes = Map.ofEntries(
                Map.entry("Np", "Neptunium"),
                Map.entry("Pu", "Plutonium"),
                Map.entry("U-233", "Uranium233"),
                Map.entry("U-235", "Uranium235"),
                Map.entry("Am", "Americium"),
                Map.entry("U-238", "Uranium238"),
                Map.entry("Ba", "Barium"),
                Map.entry("Bi", "Bismuth"),
                Map.entry("Cf", "Californium"),
                Map.entry("Cs-134", "Cesium134"),
                Map.entry("Cs-137", "Cesium137"),
                Map.entry("Co-57", "Cobalt57"),
                Map.entry("Co-60", "Cobalt60"),
                Map.entry("Eu", "Europium"),
                Map.entry("Ir", "Iridium"),
                Map.entry("Mn", "Manganese"),
                Map.entry("Se", "Selenium"),
                Map.entry("Na", "Sodium"),
                Map.entry("Sr", "Strontium"),
                Map.entry("F", "Fluorine"),
                Map.entry("Ga", "Gallium"),
                Map.entry("I-123", "Iodine123"),
                Map.entry("I-131", "Iodine131"),
                Map.entry("In", "Indium"),
                Map.entry("Pd", "Palladium"),
                Map.entry("Tc", "Technetium"),
                Map.entry("Xe", "Xenon"),
                Map.entry("K", "Potassium"),
                Map.entry("Ra", "Radium"),
                Map.entry("Th", "Thorium")
        );

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<String, String> entry : isotopes.entrySet()) {
            String symbol = entry.getKey();
            String name = entry.getValue();

            Predicate<IObsData> predicate = (obsData) -> obsData.getResult().getStringValue(4).contains(name);

            long count = Utils.countObservations(
                    new String[]{RADHelper.DEF_ADJUDICATION},
                    module,
                    predicate,
                    begin,
                    end
            );

            dataset.addValue(count, name, symbol);
        }

        try {
            var chart = chartGenerator.createChart(
                    title,
                    xLabel,
                    yLabel,
                    dataset,
                    "bar",
                    PlotOrientation.HORIZONTAL
            );

            if (chart == null) {
                document.add(new Paragraph("Disposition chart failed to create"));
                return;
            }

            BufferedImage bufferedImage = chart.createBufferedImage(1200, 600);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            Image image = new Image(ImageDataFactory.create(imageBytes)).setAutoScale(true);

            document.add(image);
            document.add(new Paragraph("\n"));
        } catch (IOException e) {
            module.getLogger().error("Error adding chart to report", e);
        }
    }

    private void addSecondaryInspectionDetails() {
        document.add(new Paragraph("Secondary Inspection Details").setFontSize(12));

        Map<String, Map<String, String>> secInspecDetails = new LinkedHashMap<>();

        for (var lane : laneUIDs.split(",")) {
            var counts = addSecondaryDetails(lane);
            secInspecDetails.put(lane, counts);
        }

        var table = tableGenerator.addLanesTable(secInspecDetails);
        if (table == null) {
            document.add(new Paragraph("Failed to secondary inspection details"));
            return;
        }
        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private Map<String, String> addSecondaryDetails(String laneUID) {
        Map<String, String> secInsDetailsMap = new LinkedHashMap<>();

        var query = Utils.getQuery(module, laneUID, RADHelper.DEF_ADJUDICATION, begin, end);

        while (query.hasNext()) {
            var entry = query.next();
            var result = entry.getResult();

            secInsDetailsMap.put("Primary Date", result.getStringValue(0));
            secInsDetailsMap.put("Primary Time", result.getStringValue(1));
            secInsDetailsMap.put("Event Record ID", result.getStringValue(2));
            secInsDetailsMap.put("Primary N.Sigma", result.getStringValue(3));
            secInsDetailsMap.put("SI Result", result.getStringValue(4));
            secInsDetailsMap.put("Cargo", result.getStringValue(5));
            secInsDetailsMap.put("Disposition #", result.getStringValue(6));
        }

        return secInsDetailsMap;
    }


    private void addAdjudicationDetails() {
        document.add(new Paragraph("\n"));

        Map<String, Map<String,String>> adjByLanesMap = new LinkedHashMap<>();

        for (var laneUID : laneUIDs.split(", ")) {
            var adjDetailsMap = collectAdjudicationDetails(laneUID);
            adjByLanesMap.put(laneUID, adjDetailsMap);
        }


        var table = tableGenerator.addLanesTable(adjByLanesMap);
        if (table == null) {
            document.add(new Paragraph("Adjudication Details Table failed to create"));
            return;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private Map<String, String> collectAdjudicationDetails(String laneUID) {
        Map<String, String> adjDetailsMap = new LinkedHashMap<>();

        var query = Utils.getQuery(module, laneUID, RADHelper.DEF_ADJUDICATION, begin, end);

        while (query.hasNext()) {
            var entry = query.next();
            var result = entry.getResult();

            adjDetailsMap.put("Username", result.getStringValue(0));
            adjDetailsMap.put("Feedback", result.getStringValue(1));
            adjDetailsMap.put("Adjudication Code", result.getStringValue(2));
            adjDetailsMap.put("Isotopes", result.getStringValue(3));
            adjDetailsMap.put("File Paths", result.getStringValue(4));
            adjDetailsMap.put("Occupancy ID", result.getStringValue(5));
            adjDetailsMap.put("Alarming System ID", result.getStringValue(6));
            adjDetailsMap.put("Vehicle ID", result.getStringValue(7));
            adjDetailsMap.put("Secondary Inspection Status", result.getStringValue(8));
        }
        return adjDetailsMap;
    }
}