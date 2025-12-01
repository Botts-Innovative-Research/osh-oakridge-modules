/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rapiscan;

import com.botts.impl.sensor.rapiscan.eml.EMLService;
import com.botts.impl.sensor.rapiscan.eml.outputs.EMLAnalysisOutput;
import com.botts.impl.sensor.rapiscan.eml.outputs.EMLContextualOutput;
import com.botts.impl.sensor.rapiscan.eml.outputs.EMLScanContextualOutput;
import com.botts.impl.sensor.rapiscan.output.*;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.RobustIPConnection;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Drew Botts
 * @since Oct 16, 2023
 */
public class RapiscanSensor extends AbstractSensorModule<RapiscanConfig> {

    private static final Logger logger = LoggerFactory.getLogger(RapiscanSensor.class);

    // Utilities
    private MessageHandler messageHandler;
    private EMLService emlService;

    // Connection
    private ICommProvider<?> commProviderModule;

    // Outputs
    private GammaOutput gammaOutput;
    private NeutronOutput neutronOutput;
    private OccupancyOutput<RapiscanSensor> occupancyOutput;
    private LocationOutput locationOutput;
    private TamperOutput tamperOutput;
    private SpeedOutput speedOutput;
    private SetupGammaOutput setupGammaOutput;
    private SetupNeutronOutput setupNeutronOutput;
    private GammaThresholdOutput gammaThresholdOutput;
    private DailyFileOutput dailyFileOutput;
    private ConnectionStatusOutput connectionStatusOutput;

    private EMLAnalysisOutput emlAnalysisOutput;
    private EMLScanContextualOutput emlScanContextualOutput;
    private EMLContextualOutput emlContextualOutput;

    private ScheduledFuture<?> heartbeatFuture;
    RobustConnection connection;
    private boolean isRunning;
//    private Thread tcpConnectionThread;
    //private static final Object heartbeatLock = new Object();

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        RapiscanThreadPoolManager.getInstance().registerSensor();

        tryConnection();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:rapiscan:", config.serialNumber);
        generateXmlID("RAPISCAN_", config.serialNumber);

        // Add outputs
        createOutputs();

        // Register GammaThresholdOutput as a listener to SetupGammaOutput
        setupGammaOutput.registerListener(gammaThresholdOutput);

        // EML integration
        if(config.emlConfig.emlEnabled){
            createEMLOutputs();
            emlService = new EMLService(this);

            // Register EMLService as a listener to SetupGammaOutput
            setupGammaOutput.registerListener(emlService);
        }
    }


    public void tryConnection() throws SensorHubException {
        logger.debug("Attempting to connect to RPM...");

        if (config.commSettings == null) throw new SensorHubException("No communication settings specified");

        connection = new RobustIPConnection(this, config.commSettings.connection, "Radiation Portal Monitor - Rapiscan") {

            @Override
            public boolean tryConnect() throws IOException {

                try {
                    var moduleReg = getParentHub().getModuleRegistry();

                    commProviderModule = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                    commProviderModule.start();

//                    if(!isConnected()) throw new SensorHubException("Comm Provider failed to start. Check communication settings.");
//                     added check to stop module if comm module is not started
                    if(!commProviderModule.isStarted()) throw new SensorHubException("Comm Provider failed to start. Check communication settings.");

                    return true;

                } catch (SensorHubException e) {
                    reportError("Cannot connect to Rapiscan Radiation Portal Monitor", e , true);
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

        // Connect to input stream
        InputStream msgIn = new BufferedInputStream(commProviderModule.getInputStream());
        messageHandler = new MessageHandler(msgIn, this);
    }

    public void createEMLOutputs(){
        emlContextualOutput = new EMLContextualOutput(this);
        addOutput(emlContextualOutput, false);
        emlContextualOutput.init();

        emlScanContextualOutput = new EMLScanContextualOutput(this);
        addOutput(emlScanContextualOutput, false);
        emlScanContextualOutput.init();

        emlAnalysisOutput = new EMLAnalysisOutput(this);
        addOutput(emlAnalysisOutput, false);
        emlAnalysisOutput.init();
    }

    public void createOutputs(){
        gammaOutput = new GammaOutput(this);
        addOutput(gammaOutput, false);
        gammaOutput.init();

        neutronOutput = new NeutronOutput(this);
        addOutput(neutronOutput, false);
        neutronOutput.init();

        occupancyOutput = new OccupancyOutput<>(this);
        addOutput(occupancyOutput, false);

        locationOutput = new LocationOutput(this);
        locationOutput.init();

        tamperOutput = new TamperOutput(this);
        addOutput(tamperOutput, false);
        tamperOutput.init();

        speedOutput = new SpeedOutput(this);
        addOutput(speedOutput, false);
        speedOutput.init();

        gammaThresholdOutput = new GammaThresholdOutput(this);
        addOutput(gammaThresholdOutput, false);
        gammaThresholdOutput.init();

        dailyFileOutput = new DailyFileOutput(this);
        addOutput(dailyFileOutput, false);
        dailyFileOutput.init();

        setupGammaOutput = new SetupGammaOutput(this);
        addOutput(setupGammaOutput, false);
        setupGammaOutput.init();

        setupNeutronOutput = new SetupNeutronOutput(this);
        addOutput(setupNeutronOutput, false);
        setupNeutronOutput.init();

        connectionStatusOutput = new ConnectionStatusOutput(this);
        addOutput(connectionStatusOutput,false);
        connectionStatusOutput.init();
    }


    @Override
    protected void doStart() throws SensorHubException {

        connection.waitForConnection();

        try{
            initMsgHandler();
        }catch(IOException e){
            throw new SensorHubException("Error initializing message handler ", e);
        }
    }


    @Override
    protected void afterStart() {
        // Begin heartbeat check
        isRunning = true;
        heartbeatFuture = RapiscanThreadPoolManager.getInstance().scheduleHeartbeat(
                this::heartbeatCheck,
                0,
                1,
                TimeUnit.SECONDS
        );
//        tcpConnectionThread = new Thread(this::heartbeat);
//        tcpConnectionThread.start();
    }

    @Override
    public boolean isConnected(){
        if(connection == null) return false;

        return connection.isConnected();
    }

    @Override
    public void doStop() {
        logger.info("Stopping RapiscanSensor {}...", getUniqueIdentifier());

        isRunning = false;

        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
            heartbeatFuture = null;
        }

        if (messageHandler != null) {
            try {
                messageHandler.stop();
            } catch (Exception e) {
                logger.error("Error stopping message handler", e);
            }
            messageHandler = null;
        }

        if (connection != null) {
            try {
                connection.cancel();
            } catch (Exception e) {
                logger.error("Error canceling connection", e);
            }
        }

        if (commProviderModule != null) {
            try {
                if (commProviderModule.isStarted()) {
                    commProviderModule.stop();
                }
            } catch (Exception e) {
                logger.error("Error stopping comm module", e);
            } finally {
                commProviderModule = null;
            }
        }

        RapiscanThreadPoolManager.getInstance().unregisterSensor();

        logger.info("RapiscanSensor {} stopped successfully", getUniqueIdentifier());
    }


    @Override
    public synchronized void updateConfig(RapiscanConfig config) throws SensorHubException {
        super.updateConfig(config);

        locationOutput.setLocationOutput(config.getLocation());
    }

    public EMLService getEmlService() {
        return this.emlService;
    }

    public GammaOutput getGammaOutput() {
        return gammaOutput;
    }

    public NeutronOutput getNeutronOutput() {
        return neutronOutput;
    }

    public OccupancyOutput<RapiscanSensor> getOccupancyOutput() {
        return occupancyOutput;
    }

    public TamperOutput getTamperOutput() {
        return tamperOutput;
    }

    public SpeedOutput getSpeedOutput() {
        return speedOutput;
    }

    public SetupGammaOutput getSetupGammaOutput() {
        return setupGammaOutput;
    }

    public SetupNeutronOutput getSetupNeutronOutput() {
        return setupNeutronOutput;
    }

    public GammaThresholdOutput getGammaThresholdOutput() {
        return gammaThresholdOutput;
    }

    public EMLAnalysisOutput getEmlAnalysisOutput() {
        return emlAnalysisOutput;
    }

    public EMLScanContextualOutput getEmlScanContextualOutput() {
        return emlScanContextualOutput;
    }

    public EMLContextualOutput getEmlContextualOutput() {
        return emlContextualOutput;
    }

    public DailyFileOutput getDailyFileOutput() {
        return dailyFileOutput;
    }

    public ConnectionStatusOutput getConnectionStatusOutput() {return connectionStatusOutput;}

    public void heartbeatCheck() {
        if (!isRunning)
            return;

        try {
            if (messageHandler == null)
                return;

            long timeSinceMsg = messageHandler.getTimeSinceLastMessage();
            boolean isReceivingMsg = timeSinceMsg < config.commSettings.connection.reconnectPeriod;

            if (connectionStatusOutput != null) {
                connectionStatusOutput.onNewMessage(isReceivingMsg);
            }

            if (!isReceivingMsg) {
                long reconnectTimeout = config.commSettings.connection.connectTimeout;
                if (timeSinceMsg > reconnectTimeout) {
                    logger.warn("Connection timeout after {}ms, attempting reconnect", timeSinceMsg);

                    if (isRunning && connection != null) {
                        try {
                            connection.reconnect();
                        } catch (Exception e) {
                            logger.error("Reconnect failed", e);
                        }
                    }
                }
            }

        } catch (Exception e) {
            if (isRunning) {
                logger.error("Error during heartbeat check", e);
            }
        }
    }
}

