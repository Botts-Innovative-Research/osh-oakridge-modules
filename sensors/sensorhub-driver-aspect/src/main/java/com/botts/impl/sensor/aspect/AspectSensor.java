/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.aspect;


import com.botts.impl.sensor.aspect.comm.IModbusTCPCommProvider;
import com.botts.impl.sensor.aspect.control.AdjudicationControl;
import com.botts.impl.sensor.aspect.output.*;
import com.botts.impl.sensor.aspect.registers.DeviceDescriptionRegisters;
import com.ghgande.j2mod.modbus.ModbusException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;

/**
 * Sensor driver for the Aspect sensor providing sensor description, output registration,
 * initialization and shutdown of the driver and outputs.
 *
 * @author Michael Elmore
 * @since December 2023
 */
public class AspectSensor extends AbstractSensorModule<AspectConfig> implements Runnable{
    private static final Logger log = LoggerFactory.getLogger(AspectSensor.class);

    IModbusTCPCommProvider<?> commProviderModule;

    MessageHandler messageHandler;

    GammaOutput gammaOutput;
    NeutronOutput neutronOutput;
    OccupancyOutput occupancyOutput;
    SpeedOutput speedOutput;
    SensorLocationOutput sensorLocationOutput;
    DailyFileOutput dailyFileOutput;
    AdjudicationControl adjudicationControl;
    ConnectionStatusOutput connectionStatusOutput;

    public int laneID;
    public int primaryDeviceAddress = -1;

    RobustConnection connection;
    private boolean isRunning;
    private Thread tcpConnectionThread;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // connect to aspect rpm
        tryConnection();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:aspect:", config.serialNumber);
        generateXmlID("Aspect", config.serialNumber);

        laneID = config.laneId;

        // add outputs
        createOutputs();
    }

    public void createOutputs(){
        // Initialize outputs
        gammaOutput = new GammaOutput(this);
        addOutput(gammaOutput, false);
        gammaOutput.init();

        neutronOutput = new NeutronOutput(this);
        addOutput(neutronOutput, false);
        neutronOutput.init();

        occupancyOutput = new OccupancyOutput(this);
        addOutput(occupancyOutput, false);
        occupancyOutput.init();

        speedOutput = new SpeedOutput(this);
        addOutput(speedOutput, false);
        speedOutput.init();

        sensorLocationOutput = new SensorLocationOutput(this);
        sensorLocationOutput.init();

        dailyFileOutput = new DailyFileOutput(this);
        addOutput(dailyFileOutput, false);
        dailyFileOutput.init();

        connectionStatusOutput = new ConnectionStatusOutput(this);
        addOutput(connectionStatusOutput, false);
        connectionStatusOutput.init();

        adjudicationControl = new AdjudicationControl(this);
        addControlInput(adjudicationControl);
        adjudicationControl.init();


    }

    public void tryConnection() throws SensorHubException {

        connection = new RobustConnection(this, config.commSettings.connection, "Radiation Portal Monitor - Aspect") {
            @Override
            public boolean tryConnect() throws IOException {
                try {
                    if(config.commSettings == null) throw new SensorHubException("No communication settings specified");

                    var moduleReg = getParentHub().getModuleRegistry();

                    commProviderModule = (IModbusTCPCommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                    commProviderModule.start();

                    if(!commProviderModule.isStarted()) throw new SensorHubException("Comm Provider failed to start. Check communication settings.");

                    return true;
                } catch (SensorHubException e) {

                    reportError("Cannot connect to Aspect Radiation Portal Monitor", e , true);
                    return false;
                }
            }

        };
        connection.waitForConnection();
    }


    public void initMsgHandler() throws SensorHubException, IOException{
//        if(commProviderModule == null || !commProviderModule.isStarted()){
//            throw new SensorHubException("Comm provider is not initialized or not started");
//        }

        var deviceDescriptionRegisters = new DeviceDescriptionRegisters(commProviderModule.getConnection());
        getLogger().info("Attempting device search...");
        for(int i = config.commSettings.protocol.addressRange.from; i < config.commSettings.protocol.addressRange.to; i++) {
            try{
                getLogger().info("Scanning for devices. Current address: #{}", i);
                deviceDescriptionRegisters.readRegisters(i);
                primaryDeviceAddress = i;
                getLogger().info("Found device at address #" + i);
                break;
            } catch (Exception e) {
                if(primaryDeviceAddress != -1 || i == config.commSettings.protocol.addressRange.to)
                    throw new ConnectException("No devices found");
            }
        }

        try {
            deviceDescriptionRegisters.readRegisters(primaryDeviceAddress);
        } catch (ModbusException e) {
            throw new RuntimeException(e);
        }

        // Start message handler
        messageHandler = new MessageHandler(this, primaryDeviceAddress);
    }

    @Override
    protected void doStart() throws SensorHubException {

        connection.waitForConnection();

        try{
            initMsgHandler();
        } catch (IOException e) {
            throw new SensorHubException("Error initializing message handler ", e);
        }

    }

    @Override
    protected void afterStart() {
        // Begin heartbeat check
        tcpConnectionThread = new Thread(this);
        isRunning = true;
        tcpConnectionThread.start();
    }

    @Override
    public void doStop() {
        if (commProviderModule != null) {
            try {
                commProviderModule.stop();
            } catch (Exception e) {
                log.error("Uncaught exception attempting to stop comm module", e);
            } finally {
                commProviderModule = null;
            }
        }

        if (messageHandler != null) {
            messageHandler = null;
        }
    }


    @Override
    public boolean isConnected(){
        if(connection == null) return false;

        return connection.isConnected();
    }

    public ConnectionStatusOutput getConnectionStatusOutput() {return connectionStatusOutput;}
    public DailyFileOutput getDailyFileOutput() {
        return dailyFileOutput;
    }
    public GammaOutput getGammaOutput() {
        return gammaOutput;
    }
    public NeutronOutput getNeutronOutput() {
        return neutronOutput;
    }
    public OccupancyOutput getOccupancyOutput() {
        return occupancyOutput;
    }
    public SpeedOutput getSpeedOutput() {
        return speedOutput;
    }


    @Override
    public void run() {

        long waitPeriod = -1;
        synchronized (this) {
            while (isRunning) {
                try{
                    long timeSinceMsg = messageHandler.getTimeSinceLastMessage();

                    boolean isReceivingMsg = timeSinceMsg < config.commSettings.connection.reconnectPeriod;
                    if(isReceivingMsg){
                        this.connectionStatusOutput.onNewMessage(true);
                        waitPeriod = -1;
                    }
                    else {
                        this.connectionStatusOutput.onNewMessage(false);

                        if(waitPeriod == -1) waitPeriod = System.currentTimeMillis();

                        long timeDisconnected = System.currentTimeMillis() - waitPeriod;

                        if(timeDisconnected > 5000){
                            connection.cancel();
                            connection.reconnect();
                            waitPeriod = -1;
                        }
                    }

                    Thread.sleep(1000);
                }catch(Exception e){
                    log.debug("Error during connection check, "+ e);
                }

            }
        }
    }

}
