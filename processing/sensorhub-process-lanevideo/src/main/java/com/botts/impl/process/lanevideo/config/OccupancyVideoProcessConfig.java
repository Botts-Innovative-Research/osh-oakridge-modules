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

package com.botts.impl.process.lanevideo.config;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.processing.ProcessConfig;

public class OccupancyVideoProcessConfig extends ProcessConfig {

    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "process001";

    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.SYSTEM_UID)
    @DisplayInfo(label = "RPM System (Containing RPM)", desc = "Parent system to read occupancy data from subsystem RPM. If this is blank, the process will attempt to use its parent system's UID.")
    public String systemUID;

    @DisplayInfo.Required
    @DisplayInfo(label = "Recording Settings")
    public RecordingConfig recordingConfig = new RecordingConfig();
}
