package com.botts.impl.sensor.aspect.comm;

import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;

import java.net.InetAddress;

/**
 * Communication provider for Modbus TCP/IP connections.
 *
 * @author Michael Elmore
 * @since December 2023
 */
public class ModbusTCPCommProvider extends AbstractModule<ModbusTCPCommProviderConfig> implements IModbusTCPCommProvider<ModbusTCPCommProviderConfig> {
    TCPMasterConnection tcpMasterConnection;

    @Override
    protected void doStart() throws SensorHubException {
        var config = this.config.protocol;

        int count = 0;
        int retryAttempts = config.retryAttempts;

        while (true) {
            try {
                InetAddress address = InetAddress.getByName(config.remoteHost);
                tcpMasterConnection = new TCPMasterConnection(address);
                tcpMasterConnection.setPort(config.remotePort);
                tcpMasterConnection.setTimeout(config.connectionTimeout);
                System.out.println("Attempting TCP Modbus connection");
                tcpMasterConnection.connect();
                System.out.println("TCP Modbus connection established");
                break;
            } catch (Exception e) {
                if(++count >= retryAttempts)
                    throw new SensorHubException("Cannot connect to remote host "
                            + config.remoteHost + ":" + config.remotePort + " via Modbus TCP", e);
            }

            try {
                Thread.sleep(config.retryDelay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void doStop() throws SensorHubException {
        try {
            tcpMasterConnection.close();
        } catch (Exception e) {
            throw new SensorHubException("Error stopping ModbusTCPCommProvider: " + e.getMessage(), e);
        }
    }

    @Override
    public TCPMasterConnection getConnection() {
        return tcpMasterConnection;
    }
}
