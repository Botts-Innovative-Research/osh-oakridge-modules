package com.botts.impl.sensor.rs350.messages;

public class RS350InstrumentCharacteristics {

    public RS350InstrumentCharacteristics(String deviceName, Double batteryCharge) {
        this.deviceName = deviceName;
        this.batteryCharge = batteryCharge;
    }

    String deviceName;
    Double batteryCharge;

    public String getDeviceName(){
        return deviceName;
    }

    public Double getBatteryCharge() {
        return batteryCharge;
    }
}
