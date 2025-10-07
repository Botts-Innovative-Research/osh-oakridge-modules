package com.botts.impl.service.oscar.reports.handlers;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.EventReportType;
import com.botts.impl.service.oscar.reports.types.EventReport;
import com.botts.impl.service.oscar.reports.types.Report;

import java.io.OutputStream;
import java.time.Instant;

public class EventReportHandler implements IReportHandler {

    OSCARServiceModule module;
    String laneUIDs;
    EventReportType eventReportType;

    public EventReportHandler(OSCARServiceModule module, String laneUIDs, EventReportType eventReportType) {
        this.module = module;
        this.laneUIDs = laneUIDs;
        this.eventReportType = eventReportType;
    }
    @Override
    public void generateReport(OutputStream out, Instant start, Instant end) {
        Report report = new EventReport(out, start, end, eventReportType, laneUIDs, module);
        report.generate();
    }
}
