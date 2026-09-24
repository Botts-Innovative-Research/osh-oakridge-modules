package com.botts.impl.process.rs350.occupancy;

import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

public class DailyFileStruct {

    final static RADHelper radHelper = new RADHelper();
    public static final String DAILY_FILE_NAME = "dailyFile";
    public static final String DAILY_FILE_LABEL = "Daily File";
    public static final String OCCUPIED_NAME = "isOccupied";
    public static final String OCCUPIED_LABEL = "Is Occupied";
    public static final String OCCUPANCY_START_TIME_NAME = "occupancyStartTime";
    public static final String ALARM_NAME = "isAlarming";
    public static final String ALARM_LABEL = "Is Alarming";
    static Time samplingTime = radHelper.createPrecisionTimeStamp();
    static Boolean occupied = radHelper.createBoolean()
            .name(OCCUPIED_NAME)
            .label(OCCUPIED_LABEL)
            .definition(RADHelper.getRadUri("RS350OccupancyStatus"))
            .value(false)
            .build();
    static Time occupancyStartTime = radHelper.createTime()
            .asPhenomenonTimeIsoUTC()
            .name(OCCUPANCY_START_TIME_NAME)
            .label("Occupancy Start Time")
            .definition(RADHelper.getRadUri("OccupancyStartTime"))
            .build();
    static Boolean alarming = radHelper.createBoolean()
            .name(ALARM_NAME)
            .label(ALARM_LABEL)
            .value(false)
            .build();

    final static DataRecord dataStruct = radHelper.createRecord()
            .name(DAILY_FILE_NAME)
            .label(DAILY_FILE_LABEL)
            .updatable(true)
            .definition(RADHelper.getRadUri("DailyFile"))
            .addField(samplingTime.getName(), samplingTime)
            .addField(occupied.getName(), occupied)
            .addField(occupancyStartTime.getName(), occupancyStartTime)
            .addField(alarming.getName(), alarming)
            .build();

    final static DataEncoding dataEncoding = new TextEncodingImpl(",", "\n");

    public static DataComponent getRecordDescription() {
        return dataStruct.copy();
    }

    public static DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }
}
