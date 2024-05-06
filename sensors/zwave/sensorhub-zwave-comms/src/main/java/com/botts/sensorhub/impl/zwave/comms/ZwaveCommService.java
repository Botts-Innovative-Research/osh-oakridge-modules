/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023-2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEventListener;
import org.openhab.binding.zwave.internal.protocol.ZWaveMessagePayloadTransaction;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.sensor.ISensorDriver;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.comm.UARTConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.slf4j.LoggerFactory;

import java.sql.Driver;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class ZwaveCommService extends AbstractModule<ZwaveCommServiceConfig> implements IServiceModule<ZwaveCommServiceConfig>,
        Runnable {

    private boolean initialized = false;

    private Thread workerThread = null;

    private final AtomicBoolean doWork = new AtomicBoolean(false);

    private final List<IMessageListener> messageListeners = new ArrayList<>();
    public List<String> commSubscribers = new ArrayList<>();

    public ZwaveCommServiceConfig.AdminPanelNodeList moduleList = new ZwaveCommServiceConfig().adminPanelNodeList;

    UARTConfig uartConfig = new UARTConfig();
    RxtxZWaveIoHandler ioHandler;
    ZWaveController zController;

    @Override
    protected void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(ZwaveCommService.class);

        uartConfig.baudRate = 115200;
        uartConfig.portName = "COM5";

        ioHandler = new RxtxZWaveIoHandler(uartConfig);
        ioHandler.start(msg -> zController.incomingPacket(msg));
        zController = new ZWaveController(ioHandler);


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

                event.getNodeId();

                messageListeners.forEach(listener -> listener.onNewDataPacket(event.getNodeId(), event));
                moduleList.setCommSubscribers(commSubscribers);

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

    public void sendConfigurations(ZWaveMessagePayloadTransaction transaction){
        //method to implement zController inherent method of sendTransaction()
        zController.sendTransaction(transaction);
    }


    public ZWaveNode getZWaveNode(int nodeID){
        //method to implement zController inherent method of getNode()
        return zController.getNode(nodeID);
    }

    // Write a method that identifies other drivers that are using it
    // Create variable driverList to hold list of subscribed drivers
    //  1. If sensorDriver.commService = moduleRegistry.getModuleByType(this)
        //  then driverList.add(sensorDriver)


//    public ZwaveCommServiceConfig.AdminPanelNodeList getModuleList() {
//        return (ZwaveCommServiceConfig.AdminPanelNodeList) commSubscribers;
//    }

    public synchronized void registerDriver(String module) {
            commSubscribers.add(module);
    }
}