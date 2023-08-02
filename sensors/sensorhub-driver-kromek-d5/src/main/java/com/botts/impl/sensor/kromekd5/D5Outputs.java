/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.kromekd5;

import org.sensorhub.api.config.DisplayInfo;

public class D5Outputs {
    @DisplayInfo(label = "Location", desc = "GPS Location of the Sensor")
    public boolean enableLocationOutput = true;

    @DisplayInfo(label = "Foreground", desc = "Foreground spectrum data")
    public boolean enableForegroundOutput = true;

    @DisplayInfo(label = "Background", desc = "Background spectrum data")
    public boolean enableBackgroundOutput = true;

    @DisplayInfo(label = "Analysis", desc = "Contains analytically derived data. Includes some Alarm data")
    public boolean enableAnalysisData = false;

}

