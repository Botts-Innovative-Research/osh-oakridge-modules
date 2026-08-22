package com.botts.impl.service.oscar.retention;

import org.sensorhub.api.config.DisplayInfo;

public class StoragePressureRetentionConfig {

    @DisplayInfo(label = "Trigger usage (%)", desc = "Start pressure cleanup when filesystem usage reaches this percentage.")
    public int triggerUsagePercent = 85;

    @DisplayInfo(label = "Target usage (%)", desc = "Continue pressure cleanup until filesystem usage reaches this percentage.")
    public int targetUsagePercent = 80;

    @DisplayInfo(label = "Check period (minutes)", desc = "How often filesystem usage is checked.")
    public int checkPeriodMinutes = 1;

    @DisplayInfo(label = "Minimum object age (minutes)", desc = "Files newer than this are never removed by pressure cleanup.")
    public int minimumObjectAgeMinutes = 10;

    @DisplayInfo(label = "Storage path", desc = "Path whose filesystem usage controls pressure cleanup.")
    public String storagePath = "files";

}
