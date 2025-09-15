package com.botts.impl.sensor.aspect.comm;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;
import org.sensorhub.impl.comm.TCPCommProvider;

/**
 * Configuration options for the Modbus TCP/IP communication provider
 */
public class ModbusTCPCommProviderConfig extends CommProviderConfig<ModbusTCPConfig> {


    @DisplayInfo(label="Connection Options")
    public RobustIPConnectionConfig connection = new RobustIPConnectionConfig();

    public ModbusTCPCommProviderConfig() {
        this.moduleClass = ModbusTCPCommProvider.class.getCanonicalName();
        this.protocol = new ModbusTCPConfig();
    }
}
