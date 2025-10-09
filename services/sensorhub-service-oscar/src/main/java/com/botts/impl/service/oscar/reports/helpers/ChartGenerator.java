package com.botts.impl.service.oscar.reports.helpers;

import com.botts.api.service.bucket.IBucketService;
import com.botts.impl.service.oscar.OSCARServiceModule;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.io.*;

public class ChartGenerator {

    OSCARServiceModule module;
    IBucketService bucketService;

    public ChartGenerator(OSCARServiceModule module) {
        this.module = module;
        this.bucketService = module.getBucketService();
    }


    public JFreeChart createChart(String title, String xAxisLabel, String yAxisLabel, DefaultCategoryDataset dataset, String chartType, PlotOrientation orientation) throws IOException {
        JFreeChart chart = null;

        switch(chartType.toLowerCase()) {
            case "bar":
                chart = ChartFactory.createBarChart(
                        title,
                        xAxisLabel,
                        yAxisLabel,
                        dataset,
                        orientation,
                        true,
                        true,
                        false
                );

                chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.STANDARD);

                chart.getCategoryPlot().getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
                chart.getCategoryPlot().getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
                chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));

                chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 12));
                chart.getLegend().setPosition(RectangleEdge.RIGHT);

                break;
            case "line":
                chart = ChartFactory.createLineChart(
                        title,
                        xAxisLabel,
                        yAxisLabel,
                        dataset,
                        orientation,
                        true,
                        true,
                        false
                );

                break;
            default:
                module.getLogger().error("Unknown chart type: {}", chartType);
        }

        if(chart == null)
            return null;

        return chart;
    }


    public JFreeChart createStackedBarChart(String title, String xAxisLabel, String yAxisLabel, DefaultCategoryDataset dataset) throws IOException {
        return ChartFactory.createStackedBarChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
    }


    public JFreeChart createStackedBarLineOverlayChart(String title, String xAxisLabel, String yAxisLabel, DefaultCategoryDataset stackedBarDataset, DefaultCategoryDataset lineDataset) throws IOException {
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

        return new JFreeChart(title, plot);

    }
}