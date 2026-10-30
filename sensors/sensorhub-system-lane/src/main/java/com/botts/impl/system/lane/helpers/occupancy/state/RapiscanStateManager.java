package com.botts.impl.system.lane.helpers.occupancy.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

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
    protected boolean parseDailyFile() {
        if (!dailyFile.hasData())
            return false;

        var message = dailyFile.getData().getStringValue(1);
        if (message == null || message.length() < 2)
            return false;

        var candidateState = message.substring(0, 2);
        if (!nonOccupiedStates.contains(candidateState)
                && !occupiedStates.contains(candidateState)
                && !alarmingStates.contains(candidateState))
            return false;

        stateChars = candidateState;
        return true;
    }
}
