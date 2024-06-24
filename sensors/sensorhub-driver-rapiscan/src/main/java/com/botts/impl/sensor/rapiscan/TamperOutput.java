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
import org.vast.swe.SWEBuilders;

import javax.xml.crypto.Data;

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

//        DataRecord tamperRecord = radHelper.createRecord()
//                .addField("LaneID", radHelper.createLaneId())
//                .addField("TamperState", radHelper.createTamperStatus())
//                .build();
//
//        SWEBuilders.DataArrayBuilder arrayBuilder = radHelper.createArray()
//                .withVariableSize("numTamperRecords")
//                .withElement("Tamper", tamperRecord);

        DataRecord recordBuilder = radHelper.createRecord()
                .name(getName())
                .label("Tamper")
                .updatable(true)
                .definition(RADHelper.getRadUri("tamper"))
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("LaneID", radHelper.createLaneId())
                .addField("TamperState", radHelper.createTamperStatus())
//                .addField("Tamper", tamperRecord)
//                .addField("numTamperRecords", radHelper.createCount()
//                        .label("numTamperRecords")
//                        .id("numTamperRecords")
//                        .value(parent.rapiscanLayerConfig.presets.size()))
//                .addField("Tamper Records", arrayBuilder)
                .build();

        dataStruct =recordBuilder;
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
        dataBlock.setIntValue(index++, parent.laneId);
        dataBlock.setBooleanValue(index++, tamperState);

        dataBlock.updateAtomCount();
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
