package com.botts.impl.system.lane.helpers.occupancy;

import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.helpers.occupancy.state.*;
import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.datastore.obs.IObsStore;
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
    private final List<FFMPEGSensorBase<?>> cameras = new ArrayList<>();
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
    //final Map<FFMPEGSensorBase<?>, Flow.Subscription> cameraSubscriptions = Collections.synchronizedMap(new HashMap<>());
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
                observationHelper.notifyOccupancyEnd();

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
                        if (this.observationHelper.isAccepting()) {
                            if (result.getStatusCode() != ICommandStatus.CommandStatusCode.ACCEPTED) {
                                this.observationHelper.reportFfmpegFailure();
                            } else {
                                for (var obs : result.getResult().getInlineRecords()) {
                                    this.observationHelper.addFfmpegOut(obs.getStringValue(), size);
                                }
                            }
                        }
                    });

                }
                wasAlarming = false;
            }
        });
    }

    public boolean isInitialized() {
        return (rpm != null);
    }

    public void setFFmpegSensors(FFMPEGSensorBase<?>... sensors) {
        cameras.addAll(Arrays.stream(sensors).toList());
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
        }
    }

    public void clearFFmpegSensors() {
        cameras.clear();
    }



    private static class ObservationHelper {
        private final ArrayList<String> ffmpegOuts = new ArrayList<>();
        private OccupancyOutput<?> occupancyOutput;
        private final Object lock = new Object();
        private int totalCams = 2;
        private int missingCams = 0;
        private final static int FILE_TIMEOUT = 2000;
        private volatile boolean isAccepting = false;

        public void notifyOccupancyEnd() {
            isAccepting = true;
        }

        public boolean isAccepting() {
            return isAccepting;
        }

        public void addFfmpegOut(String videoFile, int totalCams) {
            if (isAccepting) {
                synchronized (lock) {
                    if (this.ffmpegOuts.size() > this.totalCams - this.missingCams) // Might have files from prev occupancy
                        ffmpegOuts.clear();
                    this.ffmpegOuts.add(videoFile);
                    this.totalCams = totalCams;
                }
            }
        }

        public void reportFfmpegFailure() {
            if (isAccepting)
                missingCams++;
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
            if (occupancy.hasGammaAlarm() || occupancy.hasNeutronAlarm()) {
                try {
                    Async.waitForCondition(() -> (ffmpegOuts.size() - missingCams) >= totalCams, FILE_TIMEOUT);
                } catch (TimeoutException e) {
                    logger.error("Failed to get video files", e);
                }

                for (String path : ffmpegOuts) {
                    occupancy.addVideoPath(path);
                }
            }
            clear();
        }

        private void clear() {
            ffmpegOuts.clear();
            totalCams = 0;
            missingCams = 0;
            isAccepting = false;
        }
    }
}