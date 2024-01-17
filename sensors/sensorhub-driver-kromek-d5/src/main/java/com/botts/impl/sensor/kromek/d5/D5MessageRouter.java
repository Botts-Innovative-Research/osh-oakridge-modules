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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.botts.impl.sensor.kromek.d5.Shared.sendRequest;

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

                report = sendRequest(report, inputStream, outputStream);

                output.setData(report);
            } catch (Exception e) {
                logger.error("Error", e);
            }
        }
        count++;
    }
}
