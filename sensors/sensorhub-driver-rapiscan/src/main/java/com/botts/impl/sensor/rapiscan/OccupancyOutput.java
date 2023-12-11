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

public class OccupancyOutput  extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "Occupancy";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public OccupancyOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init(){
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Occupancy")
                .definition(RADHelper.getRadUri("occupancy"))
                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
                .addField("OccupancyCount", radHelper.createOccupancyCount())
                .addField("StartTime", radHelper.createOccupancyStartTime())
                .addField("EndTime", radHelper.createOccupancyEndTime())
                .addField("NeutronBackground", radHelper.createNeutronBackground())
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

    public void onNewMessage(long startTime, long endTime, Boolean isGammaAlarm, Boolean isNeutronAlarm, String[] csvString){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setLongValue(0, System.currentTimeMillis()/1000);
        dataBlock.setIntValue(1, Integer.parseInt(csvString[1]));
        dataBlock.setLongValue(2, startTime/1000);
        dataBlock.setLongValue(3, endTime/1000);
        dataBlock.setDoubleValue(4, Double.parseDouble(csvString[2])/1000);
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
