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

package com.botts.database.oscar;

import org.sensorhub.api.database.IObsSystemDbAutoPurgePolicy;
import org.sensorhub.impl.database.system.HistoricalObsAutoPurgeConfig;

/**
 * @author Alex Almanza
 * @since May 25, 2025
 */
public class OccupancyVideoPurgeConfig extends HistoricalObsAutoPurgeConfig {
    @Override
    public IObsSystemDbAutoPurgePolicy getPolicy() { return new OccupancyVideoPurgePolicy(this); }
}
