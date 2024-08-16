package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.tstar.responses.AuditLog;
import org.vast.swe.SWEHelper;

import java.text.SimpleDateFormat;
import java.util.Date;


public class TSTARAuditLogOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Audit Log Output";
    protected DataRecord dataStruct;
    DataEncoding dataEncoding;
    DataBlock dataBlock;
    Long auditLogTimestamp;

    public TSTARAuditLogOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        TSTARHelper tstarHelper = new TSTARHelper();

        // SWE Common data structure
        dataStruct = tstarHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("AuditLogData"))
                .addField("id", tstarHelper.createAuditLogId())
                .addField("timestamp", tstarHelper.createTimestamp())
                .addField("action", tstarHelper.createAction())
                .addField("source_ip", tstarHelper.createSourceIp())
                .addField("user_id", tstarHelper.createUserId())
                .addField("target_table", tstarHelper.createTargetTable())
                .addField("target_id", tstarHelper.createTargetId())
                .addField("target_name", tstarHelper.createTargetName())
                //data Object? : String id, int campaign_id
                .addField("user_name", tstarHelper.createUserName())
                .build();

        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }

    public void parse(AuditLog auditLog) {

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = System.currentTimeMillis() / 1000;
        setAuditLogTime(auditLog);

        dataBlock.setStringValue(0, auditLog.id);
        dataBlock.setLongValue(1, auditLogTimestamp);
        dataBlock.setStringValue(2, auditLog.action);
        dataBlock.setStringValue(3, auditLog.source_ip);
        dataBlock.setIntValue(4, auditLog.user_id);
        dataBlock.setStringValue(5, auditLog.target_table);
        dataBlock.setStringValue(6, auditLog.target_id);
        dataBlock.setStringValue(7, auditLog.target_name);
//      Object Data? : String id, int campaign_id
        dataBlock.setStringValue(8, auditLog.user_name);

        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(latestRecordTime, TSTARAuditLogOutput.this, dataBlock));
    }

    public void setAuditLogTime(AuditLog auditLog){
        // parse UTC  to epoch time for 'timestamp'
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        Date timestamp = auditLog.timestamp;
        auditLogTimestamp = timestamp.getTime()/ 1000;
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
