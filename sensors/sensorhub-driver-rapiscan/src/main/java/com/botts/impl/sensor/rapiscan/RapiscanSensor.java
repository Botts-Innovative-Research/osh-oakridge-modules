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


//import com.botts.impl.sensor.rapiscan.helpers.RapiscanActionList;
//import com.botts.impl.sensor.rapiscan.helpers.RapiscanPreset;
import com.botts.impl.sensor.rapiscan.eml.EMLOutput;
import com.botts.impl.sensor.rapiscan.eml.EMLService;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
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

//    ArrayList<ICommProvider> commProviderList= new ArrayList<>();
//    ArrayList<MessageHandler>messageHandlerList = new ArrayList<>();
//    static ArrayList<InputStream> bufferedInputStreamsList = new ArrayList<InputStream>();

    EMLOutput emlOutput;
    GammaOutput gammaOutput;
    NeutronOutput neutronOutput;
    OccupancyOutput occupancyOutput;
    LocationOutput locationOutput;
    TamperOutput tamperOutput;
    SpeedOutput speedOutput;
    GammaSetup1Output gammaSetUp1Output;
    GammaSetup2Output gammaSetUp2Output;
    GammaSetup3Output gammaSetup3Output;
    NeutronSetupOutput neutronSetupOutput;
    InputStream msgIn;

    Timer t;

    //configs
//    int neutronCount;
//    int gammaCount;
    String laneName;
//    RapiscanLayerConfig rapiscanLayerConfig;


    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:rapiscan:", String.valueOf(config.serialNumber));
        generateXmlID("Rapiscan", String.valueOf(config.serialNumber));

//        rapiscanLayerConfig = config.rapiscanLayerConfigs;
//        neutronCount = config.neutronCount;
//        gammaCount = config.gammaCount;


        laneName = config.laneName;

        if(config.isSupplementalAlgorithm){
            //do EML stuff
            //calculate the thresholds if this is true for eml service
            calculateThreshold();
            try {
                createEMLOutputs();
                startEMLService();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }

        }

        createOutputs();
    }

    public void calculateThreshold(){

    }
    public void startEMLService() throws ParserConfigurationException, IOException, SAXException {
        EMLService emlService = new EMLService(emlOutput, messageHandler);

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

        gammaSetUp1Output = new GammaSetup1Output(this);
        addOutput(gammaSetUp1Output, false);
        gammaSetUp1Output.init();

        gammaSetUp2Output = new GammaSetup2Output(this);
        addOutput(gammaSetUp2Output, false);
        gammaSetUp2Output.init();

        gammaSetup3Output = new GammaSetup3Output(this);
        addOutput(gammaSetup3Output, false);
        gammaSetup3Output.init();

        neutronSetupOutput = new NeutronSetupOutput(this);
        addOutput(neutronSetupOutput, false);
        neutronSetupOutput.init();

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
//                for (ICommProvider<?> commProvider : commProviderList) {
                    msgIn = new BufferedInputStream(commProvider.getInputStream());
                    messageHandler = new MessageHandler(msgIn, gammaOutput, neutronOutput, occupancyOutput, tamperOutput, speedOutput, gammaSetUp1Output, gammaSetUp2Output, gammaSetup3Output, neutronSetupOutput);

//                    bufferedInputStreamsList.add(msgIn);
//                    messageHandlerList.add(messageHandler);
//                }

//            csvMsgRead.readMessages(msgIn, gammaOutput, neutronOutput, occupancyOutput);

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
                System.out.println("location updated");
            }
        };
        t.scheduleAtFixedRate(tt,500,10000);

    }

    public MessageHandler getMessageHandler(){
        return messageHandler;
    }
}
