/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.zcombo;

import com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
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

import java.util.concurrent.CompletableFuture;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author your_name
 * @since date
 */
public class ZComboSensor extends AbstractSensorModule<ZComboConfig> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(ZComboSensor.class);
    public ZwaveCommService commService;

    /* TODO: Sensor configurations can be set from the admin panel in two different ways: through the individual
        sensor's Config or using the ZWaveCommServiceConfig. In both cases create a new class to handle your sensor's
         configurations. Most importantly set the sensor's nodeID and the controllerID.
            Example of implementation:
            1 Creating a class in the ZWaveCommServiceConfig
            public ZwaveCommServiceConfig.YourNewZWaveSensorDriverConfigurationClass sensorConfig = new
            ZwaveCommServiceConfig().yourNewZWaveSensorDriverConfigurationClass;
            2 Creating a class in the sensor's Config
            public SensorConfig.YourNewZWaveSensorDriverConfigurationClass sensorConfig = new YourSensorConfig()
            .yourNewZWaveSensorDriverConfigurationClass;
     */

    public ZComboConfig.ZComboSensorDriverConfigurations sensorConfig =
            new ZComboConfig().zComboSensorDriverConfigurations;
    public int configNodeId;
    public int zControllerId;

    public ZWaveEvent message;
    public int key;
    public String value;
    public int event;

    //Initialize your outputs:

    Output output;

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("Battery powered Zwave combination carbon monoxide & smoke alarm");

                SMLHelper smlLB60Z1Helper = new SMLHelper();
                smlLB60Z1Helper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlLB60Z1Helper.identifiers.modelNumber("ZCOMBO"))
                        .addClassifier(smlLB60Z1Helper.classifiers.sensorType("C02 & smoke alarm"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:zcombo]", config.serialNumber);
        generateXmlID("[ZCOMBO]", config.serialNumber);

        // Initialize & Start comm service
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

        // Create and initialize outputs

        output = new Output(this);
        addOutput(output, false);
        output.doInit();


        // TODO: Perform other initialization
    }


    @Override
    public void doStart() throws SensorHubException {


        // TODO: Perform other startup procedures
    }

    @Override
    public void doStop() throws SensorHubException {


        // TODO: Perform other shutdown procedures
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

        // TODO: Configure the node ID in the ZWaveCommServiceConfig.java or sensor's config class
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

//                tamperAlarmOutput.onNewMessage(key, value, event, false);

//                alarmType = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().name();
//                alarmValue = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();
//
//                tamperAlarmOutput.onNewMessage(alarmType, alarmValue, false);

            } else if (message instanceof ZWaveCommandClassValueEvent) {

                key = ((ZWaveCommandClassValueEvent) message).getCommandClass().getKey();
                value = ((ZWaveCommandClassValueEvent) message).getValue().toString();

//                commandClassType = ((ZWaveCommandClassValueEvent) message).getCommandClass().name();
//                commandClassValue = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                System.out.println("wadwaz " + key);
                System.out.println("wadwaz " + value);

//                handleCommandClassData(commandClassType, commandClassValue);

//                entryAlarmOutput.onNewMessage(key, value, false);


            } else if (message instanceof ZWaveInitializationStateEvent) {
                //
                //            // TODO: Familiarize with initialization stages and send configurations once the node/device hits
                //             certain stage or hits ZWaveNodeInitStage.DONE

                if (((ZWaveInitializationStateEvent) message).getStage() == ZWaveNodeInitStage.STATIC_VALUES && commService.getZWaveNode(zControllerId) != null && commService.getZWaveNode(configNodeId) != null) {
                    //
                    ZWaveNode dsb45Node = commService.getZWaveNode(configNodeId);
                    ZWaveNode zController = commService.getZWaveNode(zControllerId);
                    //
                    ZWaveBatteryCommandClass zWaveBatteryCommandClass =
                            (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);
                    //
                    commService.sendConfigurations(zWaveBatteryCommandClass.getValueMessage());

                    ZWaveWakeUpCommandClass wakeupCommandClass =
                            (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
                    if (wakeupCommandClass != null) {
                        ZWaveCommandClassTransactionPayload wakeUp =
                                wakeupCommandClass.setInterval(configNodeId, sensorConfig.wakeUpTime);

                        //minInterval = 600; intervals must set in 200 second increments
                        commService.sendConfigurations(wakeUp);
                        commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());
                        //                    System.out.println(((ZWaveWakeUpCommandClass) dsb45Node.getCommandClass(ZWaveCommandClass
                        //                    .CommandClass.COMMAND_CLASS_WAKE_UP)).getInterval());
                    }

                }
            }
        }
    }

    // TODO: Create apropriate handler functions to sort data
}
