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
import org.sensorhub.impl.database.system.SystemDriverDatabase;

/**
 * Configuration settings for the Lane Sensor System's automatic database setup.
 *
 * @author Alex Almanza
 * @since April 2025
 */
public class
LaneDatabaseConfig {

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo.ModuleType(SystemDriverDatabase.class)
    public String laneDatabaseId;

    @DisplayInfo(label = "Auto Purge Video Data", desc = "Select this to automatically add video systems to your lane database's purge policy")
    public boolean autoPurgeVideoData = true;

    @DisplayInfo(label = "Purge Period (in minutes)", desc = "Purges video data every x minutes")
    public int purgePeriodMinutes = 5;

}
