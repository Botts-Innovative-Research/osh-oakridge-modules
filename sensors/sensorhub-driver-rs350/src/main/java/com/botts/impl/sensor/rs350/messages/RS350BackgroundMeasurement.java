package com.botts.impl.sensor.rs350.messages;

import java.util.List;

public class RS350BackgroundMeasurement {
    public RS350BackgroundMeasurement(String classCode, Long startDateTime, Double realTimeDuration, List<Double> linEnCalSpectrum, List<Double> cmpEnCalSpectrum, Double gammaGrossCount, Double neutronGrossCount) {
        this.classCode = classCode;
        this.startDateTime = startDateTime;
        this.realTimeDuration = realTimeDuration;
        int linNumEl = linEnCalSpectrum.size();
        this.linEnCalSpectrum = new double[linNumEl];
        for (int idx = 0; idx < linNumEl; ++idx) {
            this.linEnCalSpectrum[idx]= linEnCalSpectrum.get(idx);}
        int cmpNumEl = cmpEnCalSpectrum.size();
        this.cmpEnCalSpectrum = new double[cmpNumEl];
        for (int idx = 0; idx < cmpNumEl; ++idx) {
            this.cmpEnCalSpectrum[idx]= cmpEnCalSpectrum.get(idx);}
        this.gammaGrossCount = gammaGrossCount;
        this.neutronGrossCount = neutronGrossCount;
    }

    String classCode;
    Long startDateTime;
    Double realTimeDuration;
    double[] linEnCalSpectrum;
    double[] cmpEnCalSpectrum;
    Double gammaGrossCount;
    Double neutronGrossCount;

    public String getClassCode(){
        return classCode;
    }

    public Long getStartDateTime(){
        return startDateTime;
    }

    public Double getRealTimeDuration(){
        return realTimeDuration;
    }

    public double[] getLinEnCalSpectrum(){
        return linEnCalSpectrum;
    }

    public double[] getCmpEnCalSpectrum(){
        return cmpEnCalSpectrum;
    }

    public Double getGammaGrossCount(){
        return gammaGrossCount;
    }

    public Double getNeutronGrossCount(){
        return neutronGrossCount;
    }
}
