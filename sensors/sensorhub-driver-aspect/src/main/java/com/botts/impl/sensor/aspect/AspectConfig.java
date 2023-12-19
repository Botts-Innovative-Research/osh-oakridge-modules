/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.aspect;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the Aspect driver exposed via the OpenSensorHub Admin panel.
 *
 * @author Michael Elmore
 * @since December 2023
 */
public class AspectConfig extends SensorConfig {
    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "sensor001";

    @DisplayInfo(desc = "Communication settings to connect to the data stream")
    public CommProviderConfig<?> commSettings;

    @DisplayInfo(desc = "RPM Location")
    public PositionConfig positionConfig = new PositionConfig();

    @Override
    public LLALocation getLocation() {
        return positionConfig.location;
    }

    @Override
    public EulerOrientation getOrientation() {
        return positionConfig.orientation;
    }
}