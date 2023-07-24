/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import org.sensorhub.api.config.DisplayInfo;

public class RS350Outputs {
    @DisplayInfo(label = "Location", desc = "GPS Location of the Sensor")
    public boolean enableLocationOutput = true;

    @DisplayInfo(label = "Status", desc = "Information on the status of the RS350 (Battery %, Scan Mode, etc)")
    public boolean enableStatusOutput = true;

    @DisplayInfo(label = "Background", desc = "Background spectrum data")
    public boolean enableBackgroundOutput = true;

    @DisplayInfo(label = "Foreground", desc = "Foreground spectrum data")
    public boolean enableForegroundOutput = true;

    @DisplayInfo(label = "Derived Data", desc = "Contains analytically derived data. Includes some Alarm data")
    public boolean enableDerivedData = false;

    @DisplayInfo(label = "Alarm", desc = "Includes Derived Data and Alarm Data")
    public boolean enableAlarmOutput = true;
}

