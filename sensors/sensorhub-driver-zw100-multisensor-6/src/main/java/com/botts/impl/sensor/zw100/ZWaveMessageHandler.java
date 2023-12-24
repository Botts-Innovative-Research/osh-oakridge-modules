package com.botts.impl.sensor.zw100;

import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEventListener;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveAlarmCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveMultiLevelSensorCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveNodeStatusEvent;
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

    String message;

    String alarmType;
    Object alarmValue;
    String alarmStrValue;
    String multiSensorType;
    Object multiSensorValue;
    String multiSensorStrValue;
    String commandClassType;
    Object commandClassValue;
    String commandClassStrValue;


    public ZWaveMessageHandler(String message, MotionOutput motionOutput,
                               RelativeHumidityOutput relativeHumidityOutput,
                               TemperatureOutput temperatureOutput, LuminanceOutput luminanceOutput,
                               UltravioletOutput ultravioletOutput, TamperAlarmOutput tamperAlarmOutput,
                               BatteryOutput batteryOutput, LocationOutput locationOutput) {

        this.message = message;
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
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue());


                    alarmType = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmType().name();
                    alarmValue = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue();

                    alarmStrValue = alarmValue.toString();

                    System.out.println(alarmType);
                    System.out.println(alarmValue);

//                    handleAlarmData(alarmType, alarmStrValue);


                } else if (event instanceof ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) {
                    System.out.println("Node " + ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getNodeId() + " SENSOR TYPE-> " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel() + ": " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue());


                    multiSensorType =
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel();

                    multiSensorValue =
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue();
                    multiSensorStrValue = multiSensorValue.toString();

//                    handleMultiSensorData(multiSensorType, multiSensorStrValue);

                    System.out.println(multiSensorType);
                    System.out.println(multiSensorValue);


                } else if (event instanceof ZWaveCommandClassValueEvent) {
                    System.out.println("Node " + ((ZWaveCommandClassValueEvent) event).getNodeId() +
                            "COMMAND CLASS NAME-> " +
                            ((ZWaveCommandClassValueEvent) event).getCommandClass().name() + ": " +
                            ((ZWaveCommandClassValueEvent) event).getValue());

                    commandClassType = ((ZWaveCommandClassValueEvent) event).getCommandClass().name();
                    commandClassValue = ((ZWaveCommandClassValueEvent) event).getValue();

                    commandClassStrValue = commandClassValue.toString();

                    System.out.println(commandClassType);
                    System.out.println(commandClassStrValue);

//                    handleCommandClassData(commandClassType, commandClassStrValue);


                } else if (event instanceof ZWaveNodeStatusEvent) {
                    System.out.println(">> Received Node Info");
                    int nodeId = event.getNodeId();
                    System.out.println("- Node " + nodeId);
                    for (ZWaveCommandClass cmdClass : zController.getNode(nodeId).getCommandClasses(0))
                        System.out.println(cmdClass);


                } else if (event instanceof ZWaveInitializationStateEvent) {
                    System.out.println(">> Node (Final)" + event.getNodeId() + " " + ((ZWaveInitializationStateEvent) event).getStage());

                    /*if (event.getNodeId() == 1) // case of controller
                    {
                        System.out.println("Changing Wake-up interval of thermostat");

                        // send command to thermostat to wake up more frequently
                        ZWaveWakeUpCommandClass wakeUp = (ZWaveWakeUpCommandClass)zController.getNode(3).getCommandClass(CommandClass.WAKE_UP);
                        SerialMessage cmd = wakeUp.setInterval(1800);
                        zController.sendData(cmd);
                    }*/
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

//    MessageHandler Class
//    - Needs Outputs from Driver
//    - One function connects and creates listener
//    - One function parses messages based on Sensor Type and calls thisOutput.onMessage(data values)
//            ***This could all be done in one function if that makes more sense

//     Parses messages based on Sensor Type and calls thisOutput.onMessage(data values) in the corresponding output


    public static String onMessage(String message) {
        return message;
    }

    public void handleAlarmData(String sensorType, String sensorValue) {
        //Alarm types
        if ((Objects.equals(sensorType, "BURGLAR")) && (Objects.equals(sensorValue, "255"))) {
            message = "Triggered";
        } else {
            message = "Un-triggered";
        }
        //need to set a timeout to reset status

        this.motionOutput.onNewMessage(message);

    }
//        public void handleMultiSensorData (String sensorType, String sensorValue){
//
//            //Multisensor types
//            if (Objects.equals(sensorType, "Temperature")) {
//                message = sensorValue;
//
//                this.temperatureOutput.onNewMessage(message);
//            }
//            else if ("RelativeHumidity".equals(sensorType)) {
//                message = sensorValue;
//
////                relativeHumidityOutput.onNewMessage(message);
//            }
//            else if ("Luminance".equals(sensorType)) {
//                message = sensorValue;
//
//                this.luminanceOutput.onNewMessage(message);
//
//            }
//            else if ("Ultraviolet".equals(sensorType)) {
//                message = sensorValue;
//
////                ultravioletOutput.onNewMessage(message);
//
//            }
//            else{System.out.println("No multisensor detected");}

//        }
//        public void handleCommandClassData (String sensorType, String sensorValue){
//
//            //Command Class Types
//            if (Objects.equals(sensorType, "COMMAND_CLASS_BATTERY")) {
//                message = sensorValue;
//
//                this.batteryOutput.onNewMessage(message);
//            }
//            else if ("COMMAND_CLASS_BASIC".equals(sensorType)) {
//                message = sensorValue;
//            }
//            else {onMessage(message);}
////            System.out.println(message);
//
//
//        }


//    }
}



//    MotionOutput motionOutput, RelativeHumidityOutput relativeHumidityOutput,
//    TemperatureOutput temperatureOutput, LuminanceOutput luminanceOutput,
//    UltravioletOutput ultravioletOutput, TamperAlarmOutput tamperAlarmOutput,
//    BatteryOutput batteryOutput, LocationOutput locationOutput