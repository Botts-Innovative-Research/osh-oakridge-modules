/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.TCPCommProviderConfig;

public class RS350Config extends SensorConfig {

    @DisplayInfo.Required
    public String serialNumber;

    @DisplayInfo(desc = "Communication settings to connect to RS-350 data stream")
    public TCPCommProviderConfig commSettings;

    @DisplayInfo.Required
    @DisplayInfo(label = "Outputs", desc = "Configuration options for source data outputs from driver")
    public RS350Outputs outputs = new RS350Outputs();
}