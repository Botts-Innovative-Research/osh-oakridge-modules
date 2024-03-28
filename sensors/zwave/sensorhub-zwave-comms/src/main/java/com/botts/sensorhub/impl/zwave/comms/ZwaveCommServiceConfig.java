/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.sensor.*;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.sensor.SensorSystemConfig;

public class ZwaveCommServiceConfig extends ServiceConfig {

    @DisplayInfo(label = "Port", desc = "USB Port name/number connected to zwave controller")
    public String portName = "COM5";

    @DisplayInfo(label = "Baud Rate", desc = "Baud rate")
    public int baudRate = 115200;

    @DisplayInfo(desc="Controller Location")
    public PositionConfig positionConfig = new PositionConfig();

    public PositionConfig.LLALocation getLocation(){return positionConfig.location;}

    @DisplayInfo(desc = "ZController ID value")
    public int controllerID = 1;

        public class ZW100SensorDriverConfigurations extends SensorDriverConfig {
            @DisplayInfo(desc = "Node ID value")
            public int nodeID = 26;
            @DisplayInfo(desc = "ZController ID value")
            public int controllerID = 1;
            @DisplayInfo (desc = "Sensitivity of Motion Sensor; Min: 1 - Max: 5")
            public int motionSensitivity = 5;

            @DisplayInfo (desc = "Motion Sensor Reset Timeout in Seconds")
            public int motionSensorReset = 10;

            @DisplayInfo (desc = "Sensor Report Interval in Seconds; Min: 240 on battery")
            public int sensorReport = 240;

            @DisplayInfo (desc = "Temperature Unit; 1 = Celcius, 2 = Farenheit")
            public int temperatureUnit = 2;

            @DisplayInfo (desc = "Wake Up Interval of Sensor in Seconds")
            public int wakeUpTime = 240;
    }

    @DisplayInfo(label = "ZW100 Config")
    public ZW100SensorDriverConfigurations zw100SensorDriverConfigurations = new ZW100SensorDriverConfigurations();

        public class WADWAZSensorDriverConfigurations extends SensorDriverConfig {
            @DisplayInfo(desc = "Node ID value")
            public int nodeID = 13;
            @DisplayInfo(desc = "ZController ID value")
            public int controllerID = 1;
            @DisplayInfo (desc = "Wake Up Interval of Sensor in Seconds; Min: 600")
            public int wakeUpTime = 600;

        }

    @DisplayInfo(label = "WADWAZ Config")
    public WADWAZSensorDriverConfigurations wadwazSensorDriverConfigurations = new WADWAZSensorDriverConfigurations();

        public class WAPIRZSensorDriverConfigurations extends SensorDriverConfig {
            @DisplayInfo(desc = "Node ID value")
            public int nodeID = 21;
            @DisplayInfo(desc = "ZController ID value")
            public int controllerID = 1;
            @DisplayInfo(desc = "After motion is detected sensor cannot be re-triggered by motion again for " +
                    "determined times; time in min")
            public int reTriggerWait = 1;
            @DisplayInfo(desc = "Wake Up Interval of Sensor in Seconds; Min: 600")
            public int wakeUpTime = 600;

        }
    @DisplayInfo(label = "WAPIRZ Config")
    public WAPIRZSensorDriverConfigurations wapirzSensorDriverConfigurations = new WAPIRZSensorDriverConfigurations();
}