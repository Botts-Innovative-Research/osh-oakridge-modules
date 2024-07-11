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

public class OccupancyOutput  extends AbstractSensorOutput<RapiscanSensor> {

    public static final String SENSOR_OUTPUT_NAME = "occupancy";
    public static final String SENSOR_OUTPUT_LABEL = "Occupancy";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public OccupancyOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init(){
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();
        var occupancyCount = radHelper.createOccupancyCount();
        var occupancyStart = radHelper.createOccupancyStartTime();
        var occupancyEnd = radHelper.createOccupancyEndTime();
        var neutronBackground = radHelper.createNeutronBackground();
        var gammaAlarm = radHelper.createGammaAlarm();
        var neutronAlarm = radHelper.createNeutronAlarm();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("occupancy"))
                .description("System occupancy count since midnight each day")
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
                .addField(occupancyCount.getName(), occupancyCount)
                .addField(occupancyStart.getName(), occupancyStart)
                .addField(occupancyEnd.getName(), occupancyEnd)
                .addField(neutronBackground.getName(), neutronBackground)
                .addField(gammaAlarm.getName(), gammaAlarm)
                .addField(neutronAlarm.getName(), neutronAlarm)
                .build();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(long startTime, long endTime, Boolean isGammaAlarm, Boolean isNeutronAlarm, String[] csvString){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }
        int index =0;

        dataBlock.setLongValue(index++, System.currentTimeMillis()/1000);
        dataBlock.setStringValue(index++, parent.laneID);
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[1])); //occupancy count
        dataBlock.setLongValue(index++, startTime/1000);
        dataBlock.setLongValue(index++, endTime/1000);
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[2])/1000); //neutron background count
        dataBlock.setBooleanValue(index++, isGammaAlarm);
        dataBlock.setBooleanValue(index++, isNeutronAlarm);

        //dataBlock.updateAtomCount();
        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), OccupancyOutput.this, dataBlock));

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
