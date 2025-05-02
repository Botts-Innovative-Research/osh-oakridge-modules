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
import com.botts.impl.system.lane.config.LaneDatabaseConfig;
import com.botts.impl.system.lane.config.RPMConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.system.SystemAddedEvent;
import org.sensorhub.api.system.SystemRemovedEvent;
import org.sensorhub.impl.comm.TCPCommProviderConfig;
import org.sensorhub.impl.database.system.MaxAgeAutoPurgeConfig;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.sensor.SensorSystemConfig;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.utils.Async;
import org.sensorhub.utils.MsgUtils;
import org.sensorhub.utils.NamedThreadFactory;
import org.vast.util.Asserts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Extended functionality of the SensorSystem class unique for Open Source Central Alarm (OSCAR)
 *
 * @author Alex Almanza
 * @since March 2025
 */
public class LaneSystem extends SensorSystem {

    private static final String URN_PREFIX = "urn:";
    private static final String RAPISCAN_URI = URN_PREFIX + "osh:sensor:rapiscan";
    private static final String ASPECT_URI = URN_PREFIX + "osh:sensor:aspect";
    private static final String PROCESS_URI = URN_PREFIX + "osh:process:occupancy";
    AbstractSensorModule<?> existingRPMModule = null;
    AbstractProcessModule<?> existingProcessModule = null;
    Flow.Subscription subscription = null;
    private ExecutorService threadPool = null;
    private BlockingQueue<Runnable> execQueue = null;

    @Override
    protected void doInit() throws SensorHubException {
        execQueue = new LinkedBlockingQueue<>(100);
        threadPool = new ThreadPoolExecutor(3, 10, 10, TimeUnit.SECONDS, execQueue, new NamedThreadFactory("LaneModuleThreadPool"));

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

        // Check state members too in case config hasn't been updated
        for (var member : getMembers().values()) {
            if (member instanceof RapiscanSensor || member instanceof AspectSensor) {
                existingRPMModule = (AbstractSensorModule<?>) member;
                break;
            }
        }

        // Check all occupancy process modules for module associated to this lane
        for (var processModule : getParentHub().getModuleRegistry().getLoadedModules(OccupancyProcessModule.class)) {
            if (processModule.getConfiguration().systemUID.equals(getUniqueIdentifier())) {
                existingProcessModule = processModule;
                break;
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

            // Process creation / attachment
            if (getLaneConfig().laneOptionsConfig.createProcess && existingProcessModule == null) {
                registeringProcessModule = true;
            }
        }

        String statusMsg = "Note: ";
        if (existingRPMModule == null)
            statusMsg += "No RPM driver found in lane.\n";
        if (existingProcessModule == null && !registeringProcessModule)
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

        // Initial creation of process after everything starts
        if (getLaneConfig().laneOptionsConfig != null && getLaneConfig().laneOptionsConfig.createProcess && existingProcessModule == null) {
            threadPool.execute(() -> {
                // Create process config
                OccupancyProcessConfig processConfig = new OccupancyProcessConfig();
                processConfig.moduleClass = OccupancyProcessModule.class.getCanonicalName();
                processConfig.serialNumber = config.uniqueID;
                processConfig.name = getConfiguration().name + " Database Process";
                processConfig.systemUID = getUniqueIdentifier();
                // TODO: Set autostart == lane autostart
                try {
                    existingProcessModule = (AbstractProcessModule<?>) getParentHub().getModuleRegistry().loadModule(processConfig);
                    reportStatus("Process module loaded with module ID " + existingProcessModule.getLocalID());
                } catch (SensorHubException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        getParentHub().getEventBus().newSubscription()
            // TODO: osh-core needs to use EventUtils for module topic IDs
            .withTopicID(EventUtils.getSystemRegistryTopicID(), getLocalID())
            .subscribe(this::handleLaneEvent)
            .thenAccept(subscription -> {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
                getLogger().info("Started module subscription to {}", getLocalID());
            });
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();
    }

    @Override
    protected void afterStart() throws SensorHubException {
        super.afterStart();
    }

    @Override
    protected void setState(ModuleEvent.ModuleState newState) {
        super.setState(newState);

        // Here we know lane has been fully started
        if (newState == ModuleEvent.ModuleState.STARTED) {

            // If process module has auto start enabled and it's NOT already started, we may as well start it too
            if (existingProcessModule != null && existingProcessModule.getConfiguration().autoStart && !existingProcessModule.isStarted()) {
                threadPool.execute(() -> {
                    try {
                        Async.waitForCondition(this::checkRPMExists, 500, 10000);
                        existingRPMModule.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
                        getParentHub().getModuleRegistry().startModuleAsync(existingProcessModule);
                    } catch (TimeoutException | SensorHubException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private boolean checkRPMExists() {
        if (existingRPMModule == null)
            return false;
        String rpmUID = existingRPMModule.getUniqueIdentifier();
        if(rpmUID == null || rpmUID.isBlank())
            return false;
        return checkSystemExistsInDatabase(rpmUID);
    }

    private boolean checkSystemExistsInDatabase(String systemUID) {
        var sysFilter = new SystemFilter.Builder().withUniqueIDs(systemUID).build();
        return getParentHub().getDatabaseRegistry().getFederatedDatabase().getSystemDescStore().select(sysFilter).findAny().isPresent();
    }

    private boolean checkSystemHasDatastreams(String systemUID) {
        var sysFilter = new SystemFilter.Builder().withUniqueIDs(systemUID).build();
        return getParentHub().getDatabaseRegistry().getFederatedDatabase().getDataStreamStore().select(new DataStreamFilter.Builder().withSystems(sysFilter).build()).findAny().isPresent();
    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
    }

    @Override
    public void cleanup() throws SensorHubException {
        super.cleanup();

        // Cancel and remove module/system subscription on cleanup
        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }

    private void addSystemsToDatabase(String databaseId, List<String> systemUIDs) {
        try {
            if (getParentHub().getModuleRegistry().getModuleById(databaseId) instanceof SystemDriverDatabase dbModule) {
                var dbModuleConfig = dbModule.getConfiguration();
                dbModuleConfig.systemUIDs.addAll(systemUIDs);
                dbModule.updateConfig(dbModuleConfig);
            }
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeSystemsFromDatabase(String databaseId, boolean deleteData, List<String> systemUIDs) {
        try {
            IObsSystemDatabase obsDatabase = null;
            // Remove system ids from database config
            if (getParentHub().getModuleRegistry().getModuleById(databaseId) instanceof SystemDriverDatabase dbModule) {
                var dbModuleConfig = dbModule.getConfiguration();
                List.of(systemUIDs).forEach(dbModuleConfig.systemUIDs::remove);
                dbModule.updateConfig(dbModuleConfig);
                // Pass along retrieved IObsSystemDatabase to clean
                obsDatabase = dbModule.getWrappedDatabase();
            }

            // Optionally delete the system data too
            if (deleteData && obsDatabase != null) {
                var transactionHandler = new SystemDatabaseTransactionHandler(getParentHub().getEventBus(), obsDatabase);
                // Don't use forEach bc nested try-catch
                for (String sysUID : systemUIDs)
                    transactionHandler.getSystemHandler(sysUID).delete(true);
            }
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }
    }

    private void addSystemsToPurgePolicy(String databaseId, List<String> systemUIDs, int purgePeriodMinutes) {
        // Add systems to purge policy
        try {
            if (getParentHub().getModuleRegistry().getModuleById(databaseId) instanceof SystemDriverDatabase dbModule) {
                var dbModuleConfig = dbModule.getConfiguration();
                // If first purge policy exists, use it and make sure the purge period matches the config
                var purgePolicies = dbModuleConfig.autoPurgeConfig;
                if (purgePolicies != null && !purgePolicies.isEmpty()) {
                    // Check if first purge policy has correct purge period, and if it has other video drivers, then add it or skip it
                    var firstPolicy = purgePolicies.getFirst();
                    if (firstPolicy.purgePeriod != purgePeriodMinutes * 60)
                        firstPolicy.purgePeriod = purgePeriodMinutes * 60;
                    // Remove asterisk just in case it's there
                    firstPolicy.systemUIDs.remove("*");
                    firstPolicy.systemUIDs.addAll(systemUIDs);
                }
                // If there's no purge policies, create a new one with the PP and MRA from config
                else {
                    var newPurgePolicy = new MaxAgeAutoPurgeConfig();
                    newPurgePolicy.systemUIDs = new ArrayList<>(systemUIDs);
                    newPurgePolicy.purgePeriod = purgePeriodMinutes * 60;
                    newPurgePolicy.maxRecordAge = purgePeriodMinutes * 60;
                    newPurgePolicy.enabled = true;
                    if (purgePolicies == null)
                        purgePolicies = new ArrayList<>();
                    purgePolicies.add(newPurgePolicy);
                }
                dbModule.updateConfig(dbModuleConfig);
            }
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeSystemsFromPurgePolicy(String databaseId, List<String> systemUIDs) {
        // Remove systems in purge policy
        try {
            if (getParentHub().getModuleRegistry().getModuleById(databaseId) instanceof SystemDriverDatabase dbModule) {
                var dbModuleConfig = dbModule.getConfiguration();
                var purgePolicies = dbModuleConfig.autoPurgeConfig;
                if (purgePolicies != null && !purgePolicies.isEmpty()) {
                    for (var policy : purgePolicies)
                        policy.systemUIDs.removeAll(systemUIDs);
                    dbModule.updateConfig(dbModuleConfig);
                }
            }
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }
    }

    private AbstractModule<?> registerSubmodule(ModuleConfig config) throws SensorHubException {
        var newMember = new SensorSystemConfig.SystemMember();
        newMember.config = config;
        var newSubmodule = (AbstractModule<?>) addSubsystem(newMember);

        // Wait for loaded module, then notify listeners of config changed. QOL for admin UI
        threadPool.execute(() -> {
            try {
                newSubmodule.waitForState(ModuleEvent.ModuleState.LOADED, 10000);
            } catch (SensorHubException e) {
                throw new RuntimeException(e);
            }
            eventHandler.publish(new ModuleEvent(this, ModuleEvent.Type.CONFIG_CHANGED));
        });

        return newSubmodule;
    }

    private void handleLaneEvent(Event e) {
        // TODO: Add config listener to remove old system from database config if system uid is updated
        // TODO: Add config listener to swap to new database if lane database is changed
        // TODO: Handle events for video drivers, RPM drivers, and process module
        // TODO: Add listener to nullify rpm module and process module if they are removed
        // TODO: If lane is not saved to config, then delete database data and process?

        if (e instanceof SystemAddedEvent event) {

            // If process is the newly added system, attach  it to database
            if (existingProcessModule != null && Objects.equals(event.getSystemUID(), existingProcessModule.getUniqueIdentifier()) && getLaneDatabaseID() != null) {
                threadPool.execute(() -> {
                    try {
                        Async.waitForCondition(() -> checkSystemExistsInDatabase(existingProcessModule.getUniqueIdentifier()), 500, 10000);
                        existingProcessModule.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
                        addSystemsToDatabase(getLaneDatabaseID(), List.of(existingProcessModule.getUniqueIdentifier()));
                    } catch (TimeoutException | SensorHubException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }

            // If lane is the newly added system, attach it to database
            if (Objects.equals(event.getSystemUID(), getUniqueIdentifier()) && getLaneDatabaseID() != null) {
                threadPool.execute(() -> {
                    try {
                        Async.waitForCondition(() -> checkSystemExistsInDatabase(getUniqueIdentifier()), 500, 10000);
                        waitForState(ModuleEvent.ModuleState.STARTED, 10000);
                        addSystemsToDatabase(getLaneDatabaseID(), List.of(getUniqueIdentifier()));
                    } catch (TimeoutException | SensorHubException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }

            // Signifies that a subsystem was added to this lane
            if (event.getParentGroupUID() != null && event.getParentGroupUID().equals(getUniqueIdentifier())) {
                // If added subsystems are video systems, then add their UIDs to the database purge policy
                if (getLaneDatabaseID() != null
                        && getLaneDatabaseConfig() != null
                        && getLaneDatabaseConfig().autoPurgeVideoData
                        && getLaneDatabaseConfig().purgePeriodMinutes > 0) {

                    // Find newly added system with this lane as parent
                    var sysFilterBuilder = new SystemFilter.Builder()
                            .withUniqueIDs(event.getSystemUID())
                            .withParents(new SystemFilter.Builder().withUniqueIDs(getUniqueIdentifier()).build());

                    // Generic video data stream filter
                    var videoDsFilter = new DataStreamFilter.Builder().withObservedProperties(VideoCamHelper.DEF_IMAGE, VideoCamHelper.DEF_VIDEOFRAME).build();

                    var videoSystems = getParentHub().getDatabaseRegistry().getFederatedDatabase().getSystemDescStore().select(sysFilterBuilder.withDataStreams(videoDsFilter).build()).toList();
                    // If system has video datastreams
                    if (!videoSystems.isEmpty()) {
                        // Add to purge policy
                        List<String> videoSystemUIDs = new ArrayList<>();
                        videoSystems.forEach(videoSystem -> videoSystemUIDs.add(videoSystem.getUniqueIdentifier()));
                        addSystemsToPurgePolicy(getLaneDatabaseID(), videoSystemUIDs, getLaneDatabaseConfig().purgePeriodMinutes);
                    }
                }

                // Anytime a subsystem is added, we need to restart our associated process, so it can pick up on new systems
                if (existingRPMModule != null && existingProcessModule != null && existingRPMModule.isStarted()) {
                    // TODO: Test that a restart changes the output structure for process
                    threadPool.execute(() -> {
                        // Re-init and start process module
                        try {
                            Async.waitForCondition(() -> checkSystemExistsInDatabase(event.getSystemUID()), 500, 10000);
                            getParentHub().getModuleRegistry().initModule(existingProcessModule.getLocalID());
                            existingProcessModule.waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
                            getParentHub().getModuleRegistry().startModuleAsync(existingProcessModule);
                        } catch (SensorHubException | TimeoutException ex) {
                            System.out.println("FAILED TO INITIALIZE AND START PROCESS MODULE");
                            throw new RuntimeException(ex);
                        }
                    });
                }
            }
        }

        else if (e instanceof SystemRemovedEvent event) {

            // Signifies that a subsystem was removed from the lane
            if (event.getParentGroupUID() != null && event.getParentGroupUID().equals(getUniqueIdentifier())) {
                // If video subsystems are removed, then remove their UIDs from the database purge policy
                if (getLaneDatabaseID() != null
                && getLaneDatabaseConfig() != null
                && getLaneDatabaseConfig().autoPurgeVideoData
                && getLaneDatabaseConfig().purgePeriodMinutes > 0) {

                    // TODO: Probably put this in its own method
                    // Find removed system with this lane as parent
                    var sysFilterBuilder = new SystemFilter.Builder()
                            .withUniqueIDs(event.getSystemUID())
                            .withParents(new SystemFilter.Builder().withUniqueIDs(getUniqueIdentifier()).build());

                    // Generic video data stream filter
                    var videoDsFilter = new DataStreamFilter.Builder().withObservedProperties(VideoCamHelper.DEF_IMAGE, VideoCamHelper.DEF_VIDEOFRAME).build();

                    var videoSystems = getParentHub().getDatabaseRegistry().getFederatedDatabase().getSystemDescStore().select(sysFilterBuilder.withDataStreams(videoDsFilter).build()).toList();
                    // If system has video datastreams
                    if (!videoSystems.isEmpty()) {
                        // Remove from purge policy
                        List<String> videoSystemUIDs = new ArrayList<>();
                        videoSystems.forEach(videoSystem -> videoSystemUIDs.add(videoSystem.getUniqueIdentifier()));
                        removeSystemsFromPurgePolicy(getLaneDatabaseID(), videoSystemUIDs);
                    }
                }

                // If RPM is removed, nullify local object
                if (event.getSystemUID().contains(RAPISCAN_URI) || event.getSystemUID().contains(ASPECT_URI))
                    existingRPMModule = null;
            }

            // If process is removed, nullify local object
            if (event.getSystemUID().contains(PROCESS_URI))
                existingProcessModule = null;
        }

        else if (e instanceof ModuleEvent event) {

            // When lane is deleted, delete all lane data and process data
            if (event.getType() == ModuleEvent.Type.DELETED && event.getSourceID().equals(getLocalID())) {
                // Auto delete lane data if specified
                if (getLaneConfig().autoDelete) {
                    String laneDatabaseId = getLaneDatabaseID();
                    if (laneDatabaseId != null) {
                        // Remove lane if nothing else
                        List<String> removalList = new ArrayList<>(List.of(getUniqueIdentifier()));
                        // Auto delete process module and remove from database
                        if (existingProcessModule != null) {
                            try {
                                getParentHub().getModuleRegistry().destroyModule(existingProcessModule.getLocalID());
                            } catch (SensorHubException ex) {
                                throw new RuntimeException(ex);
                            }
                            removalList.add(existingProcessModule.getUniqueIdentifier());
                        }
                        // Remove lane (and process) from database
                        removeSystemsFromDatabase(laneDatabaseId, true, removalList);
                    }
                }
            }

            // Module STATE_CHANGED events
            if (event.getType() == ModuleEvent.Type.STATE_CHANGED) {

                // Module STARTED events
                if (event.getNewState() == ModuleEvent.ModuleState.STARTED) {

                    // If RPM module is started, then start process module
                    if (existingRPMModule != null && Objects.equals(event.getSourceID(), existingRPMModule.getLocalID()) && existingProcessModule != null) {
                        try {
                            getParentHub().getModuleRegistry().startModuleAsync(existingProcessModule);
                        } catch (SensorHubException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                }

                // Module STOPPED events
                if (event.getNewState() == ModuleEvent.ModuleState.STOPPED) {

                    // If RPM module is stopped, then stop process module
                    if (existingRPMModule != null && Objects.equals(event.getSourceID(), existingRPMModule.getLocalID()) && existingProcessModule != null) {
                        try {
                            getParentHub().getModuleRegistry().stopModuleAsync(existingProcessModule);
                        } catch (SensorHubException ex) {
                            throw new RuntimeException(ex);
                        }
                    }


                }

            }
        }
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
                // Update connection timeout to be 5 seconds instead of 3 seconds by default
                comm.connection.connectTimeout = 5000;
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
                // Update connection timeout to be 5 seconds instead of 3 seconds by default
                comm.connection.connectTimeout = 5000;
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

    private String getLaneDatabaseID() {
        if (getLaneDatabaseConfig() != null
        && getLaneConfig().laneOptionsConfig.laneDatabaseConfig.laneDatabaseId != null
        && !getLaneConfig().laneOptionsConfig.laneDatabaseConfig.laneDatabaseId.isBlank())
            return getLaneConfig().laneOptionsConfig.laneDatabaseConfig.laneDatabaseId;
        return null;
    }

    private LaneDatabaseConfig getLaneDatabaseConfig() {
        if(getLaneConfig().laneOptionsConfig != null
        && getLaneConfig().laneOptionsConfig.laneDatabaseConfig != null)
            return getLaneConfig().laneOptionsConfig.laneDatabaseConfig;
        return null;
    }

}