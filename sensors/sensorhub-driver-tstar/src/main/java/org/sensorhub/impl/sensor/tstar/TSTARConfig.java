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

    protected String authToken;
    protected String campaignId;

    public TSTARConfig() throws IOException {
        http.user = "admin@gearsornl.com";
        http.password = "imAgearHEADnow";
        http.remoteHost = "127.0.0.1";
        http.remotePort = 10024;
    }

    public String getCampaignId() {
        return campaignId;
    }
    public String getAuthToken() {
        return authToken;
    }


}