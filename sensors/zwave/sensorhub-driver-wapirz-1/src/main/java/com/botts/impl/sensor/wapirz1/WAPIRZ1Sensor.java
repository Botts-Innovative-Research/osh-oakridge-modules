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
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveAlarmCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveMultiLevelSensorCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

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
    private int configNodeId;
    private int zControllerId;
    public ZWaveEvent message;
    public ZWaveController zController;
    public ZWaveNode node;

    // SENSOR DATA
    int key;
    String value;
    int event;
    int v1AlarmCode;
    String commandClassType;
    String commandClassValue;
    String commandClassMessage;
    boolean isMotion;
    boolean isTamperAlarm;

    // OUTPUTS
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

                sensorDescription.setDescription("Linear/GoControl WAPIRZ-1 Z-Wave Motion Detector (with Temperature Device Handler");

                SMLHelper smlWADWAZHelper = new SMLHelper();
                smlWADWAZHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("WAPIRZ-1"))
                        .addClassifier(smlWADWAZHelper.classifiers.sensorType("Motion Detector"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        configNodeId = config.wapirzSensorDriverConfigurations.nodeID;
        zControllerId = config.wapirzSensorDriverConfigurations.controllerID;

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:wapirz1]", config.serialNumber);
        generateXmlID("[WAPIRZ-1]", config.serialNumber);

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


        initAsync = true;

        ModuleRegistry moduleRegistry = getParentHub().getModuleRegistry();

        commService = moduleRegistry.getModuleByType(ZwaveCommService.class);


        if (commService == null) {

            throw new SensorHubException("CommService needs to be configured");

        } else {

            moduleRegistry.waitForModule(commService.getLocalID(), ModuleEvent.ModuleState.STARTED)
                    .thenRun(() -> commService.registerListener(this));

            CompletableFuture.runAsync(() -> {
                        zController = commService.getzController();
//                        if(config.wapirzSensorDriverConfigurations.reInitNode) {
//                            zController.reinitialiseNode(configNodeId);
//                        }

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

    }

    @Override
    public void doStart() throws SensorHubException {

        ZWaveNode node = zController.getNode(configNodeId);
//        locationOutput.setLocationOutput(config.getLocation());


//        zController = commService.getzController();
//        if(config.wapirzSensorDriverConfigurations.reInitNode) {
//            zController.reinitialiseNode(configNodeId);
//        }

//        node = new ZWaveNode(zController.getHomeId(), configNodeId, zController);
    }


    @Override
    public void doStop() throws SensorHubException {

        //Handle stopping the node

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

            ZWaveNode node = zController.getNode(id);
            node.getNodeInitStage();

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

                handleAlarmData(key, value, event, v1AlarmCode);

            } else if (message instanceof ZWaveCommandClassValueEvent) {

                commandClassType = ((ZWaveCommandClassValueEvent) message).getCommandClass().name();
                commandClassValue = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                handleCommandClassData(commandClassType, commandClassValue);

            } else if (message instanceof ZWaveInitializationStateEvent) {


//                if (((ZWaveInitializationStateEvent) message).getStage() == ZWaveNodeInitStage.EMPTYNODE) {
//
//                        if (zController.getNode(configNodeId) == null) {
//                            node = new ZWaveNode(zController.getHomeId(), configNodeId, zController);
//
//                        } else if (zController.getNode(configNodeId) != null && (zController.getNode(configNodeId).getNodeInitStage() == ZWaveNodeInitStage.EMPTYNODE)) {
//                            zController.getNode(configNodeId).initialiseNode(ZWaveNodeInitStage.IDENTIFY_NODE);
//                            node = commService.createZWaveNode(zController, configNodeId);
//                        }
//                        node.initialiseNode();

//                    ZWaveNodeInitStageAdvancer stageAdvancer = new ZWaveNodeInitStageAdvancer(node,zController);
//                    stageAdvancer.startInitialisation();
//                }
//                if (node.getNodeInitStage() == SET_WAKEUP) {
//                    // Set wakeup time to minimum interval of  600s
//                    ZWaveWakeUpCommandClass wakeupCommandClass =
//                            (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId)
//                                    .getCommandClass
//                                            (ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
//
//                    if (wakeupCommandClass != null) {
//                        ZWaveCommandClassTransactionPayload wakeUp =
//                                wakeupCommandClass.setInterval(17, config.wapirzSensorDriverConfigurations.wakeUpTime);
//
//                        commService.sendConfigurations(wakeUp);
//                        commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());
//                    }
//                }

//                if (node.getNodeInitStage() == DONE) {
//
//                        ZWaveBatteryCommandClass zWaveBatteryCommandClass =
//                                (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);
//
//                        commService.sendConfigurations(zWaveBatteryCommandClass.getValueMessage());
//
//                        ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
//                                (ZWaveConfigurationCommandClass) commService.getZWaveNode(configNodeId)
//                                        .getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);
//
////                      Config_1_1
//                        ZWaveConfigurationParameter reTriggerWait = new ZWaveConfigurationParameter(1,
//                                config.wapirzSensorDriverConfigurations.reTriggerWait, 1);
//                        ZWaveCommandClassTransactionPayload configReTriggerWait =
//                                zWaveConfigurationCommandClass.setConfigMessage(reTriggerWait);
//                        commService.sendConfigurations(configReTriggerWait);
//                        commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage
//                                (1));
//
//                    }
            }
        }
    }

    public void handleAlarmData(int key, String value, int event, int v1AlarmCode) {
        //key == 32; COMMAND_CLASS_BASIC
        //key == 7; BURGLAR

        if (key == 7 && Objects.equals(value, "255") && event == 2) {
            isMotion = true;
        } else if (key == 7 && Objects.equals(value, "0") && event == 2) {
            isMotion = false;
        }
        motionOutput.onNewMessage(isMotion);

//        key = 7; value = 255-triggered/0-untriggered; event = 2; - shook it around
//        key = 32; value = 255; isMotion = true;
//        key = 32; value = 0; isMotion = false; - or is this resetting Tamper alarm?

        if (key == 7 && Objects.equals(value, "255") && event == 3) {
            isTamperAlarm = true;
        } else if (key == 7 && Objects.equals(value, "0") && v1AlarmCode == 0) {
            isTamperAlarm = false;
        }
        tamperAlarmOutput.onNewMessage(isTamperAlarm);
    }

    public void handleCommandClassData(String commandClassType, String commandClassValue) {

        //Command Class Types
        if (Objects.equals(commandClassType, "COMMAND_CLASS_BATTERY")) {
            commandClassMessage = commandClassValue;
        }

        if (Objects.equals(commandClassType, "COMMAND_CLASS_BASIC") && commandClassValue == "0") {
            isMotion = false;
        }
        if (commandClassValue != null) {
            batteryOutput.onNewMessage(commandClassMessage);
            motionOutput.onNewMessage(isMotion);
        }
    }

}



