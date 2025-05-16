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

package com.botts.impl.process.occupancy;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Text;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.utils.Async;
import org.vast.process.ExecutableProcessImpl;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Executable process that takes system UID as input, and outputs all driver outputs from input system, when occupancy is detected.
 */
public class OccupancyDataRecorder extends ExecutableProcessImpl implements ISensorHubProcess {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("alarmrecorder", "Alarm data recording process", null, OccupancyDataRecorder.class);
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
    private Map<String, DataEncoding> outputEncodingMap;

    public OccupancyDataRecorder() {
        super(INFO);

        fac = new RADHelper();

        // Get latest output names from RADHelper. We should do this for occupancy too, but it's not likely to change.
        gammaAlarmName = fac.createGammaAlarm().getName();
        neutronAlarmName = fac.createNeutronAlarm().getName();
        startTimeName = fac.createOccupancyStartTime().getName();
        endTimeName = fac.createOccupancyEndTime().getName();

        paramData.add(SYSTEM_INPUT_PARAM, systemInputParam = fac.createText()
                .label("Parent System Input")
                .description("Parent system (Lane) that contains one RPM datastream, typically Rapiscan or Aspect.")
                .definition(SWEHelper.getPropertyUri("System"))
                .value("")
                .build());
        outputEncodingMap = new HashMap<>();
    }

    /**
     * Get new system UID input and ensure it has data streams and is connected to a database.
     */
    @Override
    public void notifyParamChange() {
        super.notifyParamChange();
        inputSystemID = systemInputParam.getData().getStringValue();

        if(!Objects.equals(inputSystemID, "")) {
            try {
                Async.waitForCondition(this::checkDataStreamInput, 500, 10000);
                Async.waitForCondition(this::checkDatabaseInput, 500, 10000);
            } catch (TimeoutException e) {
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

        // Get systems with input UID
        var sysFilter = new SystemFilter.Builder()
                .withUniqueIDs(inputSystemID)
                .includeMembers(true)
                .build();

        var db = hub.getDatabaseRegistry().getFederatedDatabase();

        // Get output data from input system UID
        var occupancyDataStreamQuery = db.getDataStreamStore().select(new DataStreamFilter.Builder()
                        .withSystems(sysFilter)
                        .withCurrentVersion()
                        .withOutputNames(OCCUPANCY_NAME)
                        .withLimit(1)
                        .build());
        var occupancyDataStreams = occupancyDataStreamQuery.collect(Collectors.toList());
        if (!occupancyDataStreams.isEmpty())
            inputData.add(OCCUPANCY_NAME, occupancyDataStreams.get(0).getRecordStructure());

        return !inputData.isEmpty();
    }

    /**
     * Ensure input system is a member of a database, so we can get historical records.
     * Also creates process outputs from input system's data stream outputs.
     * @return true if system has database
     */
    private boolean checkDatabaseInput() {
        var db = hub.getSystemDriverRegistry().getDatabase(inputSystemID);
        if(db == null)
            return false;

        outputData.clear();
        var sysFilter = new SystemFilter.Builder()
                .withUniqueIDs(inputSystemID)
                .includeMembers(true)
                .build();

        db.getDataStreamStore().select(new DataStreamFilter.Builder().withSystems(sysFilter)
                // Only record video data in process, because we don't really use the other data
                .withObservedProperties(VideoCamHelper.DEF_VIDEOFRAME, VideoCamHelper.DEF_IMAGE).build()).forEach(ds -> {
            // Don't add process data streams as outputs
            if(ds.getSystemID().getUniqueID().contains(OSHProcessInfo.URI_PREFIX))
                return;

            var struct = ds.getRecordStructure().clone();
            var encoding = ds.getRecordEncoding();
            String fullOutputName = ds.getSystemID().getUniqueID() + ":" + ds.getOutputName();
            try {
                outputData.get(fullOutputName);
                getLogger().warn("An existing video system with same UID exists in database");
            } catch (Exception e) {
                outputData.add(fullOutputName, struct);
            }
            outputEncodingMap.put(fullOutputName, encoding);
        });

        return !outputData.isEmpty();
    }

    /**
     * Check if occupancy had gamma or neutron alarm.
     * @return true if an alarm was triggered during the latest occupancy
     */
    private boolean isTriggered() {
        DataComponent occupancyInput = inputData.getComponent(OCCUPANCY_NAME);

        DataComponent gammaAlarm = occupancyInput.getComponent(gammaAlarmName);
        DataComponent neutronAlarm = occupancyInput.getComponent(neutronAlarmName);

        return gammaAlarm.getData().getBooleanValue() || neutronAlarm.getData().getBooleanValue();
    }

    /**
     * Retrieve past data for the specified process output.
     * @param fullOutputName process output name in the format "{systemUID}:{outputName}"  (e.g. urn:osh:sensor:testsensor1:myOutput)
     * @return list of observations for time specified during occupancy startTime and endTime
     */
    private List<IObsData> getPastData(String fullOutputName) {
        int separator = fullOutputName.lastIndexOf(':');
        String systemUID = fullOutputName.substring(0, separator);
        String outputName = fullOutputName.substring(separator + 1);

        DataComponent occupancyInput = inputData.getComponent(OCCUPANCY_NAME);

        DataComponent startTime = occupancyInput.getComponent(startTimeName);
        DataComponent endTime = occupancyInput.getComponent(endTimeName);

        long startFromUTC = startTime.getData().getLongValue();
        long endFromUTC = endTime.getData().getLongValue();

        Instant start = Instant.ofEpochSecond(startFromUTC);
        // TODO: Check if EML time exists
        Instant end = Instant.ofEpochSecond(endFromUTC);

        var db = hub.getSystemDriverRegistry().getDatabase(inputSystemID);

        SystemFilter systemFilter = new SystemFilter.Builder()
                .withUniqueIDs(systemUID)
                .includeMembers(false)
                .build();
        DataStreamFilter dsFilter = new DataStreamFilter.Builder()
                .withOutputNames(outputName)
                .withSystems(systemFilter)
                .build();
        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(dsFilter)
                .withPhenomenonTimeDuring(start, end)
                .withLimit(Long.MAX_VALUE)
                .build();

        return db.getObservationStore().select(filter).collect(Collectors.toList());
    }

    /**
     * Executes whenever we receive a new occupancy.
     * If occupancy has alarm, we retrieve historical records and publish those as process outputs.
     */
    @Override
    public void execute() {
        if(inputSystemID == null) {
            inputSystemID = systemInputParam.getData().getStringValue();
        }
        if(isTriggered()) {
            System.gc();
            int size = outputData.size();
            for(int i = 0; i < size; i++) {
                DataComponent output = outputData.getComponent(i);
                String fullOutputName = output.getName();
                List<IObsData> pastData = getPastData(fullOutputName);
                for(IObsData data : pastData) {
                    output.setData(data.getResult());
                    try {
                        publishData();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                pastData = null;
            }
        }
    }

    /**
     * Get the encodings for driver outputs
     * @return map of <output name, output encoding>
     */
    public Map<String, DataEncoding> getOutputEncodingMap() {
        return outputEncodingMap;
    }

    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }

}
