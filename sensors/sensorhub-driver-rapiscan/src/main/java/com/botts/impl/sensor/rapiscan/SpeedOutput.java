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
import org.vast.swe.SWEBuilders;

public class SpeedOutput  extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "Speed";

    private static final Logger logger = LoggerFactory.getLogger(SpeedOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public SpeedOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init(){
        RADHelper radHelper = new RADHelper();

        DataRecord recordBuilder = radHelper.createRecord()
                .name(getName())
                .label("Speed")
                .updatable(true)
                .definition(RADHelper.getRadUri("speed"))
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("LaneID", radHelper.createLaneId())
                .addField("speed-time", radHelper.createSpeedTimeStamp())
                .addField("speed-mph", radHelper.createSpeedMph())
                .addField("speed-kph", radHelper.createSpeedKph())
                .build();

        dataStruct =recordBuilder;
        dataEncoding = new TextEncodingImpl(",", "\n");

    }

    public void onNewMessage(String[] csvString){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        int index = 0;

        dataBlock.setLongValue(index++,System.currentTimeMillis()/1000);
        dataBlock.setIntValue(index++, parent.laneId);
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[1]));
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[2]));
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[3]));

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), SpeedOutput.this, dataBlock));

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
