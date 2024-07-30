package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import java.lang.Boolean;


public class TSTARPositionOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Position Output";
    DataRecord dataStruct;
    DataEncoding dataEncoding;

    public TSTARPositionOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

//    Position {
//        id integer($int64)
//        campaign_id integer($int64)
//        unit_id integer($int64)
//        latitude number($double)
//        longitude number($double)
//        course integer($int64)
//        speed integer($int64)
//        channel integer($int64)
//        generated_timestamp string($date-time)
//        received_timestamp string($date-time)
//    }
    protected void init() {
        TSTARHelper tstarHelper = new TSTARHelper();

        // SWE Common data structure
        dataStruct = tstarHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("EventData"))
                .addField("id", tstarHelper.createId())
                .addField("campaign-id", tstarHelper.createCampaignId())
                .addField("unit-id", tstarHelper.createUnitId())
                .addField("location", tstarHelper.createLocationVectorLLA())
                .addField("course", tstarHelper.createCourse())
                .addField("speed", tstarHelper.createSpeed())
                .addField("channel", tstarHelper.createChannel())
                .addField("generated_timestamp", tstarHelper.createGeneratedTimestamp())
                .addField("received_timestamp", tstarHelper.createReceivedTimestamp())
                .build();

        // set encoding to CSV
        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }

    public void doStart() {

    }

    public void doStop() {

    }

    public void onNewMessage() {

        DataBlock dataBlock;

        int id = 0;
        int campaign_id = 0;
        int unit_id = 0;
        double latitude = Double.NaN;
        double longitude = Double.NaN;
        int course = 0;
        int speed = 0;
        int channel = 0;
        String generated_timestamp = " ";
        String received_timestamp = " ";

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {
            dataBlock = latestRecord.renew();


            dataBlock.setDoubleValue(0, id);
            dataBlock.setIntValue(1, campaign_id);
            dataBlock.setIntValue(2, unit_id);
            dataBlock.setDoubleValue(3, latitude);
            dataBlock.setDoubleValue(4, longitude);
            dataBlock.setIntValue(5, course);
            dataBlock.setIntValue(6, speed);
            dataBlock.setIntValue(7, channel);
            dataBlock.setStringValue(8, generated_timestamp);
            dataBlock.setStringValue(9, received_timestamp);

            // update latest record and send event
            latestRecord = dataBlock;
            eventHandler.publish(new DataEvent(latestRecordTime, TSTARPositionOutput.this, dataBlock));
        }
    }
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 5;
    }
}