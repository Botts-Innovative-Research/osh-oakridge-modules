package com.botts.impl.sensor.aspect.comm;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.impl.comm.TCPCommProvider;

/**
 * Configuration options for the Modbus TCP/IP communication provider
 */
public class ModbusTCPCommProviderConfig extends CommProviderConfig<ModbusTCPConfig> {
    public ModbusTCPCommProviderConfig() {
        this.moduleClass = TCPCommProvider.class.getCanonicalName();
        this.protocol = new ModbusTCPConfig();
    }
}
