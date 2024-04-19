/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.drivername;

import com.botts.sensorhub.impl.zwave.comms.IMessageListener;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
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
public class ZWaveSensor extends AbstractSensorModule<Config> implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(ZWaveSensor.class);
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

    public Config.DRIVERNAMESensorDriverConfigurations sensorConfig =
            new Config().drivernameSensorDriverConfigurations;
    public int configNodeId;
    public int zControllerId;

    public ZWaveEvent message;
    public int key;
    public String value;
    public int event;

    //Initialize outputs:

    Output output;
    Control sensorControl;

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("Description of zwave sensor...");

                SMLHelper smlLB60Z1Helper = new SMLHelper();
                smlLB60Z1Helper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlLB60Z1Helper.identifiers.modelNumber("SENSOR"))
                        .addClassifier(smlLB60Z1Helper.classifiers.sensorType("Z-wave Sensor"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[URN]", config.serialNumber);
        generateXmlID("[XML-PREFIX]", config.serialNumber);

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

        // Create and initialize outputs and controls

        output = new Output(this);
        addOutput(output, false);
        output.doInit();

        sensorControl = new Control(this);
        addControlInput(sensorControl);
        sensorControl.init();


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

        // TODO: Configure the node ID, controller ID, & other configurations to be sent to the node/device in
        //   the sensor driver config class or the ZWaveCommServiceConfig

        configNodeId = sensorConfig.nodeID;
        zControllerId = sensorConfig.controllerID;

        if (id == configNodeId) {
            commService.getZWaveNode(configNodeId);

            this.message = message;

            // TODO: Find relevant Command Classes for sensor data; can use debugging tool to place breakpoints and
            //  easily find information within these instances.
            //  Then, assign variables to store necessary values & send this information to the output using the
            //  output.onNewMessage function or sort with a handler function

            if (message instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {
                event = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmEvent();
                key = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getAlarmType().getKey();
                value = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) message).getValue().toString();

                System.out.println("sensor" + key);
                System.out.println("sensor" + value);
                System.out.println("sensor " + event);

//                output.onNewMessage(key, value, event, false);

//            } else if (message instanceof ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) {
//
//                multiSensorType =
//                        ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) message).getSensorType().getLabel();
//                multiSensorValue =
//                        ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) message).getValue().toString();

//                // Example of feeding data to handler
//                handleMultiSensorData(multiSensorType, multiSensorValue);

//            } else if ...

//            }
            } else if (message instanceof ZWaveInitializationStateEvent) {
//
//            // TODO: Familiarize with initialization stages and send configurations once the node/device hits
//             certain stage or hits ZWaveNodeInitStage.DONE

                if (((ZWaveInitializationStateEvent) message).getStage() == ZWaveNodeInitStage.STATIC_VALUES && commService.getZWaveNode(zControllerId) != null && commService.getZWaveNode(configNodeId) != null) {
                    //
                    ZWaveNode node = commService.getZWaveNode(configNodeId);
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

                        //                    commService.sendConfigurations(wakeUp);
                        //                    commService.sendConfigurations(wakeupCommandClass.getIntervalMessage());

                        //                    System.out.println(((ZWaveWakeUpCommandClass) node.getCommandClass(ZWaveCommandClass
                        //                    .CommandClass.COMMAND_CLASS_WAKE_UP)).getInterval());
                    }

                }
            }
        }
    }

    // TODO: If needed, create appropriate handler functions to sort data

    // The example below sorts multi-sensor data based on sensorType (as opposed to command class key)

//    public void handleMultiSensorData(String sensorType, String sensorValue) {
//
//        logger.info(sensorValue);
//        System.out.println("zw " + sensorType);
//        System.out.println("zw " + sensorValue);
//
//        //Multi-sensor types
//        if (Objects.equals(sensorType, "Temperature")) {
//            sensorValueMessage = sensorValue;
//
//            temperatureOutput.onNewMessage(sensorValueMessage);
//
//        } else if ("RelativeHumidity".equals(sensorType)) {
//            sensorValueMessage = sensorValue;
//
//            relativeHumidityOutput.onNewMessage(sensorValueMessage);
//
//        } else if ("Luminance".equals(sensorType)) {
//            sensorValueMessage = sensorValue;
//
//            luminanceOutput.onNewMessage(sensorValueMessage);
//
//        } else if ("Ultraviolet".equals(sensorType)) {
//            sensorValueMessage = sensorValue;
//
//            ultravioletOutput.onNewMessage(sensorValueMessage);
//
//        } else {
//            System.out.println("No multi-sensor detected");
//
//        }
//    }
}


