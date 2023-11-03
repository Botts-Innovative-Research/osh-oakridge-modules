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

import com.botts.impl.sensor.kromek.d5.reports.*;
import com.fazecast.jSerialComm.SerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.*;
import static com.botts.impl.sensor.kromek.d5.reports.SerialReport.bytesToUInt;

public class Shared {
    static SerialReport createReport(byte componentId, byte reportId, byte[] payload) {
        switch (reportId) {
            case KROMEK_SERIAL_REPORTS_IN_DEVICE_INFO_ID:
            case KROMEK_SERIAL_REPORTS_IN_UI_SCREEN_CONFIG_ID:
            case KROMEK_SERIAL_REPORTS_OUT_UI_SCREEN_CONFIG_ID:
            case KROMEK_SERIAL_REPORTS_IN_ENERGY_SPECTRUM_ID:
            case KROMEK_SERIAL_REPORTS_IN_MAINTAINER_DEVICE_CONFIG_INDEXED_ID:
            case KROMEK_SERIAL_REPORTS_OUT_MAINTAINER_DEVICE_CONFIG_INDEXED_ID:
            case KROMEK_SERIAL_REPORTS_IN_WIFI_AP_CONFIG_ID:
            case KROMEK_SERIAL_REPORTS_OUT_WIFI_AP_CONFIG_ID:
            case KROMEK_SERIAL_REPORTS_IN_UI_BT_ENABLE_ID:
            case KROMEK_SERIAL_REPORTS_OUT_UI_BT_ENABLE_ID:
            case KROMEK_SERIAL_REPORTS_IN_UI_ALERT_CONFIG_ID:
            case KROMEK_SERIAL_REPORTS_OUT_UI_ALERT_CONFIG_ID:
            case KROMEK_SERIAL_REPORTS_IN_UI_CONFIRMATION_MODE_ID:
            case KROMEK_SERIAL_REPORTS_OUT_UI_CONFIRMATION_MODE_ID:
            case KROMEK_SERIAL_REPORTS_IN_UI_SEARCH_ID_ENABLE_ID:
            case KROMEK_SERIAL_REPORTS_OUT_UI_SEARCH_ID_ENABLE_ID:
            case KROMEK_SERIAL_REPORTS_IN_UI_ISOTOPE_ENABLE_ID:
            case KROMEK_SERIAL_REPORTS_OUT_UI_ISOTOPE_ENABLE_ID:
            case KROMEK_SERIAL_REPORTS_IN_UI_PIN_CODE_ID:
            case KROMEK_SERIAL_REPORTS_OUT_UI_PIN_CODE_ID:
            case KROMEK_SERIAL_REPORTS_IN_RESET_ACCUMULATED_DOSE_ID:
            case KROMEK_SERIAL_REPORTS_OUT_RESET_ACCUMULATED_DOSE_ID:
                throw new RuntimeException("Not implemented");
            case KROMEK_SERIAL_REPORTS_IN_RADIOMETRIC_STATUS_REPORT:
                return new KromekSerialRadiometricStatusReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_OTG_MODE_ID:
            case KROMEK_SERIAL_REPORTS_OUT_OTG_MODE_ID:
                return new KromekSerialOTGReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_ABOUT_ID:
                return new KromekSerialAboutReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_RADIATION_THRESHOLD_INDEXED_ID:
            case KROMEK_SERIAL_REPORTS_OUT_RADIATION_THRESHOLD_INDEXED_ID:
                return new KromekSerialUIRadiationThresholdsReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_REMOTE_EXT_ISOTOPE_CONFIRMATION_STATUS_ID:
                return new KromekSerialRemoteExtendedIsotopeConfirmationStatusReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_REMOTE_BACKGROUND_COLLECTION_STATUS_ID:
                return new KromekSerialRemoteBackgroundStatusReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_RADIOMETRICS_V1_ID:
                return new KromekDetectorRadiometricsV1Report(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_UTC_TIME_ID:
            case KROMEK_SERIAL_REPORTS_OUT_UTC_TIME_ID:
                return new KromekSerialUTCReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_REMOTE_ISOTOPE_CONFIRMATION_STATUS_ID:
                return new KromekSerialRemoteIsotopeConfirmationStatusReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_STATUS_ID:
                return new KromekSerialStatusReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_ETHERNET_CONFIG_ID:
            case KROMEK_SERIAL_REPORTS_OUT_ETHERNET_CONFIG_ID:
                return new KromekSerialEthernetConfigReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_COMPRESSION_ENABLED_ID:
            case KROMEK_SERIAL_REPORTS_OUT_COMPRESSION_ENABLED_ID:
                return new KromekSerialCompressionEnabledReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_DOSE_INFO_ID:
                return new KromekSerialDoseInfoReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_REMOTE_ISOTOPE_CONFIRMATION_ID:
            case KROMEK_SERIAL_REPORTS_OUT_REMOTE_ISOTOPE_CONFIRMATION_ID:
                return new KromekSerialRemoteIsotopeConfirmationReport(componentId, reportId, payload);
            case KROMEK_SERIAL_REPORTS_IN_UNIT_ID_ID:
            case KROMEK_SERIAL_REPORTS_OUT_UNIT_ID_ID:
                return new KromekSerialUnitIDReport(componentId, reportId, payload);
            default:
                throw new RuntimeException("Unknown reportId: " + reportId);
        }
    }

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

    static void printCommPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        System.out.print("Available Ports:");
        for (SerialPort port : ports) {
            System.out.print(" " + port.getSystemPortName());
        }
        System.out.println();
    }

    static void sendAndReceiveReport(SerialReport report, InputStream inputStream, OutputStream outputStream) throws IOException {
        // Create a message to send
        byte[] message = report.encodeRequest();
        System.out.println("Message: " + Arrays.toString(message));
        System.out.print("Hex data: ");
        for (byte b : message)
            System.out.printf("%02X ", b);
        System.out.println();

        // Send the framed message to the server
        outputStream.write(message);

        System.out.printf("Message sent. Requested report: 0x%02X", report.getReportId());
        System.out.println();

        // Receive data from the server
        byte[] receivedData = receiveData(inputStream);
        System.out.println("Received data: " + Arrays.toString(receivedData));

        // Decode the received data using SLIP framing
        byte[] decodedData = decodeSLIP(receivedData);

        // Print the received data
        System.out.println(" Decoded data: " + Arrays.toString(decodedData));
        System.out.println("receivedData: " + receivedData.length + " decodedData: " + decodedData.length);

        // Print as hex
        System.out.print("     Hex data: ");
        for (byte b : decodedData) {
            System.out.printf("%02X ", b);
        }
        System.out.println();

        // The first two bytes is the size
        int size = bytesToUInt(decodedData[0], decodedData[1]);
        System.out.println("Size: " + size);

        // The next byte is the mode
        int mode = decodedData[2];
        System.out.println("Mode: " + mode);

        // The next byte is the componentId then reportId
        byte componentId = decodedData[3];
        byte reportId = decodedData[4];
        System.out.printf("ComponentId: " + SerialReport.bytesToUInt(componentId) + " (0x%02X) ReportId: " + SerialReport.bytesToUInt(reportId) + " (0x%02X)", componentId, reportId);
        System.out.println();

        if (reportId == KROMEK_SERIAL_REPORTS_IN_ACK_ID) {
            System.out.println("ACK");
            return;
        }
        if (reportId == KROMEK_SERIAL_REPORTS_ACK_REPORT_ID_ERROR) {
            System.out.println("ACK Error");
            return;
        }

        // The last two bytes are the CRC
        int crc = bytesToUInt(decodedData[decodedData.length - 2], decodedData[decodedData.length - 1]);
        System.out.println("CRC: " + crc);

        // The payload is everything in between
        byte[] payload = Arrays.copyOfRange(decodedData, 5, decodedData.length - 2);

        report = createReport(componentId, reportId, payload);
        System.out.println("Report: " + report);
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
