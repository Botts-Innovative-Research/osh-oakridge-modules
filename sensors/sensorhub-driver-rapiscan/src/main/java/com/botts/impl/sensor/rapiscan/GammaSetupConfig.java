package com.botts.impl.sensor.rapiscan;

import org.sensorhub.api.config.DisplayInfo;

public class GammaSetupConfig {

    // TODO: Describe these
    @DisplayInfo(label = "Intervals")
    public int intervals = 5;

    @DisplayInfo(label = "Occupancy Holdin")
    public int occupancyHoldin = 10;

    @DisplayInfo(label = "Algorithm")
    public String algorithm = "1010";

    @DisplayInfo(label = "NSigma")
    public double nsigma = 6.0;
}
