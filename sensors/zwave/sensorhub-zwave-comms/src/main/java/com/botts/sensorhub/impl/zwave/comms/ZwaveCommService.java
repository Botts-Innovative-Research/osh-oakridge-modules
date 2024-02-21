/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023-2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEventListener;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveAlarmCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveMultiLevelSensorCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveNodeStatusEvent;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.comm.UARTConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class ZwaveCommService extends AbstractModule<ZwaveCommServiceConfig> implements IServiceModule<ZwaveCommServiceConfig>,
        Runnable {

    private boolean initialized = false;

    private Thread workerThread = null;

    private final AtomicBoolean doWork = new AtomicBoolean(false);

    private final List<IMessageListener> messageListeners = new ArrayList<>();

    UARTConfig uartConfig = new UARTConfig();
    RxtxZWaveIoHandler ioHandler;
    ZWaveController zController;
    Collection zWaveNodes;

    @Override
    protected void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(ZwaveCommService.class);

        uartConfig.baudRate = 115200;
        uartConfig.portName = "COM5";

        ioHandler = new RxtxZWaveIoHandler(uartConfig);
        ioHandler.start(msg -> zController.incomingPacket(msg));
        zController = new ZWaveController(ioHandler);


        workerThread = new Thread(this, this.getClass().getSimpleName());

        initialized = true;


        zController.addEventListener(new ZWaveEventListener() {
            public void ZWaveIncomingEvent(ZWaveEvent event) {
                logger.info("EVENT: " + event);

                if (event instanceof ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) {
                    logger.info("Node " + ((ZWaveCommandClassValueEvent) event).getNodeId() +
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


                    int alarmEvent = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmEvent();
                    String alarmType = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getAlarmType().name();
                    String alarmValue = ((ZWaveAlarmCommandClass.ZWaveAlarmValueEvent) event).getValue().toString();
//
//                    tamperAlarmOutput.onNewMessage(alarmType, alarmValue, alarmEvent, false);
//
                    logger.info("Alarm Event: " + alarmEvent);
                    logger.info("Alarm Type: " + alarmType);
                    logger.info("Alarm Value: " + alarmValue);


                } else if (event instanceof ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) {
                    logger.info("Node " + ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getNodeId() + " SENSOR TYPE-> " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel() + ": " +
                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue());

//                    multiSensorType =
//                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getSensorType().getLabel();
//
//                    multiSensorValue =
//                            ((ZWaveMultiLevelSensorCommandClass.ZWaveMultiLevelSensorValueEvent) event).getValue().toString();
//
//
//                    handleMultiSensorData(multiSensorType, multiSensorValue);
//
//                    System.out.println("Multisensor Type: " + multiSensorType);
//                    System.out.println("Multisensor Value: " + multiSensorValue);


                } else if (event instanceof ZWaveCommandClassValueEvent) {
                    logger.info("Node " + ((ZWaveCommandClassValueEvent) event).getNodeId() +
                            "COMMAND CLASS NAME-> " +
                            ((ZWaveCommandClassValueEvent) event).getCommandClass().name() + ": " +
                            ((ZWaveCommandClassValueEvent) event).getValue());


                    String commandClassType = ((ZWaveCommandClassValueEvent) event).getCommandClass().name();
                    String commandClassValue = ((ZWaveCommandClassValueEvent) event).getValue().toString();
//
//                    handleCommandClassData(commandClassType, commandClassValue);
//
//                    motionOutput.onNewMessage(commandClassType, commandClassValue, false);
//
                    logger.info("Command Class Type: " + commandClassType);
                    logger.info("Command Class Value: " + commandClassValue);


                } else if (event instanceof ZWaveNodeStatusEvent) {
                    logger.info(">> Received Node Info");
                    int nodeId = event.getNodeId();
                    logger.info("- Node " + nodeId);
                    for (ZWaveCommandClass cmdClass : zController.getNode(nodeId).getCommandClasses(0))
                        logger.info(cmdClass.toString());


                } else if (event instanceof ZWaveInitializationStateEvent) {
                    logger.info(">> Node (Final)" + event.getNodeId() + " " + ((ZWaveInitializationStateEvent) event).getStage());

                    logger.info(zController.getNodes().toString());



                    zWaveNodes = zController.getNodes();

                    logger.info(zWaveNodes.toString());
//                    logger.info(zWaveNodes.forEach();

//                    ArrayList nodeIDs = new ArrayList<Object>();

//                    for(int i = 0; i<zWaveNodes.size(); i++) {
//
//                        nodeIDs.add(zController.getNode(i).getManufacturer());
//                        logger.info(String.valueOf(nodeIDs));
//                    }

//                    handleNodeList(zWaveNodes, nodeIDs);

                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        public void run() {
                            zController.shutdown();
                            ioHandler.stop();
                        }
                    });
                }
            }
        });
    }

//    public void handleNodeCollection(ZWaveController zController, Collection nodeList) {
//        ArrayList nodeIDs = new ArrayList<Object>();
//
//        for(int i = 0; i<nodeList.size(); i++) {
//            nodeIDs.add(zController.getNode(i).getManufacturer());
//            logger.info(String.valueOf(nodeIDs));
//        }
//
//         for (Iterator ii = nodeList.iterator(); ii.hasNext();) {
//            Object nextNode = ii.next();
//            if (nextNode == null) {
//                ii.remove();
//         for (Iterator iii = nodeIDs.iterator(); iii.hasNext();) {
//             Object nextID = iii.next();
//             if (nextID == "7FFFFFFF") {
//                 ii.remove();
//             }
//         }
//
//    }
//    public void handleNodeList(Collection nodeList) {
//        for (Iterator i = nodeList.iterator(); i.hasNext();) {
//            Object nextNode = i.next();
//
//            if (nextNode == null) {
//                i.remove();
//                logger.info("Empty node has been removed");
//
//                for (Iterator ii = nodeIDs.iterator(); ii.hasNext();) {
//                    Object nodeID = ii.next();
//
//                    if (nodeID == "7FFFFFFF") {
//                        ii.remove();
//                        logger.info("Unidentified node removed)");
//                    }
//                }
//
//                break;
//            }
//            logger.info("Available nodes are: ");
//            for (Object n: nodeList) {
//                logger.info(nodeList.toString());
//            }
//        }
//    }

    @Override
    protected void doStart() throws SensorHubException {

        if (!initialized) {

            doInit();
        }

        super.doStart();

        doWork.set(true);

        workerThread.start();
    }

    @Override
    protected void doStop() throws SensorHubException {

        super.doStop();

        doWork.set(false);

        if (workerThread != null && workerThread.isAlive()) {

            try {

                // Wait for thread to end
                workerThread.join(5000);

            } catch (InterruptedException e) {

                getLogger().error("Thread {} interrupted", workerThread.getName());

                workerThread.interrupt();
            }

            workerThread = null;
        }

        messageListeners.clear();

        initialized = false;
    }

    @Override
    public void run() {
//        zController.addEventListener(new ZWaveEventListener() {
//            public void ZWaveIncomingEvent(ZWaveEvent event) {
//                logger.info("EVENT: " + event);
//
//                zController.getNode(19).initialiseNode();
//                zController.getNode(19).getNodeInitStage();
//
//                zController.identifyNode(19);
//            }});
    }
//}

//    }
//        while (doWork.get()) {
//
//            DatagramPacket receivedPacket = new DatagramPacket(buffer.array(), maxMessageBufferSize);
//
//            try {
//                initialized = true;
//            }
//
//                datagramSocket.receive(receivedPacket);
//
//                byte[] data = Arrays.copyOf(receivedPacket.getData(), receivedPacket.getLength());
//
//                JsonStreamParser jsonStreamParser = new JsonStreamParser(new String(data));
//
//                if (jsonStreamParser.hasNext()) {
//
//                    JsonObject jsonObject = jsonStreamParser.next().getAsJsonObject();
//
//                    String id = jsonObject.get("participantId").getAsString();
//                    String message = jsonObject.get("data").getAsString();
//
//                    if (id != null && message != null) {
//
//                        messageListeners.forEach(listener -> listener.onNewDataPacket(id, message));
//                    }
//                }

//                buffer.clear();

//            } catch (IOException exception) {

                // If exception occurs while still processing
//                if (doWork.get()) {
//
                    // Log the error but move on
//                    getLogger().error("Exception receiving latest packet", exception);
//                }
//            }
//        }
//    }

    public synchronized void registerListener(IMessageListener listener) {
//        create a map
//            Map <String, List<String>>

        if (!messageListeners.contains(listener)) {

            messageListeners.add(listener);

            getLogger().info("Registered packet listener");

        } else {

            getLogger().warn("Attempt to register listener that is already registered");
        }
    }

    public synchronized void unregisterListener(IMessageListener listener) {

        if (messageListeners.contains(listener) && messageListeners.remove(listener)) {

            getLogger().info("Unregistered packet listener");

        } else {

            getLogger().warn("Attempt to unregister listener that is not registered");
        }
    }
//
//    public synchronized void sendPacket(String id, String message) throws IOException {
////this is where we send configurations
//        StringWriter stringWriter = new StringWriter();
//        JsonWriter jsonWriter = new JsonWriter(stringWriter);
//
//        jsonWriter.beginObject();
//        jsonWriter.name("participantId").value(id);
//        jsonWriter.name("data").value(message);
//        jsonWriter.endObject();
//        jsonWriter.close();
//
//        byte[] buffer = stringWriter.toString().getBytes();
//
//        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, remoteAddress, config.remotePort);
//
//        try {

//            if (datagramSocket.isClosed()) {
//
//                getLogger().error("Socket is closed");
//                throw new IOException("Socket is closed");
//            }
//
//            datagramSocket.send(datagramPacket);

//        } catch (SecurityException | PortUnreachableException |
//                 IllegalBlockingModeException | IllegalArgumentException exception) {
//
//            getLogger().error("Exception occurred sending packet", exception);
//        }
//    }
}