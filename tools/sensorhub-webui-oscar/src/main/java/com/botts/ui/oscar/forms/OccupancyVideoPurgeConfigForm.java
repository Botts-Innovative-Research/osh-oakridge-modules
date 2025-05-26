package com.botts.ui.oscar.forms;

import com.botts.database.oscar.OccupancyVideoPurgeConfig;
import org.sensorhub.ui.SystemDriverDatabaseConfigForm;
import org.sensorhub.ui.data.BaseProperty;

import java.util.Map;

public class OccupancyVideoPurgeConfigForm extends SystemDriverDatabaseConfigForm {

    @Override
    public Map<String, Class<?>> getPossibleTypes(String propId, BaseProperty<?> prop)
    {
        Map<String, Class<?>> possibleTypes = super.getPossibleTypes(propId, prop);

        if (propId.equals(SystemDriverDatabaseConfigForm.PROP_AUTOPURGE))
        {
            possibleTypes.put("Auto Purge Non-Occupancy Video", OccupancyVideoPurgeConfig.class);
        }

        return possibleTypes;
    }
}
