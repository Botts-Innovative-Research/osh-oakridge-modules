package com.botts.impl.service.oscar.reports.helpers;

import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.util.*;

public class TableGenerator {

    public TableGenerator() {}

    public Table addLanesTable(Map<String, Map<String, String>> tableData){
        if (tableData.isEmpty()) return null;

        List<String> statKeys = new ArrayList<>(tableData.values().iterator().next().keySet());

        float[] columnWidths = new float[statKeys.size() + 1];
        Arrays.fill(columnWidths, 1.0f);

        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(75));

        // header row
        table.addHeaderCell(createHeaderCell("Lane UID"));
        for(String stat : statKeys){
            table.addHeaderCell(createHeaderCell(stat));
        }

        for(Map.Entry<String, Map<String, String>> entry : tableData.entrySet()){
            String laneUID = entry.getKey();
            Map<String, String> laneData = entry.getValue();

            table.addCell(createValueCell(laneUID));
            for (String stat : statKeys) {
                String value = laneData.get(stat);
                table.addCell(createValueCell(value));
            }
        }

        return table;
    }

    public Table addListToTable(Map<String, List<Map<String, String>>> adjTableData) {
        if (adjTableData.isEmpty()) return null;

        List<Map<String,String>> firstLaneList = adjTableData.values().iterator().next();
        if (firstLaneList.isEmpty()) return null;
        List<String> headers = new ArrayList<>(firstLaneList.get(0).keySet());

        headers.add(0, "Lane UID");
        float[] columnWidths = new float[] {
                1.0f,
                0.5f,
                1.0f,
                1.5f,
                1.2f,
                0.6f,
                2.0f,
                1.2f,
                0.8f,
                2.0f,
                1.0f,
                1.0f
        };

        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100));

        for (String header : headers) {
            table.addHeaderCell(createHeaderCell(header));
        }

        for (Map.Entry<String, List<Map<String,String>>> laneEntry : adjTableData.entrySet()) {
            String laneUID = laneEntry.getKey();
            List<Map<String,String>> records = laneEntry.getValue();

            boolean firstRow = true;
            for (Map<String,String> record : records) {
                table.addCell(createValueCell(firstRow ? laneUID : ""));
                firstRow = false;

                for (int i = 1; i < headers.size(); i++) {
                    table.addCell(createValueCell(record.get(headers.get(i))));
                }
            }
        }
        return table;
    }



    public Table addTable(Map<String, String> tableData){
        int columnCount = tableData.size();
        float[] columnWidths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnWidths[i] = 1;
        }
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(75));

        for(Map.Entry<String, String> entry : tableData.entrySet()){
            table.addHeaderCell(createHeaderCell(entry.getKey()));
        }

        for(Map.Entry<String, String> entry : tableData.entrySet()){
            table.addCell(createValueCell(entry.getValue()));
        }

        return table;
    }

    public Table addTableByDate(Map<String, Map<String, String>> tableDataByDate){
        if (tableDataByDate.isEmpty()) {
            return new Table(1);
        }

        // Get all unique categories across all dates
        Set<String> allCategories = new LinkedHashSet<>();
        for (Map<String, String> dateData : tableDataByDate.values()) {
            allCategories.addAll(dateData.keySet());
        }

        // Create table with columns: Date + all categories
        int columnCount = 1 + allCategories.size(); // 1 for date column
        float[] columnWidths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnWidths[i] = 1;
        }

        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(75));

        table.addHeaderCell(createHeaderCell("Date"));
        for (String category : allCategories) {
            table.addHeaderCell(createHeaderCell(category));
        }

        for (Map.Entry<String, Map<String, String>> dateEntry : tableDataByDate.entrySet()) {
            String date = dateEntry.getKey();
            Map<String, String> categoryData = dateEntry.getValue();

            table.addCell(createValueCell(date));

            for (String category : allCategories) {
                String value = categoryData.getOrDefault(category, "0");
                table.addCell(createValueCell(value));
            }
        }

        return table;
    }

    public Cell createHeaderCell(String header){
        return new Cell()
                .add(new Paragraph(header))
                .setFontSize(8)
                .setBackgroundColor(DeviceGray.GRAY)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    public Cell createValueCell(String content) {

        Paragraph p = new Paragraph(content)
                .setFontSize(7)
                .setMultipliedLeading(1f)
                .setMargin(0)
                .setPadding(0);

        Cell cell = new Cell()
                .add(p)
                .setPadding(5)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        return cell;
    }

}