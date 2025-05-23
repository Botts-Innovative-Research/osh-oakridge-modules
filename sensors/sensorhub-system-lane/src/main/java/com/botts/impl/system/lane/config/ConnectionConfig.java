package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Connection Configuration
 * @author Kalyn Stricklin
 * @since May 13, 2025
 */

public class ConnectionConfig {

//    @DisplayInfo.Required
//    @DisplayInfo(label = "Unique ID", desc = "UID for new submodule. Please do not include osh UID prefix (i.e. urn:osh:sensor:ffmpeg:*, urn:osh:sensor:rapiscan:*, urn:osh:sensor:aspect:*)")
//    public String uniqueId;
//
//    @DisplayInfo.Required
//    @DisplayInfo(label = "Label", desc = "Friendly name for module")
//    public String label;

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.REMOTE_ADDRESS)
    @DisplayInfo(label = "Remote Host")
    public String remoteHost;

}
