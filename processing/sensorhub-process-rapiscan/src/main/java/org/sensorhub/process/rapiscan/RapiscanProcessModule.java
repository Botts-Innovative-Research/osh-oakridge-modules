package org.sensorhub.process.rapiscan;

import net.opengis.OgcPropertyList;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
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

// Based on SMLProcessImpl
public class RapiscanProcessModule extends AbstractProcessModule<RapiscanProcessConfig> {

    protected SMLUtils smlUtils;
    public AggregateProcessImpl wrapperProcess;
    protected int errorCount = 0;
    protected boolean useThreads = true;
    String processUniqueID;


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

    public AggregateProcessImpl buildProcess() throws ProcessException, SensorHubException {
        ProcessHelper processHelper = new ProcessHelper();
        processHelper.getAggregateProcess().setUniqueIdentifier(processUniqueID);

        AlarmRecorder process = new AlarmRecorder();

        OshAsserts.checkValidUID(config.datastreamUID);

        processHelper.addDataSource("source0", config.datastreamUID);

        process.getParameterList().getComponent(AlarmRecorder.DATABASE_INPUT_PARAM).getData().setStringValue(config.databaseModuleID);
        process.getParameterList().getComponent(AlarmRecorder.DATASTREAM_INPUT_PARAM).getData().setStringValue(config.datastreamUID);

        process.setParentHub(getParentHub());
        process.notifyParamChange();

        processHelper.addOutputList(process.getOutputList());

        processHelper.addProcess("process0", process);

        processHelper.addConnection("components/source0/outputs/" + AlarmRecorder.OCCUPANCY_NAME
                ,"components/process0/inputs/" + AlarmRecorder.OCCUPANCY_NAME);

        for(AbstractSWEIdentifiable output : process.getOutputList()) {
            DataComponent component = (DataComponent) output;
            processHelper.addConnection("components/process0/outputs/" + component.getName(), "outputs/" + component.getName());
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

    protected void initChain() throws SensorHubException
    {
        //useThreads = processDescription.getInputList().isEmpty();

        // make process executable
        try
        {
            //smlUtils.makeProcessExecutable(wrapperProcess, true);
            wrapperProcess = (AggregateProcessImpl)smlUtils.getExecutableInstance((AggregateProcessImpl)processDescription, useThreads);
            wrapperProcess.setInstanceName("chain");
            wrapperProcess.setParentLogger(getLogger());
            wrapperProcess.init();
        }
        catch (SMLException e)
        {
            throw new ProcessingException("Cannot prepare process chain for execution", e);
        }
        catch (ProcessException e)
        {
            throw new ProcessingException(e.getMessage(), e.getCause());
        }

        // advertise process inputs and outputs
        refreshIOList(processDescription.getOutputList(), outputs);

        setState(ModuleEvent.ModuleState.INITIALIZED);
    }


    public AggregateProcessImpl getProcessChain()
    {
        return wrapperProcess;
    }


    protected void refreshIOList(OgcPropertyList<AbstractSWEIdentifiable> ioList, Map<String, DataComponent> ioMap) throws ProcessingException
    {
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
                outputInterfaces.put(ioName, new RapiscanOutputInterface(this, ioDesc));
            }
        }
    }


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
