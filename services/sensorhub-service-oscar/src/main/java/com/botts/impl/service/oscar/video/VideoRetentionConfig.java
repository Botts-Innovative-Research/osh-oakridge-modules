package com.botts.impl.service.oscar.video;

import org.sensorhub.api.config.DisplayInfo;

public class VideoRetentionConfig {

    @DisplayInfo(label = "Max Age (days)", desc = "Maximum time in days to store video clips")
    public int maxAge = 7;

    @DisplayInfo(label = "3-Frame Persistence", desc = "Enable to permanently save 3 frames from each video clip")
    public boolean use3FramePersistence = true;

}
