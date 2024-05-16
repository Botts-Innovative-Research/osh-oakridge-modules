//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.botts.sensorhub.impl.zwave.comms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
//import org.eclipse.jdt.annotation.NonNullByDefault;
//import org.eclipse.jdt.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.openhab.binding.zwave.handler.ZWaveControllerHandler;
import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortEvent;
import org.openhab.core.io.transport.serial.SerialPortEventListener;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@NonNullByDefault
public class ZWaveSerialHandler2 extends ZWaveControllerHandler {
    public static final int SERIAL_RECEIVE_TIMEOUT = 250;
    public static final long WATCHDOG_INIT_SECONDS = 5L;
    public static final long WATCHDOG_CHECK_SECONDS = 30L;
    public final Logger logger = LoggerFactory.getLogger(ZWaveSerialHandler2.class);
    public SerialPortManager serialPortManager;
    public String portId = "";
    public @Nullable SerialPort serialPort;
    public @Nullable InputStream inputStream;
    public @Nullable OutputStream outputStream;
    public int SOFCount = 0;
    public int CANCount = 0;
    public int NAKCount = 0;
    public int ACKCount = 0;
    public int OOFCount = 0;
    public int CSECount = 0;
    public @Nullable ZWaveReceiveThread receiveThread;
//    @NonNullByDefault({})
    public ScheduledFuture<?> watchdog;
    public boolean receiveTimeoutEnabled = false;

    public ZWaveSerialHandler2(Bridge thing, SerialPortManager serialPortManager) {
        super(thing);
        this.serialPortManager = serialPortManager;
    }

    public void initialize() {
        this.logger.debug("Initializing ZWave serial controller.");
        this.portId = (String)this.getConfig().get("port");
        super.initialize();
        this.watchdog = this.scheduler.schedule(this::watchSerialPort, 5L, TimeUnit.SECONDS);
    }

    public void watchSerialPort() {
        try {
            SerialPortIdentifier portIdentifier = this.serialPortManager.getIdentifier(this.portId);
            if (portIdentifier == null) {
                this.watchdog = this.scheduler.schedule(this::watchSerialPort, 30L, TimeUnit.SECONDS);
                return;
            }

            this.logger.debug("Connecting to serial port '{}'", this.portId);

            SerialPort commPort;
            try {
                commPort = portIdentifier.open("org.openhab.binding.zwave", 2000);
            } catch (IllegalStateException var7) {
                this.watchdog = this.scheduler.schedule(this::watchSerialPort, 30L, TimeUnit.SECONDS);
                return;
            }

            this.serialPort = commPort;
            commPort.setSerialPortParams(115200, 8, 1, 0);

            try {
                commPort.enableReceiveThreshold(1);
            } catch (UnsupportedCommOperationException var6) {
                this.logger.debug("Enabling receive threshold is unsupported");
            }

            try {
                commPort.enableReceiveTimeout(250);
                this.receiveTimeoutEnabled = true;
            } catch (UnsupportedCommOperationException var5) {
                this.logger.debug("Enabling receive timeout is unsupported");
            }

            this.inputStream = commPort.getInputStream();
            this.outputStream = commPort.getOutputStream();
            this.logger.debug("Starting receive thread");
            ZWaveReceiveThread zWaveReceiveThread = new ZWaveReceiveThread();
            this.receiveThread = zWaveReceiveThread;
            zWaveReceiveThread.start();
            commPort.addEventListener(zWaveReceiveThread);
            commPort.notifyOnDataAvailable(false);
            this.logger.debug("Serial port is initialized");
            this.initializeNetwork();
        } catch (PortInUseException var8) {
            this.onSerialPortError("@text/zwave.thingstate.serial_inuse");
        } catch (UnsupportedCommOperationException var9) {
            this.onSerialPortError("@text/zwave.thingstate.serial_unsupported");
        } catch (IOException var10) {
            this.onSerialPortError("@text/zwave.thingstate.controller_offline");
        } catch (TooManyListenersException var11) {
            this.onSerialPortError("@text/zwave.thingstate.serial_listeners");
        } catch (RuntimeException var12) {
            this.logger.debug("Unexpected runtime exception during serial port initialized ", var12);
            this.onSerialPortError("@text/zwave.thingstate.controller_offline");
        }

    }

    public void dispose() {
        this.disposeReceiveThread();
        this.disposeSerialConnection();
        if (this.watchdog != null && !this.watchdog.isCancelled()) {
            this.watchdog.cancel(true);
        }

        this.logger.debug("Stopped ZWave serial handler");
        super.dispose();
    }

    public void onSerialPortError(String errorMessage) {
        if (this.thing.getStatus().equals(ThingStatus.ONLINE)) {
            this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, errorMessage + " [\"" + this.portId + "\"]");
        }

        this.stopNetwork();
        this.disposeReceiveThread();
        this.disposeSerialConnection();
        this.receiveTimeoutEnabled = false;
        this.watchdog = this.scheduler.schedule(this::watchSerialPort, 5L, TimeUnit.SECONDS);
    }

    public void disposeSerialConnection() {
        this.logger.debug("Disposing serial connection");
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException var3) {
                this.logger.debug("Error while closing the input stream: {}", var3.getMessage());
            }

            this.inputStream = null;
        }

        if (this.outputStream != null) {
            try {
                this.outputStream.close();
            } catch (IOException var2) {
                this.logger.debug("Error while closing the output stream: {}", var2.getMessage());
            }

            this.outputStream = null;
        }

        if (this.serialPort != null) {
            this.serialPort.removeEventListener();
            this.serialPort.close();
            this.serialPort = null;
        }

        this.logger.debug("Serial connection disposed");
    }

    public void disposeReceiveThread() {
        this.logger.debug("Disposing receive thread");
        if (this.receiveThread != null) {
            this.receiveThread.interrupt();

            try {
                this.receiveThread.join();
            } catch (InterruptedException var2) {
            }

            this.receiveThread = null;
        }

        this.logger.debug("Receive thread dispose");
    }

    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    public void sendPacket(@Nullable SerialMessage serialMessage) {
        byte[] buffer = serialMessage.getMessageBuffer();
        if (this.serialPort == null) {
            this.logger.debug("NODE {}: Port closed sending REQUEST Message = {}", serialMessage.getMessageNode(), SerialMessage.bb2hex(buffer));
        } else {
            this.logger.debug("NODE {}: Sending REQUEST Message = {}", serialMessage.getMessageNode(), SerialMessage.bb2hex(buffer));

            try {
                synchronized(this.outputStream) {
                    this.outputStream.write(buffer);
                    this.outputStream.flush();
                    this.logger.debug("Message SENT");
                }
            } catch (IOException var5) {
                this.logger.warn("Got I/O exception {} during sending. exiting thread.", var5.getLocalizedMessage());
                this.onSerialPortError("@text/zwave.thingstate.controller_offline");
            }

        }
    }

   public class ZWaveReceiveThread extends Thread implements SerialPortEventListener {
        public static final int SOF = 1;
        public static final int ACK = 6;
        public static final int NAK = 21;
        public static final int CAN = 24;
        public final Logger logger = LoggerFactory.getLogger(ZWaveReceiveThread.class);
        public static final int SEARCH_SOF = 0;
        public static final int SEARCH_LEN = 1;
        public static final int SEARCH_DAT = 2;
        public static final int HARDWARE_ERROR = 11;
        public int rxState = 0;
        public int messageLength;
        public int rxLength;
        public byte @Nullable [] rxBuffer;

        ZWaveReceiveThread() {
            super("ZWaveReceiveInputThread");
        }

        public void serialEvent(SerialPortEvent arg0) {
            if (arg0.getEventType() == 11) {
                ZWaveSerialHandler2.this.onSerialPortError("@text/zwave.thingstate.serial_notfound");
            }

        }

        public void sendResponse(int response) {
            try {
                if (ZWaveSerialHandler2.this.serialPort == null) {
                    return;
                }

                synchronized(ZWaveSerialHandler2.this.outputStream) {
                    ZWaveSerialHandler2.this.outputStream.write(response);
                    ZWaveSerialHandler2.this.outputStream.flush();
                    this.logger.trace("Response SENT {}", response);
                }
            } catch (IOException var4) {
                this.logger.warn("Exception during send", var4);
            }

        }

        public void run() {
            this.logger.debug("Starting ZWave thread: Receive");

            try {
                ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_sof"), new DecimalType(ZWaveSerialHandler2.this.SOFCount));
                ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_ack"), new DecimalType(ZWaveSerialHandler2.this.ACKCount));
                ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_nak"), new DecimalType(ZWaveSerialHandler2.this.NAKCount));
                ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_can"), new DecimalType(ZWaveSerialHandler2.this.CANCount));
                ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_oof"), new DecimalType(ZWaveSerialHandler2.this.OOFCount));
                ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_cse"), new DecimalType(ZWaveSerialHandler2.this.CSECount));
                this.sendResponse(21);

                while(true) {
                    while(true) {
                        int nextByte;
                        while(true) {
                            if (interrupted()) {
                                return;
                            }

                            try {
                                if (ZWaveSerialHandler2.this.serialPort == null) {
                                    return;
                                }

                                nextByte = ZWaveSerialHandler2.this.inputStream.read();
                                if (nextByte != -1) {
                                    break;
                                }

                                if (!ZWaveSerialHandler2.this.receiveTimeoutEnabled) {
                                    return;
                                }

                                if (this.rxState != 0) {
                                    this.logger.debug("Receive Timeout - Sending NAK");
                                    this.rxState = 0;
                                }
                            } catch (IOException var9) {
                                this.logger.warn("Got I/O exception {} during receiving. exiting thread.", var9.getLocalizedMessage());
                                return;
                            }
                        }

                        SerialMessage recvMessage;
                        switch (this.rxState) {
                            case 0:
                                switch (nextByte) {
                                    case 1:
                                        this.logger.trace("Received SOF");
                                        ++ZWaveSerialHandler2.this.SOFCount;
                                        ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_sof"), new DecimalType(ZWaveSerialHandler2.this.SOFCount));
                                        this.rxState = 1;
                                        continue;
                                    case 6:
                                        ++ZWaveSerialHandler2.this.ACKCount;
                                        ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_ack"), new DecimalType(ZWaveSerialHandler2.this.ACKCount));
                                        this.logger.debug("Receive Message = 06");
                                        recvMessage = new SerialMessage(new byte[]{6});
                                        ZWaveSerialHandler2.this.incomingMessage(recvMessage);
                                        continue;
                                    case 21:
                                        ++ZWaveSerialHandler2.this.NAKCount;
                                        ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_nak"), new DecimalType(ZWaveSerialHandler2.this.NAKCount));
                                        this.logger.debug("Receive Message = 15");
                                        SerialMessage nakMessage = new SerialMessage(new byte[]{21});
                                        ZWaveSerialHandler2.this.incomingMessage(nakMessage);
                                        continue;
                                    case 24:
                                        ++ZWaveSerialHandler2.this.CANCount;
                                        ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_can"), new DecimalType(ZWaveSerialHandler2.this.CANCount));
                                        this.logger.debug("Receive Message = 18");
                                        SerialMessage canMessage = new SerialMessage(new byte[]{24});
                                        ZWaveSerialHandler2.this.incomingMessage(canMessage);
                                        continue;
                                    default:
                                        ++ZWaveSerialHandler2.this.OOFCount;
                                        ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_oof"), new DecimalType(ZWaveSerialHandler2.this.OOFCount));
                                        this.logger.debug(String.format("Protocol error (OOF). Got 0x%02X.", nextByte));
                                        continue;
                                }
                            case 1:
                                if (nextByte >= 4 && nextByte <= 64) {
                                    this.messageLength = (nextByte & 255) + 2;
                                    this.rxBuffer = new byte[this.messageLength];
                                    this.rxBuffer[0] = 1;
                                    this.rxBuffer[1] = (byte)nextByte;
                                    this.rxLength = 2;
                                    this.rxState = 2;
                                    break;
                                }

                                this.logger.debug("Frame length is out of limits ({})", nextByte);
                                break;
                            case 2:
                                this.rxBuffer[this.rxLength] = (byte)nextByte;
                                ++this.rxLength;
                                if (this.rxLength >= this.messageLength) {
                                    this.logger.debug("Receive Message = {}", SerialMessage.bb2hex(this.rxBuffer));
                                    recvMessage = new SerialMessage(this.rxBuffer);
                                    if (recvMessage.isValid) {
                                        this.logger.trace("Message is valid, sending ACK");
                                        this.sendResponse(6);
                                        ZWaveSerialHandler2.this.incomingMessage(recvMessage);
                                    } else {
                                        ++ZWaveSerialHandler2.this.CSECount;
                                        ZWaveSerialHandler2.this.updateState(new ChannelUID(ZWaveSerialHandler2.this.getThing().getUID(), "serial_cse"), new DecimalType(ZWaveSerialHandler2.this.CSECount));
                                        this.logger.debug("Message is invalid, discarding");
                                        this.sendResponse(21);
                                    }

                                    this.rxState = 0;
                                }
                        }
                    }
                }
            } catch (RuntimeException var10) {
                this.logger.warn("Exception during ZWave thread. ", var10);
            } finally {
                this.logger.debug("Stopped ZWave thread: Receive");
                if (ZWaveSerialHandler2.this.thing.getStatus().equals(ThingStatus.ONLINE)) {
                    ZWaveSerialHandler2.this.onSerialPortError("@text/zwave.thingstate.controller_offline");
                }

            }

        }
    }
}
