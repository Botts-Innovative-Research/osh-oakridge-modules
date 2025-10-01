package com.botts.impl.service.oscar.reports.helpers;

import com.itextpdf.layout.renderer.LineRenderer;
import org.checkerframework.checker.units.qual.C;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeriesCollection;

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

                Color color = new Color(98, 216, 236);
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


    public String createStackedBarChart(String title, String xAxisLabel, String yAxisLabel, DefaultCategoryDataset dataset, String outputFilePath) throws IOException {
        JFreeChart chart = ChartFactory.createStackedBarChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);

        File file = new File("files/reports/" + outputFilePath);
        file.getParentFile().mkdirs();
        ChartUtilities.saveChartAsPNG(file, chart, 1200, 600);

        return file.getAbsolutePath();
    }


    public String createStackedBarLineOverlayChart(String title, String xAxisLabel, String yAxisLabel, DefaultCategoryDataset stackedBarDataset, DefaultCategoryDataset lineDataset, String outputFilePath) throws IOException {
        CategoryPlot plot = new CategoryPlot();

        CategoryAxis domainAxis = new CategoryAxis(xAxisLabel);
        NumberAxis rangeAxis = new NumberAxis(yAxisLabel);
        plot.setDomainAxis(domainAxis);
        plot.setRangeAxis(rangeAxis);

        plot.setDataset(0, stackedBarDataset);
        plot.setRenderer(0, new StackedBarRenderer());

        plot.setDataset(1, lineDataset);
        LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer();
        lineRenderer.setSeriesShapesVisible(0,true);
        lineRenderer.setSeriesLinesVisible(0,true);
        plot.setRenderer(1, lineRenderer);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        plot.setOrientation(PlotOrientation.VERTICAL);

        JFreeChart chart = new JFreeChart(title, plot);

        File file = new File("files/reports/" + outputFilePath);
        file.getParentFile().mkdirs();
        ChartUtilities.saveChartAsPNG(file, chart, 1200, 600);

        return file.getAbsolutePath();

    }
}