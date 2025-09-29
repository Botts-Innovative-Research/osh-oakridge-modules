package com.botts.impl.service.oscar.spreadsheet;

import com.botts.impl.system.lane.config.LaneConfig;

import java.util.Map;

public class LaneMapper implements CSVMapper<LaneConfig> {



    public LaneConfig createConfig(Map<String, String> csvData) {
        LaneConfig config = new LaneConfig();
    }

    @Override
    public String createCSV(LaneConfig config) {
        return "";
    }

}
