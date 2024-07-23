/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rapiscan;

import com.google.gson.annotations.SerializedName;
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the [NAME] driver exposed via the OpenSensorHub Admin panel.
 *
 * Configuration settings take the form of
 * <code>
 *     DisplayInfo(desc="Description of configuration field to show in UI")
 *     public Type configOption;
 * </code>
 *
 * Containing an annotation describing the setting and if applicable its range of values
 * as well as a public access variable of the given Type
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class RapiscanConfig extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "rpm001";

    @DisplayInfo(desc = "Communication settings to connect to RS-350 data stream")
    public CommProviderConfig<?> commSettings;

    @DisplayInfo(desc="RPM Location")
    public PositionConfig positionConfig = new PositionConfig();

    @DisplayInfo(label = "Lane Name", desc = "Human readable name of the lane")
    @DisplayInfo.Required
    public String laneName;

    @DisplayInfo(label = "Lane ID", desc = "ID of lane")
    @DisplayInfo.Required
    public int laneID;

    @DisplayInfo(desc = "EML Lane Settings")
    public EMLConfig EMLConfig = new EMLConfig();


    @Override
    public LLALocation getLocation(){return positionConfig.location;}

    @Override
    public EulerOrientation getOrientation(){ return positionConfig.orientation;}


}