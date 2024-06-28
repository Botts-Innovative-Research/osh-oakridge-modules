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

public class GammaOutput extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Gamma Scan";

    private static final Logger logger = LoggerFactory.getLogger(GammaOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;


    GammaOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        dataStruct = createDataRecord();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }
    DataRecord createDataRecord(){
        dataEncoding = new TextEncodingImpl(",", "\n");
        RADHelper radHelper = new RADHelper();

//        DataRecord gammaRecord = radHelper.createRecord()
//                .addField("LaneID", radHelper.createLaneId())
//                .addField("AlarmState", radHelper.createCategory()
//                        .name("Alarm")
//                        .label("Alarm")
//                        .definition(RADHelper.getRadUri("alarm"))
//                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Gamma High", "Fault - Gamma Low"))
//                .addField("GammaGrossCount1", radHelper.createCount().name("GammaGrossCount")
//                        .label("Gamma Gross Count 1")
//                        .definition(radHelper.getRadUri("gamma-gross-count")))
//                .addField("GammaGrossCount2", radHelper.createCount().name("GammaGrossCount")
//                        .label("Gamma Gross Count 2")
//                        .definition(radHelper.getRadUri("gamma-gross-count")))
//                .addField("GammaGrossCount3", radHelper.createCount().name("GammaGrossCount")
//                        .label("Gamma Gross Count 3")
//                        .definition(radHelper.getRadUri("gamma-gross-count")))
//                .addField("GammaGrossCount4", radHelper.createCount().name("GammaGrossCount")
//                        .label("Gamma Gross Count 4")
//                        .definition(radHelper.getRadUri("gamma-gross-count")))
//                .build();

//        for(int i=1; i < parent.gammaCount+1; i++){
//            gammaRecord.addField("GammaGrossCount "+ i, radHelper.createCount().name("GammaGrossCount")
//                    .label("Gamma Gross Count "+ i)
//                    .definition(radHelper.getRadUri("gamma-gross-count")).build());
//        }

        DataRecord recordBuilder = radHelper.createRecord()
                .name(name)
                .label("Gamma Scan")
                .updatable(true)
                .definition(RADHelper.getRadUri("gamma-scan"))
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("LaneName", radHelper.createLaneId())
                .addField("AlarmState", radHelper.createCategory()
                        .name("Alarm")
                        .label("Alarm")
                        .definition(RADHelper.getRadUri("alarm"))
                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Gamma High", "Fault - Gamma Low"))
                .addField("GammaGrossCount1", radHelper.createCount().name("GammaGrossCount")
                        .label("Gamma Gross Count 1")
                        .definition(radHelper.getRadUri("gamma-gross-count")))
                .addField("GammaGrossCount2", radHelper.createCount().name("GammaGrossCount")
                        .label("Gamma Gross Count 2")
                        .definition(radHelper.getRadUri("gamma-gross-count")))
                .addField("GammaGrossCount3", radHelper.createCount().name("GammaGrossCount")
                        .label("Gamma Gross Count 3")
                        .definition(radHelper.getRadUri("gamma-gross-count")))
                .addField("GammaGrossCount4", radHelper.createCount().name("GammaGrossCount")
                        .label("Gamma Gross Count 4")
                        .definition(radHelper.getRadUri("gamma-gross-count")))
                .build();

        return recordBuilder;

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
        dataBlock.setStringValue(index++, parent.laneName);
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
