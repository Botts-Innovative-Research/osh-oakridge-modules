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

public class GammaSetUp1Output extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "Gamma Setup 1";

    private static final Logger logger = LoggerFactory.getLogger(GammaSetUp1Output.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    protected GammaSetUp1Output(RapiscanSensor parentSensor) {
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
                .label("Setup Gamma 1")
                .definition(RADHelper.getRadUri("setup-gamma-1"))
                .addField("TimeStamp", radHelper.createPrecisionTimeStamp())
                .addField("LaneID", radHelper.createLaneId())
                .addField("high-background-fault", radHelper.createHighBackgroundFault())
                .addField("low-background-fault", radHelper.createLowBackgroundFault())
                .addField("Intervals", radHelper.createIntervals())
                .addField("occupancy-holdin", radHelper.createOccupancyHoldin())
                .addField("NSigma", radHelper.createNSigma())
                .addField("placeholder", radHelper.createPlaceholder())
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
        dataBlock.setIntValue(index++, parent.laneId);
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[1])); //high bg
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[2])); // low bg
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[3])); //intervals
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[4])); //occupancy holding
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[5])); //nsigma
        dataBlock.setStringValue(index++, csvString[6]); //placeholder

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), GammaSetUp1Output.this, dataBlock));

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
