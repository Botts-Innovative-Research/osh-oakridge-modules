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

import com.botts.impl.sensor.rapiscan.control.AdjudicationControl;
import com.botts.impl.sensor.rapiscan.eml.*;
import com.botts.impl.sensor.rapiscan.eml.outputs.EMLAnalysisOutput;
import com.botts.impl.sensor.rapiscan.eml.outputs.EMLContextualOutput;
import com.botts.impl.sensor.rapiscan.eml.outputs.EMLScanContextualOutput;
import com.botts.impl.sensor.rapiscan.output.GammaThresholdOutput;
import com.botts.impl.sensor.rapiscan.output.*;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.comm.TCPCommProvider;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Drew Botts
 * @since Oct 16, 2023
 */
public class RapiscanSensor extends AbstractSensorModule<RapiscanConfig> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RapiscanSensor.class);

    // Utilities
    private MessageHandler messageHandler;
    private EMLService emlService;

    // Connection
    private ICommProvider<?> commProviderModule;

    // Outputs
    private GammaOutput gammaOutput;
    private NeutronOutput neutronOutput;
    private OccupancyOutput occupancyOutput;
    private LocationOutput locationOutput;
    private TamperOutput tamperOutput;
    private SpeedOutput speedOutput;
    private SetupGammaOutput setupGammaOutput;
    private SetupNeutronOutput setupNeutronOutput;
    private GammaThresholdOutput gammaThresholdOutput;
    private DailyFileOutput dailyFileOutput;
    private ConnectionStatusOutput connectionStatusOutput;
    private AdjudicationControl adjudicationControl;

    private EMLAnalysisOutput emlAnalysisOutput;
    private EMLScanContextualOutput emlScanContextualOutput;
    private EMLContextualOutput emlContextualOutput;

    private Thread tcpConnectionThread;
    private boolean isRunning;

    private long reconnectAttempts;
    private long count = 0;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:rapiscan:", config.serialNumber);
        generateXmlID("RAPISCAN_", config.serialNumber);

        // Add outputs
        createOutputs();
        createControls();

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

        occupancyOutput = new OccupancyOutput(this);
        addOutput(occupancyOutput, false);
        occupancyOutput.init();

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

    public void createControls() {
        adjudicationControl = new AdjudicationControl(this);
        addControlInput(adjudicationControl);
        adjudicationControl.init();
    }

    @Override
    protected void doStart() throws SensorHubException {
        reconnectAttempts = config.commSettings.connection.reconnectAttempts;

        // Initialize comm provider
        if (commProviderModule == null) {

            // we need to recreate comm provider here because it can be changed by UI
            tryConnection();
        }
    }

    @Override
    protected void afterStart() {
        // Begin heartbeat check
        tcpConnectionThread = new Thread(this);
        isRunning = true;
        tcpConnectionThread.start();
    }

    public void tryConnection() throws SensorHubException {
        synchronized (this) {
            try {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                var moduleReg = getParentHub().getModuleRegistry();

                commProviderModule = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProviderModule.start();

                // added check to stop module if comm module is not started
                if(!commProviderModule.isStarted())
                    throw new SensorHubException("Comm Provider failed to start. Check communication settings.");

                // Connect to input stream
                InputStream msgIn = new BufferedInputStream(commProviderModule.getInputStream());
                messageHandler = new MessageHandler(msgIn, this);

            } catch (SensorHubException | IOException e ) {
                commProviderModule.stop();
                commProviderModule = null;
                messageHandler = null;
                throw new SensorHubException("Error initializing sensor during start: ", e);
            }
        }
    }

    @Override
    public void doStop() {
        if (commProviderModule != null) {
            try {
                isRunning = false;
                commProviderModule.stop();
            } catch (Exception e) {
                logger.error("Uncaught exception attempting to stop comm module", e);
            } finally {
                commProviderModule = null;
                messageHandler = null;
            }
        }
    }

    @Override
    public boolean isConnected() {
        return commProviderModule != null && commProviderModule.isStarted();
    }

    // Heartbeat connection retry
    @Override
    public void run() {
        synchronized (this) {
            while (isRunning) {
                if(messageHandler.getTimeSinceLastMessage() < config.commSettings.connection.reconnectPeriod) {
//                    getLogger().debug("TCP currently connected");
                    this.connectionStatusOutput.onNewMessage(true);
                }
                else {
//                    try {
//                        // TODO: Retry connection after it gets dropped
//                        stop();
//                        reportError("Connection dropped", new SensorHubException("Rapiscan connection not received after " + config.commSettings.connection.reconnectPeriod/1000 + " seconds"));
//                    } catch (SensorHubException e) {
//                        throw new RuntimeException(e);
//                    }
                    this.connectionStatusOutput.onNewMessage(false);
//                    getLogger().debug("TCP disconnected");
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.debug("Couldn't sleep");
                }
            }
        }
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

    public OccupancyOutput getOccupancyOutput() {
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

}
