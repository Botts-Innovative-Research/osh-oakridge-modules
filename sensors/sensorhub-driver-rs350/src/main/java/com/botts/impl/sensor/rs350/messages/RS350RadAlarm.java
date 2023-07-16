package com.botts.impl.sensor.rs350.messages;

public class RS350RadAlarm {
    public RS350RadAlarm(String categoryCode, String decription) {
        this.categoryCode = categoryCode;
        this.description = decription;
    }

    String categoryCode;
    String description;
}
