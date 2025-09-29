package com.botts.impl.service.oscar.reports.helpers;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
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

                chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);

                chart.getCategoryPlot().getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
                chart.getCategoryPlot().getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
                chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
                chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 12));



                BarRenderer renderer = (BarRenderer) chart.getCategoryPlot().getRenderer();
                Color color = new Color(109, 162, 231);
                renderer.setSeriesPaint(0, color);

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
            ChartUtilities.saveChartAsPNG(file, chart, 1200, 600);

            return file.getAbsolutePath();
        }

        return null;

    }
}
