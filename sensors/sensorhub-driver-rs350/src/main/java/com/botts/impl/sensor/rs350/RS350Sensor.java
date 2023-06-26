/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sensorhub.api.sensor.SensorException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

public class RS350Sensor extends AbstractSensorModule<RS350Config> {
    private static final Logger logger = LoggerFactory.getLogger(RS350Sensor.class);

    ICommProvider<?> commProvider;
    OutputRadInstrumentInformation outputRadInstrumentInformation;
    RS350MessageHandler messageHandler;
    Boolean processLock;
    InputStream msgIn;

    public RS350Sensor() {

    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:rsi:rs350:", config.serialNumber);
        generateXmlID("rsi_rs350_", config.serialNumber);

        // Create and initialize outputs
        if (config.outputs.enableRadInstrumentInformation) {
            outputRadInstrumentInformation = new OutputRadInstrumentInformation(this);
            addOutput(outputRadInstrumentInformation, false);
            outputRadInstrumentInformation.init();
        }
    }

    @Override
    protected void doStart() throws SensorHubException {
        // init comm provider
        if (commProvider == null) {
            // we need to recreate comm provider here because it can be changed by UI
            try {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();
            } catch (Exception e) {
                commProvider = null;
                throw e;
            }
        }

        // connect to data stream
        try {
            msgIn = new BufferedInputStream(commProvider.getInputStream());
        } catch (IOException e) {
            throw new SensorException("Error while initializing communications ", e);
        }


        messageHandler = new RS350MessageHandler(this, msgIn, outputRadInstrumentInformation);

        processLock = false;

        messageHandler.parse();
    }

    @Override
    protected void doStop() throws SensorHubException {
        processLock = true;
        if (commProvider != null) {
            try {
                commProvider.stop();
            } catch (Exception e) {
                logger.error("Uncaught exception attempting to stop comms module", e);
            } finally {
                commProvider = null;
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (commProvider == null) {
            return false;
        } else {
            return commProvider.isStarted();
        }
    }
}