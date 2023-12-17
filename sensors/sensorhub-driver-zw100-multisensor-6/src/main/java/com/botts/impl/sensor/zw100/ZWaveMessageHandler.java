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
import org.sensorhub.impl.comm.rxtx.RxtxSerialCommProviderConfig;

public class ZWaveMessageHandler {

    //Create connection to Zwave
    public void ZWaveConnect(String portName, int baudRate) {

        UARTConfig uartConfig = new UARTConfig();

        uartConfig.portName = portName;
        uartConfig.baudRate = baudRate;

        RxtxZWaveIoHandler ioHandler = new RxtxZWaveIoHandler(uartConfig);
        ZWaveController zController = new ZWaveController(ioHandler);
        ioHandler.start(msg -> zController.incomingPacket(msg));

        zController.addEventListener(new ZWaveEventListener()
        {
            public void ZWaveIncomingEvent(ZWaveEvent event)
            {
                System.out.println("EVENT: " + event);

                if (event instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent)
                {
                    System.out.println("Node " + ((ZWaveCommandClassValueEvent) event).getNodeId()+ " RETRIEVING " +
                            "ALARM TYPE" +
                            "-> " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent)event).getAlarmType().name() + " Alarm: " +
                            ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue());
                }

                else if (event instanceof ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent)
                {
                    System.out.println("Node " + ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getNodeId()+ " RETRIEVING SENSOR TYPE-> " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel() + ": " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue());
                }

                else if (event instanceof ZWaveCommandClassValueEvent)
                {
                    System.out.println("Node " + ((ZWaveCommandClassValueEvent) event).getNodeId()+ " RETRIEVING " +
                            "COMMAND CLASS NAME-> " +
                            ((ZWaveCommandClassValueEvent) event).getCommandClass().name() + ": " +
                            ((ZWaveCommandClassValueEvent) event).getValue());
                }

                else if (event instanceof ZWaveNodeStatusEvent)
                {
                    System.out.println(">> Received Node Info");
                    int nodeId = event.getNodeId();
                    System.out.println("- Node " + nodeId);
                    for (ZWaveCommandClass cmdClass: zController.getNode(nodeId).getCommandClasses(0))
                        System.out.println(cmdClass);
                }

                else if (event instanceof ZWaveInitializationStateEvent)
                {
                    System.out.println(">> Node (Final)" + event.getNodeId() + " " + ((ZWaveInitializationStateEvent)event).getStage());

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
            public void run()
            {
                zController.shutdown();
                ioHandler.stop();
            }
        });

        handleData();
    }


    // Parses messages based on Sensor Type and calls thisOutput.onMessage(data values) in the corresponding output
    public void handleData() {



    }


}
