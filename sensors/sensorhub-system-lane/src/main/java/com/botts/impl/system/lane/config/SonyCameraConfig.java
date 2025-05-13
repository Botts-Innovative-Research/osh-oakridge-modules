package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration for FFMpeg driver (endpoints) based on camera manufacturer
 * @author Kyle Fitzpatrick
 * @since May 6, 2025
 */
public class SonyCameraConfig extends FFMpegConfig{

    @DisplayInfo(label = "Stream Path", desc = "Path for video stream. Only applies for the CUSTOM camera type.")
    public String streamPath = ":554/media/video1";

}
