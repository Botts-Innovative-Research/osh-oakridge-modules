package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;

public class DailyFileOutput extends org.sensorhub.impl.utils.rad.output.DailyFileOutput<RapiscanSensor> {

    public DailyFileOutput(RapiscanSensor parentSensor) {
        super(parentSensor);
    }
}
