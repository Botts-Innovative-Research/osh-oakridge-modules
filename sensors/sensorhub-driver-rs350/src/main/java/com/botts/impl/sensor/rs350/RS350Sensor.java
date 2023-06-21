/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.DataInputStreamLI;
import org.vast.swe.DataOutputStreamLI;
import java.io.IOException;

public class RS350Sensor extends AbstractSensorModule<RS350Config> {
    private static final Logger logger = LoggerFactory.getLogger(RS350Sensor.class);

    ICommProvider<?> commProvider;
    RS350Output output;

    public RS350Sensor() {

    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:rsi:rs350:", config.serialNumber);
        generateXmlID("rsi_rs350_", config.serialNumber);

        // Create and initialize output
        output = new RS350Output(this);
        addOutput(output, false);
        output.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {
        // init comm provider
        if (commProvider == null) {
            try {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                // start comm provider
                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();
                if (commProvider.getCurrentError() != null)
                    throw (SensorHubException) commProvider.getCurrentError();
            } catch (Exception e) {
                commProvider = null;
                throw e;
            }
        }

        if (null != output) {
            output.doStart(commProvider);
        }
    }

    @Override
    public void doStop() throws SensorHubException {
        // stop comm provider
        if (commProvider != null) {
            commProvider.stop();
            commProvider = null;
        }

        if (null != output) {
            output.doStop();
        }
    }

    @Override
    public boolean isConnected() {
        // Determine if sensor is connected
        return output.isAlive();
    }
}