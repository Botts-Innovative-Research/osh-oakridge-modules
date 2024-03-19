/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.wapirz1;

import com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import org.openhab.binding.zwave.internal.protocol.ZWaveConfigurationParameter;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
import org.sensorhub.api.comm.ICommProvider;

import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;

import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Cardy
 * @since 11/15/23
 */
public class WAPIRZ1Sensor extends AbstractSensorModule<WAPIRZ1Config> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(WAPIRZ1Sensor.class);

    private ZwaveCommService commService;
    private int configNodeId = 21;
    private int zControllerId = 1;
    private ZWaveEvent message;
    int key;
    String value;
    int event;
    int v1AlarmCode;
    String commandClassType;
    String commandClassValue;
    String commandClassMessage;
    MotionOutput motionOutput;
    TemperatureOutput temperatureOutput;
    BatteryOutput batteryOutput;
    TamperAlarmOutput tamperAlarmOutput;
    LocationOutput locationOutput;


    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("WAPIRZ-1 ...");

                SMLHelper smlWADWAZHelper = new SMLHelper();
                smlWADWAZHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("WAPIRZ-1"))
                        .addClassifier(smlWADWAZHelper.classifiers.sensorType(""));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:wapirz-1]", config.serialNumber);
        generateXmlID("[WAPIRZ-1]", config.serialNumber);

        initAsync = true;

        ModuleRegistry moduleRegistry = getParentHub().getModuleRegistry();

        commService = moduleRegistry.getModuleByType(ZwaveCommService.class);

        if (commService == null) {

            throw new SensorHubException("CommService needs to be configured");

        } else {

            moduleRegistry.waitForModule(commService.getLocalID(), ModuleEvent.ModuleState.STARTED)
                    .thenRun(() -> commService.registerListener(this));
//                    .thenRun(() -> logger.info("Comm service started"));

            CompletableFuture.runAsync(() -> {

                    })
                    .thenRun(() -> setState(ModuleEvent.ModuleState.INITIALIZED))
                    .exceptionally(err -> {

                        reportError(err.getMessage(), err.getCause());

                        setState(ModuleEvent.ModuleState.LOADED);

                        return null;
                    });
            //            CompletableFuture.runAsync(() -> {
//                        try {
//
//                            moduleRegistry.initModule(config.id);
//
//                        } catch (SensorException e) {
//
//                            throw new CompletionException(e);
//                        } catch (SensorHubException e) {
//                            throw new RuntimeException(e);
//                        }
//                    })
//                    .thenRun(() -> setState(ModuleEvent.ModuleState.INITIALIZED))
//                    .exceptionally(err -> {
//
//                        reportError(err.getMessage(), err.getCause());
//
//                        setState(ModuleEvent.ModuleState.LOADED);
//
//                        return null;
//                    });
        }
        // Create and initialize output
        motionOutput = new MotionOutput(this);
        addOutput(motionOutput, false);
        motionOutput.doInit();

        tamperAlarmOutput = new TamperAlarmOutput(this);
        addOutput(tamperAlarmOutput, false);
        tamperAlarmOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();

        temperatureOutput = new TemperatureOutput(this);
        addOutput(temperatureOutput, false);
        temperatureOutput.doInit();

        locationOutput = new LocationOutput(this);
        addOutput(locationOutput, false);
        locationOutput.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {

//        locationOutput.setLocationOutput(config.getLocation());

    }


    @Override
    public void doStop() throws SensorHubException {

        if (commService != null) {

            try {

                commService.stop();

            } catch (Exception e) {

                logger.error("Uncaught exception attempting to stop comms module", e);

            } finally {

                commService = null;
            }
        }

//        messageHandler.stopProcessing();
    }

    @Override
    public boolean isConnected() {
        if (commService == null) {

            return false;

        } else {

            return commService.isStarted();
        }
    }

    @Override
    public void onNewDataPacket(int id, ZWaveEvent message) {
        if (id == configNodeId) {

            this.message = message;

            if (message instanceof ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) {
                key =
                        ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) message).getSensorType().getKey();
                value =
                        ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) message).getValue().toString();

//                logger.info(String.valueOf(key));
//                logger.info(value);

                temperatureOutput.onNewMessage(key, value);

            } else if (message instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {

                event = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmEvent();
                key = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().getKey();
                value = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();
                v1AlarmCode = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getV1AlarmCode();

                logger.info(String.valueOf(key));
                logger.info(value);
                logger.info(String.valueOf(event));

                tamperAlarmOutput.onNewMessage(key, value, event, v1AlarmCode, false);
                motionOutput.onNewMessage(key, value, event, false);


            } else if (message instanceof ZWaveCommandClassValueEvent){

                commandClassType = ((ZWaveCommandClassValueEvent) message).getCommandClass().name();
                commandClassValue = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                handleCommandClassData(commandClassType, commandClassValue);

            } else if (message instanceof ZWaveInitializationStateEvent) {

                if (((ZWaveInitializationStateEvent) message).getStage() == ZWaveNodeInitStage.STATIC_VALUES && commService.getZWaveNode(zControllerId) != null && commService.getZWaveNode(configNodeId) != null) {
//                    commService.getZWaveNode(configNodeId) = zController.getNode(nodeID);

                    ZWaveBatteryCommandClass zWaveBatteryCommandClass =
                            (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);

                    commService.sendConfigurations(zWaveBatteryCommandClass.getValueMessage());

                    ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
                            (ZWaveConfigurationCommandClass) commService.getZWaveNode(configNodeId)
                                    .getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);

//                      Config_1_1
                    ZWaveConfigurationParameter reTriggerWait = new ZWaveConfigurationParameter(1, 1, 1);
                    ZWaveCommandClassTransactionPayload configReTriggerWait =
                            zWaveConfigurationCommandClass.setConfigMessage(reTriggerWait);
                    commService.sendConfigurations(configReTriggerWait);
                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage
                            (1));


//                      Set wakeup time to minimum interval of  600s
                    ZWaveWakeUpCommandClass wakeupCommandClass =
                            (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId)
                                    .getCommandClass
                                            (ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);

                    if (wakeupCommandClass != null) {
                        ZWaveCommandClassTransactionPayload wakeUp =
                                wakeupCommandClass.setInterval(17, 600);

                        commService.sendConfigurations(wakeUp);
                        commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());
                    }

                }
            }
        }
    }
    public void handleCommandClassData(String commandClassType, String commandClassValue){

        //Command Class Types
        if (Objects.equals(commandClassType, "COMMAND_CLASS_BATTERY")) {
            commandClassMessage = commandClassValue;

            batteryOutput.onNewMessage(commandClassMessage);
        }
//    public void handleCommandClassData(String sensorType, String sensorValue){
//
//            //Command Class Types
//            if (Objects.equals(sensorType, "COMMAND_CLASS_BATTERY")) {
//                message = sensorValue;
//
//                batteryOutput.onNewMessage(message);
//
//            } else if (Objects.equals(sensorType, "COMMAND_CLASS_SENSOR_MULTILEVEL")) {
//                message = sensorValue;
//
//                temperatureOutput.onNewMessage(message);
//            }
//        }
    }
}


