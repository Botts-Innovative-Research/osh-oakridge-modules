package com.botts.impl.system.lane.helpers.occupancy.state;

import com.botts.impl.sensor.aspect.enums.ChannelStatus;
import com.botts.impl.sensor.aspect.enums.Inputs;
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
        return Integer.parseInt(dailyParsed[OBJECT_COUNT]);
    }

    private int getGammaChannelStatus() {
        return Integer.parseInt(dailyParsed[GAMMA_CHANNEL_STATUS]);
    }

    private int getNeutronChannelStatus() {
        return Integer.parseInt(dailyParsed[NEUTRON_CHANNEL_STATUS]);
    }

    private int getInputSignals() {
        return Integer.parseInt(dailyParsed[INPUT_SIGNALS]);
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

    @Override
    protected boolean isAlarming() {
        return isGammaAlarm() || isNeutronAlarm();
    }

    @Override
    protected boolean isOccupied() {
        return (getInputSignals() & Inputs.Occup0.getValue()) == Inputs.Occup0.getValue()
                || (getInputSignals() & Inputs.Occup1.getValue()) == Inputs.Occup1.getValue()
                || (getInputSignals() & Inputs.Occup2.getValue()) == Inputs.Occup2.getValue()
                || (getInputSignals() & Inputs.Occup3.getValue()) == Inputs.Occup3.getValue();
    }

    @Override
    protected boolean isNonOccupied() {
        return !isAlarming() && !isOccupied();
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
