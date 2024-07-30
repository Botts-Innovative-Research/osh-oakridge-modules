package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import javax.swing.text.Position;
import java.io.IOException;
import java.lang.Boolean;


public class TSTAREventOutput extends AbstractSensorOutput<TSTARDriver>{
    private static final String SENSOR_OUTPUT_NAME = "Event Output";
    DataRecord dataStruct;
    DataEncoding dataEncoding;
    DataComponent dataComponent;

    public TSTAREventOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

//EVENT
//    id integer($int64)
//    campaign_id integer($int64)
//    unit_id integer($int64)
//    alarm boolean
//    event_type string
//    latitude number($double)
//    longitude number($double)
//    msg_data {}
//    generated_timestamp string($date-time)
//    received_timestamp string($date-time)
//    notification_sent boolean


    protected void init()
    {
        TSTARHelper tstarHelper = new TSTARHelper();
        GeoPosHelper geoHelp = new GeoPosHelper();

        // SWE Common data structure
        dataStruct = tstarHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("EventData"))
                .addField("id", tstarHelper.createId())
                .addField("campaign-id", tstarHelper.createCampaignId())
                .addField("unit-id", tstarHelper.createUnitId())
                .addField("alarm", tstarHelper.createAlarm())
                .addField("event_type", tstarHelper.createEventType())
                .addField("location", tstarHelper.createLocationVectorLatLon())
//                .addField("msg_data", tstarHelper.createMessageData())
                .addField("generated_timestamp", tstarHelper.createGeneratedTimestamp())
                .addField("received_timestamp", tstarHelper.createReceivedTimestamp())
                .addField("notification_sent", tstarHelper.createNotificationSent())
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
        Boolean alarm = false;
        String event_type = " ";
        double latitude = Double.NaN;
        double longitude = Double.NaN;
        Object msg_data = new Object();
        String generated_timestamp = " ";
        String received_timestamp = " ";
        Boolean notification_sent = false;

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {
            dataBlock = latestRecord.renew();


                dataBlock.setDoubleValue(0, id);
                dataBlock.setIntValue(1, campaign_id);
                dataBlock.setIntValue(2, unit_id);
                dataBlock.setBooleanValue(3, alarm);
                dataBlock.setStringValue(4, event_type);
                dataBlock.setDoubleValue(5, latitude);
                dataBlock.setDoubleValue(6, longitude);
                dataBlock.setUnderlyingObject(msg_data);
                dataBlock.setStringValue(8, generated_timestamp);
                dataBlock.setStringValue(9, received_timestamp);
                dataBlock.setBooleanValue(10, notification_sent);


                // update latest record and send event
                latestRecord = dataBlock;
                latestRecordTime = System.currentTimeMillis();
                eventHandler.publish(new DataEvent(latestRecordTime, TSTAREventOutput.this, dataBlock));
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