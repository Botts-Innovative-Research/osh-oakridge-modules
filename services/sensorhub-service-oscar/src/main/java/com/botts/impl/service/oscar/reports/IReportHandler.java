package com.botts.impl.service.oscar.reports;

import com.botts.impl.service.oscar.reports.types.Report;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.OutputStream;

public interface IReportHandler {

    Report createReport(OutputStream out) throws DataStoreException;

}
