package com.botts.impl.sensor.aspect.output;

import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.registers.MonitorRegisters;

import java.time.Instant;

public class DailyFileOutput extends org.sensorhub.impl.utils.rad.output.DailyFileOutput<AspectSensor> {

    String dailyFile;

    public DailyFileOutput(AspectSensor parentSensor){
        super(parentSensor);
    }

    public void getDailyFile(MonitorRegisters monitorRegisters){
        dailyFile = buildDailyFileLine(monitorRegisters);
    }

    public static String buildDailyFileLine(MonitorRegisters monitorRegisters) {
        return String.join(",",
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
        onNewMessage(dailyFile, Instant.now());
    }
}
