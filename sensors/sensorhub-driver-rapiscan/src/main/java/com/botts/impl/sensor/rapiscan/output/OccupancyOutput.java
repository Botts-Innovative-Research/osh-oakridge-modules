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
import org.vast.data.TextImpl;

public class OccupancyOutput  extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "occupancy";
    private static final String SENSOR_OUTPUT_LABEL = "Occupancy";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public OccupancyOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init(){

    }

    public void onNewMessage(long startTime, long endTime, Boolean isGammaAlarm, Boolean isNeutronAlarm, String[] csvString, int gammaMax, int neutronMax){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }
        int index =0;

        dataBlock.setLongValue(index++, System.currentTimeMillis()/1000);
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[1])); //occupancy count
        dataBlock.setLongValue(index++, startTime/1000);
        dataBlock.setLongValue(index++, endTime/1000);
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[2])/1000); //neutron background
        dataBlock.setBooleanValue(index++, isGammaAlarm);
        dataBlock.setBooleanValue(index++, isNeutronAlarm);
        dataBlock.setIntValue(index++, gammaMax);
        dataBlock.setIntValue(index++, neutronMax);
        dataBlock.setBooleanValue(index, false);
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