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
                .addField("auditLogTimestamp", tstarHelper.createTimestamp())
                .addField("action", tstarHelper.createAction())
                .addField("sourceIp", tstarHelper.createSourceIp())
                .addField("userId", tstarHelper.createUserId())
                .addField("targetTable", tstarHelper.createTargetTable())
                .addField("targetId", tstarHelper.createTargetId())
                .addField("targetName", tstarHelper.createTargetName())
                .addField("auditLogData", tstarHelper.createAuditLogData())
                .addField("userName", tstarHelper.createUserName())
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

        int i = 0;
        dataBlock.setStringValue(i++, auditLog.id);
        dataBlock.setLongValue(i++, auditLogTimestamp);
        dataBlock.setStringValue(i++, auditLog.action);
        dataBlock.setStringValue(i++, auditLog.source_ip);
        dataBlock.setIntValue(i++, auditLog.user_id);
        dataBlock.setStringValue(i++, auditLog.target_table);
        dataBlock.setStringValue(i++, auditLog.target_id);
        dataBlock.setStringValue(i++, auditLog.target_name);
        try {
            dataBlock.setStringValue(i++, auditLog.data.id);
            dataBlock.setIntValue(i++, auditLog.data.campaign_id);
        } catch (NullPointerException e){}
        dataBlock.setStringValue(i++, auditLog.user_name);

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
