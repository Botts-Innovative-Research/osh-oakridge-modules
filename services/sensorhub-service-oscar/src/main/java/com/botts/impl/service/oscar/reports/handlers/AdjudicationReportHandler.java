package com.botts.impl.service.oscar.reports.handlers;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.types.AdjudicationReport;
import com.botts.impl.service.oscar.reports.types.Report;

import java.io.OutputStream;
import java.time.Instant;

public class AdjudicationReportHandler implements IReportHandler {
    OSCARServiceModule module;
    String laneUIDs;

    public AdjudicationReportHandler(OSCARServiceModule module, String laneUIDs) {
        this.module = module;
        this.laneUIDs = laneUIDs;
    }

    @Override
    public void generateReport(OutputStream out, Instant start, Instant end) {
        Report report = new AdjudicationReport(out, start, end, laneUIDs, module);
        report.generate();
    }
}
