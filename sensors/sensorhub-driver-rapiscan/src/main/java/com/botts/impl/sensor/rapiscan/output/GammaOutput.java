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

import java.util.ArrayList;

public class GammaOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "gammaCount";
    private static final String SENSOR_OUTPUT_LABEL = "Gamma Count";

    private static final Logger logger = LoggerFactory.getLogger(GammaOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    int gsCount = 0;
    ArrayList<String> gsList1 = new ArrayList<>();
    ArrayList<String> gsList2= new ArrayList<>();
    ArrayList<String> gsList3= new ArrayList<>();
    ArrayList<String> gsList4 = new ArrayList<>();

    ArrayList<String> foreground = new ArrayList<>();
    int counts1;
    int counts2;
    int counts3;
    int counts4;

    public GammaOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var alarmState = radHelper.createGammaAlarmState();


        //GB lines
        var countCps1 = radHelper.createGammaCPS(1);
        var countCps2 = radHelper.createGammaCPS(2);
        var countCps3 = radHelper.createGammaCPS(3);
        var countCps4 = radHelper.createGammaCPS(4);

        //GA & GS lines (treated the same)
        var cp200ms1 = radHelper.createGammaCp200ms(1);
        var cp200ms2 = radHelper.createGammaCp200ms(2);
        var cp200ms3 = radHelper.createGammaCp200ms(3);
        var cp200ms4 = radHelper.createGammaCp200ms(4);

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("gamma-scan"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(alarmState.getName(), alarmState)
                .addField(countCps1.getName(), countCps1)
                .addField(countCps2.getName(), countCps2)
                .addField(countCps3.getName(), countCps3)
                .addField(countCps4.getName(), countCps4)

                .addField(cp200ms1.getName(), cp200ms1)
                .addField(cp200ms2.getName(), cp200ms2)
                .addField(cp200ms3.getName(), cp200ms3)
                .addField(cp200ms4.getName(), cp200ms4)
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

        if(csvString[0].startsWith("GS")|| csvString[0].startsWith("GA")){
            gsCount++;
            getGammaAvg(csvString);
        }

        dataBlock.setLongValue(0,timeStamp/1000);
        dataBlock.setStringValue(1, alarmState);

        //gamma cps background message
        if(csvString[0].startsWith("GB")){
            dataBlock.setIntValue(2, Integer.parseInt(csvString[1]));
            dataBlock.setIntValue(3, Integer.parseInt(csvString[2]));
            dataBlock.setIntValue(4, Integer.parseInt(csvString[3]));
            dataBlock.setIntValue(5, Integer.parseInt(csvString[4]));
        }

        //when gscount== 5 update the cps field  --> SUM(ga/gs)
        if(gsCount == 5){
            dataBlock.setIntValue(2, counts1);
            dataBlock.setIntValue(3, counts2);
            dataBlock.setIntValue(4, counts3);
            dataBlock.setIntValue(5, counts4);
            reset();
        }

        //update the cp200ms is string is gs or ga
        if(csvString[0].startsWith("GS") || csvString[0].startsWith("GA")){
            dataBlock.setIntValue(6, Integer.parseInt(csvString[1]));
            dataBlock.setIntValue(7, Integer.parseInt(csvString[2]));
            dataBlock.setIntValue(8, Integer.parseInt(csvString[3]));
            dataBlock.setIntValue(9, Integer.parseInt(csvString[4]));
        }


        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(timeStamp, GammaOutput.this, dataBlock));

    }
    public void getGammaAvg(String[] csvLine){

        //add each of the values and send to the output
        //do a running sum of the 5 values in each spot then send to the output!
        gsList1.add(csvLine[1].trim());
        gsList2.add(csvLine[2].trim());
        gsList3.add(csvLine[3].trim());
        gsList4.add(csvLine[4].trim());

        if(gsCount == 5){
            addCountsList();
        }

    }

    public void reset(){
        foreground.clear();
        gsList1.clear();
        gsList2.clear();
        gsList3.clear();
        gsList4.clear();
        gsCount = 0;
        counts1 = 0;
        counts2 = 0;
        counts3 = 0;
        counts4 = 0;

    }
    public void addCountsList() {

        for (String value : gsList1) {
            counts1 += Integer.parseInt(value.trim());
        }
        for (String value : gsList2) {
            counts2 += Integer.parseInt(value.trim());
        }
        for (String value : gsList3) {
            counts3 += Integer.parseInt(value.trim());
        }
        for (String value : gsList4) {
            counts4 += Integer.parseInt(value.trim());
        }

        foreground.add(String.valueOf(counts1).trim());
        foreground.add(String.valueOf(counts2).trim());
        foreground.add(String.valueOf(counts3).trim());
        foreground.add(String.valueOf(counts4).trim());
    }

    public ArrayList<String> getForeground(){
        return foreground;
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
