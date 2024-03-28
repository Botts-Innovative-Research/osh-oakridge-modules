/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.zw100;

import com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommServiceConfig;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.*;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorConfig;
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
 * @author cardy
 * @since 11/16/23
 */
public class ZW100Sensor extends AbstractSensorModule<ZW100Config> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(ZW100Sensor.class);
    public ZwaveCommService commService;
    public ZwaveCommServiceConfig.ZW100SensorDriverConfigurations sensorConfig = new ZwaveCommServiceConfig().zw100SensorDriverConfigurations;

    private int configNodeId;
    private int zControllerId;

    public ZWaveEvent message;

    int alarmKey;
    String alarmValue;
    int alarmEvent;
    String sensorValueMessage;
    String multiSensorType;
    String multiSensorValue;
    String commandClassType;
    String commandClassValue;
    String commandClassMessage;

    MotionOutput motionOutput;

    RelativeHumidityOutput relativeHumidityOutput;

    TemperatureOutput temperatureOutput;

    LuminanceOutput luminanceOutput;

    UltravioletOutput ultravioletOutput;

    VibrationAlarmOutput vibrationAlarmOutput;

    BatteryOutput batteryOutput;

    LocationOutput locationOutput;

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("ZW100-Multisensor-6 utilizes z-wave technology to monitor motion, " +
                        "temperature, relative humidity, UV index, luminance, and system vibration (used as a tamper " +
                        "alarm)");

                SMLHelper smlWADWAZHelper = new SMLHelper();
                smlWADWAZHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("ZW100-Multisensor-6"))
                        .addClassifier(smlWADWAZHelper.classifiers.sensorType("6 in 1 Sensor"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();


        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:zw100]", config.serialNumber);
        generateXmlID("[ZW100]", config.serialNumber);

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

        relativeHumidityOutput = new RelativeHumidityOutput(this);
        addOutput(relativeHumidityOutput, false);
        relativeHumidityOutput.doInit();

        temperatureOutput = new TemperatureOutput(this);
        addOutput(temperatureOutput, false);
        temperatureOutput.doInit();

        luminanceOutput = new LuminanceOutput(this);
        addOutput(luminanceOutput, false);
        luminanceOutput.doInit();

        ultravioletOutput = new UltravioletOutput(this);
        addOutput(ultravioletOutput, false);
        ultravioletOutput.doInit();

        vibrationAlarmOutput = new VibrationAlarmOutput(this);
        addOutput(vibrationAlarmOutput, false);
        vibrationAlarmOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();

        locationOutput = new LocationOutput(this);
        addOutput(locationOutput, false);
        locationOutput.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {
        //heal node to restart initialization in order to send configurations
//        commService.getZWaveNode(configNodeId).healNode();

//        ZWaveNode zw100Node = commService.getZWaveNode(configNodeId);
//
//     if (commService.getZWaveNode(configNodeId).getNodeInitStage() != ZWaveNodeInitStage.IDENTIFY_NODE) {
//         commService.getZWaveNode(configNodeId).setNodeStage(ZWaveNodeInitStage.IDENTIFY_NODE);
//     } else if (commService.getZWaveNode(configNodeId).getNodeInitStage() == ZWaveNodeInitStage.DONE) {
//        commService.getZWaveNode(configNodeId).healNode();

//     }

//        commService.getZWaveNode(configNodeId).getNodeState()????

//        locationOutput.setLocationOutput(config.getLocation());

    }


    @Override
    public void doStop() throws SensorHubException {

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

    // Sorts data based on message type and sends information to outputs
    @Override
    public void onNewDataPacket(int id, ZWaveEvent message) {

        configNodeId = sensorConfig.nodeID;
        zControllerId = sensorConfig.controllerID;


            if (id == configNodeId) {


            this.message = message;

            if (message instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {
                alarmKey = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().getKey();
                alarmValue = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();
                alarmEvent = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmEvent();

                motionOutput.onNewMessage(alarmKey, alarmValue, alarmEvent, false);
                vibrationAlarmOutput.onNewMessage(alarmKey, alarmValue, alarmEvent, false);

            } else if (message instanceof ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) {

                multiSensorType =
                        ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) message).getSensorType().getLabel();
                multiSensorValue =
                        ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) message).getValue().toString();

                handleMultiSensorData(multiSensorType, multiSensorValue);

            } else if (message instanceof ZWaveCommandClassValueEvent) {

                commandClassType = ((ZWaveCommandClassValueEvent) message).getCommandClass().name();
                commandClassValue = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                handleCommandClassData(commandClassType, commandClassValue);

//                motionOutput.onNewMessage(commandClassType, commandClassValue, false);

            } else if (message instanceof ZWaveInitializationStateEvent) {

                // Using Node Advancer -> check command class before running config commands (determine which init
                // stages pertain to specific commands?)

                if (((ZWaveInitializationStateEvent) message).getStage() == ZWaveNodeInitStage.DONE && commService.getZWaveNode(configNodeId) != null && commService.getZWaveNode(zControllerId) != null) {
//
                    //&& (commService.getZWaveNode(configNodeId).getNodeInitStage() == ZWaveNodeInitStage.DONE ) && (commService.getZWaveNode(zControllerId) != null)

                    // Run configuration commands on the first build after the multi-sensor is added to the network.
                    // Multisensor must be in config mode before starting the node on OSH. To access config mode:
                    //      1. If powered on battery, hold trigger button until yellow LED shows, then release.
                    //         LED should start blinking yellow. To exit press trigger button once
                    //      2. Power the multisensor by USB

//                    ZWaveNode zWaveNode = commService.getZWaveNode(configNodeId);

                    commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);

                    ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
                            (ZWaveConfigurationCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);

                    //CONFIGURATION COMMANDS

                    //Unlock Configurations
//                    ZWaveConfigurationParameter configUnlock = new ZWaveConfigurationParameter(252, 1, 1);
//                    ZWaveCommandClassTransactionPayload setConfigUnlock =
//                            zWaveConfigurationCommandClass.setConfigMessage(configUnlock);
//                    commService.sendConfigurations(setConfigUnlock);
//                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(252));

                    //Which command would be sent when the motion sensor triggered.
                    // 1 = send Basic Set CC.
                    // 2 = send Sensor Binary Report CC.
                    ZWaveConfigurationParameter motionCommand = new ZWaveConfigurationParameter(5,1,1);
                    ZWaveCommandClassTransactionPayload motionSensorTriggeredCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(motionCommand);
                    commService.sendConfigurations(motionSensorTriggeredCommand);
//                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(5));

                    //The Multisensor will send BASIC SET CC(0x00) to the associated nodes if no motion is
                    // triggered again in 10 seconds
                    ZWaveConfigurationParameter motionSensorReset = new ZWaveConfigurationParameter(3,sensorConfig.motionSensorReset,2);
                    ZWaveCommandClassTransactionPayload configMotionReset =
                            zWaveConfigurationCommandClass.setConfigMessage(motionSensorReset);
                    commService.sendConfigurations(configMotionReset);

                    //Set motion sensor sensitivity
                    ZWaveConfigurationParameter motionSensorSensitivity = new ZWaveConfigurationParameter(4,
                            sensorConfig.motionSensitivity,1);
                    ZWaveCommandClassTransactionPayload setMotionSensitivity =
                            zWaveConfigurationCommandClass.setConfigMessage(motionSensorSensitivity);
                    commService.sendConfigurations(setMotionSensitivity);
//                    logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(4),0).toString());

                    //Get report every 240 seconds/ 4 min (on battery)
                    ZWaveConfigurationParameter sensorReportInterval = new ZWaveConfigurationParameter(111,
                            sensorConfig.sensorReport, 4);
                    ZWaveCommandClassTransactionPayload setSensorReportInterval =
                            zWaveConfigurationCommandClass.setConfigMessage(sensorReportInterval);
                    commService.sendConfigurations(setSensorReportInterval);
//                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(111));

                    //Set temperature unit to F
                    ZWaveConfigurationParameter tempUnit = new ZWaveConfigurationParameter(64,sensorConfig.temperatureUnit,1);
                    ZWaveCommandClassTransactionPayload setTempUnit =
                            zWaveConfigurationCommandClass.setConfigMessage(tempUnit);
                    commService.sendConfigurations(setTempUnit);


//                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(111));
//                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(5));
//                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(64));
//                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(4));
//                    logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(111),0).toString());
//                    logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(5),0).toString());
//                    logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(64),0).toString());
//                    logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(4),0).toString());


                    //Set wakeup time to 240s
                    ZWaveWakeUpCommandClass wakeupCommandClass =
                            (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId).getCommandClass
                                    (ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);

                        if (wakeupCommandClass != null) {
                                ZWaveCommandClassTransactionPayload wakeUp =
                                        wakeupCommandClass.setInterval(configNodeId, sensorConfig.wakeUpTime);

                            commService.sendConfigurations(wakeUp);
                            commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());
//                            logger.info("INTERVAL MESSAGE: _____" + commService.getZWaveNode(zControllerId).sendTransaction(wakeupCommandClass.getIntervalMessage(),0));
                            }

//                        //Enable selective reporting only when measurements reach a certain threshold or
//                        // percentage set
//                        ZWaveConfigurationParameter selectiveReporting =
//                                new ZWaveConfigurationParameter(40,1,1);
//                        //Temperature Threshold: value contains one decimal point, e.g. if the value is set to 20,
//                        // thethreshold value =2.0°F
//                        ZWaveConfigurationParameter temperatureThreshold =
//                                new ZWaveConfigurationParameter(41, 10, 4 );
//                        //Humidity Threshold: Unit in %
//                        ZWaveConfigurationParameter relHumThreshold =
//                                new ZWaveConfigurationParameter(42, 3, 1);
//                        //Luminance Threshold
//                        ZWaveConfigurationParameter luminanceThreshold =
//                                new ZWaveConfigurationParameter(43, 30,2);
//                        //Battery Threshold: The unit is %
//                        ZWaveConfigurationParameter batteryThreshold =
//                                new ZWaveConfigurationParameter(44,2,1);
//                        //UV Threshold
//                        ZWaveConfigurationParameter uvThreshold =
//                                new ZWaveConfigurationParameter(45,1,1);
//                        //Set the default unit of the automatic temperature report in parameter 101-103
//                        ZWaveConfigurationParameter temperatureUnit =
//                                new ZWaveConfigurationParameter(64,2,1);


//                        ZWaveCommandClassTransactionPayload configSelectiveReporting =
//                                zWaveConfigurationCommandClass.setConfigMessage(selectiveReporting);
//                        ZWaveCommandClassTransactionPayload configTempThreshold =
//                                zWaveConfigurationCommandClass.setConfigMessage(temperatureThreshold);
//                        ZWaveCommandClassTransactionPayload configRelHumThreshold =
//                                zWaveConfigurationCommandClass.setConfigMessage(relHumThreshold);
//                        ZWaveCommandClassTransactionPayload configLuminanceThreshold =
//                                zWaveConfigurationCommandClass.setConfigMessage(luminanceThreshold);
//                        ZWaveCommandClassTransactionPayload configBatteryThreshold =
//                                zWaveConfigurationCommandClass.setConfigMessage(batteryThreshold);
//                        ZWaveCommandClassTransactionPayload configUvThreshold =
//                                zWaveConfigurationCommandClass.setConfigMessage(uvThreshold);
//                        ZWaveCommandClassTransactionPayload configTempUnit =
//                                zWaveConfigurationCommandClass.setConfigMessage(temperatureUnit);
////
//                         commService.sendConfigurations(configSelectiveReporting);
//                         commService.sendConfigurations(configTempThreshold);
//                         commService.sendConfigurations(configRelHumThreshold);
//                         commService.sendConfigurations(configLuminanceThreshold);
//                         commService.sendConfigurations(configBatteryThreshold);
//                         commService.sendConfigurations(configUvThreshold);
//                         commService.sendConfigurations(configTempUnit);

                    if (((ZWaveInitializationStateEvent) message).getStage() == ZWaveNodeInitStage.DONE) {
                        logger.info(commService.getZWaveNode(configNodeId).getNodeInitStage().name());
                        logger.info("INITIALIZATION COMPLETE");
                    }
                }
            }
        }
    }

    // Sorts multi-sensor data based on sensorType (as opposed to CC key)
    public void handleMultiSensorData(String sensorType, String sensorValue) {

        logger.info(sensorValue);
        System.out.println("zw " + sensorType);
        System.out.println("zw " + sensorValue);

        //Multi-sensor types
        if (Objects.equals(sensorType, "Temperature")) {
            sensorValueMessage = sensorValue;

            temperatureOutput.onNewMessage(sensorValueMessage);

        } else if ("RelativeHumidity".equals(sensorType)) {
            sensorValueMessage = sensorValue;

            relativeHumidityOutput.onNewMessage(sensorValueMessage);

        } else if ("Luminance".equals(sensorType)) {
            sensorValueMessage = sensorValue;

            luminanceOutput.onNewMessage(sensorValueMessage);

        } else if ("Ultraviolet".equals(sensorType)) {
            sensorValueMessage = sensorValue;

            ultravioletOutput.onNewMessage(sensorValueMessage);

        } else {
            System.out.println("No multi-sensor detected");

        }
    }

    // Sorts Command Class data
    public void handleCommandClassData(String sensorType, String sensorValue) {
        //Command Class Types
        if (Objects.equals(sensorType, "COMMAND_CLASS_BATTERY")) {
            commandClassMessage = sensorValue;

            batteryOutput.onNewMessage(commandClassMessage);
        }
//        else if (Objects.equals(sensorType, "COMMAND_CLASS_BASIC")) {
//            commandClassMessage = sensorValue;
//
//            motionOutput.onNewMessage(commandClassMessage);
//
//
    }
}

