package com.botts.impl.sensor.kromekd5.messages;

import java.util.List;

public class D5BackgroundMeasurement {

    public D5BackgroundMeasurement(Long startDateTime, Double duration, List<Double> spectrum, Double neutronGrossCounts){
        this.startDateTime = startDateTime;
        this.duration = duration;
        this.spectrum = spectrum;
        this.neutronGrossCounts = neutronGrossCounts;
    }

    Long startDateTime;
    Double duration;
    List<Double> spectrum;
    Double neutronGrossCounts;


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

}
