/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.reports.types;

import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ChartGenerator;
import com.botts.impl.service.oscar.reports.helpers.ChartGenerator.LineSeries;
import com.botts.impl.service.oscar.reports.helpers.ChartGenerator.StackedSeries;
import com.botts.impl.service.oscar.reports.helpers.PdfDocumentBuilder;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.ReportDataAggregator;
import com.botts.impl.service.oscar.reports.helpers.ReportDataAggregator.AdjudicationDetailRecord;
import com.botts.impl.service.oscar.reports.helpers.ReportFormat;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator.Align;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator.ColumnSpec;
import com.botts.impl.service.oscar.reports.helpers.XlsxWorkbookWriter;
import com.botts.impl.service.oscar.reports.helpers.XlsxWorkbookWriter.CellValue;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator.OcrMatchSummary;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator.VehicleIncidentData;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator.VehicleIncidentSpec;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator.VehiclePassRecord;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator.WebIdSummary;
import com.google.gson.stream.JsonWriter;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Consolidated Vehicle Incident Report supporting native vector PDF (Landscape),
 * RFC-4180 UTF-8 CSV, structured JSON, and multi-sheet OpenXML (.xlsx) workbooks.
 *
 * <p>Designed for confirmation re-scan SOPs (same vehicle passing a gross-counting RPM
 * multiple times) and secondary spectroscopic SOPs (gross-counting alarm followed by
 * RS350 spectroscopic / WebID secondary inspection across single-node or federated deployments).</p>
 */
public class VehicleIncidentReport extends Report {

    private static final DateTimeFormatter SHORT_UTC_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT).withZone(ZoneOffset.UTC);

    private final String rawSpecString;
    private final ReportFormat format;
    private final TableGenerator tableGenerator;
    private final ChartGenerator chartGenerator;

    public VehicleIncidentReport(
            OutputStream out,
            Instant startTime,
            Instant endTime,
            String rawSpecString,
            ReportFormat format,
            OSCARServiceModule module
    ) {
        super(out, startTime, endTime, module);
        this.rawSpecString = rawSpecString;
        this.format = format != null ? format : ReportFormat.PDF;
        this.tableGenerator = new TableGenerator();
        this.chartGenerator = new ChartGenerator(module);
    }

    @Override
    public void generate() throws DataStoreException {
        VehicleIncidentSpec spec;
        try {
            spec = VehicleIncidentAggregator.parseRequestSpec(rawSpecString);
        } catch (RuntimeException e) {
            throw new DataStoreException("Invalid Vehicle Incident report specification", e);
        }
        if (format != ReportFormat.JSON && spec.includeProfiles()) {
            spec = new VehicleIncidentSpec(
                    spec.rawVehicleId(),
                    spec.normalizedVehicleId(),
                    spec.matchMode(),
                    spec.laneUIDs(),
                    false,
                    spec.federatedPasses()
            );
        }

        VehicleIncidentData data = VehicleIncidentAggregator.queryVehicleIncident(
                module,
                spec,
                start,
                end,
                VehicleIncidentAggregator.MAX_SEARCH_LIMIT
        );

        try {
            switch (format) {
                case CSV -> writeCsv(data);
                case JSON -> writeJson(data);
                case XLSX -> writeXlsx(data);
                case PDF -> writePdf(data);
            }
        } catch (IOException e) {
            throw new DataStoreException("Failed to generate Vehicle Incident report (" + format + ")", e);
        }
    }

    @Override
    public String getReportType() {
        return ReportCmdType.VEHICLE_INCIDENT.name();
    }

    private void writePdf(VehicleIncidentData data) throws IOException {
        NumberFormat intFmt = NumberFormat.getIntegerInstance(Locale.US);
        List<VehiclePassRecord> passes = data.passes();

        String hostNodeLabel = (data.nodeName() != null && !data.nodeName().isBlank())
                ? data.nodeName().trim()
                : data.nodeId();
        try (PdfDocumentBuilder pdf = new PdfDocumentBuilder(
                "Vehicle Incident Report",
                hostNodeLabel,
                PdfDocumentBuilder.Orientation.LANDSCAPE
        )) {
            String siteHeader = data.distinctNodeCount() > 1
                    ? (hostNodeLabel + " (+" + (data.distinctNodeCount() - 1) + " federated)")
                    : hostNodeLabel;
            String vehicleLine = "Vehicle ID: " + data.formattedVehicleId()
                    + " (" + data.matchMode().displayLabel() + ")";

            pdf.drawReportBanner(new PdfDocumentBuilder.HeaderMetadata(
                    "Vehicle Incident Report",
                    siteHeader,
                    vehicleLine,
                    data.scopeSummary(),
                    start,
                    end
            ));

            long webIdAnalysisTotal = passes.stream().mapToLong(p -> p.webIdAnalyses().size()).sum();
            pdf.drawKpiCards(List.of(
                    new PdfDocumentBuilder.KpiCard(
                            "Total Passes",
                            intFmt.format(passes.size()),
                            data.distinctLaneCount() + (data.distinctLaneCount() == 1 ? " lane / " : " lanes / ")
                                    + data.distinctNodeCount() + (data.distinctNodeCount() == 1 ? " node" : " nodes"),
                            PdfDocumentBuilder.ACCENT_BLUE
                    ),
                    new PdfDocumentBuilder.KpiCard(
                            "Alarming Passes",
                            intFmt.format(data.alarmingPassCount()),
                            data.openAlarmPassCount() + " open, " + data.emlAutoReleasePassCount() + " EML auto",
                            PdfDocumentBuilder.ACCENT_RED
                    ),
                    new PdfDocumentBuilder.KpiCard(
                            "Spectroscopic / WebID",
                            intFmt.format(data.spectroscopicPassCount()),
                            webIdAnalysisTotal + (webIdAnalysisTotal == 1 ? " spectrum analysis" : " spectrum analyses"),
                            PdfDocumentBuilder.ACCENT_SKY
                    ),
                    new PdfDocumentBuilder.KpiCard(
                            "Confirmed Isotopes",
                            intFmt.format(data.confirmedIsotopes().size()),
                            data.confirmedIsotopes().isEmpty()
                                    ? "No isotopes recorded"
                                    : String.join(", ", data.confirmedIsotopes()),
                            PdfDocumentBuilder.ACCENT_PURPLE
                    ),
                    new PdfDocumentBuilder.KpiCard(
                            "Latest Disposition",
                            truncateShort(data.latestDispositionLabel(), 22),
                            "Group: " + data.latestDispositionGroup(),
                            ChartGenerator.colorForDispositionGroup(data.latestDispositionGroup())
                    )
            ));

            // Section 1: Chronological Cross-Lane Pass Comparison Table
            pdf.drawSectionHeader(
                    "Cross-Lane Vehicle Pass Comparison",
                    "Chronological primary gross-counting, confirmation re-scan, and secondary spectroscopic transits",
                    85f
            );

            List<ColumnSpec> passColumns = List.of(
                    new ColumnSpec("Pass #", 0.55f, Align.CENTER, true),
                    new ColumnSpec("Node / Lane", 1.25f, Align.LEFT),
                    new ColumnSpec("System Type", 0.95f, Align.LEFT),
                    new ColumnSpec("Transit Window (UTC) & +Elapsed", 1.45f, Align.LEFT),
                    new ColumnSpec("Vehicle ID & Source", 1.15f, Align.LEFT),
                    new ColumnSpec("Alarm Status", 1.10f, Align.LEFT),
                    new ColumnSpec("Peak Gamma / Neutron", 0.90f, Align.RIGHT),
                    new ColumnSpec("EML / Spectroscopic / WebID", 1.55f, Align.LEFT),
                    new ColumnSpec("Latest Disposition", 1.30f, Align.LEFT)
            );

            Instant firstPassInstant = passes.isEmpty() ? null : passes.get(0).sortInstant();
            List<List<String>> passRows = new ArrayList<>(passes.size());
            for (int i = 0; i < passes.size(); i++) {
                VehiclePassRecord pass = passes.get(i);
                String nodeLane = formatNodeAndLane(pass, data.distinctNodeCount() > 1);
                String windowCell = formatTransitWindowAndElapsed(pass, i == 0 ? null : firstPassInstant);
                String vehicleCell = formatVehicleAndSource(pass);
                String peakCounts = intFmt.format(pass.maxGammaCount()) + " / " + intFmt.format(pass.maxNeutronCount());
                String findingsCell = formatFindingsSummary(pass);
                String dispCell = formatLatestDisposition(pass);

                passRows.add(List.of(
                        "#" + (i + 1),
                        nodeLane,
                        pass.systemType(),
                        windowCell,
                        vehicleCell,
                        pass.alarmStatusLabel(),
                        peakCounts,
                        findingsCell,
                        dispCell
                ));
            }
            tableGenerator.drawTable(pdf, passColumns, passRows, false, 7.2f);

            // Section 2: Per-Pass Peak Radiation Comparison Vector Chart
            if (!passes.isEmpty()) {
                pdf.drawSectionHeader(
                        "Per-Pass Peak Radiation Comparison",
                        "Peak gamma gross counts (bars by alarm outcome) and peak neutron counts (right axis) across passes",
                        285f
                );

                List<String> categories = new ArrayList<>(passes.size());
                List<Long> alarmingGamma = new ArrayList<>(passes.size());
                List<Long> emlReleasedGamma = new ArrayList<>(passes.size());
                List<Long> clearGamma = new ArrayList<>(passes.size());
                List<Long> peakNeutron = new ArrayList<>(passes.size());

                for (int i = 0; i < passes.size(); i++) {
                    VehiclePassRecord p = passes.get(i);
                    String shortLane = humanizeLaneDisplay(p.laneName(), p.laneUid());
                    if (shortLane.length() > 16) {
                        shortLane = shortLane.substring(0, 15) + ".";
                    }
                    categories.add("Pass #" + (i + 1) + " (" + shortLane + ")");
                    long g = Math.max(0L, p.maxGammaCount());
                    if (p.emlAutoReleased()) {
                        alarmingGamma.add(0L);
                        emlReleasedGamma.add(g);
                        clearGamma.add(0L);
                    } else if (p.isAlarming()) {
                        alarmingGamma.add(g);
                        emlReleasedGamma.add(0L);
                        clearGamma.add(0L);
                    } else {
                        alarmingGamma.add(0L);
                        emlReleasedGamma.add(0L);
                        clearGamma.add(g);
                    }
                    peakNeutron.add((long) Math.max(0, p.maxNeutronCount()));
                }

                chartGenerator.drawDualAxisStackedBarLineChart(
                        pdf,
                        "Peak Gamma & Neutron Counts Across Vehicle Passes",
                        "Chronological Vehicle Pass",
                        "Peak Gamma Counts",
                        categories,
                        List.of(
                                new StackedSeries("Alarming Peak Gamma", ChartGenerator.COLOR_GAMMA, alarmingGamma),
                                new StackedSeries("EML Auto-Release Peak Gamma", ChartGenerator.COLOR_EML_SUPPRESSED, emlReleasedGamma),
                                new StackedSeries("Non-Alarming Peak Gamma", PdfDocumentBuilder.SLATE_400, clearGamma)
                        ),
                        "Peak Neutron Counts",
                        new LineSeries("Peak Neutron Counts", ChartGenerator.COLOR_NEUTRON, peakNeutron)
                );

                ReportDataAggregator.UpSetCoOccurrenceData vehicleUpSet =
                        ReportDataAggregator.buildUpSetFromVehiclePasses(passes);
                if (vehicleUpSet.totalAlarmingOccupancies() >= 5L) {
                    pdf.drawSectionHeader(
                            "Multi-Pass Alarm & Category Co-Occurrence",
                            "UpSet intersection breakdown across alarming transits for this vehicle",
                            280f
                    );
                    chartGenerator.drawUpSetChart(
                            pdf,
                            "Alarming Vehicle Transit Co-Occurrence",
                            vehicleUpSet,
                            18
                    );
                }
            }

            // Section 3: Complete Adjudication, OCR & Spectroscopic Chain
            pdf.drawSectionHeader(
                    "Adjudication, Camera OCR & Spectroscopic Chain",
                    "Complete audit trail of automated camera reads, WebID spectrum analyses, Enhanced Machine Learning evaluations, and operator dispositions",
                    85f
            );

            List<ColumnSpec> chainColumns = List.of(
                    new ColumnSpec("Pass #", 0.55f, Align.CENTER, true),
                    new ColumnSpec("Node / Lane", 1.20f, Align.LEFT),
                    new ColumnSpec("Timestamp (UTC)", 1.15f, Align.LEFT),
                    new ColumnSpec("Record Type", 0.95f, Align.LEFT),
                    new ColumnSpec("Actor / Source", 0.95f, Align.LEFT),
                    new ColumnSpec("Vehicle ID / Disposition / Result", 1.65f, Align.LEFT),
                    new ColumnSpec("Isotopes / Confidence / Notes", 2.45f, Align.LEFT)
            );

            List<List<String>> chainRows = buildAuditChainRows(passes, data.distinctNodeCount() > 1);
            tableGenerator.drawTable(pdf, chainColumns, chainRows, false, 7.1f);

            pdf.finishAndSave(out);
        }
    }

    private void writeCsv(VehicleIncidentData data) throws IOException {
        List<String> headers = passColumnHeaders();
        Instant firstInstant = data.passes().isEmpty() ? null : data.passes().get(0).sortInstant();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writeCsvRow(writer, headers);
            for (int i = 0; i < data.passes().size(); i++) {
                VehiclePassRecord p = data.passes().get(i);
                long elapsedSec = (i == 0 || firstInstant == null || Instant.EPOCH.equals(p.sortInstant()))
                        ? 0L
                        : Math.max(0L, Duration.between(firstInstant, p.sortInstant()).getSeconds());
                OcrMatchSummary ocr = p.ocr();
                AdjudicationDetailRecord adj = p.latestAdjudication();

                List<String> row = List.of(
                        String.valueOf(i + 1),
                        XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(p.nodeId()),
                        XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(p.laneName()),
                        p.laneUid(),
                        p.systemType(),
                        p.occupancyObsId(),
                        String.valueOf(p.occupancyCount()),
                        p.startTimeIso(),
                        p.endTimeIso(),
                        String.format(Locale.US, "%.3f", p.durationSeconds()),
                        String.valueOf(elapsedSec),
                        XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(p.vehicleId()),
                        p.normalizedVehicleId(),
                        p.vehicleIdSource(),
                        p.alarmStatusLabel(),
                        String.valueOf(p.gammaAlarm()),
                        String.valueOf(p.neutronAlarm()),
                        p.alarmCategoryCode(),
                        String.valueOf(p.maxGammaCount()),
                        String.valueOf(p.maxNeutronCount()),
                        String.format(Locale.US, "%.2f", p.neutronBackground()),
                        ocr != null ? XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(ocr.value()) : "",
                        ocr != null ? ocr.idType() : "",
                        ocr != null ? String.format(Locale.US, "%.2f", ocr.confidence() * 100.0) : "",
                        ocr != null ? String.valueOf(ocr.checksumValid()) : "",
                        p.emlDecision(),
                        p.emlSourceType(),
                        formatNullableDecimal(p.emlNormProbPct()),
                        formatNullableDecimal(p.emlThreatProbPct()),
                        p.confirmedIsotopes().isEmpty() ? "" : String.join("; ", p.confirmedIsotopes()),
                        adj != null ? String.valueOf(adj.adjudicationCode()) : "",
                        adj != null ? adj.dispositionLabel() : (p.isOpenAlarm() ? "UNADJUDICATED" : ""),
                        adj != null ? XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(adj.username()) : "",
                        adj != null ? adj.secondaryInspectionStatus() : "",
                        adj != null ? XlsxWorkbookWriter.guardSpreadsheetFormulaPrefix(adj.formattedSummary().replace('\n', ' ')) : ""
                );
                writeCsvRow(writer, row);
            }
            writer.flush();
        }
    }

    private void writeJson(VehicleIncidentData data) throws IOException {
        try (JsonWriter jw = new JsonWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))) {
            jw.setIndent("  ");
            jw.setHtmlSafe(false);
            VehicleIncidentAggregator.writeJsonPayload(jw, data, true);
            jw.flush();
        }
    }

    private void writeXlsx(VehicleIncidentData data) throws IOException {
        List<String> passHeaders = passColumnHeaders();
        List<Double> passWidths = List.of(
                9.0, 16.0, 18.0, 28.0, 20.0, 20.0, 12.0, 22.0, 22.0, 12.0, 14.0,
                18.0, 16.0, 14.0, 24.0, 12.0, 12.0, 16.0, 14.0, 14.0, 14.0,
                18.0, 12.0, 14.0, 14.0, 14.0, 16.0, 14.0, 14.0, 24.0,
                14.0, 28.0, 16.0, 18.0, 42.0
        );

        Instant firstInstant = data.passes().isEmpty() ? null : data.passes().get(0).sortInstant();
        List<List<CellValue>> passSheetRows = new ArrayList<>(data.passes().size());
        for (int i = 0; i < data.passes().size(); i++) {
            VehiclePassRecord p = data.passes().get(i);
            long elapsedSec = (i == 0 || firstInstant == null || Instant.EPOCH.equals(p.sortInstant()))
                    ? 0L
                    : Math.max(0L, Duration.between(firstInstant, p.sortInstant()).getSeconds());
            OcrMatchSummary ocr = p.ocr();
            AdjudicationDetailRecord adj = p.latestAdjudication();

            passSheetRows.add(List.of(
                    CellValue.integer(i + 1),
                    CellValue.text(p.nodeId()),
                    CellValue.text(p.laneName()),
                    CellValue.text(p.laneUid()),
                    CellValue.text(p.systemType()),
                    CellValue.text(p.occupancyObsId()),
                    CellValue.integer(p.occupancyCount()),
                    CellValue.text(p.startTimeIso()),
                    CellValue.text(p.endTimeIso()),
                    CellValue.decimal(p.durationSeconds()),
                    CellValue.integer(elapsedSec),
                    CellValue.text(p.vehicleId()),
                    CellValue.text(p.normalizedVehicleId()),
                    CellValue.text(p.vehicleIdSource()),
                    CellValue.text(p.alarmStatusLabel()),
                    CellValue.text(String.valueOf(p.gammaAlarm())),
                    CellValue.text(String.valueOf(p.neutronAlarm())),
                    CellValue.text(p.alarmCategoryCode().isEmpty() ? "-" : p.alarmCategoryCode()),
                    CellValue.integer(p.maxGammaCount()),
                    CellValue.integer(p.maxNeutronCount()),
                    CellValue.decimal(p.neutronBackground()),
                    CellValue.text(ocr != null ? ocr.value() : "-"),
                    CellValue.text(ocr != null ? ocr.idType() : "-"),
                    CellValue.decimal(ocr != null ? (double) ocr.confidence() * 100.0 : null),
                    CellValue.text(ocr != null ? String.valueOf(ocr.checksumValid()) : "-"),
                    CellValue.text(p.emlDecision().isEmpty() ? "-" : p.emlDecision()),
                    CellValue.text(p.emlSourceType().isEmpty() ? "-" : p.emlSourceType()),
                    CellValue.decimal(p.emlNormProbPct()),
                    CellValue.decimal(p.emlThreatProbPct()),
                    CellValue.text(p.confirmedIsotopes().isEmpty() ? "None" : String.join(", ", p.confirmedIsotopes())),
                    adj != null ? CellValue.integer(adj.adjudicationCode()) : CellValue.text("-"),
                    CellValue.text(adj != null ? adj.dispositionLabel() : (p.isOpenAlarm() ? "UNADJUDICATED" : "-")),
                    CellValue.text(adj != null ? adj.username() : "-"),
                    CellValue.text(adj != null ? adj.secondaryInspectionStatus() : "-"),
                    CellValue.text(adj != null ? adj.formattedSummary() : "-")
            ));
        }

        List<List<String>> chainTextRows = buildAuditChainRows(data.passes(), true);
        List<List<CellValue>> chainSheetRows = new ArrayList<>(chainTextRows.size());
        for (List<String> row : chainTextRows) {
            List<CellValue> cells = new ArrayList<>(row.size());
            for (String cell : row) {
                cells.add(CellValue.text(cell));
            }
            chainSheetRows.add(cells);
        }

        List<List<CellValue>> summaryRows = List.of(
                List.of(CellValue.text("Report Title"), CellValue.text("Vehicle Incident Report")),
                List.of(CellValue.text("Host Node ID"), CellValue.text(data.nodeId())),
                List.of(CellValue.text("Vehicle ID"), CellValue.text(data.formattedVehicleId())),
                List.of(CellValue.text("Normalized Vehicle ID"), CellValue.text(data.normalizedVehicleId())),
                List.of(CellValue.text("Match Mode"), CellValue.text(data.matchMode().displayLabel())),
                List.of(CellValue.text("Scope"), CellValue.text(data.scopeSummary())),
                List.of(CellValue.text("Start (UTC)"), CellValue.text(start != null ? DateTimeFormatter.ISO_INSTANT.format(start) : "")),
                List.of(CellValue.text("End (UTC)"), CellValue.text(end != null ? DateTimeFormatter.ISO_INSTANT.format(end) : "")),
                List.of(CellValue.text("Total Vehicle Passes"), CellValue.integer(data.passes().size())),
                List.of(CellValue.text("Alarming Passes"), CellValue.integer(data.alarmingPassCount())),
                List.of(CellValue.text("Open / Unadjudicated Alarms"), CellValue.integer(data.openAlarmPassCount())),
                List.of(CellValue.text("EML Auto-Released Passes"), CellValue.integer(data.emlAutoReleasePassCount())),
                List.of(CellValue.text("Spectroscopic / WebID Passes"), CellValue.integer(data.spectroscopicPassCount())),
                List.of(CellValue.text("Distinct Lanes"), CellValue.integer(data.distinctLaneCount())),
                List.of(CellValue.text("Distinct Nodes"), CellValue.integer(data.distinctNodeCount())),
                List.of(CellValue.text("Confirmed Isotopes"), CellValue.text(
                        data.confirmedIsotopes().isEmpty() ? "None" : String.join(", ", data.confirmedIsotopes()))),
                List.of(CellValue.text("Latest Disposition"), CellValue.text(data.latestDispositionLabel()))
        );

        XlsxWorkbookWriter.writeWorkbook(out, List.of(
                new XlsxWorkbookWriter.SheetSpec("Vehicle Passes", passHeaders, passWidths, passSheetRows, true),
                new XlsxWorkbookWriter.SheetSpec(
                        "Adjudication & OCR Chain",
                        List.of("Pass #", "Node / Lane", "Timestamp (UTC)", "Record Type", "Actor / Source", "Vehicle ID / Disposition / Result", "Isotopes / Confidence / Notes"),
                        List.of(10.0, 24.0, 22.0, 18.0, 18.0, 30.0, 48.0),
                        chainSheetRows,
                        true
                ),
                new XlsxWorkbookWriter.SheetSpec("Incident Summary", List.of("Metric", "Value"), List.of(30.0, 48.0), summaryRows, false)
        ));
    }

    private static List<String> passColumnHeaders() {
        return List.of(
                "Pass #",
                "Node ID",
                "Lane Name",
                "Lane UID",
                "System Type",
                "Occupancy Obs ID",
                "Occupancy #",
                "Start Time (UTC)",
                "End Time (UTC)",
                "Duration (s)",
                "Elapsed Since Pass #1 (s)",
                "Vehicle ID",
                "Normalized Vehicle ID",
                "Vehicle ID Source",
                "Alarm Status",
                "Gamma Alarm",
                "Neutron Alarm",
                "Alarm Category Code",
                "Peak Gamma Counts",
                "Peak Neutron Counts",
                "Neutron Background",
                "OCR Read",
                "OCR Type",
                "OCR Confidence (%)",
                "OCR ISO Checksum Valid",
                "EML Decision",
                "EML Source Type",
                "EML NORM (%)",
                "EML Threat (%)",
                "Confirmed Isotopes",
                "Latest Disposition Code",
                "Latest Disposition Label",
                "Adjudicated By",
                "Secondary Status",
                "Summary / Notes"
        );
    }

    private static List<List<String>> buildAuditChainRows(List<VehiclePassRecord> passes, boolean showNode) {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < passes.size(); i++) {
            VehiclePassRecord pass = passes.get(i);
            String passTag = "#" + (i + 1);
            String nodeLane = formatNodeAndLane(pass, showNode);

            if (pass.ocr() != null) {
                OcrMatchSummary ocr = pass.ocr();
                String conf = String.format(Locale.US, "%.1f%%", ocr.confidence() * 100.0);
                String isoFlag = "container".equalsIgnoreCase(ocr.idType())
                        ? (ocr.checksumValid() ? "ISO 6346 valid" : "ISO checksum unverified")
                        : "Plate / Fleet ID";
                String formattedOcrValue = VehicleIncidentAggregator.formatVehicleIdDisplay(
                        ocr.value(), ocr.normalizedValue());
                rows.add(List.of(
                        passTag,
                        nodeLane,
                        formatShortIso(ocr.frameTimeIso()),
                        "Camera OCR",
                        formatCameraSource(ocr.cameraUid()),
                        formattedOcrValue + " (" + ocr.idType() + ")",
                        "Confidence: " + conf + " | " + isoFlag + " | Reads: " + ocr.readCount()
                ));
            }

            for (WebIdSummary w : pass.webIdAnalyses()) {
                String isoSummary = w.isotopes().isEmpty()
                        ? (w.isotopeString().isEmpty() ? "No isotopes identified" : w.isotopeString())
                        : String.join(", ", w.isotopes());
                String details = String.format(
                        Locale.US,
                        "DRF: %s | Dose: %.3f | Chi2: %.2f%s",
                        w.drf().isEmpty() ? "-" : w.drf(),
                        w.estimatedDose(),
                        w.chi2(),
                        w.errorMessage().isEmpty() ? "" : (" | Error: " + w.errorMessage())
                );
                rows.add(List.of(
                        passTag,
                        nodeLane,
                        formatShortIso(w.sampleTimeIso()),
                        "WebID Spectrum",
                        "Full-Spectrum RIID",
                        isoSummary,
                        details
                ));
            }

            for (AdjudicationDetailRecord adj : pass.adjudications()) {
                boolean isEml = adj.adjudicationCode() == org.sensorhub.impl.utils.rad.model.Adjudication.EML_NON_THREAT_CODE
                        || !adj.emlDecision().isEmpty();
                String recType = isEml && "EML".equalsIgnoreCase(adj.username())
                        ? "Automated EML"
                        : "Adjudication";
                String dispText = adj.dispositionLabel()
                        + (!"-".equals(adj.vehicleId()) && !adj.vehicleId().isBlank() ? (" [" + adj.vehicleId() + "]") : "");
                String notes = adj.isotopes().isEmpty()
                        ? adj.formattedSummary()
                        : ("Isotopes: " + String.join(", ", adj.isotopes()) + " | " + adj.formattedSummary());
                rows.add(List.of(
                        passTag,
                        nodeLane,
                        formatShortIso(adj.timeUtcIso().isEmpty() ? adj.timeUtcDisplay() : adj.timeUtcIso()),
                        recType,
                        adj.username(),
                        dispText,
                        notes
                ));
            }

            if (pass.ocr() == null && pass.webIdAnalyses().isEmpty() && pass.adjudications().isEmpty()) {
                rows.add(List.of(
                        passTag,
                        nodeLane,
                        formatShortIso(pass.startTimeIso()),
                        "Open Occupancy",
                        "-",
                        pass.isOpenAlarm() ? "UNADJUDICATED ALARM" : pass.alarmStatusLabel(),
                        "Awaiting operator review"
                ));
            }
        }
        return rows;
    }

    private static String formatNodeAndLane(VehiclePassRecord pass, boolean showNode) {
        String lane = humanizeLaneDisplay(pass.laneName(), pass.laneUid());
        String nodeLabel = (pass.nodeName() != null && !pass.nodeName().isBlank())
                ? pass.nodeName().trim()
                : (pass.nodeId() != null ? pass.nodeId().trim() : "");
        if (showNode && !nodeLabel.isEmpty()) {
            String strippedLane = lane.startsWith(nodeLabel + " - ")
                    ? lane.substring((nodeLabel + " - ").length()).trim()
                    : lane;
            return nodeLabel + " / " + strippedLane + (pass.occupancyCount() > 0 ? ("\nOcc #" + pass.occupancyCount()) : "");
        }
        return lane + (pass.occupancyCount() > 0 ? ("\nOcc #" + pass.occupancyCount()) : "");
    }

    private static String humanizeLaneDisplay(String laneName, String laneUid) {
        String candidate = (laneName != null && !laneName.isBlank())
                ? laneName.trim()
                : (laneUid != null ? laneUid.trim() : "");
        if (candidate.isEmpty()) {
            return "Unknown Lane";
        }
        if (!candidate.startsWith("urn:")) {
            return candidate;
        }
        int lastColon = candidate.lastIndexOf(':');
        if (lastColon >= 0 && lastColon + 1 < candidate.length()) {
            return candidate.substring(lastColon + 1);
        }
        return candidate;
    }

    private static String formatCameraSource(String cameraUid) {
        if (cameraUid == null || cameraUid.isBlank()) {
            return "PP-OCRv3";
        }
        String trimmed = cameraUid.trim();
        if (!trimmed.startsWith("urn:")) {
            return trimmed;
        }
        String[] parts = trimmed.split(":");
        if (parts.length >= 2) {
            String tail = parts[parts.length - 1];
            String prev = parts[parts.length - 2];
            if (tail.matches("^[0-9]+$") && !prev.isBlank() && !"lane".equalsIgnoreCase(prev) && !"sensor".equalsIgnoreCase(prev)) {
                return "Camera " + tail + " (" + prev + ")";
            }
            if (!tail.isBlank()) {
                return "Camera " + tail;
            }
        }
        return trimmed;
    }

    private static String formatTransitWindowAndElapsed(VehiclePassRecord pass, Instant firstPassInstant) {
        String startShort = formatShortIso(pass.startTimeIso());
        String dur = String.format(Locale.US, "%.1fs", pass.durationSeconds());
        if (firstPassInstant == null || Instant.EPOCH.equals(pass.sortInstant())) {
            return startShort + "\n(" + dur + " transit, Pass #1 baseline)";
        }
        long deltaSec = Math.max(0L, Duration.between(firstPassInstant, pass.sortInstant()).getSeconds());
        return startShort + "\n(" + dur + " transit, +" + formatElapsedCompact(deltaSec) + ")";
    }

    private static String formatElapsedCompact(long seconds) {
        if (seconds < 60L) {
            return seconds + "s";
        }
        if (seconds < 3_600L) {
            return (seconds / 60L) + "m " + (seconds % 60L) + "s";
        }
        long hours = seconds / 3_600L;
        long mins = (seconds % 3_600L) / 60L;
        if (hours < 48L) {
            return hours + "h " + mins + "m";
        }
        return (hours / 24L) + "d " + (hours % 24L) + "h";
    }

    private static String formatVehicleAndSource(VehiclePassRecord pass) {
        String id = pass.vehicleId().isEmpty() ? "-" : pass.vehicleId();
        String src = switch (pass.vehicleIdSource()) {
            case "both" -> "Operator + OCR";
            case "adjudication" -> "Operator Adjudication";
            case "ocr" -> pass.ocr() != null
                    ? String.format(Locale.US, "Camera OCR (%.0f%%%s)",
                        pass.ocr().confidence() * 100.0,
                        pass.ocr().checksumValid() ? ", ISO valid" : "")
                    : "Camera OCR";
            default -> "Unspecified";
        };
        return id + "\n" + src;
    }

    private static String formatFindingsSummary(VehiclePassRecord pass) {
        List<String> parts = new ArrayList<>();
        if (!pass.alarmCategoryCode().isEmpty() && !"None".equalsIgnoreCase(pass.alarmCategoryCode())) {
            parts.add("Category: " + pass.alarmCategoryCode());
        }
        if (!pass.emlDecision().isEmpty() || !pass.emlSourceType().isEmpty()) {
            StringBuilder eml = new StringBuilder("EML: ");
            if (!pass.emlDecision().isEmpty()) {
                eml.append(pass.emlDecision());
            }
            if (!pass.emlSourceType().isEmpty()) {
                eml.append(" (").append(pass.emlSourceType()).append(')');
            }
            if (pass.emlThreatProbPct() != null) {
                eml.append(String.format(Locale.US, " Threat %.1f%%", pass.emlThreatProbPct()));
            }
            parts.add(eml.toString());
        }
        if (!pass.confirmedIsotopes().isEmpty()) {
            parts.add("Isotopes: " + String.join(", ", pass.confirmedIsotopes()));
        }
        if (parts.isEmpty()) {
            return pass.isAlarming() ? "Gross-count alarm (no RIID/EML)" : "Background / Clear";
        }
        return String.join("\n", parts);
    }

    private static String formatLatestDisposition(VehiclePassRecord pass) {
        AdjudicationDetailRecord adj = pass.latestAdjudication();
        if (adj != null) {
            String secondLine = !"NONE".equalsIgnoreCase(adj.secondaryInspectionStatus())
                    && !"-".equals(adj.secondaryInspectionStatus())
                    ? ("Secondary: " + adj.secondaryInspectionStatus() + " (" + adj.username() + ")")
                    : ("By: " + adj.username());
            return adj.dispositionLabel() + "\n" + secondLine;
        }
        if (pass.emlAutoReleased()) {
            return "9999 (EML Non-Threat)\nAutomated Release";
        }
        return pass.isAlarming() ? "UNADJUDICATED (OPEN)" : "No Alarm";
    }

    private static String formatShortIso(String iso) {
        if (iso == null || iso.isBlank()) {
            return "-";
        }
        try {
            return SHORT_UTC_FMT.format(Instant.parse(iso.trim()));
        } catch (Exception e) {
            return iso.trim();
        }
    }

    private static String truncateShort(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return "-";
        }
        String t = text.trim();
        return t.length() <= maxChars ? t : (t.substring(0, maxChars - 1) + "...");
    }

    private static String formatNullableDecimal(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "";
        }
        return String.format(Locale.US, "%.2f", value);
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
}
