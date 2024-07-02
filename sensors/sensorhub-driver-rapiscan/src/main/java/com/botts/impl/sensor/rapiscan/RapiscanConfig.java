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

import java.util.ArrayList;
import java.util.List;

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

//    @DisplayInfo.Required
//    @DisplayInfo(desc = "switching config layers")
//    public RapiscanLayerConfig rapiscanLayerConfigs = new RapiscanLayerConfig();

    @DisplayInfo(desc = "Communication settings to connect to RS-350 data stream")
    public CommProviderConfig<?> commSettings;

    @DisplayInfo(desc="RPM Location")
    public PositionConfig positionConfig = new PositionConfig();

//    @DisplayInfo(desc="Gamma Count")
//    public int gammaCount;
//
//    @DisplayInfo(desc="Neutron Count")
//    public int neutronCount;

    @DisplayInfo(desc="Lane Name")
    public String laneName;

    @DisplayInfo(label="Supplemental Algorithm", desc="Check if the lane is EML VM250. For all lanes not designated to be EML lanes do NOT check this box.")
    @SerializedName(value="supplementalAlgorithm", alternate={"emlEnabled"})
    public boolean isSupplementalAlgorithm = false;

    @Override
    public LLALocation getLocation(){return positionConfig.location;}

    @Override
    public EulerOrientation getOrientation(){ return positionConfig.orientation;}


}