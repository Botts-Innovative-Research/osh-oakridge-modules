package com.botts.impl.system.lane.helpers.occupancy.state;

import com.botts.impl.process.rs350.occupancy.DailyFileStruct;
import com.botts.impl.sensor.aspect.enums.ChannelStatus;
import com.botts.impl.sensor.aspect.enums.Inputs;
import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.DataComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Rs350StateManager extends StateManager {
    private static final Logger logger = LoggerFactory.getLogger(Rs350StateManager.class);
    private volatile boolean isAlarming = false;

    @Override
    protected boolean isAlarming() {
        return isAlarming;
    }

    @Override
    protected boolean isOccupied() {
        return isAlarming;
    }

    @Override
    protected boolean isNonOccupied() {
        return !isAlarming;
    }

    @Override
    protected void parseDailyFile() {
        if (dailyFile.hasData() && dailyFile.getData().getStringValue(1) != null) {
            isAlarming = ((Boolean)dailyFile.getComponent(DailyFileStruct.ALARM_NAME)).getValue();
        } else {
            logger.warn("Daily file has no data");
        }
    }
}
