package com.botts.impl.service.oscar.reports.helpers;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

public class TableGenerator {
    Document document;

    public TableGenerator(Document document) {
        this.document = document;
    }

    protected void addTable(String[] headers, Object dataset){
        Table table = new Table(UnitValue.createPercentArray(8)).useAllAvailableWidth();

        for(String header : headers){
            table.addHeaderCell(header);
        }

        for(int i = 0; i < 10; i++){
            table.addCell("cell example");
        }

        document.add(table);
    }
}
