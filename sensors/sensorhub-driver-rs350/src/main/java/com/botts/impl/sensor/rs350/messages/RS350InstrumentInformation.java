package com.botts.impl.sensor.rs350.messages;

public class RS350InstrumentInformation {
    public RS350InstrumentInformation(String manufacturerName, String identifier, String modelName, String classCode) {
        this.manufacturerName = manufacturerName;
        this.identifier = identifier;
        this.modelName = modelName;
        this.classCode = classCode;

    }

    String manufacturerName;
    String identifier;
    String modelName;
    String classCode;


}
