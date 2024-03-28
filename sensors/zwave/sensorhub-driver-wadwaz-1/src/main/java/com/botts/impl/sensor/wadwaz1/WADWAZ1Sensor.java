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
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommServiceConfig;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveAlarmCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveBatteryCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveWakeUpCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage;
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


/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Cardy
 * @since 11/15/23
 */
public class WADWAZ1Sensor extends AbstractSensorModule<WADWAZ1Config> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(WADWAZ1Sensor.class);
    public ZwaveCommService commService;
    public ZwaveCommServiceConfig.WADWAZSensorDriverConfigurations sensorConfig = new ZwaveCommServiceConfig().wadwazSensorDriverConfigurations;

//    public  WADWAZ1Config.WADWAZSensorDriverConfigurations sensorConfig =
//        new WADWAZ1Config().wadwazSensorDriverConfigurations;
    public int configNodeId;
    public int zControllerId;

    public ZWaveEvent message;
    int key;
    String value;
    int event;
    String alarmType;
    String alarmValue;
    String commandClassType;
    String commandClassValue;
    String commandClassMessage;


    EntryAlarmOutput entryAlarmOutput;
    BatteryOutput batteryOutput;
    TamperAlarmOutput tamperAlarmOutput;
//    ExternalSwitchAlarmOutput externalSwitchAlarmOutput;
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

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:wadwaz1]", config.serialNumber);
        generateXmlID("[WADWAZ-1]", config.serialNumber);

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
        entryAlarmOutput = new EntryAlarmOutput(this);
        addOutput(entryAlarmOutput, false);
        entryAlarmOutput.doInit();

        tamperAlarmOutput = new TamperAlarmOutput(this);
        addOutput(tamperAlarmOutput, false);
        tamperAlarmOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();

//        externalSwitchAlarmOutput = new ExternalSwitchAlarmOutput(this);
//        addOutput(externalSwitchAlarmOutput, false);
//        externalSwitchAlarmOutput.doInit();

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
//
    @Override
    public void onNewDataPacket(int id, ZWaveEvent message) {

        configNodeId = sensorConfig.nodeID;
        zControllerId = sensorConfig.controllerID;

        if (id == configNodeId) {
//            commService.getZWaveNode(configNodeId)

            this.message = message;

            if (message instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {

                event = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmEvent();
                key = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().getKey();
                value = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();

//                System.out.println("wadwaz " + key);
//                System.out.println("wadwaz " + value);
//                System.out.println("wadwaz " + event);

                tamperAlarmOutput.onNewMessage(key, value, event, false);

//                alarmType = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().name();
//                alarmValue = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();
//
//                tamperAlarmOutput.onNewMessage(alarmType, alarmValue, false);

            } else if (message instanceof ZWaveCommandClassValueEvent) {

                key = ((ZWaveCommandClassValueEvent) message).getCommandClass().getKey();
                value = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                commandClassType = ((ZWaveCommandClassValueEvent) message).getCommandClass().name();
                commandClassValue = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                System.out.println("wadwaz " + key);
                System.out.println("wadwaz " + value);

                handleCommandClassData(commandClassType, commandClassValue);

                entryAlarmOutput.onNewMessage(key, value, false);

            } else if (message instanceof ZWaveInitializationStateEvent) {

                if (((ZWaveInitializationStateEvent) message).getStage() == ZWaveNodeInitStage.STATIC_VALUES && commService.getZWaveNode(zControllerId) != null && commService.getZWaveNode(configNodeId) != null) {

//                ZWaveNode wadwazNode = commService.getZWaveNode(configNodeId);
//                ZWaveNode zController = commService.getZWaveNode(zControllerId);

                    ZWaveBatteryCommandClass zWaveBatteryCommandClass =
                            (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);

                    commService.sendConfigurations(zWaveBatteryCommandClass.getValueMessage());


//                ZWaveAlarmCommandClass alarmCommandClass =
//                        (ZWaveAlarmCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_ALARM);
//                ZWaveCommandClassTransactionPayload sendReport =
//                        alarmCommandClass.getMessage(ZWaveAlarmCommandClass.AlarmType.BURGLAR, 0);
//                        commService.sendConfigurations(sendReport);
//                        System.out.println("GOT THE REPORT");

                    ZWaveWakeUpCommandClass wakeupCommandClass =
                            (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
                    if (wakeupCommandClass != null) {
                        ZWaveCommandClassTransactionPayload wakeUp =
                                wakeupCommandClass.setInterval(configNodeId, sensorConfig.wakeUpTime);

                        //minInterval = 600; intervals must set in 200 second increments
                        commService.sendConfigurations(wakeUp);
                        commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());
//                    System.out.println(((ZWaveWakeUpCommandClass) wadwazNode.getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP)).getInterval());
                    }

                    ZWaveBatteryCommandClass battery =
                            (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);
                    ZWaveCommandClassTransactionPayload batteryCheck = battery.getValueMessage();
                    commService.sendConfigurations(batteryCheck);
                    System.out.println("THIS IS THE BATTERY CHECK");

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
//        else if ("COMMAND_CLASS_BASIC".equals(sensorType)) {
//            commandClassMessage = sensorValue;
//
//            batteryOutput.onNewMessage(commandClassMessage);
//        }
//        else {
//            System.out.println(message);
//        }
    }
}


