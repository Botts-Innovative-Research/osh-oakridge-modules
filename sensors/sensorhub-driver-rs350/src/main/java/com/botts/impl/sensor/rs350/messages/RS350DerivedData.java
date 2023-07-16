package com.botts.impl.sensor.rs350.messages;

public class RS350DerivedData {
    public RS350DerivedData(String alarmType, String classCode, Long startDateTime, Double duration) {
        this.alarmType = alarmType;
        this.classCode = classCode;
        this.startDateTime = startDateTime;
        this.duration = duration;
    }

    String alarmType;
    String classCode;
    Long startDateTime;
    Double duration;
}
