package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration for FFMpeg driver (endpoints) based on camera manufacturer
 * @author Kyle Fitzpatrick, Kalyn Stricklin
 * @since May 6, 2025
 */
public class AxisCameraConfig extends FFMpegConfig{

    public enum CodecEndpoint {
        H264("/axis-media/media.amp?adjustablelivestream=1&resolution=640x480&videocodec=h264&videokeyframeinterval=15"),
        MJPEG("/axis-media/media.amp?adjustablelivestream=1&resolution=640x480&videocodec=jpeg&videokeyframeinterval=15");

        private final String path;

        public String getPath() {
            return this.path;
        }

        CodecEndpoint(String path) {
            this.path = path;
        }
    }

    @DisplayInfo(label = "Stream Codec", desc = "Generate streaming URL with the selected codec.")
    public CodecEndpoint streamPath = CodecEndpoint.H264;

}
