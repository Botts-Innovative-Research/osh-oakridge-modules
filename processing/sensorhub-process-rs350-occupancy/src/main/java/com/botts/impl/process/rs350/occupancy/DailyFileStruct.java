package com.botts.impl.process.rs350.occupancy;

import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

public class DailyFileStruct {

    final static RADHelper radHelper = new RADHelper();
    public static final String DAILY_FILE_NAME = "dailyFile";
    public static final String DAILY_FILE_LABEL = "Daily File";
    static Time samplingTime = radHelper.createPrecisionTimeStamp();
    static Boolean rpmMsg = radHelper.createBoolean().name("isAlarming").label("Is Alarming").value(false).build();

    final static DataRecord dataStruct = radHelper.createRecord()
            .name(DAILY_FILE_NAME)
                .label(DAILY_FILE_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("DailyFile"))
            .addField(samplingTime.getName(), samplingTime)
            .addField(rpmMsg.getName(), rpmMsg)
            .build();

    final static DataEncoding dataEncoding = new TextEncodingImpl(",", "\n");;

    public static DataComponent getRecordDescription() {
        return dataStruct.copy();
    }

    public static DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }
}
