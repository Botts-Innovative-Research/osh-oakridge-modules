package com.botts.impl.service.oscar;

import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServiceConfig;
import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.reports.helpers.*;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.utils.rad.RADHelper;
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
import java.util.function.Predicate;
import static org.junit.Assert.*;

public class ReportTests {
    static SensorHub hub;
    static ModuleRegistry reg;

    OSCARServiceModule oscarServiceModule;
    OSCARSystem system;
    RequestReportControl requestReportControl;
    BucketService bucketService;

    public static Instant now = Instant.now();
    public static Instant begin = now.minus(365, ChronoUnit.DAYS);
    public static Instant end = now;


    @Before
    public void setup() throws SensorHubException {
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();

        var bucketServiceConfig = createBucketServiceConfig();
        var serviceConfig = createOscarServiceConfig();

        loadAndStartBucketService(bucketServiceConfig);
        loadAndStartOscarService(serviceConfig);

        requestReportControl = new RequestReportControl(system, oscarServiceModule);
        oscarServiceModule.reportControl = requestReportControl;
    }

    private void loadAndStartBucketService(BucketServiceConfig config) throws SensorHubException {
        bucketService = (BucketService) reg.loadModule(config);

        reg.startModule(bucketService.getLocalID());
        var isStarted = bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        assertTrue(isStarted);
    }

    private void loadAndStartOscarService(OSCARServiceConfig config) throws SensorHubException {
        oscarServiceModule = (OSCARServiceModule) reg.loadModule(config);

        reg.startModule(oscarServiceModule.getLocalID());
        var isStarted = oscarServiceModule.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        assertTrue(isStarted);
    }

    private BucketServiceConfig createBucketServiceConfig() throws SensorHubException {
        BucketServiceConfig bucketServiceConfig = (BucketServiceConfig) reg.createModuleConfig(new com.botts.impl.service.bucket.Descriptor());
        bucketServiceConfig.fileStoreRootDir = "oscar-test";

        List<String> buckets = new ArrayList<>();
        buckets.add("reports");
        buckets.add("videos");
        buckets.add("sitemap");

        bucketServiceConfig.initialBuckets = buckets;
        return  bucketServiceConfig;
    }

    private OSCARServiceConfig createOscarServiceConfig() throws SensorHubException {
        OSCARServiceConfig serviceConfig = (OSCARServiceConfig) reg.createModuleConfig(new Descriptor());
        serviceConfig.siteDiagramConfig = new SiteDiagramConfig();
        serviceConfig.nodeId = "test-node-id";
        serviceConfig.databaseID = "test-database-id";

        return  serviceConfig;
    }

    @Test
    public void generateLaneReport() throws Exception {
        DataComponent commandDesc =  oscarServiceModule.reportControl.getCommandDescription().copy();

        DataBlock commandData;
        commandData = commandDesc.createDataBlock();
        commandData.setStringValue(0, ReportCmdType.LANE.name());
        commandData.setTimeStamp(1, begin);
        commandData.setTimeStamp(2, end);
        commandData.setStringValue(3, "urn:osh:system:lane1");
        commandData.setStringValue(4, EventReportType.ALARMS.name());

        var res = requestReportControl.submitCommand(new CommandData(1, commandData)).get();

        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());
        assertNotNull(res.getResult());
        List<IObsData> results = res.getResult().getObservations().stream().toList();
        assertFalse(results.isEmpty());

        String resPath = results.get(0).getResult().getStringValue();
        assertNotNull(resPath);

        var stream = oscarServiceModule.getBucketService()
                .getBucketStore()
                .getObject(Constants.REPORT_BUCKET, resPath);

        assertNotNull(stream);
    }

    @Test
    public void generateSiteReport() throws Exception {
        DataComponent commandDesc =  oscarServiceModule.reportControl.getCommandDescription().copy();

        DataBlock commandData;
        commandData = commandDesc.createDataBlock();
        commandData.setStringValue(0, ReportCmdType.RDS_SITE.name());
        commandData.setTimeStamp(1, begin);
        commandData.setTimeStamp(2, end);
        commandData.setStringValue(3, "urn:osh:system:lane1");
        commandData.setStringValue(4, EventReportType.ALARMS.name());

        var res = requestReportControl.submitCommand(new CommandData(1, commandData)).get();

        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());
        assertNotNull(res.getResult());
        List<IObsData> results = res.getResult().getObservations().stream().toList();
        assertFalse(results.isEmpty());

        String resPath = results.get(0).getResult().getStringValue();
        assertNotNull(resPath);

        var stream = oscarServiceModule.getBucketService().getBucketStore().getObject(Constants.REPORT_BUCKET, resPath);
        assertNotNull(stream);
    }

    @Test
    public void generateAdjudicationReport() throws Exception {
        DataComponent commandDesc =  oscarServiceModule.reportControl.getCommandDescription().copy();

        DataBlock commandData;
        commandData = commandDesc.createDataBlock();
        commandData.setStringValue(0, ReportCmdType.ADJUDICATION.name());
        commandData.setTimeStamp(1, begin);
        commandData.setTimeStamp(2, end);
        commandData.setStringValue(3, "urn:osh:system:lane1");
        commandData.setStringValue(4, EventReportType.ALARMS.name());

        var res = requestReportControl.submitCommand(new CommandData(1, commandData)).get();

        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());
        assertNotNull(res.getResult());
        List<IObsData> results = res.getResult().getObservations().stream().toList();
        assertFalse(results.isEmpty());

        String resPath = results.get(0).getResult().getStringValue();
        assertNotNull(resPath);

        var stream = oscarServiceModule.getBucketService().getBucketStore().getObject(Constants.REPORT_BUCKET, resPath);
        assertNotNull(stream);

    }

    @Test
    public void generateEventReport() throws Exception {
        DataComponent commandDesc =  oscarServiceModule.reportControl.getCommandDescription().copy();

        DataBlock commandData;
        commandData = commandDesc.createDataBlock();
        commandData.setStringValue(0, ReportCmdType.EVENT.name());
        commandData.setTimeStamp(1, begin);
        commandData.setTimeStamp(2, end);
        commandData.setStringValue(3, "urn:osh:system:lane1");
        commandData.setStringValue(4, EventReportType.ALARMS_OCCUPANCIES.name());

        var res = requestReportControl.submitCommand(new CommandData(1, commandData)).get();

        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());
        assertNotNull(res.getResult());
        List<IObsData> results = res.getResult().getObservations().stream().toList();
        assertFalse(results.isEmpty());

        String resPath = results.get(0).getResult().getStringValue();
        assertNotNull(resPath);

        var stream = oscarServiceModule.getBucketService().getBucketStore().getObject(Constants.REPORT_BUCKET, resPath);
        assertNotNull(stream);

    }

    @Test
    public void compareCounts() throws Exception {
        long gammaCount1 = iterateCount();
        long gammaCount2 = predicateCount();

        assertEquals("Counts should be equal", gammaCount1, gammaCount2);
    }

    public long iterateCount() {
        var query = oscarServiceModule.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(RADHelper.DEF_OCCUPANCY)
                        .withValidTimeDuring(begin, end)
                        .build())
                .build()).iterator();

        var gammaCount = 0;
        while (query.hasNext()) {
            var entry  = query.next();

            var result = entry.getResult();

            var gamma = result.getBooleanValue(5);
            var neutron = result.getBooleanValue(6);

            if(gamma && !neutron){
                gammaCount++;
            }
        }
        return gammaCount;
    }


    public long predicateCount(){
        Predicate<IObsData> gammaPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);
        return Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, oscarServiceModule, gammaPredicate, begin, end);
    }


    @Test
    public void TestTable(){
        String dest = "table_test.pdf";

        try{
            PdfWriter pdfWriter = new PdfWriter(dest);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDocument);

            Map<String, String> testData = new HashMap<>();
            testData.put("Alarm Occupancy Rate", "1");
            testData.put("Neutron Alarm", "10");
            testData.put("Gamma Alarm", "15");
            testData.put("Gamma-Neutron Alarm", "5");
            testData.put("Total Occupancies", "20");
            testData.put("Primary occ", "20");
            testData.put("Another Name", "20");
            testData.put("Total", "20");

            TableGenerator tableGenerator = new TableGenerator();
            document.add(tableGenerator.addTable(testData));
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

            dataset.addValue(10, "Disposition","Real Alarm - Other");
            dataset.addValue(15, "Disposition", "False Alarm - Other");
            dataset.addValue(12, "Disposition", "Physical Inspection Negative");
            dataset.addValue(4, "Disposition", "Innocent Alarm - Medical Isotope Found");
            dataset.addValue(2, "Disposition", "Innocent Alarm - Declared Shipment of Radioactive Material");
            dataset.addValue(0, "Disposition", "No Disposition");
            dataset.addValue(1, "Disposition", "False Alarm - RIID/ASP Indicates Background Only");
            dataset.addValue(12, "Disposition", "Real Alarm - Contraband Found");
            dataset.addValue(12, "Disposition", "Tamper/Fault - Unauthorized Activity");
            dataset.addValue(4, "Disposition", "Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity");
            dataset.addValue(5, "Disposition", "Alarm - Naturally Occurring Radioactive Material (NORM) Found");


            String title = "Test Chart";
            String yLabel = "% of Total Number of Records";
            String xLabel = "Type";

            ChartGenerator chartGenerator = new ChartGenerator(oscarServiceModule);
            var chart = chartGenerator.createChart(title, xLabel, yLabel, dataset,"bar", "test_chart.png");

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
        String outFileName = "stacked_bar_chart_test.png";

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

            ChartGenerator chartGenerator = new ChartGenerator(oscarServiceModule);
            var chart = chartGenerator.createStackedBarChart(title, xLabel, yLabel, dataset, outFileName);

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

            ChartGenerator chartGenerator = new ChartGenerator(oscarServiceModule);
            var chart = chartGenerator.createStackedBarLineOverlayChart(title, xLabel, yLabel, dataset,dataset2, "bar_line_chart.png");

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

    // from chatgpt sorry -- used to simualte fake data for testing my charts to see if they work mwahhahahahaha
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


    @After
    public void cleanup() {
        if (hub != null)
            hub.stop();
    }
}