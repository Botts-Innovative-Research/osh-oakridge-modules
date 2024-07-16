package com.botts.impl.sensor.rapiscan;

import gov.llnl.ernie.api.ERNIE_lane;
import org.sensorhub.api.config.DisplayInfo;

public class LaneConfig {

    @DisplayInfo(label = "Lane Name", desc = "Human readable name of the lane")
    @DisplayInfo.Required
    public String laneName;

    @DisplayInfo(label = "Lane ID", desc = "ID of lane")
    @DisplayInfo.Required
    public int laneID;

    @DisplayInfo(label = "Collimated", desc = "Collimation status")
    public boolean isCollimated = false;

    @DisplayInfo(label = "Lane Width", desc = "Width of the lane")
    public double laneWidth = 0.0f;

    // TODO: Describe these
    @DisplayInfo(label = "Intervals")
    public int intervals = 0;

    @DisplayInfo(label = "Occupancy Holdin")
    public int occupancyHoldin = 0;

}
