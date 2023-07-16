package com.botts.impl.sensor.rs350.messages;

public class RS350Item {

    public RS350Item(String rsiScanMode, Double rsiScanNumber, Double rsiScanTimeoutNumber, String rsiAnalysisEnabled) {
        this.rsiScanMode = rsiScanMode;
        this.rsiScanNumber = rsiScanNumber;
        this.rsiScanTimeoutNumber = rsiScanTimeoutNumber;
        this.rsiAnalysisEnabled = rsiAnalysisEnabled;
    }

    String rsiScanMode;
    Double rsiScanNumber;
    Double rsiScanTimeoutNumber;
    String rsiAnalysisEnabled;

    public String getRsiScanMode() {
        return rsiScanMode;
    }

    public Double getRsiScanNumber() {
        return rsiScanNumber;
    }

    public void setRsiScanNumber(Double rsiScanNumber) {
        this.rsiScanNumber = rsiScanNumber;
    }

    public Double getRsiScanTimeoutNumber() {
        return rsiScanTimeoutNumber;
    }

    public String getRsiAnalysisEnabled() {
        return rsiAnalysisEnabled;
    }
}
