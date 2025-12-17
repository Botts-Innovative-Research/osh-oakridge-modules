package com.botts.impl.sensor.aspect.output;

import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.registers.MonitorRegisters;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

import java.time.Instant;

public class DailyFileOutput extends AbstractSensorOutput<AspectSensor> {

    private static final String SENSOR_OUTPUT_NAME = "dailyFile";
    private static final String SENSOR_OUTPUT_LABEL = "Daily File";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    String dailyFile;

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
        dailyFile = String.join(",",
                String.valueOf(monitorRegisters.getTimeElapsed()),
                String.valueOf(monitorRegisters.getInputSignals()),
                String.valueOf(monitorRegisters.getGammaChannelStatus()),
                String.valueOf(monitorRegisters.getGammaChannelCount()),
                String.valueOf(monitorRegisters.getGammaChannelBackground()),
                String.valueOf(monitorRegisters.getGammaChannelVariance()),
                String.valueOf(monitorRegisters.getNeutronChannelStatus()),
                String.valueOf(monitorRegisters.getNeutronChannelCount()),
                String.valueOf(monitorRegisters.getNeutronChannelBackground()),
                String.valueOf(monitorRegisters.getNeutronChannelVariance()),
                String.valueOf(monitorRegisters.getObjectCounter()),
                String.valueOf(monitorRegisters.getObjectMark()),
                String.valueOf(monitorRegisters.getObjectSpeed()),
                String.valueOf(monitorRegisters.getOutputSignals())
        );
    }
    public void onNewMessage() {
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

//        System.out.println("Daily File: " + dailyfile);

        Instant timeStamp = Instant.now();
        dataBlock.setTimeStamp(0, timeStamp);
        dataBlock.setStringValue(1, dailyFile);


        eventHandler.publish(new DataEvent(timeStamp.toEpochMilli(), DailyFileOutput.this, dataBlock));
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
