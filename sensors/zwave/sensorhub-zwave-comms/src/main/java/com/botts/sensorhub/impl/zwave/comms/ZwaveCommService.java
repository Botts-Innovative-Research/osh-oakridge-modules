/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023-2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import com.google.gson.stream.JsonWriter;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.IOException;

import org.sensorhub.impl.comm.UARTConfig;


public class ZwaveCommService extends AbstractModule<ZwaveCommServiceConfig> implements IServiceModule<ZwaveCommServiceConfig>,
        Runnable {

    private boolean initialized = false;

    private Thread workerThread = null;

    private final AtomicBoolean doWork = new AtomicBoolean(false);

    private final List<IMessageListener> messageListeners = new ArrayList<>();

    @Override
    protected void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(ZwaveCommService.class);

        ZwaveCommServiceConfig zwaveCommServiceConfig = getConfiguration();

            UARTConfig uartConfig = new UARTConfig();

            uartConfig.portName = zwaveCommServiceConfig.portName;
            uartConfig.baudRate = zwaveCommServiceConfig.baudRate;

            RxtxZWaveIoHandler ioHandler = new RxtxZWaveIoHandler(uartConfig);
            ZWaveController zController = new ZWaveController(ioHandler);
            ioHandler.start(msg -> zController.incomingPacket(msg));

           Collection zWaveNodes = zController.getNodes();


           logger.info(zWaveNodes.toString());


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
    }

//    public synchronized void registerListener(IMessageListener listener) {
        //create a map
            //Map <String, List<String>>

//        if (!messageListeners.contains(listener)) {
//
//            messageListeners.add(listener);
//
//            getLogger().info("Registered packet listener");
//
//        } else {
//
//            getLogger().warn("Attempt to register listener that is already registered");
//        }
//    }
//
//    public synchronized void unregisterListener(IMessageListener listener) {
//
//        if (messageListeners.contains(listener) && messageListeners.remove(listener)) {
//
//            getLogger().info("Unregistered packet listener");
//
//        } else {
//
//            getLogger().warn("Attempt to unregister listener that is not registered");
//        }
//    }
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
        }
//    }
//}