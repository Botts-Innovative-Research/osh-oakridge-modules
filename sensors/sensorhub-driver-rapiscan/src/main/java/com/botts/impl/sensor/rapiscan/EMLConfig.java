package com.botts.impl.sensor.rapiscan;

import org.sensorhub.api.config.DisplayInfo;

public class EMLConfig {

    @DisplayInfo(label="Enable EML Analysis", desc="Check if the lane is VM250. For all lanes not designated to be EML lanes do NOT check this box.")
    public boolean emlEnabled = false;

    @DisplayInfo(label = "Is Collimated", desc = "Collimation status")
    public boolean isCollimated = false;

    @DisplayInfo(label = "Lane Width (m)", desc = "Width of the lane in meters")
    public double laneWidth = 4.82f;

}
