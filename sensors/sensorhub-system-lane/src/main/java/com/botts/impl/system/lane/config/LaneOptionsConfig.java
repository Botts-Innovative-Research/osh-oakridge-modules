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

import java.util.List;

/**
 * Additional configuration settings for the Lane Sensor System.
 *
 * @author Alex Almanza
 * @since March 2025
 */
public class LaneOptionsConfig {

    @DisplayInfo(label = "Lane Database Config", desc = "Specify automatic database setup when creating this lane")
    public LaneDatabaseConfig laneDatabaseConfig = new LaneDatabaseConfig();

    @DisplayInfo(label = "Auto Create Process", desc = "Automatically create database process for this lane")
    public boolean createProcess = true;

    @DisplayInfo(label = "Initial RPM Config", desc = "Config used for initial RPM setup")
    public RPMConfig rpmConfig;

    @DisplayInfo(label = "Initial Camera Config", desc = "Config used for initial FFmpeg camera setup")
    public List<FFMpegConfig> ffmpegConfig;

}
