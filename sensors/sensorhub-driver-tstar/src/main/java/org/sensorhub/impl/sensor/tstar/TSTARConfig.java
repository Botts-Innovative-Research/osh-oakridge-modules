package org.sensorhub.impl.sensor.tstar;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;
import org.sensorhub.impl.comm.TCPCommProviderConfig;

import java.io.IOException;


public class TSTARConfig extends SensorConfig
{
    @Required
    @DisplayInfo(label="Serial Number", desc="Serial Number")
    public String serialNumber = "TSTAR001";

    @DisplayInfo(label="HTTP", desc="HTTP configuration")
    public HTTPConfig http = new HTTPConfig();

    @DisplayInfo(desc = "Communication settings to connect to data stream")
    public CommProviderConfig<?> commSettings = new CommProviderConfig<>();

//    @DisplayInfo(label="Connection Options")
//    public RobustIPConnectionConfig connection = new RobustIPConnectionConfig();


    public TSTARConfig() throws IOException {
        http.user = "admin@gearsornl.com";
        http.password = "imAgearHEADnow";
        http.remoteHost = "127.0.0.1";
        http.remotePort = 10024;
    }

//    @Required
//    @DisplayInfo(label="Fleet ID", desc="ID of vehicle fleet (will be appended to system UID)")
//    public String fleetID;
//
//    @Required
//    @DisplayInfo(desc="Communication settings to connect to AVL data")
//    public CommProviderConfig<?> commSettings = new MultipleFilesProviderConfig();

}