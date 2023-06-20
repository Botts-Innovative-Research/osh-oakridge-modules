/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.botts.impl.sensor.kromekd3s.D3sConfig;


public class RS350Sensor extends AbstractSensorModule<D3sConfig> {
    private static final Logger logger = LoggerFactory.getLogger(RS350Sensor.class);

    ICommProvider<?> commProvider;

    public RS350Sensor(){

    }

    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();

        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:kromek:d3s:", config.serialNumber);
        generateXmlID("KROMEK_D3S_", config.serialNumber);
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}