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
    Text dbInputParam;
    String inputDatabaseID;
    public static final String DATABASE_INPUT_PARAM = "databaseInput";
    Text dsInputParam;
    String inputDatastreamID;
    public static final String DATASTREAM_INPUT_PARAM = "datastreamInput";
    // TODO: Possibly replace occupancy name with string from RADHelper
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

        paramData.add(DATASTREAM_INPUT_PARAM, dsInputParam = fac.createText()
                .label("Rapiscan Datastream Input")
                .description("Rapiscan datastream to use occupancy data")
                .definition(SWEHelper.getPropertyUri("Datastream"))
                .value("")
                .build());

        paramData.add(DATABASE_INPUT_PARAM, dbInputParam = fac.createText()
                .label("Database Input")
                .description("Database to query historical results")
                .definition(SWEHelper.getPropertyUri("Database"))
                .value("")
                .build());
    }

    @Override
    public void notifyParamChange() {
        super.notifyParamChange();
        populateIDs();

        if(!Objects.equals(inputDatastreamID, "") && !Objects.equals(inputDatabaseID, "")) {
            try {
                Async.waitForCondition(this::checkDatastreamInput, 500, 10000);
                Async.waitForCondition(this::checkDatabaseInput, 500, 10000);
            } catch (TimeoutException e) {
                if(processInfo == null) {
                    throw new IllegalStateException("Rapiscan datastream " + inputDatastreamID + " not found", e);
                } else {
                    throw new IllegalStateException("Rapiscan datastream " + inputDatastreamID + " has no data", e);
                }
            }
        }
    }

    private void populateIDs() {
        inputDatabaseID = dbInputParam.getData().getStringValue();
        inputDatastreamID = dsInputParam.getData().getStringValue();
    }

    private boolean checkDatastreamInput() {
        // Clear old inputs
        inputData.clear();

        // Get systems with input UID
        var sysFilter = new SystemFilter.Builder()
                .withUniqueIDs(inputDatastreamID)
                .build();

        var db = hub.getDatabaseRegistry().getFederatedDatabase();

        // Get output data from input system UID
        db.getDataStreamStore().select(new DataStreamFilter.Builder()
                        .withSystems(sysFilter)
                        .withCurrentVersion()
                        .withOutputNames(OCCUPANCY_NAME)
                        .build())
                .forEach(ds -> {
                    inputData.add(OCCUPANCY_NAME, ds.getRecordStructure());
                });

        return !inputData.isEmpty();
    }

    private boolean checkDatabaseInput() {
        var db = hub.getDatabaseRegistry().getObsDatabaseByModuleID(inputDatabaseID);
        if(db == null) {
            return false;
        }

        outputData.clear();
        db.getDataStreamStore().values().forEach(ds -> {
            outputData.add(ds.getOutputName(), ds.getRecordStructure().copy());
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
        Instant end = Instant.ofEpochSecond(endFromUTC);

        IObsSystemDatabase inputDB = hub.getDatabaseRegistry().getObsDatabaseByModuleID(inputDatabaseID);

        DataStreamFilter dsFilter = new DataStreamFilter.Builder()
                .withOutputNames(outputName)
                .build();
        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(dsFilter)
                .withPhenomenonTimeDuring(start, end)
                .build();

        return inputDB.getObservationStore().select(filter).collect(Collectors.toList());
    }

    @Override
    public void execute() throws ProcessException {
        if(inputDatabaseID == null || inputDatastreamID == null) {
            populateIDs();
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
