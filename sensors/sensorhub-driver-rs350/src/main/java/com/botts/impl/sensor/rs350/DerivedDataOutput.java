package com.botts.impl.sensor.rs350;

import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DerivedDataOutput extends OutputBase{

    private static final String SENSOR_OUTPUT_NAME = "RS350 Derived Data";

    private static final Logger logger = LoggerFactory.getLogger(DerivedDataOutput.class);

    public DerivedDataOutput(RS350Sensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    protected void init(){
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .build();
    }
}
