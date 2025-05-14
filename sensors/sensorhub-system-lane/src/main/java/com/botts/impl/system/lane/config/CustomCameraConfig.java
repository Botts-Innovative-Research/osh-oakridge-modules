package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration for custom FFMpeg driver
 * @author Kalyn Stricklin, Kyle Fitzpatrick
 * @since May 2025
 */
public class CustomCameraConfig extends FFMpegConfig{

    @DisplayInfo(label = "Stream Path", desc = "Path for video stream. Only applies for the CUSTOM camera type.")
    public String streamPath;

    @DisplayInfo(label = "MJPEG", desc = "Select if video codec format is MJPEG. Otherwise driver will use H264.")
    public boolean isMJPEG = false;
}
