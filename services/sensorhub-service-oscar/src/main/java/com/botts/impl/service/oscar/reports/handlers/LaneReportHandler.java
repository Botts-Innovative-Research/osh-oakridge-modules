package com.botts.impl.service.oscar.reports.handlers;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.types.LaneReport;
import com.botts.impl.service.oscar.reports.types.RDSReport;
import com.botts.impl.service.oscar.reports.types.Report;

import java.io.OutputStream;
import java.time.Instant;

public class LaneReportHandler implements IReportHandler {

    String laneUIDs;
    OSCARServiceModule module;

    public LaneReportHandler(String laneUIDs, OSCARServiceModule module) {
        this.laneUIDs = laneUIDs;
        this.module = module;
    }

    @Override
    public void generateReport(OutputStream out, Instant start, Instant end) {
        Report report = new LaneReport(out, start, end, laneUIDs, module);
        report.generate();
    }
}
