package com.botts.impl.sensor.rs350.messages;

import java.util.List;

public class RS350CmpEnergyCalibration {
    public RS350CmpEnergyCalibration(List<Double> cmpEnCal) {
        int numEl = cmpEnCal.size();
        this.cmpEnCal = new double[numEl];
        for (int idx = 0; idx < numEl; ++idx) {
            this.cmpEnCal[idx]= cmpEnCal.get(idx);}
    }

    double[] cmpEnCal;

    public double[] getCmpEnCal() {
        return cmpEnCal;
    }
}
