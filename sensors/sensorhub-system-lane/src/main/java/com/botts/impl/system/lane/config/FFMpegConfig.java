package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration for FFMpeg driver (endpoints) based on camera manufacturer
 * @author Kyle Fitzpatrick
 * @since May 6, 2025
 */
public class FFMpegConfig extends ConnectionConfig{

    @DisplayInfo(label = "Username")
    public String username;

    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.PASSWORD)
    @DisplayInfo(label = "Password")
    public String password;

}
