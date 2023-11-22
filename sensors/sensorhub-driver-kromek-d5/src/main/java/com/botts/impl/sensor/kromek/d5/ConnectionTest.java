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

import com.botts.impl.sensor.kromek.d5.reports.KromekSerialCompressionEnabledReport;
import com.botts.impl.sensor.kromek.d5.reports.KromekSerialRadiometricStatusReport;
import com.botts.impl.sensor.kromek.d5.reports.SerialReport;
import com.fazecast.jSerialComm.SerialPort;
import org.junit.Test;

import java.net.Socket;

import static com.botts.impl.sensor.kromek.d5.Shared.findSerialPort;
import static com.botts.impl.sensor.kromek.d5.Shared.sendRequest;

public class ConnectionTest {
    @Test
    public void testTCP() {
        // Define the IP address and port number of the server, as configured in the D5 Settings application
        String ipAddress = "192.168.1.138";
        int portNumber = 12345;
        System.out.println("Connecting to " + ipAddress + " on port " + portNumber);

        // Create a TCP socket and connect to the server
        try (Socket clientSocket = new Socket(ipAddress, portNumber)) {
            System.out.println("Connected to server");

            var report = new KromekSerialRadiometricStatusReport();
            var inputStream = clientSocket.getInputStream();
            var outputStream = clientSocket.getOutputStream();

            var receivedReport = sendRequest(report, inputStream, outputStream);
            System.out.println(receivedReport);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSerialPort() {
        String comPortName = findSerialPort();
        if (comPortName == null) {
            System.out.println("No serial port found.");
            return;
        }

        try {
            SerialPort commPort = SerialPort.getCommPort(comPortName);
            commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

            System.out.println("Opening port " + commPort.getSystemPortName());
            if (commPort.openPort()) {
                System.out.println("Port is open.");
            } else {
                System.out.println("Failed to open port.");
                return;
            }

            SerialReport report = new KromekSerialCompressionEnabledReport();
            var inputStream = commPort.getInputStream();
            var outputStream = commPort.getOutputStream();

            var receivedReport = sendRequest(report, inputStream, outputStream);
            System.out.println(receivedReport);
            commPort.closePort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}