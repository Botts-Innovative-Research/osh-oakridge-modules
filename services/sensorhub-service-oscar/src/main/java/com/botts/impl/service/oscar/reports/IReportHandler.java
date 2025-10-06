package com.botts.impl.service.oscar.reports;

import java.io.OutputStream;
import java.time.Instant;

public interface IReportHandler {
    void generate(OutputStream out, Instant start, Instant end);
}
