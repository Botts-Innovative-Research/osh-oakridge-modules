package com.botts.impl.sensor.aspect.comm;

import org.sensorhub.api.comm.ICommConfig;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.config.DisplayInfo.ValueRange;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;

/**
 * Driver configuration options for the Modbus TCP/IP network protocol
 *
 * @author Michael Elmore
 * @since December 2023
 */
public class ModbusTCPConfig implements ICommConfig {
    @DisplayInfo(desc = "IP or DNS name of remote host")
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.REMOTE_ADDRESS)
    @DisplayInfo.AddressType(ICommNetwork.NetworkType.IP)
    @Required
    public String remoteHost;

    @DisplayInfo(desc = "Port number to connect to on remote host")
    @ValueRange(max = 65535)
    @Required
    public int remotePort = 502;

    @DisplayInfo(label = "Find device within address range")
    @Required
    public AddressRange addressRange = new AddressRange();

    public static class AddressRange {
        @DisplayInfo(label = "From")
        @Required
        public int from = 1;

        @DisplayInfo(label = "To")
        @Required
        public int to = 32;
    }

}
