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

import com.botts.impl.process.lanevideo.config.OccupancyVideoProcessConfig;
import com.botts.impl.process.lanevideo.helpers.ProcessHelper;
import net.opengis.OgcPropertyList;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataStream;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.api.utils.OshAsserts;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.controls.FileControl;
import org.sensorhub.impl.system.wrapper.AggregateProcessWrapper;
import org.sensorhub.utils.Async;
import org.vast.process.ExecutableChainImpl;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.sensorML.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

// Based on SMLProcessImpl
public class OccupancyVideoProcessModule extends AbstractProcessModule<OccupancyVideoProcessConfig> {

    protected SMLUtils smlUtils;
    public AggregateProcessImpl wrapperProcess;
    volatile OccupancyVideo alarmProcess;
    protected int errorCount = 0;
    protected boolean useThreads = true;
    String processUniqueID;
    String parentSystemUID = null;
    String videoDirectory = null;
    AbstractSensorOutput<?> rpmModule;
    Set<AbstractControlInterface<?>> ffmpegOutputs;
    Flow.Subscription dataSubscription = null;
    Flow.Subscription commandSubscription = null;
    String dsID = "";
    List<String> csIDs = new ArrayList<>();
    FeatureId occDsID;
    //List<ICommandStreamInfo> csList;
    DataComponent occDataComponent;
    volatile boolean started = false;
    SystemFilter sysFilter = new SystemFilter.Builder().build();

    /**
     * Standalone processing module to be recognized by OSH module provider, accessible in the OSH Admin UI.
     */
    public OccupancyVideoProcessModule()
    {
        wrapperProcess = new AggregateProcessImpl();
        wrapperProcess.setUniqueIdentifier(UUID.randomUUID().toString());
        initAsync = true;
    }


    @Override
    public void setParentHub(ISensorHub hub)
    {
        super.setParentHub(hub);
        smlUtils = new SMLUtils(SMLUtils.V2_0);
        smlUtils.setProcessFactory(hub.getProcessingManager());
    }

    protected void moduleInit() throws SensorHubException {
        parentSystemUID = config.systemUID;
        videoDirectory = config.recordingConfig.directory;
        if (parentSystemUID == null || parentSystemUID.isBlank()) {
            if(getParentSystem() == null || getParentSystemUID() == null || getParentSystemUID().isBlank())
                throw new SensorHubException("Please specify a system UID, or put process under parent system.");
            parentSystemUID = getParentSystemUID();
        }

        try {
            processUniqueID = OSHProcessInfo.URI_PREFIX +  "occupancy:" + config.serialNumber;
            OshAsserts.checkValidUID(processUniqueID);

            processDescription = buildProcess();

            if(processDescription.getName() == null) {
                processDescription.setName(this.getName());
            }

            initChain();
        } catch (ProcessException e) {
            throw new ProcessingException("Processing error", e);
        }

        if (dataSubscription == null) {
            getParentHub().getEventBus().newSubscription()
                    .withTopicID(EventUtils.getDataStreamDataTopicID(dsID, OccupancyVideo.DAILYFILE_NAME))
                    .consume(this::handleEvent)
                    .thenAccept(s -> {
                        dataSubscription = s;
                        s.request(Long.MAX_VALUE);
                    });
        }


        if (commandSubscription == null) {
            getParentHub().getEventBus().newSubscription()
                    .withTopicIDs(() -> csIDs.iterator())
                    .consume((Event e) -> {})
                    .thenAccept(s -> {
                        dataSubscription = s;
                        s.request(Long.MAX_VALUE);
                    });
        }


    }

    private void handleEvent(Event e) {
        if (false/*started*/) {
            try {
                wrapperProcess.run();

            } catch (Exception ignored) {}
        }
    }

    protected void  initChain() throws SensorHubException {
        // make process executable
        try {
            //smlUtils.makeProcessExecutable(wrapperProcess, true);
            wrapperProcess = (AggregateProcessImpl)smlUtils.getExecutableInstance((AggregateProcessImpl)processDescription, false);
            wrapperProcess.setInstanceName("chain");
            wrapperProcess.setParentLogger(getLogger());
            wrapperProcess.init();
            ((AbstractProcessImpl)wrapperProcess.getComponentList().get(1)).setExecutableImpl(alarmProcess);
        } catch (SMLException e) {
            throw new ProcessingException("Cannot prepare process chain for execution", e);
        } catch (ProcessException e) {
            throw new ProcessingException(e.getMessage(), e.getCause());
        }

        // advertise process inputs and outputs
        refreshIOList(processDescription.getOutputList(), outputs);
    }

    public AggregateProcessImpl buildProcess() throws ProcessException, SensorHubException {

        dsID = "";
        csIDs = new ArrayList<>();

        ProcessHelper processHelper = new ProcessHelper();
        processHelper.getAggregateProcess().setUniqueIdentifier(processUniqueID);

        SensorSystem laneGroupMod = (SensorSystem) getParentSystem();
        parentSystemUID = laneGroupMod.getLocalID();

        for (var member : laneGroupMod.getMembers().values()) {
            if (member instanceof OccupancyVideoProcessModule) { continue; }
            member.waitForState(ModuleEvent.ModuleState.INITIALIZED, 20000);
            if (member.getOutputs().containsKey(OccupancyVideo.OCCUPANCY)) {
                dsID = member.getUniqueIdentifier();
                occDataComponent = member.getOutputs().get(OccupancyVideo.OCCUPANCY).getRecordDescription();
            } else if (member instanceof FFMPEGSensorBase<?>) {
                csIDs.add(member.getUniqueIdentifier());
            }
        }
        /*
         sysFilter = new SystemFilter.Builder()
                .withUniqueIDs(parentSystemUID)
                .includeMembers(true)
                .build();

        var db = getParentHub().getDatabaseRegistry().getFederatedDatabase();
        var matchingDs = db.getDataStreamStore().select(new DataStreamFilter.Builder()
                .withSystems(sysFilter)
                .withCurrentVersion()
                .withOutputNames(OccupancyVideo.DAILYFILE_NAME)
                .withLimit(1)
                .build());

        var occupancyDs = db.getDataStreamStore().select(new DataStreamFilter.Builder()
                .withSystems(sysFilter)
                .withCurrentVersion()
                .withOutputNames(OccupancyVideo.OCCUPANCY)
                .withLimit(1)
                .build());

        var matchingCs = db.getCommandStreamStore().select(new CommandStreamFilter.Builder()
                .withSystems(sysFilter)
                .withCurrentVersion()
                .withControlInputNames(FileControl.SENSOR_CONTROL_NAME)
                .build());



        var dsList = matchingDs.toList();
        var occList = occupancyDs.toList();

        csList = matchingCs.toList();

         */
        if(laneGroupMod.getMembers().isEmpty()) {
            throw new SensorHubException("Unable to find RPM datastream in system");
        }
        /*
        if (csList.isEmpty())
            logger.warn("No control inputs for saving video");

         */

        this.alarmProcess = new OccupancyVideo();
        //alarmProcess.addFileCloseListener(this::handleFileClose);

        OshAsserts.checkValidUID(dsID);

        processHelper.addDataSource("source0", dsID);

        alarmProcess.getParameterList().getComponent(OccupancyVideo.SYSTEM_INPUT_PARAM).getData().setStringValue(parentSystemUID);
        alarmProcess.getParameterList().getComponent(OccupancyVideo.VIDEO_PREFIX).getData().setStringValue(videoDirectory);

        alarmProcess.setParentHub(getParentHub());
        alarmProcess.doInit();

        processHelper.addOutputList(alarmProcess.getOutputList());

        processHelper.addProcess("process0", alarmProcess);

        processHelper.addConnection("components/source0/outputs/" + OccupancyVideo.DAILYFILE_NAME
                ,"components/process0/inputs/" + OccupancyVideo.DAILYFILE_NAME);

        for (int i = 0; i < csIDs.size(); i++) {
            processHelper.addControlStream("video" + i, csIDs.get(i), FileControl.SENSOR_CONTROL_NAME);

            String outputName = csIDs.get(i) + ":" + FileControl.SENSOR_CONTROL_NAME;
            processHelper.addConnection("components/process0/outputs/" + outputName /*+ "/" + FileControl.FILE_IO*/,
                    "components/video" + i + "/inputs/" + FileControl.SENSOR_CONTROL_NAME /*+ "/" + FileControl.FILE_IO*/);

        }

        try {
            processHelper.writeXML(System.out);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            smlUtils.writeProcess(os, processHelper.getAggregateProcess(), true);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            return (AggregateProcessImpl) smlUtils.readProcess(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleFileClose(Long alarmTime, List<String> fileNames) {
        AtomicReference<Instant> resultTime = new AtomicReference<>(Instant.now());

        var obs = getParentHub().getEventBus().newSubscription()
                .withTopicID(EventUtils.getDataStreamDataTopicID(occDsID.getUniqueID(), OccupancyVideo.OCCUPANCY))
                .withFilter((event) -> event.getTimeStamp() > alarmTime)
                .withEventType(DataEvent.class)
                .consume((event) -> {
                    if (event instanceof DataEvent dataEvent) {
                        resultTime.set(dataEvent.getResultTime());
                        var opt = Arrays.stream(dataEvent.getRecords()).findFirst();
                        if (opt.isEmpty()) { return; }

                        occDataComponent.setData(opt.get());
                        var fileData = occDataComponent.getComponent("placeHolder"); // TODO This is a placeholder for testing
                        fileData.renewDataBlock();
                        fileData.getData().resize(fileNames.size()); // Resize file output to number of connected cameras.
                        for (int i = 0; i < fileNames.size(); i++) {
                            fileData.getData().setStringValue(i, fileNames.get(i));
                        }
                    }
                })
                .thenAccept(s -> {
                    s.request(1);
                });
        obs.join();

        this.occDataComponent.setData(
            getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                    .withSystems(sysFilter)
                    .withDataStreams(occDsID.getInternalID())
                    .withResultTime(new TemporalFilter.Builder().withSingleValue(resultTime.get()).build())
                    .withLimit(1)
                    .build()).toList().getFirst().getResult()
        );

        var fileData = occDataComponent.getComponent("placeHolder");
        if (fileData != null && fileData.getData() != null && fileData.getData().getStringValue() == null) {
            fileData.getData().resize(fileNames.size());
            for (int i = 0; i < fileNames.size(); i++) {
                fileData.getData().setStringValue(i, fileNames.get(i));
            }
        }

    }

    /**
     * Add output interface connections
     * @param ioList list of all process components
     * @param ioMap map of all inputs/outputs
     * @throws ProcessingException if unable to get IO component
     */
    protected void refreshIOList(OgcPropertyList<AbstractSWEIdentifiable> ioList, Map<String, DataComponent> ioMap) throws ProcessingException {
        ioMap.clear();
        if (ioMap == inputs)
            controlInterfaces.clear();
        else if (ioMap == outputs)
            outputInterfaces.clear();

        int numSignals = ioList.size();
        for (int i=0; i<numSignals; i++)
        {
            String ioName = ioList.getProperty(i).getName();
            AbstractSWEIdentifiable ioDesc = ioList.get(i);

            DataComponent ioComponent = SMLHelper.getIOComponent(ioDesc);
            ioMap.put(ioName, ioComponent);

            if(ioMap == inputs) {
                // TODO set control interface
            } else if(ioMap == parameters) {
                // TODO set control interfaces
            } else if(ioMap == outputs) {
                //outputInterfaces.put(ioName, new OccupancyDataOutputInterface(this, ioDesc, ioEncoding));
                // TODO Replace ^
            }
        }
    }

    @Override
    protected void doInit() throws SensorHubException
    {

        moduleInit();
        setState(ModuleEvent.ModuleState.INITIALIZED);
    }

    /**
     * Begin processing chain
     * @throws SensorHubException if unable to start process or invalid process
     */

    @Override
    protected void doStart() throws SensorHubException
    {
        //moduleInit();
        errorCount = 0;
        if (wrapperProcess == null)
            throw new ProcessingException("No valid processing chain provided");

        // start processing thread
        if (useThreads)
        {
            try {
                //((AbstractProcessImpl)wrapperProcess.getComponentList().get(1)).setExecutableImpl(alarmProcess);
                wrapperProcess.start((error)->{});
                //((AbstractProcessImpl)wrapperProcess.getComponentList().get(0)).start(e -> {}); // Start the data stream, don't need to start the others
                //alarmProcess.addFileCloseListener(this::handleFileClose);
                //wrapperProcess.start(e -> {});
            } catch (Exception e) {}
            started = true;
        }
    }


    @Override
    protected void doStop()
    {
        if (wrapperProcess != null && wrapperProcess.isExecutable()) {
            ((AbstractProcessImpl)wrapperProcess.getComponentList().get(0)).stop();
            started = false;
            wrapperProcess.stop();
            //alarmProcess.removeFileCloseListener(this::handleFileClose);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        if (dataSubscription != null) {
            dataSubscription.cancel();
            dataSubscription = null;
        }
        if (commandSubscription != null) {
            commandSubscription.cancel();
            commandSubscription = null;
        }
    }
}
