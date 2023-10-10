package com.botts.impl.sensor.kromek.d5;

import com.botts.impl.sensor.kromek.d5.message.SerialMessage;
import com.botts.impl.sensor.kromek.d5.reports.KromekSerialCompressionEnabledReport;
import com.botts.impl.sensor.kromek.d5.reports.KromekSerialEthernetConfigReport;
import com.botts.impl.sensor.kromek.d5.reports.KromekSerialUnitIDReport;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.botts.impl.sensor.kromek.d5.message.Constants.*;
import static com.botts.impl.sensor.kromek.d5.message.SerialMessage.decodeSLIP;

public class TCPTest {
    @Test
    public void asdf(){

    }

    @Test
    public void testTCP() {
        // Define the IP address and port number of the server
        String ipAddress = "192.168.1.138";
        int portNumber = 12345;

        // Create a TCP socket and connect to the server
        try (Socket clientSocket = new Socket(ipAddress, portNumber)) {
            System.out.println("Connected to server");

            // Create input and output streams for the socket
            InputStream inFromServer = clientSocket.getInputStream();
            OutputStream outToServer = clientSocket.getOutputStream();

            // Create a message to send
            SerialMessage report = new KromekSerialCompressionEnabledReport();
            byte[] message = report.encodeRequest();
            System.out.println("Message: " + Arrays.toString(message));
            System.out.print("Hex data: ");
            for (byte b : message)
                System.out.printf("%02X ", b);
            System.out.println();

            // Send the framed message to the server
            outToServer.write(message);

            // Flush the output stream to ensure the message is sent
            outToServer.flush();

            System.out.printf("Message sent. Requested report: 0x%02X", report.getReportId());
            System.out.println();

            // Receive data from the server
            byte[] receivedData = receiveData(inFromServer);
            System.out.println("Received data: " + Arrays.toString(receivedData));

            // Decode the received data using SLIP framing
            byte[] decodedData = decodeSLIP(receivedData);

            // Print the received data
            System.out.println(" Decoded data: " + Arrays.toString(decodedData));

            // Print as hex
            System.out.print("     Hex data: ");
            for (byte b : decodedData) {
                System.out.printf("%02X ", b);
            }
            System.out.println();

            //First two bytes is the size
            int size = decodedData[0] + (decodedData[1] << 8);
            System.out.println("Size: " + size);

            //Next byte is the mode
            int mode = decodedData[2];
            System.out.println("Mode: " + mode);

            //Next byte is the componentId then reportId
            byte componentId = decodedData[3];
            byte reportId = decodedData[4];
            System.out.printf("ComponentId: " + componentId + " (%02X) ReportId: " + reportId + " (%02X)", componentId, reportId);
            System.out.println();

            if (reportId == KROMEK_SERIAL_REPORTS_IN_ACK_ID) {
                System.out.println("ACK");
                return;
            }
            if (reportId == KROMEK_SERIAL_REPORTS_ACK_REPORT_ID_ERROR) {
                System.out.println("ACK Error");
                return;
            }

            //Last two bytes are the CRC
            int crc = decodedData[decodedData.length - 2] + (decodedData[decodedData.length - 1] << 8);
            System.out.println("CRC: " + crc);

            //The payload is everything in between
            byte[] payload = Arrays.copyOfRange(decodedData, 5, decodedData.length - 2);

            report = createReport(componentId, reportId, payload);
            System.out.println("Report: " + report);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SerialMessage createReport(byte componentId, byte reportId, byte[] payload) {
        if (reportId == KROMEK_SERIAL_REPORTS_IN_UNIT_ID_ID || reportId == KROMEK_SERIAL_REPORTS_OUT_UNIT_ID_ID) {
            return new KromekSerialUnitIDReport(componentId, reportId, payload);
        } else if (reportId == KROMEK_SERIAL_REPORTS_IN_ETHERNET_CONFIG_ID || reportId == KROMEK_SERIAL_REPORTS_OUT_ETHERNET_CONFIG_ID) {
            return new KromekSerialEthernetConfigReport(componentId, reportId, payload);
        } else if (reportId == KROMEK_SERIAL_REPORTS_IN_COMPRESSION_ENABLED_ID || reportId == KROMEK_SERIAL_REPORTS_OUT_COMPRESSION_ENABLED_ID) {
            return new KromekSerialCompressionEnabledReport(componentId, reportId, payload);
        } else {
            throw new RuntimeException("Unknown reportId: " + reportId);
        }
    }

    /**
     * Receive data from the given input stream.
     * Reads until a framing byte is received.
     */
    private static byte[] receiveData(InputStream in) throws IOException {
        System.out.println("Receiving data");
        List<Byte> output = new ArrayList<>();
        int b;

        b = in.read();
        System.out.println("First byte: " + (byte) b);
        if ((byte) b != KROMEK_SERIAL_FRAMING_FRAME_BYTE) {
            throw new RuntimeException("First byte is not framing byte");
        }

        // First two bytes after framing byte are the message length
        byte length1 = (byte) in.read();
        if (length1 == KROMEK_SERIAL_FRAMING_FRAME_BYTE) {
            length1 = (byte) in.read();
        }
        byte length2 = (byte) in.read();
        System.out.println("Length1: " + length1 + " Length2: " + length2);
        int length = length1 + (length2 << 8);

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
        System.out.println("Finished receiving data");
        return result;
    }
}