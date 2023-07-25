package com.botts.impl.sensor.rs350.messages;

import java.util.List;

public class RS350BackgroundMeasurement {
    public RS350BackgroundMeasurement(String classCode, Long startDateTime, Double realTimeDuration, List<Double> linEnCalSpectrum, List<Double> cmpEnCalSpectrum, Double gammaGrossCount, Double neutronGrossCount) {
        this.classCode = classCode;
        this.startDateTime = startDateTime;
        this.realTimeDuration = realTimeDuration;
        this.linEnCalSpectrum = linEnCalSpectrum;
        this.cmpEnCalSpectrum = cmpEnCalSpectrum;
        this.gammaGrossCount = gammaGrossCount;
        this.neutronGrossCount = neutronGrossCount;
    }

    String classCode;
    Long startDateTime;
    Double realTimeDuration;
    List<Double> linEnCalSpectrum;
    List<Double> cmpEnCalSpectrum;
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

    public List<Double> getLinEnCalSpectrum(){
        return linEnCalSpectrum;
    }

    public List<Double> getCmpEnCalSpectrum(){
        return cmpEnCalSpectrum;
    }

    public Double getGammaGrossCount(){
        return gammaGrossCount;
    }

    public Double getNeutronGrossCount(){
        return neutronGrossCount;
    }
}
