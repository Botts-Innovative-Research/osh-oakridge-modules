package com.botts.impl.sensor.aspect;

import com.botts.impl.sensor.aspect.registers.DeviceDescriptionRegisters;
import com.botts.impl.sensor.aspect.registers.MonitorRegisters;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import org.junit.Test;

import java.net.InetAddress;

public class J2modTest {
    @Test
    public void J2Test() {
        // We need to connect to the Aspect Hardware Emulator via TCP.
        // The IP address is 127.0.0.1, and the port is 502.
        // The Aspect Hardware Emulator is considered the slave, and we are considered the master.

        try {
            InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
            TCPMasterConnection tcpMasterConnection = new TCPMasterConnection(inetAddress);
            tcpMasterConnection.setPort(502);
            tcpMasterConnection.connect();

            DeviceDescriptionRegisters deviceDescriptionRegisters = new DeviceDescriptionRegisters(tcpMasterConnection);
            deviceDescriptionRegisters.readRegisters(4);
            // System.out.println(deviceDescriptionRegisters);

            MonitorRegisters monitorRegisters = new MonitorRegisters(tcpMasterConnection, deviceDescriptionRegisters.getMonitorRegistersBaseAddress(), deviceDescriptionRegisters.getMonitorRegistersNumberOfRegisters());
            monitorRegisters.readRegisters(4);
            System.out.println(monitorRegisters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}