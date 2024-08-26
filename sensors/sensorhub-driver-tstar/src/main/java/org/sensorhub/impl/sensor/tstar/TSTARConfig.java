package org.sensorhub.impl.sensor.tstar;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.HTTPConfig;

import java.io.IOException;


public class TSTARConfig extends SensorConfig
{
    @Required
    @DisplayInfo(label="Serial Number", desc="Serial Number")
    public String serialNumber = "TSTAR001";

    @DisplayInfo(label="HTTP", desc="HTTP configuration")
    public HTTPConfig http = new HTTPConfig();

    @DisplayInfo(label = "Username")
    public String username;

    @DisplayInfo(label = "Password")
    public String password;

    protected String authToken;
    public String campaignId;

    public TSTARConfig() throws IOException {
        http.user = "";
        http.password = "";
        http.remoteHost = "";
        http.remotePort = 10024;
    }



}