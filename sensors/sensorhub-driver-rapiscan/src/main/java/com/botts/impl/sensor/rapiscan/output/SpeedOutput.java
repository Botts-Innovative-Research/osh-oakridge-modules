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

public class SpeedOutput  extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "speed";
    private static final String SENSOR_OUTPUT_LABEL = "Speed";

    private static final Logger logger = LoggerFactory.getLogger(SpeedOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public SpeedOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init(){
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();
        var speedTime = radHelper.createSpeedTimeStamp();
        var speedMPH = radHelper.createSpeedMph();
        var speedKPH = radHelper.createSpeedKph();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("speed"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
                .addField(speedTime.getName(), speedTime)
                .addField(speedMPH.getName(), speedMPH)
                .addField(speedKPH.getName(), speedKPH)
                .build();

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
        dataBlock.setStringValue(index++, parent.laneName);
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
