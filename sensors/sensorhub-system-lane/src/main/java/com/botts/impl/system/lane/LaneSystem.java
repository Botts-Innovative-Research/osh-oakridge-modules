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
import com.botts.impl.system.lane.config.FFMpegConfig;
import com.botts.impl.system.lane.config.LaneConfig;
import com.botts.impl.system.lane.config.LaneDatabaseConfig;
import com.botts.impl.system.lane.config.RPMConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.system.SystemRemovedEvent;
import org.sensorhub.impl.comm.TCPCommProviderConfig;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.sensor.SensorSystemConfig;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.utils.MsgUtils;
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

    @Override
    protected void doInit() throws SensorHubException {
        threadPool = Executors.newSingleThreadExecutor();

        // generate unique ID
        if (config.uniqueID != null && !config.uniqueID.equals(AUTO_ID)) {
            if (config.uniqueID.startsWith(URN_PREFIX)) {
                this.uniqueID = config.uniqueID;
                String suffix = config.uniqueID.replace(URN_PREFIX, "");
                generateXmlID(DEFAULT_XMLID_PREFIX, suffix);
            } else {
                this.uniqueID = createLaneUID(config.uniqueID);
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
            // Initial FFmpeg config
            var ffmpegConfigList = getLaneConfig().laneOptionsConfig.ffmpegConfig;
            for (var simpleConfig : ffmpegConfigList) {
                FFMPEGConfig config = createFFmpegConfig(simpleConfig);
                createFFmpegModule(config);
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
            statusMsg += "No database process module found for this lane.\n";
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
            .withTopicID(EventUtils.getSystemRegistryTopicID(), ModuleRegistry.EVENT_GROUP_ID)
            .subscribe(this::handleLaneEvent)
            .thenAccept(subscription -> {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
                getLogger().info("Started module subscription to {}", getLocalID());
            });
    }

    private void createFFmpegModule(FFMPEGConfig ffmpegConfig) throws SensorHubException {
        // Get ffmpeg submodule with the same unique serial num
        // If there is a module registered for this serial number, then the driver was already registered
        var ffmpegModuleOpt = getMembers().values().stream().filter(
                module -> (
                        module instanceof FFMPEGSensor && ((FFMPEGSensor) module).getConfiguration().serialNumber.equals(ffmpegConfig.serialNumber)
                )
        ).findFirst();

        // Register the new module
        if (ffmpegModuleOpt.isEmpty()) {
            registerSubmodule(ffmpegConfig);
        } else { // If there is already a module registered, then update the config
            FFMPEGSensor module = (FFMPEGSensor) ffmpegModuleOpt.get();
            ffmpegConfig.id = module.getLocalID();
            module.updateConfig(ffmpegConfig);
        }
    }

    @Override
    public void cleanup() throws SensorHubException {
        super.cleanup();

        // Auto delete lane data if specified
        if (getLaneConfig() != null && getLaneConfig().autoDelete) {
            String laneDatabaseId = getLaneDatabaseID();
            String laneUID = getUniqueIdentifier();
            if(laneUID == null)
                laneUID = createLaneUID(config.uniqueID);
            if (laneDatabaseId != null) {
                // Remove lane if nothing else
                List<String> removalList = new ArrayList<>(List.of(laneUID));
                // Auto delete process module and remove from database
                if (existingProcessModule != null) {
                    var processDesc = existingProcessModule.getCurrentDescription();
                    String processUID;
                    if (processDesc == null)
                        processUID = createProcessUID(config.uniqueID);
                    else
                        processUID = processDesc.getUniqueIdentifier();
                    try {
                        getParentHub().getModuleRegistry().destroyModule(existingProcessModule.getLocalID());
                    } catch (SensorHubException ex) {
                        throw new RuntimeException(ex);
                    }
                    removalList.add(processUID);
                }
                // Remove lane (and process) from database
                deleteSystemsFromDatabase(laneDatabaseId, removalList);
            }
        }

        // Cancel and remove module/system subscription on cleanup
        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }

    private String createLaneUID(String suffix) {
        return URN_PREFIX + "osh:system:lane:" + suffix;
    }

    private String createProcessUID(String suffix) {
        return URN_PREFIX + "osh:process:occupancy:" + suffix;
    }

    private synchronized void deleteSystemsFromDatabase(String databaseId, List<String> systemUIDs) {
        try {
            // Collect database from configured module ID
            IObsSystemDatabase obsDatabase = ((SystemDriverDatabase) getParentHub().getModuleRegistry().getModuleById(databaseId)).getWrappedDatabase();

            // Delete the system data
            if (obsDatabase != null) {
                var transactionHandler = new SystemDatabaseTransactionHandler(getParentHub().getEventBus(), obsDatabase);
                // Don't use forEach bc nested try-catch
                for (String sysUID : systemUIDs)
                    transactionHandler.getSystemHandler(sysUID).delete(true);
            }
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        } catch (ClassCastException e) {
            throw new ClassCastException("Database ID in configuration does not correspond to a System Driver Database!");
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
        // TODO: Handle events for video drivers, RPM drivers, and process module
        // TODO: If lane is not saved to config, then delete database data and process?
        if (e instanceof SystemRemovedEvent event) {
            // Signifies that a subsystem was removed from the lane
            if (event.getParentGroupUID() != null && event.getParentGroupUID().equals(getUniqueIdentifier())) {

                // If RPM is removed, nullify local object
                if (event.getSystemUID().contains(RAPISCAN_URI) || event.getSystemUID().contains(ASPECT_URI))
                    existingRPMModule = null;
            }

            // If process is removed, nullify local object
            if (event.getSystemUID().contains(PROCESS_URI))
                existingProcessModule = null;
        }

        else if (e instanceof ModuleEvent event) {
            // Module STATE_CHANGED events
            if (event.getType() == ModuleEvent.Type.STATE_CHANGED) {
                // Module STARTED events
                if (event.getNewState() == ModuleEvent.ModuleState.STARTED) {
                    // If RPM module is started, then start process module
                    if (existingRPMModule != null && Objects.equals(event.getSourceID(), existingRPMModule.getLocalID()) && existingProcessModule != null && !existingProcessModule.isStarted()) {
                        try {
                            if (existingProcessModule.getCurrentState().equals(ModuleEvent.ModuleState.LOADED))
                                getParentHub().getModuleRegistry().initModule(existingProcessModule.getLocalID());
                            existingProcessModule.waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
                            getParentHub().getModuleRegistry().startModuleAsync(existingProcessModule);
                        } catch (SensorHubException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                }

                // Module STOPPED events
                else if (event.getNewState() == ModuleEvent.ModuleState.STOPPED) {
                    // If RPM module is stopped, then stop process module
                    if (existingRPMModule != null && Objects.equals(event.getSourceID(), existingRPMModule.getLocalID()) && existingProcessModule != null) {
                        try {
                            getParentHub().getModuleRegistry().stopModuleAsync(existingProcessModule);
                        } catch (SensorHubException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }

                // New module loaded
                else if (event.getNewState() == ModuleEvent.ModuleState.LOADED) {
                    // If process is loaded manually and null locally, find it and keep track of it
                    if (event.getModule() instanceof OccupancyProcessModule processModule && existingProcessModule == null) {
                        if (Objects.equals(processModule.getConfiguration().systemUID, this.getUniqueIdentifier())) {
                            existingProcessModule = processModule;
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

    private FFMPEGConfig createFFmpegConfig(FFMpegConfig ffmpegConfig) {
        Asserts.checkNotNull(ffmpegConfig.ffmpegType);
        Asserts.checkNotNullOrBlank(ffmpegConfig.ffmpegLabel, "Please specify an FFmpeg driver label");
        Asserts.checkNotNullOrBlank(ffmpegConfig.ffmpegUniqueId, "Please specify a unique FFmpeg ID");
        Asserts.checkNotNull(ffmpegConfig.remoteHost);

        FFMPEGConfig config = new FFMPEGConfig(); // This is the actual ffmpeg sensor config
        StringBuilder endpoint = new StringBuilder("rtsp://"); // All streams should be over rtsp

        // Add username and password if provided
        if (ffmpegConfig.username != null && !ffmpegConfig.username.isBlank()) {
            endpoint.append(ffmpegConfig.username);
            endpoint.append(":");
            endpoint.append(ffmpegConfig.password);
            endpoint.append("@");
        }
        endpoint.append(ffmpegConfig.remoteHost);

        switch(ffmpegConfig.ffmpegType) {
            // Autofill with AXIS info
            case AXIS -> {
                String path = "/axis-media/media.amp?adjustablelivestream=1&resolution=640x480&videocodec=h264"; // rtsp fps uncapped, might need to change
                endpoint.append(path);
                ffmpegConfig.streamPath = path; // Set path in the user-facing config to help indicate what's being autofilled
                config.connection.fps = 24;
            }
            // Autofill with SONY info
            case SONY -> {
                String path = ":554/media/video1";
                endpoint.append(path);
                ffmpegConfig.streamPath = path; // Set path in the user-facing config to help indicate what's being autofilled
                config.connection.fps = 24; // Setting it to 24 for now, may change it later

            }
            case CUSTOM -> {
                endpoint.append(ffmpegConfig.streamPath);
                config.connection.fps = 24;
            }
            default -> { return null; }
        }

        config.connection.isMJPEG = false;
        config.name = ffmpegConfig.ffmpegLabel;
        config.serialNumber = ffmpegConfig.ffmpegUniqueId;
        config.autoStart = true;
        config.connection.connectionString = endpoint.toString();
        config.moduleClass = FFMPEGSensor.class.getCanonicalName();
        config.connectionConfig.reconnectAttempts = 10;
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