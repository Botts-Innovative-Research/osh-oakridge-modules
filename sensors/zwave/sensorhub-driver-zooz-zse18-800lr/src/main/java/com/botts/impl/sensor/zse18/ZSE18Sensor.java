/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.zse18;

import com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.ZWaveConfigurationParameter;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEndpoint;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage.DONE;
import static org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage.GET_CONFIGURATION;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Cardy
 * @since 09/09/24
 */
public class ZSE18Sensor extends AbstractSensorModule<ZSE18Config> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(ZSE18Sensor.class);

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
    String commandClassType;
    String commandClassValue;
    String commandClassMessage;
    Boolean isVibration;
    Boolean isMotion;

    // OUTPUTS
    MotionOutput motionOutput;
    VibrationOutput vibrationOutput;
    BatteryOutput batteryOutput;
    LocationOutput locationOutput;


    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("ZooZ Bright Ideas ZSE18 800 series long range sensor with motion " +
                                "detection, vibration sensor, and low battery alerts");

                SMLHelper smlWADWAZHelper = new SMLHelper();
                smlWADWAZHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("ZSE18-800LR"))
                        .addClassifier(smlWADWAZHelper.classifiers.sensorType("Motion Detector"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        configNodeId = config.zse18SensorDriverConfigurations.nodeID;
        zControllerId = config.zse18SensorDriverConfigurations.controllerID;

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:zse18]", config.serialNumber);
        generateXmlID("[ZSE18]", config.serialNumber);

        // Create and initialize output
        motionOutput = new MotionOutput(this);
        addOutput(motionOutput, false);
        motionOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();

        vibrationOutput = new VibrationOutput(this);
        addOutput(vibrationOutput, false);
        vibrationOutput.doInit();

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

                    })

                    .thenRun(() -> setState(ModuleEvent.ModuleState.INITIALIZED))
                    .exceptionally(err -> {

                        reportError(err.getMessage(), err.getCause());

                        setState(ModuleEvent.ModuleState.LOADED);

                        return null;
                    });
        }

    }

    @Override
    public void doStart() throws SensorHubException {

        ZWaveNode node = zController.getNode(configNodeId);
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

            if (message instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {
                reportStatus("Put multi-sensor into config mode");
                event = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmEvent();
                key = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().getKey();
                value = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();

                handleAlarmData(key, value, event);

            } else if (message instanceof ZWaveCommandClassValueEvent) {

                commandClassType = ((ZWaveCommandClassValueEvent) message).getCommandClass().name();
                commandClassValue = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                handleCommandClassData(commandClassType, commandClassValue);

            } else if (message instanceof ZWaveInitializationStateEvent) {

                if (node.getNodeInitStage() == GET_CONFIGURATION) {

                    reportStatus("Put ZSE18 Motion Sensor into config mode");
//                    logger.info("Put multi-sensor into config mode");
//                    ZWaveEndpoint endpoint = commService.node.getEndpoint(0);
//                    ZWaveConfigurationCommandClass commandClass = new ZWaveConfigurationCommandClass(node,
//                            zController, endpoint);

                    ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
                            (ZWaveConfigurationCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);

                    // Param 12: PIR Sensor Sensitivity
                    ZWaveConfigurationParameter pirSensitivity = new ZWaveConfigurationParameter(12,
                            config.zse18SensorDriverConfigurations.pirSensitivity, 1);
                    ZWaveCommandClassTransactionPayload pirSensitivityCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(pirSensitivity);
                    commService.sendConfigurations(pirSensitivityCommand);

                    // Param 14: BASIC SET reports
                    ZWaveConfigurationParameter basicSetReports = new ZWaveConfigurationParameter(14,
                            config.zse18SensorDriverConfigurations.basicSetReports, 1);
                    ZWaveCommandClassTransactionPayload basicSetReportsCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(basicSetReports);
                    commService.sendConfigurations(basicSetReportsCommand);

                    //Param 15: Reverse BASIC SET
                    ZWaveConfigurationParameter reverseBasicSetReports = new ZWaveConfigurationParameter(15,
                            config.zse18SensorDriverConfigurations.reverseBasicSetReports, 1);
                    ZWaveCommandClassTransactionPayload reverseBasicSetReportsCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(reverseBasicSetReports);
                    commService.sendConfigurations(reverseBasicSetReportsCommand);

                    //Param 17: Vibration Sensor
                    ZWaveConfigurationParameter enableVibrationSensor = new ZWaveConfigurationParameter(17,
                            config.zse18SensorDriverConfigurations.enableVibrationSensor, 1);
                    ZWaveCommandClassTransactionPayload enableVibrationSensorCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(enableVibrationSensor);
                    commService.sendConfigurations(enableVibrationSensorCommand);

                    // Param 18: Trigger Interval
                    ZWaveConfigurationParameter triggerInterval = new ZWaveConfigurationParameter(18,
                            config.zse18SensorDriverConfigurations.triggerInterval, 2);
                    ZWaveCommandClassTransactionPayload triggerIntervalCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(triggerInterval);
                    commService.sendConfigurations(triggerIntervalCommand);

                    // Param 19: Notification (Alarm) or Binary Reports
                    ZWaveConfigurationParameter binarySensorReports = new ZWaveConfigurationParameter(19,
                            config.zse18SensorDriverConfigurations.binarySensorReports, 1);
                    ZWaveCommandClassTransactionPayload binarySensorReportsCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(binarySensorReports);
                    commService.sendConfigurations(binarySensorReportsCommand);

                    // Param 20: LED Indicator
                    ZWaveConfigurationParameter ledIndicator = new ZWaveConfigurationParameter(20,
                            config.zse18SensorDriverConfigurations.ledIndicator, 1);
                    ZWaveCommandClassTransactionPayload ledIndicatorCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(ledIndicator);
                    commService.sendConfigurations(ledIndicatorCommand);

                    // Param 32: Low Battery Alert
                    ZWaveConfigurationParameter lowBatteryAlert = new ZWaveConfigurationParameter(32,
                            config.zse18SensorDriverConfigurations.lowBatteryAlert, 1);
                    ZWaveCommandClassTransactionPayload lowBatteryAlertCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(lowBatteryAlert);
                    commService.sendConfigurations(lowBatteryAlertCommand);

                    // Param 254: Lock Advanced Settings
                    ZWaveConfigurationParameter lockSettings = new ZWaveConfigurationParameter(254,
                            config.zse18SensorDriverConfigurations.lockSettings, 1);
                    ZWaveCommandClassTransactionPayload lockSettingsCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(lockSettings);
                    commService.sendConfigurations(lockSettingsCommand);


                    //Set wakeup time to 240s
                    ZWaveWakeUpCommandClass wakeupCommandClass = (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId).getCommandClass
                            (ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);

                    if (wakeupCommandClass != null) {
                        ZWaveCommandClassTransactionPayload wakeUp =
                                wakeupCommandClass.setInterval(configNodeId,
                                        config.zse18SensorDriverConfigurations.wakeUpTime);

                        commService.sendConfigurations(wakeUp);
                        commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());
//                            logger.info("INTERVAL MESSAGE: _____" + commService.getZWaveNode(zControllerId).sendTransaction(wakeupCommandClass.getIntervalMessage(),0));
                    }
                }
                if (node.getNodeInitStage() == DONE) {

                    ZWaveBatteryCommandClass zWaveBatteryCommandClass =
                            (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);

                    commService.sendConfigurations(zWaveBatteryCommandClass.getValueMessage());
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
            vibrationOutput.onNewMessage(isVibration);
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
    public void handleCommandClassData(String sensorType, String sensorValue) {
        //Command Class Types
        if (Objects.equals(sensorType, "COMMAND_CLASS_BATTERY")) {
            commandClassMessage = sensorValue;

            batteryOutput.onNewMessage(commandClassMessage);
        }
    }

}



