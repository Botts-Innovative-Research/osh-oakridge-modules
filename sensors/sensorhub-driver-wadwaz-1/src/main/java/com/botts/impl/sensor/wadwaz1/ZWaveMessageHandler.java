package com.botts.impl.sensor.wadwaz1;


import org.checkerframework.checker.units.qual.C;
import org.openhab.binding.zwave.handler.ZWaveControllerHandler;
import org.openhab.binding.zwave.handler.ZWaveThingHandler;
import org.openhab.binding.zwave.internal.protocol.*;
import org.openhab.binding.zwave.internal.protocol.commandclass.*;
import org.openhab.binding.zwave.internal.protocol.event.*;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveTransactionMessageBuilder;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.comm.UARTConfig;

import javax.validation.Payload;
import java.lang.reflect.Array;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class ZWaveMessageHandler {

    EntryAlarmOutput entryAlarmOutput;
    TamperAlarmOutput tamperAlarmOutput;
    ExternalSwitchAlarmOutput externalSwitchAlarmOutput;
    BatteryOutput batteryOutput;
    LocationOutput locationOutput;


    String alarmType;
    String alarmValue;
    String commandClassType;
    String commandClassValue;
    String message;
    Timer motionTimer = new Timer(); // creating timer


    public ZWaveMessageHandler(EntryAlarmOutput entryAlarmOutput, TamperAlarmOutput tamperAlarmOutput,
                               ExternalSwitchAlarmOutput externalSwitchAlarmOutput,
                               BatteryOutput batteryOutput, LocationOutput locationOutput) {

        this.entryAlarmOutput = entryAlarmOutput;
        this.tamperAlarmOutput = tamperAlarmOutput;
        this.externalSwitchAlarmOutput = externalSwitchAlarmOutput;
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

                    System.out.println(alarmType);
                    System.out.println(alarmValue);


                } else if (event instanceof ZWaveCommandClassValueEvent) {
                    System.out.println("Node " + ((ZWaveCommandClassValueEvent) event).getNodeId() +
                            "COMMAND CLASS NAME-> " +
                            ((ZWaveCommandClassValueEvent) event).getCommandClass().name() + ": " +
                            ((ZWaveCommandClassValueEvent) event).getValue());



                    commandClassType = ((ZWaveCommandClassValueEvent) event).getCommandClass().name();
                    commandClassValue = ((ZWaveCommandClassValueEvent) event).getValue().toString();

                    handleCommandClassData(commandClassType, commandClassValue);

                    entryAlarmOutput.onNewMessage(commandClassType, commandClassValue, false);

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
//                    System.out.println(zController.getNode(13).getNodeInitStage());


                    if (((ZWaveInitializationStateEvent) event).getStage() == ZWaveNodeInitStage.STATIC_VALUES && (zController.getNode(13).getNodeInitStage() == ZWaveNodeInitStage.DONE) && (zController.getNode(1) != null) && (zController.getNode(13) != null)) {
//                      if ((zController.getNode(13).getNodeInitStage() == ZWaveNodeInitStage.DONE) && (zController.getNode(1) != null) && (zController.getNode(13) != null)) {
//                          if ((zController.getNode(13).getNodeInitStage() == ZWaveNodeInitStage.DONE) && (zController.getNode(1).getNodeInitStage() == ZWaveNodeInitStage.DONE)) {


                        System.out.println(zController.getNodes());
                        System.out.println(zController.getNode(13).getCommandClasses(0));

//                        ZWaveBatteryCommandClass zWaveBatteryCommandClass =
//                                (ZWaveBatteryCommandClass) zController.getNode(13).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);
//
//                        System.out.println(zController.sendTransaction(zWaveBatteryCommandClass.getValueMessage()));


//                        ZWaveAlarmCommandClass alarmCommandClass =
//                                (ZWaveAlarmCommandClass) zController.getNode(13).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_ALARM);
//                        ZWaveCommandClassTransactionPayload sendReport =
//                                alarmCommandClass.getMessage(ZWaveAlarmCommandClass.AlarmType.BURGLAR, 0);
//                        zController.sendTransaction(sendReport);
//                        System.out.println(zController.sendTransaction(sendReport));
//                        System.out.println("GOT THE REPORT");

                        ZWaveWakeUpCommandClass wakeupCommandClass =
                                (ZWaveWakeUpCommandClass) zController.getNode(13).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
                        if (wakeupCommandClass != null) {
                            ZWaveCommandClassTransactionPayload wakeUp =
                                    wakeupCommandClass.setInterval(13, 600);
                            //minInterval = 600; intervals must set in 200 second increments
                            zController.sendTransaction(wakeUp);
                            System.out.println("INTERVAL MESSAGE: _____" + zController.sendTransaction(wakeupCommandClass.getIntervalMessage()));
                            System.out.println(((ZWaveWakeUpCommandClass) zController.getNode(13).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP)).getInterval());
                            System.out.println(zController.sendTransaction(wakeUp));
                        }


                        ZWaveBatteryCommandClass battery =
                                (ZWaveBatteryCommandClass) zController.getNode(13).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_BATTERY);
                        ZWaveCommandClassTransactionPayload batteryCheck = battery.getValueMessage();
                        System.out.println(zController.sendTransaction(batteryCheck));
                        System.out.println("THIS IS THE BATTERY CHECK");

//};
//                    if (event.getNodeId() == 1) {
//
//                        // send command to thermostat to wake up more frequently
//                        ZWaveWakeUpCommandClass wakeUp =
//                                (ZWaveWakeUpCommandClass)zController.getNode(9).getCommandClass(ZWaveCommandClass.CommandClass.COMMAND_CLASS_WAKE_UP);
//                        ZWaveCommandClassTransactionPayload cmd = wakeUp.setInterval(9,1800);
//                        zController.sendData(cmd);
//                });

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