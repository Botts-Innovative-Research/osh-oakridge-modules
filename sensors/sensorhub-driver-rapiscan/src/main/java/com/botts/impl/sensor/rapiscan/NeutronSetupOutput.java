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

public class NeutronSetupOutput extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Neutron Setup 1";

    private static final Logger logger = LoggerFactory.getLogger(NeutronSetupOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    protected NeutronSetupOutput(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init(){
        dataStruct = createDataRecord();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }
    DataRecord createDataRecord(){
        RADHelper radHelper = new RADHelper();

        DataRecord recordBuilder = radHelper.createRecord()
                .name(getName())
                .label("Setup Neutron 1")
                .definition(RADHelper.getRadUri("setup-neutron-1"))
                .addField("TimeStamp", radHelper.createPrecisionTimeStamp())
                .addField("LaneID", radHelper.createLaneId())
                .addField("high-background-fault", radHelper.createHighBackgroundFault())
                .addField("maximum-intervals", radHelper.createMaxIntervals())
                .addField("alpha-value", radHelper.createAlphaValue())
                .addField("zmax-value", radHelper.createZmaxValue())
                .addField("sequential-intervals", radHelper.createSequentialIntervals())
                .build();

        return recordBuilder;
    }

    public void onNewMessage(String[] csvString){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }
        int index =0;
        dataBlock.setLongValue(index++,System.currentTimeMillis()/1000);
        dataBlock.setIntValue(index++, parent.laneId);
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[1])); //high bg
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[2])); //max intervals
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[3])); //alpha val
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[4])); //zmax value
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[5])); //sequential intervals

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), NeutronSetupOutput.this, dataBlock));

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

