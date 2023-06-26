/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class OutputRadInstrumentInformation extends OutputBase {
    private static final String SENSOR_OUTPUT_NAME = "RS350 Output";

    private static final Logger logger = LoggerFactory.getLogger(OutputRadInstrumentInformation.class);

    public OutputRadInstrumentInformation(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug("Output created");
    }

    @Override
    protected void init() {
        logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        // SWE Common data structure
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .addSamplingTimeIsoUTC("time")
                .addField("RadInstrumentManufacturerName", sweFactory.createText()
                        .definition(SWEHelper.getPropertyUri("RadInstrumentManufacturerName"))
                        .label("RadInstrumentManufacturerName")
                        .description("RadInstrumentManufacturerName"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing Output Complete");
    }
//    @Override
//    protected void init() {
//        logger.debug("Initializing Output");
//
//        N42Helper fac = new N42Helper();
//
//        dataStruct = fac.createRecord()
//                .name(getName())
//                .label("RadInstrumentInformation")
//                .definition(SWEHelper.getPropertyUri("RadInstrumentInformation"))
//                .addField("time", )
//                .addField("RadInstrumentManufacturerName", )
//                .addField("RadInstrumentIdentifier", )
//                .addField("RadInstrumentModelName", )
//                .addField("RadInstrumentClassCode", )
//                //RadInstrumentVersion
//                .addField("RadInstrumentComponentName", )
//                .addField("RadInstrumentComponentVersion", )
//                //RadInstrumentCharacteristics (2)
//                .addField("CharacteristicName", )
//                .addField("CharacteristicValue", )
//                .addField("CharacteristicValueUnits", )
//                .addField("CharacteristicValueDataClassCode", )
//                .build();
//
//        dataEncoding = new TextEncodingImpl(",", "\n");
//
//        logger.debug("Initializing Output Complete");
//    }

    @Override
    public void parseData() {
        DataBlock dataBlock;
        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        double time = (double) System.currentTimeMillis() / 1000;
        dataBlock.setDoubleValue(0, time);
        dataBlock.setStringValue(1, "asd");

        // Update latest record and send
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }
}
