/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.wadwaz1;

import com.botts.sensorhub.impl.zwave.comms.ZwaveCommServiceConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.comm.CommProviderConfig;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import org.sensorhub.api.sensor.SensorDriverConfig;


/**
 * Configuration settings for the {@link WADWAZ1Sensor} driver exposed via the OpenSensorHub Admin panel.
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
 * @since 11/15/23
 */
public class WADWAZ1Config extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "sensor001";

    @DisplayInfo(desc="WADWAZ1 Location")
    public PositionConfig positionConfig = new PositionConfig();



    //    @Override
//    public LLALocation getLocation(){return positionConfig.location;}

    public class WADWAZSensorDriverConfigurations extends SensorDriverConfig {
        @DisplayInfo(desc = "Node ID value")
        public int nodeID = 99;
        @DisplayInfo(desc = "ReInitialize the node: only set true on first run after adding including the device in " +
                "the zWave network")
        public boolean reInitNode = false;
        @DisplayInfo(desc = "ZController ID value")
        public int controllerID = 1;
        @DisplayInfo (desc = "Wake Up Interval of Sensor in Seconds; minInterval = 600; intervals must set in 200 second increments")
        public int wakeUpTime = 600;
    }
    @DisplayInfo(label = "WADWAZ Config")
    public WADWAZSensorDriverConfigurations wadwazSensorDriverConfigurations = new WADWAZSensorDriverConfigurations();

}
