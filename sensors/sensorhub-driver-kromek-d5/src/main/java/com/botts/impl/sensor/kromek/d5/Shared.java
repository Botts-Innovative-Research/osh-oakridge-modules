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
import com.fazecast.jSerialComm.SerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.*;
import static com.botts.impl.sensor.kromek.d5.reports.SerialReport.bytesToUInt;

public class Shared {
    /**
     * Receive data from the given input stream.
     * Reads until a framing byte is received, then reads the length, then reads the rest of the message.
     *
     * @param in The input stream to read from.
     * @return The received data, excluding overhead.
     */
    static byte[] receiveData(InputStream in) throws IOException {
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
        int length = bytesToUInt(length1, length2);

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

    /**
     * Print the available serial ports to the console.
     */
    static void printCommPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        System.out.print("Available Ports:");
        for (SerialPort port : ports) {
            System.out.print(" " + port.getSystemPortName());
        }
        System.out.println();
    }

    /**
     * Send a request and receive a response.
     *
     * @param report       The report to send.
     * @param inputStream  The input stream to read from.
     * @param outputStream The output stream to write to.
     * @return The response report, or null if the response was an ACK.
     * @throws IOException               If there was an error reading or writing to the streams.
     * @throws NoSuchMethodException     If the report class does not have a constructor that takes a byte, byte, and byte[].
     * @throws InvocationTargetException If the constructor throws an exception.
     * @throws InstantiationException    If the report class is abstract.
     * @throws IllegalAccessException    If the constructor is not public.
     */
    static SerialReport sendRequest(SerialReport report, InputStream inputStream, OutputStream outputStream) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        byte[] message = report.encodeRequest();

        // Send the framed message to the server
        outputStream.write(message);

        // Receive data from the server
        byte[] receivedData = receiveData(inputStream);

        // Decode the received data using SLIP framing
        byte[] decodedData = decodeSLIP(receivedData);

        // The first five bytes are the header
        byte componentId = decodedData[3];
        byte reportId = decodedData[4];

        // These shouldn't happen, but just in case they do, ignore them.
        // Acknowledgements are typically a response to a command, and we only send requests.
        if (reportId == KROMEK_SERIAL_REPORTS_IN_ACK_ID || reportId == KROMEK_SERIAL_REPORTS_ACK_REPORT_ID_ERROR) {
            return null;
        }

        // The payload is everything in between
        byte[] payload = Arrays.copyOfRange(decodedData, 5, decodedData.length - 2);

        // Create a new report with the payload
        report = report.getClass()
                .getDeclaredConstructor(byte.class, byte.class, byte[].class)
                .newInstance(componentId, reportId, payload);
        return report;
    }

    /**
     * Encode the given message using SLIP framing.
     * If the FRAME byte occurs in the message, then it is replaced with the byte sequence ESC, ESC_FRAME.
     * If the ESC byte occurs in the message, then the byte sequence ESC, ESC_ESC is sent instead.
     * The message is then framed with the FRAME byte at the end.
     */
    public static byte[] encodeSLIP(byte[] data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        outputStream.write(KROMEK_SERIAL_FRAMING_FRAME_BYTE);
        for (byte b : data) {
            if (b == KROMEK_SERIAL_FRAMING_FRAME_BYTE) {
                outputStream.write(KROMEK_SERIAL_FRAMING_ESC_BYTE);
                outputStream.write(KROMEK_SERIAL_FRAMING_ESC_FRAME_BYTE);
            } else if (b == KROMEK_SERIAL_FRAMING_ESC_BYTE) {
                outputStream.write(KROMEK_SERIAL_FRAMING_ESC_BYTE);
                outputStream.write(KROMEK_SERIAL_FRAMING_ESC_ESC_BYTE);
            } else {
                outputStream.write(b);
            }
        }
        outputStream.write(KROMEK_SERIAL_FRAMING_FRAME_BYTE);

        return outputStream.toByteArray();
    }

    /**
     * Decode the given message using SLIP framing.
     * If it finds any ESC bytes, it replaces the escaped byte sequences with the original bytes.
     */
    public static byte[] decodeSLIP(byte[] input) {
        List<Byte> output = new ArrayList<>();
        for (int i = 0; i < input.length; ) {
            byte b = input[i];
            if (b == KROMEK_SERIAL_FRAMING_ESC_BYTE && i < input.length - 1) {
                byte nextByte = input[i + 1];
                if (nextByte == KROMEK_SERIAL_FRAMING_ESC_FRAME_BYTE) {
                    output.add(KROMEK_SERIAL_FRAMING_FRAME_BYTE);
                } else if (nextByte == KROMEK_SERIAL_FRAMING_ESC_ESC_BYTE) {
                    output.add(KROMEK_SERIAL_FRAMING_ESC_BYTE);
                } else {
                    throw new RuntimeException("Invalid SLIP escape sequence: " + nextByte);
                }
                i += 2;
            } else {
                output.add(b);
                i++;
            }
        }

        byte[] result = new byte[output.size()];
        for (int i = 0; i < output.size(); i++) {
            result[i] = output.get(i);
        }
        return result;
    }
}
