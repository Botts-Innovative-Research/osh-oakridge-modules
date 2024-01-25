package com.botts.impl.sensor.wapirz1;


import org.openhab.binding.zwave.internal.protocol.ZWaveConfigurationParameter;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEventListener;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveNodeStatusEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
import org.sensorhub.impl.comm.UARTConfig;

import java.util.Objects;


public class ZWaveMessageHandler {

    MotionOutput motionOutput;
    TamperAlarmOutput tamperAlarmOutput;
    TemperatureOutput temperatureOutput;
    BatteryOutput batteryOutput;
    LocationOutput locationOutput;

    String alarmType;
    String alarmValue;
    String commandClassType;
    String commandClassValue;
    String message;


    public ZWaveMessageHandler(MotionOutput motionOutput, TamperAlarmOutput tamperAlarmOutput,
                               TemperatureOutput temperatureOutput,
                               BatteryOutput batteryOutput, LocationOutput locationOutput) {

        this.motionOutput = motionOutput;
        this.tamperAlarmOutput = tamperAlarmOutput;
        this.temperatureOutput = temperatureOutput;
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
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue());


                    alarmType = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmType().name();
                    alarmValue = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue().toString();

                    tamperAlarmOutput.onNewMessage(alarmType, alarmValue, false);

                    System.out.println("Alarm Type: " + alarmType);
                    System.out.println("Alarm Value: " + alarmValue);


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


                } else if (event instanceof  ZWaveBinarySensorCommandClass.ZWaveBinarySensorValueEvent) {
                    System.out.println("Node " + ((ZWaveBinarySensorCommandClass.ZWaveBinarySensorValueEvent) event).getNodeId() + " SENSOR TYPE-> " +
                            ((ZWaveBinarySensorCommandClass.ZWaveBinarySensorValueEvent) event).getSensorType().getLabel() + ": " +
                            ((ZWaveBinarySensorCommandClass.ZWaveBinarySensorValueEvent) event).getValue());

                }
//                else if (event instanceof ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) {
//                System.out.println("Node " + ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getNodeId() + " SENSOR TYPE-> " +
//                        ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel() + ": " +
//                        ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue());
//
////                    System.out.println("Multisensor Type: " + multiSensorType);
////                    System.out.println("Multisensor Value: " + multiSensorValue);
//
//
//            } else if (event instanceof ZWaveNodeStatusEvent) {
//                    System.out.println(">> Received Node Info");
//                    int nodeId = event.getNodeId();
//                    System.out.println("- Node " + nodeId);
//                    for (ZWaveCommandClass cmdClass : zController.getNode(nodeId).getCommandClasses(0))
//                        System.out.println(cmdClass);
//
//
//                }
                else if (event instanceof ZWaveInitializationStateEvent) {
                    System.out.println(">> Node (Final)" + event.getNodeId() + " " + ((ZWaveInitializationStateEvent) event).getStage());


                    if (((ZWaveInitializationStateEvent) event).getStage() == ZWaveNodeInitStage.STATIC_VALUES && (zController.getNode(17).getNodeInitStage() == ZWaveNodeInitStage.STATIC_VALUES) && (zController.getNode(1) != null) && (zController.getNode(17) != null)) {
//                       && (zController.getNode(1) != null) && (zController.getNode(13) != null)) {

//                        System.out.println(zController.getNodes());


                        ZWaveConfigurationCommandClass zWaveConfigurationCommandClass =
                                (ZWaveConfigurationCommandClass)zController.getNode(17).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_CONFIGURATION);
//
                        System.out.println(zController.sendTransaction(zWaveConfigurationCommandClass.getConfigMessage(1)));

//                        ZWaveConfigurationParameter reTriggerWait = new ZWaveConfigurationParameter(1,1,2);
//                        ZWaveCommandClassTransactionPayload configReTriggerWait =
//                                zWaveConfigurationCommandClass.setConfigMessage(reTriggerWait);
//                        zController.sendTransaction(configReTriggerWait);
//                        System.out.println(zController.sendTransaction(zWaveConfigurationCommandClass.getConfigMessage
//                        (1)));

//                      Set wakeup time to minimum interval of  600s
//                        ZWaveWakeUpCommandClass wakeupCommandClass =
//                                (ZWaveWakeUpCommandClass) zController.getNode(17).getCommandClass
//                                        (ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
//
//                        if (wakeupCommandClass != null) {
//                            ZWaveCommandClassTransactionPayload wakeUp =
//                                    wakeupCommandClass.setInterval(17, 600);
//
//                            zController.sendTransaction(wakeUp);
//                            System.out.println("INTERVAL MESSAGE: _____" + zController.sendTransaction(wakeupCommandClass.getIntervalMessage()));
//                        }


                        Runtime.getRuntime().addShutdownHook(new Thread() {
                            public void run() {
                                zController.shutdown();
                                ioHandler.stop();
                            }
                        });

                    }
                }
            }
        });
    }


    public void handleCommandClassData(String sensorType, String sensorValue){

            //Command Class Types
            if (Objects.equals(sensorType, "COMMAND_CLASS_BATTERY")) {
                message = sensorValue;

                batteryOutput.onNewMessage(message);

            } else if (Objects.equals(sensorType, "COMMAND_CLASS_SENSOR_MULTILEVEL")) {
                message = sensorValue;

                temperatureOutput.onNewMessage(message);
            }
        }
    }