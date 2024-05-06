/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.zw100;

import com.botts.sensorhub.impl.zwave.comms.ZwaveCommServiceConfig;
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorDriverConfig;

/**
 * Configuration settings for the {@link ZW100Sensor} driver exposed via the OpenSensorHub Admin panel.
 * <p>
 * Configuration settings take the form of
 * <code>
 * DisplayInfo(desc="Description of configuration field to show in UI")
 * public Type configOption;
 * </code>
 * <p>
 * Containing an annotation describing the setting and if applicable its range of values
 * as well as a public access variable of the given Type
 *
 * @author cardy
 * @since 11/16/23
 */
public class ZW100Config extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "sensor001";

    @DisplayInfo(desc="ZW100 Location")
    public PositionConfig positionConfig = new PositionConfig();

    @Override
    public LLALocation getLocation(){return positionConfig.location;}

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


//    @DisplayInfo(desc = "Node ID value")
//    public int nodeID = 26;
//
//    @DisplayInfo(desc = "ZController ID value")
//    public int controllerID = 1;
//
//    @DisplayInfo(desc = "ZW100 Configurations")
//    ZwaveCommServiceConfig.ZW100SensorDriverConfigurations sensorDriverConfiguration;

}
