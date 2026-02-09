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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataArrayImpl;
import org.vast.data.TextEncodingImpl;

public class ForegroundOutput extends AbstractSensorOutput<RS350Sensor> implements MessageHandler.MessageListener {

    private static final String SENSOR_OUTPUT_NAME = "foregroundReport";

    private static final Logger logger = LoggerFactory.getLogger(ForegroundOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public ForegroundOutput(RS350Sensor parentSensor) {
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

        var doseRate = radHelper.createDoseUSVh();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Foreground Report")
                .definition(RADHelper.getRadUri("ForegroundReport"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(duration.getName(), duration)
                .addField(linearSpectrumCount.getName(), linearSpectrumCount)
                .addField(linearSpectrumArray.getName(), linearSpectrumArray)
                .addField(compressedSpectrumCount.getName(), compressedSpectrumCount)
                .addField(compressedSpectrumArray.getName(), compressedSpectrumArray)
                .addField(gammaGrossCount.getName(), gammaGrossCount)
                .addField(neutronGrossCount.getName(), neutronGrossCount)
                .addField(doseRate.getName(), doseRate)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

    }


    public void onNewMessage(RS350Message message) {
        if (message.getRs350ForegroundMeasurement() == null) {
            return;
        }
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = System.currentTimeMillis() / 1000;

        int index = 0;
        dataBlock.setLongValue(index++, message.getRs350ForegroundMeasurement().getStartDateTime() / 1000);
        dataBlock.setDoubleValue(index++, message.getRs350ForegroundMeasurement().getRealTimeDuration());

        int linearSpectrumSize = message.getRs350ForegroundMeasurement().getLinEnCalSpectrum().length;

        dataBlock.setIntValue(index++, linearSpectrumSize);
        var linearSpectrumArray = ((DataArrayImpl)dataStruct.getComponent("linearSpectrum"));
        linearSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < linearSpectrumSize; i++) {
            dataBlock.setDoubleValue(index++, message.getRs350ForegroundMeasurement().getLinEnCalSpectrum()[i]);
        }

        int compressedSpectrumSize = message.getRs350ForegroundMeasurement().getCmpEnCalSpectrum().length;

        dataBlock.setIntValue(index++, compressedSpectrumSize);
        var compressedSpectrumArray = ((DataArrayImpl)dataStruct.getComponent("compressedSpectrum"));
        compressedSpectrumArray.updateSize();
        dataBlock.updateAtomCount();
        for (int i = 0; i < compressedSpectrumSize; i++) {
            dataBlock.setDoubleValue(index++, message.getRs350ForegroundMeasurement().getCmpEnCalSpectrum()[i]);
        }

        dataBlock.setDoubleValue(index++, message.getRs350ForegroundMeasurement().getGammaGrossCount());
        dataBlock.setDoubleValue(index++, message.getRs350ForegroundMeasurement().getNeutronGrossCount());
        dataBlock.setDoubleValue(index, message.getRs350ForegroundMeasurement().getDoseRate());

        eventHandler.publish(new DataEvent(latestRecordTime, ForegroundOutput.this, dataBlock));

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
