package com.botts.impl.system.database.types;

import com.itextpdf.layout.Document;

public abstract class Report {
    Document document;

    public Report(Document document) {
        this.document = document;
    }

    public abstract void generate();
}
