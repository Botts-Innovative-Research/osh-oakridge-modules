package com.botts.impl.sensor.kromek.d5;

import com.botts.impl.sensor.kromek.d5.reports.*;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.*;
import static com.botts.impl.sensor.kromek.d5.reports.SerialReport.bytesToUInt;
import static com.botts.impl.sensor.kromek.d5.reports.SerialReport.decodeSLIP;

public class TCPTest {
    @Test
    public void testTCP() {
        // Define the IP address and port number of the server
        String ipAddress = "192.168.1.138";
        int portNumber = 12345;
        System.out.println("Connecting to " + ipAddress + " on port " + portNumber);

        // Create a TCP socket and connect to the server
        try (Socket clientSocket = new Socket(ipAddress, portNumber)) {
            SerialReport report = new KromekSerialRadiometricStatusReport();

            System.out.println("Connected to server");

            // Create input and output streams for the socket
            InputStream inFromServer = clientSocket.getInputStream();
            OutputStream outToServer = clientSocket.getOutputStream();

            // Create a message to send
            byte[] message = report.encodeRequest();
            System.out.println("Message: " + Arrays.toString(message));
            System.out.print("Hex data: ");
            for (byte b : message)
                System.out.printf("%02X ", b);
            System.out.println();

            // Send the framed message to the server
            outToServer.write(message);

            System.out.printf("Message sent. Requested report: 0x%02X", report.getReportId());
            System.out.println();

            // Receive data from the server
            byte[] receivedData = receiveData(inFromServer);
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SerialReport createReport(byte componentId, byte reportId, byte[] payload) {
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
     * Reads until a framing byte is received.
     */
    private static byte[] receiveData(InputStream in) throws IOException {
        List<Byte> output = new ArrayList<>();
        int b;

        // Read until we get something other than the framing byte
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
        System.out.println("output: " + output.size() + " length: " + length);

        byte[] result = new byte[output.size()];
        for (int i = 0; i < output.size(); i++) {
            result[i] = output.get(i);
        }
        return result;
    }
}