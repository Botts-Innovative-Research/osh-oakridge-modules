//package com.botts.sensorhub.impl.zwave.comms;
//
//import gnu.io.CommPortIdentifier;
//import gnu.io.NoSuchPortException;
//import gnu.io.SerialPort;
//import org.openhab.binding.zwave.internal.protocol.ZWaveController;
//import org.openhab.core.io.transport.serial.SerialPortIdentifier;
//import org.openhab.core.io.transport.serial.SerialPortManager;
//import org.openhab.core.io.transport.serial.internal.SerialPortManagerImpl;
//import org.openhab.core.io.transport.serial.internal.SerialPortRegistry;
//import org.openhab.core.thing.Bridge;
//import org.openhab.core.thing.ChannelUID;
//import org.openhab.core.thing.binding.BaseThingHandler;
//import org.openhab.core.types.Command;
//
//import javax.annotation.Nullable;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.util.stream.Stream;
//
//public class ZWaveSerialHandler extends org.openhab.binding.zwave.handler.ZWaveSerialHandler implements SerialPortManager {
//    String portId;
//    Bridge thing;
//    InputStream is;
//    OutputStream os;
//    SerialPort serialPort;
//    RxtxZWaveIoHandler rxtxHandler;
//    Thread recieveThread;
//    private ZWaveController controller;
//    CommPortIdentifier serialPortManager;
//    BaseThingHandler baseThingHandler;
//
//
//
//    public ZWaveSerialHandler(Bridge thing, SerialPortManager serialPortManager) {
//        super(thing, serialPortManager);
//        this.thing = thing;
//        this.serialPortManager = (CommPortIdentifier) serialPortManager;
//    }
//
//
//    @Override
//    public void initialize() {
//
//        rxtxHandler.start(msg -> controller.incomingPacket(msg));
//
//        try {
//            this.serialPortManager = CommPortIdentifier.getPortIdentifier(rxtxHandler.config.portName);
//        } catch (NoSuchPortException e) {
//            throw new RuntimeException(e);
//        }
//        super.initialize();
//        this.portId = getPortId(rxtxHandler.serialPort.getName());
//        this.is = getIs(rxtxHandler.is);
//        this.os = getOs(rxtxHandler.os);
//        this.recieveThread = getRecieveThread(rxtxHandler.receiveThread);
//        this.serialPort = getSerialPort(rxtxHandler.serialPort);
////        this.controller = getController();
//
//    }
//
//
////    public void initializeNetwork() {
////
////    Map<String, String> config = new HashMap();
////        config.put("masterController",this.isMaster.toString());
////        config.put("sucNode",this.sucNode.toString());
////        config.put("secureInclusion",this.secureInclusionMode.toString());
////        config.put("networkKey",this.networkKey);
////        config.put("wakeupDefaultPeriod",this.wakeupDefaultPeriod.toString());
////        config.put("maxAwakePeriod",this.maxAwakePeriod.toString());
////        this.controller = new ZWaveController(this,config);
////}
//
//    public String getPortId(String portId) {
//        return portId;
//    }
//    public InputStream getIs(InputStream is) {
//        return is;
//    }
//    public OutputStream getOs(OutputStream os) {
//        return os;
//    }
//    public Thread getRecieveThread(Thread recieveThread){
//        return recieveThread;
//    }
//    public SerialPort getSerialPort(SerialPort serialPort){
//        return serialPort;
//    }
//    public CommPortIdentifier getCommPortIdentifier(CommPortIdentifier commPortIdentifier){
//        return commPortIdentifier;
//    }
//
//    public ZWaveController getController() {
//        return controller;
//    }
//
//    public void setRxtxHandler(RxtxZWaveIoHandler ioHandler){
//        this.rxtxHandler = ioHandler;
//    }
//
//    public void startBaseThingHandler(Bridge thing){
//        baseThingHandler = new BaseThingHandler(thing) {
//            @Override
//            public void initialize() {
//                this.initialize();
//            }
//
//            @Override
//            public void handleCommand(ChannelUID channelUID, Command command) {
//
//            }
//        };
//    }
//    @Override
//    @Nullable
//    public SerialPortIdentifier getIdentifier(String name) {
//        return null;
//    }
//
//    @Override
//    public Stream<SerialPortIdentifier> getIdentifiers() {
//        return null;
//    }
//
//
//}
