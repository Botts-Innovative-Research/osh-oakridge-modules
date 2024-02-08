///***************************** BEGIN LICENSE BLOCK ***************************
//
// Copyright (C) 2023-2024 Botts Innovative Research, Inc. All Rights Reserved.
//
// ******************************* END LICENSE BLOCK ***************************/
//package com.botts.sensorhub.impl.tstar.comms;
//
//import com.google.gson.JsonObject;
//import com.google.gson.JsonStreamParser;
//import com.google.gson.stream.JsonWriter;
//import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.IServiceModule;
//import org.sensorhub.impl.module.AbstractModule;
//import org.slf4j.LoggerFactory;
//
import java.io.IOException;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
//
//public class CommService extends AbstractModule<CommServiceConfig> implements IServiceModule<CommServiceConfig>, Runnable {
//
//    private boolean initialized = false;
//
//    private InetAddress remoteAddress = null;
//
//    private DatagramSocket datagramSocket = null;
//
//    private int maxMessageBufferSize = CommServiceConfig.DEFAULT_BUFFER_SIZE;
//
//    private Thread workerThread = null;
//
//    private final AtomicBoolean doWork = new AtomicBoolean(false);
//
//    private final List<IMessageListener> messageListeners = new ArrayList<>();
//
//    @Override
//    protected void doInit() throws SensorHubException {
//
//        super.doInit();
//
//        logger = LoggerFactory.getLogger(CommService.class);
//
//        CommServiceConfig config = getConfiguration();
//
//        try {
//
//            remoteAddress = InetAddress.getByName(config.remoteAddress);
//
//            datagramSocket = new DatagramSocket(config.localPort);
//
//            datagramSocket.setBroadcast(false);
//
//        } catch (IOException exception) {
//
//            throw new SensorHubException("Failed to create DatagramSocket", exception);
//        }
//
//        if (config.bufferSize > 0) {
//
//            maxMessageBufferSize = config.bufferSize;
//        }
//
//        workerThread = new Thread(this, this.getClass().getSimpleName());
//
//        initialized = true;
//    }
//
//    @Override
//    protected void doStart() throws SensorHubException {
//
//        if (!initialized) {
//
//            doInit();
//        }
//
//        super.doStart();
//
//        doWork.set(true);
//
//        workerThread.start();
//    }
//
//    @Override
//    protected void doStop() throws SensorHubException {
//
//        super.doStop();
//
//        doWork.set(false);
//
//        if (workerThread != null && workerThread.isAlive()) {
//
//            try {
//
//                // Wait for thread to end
//                workerThread.join(5000);
//
//            } catch (InterruptedException e) {
//
//                getLogger().error("Thread {} interrupted", workerThread.getName());
//
//                workerThread.interrupt();
//            }
//
//            workerThread = null;
//        }
//
//        datagramSocket.close();
//
//        datagramSocket = null;
//
//        messageListeners.clear();
//
//        initialized = false;
//    }
//
//    @Override
//    public void run() {
//
//        ByteBuffer buffer = ByteBuffer.wrap(new byte[maxMessageBufferSize]);
//
//        while (doWork.get()) {
//
//            DatagramPacket receivedPacket = new DatagramPacket(buffer.array(), maxMessageBufferSize);
//
//            try {
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
//
//                buffer.clear();
//
//            } catch (IOException exception) {
//
//                // If exception occurs while still processing
//                if (doWork.get()) {
//
//                    // Log the error but move on
//                    getLogger().error("Exception receiving latest packet", exception);
//                }
//            }
//        }
//    }
//
//    public synchronized void registerListener(IMessageListener listener) {
//
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
//
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
//
//            if (datagramSocket.isClosed()) {
//
//                getLogger().error("Socket is closed");
//                throw new IOException("Socket is closed");
//            }
//
//            datagramSocket.send(datagramPacket);
//
//        } catch (SecurityException | PortUnreachableException |
//                 IllegalBlockingModeException | IllegalArgumentException exception) {
//
//            getLogger().error("Exception occurred sending packet", exception);
//        }
//    }
//}