package com.botts.impl.sensor.kromekd5.messages;

public class D5Location {
    public D5Location(Double latitude, Double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    Double latitude;
    Double longitude;

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
