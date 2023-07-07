package com.botts.impl.sensor.rs350;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForegroundOutput  extends OutputBase{

    private static final String SENSOR_OUTPUT_NAME = "RS350 Foreground Report";

    private static final Logger logger = LoggerFactory.getLogger(StatusOutput.class);

    public ForegroundOutput(RS350Sensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    protected void init(){
        //TODO: Import N42Helper

        // OUTPUT

        // Start Date Time
        // Duration
        // Lin Spectrum (N42 Helper)
        // Cmp Spectrum (N42 Helper)
        // Gamma Gross Counts (N42 Helper)
        // Neutron Gross Counts (N42 Helper)


    }
}
