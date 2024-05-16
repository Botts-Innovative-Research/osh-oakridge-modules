/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

import org.checkerframework.checker.units.qual.A;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.sensor.*;
import org.sensorhub.api.service.ServiceConfig;

import java.util.*;
import java.util.function.Consumer;

public class ZwaveCommServiceConfig extends ServiceConfig {


    @DisplayInfo(label = "Port", desc = "USB Port name/number connected to zwave controller")
    public String portName = "COM7";

    @DisplayInfo(label = "Baud Rate", desc = "Baud rate")
    public int baudRate = 115200;

    @DisplayInfo(desc="Controller Location")
    public PositionConfig positionConfig = new PositionConfig();

    public PositionConfig.LLALocation getLocation(){return positionConfig.location;}

    @DisplayInfo(desc = "ZController ID value")
    public int controllerID = 1;


    public class AdminPanelNodeList {

        public void setCommSubscribers(Collection<ZWaveNode> nodeList) {

            List<String> strNodeList = new ArrayList<>();
            Iterator iterator = nodeList.iterator();

            while (iterator.hasNext()){
                strNodeList.add(iterator.next().toString());
            }

            this.commSubscribers = strNodeList;
            }

            @DisplayInfo
            public List<String> commSubscribers = new ArrayList<>();
        }

        @DisplayInfo(desc = "Sensor Drivers Subscribed to ZWaveCommService")
        public AdminPanelNodeList adminPanelNodeList = new AdminPanelNodeList();


    public class ZW100SensorDriverConfigurations extends SensorDriverConfig {
        @DisplayInfo(desc = "Node ID value")
        public int nodeID = 26;
        @DisplayInfo(desc = "ZController ID value")
        public int controllerID = 1;
        @DisplayInfo (desc = "Wake Up Interval of Sensor in Seconds")
        public int wakeUpTime = 240;
        @DisplayInfo (desc = "Which command would be sent when the motion sensor triggered." +
                "1 = send Basic Set CC." +
                "2 = send Sensor Binary Report CC.")
        public int motionCommand = 1;
        @DisplayInfo (desc = "Sensitivity of Motion Sensor; Min: 1 - Max: 5")
        public int motionSensitivity = 5;
        @DisplayInfo (desc = "Motion Sensor Reset Timeout in Seconds")
        public int motionSensorReset = 10;
        @DisplayInfo (desc = "Sensor Report Interval in Seconds; Min: 240 on battery")
        public int sensorReport = 240;
        @DisplayInfo (desc = "Temperature Unit; 1 = Celcius, 2 = Farenheit")
        public int temperatureUnit = 2;
        @DisplayInfo (desc = "Enable selective reporting only when measurements reach a certain threshold or percentage set")
        public int selectiveReporting = 1;
        @DisplayInfo (desc = "Temperature Threshold: value contains one decimal point, e.g. if the value is set to " +
                "20, the threshold value =2.0°F")
        public int temperatureThreshold = 2;
        @DisplayInfo (desc = "Humidity Threshold: Unit in %")
        public int humidityThreshold = 2;
        @DisplayInfo (desc = "Luminance Threshold")
        public int luminanceThreshold = 2;
        @DisplayInfo (desc = "Battery Threshold: The unit is %")
        public int batteryThreshold = 2;
        @DisplayInfo (desc = "UV Threshold")
        public int UVThreshold = 2;

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