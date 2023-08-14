/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RS350Sensor extends AbstractSensorModule<RS350Config> {

    private static final Logger logger = LoggerFactory.getLogger(RS350Sensor.class);

    ICommProvider<?> commProvider;

    LocationOutput locationOutput;

    StatusOutput statusOutput;

    BackgroundOutput backgroundOutput;

    ForegroundOutput foregroundOutput;

    DerivedDataOutput derivedDataOutput;

    AlarmOutput alarmOutput;

//    RS350MessageHandler messageHandler;
    MessageHandler messageHandler;

    InputStream msgIn;

    public RS350Sensor() {

    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:rsi:rs350:", config.serialNumber);
        generateXmlID("rsi_rs350_", config.serialNumber);

        if (config.outputs.enableLocationOutput) {
            locationOutput = new LocationOutput(this);
            addOutput(locationOutput, false);
            locationOutput.init();
            messageHandler.addMessageListener(locationOutput);
        }

        if (config.outputs.enableStatusOutput) {
            statusOutput = new StatusOutput(this);
            addOutput(statusOutput, false);
            statusOutput.init();
            messageHandler.addMessageListener(statusOutput);
        }

        if (config.outputs.enableBackgroundOutput) {
            backgroundOutput = new BackgroundOutput(this);
            addOutput(backgroundOutput, false);
            backgroundOutput.init();
            messageHandler.addMessageListener(backgroundOutput);
        }

        if (config.outputs.enableForegroundOutput) {
            foregroundOutput = new ForegroundOutput(this);
            addOutput(foregroundOutput, false);
            foregroundOutput.init();
            messageHandler.addMessageListener(foregroundOutput);
        }

        if (config.outputs.enableDerivedData) {
            derivedDataOutput = new DerivedDataOutput(this);
            addOutput(derivedDataOutput, false);
            derivedDataOutput.init();
            messageHandler.addMessageListener(derivedDataOutput);
        }

        if (config.outputs.enableAlarmOutput) {
            alarmOutput = new AlarmOutput(this);
            addOutput(alarmOutput, false);
            alarmOutput.init();
            messageHandler.addMessageListener(alarmOutput);
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

//        messageHandler = new RS350MessageHandler(this, msgIn, locationOutput, statusOutput, backgroundOutput, foregroundOutput, alarmOutput);
//        messageHandler.parse();

        messageHandler = new MessageHandler(msgIn, "</RadInstrumentData>");
    }

    @Override
    protected void doStop() throws SensorHubException {

        if (commProvider != null) {

            try {

                commProvider.stop();

            } catch (Exception e) {

                logger.error("Uncaught exception attempting to stop comms module", e);

            } finally {

                commProvider = null;
            }
        }

        messageHandler.stopProcessing();
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