package com.botts.impl.sensor.kromekd5.messages;

import com.botts.impl.utils.n42.NuclideType;

import java.util.List;

public class D5AnalysisResults {
    public D5AnalysisResults(String resultStatus, List<NuclideType> results){

    }

    String resultStatus;
    List<NuclideType> results;

    public String getResultStatus() {
        return resultStatus;
    }

    public List<NuclideType> getResults() {
        return results;
    }
}
