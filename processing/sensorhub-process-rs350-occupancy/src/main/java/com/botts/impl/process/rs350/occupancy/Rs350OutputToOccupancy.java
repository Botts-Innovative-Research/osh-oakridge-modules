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

package com.botts.impl.process.rs350.occupancy;

import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.sensorhub.utils.Async;
import org.slf4j.Logger;
import org.vast.process.DataConnection;
import org.vast.process.ExecutableProcessImpl;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Executable process that takes system UID as input, and outputs all driver outputs from input system, when occupancy is detected.
 */
public class Rs350OutputToOccupancy extends ExecutableProcessImpl implements ISensorHubProcess {
    Logger logger = getLogger();
    public static final OSHProcessInfo INFO = new OSHProcessInfo("alarmrecorder", "Alarm data recording process", null, Rs350OutputToOccupancy.class);
    ISensorHub hub;
    Text systemInputParam;
    String inputSystemID;
    private static final RADHelper fac = new RADHelper();
    public static final double DAILYFILE_INTERVAL_MS = 500;
    public static final String SYSTEM_INPUT_PARAM = "systemInput";
    public static final OccupancyOutput occupancyOutput = new OccupancyOutput(new SensorSystem());

    public static final String OCCUPANCY_NAME = occupancyOutput.getName();
    public static final String ALARM_NAME = "alarm";
    public static final String BACKGROUND_NAME = "backgroundReport";
    public static final String FOREGROUND_NAME = "foregroundReport";
    private static final String DURATION_NAME = fac.createDuration().getName();
    private static final String ALARM_DESCRIPTION_NAME = fac.createAlarmDescription().getName();
    private static final String GAMMA_GROSS_COUNT_NAME = fac.createGammaGrossCount().getName();
    private static final String NEUTRON_GROSS_COUNT_NAME = fac.createNeutronGrossCount().getName();

    public static final List<String> RS350_OUTPUTS = List.of(ALARM_NAME, BACKGROUND_NAME, FOREGROUND_NAME);
    public final List<CompletableFuture<?>> completableFutures = new ArrayList<>(RS350_OUTPUTS.size());
    private Occupancy.Builder occupancyBuilder;


    private volatile boolean isAlarming = false;
    private int occupancyCount = 0;
    private double latestNeutronBackground = 0;
    private int maxGammaCount = 0;
    private int maxNeutronCount = 0;

    private volatile boolean doPublishOccupancy = false;
    private double lastDailyfileTime = 0;

    // Will be "cheating" a bit: subscribing to datastreams here instead of using data queues and sml process
    // TODO: When creating an occupancy based on an alarm, make SURE that the category is Radiological.
    /* Need to collect:
    private double samplingTime; AlarmOutput.getStartDateTime
    private int occupancyCount; keep a running counter here
    private double startTime; AlarmOutput.getStartDateTime
    private double endTime; AlarmOutput.samplingTime + AlarmOutput.duration
    private double neutronBackground; // Gross background neutron counts / live time. Use the last background output before start of foreground
    private boolean hasGammaAlarm; // Check the alarm desc for "Gamma"
    private boolean hasNeutronAlarm; // Check alarm desc for "Neturon"
    private int maxGammaCount; // Grab the foreground counts with time between start and end time and find the max
    private int maxNeutronCount;
    private List<String> adjudicatedIds = new ArrayList<>(); DON'T NEED
    private List<String> videoPaths = new ArrayList<>(); DON'T NEED
     */
    // In total, we need to ingest:
    //  1: AlarmOutput
    //  2: BackgroundOutput
    //  3: ForegroundOutput

    // Want to output: (maybe subject to change)
    //  1: Occupancy (only AFTER each alarm)
    //  2: DailyFile (only value: isAlarming boolean) (used to manage the FFmpeg recording)

    // Questions:
        // What's the best way to trigger the FFmpeg recording?
            // For now:
            // Trigger an occupancy when ForegroundOutput (publish isAlarming True)
            // Report occupancy output on AlarmOutput (publish isAlarming False)

    public Rs350OutputToOccupancy() {
        super(INFO);

        paramData.add(SYSTEM_INPUT_PARAM, systemInputParam = fac.createText()
                .label("Parent System Input")
                .description("Parent system (Lane) that contains one RPM datastream, typically Rapiscan or Aspect.")
                .definition(SWEHelper.getPropertyUri("System"))
                .value("")
                .build());

        outputData.add(OccupancyOutput.NAME, occupancyOutput.getRecordDescription());
        outputData.add(DailyFileStruct.DAILY_FILE_NAME, DailyFileStruct.getRecordDescription());
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
                Async.waitForCondition(() -> checkDataStreamInput(RS350_OUTPUTS.toArray(new String[0])), 500, 10000);
            } catch (TimeoutException e) {
                if(processInfo == null)
                    throw new IllegalStateException("RPM data stream " + inputSystemID + " not found", e);
                else
                    throw new IllegalStateException("RPM data stream " + inputSystemID + " has no data", e);
            }
        }
        for (var output : outputData) {
            ((DataComponent) output).renewDataBlock();
        }
    }

    private void setOccupancyOutput(Occupancy occupancy) {
        ((DataComponent) outputData.get(OCCUPANCY_NAME)).setData(Occupancy.fromOccupancy(occupancy));
    }

    /**
     * Ensure the input system has an RPM data stream with "occupancy" output.
     * Also adds process input as "occupancy" output, if exists.
     * @return true if occupancy output exists
     */
    private boolean checkDataStreamInput(String... dataStreamNames) {
        // Clear old inputs
        inputData.clear();

        // Get systems with input UID
        var sysFilter = new SystemFilter.Builder()
                .withUniqueIDs(inputSystemID)
                .includeMembers(true)
                .build();

        var db = hub.getDatabaseRegistry().getFederatedDatabase();

        for (String dataStreamName : dataStreamNames) {
            // Get output data from input system UID
            var occupancyDataStreamQuery = db.getDataStreamStore().select(new DataStreamFilter.Builder()
                    .withSystems(sysFilter)
                    .withCurrentVersion()
                    .withOutputNames(dataStreamName)
                    .withLimit(1)
                    .build());

            var occupancyDataStreams = occupancyDataStreamQuery.collect(Collectors.toList());
            if (!occupancyDataStreams.isEmpty()) {
                inputData.add(dataStreamName, occupancyDataStreams.get(0).getRecordStructure().copy());

                var future = hub.getEventBus().newSubscription(DataEvent.class)
                        .withTopicID(EventUtils.getDataStreamDataTopicID(inputSystemID, dataStreamName))
                        .subscribe(this::processDataEvent);

                completableFutures.add(future);
            }
        }

        for (var input : inputData) {
            ((DataComponent) input).renewDataBlock();
        }
        return !inputData.isEmpty();
    }

    private void processDataEvent(DataEvent event) {
        String outputName = event.getOutputName();
        if (!RS350_OUTPUTS.contains(outputName))
            return;

        switch (outputName) {
            case ALARM_NAME -> processAlarm(event.getRecords());
            case BACKGROUND_NAME -> processBackground(event.getRecords());
            case FOREGROUND_NAME -> processForeground(event.getRecords());
        }
    }

    private synchronized void processAlarm(DataBlock... eventRecords) {
        DataComponent alarmComponent = ((DataComponent) inputData.get(ALARM_NAME)).copy();

        for (DataBlock eventRecord : eventRecords) {
            if (!isAlarming) {
                logger.warn("Alarm before foreground!");
            }
            alarmComponent.setData(eventRecord);

            double startTime = ((Time) alarmComponent.getComponent("Sampling Time")).getValue().getAsDouble();
            double endTime = startTime + ((Quantity) alarmComponent.getComponent(DURATION_NAME)).getValue();
            String alarmDesc = alarmComponent.getComponent(ALARM_DESCRIPTION_NAME).getData().getStringValue().toLowerCase();
            boolean hasGammaAlarm = alarmDesc.contains("gamma");
            boolean hasNeutronAlarm = alarmDesc.contains("neutron");
            occupancyCount++;

            occupancyBuilder = new Occupancy.Builder();
            occupancyBuilder.startTime(startTime).endTime(endTime).samplingTime(startTime);
            occupancyBuilder.gammaAlarm(hasGammaAlarm).neutronAlarm(hasNeutronAlarm);
            occupancyBuilder.maxGammaCount(maxGammaCount).maxNeutronCount(maxNeutronCount);
            occupancyBuilder.neutronBackground(latestNeutronBackground);
            occupancyBuilder.occupancyCount(occupancyCount);

            setOccupancyOutput(occupancyBuilder.build());

            isAlarming = false;
            doPublishOccupancy = true;
        }
    }

    // Concerned w/ max gamma and neutron counts
    private synchronized void processForeground(DataBlock... eventRecords) {
        DataComponent foregroundComponent = ((DataComponent) inputData.get(FOREGROUND_NAME)).copy();

        for (DataBlock eventRecord : eventRecords) {
            foregroundComponent.setData(eventRecord);

            if (!isAlarming) {
                isAlarming = true;
            }

            int newGrossGamma = ((Count)foregroundComponent.getComponent(GAMMA_GROSS_COUNT_NAME)).getValue();
            int newGrossNeutron = ((Count)foregroundComponent.getComponent(NEUTRON_GROSS_COUNT_NAME)).getValue();

            if (newGrossGamma > maxGammaCount) {
                maxGammaCount = newGrossGamma;
            }
            if (newGrossNeutron > maxNeutronCount) {
                maxNeutronCount = newGrossNeutron;
            }
        }
    }

    // Concerned with background neutron counts
    private synchronized void processBackground(DataBlock... eventRecords) {
        DataComponent backgroundComponent = ((DataComponent) inputData.get(BACKGROUND_NAME)).copy();

        for (DataBlock eventRecord : eventRecords) {
            if (isAlarming) {
                logger.warn("Background during foreground!");
            }
            backgroundComponent.setData(eventRecord);

            Count grossCount = (Count) backgroundComponent.getComponent(NEUTRON_GROSS_COUNT_NAME);
            Quantity liveTime = (Quantity) backgroundComponent.getComponent(DURATION_NAME);
            latestNeutronBackground = grossCount.getValue() / liveTime.getValue();
        }
    }

    @Override
    protected void publishData() throws InterruptedException {
        if (doPublishOccupancy) {
            publishData(OCCUPANCY_NAME);
            doPublishOccupancy = false;
        }

        double currentTime = System.currentTimeMillis();
        if (currentTime - lastDailyfileTime > DAILYFILE_INTERVAL_MS) {
            updateDailyFile(isAlarming);
            publishData(DailyFileStruct.DAILY_FILE_NAME);
            lastDailyfileTime = currentTime;
        }
    }

    private void updateDailyFile(boolean isAlarming) {
        DataComponent dailyFile = outputData.getComponent(DailyFileStruct.DAILY_FILE_NAME);
        if (!dailyFile.hasData()) {
            dailyFile.renewDataBlock();
        }
        dailyFile.getComponent(DailyFileStruct.ALARM_NAME).getData().setBooleanValue(isAlarming);
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

    }

    @Override
    public void dispose() {
        completableFutures.forEach(future -> future.cancel(true));
    }

    /**
     * Get the encodings for driver outputs
     * @return map of <output name, output encoding>
     */
    public Map<String, DataEncoding> getOutputEncodingMap() {
        Map<String, DataEncoding> outputEncodingMap = new HashMap<>();
        outputEncodingMap.put(OCCUPANCY_NAME, occupancyOutput.getRecommendedEncoding());
        outputEncodingMap.put(DailyFileStruct.DAILY_FILE_NAME, DailyFileStruct.getRecommendedEncoding());
        return outputEncodingMap;
    }

    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }

}
