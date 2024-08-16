package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.tstar.responses.Event;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import javax.swing.text.Position;
import java.io.IOException;
import java.lang.Boolean;
import java.text.SimpleDateFormat;
import java.util.Date;


public class TSTAREventOutput extends AbstractSensorOutput<TSTARDriver>{
    private static final String SENSOR_OUTPUT_NAME = "Event Output";
    protected DataRecord dataStruct;
    DataEncoding dataEncoding;
    DataBlock dataBlock;
    Long eventGeneratedTimestamp;
    Long eventReceivedTimestamp;
    Long eventAckTimestamp;

    public TSTAREventOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init()
    {
        TSTARHelper tstarHelper = new TSTARHelper();

        // SWE Common data structure
        dataStruct = tstarHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("EventData"))
                .addField("id", tstarHelper.createEventId())
                .addField("event_type", tstarHelper.createEventType())
                .addField("alarm", tstarHelper.createAlarm())
                .addField("campaign_id", tstarHelper.createCampaignId())
                .addField("unit-id", tstarHelper.createUnitId())
                .addField("generated_timestamp", tstarHelper.createGeneratedTimestamp())
                .addField("received_timestamp", tstarHelper.createReceivedTimestamp())
                .addField("acknowledged_by", tstarHelper.createAcknowledgedBy())
                .addField("location", tstarHelper.createLocationVectorLatLon())
                .addField("ack_timestamp", tstarHelper.createAckTimestamp())
//                .addField("msg_data", tstarHelper.createMessageData())
                .addField("notification_sent", tstarHelper.createNotificationSent())
                .build();

        // set encoding to CSV
        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }

    public void parse(Event event) {

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {
            dataBlock = latestRecord.renew();
        }
        latestRecordTime = System.currentTimeMillis() / 1000;
        setEventTimes(event);

                dataBlock.setIntValue(0, event.id);
                dataBlock.setStringValue(1, event.event_type);
                dataBlock.setBooleanValue(2, event.alarm);
                dataBlock.setIntValue(3, event.campaign_id);
                dataBlock.setIntValue(4, event.unit_id);
                dataBlock.setLongValue(5, eventGeneratedTimestamp);
                dataBlock.setLongValue(6, eventReceivedTimestamp);
                dataBlock.setDoubleValue(7, event.latitude);
                dataBlock.setDoubleValue(8, event.longitude);
                dataBlock.setLongValue(9, eventAckTimestamp);
                //msg_data Object? : int source_id, String unit_name, String sensor_name
                dataBlock.setBooleanValue(10, event.notification_sent);


                // update latest record and send event
                latestRecord = dataBlock;
                latestRecordTime = System.currentTimeMillis();
                eventHandler.publish(new DataEvent(latestRecordTime, TSTAREventOutput.this, dataBlock));
            }


    public void setEventTimes(Event event){
        // parse UTC  to epoch time for 'generated_timestamp,' 'received_timestamp,' & ack_timestamp
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        Date generatedTimestamp = event.generated_timestamp;
        eventGeneratedTimestamp = generatedTimestamp.getTime()/ 1000;

        Date receivedTimestamp = event.received_timestamp;
        eventReceivedTimestamp = receivedTimestamp.getTime() / 1000;


        if (event.ack_timestamp != null){
            Date ackTimestamp = event.ack_timestamp;
            eventAckTimestamp = ackTimestamp.getTime() / 1000;
        } else {
            eventAckTimestamp = 0L;
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
        return 1;
    }
}