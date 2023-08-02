package com.botts.impl.sensor.kromekd5.messages;

import java.util.List;

public class D5EnergyCalibration {
    public D5EnergyCalibration(List<Double> energyCalibration){
        this.energyCalibration = energyCalibration;
    }

    List<Double> energyCalibration;

    public List<Double> getEnergyCalibration() {
        return energyCalibration;
    }
}
