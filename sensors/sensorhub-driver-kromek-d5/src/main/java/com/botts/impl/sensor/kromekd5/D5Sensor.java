/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.kromekd5;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sensorhub.api.sensor.SensorException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

public class D5Sensor extends AbstractSensorModule<D5Config> {
    private static final Logger logger = LoggerFactory.getLogger(D5Sensor.class);

    ICommProvider<?> commProvider;
    LocationOutput locationOutput;
    BackgroundOutput backgroundOutput;
    ForegroundOutput foregroundOutput;
    AnalysisOutput analysisOutput;

    D5MessageHandler messageHandler;
    Boolean processLock;
    InputStream msgIn;

    public D5Sensor() {

    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:kromek:d5:", config.serialNumber);
        generateXmlID("kromek_d5_", config.serialNumber);

        if (config.outputs.enableLocationOutput){
            locationOutput = new LocationOutput(this);
            addOutput(locationOutput, false);
            locationOutput.init();
        }


        if (config.outputs.enableBackgroundOutput){
            backgroundOutput = new BackgroundOutput(this);
            addOutput(backgroundOutput, false);
            backgroundOutput.init();
        }

        if (config.outputs.enableForegroundOutput){
            foregroundOutput = new ForegroundOutput(this);
            addOutput(foregroundOutput, false);
            foregroundOutput.init();
        }


        if (config.outputs.enableAnalysisData){
            analysisOutput = new AnalysisOutput(this);
            addOutput(analysisOutput, false);
            analysisOutput.init();
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


        messageHandler = new D5MessageHandler(this, msgIn, locationOutput, foregroundOutput, backgroundOutput, analysisOutput);

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