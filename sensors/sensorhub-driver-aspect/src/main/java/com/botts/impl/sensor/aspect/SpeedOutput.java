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

public class SpeedOutput extends AbstractSensorOutput<AspectSensor> {

    private static final String SENSOR_OUTPUT_NAME = "Speed";


    private static final Logger logger = LoggerFactory.getLogger(SpeedOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public SpeedOutput(AspectSensor parentSensor){super(SENSOR_OUTPUT_NAME, parentSensor);}

    protected void init(){
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Speed")
                .definition(RADHelper.getRadUri("speed"))
                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
                .addField("Mph", radHelper.createQuantity().name("mph").label("Mph").definition(RADHelper.getPropertyUri("mph")).uomCode("mph"))
                .addField("Kph", radHelper.createQuantity().name("kph").label("Kph").definition(RADHelper.getPropertyUri("kph")).uomCode("kph"))
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

    }

    public void onNewMessage(String[] csvString, long timeStamp){

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setLongValue(0,timeStamp/1000);
        dataBlock.setDoubleValue(1, Double.parseDouble(csvString[1]));
        dataBlock.setDoubleValue(2, Double.parseDouble(csvString[2]));

        eventHandler.publish(new DataEvent(timeStamp, SpeedOutput.this, dataBlock));


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
