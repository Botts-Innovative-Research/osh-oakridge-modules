package com.botts.impl.sensor.rs350.messages;

import java.util.List;

public class RS350ForegroundMeasurement {
    public RS350ForegroundMeasurement(String classCode, Long startDateTime, Double realTimeDuration, List<Double> linEnCalSpectrum, List<Double> cmpEnCalSpectrum, Double gammaGrossCount, Double neutronGrossCount, Double doseRate, Double lat, Double lon, Double alt) {
        this.classCode = classCode;
        this.startDateTime = startDateTime;
        this.realTimeDuration = realTimeDuration;
        this.linEnCalSpectrum = linEnCalSpectrum;
        this.cmpEnCalSpectrum = cmpEnCalSpectrum;
        this.gammaGrossCount = gammaGrossCount;
        this.neutronGrossCount = neutronGrossCount;
        this.doseRate = doseRate;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    String classCode;
    Long startDateTime;
    Double realTimeDuration;
    List<Double> linEnCalSpectrum;
    List<Double> cmpEnCalSpectrum;
    Double gammaGrossCount;
    Double neutronGrossCount;
    Double doseRate;
    Double lat;
    Double lon;
    Double alt;

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

    public Double getDoseRate(){
        return doseRate;
    }

    public Double getLat(){
        return lat;
    }

    public Double getLon(){
        return lon;
    }

    public Double getAlt(){
        return alt;
    }
}
