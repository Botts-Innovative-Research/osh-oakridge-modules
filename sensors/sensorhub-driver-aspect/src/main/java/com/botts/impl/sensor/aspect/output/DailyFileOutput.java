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

public class DailyFileOutput extends AbstractSensorOutput<AspectSensor> {

    private static final String SENSOR_OUTPUT_NAME = "dailyFile";
    private static final String SENSOR_OUTPUT_LABEL = "Daily File";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    ArrayList<String> dailyfile;

    public DailyFileOutput(AspectSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var monitorRegisters = radHelper.createMonitorRegister();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("gamma-count"))
                .addField(samplingTime.getName(), samplingTime)
                .addField("Monitor Registers", radHelper.createText())
                .addField(monitorRegisters.getName(), monitorRegisters)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void getDailyFile(MonitorRegisters monitorRegisters){
        dailyfile = new ArrayList<>();

        dailyfile.add(String.valueOf(monitorRegisters.getInputSignals()));
        dailyfile.add(String.valueOf(monitorRegisters.getGammaChannelStatus()));
        dailyfile.add(String.valueOf(monitorRegisters.getGammaChannelCount()));
        dailyfile.add(String.valueOf(monitorRegisters.getGammaChannelBackground()));
        dailyfile.add(String.valueOf(monitorRegisters.getGammaChannelVariance()));
        dailyfile.add(String.valueOf(monitorRegisters.getGammaVarianceBackground()));
        dailyfile.add(String.valueOf(monitorRegisters.getNeutronChannelStatus()));
        dailyfile.add(String.valueOf(monitorRegisters.getNeutronChannelCount()));
        dailyfile.add(String.valueOf(monitorRegisters.getNeutronChannelBackground()));
        dailyfile.add(String.valueOf(monitorRegisters.getNeutronChannelVariance()));
        dailyfile.add(String.valueOf(monitorRegisters.getNeutronVarianceBackground()));
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
        long timeStamp = System.currentTimeMillis();

        String[] msgNames = new String[]{
                "inputSignals",
                "gammaChannelStatus",
                "gammaChannelCount",
                "gammaChannelBackground",
                "gammaChannelVariance",
                "gammaChannelVariance/Background",
                "neutronChannelStatus",
                "neutronChannelCount",
                "neutronChannelBackground",
                "neutronChannelVariance",
                "neutronChannelVariance/Background",
                "objectNumber",
                "WheelsNumber",
                "Velocity",
                "Output Signals"
        };
        dataBlock.setLongValue(0, timeStamp);
        dataBlock.setStringValue(1, Arrays.toString(msgNames));
        dataBlock.setStringValue(2, String.valueOf(dailyfile));

        eventHandler.publish(new DataEvent(timeStamp, DailyFileOutput.this, dataBlock));
        dailyfile.clear();
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
