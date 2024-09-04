/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023-2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import org.openhab.binding.zwave.handler.ZWaveSerialHandler;
import org.openhab.binding.zwave.internal.protocol.*;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveMultiInstanceCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeInitStage;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveNodeSerializer;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.type.BridgeType;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.comm.UARTConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class ZwaveCommService extends AbstractModule<ZwaveCommServiceConfig> implements IServiceModule<ZwaveCommServiceConfig>,
        Runnable {

    private boolean initialized = false;

    private Thread workerThread = null;

    private final AtomicBoolean doWork = new AtomicBoolean(false);

    private final List<IMessageListener> messageListeners = new ArrayList<>();

    UARTConfig uartConfig = new UARTConfig();
    RxtxZWaveIoHandler ioHandler;
    public ZWaveController zController;
    public ZWaveNode node;
    public String thingUID;
    public Thing thing;
    public Bridge bridge;

    @Override
    protected void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(ZwaveCommService.class);
//
        uartConfig.baudRate = config.baudRate;
        uartConfig.portName = config.portName;

        ThingTypeUID bridgeUID = new ThingTypeUID("zwave:serial_zstick");
        Bridge controller = new BridgeImpl(bridgeUID,"zwave-serial_zstick-40a62c8264");


        ioHandler = new RxtxZWaveIoHandler(uartConfig);
        ioHandler.start(msg -> zController.incomingPacket(msg));
        zController = new ZWaveController(ioHandler);


//        try {
//            ioHandler.start(msg -> zController.incomingPacket(msg));
//        } catch (NullPointerException nullPointerException){
//            if (this.zController == null){
//                logger.info("Restart the Comm Service");
//            }
//        }


//        try {
//            ioHandler.start(msg -> zController.incomingPacket(msg));
//        } catch (NullPointerException e) {
//            while (zController == null){
//                try {
//                    wait(5000);
//                    ioHandler.start(msg -> zController.incomingPacket(msg));
//
//                } catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        } finally {
//            zController = new ZWaveController(ioHandler);
//        }

        workerThread = new Thread(this, this.getClass().getSimpleName());

        initialized = true;
    }

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

        if (ioHandler != null) {

            try {

                ioHandler.stop();

            } catch (Exception e) {

                logger.error("Uncaught exception attempting to stop comms module", e);

            } finally {

                ioHandler = null;
            }
        }

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
        //adds an event listener and sends the incoming events to the subscribed messageListeners
        zController.addEventListener(new ZWaveEventListener() {


            public void ZWaveIncomingEvent(ZWaveEvent event) {
                logger.info("EVENT: " + event);

                //
                event.getNodeId();

                Collection<ZWaveNode> nodeList = zController.getNodes();
                config.nodeList.setCommSubscribers(nodeList);

                messageListeners.forEach(listener -> listener.onNewDataPacket(event.getNodeId(), event));

                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        zController.shutdown();
                        ioHandler.stop();
                    }
                });
            }
        });
}


    public synchronized void registerListener(IMessageListener listener) {
        // registers drivers to the comm service
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


    public ZWaveNode createZWaveNode(ZWaveController zController, int configNodeId) {
        boolean serializedOk = false;
        ZWaveNode node = null;

        ZWaveNodeSerializer nodeSerializer = new ZWaveNodeSerializer();
        node = nodeSerializer.deserializeNode(zController.getHomeId(), configNodeId);

        if (node != null) {
            if (node.getManufacturer() != Integer.MAX_VALUE && node.getHomeId() == zController.getHomeId() && node.getNodeId() == configNodeId) {
                serializedOk = true;
                logger.debug("NODE {}: Restore from config: Ok.", configNodeId);
                node.setRestoredFromConfigfile(zController);
                Iterator var4 = node.getCommandClasses(0).iterator();

                while(var4.hasNext()) {
                    ZWaveCommandClass commandClass = (ZWaveCommandClass)var4.next();
                    commandClass.initialise(node, zController, (ZWaveEndpoint)null);
                    if (commandClass instanceof ZWaveEventListener) {
                        zController.addEventListener((ZWaveEventListener)commandClass);
                    }

                    if (commandClass instanceof ZWaveMultiInstanceCommandClass) {
                        for(int endpointNumber = 1; endpointNumber < node.getEndpointCount(); ++endpointNumber) {
                            ZWaveEndpoint endPoint = node.getEndpoint(endpointNumber);
                            Iterator var8 = endPoint.getCommandClasses().iterator();

                            while(var8.hasNext()) {
                                ZWaveCommandClass endpointCommandClass = (ZWaveCommandClass)var8.next();
                                endpointCommandClass.initialise(node, zController, endPoint);
                                if (endpointCommandClass instanceof ZWaveEventListener) {
                                    zController.addEventListener((ZWaveEventListener)endpointCommandClass);
                                }
                            }
                        }
                    }
//
//                    if (commandClass instanceof ZWaveSecurityCommandClass) {
//                        ((ZWaveSecurityCommandClass)commandClass).setNetworkKey(zController.networkSecurityKey);
//                    }
                }
            } else {
                logger.warn("NODE {}: Restore from config: Error. Data invalid, ignoring config.", configNodeId);
                node = null;
            }
        }

        if (node == null) {
            node = new ZWaveNode(zController.getHomeId(), configNodeId, zController);
        }

        if (configNodeId == zController.getOwnNodeId()) {
            node.setDeviceId(zController.getDeviceId());
            node.setDeviceType(zController.getDeviceType());
            node.setManufacturer(zController.getManufactureId());
        }

        if (!zController.getNodes().contains(node)) {
            zController.getNodes().add(node);
            if (serializedOk) {
                ZWaveEvent zEvent = new ZWaveInitializationStateEvent(node.getNodeId(), ZWaveNodeInitStage.DISCOVERY_COMPLETE);
                zController.notifyEventListeners(zEvent);
            }
        }
        return node;
    }


    public void sendConfigurations(ZWaveMessagePayloadTransaction transaction){
        //method to implement zController inherent method of sendTransaction()
        zController.sendTransaction(transaction);
    }


    public ZWaveNode getZWaveNode(int nodeID){
        //method to implement zController inherent method of getNode()
        return zController.getNode(nodeID);
    }

    public ZWaveController getzController() {
        return zController;
    }

    // Write a method that identifies other drivers that are using it
    // Create variable driverList to hold list of subscribed drivers
    //  1. If sensorDriver.commService = moduleRegistry.getModuleByType(this)
        //  then driverList.add(sensorDriver)


//    public ZwaveCommServiceConfig.AdminPanelNodeList getModuleList() {
//        return (ZwaveCommServiceConfig.AdminPanelNodeList) commSubscribers;
//    }

//    public synchronized void registerDriver(String module) {
//            commSubscribers.add(module);
//    }
}