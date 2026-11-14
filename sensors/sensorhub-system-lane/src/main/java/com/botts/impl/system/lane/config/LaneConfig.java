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

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration settings for the Lane Sensor System.
 *
 * @author Alex Almanza
 * @since March 2025
 */
public class LaneConfig extends SensorSystemConfig {

    public static final int DEFAULT_EXTENDED_OCCUPANCY_THRESHOLD_MINUTES = 1;
    public static final int MIN_EXTENDED_OCCUPANCY_THRESHOLD_MINUTES = 1;
    public static final int MAX_EXTENDED_OCCUPANCY_THRESHOLD_MINUTES = 1440;

    @DisplayInfo(desc = "Operational view keys that may display this lane. Use lowercase letters, numbers, and hyphens only.")
    public List<String> operationalViewKeys = new ArrayList<>();

    @DisplayInfo(label = "Extended Occupancy Threshold (min)", desc = "Duration in minutes (1-1440) before an active occupancy is logged as an extended occupancy fault (default: 1 minute).")
    public int extendedOccupancyThresholdMinutes = DEFAULT_EXTENDED_OCCUPANCY_THRESHOLD_MINUTES;

    @DisplayInfo(desc = "Additional options when creating an RPM lane.")
    public LaneOptionsConfig laneOptionsConfig;

    @DisplayInfo(label = "Vehicle OCR", desc = "Optional asynchronous OCR of container numbers and license plates from lane camera recordings. Disabled by default.")
    public OcrConfig ocrConfig = new OcrConfig();

    @DisplayInfo(label = "Delete Data on Lane Removal", desc = "Select this to automatically delete all records of this lane when the lane is deleted.")
    public boolean autoDelete = true;
}
