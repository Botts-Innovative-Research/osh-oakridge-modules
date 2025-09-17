package com.botts.impl.process.lanevideo.config;

import org.sensorhub.api.config.DisplayInfo;

public class RecordingConfig {
    @DisplayInfo.Required
    @DisplayInfo(label = "Video Directory", desc = "Directory (if relative) should start with \"./\" and always end with \"/\".")
    public String directory = "./web/";
}
