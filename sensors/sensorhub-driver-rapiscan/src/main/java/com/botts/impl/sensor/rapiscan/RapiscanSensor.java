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

import com.botts.impl.sensor.rapiscan.eml.EMLOutput;
import com.botts.impl.sensor.rapiscan.eml.EMLService;
import com.botts.impl.sensor.rapiscan.output.*;
import gov.llnl.ernie.api.ERNIE_lane;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.comm.TCPCommProvider;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Drew Botts
 * @since Oct 16, 2023
 */
public class RapiscanSensor extends AbstractSensorModule<RapiscanConfig> {

    MessageHandler messageHandler;

    private static final Logger logger = LoggerFactory.getLogger(RapiscanSensor.class);

    ICommProvider<?> commProvider;
    EMLOutput emlOutput = null;
    GammaOutput gammaOutput;
    NeutronOutput neutronOutput;
    OccupancyOutput occupancyOutput;
    LocationOutput locationOutput;
    TamperOutput tamperOutput;
    SpeedOutput speedOutput;
    SetupGamma1Output setUpGamma1Output;
    SetupGamma2Output setUpGamma2Output;
    SetupGamma3Output setupGamma3Output;
    SetupNeutronOutput setupNeutronOutput;
    GammaThresholdOutput gammaThresholdOutput;
    InputStream msgIn;
    ERNIE_lane ernieLane = null;
    EMLService emlService = null;

    Timer t;
    public String laneName;


    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:rapiscan:", config.serialNumber);
        generateXmlID("Rapiscan", config.serialNumber);

        laneName = config.laneConfig.laneName;

        // TODO: EML integration
        if(config.isSupplementalAlgorithm){
            createEMLOutputs();
            emlService = new EMLService(this);
        }

        createOutputs();
    }

    public void createEMLOutputs(){
        emlOutput = new EMLOutput(this);
        addOutput(emlOutput, false);
        emlOutput.init();
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
        addOutput(locationOutput, false);
        locationOutput.init();

        tamperOutput = new TamperOutput(this);
        addOutput(tamperOutput, false);
        tamperOutput.init();

        speedOutput = new SpeedOutput(this);
        addOutput(speedOutput, false);
        speedOutput.init();

        setUpGamma1Output = new SetupGamma1Output(this);
        addOutput(setUpGamma1Output, false);
        setUpGamma1Output.init();

        setUpGamma2Output = new SetupGamma2Output(this);
        addOutput(setUpGamma2Output, false);
        setUpGamma2Output.init();

        setupGamma3Output = new SetupGamma3Output(this);
        addOutput(setupGamma3Output, false);
        setupGamma3Output.init();

        setupNeutronOutput = new SetupNeutronOutput(this);
        addOutput(setupNeutronOutput, false);
        setupNeutronOutput.init();

        gammaThresholdOutput = new GammaThresholdOutput(this);
        addOutput(gammaThresholdOutput, false);
        gammaThresholdOutput.init();
    }

    @Override
    protected void doStart() throws SensorHubException {

//        locationOutput.setLocationOutput(config.getLocation());
        setLocationRepeatTimer();

        // init comm provider
        if (commProvider == null) {

            // we need to recreate comm provider here because it can be changed by UI
            try {

                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                var moduleReg = getParentHub().getModuleRegistry();

                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();

//                commProviderList.add(commProvider);

            } catch (Exception e) {
                commProvider = null;
                throw new SensorException("error during  start of Sensor: {}", e);
            }


//         connect to data stream
            try {
                TCPCommProvider tcp = null;

                if (commProvider instanceof TCPCommProvider) {
                    tcp = (TCPCommProvider) commProvider;
                }

                if (config.isSupplementalAlgorithm && tcp != null) {
                    String port = String.valueOf(tcp.getConfiguration().protocol.remotePort);
                    ernieLane = new ERNIE_lane(
                            port,
                            config.laneConfig.laneID,
                            config.laneConfig.isCollimated,
                            config.laneConfig.laneWidth,
                            config.laneConfig.intervals,
                            config.laneConfig.occupancyHoldin
                    );
                }

                msgIn = new BufferedInputStream(commProvider.getInputStream());
                messageHandler = new MessageHandler(msgIn,
                        gammaOutput,
                        neutronOutput,
                        occupancyOutput,
                        tamperOutput,
                        speedOutput,
                        setUpGamma1Output,
                        setUpGamma2Output,
                        setupGamma3Output,
                        setupNeutronOutput,
                        emlOutput,
                        emlService,
                        gammaThresholdOutput);

            } catch (IOException e) {

                throw new SensorException("Error while initializing communications ", e);
            }
        }
    }

    @Override
    public void doStop() throws SensorHubException {

//        if(!commProviderList.isEmpty()){
//            try {
//                t.cancel();
//                t.purge();
//                for(ICommProvider<?> commProvider: commProviderList){
//                    commProvider.stop();
//                }
//            } catch (Exception e) {
//                logger.error("Uncaught exception attempting to stop comms module", e);
//            } finally {
//                commProviderList.clear();
//            }
//        }
        if (commProvider != null) {

            try {
                t.cancel();
                t.purge();
                commProvider.stop();

            } catch (Exception e) {

                logger.error("Uncaught exception attempting to stop comms module", e);

            } finally {

                commProvider = null;
            }
        }

//        messageHandlerList.forEach(MessageHandler::stopProcessing);

    }

    @Override
    public boolean isConnected() {
        if (commProvider == null) {

            return false;

        } else {

            return commProvider.isStarted();
        }
//        for(ICommProvider<?> commProvider: commProviderList){
//            if(commProvider.isStarted()){
//                return true;
//            }
//        }
//        return false;
    }

    void setLocationRepeatTimer(){
        t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                locationOutput.setLocationOutput(config.getLocation());
//                System.out.println("location updated");
            }
        };
        t.scheduleAtFixedRate(tt,500,10000);

    }

    public MessageHandler getMessageHandler(){
        return messageHandler;
    }

    public ERNIE_lane getErnieLane() {
        return this.ernieLane;
    }

    public EMLService getEmlService() {
        return this.emlService;
    }

}
