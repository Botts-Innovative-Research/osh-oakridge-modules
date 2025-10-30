/*******************************************************************************

  The contents of this file are subject to the Mozilla Public License, v. 2.0.
  If a copy of the MPL was not distributed with this file, You can obtain one
  at http://mozilla.org/MPL/2.0/.

  Software distributed under the License is distributed on an "AS IS" basis,
  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  for the specific language governing rights and limitations under the License.

  The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
  Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.sensor.SensorSystemConfig;

/**
 * Configuration settings for the Lane Sensor System.
 *
 * @author Alex Almanza
 * @since March 2025
 */
public class LaneConfig extends SensorSystemConfig {

    @DisplayInfo(desc = "Additional options when creating an RPM lane.")
    public LaneOptionsConfig laneOptionsConfig;

    @DisplayInfo(label = "Delete Data on Lane Removal", desc = "Select this to automatically delete all records of this lane when the lane is deleted.")
    public boolean autoDelete = true;
}