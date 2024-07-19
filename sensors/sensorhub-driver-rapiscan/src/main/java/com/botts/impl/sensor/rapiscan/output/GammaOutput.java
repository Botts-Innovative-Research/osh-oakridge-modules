package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class GammaOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "gammaScan";
    private static final String SENSOR_OUTPUT_LABEL = "Gamma Scan";

    private static final Logger logger = LoggerFactory.getLogger(GammaOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;


    public GammaOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var alarmState = radHelper.createGammaAlarmState();
        var count1 = radHelper.createGammaGrossCount(1);
        var count2 = radHelper.createGammaGrossCount(2);
        var count3 = radHelper.createGammaGrossCount(3);
        var count4 = radHelper.createGammaGrossCount(4);

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("gamma-scan"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(alarmState.getName(), alarmState)
                .addField(count1.getName(), count1)
                .addField(count2.getName(), count2)
                .addField(count3.getName(), count3)
                .addField(count4.getName(), count4)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(String[] csvString, long timeStamp, String alarmState){

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        int index =0;

        dataBlock.setLongValue(index++,timeStamp/1000);
        dataBlock.setStringValue(index++, alarmState);

        dataBlock.setIntValue(index++, Integer.parseInt(csvString[1]));
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[2]));
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[3]));
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[4]));

//        for(int i=1; i< csvString.length; i++){
//            dataBlock.setIntValue(index++, Integer.parseInt(csvString[i]));
//        }

        latestRecord = dataBlock;
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
