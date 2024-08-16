package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.tstar.responses.PositionLog;
import org.vast.swe.SWEHelper;
import java.lang.Boolean;
import java.text.SimpleDateFormat;
import java.util.Date;


public class TSTARPositionOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Position Output";
    protected DataRecord dataStruct;
    DataEncoding dataEncoding;
    DataBlock dataBlock;
    Long positionGeneratedTimestamp;
    Long positionReceivedTimestamp;

    public TSTARPositionOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        TSTARHelper tstarHelper = new TSTARHelper();

        // SWE Common data structure
        dataStruct = tstarHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("PositionData"))
                .addField("id", tstarHelper.createPositionLogId())
                .addField("unit-id", tstarHelper.createUnitId())
                .addField("location", tstarHelper.createLocationVectorLatLon())
                .addField("position", tstarHelper.createPosition())
                .addField("course", tstarHelper.createCourse())
                .addField("speed", tstarHelper.createSpeed())
                .addField("generated_timestamp", tstarHelper.createGeneratedTimestamp())
                .addField("received_timestamp", tstarHelper.createReceivedTimestamp())
                .addField("campaign-id", tstarHelper.createCampaignId())
                .addField("channel", tstarHelper.createChannel())
                .build();

        // set encoding to CSV
        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }
    public void parse(PositionLog position) {

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {
            dataBlock = latestRecord.renew();
        }
        latestRecordTime = System.currentTimeMillis() / 1000;
        setPositionTimes(position);

            dataBlock.setIntValue(0, position.id);
            dataBlock.setIntValue(1, position.unit_id);
            dataBlock.setDoubleValue(2, position.latitude);
            dataBlock.setDoubleValue(3, position.longitude);
            dataBlock.setStringValue(4, position.position);
            dataBlock.setIntValue(5, position.course);
            dataBlock.setIntValue(6, position.speed);
            dataBlock.setLongValue(7, positionGeneratedTimestamp);
            dataBlock.setLongValue(8, positionReceivedTimestamp);
            dataBlock.setStringValue(9, position.channel);

            // update latest record and send event
            latestRecord = dataBlock;
            eventHandler.publish(new DataEvent(latestRecordTime, TSTARPositionOutput.this, dataBlock));

    }
    public void setPositionTimes(PositionLog position){
        // parse UTC  to epoch time for 'generated_timestamp' and 'received_timestamp'
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        Date generatedTimestamp = position.generated_timestamp;
        positionGeneratedTimestamp = generatedTimestamp.getTime()/ 1000;

        Date receivedTimestamp = position.received_timestamp;
        positionReceivedTimestamp = receivedTimestamp.getTime() / 1000;
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