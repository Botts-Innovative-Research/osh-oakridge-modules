/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.sensorhub.impl.zwave.comms;

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

    @DisplayInfo(label = "Is master controller")
    public boolean masterController = true;

    @DisplayInfo(desc="Controller Location")
    public PositionConfig positionConfig = new PositionConfig();

    public PositionConfig.LLALocation getLocation(){return positionConfig.location;}

    @DisplayInfo(desc = "ZController ID value")
    public int controllerID = 1;


    public class NodeList {

        public void setCommSubscribers(Collection<ZWaveNode> nodeList) {

            List<String> strNodeList = new ArrayList<>();
            Iterator iterator = nodeList.iterator();


            while (iterator.hasNext()){
                ZWaveNode zWaveNode = (ZWaveNode)iterator.next();

                String nodeIdNumber = String.valueOf(zWaveNode.getNodeId());
                int sensorId = zWaveNode.getDeviceType();
                String sensorName = "";

                switch (sensorId){
                    case 8193: sensorName = "WADWAZ";
                    break;
                    case 8194: sensorName = "WAPIRZ";
                    break;
                    case 258: sensorName = "ZW100";
                    break;
                    default: sensorName = "Unknown";
                }

                strNodeList.add(sensorName + " NodeID: " + nodeIdNumber);
            }

            this.commSubscribers = strNodeList;
        }

        @DisplayInfo
            public List<String> commSubscribers = new ArrayList<>();

    }
    @DisplayInfo(desc = "Sensor Drivers Subscribed to ZWaveCommService")
    public NodeList nodeList = new NodeList();

}