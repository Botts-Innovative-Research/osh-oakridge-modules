package com.botts.impl.system.lane.helpers.occupancy.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class RapiscanStateManager extends StateManager {
    private static final Logger logger = LoggerFactory.getLogger(RapiscanStateManager.class);
    String stateChars = "";
    static final Set<String> nonOccupiedStates = Set.of("GB", "GH", "GL", "NB", "NH", "GX");
    static final Set<String> occupiedStates = Set.of("GS", "NS");
    static final Set<String> alarmingStates = Set.of("GA", "NA");

    @Override
    protected boolean isAlarming() {
        return alarmingStates.contains(stateChars);
    }

    @Override
    protected boolean isOccupied() {
        return occupiedStates.contains(stateChars);
    }

    protected boolean isNonOccupied() {
        return nonOccupiedStates.contains(stateChars);
    }



    @Override
    protected void parseDailyFile() {
        if (dailyFile.hasData() && dailyFile.getData().getStringValue(1) != null) {
            stateChars = dailyFile.getData().getStringValue(1).substring(0, 2);
        } else {
            //logger.warn("Daily file has no data");
        }
    }
}
