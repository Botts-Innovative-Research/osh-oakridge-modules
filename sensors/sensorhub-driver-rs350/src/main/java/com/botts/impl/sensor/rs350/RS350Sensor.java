/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import com.botts.impl.sensor.rs350.output.*;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.comm.RobustIPConnection;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RS350Sensor extends AbstractSensorModule<RS350Config> {

    private static final Logger logger = LoggerFactory.getLogger(RS350Sensor.class);

    ICommProvider<?> commProvider;
    StatusOutput statusOutput;
    BackgroundOutput backgroundOutput;
    ForegroundOutput foregroundOutput;
    AlarmOutput alarmOutput;
    ConnectionStatusOutput connectionStatusOutput;

    RobustConnection connection;
    MessageHandler messageHandler;
    InputStream msgIn;
    private boolean isRunning;
    private Thread tcpConnectionThread;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();
        generateUniqueID("urn:osh:sensor:rsi:rs350:", config.serialNumber);
        generateXmlID("rsi_rs350_", config.serialNumber);

        tryConnection();

        createOutputs();
    }

    private void createOutputs(){
        if (config.outputs.enableStatusOutput) {
            statusOutput = new StatusOutput(this);
            addOutput(statusOutput, false);
            statusOutput.init();
        }

        if (config.outputs.enableBackgroundOutput) {
            backgroundOutput = new BackgroundOutput(this);
            addOutput(backgroundOutput, false);
            backgroundOutput.init();
        }

        if (config.outputs.enableForegroundOutput) {
            foregroundOutput = new ForegroundOutput(this);
            addOutput(foregroundOutput, false);
            foregroundOutput.init();
        }

        if (config.outputs.enableAlarmOutput) {
            alarmOutput = new AlarmOutput(this);
            addOutput(alarmOutput, false);
            alarmOutput.init();
        }

        connectionStatusOutput = new ConnectionStatusOutput(this);
        addOutput(connectionStatusOutput, false);
        connectionStatusOutput.init();
    }

    private void tryConnection() throws SensorHubException {
        if (config.commSettings == null) throw new SensorHubException("No communication settings specified");

        connection = new RobustIPConnection(this, config.commSettings.connection, "RS-350") {
            @Override
            public boolean tryConnect() throws IOException {

                try {
                    var moduleReg = getParentHub().getModuleRegistry();

                    commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);

                    commProvider.start();

                    if(!commProvider.isStarted()) throw new SensorHubException("Comm Provider failed to start. Check communication settings.");

                    return true;

                } catch (Exception e) {
                    reportError("Cannot connect to RS350 ", e , true);
                    return false;
                }
            }
        };
        connection.waitForConnection();
    }
    @Override
    protected void doStart() throws SensorHubException {

        connection.waitForConnection();
        try {
            msgIn = new BufferedInputStream(commProvider.getInputStream());
        } catch (IOException e) {
            throw new SensorException("Error while initializing communications ", e);
        }

        messageHandler = new MessageHandler(msgIn, "</RadInstrumentData>");

        if (config.outputs.enableStatusOutput) {
            messageHandler.addMessageListener(statusOutput);
        }

        if (config.outputs.enableBackgroundOutput) {
            messageHandler.addMessageListener(backgroundOutput);
        }

        if (config.outputs.enableForegroundOutput) {
            messageHandler.addMessageListener(foregroundOutput);
        }

        if (config.outputs.enableAlarmOutput) {
            messageHandler.addMessageListener(alarmOutput);
        }
    }

    @Override
    protected void afterStart() {
        // Begin heartbeat check
        tcpConnectionThread = new Thread(this::heartbeat);
        isRunning = true;
        tcpConnectionThread.start();
    }

    @Override
    protected void doStop() throws SensorHubException {
        isRunning = false;
        if(tcpConnectionThread != null)
            tcpConnectionThread = null;

        if (connection != null) {
            try {
                connection.cancel();
            } catch (Exception e) {
                logger.error("Error canceling connection", e);
            }
        }

        if (commProvider != null) {
            try {
                if (commProvider.isStarted()) {
                    commProvider.stop();
                }
            } catch (Exception e) {
                logger.error("Error stopping comm module", e);
            } finally {
                commProvider = null;
            }
        }


        messageHandler.stopProcessing();
    }

    public ConnectionStatusOutput getConnectionStatusOutput() {
        return connectionStatusOutput;
    }



    @Override
    public boolean isConnected() {
        if(connection == null) return false;

        return connection.isConnected();
    }

    public void checkConnection(){
        // if connection is lost then reconnect in here !!!
        if(!isConnected()){
            reportStatus("RS350 connection lost. Trying to reconnect...");
            connection.reconnect();
        }
    }


    public void heartbeat() {

        while (isRunning) {
            if(messageHandler.getTimeSinceLastMessage() < config.commSettings.connection.reconnectPeriod) {

                this.connectionStatusOutput.onNewMessage(true);
            }
            else {
                this.connectionStatusOutput.onNewMessage(false);

                checkConnection();
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.debug("Couldn't sleep");
            }
        }
    }
}