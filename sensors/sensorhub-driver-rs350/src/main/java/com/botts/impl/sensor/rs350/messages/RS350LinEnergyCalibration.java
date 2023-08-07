package com.botts.impl.sensor.rs350.messages;

import java.util.List;

public class RS350LinEnergyCalibration {
    public RS350LinEnergyCalibration(List<Double> linEnCal) {
//        this.linEnCal = linEnCal.toArray(new Double[linEnCal.size()]);
        int numEl = linEnCal.size();
        this.linEnCal = new double[numEl];
        for (int idx = 0; idx < numEl; ++idx) {
            this.linEnCal[idx]= linEnCal.get(idx);}
    }

    double[] linEnCal;

    public double[] getLinEnCal() {
        return linEnCal;
    }
}