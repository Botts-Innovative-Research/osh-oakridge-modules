package com.botts.impl.sensor.rs350.messages;

public class RS350DerivedData {
    public RS350DerivedData(String remark, String classCode, Long startDateTime, Double duration) {
        this.remark = remark;
        this.classCode = classCode;
        this.startDateTime = startDateTime;
        this.duration = duration;
    }

    String remark;
    String classCode;
    Long startDateTime;
    Double duration;

    public String getRemark() {
        return remark;
    }

    public String getClassCode() {
        return classCode;
    }

    public Long getStartDateTime() {
        return startDateTime;
    }

    public Double getDuration() {
        return duration;
    }
}
