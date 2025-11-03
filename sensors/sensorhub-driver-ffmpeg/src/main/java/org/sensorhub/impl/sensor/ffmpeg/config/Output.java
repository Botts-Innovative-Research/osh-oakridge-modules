package org.sensorhub.impl.sensor.ffmpeg.config;

import org.sensorhub.api.config.DisplayInfo;

public class Output {
    @DisplayInfo(label = "HLS", desc = "Enable/disable HLS and its file path output.")
    public boolean useHLS = false;

    @DisplayInfo(label = "Video Frames", desc = "Enable/disable binary video frame output.")
    public boolean useVideoFrames = false;
}
