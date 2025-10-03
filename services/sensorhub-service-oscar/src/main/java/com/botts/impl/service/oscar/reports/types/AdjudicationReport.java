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
import java.util.HashMap;
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
        addSecondaryInspectionDetails();

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
        String xLabel = "Type";

        long realAlarmOtherCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.realAlarmOtherPredicate, begin, end);
        long falseAlarmOtherCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.falseAlarmOtherPredicate, begin, end);
        long physicalInspecNegCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.phyInsPredicate, begin, end);
        long innAlarmMedicalCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.incMedPredicate, begin, end);
        long incRadioMatCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.incRadioPredicate, begin, end);
        long noDisCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.noDisPredicate, begin, end);
        long falseAlarmRiidCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.falseAlarmRIIDPredicate, begin, end);
        long realAlarmContCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.realAlarmContraPredicate, begin, end);
        long tamperFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.tamperFaultPredicate, begin, end);
        long alarmTamperFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.alarmTamperFaultPredicate, begin, end);
        long alarmNaturallyCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, Utils.alarmNORMPredicate, begin, end);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        dataset.addValue(realAlarmOtherCount, "Disposition", "Real Alarm - Other");
        dataset.addValue(falseAlarmOtherCount, "Disposition", "False Alarm - Other");
        dataset.addValue(physicalInspecNegCount, "Disposition", "Physical Inspection Negative");
        dataset.addValue(innAlarmMedicalCount, "Disposition", "Innocent Alarm - Medical Isotope Found");
        dataset.addValue(incRadioMatCount, "Disposition", "Innocent Alarm - Declared Shipment of Radioactive Material");
        dataset.addValue(noDisCount, "Disposition", "No Disposition");
        dataset.addValue(falseAlarmRiidCount, "Disposition", "False Alarm - RIID/ASP Indicates Background Only");
        dataset.addValue(realAlarmContCount, "Disposition", "Real Alarm - Contraband Found");
        dataset.addValue(tamperFaultCount, "Disposition", "Tamper/Fault - Unauthorized Activity");
        dataset.addValue(alarmTamperFaultCount, "Disposition", "Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity");
        dataset.addValue(alarmNaturallyCount, "Disposition", "Alarm - Naturally Occurring Radioactive Material (NORM) Found");


        try {
            var chart = chartGenerator.createChart(
                    title,
                    xLabel,
                    yLabel,
                    dataset,
                    "bar",
                    begin  + "_dispostion_chart.png"
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

        Map<String, String> isotopeResults = new HashMap<>();

        Predicate<IObsData> predicate = (obsData) -> true;

        long value = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, predicate, begin, end);

        isotopeResults.put("Neptunium", "");
        isotopeResults.put("Plutonium", "");
        isotopeResults.put("Uranium233", "");
        isotopeResults.put("Uranium235", "");
        isotopeResults.put("Americium", "");
        isotopeResults.put("Uranium238", "");
        isotopeResults.put("Barium", "");
        isotopeResults.put("Bismuth", "");
        isotopeResults.put("Californium", "");
        isotopeResults.put("Cesium134", "");
        isotopeResults.put("Cesium137", "");
        isotopeResults.put("Cobalt57", "");
        isotopeResults.put("Cobalt60", "");
        isotopeResults.put("Europium", "");
        isotopeResults.put("Iridium", "");
        isotopeResults.put("Manganese", "");
        isotopeResults.put("Selenium", "");
        isotopeResults.put("Sodium", "");
        isotopeResults.put("Strontium", "");
        isotopeResults.put("Fluorine", "");
        isotopeResults.put("Gallium", "");
        isotopeResults.put("Iodine123", "");
        isotopeResults.put("Iodine131", "");
        isotopeResults.put("Indium", "");
        isotopeResults.put("Palladium", "");
        isotopeResults.put("Technetium", "");
        isotopeResults.put("Xenon", "");
        isotopeResults.put("Potassium", "");
        isotopeResults.put("Radium", "");
        isotopeResults.put("Thorium", "");

        var table = tableGenerator.addTable(isotopeResults);
        if (table == null) {
            document.add(new Paragraph("Table results failed to create"));
            return;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addSecondaryInspectionDetails() {
        document.add(new Paragraph("Secondary Inspection Details").setFontSize(12));
//
////        Map<String, Map<String, String>> countsLane = new LinkedHashMap<>();
//
////        for (var lane : laneUIDs.split(",")) {
////            var counts = calculateDetailsCounts(lane);
////            countsLane.put(lane, counts);
////        }
////
////        var table = tableGenerator.addLanesTable(countsLane);
////        if (table == null) {
////            document.add(new Paragraph("Failed to secondary inspection details"));
////            return;
////        }
//
//        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private Map<String, String> addDetails(String laneUID) {
        Map<String, String> secInsDetailsMap = new HashMap<>();

        Predicate<IObsData> predicate = (obsData) -> true;


        secInsDetailsMap.put("Primary Date", "");
        secInsDetailsMap.put("Primary Time", "");
        secInsDetailsMap.put("Event Record ID", "");
        secInsDetailsMap.put("Primary N.Sigma", "");
        secInsDetailsMap.put("SI Result", "");
        secInsDetailsMap.put("Cargo", "");
        secInsDetailsMap.put("Disposition #", "");

        return secInsDetailsMap;
    }
}