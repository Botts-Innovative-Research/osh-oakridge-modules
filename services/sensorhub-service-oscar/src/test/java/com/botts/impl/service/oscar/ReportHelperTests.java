package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.reports.helpers.ChartGenerator;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.Assert.assertNotNull;

public class ReportHelperTests {

    public static Instant now = Instant.now();
    public static Instant begin = now.minus(365, ChronoUnit.DAYS);
    public static Instant end = now;


    @Test
    public void TestLanesTable(){
        String dest = "table_lanes_test.pdf";

        try{
            PdfWriter pdfWriter = new PdfWriter(dest);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDocument);

            Map<String, String> testData1 = new HashMap<>();
            testData1.put("Alarm Occupancy Rate", "1");
            testData1.put("Neutron Alarm", "10");
            testData1.put("Gamma Alarm", "15");
            testData1.put("Gamma-Neutron Alarm", "5");
            testData1.put("Total Occupancies", "20");
            testData1.put("Primary Occupancies", "20");
            testData1.put("Another Name", "20");
            testData1.put("Total", "20");

            Map<String, String> testData2 = new HashMap<>();
            testData2.put("Alarm Occupancy Rate", "1");
            testData2.put("Neutron Alarm", "10");
            testData2.put("Gamma Alarm", "15");
            testData2.put("Gamma-Neutron Alarm", "5");
            testData2.put("Total Occupancies", "20");
            testData2.put("Primary Occupancies", "20");
            testData2.put("Another Name", "20");
            testData2.put("Total", "20");

            Map<String, Map<String, String>> testData = new HashMap<>();
            testData.put("uid:lane:1", testData1);
            testData.put("uid:lane:2", testData2);

            TableGenerator tableGenerator = new TableGenerator();
            document.add(tableGenerator.addLanesTable(testData));
            document.close();

            System.out.println("PDF Created with table: "+ dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void TestTable(){
        String dest = "table_test.pdf";

        try{
            PdfWriter pdfWriter = new PdfWriter(dest);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDocument);

            Map<String, String> testData1 = new HashMap<>();
            testData1.put("Alarm Occupancy Rate", "1");
            testData1.put("Neutron Alarm", "10");
            testData1.put("Gamma Alarm", "15");
            testData1.put("Gamma-Neutron Alarm", "5");
            testData1.put("Total Occupancies", "20");
            testData1.put("Primary Occupancies", "20");
            testData1.put("Another Name", "20");
            testData1.put("Total", "20");

            TableGenerator tableGenerator = new TableGenerator();
            document.add(tableGenerator.addTable(testData1));
            document.close();

            System.out.println("PDF Created with table: "+ dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void TestBarChart(){
        String dest = "chart_test.pdf";

        try{
            PdfWriter pdfWriter = new PdfWriter(dest);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDocument);

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            dataset.addValue(.01 * 100.0, "Real Alarm - Other","Real Alarm - Other");
            dataset.addValue(.05 * 100.0, "False Alarm - Other", "False Alarm - Other");
            dataset.addValue(.02 * 100.0, "Physical Inspection Negative", "Physical Inspection Negative");
            dataset.addValue(0.04 * 100.0, "Innocent Alarm - Medical Isotope Found", "Innocent Alarm - Medical Isotope Found");
            dataset.addValue(.07 * 100.0, "Innocent Alarm - Declared Shipment of Radioactive Material", "Innocent Alarm - Declared Shipment of Radioactive Material");
            dataset.addValue(0, "No Disposition", "No Disposition");
            dataset.addValue(.15 * 100.0, "False Alarm - RIID/ASP Indicates Background Only", "False Alarm - RIID/ASP Indicates Background Only");
            dataset.addValue(.05 * 100.0, "Real Alarm - Contraband Found", "Real Alarm - Contraband Found");
            dataset.addValue(.02 * 100.0, "Tamper/Fault - Unauthorized Activity", "Tamper/Fault - Unauthorized Activity");
            dataset.addValue(.0 * 100.0, "Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity", "Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity");
            dataset.addValue(.25 * 100.0, "Alarm - Naturally Occurring Radioactive Material (NORM) Found", "Alarm - Naturally Occurring Radioactive Material (NORM) Found");


            String title = "Test Chart";
            String yLabel = "% of Total Number of Records";

            ChartGenerator chartGenerator = new ChartGenerator(new OSCARServiceModule());
            var chart = chartGenerator.createChart(
                    title,
                    null,
                    yLabel, dataset,
                    "bar",
                    PlotOrientation.HORIZONTAL
            );

            assertNotNull(chart);


            BufferedImage bufferedImage = chart.createBufferedImage(1200, 600);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            Image image = new Image(ImageDataFactory.create(imageBytes)).setAutoScale(true);

            document.add(image);

            document.close();

            System.out.println("PDF created with chart: "+ dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void TestStackedBarChart(){
        String dest = "stacked_bar_chart_test.pdf";

        try{
            PdfWriter pdfWriter = new PdfWriter(dest);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDocument);

            int days = 10;

            Map<Instant, Long> gammaDaily = generateFakeData(days, 10, 50);
            Map<Instant, Long> gammaNeutronDaily = generateFakeData(days, 20, 60);
            Map<Instant, Long> neutronDaily = generateFakeData(days, 5, 30);
            Map<Instant, Long> emlSuppressedDaily = generateFakeData(days, 0, 10);


            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for(Map.Entry<Instant, Long> entry : gammaDaily.entrySet()){
                dataset.addValue(entry.getValue(), "Gamma", formatter.format(entry.getKey()));
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

            String title = "Test Chart";
            String yLabel = "Count";
            String xLabel = "Date";

            ChartGenerator chartGenerator = new ChartGenerator(new OSCARServiceModule());
            var chart = chartGenerator.createStackedBarChart(
                    title,
                    xLabel,
                    yLabel,
                    dataset
            );

            assertNotNull(chart);

            BufferedImage bufferedImage = chart.createBufferedImage(1200, 600);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            Image image = new Image(ImageDataFactory.create(imageBytes)).setAutoScale(true);

            document.add(image);

            document.close();

            System.out.println("PDF created with chart: "+ dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    @Test
    public void TestStackedBarLineOverlayChart(){
        String dest = "bar_line_chart.pdf";

        try{
            PdfWriter pdfWriter = new PdfWriter(dest);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDocument);

            int days = 10;

            Map<Instant, Long> gammaDaily = generateFakeData(days, 10, 50);
            Map<Instant, Long> gammaNeutronDaily = generateFakeData(days, 20, 60);
            Map<Instant, Long> neutronDaily = generateFakeData(days, 5, 30);
            Map<Instant, Long> emlSuppressedDaily = generateFakeData(days, 0, 10);
            Map<Instant, Long> totalOccupancyDaily = generateFakeData(days, 100, 200);


            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for(Map.Entry<Instant, Long> entry : gammaDaily.entrySet()){
                dataset.addValue(entry.getValue(), "Gamma", formatter.format(entry.getKey()));
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

            DefaultCategoryDataset dataset2 = new DefaultCategoryDataset();
            for(Map.Entry<Instant, Long> entry : totalOccupancyDaily.entrySet()){
                dataset2.addValue(entry.getValue(), "Total occupancy",  formatter.format(entry.getKey()));
            }
            String title = "Test Chart";
            String yLabel = "Count";
            String xLabel = "Date";

            ChartGenerator chartGenerator = new ChartGenerator(new OSCARServiceModule());
            var chart = chartGenerator.createStackedBarLineOverlayChart(
                    title,
                    xLabel,
                    yLabel,
                    dataset,
                    dataset2
            );

            assertNotNull(chart);

            BufferedImage bufferedImage = chart.createBufferedImage(1200, 600);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            Image image = new Image(ImageDataFactory.create(imageBytes)).setAutoScale(true);

            document.add(image);

            document.close();

            System.out.println("PDF created with chart: "+ dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // from chatgpt sorry -- used to simulate fake data for testing my charts to see if they work mwahhahahahaha
    public static Map<Instant, Long> generateFakeData(int days, long min, long max) {
        Map<Instant, Long> data = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        Random rand = new Random();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Instant instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
            long value = min + (long)(rand.nextDouble() * (max - min));
            data.put(instant, value);
        }

        return data;
    }

}
