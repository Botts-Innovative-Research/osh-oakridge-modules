package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class SetupGamma3Output extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "setupGamma3";
    private static final String SENSOR_OUTPUT_LABEL = "Setup Gamma 3";

    private static final Logger logger = LoggerFactory.getLogger(SetupGamma3Output.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public SetupGamma3Output(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init(){
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();
        var auxLLD = radHelper.createAuxiliaryLowerDiscriminator();
        var auxULD = radHelper.createAuxiliaryUpperDiscriminator();
        var backgroundTime = radHelper.createBackgroundTime();
        var backgroundNSigma = radHelper.createBackgroundNSigma();
        var firmwareVersion = radHelper.createFirmwareVersion();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("setup-gamma-3"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
                .addField(auxLLD.getName(), auxLLD)
                .addField(auxULD.getName(), auxULD)
                .addField(backgroundTime.getName(), backgroundTime)
                .addField(backgroundNSigma.getName(), backgroundNSigma)
                .addField(firmwareVersion.getName(), firmwareVersion)
                .build();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(String[] csvString){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }
        int index =0;
        dataBlock.setLongValue(index++,System.currentTimeMillis()/1000);
        dataBlock.setStringValue(index++, parent.laneName);
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[1])); //low discrim
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[2])); //high discrim
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[3])); //background time
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[4])); //background nsgima
        dataBlock.setStringValue(index++, csvString[5]); //version of firmware

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), SetupGamma3Output.this, dataBlock));

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

