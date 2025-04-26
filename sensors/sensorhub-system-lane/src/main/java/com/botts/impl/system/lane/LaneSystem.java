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


import com.botts.impl.process.occupancy.OccupancyProcessConfig;
import com.botts.impl.process.occupancy.OccupancyProcessModule;
import com.botts.impl.sensor.aspect.AspectConfig;
import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.comm.ModbusTCPCommProviderConfig;
import com.botts.impl.sensor.rapiscan.RapiscanConfig;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.config.LaneConfig;
import com.botts.impl.system.lane.config.RPMConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.TCPCommProviderConfig;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.sensor.SensorSystemConfig;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.utils.MsgUtils;
import org.vast.util.Asserts;

/**
 * Extended functionality of the SensorSystem class unique for Open Source Central Alarm (OSCAR)
 *
 * @author Alex Almanza
 * @since March 2025
 */
public class LaneSystem extends SensorSystem {

    private static final String URN_PREFIX = "urn:";
    AbstractSensorModule<?> existingRPMModule = null;
    AbstractProcessModule<?> existingProcessModule = null;

    @Override
    protected void doInit() throws SensorHubException {
        // Register to receive module events from this module, so we know when it is being deleted
        eventHandler.registerListener(this::handleEvent);
//        getParentHub().getEventBus().newSubscription()
//                .withTopicID(ModuleRegistry.EVENT_GROUP_ID)
//                .subscribe(this::handleEvent);

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
        if (config.name.length() > 12)
            throw new SensorHubException("Lane name must be 12 or less characters", new IllegalArgumentException("Module name must be 12 or less characters"));

//        // If we already have RPM submodules, don't set up new ones
//        for (var member : config.subsystems) {
//            if (member.config instanceof RapiscanConfig || member.config instanceof AspectConfig) {
//                break;
//            }
//        }

        // Check state members too in case config hasn't been updated
        for (var member : getMembers().values()) {
            if (member instanceof RapiscanSensor || member instanceof AspectSensor) {
                existingRPMModule = (AbstractSensorModule<?>) member;
            }
            if (member instanceof OccupancyProcessModule) {
                existingProcessModule = (AbstractProcessModule<?>) member;
            }
        }

        // Variable to signify asynchronous registration
        boolean registeringProcessModule = false;

        // Lane database and process setup
        if (getLaneConfig().laneOptionsConfig != null) {
            // Initial RPM config
            var rpmConfig = getLaneConfig().laneOptionsConfig.rpmConfig;
            if (rpmConfig != null && existingRPMModule == null) {
                // Create Rapiscan or Aspect config, then add as submodule
                var config = createRPMConfig(rpmConfig);
                existingRPMModule = (AbstractSensorModule<?>) registerSubmodule(config);
            }

            // Attach this system to database
            var dbConfig = getLaneConfig().laneOptionsConfig.laneDatabaseConfig;
            if (dbConfig != null && !dbConfig.laneDatabaseId.isBlank()) {
                String laneDbId = getLaneConfig().laneOptionsConfig.laneDatabaseConfig.laneDatabaseId;
                // Start thread that waits for this system to be initialized before adding it to database
                new Thread(() -> {
                    try {
                        // Wait for initialized so that we have a system UID associated with this module
                        waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
                        if (getParentHub().getModuleRegistry().getModuleById(laneDbId) instanceof SystemDriverDatabase dbModule) {
                            var dbModuleConfig = dbModule.getConfiguration();
                            dbModuleConfig.systemUIDs.add(this.getUniqueIdentifier());
                            dbModule.updateConfig(dbModuleConfig);
                        }
                    } catch (SensorHubException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }

            // Process creation / attachment
            if (getLaneConfig().laneOptionsConfig.createProcess && existingProcessModule == null) {
                // Create process config
                OccupancyProcessConfig processConfig = new OccupancyProcessConfig();
                processConfig.moduleClass = OccupancyProcessModule.class.getCanonicalName();
                // This will be unique since serialNumber becomes prefixed. It's just easy to use the lane ID
                processConfig.serialNumber = config.uniqueID;
                processConfig.name = "Database Process";
                processConfig.autoStart = true;
                registeringProcessModule = true;
                // Add process as submodule
                new Thread(() -> {
                    try {
                        // Need to wait until lane is initialized so that the process can find the database
                        waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
                        existingProcessModule = (AbstractProcessModule<?>) registerSubmodule(processConfig);
                        existingProcessModule.waitForState(ModuleEvent.ModuleState.LOADED, 10000);
                        existingProcessModule.init();
                    } catch (SensorHubException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        }

        String statusMsg = "Note: ";
        if (existingRPMModule == null)
            statusMsg += "No RPM driver found in lane.\n";
        if (existingProcessModule == null || !registeringProcessModule)
            statusMsg += "No database process module found in lane.\n";
        if (!statusMsg.equalsIgnoreCase("Note: "))
            reportStatus(statusMsg);

        // Init modules all modules except process module as it requires other modules to be started
        for (var module: getMembers().values()) {
            if (module != null) {
                try {
                    module.init();
                }
                catch (Exception e) {
                    getLogger().error("Cannot initialize system component {}", MsgUtils.moduleString(module), e);
                }
            }
        }
    }

    private AbstractModule<?> registerSubmodule(ModuleConfig config) throws SensorHubException {
        var newMember = new SensorSystemConfig.SystemMember();
        newMember.config = config;
        var newSubmodule = (AbstractModule<?>) addSubsystem(newMember);

        // Wait for loaded module, then notify listeners of config changed
        // TODO: osh-core needs proper submodule event
        new Thread(() -> {
            try {
                existingRPMModule.waitForState(ModuleEvent.ModuleState.LOADED, 10000);
            } catch (SensorHubException e) {
                throw new RuntimeException(e);
            }
            eventHandler.publish(new ModuleEvent(this, ModuleEvent.Type.CONFIG_CHANGED));
        }).start();

        return newSubmodule;
    }

    @Override
    protected void handleEvent(Event e) {
        super.handleEvent(e);

        // TODO: Add config listener to remove old system from database config if system uid is updated
        // TODO: Add config listener to swap to new database if lane database is changed
        // TODO: Handle events for video drivers, RPM drivers, and process module

        if (e instanceof ModuleEvent event) {

            // In case lane is deleted, delete all data from database
            if (event.getType() == ModuleEvent.Type.DELETED && getLaneConfig().autoDelete) {
                String uid = getUniqueIdentifier();
                if (uid != null && !uid.isBlank()) {
                    // Get lane database
                    var db = getParentHub().getSystemDriverRegistry().getDatabase(getUniqueIdentifier());
                    if (db != null) {
                        // Create transaction handler to delete system from lane database
                        var eventBus = getParentHub().getEventBus();
                        var transactionHandler = new SystemDatabaseTransactionHandler(eventBus, db);
                        try {
                            transactionHandler.getSystemHandler(uid).delete(true);
                        } catch (DataStoreException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        }
    }

    @Override
    public synchronized void updateConfig(SensorSystemConfig config) throws SensorHubException {
        super.updateConfig(config);

        // Listener
    }

    @Override
    protected void setState(ModuleEvent.ModuleState newState) {
        super.setState(newState);

//        // Ensure that autoStart starts modules after Sensor System is enabled
//        if (newState == ModuleEvent.ModuleState.STARTED) {
//            // Get list of exclusive modules
//            List<IModule<?>> otherModules = new ArrayList<>();
//            for (var module : getMembers().values())
//                if (module != null && module != existingProcessModule && module.getConfiguration().autoStart)
//                    otherModules.add(module);
//
//            // Create list of asynchronous module actions
//            List<CompletableFuture<Void>> asyncActions = new ArrayList<>();
//            for (var module : otherModules) {
//                asyncActions.add(CompletableFuture.runAsync(() -> {
//                    try {
//                        module.waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
//                        module.start();
//                        module.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
//                    } catch (SensorHubException e) {
//                        throw new RuntimeException(e);
//                    }
//                }));
//            }
//
//            // Wait for all module actions to finish before starting excluded module's action
//            CompletableFuture.allOf(asyncActions.toArray(new CompletableFuture[0])).thenRunAsync(() -> {
//                try {
//                    if (existingProcessModule.getConfiguration().autoStart)
//                        existingProcessModule.start();
//                } catch (SensorHubException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//        }
    }

    private SensorConfig createRPMConfig(RPMConfig rpmConfig) {
        Asserts.checkNotNull(rpmConfig.rpmType);
        Asserts.checkNotNullOrBlank(rpmConfig.rpmLabel, "Please specify an RPM driver label");
        Asserts.checkNotNullOrBlank(rpmConfig.rpmUniqueId, "Please specify a unique RPM ID");
        Asserts.checkNotNull(rpmConfig.remoteHost);

        // TODO: Make Rapiscan and Aspect drivers use common class/interface and common configs
        SensorConfig config;
        switch(rpmConfig.rpmType) {
            // Create Aspect config
            case ASPECT -> {
                AspectConfig aspectConfig = new AspectConfig();
                aspectConfig.serialNumber = rpmConfig.rpmUniqueId;
                aspectConfig.moduleClass = AspectSensor.class.getCanonicalName();
                // Setup communication config
                var comm = aspectConfig.commSettings = new ModbusTCPCommProviderConfig();
                comm.protocol.remoteHost = rpmConfig.remoteHost;
                comm.protocol.remotePort = rpmConfig.remotePort;
                config = aspectConfig;
            }
            // Create Rapiscan config
            case RAPISCAN -> {
                RapiscanConfig rapiscanConfig = new RapiscanConfig();
                rapiscanConfig.serialNumber = rpmConfig.rpmUniqueId;
                rapiscanConfig.moduleClass = RapiscanSensor.class.getCanonicalName();
                // Setup communication config
                var comm = rapiscanConfig.commSettings = new TCPCommProviderConfig();
                comm.protocol.remoteHost = rpmConfig.remoteHost;
                comm.protocol.remotePort = rpmConfig.remotePort;
                config = rapiscanConfig;
            }
            default -> { return null; }
        }

        // Use label from config
        config.name = rpmConfig.rpmLabel;
        config.autoStart = true;

        return config;
    }

    private LaneConfig getLaneConfig() {
        return (LaneConfig) this.config;
    }

}