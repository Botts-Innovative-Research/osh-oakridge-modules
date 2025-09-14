package com.botts.impl.service.oscar.reports.types;

import com.itextpdf.layout.Document;

public abstract class Report {
    Document document;
    String startTime;
    String endTime;

    public Report(Document document, String startTime,  String endTime) {
        this.document = document;
    }

    public abstract void generate();
}
