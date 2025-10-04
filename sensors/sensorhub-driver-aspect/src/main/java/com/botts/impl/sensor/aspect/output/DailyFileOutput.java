package com.botts.impl.sensor.aspect.output;

import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.registers.MonitorRegisters;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.checkerframework.checker.units.qual.A;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DailyFileOutput extends AbstractSensorOutput<AspectSensor> {

    private static final String SENSOR_OUTPUT_NAME = "dailyFile";
    private static final String SENSOR_OUTPUT_LABEL = "Daily File";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    List<String> dailyfile = new ArrayList<>();
    StringBuilder dailyFileString = new StringBuilder();

    public DailyFileOutput(AspectSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var aspectMessage = radHelper.createAspectMessageFile();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("DailyFile"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(aspectMessage.getName(), aspectMessage)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void getDailyFile(MonitorRegisters monitorRegisters){
        dailyfile.clear();

        dailyfile.add(String.valueOf(monitorRegisters.getTimeElapsed()));
        dailyfile.add(String.valueOf(monitorRegisters.getInputSignals()));
        dailyfile.add(String.valueOf(monitorRegisters.getGammaChannelStatus()));
        dailyfile.add(String.valueOf(monitorRegisters.getGammaChannelCount()));
        dailyfile.add(String.valueOf(monitorRegisters.getGammaChannelBackground()));
        dailyfile.add(String.valueOf(monitorRegisters.getGammaChannelVariance()));
        dailyfile.add(String.valueOf(monitorRegisters.getNeutronChannelStatus()));
        dailyfile.add(String.valueOf(monitorRegisters.getNeutronChannelCount()));
        dailyfile.add(String.valueOf(monitorRegisters.getNeutronChannelBackground()));
        dailyfile.add(String.valueOf(monitorRegisters.getNeutronChannelVariance()));
        dailyfile.add(String.valueOf(monitorRegisters.getObjectCounter()));
        dailyfile.add(String.valueOf(monitorRegisters.getObjectMark()));
        dailyfile.add(String.valueOf(monitorRegisters.getObjectSpeed()));
        dailyfile.add(String.valueOf(monitorRegisters.getOutputSignals()));
    }
    public void onNewMessage() {
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }
        long timeStamp = System.currentTimeMillis()/1000;

//        System.out.println("Daily File: " + dailyfile);

        for(int i=0; i < dailyfile.size(); i++){

            dailyFileString.append(dailyfile.get(i));
            if(i< dailyfile.size()-1){
                dailyFileString.append(",");
            }
        }
        dataBlock.setLongValue(0, timeStamp);
        dataBlock.setStringValue(1, String.valueOf(dailyFileString));


        eventHandler.publish(new DataEvent(timeStamp, DailyFileOutput.this, dataBlock));
        dailyFileString.setLength(0);
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
