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

import com.botts.impl.utils.n42.RadAlarmCategoryCodeSimpleType;
import com.google.common.util.concurrent.AtomicDouble;
import net.opengis.sensorml.v20.IOPropertyList;
import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsEvent;
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
import org.vast.process.ExecutableProcessImpl;
import org.vast.swe.SWEHelper;

import java.util.*;
import java.util.concurrent.Flow;
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
    private static final RADHelper radHelper = new RADHelper();
    public static final double DAILYFILE_INTERVAL_MS = 500;
    public static final double OCCUPANCY_INTERVAL_MS = 5000;
    public static final String SYSTEM_INPUT_PARAM = "systemInput";
    public static final OccupancyOutput occupancyOutput = new OccupancyOutput(new SensorSystem());

    public static final String OCCUPANCY_NAME = occupancyOutput.getName();
    public static final String ALARM_NAME = "alarm";
    public static final String BACKGROUND_NAME = "backgroundReport";
    public static final String FOREGROUND_NAME = "foregroundReport";
    private static final String SAMPLING_TIME_NAME = radHelper.createPrecisionTimeStamp().getName();
    private static final String DURATION_NAME = radHelper.createDuration().getName();
    private static final String ALARM_CAT_NAME = radHelper.createAlarmCatCode().getName();
    private static final String GAMMA_GROSS_COUNT_NAME = radHelper.createGammaGrossCount().getName();
    private static final String NEUTRON_GROSS_COUNT_NAME = radHelper.createNeutronGrossCount().getName();

    // All RS350 outputs. Some arrive less regularly, so we don't want to connect as input and block while waiting
    public static final List<String> ALL_OUTPUTS = List.of(ALARM_NAME, BACKGROUND_NAME, FOREGROUND_NAME);
    // Subset of RS350 outputs we want to connect to process input
    public static final List<String> RS350_OUTPUTS = List.of(FOREGROUND_NAME);
    public final List<Flow.Subscription> subscriptions = new ArrayList<>(ALL_OUTPUTS.size());
    private Occupancy.Builder occupancyBuilder;
    private IOPropertyList allInputs = new IOPropertyList();

    private int occupancyCount = 0;
    private double latestNeutronBackground = 0;
    private int maxGammaCount = 0;
    private int maxNeutronCount = 0;

    private volatile boolean doPublishOccupancy = false;
    private AtomicDouble lastAlarmTime = new AtomicDouble(0);
    private double lastDailyFileTime = 0;
    private double alarmDuration;
    private boolean reportingDailyFile = false;

    private boolean hasStarted = false;

    // Will be "cheating" a bit: subscribing to datastreams here instead of using data queues and sml process
    // TODO: When creating an occupancy based on an alarm, make SURE that the category is Radiological.
    /* Need to collect:
    private double samplingTime; AlarmOutput.getStartDateTime
    private int occupancyCount; keep a running counter here
    private double startTime; AlarmOutput.getStartDateTime
    private double endTime; AlarmOutput.samplingTime + AlarmOutput.duration
    private double neutronBackground; // Gross background neutron counts / live time.
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

        paramData.add(SYSTEM_INPUT_PARAM, systemInputParam = radHelper.createText()
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
                Async.waitForCondition(() -> checkDataStreamInput(ALL_OUTPUTS.toArray(new String[0])), 500, 10000);
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
        allInputs.clear();

        for (var subscription : subscriptions) {
            subscription.cancel();
        }
        subscriptions.clear();

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
                if (RS350_OUTPUTS.contains(dataStreamName))
                    inputData.add(dataStreamName, occupancyDataStreams.get(0).getRecordStructure().copy());

                // Separate list of all data we actually care about
                allInputs.add(dataStreamName, occupancyDataStreams.get(0).getRecordStructure().copy());

                hub.getEventBus().newSubscription(ObsEvent.class)
                        .withTopicID(EventUtils.getDataStreamDataTopicID(inputSystemID, dataStreamName))
                        .subscribe(this::processDataEvent)
                        .thenAccept(subscription -> {
                            subscriptions.add(subscription);
                            subscription.request(Long.MAX_VALUE);
                            logger.info("Started subscription to " + dataStreamName);
                        });

            }
        }

        for (var input : inputData) {
            ((DataComponent) input).renewDataBlock();
        }
        for (var input : allInputs) {
            ((DataComponent) input).renewDataBlock();
        }
        return !inputData.isEmpty();
    }

    private void processDataEvent(ObsEvent event) {
        String outputName = event.getOutputName();
        if (!ALL_OUTPUTS.contains(outputName)) {
            logger.warn("Received data event for unknown output: {}", outputName);
            return;
        }

        DataBlock[] obsResults = Arrays.stream(event.getObservations()).map(IObsData::getResult).toArray(DataBlock[]::new);

        switch (outputName) {
            case ALARM_NAME -> processAlarm(obsResults);
            case BACKGROUND_NAME -> processBackground(obsResults);
            case FOREGROUND_NAME -> processForeground(obsResults);
            default -> logger.debug("(Default) Received data event for unknown output: {}", outputName);
        }
    }

    private synchronized void processAlarm(DataBlock... eventRecords) {
        if (System.currentTimeMillis() - lastAlarmTime.get() < OCCUPANCY_INTERVAL_MS) {
            return;
        }
        lastAlarmTime.set(System.currentTimeMillis());

        DataComponent alarmComponent = ((DataComponent) allInputs.get(ALARM_NAME)).copy();

        var eventRecord = eventRecords[0];
        alarmComponent.setData(eventRecord);

        double startTime = ((Time) alarmComponent.getComponent(SAMPLING_TIME_NAME)).getValue().getAsDouble();
        double endTime = startTime + ((Quantity) alarmComponent.getComponent(DURATION_NAME)).getValue();
        alarmDuration = endTime - startTime;


        // TODO This needs to be tested
        //String alarmCat = alarmComponent.getComponent(ALARM_CAT_NAME).getData().getStringValue().toLowerCase();
        String alarmCat = RadAlarmCategoryCodeSimpleType.GAMMA.value();
        boolean hasGammaAlarm = alarmCat.contains(RadAlarmCategoryCodeSimpleType.GAMMA.value());
        boolean hasNeutronAlarm = alarmCat.contains(RadAlarmCategoryCodeSimpleType.NEUTRON.value());
        occupancyCount++;

        occupancyBuilder = new Occupancy.Builder();
        occupancyBuilder.startTime(startTime).endTime(endTime).samplingTime(startTime);
        occupancyBuilder.gammaAlarm(hasGammaAlarm).neutronAlarm(hasNeutronAlarm);
        occupancyBuilder.maxGammaCount(maxGammaCount).maxNeutronCount(maxNeutronCount);
        occupancyBuilder.neutronBackground(latestNeutronBackground);
        occupancyBuilder.occupancyCount(occupancyCount);

        setOccupancyOutput(occupancyBuilder.build());

        doPublishOccupancy = true;

    }

    // Concerned w/ max gamma and neutron counts
    private synchronized void processForeground(DataBlock... eventRecords) {
        DataComponent foregroundComponent = ((DataComponent) allInputs.get(FOREGROUND_NAME)).copy();

        for (DataBlock eventRecord : eventRecords) {
            foregroundComponent.setData(eventRecord);

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
        DataComponent backgroundComponent = ((DataComponent) allInputs.get(BACKGROUND_NAME)).copy();

        for (DataBlock eventRecord : eventRecords) {
            backgroundComponent.setData(eventRecord);

            Count grossCount = (Count) backgroundComponent.getComponent(NEUTRON_GROSS_COUNT_NAME);
            Quantity liveTime = (Quantity) backgroundComponent.getComponent(DURATION_NAME);
            latestNeutronBackground = grossCount.getValue() / liveTime.getValue();
        }
    }

    @Override
    protected void publishData() throws InterruptedException {
        publishData(OCCUPANCY_NAME);

        updateDailyFile();
    }

    private void updateDailyFile() {
        boolean isAlarming = System.currentTimeMillis() - (alarmDuration * 1000) < lastAlarmTime.get();
        boolean isDailyFileIntervalElapsed = System.currentTimeMillis() - lastDailyFileTime > DAILYFILE_INTERVAL_MS;
        // Don't need to publish dailyfile again if we have already published during this alarm (or non-alarm)
        if (isAlarming == reportingDailyFile && !isDailyFileIntervalElapsed) {
            return;
        }
        reportingDailyFile = isAlarming;

        DataComponent dailyFile = outputData.getComponent(DailyFileStruct.DAILY_FILE_NAME);
        if (!dailyFile.hasData()) {
            dailyFile.renewDataBlock();
        }
        dailyFile.getComponent(DailyFileStruct.ALARM_NAME).getData().setBooleanValue(isAlarming);
        dailyFile.getComponent(DailyFileStruct.samplingTime.getName()).getData().setDoubleValue(System.currentTimeMillis() / 1000.0);
        try {
            publishData(DailyFileStruct.DAILY_FILE_NAME);
        } catch (InterruptedException e) {
            logger.error("Error publishing daily file", e);
        }
    }

    @Override
    public void execute() {
        if(inputSystemID == null) {
            inputSystemID = systemInputParam.getData().getStringValue();
        }
        if (!hasStarted) {
            hasStarted = true;
            notifyParamChange();
        }
    }

    @Override
    public void dispose() {
        subscriptions.forEach(Flow.Subscription::cancel);
        hasStarted = false;
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
