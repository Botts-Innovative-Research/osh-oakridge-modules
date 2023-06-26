/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
//package com.botts.impl.sensor.rs350;
//
//import net.opengis.swe.v20.DataBlock;
//import org.sensorhub.api.data.DataEvent;
//import org.vast.swe.SWEHelper;
//import org.vast.swe.helper.GeoPosHelper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.vast.data.TextEncodingImpl;
//
//public class OutputRadItemInformation extends OutputBase {
//    private static final String SENSOR_OUTPUT_NAME = "RadItemInformation Output";
//
//    private static final Logger logger = LoggerFactory.getLogger(OutputRadInstrumentInformation.class);
//
//    public OutputRadItemInformation(RS350Sensor parentSensor) {
//        super(SENSOR_OUTPUT_NAME, parentSensor);
//        logger.debug("RadItemInformation Output created");
//    }
//
//    @Override
//    protected void init() {
//        logger.debug("Initializing Output");
//
//        N42Helper fac = new N42Helper();
//
//        dataStruct = fac.createRecord()
//                .name(getName())
//                .label("RadDetectorInformation")
//                .definition(SWEHelper.getPropertyUri("RadDetectorInformation"))
//                .addField("time", )
//                //4 of these
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
//
//    @Override
//    public void parseData() {
//
//    }
//}
