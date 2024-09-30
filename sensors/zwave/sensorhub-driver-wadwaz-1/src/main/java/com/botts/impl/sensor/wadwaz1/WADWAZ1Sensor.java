/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.wadwaz1;

import com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.internal.ThingImpl;
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
import static org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage.SET_WAKEUP;


/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Cardy
 * @since 11/15/23
 */
public class WADWAZ1Sensor extends AbstractSensorModule<WADWAZ1Config> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(WADWAZ1Sensor.class);
    public ZwaveCommService commService;
    private int configNodeId;
    private int zControllerId;
    public ZWaveEvent message;
    public ZWaveController zController;
//    public ThingTypeUID thingTypeUID = new ThingTypeUID("zwave","linear_wadwaz1_00_000");

    // SENSOR DATA
    int key;
    String value;
    int event;
    String alarmType;
    String alarmValue;
    String commandClassType;
    String commandClassValue;
    String commandClassMessage;
    Boolean isEntryAlarm;
    Boolean isTamperAlarm;

    // OUTPUTS
    EntryAlarmOutput entryAlarmOutput;
    BatteryOutput batteryOutput;
    TamperAlarmOutput tamperAlarmOutput;
    LocationOutput locationOutput;

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("WADWAZ-1 window and door sensor for monitoring open/closed " +
                        "status, device tampering, and optional external switch alarm status");

                SMLHelper smlWADWAZHelper = new SMLHelper();
                smlWADWAZHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("WADWAZ-1"))
                        .addClassifier(smlWADWAZHelper.classifiers.sensorType("Window/Door Sensor"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        configNodeId = config.wadwazSensorDriverConfigurations.nodeID;
        zControllerId = config.wadwazSensorDriverConfigurations.controllerID;

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:wadwaz1]", config.serialNumber);
        generateXmlID("[WADWAZ-1]", config.serialNumber);

        // Create and initialize output
        entryAlarmOutput = new EntryAlarmOutput(this);
        addOutput(entryAlarmOutput, false);
        entryAlarmOutput.doInit();

        tamperAlarmOutput = new TamperAlarmOutput(this);
        addOutput(tamperAlarmOutput, false);
        tamperAlarmOutput.doInit();

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

//        ZWaveNode node = zController.getNode(configNodeId);
//        ThingTypeUID thingTypeUID = new ThingTypeUID("zwave","linear_wadwaz1_00_000");
//        Thing thing = new ThingImpl(thingTypeUID, "Wadwaz");
//        thing.setBridgeUID(commService.thing.getBridgeUID());
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
//
    @Override
    public void onNewDataPacket(int id, ZWaveEvent message) {

        if (id == configNodeId) {

            ZWaveNode node = zController.getNode(id);
            node.getNodeInitStage();

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

                System.out.println("wadwaz " + key);
                System.out.println("wadwaz " + value);

                handleCommandClassData(commandClassType, commandClassValue);

            } else if (message instanceof ZWaveBinarySensorCommandClass.ZWaveBinarySensorValueEvent) {

                ((ZWaveBinarySensorCommandClass.ZWaveBinarySensorValueEvent) message).getValue();
                ((ZWaveBinarySensorCommandClass.ZWaveBinarySensorValueEvent) message).getSensorType();

            } else if (message instanceof ZWaveInitializationStateEvent) {

                if (node.getNodeInitStage() == SET_WAKEUP) {

                    ZWaveWakeUpCommandClass wakeupCommandClass =
                            (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
                    if (wakeupCommandClass != null) {
                        ZWaveCommandClassTransactionPayload wakeUp =
                                wakeupCommandClass.setInterval(configNodeId,
                                        config.wadwazSensorDriverConfigurations.wakeUpTime);

                        //minInterval = 600; intervals must set in 200 second increments
                        commService.sendConfigurations(wakeUp);
                        commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());
//                    System.out.println(((ZWaveWakeUpCommandClass) wadwazNode.getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP)).getInterval());
                    }
                }

                if (node.getNodeInitStage() == DONE) {

                    ZWaveBatteryCommandClass battery =
                            (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);
                    ZWaveCommandClassTransactionPayload batteryCheck = battery.getValueMessage();
                    commService.sendConfigurations(batteryCheck);
                    System.out.println("THIS IS THE BATTERY CHECK");

                }
            }
        }
    }

    public void handleTamperAlarm(int key, String value, int event){
        if (key == 7 && Objects.equals(value, "255") && event == 3) {
            isTamperAlarm = true;
        } else if (key == 7 && Objects.equals(value, "0") && event == 2) {
            isTamperAlarm = false;
        } else if (key == 48 && Objects.equals(value, "255")){
            isTamperAlarm = true;
        } else if (key == 48 && Objects.equals(value, "0")){
            isTamperAlarm = false;
        }

        if (isTamperAlarm != null){
                tamperAlarmOutput.onNewMessage(isTamperAlarm);
        }
    }

    public void handleEntryAlarm(int key, String value, int event) {
        if (key == 32 && Objects.equals(value, "255")) {
            isEntryAlarm = true;
        } else if (key == 32 && Objects.equals(value, "0")){
            isEntryAlarm = false;
        }

        if (isEntryAlarm != null) {
            entryAlarmOutput.onNewMessage(isEntryAlarm);

        }
    }
    public void handleCommandClassData(String commandClassType, String commandClassValue){

        //Command Class Types
        if (Objects.equals(commandClassType, "COMMAND_CLASS_BATTERY")) {
            commandClassMessage = commandClassValue;

            batteryOutput.onNewMessage(commandClassMessage);
        }

//        handleEntryAlarm(key, value, event);
        //        if (key == 128 ){   //key 128 = "COMMAND_CLASS_BATTERY"
//            battery = batteryLevel;
//        }
    }
}


