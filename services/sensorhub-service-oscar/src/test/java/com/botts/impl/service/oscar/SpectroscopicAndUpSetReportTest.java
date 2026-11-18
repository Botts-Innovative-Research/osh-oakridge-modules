package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.reports.helpers.ChartGenerator;
import com.botts.impl.service.oscar.reports.helpers.PdfDocumentBuilder;
import com.botts.impl.service.oscar.reports.helpers.ReportDataAggregator;
import com.botts.impl.service.oscar.reports.helpers.XlsxWorkbookWriter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.output.N42Output;
import org.sensorhub.impl.utils.rad.webid.WebIdAnalyzer;
import org.vast.swe.fast.JsonDataWriterGson;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

/**
 * Unit coverage for multi-vendor secondary-inspection spectroscopic parsing/enrichment
 * (.n42, .spe, RadComm Syclone .xml) and UpSet-style alarm/source-category co-occurrence
 * aggregation and vector PDF/JSON/XLSX report rendering.
 */
public class SpectroscopicAndUpSetReportTest {

    @Test
    public void testCountedZeroesExpansionAndXmlSanitization() throws Exception {
        String quirkyVendorXml = "\uFEFF<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<RadInstrumentData xmlns=\"http://physics.nist.gov/N42/2011/N42\" xmlns:InterSpec=\"https://sandia.gov/InterSpec\">\n"
                + "  <InterSpec:ExtraMetadata><InterSpec:Foo>bar</InterSpec:Foo></InterSpec:ExtraMetadata>\n"
                + "  <RadMeasurement id=\"FG-1\">\n"
                + "    <MeasurementClassCode>Foreground</MeasurementClassCode>\n"
                + "    <StartDateTime>2026-11-15T12:00:00Z</StartDateTime>\n"
                + "    <RealTimeDuration>PT2M2S</RealTimeDuration>\n"
                + "    <Spectrum id=\"Spec-1\">\n"
                + "      <LiveTimeDuration>PT120S</LiveTimeDuration>\n"
                + "      <ChannelData compressionCode=\"CountedZeroes\">0 3 42 18 0 2 7</ChannelData>\n"
                + "    </Spectrum>\n"
                + "  </RadMeasurement>\n"
                + "  <RadMeasurement id=\"FG-GC\">\n"
                + "    <MeasurementClassCode>Foreground</MeasurementClassCode>\n"
                + "    <StartDateTime>2026-11-15T12:00:00Z</StartDateTime>\n"
                + "    <RealTimeDuration>PT2M2S</RealTimeDuration>\n"
                + "    <GrossCounts id=\"NeutronGrossCounts\">\n"
                + "      <CountData>14</CountData>\n"
                + "    </GrossCounts>\n"
                + "  </RadMeasurement>\n"
                + "  <AnalysisResults id=\"AR-1\">\n"
                + "    <NuclideAnalysisResult>\n"
                + "      <Nuclide>\n"
                + "        <NuclideIdentifiedIndicator>true</NuclideIdentifiedIndicator>\n"
                + "        <NuclideName>I-131</NuclideName>\n"
                + "        <NuclideIDConfidenceDescription>High</NuclideIDConfidenceDescription>\n"
                + "        <NuclideCategoryDescription>Medical</NuclideCategoryDescription>\n"
                + "      </Nuclide>\n"
                + "    </NuclideAnalysisResult>\n"
                + "  </AnalysisResults>\n"
                + "</RadInstrumentData>";

        String sanitized = N42Output.sanitizeN42Xml(quirkyVendorXml);
        assertFalse(sanitized.startsWith("\uFEFF"));
        assertFalse(sanitized.contains("InterSpec:"));
        assertTrue(sanitized.contains("<NuclideAnalysisResults>"));

        var parsed = new RADHelper().getRadInstrumentData(sanitized);
        assertNotNull(parsed);

        SensorSystem dummyParent = new SensorSystem() {
            @Override
            public String getUniqueIdentifier() {
                return "urn:osh:test:lane1";
            }
        };
        N42Output<SensorSystem> n42Output = new N42Output<>(dummyParent);
        n42Output.onNewMessage(sanitized, "R225_I131.n42", "occ-test-123");

        JsonObject n42Json = toConSysJson(n42Output);
        assertEquals("R225_I131.n42", n42Json.get("fileName").getAsString());
        assertEquals("occ-test-123", n42Json.get("occupancyObsId").getAsString());

        JsonArray fgArr = n42Json.getAsJsonArray("foregroundReports");
        assertEquals("Sibling spectrum + gross-count Foreground blocks should consolidate into 1 report",
                1, fgArr.size());
        JsonObject fg0 = fgArr.get(0).getAsJsonObject();
        assertEquals(122.0, fg0.get("duration").getAsDouble(), 0.01);
        assertEquals(8, fg0.get("linearSpectrumCount").getAsInt());
        assertEquals(8, fg0.getAsJsonArray("linearSpectrum").size());
        assertEquals(67.0, fg0.get("gammaGrossCount").getAsDouble(), 0.01);
        assertEquals(14.0, fg0.get("neutronGrossCount").getAsDouble(), 0.01);

        JsonArray alarmArr = n42Json.getAsJsonArray("alarmReports");
        assertTrue("NuclideAnalysisResults should populate alarmReports", alarmArr.size() >= 1);
        String alarmDesc = alarmArr.get(0).getAsJsonObject().get("alarmDescription").getAsString();
        assertTrue("Expected I-131 Medical description, got: " + alarmDesc,
                alarmDesc.contains("I-131") && alarmDesc.contains("Medical"));
    }

    private static JsonObject toConSysJson(N42Output<?> output) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonDataWriterGson writer = new JsonDataWriterGson();
        writer.setDataComponents(output.getRecordDescription().copy());
        writer.setOutput(baos);
        writer.write(output.getLatestRecord());
        writer.flush();
        return JsonParser.parseString(baos.toString(StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    public void testSycloneXmlAndFlirSpeConversionAndMultiSampleTrim() throws Exception {
        WebIdAnalyzer analyzer = new WebIdAnalyzer((org.sensorhub.impl.utils.rad.webid.WebIdClient) null, null);

        StringBuilder channels = new StringBuilder();
        for (int i = 0; i < 1024; i++) {
            if (i > 0) channels.append(' ');
            channels.append(i == 121 ? 480 : (i < 200 ? 12 : 1));
        }
        String sycloneXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<RadSamplerData>\n"
                + "  <SerialNumber>SYC-901</SerialNumber>\n"
                + "  <DateTime>2026-11-15T12:15:00Z</DateTime>\n"
                + "  <AcquisitionTime>60</AcquisitionTime>\n"
                + "  <Neutrons>3</Neutrons>\n"
                + "  <EnergyCalibrationCoefficients><A>0.0</A><B>3.0</B><C>0.0</C></EnergyCalibrationCoefficients>\n"
                + "  <ChannelDataString>" + channels + "</ChannelDataString>\n"
                + "  <IdentificationData>\n"
                + "    <Isotopes>\n"
                + "      <IsotopeData><IsotopeName>Iodine 131</IsotopeName><TYPE>Medical</TYPE><SNR>24.95</SNR></IsotopeData>\n"
                + "    </Isotopes>\n"
                + "  </IdentificationData>\n"
                + "</RadSamplerData>";

        String sycloneN42 = analyzer.toEnrichedN42Xml(sycloneXml.getBytes(StandardCharsets.UTF_8), "syclone_i131.xml");
        assertNotNull(sycloneN42);
        assertTrue(sycloneN42.contains("http://physics.nist.gov/N42/2011/N42"));
        assertTrue(sycloneN42.contains("<NuclideName>Iodine 131</NuclideName>"));

        SensorSystem dummyParent = new SensorSystem() {
            @Override
            public String getUniqueIdentifier() {
                return "urn:osh:test:lane1";
            }
        };
        N42Output<SensorSystem> n42Output = new N42Output<>(dummyParent);
        n42Output.onNewMessage(sycloneN42, "syclone_i131.xml", "occ-syc-1");

        JsonObject sycRec = toConSysJson(n42Output);
        assertEquals(1, sycRec.getAsJsonArray("foregroundReports").size());
        assertEquals(1, sycRec.getAsJsonArray("alarmReports").size());

        // Test Symetrica multi-sample trimming
        String multiSample = "<RadInstrumentData xmlns=\"http://physics.nist.gov/N42/2011/N42\">"
                + "<RadMeasurementGroup id=\"G1\"/>"
                + "<RadMeasurement id=\"Cal\" radMeasurementGroupReferences=\"G1\"><MeasurementClassCode>Calibration</MeasurementClassCode></RadMeasurement>"
                + "<RadMeasurement id=\"NS\"><MeasurementClassCode>NotSpecified</MeasurementClassCode></RadMeasurement>"
                + "<RadMeasurement id=\"BG\"><MeasurementClassCode>Background</MeasurementClassCode></RadMeasurement>"
                + "<RadMeasurement id=\"FG\"><MeasurementClassCode>Foreground</MeasurementClassCode></RadMeasurement>"
                + "</RadInstrumentData>";
        String trimmed = WebIdAnalyzer.trimMultiSampleN42(multiSample);
        assertFalse(trimmed.contains("Calibration"));
        assertFalse(trimmed.contains("NotSpecified"));
        assertFalse(trimmed.contains("RadMeasurementGroup"));
        assertTrue(trimmed.contains("<MeasurementClassCode>Foreground</MeasurementClassCode>"));
        assertTrue(trimmed.contains("<MeasurementClassCode>Background</MeasurementClassCode>"));

        assertEquals("Kromek-GR1-CZT", WebIdAnalyzer.inferFallbackDrf("GR1-1_sample.spe", "Kromek GR1", ""));
        assertEquals("IdentiFINDER-R300", WebIdAnalyzer.inferFallbackDrf("R300ZH-1.spe", "", ""));
        assertEquals("IdentiFINDER-LaBr3", WebIdAnalyzer.inferFallbackDrf("SRM-1_test.n42", "", ""));
        assertEquals("1x1/CsI Side", WebIdAnalyzer.inferFallbackDrf("SNIPER-gamma.spe", "", ""));

        // Pure-Java IAEA .spe conversion check (works on Linux/Docker even without native SpecUtils JNI)
        String syntheticSpe = "$DEVICE_ID:\nidentiFINDER R300 ZH\nSN# 103156006704\n"
                + "$DATE_MEA:\n10/16/2025 10:52:45\u202fAM\n"
                + "$MEAS_TIM:\n120 120\n"
                + "$ENER_FIT:\n0.0 3.0\n"
                + "$DATA:\n0 7\n0\n4\n19\n88\n41\n9\n2\n0\n"
                + "$FLIR_NEUTRON_SWMM:\n120\n2.000\n0.000\n1.000\n"
                + "$FLIR_ANALYSIS_RESULTS:\n1\nI-131\nMED\nInnocent\n10\n";
        String speN42 = analyzer.toEnrichedN42Xml(syntheticSpe.getBytes(StandardCharsets.UTF_8), "R300_I131.spe");
        assertNotNull(speN42);
        n42Output.onNewMessage(speN42, "R300_I131.spe", "occ-spe-1");
        JsonObject speRec = toConSysJson(n42Output);
        assertEquals(1, speRec.getAsJsonArray("foregroundReports").size());
        assertEquals(120.0, speRec.getAsJsonArray("foregroundReports").get(0).getAsJsonObject().get("duration").getAsDouble(), 0.01);
        assertEquals(163.0, speRec.getAsJsonArray("foregroundReports").get(0).getAsJsonObject().get("gammaGrossCount").getAsDouble(), 0.01);
        assertEquals(2.0, speRec.getAsJsonArray("foregroundReports").get(0).getAsJsonObject().get("neutronGrossCount").getAsDouble(), 0.01);
        assertTrue(speRec.getAsJsonArray("alarmReports").get(0).getAsJsonObject().get("alarmDescription").getAsString().contains("I-131"));

        // Optional local corpus verification when developer provides -Doscar.localRiidSamplesDir=... (never stored in repo)
        String localDirProp = System.getProperty("oscar.localRiidSamplesDir",
                System.getenv().getOrDefault("OSCAR_LOCAL_RIID_SAMPLES_DIR", ""));
        if (localDirProp != null && !localDirProp.isBlank()) {
            Path localDir = Path.of(localDirProp);
            if (Files.isDirectory(localDir)) {
                List<Path> sampleFiles = new ArrayList<>();
                java.util.Map<String, Integer> perBucket = new java.util.HashMap<>();
                try (Stream<Path> walk = Files.walk(localDir)) {
                    walk.filter(Files::isRegularFile)
                            .sorted()
                            .filter(p -> {
                                String n = p.getFileName().toString().toLowerCase();
                                return n.endsWith(".n42") || n.endsWith(".spe") || n.endsWith(".xml");
                            })
                            .forEach(p -> {
                                String name = p.getFileName().toString();
                                String[] segs = name.split("__");
                                String deviceBucket = segs.length >= 3 ? segs[2] : name.substring(0, Math.min(8, name.length()));
                                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
                                String key = deviceBucket + ext;
                                int seen = perBucket.getOrDefault(key, 0);
                                if (seen < 6) {
                                    perBucket.put(key, seen + 1);
                                    sampleFiles.add(p);
                                }
                            });
                }
                assertFalse("Expected local RIID sample files when property is provided", sampleFiles.isEmpty());
                int parsedCount = 0;
                for (Path p : sampleFiles) {
                    byte[] raw = Files.readAllBytes(p);
                    String enriched = analyzer.toEnrichedN42Xml(raw, p.getFileName().toString());
                    assertNotNull("Failed enriching " + p, enriched);
                    n42Output.onNewMessage(enriched, p.getFileName().toString(), "occ-local");
                    JsonObject recJson = toConSysJson(n42Output);
                    int fg = recJson.getAsJsonArray("foregroundReports").size();
                    int bg = recJson.getAsJsonArray("backgroundReports").size();
                    assertTrue("Expected non-empty foreground or background spectrum for " + p.getFileName(), fg + bg >= 1);
                    parsedCount++;
                }
                assertEquals(sampleFiles.size(), parsedCount);
            }
        }
    }

    @Test
    public void testUpSetAggregationAndPdfJsonXlsxRendering() throws Exception {
        // Verify isotope symbol normalization and source-category bit classification
        assertEquals("I-131", ReportDataAggregator.normalizeIsotopeSymbol("Iodine131"));
        assertEquals("I-131", ReportDataAggregator.normalizeIsotopeSymbol("I131"));
        assertEquals("I-131", ReportDataAggregator.normalizeIsotopeSymbol("Iodine 131"));
        assertEquals("K", ReportDataAggregator.normalizeIsotopeSymbol("Potassium"));
        assertEquals("K", ReportDataAggregator.normalizeIsotopeSymbol("K40"));
        assertEquals("Ba", ReportDataAggregator.normalizeIsotopeSymbol("Ba133"));
        assertEquals("U-235", ReportDataAggregator.normalizeIsotopeSymbol("Uranium235"));

        assertEquals(ReportDataAggregator.UPSET_BIT_MEDICAL, ReportDataAggregator.classifySourceCategoryBits("I131"));
        assertEquals(ReportDataAggregator.UPSET_BIT_NORM, ReportDataAggregator.classifySourceCategoryBits("K40"));
        assertEquals(ReportDataAggregator.UPSET_BIT_INDUSTRIAL, ReportDataAggregator.classifySourceCategoryBits("Ba133"));
        assertEquals(ReportDataAggregator.UPSET_BIT_SNM, ReportDataAggregator.classifySourceCategoryBits("U235"));
        assertEquals(ReportDataAggregator.UPSET_BIT_UNKNOWN, ReportDataAggregator.classifySourceCategoryBits("Unk-634"));

        // Model the multi-category occupancy intersections (including SNM highlight subsets)
        long[] countsByMask = new long[128];
        int g = ReportDataAggregator.UPSET_BIT_GAMMA;
        int norm = ReportDataAggregator.UPSET_BIT_NORM;
        int unk = ReportDataAggregator.UPSET_BIT_UNKNOWN;
        int ind = ReportDataAggregator.UPSET_BIT_INDUSTRIAL;
        int med = ReportDataAggregator.UPSET_BIT_MEDICAL;
        int snm = ReportDataAggregator.UPSET_BIT_SNM;
        int neut = ReportDataAggregator.UPSET_BIT_NEUTRON;

        countsByMask[g | norm] = 4399;
        countsByMask[g | unk] = 278;
        countsByMask[g | ind] = 231;
        countsByMask[g | norm | unk] = 152;
        countsByMask[g | med] = 123;
        countsByMask[g | norm | ind] = 56;
        countsByMask[g | snm] = 35;
        countsByMask[neut] = 26;
        countsByMask[g | norm | med] = 22;
        countsByMask[g | ind | snm] = 9;
        countsByMask[g | norm | snm] = 5;
        countsByMask[g | unk | snm] = 4;
        countsByMask[g | neut] = 2;

        long total = 0;
        for (long c : countsByMask) total += c;

        ReportDataAggregator.UpSetCoOccurrenceData upSet = ReportDataAggregator.finalizeUpSetData(countsByMask, total);
        assertEquals(5342L, upSet.totalAlarmingOccupancies());
        assertEquals("Gamma", upSet.sets().get(0).name());
        assertEquals(5316L, upSet.sets().get(0).count());
        assertEquals("NORM", upSet.sets().get(1).name());
        assertEquals(4634L, upSet.sets().get(1).count());
        assertTrue(upSet.intersections().stream().anyMatch(ReportDataAggregator.UpSetIntersection::snmPresent));

        // 1. JSON serialization check
        StringWriter sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.beginObject();
            ReportDataAggregator.writeUpSetJson(jw, upSet);
            jw.endObject();
        }
        JsonObject parsedJson = JsonParser.parseString(sw.toString()).getAsJsonObject()
                .getAsJsonObject("upSetCoOccurrence");
        assertEquals(5342L, parsedJson.get("totalOccupancies").getAsLong());
        assertEquals(7, parsedJson.getAsJsonArray("sets").size());
        assertEquals(13, parsedJson.getAsJsonArray("intersections").size());

        // 2. XLSX workbook sheet check
        XlsxWorkbookWriter.SheetSpec sheet = ReportDataAggregator.buildUpSetXlsxSheet(upSet);
        ByteArrayOutputStream xlsxBaos = new ByteArrayOutputStream();
        XlsxWorkbookWriter.writeWorkbook(xlsxBaos, List.of(sheet));
        byte[] xlsxBytes = xlsxBaos.toByteArray();
        assertTrue(xlsxBytes.length > 500);
        boolean sawSheetXml = false;
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(xlsxBytes))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if ("xl/worksheets/sheet1.xml".equals(ze.getName())) {
                    sawSheetXml = true;
                }
            }
        }
        assertTrue(sawSheetXml);

        // 3. Vector PDFBox 3 UpSet chart rendering check
        ByteArrayOutputStream pdfBaos = new ByteArrayOutputStream();
        Instant now = Instant.parse("2026-11-15T18:00:00Z");
        try (PdfDocumentBuilder pdf = new PdfDocumentBuilder("Adjudication Report", "OSCAR-TEST-NODE")) {
            pdf.drawReportBanner(new PdfDocumentBuilder.HeaderMetadata(
                    "Adjudication Report",
                    "OSCAR-TEST-NODE",
                    "Alarm & Source-Category Co-Occurrence Preview",
                    "Scope: All Configured Site Lanes (50 lanes)",
                    now.minusSeconds(86400 * 30L),
                    now
            ));
            new ChartGenerator().drawUpSetChart(
                    pdf,
                    "Occupancies by Alarm & Source-Category Combination (SNM Highlighted)",
                    upSet,
                    18
            );
            pdf.finishAndSave(pdfBaos);
        }

        byte[] pdfBytes = pdfBaos.toByteArray();
        assertTrue(pdfBytes.length > 2000);
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertEquals(1, doc.getNumberOfPages());
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Gamma"));
            assertTrue(text.contains("NORM"));
            assertTrue(text.contains("SNM"));
            assertTrue(text.contains("Occupancies"));
            assertTrue(text.contains("4,399"));
        }

        String previewOut = System.getProperty("oscar.upsetPreviewPdf",
                System.getenv().getOrDefault("OSCAR_UPSET_PREVIEW_PDF", ""));
        if (previewOut != null && !previewOut.isBlank()) {
            Path outPath = Path.of(previewOut);
            Files.createDirectories(outPath.getParent());
            Files.write(outPath, pdfBytes);
        }
    }

    @Test
    public void testUpSetPopulatedCategoriesFirstAndOtherSnmRollupExactSum() throws Exception {
        int g = ReportDataAggregator.UPSET_BIT_GAMMA;
        int norm = ReportDataAggregator.UPSET_BIT_NORM;
        int unk = ReportDataAggregator.UPSET_BIT_UNKNOWN;
        int ind = ReportDataAggregator.UPSET_BIT_INDUSTRIAL;
        int med = ReportDataAggregator.UPSET_BIT_MEDICAL;
        int snm = ReportDataAggregator.UPSET_BIT_SNM;
        int neut = ReportDataAggregator.UPSET_BIT_NEUTRON;

        // 1. Verify populated categories always come before zero-count categories
        long[] sparseMasks = new long[128];
        sparseMasks[g] = 12;
        sparseMasks[snm | neut] = 3;
        ReportDataAggregator.UpSetCoOccurrenceData sparseUpSet =
                ReportDataAggregator.finalizeUpSetData(sparseMasks, 15L);
        List<ReportDataAggregator.UpSetSetTotal> orderedSparse =
                ChartGenerator.orderDisplaySetsPopulatedFirst(sparseUpSet.sets());
        assertEquals("Gamma", orderedSparse.get(0).name());
        assertEquals(12L, orderedSparse.get(0).count());
        assertEquals("SNM", orderedSparse.get(1).name());
        assertEquals(3L, orderedSparse.get(1).count());
        assertEquals("Neutron", orderedSparse.get(2).name());
        assertEquals(3L, orderedSparse.get(2).count());
        for (int i = 3; i < orderedSparse.size(); i++) {
            assertEquals("Zero-count categories must appear last", 0L, orderedSparse.get(i).count());
        }

        // 2. Reproduce the exact 38-SNM (10 in top-18 + 28 across rare tail combinations) scenario
        long[] countsByMask = new long[128];
        countsByMask[g | unk] = 48868;
        countsByMask[g] = 17367;
        countsByMask[neut | unk] = 6480;
        countsByMask[unk] = 5365;
        countsByMask[g | norm] = 3706;
        countsByMask[g | ind] = 956;
        countsByMask[ind | unk] = 819;
        countsByMask[norm | neut] = 800;
        countsByMask[g | neut | unk] = 327;
        countsByMask[g | neut] = 198;
        countsByMask[med | neut] = 145;
        countsByMask[g | med] = 79;
        countsByMask[g | norm | unk] = 72;
        countsByMask[g | ind | norm] = 59;
        countsByMask[ind | med | neut] = 24;
        countsByMask[g | ind | med | neut] = 24;
        countsByMask[g | med | neut] = 12;
        countsByMask[snm | neut] = 10; // Rank #18!
        // Rare SNM tail combinations in ranks #19..#23 totaling 28 occupancies (10 + 28 = 38 total SNM)
        countsByMask[g | snm] = 9;
        countsByMask[g | snm | neut] = 8;
        countsByMask[g | unk | snm] = 6;
        countsByMask[g | norm | snm] = 3;
        countsByMask[snm] = 2;

        long total = 0L;
        for (long c : countsByMask) {
            total += c;
        }
        ReportDataAggregator.UpSetCoOccurrenceData data =
                ReportDataAggregator.finalizeUpSetData(countsByMask, total);

        long expectedSnmSetTotal = data.sets().stream()
                .filter(ReportDataAggregator.UpSetSetTotal::snm)
                .mapToLong(ReportDataAggregator.UpSetSetTotal::count)
                .sum();
        assertEquals(38L, expectedSnmSetTotal);

        List<ChartGenerator.DisplayedUpSetColumn> cols =
                ChartGenerator.buildDisplayedUpSetColumns(data.intersections(), 18);
        assertEquals(18, cols.size());
        ChartGenerator.DisplayedUpSetColumn lastCol = cols.get(cols.size() - 1);
        assertTrue(lastCol.otherSnmRollup());
        assertEquals("Other SNM", lastCol.label());
        assertEquals(28L, lastCol.count());

        long displayedSnmSum = cols.stream()
                .filter(ChartGenerator.DisplayedUpSetColumn::snmPresent)
                .mapToLong(ChartGenerator.DisplayedUpSetColumn::count)
                .sum();
        assertEquals("Displayed SNM vertical bars (10 + 28) must equal left SNM bar (38)",
                expectedSnmSetTotal, displayedSnmSum);

        ByteArrayOutputStream pdfBaos = new ByteArrayOutputStream();
        Instant now = Instant.parse("2026-11-18T02:00:00Z");
        try (PdfDocumentBuilder pdf = new PdfDocumentBuilder("Adjudication Report", "default")) {
            pdf.drawReportBanner(new PdfDocumentBuilder.HeaderMetadata(
                    "Adjudication Report",
                    "default",
                    "All Selected Lanes (50 Lanes)",
                    "Scope: 50 lanes",
                    now.minusSeconds(86400 * 30L),
                    now
            ));
            new ChartGenerator().drawUpSetChart(
                    pdf,
                    "Alarm & Source Category Co-Occurrence (Occupancies)",
                    data,
                    18
            );
            pdf.finishAndSave(pdfBaos);
        }
        byte[] pdfBytes = pdfBaos.toByteArray();
        String rollupPreviewOut = System.getProperty("oscar.upsetRollupPreviewPdf",
                System.getenv().getOrDefault("OSCAR_UPSET_ROLLUP_PREVIEW_PDF", ""));
        if (!rollupPreviewOut.isBlank()) {
            Path p = Path.of(rollupPreviewOut);
            Files.createDirectories(p.getParent());
            Files.write(p, pdfBytes);
        }
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Other SNM"));
            assertTrue(text.contains("28"));
            assertTrue(text.contains("10"));
            assertTrue(text.contains("38"));
        }
    }
}
