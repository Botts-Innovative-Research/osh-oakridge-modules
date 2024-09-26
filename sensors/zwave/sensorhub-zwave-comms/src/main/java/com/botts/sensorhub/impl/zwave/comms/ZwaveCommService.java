/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023-2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import org.openhab.binding.zwave.handler.ZWaveControllerHandler;
import org.openhab.binding.zwave.handler.ZWaveThingHandler;
import org.openhab.binding.zwave.internal.protocol.*;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingImpl;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.comm.UARTConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openhab.binding.zwave.ZWaveBindingConstants.*;


public class ZwaveCommService extends AbstractModule<ZwaveCommServiceConfig> implements IServiceModule<ZwaveCommServiceConfig>,
        Runnable {

    private boolean initialized = false;

    private Thread workerThread = null;

    private final AtomicBoolean doWork = new AtomicBoolean(false);

    private final List<IMessageListener> messageListeners = new ArrayList<>();

    UARTConfig uartConfig = new UARTConfig();
    RxtxZWaveIoHandler ioHandler;
//    com.botts.sensorhub.impl.zwave.comms.ZWaveSerialHandler serialHandler;
    public ZWaveController zController;
    public ThingUID thingUID;
    public ThingTypeUID bridgeUID;
    public Thing thing;
    public Bridge controller;
    public ZWaveAssociationGroup associationGroup;
    public ZWaveAssociation association;
    public ZWaveControllerHandler controllerHandler;
    public ZWaveThingHandler handler;
    public SerialPortManager serialPortManager;

    @Override
    protected void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(ZwaveCommService.class);
//
        uartConfig.baudRate = config.baudRate;
        uartConfig.portName = config.portName;


//        bridgeUID = new ThingTypeUID("zwave", "aeon_zw090_00_000");
        bridgeUID = CONTROLLER_SERIAL;
        thingUID = new ThingUID(bridgeUID, "controller");
        controller = new BridgeImpl(bridgeUID, thingUID);
        controller.setBridgeUID(thingUID);
        thing = new ThingImpl(bridgeUID, "controller");
        thing.setBridgeUID(controller.getBridgeUID());

        controller.getConfiguration().put(CONFIGURATION_NODEID, 1);
        controller.getConfiguration().put(CONFIGURATION_NETWORKKEY, "XXX");

        thing.getConfiguration().put(CONFIGURATION_NODEID, 1);


        Map<String, String> config = new HashMap();
        config.put("masterController", "true");
        config.put("sucNode", "1");


        ioHandler = new RxtxZWaveIoHandler(uartConfig);
//        serialHandler = new ZWaveSerialHandler(controller, serialPortManager);
        ioHandler.start(msg -> zController.incomingPacket(msg));
//        serialHandler.setRxtxHandler(ioHandler);
//        serialHandler.initialize();
        zController = new ZWaveController(ioHandler, config);

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

//        logger.info("The controller is master: " + zController.isMasterController());
        zController.addEventListener(new ZWaveEventListener() {

            public void ZWaveIncomingEvent(ZWaveEvent event) {
                logger.info("EVENT: " + event);

                //
                event.getNodeId();

                Collection<ZWaveNode> nodeList = zController.getNodes();
                config.nodeList.setCommSubscribers(nodeList);

                messageListeners.forEach(listener -> listener.onNewDataPacket(event.getNodeId(), event));

//                nodeList.forEach(node -> zController.getNode(node.getNodeId()).setAssociationGroup(group));


                associationGroup = new ZWaveAssociationGroup(1);

//                association = new ZWaveAssociation(zController.getOwnNodeId());
//                if (!associationGroup.isAssociated(association)) {
//                    associationGroup.addAssociation(association);
//                    logger.info("These are the association group command classes: " + associationGroup.getCommandClasses());
//                }


//                if (setAssociation(2) != null && !associationGroup.isAssociated(setAssociation(2))){
//                    associationGroup.addAssociation(setAssociation(2));
//                    logger.info("Node 2 associated with the controller: " + associationGroup.getAssociations());
//                }


//                    nodeList.forEach(node -> {
//                                if (!associationGroup.isAssociated(setAssociation(node.getNodeId()))) {
//                                    associationGroup.addAssociation(setAssociation(node.getNodeId()));
//                                }
//                            });



//                List<ZWaveAssociation> associations = new ArrayList<>();
//                associations.forEach(messageListeners -> messageListeners.getNode());
//                associationGroup.setAssociations();
//                if (group.getAssociationCnt() != 5) {
//                    nodeList.forEach(node -> setAssociations(node.getNodeId()));
//                }
//                group.getAssociations();
//                group.getCommandClasses();

                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        zController.shutdown();
                        ioHandler.stop();
                    }
                });
            }
        });
    }
//    public void setAssociations(int nodeId) {
//
//        ZWaveAssociation association = new ZWaveAssociation(nodeId);
//        group.addAssociation(association);
//    }

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

    public void sendConfigurations(ZWaveMessagePayloadTransaction transaction){
        //method to implement zController inherent method of sendTransaction()
        zController.sendTransaction(transaction);
    }

    public ZWaveNode getZWaveNode(int nodeID){
        //method to implement zController inherent method of getNode()
        return zController.getNode(nodeID);
    }

    public RxtxZWaveIoHandler getRxtxHandler (RxtxZWaveIoHandler rxtxZWaveIoHandler){
        return rxtxZWaveIoHandler;
    }
    public ZWaveController getzController() {
        return zController;
    }

    public ZWaveAssociationGroup getAssociationGroup() {
        return associationGroup;
    }

    public ZWaveAssociation setAssociation(int nodeID){
        return new ZWaveAssociation(nodeID);
    }
}