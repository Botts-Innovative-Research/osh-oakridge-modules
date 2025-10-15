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
import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.comm.ModbusTCPCommProvider;
import com.botts.impl.sensor.aspect.comm.ModbusTCPCommProviderConfig;
import com.botts.impl.sensor.rapiscan.EMLConfig;
import com.botts.impl.sensor.rapiscan.RapiscanConfig;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.config.*;
import com.botts.impl.system.lane.helpers.occupancy.OccupancyWrapper;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.system.SystemRemovedEvent;
import org.sensorhub.impl.comm.TCPCommProviderConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.sensor.SensorSystemConfig;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.utils.MsgUtils;
import org.vast.util.Asserts;

import java.util.*;
import java.util.concurrent.*;

/**
 * Extended functionality of the SensorSystem class unique for Open Source Central Alarm (OSCAR)
 *
 * @author Alex Almanza
 * @author Kyle Fitzpatrick
 * @author Kalyn Stricklin
 * @since March 2025
 */
public class LaneSystem extends SensorSystem {

    private static final String URN_PREFIX = "urn:";
    private static final String RAPISCAN_URI = URN_PREFIX + "osh:sensor:rapiscan";
    private static final String ASPECT_URI = URN_PREFIX + "osh:sensor:aspect";
    private static final String PROCESS_URI = URN_PREFIX + "osh:process:occupancy";
    private static final String VIDEO_CLIPS_DIRECTORY = "clips/";
    private static final String VIDEO_STREAMING_DIRECTORY = "streaming/";

    AbstractSensorModule<?> existingRPMModule = null;
    Flow.Subscription subscription = null;
    private ExecutorService threadPool = null;
    Map<String, FFMPEGConfig> ffmpegConfigs = null;
    OccupancyWrapper occupancyWrapper;


    @Override
    protected void doInit() throws SensorHubException {
        threadPool = Executors.newSingleThreadExecutor();
        ffmpegConfigs = new HashMap<>();
        occupancyWrapper = null;

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
            }else if (member instanceof FFMPEGSensor) {
                ffmpegConfigs.put(member.getLocalID(), ((FFMPEGSensor) member).getConfiguration());
            }
        }

        // Lane database and process setup
        if (getConfiguration().laneOptionsConfig != null) {
            // Initial RPM config
            var rpmConfig = getConfiguration().laneOptionsConfig.rpmConfig;
            if (rpmConfig != null && existingRPMModule == null) {
                // Create Rapiscan or Aspect config, then add as submodule
                var config = createRPMConfig(rpmConfig);
                existingRPMModule = (AbstractSensorModule<?>) registerSubmodule(config);
            }
            if (existingRPMModule != null) {
                occupancyWrapper = new OccupancyWrapper(getParentHub(), existingRPMModule);
                //occupancyWrapper.videoNamePrefix = BASE_VIDEO_DIRECTORY + "lane" + getConfiguration().groupID + "/";
            }
            // Initial FFmpeg config
            var ffmpegConfigList = getConfiguration().laneOptionsConfig.ffmpegConfig;
            for (var simpleConfig : ffmpegConfigList) {
                FFMPEGConfig config = createFFmpegConfig(simpleConfig, ffmpegConfigList.indexOf(simpleConfig));
                var ffmpegModule = createFFmpegModule(config);
                if (occupancyWrapper != null) {
                    occupancyWrapper.addFFmpegSensor(ffmpegModule);
                }
            }
        }

        String statusMsg = "Note: ";
        if (existingRPMModule == null)
            statusMsg += "No RPM driver found in lane.\n";
        if (!statusMsg.equalsIgnoreCase("Note: "))
            reportStatus(statusMsg);

        boolean rpmFailedToInit = false;

        // Init modules all modules except process module as it requires other modules to be started
        for (var module: getMembers().values()) {
            if (module != null) {
                try {
                    /*
                    threadPool.execute(() -> {
                        try {
                            module.init();
                        } catch (SensorHubException e) {
                            throw new RuntimeException(e);
                        }
                    });

                     */
                    module.init();
                }
                catch (Exception e) {
                    // If rapiscan fails to initialize, then don't load process module
                    if (module instanceof RapiscanSensor || module instanceof AspectSensor)
                        rpmFailedToInit = true;
                    getLogger().error("Cannot initialize system component {}", MsgUtils.moduleString(module), e);
                }
            }
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

    private FFMPEGSensorBase<?> createFFmpegModule(FFMPEGConfig ffmpegConfig) throws SensorHubException {
        // Get ffmpeg submodule with the same unique serial num
        // If there is a module registered for this serial number, then the driver was already registered
        var ffmpegModuleOpt = getMembers().values().stream().filter(
                module -> (
                        module instanceof FFMPEGSensor && ((FFMPEGSensor) module).getConfiguration().serialNumber.equals(ffmpegConfig.serialNumber)
                )
        ).findFirst();

        // Register the new module
        if (ffmpegModuleOpt.isEmpty()) {
            return (FFMPEGSensorBase<?>) registerSubmodule(ffmpegConfig);
        } else { // If there is already a module registered, then update the config
            FFMPEGSensor module = (FFMPEGSensor) ffmpegModuleOpt.get();
            ffmpegConfig.id = module.getLocalID();
            module.updateConfig(ffmpegConfig);
            return module;
        }
    }

    @Override
    public void cleanup() throws SensorHubException {
        super.cleanup();

        // Auto delete lane data if specified
        if (getConfiguration() != null && getConfiguration().autoDelete) {
            IObsSystemDatabase db = getParentHub().getSystemDriverRegistry().getDatabase(getUniqueIdentifier());

            String laneUID = getUniqueIdentifier();
            if(laneUID == null)
                laneUID = createLaneUID(config.uniqueID);
            if (db != null) {
                // Remove lane if nothing else
                List<String> removalList = new ArrayList<>(List.of(laneUID));
                // Remove lane (and process) from database
                deleteSystemsFromDatabase(removalList);
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

    private synchronized void deleteSystemsFromDatabase(List<String> systemUIDs) {
        // Delete the system data. Use for-loop in case of different databases
        for (String sysUID : systemUIDs) {
            IObsSystemDatabase obsDatabase = getParentHub().getSystemDriverRegistry().getDatabase(sysUID);
            if (obsDatabase == null)
                return;

            var sysFilter = new SystemFilter.Builder()
                    .withUniqueIDs(sysUID)
                    .includeMembers(true)
                    .build();

            // Delete old observations
            obsDatabase.getObservationStore().removeEntries(new ObsFilter.Builder()
                    .withDataStreams()
                    .withSystems(sysFilter)
                    .done()
                    .build());
            // Delete old data streams
            obsDatabase.getDataStreamStore().removeEntries(new DataStreamFilter.Builder()
                    .withSystems(sysFilter)
                    .build());

            // Delete old systems
            obsDatabase.getSystemDescStore().removeEntries(sysFilter);
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
                if (event.getSystemUID().contains(RAPISCAN_URI) || event.getSystemUID().contains(ASPECT_URI)) {
                    occupancyWrapper.removeRpmSensor();
                    existingRPMModule = null;
                }
            }

        }

        else if (e instanceof ModuleEvent event) {
            // Module STATE_CHANGED events
            if (event.getType() == ModuleEvent.Type.STATE_CHANGED) {

                if (event.getModule() == existingRPMModule && event.getNewState() == ModuleEvent.ModuleState.STARTED) {
                    occupancyWrapper.start();
                }

                if (event.getNewState() == ModuleEvent.ModuleState.STOPPING) {
                    occupancyWrapper.stop();
                }

                // New module loaded
                if (event.getNewState() == ModuleEvent.ModuleState.LOADED) {

                    if (event.getModule() instanceof FFMPEGSensor ffmpegDriver){
                        if(!ffmpegConfigs.containsKey(ffmpegDriver.getLocalID()))
                            ffmpegConfigs.put(ffmpegDriver.getLocalID(), ffmpegDriver.getConfiguration());
                    }
                }
            }

            else if (event.getType().equals(ModuleEvent.Type.CONFIG_CHANGED)) {
                // FFmpeg config changed events
                if (event.getModule() instanceof FFMPEGSensor ffmpegDriver) {
                    // Let's only handle our own ffmpeg children
                    if (ffmpegDriver.getParentSystem() == null || !ffmpegDriver.getParentSystem().equals(this))
                        return;

                    var oldConfig = ffmpegConfigs.get(ffmpegDriver.getLocalID());

                    if (oldConfig == null) {
                        ffmpegConfigs.put(ffmpegDriver.getLocalID(), ffmpegDriver.getConfiguration());
                        return;
                    }

                    var newConfig = ffmpegDriver.getConfiguration();

                    // If important parts of configuration are updated, remove data from old driver
                    if (newConfig.connection.useTCP != oldConfig.connection.useTCP
                    || !Objects.equals(newConfig.connection.connectionString, oldConfig.connection.connectionString)
                    || !Objects.equals(newConfig.connection.transportStreamPath, oldConfig.connection.transportStreamPath)) {
                        if (ffmpegDriver.getUniqueIdentifier() != null && getParentHub().getSystemDriverRegistry().getDatabase(getUniqueIdentifier()) != null) {

                            try {
                                ffmpegDriver.stop();
                                ffmpegDriver.waitForState(ModuleEvent.ModuleState.INITIALIZED, 5000);
                                deleteSystemsFromDatabase(List.of(ffmpegDriver.getUniqueIdentifier()));
                                getParentHub().getSystemDriverRegistry().register(ffmpegDriver);
                                if (ffmpegDriver.getConfiguration().autoStart)
                                    ffmpegDriver.start();
                            } catch (SensorHubException ex) {
                                getLogger().error("Failed to delete FFmpeg driver from database. Please delete old FFmpeg system in database to avoid further issues" + ex.getMessage());
                            }
                        }
                        ffmpegConfigs.put(ffmpegDriver.getLocalID(), ffmpegDriver.getConfiguration());

                    }
                }
            }

            else if (event.getType().equals(ModuleEvent.Type.ERROR)) {
                // If lane fails to load, then perform some magic
                if (Objects.equals(event.getModule(), this)) {
                    // Check for SensorException
                    if (event.getError() != null && event.getError() instanceof SensorHubException hubException
                    && hubException.getCause() instanceof SensorException sensorException) {
                        // Check for driver failed to register error
                        if (sensorException.getCause() instanceof NullPointerException) {
                            // Find prev UID and delete from database
                            IObsSystemDatabase database = null;
                            if (getUniqueIdentifier() != null)
                                database = getParentHub().getSystemDriverRegistry().getDatabase(getUniqueIdentifier());
                            long systemAndMembers = 0;
                            if (database != null)
                                systemAndMembers = database.getSystemDescStore().select(new SystemFilter.Builder().withUniqueIDs(getUniqueIdentifier()).includeMembers(true).build()).count();
                            // Only delete from database if lane doesn't have children
                            if (systemAndMembers == 1) {
                                var transactionHandler = new SystemDatabaseTransactionHandler(getParentHub().getEventBus(), database);
                                try {
                                    transactionHandler.getSystemHandler(getUniqueIdentifier()).delete(true);
                                } catch (DataStoreException ex) {
                                    getLogger().error("Failed to remove system from database: {}", ex.getMessage());
                                }
                            }
                        }

                        // Check for illegal state (command receiver already connected, etc.)
                        if (sensorException.getCause() instanceof IllegalStateException) {
                            // TODO: Add fix. Current fix is restarting OSH
                        }

                    }
                }
            }

        }
    }

    private SensorConfig createRPMConfig(RPMConfig rpmConfig) {
        Asserts.checkNotNull(rpmConfig.remoteHost);

        SensorConfig config = null;

        if(rpmConfig instanceof AspectRPMConfig aspectRPMConfig){
            AspectConfig aspectConfig = new AspectConfig();
            aspectConfig.serialNumber = getConfiguration().uniqueID;
            aspectConfig.moduleClass = AspectSensor.class.getCanonicalName();

            // Setup communication config
            var comm = aspectConfig.commSettings = new ModbusTCPCommProviderConfig();
            comm.protocol.remoteHost = aspectRPMConfig.remoteHost;
            comm.protocol.remotePort = aspectRPMConfig.remotePort;

            comm.protocol.addressRange.from = aspectRPMConfig.addressRange.from;
            comm.protocol.addressRange.to = aspectRPMConfig.addressRange.to;
            // Update connection timeout to be 5 seconds instead of 3 seconds by default
            comm.connection.connectTimeout = 5000;
            comm.connection.reconnectAttempts = 10;
            comm.moduleClass = ModbusTCPCommProvider.class.getCanonicalName();
            config = aspectConfig;
        }else if(rpmConfig instanceof RapiscanRPMConfig rapiscanRPMConfig){
            RapiscanConfig rapiscanConfig = new RapiscanConfig();
            rapiscanConfig.serialNumber = getConfiguration().uniqueID;
            rapiscanConfig.moduleClass = RapiscanSensor.class.getCanonicalName();

            // add eml for rapiscan
            if (rapiscanRPMConfig.emlConfig != null) {
                var eml = rapiscanConfig.emlConfig = new EMLConfig();
                eml.emlEnabled = rapiscanRPMConfig.emlConfig.emlEnabled;
                eml.isCollimated = rapiscanRPMConfig.emlConfig.isCollimated;
                eml.laneWidth = rapiscanRPMConfig.emlConfig.laneWidth;
            }

            // Setup communication config
            var comm = rapiscanConfig.commSettings = new TCPCommProviderConfig();
            comm.protocol.remoteHost = rapiscanRPMConfig.remoteHost;
            comm.protocol.remotePort = rapiscanRPMConfig.remotePort;
            // Update connection timeout to be 5 seconds instead of 3 seconds by default
            comm.connection.connectTimeout = 5000;
            comm.connection.reconnectAttempts = 10;

            config = rapiscanConfig;
        }else{
            reportError("RPM Config specified is invalid, config must be of type AspectRPMConfig or RapiscanRPMConfig", new IllegalArgumentException());
        }

        config.name = getConfiguration().name + " - RPM";
        config.autoStart = true;

        return config;
    }
    
    private FFMPEGConfig createFFmpegConfig(FFMpegConfig ffmpegConfig, int videoIndex) {
        String defaultAxis = "/axis-media/media.amp?adjustablelivestream=1&resolution=640x480&videocodec=h264&videokeyframeinterval=15";

        Asserts.checkNotNull(ffmpegConfig.remoteHost);

        // This is the actual ffmpeg sensor config
        StringBuilder endpoint = new StringBuilder("rtsp://"); // All streams should be over rtsp

        // Add username and password if provided
        if (ffmpegConfig.username != null && !ffmpegConfig.username.isBlank()) {
            endpoint.append(ffmpegConfig.username);
            endpoint.append(":");
            endpoint.append(ffmpegConfig.password);
            endpoint.append("@");
        }
        endpoint.append(ffmpegConfig.remoteHost);

        FFMPEGConfig config = new FFMPEGConfig();

        String path = defaultAxis;

        if(ffmpegConfig instanceof AxisCameraConfig axisVideoConfig){
             path = axisVideoConfig.streamPath.getPath();
        }else if (ffmpegConfig instanceof SonyCameraConfig sonyVideoConfig){
             path = sonyVideoConfig.streamPath;
        }else if(ffmpegConfig instanceof CustomCameraConfig customVideoConfig){
             path = !customVideoConfig.streamPath.isEmpty() ? customVideoConfig.streamPath : defaultAxis;
        }

        endpoint.append(path);

        config.connection.useTCP = true;
        config.connection.fps = 24;
        config.name = getConfiguration().name + " - Camera " + videoIndex;
        config.serialNumber = "lane:" + getConfiguration().uniqueID + ":" + videoIndex;
        config.autoStart = true;
        config.connection.connectionString = endpoint.toString();
        config.moduleClass = FFMPEGSensor.class.getCanonicalName();
        config.connectionConfig.connectTimeout = 5000;
        config.connectionConfig.reconnectAttempts = 10;
        config.output.useHLS = false;
        config.fileConfig.hlsDirectory = VIDEO_STREAMING_DIRECTORY;
        config.fileConfig.videoClipDirectory = VIDEO_CLIPS_DIRECTORY;
        return config;
    }

    @Override
    public LaneConfig getConfiguration() {
        return (LaneConfig) this.config;
    }
}