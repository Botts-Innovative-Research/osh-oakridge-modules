package com.botts.impl.service.oscar.reports.handlers;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.types.RDSReport;
import com.botts.impl.service.oscar.reports.types.Report;

import java.io.OutputStream;
import java.time.Instant;

public class RdsSiteReportHandler implements IReportHandler {

    private final OSCARServiceModule module;

    public RdsSiteReportHandler(OSCARServiceModule module) {
        this.module = module;
    }
    @Override
    public void generateReport(OutputStream out, Instant start, Instant end) {
        Report report = new RDSReport(out, start, end, module);
        report.generate();
    }
}
