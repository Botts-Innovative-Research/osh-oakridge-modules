package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration for FFMpeg driver (endpoints) based on camera manufacturer
 * @author Kyle Fitzpatrick, Kalyn Stricklin
 * @since May 6, 2025
 */
public class AxisCameraConfig extends FFMpegConfig{

    @DisplayInfo(label = "Stream Path", desc = "Path for video stream. Only applies for the CUSTOM camera type.")
    public String streamPath = "/axis-media/media.amp?adjustablelivestream=1&resolution=640x480&videocodec=h264&videokeyframeinterval=15";

}
