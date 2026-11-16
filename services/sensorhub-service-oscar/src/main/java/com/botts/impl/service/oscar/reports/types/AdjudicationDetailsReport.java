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
import com.google.gson.stream.JsonWriter;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.module.IModule;
import com.botts.impl.system.lane.LaneSystem;

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

/**
 * Standalone Adjudication Details report supporting native vector PDF (Landscape),
 * RFC-4180 UTF-8 CSV, structured JSON, and OpenXML (.xlsx) data extracts.
 */
public class AdjudicationDetailsReport extends Report {

    private final String laneUIDs;
    private final ReportFormat format;
    private final TableGenerator tableGenerator;

    public AdjudicationDetailsReport(
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
    }

    @Override
    public void generate() throws DataStoreException {
        String nodeId = (module != null && module.getConfiguration() != null && module.getConfiguration().nodeId != null)
                ? module.getConfiguration().nodeId
                : "oscar-node";

        List<String> targetLanes = ReportDataAggregator.resolveTargetLaneUIDs(module, laneUIDs);
        Map<String, String> multilineLaneLabels = ReportDataAggregator.resolveLaneDisplayLabels(module, targetLanes, true);
        Map<String, String> laneFriendlyNames = resolveLaneNamesOnly(module, targetLanes);
        String scopeLine = ReportDataAggregator.buildBannerScopeSummary(module, laneUIDs);

        Map<String, List<Map<String, String>>> detailsByLane = new LinkedHashMap<>();
        List<ReportDataAggregator.AdjudicationDetailRecord> allRecords = new ArrayList<>();
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
            detailsByLane.put(laneUid, summary.detailRows());
            allRecords.addAll(summary.structuredRecords());
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
                    }
                }
            }
            for (Map.Entry<String, Long> isoEntry : summary.isotopeCountsBySymbol().entrySet()) {
                if (isoEntry.getValue() != null && isoEntry.getValue() > 0L) {
                    activeIsotopesAcrossScope.add(isoEntry.getKey());
                }
            }
        }

        try {
            switch (format) {
                case CSV -> writeCsv(allRecords, laneFriendlyNames);
                case JSON -> writeJson(
                        nodeId,
                        scopeLine,
                        targetLanes.size(),
                        grandTotalRecords,
                        realAlarmCount,
                        innocentOrAutoCount,
                        falseAlarmCount,
                        secondaryCompletedCount,
                        spectroscopicSecondaryCount,
                        activeIsotopesAcrossScope,
                        allRecords,
                        laneFriendlyNames
                );
                case XLSX -> writeXlsx(
                        nodeId,
                        scopeLine,
                        targetLanes.size(),
                        grandTotalRecords,
                        realAlarmCount,
                        innocentOrAutoCount,
                        falseAlarmCount,
                        secondaryCompletedCount,
                        spectroscopicSecondaryCount,
                        activeIsotopesAcrossScope,
                        allRecords,
                        laneFriendlyNames
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
                        detailsByLane,
                        multilineLaneLabels
                );
            }
        } catch (IOException e) {
            throw new DataStoreException("Failed to generate Adjudication Details report (" + format + ")", e);
        }
    }

    @Override
    public String getReportType() {
        return ReportCmdType.ADJUDICATION_DETAILS.name();
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
            Map<String, List<Map<String, String>>> detailsByLane,
            Map<String, String> multilineLaneLabels
    ) throws IOException {
        NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
        String laneSubtitle = targetLanes.size() + (targetLanes.size() == 1 ? " lane" : " lanes")
                + (spectroscopicSecondaryCount > 0L ? " · " + intFmt.format(spectroscopicSecondaryCount) + " spec. sec." : "");
        try (PdfDocumentBuilder pdf = new PdfDocumentBuilder(
                "Adjudication Details Report",
                nodeId,
                PdfDocumentBuilder.Orientation.LANDSCAPE
        )) {
            pdf.drawReportBanner(new PdfDocumentBuilder.HeaderMetadata(
                    "Adjudication Details Report",
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

            pdf.drawSectionHeader(
                    "Adjudication Details",
                    "Complete operator and automated EML adjudication records",
                    90f
            );
            tableGenerator.drawAdjudicationDetailsTable(pdf, detailsByLane, multilineLaneLabels);
            pdf.finishAndSave(out);
        }
    }

    private void writeCsv(
            List<ReportDataAggregator.AdjudicationDetailRecord> records,
            Map<String, String> laneFriendlyNames
    ) throws IOException {
        List<String> headers = detailsColumnHeaders();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writeCsvRow(writer, headers);
            for (ReportDataAggregator.AdjudicationDetailRecord rec : records) {
                String laneName = laneFriendlyNames.getOrDefault(rec.laneUid(), rec.laneUid());
                List<String> row = List.of(
                        XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(laneName),
                        rec.laneUid(),
                        String.valueOf(rec.sequenceNumber()),
                        rec.timeUtcIso().isEmpty() ? rec.timeUtcDisplay() : rec.timeUtcIso(),
                        XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(rec.username()),
                        String.valueOf(rec.adjudicationCode()),
                        rec.dispositionLabel(),
                        rec.dispositionGroup(),
                        rec.isotopes().isEmpty() ? "" : String.join("; ", rec.isotopes()),
                        rec.secondaryInspectionStatus(),
                        XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(rec.vehicleId()),
                        rec.occupancyObsId(),
                        XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(rec.formattedSummary().replace('\n', ' ')),
                        rec.emlDecision(),
                        rec.emlSourceType(),
                        formatNullableDecimal(rec.emlNormProbPct()),
                        formatNullableDecimal(rec.emlNonEmittingProbPct()),
                        formatNullableDecimal(rec.emlThreatProbPct()),
                        rec.filePaths().isEmpty() ? "" : String.join("; ", rec.filePaths())
                );
                writeCsvRow(writer, row);
            }
            writer.flush();
        }
    }

    private static void writeCsvRow(BufferedWriter writer, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            String val = cells.get(i) != null ? cells.get(i) : "";
            boolean needsQuotes = val.indexOf(',') >= 0
                    || val.indexOf('"') >= 0
                    || val.indexOf('\n') >= 0
                    || val.indexOf('\r') >= 0;
            if (needsQuotes) {
                writer.write('"');
                writer.write(val.replace("\"", "\"\""));
                writer.write('"');
            } else {
                writer.write(val);
            }
        }
        writer.write("\r\n");
    }

    private void writeJson(
            String nodeId,
            String scopeLine,
            int laneCount,
            long totalRecords,
            long realAlarmCount,
            long innocentOrAutoCount,
            long falseAlarmCount,
            long secondaryCompletedCount,
            long spectroscopicSecondaryCount,
            Set<String> activeIsotopes,
            List<ReportDataAggregator.AdjudicationDetailRecord> records,
            Map<String, String> laneFriendlyNames
    ) throws IOException {
        try (JsonWriter jw = new JsonWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))) {
            jw.setIndent("  ");
            jw.setHtmlSafe(false);
            jw.beginObject();
            jw.name("reportType").value(ReportCmdType.ADJUDICATION_DETAILS.name());
            jw.name("reportTitle").value("Adjudication Details Report");
            jw.name("nodeId").value(nodeId);
            jw.name("scope").value(scopeLine);
            jw.name("startDateTime").value(start != null ? DateTimeFormatter.ISO_INSTANT.format(start) : "");
            jw.name("endDateTime").value(end != null ? DateTimeFormatter.ISO_INSTANT.format(end) : "");
            jw.name("generatedAt").value(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

            jw.name("summary").beginObject();
            jw.name("totalRecords").value(totalRecords);
            jw.name("laneCount").value(laneCount);
            jw.name("realAlarms").value(realAlarmCount);
            jw.name("innocentAndAutomated").value(innocentOrAutoCount);
            jw.name("falseAlarms").value(falseAlarmCount);
            jw.name("secondaryInspectionsCompleted").value(secondaryCompletedCount);
            jw.name("spectroscopicSecondaries").value(spectroscopicSecondaryCount);
            jw.name("identifiedIsotopes").beginArray();
            for (String iso : activeIsotopes) {
                jw.value(iso);
            }
            jw.endArray();
            jw.endObject();

            jw.name("records").beginArray();
            for (ReportDataAggregator.AdjudicationDetailRecord rec : records) {
                jw.beginObject();
                jw.name("laneName").value(laneFriendlyNames.getOrDefault(rec.laneUid(), rec.laneUid()));
                jw.name("laneUid").value(rec.laneUid());
                jw.name("sequenceNumber").value(rec.sequenceNumber());
                jw.name("timeUtc").value(rec.timeUtcIso().isEmpty() ? rec.timeUtcDisplay() : rec.timeUtcIso());
                jw.name("username").value(rec.username());
                jw.name("adjudicationCode").value(rec.adjudicationCode());
                jw.name("dispositionLabel").value(rec.dispositionLabel());
                jw.name("dispositionGroup").value(rec.dispositionGroup());
                jw.name("isotopes").beginArray();
                for (String iso : rec.isotopes()) {
                    jw.value(iso);
                }
                jw.endArray();
                jw.name("secondaryInspectionStatus").value(rec.secondaryInspectionStatus());
                jw.name("vehicleId").value(rec.vehicleId());
                jw.name("occupancyObsId").value(rec.occupancyObsId());
                jw.name("summary").value(rec.formattedSummary());
                if (!rec.emlDecision().isEmpty() || !rec.emlSourceType().isEmpty()
                        || rec.emlNormProbPct() != null || rec.emlNonEmittingProbPct() != null || rec.emlThreatProbPct() != null) {
                    jw.name("eml").beginObject();
                    if (!rec.emlDecision().isEmpty()) {
                        jw.name("decision").value(rec.emlDecision());
                    }
                    if (!rec.emlSourceType().isEmpty()) {
                        jw.name("sourceType").value(rec.emlSourceType());
                    }
                    if (rec.emlNormProbPct() != null) {
                        jw.name("normProbabilityPct").value(roundTwoDecimals(rec.emlNormProbPct()));
                    }
                    if (rec.emlNonEmittingProbPct() != null) {
                        jw.name("nonEmittingProbabilityPct").value(roundTwoDecimals(rec.emlNonEmittingProbPct()));
                    }
                    if (rec.emlThreatProbPct() != null) {
                        jw.name("threatProbabilityPct").value(roundTwoDecimals(rec.emlThreatProbPct()));
                    }
                    if (rec.emlVehicleClass() != null) {
                        jw.name("vehicleClass").value(rec.emlVehicleClass());
                    }
                    if (rec.emlVehicleLengthMeters() != null) {
                        jw.name("vehicleLengthMeters").value(roundTwoDecimals(rec.emlVehicleLengthMeters()));
                    }
                    jw.endObject();
                }
                jw.name("filePaths").beginArray();
                for (String fp : rec.filePaths()) {
                    jw.value(fp);
                }
                jw.endArray();
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
            int laneCount,
            long totalRecords,
            long realAlarmCount,
            long innocentOrAutoCount,
            long falseAlarmCount,
            long secondaryCompletedCount,
            long spectroscopicSecondaryCount,
            Set<String> activeIsotopes,
            List<ReportDataAggregator.AdjudicationDetailRecord> records,
            Map<String, String> laneFriendlyNames
    ) throws IOException {
        List<String> headers = detailsColumnHeaders();
        List<Double> widths = List.of(
                18.0, 28.0, 10.0, 22.0, 15.0, 14.0, 30.0, 18.0,
                22.0, 20.0, 16.0, 20.0, 42.0, 15.0, 16.0,
                16.0, 20.0, 16.0, 32.0
        );

        List<List<CellValue>> detailSheetRows = new ArrayList<>(records.size());
        for (ReportDataAggregator.AdjudicationDetailRecord rec : records) {
            String laneName = laneFriendlyNames.getOrDefault(rec.laneUid(), rec.laneUid());
            detailSheetRows.add(List.of(
                    CellValue.text(laneName),
                    CellValue.text(rec.laneUid()),
                    CellValue.integer(rec.sequenceNumber()),
                    CellValue.text(rec.timeUtcIso().isEmpty() ? rec.timeUtcDisplay() : rec.timeUtcIso()),
                    CellValue.text(rec.username()),
                    CellValue.integer(rec.adjudicationCode()),
                    CellValue.text(rec.dispositionLabel()),
                    CellValue.text(rec.dispositionGroup()),
                    CellValue.text(rec.isotopes().isEmpty() ? "None" : String.join(", ", rec.isotopes())),
                    CellValue.text(rec.secondaryInspectionStatus()),
                    CellValue.text(rec.vehicleId()),
                    CellValue.text(rec.occupancyObsId()),
                    CellValue.text(rec.formattedSummary()),
                    CellValue.text(rec.emlDecision().isEmpty() ? "-" : rec.emlDecision()),
                    CellValue.text(rec.emlSourceType().isEmpty() ? "-" : rec.emlSourceType()),
                    CellValue.decimal(rec.emlNormProbPct()),
                    CellValue.decimal(rec.emlNonEmittingProbPct()),
                    CellValue.decimal(rec.emlThreatProbPct()),
                    CellValue.text(rec.filePaths().isEmpty() ? "None" : String.join(", ", rec.filePaths()))
            ));
        }

        List<List<CellValue>> summaryRows = List.of(
                List.of(CellValue.text("Report Title"), CellValue.text("Adjudication Details Report")),
                List.of(CellValue.text("Node ID"), CellValue.text(nodeId)),
                List.of(CellValue.text("Scope"), CellValue.text(scopeLine)),
                List.of(CellValue.text("Start (UTC)"), CellValue.text(start != null ? DateTimeFormatter.ISO_INSTANT.format(start) : "")),
                List.of(CellValue.text("End (UTC)"), CellValue.text(end != null ? DateTimeFormatter.ISO_INSTANT.format(end) : "")),
                List.of(CellValue.text("Configured / Selected Lanes"), CellValue.integer(laneCount)),
                List.of(CellValue.text("Total Adjudications"), CellValue.integer(totalRecords)),
                List.of(CellValue.text("Real Alarms (Codes 1-2)"), CellValue.integer(realAlarmCount)),
                List.of(CellValue.text("Innocent & EML Automated"), CellValue.integer(innocentOrAutoCount)),
                List.of(CellValue.text("False Alarms (Codes 6-8)"), CellValue.integer(falseAlarmCount)),
                List.of(CellValue.text("Secondary Inspections Completed"), CellValue.integer(secondaryCompletedCount)),
                List.of(CellValue.text("Spectroscopic Secondaries"), CellValue.integer(spectroscopicSecondaryCount)),
                List.of(CellValue.text("Identified Isotopes"), CellValue.text(activeIsotopes.isEmpty() ? "None" : String.join(", ", activeIsotopes)))
        );

        XlsxWorkbookWriter.writeWorkbook(out, List.of(
                new XlsxWorkbookWriter.SheetSpec("Adjudication Details", headers, widths, detailSheetRows, true),
                new XlsxWorkbookWriter.SheetSpec("Summary", List.of("Metric", "Value"), List.of(30.0, 48.0), summaryRows, false)
        ));
    }

    private static List<String> detailsColumnHeaders() {
        return List.of(
                "Lane Name",
                "Lane UID",
                "Record #",
                "Time (UTC)",
                "Username",
                "Disposition Code",
                "Disposition Label",
                "Disposition Group",
                "Isotopes",
                "Secondary Status",
                "Vehicle ID",
                "Occupancy Obs ID",
                "Feedback / EML Summary",
                "EML Decision",
                "EML Source Type",
                "EML NORM (%)",
                "EML Non-Emitting (%)",
                "EML Threat (%)",
                "Attached Files"
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

    private static String formatNullableDecimal(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "";
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private static double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
