package com.botts.impl.system.lane.helpers.occupancy.state;

import com.botts.impl.process.rs350.occupancy.DailyFileStruct;
import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.Time;

import java.time.Instant;

public class Rs350StateManager extends StateManager {
    private volatile boolean isAlarming = false;
    private volatile boolean isOccupied = false;
    private Instant occupancyStartTimeHint;

    @Override
    protected boolean isAlarming() {
        return isAlarming;
    }

    @Override
    protected boolean isOccupied() {
        return isOccupied;
    }

    @Override
    protected boolean isNonOccupied() {
        return !isOccupied && !isAlarming;
    }

    @Override
    protected boolean parseDailyFile() {
        if (!dailyFile.hasData())
            return false;

        var occupiedComponent = dailyFile.getComponent(DailyFileStruct.OCCUPIED_NAME);
        var startTimeComponent = dailyFile.getComponent(DailyFileStruct.OCCUPANCY_START_TIME_NAME);
        var alarmComponent = dailyFile.getComponent(DailyFileStruct.ALARM_NAME);
        if (!(occupiedComponent instanceof Boolean) || !occupiedComponent.hasData()
                || !(startTimeComponent instanceof Time) || !startTimeComponent.hasData()
                || !(alarmComponent instanceof Boolean) || !alarmComponent.hasData())
            return false;

        isOccupied = ((Boolean) occupiedComponent).getValue();
        isAlarming = ((Boolean) alarmComponent).getValue();
        double startTime = ((Time) startTimeComponent).getValue().getAsDouble();
        occupancyStartTimeHint = isOccupied && startTime > 0.0
                ? toInstant(startTime)
                : null;
        return true;
    }

    @Override
    public boolean hasActiveOccupancy() {
        return isOccupied;
    }

    @Override
    public Instant getOccupancyStartTimeHint() {
        return occupancyStartTimeHint;
    }

    private static Instant toInstant(double epochSeconds) {
        long seconds = (long) Math.floor(epochSeconds);
        long nanos = Math.round((epochSeconds - seconds) * 1_000_000_000.0);
        if (nanos == 1_000_000_000L) {
            seconds++;
            nanos = 0;
        }
        return Instant.ofEpochSecond(seconds, nanos);
    }
}
