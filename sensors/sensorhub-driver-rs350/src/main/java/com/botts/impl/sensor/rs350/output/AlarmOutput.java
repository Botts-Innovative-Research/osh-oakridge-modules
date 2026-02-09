package com.botts.impl.sensor.rs350.output;

import com.botts.impl.sensor.rs350.MessageHandler;
import com.botts.impl.sensor.rs350.RS350Sensor;
import com.botts.impl.sensor.rs350.messages.RS350Message;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

public class AlarmOutput extends AbstractSensorOutput<RS350Sensor> implements MessageHandler.MessageListener {

    private static final String SENSOR_OUTPUT_NAME = "alarm";
    private static final String SENSOR_OUTPUT_LABEL = "Alarm Output";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public AlarmOutput(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();
        var samplingTime = radHelper.createPrecisionTimeStamp();
        var duration = radHelper.createDuration();
        var alarmCategory = radHelper.createAlarmCatCode();
        var remark = radHelper.createRemark();
        var measurementClass = radHelper.createMeasurementClassCode();
        var alarmDescription = radHelper.createAlarmDescription();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("AlarmOutput"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(duration.getName(), duration)
                .addField(remark.getName(), remark)
                .addField(measurementClass.getName(), measurementClass)
                .addField(alarmCategory.getName(), alarmCategory)
                .addField(alarmDescription.getName(), alarmDescription)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(RS350Message message) {
        if (message.getRs350RadAlarm() == null || message.getRs350DerivedData() == null) {
            return;
        }

        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = System.currentTimeMillis() / 1000;

        dataBlock.setLongValue(0, message.getRs350DerivedData().getStartDateTime() / 1000);
        dataBlock.setDoubleValue(1, message.getRs350DerivedData().getDuration());
        dataBlock.setStringValue(2, message.getRs350DerivedData().getRemark());
        dataBlock.setStringValue(3, message.getRs350DerivedData().getClassCode());
        dataBlock.setStringValue(4, message.getRs350RadAlarm().getCategoryCode());
        dataBlock.setStringValue(5, message.getRs350RadAlarm().getDescription());

        eventHandler.publish(new DataEvent(latestRecordTime, AlarmOutput.this, dataBlock));
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
