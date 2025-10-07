package com.botts.impl.service.oscar.reports.handlers;

import java.io.OutputStream;
import java.time.Instant;

public interface IReportHandler {
    void generateReport(OutputStream out, Instant start, Instant end);
}
