package com.botts.impl.sensor.rapiscan;

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

public class TamperOutput  extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "Tamper";

    private static final Logger logger = LoggerFactory.getLogger(TamperOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public TamperOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init(){
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Tamper")
                .definition(RADHelper.getRadUri("tamper"))
                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
                .addField("TamperState", radHelper.createTamperStatus())
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

    }

    public void onNewMessage(boolean tamperState){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setLongValue(0, System.currentTimeMillis()/1000);
        dataBlock.setBooleanValue(1, tamperState);


        eventHandler.publish(new DataEvent(System.currentTimeMillis(), TamperOutput.this, dataBlock));

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
