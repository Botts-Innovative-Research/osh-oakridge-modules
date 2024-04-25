/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.lb60z1lightbulb;

import com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.ZWaveConfigurationParameter;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveNodeStatusEvent;
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
 * @author cardy
 * 04/10/24
 */
public class LB60ZSensor extends AbstractSensorModule<LB60ZConfig> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(LB60ZSensor.class);
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
    public LB60ZConfig.LB60ZSensorDriverConfigurations sensorConfig =
            new LB60ZConfig().lb60zSensorDriverConfigurations;

    public int configNodeId;
    public int zControllerId;

    public ZWaveEvent message;
    public int key;
    public String value;
    public int event;

    //Initialize your outputs & controls:
    LB60ZControl lbControlInterface;
    LightbulbStatusOutput lightbulbStatusOutput;

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("LinearLinc Radio Frequency Controlled Smart LED Lightbulb");

                SMLHelper smlLB60Z1Helper = new SMLHelper();
                smlLB60Z1Helper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlLB60Z1Helper.identifiers.modelNumber("LB60Z-1"))
                        .addClassifier(smlLB60Z1Helper.classifiers.sensorType("Dimmable Smart Lightbulb"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:lb60z]", config.serialNumber);
        generateXmlID("[LB60Z-1]", config.serialNumber);

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

        // Create and initialize outputs/controls

        lightbulbStatusOutput = new LightbulbStatusOutput(this);
        addOutput(lightbulbStatusOutput, false);
        lightbulbStatusOutput.doInit();

//      add lightbulb controller
        lbControlInterface = new LB60ZControl(this);
        addControlInput(lbControlInterface);
        lbControlInterface.init();


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

        configNodeId = sensorConfig.nodeID;
        zControllerId = sensorConfig.controllerID;

        if (id == configNodeId) {
//            commService.getZWaveNode(configNodeId);

            this.message = message;

            if (message instanceof ZWaveCommandClassValueEvent) {

                key = ((ZWaveCommandClassValueEvent) message).getCommandClass().getKey();
                value = ((ZWaveCommandClassValueEvent) message).getValue().toString();

                lightbulbStatusOutput.onNewMessage(key, value);


//            } else if (message instanceof ZWaveMultiLevelSwitchCommandClass.) {
//                event = ((ZWaveMultiLevelSwitchCommandClass.ZWaveStartStopEvent) message).getType();
//                key = ((ZWaveMultiLevelSwitchCommandClass.ZWaveStartStopEvent) message).getValue().;
//                value = ((ZWaveMultiLevelSwitchCommandClass.ZWaveStartStopEvent) message).getValue().toString();
//
//                System.out.println("sensor" + key);
//                System.out.println("sensor" + value);
//                System.out.println("sensor " + event);


          } else if (message instanceof ZWaveNodeStatusEvent) {
                System.out.println(((ZWaveNodeStatusEvent) message).getState());
                System.out.println(((ZWaveNodeStatusEvent) message).getStage());

          } else if (message instanceof ZWaveInitializationStateEvent) {

                if (((ZWaveInitializationStateEvent) message).getStage() == ZWaveNodeInitStage.DYNAMIC_VALUES && commService.getZWaveNode(zControllerId) != null && commService.getZWaveNode(configNodeId) != null) {
    //
//                    ZWaveNode lb60zNode = commService.getZWaveNode(configNodeId);
//                    ZWaveNode zController = commService.getZWaveNode(zControllerId);

//                    ZWaveMultiLevelSwitchCommandClass zWaveMultiLevelSwitchCommandClass =
//                            (ZWaveMultiLevelSwitchCommandClass) commService.getZWaveNode(configNodeId).getCommandClass((ZWaveCommandClass.CommandClass.COMMAND_CLASS_SWITCH_MULTILEVEL));
//                    ZWaveCommandClassTransactionPayload status = zWaveMultiLevelSwitchCommandClass.getValueMessage();
//
                    //sets the dimmness of the lightbulb to the written value
//                    commService.sendConfigurations(status);
//
//                    commService.sendConfigurations(zWaveMultiLevelSwitchCommandClass.setValueMessage(15));

                    ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
                            (ZWaveConfigurationCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);

                    ZWaveConfigurationParameter dimMemory = new ZWaveConfigurationParameter(1,
                            sensorConfig.dimLevelMemory, 1);
                    ZWaveCommandClassTransactionPayload configDimMemory =
                            zWaveConfigurationCommandClass.setConfigMessage(dimMemory);
                    commService.sendConfigurations(configDimMemory);

                    //If dimMemory = 1 then admin panel needs to change....

                    ZWavePowerLevelCommandClass zWavePowerLevelCommandClass=
                            (ZWavePowerLevelCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_POWERLEVEL);
                    ZWaveCommandClassTransactionPayload powerlevel = zWavePowerLevelCommandClass.getValueMessage();
                    commService.sendConfigurations(powerlevel);

                    int powerLevel = zWavePowerLevelCommandClass.getLevel();

                    logger.info("THIS IS THE POWERLEVEL" + String.valueOf(powerLevel));

                    ZWaveWakeUpCommandClass wakeupCommandClass =
                            (ZWaveWakeUpCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
                    if (wakeupCommandClass != null) {
                        ZWaveCommandClassTransactionPayload wakeUp =
                                wakeupCommandClass.setInterval(configNodeId, sensorConfig.wakeUpTime);
                        commService.sendConfigurations(wakeUp);
                    }
                }

            }
        }
    }


    // TODO: Create appropriate handler functions to sort data


}
