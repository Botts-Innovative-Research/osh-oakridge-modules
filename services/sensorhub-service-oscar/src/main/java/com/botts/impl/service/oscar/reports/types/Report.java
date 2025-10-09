package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.OutputStream;
import java.time.Instant;

public abstract class Report {

    OutputStream out;
    Instant start;
    Instant end;
    OSCARServiceModule module;

    protected Report(OutputStream out, Instant start, Instant end, OSCARServiceModule module) {
        this.out = out;
        this.start = start;
        this.end = end;
        this.module = module;
    }

    public abstract void generate() throws DataStoreException;

    public abstract String getReportType();
}