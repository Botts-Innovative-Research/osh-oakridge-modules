package com.botts.impl.process.lanevideo.config;

import org.sensorhub.api.config.DisplayInfo;

public class CameraIdConfig {
    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.SYSTEM_UID)
    @DisplayInfo(label = "Camera ID")
    public String cameraId;
}
