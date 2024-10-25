package org.sensorhub.process.rapiscan;

import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.utils.Async;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class AlarmRecorder extends ExecutableProcessImpl implements ISensorHubProcess {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("alarmrecorder", "Alarm data recording process", null, AlarmRecorder.class);
    ISensorHub hub;
    Text systemInputParam;
    String inputSystemID;
    public static final String SYSTEM_INPUT_PARAM = "systemInput";
    public static final String OCCUPANCY_NAME = "occupancy";
    private final String gammaAlarmName;
    private final String neutronAlarmName;
    private final String startTimeName;
    private final String endTimeName;
    private final RADHelper fac;

    public AlarmRecorder() {
        super(INFO);

        fac = new RADHelper();

        gammaAlarmName = fac.createGammaAlarm().getName();
        neutronAlarmName = fac.createNeutronAlarm().getName();
        startTimeName = fac.createOccupancyStartTime().getName();
        endTimeName = fac.createOccupancyEndTime().getName();

        paramData.add(SYSTEM_INPUT_PARAM, systemInputParam = fac.createText()
                .label("Parent System Input")
                .description("Parent system (Lane) that contains one RPM datastream, typically Rapiscan or Aspect. ")
                .definition(SWEHelper.getPropertyUri("System"))
                .value("")
                .build());

    }

    @Override
    public void notifyParamChange() {
        super.notifyParamChange();
        inputSystemID = systemInputParam.getData().getStringValue();

        if(!Objects.equals(inputSystemID, "")) {
            try {
                Async.waitForCondition(this::checkDatastreamInput, 500, 10000);
                Async.waitForCondition(this::checkDatabaseInput, 500, 10000);
            } catch (TimeoutException e) {
                if(processInfo == null)
                    throw new IllegalStateException("RPM datastream " + inputSystemID + " not found", e);
                else
                    throw new IllegalStateException("RPM datastream " + inputSystemID + " has no data", e);
            }
        }
    }

    // Ensure system contains a rapiscan datastream
    private boolean checkDatastreamInput() {
        // Clear old inputs
        inputData.clear();

        // Get systems with input UID
        var sysFilter = new SystemFilter.Builder()
                .withUniqueIDs(inputSystemID)
                .includeMembers(true)
                .build();

        var db = hub.getDatabaseRegistry().getFederatedDatabase();

        // Get output data from input system UID
        var stuff = db.getDataStreamStore().select(new DataStreamFilter.Builder()
                        .withSystems(sysFilter)
                        .withCurrentVersion()
                        .withOutputNames(OCCUPANCY_NAME)
                        .withLimit(1)
                        .build());
        var colStuff = stuff.collect(Collectors.toList());
        for(var item : colStuff) {
            inputData.add(OCCUPANCY_NAME, item.getRecordStructure());
        }

        return !inputData.isEmpty();
    }

    private boolean checkDatabaseInput() {
        var db = hub.getSystemDriverRegistry().getDatabase(inputSystemID);
        if(db == null)
            return false;

        outputData.clear();
        db.getDataStreamStore().values().forEach(ds -> {
            var dsTopicId = EventUtils.getDataStreamDataTopicID(ds);
            outputData.add(dsTopicId, ds.getRecordStructure().copy());
        });

        return !outputData.isEmpty();
    }

    private boolean isTriggered() {
        DataComponent occupancyInput = inputData.getComponent(OCCUPANCY_NAME);

        DataComponent gammaAlarm = occupancyInput.getComponent(gammaAlarmName);
        DataComponent neutronAlarm = occupancyInput.getComponent(neutronAlarmName);

        if(gammaAlarm.getData().getBooleanValue() || neutronAlarm.getData().getBooleanValue()) {
            return true;
        }
        return false;
    }

    private List<IObsData> getPastData(String outputName) {
        DataComponent occupancyInput = inputData.getComponent(OCCUPANCY_NAME);

        DataComponent startTime = occupancyInput.getComponent(startTimeName);
        DataComponent endTime = occupancyInput.getComponent(endTimeName);

        long startFromUTC = startTime.getData().getLongValue();
        long endFromUTC = endTime.getData().getLongValue();

        Instant start = Instant.ofEpochSecond(startFromUTC);
        // TODO: Check if EML time exists
        Instant end = Instant.ofEpochSecond(endFromUTC);

        var db = hub.getSystemDriverRegistry().getDatabase(inputSystemID);

        DataStreamFilter dsFilter = new DataStreamFilter.Builder()
                .withOutputNames(outputName)
                .build();
        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(dsFilter)
                .withPhenomenonTimeDuring(start, end)
                .build();

        return db.getObservationStore().select(filter).collect(Collectors.toList());
    }

    @Override
    public void execute() throws ProcessException {
        if(inputSystemID == null) {
            inputSystemID = systemInputParam.getData().getStringValue();
        }
        // TODO: Use radhelper to get names of occupancy inputs such as start time, end time, alarms
        if(isTriggered()) {
            int size = outputData.size();
            for(int i = 0; i < size; i++) {
                DataComponent item = outputData.getComponent(i);
                String itemName = item.getName();

                for(IObsData data : getPastData(itemName)) {
                    item.setData(data.getResult());
                    try {
                        publishData();
                        publishData(itemName);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }
}
