package com.botts.impl.sensor.kromek.d5;

import com.botts.impl.sensor.kromek.d5.reports.SerialReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.botts.impl.sensor.kromek.d5.message.Constants.*;
import static com.botts.impl.sensor.kromek.d5.reports.SerialReport.bytesToUInt;
import static com.botts.impl.sensor.kromek.d5.reports.SerialReport.decodeSLIP;

public class D5MessageRouter implements Runnable {
    Thread worker;
    D5Sensor sensor;
    D5Config config;
    InputStream inputStream;
    OutputStream outputStream;
    private static final Logger logger = LoggerFactory.getLogger(D5Sensor.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public D5MessageRouter(D5Sensor sensor, InputStream inputStream, OutputStream outputStream) {
        this.sensor = sensor;
        this.inputStream = inputStream;
        this.outputStream = outputStream;

        config = sensor.getConfiguration();
        worker = new Thread(this, "Message Router");
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized void run() {
        logger.info("Starting Message Router");
        while (!sensor.processLock) {
            // For each active output, send a request and receive a response
            for (Map.Entry<Class<?>, D5Output> entry : sensor.outputs.entrySet()) {
                Class<?> reportClass = entry.getKey();
                D5Output output = entry.getValue();

                try {
                    // Create a message to send
                    var report = (SerialReport) reportClass.getDeclaredConstructor().newInstance();

                    logger.info("Sending " + report.getClass().getSimpleName());
                    byte[] message = report.encodeRequest();

                    // Send the framed message to the server
                    outputStream.write(message);

                    // Receive data from the server
                    byte[] receivedData = receiveData(inputStream);
                    logger.info("Received data: " + Arrays.toString(receivedData));

                    // Decode the received data using SLIP framing
                    byte[] decodedData = decodeSLIP(receivedData);

                    // First five bytes are the header
                    int size = bytesToUInt(decodedData[0], decodedData[1]); //Size is the first two bytes
                    int mode = decodedData[2];
                    byte componentId = decodedData[3];
                    byte reportId = decodedData[4];

                    //Last two bytes are the CRC
                    int crc = bytesToUInt(decodedData[decodedData.length - 2], decodedData[decodedData.length - 1]);

                    if (reportId == KROMEK_SERIAL_REPORTS_IN_ACK_ID) {
                        logger.info("ACK");
                        continue;
                    }
                    if (reportId == KROMEK_SERIAL_REPORTS_ACK_REPORT_ID_ERROR) {
                        logger.info("ACK Error");
                        continue;
                    }

                    //The payload is everything in between
                    byte[] payload = Arrays.copyOfRange(decodedData, 5, decodedData.length - 2);

                    //Create a new report with the payload
                    report = (SerialReport) reportClass
                            .getDeclaredConstructor(byte.class, byte.class, byte[].class)
                            .newInstance(componentId, reportId, payload);

                    output.setData(report);
                } catch (Exception e) {
                    logger.error("Error", e);
                }
            }

            // Wait 1 second before sending another message
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
            }
        }
    }

    /**
     * Receive data from the given input stream.
     * Reads until a framing byte is received.
     */
    private static byte[] receiveData(InputStream in) throws IOException {
        logger.info("Receiving data...");
        List<Byte> output = new ArrayList<>();
        int b;

        // Read until we get a framing byte
        do {
            b = in.read();
        } while ((byte) b != KROMEK_SERIAL_FRAMING_FRAME_BYTE);

        // Read until we get a non-framing byte. Extra framing bytes are harmless and can be ignored.
        do {
            b = in.read();
        } while ((byte) b == KROMEK_SERIAL_FRAMING_FRAME_BYTE);

        // First two bytes after framing byte are the message length
        byte length1 = (byte) b;
        byte length2 = (byte) in.read();
        logger.info(String.format("Length: 0x%02X 0x%02X", length1, length2));
        int length = bytesToUInt(length1, length2);
        logger.info("Length: " + length);

        output.add(length1);
        output.add(length2);

        // Read the rest of the message, excluding the two bytes we already read
        for (int i = 0; i < length - 2; i++) {
            b = in.read();
            output.add((byte) b);
        }

        byte[] result = new byte[output.size()];
        for (int i = 0; i < output.size(); i++) {
            result[i] = output.get(i);
        }
        return result;
    }
}
