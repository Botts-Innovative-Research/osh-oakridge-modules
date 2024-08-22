package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.tstar.responses.MessageLog;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;


public class TSTARMessageLogOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Message Log Output";
    protected DataRecord dataStruct;
    DataEncoding dataEncoding;
    Long msgLogTimestamp;
    Long msgLogDelivered;
    TSTARHelper tstarHelper = new TSTARHelper();

    public TSTARMessageLogOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        dataStruct = createDataRecord();
        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }
    public DataRecord createDataRecord(){
        TSTARHelper tstarHelper = new TSTARHelper();
        return tstarHelper.createRecord()
                // SWE Common data structure

                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("MessageLogData"))
                .addField("messageLogId", tstarHelper.createMessageLogId())
                .addField("timestamp", tstarHelper.createTimestamp())
                .addField("delivered", tstarHelper.createDelivered())
                .addField("direction", tstarHelper.createDirection())
                .addField("channel", tstarHelper.createChannel())
                .addField("meta", tstarHelper.createMeta()) //Fields(3): key_id, channel_info{remote}, nonce_counter
                .addField("unitId", tstarHelper.createUnitId())
                .addField("campaignId", tstarHelper.createCampaignId())
                .addField("position", tstarHelper.createPosition())
                .addField("rawPacketData", tstarHelper.createRawPacket())
                .addField("messageLogMessage",tstarHelper.createMsgLogMessage()) //Fields(2): low, high
//                .addField("unitName", tstarHelper.createUnitName())
                .build();

        // set encoding to CSV
    }

    public void parse(MessageLog msgLog) {

        dataStruct = createDataRecord();
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataStruct.setData(dataBlock);

        latestRecordTime = System.currentTimeMillis() / 1000;
        setMessageLogTime(msgLog);

        int i = 0;
        int numArrayFields = msgLog.raw_packet.data.length;
        int[] packetData = msgLog.raw_packet.data;

        dataBlock.setIntValue(i++, msgLog.id);
        dataBlock.setLongValue(i++, msgLogTimestamp);
        try {
            dataBlock.setLongValue(i++, msgLogDelivered);
        } catch (NullPointerException e){}
        dataBlock.setStringValue(i++, msgLog.direction);
        dataBlock.setStringValue(i++, msgLog.channel);
        dataBlock.setIntValue(i++, msgLog.meta.key_id);
        dataBlock.setStringValue(i++, msgLog.meta.channel_info.remote);
        dataBlock.setIntValue(i++, msgLog.meta.nonce_counter);
        dataBlock.setIntValue(i++, msgLog.unit_id);
        dataBlock.setIntValue(i++, msgLog.campaign_id);
        dataBlock.setStringValue(i++, msgLog.position);

        dataBlock.setStringValue(i++, msgLog.raw_packet.type);
        dataBlock.setIntValue(i++, msgLog.raw_packet.data.length);

        var array = ((DataArrayImpl) dataStruct.getComponent("rawPacketData").getComponent("arrayData"));
        array.updateSize();

        for (int ix = 0; ix < msgLog.raw_packet.data.length; ix++) {
            dataBlock.setIntValue(i++, msgLog.raw_packet.data[ix]);
        }
//        //Low {Gps}
        try {
            var gpsLow = msgLog.message.low.gps;
            dataBlock.setDoubleValue(i++, gpsLow.hdop);
            dataBlock.setDoubleValue(i++, gpsLow.pdop);
            dataBlock.setDoubleValue(i++, gpsLow.vdop);
            dataBlock.setDoubleValue(i++, gpsLow.speed);
            dataBlock.setDoubleValue(i++, gpsLow.course);
            dataBlock.setDoubleValue(i++, gpsLow.altitude);
            dataBlock.setStringValue(i++, gpsLow.fix_type);
            dataBlock.setDoubleValue(i++, gpsLow.latitude);
            dataBlock.setDoubleValue(i++, gpsLow.longitude);
            dataBlock.setIntValue(i++, gpsLow.timestamp);
            dataBlock.setIntValue(i++, gpsLow.num_satellites);
        } catch (NullPointerException e){}

        //Low
        try {
            dataBlock.setStringValue(i++, msgLog.message.low.arm_state);
        } catch (NullPointerException e) {}

        //Low {Event log[]}
        try {
            var event_log = msgLog.message.low.event_log;

            dataBlock.setStringValue(i++, event_log[0].fix_type);
            dataBlock.setDoubleValue(i++, event_log[0].latitude);
            dataBlock.setDoubleValue(i++, event_log[0].longitude);
            dataBlock.setIntValue(i++, event_log[0].source_id);
            dataBlock.setIntValue(i++, event_log[0].timestamp);
            dataBlock.setStringValue(i++, event_log[0].event_type);
        } catch (NullPointerException e) {}

        //Low
        try {
            var low = msgLog.message.low;

            dataBlock.setIntValue(i++, low.timestamp);
            dataBlock.setStringValue(i++, low.power_mode);
            dataBlock.setIntValue(i++, low.battery_voltage);
            dataBlock.setIntValue(i++, low.event_log_length);
            dataBlock.setIntValue(i++, low.position_log_length);
            dataBlock.setIntValue(i++, low.checkin_schedule_sec);
        } catch (NullPointerException e) {}

        //High {UiData{Sensors[]}
        try {
            var sensors = msgLog.message.high.ui_data.sensors;
            dataBlock.setIntValue(i++, sensors.length);

            var sensorArray =
                    ((DataArrayImpl) dataStruct.getComponent("messageLogMessage").getComponent("high").getComponent(
                            "uiData").getComponent("sensors").getComponent ("sensorArray"));
            sensorArray.updateSize();

            for (int ixx = 0; ixx < sensors.length; ixx++) {
                dataBlock.setStringValue(i++, sensors[ixx].name);
                dataBlock.setIntValue(i++, sensors[ixx].node_id);
                dataBlock.setIntValue(i++, sensors[ixx].position_x);
                dataBlock.setIntValue(i++, sensors[ixx].position_y);
            }
        } catch (NullPointerException e) {}

        try {
            var uiDataHigh = msgLog.message.high.ui_data;

            dataBlock.setStringValue(i++, uiDataHigh.campaign_name);
            dataBlock.setStringValue(i++, uiDataHigh.container_image);

        } catch (NullPointerException e) {}


//            dataBlock.setStringValue(i++, msgLog.unit_name);




        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(latestRecordTime, TSTARMessageLogOutput.this, dataBlock));
    }
    public void setMessageLogTime(MessageLog msgLog) {
        // parse UTC  to epoch time
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        if (msgLog.timestamp != null) {
            Date messageLogTimestamp = msgLog.timestamp;
            msgLogTimestamp = messageLogTimestamp.getTime() / 1000;
        } else {
            msgLogTimestamp = 0L;
        }
        if (msgLog.delivered != null) {
            Date messageLogDelivered = msgLog.delivered;
            msgLogDelivered = messageLogDelivered.getTime() / 1000;
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
