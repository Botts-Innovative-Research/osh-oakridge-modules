package com.botts.impl.service.oscar.reports.helpers;

import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import java.util.Map;

public class TableGenerator {

    public TableGenerator() {}

    public Table addTable(Map<String, String> tableData){
        int columnCount = tableData.size();
        float[] columnWidths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnWidths[i] = 1;
        }
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        for(Map.Entry<String, String> entry : tableData.entrySet()){
            table.addHeaderCell(createHeaderCell(entry.getKey()));
        }

        for(Map.Entry<String, String> entry : tableData.entrySet()){
            table.addCell(createValueCell(entry.getValue()));
        }

       return table;
    }

    public Cell createHeaderCell(String header){
        return new Cell()
                .add(new Paragraph(header))
                .setFontSize(10)
                .setBackgroundColor(DeviceGray.GRAY)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    public Cell createValueCell(String value){
        return new Cell()
                .add(new Paragraph(value))
                .setFontSize(9)
                .setBackgroundColor(DeviceGray.WHITE)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }
}
