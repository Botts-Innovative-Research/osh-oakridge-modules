/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (c) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */

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

import static com.botts.impl.sensor.kromek.d5.reports.Constants.*;
import static com.botts.impl.sensor.kromek.d5.reports.SerialReport.bytesToUInt;
import static com.botts.impl.sensor.kromek.d5.reports.SerialReport.decodeSLIP;

/**
 * This class is responsible for sending and receiving messages to and from the sensor.
 * Requests are sent to the sensor and responses are received every second unless the polling rate is changed for a
 * particular report.
 *
 * @author Michael Elmore
 * @since Oct. 2023
 */
public class D5MessageRouter implements Runnable {
    Thread worker;
    D5Sensor sensor;
    D5Config config;
    InputStream inputStream;
    OutputStream outputStream;
    private static final Logger logger = LoggerFactory.getLogger(D5Sensor.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private int count = 0;

    public D5MessageRouter(D5Sensor sensor, InputStream inputStream, OutputStream outputStream) {
        this.sensor = sensor;
        this.inputStream = inputStream;
        this.outputStream = outputStream;

        config = sensor.getConfiguration();
        worker = new Thread(this, "Message Router");
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    public synchronized void run() {
        if (sensor.processLock) return;

        // For each active output, send a request and receive a response
        for (Map.Entry<Class<?>, D5Output> entry : sensor.outputs.entrySet()) {
            Class<?> reportClass = entry.getKey();
            D5Output output = entry.getValue();

            try {
                // Create a message to send
                var report = (SerialReport) reportClass.getDeclaredConstructor().newInstance();

                // All reports are sent on the first iteration (when count == 0)
                if (count != 0 && report.getPollingRate() == 0) {
                    // If the polling rate is 0, the report is not sent.
                    // This is used for reports that are only sent once.
                    continue;
                } else if (count != 0 && count % report.getPollingRate() != 0) {
                    // If the polling rate is not 0, the report is sent every N iterations
                    continue;
                }

                logger.info("Sending " + report.getClass().getSimpleName());
                byte[] message = report.encodeRequest();

                // Send the framed message to the server
                outputStream.write(message);

                // Receive data from the server
                byte[] receivedData = receiveData(inputStream);
                logger.info("Received data: " + Arrays.toString(receivedData));

                // Decode the received data using SLIP framing
                byte[] decodedData = decodeSLIP(receivedData);

                // The first five bytes are the header
                byte componentId = decodedData[3];
                byte reportId = decodedData[4];

                if (reportId == KROMEK_SERIAL_REPORTS_IN_ACK_ID) {
                    logger.info("ACK");
                    continue;
                }
                if (reportId == KROMEK_SERIAL_REPORTS_ACK_REPORT_ID_ERROR) {
                    logger.info("ACK Error");
                    continue;
                }

                // The payload is everything in between
                byte[] payload = Arrays.copyOfRange(decodedData, 5, decodedData.length - 2);

                // Create a new report with the payload
                report = (SerialReport) reportClass
                        .getDeclaredConstructor(byte.class, byte.class, byte[].class)
                        .newInstance(componentId, reportId, payload);

                logger.info("Received " + report.getClass().getSimpleName() + ": " + report);

                output.setData(report);
            } catch (Exception e) {
                logger.error("Error", e);
            }
        }
        count++;
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

        // The first two bytes after framing byte are the message length
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
