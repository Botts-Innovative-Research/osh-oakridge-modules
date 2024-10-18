/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.ds100;

import com.botts.sensorhub.impl.zwave.comms.ZwaveCommServiceConfig;
import org.checkerframework.framework.qual.DefaultQualifierForUse;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.sensor.SensorDriverConfig;


/**
 * Configuration settings for the {@link DS100Sensor} driver exposed via the OpenSensorHub Admin panel.
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
public class DS100Config extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "sensor001";

    @DisplayInfo(desc = "Communication settings to connect to data stream")
    public CommProviderConfig<?> commSettings;

    @DisplayInfo(desc="DS100 Location")
    public PositionConfig positionConfig = new PositionConfig();

    @Override
    public LLALocation getLocation(){return positionConfig.location;}

    public class DS100SensorDriverConfigurations extends SensorDriverConfig {
        @DisplayInfo(desc = "Node ID value")
        public int nodeID = 3;
        @DisplayInfo(desc = "ReInitialize the node: only set true on first run after adding including the device in " +
                "the zWave network")
        public boolean reInitNode = false;
        @DisplayInfo(desc = "ZController ID value")
        public int controllerID = 1;

        @DisplayInfo (desc = "Enable or disable BASIC SET reports when motion is triggered for Group 2 of associated devices" +
                "0 - disabled, " +
                "1 - enabled")
        public int basicSetReports = 1;

        @DisplayInfo (desc = "Reverse values sent in BASIC SET reports when motion is triggered for Group 2 of " +
                "associated devices " +
                "0 - BASIC SET sends 255 when motion is triggered, BASIC SET sends 0 when motion times out " +
                "1 - BASIC SET sends 0 when motion is triggered, BASIC SET sends 255 when motion times out.")
        public int reverseBasicSetReports = 0;

        @DisplayInfo (desc = "Low Battery Alert: percent battery left " +
                " range 10 to 50 may be set. (set in percentages)")
        public int lowBatteryAlert = 10;

        @DisplayInfo(desc = "Wake Up Interval of Sensor in Seconds; Min: 600")
        public int wakeUpTime = 600;

    }
    @DisplayInfo(label = "DS100 Config")
    public DS100SensorDriverConfigurations ds100SensorDriverConfigurations =
            new DS100SensorDriverConfigurations();
}

