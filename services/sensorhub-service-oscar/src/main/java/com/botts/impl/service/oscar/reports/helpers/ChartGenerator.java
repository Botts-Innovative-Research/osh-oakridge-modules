package com.botts.impl.service.oscar.reports.helpers;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;

public class ChartGenerator {


    public String createChart(String title, String xAxisLabel, String yAxisLabel, DefaultCategoryDataset dataset, String chartType, String outputFilePath) throws IOException {
        JFreeChart chart = null;

        switch(chartType.toLowerCase()) {
            case "bar":
                chart = ChartFactory.createBarChart(
                        title, xAxisLabel, yAxisLabel,
                        dataset,
                        PlotOrientation.VERTICAL, true, true, false
                );
                break;
            case "line":
                chart = ChartFactory.createLineChart(
                        title, xAxisLabel, yAxisLabel, dataset,
                        PlotOrientation.VERTICAL, true, true, false
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown chart type: " + chartType);
        }


        if(chart != null){
            File file = new File("files/reports/" + outputFilePath);
            file.getParentFile().mkdirs();
            ChartUtilities.saveChartAsPNG(file, chart, 800, 600);

            return file.getAbsolutePath();
        }

        return null;

    }
}
