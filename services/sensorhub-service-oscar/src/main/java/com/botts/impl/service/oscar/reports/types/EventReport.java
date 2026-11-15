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

package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ChartGenerator;
import com.botts.impl.service.oscar.reports.helpers.EventReportType;
import com.botts.impl.service.oscar.reports.helpers.PdfDocumentBuilder;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.ReportDataAggregator;
import com.botts.impl.service.oscar.reports.helpers.ReportFormat;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.botts.impl.service.oscar.reports.helpers.XlsxWorkbookWriter;
import com.botts.impl.service.oscar.reports.helpers.XlsxWorkbookWriter.CellValue;
import com.google.gson.stream.JsonWriter;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventReport extends Report {

    private final EventReportType eventType;
    private final String laneUIDs;
    private final ReportFormat format;
    private final TableGenerator tableGenerator;
    private final ChartGenerator chartGenerator;

    public EventReport(OutputStream outputStream, Instant startTime, Instant endTime,
                       EventReportType eventType, String laneUIDs, OSCARServiceModule module) {
        this(outputStream, startTime, endTime, eventType, laneUIDs, ReportFormat.PDF, module);
    }

    public EventReport(
            OutputStream outputStream,
            Instant startTime,
            Instant endTime,
            EventReportType eventType,
            String laneUIDs,
            ReportFormat format,
            OSCARServiceModule module
    ) {
        super(outputStream, startTime, endTime, module);
        this.eventType = eventType != null ? eventType : EventReportType.ALARMS;
        this.laneUIDs = laneUIDs;
        this.format = format != null ? format : ReportFormat.PDF;
        this.tableGenerator = new TableGenerator();
        this.chartGenerator = new ChartGenerator(module);
    }

    @Override
    public void generate() throws DataStoreException {
        String nodeId = (module != null && module.getConfiguration() != null && module.getConfiguration().nodeId != null)
                ? module.getConfiguration().nodeId
                : "oscar-node";
        String scopeLine = ReportDataAggregator.buildBannerScopeSummary(module, laneUIDs);

        Map<String, ReportDataAggregator.ObservationMetrics> dailyMetrics =
                ReportDataAggregator.aggregateDailyEventMetrics(module, laneUIDs, start, end, eventType);

        ReportDataAggregator.ObservationMetrics totals = new ReportDataAggregator.ObservationMetrics();
        for (ReportDataAggregator.ObservationMetrics m : dailyMetrics.values()) {
            totals.add(m);
        }

        boolean includeUpSet = (eventType == EventReportType.ALARMS || eventType == EventReportType.ALARMS_OCCUPANCIES)
                && format != ReportFormat.CSV;
        ReportDataAggregator.UpSetCoOccurrenceData upSetData = includeUpSet
                ? ReportDataAggregator.aggregateUpSetCoOccurrence(
                        module,
                        java.util.Arrays.asList(Utils.parseLaneUIDs(laneUIDs)),
                        start,
                        end,
                        null
                )
                : ReportDataAggregator.UpSetCoOccurrenceData.empty();

        try {
            switch (format) {
                case CSV -> writeCsv(dailyMetrics);
                case JSON -> writeJson(nodeId, scopeLine, dailyMetrics, totals, upSetData);
                case XLSX -> writeXlsx(nodeId, scopeLine, dailyMetrics, totals, upSetData);
                case PDF -> writePdf(nodeId, scopeLine, dailyMetrics, totals, upSetData);
            }
        } catch (IOException e) {
            throw new DataStoreException("Failed to generate Event Report (" + format + ")", e);
        }
    }

    private void writePdf(
            String nodeId,
            String scopeLine,
            Map<String, ReportDataAggregator.ObservationMetrics> dailyMetrics,
            ReportDataAggregator.ObservationMetrics totals,
            ReportDataAggregator.UpSetCoOccurrenceData upSetData
    ) throws IOException {
        NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);

        try (PdfDocumentBuilder pdf = new PdfDocumentBuilder("Event Report", nodeId)) {
            pdf.drawReportBanner(new PdfDocumentBuilder.HeaderMetadata(
                    "Event Report",
                    nodeId,
                    "Event Type: " + eventType,
                    scopeLine,
                    start,
                    end
            ));

            if (eventType == EventReportType.ALARMS_OCCUPANCIES) {
                renderAlarmsAndOccupancies(pdf, dailyMetrics, totals, upSetData, intFmt);
            } else if (eventType == EventReportType.ALARMS) {
                renderAlarmsOnly(pdf, dailyMetrics, totals, upSetData, intFmt);
            } else if (eventType == EventReportType.SOH) {
                renderSoh(pdf, dailyMetrics, totals, intFmt);
            } else {
                pdf.drawCalloutBox("No specific event subtype selected (Event Type: " + eventType + ").");
            }

            pdf.finishAndSave(out);
        }
    }

    private void writeCsv(
            Map<String, ReportDataAggregator.ObservationMetrics> dailyMetrics
    ) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            XlsxWorkbookWriter.writeCsvRow(writer, eventColumnHeaders());
            for (Map.Entry<String, ReportDataAggregator.ObservationMetrics> entry : dailyMetrics.entrySet()) {
                String date = entry.getKey();
                ReportDataAggregator.ObservationMetrics m = entry.getValue();
                XlsxWorkbookWriter.writeCsvRow(writer, buildCsvRow(date, m));
            }
            writer.flush();
        }
    }

    private List<String> eventColumnHeaders() {
        return switch (eventType) {
            case ALARMS_OCCUPANCIES -> List.of(
                    "Date (UTC)",
                    "Gamma",
                    "Gamma-Neutron",
                    "Neutron",
                    "EML-Suppressed",
                    "Total Alarms",
                    "Total Occupancy",
                    "Alarm Occupancy Rate (%)",
                    "EML Alarm Rate (%)"
            );
            case ALARMS -> List.of(
                    "Date (UTC)",
                    "Gamma",
                    "Gamma-Neutron",
                    "Neutron",
                    "EML-Suppressed",
                    "Total Alarms",
                    "EML Alarm Rate (%)"
            );
            case SOH -> List.of(
                    "Date (UTC)",
                    "Gamma High",
                    "Gamma Low",
                    "Neutron High",
                    "Extended Occupancy",
                    "Tamper",
                    "Total SOH Events"
            );
            default -> List.of(
                    "Date (UTC)",
                    "Total Occupancy",
                    "Total Alarms",
                    "Gamma",
                    "Gamma-Neutron",
                    "Neutron",
                    "EML-Suppressed",
                    "Total SOH Events",
                    "Gamma High",
                    "Gamma Low",
                    "Neutron High",
                    "Extended Occupancy",
                    "Tamper"
            );
        };
    }

    private List<String> buildCsvRow(String date, ReportDataAggregator.ObservationMetrics m) {
        return switch (eventType) {
            case ALARMS_OCCUPANCIES -> List.of(
                    date,
                    String.valueOf(m.gammaAlarm),
                    String.valueOf(m.gammaNeutronAlarm),
                    String.valueOf(m.neutronAlarm),
                    String.valueOf(m.emlSuppressed),
                    String.valueOf(m.totalAlarms()),
                    String.valueOf(m.totalOccupancies),
                    String.format(Locale.US, "%.2f", m.alarmOccupancyRatePct()),
                    String.format(Locale.US, "%.2f", m.emlAlarmRatePct())
            );
            case ALARMS -> List.of(
                    date,
                    String.valueOf(m.gammaAlarm),
                    String.valueOf(m.gammaNeutronAlarm),
                    String.valueOf(m.neutronAlarm),
                    String.valueOf(m.emlSuppressed),
                    String.valueOf(m.totalAlarms()),
                    String.format(Locale.US, "%.2f", m.emlAlarmRatePct())
            );
            case SOH -> List.of(
                    date,
                    String.valueOf(m.gammaHighFault),
                    String.valueOf(m.gammaLowFault),
                    String.valueOf(m.neutronHighFault),
                    String.valueOf(m.extendedOccupancyFault),
                    String.valueOf(m.tamper),
                    String.valueOf(m.totalFaultsAndTamper())
            );
            default -> List.of(
                    date,
                    String.valueOf(m.totalOccupancies),
                    String.valueOf(m.totalAlarms()),
                    String.valueOf(m.gammaAlarm),
                    String.valueOf(m.gammaNeutronAlarm),
                    String.valueOf(m.neutronAlarm),
                    String.valueOf(m.emlSuppressed),
                    String.valueOf(m.totalFaultsAndTamper()),
                    String.valueOf(m.gammaHighFault),
                    String.valueOf(m.gammaLowFault),
                    String.valueOf(m.neutronHighFault),
                    String.valueOf(m.extendedOccupancyFault),
                    String.valueOf(m.tamper)
            );
        };
    }

    private void writeJson(
            String nodeId,
            String scopeLine,
            Map<String, ReportDataAggregator.ObservationMetrics> dailyMetrics,
            ReportDataAggregator.ObservationMetrics totals,
            ReportDataAggregator.UpSetCoOccurrenceData upSetData
    ) throws IOException {
        try (JsonWriter jw = new JsonWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))) {
            jw.setIndent("  ");
            jw.setHtmlSafe(false);
            jw.beginObject();
            jw.name("reportType").value(ReportCmdType.EVENT.name());
            jw.name("reportTitle").value("Event Report");
            jw.name("eventType").value(eventType.name());
            jw.name("nodeId").value(nodeId);
            jw.name("scope").value(scopeLine);
            jw.name("startDateTime").value(start != null ? DateTimeFormatter.ISO_INSTANT.format(start) : "");
            jw.name("endDateTime").value(end != null ? DateTimeFormatter.ISO_INSTANT.format(end) : "");
            jw.name("generatedAt").value(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

            jw.name("summary").beginObject();
            jw.name("eventType").value(eventType.name());
            jw.name("dayCount").value(dailyMetrics.size());
            jw.name("totalOccupancies").value(totals.totalOccupancies);
            jw.name("totalAlarms").value(totals.totalAlarms());
            jw.name("gammaAlarm").value(totals.gammaAlarm);
            jw.name("gammaNeutronAlarm").value(totals.gammaNeutronAlarm);
            jw.name("neutronAlarm").value(totals.neutronAlarm);
            jw.name("emlSuppressed").value(totals.emlSuppressed);
            jw.name("alarmOccupancyRatePct").value(XlsxWorkbookWriter.roundTwoDecimals(totals.alarmOccupancyRatePct()));
            jw.name("emlAlarmRatePct").value(XlsxWorkbookWriter.roundTwoDecimals(totals.emlAlarmRatePct()));
            jw.name("totalSohEvents").value(totals.totalFaultsAndTamper());
            jw.name("gammaHighFault").value(totals.gammaHighFault);
            jw.name("gammaLowFault").value(totals.gammaLowFault);
            jw.name("neutronHighFault").value(totals.neutronHighFault);
            jw.name("extendedOccupancyFault").value(totals.extendedOccupancyFault);
            jw.name("tamper").value(totals.tamper);
            jw.endObject();

            if (eventType == EventReportType.ALARMS || eventType == EventReportType.ALARMS_OCCUPANCIES) {
                ReportDataAggregator.writeUpSetJson(jw, upSetData);
            }

            jw.name("records").beginArray();
            for (Map.Entry<String, ReportDataAggregator.ObservationMetrics> entry : dailyMetrics.entrySet()) {
                String date = entry.getKey();
                ReportDataAggregator.ObservationMetrics m = entry.getValue();
                jw.beginObject();
                jw.name("dateUtc").value(date);
                if (eventType == EventReportType.SOH) {
                    jw.name("gammaHighFault").value(m.gammaHighFault);
                    jw.name("gammaLowFault").value(m.gammaLowFault);
                    jw.name("neutronHighFault").value(m.neutronHighFault);
                    jw.name("extendedOccupancyFault").value(m.extendedOccupancyFault);
                    jw.name("tamper").value(m.tamper);
                    jw.name("totalSohEvents").value(m.totalFaultsAndTamper());
                } else if (eventType == EventReportType.ALARMS) {
                    jw.name("gammaAlarm").value(m.gammaAlarm);
                    jw.name("gammaNeutronAlarm").value(m.gammaNeutronAlarm);
                    jw.name("neutronAlarm").value(m.neutronAlarm);
                    jw.name("emlSuppressed").value(m.emlSuppressed);
                    jw.name("totalAlarms").value(m.totalAlarms());
                    jw.name("emlAlarmRatePct").value(XlsxWorkbookWriter.roundTwoDecimals(m.emlAlarmRatePct()));
                } else {
                    jw.name("gammaAlarm").value(m.gammaAlarm);
                    jw.name("gammaNeutronAlarm").value(m.gammaNeutronAlarm);
                    jw.name("neutronAlarm").value(m.neutronAlarm);
                    jw.name("emlSuppressed").value(m.emlSuppressed);
                    jw.name("totalAlarms").value(m.totalAlarms());
                    jw.name("totalOccupancies").value(m.totalOccupancies);
                    jw.name("alarmOccupancyRatePct").value(XlsxWorkbookWriter.roundTwoDecimals(m.alarmOccupancyRatePct()));
                    jw.name("emlAlarmRatePct").value(XlsxWorkbookWriter.roundTwoDecimals(m.emlAlarmRatePct()));
                }
                jw.endObject();
            }
            jw.endArray();
            jw.endObject();
            jw.flush();
        }
    }

    private void writeXlsx(
            String nodeId,
            String scopeLine,
            Map<String, ReportDataAggregator.ObservationMetrics> dailyMetrics,
            ReportDataAggregator.ObservationMetrics totals,
            ReportDataAggregator.UpSetCoOccurrenceData upSetData
    ) throws IOException {
        List<String> headers = eventColumnHeaders();
        List<Double> widths = new ArrayList<>(headers.size());
        for (String h : headers) {
            widths.add(Math.max(16.0, h.length() + 4.0));
        }

        List<List<CellValue>> rows = new ArrayList<>(dailyMetrics.size());
        for (Map.Entry<String, ReportDataAggregator.ObservationMetrics> entry : dailyMetrics.entrySet()) {
            String date = entry.getKey();
            ReportDataAggregator.ObservationMetrics m = entry.getValue();
            rows.add(buildXlsxRow(date, m));
        }

        List<List<CellValue>> summaryRows = List.of(
                List.of(CellValue.text("Report Title"), CellValue.text("Event Report")),
                List.of(CellValue.text("Event Type"), CellValue.text(eventType.name())),
                List.of(CellValue.text("Node ID"), CellValue.text(nodeId)),
                List.of(CellValue.text("Scope"), CellValue.text(scopeLine)),
                List.of(CellValue.text("Start (UTC)"), CellValue.text(start != null ? DateTimeFormatter.ISO_INSTANT.format(start) : "")),
                List.of(CellValue.text("End (UTC)"), CellValue.text(end != null ? DateTimeFormatter.ISO_INSTANT.format(end) : "")),
                List.of(CellValue.text("UTC Days Reported"), CellValue.integer(dailyMetrics.size())),
                List.of(CellValue.text("Total Occupancies"), CellValue.integer(totals.totalOccupancies)),
                List.of(CellValue.text("Total Alarms"), CellValue.integer(totals.totalAlarms())),
                List.of(CellValue.text("EML Suppressed"), CellValue.integer(totals.emlSuppressed)),
                List.of(CellValue.text("Total SOH Events"), CellValue.integer(totals.totalFaultsAndTamper()))
        );

        List<XlsxWorkbookWriter.SheetSpec> sheets = new ArrayList<>(3);
        sheets.add(new XlsxWorkbookWriter.SheetSpec("Daily Event Breakdown", headers, widths, rows, true));
        if (eventType == EventReportType.ALARMS || eventType == EventReportType.ALARMS_OCCUPANCIES) {
            sheets.add(ReportDataAggregator.buildUpSetXlsxSheet(upSetData));
        }
        sheets.add(new XlsxWorkbookWriter.SheetSpec("Summary", List.of("Metric", "Value"), List.of(28.0, 46.0), summaryRows, false));
        XlsxWorkbookWriter.writeWorkbook(out, sheets);
    }

    private List<CellValue> buildXlsxRow(String date, ReportDataAggregator.ObservationMetrics m) {
        return switch (eventType) {
            case ALARMS_OCCUPANCIES -> List.of(
                    CellValue.text(date),
                    CellValue.integer(m.gammaAlarm),
                    CellValue.integer(m.gammaNeutronAlarm),
                    CellValue.integer(m.neutronAlarm),
                    CellValue.integer(m.emlSuppressed),
                    CellValue.integer(m.totalAlarms()),
                    CellValue.integer(m.totalOccupancies),
                    CellValue.decimal(m.alarmOccupancyRatePct()),
                    CellValue.decimal(m.emlAlarmRatePct())
            );
            case ALARMS -> List.of(
                    CellValue.text(date),
                    CellValue.integer(m.gammaAlarm),
                    CellValue.integer(m.gammaNeutronAlarm),
                    CellValue.integer(m.neutronAlarm),
                    CellValue.integer(m.emlSuppressed),
                    CellValue.integer(m.totalAlarms()),
                    CellValue.decimal(m.emlAlarmRatePct())
            );
            case SOH -> List.of(
                    CellValue.text(date),
                    CellValue.integer(m.gammaHighFault),
                    CellValue.integer(m.gammaLowFault),
                    CellValue.integer(m.neutronHighFault),
                    CellValue.integer(m.extendedOccupancyFault),
                    CellValue.integer(m.tamper),
                    CellValue.integer(m.totalFaultsAndTamper())
            );
            default -> List.of(
                    CellValue.text(date),
                    CellValue.integer(m.totalOccupancies),
                    CellValue.integer(m.totalAlarms()),
                    CellValue.integer(m.gammaAlarm),
                    CellValue.integer(m.gammaNeutronAlarm),
                    CellValue.integer(m.neutronAlarm),
                    CellValue.integer(m.emlSuppressed),
                    CellValue.integer(m.totalFaultsAndTamper()),
                    CellValue.integer(m.gammaHighFault),
                    CellValue.integer(m.gammaLowFault),
                    CellValue.integer(m.neutronHighFault),
                    CellValue.integer(m.extendedOccupancyFault),
                    CellValue.integer(m.tamper)
            );
        };
    }

    @Override
    public String getReportType() {
        return ReportCmdType.EVENT.name();
    }

    private void renderAlarmsAndOccupancies(
            PdfDocumentBuilder pdf,
            Map<String, ReportDataAggregator.ObservationMetrics> dailyMetrics,
            ReportDataAggregator.ObservationMetrics totals,
            ReportDataAggregator.UpSetCoOccurrenceData upSetData,
            NumberFormat intFmt
    ) throws IOException {
        pdf.drawKpiCards(List.of(
                new PdfDocumentBuilder.KpiCard(
                        "Total Occupancies",
                        intFmt.format(totals.totalOccupancies),
                        "Alarm Rate: " + Utils.formatPercentage(totals.alarmOccupancyRatePct()),
                        ChartGenerator.COLOR_OCCUPANCY_LINE
                ),
                new PdfDocumentBuilder.KpiCard(
                        "Total Alarms",
                        intFmt.format(totals.totalAlarms()),
                        "Gamma: " + intFmt.format(totals.gammaAlarm),
                        ChartGenerator.COLOR_GAMMA
                ),
                new PdfDocumentBuilder.KpiCard(
                        "Neutron & G+N Alarms",
                        intFmt.format(totals.neutronAlarm + totals.gammaNeutronAlarm),
                        "N: " + intFmt.format(totals.neutronAlarm) + " | G+N: " + intFmt.format(totals.gammaNeutronAlarm),
                        ChartGenerator.COLOR_GAMMA_NEUTRON
                ),
                new PdfDocumentBuilder.KpiCard(
                        "EML Suppressed",
                        intFmt.format(totals.emlSuppressed),
                        "EML Rate: " + Utils.formatPercentage(totals.emlAlarmRatePct()),
                        ChartGenerator.COLOR_EML_SUPPRESSED
                )
        ));

        List<String> dates = new ArrayList<>(dailyMetrics.keySet());
        List<Long> gammaVals = new ArrayList<>(dates.size());
        List<Long> gnVals = new ArrayList<>(dates.size());
        List<Long> neutronVals = new ArrayList<>(dates.size());
        List<Long> emlVals = new ArrayList<>(dates.size());
        List<Long> occVals = new ArrayList<>(dates.size());
        Map<String, Map<String, String>> tableByDate = new LinkedHashMap<>();

        for (String date : dates) {
            ReportDataAggregator.ObservationMetrics m = dailyMetrics.get(date);
            gammaVals.add(m.gammaAlarm);
            gnVals.add(m.gammaNeutronAlarm);
            neutronVals.add(m.neutronAlarm);
            emlVals.add(m.emlSuppressed);
            occVals.add(m.totalOccupancies);

            Map<String, String> row = new LinkedHashMap<>();
            row.put("Gamma", String.valueOf(m.gammaAlarm));
            row.put("Gamma-Neutron", String.valueOf(m.gammaNeutronAlarm));
            row.put("Neutron", String.valueOf(m.neutronAlarm));
            row.put("EML-Suppressed", String.valueOf(m.emlSuppressed));
            row.put("Total Occupancy", String.valueOf(m.totalOccupancies));
            tableByDate.put(date, row);
        }

        pdf.drawSectionHeader(
                "Alarms and Occupancies by UTC Day",
                "Dual-axis view: stacked alarms/suppressions on left axis, total occupancies on independent right axis",
                245f
        );

        List<ChartGenerator.StackedSeries> barSeries = List.of(
                new ChartGenerator.StackedSeries("Gamma", ChartGenerator.COLOR_GAMMA, gammaVals),
                new ChartGenerator.StackedSeries("Gamma-Neutron", ChartGenerator.COLOR_GAMMA_NEUTRON, gnVals),
                new ChartGenerator.StackedSeries("Neutron", ChartGenerator.COLOR_NEUTRON, neutronVals),
                new ChartGenerator.StackedSeries("EML-Suppressed", ChartGenerator.COLOR_EML_SUPPRESSED, emlVals)
        );
        ChartGenerator.LineSeries occLine = new ChartGenerator.LineSeries(
                "Total Occupancy (Right Axis)", ChartGenerator.COLOR_OCCUPANCY_LINE, occVals
        );

        chartGenerator.drawDualAxisStackedBarLineChart(
                pdf,
                "Alarms and Occupancies",
                "Date (UTC)",
                "Alarm / Suppressed Count",
                dates,
                barSeries,
                "Total Occupancies",
                occLine
        );

        if (upSetData != null && upSetData.totalAlarmingOccupancies() > 0L) {
            pdf.drawSectionHeader(
                    "Alarm & Source Category Co-Occurrence (UpSet)",
                    "Intersection cardinality across Gamma, Neutron, NORM, Industrial, Medical, SNM, and Unknown occupancies",
                    280f
            );
            chartGenerator.drawUpSetChart(
                    pdf,
                    "Alarm & Source Category Co-Occurrence (Occupancies)",
                    upSetData,
                    18
            );
        }

        if (dailyMetrics.size() > 1) {
            pdf.drawSectionHeader("Daily Breakdown Table", "Exact daily alarm, EML suppression, and occupancy counts (UTC)", 70f);
            tableGenerator.drawTableByDate(pdf, tableByDate, false);
        }
    }

    private void renderAlarmsOnly(
            PdfDocumentBuilder pdf,
            Map<String, ReportDataAggregator.ObservationMetrics> dailyMetrics,
            ReportDataAggregator.ObservationMetrics totals,
            ReportDataAggregator.UpSetCoOccurrenceData upSetData,
            NumberFormat intFmt
    ) throws IOException {
        pdf.drawKpiCards(List.of(
                new PdfDocumentBuilder.KpiCard(
                        "Total Alarms",
                        intFmt.format(totals.totalAlarms()),
                        dailyMetrics.size() + (dailyMetrics.size() == 1 ? " UTC day" : " UTC days"),
                        ChartGenerator.COLOR_GAMMA
                ),
                new PdfDocumentBuilder.KpiCard(
                        "Gamma Alarms",
                        intFmt.format(totals.gammaAlarm),
                        "Gamma-only triggers",
                        ChartGenerator.COLOR_GAMMA
                ),
                new PdfDocumentBuilder.KpiCard(
                        "Neutron & G+N Alarms",
                        intFmt.format(totals.neutronAlarm + totals.gammaNeutronAlarm),
                        "N: " + intFmt.format(totals.neutronAlarm) + " | G+N: " + intFmt.format(totals.gammaNeutronAlarm),
                        ChartGenerator.COLOR_GAMMA_NEUTRON
                ),
                new PdfDocumentBuilder.KpiCard(
                        "EML Suppressed",
                        intFmt.format(totals.emlSuppressed),
                        "EML Rate: " + Utils.formatPercentage(totals.emlAlarmRatePct()),
                        ChartGenerator.COLOR_EML_SUPPRESSED
                )
        ));

        List<String> dates = new ArrayList<>(dailyMetrics.keySet());
        List<Long> gammaVals = new ArrayList<>(dates.size());
        List<Long> gnVals = new ArrayList<>(dates.size());
        List<Long> neutronVals = new ArrayList<>(dates.size());
        List<Long> emlVals = new ArrayList<>(dates.size());
        Map<String, Map<String, String>> tableByDate = new LinkedHashMap<>();

        for (String date : dates) {
            ReportDataAggregator.ObservationMetrics m = dailyMetrics.get(date);
            gammaVals.add(m.gammaAlarm);
            gnVals.add(m.gammaNeutronAlarm);
            neutronVals.add(m.neutronAlarm);
            emlVals.add(m.emlSuppressed);

            Map<String, String> row = new LinkedHashMap<>();
            row.put("Gamma", String.valueOf(m.gammaAlarm));
            row.put("Gamma-Neutron", String.valueOf(m.gammaNeutronAlarm));
            row.put("Neutron", String.valueOf(m.neutronAlarm));
            row.put("EML-Suppressed", String.valueOf(m.emlSuppressed));
            tableByDate.put(date, row);
        }

        pdf.drawSectionHeader("Alarm Statistics by UTC Day", "Stacked daily radiation alarms and EML suppressions", 245f);
        chartGenerator.drawStackedBarChart(
                pdf,
                "Alarms",
                "Date (UTC)",
                "Count",
                dates,
                List.of(
                        new ChartGenerator.StackedSeries("Gamma", ChartGenerator.COLOR_GAMMA, gammaVals),
                        new ChartGenerator.StackedSeries("Gamma-Neutron", ChartGenerator.COLOR_GAMMA_NEUTRON, gnVals),
                        new ChartGenerator.StackedSeries("Neutron", ChartGenerator.COLOR_NEUTRON, neutronVals),
                        new ChartGenerator.StackedSeries("EML-Suppressed", ChartGenerator.COLOR_EML_SUPPRESSED, emlVals)
                )
        );

        if (upSetData != null && upSetData.totalAlarmingOccupancies() > 0L) {
            pdf.drawSectionHeader(
                    "Alarm & Source Category Co-Occurrence (UpSet)",
                    "Intersection cardinality across Gamma, Neutron, NORM, Industrial, Medical, SNM, and Unknown occupancies",
                    280f
            );
            chartGenerator.drawUpSetChart(
                    pdf,
                    "Alarm & Source Category Co-Occurrence (Occupancies)",
                    upSetData,
                    18
            );
        }

        if (dailyMetrics.size() > 1) {
            pdf.drawSectionHeader("Daily Alarm Breakdown", "Exact daily counts across alarm categories (UTC)", 70f);
            tableGenerator.drawTableByDate(pdf, tableByDate, false);
        }
    }

    private void renderSoh(
            PdfDocumentBuilder pdf,
            Map<String, ReportDataAggregator.ObservationMetrics> dailyMetrics,
            ReportDataAggregator.ObservationMetrics totals,
            NumberFormat intFmt
    ) throws IOException {
        pdf.drawKpiCards(List.of(
                new PdfDocumentBuilder.KpiCard(
                        "Total SOH Events",
                        intFmt.format(totals.totalFaultsAndTamper()),
                        dailyMetrics.size() + (dailyMetrics.size() == 1 ? " UTC day" : " UTC days"),
                        ChartGenerator.COLOR_TAMPER
                ),
                new PdfDocumentBuilder.KpiCard(
                        "Gamma High Faults",
                        intFmt.format(totals.gammaHighFault),
                        "Fault - Gamma High",
                        ChartGenerator.COLOR_GAMMA
                ),
                new PdfDocumentBuilder.KpiCard(
                        "Gamma Low Faults",
                        intFmt.format(totals.gammaLowFault),
                        "Fault - Gamma Low",
                        ChartGenerator.COLOR_GAMMA_LOW
                ),
                new PdfDocumentBuilder.KpiCard(
                        "Neutron High, Ext. Occ & Tamper",
                        intFmt.format(totals.neutronHighFault + totals.extendedOccupancyFault + totals.tamper),
                        "N-High: " + intFmt.format(totals.neutronHighFault)
                                + " | Ext: " + intFmt.format(totals.extendedOccupancyFault)
                                + " | Tamper: " + intFmt.format(totals.tamper),
                        ChartGenerator.COLOR_NEUTRON
                )
        ));

        List<String> dates = new ArrayList<>(dailyMetrics.keySet());
        List<Long> ghVals = new ArrayList<>(dates.size());
        List<Long> glVals = new ArrayList<>(dates.size());
        List<Long> nhVals = new ArrayList<>(dates.size());
        List<Long> extVals = new ArrayList<>(dates.size());
        List<Long> tamperVals = new ArrayList<>(dates.size());
        Map<String, Map<String, String>> tableByDate = new LinkedHashMap<>();

        for (String date : dates) {
            ReportDataAggregator.ObservationMetrics m = dailyMetrics.get(date);
            ghVals.add(m.gammaHighFault);
            glVals.add(m.gammaLowFault);
            nhVals.add(m.neutronHighFault);
            extVals.add(m.extendedOccupancyFault);
            tamperVals.add(m.tamper);

            Map<String, String> row = new LinkedHashMap<>();
            row.put("Gamma High", String.valueOf(m.gammaHighFault));
            row.put("Gamma Low", String.valueOf(m.gammaLowFault));
            row.put("Neutron High", String.valueOf(m.neutronHighFault));
            row.put("Extended Occupancy", String.valueOf(m.extendedOccupancyFault));
            row.put("Tamper", String.valueOf(m.tamper));
            tableByDate.put(date, row);
        }

        pdf.drawSectionHeader("State of Health (SOH) by UTC Day", "Daily detector fault, extended occupancy fault, and tamper occurrences", 245f);
        chartGenerator.drawStackedBarChart(
                pdf,
                "SOH",
                "Date (UTC)",
                "Count",
                dates,
                List.of(
                        new ChartGenerator.StackedSeries("Gamma High", ChartGenerator.COLOR_GAMMA, ghVals),
                        new ChartGenerator.StackedSeries("Gamma Low", ChartGenerator.COLOR_GAMMA_LOW, glVals),
                        new ChartGenerator.StackedSeries("Neutron High", ChartGenerator.COLOR_NEUTRON, nhVals),
                        new ChartGenerator.StackedSeries("Extended Occupancy", ChartGenerator.COLOR_EXTENDED_OCCUPANCY, extVals),
                        new ChartGenerator.StackedSeries("Tamper", ChartGenerator.COLOR_TAMPER, tamperVals)
                )
        );

        if (dailyMetrics.size() > 1) {
            pdf.drawSectionHeader("Daily SOH Breakdown", "Exact daily fault and tamper counts (UTC)", 70f);
            tableGenerator.drawTableByDate(pdf, tableByDate, false);
        }
    }
}
