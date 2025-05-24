package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Connection Configuration
 * @author Kalyn Stricklin
 * @since May 13, 2025
 */

public class ConnectionConfig {

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.REMOTE_ADDRESS)
    @DisplayInfo(label = "Remote Host")
    public String remoteHost;

}
