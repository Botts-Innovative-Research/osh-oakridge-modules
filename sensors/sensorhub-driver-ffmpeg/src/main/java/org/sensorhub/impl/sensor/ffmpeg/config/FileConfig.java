package org.sensorhub.impl.sensor.ffmpeg.config;

import org.sensorhub.api.config.DisplayInfo;

public class FileConfig {
    @DisplayInfo(label = "Clips Directory", desc = "Directory for saving video clips.")
    @DisplayInfo.FieldType(value = DisplayInfo.FieldType.Type.FILESYSTEM_PATH)
    public String videoClipDirectory;

    @DisplayInfo(label = "HLS Directory", desc = "Directory for saving HLS files. Only takes affect when HLS is enabled.")
    @DisplayInfo.FieldType(value = DisplayInfo.FieldType.Type.FILESYSTEM_PATH)
    public String hlsDirectory;
}
