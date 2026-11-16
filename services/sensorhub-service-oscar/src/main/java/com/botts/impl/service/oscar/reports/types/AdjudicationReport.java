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
import com.botts.impl.service.oscar.reports.helpers.PdfDocumentBuilder;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.ReportDataAggregator;
import com.botts.impl.service.oscar.reports.helpers.ReportFormat;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.XlsxWorkbookWriter;
import com.botts.impl.service.oscar.reports.helpers.XlsxWorkbookWriter.CellValue;
import com.botts.impl.system.lane.LaneSystem;
import com.google.gson.stream.JsonWriter;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.module.IModule;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdjudicationReport extends Report {

    private final String laneUIDs;
    private final ReportFormat format;
    private final TableGenerator tableGenerator;
    private final ChartGenerator chartGenerator;

    public AdjudicationReport(OutputStream out, Instant startTime, Instant endTime, String laneUIDs, OSCARServiceModule module) {
        this(out, startTime, endTime, laneUIDs, ReportFormat.PDF, module);
    }

    public AdjudicationReport(
            OutputStream out,
            Instant startTime,
            Instant endTime,
            String laneUIDs,
            ReportFormat format,
            OSCARServiceModule module
    ) {
        super(out, startTime, endTime, module);
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

        List<String> targetLanes = ReportDataAggregator.resolveTargetLaneUIDs(module, laneUIDs);
        Map<String, String> inlineLaneLabels = ReportDataAggregator.resolveLaneDisplayLabels(module, targetLanes, false);
        Map<String, String> laneFriendlyNames = resolveLaneNamesOnly(module, targetLanes);

        // Single-pass aggregation per lane
        Map<String, ReportDataAggregator.AdjudicationLaneSummary> summariesByLane = new LinkedHashMap<>();
        long grandTotalRecords = 0L;
        long realAlarmCount = 0L;
        long innocentOrAutoCount = 0L;
        long falseAlarmCount = 0L;
        long secondaryCompletedCount = 0L;
        long spectroscopicSecondaryCount = 0L;
        Set<String> activeIsotopesAcrossScope = new LinkedHashSet<>();

        for (String laneUid : targetLanes) {
            ReportDataAggregator.AdjudicationLaneSummary summary =
                    ReportDataAggregator.aggregateAdjudicationsForLane(module, laneUid, start, end);
            summariesByLane.put(laneUid, summary);
            grandTotalRecords += summary.totalRecords();

            for (ReportDataAggregator.AdjudicationDetailRecord rec : summary.structuredRecords()) {
                if ("COMPLETED".equalsIgnoreCase(rec.secondaryInspectionStatus())) {
                    secondaryCompletedCount++;
                }
                if (ReportDataAggregator.isSpectroscopicSecondaryRecord(rec)) {
                    spectroscopicSecondaryCount++;
                }
            }

            for (ReportDataAggregator.DispositionCodeSpec spec : summary.dispositionSpecs()) {
                long c = summary.countsByCode().getOrDefault(spec.code(), 0L);
                if (c <= 0L) {
                    continue;
                }
                switch (spec.group()) {
                    case "Real Alarm" -> realAlarmCount += c;
                    case "Innocent Alarm", "Automated" -> innocentOrAutoCount += c;
                    case "False Alarm" -> falseAlarmCount += c;
                    default -> {
                        // Tracked in full Disposition breakdown table
                    }
                }
            }
            for (Map.Entry<String, Long> isoEntry : summary.isotopeCountsBySymbol().entrySet()) {
                if (isoEntry.getValue() != null && isoEntry.getValue() > 0L) {
                    activeIsotopesAcrossScope.add(isoEntry.getKey());
                }
            }
        }

        String scopeLine = ReportDataAggregator.buildBannerScopeSummary(module, laneUIDs);
        ReportDataAggregator.AdjudicationLaneSummary allLanesSummary = buildAllLanesSummary(targetLanes, summariesByLane);
        ReportDataAggregator.UpSetCoOccurrenceData upSetData = (format == ReportFormat.CSV)
                ? ReportDataAggregator.UpSetCoOccurrenceData.empty()
                : ReportDataAggregator.aggregateUpSetCoOccurrence(module, targetLanes, start, end, summariesByLane);

        try {
            switch (format) {
                case CSV -> writeCsv(targetLanes, allLanesSummary, summariesByLane, laneFriendlyNames);
                case JSON -> writeJson(
                        nodeId,
                        scopeLine,
                        targetLanes,
                        grandTotalRecords,
                        realAlarmCount,
                        innocentOrAutoCount,
                        falseAlarmCount,
                        secondaryCompletedCount,
                        spectroscopicSecondaryCount,
                        activeIsotopesAcrossScope,
                        allLanesSummary,
                        summariesByLane,
                        laneFriendlyNames,
                        upSetData
                );
                case XLSX -> writeXlsx(
                        nodeId,
                        scopeLine,
                        targetLanes,
                        grandTotalRecords,
                        realAlarmCount,
                        innocentOrAutoCount,
                        falseAlarmCount,
                        secondaryCompletedCount,
                        spectroscopicSecondaryCount,
                        activeIsotopesAcrossScope,
                        allLanesSummary,
                        summariesByLane,
                        laneFriendlyNames,
                        upSetData
                );
                case PDF -> writePdf(
                        nodeId,
                        scopeLine,
                        targetLanes,
                        grandTotalRecords,
                        realAlarmCount,
                        innocentOrAutoCount,
                        falseAlarmCount,
                        spectroscopicSecondaryCount,
                        activeIsotopesAcrossScope,
                        allLanesSummary,
                        summariesByLane,
                        inlineLaneLabels,
                        upSetData
                );
            }
        } catch (IOException e) {
            throw new DataStoreException("Failed to generate Adjudication Report (" + format + ")", e);
        }
    }

    private void writePdf(
            String nodeId,
            String scopeLine,
            List<String> targetLanes,
            long grandTotalRecords,
            long realAlarmCount,
            long innocentOrAutoCount,
            long falseAlarmCount,
            long spectroscopicSecondaryCount,
            Set<String> activeIsotopesAcrossScope,
            ReportDataAggregator.AdjudicationLaneSummary allLanesSummary,
            Map<String, ReportDataAggregator.AdjudicationLaneSummary> summariesByLane,
            Map<String, String> inlineLaneLabels,
            ReportDataAggregator.UpSetCoOccurrenceData upSetData
    ) throws IOException {
        NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
        String laneSubtitle = targetLanes.size() + (targetLanes.size() == 1 ? " lane" : " lanes")
                + (spectroscopicSecondaryCount > 0L ? " · " + intFmt.format(spectroscopicSecondaryCount) + " spec. sec." : "");

        try (PdfDocumentBuilder pdf = new PdfDocumentBuilder("Adjudication Report", nodeId)) {
            pdf.drawReportBanner(new PdfDocumentBuilder.HeaderMetadata(
                    "Adjudication Report",
                    nodeId,
                    null,
                    scopeLine,
                    start,
                    end
            ));

            pdf.drawKpiCards(List.of(
                    new PdfDocumentBuilder.KpiCard(
                            "Total Adjudications",
                            intFmt.format(grandTotalRecords),
                            laneSubtitle,
                            PdfDocumentBuilder.ACCENT_BLUE
                    ),
                    new PdfDocumentBuilder.KpiCard(
                            "Real Alarms",
                            intFmt.format(realAlarmCount),
                            "Codes 1 - 2",
                            ChartGenerator.GROUP_REAL_ALARM
                    ),
                    new PdfDocumentBuilder.KpiCard(
                            "Innocent & EML Auto",
                            intFmt.format(innocentOrAutoCount),
                            "Codes 3 - 5 & EML Non-Threat",
                            ChartGenerator.GROUP_AUTOMATED
                    ),
                    new PdfDocumentBuilder.KpiCard(
                            "False Alarms",
                            intFmt.format(falseAlarmCount),
                            "Codes 6 - 8",
                            ChartGenerator.GROUP_FALSE_ALARM
                    ),
                    new PdfDocumentBuilder.KpiCard(
                            "Isotopes Identified",
                            intFmt.format(activeIsotopesAcrossScope.size()),
                            activeIsotopesAcrossScope.isEmpty() ? "No isotopes recorded" : String.join(", ", activeIsotopesAcrossScope),
                            PdfDocumentBuilder.ACCENT_PURPLE
                    )
            ));

            if (targetLanes.isEmpty()) {
                pdf.drawCalloutBox("No lanes selected or configured for Adjudication Report.");
            } else {
                String allLanesLabel = "All Selected Lanes (" + targetLanes.size()
                        + (targetLanes.size() == 1 ? " Lane)" : " Lanes)");
                if (upSetData != null && upSetData.totalAlarmingOccupancies() > 0L) {
                    pdf.drawSectionHeader(
                            "Alarm & Source Category Co-Occurrence - " + allLanesLabel,
                            "UpSet intersection breakdown across Gamma, Neutron, NORM, Industrial, Medical, SNM, and Unknown occupancies",
                            280f
                    );
                    chartGenerator.drawUpSetChart(
                            pdf,
                            "Alarm & Source Category Co-Occurrence (Occupancies)",
                            upSetData,
                            18
                    );
                }
                renderLaneDispositionSection(pdf, allLanesLabel, allLanesSummary, intFmt);
                if (allLanesSummary != null && allLanesSummary.totalRecords() > 0L) {
                    renderLaneIsotopeSection(pdf, allLanesLabel, allLanesSummary, intFmt);
                }
            }

            for (String laneUid : targetLanes) {
                String friendlyLane = inlineLaneLabels.getOrDefault(laneUid, laneUid);
                ReportDataAggregator.AdjudicationLaneSummary summary = summariesByLane.get(laneUid);
                renderLaneDispositionSection(pdf, friendlyLane, summary, intFmt);
                if (summary != null && summary.totalRecords() > 0L) {
                    renderLaneIsotopeSection(pdf, friendlyLane, summary, intFmt);
                }
            }

            pdf.finishAndSave(out);
        }
    }

    private void writeCsv(
            List<String> targetLanes,
            ReportDataAggregator.AdjudicationLaneSummary allLanesSummary,
            Map<String, ReportDataAggregator.AdjudicationLaneSummary> summariesByLane,
            Map<String, String> laneFriendlyNames
    ) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            XlsxWorkbookWriter.writeCsvRow(writer, List.of(
                    "Lane Name",
                    "Lane UID",
                    "Section",
                    "Code / Isotope",
                    "Label",
                    "Group",
                    "Count",
                    "Percent (%)"
            ));
            if (!targetLanes.isEmpty() && allLanesSummary != null) {
                writeSummaryCsvRows(
                        writer,
                        "All Selected Lanes (" + targetLanes.size() + ")",
                        "ALL_LANES",
                        allLanesSummary
                );
            }
            for (String laneUid : targetLanes) {
                String laneName = laneFriendlyNames.getOrDefault(laneUid, laneUid);
                ReportDataAggregator.AdjudicationLaneSummary summary = summariesByLane.get(laneUid);
                if (summary == null) {
                    continue;
                }
                writeSummaryCsvRows(writer, laneName, laneUid, summary);
            }
            writer.flush();
        }
    }

    private static void writeSummaryCsvRows(
            BufferedWriter writer,
            String laneName,
            String laneUid,
            ReportDataAggregator.AdjudicationLaneSummary summary
    ) throws IOException {
        long total = summary.totalRecords();
        for (ReportDataAggregator.DispositionCodeSpec spec : summary.dispositionSpecs()) {
            long count = summary.countsByCode().getOrDefault(spec.code(), 0L);
            double pct = total > 0L ? (count * 100.0) / total : 0.0;
            String label = (spec.label() == null || spec.label().isBlank()) ? "(None)" : spec.label();
            XlsxWorkbookWriter.writeCsvRow(writer, List.of(
                    XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(laneName),
                    laneUid,
                    "Disposition",
                    String.valueOf(spec.code()),
                    XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(label),
                    spec.group(),
                    String.valueOf(count),
                    String.format(Locale.US, "%.1f", pct)
            ));
        }
        for (Map.Entry<String, Long> isoEntry : summary.isotopeCountsBySymbol().entrySet()) {
            long count = isoEntry.getValue() != null ? isoEntry.getValue() : 0L;
            if (count <= 0L) {
                continue;
            }
            String symbol = isoEntry.getKey();
            String fullName = ReportDataAggregator.STANDARD_ISOTOPES.getOrDefault(symbol, symbol);
            XlsxWorkbookWriter.writeCsvRow(writer, List.of(
                    XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(laneName),
                    laneUid,
                    "Isotope",
                    symbol,
                    fullName,
                    "Identified Isotope",
                    String.valueOf(count),
                    ""
            ));
        }
    }

    private void writeJson(
            String nodeId,
            String scopeLine,
            List<String> targetLanes,
            long grandTotalRecords,
            long realAlarmCount,
            long innocentOrAutoCount,
            long falseAlarmCount,
            long secondaryCompletedCount,
            long spectroscopicSecondaryCount,
            Set<String> activeIsotopesAcrossScope,
            ReportDataAggregator.AdjudicationLaneSummary allLanesSummary,
            Map<String, ReportDataAggregator.AdjudicationLaneSummary> summariesByLane,
            Map<String, String> laneFriendlyNames,
            ReportDataAggregator.UpSetCoOccurrenceData upSetData
    ) throws IOException {
        try (JsonWriter jw = new JsonWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))) {
            jw.setIndent("  ");
            jw.setHtmlSafe(false);
            jw.beginObject();
            jw.name("reportType").value(ReportCmdType.ADJUDICATION.name());
            jw.name("reportTitle").value("Adjudication Report");
            jw.name("nodeId").value(nodeId);
            jw.name("scope").value(scopeLine);
            jw.name("startDateTime").value(start != null ? DateTimeFormatter.ISO_INSTANT.format(start) : "");
            jw.name("endDateTime").value(end != null ? DateTimeFormatter.ISO_INSTANT.format(end) : "");
            jw.name("generatedAt").value(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

            jw.name("summary").beginObject();
            jw.name("totalRecords").value(grandTotalRecords);
            jw.name("laneCount").value(targetLanes.size());
            jw.name("realAlarms").value(realAlarmCount);
            jw.name("innocentAndAutomated").value(innocentOrAutoCount);
            jw.name("falseAlarms").value(falseAlarmCount);
            jw.name("secondaryInspectionsCompleted").value(secondaryCompletedCount);
            jw.name("spectroscopicSecondaries").value(spectroscopicSecondaryCount);
            jw.name("identifiedIsotopes").beginArray();
            for (String iso : activeIsotopesAcrossScope) {
                jw.value(iso);
            }
            jw.endArray();
            jw.endObject();

            ReportDataAggregator.writeUpSetJson(jw, upSetData);

            jw.name("records").beginArray();
            if (!targetLanes.isEmpty() && allLanesSummary != null) {
                writeSummaryJsonRecords(
                        jw,
                        "All Selected Lanes (" + targetLanes.size() + ")",
                        "ALL_LANES",
                        allLanesSummary
                );
            }
            for (String laneUid : targetLanes) {
                String laneName = laneFriendlyNames.getOrDefault(laneUid, laneUid);
                ReportDataAggregator.AdjudicationLaneSummary summary = summariesByLane.get(laneUid);
                if (summary == null) {
                    continue;
                }
                writeSummaryJsonRecords(jw, laneName, laneUid, summary);
            }
            jw.endArray();
            jw.endObject();
            jw.flush();
        }
    }

    private static void writeSummaryJsonRecords(
            JsonWriter jw,
            String laneName,
            String laneUid,
            ReportDataAggregator.AdjudicationLaneSummary summary
    ) throws IOException {
        long total = summary.totalRecords();
        for (ReportDataAggregator.DispositionCodeSpec spec : summary.dispositionSpecs()) {
            long count = summary.countsByCode().getOrDefault(spec.code(), 0L);
            double pct = total > 0L ? (count * 100.0) / total : 0.0;
            String label = (spec.label() == null || spec.label().isBlank()) ? "(None)" : spec.label();
            jw.beginObject();
            jw.name("laneName").value(laneName);
            jw.name("laneUid").value(laneUid);
            jw.name("section").value("Disposition");
            jw.name("codeOrSymbol").value(String.valueOf(spec.code()));
            jw.name("label").value(label);
            jw.name("group").value(spec.group());
            jw.name("count").value(count);
            jw.name("percentPct").value(XlsxWorkbookWriter.roundTwoDecimals(pct));
            jw.endObject();
        }
        for (Map.Entry<String, Long> isoEntry : summary.isotopeCountsBySymbol().entrySet()) {
            long count = isoEntry.getValue() != null ? isoEntry.getValue() : 0L;
            if (count <= 0L) {
                continue;
            }
            String symbol = isoEntry.getKey();
            String fullName = ReportDataAggregator.STANDARD_ISOTOPES.getOrDefault(symbol, symbol);
            jw.beginObject();
            jw.name("laneName").value(laneName);
            jw.name("laneUid").value(laneUid);
            jw.name("section").value("Isotope");
            jw.name("codeOrSymbol").value(symbol);
            jw.name("label").value(fullName);
            jw.name("group").value("Identified Isotope");
            jw.name("count").value(count);
            jw.name("percentPct").nullValue();
            jw.endObject();
        }
    }

    private void writeXlsx(
            String nodeId,
            String scopeLine,
            List<String> targetLanes,
            long grandTotalRecords,
            long realAlarmCount,
            long innocentOrAutoCount,
            long falseAlarmCount,
            long secondaryCompletedCount,
            long spectroscopicSecondaryCount,
            Set<String> activeIsotopesAcrossScope,
            ReportDataAggregator.AdjudicationLaneSummary allLanesSummary,
            Map<String, ReportDataAggregator.AdjudicationLaneSummary> summariesByLane,
            Map<String, String> laneFriendlyNames,
            ReportDataAggregator.UpSetCoOccurrenceData upSetData
    ) throws IOException {
        List<List<CellValue>> dispRows = new ArrayList<>();
        List<List<CellValue>> isoRows = new ArrayList<>();

        if (!targetLanes.isEmpty() && allLanesSummary != null) {
            appendSummaryXlsxRows(
                    dispRows,
                    isoRows,
                    "All Selected Lanes (" + targetLanes.size() + ")",
                    "ALL_LANES",
                    allLanesSummary
            );
        }
        for (String laneUid : targetLanes) {
            String laneName = laneFriendlyNames.getOrDefault(laneUid, laneUid);
            ReportDataAggregator.AdjudicationLaneSummary summary = summariesByLane.get(laneUid);
            if (summary == null) {
                continue;
            }
            appendSummaryXlsxRows(dispRows, isoRows, laneName, laneUid, summary);
        }

        List<List<CellValue>> summaryRows = List.of(
                List.of(CellValue.text("Report Title"), CellValue.text("Adjudication Report")),
                List.of(CellValue.text("Node ID"), CellValue.text(nodeId)),
                List.of(CellValue.text("Scope"), CellValue.text(scopeLine)),
                List.of(CellValue.text("Start (UTC)"), CellValue.text(start != null ? DateTimeFormatter.ISO_INSTANT.format(start) : "")),
                List.of(CellValue.text("End (UTC)"), CellValue.text(end != null ? DateTimeFormatter.ISO_INSTANT.format(end) : "")),
                List.of(CellValue.text("Configured / Selected Lanes"), CellValue.integer(targetLanes.size())),
                List.of(CellValue.text("Total Adjudications"), CellValue.integer(grandTotalRecords)),
                List.of(CellValue.text("Real Alarms (Codes 1-2)"), CellValue.integer(realAlarmCount)),
                List.of(CellValue.text("Innocent & EML Automated"), CellValue.integer(innocentOrAutoCount)),
                List.of(CellValue.text("False Alarms (Codes 6-8)"), CellValue.integer(falseAlarmCount)),
                List.of(CellValue.text("Secondary Inspections Completed"), CellValue.integer(secondaryCompletedCount)),
                List.of(CellValue.text("Spectroscopic Secondaries"), CellValue.integer(spectroscopicSecondaryCount)),
                List.of(CellValue.text("Identified Isotopes"), CellValue.text(activeIsotopesAcrossScope.isEmpty() ? "None" : String.join(", ", activeIsotopesAcrossScope)))
        );

        XlsxWorkbookWriter.writeWorkbook(out, List.of(
                new XlsxWorkbookWriter.SheetSpec(
                        "Disposition Summary",
                        List.of("Lane Name", "Lane UID", "Disposition Code", "Disposition Label", "Disposition Group", "Count", "Percent (%)", "Lane Total Records"),
                        List.of(20.0, 28.0, 16.0, 32.0, 18.0, 14.0, 14.0, 18.0),
                        dispRows,
                        true
                ),
                new XlsxWorkbookWriter.SheetSpec(
                        "Isotope Summary",
                        List.of("Lane Name", "Lane UID", "Isotope Symbol", "Isotope Name", "Count"),
                        List.of(20.0, 28.0, 18.0, 24.0, 14.0),
                        isoRows,
                        true
                ),
                ReportDataAggregator.buildUpSetXlsxSheet(upSetData),
                new XlsxWorkbookWriter.SheetSpec(
                        "Summary",
                        List.of("Metric", "Value"),
                        List.of(28.0, 46.0),
                        summaryRows,
                        false
                )
        ));
    }

    private static void appendSummaryXlsxRows(
            List<List<CellValue>> dispRows,
            List<List<CellValue>> isoRows,
            String laneName,
            String laneUid,
            ReportDataAggregator.AdjudicationLaneSummary summary
    ) {
        long total = summary.totalRecords();
        for (ReportDataAggregator.DispositionCodeSpec spec : summary.dispositionSpecs()) {
            long count = summary.countsByCode().getOrDefault(spec.code(), 0L);
            double pct = total > 0L ? (count * 100.0) / total : 0.0;
            String label = (spec.label() == null || spec.label().isBlank()) ? "(None)" : spec.label();
            dispRows.add(List.of(
                    CellValue.text(laneName),
                    CellValue.text(laneUid),
                    CellValue.integer(spec.code()),
                    CellValue.text(label),
                    CellValue.text(spec.group()),
                    CellValue.integer(count),
                    CellValue.decimal(pct),
                    CellValue.integer(total)
            ));
        }
        for (Map.Entry<String, Long> isoEntry : summary.isotopeCountsBySymbol().entrySet()) {
            long count = isoEntry.getValue() != null ? isoEntry.getValue() : 0L;
            if (count <= 0L) {
                continue;
            }
            String symbol = isoEntry.getKey();
            String fullName = ReportDataAggregator.STANDARD_ISOTOPES.getOrDefault(symbol, symbol);
            isoRows.add(List.of(
                    CellValue.text(laneName),
                    CellValue.text(laneUid),
                    CellValue.text(symbol),
                    CellValue.text(fullName),
                    CellValue.integer(count)
            ));
        }
    }

    private static ReportDataAggregator.AdjudicationLaneSummary buildAllLanesSummary(
            List<String> targetLanes,
            Map<String, ReportDataAggregator.AdjudicationLaneSummary> summariesByLane
    ) {
        long totalRecords = 0L;
        Map<Integer, ReportDataAggregator.DispositionCodeSpec> specByCode = new LinkedHashMap<>();
        Map<Integer, Long> countsByCode = new LinkedHashMap<>();
        Map<String, Long> isotopeCounts = new LinkedHashMap<>();
        for (String symbol : ReportDataAggregator.STANDARD_ISOTOPES.keySet()) {
            isotopeCounts.put(symbol, 0L);
        }

        if (targetLanes != null) {
            for (String laneUid : targetLanes) {
                ReportDataAggregator.AdjudicationLaneSummary s = summariesByLane.get(laneUid);
                if (s == null) {
                    continue;
                }
                totalRecords += s.totalRecords();
                if (s.dispositionSpecs() != null) {
                    for (ReportDataAggregator.DispositionCodeSpec spec : s.dispositionSpecs()) {
                        specByCode.putIfAbsent(spec.code(), spec);
                    }
                }
                if (s.countsByCode() != null) {
                    for (Map.Entry<Integer, Long> entry : s.countsByCode().entrySet()) {
                        countsByCode.merge(entry.getKey(), entry.getValue() != null ? entry.getValue() : 0L, Long::sum);
                    }
                }
                if (s.isotopeCountsBySymbol() != null) {
                    for (Map.Entry<String, Long> entry : s.isotopeCountsBySymbol().entrySet()) {
                        isotopeCounts.merge(entry.getKey(), entry.getValue() != null ? entry.getValue() : 0L, Long::sum);
                    }
                }
            }
        }

        return new ReportDataAggregator.AdjudicationLaneSummary(
                "ALL_LANES",
                totalRecords,
                new ArrayList<>(specByCode.values()),
                countsByCode,
                isotopeCounts,
                List.of(),
                List.of()
        );
    }

    private static Map<String, String> resolveLaneNamesOnly(OSCARServiceModule module, List<String> laneUIDs) {
        Map<String, String> names = new HashMap<>();
        if (module != null && module.getParentHub() != null && module.getParentHub().getModuleRegistry() != null) {
            for (IModule<?> mod : module.getParentHub().getModuleRegistry().getLoadedModules()) {
                if (mod instanceof LaneSystem lane && lane.getUniqueIdentifier() != null && lane.getName() != null && !lane.getName().isBlank()) {
                    names.put(lane.getUniqueIdentifier().trim(), lane.getName().trim());
                }
            }
        }
        Map<String, String> out = new LinkedHashMap<>();
        if (laneUIDs != null) {
            for (String uid : laneUIDs) {
                if (uid != null && !uid.isBlank()) {
                    String trimmed = uid.trim();
                    out.put(trimmed, names.getOrDefault(trimmed, trimmed));
                }
            }
        }
        return out;
    }

    @Override
    public String getReportType() {
        return ReportCmdType.ADJUDICATION.name();
    }

    private void renderLaneDispositionSection(
            PdfDocumentBuilder pdf,
            String friendlyLane,
            ReportDataAggregator.AdjudicationLaneSummary summary,
            NumberFormat intFmt
    ) throws IOException {
        pdf.drawSectionHeader(
                "Disposition - " + friendlyLane,
                "Percentage and count breakdown by built-in, custom, and automated disposition codes",
                80f
        );

        if (summary == null || summary.totalRecords() == 0L) {
            pdf.drawCalloutBox("No data available for the selected time period and selected lane: " + friendlyLane);
            return;
        }

        List<ChartGenerator.HorizontalBarItem> barItems = new ArrayList<>();
        Map<String, Color> presentGroups = new LinkedHashMap<>();

        for (ReportDataAggregator.DispositionCodeSpec spec : summary.dispositionSpecs()) {
            long count = summary.countsByCode().getOrDefault(spec.code(), 0L);
            double pct = (count * 100.0) / summary.totalRecords();
            Color groupColor = ChartGenerator.colorForDispositionGroup(spec.group());

            if (spec.label() != null && !spec.label().isBlank()) {
                presentGroups.putIfAbsent(spec.group(), groupColor);
                String callout = count > 0L
                        ? String.format(Locale.US, "%s (%.1f%%)", intFmt.format(count), pct)
                        : "0 (0.0%)";
                barItems.add(new ChartGenerator.HorizontalBarItem(
                        spec.label(),
                        pct,
                        callout,
                        groupColor,
                        count > 0L
                ));
            }
        }

        List<ChartGenerator.LegendItem> groupLegend = new ArrayList<>();
        for (Map.Entry<String, Color> g : presentGroups.entrySet()) {
            groupLegend.add(new ChartGenerator.LegendItem(g.getKey(), g.getValue()));
        }

        chartGenerator.drawHorizontalBarChart(
                pdf,
                "Disposition",
                "% of Total Number of Records",
                barItems,
                groupLegend,
                true,
                false
        );
    }

    private void renderLaneIsotopeSection(
            PdfDocumentBuilder pdf,
            String friendlyLane,
            ReportDataAggregator.AdjudicationLaneSummary summary,
            NumberFormat intFmt
    ) throws IOException {
        pdf.drawSectionHeader(
                "Isotope Results - " + friendlyLane,
                "Non-zero isotope identification counts recorded in completed adjudications",
                80f
        );

        Map<String, Long> isoCounts = summary != null ? summary.isotopeCountsBySymbol() : Map.of();
        List<ChartGenerator.HorizontalBarItem> barItems = new ArrayList<>();

        for (Map.Entry<String, Long> entry : isoCounts.entrySet()) {
            long count = entry.getValue() != null ? entry.getValue() : 0L;
            if (count <= 0L) {
                continue;
            }
            String symbol = entry.getKey();
            String fullName = ReportDataAggregator.STANDARD_ISOTOPES.get(symbol);
            String catLabel = (fullName != null && !fullName.equalsIgnoreCase(symbol))
                    ? symbol + " (" + fullName + ")"
                    : symbol;
            barItems.add(new ChartGenerator.HorizontalBarItem(
                    catLabel,
                    count,
                    intFmt.format(count),
                    ChartGenerator.COLOR_NEUTRON,
                    true
            ));
        }

        if (barItems.isEmpty()) {
            pdf.drawCalloutBox("No isotopes were identified in the completed adjudications for " + friendlyLane + ".");
            return;
        }

        chartGenerator.drawHorizontalBarChart(
                pdf,
                "Isotope Results",
                "Count",
                barItems,
                List.of(new ChartGenerator.LegendItem("Identified Isotope Count", ChartGenerator.COLOR_NEUTRON)),
                false,
                true
        );
    }
}
