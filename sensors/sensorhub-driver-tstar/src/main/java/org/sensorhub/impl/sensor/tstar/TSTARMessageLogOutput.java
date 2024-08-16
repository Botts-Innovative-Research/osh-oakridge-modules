package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.tstar.responses.MessageLog;
import org.vast.swe.SWEHelper;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;


public class TSTARMessageLogOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Message Log Output";
    protected DataRecord dataStruct;
    DataEncoding dataEncoding;
    DataBlock dataBlock;
    Long msgLogTimestamp;
    Long msgLogDelivered;

    public TSTARMessageLogOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        TSTARHelper tstarHelper = new TSTARHelper();

        // SWE Common data structure
        dataStruct = tstarHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("MessageLogData"))
                .addField("message_log_id", tstarHelper.createMessageLogId())
                .addField("timestamp", tstarHelper.createTimestamp())
                .addField("delivered", tstarHelper.createDelivered())
                .addField("in", tstarHelper.createIn())
                .addField("channel", tstarHelper.createChannel())
//                .addField("meta", tstarHelper.createMeta())
//                .addField("unit_id", tstarHelper.createVehicle())
//                .addField("campaign_id", tstarHelper.createCargo())
//                .addField("position", tstarHelper.createLastActivity())
//                .addField("raw_packet", tstarHelper.createDeleted())
                .build();

        // set encoding to CSV
        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }

    public void parse(MessageLog messageLog) {

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = System.currentTimeMillis() / 1000;
        setMessageLogTime(messageLog);

        dataBlock.setIntValue(0, messageLog.id);
        dataBlock.setLongValue(1, msgLogTimestamp);
        dataBlock.setLongValue(2, msgLogDelivered);
        dataBlock.setStringValue(3, messageLog.in);
        dataBlock.setStringValue(4, messageLog.channel);
//        dataBlock.setStringValue();
//        dataBlock.setStringValue(7, campaign.cargo);
//        dataBlock.setLongValue(8, campaignLastActivity);
//        dataBlock.setBooleanValue(9, campaign.deleted);

        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(latestRecordTime, TSTARMessageLogOutput.this, dataBlock));
    }

    public void setMessageLogTime(MessageLog messageLog) {
        // parse UTC  to epoch time
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        if (messageLog.timestamp != null) {
            Date messageLogTimestamp = messageLog.timestamp;
            msgLogTimestamp = messageLogTimestamp.getTime() / 1000;
        } else {
            msgLogTimestamp = 0L;
        }
        if (messageLog.delivered != null) {
            Date messageLogDelivered = messageLog.delivered;
            msgLogDelivered = messageLogDelivered.getTime() / 1000;
        } else {
            msgLogDelivered = 0L;
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
