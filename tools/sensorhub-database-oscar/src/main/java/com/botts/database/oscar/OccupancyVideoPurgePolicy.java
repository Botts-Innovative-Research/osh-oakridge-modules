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

import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.database.IObsSystemDbAutoPurgePolicy;
import org.sensorhub.api.datastore.TemporalFilter;
import org.slf4j.Logger;

import java.util.Collection;

/**
 * Implementation of purge policy to remove unneeded video data after an RPM occupancy occurs
 *
 * @author Alex Almanza
 * @since May 25, 2025
 */
public class OccupancyVideoPurgePolicy implements IObsSystemDbAutoPurgePolicy {

    OccupancyVideoPurgeConfig config;

    OccupancyVideoPurgePolicy(OccupancyVideoPurgeConfig config) { this.config = config; }

    @Override
    public void trimStorage(IObsSystemDatabase db, Logger log, Collection<String> systemUIDs) {
        // TODO: Remove all video data that doesn't have an associated occupancy time frame

        // It's possible we should extend the max record age purge policy as well, to purge video that has been in the db too long; imagine no occupancy for a while, we need to purge in between occupancies, as well as by a max age

        // Loop through each RPM system in database
        // Check which time periods contain occupancies
        // Purge video data in between each occupancy

        // We don't really care that much about the "systemUIDs" because we can just check for lanes in the database,
        // use their RPM to check time periods between occupancies,
        // and purge their video data streams if available
    }
}