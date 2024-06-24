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
import org.vast.data.*;
import org.vast.swe.SWEBuilders;


public class NeutronOutput extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Neutron Scan";

    private static final Logger logger = LoggerFactory.getLogger(NeutronOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public NeutronOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        RADHelper radHelper = new RADHelper();

//        DataRecord neutronRecord = radHelper.createRecord()
//                .addField("LaneID", radHelper.createLaneId())
//                .addField("AlarmState", radHelper.createCategory()
//                        .name("Alarm")
//                        .label("Alarm")
//                        .definition(RADHelper.getRadUri("alarm"))
//                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High"))
//                .addField("NeutronGrossCount1", radHelper.createCount()
//                        .name("NeutronGrossCount")
//                        .label("Neutron Gross Count 1")
//                        .definition(radHelper.getRadUri("neutron-gross-count")))
//                .addField("NeutronGrossCount2", radHelper.createCount()
//                        .name("NeutronGrossCount")
//                        .label("Neutron Gross Count 2")
//                        .definition(radHelper.getRadUri("neutron-gross-count")))
//                .addField("NeutronGrossCount3", radHelper.createCount()
//                        .name("NeutronGrossCount")
//                        .label("Neutron Gross Count 3")
//                        .definition(radHelper.getRadUri("neutron-gross-count")))
//                .addField("NeutronGrossCount4", radHelper.createCount()
//                        .name("NeutronGrossCount")
//                        .label("Neutron Gross Count 4")
//                        .definition(radHelper.getRadUri("neutron-gross-count")))
//
//                .build();

//        for(int i=1; i<parent.neutronCount+1; i++){
//            neutronRecord.addField("NeutronGrossCount "+ i, radHelper.createCount().name("NeutronGrossCount")
//                    .label("Neutron Gross Count "+i)
//                    .definition(radHelper.getRadUri("neutron-gross-count")).build());
//        }

        DataRecord recordBuilder = radHelper.createRecord()
                .name(name)
                .label("Neutron Scan")
                .updatable(true)
                .definition(RADHelper.getRadUri("neutron-scan"))
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("LaneID", radHelper.createLaneId())
                .addField("AlarmState", radHelper.createCategory()
                        .name("Alarm")
                        .label("Alarm")
                        .definition(RADHelper.getRadUri("alarm"))
                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High"))
                .addField("NeutronGrossCount1", radHelper.createCount()
                        .name("NeutronGrossCount")
                        .label("Neutron Gross Count 1")
                        .definition(radHelper.getRadUri("neutron-gross-count")))
                .addField("NeutronGrossCount2", radHelper.createCount()
                        .name("NeutronGrossCount")
                        .label("Neutron Gross Count 2")
                        .definition(radHelper.getRadUri("neutron-gross-count")))
                .addField("NeutronGrossCount3", radHelper.createCount()
                        .name("NeutronGrossCount")
                        .label("Neutron Gross Count 3")
                        .definition(radHelper.getRadUri("neutron-gross-count")))
                .addField("NeutronGrossCount4", radHelper.createCount()
                        .name("NeutronGrossCount")
                        .label("Neutron Gross Count 4")
                        .definition(radHelper.getRadUri("neutron-gross-count")))
//                .addField("Neutron Scan", neutronRecord)
                .build();
        dataEncoding = radHelper.newTextEncoding(",", "\n");
        dataStruct =recordBuilder;
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
        dataBlock.setIntValue(index++, parent.laneId);
        dataBlock.setStringValue(index++, alarmState);

        dataBlock.setIntValue(index++, Integer.parseInt(csvString[1]));
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[2]));
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[3]));
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[4]));
//        for(int i=1; i< csvString.length; i++){
//            dataBlock.setIntValue(index++, Integer.parseInt(csvString[i]));
//        }

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
