package com.botts.impl.sensor.rapiscan.emlServices.types;

public enum RelayType {

    OFF(0), ON(1), AUTO(3);

    int value;

    RelayType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
