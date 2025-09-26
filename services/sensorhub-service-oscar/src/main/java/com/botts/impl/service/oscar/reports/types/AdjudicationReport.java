package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ChartGenerator;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import org.checkerframework.checker.units.qual.C;
import org.jfree.data.category.DefaultCategoryDataset;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class AdjudicationReport extends Report {
    private static final Logger log = LoggerFactory.getLogger(RDSReport.class);

    String reportTitle = "Adjudication Report";
    Document document;
    PdfDocument pdfDocument;
    String pdfFileName;

    OSCARServiceModule module;
    TableGenerator tableGenerator;
    ChartGenerator chartGenerator;

    String eventId;
    String laneUID;
    Instant begin;
    Instant end;
    public AdjudicationReport(Instant startTime, Instant endTime, String eventId, String laneUID, OSCARServiceModule module) {
        try {
            pdfFileName = ReportCmdType.ADJUDICATION.name() + "_" + startTime + "_"+ endTime + ".pdf";
            File file = new File("files/reports/" + pdfFileName);
            file.getParentFile().mkdirs();

            pdfDocument = new PdfDocument(new PdfWriter(file));
            document = new Document(pdfDocument);
        } catch (IOException e) {
            document.close();
            pdfDocument.close();
            log.error(e.getMessage(), e);
            return;
        }
        this.module = module;
        this.eventId = eventId;
        this.laneUID = laneUID;
        this.begin = startTime;
        this.end = endTime;
        this.tableGenerator = new TableGenerator();
        this.chartGenerator = new ChartGenerator();
    }

    @Override
    public String generate() {
        addHeader();
        addDisposition();
//        addSecondaryInspectionResults();
//        addSecondaryInspectionDetails();

        document.close();
        pdfDocument.close();
        chartGenerator = null;
        tableGenerator = null;

        return pdfFileName;
    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane UID: " + laneUID).setFontSize(12));
        document.add(new Paragraph("Event ID: " + eventId).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addDisposition(){
        document.add(new Paragraph("Disposition").setFontSize(12));
        
        String title = "Disposition";
        String yLabel = "% of Total Number of Records";
        String xLabel = "Type";

        Predicate<IObsData> realAlarmOtherPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("Real Alarm - Other");};
        Predicate<IObsData> falseAlarmOtherPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("False Alarm - Other");};
        Predicate<IObsData> phyInsPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("Physical Inspection");};
        Predicate<IObsData> incMedPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("Innocent Alarm - Medical Isotope Found");};
        Predicate<IObsData> incRadioPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("Innocent Alarm - Declared Shipment of Radioactive Material");};
        Predicate<IObsData> noDisPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("No Disposition");};
        Predicate<IObsData> falseAlarmRIIDPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("False Alarm - RIID/ASP Indicates Background Only");};
        Predicate<IObsData> realAlarmContraPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("Real Alarm - Contraband Found");};
        Predicate<IObsData> tamperFaultPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("Tamper/Fault - Unauthorized Activity");};
        Predicate<IObsData> alarmTamperFaultPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("Alarm/Tamper/Fault- Authorized Test/Maintenance/Training Activity");};
        Predicate<IObsData> alarmNORMPredicate = (obsData) -> {return obsData.getResult().getStringValue(3).contains("Alarm - Naturally Occurring Radioactive Material (NORM) Found");};


        long realAlarmOtherCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, realAlarmOtherPredicate, begin, end);
        long falseAlarmOtherCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, falseAlarmOtherPredicate, begin, end);
        long physicalInspecNegCount =  Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, phyInsPredicate, begin, end);
        long innAlarmMedicalCount =  Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, incMedPredicate, begin, end);
        long incRadioMatCount =  Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, incRadioPredicate, begin, end);
        long noDisCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, noDisPredicate, begin, end);
        long falseAlarmRiidCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, falseAlarmRIIDPredicate, begin, end);
        long realAlarmContCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, realAlarmContraPredicate, begin, end);
        long tamperFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, tamperFaultPredicate, begin, end);
        long alarmTamperFaultCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, alarmTamperFaultPredicate, begin, end);
        long alarmNaturallyCount = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, alarmNORMPredicate, begin, end);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        dataset.addValue(realAlarmOtherCount, "Disposition","Real Alarm - Other");
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
            String chartPath = chartGenerator.createChart(title, xLabel, yLabel,dataset, "bar", eventId + "_" + laneUID + "_dispostion_chart.png");

            if(chartPath == null){
              document.add(new Paragraph("Disposition chart failed to create"));
            }
            Image image = new Image(ImageDataFactory.create(chartPath)).setAutoScale(true);
            document.add(image);



        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }
        document.add(new Paragraph("\n"));
    }

    private void addSecondaryInspectionResults(){
        document.add(new Paragraph("Secondary Inspection Results").setFontSize(12));

        addSecondaryInspectionIsotopeResults();

        document.add(new Paragraph("\n"));
    }

    private void addSecondaryInspectionIsotopeResults(){
        document.add(new Paragraph("\n"));

        Map<String, String> isotopeResults = new HashMap<>();

        Predicate<IObsData> predicate = (obsData) -> {return true;};

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
    }

    private void addSecondaryInspectionDetails(){
        document.add(new Paragraph("Secondary Inspection Details").setFontSize(12));

        Map<String, String> secInsDetailsMap = new HashMap<>();

        Predicate<IObsData> predicate = (obsData) -> {return true;};

        long value = Utils.countObservations(new String[]{RADHelper.DEF_ADJUDICATION}, module, predicate, begin, end);

        secInsDetailsMap.put("Primary Date", "");
        secInsDetailsMap.put("Primary Time", "");
        secInsDetailsMap.put("Event Record ID", "");
        secInsDetailsMap.put("Primary N.Sigma", "");
        secInsDetailsMap.put("SI Result", "");
        secInsDetailsMap.put("Cargo", "");
        secInsDetailsMap.put("Disposition #", "");

        var table = tableGenerator.addTable(secInsDetailsMap);
        document.add(table);

        document.add(new Paragraph("\n"));
    }
}
