package com.botts.impl.sensor.rs350.messages;

import java.util.List;

public class RS350CmpEnergyCalibration {
    public RS350CmpEnergyCalibration(List<Double> cmpEnCal) {

        this.cmpEnCal = cmpEnCal;
    }

    List<Double> cmpEnCal;

    public List<Double> getCmpEnCal() {
        return cmpEnCal;
    }
}
