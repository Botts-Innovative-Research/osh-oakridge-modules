package com.botts.impl.sensor.zw100;

import org.openhab.binding.zwave.internal.protocol.*;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveNodeStatusEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
import org.sensorhub.impl.comm.UARTConfig;

import java.util.*;


public class ZWaveMessageHandler {

    MotionOutput motionOutput;
    RelativeHumidityOutput relativeHumidityOutput;
    TemperatureOutput temperatureOutput;
    LuminanceOutput luminanceOutput;
    UltravioletOutput ultravioletOutput;
    TamperAlarmOutput tamperAlarmOutput;
    BatteryOutput batteryOutput;
    LocationOutput locationOutput;

    ZWaveMultiLevelSensorCommandClass.SensorType multilevelSensorType;
    String alarmType;
    String alarmValue;
    int alarmEvent;
    String multiSensorType;
    String multiSensorValue;
    String commandClassType;
    String commandClassValue;
    String message;


    public ZWaveMessageHandler(MotionOutput motionOutput, RelativeHumidityOutput relativeHumidityOutput,
                               TemperatureOutput temperatureOutput, LuminanceOutput luminanceOutput,
                               UltravioletOutput ultravioletOutput, TamperAlarmOutput tamperAlarmOutput,
                               BatteryOutput batteryOutput, LocationOutput locationOutput) {

        this.motionOutput = motionOutput;
        this.relativeHumidityOutput = relativeHumidityOutput;
        this.temperatureOutput = temperatureOutput;
        this.luminanceOutput = luminanceOutput;
        this.ultravioletOutput = ultravioletOutput;
        this.tamperAlarmOutput = tamperAlarmOutput;
        this.batteryOutput = batteryOutput;
        this.locationOutput = locationOutput;

    }

    //Create connection to Zwave
    public void ZWaveConnect(String portName, int baudRate) {

        UARTConfig uartConfig = new UARTConfig();

        uartConfig.portName = portName;
        uartConfig.baudRate = baudRate;

        RxtxZWaveIoHandler ioHandler = new RxtxZWaveIoHandler(uartConfig);
        ZWaveController zController = new ZWaveController(ioHandler);
        ioHandler.start(msg -> zController.incomingPacket(msg));


        zController.addEventListener(new ZWaveEventListener() {
            public void ZWaveIncomingEvent(ZWaveEvent event) {


                System.out.println("EVENT: " + event);

                if (event instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {
                    System.out.println("Node " + ((ZWaveCommandClassValueEvent) event).getNodeId() +
                            " ALARM TYPE" +
                            "-> " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmType().name() + " Alarm: " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue() + " Alarm Status: " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmStatus() + " V1 Alarm Code:" +
                            " " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getV1AlarmCode() + " V1 Alarm " +
                            "Level:" +
                            " " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getV1AlarmLevel() + " Alarm Event: " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmEvent() + " Declaring " +
                            "Class: " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmType().getDeclaringClass() + " Report Type Name: " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getReportType().name());


                    alarmEvent = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmEvent();
                    alarmType = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmType().name();
                    alarmValue = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue().toString();

                    tamperAlarmOutput.onNewMessage(alarmType, alarmValue, alarmEvent, false);

                    System.out.println("Alarm Event: " + alarmEvent);
                    System.out.println("Alarm Type: " + alarmType);
                    System.out.println("Alarm Value: " + alarmValue);


                } else if (event instanceof ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) {
                    System.out.println("Node " + ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getNodeId() + " SENSOR TYPE-> " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel() + ": " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue());

                    multiSensorType =
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel();

                    multiSensorValue =
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue().toString();


                    handleMultiSensorData(multiSensorType, multiSensorValue);

//                    System.out.println("Multisensor Type: " + multiSensorType);
//                    System.out.println("Multisensor Value: " + multiSensorValue);


                } else if (event instanceof ZWaveCommandClassValueEvent) {
                    System.out.println("Node " + ((ZWaveCommandClassValueEvent) event).getNodeId() +
                            "COMMAND CLASS NAME-> " +
                            ((ZWaveCommandClassValueEvent) event).getCommandClass().name() + ": " +
                            ((ZWaveCommandClassValueEvent) event).getValue());


                    commandClassType = ((ZWaveCommandClassValueEvent) event).getCommandClass().name();
                    commandClassValue = ((ZWaveCommandClassValueEvent) event).getValue().toString();

                    handleCommandClassData(commandClassType, commandClassValue);

                    motionOutput.onNewMessage(commandClassType, commandClassValue, false);

                    System.out.println("Command Class Type: " + commandClassType);
                    System.out.println("Command Class Value: " + commandClassValue);


                } else if (event instanceof ZWaveNodeStatusEvent) {
                    System.out.println(">> Received Node Info");
                    int nodeId = event.getNodeId();
                    System.out.println("- Node " + nodeId);
                    for (ZWaveCommandClass cmdClass : zController.getNode(nodeId).getCommandClasses(0))
                        System.out.println(cmdClass);


                } else if (event instanceof ZWaveInitializationStateEvent) {
                    System.out.println(">> Node (Final)" + event.getNodeId() + " " + ((ZWaveInitializationStateEvent) event).getStage());

                    System.out.println(zController.getNodes());



                    // Using Node Advancer -> check command class before running config commands (determine which init
                    // stages pertain to specific commands?)

//                    if (((ZWaveInitializationStateEvent) event).getStage() == ZWaveNodeInitStage.DONE &&
//                    (zController.getNode(19) != null) && (zController.getNode(1) != null)) {
                    if (((ZWaveInitializationStateEvent) event).getStage() == ZWaveNodeInitStage.DONE && (zController.getNode(19).getNodeInitStage() == ZWaveNodeInitStage.DONE ) && (zController.getNode(1) != null)) {
//                        if (((ZWaveInitializationStateEvent) event).getStage() == ZWaveNodeInitStage.DONE && (zController.getNode(19).getNodeInitStage() == ZWaveNodeInitStage.DONE ) && (zController.getNode(1) != null)) {

//                        System.out.println(zController.getNode(19).getCommandClasses(0));

                        // Run configuration commands on the first build after the multisensor is added to the network.
                        // Multisensor must be in config mode before starting the node on OSH. To access config mode:
                        //      1. If powered on battery, hold trigger button until yellow LED shows, then release.
                        //         LED should start blinking yellow. To exit press trigger button once
                        //      2. Power the multisensor by USB
                        //

                        ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
                                (ZWaveConfigurationCommandClass)zController.getNode(19).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);

                        //CONFIGURATION COMMANDS

                        //Unlock Configurations
//                        ZWaveConfigurationParameter configUnlock = new ZWaveConfigurationParameter(252, 1, 1);
//                        ZWaveCommandClassTransactionPayload setConfigUnlock =
//                                zWaveConfigurationCommandClass.setConfigMessage(configUnlock);
//                        zController.sendTransaction(setConfigUnlock);


//                        System.out.println(zController.sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(252)));
//
//                        //Set node ID?
//
//                        //Get report every 240 seconds/ 4 min (on battery)
                        ZWaveConfigurationParameter sensorReportInterval = new ZWaveConfigurationParameter(111,240, 4);
                        ZWaveCommandClassTransactionPayload setSensorReportInterval =
                                zWaveConfigurationCommandClass.setConfigMessage(sensorReportInterval);
                        zController.sendTransaction(setSensorReportInterval);
//
////                        Set temperature unit to F
//                        ZWaveConfigurationParameter tempUnit = new ZWaveConfigurationParameter(64,2,1);
//                        ZWaveCommandClassTransactionPayload setTempUnit =
//                                zWaveConfigurationCommandClass.setConfigMessage(tempUnit);
//                        zController.sendTransaction(setTempUnit);
//
//                        System.out.println("CONFIG " +
//                                "MESSAGE__________________________________________________________");
                        System.out.println(zController.sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(111)));
                        System.out.println(zController.sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(5)));
                        System.out.println(zController.sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(64)));

//                        Set wakeup time to 240s
//                            ZWaveWakeUpCommandClass wakeupCommandClass =
//                                    (ZWaveWakeUpCommandClass) zController.getNode(19).getCommandClass
//                                    (ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
//
//                            if (wakeupCommandClass != null) {
//                                ZWaveCommandClassTransactionPayload wakeUp =
//                                        wakeupCommandClass.setInterval(19, 240);
//
//                                zController.sendTransaction(wakeUp);
//                                System.out.println("INTERVAL MESSAGE: _____" + zController.sendTransaction(wakeupCommandClass.getIntervalMessage()));
//                            }


                        //The Multisensor will send BASIC SET CC(0x00) to the associated nodes if no motion is
                        // triggered again in 10 seconds
//                        ZWaveConfigurationParameter motionSensorReset =
//                                new ZWaveConfigurationParameter(3,10,2);
//                        //Send Basic Set CC
//                        ZWaveConfigurationParameter motionSensorTriggeredCommand =
//                                new ZWaveConfigurationParameter(5,1,1);
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
                        //Which command would be sent when the motion sensor triggered.
                          //1 = send Basic Set CC.
                          //2 = send Sensor Binary Report CC.
//                        ZWaveConfigurationParameter motionCommand =
//                                new ZWaveConfigurationParameter(5,1,1);
//                        ZWaveCommandClassTransactionPayload configMotionCommand =
//                                zWaveConfigurationCommandClass.setConfigMessage(motionCommand);
//                        zController.sendTransaction(configMotionCommand);
////
//
//                        ZWaveCommandClassTransactionPayload configMotionReset =
//                                zWaveConfigurationCommandClass.setConfigMessage(motionSensorReset);
//                        ZWaveCommandClassTransactionPayload configMotionTriggeredCommand =
//                                zWaveConfigurationCommandClass.setConfigMessage(motionSensorTriggeredCommand);
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
//                        zController.sendTransaction(configMotionReset);
//                        zController.sendTransaction(configMotionTriggeredCommand);
//                        zController.sendTransaction(configSelectiveReporting);
//                        zController.sendTransaction(configTempThreshold);
//                        zController.sendTransaction(configRelHumThreshold);
//                        zController.sendTransaction(configLuminanceThreshold);
//                        zController.sendTransaction(configBatteryThreshold);
//                        zController.sendTransaction(configUvThreshold);
//                        zController.sendTransaction(configTempUnit);

                    }
                }
            }
                });


        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                zController.shutdown();
                ioHandler.stop();
            }
        });

    }

        public void handleMultiSensorData (String sensorType, String sensorValue) {

            System.out.println(sensorValue);

            //Multi-sensor types
            if (Objects.equals(sensorType, "Temperature")) {
                message = sensorValue;

                temperatureOutput.onNewMessage(message);

            } else if ("RelativeHumidity".equals(sensorType)) {
                message = sensorValue;

                relativeHumidityOutput.onNewMessage(message);

            } else if ("Luminance".equals(sensorType)) {
                message = sensorValue;

                luminanceOutput.onNewMessage(message);

            } else if ("Ultraviolet".equals(sensorType)) {
                message = sensorValue;

                ultravioletOutput.onNewMessage(message);

            } else {
                System.out.println("No multi-sensor detected");

            }
        }
        public void handleCommandClassData(String sensorType, String sensorValue){

            //Command Class Types
            if (Objects.equals(sensorType, "COMMAND_CLASS_BATTERY")) {
                message = sensorValue;

                batteryOutput.onNewMessage(message);
            }
        }
    }