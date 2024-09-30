package com.botts.sensorhub.impl.zwave.comms;

import gnu.io.*;
import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveIoHandler;
import org.sensorhub.impl.comm.UARTConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;



public class RxtxZWaveIoHandler implements ZWaveIoHandler
{
    static final Logger log = LoggerFactory.getLogger(RxtxZWaveIoHandler.class);

    final UARTConfig config;
    public SerialPort serialPort;
    public InputStream is;
    public OutputStream os;
    public Thread receiveThread;
    Consumer<SerialMessage> onReceive;


    public RxtxZWaveIoHandler(UARTConfig config)
    {
        this.config = config;


//        Map<String, String> controllerConfig = new HashMap();
//        controllerConfig.put("masterController", "true");
//        controllerConfig.put("sucNode", "1");
//
//        this.controller = new ZWaveController(this, controllerConfig);

    }

    // Edit start
    public void start(Consumer<SerialMessage> onReceive)
    {
        this.onReceive = onReceive;

        try
        {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(config.portName);

            if (portIdentifier.isCurrentlyOwned())
            {
                throw new PortInUseException();
            }
            else
            {
                CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

                if (commPort instanceof SerialPort)
                {
                    serialPort = (SerialPort) commPort;

                    // get parity code
                    int parity;
                    if (config.parity == null)
                    {
                        parity = SerialPort.PARITY_NONE;
                    }
                    else
                    {
                        switch (config.parity)
                        {
                            case PARITY_EVEN:
                                parity = SerialPort.PARITY_EVEN;
                                break;

                            case PARITY_ODD:
                                parity = SerialPort.PARITY_ODD;
                                break;

                            case PARITY_MARK:
                                parity = SerialPort.PARITY_MARK;
                                break;

                            case PARITY_SPACE:
                                parity = SerialPort.PARITY_SPACE;
                                break;

                            default:
                                parity = SerialPort.PARITY_NONE;
                        }
                    }


                    // configure serial port
                    serialPort.setSerialPortParams(
                            config.baudRate,
                            config.dataBits,
                            config.stopBits,
                            parity);

                    // set read thresholds
                    if (config.receiveTimeout >= 0)
                        serialPort.enableReceiveTimeout(config.receiveTimeout);
                    else
                        serialPort.disableReceiveTimeout();

                    if (config.receiveThreshold >= 0)
                        serialPort.enableReceiveThreshold(config.receiveThreshold);
                    else
                        serialPort.disableReceiveThreshold();

                    // obtain input/output streams
                    is = serialPort.getInputStream();
                    os = serialPort.getOutputStream();

                    startReceiveThread();

                    log.info("Connected to serial port {}", config.portName);
                }
                else
                {
                    log.error("Port {} is not a serial port", config.portName);
                }
            }
        }
        catch (NoSuchPortException e)
        {
            throw new IllegalStateException("Invalid serial port " + config.portName);
        }
        catch (PortInUseException e)
        {
            throw new IllegalStateException("Port " + config.portName + " is currently in use");
        }
        catch (UnsupportedCommOperationException e)
        {
            throw new IllegalStateException("Invalid serial port configuration for " + config.portName);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot connect to serial port " + config.portName);
        }
        catch (UnsatisfiedLinkError e)
        {
            throw new IllegalStateException("Cannot load RX/TX native library", e);
        }
    }


    protected void startReceiveThread()
    {
        boolean receiveTimeoutEnabled = true;

        receiveThread = new Thread() {
            private static final int SOF = 0x01;
            private static final int ACK = 0x06;
            private static final int NAK = 0x21; //15?
            private static final int CAN = 0x24; //18?
            private static final int SEARCH_SOF = 0;
            private static final int SEARCH_LEN = 1;
            private static final int SEARCH_DAT = 2;
            private static final int HARDWARE_ERROR = 11;

            private int rxState = SEARCH_SOF;
            private int messageLength;
            private int rxLength;
            private byte[] rxBuffer;

            private int SOFCount = 0;
            private int CANCount = 0;
            private int NAKCount = 0;
            private int ACKCount = 0;
            private int OOFCount = 0;
            private int CSECount = 0;

            private void sendResponse(int response) {
                try {
                    if (serialPort == null) {
                        return;
                    }
                    synchronized (os) {
                        os.write(response);
                        os.flush();
                        log.trace("Response SENT {}", response);
                    }
                } catch (IOException e) {
                    log.warn("Exception during send", e);
                }
            }

            private void incomingMessage(SerialMessage msg) {
                onReceive.accept(msg);
            }

            public void run() {
                log.debug("Starting ZWave thread: Receive");
                try {
                    sendResponse(NAK);

                    while (!interrupted()) {
                        int nextByte;

                        try {
                            if (serialPort == null) {
                                break;
                            }

                            nextByte = is.read();
                            // logger.debug("SERIAL:: STATE {}, nextByte {}, count {} ", rxState, nextByte, rxLength);

                            // If receiveTimeout is enabled, a -1 byte value means a timeout
                            // Otherwise, there is an error in the serial connection
                            if (nextByte == -1) {
                                if (!receiveTimeoutEnabled) {
                                    break;
                                }
                                if (rxState != SEARCH_SOF) {
                                    // If we're not searching for a new frame when we get a timeout, something bad happened
                                    log.debug("Receive Timeout - Sending NAK");
                                    rxState = SEARCH_SOF;
                                }
                                continue;
                            }
                        } catch (IOException e) {
                            log.warn("Got I/O exception {} during receiving. exiting thread.", e.getLocalizedMessage());
                            break;
                        }

                        switch (rxState) {
                            case SEARCH_SOF:
                                switch (nextByte) {
                                    case SOF:
                                        log.trace("Received SOF");

                                        // Keep track of statistics
                                        SOFCount++;
                                        rxState = SEARCH_LEN;
                                        break;

                                    case ACK:
                                        // Keep track of statistics
                                        ACKCount++;
                                        log.debug("Receive Message = 06");
                                        SerialMessage ackMessage = new SerialMessage(new byte[] { ACK });
                                        incomingMessage(ackMessage);
                                        break;

                                    case NAK:
                                        // A NAK means the CRC was incorrectly received by the controller
                                        NAKCount++;
                                        log.debug("Receive Message = 15");
                                        SerialMessage nakMessage = new SerialMessage(new byte[] { NAK });
                                        incomingMessage(nakMessage);
                                        break;

                                    case CAN:
                                        // The CAN means that the controller dropped the frame
                                        CANCount++;
                                        // logger.debug("Protocol error (CAN)");
                                        log.debug("Receive Message = 18");
                                        SerialMessage canMessage = new SerialMessage(new byte[] { CAN });
                                        incomingMessage(canMessage);
                                        break;

                                    default:
                                        OOFCount++;
                                        log.debug(String.format("Protocol error (OOF). Got 0x%02X.", nextByte));
                                        // Let the timeout deal with sending the NAK
                                        break;
                                }
                                break;

                            case SEARCH_LEN:
                                // Sanity check the frame length
                                if (nextByte < 4 || nextByte > 64) {
                                    log.debug("Frame length is out of limits ({})", nextByte);

                                    break;
                                }
                                messageLength = (nextByte & 0xff) + 2;

                                rxBuffer = new byte[messageLength];
                                rxBuffer[0] = SOF;
                                rxBuffer[1] = (byte) nextByte;
                                rxLength = 2;
                                rxState = SEARCH_DAT;
                                break;

                            case SEARCH_DAT:
                                rxBuffer[rxLength] = (byte) nextByte;
                                rxLength++;

                                if (rxLength < messageLength) {
                                    break;
                                }

                                log.debug("Receive Message = {}", SerialMessage.bb2hex(rxBuffer));
                                SerialMessage recvMessage = new SerialMessage(rxBuffer);
                                if (recvMessage.isValid) {
                                    log.trace("Message is valid, sending ACK");
                                    sendResponse(ACK);

                                    incomingMessage(recvMessage);
                                } else {
                                    CSECount++;
                                    log.debug("Message is invalid, discarding");
                                    sendResponse(NAK);
                                }

                                rxState = SEARCH_SOF;
                                break;
                        }

                    }
                } catch (RuntimeException e) {
                    log.warn("Exception during ZWave thread. ", e);
                } finally {
                    log.debug("Stopped ZWave thread: Receive");
                    /*if (thing.getStatus().equals(ThingStatus.ONLINE)) {
                        onSerialPortError(ZWaveBindingConstants.OFFLINE_CTLR_OFFLINE);
                    }*/
                }
            }
        };

        receiveThread.start();
    }


    public void stop()
    {
        if (receiveThread != null)
            receiveThread.interrupt();

        if (serialPort != null)
        {
            serialPort.close();
            serialPort = null;
        }

        is = null;
        os = null;
    }


    @Override
    public void sendPacket(SerialMessage msg)
    {
        byte[] buffer = msg.getMessageBuffer();

        try {
            synchronized (os) {
                os.write(buffer);
                os.flush();
                log.debug("Message SENT");
            }
        } catch (IOException e) {
            log.warn("Got I/O exception {} during sending. exiting thread.", e.getLocalizedMessage());
            //onSerialPortError(ZWaveBindingConstants.OFFLINE_CTLR_OFFLINE);
        }
    }
}
