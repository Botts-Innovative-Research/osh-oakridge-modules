/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.zse18;

import com.botts.sensorhub.impl.zwave.comms.ZwaveCommServiceConfig;
import org.checkerframework.framework.qual.DefaultQualifierForUse;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.sensor.SensorDriverConfig;


/**
 * Configuration settings for the {@link ZSE18Sensor} driver exposed via the OpenSensorHub Admin panel.
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
 * @since 09/09/24
 */
public class ZSE18Config extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "sensor001";

    @DisplayInfo(desc = "Communication settings to connect to data stream")
    public CommProviderConfig<?> commSettings;

    @DisplayInfo(desc="ZSE18 Location")
    public PositionConfig positionConfig = new PositionConfig();

    @Override
    public LLALocation getLocation(){return positionConfig.location;}

    public class ZSE18SensorDriverConfigurations extends SensorDriverConfig {
        @DisplayInfo(desc = "Node ID value")
        public int nodeID = 5;
        @DisplayInfo(desc = "ReInitialize the node: only set true on first run after adding including the device in " +
                "the zWave network")
        public boolean reInitNode = false;
        @DisplayInfo(desc = "ZController ID value")
        public int controllerID = 1;
        @DisplayInfo(desc = "PIR sensor sensitivity - Range: 1-8 (low-high)")
        public int pirSensitivity = 8;
        @DisplayInfo (desc = "Enable or disable BASIC SET reports when motion is triggered for Group 2 of associated devices" +
                "0 - disabled, " +
                "1 - enabled")
        public int basicSetReports = 1;
        @DisplayInfo (desc = "Reverse values sent in BASIC SET reports when motion is triggered for Group 2 of " +
                "associated devices " +
                "0 - BASIC SET sends 255 when motion is triggered, BASIC SET sends 0 when motion times out " +
                "1 - BASIC SET sends 0 when motion is triggered, BASIC SET sends 255 when motion times out.")
        public int reverseBasicSetReports = 0;

        @DisplayInfo (desc = "Enable or disable vibration sensor" +
                "0 - disabled" +
                "1 - enabled")
        public int enableVibrationSensor = 1;

        @DisplayInfo(desc = "Set trigger interval: e.g 3=6 seconds (add 3 seconds to value set)" +
                "the time when motion is reported again after initial trigger." +
                "NOTE: Small interval will increase activity and decrease battery life. " +
                "Values in the range 3 to 65535 may be set." +
                "The manufacturer defined default value is 27.")
        public int triggerInterval = 27;

        @DisplayInfo (desc = "Notification Alarm or Binary Reports:" +
                "Enable or disable binary sensor reports when motion is detected. " +
                "0 – don’t send binary sensor reports when motion is detected, send notification reports instead (default); " +
                "1 – send binary sensor reports when motion is detected instead of the notification report.")
        public int binarySensorReports = 0;

        @DisplayInfo (desc = "Enable/Disable LED indicator: " +
                "0 - disabled, " +
                "1 - enabled")
        public int ledIndicator = 1;

        @DisplayInfo (desc = "Low Battery Alert: percent battery left " +
                " range 10 to 50 may be set. (set in percentages)")
        public int lowBatteryAlert = 10;

        @DisplayInfo(desc = "Lock settings: used to lock the current advanced settings and prevent any parameter " +
                "values to be changed until unlocked again" +
                "0 - settings unlocked (default)," +
                "1 - settings locked")
        public int lockSettings = 0;

        @DisplayInfo(desc = "Wake Up Interval of Sensor in Seconds; Min: 600")
        public int wakeUpTime = 600;

    }
    @DisplayInfo(label = "ZSE18 Config")
    public ZSE18SensorDriverConfigurations zse18SensorDriverConfigurations =
            new ZSE18SensorDriverConfigurations();
}
