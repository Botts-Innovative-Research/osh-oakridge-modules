package com.botts.impl.sensor.rs350.messages;

import java.util.List;

public class RS350LinEnergyCalibration {
    public RS350LinEnergyCalibration(List<Double> linEnCal) {
        this.linEnCal = linEnCal;
    }

    List<Double> linEnCal;

    public List<Double> getLinEnCal() {
        return linEnCal;
    }
}
