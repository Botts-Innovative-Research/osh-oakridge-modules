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

import javax.xml.datatype.Duration;
import java.util.List;

public class N42Output<T extends IDataProducerModule<?>> extends AbstractSensorOutput<T> {

    private static final RADHelper radHelper = new RADHelper();

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

        var foregroundStruct = radHelper.createRecord()
                .name("Foreground Report")
                .label("Foreground Report")
                .definition(RADHelper.getRadUri("ForegroundReport"))
                .addField(samplingTime.getName(), samplingTime.copy())
                .addField(duration.getName(), duration)
                .addField(linearSpectrumCount.getName(), linearSpectrumCount)
                .addField(linearSpectrumArray.getName(), linearSpectrumArray)
                .addField(compressedSpectrumCount.getName(), compressedSpectrumCount)
                .addField(compressedSpectrumArray.getName(), compressedSpectrumArray)
                .addField(gammaGrossCount.getName(), gammaGrossCount)
                .addField(neutronGrossCount.getName(), neutronGrossCount)
                .addField(doseRate.getName(), doseRate)
                .build();

        var backgroundStruct = radHelper.createRecord()
                .name("Background Report")
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
                .name("Alarm Report")
                .label("Alarm Report")
                .definition(RADHelper.getRadUri("AlarmOutput"))
                .addField(samplingTime.getName(), samplingTime.copy())
                //.addField(duration.getName(), duration)
                //.addField(remark.getName(), remark)
                //.addField(measurementClass.getName(), measurementClass)
                .addField(alarmCategory.getName(), alarmCategory)
                .addField(alarmDescription.getName(), alarmDescription)
                .build();

        foregroundReports = radHelper.createArray()
                .name("Foreground Reports")
                .label("Foreground Reports")
                .definition(RADHelper.getRadUri("ForegroundReports"))
                .withVariableSize("foregroundReportCount")
                .withElement(foregroundStruct.getName(), foregroundStruct)
                .build();

        foregroundCount = radHelper.createArraySize("foregroundReportCount", "foregroundReportCount", "Foreground Report Count");

        backgroundReports = radHelper.createArray()
                .name("Background Reports")
                .label("Background Reports")
                .definition(RADHelper.getRadUri("BackgroundReports"))
                .withVariableSize("backgroundReportCount")
                .withElement(backgroundStruct.getName(), backgroundStruct)
                .build();

        backgroundCount = radHelper.createArraySize("backgroundReportCount", "backgroundReportCount", "Background Report Count");

        alarmReports = radHelper.createArray()
                .name("Alarm Reports")
                .label("Alarm Reports")
                .definition(RADHelper.getRadUri("AlarmReports"))
                .withVariableSize("alarmReportCount")
                .withElement(alarmStruct.getName(), alarmStruct)
                .build();

        alarmCount = radHelper.createArraySize("alarmReportCount", "alarmReportCount", "Alarm Report Count");

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("N42Report"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(fileName.getName(), fileName)
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


    public synchronized void onNewMessage(String n42Message, String fileName) {
        parseN42Message(n42Message, fileName);

        eventHandler.publish(new DataEvent(latestRecordTime, N42Output.this, dataStruct.getData().clone()));
    }


    protected void parseN42Message(String n42Message, String fileName) {

        // Determine counts
        var message = radHelper.getRadInstrumentData(n42Message);
        int foregroundElementCount = 0;
        int backgroundElementCount = 0;
        int alarmElementCount = 0;

        for (var jaxbElement : message.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration()) {
            Class<?> jaxbType = jaxbElement.getDeclaredType();

            if (jaxbType == RadMeasurementType.class) {
                RadMeasurementType radMeasurementType = (RadMeasurementType) jaxbElement.getValue();
                String measurementClass = radMeasurementType.getMeasurementClassCode().value();

                switch (measurementClass) {
                    case "Background":
                        backgroundElementCount++;
                        break;
                    case "Foreground":
                        foregroundElementCount++;
                        break;
                    default:
                }
            } else if (jaxbType == AnalysisResultsType.class) {
                AnalysisResultsType analysisResultsType = (AnalysisResultsType) jaxbElement.getValue();
                alarmElementCount += analysisResultsType.getRadAlarm().size();
            }
        }

        foregroundCount.setValue(foregroundElementCount);
        foregroundReports.updateSize();
        foregroundReports.getData().updateAtomCount();
        foregroundReports.assignNewDataBlock();

        backgroundCount.setValue(backgroundElementCount);
        backgroundReports.updateSize();
        backgroundReports.getData().updateAtomCount();
        backgroundReports.assignNewDataBlock();

        alarmCount.setValue(alarmElementCount);
        alarmReports.updateSize();
        alarmReports.getData().updateAtomCount();
        alarmReports.assignNewDataBlock();

        // Populate components with corresponding data
        int foregroundIndex = 0;
        int backgroundIndex = 0;
        int alarmIndex = 0;

        for (var jaxbElement : message.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration()) {
            Class<?> jaxbType = jaxbElement.getDeclaredType();

            if (jaxbType == RadMeasurementType.class) {
                RadMeasurementType radMeasurementType = (RadMeasurementType) jaxbElement.getValue();
                String measurementClass = radMeasurementType.getMeasurementClassCode().value();

                switch (measurementClass) {
                    case "Background": {
                        var reportComponent = backgroundReports.getComponent(backgroundIndex++);
                        //reportComponent.getData().updateAtomCount();
                        cleanRadMeasurement(radMeasurementType);
                        populateBackgroundReport(reportComponent, radMeasurementType.getMeasurementClassCode().name(),
                                radMeasurementType.getStartDateTime().toGregorianCalendar().getTimeInMillis(),
                                durationToDouble(radMeasurementType.getRealTimeDuration()),
                                radMeasurementType.getSpectrum().get(0).getChannelData().getValue(),
                                radMeasurementType.getSpectrum().get(1).getChannelData().getValue(),
                                radMeasurementType.getGrossCounts().get(0).getCountData().get(0),
                                radMeasurementType.getGrossCounts().get(1).getCountData().get(0));
                    }
                    break;
                    case "Foreground": {
                        var reportComponent = foregroundReports.getComponent(foregroundIndex++);
                        //reportComponent.getData().updateAtomCount();

                        Double lat = 0.0;
                        Double lon = 0.0;
                        Double elv = 0.0;
                        try {
                            if (radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getLongitudeValue() != null) {
                                lat = radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getLatitudeValue().getValue().doubleValue();
                                lon = radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getLongitudeValue().getValue().doubleValue();
                                elv = radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getElevationValue().doubleValue();
                            }
                        } catch (NullPointerException ignored) {
                        }
                        cleanRadMeasurement(radMeasurementType);
                        populateForegroundReport(reportComponent, radMeasurementType.getMeasurementClassCode().name(),
                                radMeasurementType.getStartDateTime().toGregorianCalendar().getTimeInMillis(),
                                durationToDouble(radMeasurementType.getRealTimeDuration()),
                                radMeasurementType.getSpectrum().get(0).getChannelData().getValue(),
                                radMeasurementType.getSpectrum().get(1).getChannelData().getValue(),
                                radMeasurementType.getGrossCounts().get(0).getCountData().get(0),
                                radMeasurementType.getGrossCounts().get(1).getCountData().get(0),
                                radMeasurementType.getDoseRate().get(0).getDoseRateValue().getValue(), lat, lon, elv);
                    }
                    break;
                }
            } else if (jaxbType == AnalysisResultsType.class) {
                AnalysisResultsType analysisResultsType = (AnalysisResultsType) jaxbElement.getValue();
                for (var radAlarmType : analysisResultsType.getRadAlarm()) {
                    var reportComponent = alarmReports.getComponent(alarmIndex++);
                    populateAlarmReport(reportComponent,
                            radAlarmType.getRadAlarmDateTime().toGregorianCalendar().getTimeInMillis(),
                            radAlarmType.getRadAlarmCategoryCode().value(),
                            radAlarmType.getRadAlarmDescription());
                }
            }
        }

        // 0 double
        // 1 int
        // 2 list mixed
        // 3 int
        // 4 list mixed
        // 5 int
        // 6 parallel

        DataBlockMixed dataBlock = (DataBlockMixed) dataStruct.getData();
        //dataStruct.toString();

        int index = 0;
        latestRecordTime = System.currentTimeMillis() / 1000;
        dataBlock.setLongValue(index++, latestRecordTime);
        dataBlock.setStringValue(index++, fileName);
        dataBlock.setIntValue(index++, foregroundCount.getValue());
        dataBlock.setBlock(index++, (AbstractDataBlock) foregroundReports.getData());
        dataBlock.setIntValue(index++, backgroundCount.getValue());
        dataBlock.setBlock(index++, (AbstractDataBlock) backgroundReports.getData());
        dataBlock.setIntValue(index++, alarmCount.getValue());
        dataBlock.setBlock(index++, (AbstractDataBlock) alarmReports.getData());

        dataBlock.updateAtomCount();

        /*
        dataStruct.getComponent(index++).setData(foregroundCount.getData());
        dataStruct.getComponent(index++).setData(foregroundReports.getData());
        dataStruct.getComponent(index++).setData(backgroundCount.getData());
        dataStruct.getComponent(index++).setData(backgroundReports.getData());
        dataStruct.getComponent(index++).setData(alarmCount.getData());
        dataStruct.getComponent(index++).setData(alarmReports.getData());

         */
    }

    private static void cleanRadMeasurement(RadMeasurementType radMeasurementType) {
        while (radMeasurementType.getSpectrum().size() < 2) {
            var spectrum = new SpectrumType();
            var channelData = new ChannelDataType();
            spectrum.setChannelData(channelData);
            radMeasurementType.getSpectrum().add(spectrum);
        }
        while (radMeasurementType.getGrossCounts().size() < 2) {
            var counts = new GrossCountsType();
            counts.getCountData().add(Double.NaN);
            radMeasurementType.getGrossCounts().add(counts);
        }
        while (radMeasurementType.getDoseRate().size() < 1) {
            var doseRate = new DoseRateType();
            var drValue = new DoseRateuSvhType();
            drValue.setValue(Double.NaN);
            doseRate.setDoseRateValue(drValue);
            radMeasurementType.getDoseRate().add(doseRate);
        }
    }

    private void populateBackgroundReport(DataComponent backgroundComponent, String classCode, Long startDateTime, Double realTimeDuration, List<Double> linEnCalSpectrum, List<Double> cmpEnCalSpectrum, Double gammaGrossCount, Double neutronGrossCount) {
        int index = 0;
        var dataBlock = backgroundComponent.getData();
        dataBlock.setLongValue(index++, startDateTime / 1000);
        dataBlock.setDoubleValue(index++, realTimeDuration);

        // -- Linear Spectrum --
        int linearSpectrumSize = linEnCalSpectrum.size();

        dataBlock.setIntValue(index++, linearSpectrumSize);
        var linearSpectrumArray = ((DataArrayImpl)backgroundComponent.getComponent("linearSpectrum"));
        linearSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < linearSpectrumSize; i++) {
            dataBlock.setDoubleValue(index++, linEnCalSpectrum.get(i));
        }

        // -- Compressed Spectrum --
        int compressedSpectrumSize = cmpEnCalSpectrum.size();

        dataBlock.setIntValue(index++, compressedSpectrumSize);
        var compressedSpectrumArray = ((DataArrayImpl)backgroundComponent.getComponent("compressedSpectrum"));
        compressedSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < compressedSpectrumSize; i++) {
            dataBlock.setDoubleValue(index++, cmpEnCalSpectrum.get(i));
        }

        dataBlock.setDoubleValue(index++, gammaGrossCount);
        dataBlock.setDoubleValue(index++, neutronGrossCount);
    }

    private void populateForegroundReport(DataComponent foregroundComponent, String classCode, Long startDateTime, Double realTimeDuration, List<Double> linEnCalSpectrum, List<Double> cmpEnCalSpectrum, Double gammaGrossCount, Double neutronGrossCount, Double doseRate, Double lat, Double lon, Double alt) {
        int index = 0;
        var dataBlock = foregroundComponent.getData();
        dataBlock.setLongValue(index++, startDateTime / 1000);
        dataBlock.setDoubleValue(index++, realTimeDuration);

        // -- Linear Spectrum --
        int linearSpectrumSize = linEnCalSpectrum.size();

        dataBlock.setIntValue(index++, linearSpectrumSize);
        var linearSpectrumArray = ((DataArrayImpl)foregroundComponent.getComponent("linearSpectrum"));
        linearSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < linearSpectrumSize; i++) {
            dataBlock.setDoubleValue(index++, linEnCalSpectrum.get(i));
        }

        // -- Compressed Spectrum --
        int compressedSpectrumSize = cmpEnCalSpectrum.size();

        dataBlock.setIntValue(index++, compressedSpectrumSize);
        var compressedSpectrumArray = ((DataArrayImpl)foregroundComponent.getComponent("compressedSpectrum"));
        compressedSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < compressedSpectrumSize; i++) {
            dataBlock.setDoubleValue(index++, cmpEnCalSpectrum.get(i));
        }

        dataBlock.setDoubleValue(index++, gammaGrossCount);
        dataBlock.setDoubleValue(index++, neutronGrossCount);
        dataBlock.setDoubleValue(index++, doseRate);
    }

    private void populateAlarmReport(DataComponent alarmComponent, Long startDateTime, String categoryCode, String description) {
        int index = 0;
        var dataBlock = alarmComponent.getData();
        dataBlock.setLongValue(index++, startDateTime / 1000);
        dataBlock.setStringValue(index++, categoryCode);
        dataBlock.setStringValue(index++, description);
    }

    private static double durationToDouble(Duration duration) {
        return duration.multiply(1000).getSeconds() / 1000.0;
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