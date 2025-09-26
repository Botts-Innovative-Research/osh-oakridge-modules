package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ChartGenerator;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
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

public class EventReportTodo extends Report {
    private static final Logger log = LoggerFactory.getLogger(EventReportTodo.class);

    String reportTitle = "Event Report";

    Document document;
    PdfWriter pdfWriter;
    PdfDocument pdfDocument;
    String pdfFileName;

    OSCARServiceModule module;
    TableGenerator tableGenerator;
    ChartGenerator chartGenerator;

    String laneUID;
    String eventId;
    Instant start;
    Instant end;

    public EventReportTodo(Instant startTime, Instant endTime, String eventId, String laneUID, OSCARServiceModule module) {
        try {

            pdfFileName = ReportCmdType.EVENT.name()+ "_" + laneUID + "_" + eventId + "_" + startTime + "_"+ endTime + ".pdf";
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
        this.eventId = eventId;
        this.laneUID = laneUID;
        this.module = module;
        this.start = startTime;
        this.end = endTime;
        this.tableGenerator = new TableGenerator();
        this.chartGenerator = new ChartGenerator();

    }

    @Override
    public String generate() {
        addHeader();

        // check for data before calling these
//        addAlarmStatistics();
//        addFaultStatistics();
//        addStatistics();
//        addOccupancyStatistics();

        document.close();
        pdfDocument.close();
        chartGenerator = null;
        tableGenerator = null;
        return pdfFileName;
    }

    private void addHeader(){
        document.add(new Paragraph(reportTitle).setFontSize(16).simulateBold());
        document.add(new Paragraph("Event Report").setFontSize(16).simulateBold());
        document.add(new Paragraph("Lane ID:" + laneUID).setFontSize(12));
        document.add(new Paragraph("Event ID:" + eventId).setFontSize(12));
        document.add(new Paragraph("\n"));
    }

    private void addOccupancyStatistics(){
        document.add(new Paragraph("Occupancy Statistics").setFontSize(12));
    }

    private void addAlarmStatisticsByDay(){
        document.add(new Paragraph("Alarm Statistics").setFontSize(12));

        String[] observedProperties = new String[]{RADHelper.DEF_OCCUPANCY};

        Predicate<IObsData> gammaNeutronPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);};

        Predicate<IObsData> gammaPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);};

        Predicate<IObsData> neutronPredicate = (obsData) -> {return !obsData.getResult().getBooleanValue(5) && obsData.getResult().getBooleanValue(6);};;

        Predicate<IObsData> occupancyTotalPredicate = (obsData) -> {return true;};

        Map<Instant, Long> gammaDaily = Utils.countObservationsByDay(observedProperties, module, gammaPredicate, start, end);
        Map<Instant, Long> neutronDaily = Utils.countObservationsByDay(observedProperties, module, neutronPredicate, start, end);
        Map<Instant, Long> gammaNeutronDaily = Utils.countObservationsByDay(observedProperties, module, gammaNeutronPredicate, start, end);
        Map<Instant, Long> totalOccupancyDaily = Utils.countObservationsByDay(observedProperties, module, occupancyTotalPredicate, start, end);


        DefaultCategoryDataset gammaDailyDataSet = new DefaultCategoryDataset();
        for(Map.Entry<Instant, Long> entry : gammaDaily.entrySet()){
            gammaDailyDataSet.addValue(entry.getValue(), "Daily", "Gamma");
        }


        DefaultCategoryDataset gammaNeutronDailyDataSet = new DefaultCategoryDataset();
        for(Map.Entry<Instant, Long> entry : gammaNeutronDaily.entrySet()){
            gammaNeutronDailyDataSet.addValue(entry.getValue(), "Daily", "Gamma");
        }
        DefaultCategoryDataset neutronDailyDataSet = new DefaultCategoryDataset();
        for(Map.Entry<Instant, Long> entry : neutronDaily.entrySet()){
            neutronDailyDataSet.addValue(entry.getValue(), "Daily", "Neutron");
        }
        DefaultCategoryDataset totalOccupancyDataSet = new DefaultCategoryDataset();
        for(Map.Entry<Instant, Long> entry : totalOccupancyDaily.entrySet()){
            totalOccupancyDataSet.addValue(entry.getValue(), "Daily", "Total");
        }

    }

    private void addFaultStatistics(){
        document.add(new Paragraph("Fault Statistics").setFontSize(12));
        //        addTableToPdf(alarmOccupancyHeaders, null);

    }

    private void addStatistics(){
        document.add(new Paragraph("Statistics").setFontSize(12));
        //        addTableToPdf(alarmOccupancyHeaders, null);

    }


}
