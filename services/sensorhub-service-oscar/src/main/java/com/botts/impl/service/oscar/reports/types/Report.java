package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.reports.helpers.ReportType;
import com.itextpdf.layout.Document;

public abstract class Report {

    public abstract String generate();
}
