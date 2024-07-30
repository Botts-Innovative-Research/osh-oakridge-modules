package com.botts.impl.sensor.rapiscan;

import org.sensorhub.api.config.DisplayInfo;

public class SetupGammaConfig {

    // TODO: Describe these
    @DisplayInfo(label = "Intervals")
    public int intervals = 5;

    @DisplayInfo(label = "Occupancy Holdin")
    public int occupancyHoldin = 10;

    @DisplayInfo(label = "Algorithm", desc = "Threshold algorithm types enabled (SUM, HORIZ, VERT, SINGLE)")
    public String algorithm = "1000";

    @DisplayInfo(label = "N Sigma", desc = "N value for threshold calculation")
    public double nsigma = 7.0;
}
