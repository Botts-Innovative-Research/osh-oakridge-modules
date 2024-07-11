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

public class SetupGamma2Output extends AbstractSensorOutput<RapiscanSensor> {

    public static final String SENSOR_OUTPUT_NAME = "setupGamma2";
    public static final String SENSOR_OUTPUT_LABEL = "Setup Gamma 2";

    private static final Logger logger = LoggerFactory.getLogger(SetupGamma2Output.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public SetupGamma2Output(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init(){
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();
        var detectors = radHelper.createDetectors();
        var controlLLD = radHelper.createLowerControlDiscriminator();
        var controlULD = radHelper.createUpperControlDiscriminator();
        var relayOutput = radHelper.createRelayOutput();
        var algorithm = radHelper.createAlgorithm();
        var version = radHelper.createSoftwareVersion();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("setup-gamma-2"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
                .addField(detectors.getName(), detectors)
                .addField(controlLLD.getName(), controlLLD)
                .addField(controlULD.getName(), controlULD)
                .addField(relayOutput.getName(), relayOutput)
                .addField(algorithm.getName(), algorithm)
                .addField(version.getName(), version)
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
        dataBlock.setStringValue(index++, parent.laneID);
        dataBlock.setStringValue(index++, csvString[1]); //detectors
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[2])); //low discrim
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[3])); //high discrim
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[4])); //relay out
        dataBlock.setStringValue(index++, csvString[5]); //algorithm
        dataBlock.setStringValue(index++, csvString[6]); //version software

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), SetupGamma2Output.this, dataBlock));

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
