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

public class GammaSetup2Output extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Gamma Setup 2";

    private static final Logger logger = LoggerFactory.getLogger(GammaSetup2Output.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    protected GammaSetup2Output(RapiscanSensor parentSensor) {
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
                .label("Setup Gamma 2")
                .definition(RADHelper.getRadUri("setup-gamma-2"))
                .addField("TimeStamp", radHelper.createPrecisionTimeStamp())
                .addField("LaneName", radHelper.createLaneId())
                .addField("detectors-on-line", radHelper.createDetectors())
                .addField("control-lower-level-discriminator", radHelper.createLowerControlDiscriminator())
                .addField("control-upper-level-discriminator", radHelper.createUpperControlDiscriminator())
                .addField("relay-output", radHelper.createRelayOutput())
                .addField("algorithm", radHelper.createAlgorithm())
                .addField("version-software", radHelper.createSoftwareVersion())
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
        dataBlock.setStringValue(index++, csvString[1]); //detectors
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[2])); //low discrim
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[3])); //high discrim
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[4])); //relay out
        dataBlock.setStringValue(index++, csvString[5]); //algorithm
        dataBlock.setStringValue(index++, csvString[6]); //version software

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), GammaSetup2Output.this, dataBlock));

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
