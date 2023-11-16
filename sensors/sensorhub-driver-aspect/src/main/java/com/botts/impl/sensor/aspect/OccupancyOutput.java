package com.botts.impl.sensor.aspect;

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

public class OccupancyOutput  extends AbstractSensorOutput<AspectSensor> {

    private static final String SENSOR_OUTPUT_NAME = "Occupancy";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public OccupancyOutput(AspectSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init(){
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Occupancy")
                .definition(RADHelper.getRadUri("occupancy"))
                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
                .addField("OccupancyDailyCount", radHelper.createOccupancyCount())
                .addField("NeutronBackground", radHelper.createOccupancyNeutronBackground())
                .addField("StartTime", radHelper.createOccupancyStartTime())
                .addField("EndTime", radHelper.createOccupancyEndTime())
                .addField("GammaAlarm",
                        radHelper.createBoolean()
                                .name("gamma-alarm")
                                .label("Gamma Alarm")
                                .definition(RADHelper.getRadUri("gamma-alarm")))
                .addField("NeutronAlarm",
                        radHelper.createBoolean()
                                .name("neutron-alarm")
                                .label("Neutron Alarm")
                                .definition(RADHelper.getRadUri("neutron-alarm")))
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

    }

    public void onNewMessage(long startTime, long endTime, Boolean isGammaAlarm, Boolean isNeutronAlarm, String[] csvLine){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }


        dataBlock.setLongValue(0, System.currentTimeMillis()/1000);
        dataBlock.setIntValue(1, Integer.getInteger(csvLine[1]));
        //Note that in the GX line, one does not divide by 1000 to get the average neutron background like you do for Rapiscan.
        dataBlock.setLongValue(2, Long.getLong(csvLine[2]));
        dataBlock.setLongValue(3, startTime/1000);
        dataBlock.setLongValue(4, endTime/1000);
        dataBlock.setBooleanValue(5, isGammaAlarm);
        dataBlock.setBooleanValue(6, isNeutronAlarm);

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
