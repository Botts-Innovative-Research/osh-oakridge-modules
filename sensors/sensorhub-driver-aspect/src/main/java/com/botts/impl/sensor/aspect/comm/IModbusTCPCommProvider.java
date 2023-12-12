package com.botts.impl.sensor.aspect.comm;

import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.module.IModule;

/**
 * Interface for Modbus communication providers
 *
 * @param <ConfigType> Comm module config type
 * @author Michael Elmore
 * @since December 2023
 */
public interface IModbusTCPCommProvider<ConfigType extends CommProviderConfig<?>> extends IModule<ConfigType> {
    TCPMasterConnection getConnection();
}
