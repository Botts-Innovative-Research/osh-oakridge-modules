package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;

public class NeutronOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "neutronCounts";
    private static final String SENSOR_OUTPUT_LABEL = "Neutron Counts";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public NeutronOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var alarmState = radHelper.createNeutronAlarmState();
        var grossCount = radHelper.createNeutronGrossCount();
        var count1 = radHelper.createNeutronCount(1);
        var count2 = radHelper.createNeutronCount(2);
        var count3 = radHelper.createNeutronCount(3);
        var count4 = radHelper.createNeutronCount(4);

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("neutron-counts"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(alarmState.getName(), alarmState)
                .addField(grossCount.getName(), grossCount)
                .addField(count1.getName(), count1)
                .addField(count2.getName(), count2)
                .addField(count3.getName(), count3)
                .addField(count4.getName(), count4)
                .build();

        dataEncoding = radHelper.newTextEncoding(",", "\n");
    }

    public void onNewMessage(String[] csvString, long timeStamp, String alarmState){

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        int index =0;

        int c1 = Integer.parseInt(csvString[1]);
        int c2 = Integer.parseInt(csvString[2]);
        int c3 = Integer.parseInt(csvString[3]);
        int c4 = Integer.parseInt(csvString[4]);

        dataBlock.setLongValue(index++,timeStamp/1000);
        dataBlock.setStringValue(index++, alarmState);
        dataBlock.setIntValue(index++, c1 + c2 + c3 + c4);
        dataBlock.setIntValue(index++, c1);
        dataBlock.setIntValue(index++, c2);
        dataBlock.setIntValue(index++, c3);
        dataBlock.setIntValue(index, c4);

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(timeStamp, NeutronOutput.this, dataBlock));
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
