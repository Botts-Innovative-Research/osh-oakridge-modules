package com.botts.impl.sensor.zw100;

import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEventListener;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveAlarmCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveMultiLevelSensorCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveWakeUpCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveNodeStatusEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStageAdvancer;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeSerializer;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.comm.UARTConfig;

import java.lang.reflect.Array;
import java.sql.SQLOutput;
import java.util.*;


public class ZWaveMessageHandler {

    MotionOutput motionOutput;
    TamperAlarmOutput tamperAlarmOutput;
    BatteryOutput batteryOutput;
    LocationOutput locationOutput;


    String alarmType;
    String alarmValue;
    String multiSensorType;
    String multiSensorValue;
    String commandClassType;
    String commandClassValue;
    String message;
    Timer motionTimer = new Timer(); // creating timer


    public ZWaveMessageHandler(MotionOutput motionOutput, TamperAlarmOutput tamperAlarmOutput,
                               BatteryOutput batteryOutput, LocationOutput locationOutput) {

        this.motionOutput = motionOutput;
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
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue());


                    alarmType = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmType().name();
                    alarmValue = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue().toString();


//                    try {
//                        motionOutput.motionEventB(alarmType, alarmValue);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }


//                    System.out.println(alarmType);
//                    System.out.println(alarmValue);


                } else if (event instanceof ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) {
                    System.out.println("Node " + ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getNodeId() + " SENSOR TYPE-> " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel() + ": " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue());


                    multiSensorType =
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel();

                    multiSensorValue =
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue().toString();


                    handleMultiSensorData(multiSensorType, multiSensorValue);

//                    temperatureOutput.onNewMessage(multiSensorType, multiSensorValue);

//                    sensorTypeList.add(multiSensorType);
//                    sensorValueList.add(multiSensorValue);
//
//                    System.out.println(sensorTypeList);
//                    System.out.println(sensorValueList);

//                    System.out.println(multiSensorType);
//                    System.out.println(multiSensorValue);


                } else if (event instanceof ZWaveCommandClassValueEvent) {
                    System.out.println("Node " + ((ZWaveCommandClassValueEvent) event).getNodeId() +
                            "COMMAND CLASS NAME-> " +
                            ((ZWaveCommandClassValueEvent) event).getCommandClass().name() + ": " +
                            ((ZWaveCommandClassValueEvent) event).getValue());

                    commandClassType = ((ZWaveCommandClassValueEvent) event).getCommandClass().name();
                    commandClassValue = ((ZWaveCommandClassValueEvent) event).getValue().toString();

                    handleCommandClassData(commandClassType, commandClassValue);

                    motionOutput.onNewMessage(commandClassType, commandClassValue, false);
                    tamperAlarmOutput.onNewMessage(commandClassType, commandClassValue, false);

//                    System.out.println(commandClassType);
//                    System.out.println(commandClassValue);


                } else if (event instanceof ZWaveNodeStatusEvent) {
                    System.out.println(">> Received Node Info");
                    int nodeId = event.getNodeId();
                    System.out.println("- Node " + nodeId);
                    for (ZWaveCommandClass cmdClass : zController.getNode(nodeId).getCommandClasses(0))
                        System.out.println(cmdClass);


                } else if (event instanceof ZWaveInitializationStateEvent) {
                    System.out.println(">> Node (Final)" + event.getNodeId() + " " + ((ZWaveInitializationStateEvent) event).getStage());

                    System.out.println(zController.getNodes());


                    if (((ZWaveInitializationStateEvent) event).getStage() == ZWaveNodeInitStage.DISCOVERY_COMPLETE) {
                        if (zController.getNode(9) != null) {
                            System.out.println("NODE 9 " + zController.getNode(9).isAwake() + "________________");
                            System.out.println("NODE 1 " + zController.getNode(1).isAwake() + "________________");

                            ZWaveMultiLevelSensorCommandClass multiLevelSensorCommandClass =
                                    (ZWaveMultiLevelSensorCommandClass) zController.getNode(9).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_SENSOR_MULTILEVEL);

                            ZWaveWakeUpCommandClass wakeupCommandClass =
                                    (ZWaveWakeUpCommandClass) zController.getNode(9).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);

                            if (wakeupCommandClass != null) {
                                ZWaveCommandClassTransactionPayload cmd =
                                        wakeupCommandClass.setInterval(9, 1800);

                                ZWaveCommandClassTransactionPayload multi =
                                        multiLevelSensorCommandClass.getValueMessage();
                                ZWaveCommandClassTransactionPayload lum =
                                        multiLevelSensorCommandClass.getMessage(ZWaveMultiLevelSensorCommandClass.SensorType.getSensorType(3));
                                multiLevelSensorCommandClass.getSupportedSensorMessage();

//                                Collection<ZWaveCommandClassTransactionPayload> mo =
//                                        multiLevelSensorCommandClass.getDynamicValues(true);

                                zController.getNode(1).setAwake(true);
                                zController.sendData(lum);
                                zController.sendData(multi);
                                zController.sendData(cmd);
//                                zController.updateControllerProperty();
//                                zController.sendData((ZWaveCommandClassTransactionPayload) mo);

                                zController.getNode(9).setAwake(false);

                            }
                        }
                    }




//                            if (wakeupCommandClass.getTargetNodeId() == zController.getOwnNodeId()) {
//                                System.out.println(("NODE {}: Node advancer: SET_WAKEUP - TargetNode is set to " +
//                                        "controller" +
//                                        event.getNodeId()));
//                            } else {
//                                int value = zController.getSystemDefaultWakeupPeriod();
//                                if (wakeupCommandClass.getInterval() == 0 && value != 0) {
//                                    System.out.println("NODE {}: Node advancer: SET_WAKEUP - Interval is currently 0." +
//                                            " Set to {}" +
//                                            event.getNodeId() + value);
//                                } else {
//                                    wakeupCommandClass.setInterval(9,15);
//                                    System.out.println(wakeupCommandClass.getInterval());
//                                }
//                            }
//                            }
//                    }
                }
            }
//                                ZWaveCommandClassTransactionPayload cmd =
//                                zWaveWakeUpCommandClass.setInterval(9, 3000);
//                        zController.sendData(cmd);

//

//                    if (event.getNodeId() == 1) {
//
//                        // send command to thermostat to wake up more frequently
//                        ZWaveWakeUpCommandClass wakeUp =
//                                (ZWaveWakeUpCommandClass)zController.getNode(9).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
//                        ZWaveCommandClassTransactionPayload cmd = wakeUp.setInterval(9,1800);
//                        zController.sendData(cmd);
////
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

            //Multisensor types
            if (Objects.equals(sensorType, "Temperature")) {
                message = sensorValue;

                this.temperatureOutput.onNewMessage(message);
            } else if ("RelativeHumidity".equals(sensorType)) {
                message = sensorValue;

                relativeHumidityOutput.onNewMessage(message);
            } else if ("Luminance".equals(sensorType)) {
                message = sensorValue;

                this.luminanceOutput.onNewMessage(message);

            } else if ("Ultraviolet".equals(sensorType)) {
                message = sensorValue;

                ultravioletOutput.onNewMessage(message);

            } else {
                System.out.println("No multisensor detected");

            }
        }
        public void handleCommandClassData(String sensorType, String sensorValue){

            //Command Class Types
            if (Objects.equals(sensorType, "COMMAND_CLASS_BATTERY")) {
                message = sensorValue;

                this.batteryOutput.onNewMessage(message);
            }
            else if ("COMMAND_CLASS_BASIC".equals(sensorType)) {
                message = sensorValue;
            }
            else {
                System.out.println(message);
            }
        }
    }