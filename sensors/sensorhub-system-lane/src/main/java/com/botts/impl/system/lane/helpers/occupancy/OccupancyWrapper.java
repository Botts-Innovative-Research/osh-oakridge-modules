package com.botts.impl.system.lane.helpers.occupancy;

import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.helpers.occupancy.state.*;
import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.controls.FileControl;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.sensorhub.utils.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Asserts;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeoutException;

public class OccupancyWrapper {
    public static final String OCCUPANCY = "occupancy";
    //public static final String VIDEO_FILE_OUTPUT = FileOutput.outputName;
    public static final String DAILYFILE_NAME = "dailyFile";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyWrapper.class);
    private List<FFMPEGSensorBase<?>> cameras = new ArrayList<>();
    private AbstractSensorModule<?> rpm;
    private volatile StateManager stateManager;
    private final ObservationHelper observationHelper = new ObservationHelper();
    static final int OBS_BUFFER_SECONDS = 1;
    static final int DATA_FILE_COUNT_INDEX = 11;
    static final int DATA_FILE_NAME_INDEX = 12;
    static final int GAMMA_INDEX = 5;
    static final int NEUTRON_INDEX = 6;
    Instant startTime = Instant.now();
    Instant endTime = Instant.now();
    ISensorHub hub;
    Text systemInputParam, videoPrefixParam;
    String inputSystemClass;
    boolean doPublish = false;
    boolean wasAlarming = false;
    long alarmTime;
    IObsStore rpmObs;
    Flow.Subscription dailyFileSubscription;
    final Map<FFMPEGSensorBase<?>, Flow.Subscription> cameraSubscriptions = Collections.synchronizedMap(new HashMap<>());
    Flow.Subscription occupancySubscription;
    List<String> fileNames = new ArrayList<>();
    private long cmdId = 0;

    public OccupancyWrapper(ISensorHub hub) {
        this.hub = hub;
    }

    public OccupancyWrapper(ISensorHub hub, AbstractSensorModule<?> rpm) {
        this(hub);
        setRpmSensor(rpm);
    }

    public OccupancyWrapper(ISensorHub hub, AbstractSensorModule<?> rpm, FFMPEGSensorBase<?>... cameras) {
        this(hub);
        init(rpm, cameras);
    }

    public OccupancyWrapper(ISensorHub hub, AbstractSensorModule<?> rpm, List<FFMPEGSensorBase<?>> cameras) {
        this(hub);
        init(rpm, cameras);
    }

    public void init(AbstractSensorModule<?> rpm, FFMPEGSensorBase<?>... cameras) {
        setRpmSensor(rpm);
        setFFmpegSensors(cameras);
        //registerStateListener();
    }

    public void init(AbstractSensorModule<?> rpm, List<FFMPEGSensorBase<?>> cameras) {
        setRpmSensor(rpm);
        setFFmpegSensors(cameras);
    }

    public void start() {
        if (!isInitialized()) {
            logger.warn("Cannot start; init this object first!");
            return;
        }

        registerStateListener();
        registerDailyFileListener();

        try {
            var occOut = rpm.getOutputs().values().stream().filter((predicate) -> predicate instanceof OccupancyOutput).findFirst();
            if (occOut.isPresent()) {
                observationHelper.setOccupancyOutput((OccupancyOutput<?>) occOut.get());
            } else {
                logger.error("Could not find occupancy output; cannot add video paths to occupancy observations.");
            }
        } catch (Exception ignored) {
        }
    }

    public void stop() {
        if (dailyFileSubscription != null) {
            dailyFileSubscription.cancel();
        }
        dailyFileSubscription = null;

        if (occupancySubscription != null) {
            occupancySubscription.cancel();
        }
        occupancySubscription = null;

        for (var subscription : cameraSubscriptions.values()) {
            subscription.cancel();
        }
        cameraSubscriptions.clear();
    }

    public void registerDailyFileListener() {
        hub.getEventBus().newSubscription().withEventType(ObsEvent.class)
                .withTopicID(EventUtils.getDataStreamDataTopicID(rpm.getUniqueIdentifier(), DAILYFILE_NAME))
                .subscribe((event) -> {
                    ObsEvent obsEvent = (ObsEvent) event;
                    var record = rpm.getOutputs().get(DAILYFILE_NAME).getRecordDescription();
                    var observations = obsEvent.getObservations();

                    for (var obs : observations) {
                        record.setData(obs.getResult());
                        stateManager.updateDailyFile(record);
                    }
                }).thenAccept(subscription -> {
                    this.dailyFileSubscription = subscription;
                    subscription.request(Long.MAX_VALUE);
                    logger.info("Started subscription to rpm dailyfile event.");
                });
    }

    private void registerStateListener() {
        if (stateManager == null) {
            if (rpm == null) {
                logger.error("State manager is null. Add an rpm before setting the state listener.");
                return;
            } else {
                setStateManager(rpm);
            }
        }
        stateManager.clearListeners();

        stateManager.addListener((from, to) -> {
            // Start recording if leaving non-occupancy state
            if (from == StateManager.State.NON_OCCUPANCY) {
                startTime = Instant.now();
                wasAlarming = false;
                int size = cameras.size();
                fileNames = new ArrayList<>(size);
                //observationHelper.clear();

                for(int i = 0; i < size; i++) {
                    IStreamingControlInterface commandInterface = cameras.get(i).getCommandInputs().values().stream().findFirst().get();
                    DataComponent command = commandInterface.getCommandDescription().clone();
                    command.renewDataBlock();
                    DataChoice fileIO = (DataChoice) command.getComponent(0);

                    fileIO.setSelectedItem(FileControl.CMD_OPEN_FILE);
                    var item = fileIO.getSelectedItem();
                    if (!item.hasData()) {
                        item.renewDataBlock();
                    }

                    String fileName = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS").withLocale(Locale.US).withZone(ZoneId.systemDefault()).format(startTime) + ".mp4";
                    item.getData().setStringValue(fileName); // TODO Make sure this file name works, maybe add which alarm triggered
                    fileNames.add(fileName);
                    commandInterface.submitCommand(new CommandData(++cmdId, command.getData()));
                }
            }

            if (to == StateManager.State.ALARMING_OCCUPANCY) {
                wasAlarming = true;
                alarmTime = System.currentTimeMillis();
            } else if (to == StateManager.State.NON_OCCUPANCY) { // End recording when entering non-occupancy state
                endTime = Instant.now();
                int size = cameras.size();
                for (int i = 0; i < size; i++) {
                    IStreamingControlInterface commandInterface = cameras.get(i).getCommandInputs().values().stream().findFirst().get();
                    DataComponent command = commandInterface.getCommandDescription().clone();
                    command.renewDataBlock();
                    DataChoice fileIO = (DataChoice) command.getComponent(0);
                    fileIO.setSelectedItem(FileControl.CMD_CLOSE_FILE);
                    var fileIoItem = fileIO.getSelectedItem();

                    if (fileIoItem.getData() == null) {
                        fileIoItem.renewDataBlock();
                    }

                    fileIoItem.getData().setBooleanValue(wasAlarming); // boolean determines whether the video recording is saved
                    commandInterface.submitCommand(new CommandData(++cmdId, command.getData())).whenComplete((result, error) -> {
                        for (var obs : result.getResult().getInlineRecords()) {
                            this.observationHelper.addFfmpegOut(obs.getStringValue(), size);
                        }
                    });
                }

                //doPublish = true;
                /*
                if (wasAlarming) {
                    StringBuilder csvFileNames = new StringBuilder();
                    for (int i = 0; i < fileNames.size(); i++) {
                        // Get the file name from the output component
                        csvFileNames.append(fileNames.get(i)).append(", ");
                    }
                    csvFileNames.replace(csvFileNames.lastIndexOf(", "), csvFileNames.length(), "");

                    if (rpmObs == null) {
                        logger.warn("Rpm observation store is null; could not write file name to occupancy.");
                    } else {


                        var occObs = rpmObs.select(new ObsFilter.Builder()
                                .withResultTimeDuring(startTime.minusSeconds(OBS_BUFFER_SECONDS), endTime.plusSeconds(OBS_BUFFER_SECONDS))
                                .withValuePredicate(obsData -> {
                                    obsData.getResult().setBooleanValue(););
                                    return true;
                                })
                                .withLimit(1)
                                .build()).toList();



                        if (occObs.isEmpty()) {
                            logger.error("SOMETHING WENT WRONG! OCCUPANCY TRIGGERED WITHOUT A CORRESPONDING OCCUPANCY RECORD!");
                        } else {
                            occObs.getFirst().getResult().setStringValue(DATA_FILE_NAME_INDEX, csvFileNames.toString());
                        }

                    }
                }

                 */
            }
        });
    }

    public boolean isInitialized() {
        return (rpm != null) && (cameras != null) && (!cameras.isEmpty());
    }

    public void setFFmpegSensors(FFMPEGSensorBase<?>... sensors) {
        cameras = Arrays.stream(sensors).toList();
    }

    public void setFFmpegSensors(List<FFMPEGSensorBase<?>> sensors) {
        cameras.addAll(sensors);
    }

    public void addFFmpegSensor(FFMPEGSensorBase<?> sensor) {
        if (!cameras.contains(sensor)) {
            cameras.add(sensor);
        }
    }

    public void setRpmSensor(AbstractSensorModule<?> sensor) {
        Asserts.checkNotNull(sensor);
        // TODO For now, assuming the added sensor is an rpm. Need to figure out the correct way to check.
        if (setStateManager(sensor)) {
            rpm = sensor;
        }
    }

    private boolean setStateManager(AbstractSensorModule<?> sensor) {
        if (sensor instanceof AspectSensor) {
            stateManager = new AspectStateManager();
        } else if (sensor instanceof RapiscanSensor) {
            stateManager = new RapiscanStateManager();
        } else {
            logger.error("Could not determine RPM type from provided module.");
            return false;
        }
        return true;
    }

    public void removeRpmSensor() {
        if (dailyFileSubscription != null) {
            dailyFileSubscription.cancel();
        }
        if (occupancySubscription != null) {
            occupancySubscription.cancel();
        }
        rpm = null;
        rpmObs = null;
        stateManager = null;
    }

    public void removeFFmpegSensor(FFMPEGSensorBase<?> sensor) {
        if (cameras.contains(sensor)) {
            cameras.remove(sensor);
            if (cameraSubscriptions.containsKey(sensor)) {
                cameraSubscriptions.get(sensor).cancel();
            }
        }
    }

    public void clearFFmpegSensors() {
        cameras.clear();
        for (var subscription : cameraSubscriptions.values()) {
            subscription.cancel();
        }
        cameraSubscriptions.clear();
    }

    private static class ObservationHelper {
        private final ArrayList<String> ffmpegOuts = new ArrayList<>();
        private OccupancyOutput<?> occupancyOutput;
        private final Object lock = new Object();
        private int totalCams = 2;
        private final static int FILE_TIMEOUT = 1000;

        public void addFfmpegOut(String videoFile, int totalCams) {
            synchronized (lock) {
                this.ffmpegOuts.add(videoFile);
                this.totalCams = totalCams;
            }
        }

        public void setOccupancyOutput(OccupancyOutput<?> occupancyOutput) {
            synchronized (lock) {
                if (this.occupancyOutput != null)
                    this.occupancyOutput.removeOccupancyCallback(this::occupancyCallback);

                this.occupancyOutput = occupancyOutput;
                this.occupancyOutput.addOccupancyCallback(this::occupancyCallback);
            }
        }

        private void occupancyCallback(Occupancy occupancy) {
            try {
                Async.waitForCondition(() -> ffmpegOuts.size() >= totalCams, FILE_TIMEOUT);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }

            for (String path : ffmpegOuts) {
                occupancy.addVideoPath(path);
            }
            clear();
        }

        // Called when transitioning to any occupancy state (from non-occupancy)
        private void clear() {
            ffmpegOuts.clear();
            totalCams = 0;
        }
    }
}