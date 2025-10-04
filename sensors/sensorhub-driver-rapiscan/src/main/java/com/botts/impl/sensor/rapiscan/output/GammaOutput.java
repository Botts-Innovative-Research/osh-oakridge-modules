package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

import java.util.Objects;

public class GammaOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "gammaCounts";
    private static final String SENSOR_OUTPUT_LABEL = "Gamma Counts";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public GammaOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var alarmState = radHelper.createGammaAlarmState();

        var grossCount = radHelper.createGammaGrossCount();

        // GB, GH, GL lines and 1 second GS/GA lines
        var count1 = radHelper.createGammaCount(1);
        var count2 = radHelper.createGammaCount(2);
        var count3 = radHelper.createGammaCount(3);
        var count4 = radHelper.createGammaCount(4);

        // GA/GS lines (treated the same)
        var countPerInterval1 = radHelper.createGammaCountPerInterval(1);
        var countPerInterval2 = radHelper.createGammaCountPerInterval(2);
        var countPerInterval3 = radHelper.createGammaCountPerInterval(3);
        var countPerInterval4 = radHelper.createGammaCountPerInterval(4);

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("Gamma"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(alarmState.getName(), alarmState)
                .addField(grossCount.getName(), grossCount)
                .addField(count1.getName(), count1)
                .addField(count2.getName(), count2)
                .addField(count3.getName(), count3)
                .addField(count4.getName(), count4)
                .addField(countPerInterval1.getName(), countPerInterval1)
                .addField(countPerInterval2.getName(), countPerInterval2)
                .addField(countPerInterval3.getName(), countPerInterval3)
                .addField(countPerInterval4.getName(), countPerInterval4)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(String[] csvString, long timeStamp, String alarmState, int[] foregroundCountsPerSecond) {
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        dataBlock.setLongValue(0,timeStamp/1000);
        dataBlock.setStringValue(1, alarmState);

        String mainChar = csvString[0];
        if(Objects.equals(mainChar, "GA") || Objects.equals(mainChar, "GS")) {
            if(foregroundCountsPerSecond != null) {
                dataBlock.setIntValue(2, foregroundCountsPerSecond[0]
                + foregroundCountsPerSecond[1]
                + foregroundCountsPerSecond[2]
                + foregroundCountsPerSecond[3]);
                dataBlock.setIntValue(3, foregroundCountsPerSecond[0]);
                dataBlock.setIntValue(4, foregroundCountsPerSecond[1]);
                dataBlock.setIntValue(5, foregroundCountsPerSecond[2]);
                dataBlock.setIntValue(6, foregroundCountsPerSecond[3]);
            }
            dataBlock.setIntValue(7, Integer.parseInt(csvString[1]));
            dataBlock.setIntValue(8, Integer.parseInt(csvString[2]));
            dataBlock.setIntValue(9, Integer.parseInt(csvString[3]));
            dataBlock.setIntValue(10, Integer.parseInt(csvString[4]));
        } else {
            dataBlock.setIntValue(2,
                    Integer.parseInt(csvString[1])
                            + Integer.parseInt(csvString[2])
                            + Integer.parseInt(csvString[3])
                            + Integer.parseInt(csvString[4]));
            dataBlock.setIntValue(3, Integer.parseInt(csvString[1]));
            dataBlock.setIntValue(4, Integer.parseInt(csvString[2]));
            dataBlock.setIntValue(5, Integer.parseInt(csvString[3]));
            dataBlock.setIntValue(6, Integer.parseInt(csvString[4]));
        }

        eventHandler.publish(new DataEvent(timeStamp, GammaOutput.this, dataBlock));
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
