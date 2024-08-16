package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.tstar.responses.UnitLog;
import org.vast.swe.SWEHelper;

import java.text.SimpleDateFormat;
import java.util.Date;


public class TSTARUnitLogOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Unit Log Output";
    protected DataRecord dataStruct;
    DataEncoding dataEncoding;
    DataBlock dataBlock;
    Long unitLogTimestamp;

    public TSTARUnitLogOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        TSTARHelper tstarHelper = new TSTARHelper();

        // SWE Common data structure
        dataStruct = tstarHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("UnitLogData"))
                .addField("id",tstarHelper.createUnitLogId())
                .addField("unit_id", tstarHelper.createUnitId())
                .addField("timestamp", tstarHelper.createTimestamp())
                .addField("level", tstarHelper.createLevel())
                .addField("msg", tstarHelper.createUnitLogMsg())
                .build();

        // set encoding to CSV
        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }

    public void parse(UnitLog unitLog) {

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = System.currentTimeMillis() / 1000;
        setUnitLogTime(unitLog);

        dataBlock.setIntValue(0, unitLog.id);
        dataBlock.setIntValue(1, unitLog.unit_id);
        dataBlock.setLongValue(2, unitLogTimestamp);
        dataBlock.setStringValue(3,unitLog.level);
        dataBlock.setStringValue(4, unitLog.msg);

        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(latestRecordTime, TSTARUnitLogOutput.this, dataBlock));
    }

    public void setUnitLogTime(UnitLog unitLog){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        Date timestamp =unitLog.timestamp;
        unitLogTimestamp = timestamp.getTime()/ 1000;
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
