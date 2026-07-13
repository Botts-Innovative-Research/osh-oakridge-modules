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

package com.botts.impl.system.lane.mobile.config;

import com.botts.impl.system.lane.config.LaneConfig;
import com.botts.impl.system.lane.config.RPMConfig;
import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration for a mobile radiation detector (RS-350 backpack or Kromek D5)
 * deployed as a trackable system. Extends the lane config so the runtime is
 * shared, but exposes a single detector connection instead of the fixed-portal
 * options.
 *
 * @since July 2026
 */
public class MobileDetectorConfig extends LaneConfig {

    @DisplayInfo(label = "Detector Connection", desc = "Detector type (RS-350 or Kromek D5) and its TCP endpoint. " +
            "This is the single place to configure the detector's address: changes are pushed down to the detector " +
            "driver every time the module is (re)initialized.")
    public RPMConfig detectorConfig;
}
