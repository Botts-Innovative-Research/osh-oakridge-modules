package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.reports.helpers.ChartGenerator;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import net.opengis.swe.v20.DataChoice;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.Test;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.utils.rad.RADHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class ReportTests {
    OSCARServiceModule module = new OSCARServiceModule();
    OSCARSystem system = new OSCARSystem("nodeId");
    RequestReportControl requestReportControl = new RequestReportControl(system, module);
    public static Instant now = Instant.now();
    public static Instant begin = now.minus(365, ChronoUnit.DAYS);
    public static Instant end = now;

    @Test
    public void generateLaneReport() throws Exception {

        DataChoice cmdChoice = (DataChoice) module.reportControl.getCommandDescription().copy();

        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(ReportCmdType.LANE.name());
        cmdChoice.getSelectedItem().getData().setTimeStamp(0, begin);
        cmdChoice.getSelectedItem().getData().setTimeStamp(1, end);
        cmdChoice.getSelectedItem().getData().setStringValue(2, "urn:osh:system:lane1");

        var res = requestReportControl.submitCommand(new CommandData(1, cmdChoice.getData())).get();

        assertEquals(res.getStatusCode(), ICommandStatus.CommandStatusCode.ACCEPTED);
        assertNotNull(res.getResult());
        List<IObsData> results = res.getResult().getObservations().stream().toList();
        assertFalse(results.isEmpty());

        String resPath = results.get(0).getResult().getStringValue();
        assertNotNull(resPath);

        // check if file exists
        Files.exists(Path.of("web/" + resPath));
    }

    @Test
    public void generateSiteReport() throws Exception {

        DataChoice cmdChoice = (DataChoice) module.reportControl.getCommandDescription().copy();

        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(ReportCmdType.RDS_SITE.name());
        cmdChoice.getSelectedItem().getData().setTimeStamp(0,Instant.now());
        cmdChoice.getSelectedItem().getData().setTimeStamp(1, Instant.now());

        var res = requestReportControl.submitCommand(new CommandData(2, cmdChoice.getData())).get();

        assertEquals(res.getStatusCode(), ICommandStatus.CommandStatusCode.ACCEPTED);
        assertNotNull(res.getResult());
        List<IObsData> results = res.getResult().getObservations().stream().toList();
        assertFalse(results.isEmpty());

        String resPath = results.get(0).getResult().getStringValue();
        assertNotNull(resPath);

        // check if file exists
        Files.exists(Path.of("web/" + resPath));
    }


    @Test
    public void generateAdjudicationReport() throws Exception {


        DataChoice cmdChoice = (DataChoice) module.reportControl.getCommandDescription().copy();

        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(ReportCmdType.ADJUDICATION.name());
        cmdChoice.getSelectedItem().getData().setTimeStamp(0,Instant.now());
        cmdChoice.getSelectedItem().getData().setTimeStamp(1, Instant.now());

        var res = requestReportControl.submitCommand(new CommandData(3, cmdChoice.getData())).get();

        assertEquals(res.getStatusCode(), ICommandStatus.CommandStatusCode.ACCEPTED);
        assertNotNull(res.getResult());
        List<IObsData> results = res.getResult().getObservations().stream().toList();
        assertFalse(results.isEmpty());

        String resPath = results.get(0).getResult().getStringValue();
        assertNotNull(resPath);

        // check if file exists
        Files.exists(Path.of("web/" + resPath));
    }

    @Test
    public void generateEventReport() throws Exception {


        DataChoice cmdChoice = (DataChoice) module.reportControl.getCommandDescription().copy();

        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(ReportCmdType.EVENT.name());
        cmdChoice.getSelectedItem().getData().setTimeStamp(0,Instant.now());
        cmdChoice.getSelectedItem().getData().setTimeStamp(1, Instant.now());

        var res = requestReportControl.submitCommand(new CommandData(4, cmdChoice.getData())).get();

        assertEquals(res.getStatusCode(), ICommandStatus.CommandStatusCode.ACCEPTED);
        assertNotNull(res.getResult());
        List<IObsData> results = res.getResult().getObservations().stream().toList();
        assertFalse(results.isEmpty());

        String resPath = results.get(0).getResult().getStringValue();
        assertNotNull(resPath);

        // check if file exists
        Files.exists(Path.of("web/" + resPath));
    }



    @Test
    public void compareCounts() throws Exception {
        long gammaCount1 = iterateCount();
        long gammaCount2 = predicateCount();

        assertEquals("Counts should be equal", gammaCount1, gammaCount2);
    }


    public long iterateCount() throws Exception {
        var query = module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
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


    public long predicateCount() throws Exception {
        Predicate<IObsData> gammaPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);};
        long gammaAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, gammaPredicate, begin, end);

        return gammaAlarmCount;
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
    public void TestChart(){
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

            ChartGenerator chartGenerator = new ChartGenerator();
            String chartPath = chartGenerator.createChart(title, xLabel, yLabel, dataset,"bar", "test_chart.png");

            assertNotNull(chartPath);
            Files.exists(Path.of(chartPath));

            Image image = new Image(ImageDataFactory.create(chartPath)).setAutoScale(true);

            document.add(image);

            document.close();

            System.out.println("PDF created with chart: "+ dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
