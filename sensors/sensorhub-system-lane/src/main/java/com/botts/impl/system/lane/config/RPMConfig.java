package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

public class RPMConfig extends ConnectionConfig{


    @DisplayInfo.Required
    @DisplayInfo(label = "Remote Port")
    public int remotePort;
}
