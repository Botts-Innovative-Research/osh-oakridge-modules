/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class OutputBase extends AbstractSensorOutput<RS350Sensor> {
     DataRecord dataStruct;
     DataEncoding dataEncoding;

    public OutputBase(String outputName, RS350Sensor parentSensor) {
        super(outputName, parentSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    protected  void init() {
        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        // SWE Common data structure
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .addSamplingTimeIsoUTC("time")
                .addField("test", sweFactory.createText()
                        .definition(SWEHelper.getPropertyUri("test"))
                        .label("test")
                        .description("test"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    public void parseData() {

    }

    public void start(){
    }

    public void stop() {
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
