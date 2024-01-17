package com.botts.impl.sensor.aspect.comm;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * Communication provider for Modbus TCP/IP connections.
 *
 * @author Michael Elmore
 * @since December 2023
 */
public class ModbusTCPCommModuleDescriptor extends JarModuleProvider implements IModuleProvider {
    @Override
    public String getModuleName() {
        return "Modbus TCP Comm Driver";
    }

    @Override
    public String getModuleDescription() {
        return "Modbus TCP/IP communication provider using J2Mod.";
    }

    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return ModbusTCPCommProvider.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return ModbusTCPCommProviderConfig.class;
    }
}
