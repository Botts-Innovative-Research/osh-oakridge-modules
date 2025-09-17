package com.botts.impl.system.lane.helpers.occupancy.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class RapiscanStateManager extends StateManager {
    private static final Logger logger = LoggerFactory.getLogger(RapiscanStateManager.class);
    String stateChars = "";
    static final Set<String> nonOccupiedStates = Set.of("GB", "GH", "GL", "NB", "NH");
    static final Set<String> occupiedStates = Set.of("GS", "NS");
    static final Set<String> alarmingStates = Set.of("GA", "NA");

    @Override
    protected void initStates() {
        Runnable[] nonOccupancyActions = new Runnable[3];
        Runnable[] occupancyActions = new Runnable[3];
        Runnable[] alarmActions = new Runnable[3];

        // The "to" and "from" transitional runnables may be unneeded. Keeping them only for the time being.
        // --------------------------- Non-occupancy actions ---------------------------
        nonOccupancyActions[TO_STATE] = () -> {};
        nonOccupancyActions[DURING_STATE] = () -> {
            if (occupiedStates.contains(stateChars))
                transitionToState(State.OCCUPANCY);
            else if (alarmingStates.contains(stateChars)) {
                // Might be possible to receive alarm characters when previously not occupied? Including transition in case
                transitionToState(State.ALARMING_OCCUPANCY);
            }
        };
        nonOccupancyActions[FROM_STATE] = () -> {};

        // --------------------------- Non-alarming occupancy actions ---------------------------
        occupancyActions[TO_STATE] = () -> {};
        occupancyActions[DURING_STATE] = () -> {
            if (alarmingStates.contains(stateChars))
                transitionToState(State.ALARMING_OCCUPANCY);
            else if (nonOccupiedStates.contains(stateChars))
                transitionToState(State.NON_OCCUPANCY);
        };
        occupancyActions[FROM_STATE] = () -> {};

        // --------------------------- Alarming occupancy actions ---------------------------
        alarmActions[TO_STATE] = () -> {};
        alarmActions[DURING_STATE] = () -> {
            if (nonOccupiedStates.contains(stateChars))
                transitionToState(State.NON_OCCUPANCY);
        };
        alarmActions[FROM_STATE] = () -> {};

        registerState(State.NON_OCCUPANCY,  nonOccupancyActions);
        registerState(State.OCCUPANCY,  occupancyActions);
        registerState(State.ALARMING_OCCUPANCY,  alarmActions);
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
