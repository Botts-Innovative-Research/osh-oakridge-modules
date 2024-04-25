/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.dsb45;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorDriverConfig;

/**
 * Configuration settings for the {@link DSB45Sensor} driver exposed via the OpenSensorHub Admin panel.
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
 * @author your_name
 * @since date
 */
public class DSB45Config extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "sensor001";


    //TODO: Example of creating and implementing a class in the sensor driver to make configurations manageable from
    // the OSH admin panel (this can also be done in the ZWaveCommServiceConfig)

    public class DSB45SensorDriverConfigurations extends SensorDriverConfig {

        //reference: https://github.com/openhab/org.openhab.binding.zwave/blob/main/doc/aeon/dsb45_0_0.md
        @DisplayInfo(desc = "Node ID value")
        public int nodeID = 32;
        @DisplayInfo(desc = "ZController ID value")
        public int controllerID = 1;

        @DisplayInfo (desc = "Wake Up Interval of Sensor in Seconds")
        public int wakeUpTime = 240;
        @DisplayInfo(desc = "Toggle the sensor binary report value:" +
                "default value is 0 (Open: 00, Close: FF)" +
                "1 (Open: FF, Close: 00).")
        public int sensorBinaryReport = 0;

        @DisplayInfo(desc = "Enable wake up 10 minutes when the power is switched on:" +
                "0 = disable," +
                "1 = enable")
        public int intervalWakeUpTime = 0;

        @DisplayInfo (desc = "Toggle the basic set value when the Magnet switch is opened /closed:" +
                "default value is 0 (Open: 00, Close: FF)" +
                "1 (Open: FF, Close: 00)")
        public int basicSet = 0;

        @DisplayInfo(desc = "Reports that will be sent:" +
                "0 Do not send anything" +
                "1 Send battery report" +
                "16 Send Sensor Binary report" +
                "17 Send Sensor Binary and Battery reports")
        public int sensorReport = 0;

        @DisplayInfo(desc = "Value Description:" +
                "0 Do not send anything" +
                "1 Send Basic Set" +
                "16 Send ALARM" +
                "17 Send Basic Set and ALARM")
        public int valueDescription = 0;

    }

    @DisplayInfo(label = "DSB45 Config")
    public DSB45SensorDriverConfigurations dsb45SensorDriverConfigurations =
            new DSB45SensorDriverConfigurations();
}
