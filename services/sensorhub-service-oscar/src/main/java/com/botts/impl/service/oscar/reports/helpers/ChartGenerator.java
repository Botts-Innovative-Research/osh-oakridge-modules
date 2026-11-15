/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.reports.helpers;

import com.botts.impl.service.oscar.OSCARServiceModule;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.awt.Color;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure vector PDF chart renderer built on Apache PDFBox 3.
 * Produces crisp, resolution-independent stacked bar charts, dual-axis bar + line charts,
 * and single-bar-per-category horizontal charts without raster images or AWT BufferedImages.
 */
public class ChartGenerator {

    public record StackedSeries(String name, Color color, List<Long> values) {}

    public record LineSeries(String name, Color color, List<Long> values) {}

    public record LegendItem(String label, Color color, boolean isLine) {
        public LegendItem(String label, Color color) {
            this(label, color, false);
        }
    }

    public record HorizontalBarItem(
            String categoryLabel,
            double value,
            String valueCallout,
            Color barColor,
            boolean emphasize
    ) {}

    // High-contrast accessible series palette
    public static final Color COLOR_GAMMA = new Color(220, 38, 38);           // #DC2626 Crimson
    public static final Color COLOR_GAMMA_NEUTRON = new Color(124, 58, 237);  // #7C3AED Purple
    public static final Color COLOR_NEUTRON = new Color(37, 99, 235);         // #2563EB Royal Blue
    public static final Color COLOR_EML_SUPPRESSED = new Color(13, 148, 136); // #0D9488 Teal
    public static final Color COLOR_GAMMA_LOW = new Color(245, 158, 11);      // #F59E0B Amber
    public static final Color COLOR_TAMPER = new Color(217, 119, 6);          // #D97706 Deep Orange
    public static final Color COLOR_EXTENDED_OCCUPANCY = new Color(124, 58, 237); // #7C3AED Purple
    public static final Color COLOR_OCCUPANCY_LINE = new Color(3, 105, 161);  // #0369A1 Ocean Blue

    // Disposition group palette
    public static final Color GROUP_REAL_ALARM = new Color(220, 38, 38);      // #DC2626
    public static final Color GROUP_INNOCENT = new Color(37, 99, 235);        // #2563EB
    public static final Color GROUP_FALSE_ALARM = new Color(22, 163, 74);     // #16A34A
    public static final Color GROUP_TEST_MAINT = new Color(217, 119, 6);      // #D97706
    public static final Color GROUP_TAMPER_FAULT = new Color(124, 58, 237);   // #7C3AED
    public static final Color GROUP_AUTOMATED = new Color(13, 148, 136);      // #0D9488
    public static final Color GROUP_CUSTOM = new Color(2, 132, 199);          // #0284C7
    public static final Color GROUP_OTHER = new Color(100, 116, 139);         // #64748B

    private static final int DEFAULT_GRID_INTERVALS = 5;

    private final OSCARServiceModule module;

    public ChartGenerator() {
        this(null);
    }

    public ChartGenerator(OSCARServiceModule module) {
        this.module = module;
    }

    public static Color colorForDispositionGroup(String group) {
        if (group == null) {
            return GROUP_OTHER;
        }
        return switch (group) {
            case "Real Alarm" -> GROUP_REAL_ALARM;
            case "Innocent Alarm" -> GROUP_INNOCENT;
            case "False Alarm" -> GROUP_FALSE_ALARM;
            case "Test/Maintenance" -> GROUP_TEST_MAINT;
            case "Tamper/Fault" -> GROUP_TAMPER_FAULT;
            case "Automated" -> GROUP_AUTOMATED;
            case "Custom" -> GROUP_CUSTOM;
            default -> GROUP_OTHER;
        };
    }

    /**
     * Draws a vertical stacked bar chart card (used for Alarms and SOH daily reports).
     */
    public void drawStackedBarChart(
            PdfDocumentBuilder pdf,
            String title,
            String xAxisLabel,
            String yAxisLabel,
            List<String> categories,
            List<StackedSeries> barSeries
    ) throws IOException {
        drawDualAxisStackedBarLineChart(pdf, title, xAxisLabel, yAxisLabel, categories, barSeries, null, null);
    }

    /**
     * Draws a vertical stacked bar chart with an optional independent right-axis line + marker overlay
     * (used for Alarms and Occupancies so high occupancy counts never crush small alarm counts).
     */
    public void drawDualAxisStackedBarLineChart(
            PdfDocumentBuilder pdf,
            String title,
            String xAxisLabel,
            String leftYAxisLabel,
            List<String> categories,
            List<StackedSeries> barSeries,
            String rightYAxisLabel,
            LineSeries lineSeries
    ) throws IOException {
        int numCats = categories != null ? categories.size() : 0;
        boolean rotateXLabels = numCats > 6;
        final float rotatedFontSize = 6.8f;
        final double rotatedAngleRad = Math.toRadians(35.0);
        final float maxRotatedLabelWidth = 76f;

        PDType0Font regular = pdf.getRegularFont();
        PDType0Font bold = pdf.getBoldFont();

        // Legend items (computed upfront so multi-row legends and rotated X labels size the card accurately)
        List<LegendItem> legendItems = new ArrayList<>();
        if (barSeries != null) {
            for (StackedSeries s : barSeries) {
                legendItems.add(new LegendItem(s.name(), s.color(), false));
            }
        }
        if (lineSeries != null) {
            legendItems.add(new LegendItem(lineSeries.name(), lineSeries.color(), true));
        }

        float cardW = pdf.getContentWidth();
        int legendRows = Math.max(1, estimateLegendRows(pdf, legendItems, cardW - 24f));

        int maxLabels = rotateXLabels ? 14 : 6;
        int stride = numCats > 0 ? Math.max(1, (int) Math.ceil(numCats / (double) maxLabels)) : 1;
        boolean[] labelMask = new boolean[numCats];
        float maxPreClippedRotatedW = 0f;
        if (numCats > 0) {
            float estPlotW = Math.max(120f, cardW - 76f);
            float estSlotW = estPlotW / numCats;
            int minGapSlots;
            if (stride <= 1 && (!rotateXLabels || estSlotW * Math.sin(rotatedAngleRad) >= rotatedFontSize * 1.6f)) {
                minGapSlots = 1;
            } else if (rotateXLabels) {
                int perpMinSlots = (int) Math.ceil((rotatedFontSize * 1.75f)
                        / Math.max(1f, estSlotW * (float) Math.sin(rotatedAngleRad)));
                minGapSlots = Math.max(2, Math.max((stride + 1) / 2, perpMinSlots));
            } else {
                minGapSlots = Math.max(2, (stride + 1) / 2);
            }
            int prevLabeledIdx = -minGapSlots;
            for (int i = 0; i < numCats; i++) {
                boolean isFirst = (i == 0);
                boolean isLast = (i == numCats - 1);
                boolean shouldLabel = isFirst || (i % stride == 0 && (numCats - 1 - i) >= minGapSlots) || isLast;
                if (!shouldLabel || (i - prevLabeledIdx < minGapSlots && !isLast)) {
                    continue;
                }
                prevLabeledIdx = i;
                labelMask[i] = true;
                if (rotateXLabels) {
                    String rawCat = categories.get(i) != null ? categories.get(i) : "";
                    String preClipped = pdf.truncateToWidth(regular, rotatedFontSize, rawCat, maxRotatedLabelWidth);
                    float w = pdf.measureTextWidth(regular, rotatedFontSize, preClipped);
                    if (w > maxPreClippedRotatedW) {
                        maxPreClippedRotatedW = w;
                    }
                }
            }
        }

        boolean hasXAxisTitle = xAxisLabel != null && !xAxisLabel.isBlank();
        float bottomMargin;
        if (numCats > 0 && rotateXLabels) {
            float xTitleReserve = hasXAxisTitle ? 19f : 7f;
            bottomMargin = Math.max(46f,
                    (float) Math.ceil(maxPreClippedRotatedW * Math.sin(rotatedAngleRad)) + 11f + xTitleReserve);
        } else {
            bottomMargin = hasXAxisTitle ? 32f : 20f;
        }

        float cardHeight = 200f + bottomMargin + (legendRows - 1) * 12f;
        pdf.ensureSpace(cardHeight + 10f);

        float cardX = PdfDocumentBuilder.MARGIN_X;
        float cardTopY = pdf.getCursorY();
        float cardBottomY = cardTopY - cardHeight;

        PDPageContentStream cs = pdf.getContentStream();

        // Card container
        cs.setNonStrokingColor(Color.WHITE);
        cs.setStrokingColor(PdfDocumentBuilder.SLATE_200);
        cs.setLineWidth(0.75f);
        cs.addRect(cardX, cardBottomY, cardW, cardHeight);
        cs.fillAndStroke();

        // Chart title
        pdf.drawText(bold, 10f, PdfDocumentBuilder.SLATE_900, cardX + 12f, cardTopY - 16f, title);

        float legendBottomY = drawLegendRow(pdf, legendItems, cardX + 12f, cardTopY - 29f, cardW - 24f);

        if (numCats == 0) {
            pdf.drawText(regular, 8.5f, PdfDocumentBuilder.SLATE_500,
                    cardX + 16f, (cardTopY + cardBottomY) * 0.5f, "No time intervals in requested window.");
            pdf.setCursorY(cardBottomY - 12f);
            return;
        }

        boolean hasRightAxis = lineSeries != null && lineSeries.values() != null && !lineSeries.values().isEmpty();

        // Compute max values
        long maxStacked = 0L;
        long[] dayBarTotals = new long[numCats];
        if (barSeries != null) {
            for (int i = 0; i < numCats; i++) {
                long sum = 0L;
                for (StackedSeries s : barSeries) {
                    if (s.values() != null && i < s.values().size()) {
                        sum += Math.max(0L, s.values().get(i));
                    }
                }
                dayBarTotals[i] = sum;
                if (sum > maxStacked) {
                    maxStacked = sum;
                }
            }
        }

        long maxLine = 0L;
        if (hasRightAxis) {
            for (int i = 0; i < Math.min(numCats, lineSeries.values().size()); i++) {
                long v = Math.max(0L, lineSeries.values().get(i));
                if (v > maxLine) {
                    maxLine = v;
                }
            }
        }

        int intervals = DEFAULT_GRID_INTERVALS;
        long leftStep = computeNiceIntegerStep(maxStacked, intervals, !hasRightAxis && numCats <= 14 && maxStacked > 0);
        long leftAxisMax = leftStep * intervals;

        long rightStep = hasRightAxis ? computeNiceIntegerStep(maxLine, intervals, maxLine > 0) : 1L;
        long rightAxisMax = rightStep * intervals;

        NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
        float maxLeftTickW = pdf.measureTextWidth(regular, 7.5f, intFmt.format(leftAxisMax));
        float maxRightTickW = hasRightAxis ? pdf.measureTextWidth(regular, 7.5f, intFmt.format(rightAxisMax)) : 0f;

        boolean hasTopAxisTitles = (leftYAxisLabel != null && !leftYAxisLabel.isBlank())
                || (hasRightAxis && rightYAxisLabel != null && !rightYAxisLabel.isBlank());

        float basePlotLeft = cardX + Math.max(32f, 18f + maxLeftTickW);
        float plotLeft = (rotateXLabels && maxPreClippedRotatedW > 44f)
                ? Math.max(basePlotLeft, cardX + 48f)
                : basePlotLeft;
        float plotRight = hasRightAxis
                ? (cardX + cardW - Math.max(34f, 18f + maxRightTickW))
                : (cardX + cardW - 16f);
        float plotTop = legendBottomY - (hasTopAxisTitles ? 18f : 10f);
        float plotBottom = cardBottomY + bottomMargin;
        float plotW = Math.max(40f, plotRight - plotLeft);
        float plotH = Math.max(40f, plotTop - plotBottom);

        // Horizontal gridlines + Left & Right Y-axis tick labels
        for (int t = 0; t <= intervals; t++) {
            float ratio = t / (float) intervals;
            float gy = plotBottom + ratio * plotH;

            cs.setStrokingColor(t == 0 ? PdfDocumentBuilder.SLATE_400 : PdfDocumentBuilder.SLATE_200);
            cs.setLineWidth(t == 0 ? 0.8f : 0.5f);
            cs.moveTo(plotLeft, gy);
            cs.lineTo(plotRight, gy);
            cs.stroke();

            String leftLabel = intFmt.format(t * leftStep);
            float lw = pdf.measureTextWidth(regular, 7.5f, leftLabel);
            pdf.drawText(regular, 7.5f, PdfDocumentBuilder.SLATE_600, plotLeft - 5f - lw, gy - 2.5f, leftLabel);

            if (hasRightAxis) {
                String rightLabel = intFmt.format(t * rightStep);
                pdf.drawText(regular, 7.5f, COLOR_OCCUPANCY_LINE, plotRight + 5f, gy - 2.5f, rightLabel);
            }
        }

        // Left & Right vertical axis lines
        cs.setStrokingColor(PdfDocumentBuilder.SLATE_400);
        cs.setLineWidth(0.75f);
        cs.moveTo(plotLeft, plotBottom);
        cs.lineTo(plotLeft, plotTop);
        cs.stroke();

        if (hasRightAxis) {
            cs.setStrokingColor(COLOR_OCCUPANCY_LINE);
            cs.setLineWidth(0.75f);
            cs.moveTo(plotRight, plotBottom);
            cs.lineTo(plotRight, plotTop);
            cs.stroke();
        }

        // Horizontal top-of-axis titles (eliminates vertical head-tilting and preserves horizontal text extraction)
        if (leftYAxisLabel != null && !leftYAxisLabel.isBlank()) {
            pdf.drawText(bold, 7.6f, PdfDocumentBuilder.SLATE_700,
                    cardX + 12f, plotTop + 6.5f, leftYAxisLabel);
        }
        if (hasRightAxis && rightYAxisLabel != null && !rightYAxisLabel.isBlank()) {
            float titleW = pdf.measureTextWidth(bold, 7.6f, rightYAxisLabel);
            pdf.drawText(bold, 7.6f, COLOR_OCCUPANCY_LINE,
                    cardX + cardW - 12f - titleW, plotTop + 6.5f, rightYAxisLabel);
        }
        if (xAxisLabel != null && !xAxisLabel.isBlank()) {
            float xTitleW = pdf.measureTextWidth(bold, 7.8f, xAxisLabel);
            pdf.drawText(bold, 7.8f, PdfDocumentBuilder.SLATE_700,
                    plotLeft + (plotW - xTitleW) * 0.5f, cardBottomY + 7f, xAxisLabel);
        }

        // Draw stacked bars
        float slotW = plotW / numCats;
        float barW = Math.min(32f, Math.max(1.5f, slotW * 0.62f));

        if (barSeries != null && !barSeries.isEmpty()) {
            for (int i = 0; i < numCats; i++) {
                float cx = plotLeft + (i + 0.5f) * slotW;
                float bx = cx - barW * 0.5f;
                float currentBaseY = plotBottom;
                int drawnSegments = 0;

                for (StackedSeries s : barSeries) {
                    long val = (s.values() != null && i < s.values().size()) ? Math.max(0L, s.values().get(i)) : 0L;
                    if (val <= 0L) {
                        continue;
                    }
                    float segH = (val / (float) leftAxisMax) * plotH;
                    segH = Math.max(1.2f, segH);

                    cs.setNonStrokingColor(s.color());
                    cs.addRect(bx, currentBaseY, barW, segH);
                    cs.fill();

                    if (drawnSegments > 0 && barW >= 3f) {
                        cs.setStrokingColor(Color.WHITE);
                        cs.setLineWidth(0.6f);
                        cs.moveTo(bx, currentBaseY);
                        cs.lineTo(bx + barW, currentBaseY);
                        cs.stroke();
                    }

                    currentBaseY += segH;
                    drawnSegments++;
                }

                // Direct bar-total callout only on single-day charts (multi-day reports pair the chart with the daily breakdown table)
                if (!hasRightAxis && numCats == 1 && dayBarTotals[i] > 0) {
                    String totalStr = intFmt.format(dayBarTotals[i]);
                    float tw = pdf.measureTextWidth(bold, 7f, totalStr);
                    float labelY = Math.min(plotTop - 5f, currentBaseY + 3f);
                    pdf.drawText(bold, 7f, PdfDocumentBuilder.SLATE_700, cx - tw * 0.5f, labelY, totalStr);
                }
            }
        }

        // Draw secondary line + markers + 1-day/compact callouts
        if (hasRightAxis) {
            int nPts = Math.min(numCats, lineSeries.values().size());
            float[] px = new float[nPts];
            float[] py = new float[nPts];
            for (int i = 0; i < nPts; i++) {
                long v = Math.max(0L, lineSeries.values().get(i));
                px[i] = plotLeft + (i + 0.5f) * slotW;
                py[i] = plotBottom + (v / (float) rightAxisMax) * plotH;
            }

            if (nPts == 1) {
                // Dashed horizontal guide to right Y-axis so a single-day point has unambiguous scale context
                cs.setLineDashPattern(new float[]{3f, 3f}, 0f);
                cs.setStrokingColor(COLOR_OCCUPANCY_LINE);
                cs.setLineWidth(0.8f);
                cs.moveTo(px[0], py[0]);
                cs.lineTo(plotRight, py[0]);
                cs.stroke();
                cs.setLineDashPattern(new float[]{}, 0f);

                pdf.drawFilledCircle(px[0], py[0], 4.6f, Color.WHITE, COLOR_OCCUPANCY_LINE, 1.4f);
                pdf.drawFilledCircle(px[0], py[0], 2.4f, PdfDocumentBuilder.SLATE_900, null, 0f);

                String callout = "Occupancies: " + intFmt.format(Math.max(0L, lineSeries.values().get(0)));
                float cw = pdf.measureTextWidth(bold, 7.5f, callout) + 10f;
                float ch = 13f;
                float badgeX = Math.max(plotLeft + 4f, Math.min(plotRight - cw - 4f, px[0] - cw * 0.5f));
                float badgeY = (py[0] + 7f + ch <= plotTop) ? (py[0] + 7f) : Math.max(plotBottom + 4f, py[0] - ch - 7f);

                cs.setNonStrokingColor(Color.WHITE);
                cs.setStrokingColor(COLOR_OCCUPANCY_LINE);
                cs.setLineWidth(0.7f);
                cs.addRect(badgeX, badgeY, cw, ch);
                cs.fillAndStroke();
                pdf.drawText(bold, 7.5f, PdfDocumentBuilder.SLATE_900, badgeX + 5f, badgeY + 3.5f, callout);
            } else if (nPts >= 2) {
                cs.setStrokingColor(lineSeries.color());
                cs.setLineWidth(1.75f);
                cs.moveTo(px[0], py[0]);
                for (int i = 1; i < nPts; i++) {
                    cs.lineTo(px[i], py[i]);
                }
                cs.stroke();

                for (int i = 0; i < nPts; i++) {
                    long val = Math.max(0L, lineSeries.values().get(i));
                    if (nPts <= 35 || val > 0L || i == 0 || i == nPts - 1) {
                        float rOuter = nPts <= 20 ? 3.2f : 2.3f;
                        float rInner = nPts <= 20 ? 1.8f : 1.3f;
                        pdf.drawFilledCircle(px[i], py[i], rOuter, Color.WHITE, lineSeries.color(), 1.1f);
                        pdf.drawFilledCircle(px[i], py[i], rInner, lineSeries.color(), null, 0f);
                    }
                }
            }
        }

        // X-axis category ticks and auto-thinned labels
        for (int i = 0; i < numCats; i++) {
            if (!labelMask[i]) {
                continue;
            }

            float cx = plotLeft + (i + 0.5f) * slotW;
            cs.setStrokingColor(PdfDocumentBuilder.SLATE_400);
            cs.setLineWidth(0.6f);
            cs.moveTo(cx, plotBottom);
            cs.lineTo(cx, plotBottom - 3.5f);
            cs.stroke();

            String cat = categories.get(i) != null ? categories.get(i) : "";
            if (!rotateXLabels) {
                String fittedCat = pdf.truncateToWidth(regular, 7.5f, cat, Math.max(36f, slotW - 4f));
                float cw = pdf.measureTextWidth(regular, 7.5f, fittedCat);
                float lx = Math.max(cardX + 6f, Math.min(cardX + cardW - 6f - cw, cx - cw * 0.5f));
                pdf.drawText(regular, 7.5f, PdfDocumentBuilder.SLATE_600, lx, plotBottom - 12f, fittedCat);
            } else {
                // Positive counter-clockwise rotation (+35 deg) in PDFBox (+Y up) slopes from
                // bottom-left (tx, ty) up-right to terminate directly under the tick at (cx + 2, plotBottom - 8.5).
                float capSin = rotatedFontSize * 0.75f * (float) Math.sin(rotatedAngleRad);
                float maxAvailCosW = (float) (((cx + 2f) - (cardX + 5f) - capSin) / Math.cos(rotatedAngleRad));
                float allowedW = Math.max(24f, Math.min(maxRotatedLabelWidth, maxAvailCosW));
                String fittedCat = pdf.truncateToWidth(regular, rotatedFontSize, cat, allowedW);
                float cw = pdf.measureTextWidth(regular, rotatedFontSize, fittedCat);
                float tx = cx + 2f - (float) (cw * Math.cos(rotatedAngleRad));
                float ty = plotBottom - 8.5f - (float) (cw * Math.sin(rotatedAngleRad));
                pdf.drawRotatedText(regular, rotatedFontSize, PdfDocumentBuilder.SLATE_600, tx, ty, rotatedAngleRad, fittedCat);
            }
        }

        pdf.setCursorY(cardBottomY - 12f);
    }

    /**
     * Draws a horizontal single-bar-per-category chart card (used for Disposition and Isotope Results).
     * Eliminates the legacy empty-series-slot hairline bug and renders direct end-of-bar value labels.
     */
    public void drawHorizontalBarChart(
            PdfDocumentBuilder pdf,
            String title,
            String xAxisLabel,
            List<HorizontalBarItem> items,
            List<LegendItem> groupLegend,
            boolean isPercentageScale,
            boolean compactRows
    ) throws IOException {
        if (items == null || items.isEmpty()) {
            pdf.drawCalloutBox("No category items available for " + title + ".");
            return;
        }

        PDType0Font regular = pdf.getRegularFont();
        PDType0Font bold = pdf.getBoldFont();

        float rowHeight = compactRows ? 11.4f : 18f;
        float barHeight = compactRows ? 7.4f : 11.2f;
        float labelFontSize = compactRows ? 7.2f : 7.8f;

        // Estimate legend height before computing card height
        float cardW = pdf.getContentWidth();
        int legendRows = estimateLegendRows(pdf, groupLegend, cardW - 24f);
        float headerBlockH = 22f + (legendRows > 0 ? legendRows * 12f + 4f : 0f);
        float footerBlockH = 30f;
        float plotH = items.size() * rowHeight + 6f;
        float cardHeight = headerBlockH + plotH + footerBlockH;

        pdf.ensureSpace(cardHeight + 10f);

        float cardX = PdfDocumentBuilder.MARGIN_X;
        float cardTopY = pdf.getCursorY();
        float cardBottomY = cardTopY - cardHeight;
        PDPageContentStream cs = pdf.getContentStream();

        // Card container
        cs.setNonStrokingColor(Color.WHITE);
        cs.setStrokingColor(PdfDocumentBuilder.SLATE_200);
        cs.setLineWidth(0.75f);
        cs.addRect(cardX, cardBottomY, cardW, cardHeight);
        cs.fillAndStroke();

        // Title & group legend
        pdf.drawText(bold, 10f, PdfDocumentBuilder.SLATE_900, cardX + 12f, cardTopY - 16f, title);
        float legendBottomY = (groupLegend != null && !groupLegend.isEmpty())
                ? drawLegendRow(pdf, groupLegend, cardX + 12f, cardTopY - 29f, cardW - 24f)
                : (cardTopY - 20f);

        // Compute left category column width
        float maxCatLabelW = 0f;
        float maxAllowedLabelW = compactRows ? 140f : 255f;
        for (HorizontalBarItem item : items) {
            PDType0Font f = item.emphasize() ? bold : regular;
            float w = pdf.measureTextWidth(f, labelFontSize, item.categoryLabel());
            if (w > maxCatLabelW) {
                maxCatLabelW = w;
            }
        }
        float labelColW = Math.max(compactRows ? 74f : 110f, Math.min(maxAllowedLabelW, maxCatLabelW + 10f));

        // Compute max callout width so value labels never touch the right card border
        float maxCalloutW = 24f;
        double maxValue = 0.0;
        for (HorizontalBarItem item : items) {
            if (item.value() > maxValue) {
                maxValue = item.value();
            }
            if (item.valueCallout() != null) {
                float cw = pdf.measureTextWidth(bold, labelFontSize, item.valueCallout());
                if (cw > maxCalloutW) {
                    maxCalloutW = cw;
                }
            }
        }

        float plotLeft = cardX + 12f + labelColW;
        float plotRight = cardX + cardW - 14f;
        float plotTop = legendBottomY - 6f;
        float plotBottom = cardBottomY + footerBlockH;
        float plotW = Math.max(80f, plotRight - plotLeft);
        // Usable bar span leaves headroom on the right for the end-of-bar value callout
        float barMaxSpan = Math.max(40f, plotW - maxCalloutW - 12f);

        int intervals = DEFAULT_GRID_INTERVALS;
        double axisStep;
        double axisMax;
        if (isPercentageScale) {
            if (maxValue <= 0.0) {
                axisStep = 20.0;
            } else {
                double raw = maxValue / intervals;
                if (raw <= 2.0) axisStep = 2.0;
                else if (raw <= 5.0) axisStep = 5.0;
                else if (raw <= 10.0) axisStep = 10.0;
                else if (raw <= 20.0) axisStep = 20.0;
                else axisStep = 25.0;
            }
            axisMax = Math.min(100.0, Math.max(axisStep * intervals, Math.ceil(maxValue / axisStep) * axisStep));
            intervals = Math.max(1, (int) Math.round(axisMax / axisStep));
        } else {
            long countStep = computeNiceIntegerStep((long) Math.ceil(maxValue), intervals, false);
            axisStep = countStep;
            axisMax = countStep * intervals;
        }

        NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);

        // Vertical gridlines & X-axis tick labels
        for (int t = 0; t <= intervals; t++) {
            double tickVal = t * axisStep;
            float gx = plotLeft + (float) (tickVal / axisMax) * barMaxSpan;

            cs.setStrokingColor(t == 0 ? PdfDocumentBuilder.SLATE_400 : PdfDocumentBuilder.SLATE_200);
            cs.setLineWidth(t == 0 ? 0.8f : 0.5f);
            cs.moveTo(gx, plotBottom);
            cs.lineTo(gx, plotTop);
            cs.stroke();

            String tickText = isPercentageScale
                    ? String.format(Locale.US, "%.0f%%", tickVal)
                    : intFmt.format(Math.round(tickVal));
            float tw = pdf.measureTextWidth(regular, 7.3f, tickText);
            pdf.drawText(regular, 7.3f, PdfDocumentBuilder.SLATE_600, gx - tw * 0.5f, plotBottom - 10.5f, tickText);
        }

        // Bottom X-axis line
        cs.setStrokingColor(PdfDocumentBuilder.SLATE_400);
        cs.setLineWidth(0.75f);
        cs.moveTo(plotLeft, plotBottom);
        cs.lineTo(plotLeft + barMaxSpan, plotBottom);
        cs.stroke();

        // X-axis title
        if (xAxisLabel != null && !xAxisLabel.isBlank()) {
            float xw = pdf.measureTextWidth(bold, 7.8f, xAxisLabel);
            pdf.drawText(bold, 7.8f, PdfDocumentBuilder.SLATE_700,
                    plotLeft + (barMaxSpan - xw) * 0.5f, cardBottomY + 6.5f, xAxisLabel);
        }

        // Draw each category row (top to bottom)
        for (int i = 0; i < items.size(); i++) {
            HorizontalBarItem item = items.get(i);
            float rowTop = plotTop - 3f - i * rowHeight;
            float rowCenterY = rowTop - rowHeight * 0.5f;

            // Subtle zebra guide on odd rows for easy eye tracking across wide charts
            if (i % 2 == 1) {
                cs.setNonStrokingColor(PdfDocumentBuilder.SLATE_50);
                cs.addRect(cardX + 8f, rowTop - rowHeight, cardW - 16f, rowHeight);
                cs.fill();
            }

            // Re-draw zero axis tick line slice over row tint
            cs.setStrokingColor(PdfDocumentBuilder.SLATE_400);
            cs.setLineWidth(0.75f);
            cs.moveTo(plotLeft, rowTop - rowHeight);
            cs.lineTo(plotLeft, rowTop);
            cs.stroke();

            // Category label (right-aligned against plotLeft - 6f)
            PDType0Font catFont = item.emphasize() ? bold : regular;
            Color catColor = item.emphasize() ? PdfDocumentBuilder.SLATE_900 : PdfDocumentBuilder.SLATE_600;
            String fittedCat = pdf.truncateToWidth(catFont, labelFontSize, item.categoryLabel(), labelColW - 8f);
            float cw = pdf.measureTextWidth(catFont, labelFontSize, fittedCat);
            pdf.drawText(catFont, labelFontSize, catColor,
                    plotLeft - 6f - cw, rowCenterY - labelFontSize * 0.32f, fittedCat);

            // Full-thickness single bar
            float barW = 0f;
            if (item.value() > 0.0 && axisMax > 0.0) {
                barW = (float) Math.min(1.0, item.value() / axisMax) * barMaxSpan;
                barW = Math.max(2.0f, barW);
                Color fill = item.barColor() != null ? item.barColor() : COLOR_NEUTRON;
                cs.setNonStrokingColor(fill);
                cs.addRect(plotLeft + 0.4f, rowCenterY - barHeight * 0.5f, barW, barHeight);
                cs.fill();
            }

            // End-of-bar value callout
            if (item.valueCallout() != null && !item.valueCallout().isBlank()) {
                boolean nonZero = item.value() > 0.0;
                PDType0Font valFont = nonZero ? bold : regular;
                Color valColor = nonZero ? PdfDocumentBuilder.SLATE_900 : PdfDocumentBuilder.SLATE_400;
                pdf.drawText(valFont, labelFontSize - 0.2f, valColor,
                        plotLeft + barW + 4.5f, rowCenterY - labelFontSize * 0.32f, item.valueCallout());
            }
        }

        pdf.setCursorY(cardBottomY - 12f);
    }

    private int estimateLegendRows(PdfDocumentBuilder pdf, List<LegendItem> items, float maxWidth) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int rows = 1;
        float curX = 0f;
        for (LegendItem item : items) {
            float itemW = 14f + pdf.measureTextWidth(pdf.getRegularFont(), 7.6f, item.label()) + 12f;
            if (curX > 0f && curX + itemW > maxWidth) {
                rows++;
                curX = itemW;
            } else {
                curX += itemW;
            }
        }
        return rows;
    }

    private float drawLegendRow(PdfDocumentBuilder pdf, List<LegendItem> items, float startX, float topBaselineY, float maxWidth)
            throws IOException {
        if (items == null || items.isEmpty()) {
            return topBaselineY;
        }
        PDPageContentStream cs = pdf.getContentStream();
        PDType0Font regular = pdf.getRegularFont();
        float curX = startX;
        float curY = topBaselineY;

        for (LegendItem item : items) {
            float textW = pdf.measureTextWidth(regular, 7.6f, item.label());
            float itemW = 13f + textW + 12f;
            if (curX > startX && curX + itemW > startX + maxWidth) {
                curX = startX;
                curY -= 12f;
            }

            if (item.isLine()) {
                cs.setStrokingColor(item.color());
                cs.setLineWidth(1.6f);
                cs.moveTo(curX, curY + 2.8f);
                cs.lineTo(curX + 10f, curY + 2.8f);
                cs.stroke();
                pdf.drawFilledCircle(curX + 5f, curY + 2.8f, 2.1f, item.color(), Color.WHITE, 0.6f);
            } else {
                cs.setNonStrokingColor(item.color());
                cs.addRect(curX, curY - 0.5f, 7.5f, 7.5f);
                cs.fill();
            }

            pdf.drawText(regular, 7.6f, PdfDocumentBuilder.SLATE_700, curX + 11f, curY, item.label());
            curX += itemW;
        }
        return curY - 4f;
    }

    /**
     * Computes a clean integer tick step (1, 2, 5, 10, 20, 25, 50, 100, ...) so count axes
     * never render fractional decimals like "0.0000000" or "1.4".
     */
    static long computeNiceIntegerStep(long maxVal, int intervals, boolean leaveTopLabelHeadroom) {
        int safeIntervals = Math.max(1, intervals);
        long effectiveMax = Math.max(0L, maxVal);
        if (effectiveMax == 0L) {
            return 1L;
        }
        double targetMax = leaveTopLabelHeadroom ? (effectiveMax * 1.08) : (double) effectiveMax;
        double rawStep = Math.ceil(targetMax / safeIntervals);
        if (rawStep <= 1.0) {
            return 1L;
        }
        double mag = Math.pow(10.0, Math.floor(Math.log10(rawStep)));
        double norm = rawStep / mag;
        long niceMult;
        if (norm <= 1.0) niceMult = 1L;
        else if (norm <= 2.0) niceMult = 2L;
        else if (norm <= 2.5 && mag >= 10.0) niceMult = 25L; // handled below
        else if (norm <= 5.0) niceMult = 5L;
        else niceMult = 10L;

        long step;
        if (niceMult == 25L) {
            step = Math.max(1L, Math.round((mag / 10.0) * 25.0));
        } else {
            step = Math.max(1L, Math.round(mag * niceMult));
        }
        if (leaveTopLabelHeadroom && step * safeIntervals <= effectiveMax) {
            step = computeNiceIntegerStep(effectiveMax + step, safeIntervals, false);
        }
        return step;
    }

    public OSCARServiceModule getModule() {
        return module;
    }
}
