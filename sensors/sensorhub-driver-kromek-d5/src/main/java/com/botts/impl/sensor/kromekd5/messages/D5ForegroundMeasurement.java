package com.botts.impl.sensor.kromekd5.messages;

import java.util.List;

public class D5ForegroundMeasurement {

    public D5ForegroundMeasurement(Long startDateTime, Double duration, List<Double> spectrum, Double neutronGrossCounts, Double doseRate){
        this.startDateTime = startDateTime;
        this.duration = duration;
        this.spectrum = spectrum;
        this.neutronGrossCounts = neutronGrossCounts;
        this.doseRate = doseRate;

    }

    Long startDateTime;
    Double duration;
    List<Double> spectrum;
    Double neutronGrossCounts;
    Double doseRate;

    public Long getStartDateTime() {
        return startDateTime;
    }

    public Double getDuration() {
        return duration;
    }

    public List<Double> getSpectrum() {
        return spectrum;
    }

    public Double getNeutronGrossCounts() {
        return neutronGrossCounts;
    }

    public Double getDoseRate() {
        return doseRate;
    }
}
