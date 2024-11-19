package org.sensorhub.process.rapiscan;

import net.opengis.OgcPropertyList;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.utils.OshAsserts;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.process.rapiscan.helpers.ProcessHelper;
import org.sensorhub.process.rapiscan.helpers.RapiscanOutputInterface;
import org.vast.process.ProcessException;
import org.vast.sensorML.AggregateProcessImpl;
import org.vast.sensorML.SMLException;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SMLUtils;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Based on SMLProcessImpl
public class RapiscanProcessModule extends AbstractProcessModule<RapiscanProcessConfig> {

    protected SMLUtils smlUtils;
    public AggregateProcessImpl wrapperProcess;
    protected int errorCount = 0;
    protected boolean useThreads = true;
    String processUniqueID;
    AlarmRecorder alarmProcess;

    /**
     * Standalone processing module to be recognized by OSH module provider, accessible in the OSH Admin UI.
     */
    public RapiscanProcessModule()
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
        try {
            processUniqueID = "urn:osh:process:rapiscan:" + config.serialNumber;
            OshAsserts.checkValidUID(processUniqueID);

            processDescription = buildProcess();

            if(processDescription.getName() == null) {
                processDescription.setName(this.getName());
            }

            initChain();
        } catch (ProcessException e) {
            throw new ProcessingException("Processing error", e);
        }
    }

    /**
     * Build SensorML process description via ProcessHelper.
     * @return process chain implementation to be executed
     * @throws ProcessException if not able to read process
     * @throws SensorHubException if no RPM data streams
     */
    public AggregateProcessImpl buildProcess() throws ProcessException, SensorHubException {
        ProcessHelper processHelper = new ProcessHelper();
        processHelper.getAggregateProcess().setUniqueIdentifier(processUniqueID);

        var sysFilter = new SystemFilter.Builder()
                .withUniqueIDs(config.systemUID)
                .includeMembers(true)
                .build();

        var db = getParentHub().getDatabaseRegistry().getFederatedDatabase();
        var matchingDs = db.getDataStreamStore().select(new DataStreamFilter.Builder()
                .withSystems(sysFilter)
                .withCurrentVersion()
                .withOutputNames(AlarmRecorder.OCCUPANCY_NAME)
                .withLimit(1)
                .build());

        var dsList = matchingDs.collect(Collectors.toList());
        if(dsList.size() != 1)
            throw new SensorHubException("Unable to find RPM datastream in system");
        var rpmDs = dsList.get(0);

        String dataStreamUID = rpmDs.getSystemID().getUniqueID();

        this.alarmProcess = new AlarmRecorder();

        OshAsserts.checkValidUID(dataStreamUID);

        processHelper.addDataSource("source0", dataStreamUID);

        alarmProcess.getParameterList().getComponent(AlarmRecorder.SYSTEM_INPUT_PARAM).getData().setStringValue(config.systemUID);

        alarmProcess.setParentHub(getParentHub());
        alarmProcess.notifyParamChange();

        processHelper.addOutputList(alarmProcess.getOutputList());

        processHelper.addProcess("process0", alarmProcess);

        processHelper.addConnection("components/source0/outputs/" + AlarmRecorder.OCCUPANCY_NAME
                ,"components/process0/inputs/" + AlarmRecorder.OCCUPANCY_NAME);

        for(AbstractSWEIdentifiable systemOutput : alarmProcess.getOutputList()) {
            DataComponent systemComponent = (DataComponent) systemOutput;
            if(systemComponent.getComponentCount() > 0)
                processHelper.addConnection("components/process0/outputs/" + systemComponent.getName(),
                        "outputs/" + systemComponent.getName());
        }

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            smlUtils.writeProcess(os, processHelper.getAggregateProcess(), true);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            return (AggregateProcessImpl) smlUtils.readProcess(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void  initChain() throws SensorHubException {
        // make process executable
        try {
            //smlUtils.makeProcessExecutable(wrapperProcess, true);
            wrapperProcess = (AggregateProcessImpl)smlUtils.getExecutableInstance((AggregateProcessImpl)processDescription, useThreads);
            wrapperProcess.setInstanceName("chain");
            wrapperProcess.setParentLogger(getLogger());
            wrapperProcess.init();
        } catch (SMLException e) {
            throw new ProcessingException("Cannot prepare process chain for execution", e);
        } catch (ProcessException e) {
            throw new ProcessingException(e.getMessage(), e.getCause());
        }

        // advertise process inputs and outputs
        refreshIOList(processDescription.getOutputList(), outputs, alarmProcess.getOutputEncodingMap());

        setState(ModuleEvent.ModuleState.INITIALIZED);
    }


    public AggregateProcessImpl getProcessChain()
    {
        return wrapperProcess;
    }


    /**
     * Add output interface connections
     * @param ioList list of all process components
     * @param ioMap map of all inputs/outputs
     * @param encodingMap map of data encodings
     * @throws ProcessingException if unable to get IO component
     */
    protected void refreshIOList(OgcPropertyList<AbstractSWEIdentifiable> ioList, Map<String, DataComponent> ioMap, Map<String, DataEncoding> encodingMap) throws ProcessingException {
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
            DataEncoding ioEncoding = encodingMap.get(ioName);
            ioMap.put(ioName, ioComponent);

            if(ioMap == inputs) {
                // TODO set control interface
            } else if(ioMap == parameters) {
                // TODO set control interfaces
            } else if(ioMap == outputs) {
                outputInterfaces.put(ioName, new RapiscanOutputInterface(this, ioDesc, ioEncoding));
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
            try
            {
                wrapperProcess.start(e-> {
                    reportError("Error while executing process chain", e);
                });
            }
            catch (ProcessException e)
            {
                throw new ProcessingException("Cannot start process chain thread", e);
            }
        }
    }


    @Override
    protected void doStop()
    {
        if (wrapperProcess != null && wrapperProcess.isExecutable())
            wrapperProcess.stop();
    }

}
