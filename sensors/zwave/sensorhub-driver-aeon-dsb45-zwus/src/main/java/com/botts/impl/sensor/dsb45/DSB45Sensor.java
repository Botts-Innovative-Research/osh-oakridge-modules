/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.dsb45;

import com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.ZWaveConfigurationParameter;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
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
 * @author your_name
 * @since date
 */
public class DSB45Sensor extends AbstractSensorModule<DSB45Config> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(DSB45Sensor.class);
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

    public DSB45Config.DSB45SensorDriverConfigurations sensorConfig = new DSB45Config()
            .dsb45SensorDriverConfigurations;
    public int configNodeId;
    public int zControllerId;

    public ZWaveEvent message;
    public int key;
    public String value;
    public int event;


    //Initialize your output & control classes
    FloodAlarmOutput floodAlarmOutput;
    BatteryOutput batteryOutput;

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("Detects presence of water or absence of water with accuracy of as little as 0.03% of an inch");

                SMLHelper smlLB60Z1Helper = new SMLHelper();
                smlLB60Z1Helper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlLB60Z1Helper.identifiers.modelNumber("DSB45-ZWUS"))
                        .addClassifier(smlLB60Z1Helper.classifiers.sensorType("Water sensor"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:dsb45]", config.serialNumber);
        generateXmlID("[DSB45-ZWUS]", config.serialNumber);

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

        floodAlarmOutput = new FloodAlarmOutput(this);
        addOutput(floodAlarmOutput, false);
        floodAlarmOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();


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

            // TODO: Assign variables to grab necessary values from different zwave classes and events

            if (message instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {
                event = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmEvent();
                key = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().getKey();
                value = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();

                System.out.println("sensor" + key);
                System.out.println("sensor" + value);
                System.out.println("sensor " + event);

            } else if (message instanceof ZWaveCommandClassValueEvent) {

                key = ((ZWaveCommandClassValueEvent) message).getCommandClass().getKey();
                value = ((ZWaveCommandClassValueEvent) message).getValue().toString();


                System.out.println("sensor " + key);
                System.out.println("sensor " + value);

                floodAlarmOutput.onNewMessage(key, value, false);
                batteryOutput.onNewMessage(key, value);

//            } else if ...


            } else if (message instanceof ZWaveInitializationStateEvent) {

                if (((ZWaveInitializationStateEvent) message).getStage() == ZWaveNodeInitStage.STATIC_VALUES && commService.getZWaveNode(zControllerId) != null && commService.getZWaveNode(configNodeId) != null) {
//

                    ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
                            (ZWaveConfigurationCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);

                    ZWaveConfigurationParameter sensorBinaryReport = new ZWaveConfigurationParameter(1,
                            sensorConfig.sensorBinaryReport, 1);
                    ZWaveCommandClassTransactionPayload configBinaryReport =
                            zWaveConfigurationCommandClass.setConfigMessage(sensorBinaryReport);
                    commService.sendConfigurations(configBinaryReport);

                    ZWaveConfigurationParameter intervalWakeUpTime = new ZWaveConfigurationParameter(2,
                            sensorConfig.intervalWakeUpTime, 1);
                    ZWaveCommandClassTransactionPayload configIntervalWakeUpTime =
                            zWaveConfigurationCommandClass.setConfigMessage(intervalWakeUpTime);
                    commService.sendConfigurations(configIntervalWakeUpTime);

                    ZWaveConfigurationParameter basicSetValue = new ZWaveConfigurationParameter(3,
                            sensorConfig.basicSet, 1);
                    ZWaveCommandClassTransactionPayload configBasicSetValue =
                            zWaveConfigurationCommandClass.setConfigMessage(basicSetValue);
                    commService.sendConfigurations(configBasicSetValue);

                    ZWaveConfigurationParameter sensorReport = new ZWaveConfigurationParameter(121,
                            sensorConfig.sensorReport, 4);
                    ZWaveCommandClassTransactionPayload configSensorReport =
                            zWaveConfigurationCommandClass.setConfigMessage(sensorReport);
                    commService.sendConfigurations(configSensorReport);

//                    ZWaveConfigurationParameter sensorBinaryReport = new ZWaveConfigurationParameter(1,
//                            sensorConfig.sensorBinaryReport, 1);
//                    ZWaveCommandClassTransactionPayload configBinaryReport =
//                            zWaveConfigurationCommandClass.setConfigMessage(sensorBinaryReport);
//                    commService.sendConfigurations(configBinaryReport);







                    ZWaveBatteryCommandClass zWaveBatteryCommandClass =
                            (ZWaveBatteryCommandClass) commService.getZWaveNode(configNodeId).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);
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

//        public void handleCommandClassData(String commandClassType, String commandClassValue){
//
//            //Command Class Types
//            if (Objects.equals(commandClassType, "COMMAND_CLASS_BATTERY")) {
//                commandClassMessage = commandClassValue;
//
//                batteryOutput.onNewMessage(commandClassMessage);
//            }
        // TODO: Create apropriate handler functions to sort data
    }
}
