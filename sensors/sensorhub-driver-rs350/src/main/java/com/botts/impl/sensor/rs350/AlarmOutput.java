package com.botts.impl.sensor.rs350;

import com.botts.impl.sensor.rs350.messages.RS350Message;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class AlarmOutput extends OutputBase {

    private static final String SENSOR_OUTPUT_NAME = "RS350 Alarm";

    private static final Logger logger = LoggerFactory.getLogger(AlarmOutput.class);

    public AlarmOutput(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    protected void init() {
        //TODO: Import N42Helper
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Alarm")
                .definition(RADHelper.getRadUri("Alarm-output"))
                // Derived Data
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("Duration",
                        radHelper.createQuantity()
                                .name("Duration")
                                .label("Duration")
                                .definition(RADHelper.getRadUri("duration")))
                .addField("Remark",
                        radHelper.createText()
                                .name("Remark")
                                .label("Remark")
                                .definition(RADHelper.getRadUri("alarm-remark")))
                .addField("MeasurementClassCode", radHelper.createMeasurementClassCode())
                // RAD Alarm
                .addField("AlarmCategoryCode", radHelper.createAlarmCatCode())
                .addField("AlarmDescription",
                        radHelper.createText()
                                .name("AlarmDescription")
                                .label("Alarm Description")
                                .definition(RADHelper.getRadUri("alarm-description")))
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

//    public void parseData(RS350Message msg) {
//        if (latestRecord == null)
//            dataBlock = dataStruct.createDataBlock();
//        else
//            dataBlock = latestRecord.renew();
//
//        latestRecordTime = System.currentTimeMillis() / 1000;
//
//        dataBlock.setLongValue(0, msg.getRs350DerivedData().getStartDateTime() / 1000);
//        dataBlock.setDoubleValue(1, msg.getRs350DerivedData().getDuration());
//        dataBlock.setStringValue(2, msg.getRs350DerivedData().getRemark());
//        dataBlock.setStringValue(3, msg.getRs350DerivedData().getClassCode());
//        dataBlock.setStringValue(4, msg.getRs350RadAlarm().getCategoryCode());
//        dataBlock.setStringValue(5, msg.getRs350RadAlarm().getDescription());
//
//        eventHandler.publish(new DataEvent(latestRecordTime, AlarmOutput.this, dataBlock));
//    }


    @Override
    public void onNewMessage(RS350Message message) {

        if (message.getRs350RadAlarm() != null) {

            createOrRenewDataBlock();

            latestRecordTime = System.currentTimeMillis() / 1000;

            dataBlock.setLongValue(0, message.getRs350DerivedData().getStartDateTime() / 1000);
            dataBlock.setDoubleValue(1, message.getRs350DerivedData().getDuration());
            dataBlock.setStringValue(2, message.getRs350DerivedData().getRemark());
            dataBlock.setStringValue(3, message.getRs350DerivedData().getClassCode());
            dataBlock.setStringValue(4, message.getRs350RadAlarm().getCategoryCode());
            dataBlock.setStringValue(5, message.getRs350RadAlarm().getDescription());

            eventHandler.publish(new DataEvent(latestRecordTime, AlarmOutput.this, dataBlock));
        }
    }
}
