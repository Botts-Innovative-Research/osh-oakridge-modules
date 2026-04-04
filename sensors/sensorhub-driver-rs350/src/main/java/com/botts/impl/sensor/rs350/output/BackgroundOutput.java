package com.botts.impl.sensor.rs350.output;

import com.botts.impl.sensor.rs350.MessageHandler;
import com.botts.impl.sensor.rs350.RS350Sensor;
import com.botts.impl.sensor.rs350.messages.RS350Message;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.DataArrayImpl;
import org.vast.data.TextEncodingImpl;

public class BackgroundOutput extends AbstractSensorOutput<RS350Sensor> implements MessageHandler.BackgroundListener {

    private static final String SENSOR_OUTPUT_NAME = "backgroundReport";
    private static final String SENSOR_OUTPUT_LABEL = "Background Report";


    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public BackgroundOutput(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var duration = radHelper.createDuration();
        var linearSpectrumCount = radHelper.createArraySize("linearSpectrumCount", "linearSpectrumCount", "Linear Spectrum Count");
        var linearSpectrumArray = radHelper.createLinearSpectrum();
        var compressedSpectrumCount = radHelper.createArraySize("compressedSpectrumCount", "compressedSpectrumCount", "Compressed Spectrum Count");
        var compressedSpectrumArray = radHelper.createCompressedSpectrum();
        var gammaGrossCount = radHelper.createGammaGrossCount();
        var neutronGrossCount = radHelper.createNeutronGrossCount();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("BackgroundReport"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(duration.getName(), duration)
                .addField(linearSpectrumCount.getName(), linearSpectrumCount)
                .addField(linearSpectrumArray.getName(), linearSpectrumArray)
                .addField(compressedSpectrumCount.getName(), compressedSpectrumCount)
                .addField(compressedSpectrumArray.getName(), compressedSpectrumArray)
                .addField(gammaGrossCount.getName(), gammaGrossCount)
                .addField(neutronGrossCount.getName(), neutronGrossCount)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(RS350Message message) {
        dataStruct.clearData();
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
            dataBlock.updateAtomCount();
            dataStruct.setData(dataBlock);
        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = System.currentTimeMillis() / 1000;

        int index = 0;
        dataBlock.setLongValue(index++, message.getRs350BackgroundMeasurement().getStartDateTime() / 1000);
        dataBlock.setDoubleValue(index++, message.getRs350BackgroundMeasurement().getRealTimeDuration());

        int linearSpectrumSize = message.getRs350BackgroundMeasurement().getLinEnCalSpectrum().size();

        dataBlock.setIntValue(index++, linearSpectrumSize);
        var linearSpectrumArray = ((DataArrayImpl)dataStruct.getComponent("linearSpectrum"));
        linearSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < linearSpectrumSize; i++) {
            dataBlock.setDoubleValue(index++, message.getRs350BackgroundMeasurement().getLinEnCalSpectrum().get(i));
        }

        int compressedSpectrumSize = message.getRs350BackgroundMeasurement().getCmpEnCalSpectrum().size();

        dataBlock.setIntValue(index++, compressedSpectrumSize);
        var compressedSpectrumArray = ((DataArrayImpl)dataStruct.getComponent("compressedSpectrum"));
        compressedSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < compressedSpectrumSize; i++) {
            dataBlock.setDoubleValue(index++, message.getRs350BackgroundMeasurement().getCmpEnCalSpectrum().get(i));
        }

        dataBlock.setDoubleValue(index++, message.getRs350BackgroundMeasurement().getGammaGrossCount());
        dataBlock.setDoubleValue(index, message.getRs350BackgroundMeasurement().getNeutronGrossCount());

        eventHandler.publish(new DataEvent(latestRecordTime, BackgroundOutput.this, dataBlock));
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
