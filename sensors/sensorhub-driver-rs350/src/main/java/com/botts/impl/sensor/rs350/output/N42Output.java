package com.botts.impl.sensor.rs350.output;

import com.botts.impl.sensor.rs350.MessageHandler;
import com.botts.impl.sensor.rs350.RS350Sensor;
import com.botts.impl.sensor.rs350.messages.RS350Message;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

public class N42Output extends AbstractSensorOutput<RS350Sensor> {

    private static final RADHelper radHelper = new RADHelper();
    private static final Text n42Report = radHelper.createN42Report();

    private static final String SENSOR_OUTPUT_NAME = n42Report.getName();
    private static final String SENSOR_OUTPUT_LABEL = n42Report.getLabel();

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public N42Output(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        var samplingTime = radHelper.createPrecisionTimeStamp();
        var n42Output = n42Report.copy();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(n42Output.getDefinition())
                .addField(samplingTime.getName(), samplingTime)
                .addField(n42Output.getName(), n42Output)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }


    public void onNewMessage(String n42Message) {
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }


        latestRecordTime = System.currentTimeMillis() / 1000;

        int index = 0;
        dataBlock.setLongValue(index++, latestRecordTime);
        dataBlock.setStringValue(index, n42Message);

        eventHandler.publish(new DataEvent(latestRecordTime, N42Output.this, dataBlock));
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