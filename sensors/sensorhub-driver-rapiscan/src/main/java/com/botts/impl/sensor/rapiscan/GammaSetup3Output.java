package com.botts.impl.sensor.rapiscan;

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

public class GammaSetup3Output extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Gamma Setup 3";

    private static final Logger logger = LoggerFactory.getLogger(GammaSetup3Output.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    protected GammaSetup3Output(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init(){
        dataStruct = createDataRecord();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }
    DataRecord createDataRecord(){
        RADHelper radHelper = new RADHelper();

        DataRecord recordBuilder = radHelper.createRecord()
                .name(getName())
                .label("Setup Gamma 3")
                .definition(RADHelper.getRadUri("setup-gamma-3"))
                .addField("TimeStamp", radHelper.createPrecisionTimeStamp())
                .addField("LaneName", radHelper.createLaneId())
                .addField("auxiliary-lower-level-discriminator", radHelper.createAuxiliaryLowerDiscriminator())
                .addField("auxiliary-upper-level-discriminator", radHelper.createAuxiliaryUpperDiscriminator())
                .addField("background-time", radHelper.createBackgroundTime())
                .addField("background-nsigma", radHelper.createBackgroundNSigma())
                .addField("version-firmware", radHelper.createFirmwareVersion())
                .build();


        return recordBuilder;
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
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), GammaSetup3Output.this, dataBlock));

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

