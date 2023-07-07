package com.botts.impl.sensor.rs350;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmOutput  extends OutputBase{

    private static final String SENSOR_OUTPUT_NAME = "RS350 Alarm";

    private static final Logger logger = LoggerFactory.getLogger(StatusOutput.class);

    public AlarmOutput(RS350Sensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    protected void init(){
        //TODO: Import N42Helper

        // OUTPUT

        // Start Date Time
        // Duration
        // Remark
        // Measurement Class Code (N42 helper)




    }
}
