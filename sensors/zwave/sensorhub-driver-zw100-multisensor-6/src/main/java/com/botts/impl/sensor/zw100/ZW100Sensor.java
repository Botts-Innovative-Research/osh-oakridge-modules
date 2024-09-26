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

import  com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.eclipse.jetty.client.ProxyProtocolClientConnectionFactory;
import org.openhab.binding.zwave.handler.ZWaveThingHandler;
import org.openhab.binding.zwave.internal.protocol.*;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveAssociationEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStageAdvancer;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
//import com.vaadin.ui.Notification;
//import com.vaadin.ui.Window;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingImpl;
import org.openhab.core.types.Command;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.AbstractSensorModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.zwave.ZWaveBindingConstants.ZWAVE_THING_UID;
import static org.openhab.binding.zwave.internal.protocol.ZWaveNodeState.ALIVE;
import static org.openhab.binding.zwave.internal.protocol.ZWaveNodeState.INITIALIZING;
import static org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass.*;
import static org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage.*;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author cardy
 * @since 11/16/23
 */
public class ZW100Sensor extends AbstractSensorModule<ZW100Config> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(ZW100Sensor.class);
    public ZwaveCommService commService;
    private int configNodeId;
    private int zControllerId;

    public ZWaveEvent message;
    public ZWaveController zController;
    public ThingUID thingUID;
    public ThingTypeUID thingTypeUID;
    public Thing thing;
    public Bridge controller;

    // SENSOR DATA
    int alarmKey;
    String alarmValue;
    int alarmEvent;
    String sensorValueMessage;
    String multiSensorType;
    String multiSensorValue;
    String commandClassType;
    String commandClassValue;
    String commandClassMessage;
    Boolean isVibration;
    Boolean isMotion;

    // OUTPUTS
    MotionOutput motionOutput;
    RelativeHumidityOutput relativeHumidityOutput;
    TemperatureOutput temperatureOutput;
    LuminanceOutput luminanceOutput;
    UltravioletOutput ultravioletOutput;
    VibrationAlarmOutput vibrationAlarmOutput;
    BatteryOutput batteryOutput;
    LocationOutput locationOutput;

    public ZWaveAssociationGroup associationGroup;
    public ZWaveAssociation association;
    Object associationEvent;

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

        configNodeId = config.zw100SensorDriverConfigurations.nodeID;
        zControllerId = config.zw100SensorDriverConfigurations.controllerID;

        super.doInit();

        // Generate identifiers
            generateUniqueID("[urn:osh:sensor:zw100]", config.serialNumber);
        generateXmlID("[ZW100]", config.serialNumber);

        // Create and initialize output
        motionOutput = new MotionOutput(this);
        addOutput(motionOutput, false);
        motionOutput.doInit();

        vibrationAlarmOutput = new VibrationAlarmOutput(this);
        addOutput(vibrationAlarmOutput, false);
        vibrationAlarmOutput.doInit();

        temperatureOutput = new TemperatureOutput(this);
        addOutput(temperatureOutput, false);
        temperatureOutput.doInit();

        relativeHumidityOutput = new RelativeHumidityOutput(this);
        addOutput(relativeHumidityOutput, false);
        relativeHumidityOutput.doInit();

        luminanceOutput = new LuminanceOutput(this);
        addOutput(luminanceOutput, false);
        luminanceOutput.doInit();

        ultravioletOutput = new UltravioletOutput(this);
        addOutput(ultravioletOutput, false);
        ultravioletOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();

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
                        zController.getNodes();

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

//        controller = commService.controller;
//        thingTypeUID = new ThingTypeUID("zwave", "aeon_zw100_00_000");
////        thingTypeUID = ZWAVE_THING_UID;
////        thingUID = new ThingUID("aeon_zw100_00_000:sensor:node_" + configNodeId);
//
//        thing = new ThingImpl(thingTypeUID, "node_" + configNodeId);
////        thing.setBridgeUID(commService.controller.getBridgeUID());
//        thing.setProperty("node_id=2", null);
//        thing.setBridgeUID(controller.getBridgeUID());
//
//        ZWaveThingHandler zWaveThingHandler = new ZWaveThingHandler(thing);
//        thing.setHandler(zWaveThingHandler);
//        zWaveThingHandler.initialize();
//        zWaveThingHandler.handleConfigurationUpdate(thing.getConfiguration().getProperties());


    }

    @Override
    public void doStart() throws SensorHubException {

    }

    @Override
    public void doStop() throws SensorHubException {

        doStop();
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
         if (id == configNodeId) {

             ZWaveNode node = zController.getNode(id);
             node.getNodeInitStage();

             this.message = message;

//             if ( association == null) {
//                 associationGroup = new ZWaveAssociationGroup(1);
//                 association = new ZWaveAssociation(node.getNodeId());
//
//                if (!associationGroup.isAssociated(association)) {
//                 associationGroup.addAssociation(association);
//                 logger.info("ZW100 Sensor associated with the controller");
//                 }
//             }


//                if (node.getNodeInitStage().isStaticComplete()){
             if (message instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {
                 alarmKey = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().getKey();
                 alarmValue = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();
                 alarmEvent = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmEvent();

                 handleAlarmData(alarmKey, alarmValue, alarmEvent);

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

             } else if (message instanceof ZWaveAssociationEvent) {
                 associationEvent = ((ZWaveAssociationEvent)message).getValue();

             } else if (message instanceof ZWaveInitializationStateEvent) {
                        logger.info(message.toString());

                        if (node.getNodeInitStage() == IDENTIFY_NODE) {
                            reportStatus("Node: " + configNodeId + " - ZW100 beginning initialization");
                        } else if (node.getNodeInitStage() == REQUEST_NIF){
                            clearStatus();
                            reportStatus("Put ZW100 multisensor into config mode");
                        }


                        // Using Node Advancer -> check command class before running config commands (determine which init
                        // stages pertain to specific commands?)

                        if (node.getNodeInitStage() == SET_ASSOCIATION) {

                            ZWaveAssociationCommandClass zWaveAssociationCommandClass =
                                    (ZWaveAssociationCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_ASSOCIATION);

                            if (zWaveAssociationCommandClass != null) {
                                association = new ZWaveAssociation(commService.getzController().getOwnNodeId(), 1);
                            } else {
                                association = new ZWaveAssociation(commService.getzController().getOwnNodeId());
                            }
//                            associationGroup = new ZWaveAssociationGroup(1);
//                            association = new ZWaveAssociation(zController.getOwnNodeId());

                            ZWaveCommandClassTransactionPayload setAssociation =
                                    commService.getZWaveNode(configNodeId).setAssociation(1, association);
                            commService.sendConfigurations(setAssociation);

                            ZWaveAssociationGroupInfoCommandClass zWaveAssociationGroupInfoCommandClass =
                                    (ZWaveAssociationGroupInfoCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(COMMAND_CLASS_ASSOCIATION_GRP_INFO);


                            ZWavePlusCommandClass zWavePlusCommandClass =
                                    (ZWavePlusCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(COMMAND_CLASS_ZWAVEPLUS_INFO);
                            zWavePlusCommandClass.getValueMessage();


//                            ZWaveCommandClassTransactionPayload setAssociation =
//                                    commService.getZWaveNode(zControllerId).setAssociation(1, association);
//                            commService.sendConfigurations(setAssociation);
//
//                                commService.getZWaveNode(configNodeId).getAssociation(1).getDestinationNode();
//                                    (ZWaveAssociationGroupInfoCommandClass) commService.getZWaveNode(configNodeId).setAssociation(1, association)

                        }

                        if (node.getNodeInitStage() == SET_LIFELINE) {

                            ZWaveMultiAssociationCommandClass zWaveMultiAssociationCommandClass =
                                    (ZWaveMultiAssociationCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION);

                            ZWaveAssociationCommandClass zWaveAssociationCommandClass =
                                    (ZWaveAssociationCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_ASSOCIATION);

                            if (zWaveMultiAssociationCommandClass != null && zWaveAssociationCommandClass != null) {
                                association = new ZWaveAssociation(commService.getzController().getOwnNodeId(), 1);
                            } else {
                                association = new ZWaveAssociation(commService.getzController().getOwnNodeId());
                            }

                            ZWaveCommandClassTransactionPayload setAssociation =
                                    commService.getZWaveNode(configNodeId).setAssociation(1, association);
                            commService.sendConfigurations(setAssociation);


                           if (zWaveAssociationCommandClass != null) {
                               ZWaveCommandClassTransactionPayload associationMsg =
//                                    zWaveAssociationCommandClass.setAssociationMessage(1, configNodeId);
                                       zWaveAssociationCommandClass.getAssociationMessage(1);
                               commService.sendConfigurations(associationMsg);
                               logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveAssociationCommandClass.getAssociationMessage(1), 0).toString());
                           }

//                            Collection<ZWaveAssociationGroup> associations =
//                                    commService.getZWaveNode(configNodeId).getAssociationGroups().values();
//                            logger.info(associations.toString());

                        } if (node.getNodeInitStage() == STATIC_VALUES) {


                            // Run configuration commands on the first build after the multi-sensor is added to the network.
                            // Multisensor must be in config mode before starting the node on OSH. To access config mode:
                            //      1. If powered on battery, hold trigger button until yellow LED shows, then release.
                            //         LED should start blinking yellow. To exit press trigger button once
                            //      2. Power the multisensor by USB


                            ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
                                    (ZWaveConfigurationCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(COMMAND_CLASS_CONFIGURATION);

                            //ASSOCIATION GROUP COMMANDS

                            ZWaveAssociationCommandClass zWaveAssociationCommandClass =
                                    (ZWaveAssociationCommandClass)commService.getZWaveNode(configNodeId).getCommandClass(COMMAND_CLASS_ASSOCIATION);

                            ZWaveCommandClassTransactionPayload getAssociations =
                                    zWaveAssociationCommandClass.getAssociationMessage(1);
                            commService.sendConfigurations(getAssociations);
                            logger.info("ASSOCIATION MESSAGE ______________________");


                            ZWaveAssociationGroupInfoCommandClass zWaveAssociationGroupInfoCommandClass =
                                    (ZWaveAssociationGroupInfoCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(COMMAND_CLASS_ASSOCIATION_GRP_INFO);

                            ZWaveCommandClassTransactionPayload commandListMessage =
                                    zWaveAssociationGroupInfoCommandClass.getCommandListMessage(1);
                            commService.sendConfigurations(commandListMessage);
                            logger.info("COMMAND LIST MESSAGE ______________________");

                            ZWaveCommandClassTransactionPayload infoMessage =
                                    zWaveAssociationGroupInfoCommandClass.getInfoMessage(1);
                            commService.sendConfigurations(infoMessage);




                            //CONFIGURATION COMMANDS


                            //Unlock Configurations
//                    ZWaveConfigurationParameter configUnlock = new ZWaveConfigurationParameter(252, 1, 1);
//                    ZWaveCommandClassTransactionPayload setConfigUnlock =
//                            zWaveConfigurationCommandClass.setConfigMessage(configUnlock);
//                    commService.sendConfigurations(setConfigUnlock);
//                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(252));

                            //Set wakeup time to 240s
                            ZWaveWakeUpCommandClass wakeupCommandClass = (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId).getCommandClass
                                    (COMMAND_CLASS_WAKE_UP);

                            if (wakeupCommandClass != null) {
                                ZWaveCommandClassTransactionPayload wakeUp =
                                        wakeupCommandClass.setInterval(configNodeId, config.zw100SensorDriverConfigurations.wakeUpTime);

                                commService.sendConfigurations(wakeUp);
                                commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());
                            logger.info("INTERVAL MESSAGE: _____" + commService.getZWaveNode(zControllerId).sendTransaction(wakeupCommandClass.getIntervalMessage(),0));
                            }

                            //Parameter 2: Stay Awake in Battery Mode
                            // Stay awake for 10 minutes at power on Enable/Disable waking up for 10 minutes when re-power on (battery mode) the MultiSensor
                            // Value = 0, disable
                            // Value = 1, enable

                            ZWaveConfigurationParameter stayAwake = new ZWaveConfigurationParameter(2,
                                    config.zw100SensorDriverConfigurations.stayAwake, 1);
                            ZWaveCommandClassTransactionPayload configStayAwake =
                                    zWaveConfigurationCommandClass.setConfigMessage(stayAwake);
                            commService.sendConfigurations(configStayAwake);
                            System.out.println("Stay awake for 10 mins after powered on: enabled");
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(2), 0).toString());


                            //Parameter 3: Motion Sensor reset timeout - Multisensor will send BASIC SET CC(0x00)
                            // to the associated nodes if no motion is triggered again in x seconds
                            // Default: 4 minutes
                            // Allowed values: Range: 10-3600.
                            ZWaveConfigurationParameter motionSensorReset = new ZWaveConfigurationParameter(3,
                                    config.zw100SensorDriverConfigurations.motionSensorReset, 2);
                            ZWaveCommandClassTransactionPayload configMotionReset =
                                    zWaveConfigurationCommandClass.setConfigMessage(motionSensorReset);
                            commService.sendConfigurations(configMotionReset);
                            System.out.println("Basic set no motion after 10 seconds");
                            commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(3));
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(3), 0).toString());


                            //Parameter 4: Motion sensor sensitivity - Sensitivity level of PIR sensor
                            // (1=minimum, 5=maximum)
                            ZWaveConfigurationParameter motionSensorSensitivity = new ZWaveConfigurationParameter(4,
                                    config.zw100SensorDriverConfigurations.motionSensitivity, 1);
                            ZWaveCommandClassTransactionPayload setMotionSensitivity =
                                    zWaveConfigurationCommandClass.setConfigMessage(motionSensorSensitivity);
                            commService.sendConfigurations(setMotionSensitivity);
                            System.out.println("Motion sensitivity set to max");
                            commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(4));
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(4), 0).toString());


                            //Parameter 5: Motion Sensor Triggered Command - Which command would be sent when the
                            // motion sensor triggered.
                            // 1 = send Basic Set CC.
                            // 2 = send Sensor Binary Report CC.
                            ZWaveConfigurationParameter motionCommand = new ZWaveConfigurationParameter(5, config.zw100SensorDriverConfigurations.motionCommand, 1);
                            ZWaveCommandClassTransactionPayload motionSensorTriggeredCommand =
                                    zWaveConfigurationCommandClass.setConfigMessage(motionCommand);
                            commService.sendConfigurations(motionSensorTriggeredCommand);
                            System.out.println("Motion sensor set to Basic Set");
                            commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(5));
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(5), 0).toString());


                            //Get report every x seconds - min = 240sec/4 min (on battery)
                            ZWaveConfigurationParameter sensorReportInterval = new ZWaveConfigurationParameter(111,
                                    config.zw100SensorDriverConfigurations.sensorReport, 4);
                            ZWaveCommandClassTransactionPayload setSensorReportInterval =
                                    zWaveConfigurationCommandClass.setConfigMessage(sensorReportInterval);
                            commService.sendConfigurations(setSensorReportInterval);
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(111), 0).toString());


                            //Set the default unit of the automatic temperature report in parameter 101-103
                            ZWaveConfigurationParameter tempUnit = new ZWaveConfigurationParameter(64, config.zw100SensorDriverConfigurations.temperatureUnit, 1);
                            ZWaveCommandClassTransactionPayload setTempUnit =
                                    zWaveConfigurationCommandClass.setConfigMessage(tempUnit);
                            commService.sendConfigurations(setTempUnit);
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(64), 0).toString());


                            //Parameter 40: Selective Reporting - Enable selective reporting only when measurements
                            // reach a certain threshold or percentage set
                            ZWaveConfigurationParameter selectiveReporting = new ZWaveConfigurationParameter(40, config.zw100SensorDriverConfigurations.selectiveReporting, 1);
                            ZWaveCommandClassTransactionPayload configSelectiveReporting =
                                    zWaveConfigurationCommandClass.setConfigMessage(selectiveReporting);
                            commService.sendConfigurations(configSelectiveReporting);
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(40), 0).toString());


                            //Parameter 41: Temperature Threshold - value contains one decimal point, e.g. if the
                            // value is set to 20, the threshold value = 2.0°F
                            ZWaveConfigurationParameter temperatureThreshold = new ZWaveConfigurationParameter(41,
                                    config.zw100SensorDriverConfigurations.temperatureThreshold, 4);
                            ZWaveCommandClassTransactionPayload configTempThreshold =
                                    zWaveConfigurationCommandClass.setConfigMessage(temperatureThreshold);
                            commService.sendConfigurations(configTempThreshold);
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(41), 0).toString());


                            //Parameter 42: Humidity Threshold - Unit in %
                            ZWaveConfigurationParameter relHumThreshold = new ZWaveConfigurationParameter(42, config.zw100SensorDriverConfigurations.humidityThreshold,
                                    1);
                            ZWaveCommandClassTransactionPayload configRelHumThreshold =
                                    zWaveConfigurationCommandClass.setConfigMessage(relHumThreshold);
                            commService.sendConfigurations(configRelHumThreshold);
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(42), 0).toString());


                            //Parameter 43: Luminance Threshold
                            ZWaveConfigurationParameter luminanceThreshold = new ZWaveConfigurationParameter(43, config.zw100SensorDriverConfigurations.luminanceThreshold
                                    , 2);
                            ZWaveCommandClassTransactionPayload configLuminanceThreshold =
                                    zWaveConfigurationCommandClass.setConfigMessage(luminanceThreshold);
                            commService.sendConfigurations(configLuminanceThreshold);
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(43), 0).toString());


                            //Parameter 44: Battery Threshold - Unit in %
                            ZWaveConfigurationParameter batteryThreshold = new ZWaveConfigurationParameter(44, config.zw100SensorDriverConfigurations.batteryThreshold, 1);
                            ZWaveCommandClassTransactionPayload configBatteryThreshold =
                                    zWaveConfigurationCommandClass.setConfigMessage(batteryThreshold);
                            commService.sendConfigurations(configBatteryThreshold);
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(44), 0).toString());


                            //Parameter 45: Ultraviolet Threshold
                            ZWaveConfigurationParameter uvThreshold = new ZWaveConfigurationParameter(45, config.zw100SensorDriverConfigurations.UVThreshold, 1);
                            ZWaveCommandClassTransactionPayload configUvThreshold =
                                    zWaveConfigurationCommandClass.setConfigMessage(uvThreshold);
                            commService.sendConfigurations(configUvThreshold);
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(45), 0).toString());


                            //Parameter 46: Send Alarm Report if low temperature
                            ZWaveConfigurationParameter lowTempAlarmReport = new ZWaveConfigurationParameter(46,
                                    config.zw100SensorDriverConfigurations.lowTemperatureReport,
                                    1);
                            ZWaveCommandClassTransactionPayload configLowTempAlarmReport =
                                    zWaveConfigurationCommandClass.setConfigMessage((lowTempAlarmReport));
                            commService.sendConfigurations((configLowTempAlarmReport));
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(46), 0).toString());


                            //Parameter 101: Group 1 Periodic Reports
                            ZWaveConfigurationParameter reportGroup1 = new ZWaveConfigurationParameter(101, 241, 4);
                            ZWaveCommandClassTransactionPayload setReportGroup1 =
                                    zWaveConfigurationCommandClass.setConfigMessage(reportGroup1);
                            commService.sendConfigurations(setReportGroup1);
                            logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(101), 0).toString());
                            clearStatus();

                            //getConfigMessage with the associated parameter # will return the configuration information:

//                    commService.sendConfigurations(zWaveConfigurationCommandClass.getConfigMessage(111));
//                    logger.info(commService.getZWaveNode(zControllerId).sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(111),0).toString());


//                        } else if (node.getNodeInitStage() == DYNAMIC_VALUES) {
//                            ZWaveAssociationGroupInfoCommandClass zWaveAssociationGroupInfoCommandClass =
//                                    (ZWaveAssociationGroupInfoCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(COMMAND_CLASS_ASSOCIATION_GRP_INFO);
//
//                            zWaveAssociationGroupInfoCommandClass.initialize(true);

                        } else if (node.getNodeInitStage() == ZWaveNodeInitStage.DONE) {

                            clearStatus();
//
                            ZWaveBatteryCommandClass battery =
                                    (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);
                            ZWaveCommandClassTransactionPayload batteryCheck = battery.getValueMessage();
                            commService.sendConfigurations(batteryCheck);
                            System.out.println("THIS IS THE BATTERY CHECK");

                            reportStatus("ZW100 Initialization Complete");

                            CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS).execute(this::clearStatus);

                        }
                    }
                }
            }

    public void handleAlarmData(int key, String value, int event) {
        handleMotionData(key, value, event);
        handleVibrationData(key, value, event);
    }

    public void handleVibrationData(int key, String value, int event) {
        //key == 32; COMMAND_CLASS_BASIC
        //key == 7; BURGLAR

        if (key == 7 && Objects.equals(value, "255") && event == 3) {
            isVibration = true;
        } else if (key == 7 && Objects.equals(value, "255") && event == 0) {
            isVibration = false;
        }

        if (isVibration != null) {
            vibrationAlarmOutput.onNewMessage(isVibration);
        }
    }
    public void handleMotionData(int key, String value, int event) {

        if (key == 32 && Objects.equals(value, "255")){
            isMotion = true;
        } else if (key == 32 && Objects.equals(value, "0")){
            isMotion = false;
        } else if (key == 7 && Objects.equals(value, "255") && event == 8){
            isMotion = true;
        } else if (key == 7 && Objects.equals(value, "255") && event == 0){
            isMotion = false;
        } else if (key == 7 && Objects.equals(value, "0") && event == 8){
            isMotion = false;
        }

        if (isMotion != null) {
            motionOutput.onNewMessage(isMotion);
        }
    }
    // Sorts multi-sensor data based on sensorType (as opposed to CC key)
    public void handleMultiSensorData(String sensorType, String sensorValue) {

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
    }
}

