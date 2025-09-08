package com.botts.impl.process.lanevideo.helpers.occupancy.state;

import com.botts.impl.sensor.aspect.enums.*;

import net.opengis.swe.v20.DataComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AspectStateManager extends StateManager {
    private static final Logger logger = LoggerFactory.getLogger(AspectStateManager.class);
    private int objectCount = 0;
    String[] dailyParsed;

    // Daily file indices
    private static final int INPUT_SIGNALS = 1;
    private static final int GAMMA_CHANNEL_STATUS = 2;
    private static final int NEUTRON_CHANNEL_STATUS = 6;
    private static final int OBJECT_COUNT = 9;


    private int getObjectCount() {
        return Integer.getInteger(dailyParsed[OBJECT_COUNT]);
    }

    private int getGammaChannelStatus() {
        return Integer.getInteger(dailyParsed[GAMMA_CHANNEL_STATUS]);
    }

    private int getNeutronChannelStatus() {
        return Integer.getInteger(dailyParsed[NEUTRON_CHANNEL_STATUS]);
    }

    private int getInputSignals() {
        return Integer.getInteger(dailyParsed[INPUT_SIGNALS]);
    }

    // The following three methods are from the aspect MonitorRegisters class
    private boolean isGammaAlarm() {
        return (getGammaChannelStatus() & ChannelStatus.Alarm1.getValue()) == ChannelStatus.Alarm1.getValue()
                || (getGammaChannelStatus() & ChannelStatus.Alarm2.getValue()) == ChannelStatus.Alarm2.getValue();
    }

    private boolean isNeutronAlarm() {
        return (getNeutronChannelStatus() & ChannelStatus.Alarm1.getValue()) == ChannelStatus.Alarm1.getValue()
                || (getNeutronChannelStatus() & ChannelStatus.Alarm2.getValue()) == ChannelStatus.Alarm2.getValue();
    }

    protected boolean isAlarming() {
        return isGammaAlarm() || isNeutronAlarm();
    }

    protected boolean isOccupied() {
        return (getInputSignals() & Inputs.Occup0.getValue()) == Inputs.Occup0.getValue()
                || (getInputSignals() & Inputs.Occup1.getValue()) == Inputs.Occup1.getValue()
                || (getInputSignals() & Inputs.Occup2.getValue()) == Inputs.Occup2.getValue()
                || (getInputSignals() & Inputs.Occup3.getValue()) == Inputs.Occup3.getValue();
    }

    @Override
    protected void initStates() {
        Runnable[] nonOccupancyActions = new Runnable[3];
        Runnable[] occupancyActions = new Runnable[3];
        Runnable[] alarmActions = new Runnable[3];

        // The "to" and "from" transitional runnables may be unneeded. Keeping them only for the time being.
        // --------------------------- Non-occupancy actions ---------------------------
        nonOccupancyActions[TO_STATE] = () -> {};
        nonOccupancyActions[DURING_STATE] = () -> {
            if (isOccupied())
                transitionToState(State.OCCUPANCY);
            // The commented code is temporary. May be needed if isOccupied does not work for some reason.
            /*
            int newCount = getObjectCount();
            boolean objectCountChanged = newCount != objectCount;
            objectCount = newCount;
            if (objectCountChanged) {
                transitionToState(State.OCCUPANCY);
            }
             */
        };
        nonOccupancyActions[FROM_STATE] = () -> {};

        // --------------------------- Non-alarming occupancy actions ---------------------------
        occupancyActions[TO_STATE] = () -> {};
        occupancyActions[DURING_STATE] = () -> {
            if (isAlarming())
                transitionToState(State.ALARMING_OCCUPANCY);
            else if (!isOccupied())
                transitionToState(State.NON_OCCUPANCY);
        };
        occupancyActions[FROM_STATE] = () -> {};

        // --------------------------- Alarming occupancy actions ---------------------------
        alarmActions[TO_STATE] = () -> {};
        alarmActions[DURING_STATE] = () -> {
            if (!isOccupied())
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
            dailyParsed = dailyFile.getData().getStringValue(1).split(",");
        } else {
            logger.warn("Daily file has no data");
        }
    }
}
