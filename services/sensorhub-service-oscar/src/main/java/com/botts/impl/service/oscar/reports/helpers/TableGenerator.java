package com.botts.impl.service.oscar.reports.helpers;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.util.HashMap;

public class TableGenerator {
    Document document;

    public TableGenerator(Document document) {
        this.document = document;
    }

    public void addTable(String[] headers, Object dataset){
        Table table = new Table(UnitValue.createPercentArray(8)).useAllAvailableWidth();

//        for(String header : headers){
//            table.addHeaderCell(header);
//        }
//
//        for(int i = 0; i < 10; i++){
//            table.addCell("cell example");
//        }

        document.add(table);
    }

    public void addTable(HashMap<String, Double> tableData){
        Table table = new Table(UnitValue.createPercentArray(8)).useAllAvailableWidth();

        for(String key : tableData.keySet()){
            table.addHeaderCell(key);
//            table.addCell(String.valueOf(tableData.get(key)));
        }
//        for(String header : headers){
//            table.addHeaderCell(header);
//        }
//
//        for(int i = 0; i < 10; i++){
//            table.addCell("cell example");
//        }

        document.add(table);
    }
}
