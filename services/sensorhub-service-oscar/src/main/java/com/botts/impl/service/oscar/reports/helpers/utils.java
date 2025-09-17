package com.botts.impl.service.oscar.reports.helpers;

public class utils {

    public double calcEMLAlarmRate(int emlSupp, int alarm){
        if (emlSupp + alarm == 0) return 0.0;

        return emlSupp / (emlSupp + alarm);
    }

    public double calculateAlarmingOccRate(int alarm, int occupancyCount){
        if(occupancyCount == 0) return 0.0;

        return alarm / occupancyCount;
    }

}
