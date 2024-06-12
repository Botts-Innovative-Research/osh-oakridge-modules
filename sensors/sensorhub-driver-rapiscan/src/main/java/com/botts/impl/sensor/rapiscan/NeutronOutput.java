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

import java.util.List;

public class NeutronOutput extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Neutron Scan";

    private static final Logger logger = LoggerFactory.getLogger(NeutronOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;


    public NeutronOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        RADHelper radHelper = new RADHelper();
        SWEBuilders.DataRecordBuilder recordBuilder;
        recordBuilder = radHelper.createRecord()
                .name(name)
                .label("Neutron Scan")
                .definition(RADHelper.getRadUri("neutron-scan"))
                .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
                .addField("AlarmState", radHelper.createCategory()
                        .name("Alarm")
                        .label("Alarm")
                        .definition(RADHelper.getRadUri("alarm"))
                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High"));

        for(int i=1; i<parent.neutronCount+1; i++){
            recordBuilder.addField("NeutronGrossCount "+ i, radHelper.createCount().name("NeutronGrossCount")
                    .label("Neutron Gross Count "+i)
                    .definition(radHelper.getRadUri("neutron-gross-count")));
        }

        dataStruct = recordBuilder.build();


//        dataStruct = radHelper.createRecord()
//                .name(getName())
//                .label("Neutron Scan")
//                .definition(RADHelper.getRadUri("neutron-scan"))
//                .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
////                .addField("Neutron1", radHelper.createNeutronGrossCount())
////                .addField("Neutron2", radHelper.createNeutronGrossCount())
////                .addField("Neutron3", radHelper.createNeutronGrossCount())
////                .addField("Neutron4", radHelper.createNeutronGrossCount())
//                .addField("AlarmState",
//                        radHelper.createCategory()
//                                .name("Alarm")
//                                .label("Alarm")
//                                .definition(RADHelper.getRadUri("alarm"))
//                                .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High"))
//                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");


    }

    public void onNewMessage(String[] csvString, long timeStamp, String alarmState){

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setLongValue(0,timeStamp/1000);
        dataBlock.setStringValue(1, alarmState);
        for(int i=2; i< parent.neutronCount+2; i++){
            dataBlock.setIntValue(i, Integer.parseInt(csvString[i-1]));
        }
//        dataBlock.setIntValue(1, Integer.parseInt(csvString[1]));
//        dataBlock.setIntValue(2, Integer.parseInt(csvString[2]));
//        dataBlock.setIntValue(3, Integer.parseInt(csvString[3]));
//        dataBlock.setIntValue(4, Integer.parseInt(csvString[4]));


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
