package com.botts.impl.system.database;

import org.sensorhub.api.database.IObsSystemDbAutoPurgePolicy;
import org.sensorhub.impl.database.system.HistoricalObsAutoPurgeConfig;


/**
 *
 * Configuration for automatic database cleanup based on RPM alarming occupancies and record age.
 *
 * @author Kalyn Stricklin
 * @since May 26, 2025
 */
public class OccupancyVideoPurgePolicyConfig extends HistoricalObsAutoPurgeConfig {


    @Override
    public IObsSystemDbAutoPurgePolicy getPolicy() {
        return new OccupancyVideoPurgePolicy(this);
    }
}
