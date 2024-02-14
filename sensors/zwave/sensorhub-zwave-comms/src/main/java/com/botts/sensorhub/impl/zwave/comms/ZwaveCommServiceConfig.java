/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.service.ServiceConfig;

public class ZwaveCommServiceConfig extends ServiceConfig {


    @DisplayInfo(label = "Port", desc = "USB Port name/number connected to zwave controller")
    public String portName = "COM5";

    @DisplayInfo(label = "Baud Rate", desc = "Port on which to receive cross domain messages")
    public int baudRate = 115200;

}