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

public class TamperOutput  extends AbstractSensorOutput<RapiscanSensor> {

    public static final String SENSOR_OUTPUT_NAME = "tamper";
    public static final String SENSOR_OUTPUT_LABEL = "Tamper";

    private static final Logger logger = LoggerFactory.getLogger(TamperOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public TamperOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init(){
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();
        var tamperState = radHelper.createTamperStatus();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("tamper"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
                .addField(tamperState.getName(), tamperState)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(boolean tamperState){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        int index = 0;

        dataBlock.setLongValue(index++, System.currentTimeMillis()/1000);
        dataBlock.setStringValue(index++, parent.laneID);
        dataBlock.setBooleanValue(index++, tamperState);

        //dataBlock.updateAtomCount();
        latestRecord = dataBlock;
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
