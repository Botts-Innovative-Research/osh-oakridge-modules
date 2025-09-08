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

import com.botts.impl.process.lanevideo.config.OccupancyProcessConfig;
import com.botts.impl.process.lanevideo.helpers.ProcessHelper;
import net.opengis.OgcPropertyList;
import net.opengis.sensorml.v20.Link;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.api.utils.OshAsserts;
import org.sensorhub.impl.command.AbstractControlInterface;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.ffmpeg.controls.FileControl;
import org.vast.process.ProcessException;
import org.vast.sensorML.AggregateProcessImpl;
import org.vast.sensorML.SMLException;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SMLUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Flow;

// Based on SMLProcessImpl
public class OccupancyProcessModule extends AbstractProcessModule<OccupancyProcessConfig> {

    protected SMLUtils smlUtils;
    public AggregateProcessImpl wrapperProcess;
    OccupancyVideo alarmProcess;
    protected int errorCount = 0;
    protected boolean useThreads = true;
    String processUniqueID;
    String parentSystemUID = null;
    AbstractSensorOutput<?> rpmModule;
    Set<AbstractControlInterface<?>> ffmpegOutputs;
    Flow.Subscription subscription = null;
    String dsID = "";
    volatile boolean started = false;

    /**
     * Standalone processing module to be recognized by OSH module provider, accessible in the OSH Admin UI.
     */
    public OccupancyProcessModule()
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


    @Override
    protected void doInit() throws SensorHubException {
        parentSystemUID = config.systemUID;
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

        if (subscription == null)
            getParentHub().getEventBus().newSubscription()
                    .withTopicID(EventUtils.getDataStreamDataTopicID(dsID, OccupancyVideo.DAILYFILE_NAME))
                    .consume(this::handleEvent)
                    .thenAccept(s -> {
                        subscription = s;
                        s.request(Long.MAX_VALUE);
                    });
    }

    private void handleEvent(Event e) {
        if (started) {
            try {
                ;
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
        } catch (SMLException e) {
            throw new ProcessingException("Cannot prepare process chain for execution", e);
        } catch (ProcessException e) {
            throw new ProcessingException(e.getMessage(), e.getCause());
        }

        // advertise process inputs and outputs
        refreshIOList(processDescription.getOutputList(), outputs);

        setState(ModuleEvent.ModuleState.INITIALIZED);
    }

    public AggregateProcessImpl buildProcess() throws ProcessException, SensorHubException {

        ProcessHelper processHelper = new ProcessHelper();
        processHelper.getAggregateProcess().setUniqueIdentifier(processUniqueID);

        var sysFilter = new SystemFilter.Builder()
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

        var matchingCs = db.getCommandStreamStore().select(new CommandStreamFilter.Builder()
                .withSystems(sysFilter)
                .withCurrentVersion()
                .withControlInputNames(FileControl.SENSOR_CONTROL_NAME)
                .build());

        var dsList = matchingDs.toList();
        var csList = matchingCs.toList();
        if(dsList.size() != 1)
            throw new SensorHubException("Unable to find RPM datastream in system");
        if (csList.isEmpty())
            logger.warn("No control inputs for saving video");

        var rpmDs = dsList.get(0);

        dsID = rpmDs.getSystemID().getUniqueID();

        this.alarmProcess = new OccupancyVideo();

        OshAsserts.checkValidUID(dsID);

        processHelper.addDataSource("source0", dsID);

        alarmProcess.getParameterList().getComponent(OccupancyVideo.SYSTEM_INPUT_PARAM).getData().setStringValue(parentSystemUID);

        alarmProcess.setParentHub(getParentHub());
        alarmProcess.doInit();

        processHelper.addOutputList(alarmProcess.getOutputList());

        processHelper.addProcess("process0", alarmProcess);

        processHelper.addConnection("components/source0/outputs/" + OccupancyVideo.DAILYFILE_NAME
                ,"components/process0/inputs/" + OccupancyVideo.DAILYFILE_NAME);

        for (int i = 0; i < csList.size(); i++) {
            var uid = csList.get(i).getSystemID().getUniqueID();
            processHelper.addControlStream("video" + i, uid, FileControl.SENSOR_CONTROL_NAME);

            String outputName = uid + ":" + FileControl.SENSOR_CONTROL_NAME;
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

    /**
     * Begin processing chain
     * @throws SensorHubException if unable to start process or invalid process
     */
    @Override
    protected void doStart() throws SensorHubException
    {
        errorCount = 0;
        if (wrapperProcess == null)
            throw new ProcessingException("No valid processing chain provided");

        // start processing thread
        if (useThreads)
        {
            try {
                wrapperProcess.start(e -> {
                });
            } catch (ProcessException e) {}
            started = true;
        }
    }


    @Override
    protected void doStop()
    {
        if (wrapperProcess != null && wrapperProcess.isExecutable()) {
            started = false;
            wrapperProcess.stop();
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }
}
