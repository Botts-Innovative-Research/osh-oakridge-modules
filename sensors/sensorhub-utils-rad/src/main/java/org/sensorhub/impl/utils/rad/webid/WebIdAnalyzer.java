package org.sensorhub.impl.utils.rad.webid;

import gov.sandia.specutils.SpecUtilsNativeLoader;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.module.IModule;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.impl.utils.rad.cambio.CambioConverter;
import org.sensorhub.impl.utils.rad.model.OccupancyExtended;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.sensorhub.impl.utils.rad.output.N42Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebIdAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(WebIdAnalyzer.class);

    private static final String RADDATA_PREFIX = "RADDATA://";
    private static final Set<String> SPECTROSCOPIC_EXTENSIONS = Set.of(
            ".n42", ".spe", ".spc", ".chn", ".pcf", ".cnf", ".tka", ".mca", ".phd", ".lzs", ".mps", ".aram"
    );
    private static final Pattern RAD_SAMPLER_TAG_PATTERN = Pattern.compile("<([A-Za-z0-9_]+)>([\\s\\S]*?)</\\1>");
    private static final Pattern RAD_SAMPLER_ISOTOPE_BLOCK = Pattern.compile("<IsotopeData>([\\s\\S]*?)</IsotopeData>");
    private static final Pattern RAD_SAMPLER_CAL_BLOCK = Pattern.compile("<EnergyCalibrationCoefficients>([\\s\\S]*?)</EnergyCalibrationCoefficients>");
    private static final Pattern RAD_MEASUREMENT_BLOCK = Pattern.compile("<RadMeasurement\\b[\\s\\S]*?</RadMeasurement>");

    ISensorHub hub;
    WebIdClient webIdClient;
    IdEncoder obsIdEncoder;

    private record VendorNuclide(String name, String category, String confidence) {}

    public WebIdAnalyzer(WebIdClient webIdClient, ISensorHub hub) {
        this.hub = hub;
        this.webIdClient = webIdClient;
        this.obsIdEncoder = hub != null && hub.getIdEncoders() != null ? hub.getIdEncoders().getObsIdEncoder() : null;
    }

    /**
     * Determines whether an uploaded file/payload contains spectroscopic radiation data
     * that should be parsed and published to OSCAR's {@link N42Output}.
     */
    public static boolean isSpectroscopicPayload(String fileName, byte[] data) {
        if (isRadDataQrCodeBytes(data)) {
            return true;
        }
        String lowerName = fileName != null ? fileName.trim().toLowerCase(Locale.ROOT) : "";
        for (String ext : SPECTROSCOPIC_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        if (lowerName.endsWith(".n42.xml")) {
            return true;
        }
        if (data != null && data.length > 0 && (lowerName.endsWith(".xml") || lowerName.isEmpty())) {
            int probeLen = Math.min(data.length, 4096);
            String head = new String(data, 0, probeLen, StandardCharsets.UTF_8);
            return head.contains("<RadInstrumentData")
                    || head.contains("<N42InstrumentData")
                    || head.contains("<RadSamplerData")
                    || head.contains("$SPEC_REM:")
                    || head.contains("$MEAS_TIM:")
                    || head.contains("$DATA:");
        }
        return false;
    }

    /**
     * Converts (if needed) and publishes spectroscopic foreground/background attachments to the lane's
     * {@link N42Output} datastream so OSCAR's N42 Report section always renders device spectra and alarms.
     */
    public void publishLocalN42Report(WebIdRequestContext webIdContext) {
        if (webIdContext == null || webIdContext.laneUid == null || webIdContext.laneUid.isBlank()) {
            return;
        }

        N42Output<?> n42output = resolveLaneN42Output(webIdContext.laneUid);
        if (n42output == null) {
            logger.warn("No N42Output found on lane {} for spectroscopic upload", webIdContext.laneUid);
            return;
        }

        String dir = webIdContext.getDirectory() != null ? webIdContext.getDirectory() : "";
        Path objectDir = Path.of(dir);
        String occupancyObsId = webIdContext.occupancyObsId;

        String fgName = webIdContext.hasForegroundFileName()
                ? webIdContext.getForegroundFileName()
                : webIdContext.getObjectKey();
        if (webIdContext.foregroundData != null
                && !isRadDataQrCode(webIdContext.foregroundData)
                && isSpectroscopicPayload(fgName, webIdContext.foregroundData)) {
            publishSinglePartToN42(n42output, webIdContext.foregroundData, fgName, objectDir, occupancyObsId);
        }

        if (webIdContext.backgroundData != null
                && webIdContext.hasBackgroundFileName()
                && !isRadDataQrCode(webIdContext.backgroundData)
                && isSpectroscopicPayload(webIdContext.getBackgroundFileName(), webIdContext.backgroundData)) {
            publishSinglePartToN42(
                    n42output,
                    webIdContext.backgroundData,
                    webIdContext.getBackgroundFileName(),
                    objectDir,
                    occupancyObsId
            );
        }
    }

    private void publishSinglePartToN42(
            N42Output<?> n42output,
            byte[] rawBytes,
            String rawFileName,
            Path objectDir,
            String occupancyObsId
    ) {
        String effectiveName = (rawFileName != null && !rawFileName.isBlank())
                ? rawFileName
                : (UUID.randomUUID() + ".n42");
        try {
            String n42Xml = toEnrichedN42Xml(rawBytes, effectiveName);
            if (n42Xml != null && !n42Xml.isBlank()) {
                String storedName = objectDir.toString().isEmpty()
                        ? effectiveName
                        : objectDir.resolve(effectiveName).toString();
                n42output.onNewMessage(n42Xml, storedName, occupancyObsId);
            }
        } catch (Throwable e) {
            logger.error("Failed to publish N42 report for spectroscopic file {}", effectiveName, e);
        }
    }

    private N42Output<?> resolveLaneN42Output(String laneUid) {
        if (hub == null || hub.getModuleRegistry() == null || laneUid == null || laneUid.isBlank()) {
            return null;
        }
        try {
            for (IModule<?> module : hub.getModuleRegistry().getLoadedModules()) {
                if (module instanceof IDataProducerModule<?> producer) {
                    try {
                        if (laneUid.equals(producer.getUniqueIdentifier())) {
                            var out = producer.getOutputs().get(N42Output.SENSOR_OUTPUT_NAME);
                            if (out instanceof N42Output<?> n42) {
                                return n42;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to resolve lane N42Output for UID: {}", laneUid, e);
        }
        return null;
    }

    public WebIdAnalysis processWebIdRequest(WebIdRequestContext webIdContext) {
        // Always publish local N42 spectrum & device alarm report first
        publishLocalN42Report(webIdContext);

        WebIdAnalysis analysis = null;
        boolean reachable = webIdClient != null && webIdClient.isReachable();
        logger.info("WebID reachability check: client={}, reachable={}", webIdClient != null ? "present" : "null", reachable);
        if (reachable) {
            analysis = processWebIdAnalysis(webIdContext);
        } else {
            logger.info("WebID unreachable; local N42 report published for (objectKey={}, foregroundFileName={})",
                    webIdContext.getObjectKey(), webIdContext.getForegroundFileName());
        }
        return analysis;
    }

    private WebIdAnalysis processWebIdAnalysis(WebIdRequestContext webIdContext) {
        try {
            if (webIdContext.foregroundData == null) {
                throw new IllegalArgumentException("Foreground data is null");
            }

            boolean isQrCodeData = isRadDataQrCode(webIdContext.foregroundData);
            String fgName = webIdContext.hasForegroundFileName()
                    ? webIdContext.getForegroundFileName()
                    : webIdContext.getObjectKey();

            WebIdAnalysis analysis = analyzeWithSmartFallback(webIdContext, fgName, isQrCodeData);
            if (analysis == null) {
                return null;
            }

            analysis.setSampleTime(Instant.now());
            analysis.setOccupancyObsId(webIdContext.occupancyObsId != null ? webIdContext.occupancyObsId : "");

            // Insert WebId observation into lane database
            String encodedWebIdObsId = null;
            if (webIdContext.laneUid != null && !webIdContext.laneUid.isBlank() && hub != null && obsIdEncoder != null) {
                try {
                    var laneDb = hub.getSystemDriverRegistry().getDatabase(webIdContext.laneUid);
                    if (laneDb == null) {
                        logger.error("No database found for lane UID: {}", webIdContext.laneUid);
                        return analysis;
                    }

                    var obsStore = laneDb.getObservationStore();
                    var dsKeys = obsStore.getDataStreams().selectKeys(new DataStreamFilter.Builder()
                            .withSystems()
                            .withUniqueIDs(webIdContext.laneUid)
                            .done()
                            .withOutputNames("webIdAnalysis")
                            .build()).toList();

                    if (dsKeys.isEmpty()) {
                        logger.error("No webIdAnalysis datastream found for lane: {}", webIdContext.laneUid);
                        return analysis;
                    }

                    var dsId = dsKeys.get(0).getInternalID();
                    DataBlock webIdDataBlock = WebIdAnalysis.fromWebIdAnalysis(analysis);

                    var txnHandler = new SystemDatabaseTransactionHandler(hub.getEventBus(), laneDb);
                    var obsId = txnHandler.getDataStreamHandler(dsId).addObs(webIdDataBlock);

                    encodedWebIdObsId = obsIdEncoder.encodeID(obsId);
                    logger.info("WebId analysis observation stored with ID: {}", encodedWebIdObsId);
                } catch (Exception e) {
                    logger.error("Failed to insert WebId observation into lane database", e);
                }
            }

            // Attach WebId obs ID to occupancy observation
            if (encodedWebIdObsId != null
                    && webIdContext.occupancyObsId != null
                    && !webIdContext.occupancyObsId.isBlank()
                    && webIdContext.laneUid != null
                    && hub != null
                    && obsIdEncoder != null) {
                try {
                    BigId decodedOccObsId = obsIdEncoder.decodeID(webIdContext.occupancyObsId);
                    var laneDb = hub.getSystemDriverRegistry().getDatabase(webIdContext.laneUid);
                    if (laneDb == null) {
                        logger.error("No database found for lane UID when updating occupancy: {}", webIdContext.laneUid);
                        return analysis;
                    }

                    var obsStore = laneDb.getObservationStore();
                    var obs = obsStore.get(decodedOccObsId);
                    if (obs == null) {
                        logger.error("Occupancy observation not found for ID: {}", webIdContext.occupancyObsId);
                        return analysis;
                    }

                    var dsInfo = obsStore.getDataStreams().get(new DataStreamKey(obs.getDataStreamID()));
                    var occupancyRecord = dsInfo != null ? dsInfo.getRecordStructure() : null;

                    var occupancy = OccupancyExtended.readOccupancy(occupancyRecord, obs.getResult());
                    occupancy.addWebIdObsId(encodedWebIdObsId);

                    DataBlock newOccupancyResult = OccupancyExtended.writeOccupancy(occupancyRecord, occupancy);
                    obs.getResult().setUnderlyingObject(newOccupancyResult.getUnderlyingObject());
                    obs.getResult().updateAtomCount();

                    obsStore.put(decodedOccObsId, obs);
                    logger.info("Occupancy observation {} updated with WebId obs ID: {}", webIdContext.occupancyObsId, encodedWebIdObsId);
                } catch (Exception e) {
                    logger.error("Failed to update occupancy observation with WebId obs ID", e);
                }
            }

            return analysis;
        } catch (Exception e) {
            logger.error("WebId analysis processing failed", e);
            return null;
        }
    }

    private WebIdAnalysis analyzeWithSmartFallback(
            WebIdRequestContext webIdContext,
            String fgName,
            boolean isQrCodeData
    ) throws Exception {
        byte[] fgPayload = webIdContext.foregroundData;
        String rawText = isQrCodeData ? "" : new String(fgPayload, StandardCharsets.UTF_8);

        // RadComm Syclone <RadSamplerData> is proprietary XML not natively parsed by Sandia WebID; convert upfront
        if (!isQrCodeData && rawText.contains("<RadSamplerData")) {
            String converted = convertRadSamplerDataToN42(rawText);
            if (converted != null) {
                fgPayload = converted.getBytes(StandardCharsets.UTF_8);
            }
        }

        byte[] bgPayload = webIdContext.backgroundData;
        if (bgPayload != null && !isN42_2012(bgPayload)) {
            try {
                String bgXml = toEnrichedN42Xml(bgPayload, webIdContext.getBackgroundFileName());
                if (bgXml != null) {
                    bgPayload = bgXml.getBytes(StandardCharsets.UTF_8);
                }
            } catch (Throwable ignored) {
            }
        }

        // Sandia WebID prohibits synthesizeBackground=true when an explicit background file is provided
        boolean synthBg = bgPayload == null && webIdContext.synthesizeBackground;
        String requestedDrf = (webIdContext.drf != null && !webIdContext.drf.isBlank())
                ? webIdContext.drf.trim()
                : "auto";
        boolean autoDrfRequested = "auto".equalsIgnoreCase(requestedDrf);

        WebIdAnalysis firstAttempt = null;
        Exception firstException = null;
        try {
            firstAttempt = webIdClient.analyze(buildRequest(fgPayload, bgPayload, requestedDrf, synthBg));
            if (firstAttempt != null && firstAttempt.getAnalysisError() == 0) {
                logger.info("WebID analysis succeeded on initial attempt (drf={})", firstAttempt.getDrf());
                return firstAttempt;
            }
        } catch (Exception e) {
            firstException = e;
            if (isQrCodeData) {
                throw new IOException("WebID analysis failed for QR code data", e);
            }
        }

        if (isQrCodeData) {
            return firstAttempt;
        }

        String errMsg = firstAttempt != null && firstAttempt.getErrorMessage() != null
                ? firstAttempt.getErrorMessage()
                : (firstException != null && firstException.getMessage() != null ? firstException.getMessage() : "");
        String lowerErr = errMsg.toLowerCase(Locale.ROOT);
        logger.info("WebID initial attempt incomplete (err={}), attempting smart N42/DRF fallback", errMsg);

        // When Sandia WebID natively parses an IAEA .spe file except for unknown DRF (e.g. GR1-1 / SNIPER GN),
        // retry the original .spe payload with our inferred DRF first before converting to synthetic N42.
        if (autoDrfRequested && lowerErr.contains("detector type") && isIaeaSpeText(rawText)) {
            String speDrf = inferFallbackDrf(fgName, rawText, "");
            try {
                WebIdAnalysis speDrfAttempt = webIdClient.analyze(buildRequest(webIdContext.foregroundData, bgPayload, speDrf, synthBg));
                if (speDrfAttempt != null && speDrfAttempt.getAnalysisError() == 0) {
                    logger.info("WebID raw .spe retry succeeded with inferred DRF={}", speDrf);
                    return speDrfAttempt;
                }
            } catch (Exception ignored) {
            }
        }

        String n42Xml;
        try {
            n42Xml = toEnrichedN42Xml(webIdContext.foregroundData, fgName);
        } catch (Throwable convErr) {
            if (firstAttempt != null) {
                return firstAttempt;
            }
            if (firstException != null) {
                throw firstException;
            }
            throw new IOException("Spectroscopic conversion failed", convErr);
        }

        boolean retrySynthBg = synthBg;
        if (lowerErr.contains("more than one foreground") || countRadMeasurements(n42Xml) > 2) {
            n42Xml = trimMultiSampleN42(n42Xml);
        }
        boolean hasEmbeddedBg = n42Xml.contains("<MeasurementClassCode>Background</MeasurementClassCode>");
        if (bgPayload == null) {
            retrySynthBg = !hasEmbeddedBg;
        }

        String retryDrf = requestedDrf;
        if (autoDrfRequested && (lowerErr.contains("detector type")
                || lowerErr.contains("failed to parse")
                || rawText.contains("<RadSamplerData"))) {
            retryDrf = inferFallbackDrf(fgName, rawText, n42Xml);
        }

        byte[] retryFgBytes = n42Xml.getBytes(StandardCharsets.UTF_8);
        WebIdAnalysis retryAttempt = webIdClient.analyze(buildRequest(retryFgBytes, bgPayload, retryDrf, retrySynthBg));
        if (retryAttempt != null && retryAttempt.getAnalysisError() != 0 && autoDrfRequested) {
            String retryErr = retryAttempt.getErrorMessage() != null
                    ? retryAttempt.getErrorMessage().toLowerCase(Locale.ROOT)
                    : "";
            if (retryErr.contains("detector type")) {
                String fallbackDrf = inferFallbackDrf(fgName, rawText, n42Xml);
                String nextDrf = fallbackDrf.equals(retryDrf) ? "2x2/NaI" : fallbackDrf;
                retryAttempt = webIdClient.analyze(buildRequest(retryFgBytes, bgPayload, nextDrf, retrySynthBg));
            }
        }
        return retryAttempt != null ? retryAttempt : firstAttempt;
    }

    private static WebIdRequest buildRequest(byte[] fg, byte[] bg, String drf, boolean synthBg) {
        WebIdRequest.Builder builder = new WebIdRequest.Builder()
                .foreground(new ByteArrayInputStream(fg))
                .synthesizeBackground(synthBg);
        if (bg != null) {
            builder.background(new ByteArrayInputStream(bg));
        }
        if (drf != null && !drf.isBlank() && !"auto".equalsIgnoreCase(drf.trim())) {
            builder.drf(drf.trim());
        }
        return builder.build();
    }

    /**
     * Converts any supported spectroscopic payload into sanitized ANSI N42.42-2012 XML while preserving
     * vendor-embedded nuclide identifications (such as FLIR {@code $FLIR_ANALYSIS_RESULTS:} in {@code .spe}
     * and RadComm Syclone {@code <RadSamplerData><IdentificationData><Isotopes>} in {@code .xml}).
     */
    public String toEnrichedN42Xml(byte[] rawBytes, String fileName) throws Exception {
        if (rawBytes == null || rawBytes.length == 0) {
            return null;
        }
        String rawText = new String(rawBytes, StandardCharsets.UTF_8);
        if (isN42_2012Text(rawText)) {
            return N42Output.sanitizeN42Xml(rawText);
        }
        if (rawText.contains("<RadSamplerData")) {
            String converted = convertRadSamplerDataToN42(rawText);
            if (converted != null) {
                return N42Output.sanitizeN42Xml(converted);
            }
        }

        List<VendorNuclide> vendorNuclides = extractFlirSpeNuclides(rawText);
        String sourceFileName = (fileName != null && !fileName.isBlank())
                ? fileName
                : (UUID.randomUUID() + ".spe");

        String sanitized = null;
        if (SpecUtilsNativeLoader.isAvailable()) {
            try {
                byte[] n42Bytes = new CambioConverter().convertToN42(new ByteArrayInputStream(rawBytes), sourceFileName);
                sanitized = N42Output.sanitizeN42Xml(new String(n42Bytes, StandardCharsets.UTF_8));
            } catch (Throwable cambioErr) {
                if (!isIaeaSpeText(rawText)) {
                    throw cambioErr instanceof Exception ex ? ex : new IOException("Cambio conversion failed", cambioErr);
                }
                logger.debug("Cambio conversion unavailable/failed for {}; using pure-Java IAEA SPE converter", sourceFileName);
            }
        }

        if (sanitized == null) {
            if (isIaeaSpeText(rawText)) {
                String convertedSpe = convertIaeaSpeToN42(rawText, sourceFileName);
                if (convertedSpe != null) {
                    sanitized = N42Output.sanitizeN42Xml(convertedSpe);
                }
            }
            if (sanitized == null) {
                // Trigger standard CambioConverter exception message when neither JNI nor pure-Java SPE applies
                byte[] n42Bytes = new CambioConverter().convertToN42(new ByteArrayInputStream(rawBytes), sourceFileName);
                sanitized = N42Output.sanitizeN42Xml(new String(n42Bytes, StandardCharsets.UTF_8));
            }
        }

        if (!vendorNuclides.isEmpty() && !sanitized.contains("<Nuclide>")) {
            sanitized = injectVendorNuclidesIntoN42(sanitized, vendorNuclides);
        }
        return sanitized;
    }

    static boolean isIaeaSpeText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.contains("$DATA:")
                && (text.contains("$MEAS_TIM:")
                || text.contains("$SPEC_REM:")
                || text.contains("$SPEC_ID:")
                || text.contains("$DEVICE_ID:")
                || text.contains("$DATE_MEA:"));
    }

    private static final List<DateTimeFormatter> SPE_DATE_FORMATTERS = List.of(
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yyyy h:mm:ss a").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/yyyy H:mm:ss").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-M-yyyy H:mm:ss").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M-d-yyyy H:mm:ss").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy-MM-dd H:mm:ss").toFormatter(Locale.US)
    );

    /**
     * Pure-Java converter for IAEA ASCII {@code .spe} spectra (Kromek GR1, FLIR identiFINDER R300,
     * Ortec/ Detective / SNIPER-GN gamma & neutron {@code .spe}, etc.) so Linux/Docker deployments
     * work seamlessly even when the platform-specific SpecUtils JNI library is absent.
     */
    static String convertIaeaSpeToN42(String speText, String fileName) {
        if (!isIaeaSpeText(speText)) {
            return null;
        }
        String normalizedText = speText.charAt(0) == '\uFEFF' ? speText.substring(1) : speText;
        String[] lines = normalizedText.split("\\r?\\n");

        String manufacturer = "RIID";
        String model = "Spectrometer";
        String serial = "SPE-1";
        String detKind = "NaI";
        String startIso = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        double liveSec = 60.0;
        double realSec = 60.0;
        String coeffValues = null;
        List<Double> channels = new ArrayList<>(1024);
        double flirNeutronCounts = Double.NaN;
        double flirMeanDoseUsvh = Double.NaN;
        boolean backgroundClass = false;

        String lowerFile = fileName != null ? fileName.toLowerCase(Locale.ROOT) : "";
        boolean neutronSpectrumFile = lowerFile.endsWith("-neutron.spe") || lowerFile.contains("_neutron.spe");

        for (int i = 0; i < lines.length; i++) {
            String tag = lines[i].trim();
            if (tag.isEmpty() || !tag.startsWith("$")) {
                continue;
            }
            String upperTag = tag.toUpperCase(Locale.ROOT);
            switch (upperTag) {
                case "$SPEC_REM:", "$SPEC_ID:", "$APPLICATION_ID:", "$DEVICE_ID:" -> {
                    List<String> sec = readSpeSectionLines(lines, i + 1);
                    String joined = String.join(" ", sec);
                    String lower = joined.toLowerCase(Locale.ROOT);
                    if (lower.contains("background")) {
                        backgroundClass = true;
                    }
                    if ("$SPEC_ID:".equals(upperTag) && lower.endsWith("-neutron")) {
                        neutronSpectrumFile = true;
                    }
                    if (lower.contains("gr-1") || lower.contains("gr1") || lower.contains("kromek") || lower.contains("sab3r")) {
                        manufacturer = "Kromek";
                        model = "GR1";
                        detKind = "CZT";
                    } else if (lower.contains("r300") || lower.contains("identifinder")) {
                        manufacturer = "FLIR";
                        model = "identiFINDER R300";
                        detKind = "CZT";
                    } else if (lower.contains("sniper")) {
                        manufacturer = "SNIPER";
                        model = "SNIPER-GN";
                        detKind = "CsI";
                    }
                    for (String s : sec) {
                        String st = s.trim();
                        if (st.startsWith("SN#")) {
                            serial = st.substring(3).trim();
                        } else if ("$SPEC_ID:".equals(upperTag) && !st.isEmpty()) {
                            serial = st;
                        }
                    }
                }
                case "$FLIR_SPECTRUM_TYPE:" -> {
                    List<String> sec = readSpeSectionLines(lines, i + 1);
                    for (String s : sec) {
                        if (s.toLowerCase(Locale.ROOT).contains("background")) {
                            backgroundClass = true;
                        }
                    }
                }
                case "$DATE_MEA:" -> {
                    List<String> sec = readSpeSectionLines(lines, i + 1);
                    if (!sec.isEmpty()) {
                        String parsedIso = parseSpeDateToIso(sec.get(0));
                        if (parsedIso != null) {
                            startIso = parsedIso;
                        }
                    }
                }
                case "$MEAS_TIM:" -> {
                    List<String> sec = readSpeSectionLines(lines, i + 1);
                    if (!sec.isEmpty()) {
                        String[] parts = sec.get(0).trim().split("\\s+");
                        if (parts.length >= 1) {
                            liveSec = parsePositiveDouble(parts[0], liveSec);
                            realSec = parts.length >= 2 ? parsePositiveDouble(parts[1], liveSec) : liveSec;
                        }
                    }
                }
                case "$RT:" -> {
                    List<String> sec = readSpeSectionLines(lines, i + 1);
                    if (!sec.isEmpty()) {
                        realSec = parsePositiveDouble(sec.get(0).trim(), realSec);
                    }
                }
                case "$ENER_FIT:", "$SPEC_CAL:", "$MCA_CAL:" -> {
                    List<String> sec = readSpeSectionLines(lines, i + 1);
                    List<Double> nums = new ArrayList<>();
                    for (String s : sec) {
                        for (String tok : s.trim().split("\\s+")) {
                            try {
                                nums.add(Double.parseDouble(tok));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    // Note: $MCA_CAL: first line can be polynomial order integer (e.g. "2" or "3")
                    if ("$MCA_CAL:".equals(upperTag) && nums.size() >= 3 && (nums.get(0) == 2.0 || nums.get(0) == 3.0)) {
                        nums = nums.subList(1, nums.size());
                    }
                    if (nums.size() >= 2) {
                        double c0 = nums.get(0);
                        double c1 = nums.get(1);
                        double c2 = nums.size() >= 3 ? nums.get(2) : 0.0;
                        if (Math.abs(c1) > 1e-9) {
                            coeffValues = c0 + " " + c1 + " " + c2;
                        }
                    }
                }
                case "$FLIR_NEUTRON_SWMM:" -> {
                    List<String> sec = readSpeSectionLines(lines, i + 1);
                    if (sec.size() >= 2) {
                        try {
                            flirNeutronCounts = Double.parseDouble(sec.get(1).trim());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                case "$FLIR_DOSE_RATE_SWMM:" -> {
                    List<String> sec = readSpeSectionLines(lines, i + 1);
                    if (sec.size() >= 2) {
                        try {
                            double samples = Double.parseDouble(sec.get(0).trim());
                            double sumDose = Double.parseDouble(sec.get(1).trim());
                            if (samples > 0.0) {
                                flirMeanDoseUsvh = sumDose / samples;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                case "$DATA:" -> {
                    if (i + 1 < lines.length) {
                        // i+1 holds "firstChannel lastChannel" (e.g. "0 1023" or "0 4096")
                        int cursor = i + 2;
                        while (cursor < lines.length) {
                            String dl = lines[cursor].trim();
                            if (dl.startsWith("$")) {
                                break;
                            }
                            if (!dl.isEmpty()) {
                                for (String tok : dl.split("\\s+")) {
                                    try {
                                        channels.add(Double.parseDouble(tok));
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                            cursor++;
                        }
                        i = cursor - 1;
                    }
                }
                default -> {
                }
            }
        }

        if (channels.isEmpty()) {
            return null;
        }

        if (lowerFile.contains("sniper") && "RIID".equals(manufacturer)) {
            manufacturer = "SNIPER";
            model = "SNIPER-GN";
            detKind = "CsI";
        } else if ((lowerFile.contains("gr1") || lowerFile.contains("gr-1")) && "RIID".equals(manufacturer)) {
            manufacturer = "Kromek";
            model = "GR1";
            detKind = "CZT";
        } else if (lowerFile.contains("r300") && "RIID".equals(manufacturer)) {
            manufacturer = "FLIR";
            model = "identiFINDER R300";
            detKind = "CZT";
        }

        if (coeffValues == null) {
            coeffValues = channels.size() >= 4000 ? "0.0 0.7324219 0.0" : "0.0 3.0 0.0";
        }

        double channelSum = 0.0;
        StringBuilder chSb = new StringBuilder(channels.size() * 4);
        for (int idx = 0; idx < channels.size(); idx++) {
            if (idx > 0) {
                chSb.append(' ');
            }
            double v = channels.get(idx);
            channelSum += v;
            if (v == Math.rint(v)) {
                chSb.append((long) v);
            } else {
                chSb.append(v);
            }
        }

        double gammaCounts = neutronSpectrumFile ? 0.0 : channelSum;
        double neutronCounts = neutronSpectrumFile
                ? channelSum
                : (Double.isNaN(flirNeutronCounts) ? 0.0 : flirNeutronCounts);
        String classCode = backgroundClass ? "Background" : "Foreground";
        String realDurIso = String.format(Locale.US, "PT%.3fS", realSec);
        String liveDurIso = String.format(Locale.US, "PT%.3fS", liveSec);

        StringBuilder sb = new StringBuilder(chSb.length() + 2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<RadInstrumentData xmlns=\"http://physics.nist.gov/N42/2011/N42\">\n");
        sb.append("  <RadInstrumentInformation id=\"RadInst-1\">\n");
        sb.append("    <RadInstrumentManufacturerName>").append(escapeXml(manufacturer)).append("</RadInstrumentManufacturerName>\n");
        sb.append("    <RadInstrumentIdentifier>").append(escapeXml(serial)).append("</RadInstrumentIdentifier>\n");
        sb.append("    <RadInstrumentModelName>").append(escapeXml(model)).append("</RadInstrumentModelName>\n");
        sb.append("    <RadInstrumentClassCode>Radionuclide Identifier</RadInstrumentClassCode>\n");
        sb.append("    <RadInstrumentVersion>\n");
        sb.append("      <RadInstrumentComponentName>Firmware</RadInstrumentComponentName>\n");
        sb.append("      <RadInstrumentComponentVersion>1.0</RadInstrumentComponentVersion>\n");
        sb.append("    </RadInstrumentVersion>\n");
        sb.append("  </RadInstrumentInformation>\n");
        sb.append("  <RadDetectorInformation id=\"GammaDet-1\">\n");
        sb.append("    <RadDetectorCategoryCode>Gamma</RadDetectorCategoryCode>\n");
        sb.append("    <RadDetectorKindCode>").append(escapeXml(detKind)).append("</RadDetectorKindCode>\n");
        sb.append("  </RadDetectorInformation>\n");
        sb.append("  <RadDetectorInformation id=\"NeutronDet-1\">\n");
        sb.append("    <RadDetectorCategoryCode>Neutron</RadDetectorCategoryCode>\n");
        sb.append("    <RadDetectorKindCode>He3</RadDetectorKindCode>\n");
        sb.append("  </RadDetectorInformation>\n");
        sb.append("  <EnergyCalibration id=\"EnergyCal-1\">\n");
        sb.append("    <CoefficientValues>").append(escapeXml(coeffValues)).append("</CoefficientValues>\n");
        sb.append("  </EnergyCalibration>\n");
        sb.append("  <RadMeasurement id=\"Meas-1\">\n");
        sb.append("    <MeasurementClassCode>").append(classCode).append("</MeasurementClassCode>\n");
        sb.append("    <StartDateTime>").append(escapeXml(startIso)).append("</StartDateTime>\n");
        sb.append("    <RealTimeDuration>").append(realDurIso).append("</RealTimeDuration>\n");
        sb.append("    <Spectrum id=\"Spec-1\" radDetectorInformationReference=\"GammaDet-1\" energyCalibrationReference=\"EnergyCal-1\">\n");
        sb.append("      <LiveTimeDuration>").append(liveDurIso).append("</LiveTimeDuration>\n");
        sb.append("      <ChannelData>").append(chSb).append("</ChannelData>\n");
        sb.append("    </Spectrum>\n");
        sb.append("    <GrossCounts id=\"GrossGamma-1\" radDetectorInformationReference=\"GammaDet-1\">\n");
        sb.append("      <LiveTimeDuration>").append(liveDurIso).append("</LiveTimeDuration>\n");
        sb.append("      <CountData>").append(String.format(Locale.US, "%.1f", gammaCounts)).append("</CountData>\n");
        sb.append("    </GrossCounts>\n");
        sb.append("    <GrossCounts id=\"GrossNeutron-1\" radDetectorInformationReference=\"NeutronDet-1\">\n");
        sb.append("      <LiveTimeDuration>").append(liveDurIso).append("</LiveTimeDuration>\n");
        sb.append("      <CountData>").append(String.format(Locale.US, "%.1f", neutronCounts)).append("</CountData>\n");
        sb.append("    </GrossCounts>\n");
        if (!Double.isNaN(flirMeanDoseUsvh) && flirMeanDoseUsvh >= 0.0) {
            sb.append("    <DoseRate id=\"DoseRate-1\" radDetectorInformationReference=\"GammaDet-1\">\n");
            sb.append("      <DoseRateValue>").append(String.format(Locale.US, "%.4f", flirMeanDoseUsvh)).append("</DoseRateValue>\n");
            sb.append("    </DoseRate>\n");
        }
        sb.append("  </RadMeasurement>\n");
        sb.append("</RadInstrumentData>\n");
        return sb.toString();
    }

    private static List<String> readSpeSectionLines(String[] lines, int startIdx) {
        List<String> out = new ArrayList<>();
        for (int idx = startIdx; idx < lines.length; idx++) {
            String trimmed = lines[idx].trim();
            if (trimmed.startsWith("$")) {
                break;
            }
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static double parsePositiveDouble(String raw, double fallback) {
        try {
            double v = Double.parseDouble(raw);
            return v > 0.0 ? v : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String parseSpeDateToIso(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        String cleaned = rawDate.replaceAll("[\\u00A0\\u202F\\s]+", " ").trim();
        for (DateTimeFormatter fmt : SPE_DATE_FORMATTERS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(cleaned, fmt);
                return DateTimeFormatter.ISO_INSTANT.format(ldt.toInstant(ZoneOffset.UTC));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean isN42_2012(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }
        int len = Math.min(data.length, 4096);
        return isN42_2012Text(new String(data, 0, len, StandardCharsets.UTF_8));
    }

    private static boolean isN42_2012Text(String text) {
        return text != null
                && text.contains("<RadInstrumentData")
                && text.contains("http://physics.nist.gov/N42/2011/N42");
    }

    private static List<VendorNuclide> extractFlirSpeNuclides(String speText) {
        List<VendorNuclide> nuclides = new ArrayList<>();
        if (speText == null || !speText.contains("$FLIR_ANALYSIS_RESULTS:")) {
            return nuclides;
        }
        String[] lines = speText.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            if ("$FLIR_ANALYSIS_RESULTS:".equalsIgnoreCase(lines[i].trim())) {
                if (i + 1 >= lines.length) {
                    break;
                }
                int count;
                try {
                    count = Integer.parseInt(lines[i + 1].trim());
                } catch (NumberFormatException e) {
                    break;
                }
                int cursor = i + 2;
                for (int n = 0; n < count && cursor < lines.length; n++) {
                    String name = lines[cursor++].trim();
                    if (name.startsWith("$")) {
                        break;
                    }
                    String cat = cursor < lines.length && !lines[cursor].trim().startsWith("$") ? lines[cursor++].trim() : "";
                    String sev = cursor < lines.length && !lines[cursor].trim().startsWith("$") ? lines[cursor++].trim() : "";
                    String conf = cursor < lines.length && !lines[cursor].trim().startsWith("$") ? lines[cursor++].trim() : "";
                    if (!name.isEmpty() && !"none".equalsIgnoreCase(name)) {
                        String mergedCat = !cat.isEmpty() ? cat : sev;
                        nuclides.add(new VendorNuclide(name, mergedCat, conf));
                    }
                }
                break;
            }
        }
        return nuclides;
    }

    private static String injectVendorNuclidesIntoN42(String n42Xml, List<VendorNuclide> nuclides) {
        if (n42Xml == null || nuclides == null || nuclides.isEmpty()) {
            return n42Xml;
        }
        int closeIdx = n42Xml.lastIndexOf("</RadInstrumentData>");
        if (closeIdx < 0) {
            return n42Xml;
        }
        StringBuilder block = new StringBuilder();
        block.append("  <AnalysisResults id=\"VendorEmbeddedAnalysis\">\n");
        block.append("    <NuclideAnalysisResults>\n");
        for (VendorNuclide vn : nuclides) {
            block.append("      <Nuclide>\n");
            block.append("        <NuclideIdentifiedIndicator>true</NuclideIdentifiedIndicator>\n");
            block.append("        <NuclideName>").append(escapeXml(vn.name())).append("</NuclideName>\n");
            if (vn.confidence() != null && !vn.confidence().isBlank()) {
                try {
                    double numeric = Double.parseDouble(vn.confidence().trim());
                    block.append("        <NuclideIDConfidenceValue>").append(numeric).append("</NuclideIDConfidenceValue>\n");
                } catch (NumberFormatException e) {
                    block.append("        <NuclideIDConfidenceDescription>").append(escapeXml(vn.confidence().trim())).append("</NuclideIDConfidenceDescription>\n");
                }
            }
            if (vn.category() != null && !vn.category().isBlank()) {
                block.append("        <NuclideCategoryDescription>").append(escapeXml(vn.category().trim())).append("</NuclideCategoryDescription>\n");
            }
            block.append("      </Nuclide>\n");
        }
        block.append("    </NuclideAnalysisResults>\n");
        block.append("  </AnalysisResults>\n");
        return n42Xml.substring(0, closeIdx) + block + n42Xml.substring(closeIdx);
    }

    /**
     * Converts RadComm Syclone {@code <RadSamplerData>} XML into valid ANSI N42.42-2012 XML
     * preserving channel spectrum, quadratic energy calibration, neutron counts, duration, and isotope IDs.
     */
    static String convertRadSamplerDataToN42(String xml) {
        if (xml == null || !xml.contains("<RadSamplerData")) {
            return null;
        }
        String channelData = extractSimpleXmlTag(xml, "ChannelDataString");
        if (channelData == null || channelData.isBlank()) {
            return null;
        }
        String[] tokens = channelData.trim().split("\\s+");
        double gammaSum = 0.0;
        for (String tok : tokens) {
            try {
                gammaSum += Double.parseDouble(tok);
            } catch (NumberFormatException ignored) {
            }
        }

        String rawDateTime = extractSimpleXmlTag(xml, "DateTime");
        String startIso = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        if (rawDateTime != null && !rawDateTime.isBlank()) {
            String dt = rawDateTime.trim();
            startIso = (dt.endsWith("Z") || dt.matches(".*[+-]\\d\\d:\\d\\d$")) ? dt : (dt + "Z");
        }

        double acqSec = 60.0;
        String rawAcq = extractSimpleXmlTag(xml, "AcquisitionTime");
        if (rawAcq != null && !rawAcq.isBlank()) {
            try {
                double parsed = Double.parseDouble(rawAcq.trim());
                if (parsed > 0.0) {
                    acqSec = parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        double neutrons = 0.0;
        String rawNeutrons = extractSimpleXmlTag(xml, "Neutrons");
        if (rawNeutrons != null && !rawNeutrons.isBlank()) {
            try {
                neutrons = Double.parseDouble(rawNeutrons.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String serial = extractSimpleXmlTag(xml, "SerialNumber");
        if (serial == null || serial.isBlank()) {
            serial = "Syclone";
        }

        // Syclone EnergyCalibrationCoefficients: E(ch) = C + B*ch + A*ch^2 -> N42 CoefficientValues: C B A
        String coeffValues = "0.0 3.0 0.0";
        Matcher calMatcher = RAD_SAMPLER_CAL_BLOCK.matcher(xml);
        if (calMatcher.find()) {
            String calBlock = calMatcher.group(1);
            String aStr = extractSimpleXmlTag(calBlock, "A");
            String bStr = extractSimpleXmlTag(calBlock, "B");
            String cStr = extractSimpleXmlTag(calBlock, "C");
            if (bStr != null && cStr != null) {
                coeffValues = cStr.trim() + " " + bStr.trim() + " " + (aStr != null ? aStr.trim() : "0.0");
            }
        }

        List<VendorNuclide> nuclides = new ArrayList<>();
        Matcher isoMatcher = RAD_SAMPLER_ISOTOPE_BLOCK.matcher(xml);
        while (isoMatcher.find()) {
            String block = isoMatcher.group(1);
            String isoName = extractSimpleXmlTag(block, "IsotopeName");
            String snr = extractSimpleXmlTag(block, "SNR");
            String type = extractSimpleXmlTag(block, "TYPE");
            if (isoName != null && !isoName.isBlank()) {
                nuclides.add(new VendorNuclide(isoName.trim(), type != null ? type.trim() : "", snr != null ? snr.trim() : ""));
            }
        }

        String durationIso = String.format(Locale.US, "PT%.3fS", acqSec);
        StringBuilder sb = new StringBuilder(channelData.length() + 2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<RadInstrumentData xmlns=\"http://physics.nist.gov/N42/2011/N42\">\n");
        sb.append("  <RadInstrumentInformation id=\"RadInst-1\">\n");
        sb.append("    <RadInstrumentManufacturerName>RadComm</RadInstrumentManufacturerName>\n");
        sb.append("    <RadInstrumentIdentifier>").append(escapeXml(serial.trim())).append("</RadInstrumentIdentifier>\n");
        sb.append("    <RadInstrumentModelName>Syclone</RadInstrumentModelName>\n");
        sb.append("    <RadInstrumentClassCode>Radionuclide Identifier</RadInstrumentClassCode>\n");
        sb.append("    <RadInstrumentVersion>\n");
        sb.append("      <RadInstrumentComponentName>Firmware</RadInstrumentComponentName>\n");
        sb.append("      <RadInstrumentComponentVersion>1.0</RadInstrumentComponentVersion>\n");
        sb.append("    </RadInstrumentVersion>\n");
        sb.append("  </RadInstrumentInformation>\n");
        sb.append("  <RadDetectorInformation id=\"GammaDet-1\">\n");
        sb.append("    <RadDetectorCategoryCode>Gamma</RadDetectorCategoryCode>\n");
        sb.append("    <RadDetectorKindCode>NaI</RadDetectorKindCode>\n");
        sb.append("  </RadDetectorInformation>\n");
        sb.append("  <RadDetectorInformation id=\"NeutronDet-1\">\n");
        sb.append("    <RadDetectorCategoryCode>Neutron</RadDetectorCategoryCode>\n");
        sb.append("    <RadDetectorKindCode>He3</RadDetectorKindCode>\n");
        sb.append("  </RadDetectorInformation>\n");
        sb.append("  <EnergyCalibration id=\"EnergyCal-1\">\n");
        sb.append("    <CoefficientValues>").append(escapeXml(coeffValues)).append("</CoefficientValues>\n");
        sb.append("  </EnergyCalibration>\n");
        sb.append("  <RadMeasurement id=\"Meas-1\">\n");
        sb.append("    <MeasurementClassCode>Foreground</MeasurementClassCode>\n");
        sb.append("    <StartDateTime>").append(escapeXml(startIso)).append("</StartDateTime>\n");
        sb.append("    <RealTimeDuration>").append(durationIso).append("</RealTimeDuration>\n");
        sb.append("    <Spectrum id=\"Spec-1\" radDetectorInformationReference=\"GammaDet-1\" energyCalibrationReference=\"EnergyCal-1\">\n");
        sb.append("      <LiveTimeDuration>").append(durationIso).append("</LiveTimeDuration>\n");
        sb.append("      <ChannelData>").append(channelData.trim()).append("</ChannelData>\n");
        sb.append("    </Spectrum>\n");
        sb.append("    <GrossCounts id=\"GrossGamma-1\" radDetectorInformationReference=\"GammaDet-1\">\n");
        sb.append("      <LiveTimeDuration>").append(durationIso).append("</LiveTimeDuration>\n");
        sb.append("      <CountData>").append(String.format(Locale.US, "%.1f", gammaSum)).append("</CountData>\n");
        sb.append("    </GrossCounts>\n");
        sb.append("    <GrossCounts id=\"GrossNeutron-1\" radDetectorInformationReference=\"NeutronDet-1\">\n");
        sb.append("      <LiveTimeDuration>").append(durationIso).append("</LiveTimeDuration>\n");
        sb.append("      <CountData>").append(String.format(Locale.US, "%.1f", neutrons)).append("</CountData>\n");
        sb.append("    </GrossCounts>\n");
        sb.append("  </RadMeasurement>\n");
        sb.append("</RadInstrumentData>\n");

        String built = sb.toString();
        if (!nuclides.isEmpty()) {
            built = injectVendorNuclidesIntoN42(built, nuclides);
        }
        return built;
    }

    private static String extractSimpleXmlTag(String xml, String tagName) {
        Matcher m = Pattern.compile("<" + Pattern.quote(tagName) + ">([\\s\\S]*?)</" + Pattern.quote(tagName) + ">").matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    private static int countRadMeasurements(String n42Xml) {
        if (n42Xml == null) {
            return 0;
        }
        int count = 0;
        Matcher m = RAD_MEASUREMENT_BLOCK.matcher(n42Xml);
        while (m.find()) {
            count++;
        }
        return count;
    }

    public static String trimMultiSampleN42(String n42Xml) {
        if (n42Xml == null || n42Xml.isBlank()) {
            return n42Xml;
        }
        Matcher m = RAD_MEASUREMENT_BLOCK.matcher(n42Xml);
        StringBuilder sb = new StringBuilder(n42Xml.length());
        while (m.find()) {
            String block = m.group(0);
            if (block.contains("<MeasurementClassCode>Foreground</MeasurementClassCode>")
                    || block.contains("<MeasurementClassCode>Background</MeasurementClassCode>")) {
                m.appendReplacement(sb, Matcher.quoteReplacement(block));
            } else {
                m.appendReplacement(sb, "");
            }
        }
        m.appendTail(sb);
        String cleaned = sb.toString();
        cleaned = cleaned.replaceAll("(?s)<RadMeasurementGroup\\b[^>]*/>", "");
        cleaned = cleaned.replaceAll("(?s)<RadMeasurementGroup\\b.*?</RadMeasurementGroup>", "");
        cleaned = cleaned.replaceAll("\\s+radMeasurementGroupReferences=\"[^\"]*\"", "");
        cleaned = cleaned.replaceAll("\\s+derivedDataReferences=\"[^\"]*\"", "");
        cleaned = cleaned.replaceAll("(?s)<DerivedData\\b[^>]*/>", "");
        cleaned = cleaned.replaceAll("(?s)<DerivedData\\b.*?</DerivedData>", "");
        return cleaned;
    }

    public static String inferFallbackDrf(String fileName, String rawText, String n42Xml) {
        String combined = ((fileName != null ? fileName : "") + "\n"
                + (rawText != null ? rawText.substring(0, Math.min(rawText.length(), 4000)) : "") + "\n"
                + (n42Xml != null ? n42Xml.substring(0, Math.min(n42Xml.length(), 4000)) : ""))
                .toLowerCase(Locale.ROOT);

        if (combined.contains("gr-1") || combined.contains("gr1") || combined.contains("kromek")) {
            return "Kromek-GR1-CZT";
        }
        if (combined.contains("r300")) {
            return "IdentiFINDER-R300";
        }
        if (combined.contains("srm50lab") || combined.contains("srm-") || combined.contains("labr")) {
            return "IdentiFINDER-LaBr3";
        }
        if (combined.contains("r225")) {
            return "IdentiFINDER-NG";
        }
        if (combined.contains("symetrica") || combined.contains("verifinder")
                || combined.contains("sn12n") || combined.contains("sn23n")
                || combined.contains("s100") || combined.contains("s200")) {
            return "Verifinder";
        }
        if (combined.contains("sniper") || combined.contains("csi")) {
            return "1x1/CsI Side";
        }
        return "2x2/NaI";
    }

    private static String escapeXml(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private boolean isRadDataQrCode(byte[] data) {
        return isRadDataQrCodeBytes(data);
    }

    private static boolean isRadDataQrCodeBytes(byte[] data) {
        if (data == null || data.length < RADDATA_PREFIX.length()) {
            return false;
        }
        String prefix = new String(data, 0, RADDATA_PREFIX.length(), StandardCharsets.UTF_8);
        return RADDATA_PREFIX.equals(prefix);
    }
}
