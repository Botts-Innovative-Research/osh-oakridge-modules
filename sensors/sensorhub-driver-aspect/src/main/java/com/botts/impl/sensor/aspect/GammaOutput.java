package com.botts.impl.sensor.aspect;

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

import java.util.List;

public class GammaOutput extends AbstractSensorOutput<AspectSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Gamma Scan";

    private static final Logger logger = LoggerFactory.getLogger(GammaOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    GammaOutput(AspectSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Gamma Scan")
                .definition(RADHelper.getRadUri("gamma-scan"))
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("Gamma1", radHelper.createGammaGrossCount())
                .addField("Gamma2", radHelper.createGammaGrossCount())
                .addField("Gamma3", radHelper.createGammaGrossCount())
                .addField("Gamma4", radHelper.createGammaGrossCount())
                .addField("Alarm State",
                        radHelper.createCategory()
                                .name("Alarm")
                                .label("Alarm")
                                .definition(RADHelper.getRadUri("alarm"))
                                .addAllowedValues("Alarm", "Background", "Scan", "Fault - Gamma High", "Fault - Gamma Low"))
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");


    }

    public void onNewMessage(String[] csvString, long timeStamp, String alarmState){

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setLongValue(0,timeStamp/1000);
        dataBlock.setIntValue(1, Integer.parseInt(csvString[1]));
        dataBlock.setIntValue(2, Integer.parseInt(csvString[2]));
        dataBlock.setIntValue(3, Integer.parseInt(csvString[3]));
        dataBlock.setIntValue(4, Integer.parseInt(csvString[4]));
        dataBlock.setStringValue(5, alarmState);

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
