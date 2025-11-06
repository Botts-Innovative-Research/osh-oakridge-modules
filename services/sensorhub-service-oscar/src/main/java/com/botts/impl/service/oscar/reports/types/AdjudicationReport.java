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
import com.itextpdf.layout.properties.UnitValue;
import org.checkerframework.checker.units.qual.A;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.utils.rad.RADHelper;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

public class AdjudicationReport extends Report {

    Document document;
    PdfDocument pdfDocument;
    
    TableGenerator tableGenerator;
    ChartGenerator chartGenerator;

    String laneUIDs;

    private static final List<Map<String, Object>> ADJUDICATION_CODES = List.of(
            Map.of("code", 0, "label", "", "group", "Other"),
            Map.of("code", 1, "label", "Code 1: Contraband Found", "group", "Real Alarm"),
            Map.of("code", 2, "label", "Code 2: Other", "group", "Real Alarm"),
            Map.of("code", 3, "label", "Code 3: Medical Isotope Found", "group", "Innocent Alarm"),
            Map.of("code", 4, "label", "Code 4: NORM Found", "group", "Innocent Alarm"),
            Map.of("code", 5, "label", "Code 5: Declared Shipment of Radioactive Material", "group", "Innocent Alarm"),
            Map.of("code", 6, "label", "Code 6: Physical Inspection Negative", "group", "False Alarm"),
            Map.of("code", 7, "label", "Code 7: RIID/ASP Indicates Background Only", "group", "False Alarm"),
            Map.of("code", 8, "label", "Code 8: Other", "group", "False Alarm"),
            Map.of("code", 9, "label", "Code 9: Authorized Test, Maintenance, or Training Activity", "group", "Test/Maintenance"),
            Map.of("code", 10, "label", "Code 10: Unauthorized Activity", "group", "Tamper/Fault"),
            Map.of("code", 11, "label", "Code 11: Other", "group", "Other")
    );
    
    public AdjudicationReport(OutputStream out, Instant startTime, Instant endTime, String laneUIDs, OSCARServiceModule module) {
        super(out, startTime, endTime, module);

        pdfDocument = new PdfDocument(new PdfWriter(out));
        document = new Document(pdfDocument);

        this.laneUIDs = laneUIDs;
        this.tableGenerator = new TableGenerator();
        this.chartGenerator = new ChartGenerator(module);
    }

    @Override
    public void generate(){
        addHeader();

        for (var laneUID : laneUIDs.split(",")){
            createDispositionChart(laneUID);
            createIsotopeChart(laneUID);
        }

        document.close();
        chartGenerator = null;
        tableGenerator = null;
    }

    @Override
    public String getReportType() {
        return ReportCmdType.ADJUDICATION.name();
    }

    private void addHeader() {
        document.add(new Paragraph("Adjudication Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane UID: " + laneUIDs).setFontSize(12));
        document.add(new Paragraph("\n"));
    }


    private void createDispositionChart(String laneUID) {
        document.add(new Paragraph("Disposition - " + laneUID).setFontSize(12));

        String title =  "Disposition";
        String yLabel = "% of Total Number of Records";


        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        long totalRecords = Utils.countStatusResults(laneUID, module, cmdData -> true, start, end);

        if (totalRecords == 0) {
            document.add(new Paragraph("No data available for the selected time period and selected lane: "+ laneUID));
            return;
        }

        List<Map<String, String>> tableRows = new ArrayList<>();

        for (var item : ADJUDICATION_CODES) {
            int code = (int) item.get("code");
            String label = (String) item.get("label");
            String group = (String) item.get("group");

            Predicate<ICommandStatus> predicate = (cmdData) ->
                    cmdData.getResult().getInlineRecords().stream().toList().get(2).getIntValue() == code;

            long count = Utils.countStatusResults(laneUID, module, predicate, start, end);
            double percentage = (count / (double) totalRecords) * 100.0;

            if (label != null && !label.isEmpty()) {
                dataset.addValue(percentage, group, label);
            }

            tableRows.add(Map.of(
                    "Code", String.valueOf(code),
                    "Label", label.isEmpty() ? "(None)" : label,
                    "Group", group,
                    "Count", String.valueOf(count),
                    "Percent", String.format("%.1f%%", percentage)
            ));
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

            CategoryPlot plot = chart.getCategoryPlot();
            plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(dataset.getRowIndex("Other"), Color.GRAY);
            renderer.setSeriesPaint(dataset.getRowIndex("Real Alarm"), Color.RED);
            renderer.setSeriesPaint(dataset.getRowIndex("Innocent Alarm"), Color.BLUE);
            renderer.setSeriesPaint(dataset.getRowIndex("False Alarm"), Color.GREEN);
            renderer.setSeriesPaint(dataset.getRowIndex("Test/Maintenance"), Color.ORANGE);
            renderer.setSeriesPaint(dataset.getRowIndex("Tamper/Fault"), new Color(128, 0, 128));
            renderer.setItemMargin(0.05);

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setNumberFormatOverride(new DecimalFormat("0'%'"));
            rangeAxis.setTickUnit(new NumberTickUnit(10));

            Image image = addChartAsImage(chart);
            document.add(image);
            document.add(new Paragraph("\n"));

            Table table = createDispositionTable(tableRows);
            document.add(table);
            document.add(new Paragraph("\n"));

        } catch (IOException e) {
            module.getLogger().error("Error adding chart to report", e);
        }
    }

    private Table createDispositionTable(List<Map<String, String>> rows) {
        if (rows.isEmpty())
            return null;

        List<String> headers = List.of("Code", "Label", "Group", "Count", "Percent");
        float[] columnWidths = new float[headers.size()];
        Arrays.fill(columnWidths, 1.0f);

        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100));

        for (String header : headers) {
            table.addHeaderCell(tableGenerator.createHeaderCell(header));
        }

        for (Map<String, String> row : rows) {
            for (String header : headers) {
                table.addCell(tableGenerator.createValueCell(row.get(header)));
            }
        }
        return table;
    }


    private Image addChartAsImage(JFreeChart chart) throws IOException {
        BufferedImage bufferedImage = chart.createBufferedImage(1200, 600);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        Image image = new Image(ImageDataFactory.create(imageBytes)).setAutoScale(true);

       return image;
    }

    private void createIsotopeChart(String laneUID) {
        document.add(new Paragraph("SI Results - " + laneUID).setFontSize(12));

        String title = "Isotope Results";
        String yLabel = "Count";
        String xLabel = "Isotope";

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
                Map.entry("Th", "Thorium"),
                Map.entry("Unk", "Unknown")
        );

        Map<String, String> tableData = new LinkedHashMap<>();

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<String, String> entry : isotopes.entrySet()) {
            String symbol = entry.getKey();
            String name = entry.getValue();

            Predicate<ICommandStatus> predicate = (cmdData) -> cmdData.getResult().getInlineRecords().stream().toList().get(4).getStringValue().contains(name);

            long count = Utils.countStatusResults(
                    laneUID,
                    module,
                    predicate,
                    start,
                    end
            );

            dataset.addValue(count, name, symbol);
            tableData.put(symbol, String.valueOf(count));
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
                document.add(new Paragraph("Isotopse chart failed to create"));
                return;
            }

            CategoryPlot plot = chart.getCategoryPlot();
            plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

            Image image = addChartAsImage(chart);

            document.add(image);
            document.add(new Paragraph("\n"));

            Table table = tableGenerator.addTable(tableData);
            document.add(table);
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

        var query = Utils.getQuery(module, laneUID, start, end, RADHelper.DEF_ADJUDICATION);

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

        var query = Utils.queryCommandStatus(module, laneUID, start, end);
        while (query.hasNext()) {
            var entry = query.next();
            var result = entry.getResult().getInlineRecords().stream().toList().get(0);


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