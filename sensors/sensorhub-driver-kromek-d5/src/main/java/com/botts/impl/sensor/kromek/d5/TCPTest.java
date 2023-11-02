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

import com.botts.impl.sensor.kromek.d5.reports.KromekSerialRadiometricStatusReport;
import org.junit.Test;

import java.net.Socket;

import static com.botts.impl.sensor.kromek.d5.Shared.sendAndReceiveReport;

public class TCPTest {
    @Test
    public void testTCP() {
        // Define the IP address and port number of the server
        String ipAddress = "192.168.1.138";
        int portNumber = 12345;
        System.out.println("Connecting to " + ipAddress + " on port " + portNumber);

        // Create a TCP socket and connect to the server
        try (Socket clientSocket = new Socket(ipAddress, portNumber)) {
            System.out.println("Connected to server");

            var report = new KromekSerialRadiometricStatusReport();
            var inputStream = clientSocket.getInputStream();
            var outputStream = clientSocket.getOutputStream();

            sendAndReceiveReport(report, inputStream, outputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}