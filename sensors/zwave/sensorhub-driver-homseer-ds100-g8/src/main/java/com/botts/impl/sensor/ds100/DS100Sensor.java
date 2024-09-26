/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.ds100;

import com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.ZWaveConfigurationParameter;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
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
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveAlarmCommandClass.AlarmType.ACCESS_CONTROL;
import static org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveAlarmCommandClass.AlarmType.BURGLAR;
import static org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage.*;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Cardy
 * @since 09/09/24
 */
public class DS100Sensor extends AbstractSensorModule<DS100Config> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(DS100Sensor.class);

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
    Boolean isEntryAlarm;
    Boolean isTamperAlarm;

    // OUTPUTS
    EntryAlarmOutput entryAlarmOutput;
    TamperAlarmOutput tamperAlarmOutput;
    BatteryOutput batteryOutput;
    LocationOutput locationOutput;


    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("HomeSeer DS100 G8 long range z-wave window door sensor");

                SMLHelper smlWADWAZHelper = new SMLHelper();
                smlWADWAZHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("DS100-G8"))
                        .addClassifier(smlWADWAZHelper.classifiers.sensorType("Window/Door Sensor"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        configNodeId = config.ds100SensorDriverConfigurations.nodeID;
        zControllerId = config.ds100SensorDriverConfigurations.controllerID;

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:ds100]", config.serialNumber);
        generateXmlID("[DS100]", config.serialNumber);

        // Create and initialize output
        entryAlarmOutput = new EntryAlarmOutput(this);
        addOutput(entryAlarmOutput, false);
        entryAlarmOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();

        tamperAlarmOutput = new TamperAlarmOutput(this);
        addOutput(tamperAlarmOutput, false);
        tamperAlarmOutput.doInit();

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

            this.message = message;

            if (message instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {

                event = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmEvent();
                key = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().getKey();
                value = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();

                handleTamperAlarm(key, value, event);
                handleEntryAlarm(key, value, event);

            } else if (message instanceof ZWaveCommandClassValueEvent) {

                key = ((ZWaveCommandClassValueEvent) message).getCommandClass().getKey();
                value = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                commandClassType = ((ZWaveCommandClassValueEvent) message).getCommandClass().name();
                commandClassValue = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                handleCommandClassData(commandClassType, commandClassValue);

            } else if (message instanceof ZWaveInitializationStateEvent) {

                if (node.getNodeInitStage() == IDENTIFY_NODE) {
                    reportStatus("Node: " + configNodeId + " - DS100 motion sensor beginning initialization");
                } else if (node.getNodeInitStage() == SET_WAKEUP) {

                    ZWaveWakeUpCommandClass wakeupCommandClass =
                            (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
                    if (wakeupCommandClass != null) {
                        ZWaveCommandClassTransactionPayload wakeUp =
                                wakeupCommandClass.setInterval(configNodeId,
                                        config.ds100SensorDriverConfigurations.wakeUpTime);

                        //minInterval = 600; intervals must set in 200 second increments
                        commService.sendConfigurations(wakeUp);
                        commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());
//                    System.out.println(((ZWaveWakeUpCommandClass) wadwazNode.getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP)).getInterval());
                    }
                } else if (node.getNodeInitStage() == STATIC_VALUES) {


//                    ZWaveEndpoint endpoint = commService.node.getEndpoint(0);
//                    ZWaveConfigurationCommandClass commandClass = new ZWaveConfigurationCommandClass(node,
//                            zController, endpoint);

                    ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
                            (ZWaveConfigurationCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);

                    // Param 14: BASIC SET reports
                    ZWaveConfigurationParameter basicSetReports = new ZWaveConfigurationParameter(14,
                            config.ds100SensorDriverConfigurations.basicSetReports, 1);
                    ZWaveCommandClassTransactionPayload basicSetReportsCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(basicSetReports);
                    commService.sendConfigurations(basicSetReportsCommand);

                    //Param 15: Reverse BASIC SET
                    ZWaveConfigurationParameter reverseBasicSetReports = new ZWaveConfigurationParameter(15,
                            config.ds100SensorDriverConfigurations.reverseBasicSetReports, 1);
                    ZWaveCommandClassTransactionPayload reverseBasicSetReportsCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(reverseBasicSetReports);
                    commService.sendConfigurations(reverseBasicSetReportsCommand);

                    // Param 32: Low Battery Alert
                    ZWaveConfigurationParameter lowBatteryAlert = new ZWaveConfigurationParameter(32,
                            config.ds100SensorDriverConfigurations.lowBatteryAlert, 1);
                    ZWaveCommandClassTransactionPayload lowBatteryAlertCommand =
                            zWaveConfigurationCommandClass.setConfigMessage(lowBatteryAlert);
                    commService.sendConfigurations(lowBatteryAlertCommand);

                    clearStatus();


                } else if (node.getNodeInitStage() == DONE) {

                    clearStatus();
                        ZWaveBatteryCommandClass battery =
                                (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);
                        ZWaveCommandClassTransactionPayload batteryCheck = battery.getValueMessage();
                        commService.sendConfigurations(batteryCheck);
                        System.out.println("THIS IS THE BATTERY CHECK");

                    reportStatus("DS100 Initialization Complete");

                    CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS).execute(this::clearStatus);

                }
            }
        }
    }

    public void handleTamperAlarm(int key, String value, int event){
        if (key == 7 && Objects.equals(value, "255") && event == 3) {
            isTamperAlarm = true;
        } else if (key == 7 && Objects.equals(value, "255") && event == 0){
            isTamperAlarm = false;
        } else if (key == 32 && Objects.equals(value, "255") && event == 0) {
            isTamperAlarm = false;
        }

        if (isTamperAlarm != null){
            tamperAlarmOutput.onNewMessage(isTamperAlarm);
        }
    }

    public void handleEntryAlarm(int key, String value, int event) {
        if (key == 6 && Objects.equals(value, "255") && event == 22) {
            isEntryAlarm = true;
        } else if (key == 6 && Objects.equals(value, "255") && event == 23) {
            isEntryAlarm = false;
        }

        if (isEntryAlarm != null) {
            entryAlarmOutput.onNewMessage(isEntryAlarm);

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



