package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration for FFMpeg driver (endpoints) based on camera manufacturer
 * @author Kyle Fitzpatrick
 * @since May 6, 2025
 */
public class FFMpegConfig {
    @DisplayInfo.Required
    @DisplayInfo(label = "FFmpeg Camera Type", desc = "FFmpeg Camera module to generate for this lane")
    public CameraType ffmpegType;

    @DisplayInfo.Required
    @DisplayInfo(label = "FFmpeg Unique ID", desc = "FFmpeg UID for new submodule if RPM type is specified. Please do not include osh UID prefix (i.e. urn:osh:sensor:rapiscan)")
    public String ffmpegUniqueId;

    @DisplayInfo.Required
    @DisplayInfo(label = "FFmpeg Label", desc = "Friendly name for FFmpeg module")
    public String ffmpegLabel;

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.REMOTE_ADDRESS)
    @DisplayInfo(label = "Remote Host")
    public String remoteHost;

    @DisplayInfo(label = "Username")
    public String username;

    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.PASSWORD)
    @DisplayInfo(label = "Password")
    public String password;

    // Port, fps, full endpoint should be autofilled
}
