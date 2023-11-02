package com.botts.impl.sensor.kromek.d5;

import com.botts.impl.sensor.kromek.d5.reports.KromekSerialCompressionEnabledReport;
import com.botts.impl.sensor.kromek.d5.reports.SerialReport;
import com.fazecast.jSerialComm.SerialPort;
import org.junit.Test;

import static com.botts.impl.sensor.kromek.d5.Shared.printCommPorts;
import static com.botts.impl.sensor.kromek.d5.Shared.sendAndReceiveReport;

public class SerialPortTest {
    @Test
    public void testSerialPort() {
        // COM3 is the port the Kromek D5 is connected to on my machine
        String comPortName = "COM3";

        try {
            printCommPorts();

            SerialPort commPort = SerialPort.getCommPort(comPortName);
            System.out.println("Opening port " + commPort.getSystemPortName());
            if (commPort.openPort()) {
                System.out.println("Port is open.");
            } else {
                System.out.println("Failed to open port.");
                return;
            }

            commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

            SerialReport report = new KromekSerialCompressionEnabledReport();
            var inputStream = commPort.getInputStream();
            var outputStream = commPort.getOutputStream();

            sendAndReceiveReport(report, inputStream, outputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}