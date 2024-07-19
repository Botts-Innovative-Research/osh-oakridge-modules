package com.botts.impl.sensor.rapiscan;

import com.google.gson.annotations.SerializedName;
import org.sensorhub.api.config.DisplayInfo;

public class EMLConfig {

    @DisplayInfo(label = "Collimated", desc = "Collimation status")
    public boolean isCollimated = false;

    @DisplayInfo(label="Supplemental Algorithm", desc="Check if the lane is VM250. For all lanes not designated to be EML lanes do NOT check this box.")
    @SerializedName(value="supplementalAlgorithm", alternate={"emlEnabled"})
    public boolean isSupplementalAlgorithm = false;
    @DisplayInfo(label = "Lane Width", desc = "Width of the lane")
    public double laneWidth = 0.0f;

    // TODO: Describe these
    @DisplayInfo(label = "Intervals")
    public int intervals = 0;

    @DisplayInfo(label = "Occupancy Holdin")
    public int occupancyHoldin = 0;
}
