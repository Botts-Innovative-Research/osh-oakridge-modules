/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.system.lane;


import com.botts.impl.sensor.aspect.AspectConfig;
import com.botts.impl.sensor.aspect.AspectDescriptor;
import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.rapiscan.RapiscanConfig;
import com.botts.impl.sensor.rapiscan.RapiscanDescriptor;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.config.LaneConfig;
import com.botts.impl.system.lane.config.RPMType;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabaseModule;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.system.ISystemDriverDatabase;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.sensor.SensorSystemConfig;
import org.sensorhub.utils.MsgUtils;

import java.util.UUID;

/**
 * Extended functionality of the SensorSystem class unique for OpenSource Central Alarm (OSCAR)
 *
 * @author Alex Almanza
 * @since March 2025
 */
public class LaneSystem extends SensorSystem {

    private static final String URN_PREFIX = "urn:";
    AbstractSensorModule<?> rpmModule = null;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // generate unique ID
        if (config.uniqueID != null && !config.uniqueID.equals(AUTO_ID)) {
            if (config.uniqueID.startsWith(URN_PREFIX)) {
                this.uniqueID = config.uniqueID;
                String suffix = config.uniqueID.replace(URN_PREFIX, "");
                generateXmlID(DEFAULT_XMLID_PREFIX, suffix);
            } else {
                this.uniqueID = URN_PREFIX + "osh:system:lane:" + config.uniqueID;
                generateXmlID(DEFAULT_XMLID_PREFIX, config.uniqueID);
            }
        }

        // Ensure name is at most 12 characters
        if (config.name.length() > 12) {
            reportError("Lane name must be 12 or less characters", new IllegalArgumentException("Module name must be 12 or less characters"));
        }

        // If we already have RPM submodules, don't set up new ones
        boolean containsRpm = false;
        for (var member : config.subsystems) {
            if (member.config instanceof RapiscanConfig || member.config instanceof AspectConfig) {
                containsRpm = true;
                break;
            }
        }
        // Check state members too in case config hasn't been updated
        for (var member : getMembers().values()) {
            if(containsRpm)
                break;
            if (member instanceof RapiscanSensor || member instanceof AspectSensor) {
                rpmModule = (AbstractSensorModule<?>) member;
                containsRpm = true;
                break;
            }
        }

        // If RPM exists, ensure that database & process are set up with correct UIDs
        if (containsRpm) {

        }
        // If RPM does not exist, and RPM is specified in config, then set up submodule
        else {
            // RPM type and unique ID must be specified
            if (getLaneConfig().rpmType != null && getLaneConfig().rpmUniqueId != null && !getLaneConfig().rpmUniqueId.isBlank()) {
                // Create Rapiscan or Aspect config, then add as submodule
                var config = createRPMConfig(getLaneConfig().rpmType);
                var newMember = new SensorSystemConfig.SystemMember();
                newMember.config = config;
                rpmModule = (AbstractSensorModule<?>) addSubsystem(newMember);
                containsRpm = true;
            }


        }

        // Lane database and process setup
        if (getLaneConfig().laneDataConfig != null) {
            // Attach this system to database
            String laneDbId = getLaneConfig().laneDataConfig.laneDatabaseConfig.laneDatabaseId;
            if (!laneDbId.isBlank()) {
                // Start thread that waits for this system to be initialized before adding it to database
                // TODO: Add config listener to remove old system from config if system uid is updated
                // TODO: Add config listener to swap to new database if lane database is changed
                // TODO: Add listener to remove system from database if system is deleted
                new Thread(() -> {
                    try {
                        waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
                        if (getParentHub().getModuleRegistry().getModuleById(laneDbId) instanceof SystemDriverDatabase)
                            ((SystemDriverDatabase) getParentHub().getModuleRegistry().getModuleById(laneDbId)).getConfiguration().systemUIDs.add(this.getUniqueIdentifier());
                    } catch (SensorHubException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }

        }

    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

//        var submoduleTest = getMembers().get("");
//        submoduleTest.registerListener(submoduleAddedListener);
    }

    private SensorConfig createRPMConfig(RPMType rpmType) {
        // TODO: Make Rapiscan and Aspect drivers use common class/interface and common configs
        SensorConfig config;
        // Create Aspect config
        if (rpmType == RPMType.ASPECT) {
            AspectConfig aspectConfig = new AspectConfig();
            aspectConfig.serialNumber = getLaneConfig().rpmUniqueId;
            aspectConfig.moduleClass = AspectSensor.class.getCanonicalName();
            config = aspectConfig;
        }
        // Create Rapiscan config
        else if (rpmType == RPMType.RAPISCAN) {
            RapiscanConfig rapiscanConfig = new RapiscanConfig();
            rapiscanConfig.serialNumber = getLaneConfig().rpmUniqueId;
            rapiscanConfig.moduleClass = RapiscanSensor.class.getCanonicalName();
            config = rapiscanConfig;
        } else return null;

        // Use label from config if available
        if (!getLaneConfig().rpmLabel.isBlank())
            config.name = getLaneConfig().rpmLabel;

        return config;
    }

    public LaneConfig getLaneConfig() {
        return (LaneConfig) this.config;
    }

}