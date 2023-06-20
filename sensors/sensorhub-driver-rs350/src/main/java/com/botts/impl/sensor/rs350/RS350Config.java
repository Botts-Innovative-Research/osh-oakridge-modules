/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

public class D3sConfig extends SensorConfig{

    @DisplayInfo.Required
    public String serialNumber;


    @DisplayInfo(desc="Communication settings to connect to Kromek D3s data stream")
    public CommProviderConfig<?> commSettings;
}