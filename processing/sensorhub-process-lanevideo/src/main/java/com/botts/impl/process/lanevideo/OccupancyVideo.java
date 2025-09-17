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

package com.botts.impl.process.lanevideo;

import com.botts.impl.process.lanevideo.helpers.occupancy.state.AspectStateManager;
import com.botts.impl.process.lanevideo.helpers.occupancy.state.RapiscanStateManager;
import com.botts.impl.process.lanevideo.helpers.occupancy.state.StateManager;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Text;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.utils.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.process.DataConnection;
import org.vast.process.DataConnectionList;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import org.sensorhub.impl.sensor.ffmpeg.controls.FileControl;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Executable process that takes system UID as input, and outputs all driver outputs from input system, when occupancy is detected.
 */
public class OccupancyVideo extends ExecutableProcessImpl implements ISensorHubProcess {
    public static final String RAP_PREFIX = "urn:osh:sensor:rapiscan";
    public static final OSHProcessInfo INFO = new OSHProcessInfo("lanevideo", "Lane video recording process", null, OccupancyVideo.class);
    private static final Logger logger = LoggerFactory.getLogger(OccupancyVideo.class);
    static final int OBS_BUFFER_SECONDS = 1;
    static final int DATA_FILE_NAME_INDEX = 10;
    ISensorHub hub;
    Text systemInputParam, videoPrefixParam;
    String inputSystemID, videoNamePrefix;
    String inputSystemClass;
    boolean doPublish = false;
    boolean wasAlarming = false;
    long alarmTime;
    volatile Set<BiConsumer<Long, List<String>>> fileCloseListener = new HashSet<>();
    IObsStore rpmObs;
    ObsFilter occFilter;
    DataComponent occupancyStruct;

    Instant startTime = Instant.now();
    Instant endTime = Instant.now();

    int objectCount = 0;

    public static final String OCCUPANCY = "occupancy";
    public static final String SYSTEM_INPUT_PARAM = "systemInput";
    public static final String VIDEO_PREFIX = "videoPrefix";
    public static final String DAILYFILE_NAME = "dailyFile";
    private final RADHelper fac;
    private StateManager stateManager;

    public OccupancyVideo() {
        super(INFO);

        fac = new RADHelper();

        paramData.add(SYSTEM_INPUT_PARAM, systemInputParam = fac.createText()
                .label("Parent System Input")
                .description("Parent system (Lane) that contains one RPM datastream, typically Rapiscan or Aspect.")
                .definition(SWEHelper.getPropertyUri("System"))
                .value("")
                .build());

        paramData.add(VIDEO_PREFIX, videoPrefixParam = fac.createText()
                .label("Output Video Name Prefix")
                .description("Directory and/or name prefix for the video file.")
                .definition(SWEHelper.getPropertyUri("System"))
                .value("")
                .build());

    }

    public void doInit() {
        super.notifyParamChange();
        inputSystemID = systemInputParam.getData().getStringValue();
        videoNamePrefix = videoPrefixParam.getData().getStringValue();

        if(!Objects.equals(inputSystemID, "")) {
            try {
                Async.waitForCondition(this::checkDataStreamInput, 500, 10000);
                Async.waitForCondition(this::checkDatabaseInput, 500, 10000);
            } catch (Exception e) {
                if(processInfo == null)
                    throw new IllegalStateException("RPM data stream " + inputSystemID + " not found", e);
                else
                    throw new IllegalStateException("RPM data stream " + inputSystemID + " has no data", e);
            }
        }
    }

    /**
     * Ensure the input system has an RPM data stream with "occupancy" output.
     * Also adds process input as "occupancy" output, if exists.
     * @return true if occupancy output exists
     */
    private boolean checkDataStreamInput() {
        // Clear old inputs
        inputData.clear();
        outputData.clear();
        rpmObs = null;
        SensorSystem laneGroupMod;

        try {
            laneGroupMod = (SensorSystem)hub.getModuleRegistry().getModuleById(systemInputParam.getData().getStringValue());
        } catch (SensorHubException e) {
            return false;
        }

        for (var member : laneGroupMod.getMembers().values()) {
            if (member instanceof OccupancyVideoProcessModule) { continue; }
            try {
                member.waitForState(ModuleEvent.ModuleState.INITIALIZED, 9000);
            } catch (SensorHubException e) {
                return false;
            }
            if (member.getOutputs().containsKey(OccupancyVideo.DAILYFILE_NAME)) {
                inputData.add(DAILYFILE_NAME, member.getOutputs().get(OccupancyVideo.DAILYFILE_NAME).getRecordDescription());
            } else if (member instanceof FFMPEGSensorBase<?> ffmpegSensor) {
                outputData.add(ffmpegSensor.getUniqueIdentifier() + ":" + FileControl.SENSOR_CONTROL_NAME, ffmpegSensor.getCommandInputs().get(FileControl.SENSOR_CONTROL_NAME).getCommandDescription());
            }
            for (int i = 0; i < outputData.size(); i++) {
                var component = outputData.getComponent(i);
                fillData(component);
            }
        }
        /*
        if (!dailyFileDataStreams.isEmpty()) {
            var ds = dailyFileDataStreams.get(0);
            inputData.add(DAILYFILE_NAME, ds.getRecordStructure().clone());

            //((IStreamingDataInterface)((AbstractSensorModule)hub.getModuleRegistry().getLoadedModuleById()).getOutputs().get(OCCUPANCY)).getRecordDescription();
        }
         */
        return !inputData.isEmpty();
    }

    private void fillData(DataComponent component) {
        component.renewDataBlock();
        for (int i = 0; i < component.getComponentCount(); i++) {
            fillData(component.getComponent(i));
        }
    }

    public void initStateManager() {
        // Get systems with input UID
        /*
        var sysFilter = new SystemFilter.Builder()
                .withUniqueIDs(systemInputParam.getData().getStringValue())
                .includeMembers(true)
                .build();

        var db = hub.getDatabaseRegistry().getFederatedDatabase();

        // Get output data from input system UID
        var occupancyDataStreamQuery = db.getDataStreamStore().select(new DataStreamFilter.Builder()
                .withSystems(sysFilter)
                .withCurrentVersion()
                .withOutputNames(DAILYFILE_NAME)
                .withLimit(1)
                .build());
        var occupancyDataStreams = occupancyDataStreamQuery.collect(Collectors.toList());
        var ds = occupancyDataStreams.get(0);

         */
        String uid;
        try {
            uid = ((SensorSystem) hub.getModuleRegistry().getModuleById(systemInputParam.getData().getStringValue())).getMembers().values().stream().filter(
                    (module) -> {
                        return module.getOutputs().containsKey(DAILYFILE_NAME);
                    }
            ).findFirst().get().getUniqueIdentifier();
        } catch (SensorHubException e) { return; }

        try {
            rpmObs = hub.getSystemDriverRegistry().getDatabase(uid).getObservationStore();
        } catch (Exception ignored) {

        }

        if (uid.contains(RAP_PREFIX)) {
            stateManager = new RapiscanStateManager();
        } else {
            stateManager = new AspectStateManager();
        }

        outputData.getComponent(0).renewDataBlock();

        stateManager.addListener((from, to) -> {
            Object[] connections = outputConnections.values().toArray();
            // Start recording if leaving non-occupancy state
            if (from == StateManager.State.NON_OCCUPANCY) {
                startTime = Instant.now();
                wasAlarming = false;
                int size = outputData.size();

                for(int i = 0; i < size; i++) {
                    DataChoice fileIO = ((DataChoice)outputData.getComponent(i).getComponent(0));
                    fileIO.setSelectedItem(FileControl.CMD_OPEN_FILE);
                    var item = fileIO.getSelectedItem();
                    if (!item.hasData()) {
                        item.renewDataBlock();
                    }
                    item.getData().setStringValue(videoNamePrefix + "cam" + i + "_" + System.currentTimeMillis() + ".mp4"); // TODO Make sure this file name works, maybe add which alarm triggered
                    ((DataConnectionList)connections[i]).get(0).getSourceComponent().setData(outputData.getComponent(i).getData());
                }

                try {
                    //doPublish = true;
                    super.publishData();
                } catch (InterruptedException e) {
                    logger.error("Interrupted while publishing video file start command", e);
                }
            }

            if (to == StateManager.State.ALARMING_OCCUPANCY) {
                wasAlarming = true;
                alarmTime = System.currentTimeMillis();
            } else if (to == StateManager.State.NON_OCCUPANCY) { // End recording when entering non-occupancy state
                endTime = Instant.now();
                int size = outputData.size();
                for(int i = 0; i < size; i++) {
                    DataChoice fileIO = ((DataChoice)outputData.getComponent(i).getComponent(0));
                    fileIO.setSelectedItem(FileControl.CMD_CLOSE_FILE);
                    var fileIoItem = fileIO.getSelectedItem();

                    if (fileIoItem.getData() == null) {
                        fileIoItem.renewDataBlock();
                    }
                    fileIoItem.getData().setBooleanValue(wasAlarming); // boolean determines whether the video recording is saved
                    ((DataConnectionList)connections[i]).get(0).getSourceComponent().setData(outputData.getComponent(i).getData());
                }
                try {
                    //doPublish = true;

                    super.publishData();

                    if (wasAlarming) {
                        StringBuilder fileNames = new StringBuilder();
                        for (int i = 0; i < outputData.size(); i++) {
                            // Get the file name from the output component
                            fileNames.append(((DataChoice) outputData.getComponent(i).getComponent(0)).getComponent(FileControl.CMD_OPEN_FILE).getData().getStringValue()).append(", ");
                        }
                        fileNames.replace(fileNames.lastIndexOf(", "), fileNames.length(), "");

                        var occObs = rpmObs.select(new ObsFilter.Builder()
                                .withDataStreams(new DataStreamFilter.Builder()
                                        .withOutputNames(OCCUPANCY).build())
                                .withResultTimeDuring(startTime.minusSeconds(OBS_BUFFER_SECONDS), endTime.plusSeconds(OBS_BUFFER_SECONDS))
                                .withLimit(1)
                                .build()).toList();

                        if (occObs.isEmpty()) {
                            logger.error("SOMETHING WENT WRONG! OCCUPANCY TRIGGERED WITHOUT A CORRESPONDING OCCUPANCY RECORD!");
                        } else {
                            occObs.getFirst().getResult().setStringValue(DATA_FILE_NAME_INDEX, fileNames.toString());
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error("Interrupted while publishing video file stop command", e);
                }
            }
        });
    }

    private void writeFileName(Instant start, Instant end) {

    }

    public void addFileCloseListener(BiConsumer<Long, List<String>> listener) {
        fileCloseListener.add(listener);
    }

    public void removeFileCloseListener(BiConsumer<Long, List<String>> listener) {
        fileCloseListener.remove(listener);
    }

    /**
     * Ensure input system is a member of a database, so we can get historical records.
     * Also creates process outputs from input system's data stream outputs.
     * @return true if system has database
     */
    private boolean checkDatabaseInput() {
        /*
        // Get systems with input UID
        var sysFilter = new SystemFilter.Builder()
                .withUniqueIDs(inputSystemID)
                .includeMembers(true)
                .build();

        var db = hub.getDatabaseRegistry().getFederatedDatabase();

        db.getCommandStreamStore().select(new CommandStreamFilter.Builder().withSystems(sysFilter)
                // Only record video data in process, because we don't really use the other data
                .withControlInputNames(FileControl.SENSOR_CONTROL_NAME).build()).forEach(ds -> {
            // Don't add process data streams as outputs
            if(ds.getSystemID().getUniqueID().contains(OSHProcessInfo.URI_PREFIX))
                return;

            var struct = ds.getRecordStructure().clone();
            String fullOutputName = ds.getSystemID().getUniqueID() + ":" + ds.getControlInputName();
            try {
                outputData.get(fullOutputName);
                getLogger().warn("An existing video system with same UID exists in database");
            } catch (Exception e) {
                outputData.add(fullOutputName, struct);
            }
        });

         */
        return true;
    }

    @Override
    public void init() throws ProcessException {
        doInit();

        initStateManager();
        super.init();
    }

    /**
     * Update the occupancy state upon receiving a new daily file.
     */
    @Override
    public void execute() {
        if (rpmObs == null) {
            try {
                rpmObs = hub.getSystemDriverRegistry().getDatabase(
                        ((SensorSystem)hub.getModuleRegistry().getModuleById(systemInputParam.getData().getStringValue())).getUniqueIdentifier()
                ).getObservationStore();
                //rpmObs = hub.getSystemDriverRegistry().getDatabase(systemInputParam.getData().getStringValue()).getObservationStore();
            } catch (Exception ignored) {}
        }

        if(inputSystemID == null) {
            inputSystemID = systemInputParam.getData().getStringValue();
        }
        if (stateManager != null)
            stateManager.updateDailyFile(inputConnections.get(DAILYFILE_NAME).get(0).getDestinationComponent());
    }

    @Override
    public void publishData() throws InterruptedException {
        if (doPublish) {
            super.publishData();
        }
        doPublish = false;
    }

    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }

}
