/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.
 ******************************************************************************/

package com.botts.impl.system.lane.mobile;

import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.D5RPMConfig;
import com.botts.impl.system.lane.config.LaneOptionsConfig;
import com.botts.impl.system.lane.config.RS350RPMConfig;
import com.botts.impl.system.lane.mobile.config.MobileDetectorConfig;
import org.sensorhub.api.common.SensorHubException;

/**
 * Lane system specialization for mobile radiation detectors (RS-350 backpack,
 * Kromek D5 walker). Shares the LaneSystem runtime and system UID scheme
 * (urn:osh:system:lane:*) so the OSCAR viewer treats it as a lane, but is
 * configured through a single detector connection: {@link MobileDetectorConfig#detectorConfig}
 * is mapped onto the inherited lane RPM config before init, and mobile-only
 * constraints are enforced (no cameras, RS-350/D5 detector types only).
 *
 * @since July 2026
 */
public class MobileDetectorSystem extends LaneSystem {

    @Override
    protected void doInit() throws SensorHubException {
        var config = getConfiguration();

        if (config.detectorConfig != null) {
            if (config.laneOptionsConfig == null)
                config.laneOptionsConfig = new LaneOptionsConfig();
            config.laneOptionsConfig.rpmConfig = config.detectorConfig;
            // mobile units carry no lane cameras
            config.laneOptionsConfig.ffmpegConfig = null;
        } else if (config.laneOptionsConfig != null && config.laneOptionsConfig.rpmConfig != null) {
            // module migrated from a plain lane: adopt its RPM config as the detector connection
            config.detectorConfig = config.laneOptionsConfig.rpmConfig;
        }

        if (config.detectorConfig != null
                && !(config.detectorConfig instanceof RS350RPMConfig || config.detectorConfig instanceof D5RPMConfig))
            throw new SensorHubException("Mobile Detector System supports RS-350 or Kromek D5 detectors only");

        super.doInit();
    }

    @Override
    public MobileDetectorConfig getConfiguration() {
        return (MobileDetectorConfig) this.config;
    }
}
