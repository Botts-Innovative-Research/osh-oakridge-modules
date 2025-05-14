package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration for custom FFMpeg driver
 * @author Alex Almanza, Kalyn Stricklin
 * @since May 13, 2025
 */

public class RPMConfig extends ConnectionConfig{

    @DisplayInfo.Required
    @DisplayInfo(label = "Remote Port")
    public int remotePort;
}
