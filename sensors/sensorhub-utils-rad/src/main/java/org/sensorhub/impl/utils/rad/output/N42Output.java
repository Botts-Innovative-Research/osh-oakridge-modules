package org.sensorhub.impl.utils.rad.output;

import com.botts.impl.utils.n42.*;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockMixed;
import org.vast.data.TextEncodingImpl;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import jakarta.xml.bind.JAXBElement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class N42Output<T extends IDataProducerModule<?>> extends AbstractSensorOutput<T> {

    private static final RADHelper radHelper = new RADHelper();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(N42Output.class);

    public static final String SENSOR_OUTPUT_NAME = "n42Report";
    public static final String SENSOR_OUTPUT_LABEL = "N42 Report";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    Count foregroundCount;
    DataArray foregroundReports;
    Count backgroundCount;
    DataArray backgroundReports;
    Count alarmCount;
    DataArray alarmReports;

    public N42Output(T parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
        init();
    }

    public void init() {

        // Foreground/Background Report
        var samplingTime = radHelper.createPrecisionTimeStamp();
        var duration = radHelper.createDuration();
        var linearSpectrumCount = radHelper.createArraySize("linearSpectrumCount", "linearSpectrumCount", "Linear Spectrum Count");
        var linearSpectrumArray = radHelper.createLinearSpectrum();
        var compressedSpectrumCount = radHelper.createArraySize("compressedSpectrumCount", "compressedSpectrumCount", "Compressed Spectrum Count");
        var compressedSpectrumArray = radHelper.createCompressedSpectrum();
        var gammaGrossCount = radHelper.createGammaGrossCount();
        var neutronGrossCount = radHelper.createNeutronGrossCount();
        var doseRate = radHelper.createDoseUSVh();

        // Alarm Report
        var alarmCategory = radHelper.createAlarmCatCode();
        var remark = radHelper.createRemark();
        var measurementClass = radHelper.createMeasurementClassCode();
        var alarmDescription = radHelper.createAlarmDescription();

        var fileName = radHelper.createText().name("fileName").description("File Name").label("File Name").build();
        var occupancyObsId = radHelper.createText().name("occupancyObsId").description("Encoded ID of the occupancy observation that produced or contains this N42 record").label("Occupancy Obs ID").build();

        var foregroundStruct = radHelper.createRecord()
                .name("foregroundReport")
                .label("Foreground Report")
                .definition(RADHelper.getRadUri("ForegroundReport"))
                .addField(samplingTime.getName(), samplingTime.copy())
                .addField(duration.getName(), duration.copy())
                .addField(linearSpectrumCount.getName(), linearSpectrumCount.copy())
                .addField(linearSpectrumArray.getName(), linearSpectrumArray.copy())
                .addField(compressedSpectrumCount.getName(), compressedSpectrumCount.copy())
                .addField(compressedSpectrumArray.getName(), compressedSpectrumArray.copy())
                .addField(gammaGrossCount.getName(), gammaGrossCount.copy())
                .addField(neutronGrossCount.getName(), neutronGrossCount.copy())
                .addField(doseRate.getName(), doseRate.copy())
                .build();

        var backgroundStruct = radHelper.createRecord()
                .name("backgroundReport")
                .label("Background Report")
                .definition(RADHelper.getRadUri("BackgroundReport"))
                .addField(samplingTime.getName(), samplingTime.copy())
                .addField(duration.getName(), duration.copy())
                .addField(linearSpectrumCount.getName(), linearSpectrumCount.copy())
                .addField(linearSpectrumArray.getName(), linearSpectrumArray.copy())
                .addField(compressedSpectrumCount.getName(), compressedSpectrumCount.copy())
                .addField(compressedSpectrumArray.getName(), compressedSpectrumArray.copy())
                .addField(gammaGrossCount.getName(), gammaGrossCount.copy())
                .addField(neutronGrossCount.getName(), neutronGrossCount.copy())
                .build();

        var alarmStruct = radHelper.createRecord()
                .name("alarmReport")
                .label("Alarm Report")
                .definition(RADHelper.getRadUri("AlarmOutput"))
                .addField(samplingTime.getName(), samplingTime.copy())
                //.addField(duration.getName(), duration)
                //.addField(remark.getName(), remark)
                //.addField(measurementClass.getName(), measurementClass)
                .addField(alarmCategory.getName(), alarmCategory.copy())
                .addField(alarmDescription.getName(), alarmDescription.copy())
                .build();

        foregroundReports = radHelper.createArray()
                .name("foregroundReports")
                .label("Foreground Reports")
                .definition(RADHelper.getRadUri("ForegroundReports"))
                .withVariableSize("foregroundReportCount")
                .withElement(foregroundStruct.getName(), foregroundStruct.copy())
                .build();

        foregroundCount = radHelper.createArraySize("foregroundReportCount", "foregroundReportCount", "Foreground Report Count");

        backgroundReports = radHelper.createArray()
                .name("backgroundReports")
                .label("Background Reports")
                .definition(RADHelper.getRadUri("BackgroundReports"))
                .withVariableSize("backgroundReportCount")
                .withElement(backgroundStruct.getName(), backgroundStruct.copy())
                .build();

        backgroundCount = radHelper.createArraySize("backgroundReportCount", "backgroundReportCount", "Background Report Count");

        alarmReports = radHelper.createArray()
                .name("alarmReports")
                .label("Alarm Reports")
                .definition(RADHelper.getRadUri("AlarmReports"))
                .withVariableSize("alarmReportCount")
                .withElement(alarmStruct.getName(), alarmStruct.copy())
                .build();

        alarmCount = radHelper.createArraySize("alarmReportCount", "alarmReportCount", "Alarm Report Count");

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("N42Report"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(fileName.getName(), fileName)
                .addField(occupancyObsId.getName(), occupancyObsId)
                .addField(foregroundCount.getName(), foregroundCount)
                .addField(foregroundReports.getName(), foregroundReports)
                .addField(backgroundCount.getName(), backgroundCount)
                .addField(backgroundReports.getName(), backgroundReports)
                .addField(alarmCount.getName(), alarmCount)
                .addField(alarmReports.getName(), alarmReports)
                .build();

        dataStruct.renewDataBlock();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }


    /**
     * @deprecated prefer {@link #onNewMessage(String, String, String)} so that the
     * resulting N42 observation is correlated with the originating occupancy.
     */
    @Deprecated
    public synchronized void onNewMessage(String n42Message, String fileName) {
        onNewMessage(n42Message, fileName, null);
    }

    public synchronized void onNewMessage(String n42Message, String fileName, String occupancyObsId) {
        parseN42Message(n42Message, fileName, occupancyObsId);
        latestRecord = dataStruct.getData().clone();
        eventHandler.publish(new DataEvent(latestRecordTime, N42Output.this, latestRecord));
    }


    private record ParsedMeasurement(
            String classCode,
            long startDateTimeMs,
            double realTimeDurationSec,
            List<Double> linearSpectrum,
            List<Double> compressedSpectrum,
            double gammaGrossCount,
            double neutronGrossCount,
            double doseRate,
            double lat,
            double lon,
            double elv
    ) {}

    private record ParsedAlarm(
            long startDateTimeMs,
            String categoryCode,
            String description
    ) {}

    /**
     * Normalizes vendor N42 XML quirks before JAXB unmarshalling:
     * - Strips leading UTF-8 BOM
     * - Removes undeclared InterSpec extension elements/attributes emitted by SpecUtils
     * - Normalizes FLIR R225 singular {@code <NuclideAnalysisResult>} tags to N42-2012 {@code <NuclideAnalysisResults>}
     */
    public static String sanitizeN42Xml(String n42Xml) {
        if (n42Xml == null || n42Xml.isEmpty()) {
            return n42Xml;
        }
        String cleaned = n42Xml;
        if (!cleaned.isEmpty() && cleaned.charAt(0) == '\uFEFF') {
            cleaned = cleaned.substring(1);
        }
        cleaned = cleaned.replaceAll("<InterSpec:[^>]*?/>", "");
        String prev;
        int guard = 0;
        do {
            prev = cleaned;
            cleaned = cleaned.replaceAll("(?s)<(InterSpec:[A-Za-z0-9_.-]+)(?:\\s[^>]*)?>((?!<InterSpec:).)*?</\\1>", "");
        } while (!cleaned.equals(prev) && ++guard < 16);
        cleaned = cleaned.replaceAll("</?InterSpec:[^>]*>", "");
        cleaned = cleaned.replaceAll("\\s+(?:xmlns:)?InterSpec:[A-Za-z0-9_.-]*=\"[^\"]*\"", "");
        cleaned = cleaned.replaceAll("\\s+xmlns:InterSpec=\"[^\"]*\"", "");
        cleaned = cleaned.replaceAll("<(/?)NuclideAnalysisResult(\\s|>)", "<$1NuclideAnalysisResults$2");
        return cleaned;
    }

    /**
     * Expands N42-2012 {@code CountedZeroes} run-length channel encoding ({@code 0 N -> N zeroes})
     * so downstream spectrum charts and gross-count sums operate on linear channel bins.
     */
    public static List<Double> expandChannelData(ChannelDataType channelData) {
        if (channelData == null || channelData.getValue() == null || channelData.getValue().isEmpty()) {
            return Collections.emptyList();
        }
        List<Double> raw = channelData.getValue();
        if (channelData.getCompressionCode() == CompressionCodeSimpleType.COUNTED_ZEROES) {
            List<Double> expanded = new ArrayList<>(Math.max(raw.size(), 1024));
            for (int i = 0; i < raw.size(); i++) {
                Double v = raw.get(i);
                double val = v != null ? v : 0.0;
                if (val == 0.0 && i + 1 < raw.size()) {
                    Double runObj = raw.get(++i);
                    int runLen = runObj != null ? Math.max(0, Math.min(65536, (int) Math.round(runObj))) : 1;
                    for (int z = 0; z < runLen; z++) {
                        expanded.add(0.0);
                    }
                } else {
                    expanded.add(val);
                }
            }
            return expanded;
        }
        List<Double> copy = new ArrayList<>(raw.size());
        for (Double v : raw) {
            copy.add(v != null ? v : 0.0);
        }
        return copy;
    }

    protected void parseN42Message(String n42Message, String fileName, String occupancyObsId) {

        foregroundReports.clearData();
        backgroundReports.clearData();
        alarmReports.clearData();

        String sanitizedXml = sanitizeN42Xml(n42Message);
        var message = radHelper.getRadInstrumentData(sanitizedXml);
        long nowMs = Instant.now().toEpochMilli();

        List<RadMeasurementType> rawFg = new ArrayList<>();
        List<RadMeasurementType> rawBg = new ArrayList<>();
        List<RadMeasurementType> rawUnspecifiedWithSpectrum = new ArrayList<>();
        List<AnalysisResultsType> rawAnalyses = new ArrayList<>();

        if (message != null && message.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration() != null) {
            for (var jaxbElement : message.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration()) {
                if (jaxbElement == null || jaxbElement.getValue() == null) {
                    continue;
                }
                Class<?> jaxbType = jaxbElement.getDeclaredType();
                if (jaxbType == RadMeasurementType.class) {
                    RadMeasurementType rm = (RadMeasurementType) jaxbElement.getValue();
                    String mc = rm.getMeasurementClassCode() != null ? rm.getMeasurementClassCode().value() : "NotSpecified";
                    if ("Foreground".equalsIgnoreCase(mc)) {
                        rawFg.add(rm);
                    } else if ("Background".equalsIgnoreCase(mc)) {
                        rawBg.add(rm);
                    } else if (!"Calibration".equalsIgnoreCase(mc)
                            && !"IntrinsicActivity".equalsIgnoreCase(mc)
                            && hasNonEmptySpectrum(rm)) {
                        rawUnspecifiedWithSpectrum.add(rm);
                    }
                } else if (jaxbType == AnalysisResultsType.class) {
                    rawAnalyses.add((AnalysisResultsType) jaxbElement.getValue());
                }
            }
        }

        if (rawFg.isEmpty() && !rawUnspecifiedWithSpectrum.isEmpty()) {
            rawFg.addAll(rawUnspecifiedWithSpectrum);
        }

        List<ParsedMeasurement> parsedForeground = consolidateMeasurements(rawFg, "FOREGROUND", nowMs);
        List<ParsedMeasurement> parsedBackground = consolidateMeasurements(rawBg, "BACKGROUND", nowMs);

        long fallbackAlarmTimeMs = !parsedForeground.isEmpty()
                ? parsedForeground.get(0).startDateTimeMs()
                : (!parsedBackground.isEmpty() ? parsedBackground.get(0).startDateTimeMs() : nowMs);
        List<ParsedAlarm> parsedAlarms = extractAlarmAndNuclideReports(rawAnalyses, fallbackAlarmTimeMs);

        foregroundCount.setValue(parsedForeground.size());
        foregroundReports.updateSize();
        foregroundReports.setElementCount(foregroundCount);
        foregroundReports.assignNewDataBlock();

        backgroundCount.setValue(parsedBackground.size());
        backgroundReports.updateSize();
        backgroundReports.setElementCount(backgroundCount);
        backgroundReports.assignNewDataBlock();

        alarmCount.setValue(parsedAlarms.size());
        alarmReports.updateSize();
        alarmReports.setElementCount(alarmCount);
        alarmReports.assignNewDataBlock();

        for (int i = 0; i < parsedBackground.size(); i++) {
            ParsedMeasurement pm = parsedBackground.get(i);
            var reportComponent = backgroundReports.getComponent(i);
            populateBackgroundReport(
                    reportComponent,
                    pm.classCode(),
                    pm.startDateTimeMs(),
                    pm.realTimeDurationSec(),
                    pm.linearSpectrum(),
                    pm.compressedSpectrum(),
                    pm.gammaGrossCount(),
                    pm.neutronGrossCount()
            );
        }

        for (int i = 0; i < parsedForeground.size(); i++) {
            ParsedMeasurement pm = parsedForeground.get(i);
            var reportComponent = foregroundReports.getComponent(i);
            populateForegroundReport(
                    reportComponent,
                    pm.classCode(),
                    pm.startDateTimeMs(),
                    pm.realTimeDurationSec(),
                    pm.linearSpectrum(),
                    pm.compressedSpectrum(),
                    pm.gammaGrossCount(),
                    pm.neutronGrossCount(),
                    pm.doseRate(),
                    pm.lat(),
                    pm.lon(),
                    pm.elv()
            );
        }

        for (int i = 0; i < parsedAlarms.size(); i++) {
            ParsedAlarm pa = parsedAlarms.get(i);
            var reportComponent = alarmReports.getComponent(i);
            populateAlarmReport(
                    reportComponent,
                    pa.startDateTimeMs(),
                    pa.categoryCode(),
                    pa.description()
            );
        }

        DataBlockMixed dataBlock = (DataBlockMixed) dataStruct.getData();

        latestRecordTime = System.currentTimeMillis() / 1000;
        AbstractDataBlock[] rootBlocks = dataBlock.getUnderlyingObject();
        rootBlocks[0].setLongValue(0, latestRecordTime);
        rootBlocks[1].setStringValue(0, fileName != null ? fileName : "");
        rootBlocks[2].setStringValue(0, occupancyObsId != null ? occupancyObsId : "");
        rootBlocks[3].setIntValue(0, foregroundCount.getValue());
        dataBlock.setBlock(4, (AbstractDataBlock) foregroundReports.getData());
        rootBlocks[5].setIntValue(0, backgroundCount.getValue());
        dataBlock.setBlock(6, (AbstractDataBlock) backgroundReports.getData());
        rootBlocks[7].setIntValue(0, alarmCount.getValue());
        dataBlock.setBlock(8, (AbstractDataBlock) alarmReports.getData());

        dataBlock.updateAtomCount();
    }

    private static boolean hasNonEmptySpectrum(RadMeasurementType rm) {
        if (rm == null || rm.getSpectrum() == null || rm.getSpectrum().isEmpty()) {
            return false;
        }
        for (SpectrumType sp : rm.getSpectrum()) {
            if (sp != null && sp.getChannelData() != null && !sp.getChannelData().getValue().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts N42 {@link RadMeasurementType} blocks into {@link ParsedMeasurement} items while merging
     * sibling neutron/gamma gross-count or dose-rate blocks onto spectrum-bearing blocks (e.g. Symetrica S100/S200).
     */
    private static List<ParsedMeasurement> consolidateMeasurements(
            List<RadMeasurementType> measurements,
            String defaultClassName,
            long fallbackTimeMs
    ) {
        if (measurements == null || measurements.isEmpty()) {
            return Collections.emptyList();
        }

        boolean anyHasSpectrum = measurements.stream().anyMatch(N42Output::hasNonEmptySpectrum);
        double siblingGammaCounts = Double.NaN;
        double siblingNeutronCounts = Double.NaN;
        double siblingDoseRate = Double.NaN;

        if (anyHasSpectrum) {
            for (RadMeasurementType rm : measurements) {
                if (!hasNonEmptySpectrum(rm)) {
                    double[] gn = extractGammaAndNeutronGrossCounts(rm, Collections.emptyList());
                    if (!Double.isNaN(gn[0]) && Double.isNaN(siblingGammaCounts)) {
                        siblingGammaCounts = gn[0];
                    }
                    if (!Double.isNaN(gn[1]) && Double.isNaN(siblingNeutronCounts)) {
                        siblingNeutronCounts = gn[1];
                    }
                    double dr = extractDoseRate(rm);
                    if (!Double.isNaN(dr) && Double.isNaN(siblingDoseRate)) {
                        siblingDoseRate = dr;
                    }
                }
            }
        }

        List<ParsedMeasurement> result = new ArrayList<>();
        for (RadMeasurementType rm : measurements) {
            if (anyHasSpectrum && !hasNonEmptySpectrum(rm)) {
                continue;
            }

            List<Double> linSpec = Collections.emptyList();
            List<Double> cmpSpec = Collections.emptyList();
            if (rm.getSpectrum() != null && !rm.getSpectrum().isEmpty()) {
                if (rm.getSpectrum().get(0) != null) {
                    linSpec = expandChannelData(rm.getSpectrum().get(0).getChannelData());
                }
                if (rm.getSpectrum().size() > 1 && rm.getSpectrum().get(1) != null) {
                    cmpSpec = expandChannelData(rm.getSpectrum().get(1).getChannelData());
                }
            }

            double[] gn = extractGammaAndNeutronGrossCounts(rm, linSpec);
            double gammaGross = !Double.isNaN(gn[0]) ? gn[0] : siblingGammaCounts;
            double neutronGross = !Double.isNaN(gn[1]) ? gn[1] : siblingNeutronCounts;
            double doseRate = extractDoseRate(rm);
            if (Double.isNaN(doseRate)) {
                doseRate = siblingDoseRate;
            }

            long startMs = fallbackTimeMs;
            if (rm.getStartDateTime() != null) {
                try {
                    startMs = rm.getStartDateTime().toGregorianCalendar().getTimeInMillis();
                } catch (Exception ignored) {
                }
            }

            double durationSec = durationToDouble(rm.getRealTimeDuration());
            if ((Double.isNaN(durationSec) || durationSec <= 0.0)
                    && rm.getSpectrum() != null
                    && !rm.getSpectrum().isEmpty()
                    && rm.getSpectrum().get(0) != null) {
                durationSec = durationToDouble(rm.getSpectrum().get(0).getLiveTimeDuration());
            }
            if (Double.isNaN(durationSec)) {
                durationSec = 0.0;
            }

            double lat = 0.0;
            double lon = 0.0;
            double elv = 0.0;
            try {
                if (rm.getRadInstrumentState() != null
                        && rm.getRadInstrumentState().getStateVector() != null
                        && rm.getRadInstrumentState().getStateVector().getGeographicPoint() != null
                        && rm.getRadInstrumentState().getStateVector().getGeographicPoint().getLongitudeValue() != null) {
                    var gp = rm.getRadInstrumentState().getStateVector().getGeographicPoint();
                    if (gp.getLatitudeValue() != null && gp.getLatitudeValue().getValue() != null) {
                        lat = gp.getLatitudeValue().getValue().doubleValue();
                    }
                    if (gp.getLongitudeValue() != null && gp.getLongitudeValue().getValue() != null) {
                        lon = gp.getLongitudeValue().getValue().doubleValue();
                    }
                    if (gp.getElevationValue() != null) {
                        elv = gp.getElevationValue().doubleValue();
                    }
                }
            } catch (Exception ignored) {
            }

            String classCode = rm.getMeasurementClassCode() != null
                    ? rm.getMeasurementClassCode().name()
                    : defaultClassName;

            result.add(new ParsedMeasurement(
                    classCode,
                    startMs,
                    durationSec,
                    linSpec,
                    cmpSpec,
                    gammaGross,
                    neutronGross,
                    doseRate,
                    lat,
                    lon,
                    elv
            ));
        }

        return result;
    }

    private static double[] extractGammaAndNeutronGrossCounts(RadMeasurementType rm, List<Double> linSpec) {
        double gamma = Double.NaN;
        double neutron = Double.NaN;
        boolean foundTotalNeutron = false;

        if (rm != null && rm.getGrossCounts() != null && !rm.getGrossCounts().isEmpty()) {
            List<GrossCountsType> gcs = rm.getGrossCounts();
            List<Double> unlabeled = new ArrayList<>();

            for (GrossCountsType gc : gcs) {
                if (gc == null || gc.getCountData() == null || gc.getCountData().isEmpty() || gc.getCountData().get(0) == null) {
                    continue;
                }
                double val = gc.getCountData().get(0);
                String hint = buildGrossCountHint(gc);
                if (hint.contains("neutron")) {
                    boolean isMinOrMax = hint.contains("minimum") || hint.contains("maximum");
                    if (!isMinOrMax) {
                        neutron = val;
                        foundTotalNeutron = true;
                    } else if (!foundTotalNeutron && Double.isNaN(neutron)) {
                        neutron = val;
                    }
                } else if (hint.contains("gamma")) {
                    if (Double.isNaN(gamma)) {
                        gamma = val;
                    }
                } else {
                    unlabeled.add(val);
                }
            }

            if (Double.isNaN(gamma) && !unlabeled.isEmpty()) {
                gamma = unlabeled.get(0);
            }
            if (Double.isNaN(neutron) && unlabeled.size() > 1) {
                neutron = unlabeled.get(1);
            }
        }

        if (Double.isNaN(gamma) && linSpec != null && !linSpec.isEmpty()) {
            double sum = 0.0;
            for (Double c : linSpec) {
                if (c != null && !Double.isNaN(c)) {
                    sum += c;
                }
            }
            gamma = sum;
        }

        return new double[]{gamma, neutron};
    }

    private static String buildGrossCountHint(GrossCountsType gc) {
        StringBuilder sb = new StringBuilder();
        if (gc.getId() != null) {
            sb.append(gc.getId()).append(' ');
        }
        Object detRef = gc.getRadDetectorInformationReference();
        if (detRef instanceof RadDetectorInformationType detInfo) {
            if (detInfo.getId() != null) {
                sb.append(detInfo.getId()).append(' ');
            }
            if (detInfo.getRadDetectorName() != null) {
                sb.append(detInfo.getRadDetectorName()).append(' ');
            }
            if (detInfo.getRadDetectorCategoryCode() != null) {
                sb.append(detInfo.getRadDetectorCategoryCode().value()).append(' ');
            }
        } else if (detRef != null) {
            sb.append(detRef.toString());
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static double extractDoseRate(RadMeasurementType rm) {
        if (rm == null || rm.getDoseRate() == null || rm.getDoseRate().isEmpty()) {
            return Double.NaN;
        }
        for (DoseRateType dr : rm.getDoseRate()) {
            if (dr != null && dr.getDoseRateValue() != null) {
                return dr.getDoseRateValue().getValue();
            }
        }
        return Double.NaN;
    }

    private static List<ParsedAlarm> extractAlarmAndNuclideReports(
            List<AnalysisResultsType> analyses,
            long fallbackTimeMs
    ) {
        if (analyses == null || analyses.isEmpty()) {
            return Collections.emptyList();
        }

        List<ParsedAlarm> out = new ArrayList<>();
        Set<String> emittedNuclideKeys = new LinkedHashSet<>();
        List<ParsedAlarm> pendingRadAlarms = new ArrayList<>();

        for (AnalysisResultsType ar : analyses) {
            if (ar == null) {
                continue;
            }
            long analysisTimeMs = fallbackTimeMs;
            if (ar.getAnalysisStartDateTime() != null) {
                try {
                    long candidate = ar.getAnalysisStartDateTime().toGregorianCalendar().getTimeInMillis();
                    if (candidate > 86_400_000L) {
                        analysisTimeMs = candidate;
                    }
                } catch (Exception ignored) {
                }
            }

            if (ar.getNuclideAnalysisResults() != null && ar.getNuclideAnalysisResults().getNuclide() != null) {
                for (NuclideType nuclide : ar.getNuclideAnalysisResults().getNuclide()) {
                    if (nuclide == null || nuclide.getNuclideName() == null) {
                        continue;
                    }
                    String rawName = nuclide.getNuclideName().trim();
                    if (rawName.isEmpty()
                            || "none".equalsIgnoreCase(rawName)
                            || "background".equalsIgnoreCase(rawName)
                            || "nothing".equalsIgnoreCase(rawName)) {
                        continue;
                    }

                    String category = normalizeDeviceNuclideCategory(nuclide.getNuclideCategoryDescription());
                    String confidence = extractNuclideConfidenceText(nuclide);

                    StringBuilder desc = new StringBuilder("Isotope: ").append(rawName);
                    List<String> metaParts = new ArrayList<>(2);
                    if (!category.isEmpty()) {
                        metaParts.add(category);
                    }
                    if (!confidence.isEmpty()) {
                        metaParts.add("Confidence: " + confidence);
                    }
                    if (!metaParts.isEmpty()) {
                        desc.append(" (").append(String.join(", ", metaParts)).append(")");
                    }

                    String dedupKey = rawName.replaceAll("[\\s_-]+", "").toLowerCase(Locale.ROOT);
                    if (emittedNuclideKeys.add(dedupKey)) {
                        out.add(new ParsedAlarm(analysisTimeMs, "Isotope", desc.toString()));
                    }
                }
            }

            if (ar.getRadAlarm() != null) {
                for (RadAlarmType radAlarm : ar.getRadAlarm()) {
                    if (radAlarm == null) {
                        continue;
                    }
                    cleanRadAlarm(radAlarm);
                    long alarmMs = analysisTimeMs;
                    if (radAlarm.getRadAlarmDateTime() != null) {
                        try {
                            alarmMs = radAlarm.getRadAlarmDateTime().toGregorianCalendar().getTimeInMillis();
                        } catch (Exception ignored) {
                        }
                    }
                    String cat = radAlarm.getRadAlarmCategoryCode() != null
                            ? radAlarm.getRadAlarmCategoryCode().value()
                            : "Other";
                    String desc = radAlarm.getRadAlarmDescription() != null
                            ? radAlarm.getRadAlarmDescription().trim()
                            : "";
                    pendingRadAlarms.add(new ParsedAlarm(alarmMs, cat, desc));
                }
            }
        }

        for (ParsedAlarm ra : pendingRadAlarms) {
            if ("Isotope".equalsIgnoreCase(ra.categoryCode()) && ra.description().isEmpty() && !emittedNuclideKeys.isEmpty()) {
                // Skip blank Isotope alarm placeholders (e.g. SRM N42) when explicit <Nuclide> entries were emitted
                continue;
            }
            if ("Isotope".equalsIgnoreCase(ra.categoryCode()) && !ra.description().isEmpty()) {
                int colon = ra.description().lastIndexOf(':');
                if (colon >= 0 && colon + 1 < ra.description().length()) {
                    String trailingNuclide = ra.description().substring(colon + 1).trim()
                            .replaceAll("[\\s_-]+", "")
                            .toLowerCase(Locale.ROOT);
                    if (emittedNuclideKeys.contains(trailingNuclide)) {
                        continue;
                    }
                }
            }
            out.add(ra);
        }

        return out;
    }

    private static String normalizeDeviceNuclideCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return "";
        }
        String trimmed = rawCategory.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (upper.contains("USAGE:MED") || "MED".equals(upper) || upper.contains("MEDICAL")) {
            return "Medical";
        }
        if (upper.contains("USAGE:IND") || "IND".equals(upper) || upper.contains("INDUSTRIAL")) {
            return "Industrial";
        }
        if (upper.contains("USAGE:SNM") || "SNM".equals(upper) || upper.contains("NUCLEAR") || upper.contains("SPECIAL")) {
            return "SNM";
        }
        if (upper.contains("USAGE:NORM") || "NORM".equals(upper) || upper.contains("NATURAL")) {
            return "NORM";
        }
        return trimmed;
    }

    private static String extractNuclideConfidenceText(NuclideType nuclide) {
        if (nuclide.getNuclideIdentificationConfidence() == null || nuclide.getNuclideIdentificationConfidence().isEmpty()) {
            return "";
        }
        for (JAXBElement<?> elem : nuclide.getNuclideIdentificationConfidence()) {
            if (elem == null || elem.getValue() == null) {
                continue;
            }
            String valStr = elem.getValue().toString().trim();
            if (!valStr.isEmpty()) {
                return valStr;
            }
        }
        return "";
    }

    private static void cleanRadAlarm(RadAlarmType radAlarmType) {
        if (radAlarmType.getRadAlarmDateTime() == null) {
            try {
                var gregorianCalendar = GregorianCalendar.from(ZonedDateTime.now(ZoneOffset.UTC));
                var xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
                radAlarmType.setRadAlarmDateTime(xmlGregorianCalendar);
            } catch (DatatypeConfigurationException e) {
                logger.warn("Failed to set RadAlarmDateTime", e);
            }
        }
        if (radAlarmType.getRadAlarmCategoryCode() == null) {
            radAlarmType.setRadAlarmCategoryCode(RadAlarmCategoryCodeSimpleType.OTHER); // Stand-in for unknown category
        }
        if (radAlarmType.getRadAlarmDescription() == null) {
            radAlarmType.setRadAlarmDescription("");
        }
    }

    private void populateBackgroundReport(DataComponent backgroundComponent, String classCode, Long startDateTime, Double realTimeDuration, List<Double> linEnCalSpectrum, List<Double> cmpEnCalSpectrum, Double gammaGrossCount, Double neutronGrossCount) {
        int index = 0;
        var dataBlock = backgroundComponent.getData();
        dataBlock.setLongValue(index++, startDateTime / 1000);
        dataBlock.setDoubleValue(index++, (realTimeDuration == null || Double.isNaN(realTimeDuration)) ? 0.0 : realTimeDuration);

        // -- Linear Spectrum --
        int linearSpectrumSize = linEnCalSpectrum.size();

        dataBlock.setIntValue(index++, linearSpectrumSize);
        var linearSpectrumArray = ((DataArrayImpl)backgroundComponent.getComponent("linearSpectrum"));
        linearSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < linearSpectrumSize; i++) {
            Double v = linEnCalSpectrum.get(i);
            dataBlock.setDoubleValue(index++, (v == null || Double.isNaN(v)) ? 0.0 : v);
        }

        // -- Compressed Spectrum --
        int compressedSpectrumSize = cmpEnCalSpectrum.size();

        dataBlock.setIntValue(index++, compressedSpectrumSize);
        var compressedSpectrumArray = ((DataArrayImpl)backgroundComponent.getComponent("compressedSpectrum"));
        compressedSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < compressedSpectrumSize; i++) {
            Double v = cmpEnCalSpectrum.get(i);
            dataBlock.setDoubleValue(index++, (v == null || Double.isNaN(v)) ? 0.0 : v);
        }

        dataBlock.setDoubleValue(index++, (gammaGrossCount == null || Double.isNaN(gammaGrossCount)) ? 0.0 : gammaGrossCount);
        dataBlock.setDoubleValue(index++, (neutronGrossCount == null || Double.isNaN(neutronGrossCount)) ? 0.0 : neutronGrossCount);
    }

    private void populateForegroundReport(DataComponent foregroundComponent, String classCode, Long startDateTime, Double realTimeDuration, List<Double> linEnCalSpectrum, List<Double> cmpEnCalSpectrum, Double gammaGrossCount, Double neutronGrossCount, Double doseRate, Double lat, Double lon, Double alt) {
        int index = 0;
        var dataBlock = foregroundComponent.getData();
        dataBlock.setLongValue(index++, startDateTime / 1000);
        dataBlock.setDoubleValue(index++, (realTimeDuration == null || Double.isNaN(realTimeDuration)) ? 0.0 : realTimeDuration);

        // -- Linear Spectrum --
        int linearSpectrumSize = linEnCalSpectrum.size();

        dataBlock.setIntValue(index++, linearSpectrumSize);
        var linearSpectrumArray = ((DataArrayImpl)foregroundComponent.getComponent("linearSpectrum"));
        linearSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < linearSpectrumSize; i++) {
            Double v = linEnCalSpectrum.get(i);
            dataBlock.setDoubleValue(index++, (v == null || Double.isNaN(v)) ? 0.0 : v);
        }

        // -- Compressed Spectrum --
        int compressedSpectrumSize = cmpEnCalSpectrum.size();

        dataBlock.setIntValue(index++, compressedSpectrumSize);
        var compressedSpectrumArray = ((DataArrayImpl)foregroundComponent.getComponent("compressedSpectrum"));
        compressedSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < compressedSpectrumSize; i++) {
            Double v = cmpEnCalSpectrum.get(i);
            dataBlock.setDoubleValue(index++, (v == null || Double.isNaN(v)) ? 0.0 : v);
        }

        dataBlock.setDoubleValue(index++, (gammaGrossCount == null || Double.isNaN(gammaGrossCount)) ? 0.0 : gammaGrossCount);
        dataBlock.setDoubleValue(index++, (neutronGrossCount == null || Double.isNaN(neutronGrossCount)) ? 0.0 : neutronGrossCount);
        dataBlock.setDoubleValue(index++, (doseRate == null || Double.isNaN(doseRate)) ? 0.0 : doseRate);
    }

    private void populateAlarmReport(DataComponent alarmComponent, Long startDateTime, String categoryCode, String description) {
        int index = 0;
        var dataBlock = alarmComponent.getData();
        dataBlock.setLongValue(index++, startDateTime / 1000);
        dataBlock.setStringValue(index++, categoryCode);
        dataBlock.setStringValue(index++, description);
    }

    private static double durationToDouble(Duration duration) {
        if (duration == null) {
            return 0.0;
        }
        try {
            return duration.getTimeInMillis(new java.util.Date(0L)) / 1000.0;
        } catch (Exception e) {
            return duration.multiply(1000).getSeconds() / 1000.0;
        }
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
}