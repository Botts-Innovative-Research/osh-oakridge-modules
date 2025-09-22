package com.botts.impl.service.oscar.reports.helpers;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.util.HashMap;
import java.util.Map;

public class TableGenerator {
    Document document;

    public TableGenerator(Document document) {
        this.document = document;
    }

    public void addTable(Map<String, String> tableData){
        Table table = new Table(UnitValue.createPercentArray(8)).useAllAvailableWidth();

        for(String key : tableData.keySet()){
            table.addHeaderCell(key);
            table.addCell(tableData.get(key));
        }

        document.add(table);
    }
}
