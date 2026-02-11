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
import org.vast.data.DataBlockMixed;
import org.vast.data.DataValue;
import org.vast.data.TextEncodingImpl;

public class StatusOutput extends AbstractSensorOutput<RS350Sensor> implements MessageHandler.StatusListener {

    private static final String SENSOR_OUTPUT_NAME = "status";
    private static final String SENSOR_OUTPUT_LABEL = "Status";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public StatusOutput(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();
        var samplingTime = radHelper.createPrecisionTimeStamp();
        var batteryCharge = radHelper.createBatteryCharge();
        var scanMode = radHelper.createScanMode();
        var scanTimeout = radHelper.createScanTimeout();
        var linearCalibration = radHelper.createLinearCalibration();
        var compressedCalibration = radHelper.createCompressedCalibration();
        var analysisEnabled = radHelper.createAnalysisEnabled();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(radHelper.getRadUri("DeviceStatus"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(batteryCharge.getName(), batteryCharge)
                .addField(scanMode.getName(), scanMode)
                .addField(scanTimeout.getName(), scanTimeout)
                .addField(analysisEnabled.getName(), analysisEnabled)
                .addField(linearCalibration.getName(), linearCalibration)
                .addField(compressedCalibration.getName(), compressedCalibration)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }


    public void onNewMessage(RS350Message message) {
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }


        latestRecordTime = System.currentTimeMillis() / 1000;

        int index = 0;
        dataBlock.setLongValue(index++, latestRecordTime);
        dataBlock.setDoubleValue(index++, message.getRs350InstrumentCharacteristics().getBatteryCharge());
        dataBlock.setStringValue(index++, message.getRs350Item().getRsiScanMode());
        dataBlock.setDoubleValue(index++, message.getRs350Item().getRsiScanTimeoutNumber());
        dataBlock.setBooleanValue(index++, message.getRs350Item().getRsiAnalysisEnabled());

        for (int i = 0; i < 3; i++) {
            dataBlock.setDoubleValue(index++, message.getRs350LinEnergyCalibration().getLinEnCal()[i]);
        }

        for (int i = 0; i < 3; i++) {
            dataBlock.setDoubleValue(index++, message.getRs350CmpEnergyCalibration().getCmpEnCal()[i]);
        }
        eventHandler.publish(new DataEvent(latestRecordTime, StatusOutput.this, dataBlock));
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
