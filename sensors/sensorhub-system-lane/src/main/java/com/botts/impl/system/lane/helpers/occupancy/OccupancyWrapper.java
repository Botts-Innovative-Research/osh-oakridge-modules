package com.botts.impl.system.lane.helpers.occupancy;

import com.botts.impl.process.rs350.occupancy.Rs350OccupancyProcessModule;
import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.helpers.occupancy.state.*;
import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.event.EventUtils;
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
    public static final String DAILYFILE_NAME = "dailyFile";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyWrapper.class);
    private final List<FFMPEGSensorBase<?>> cameras = new ArrayList<>();
    private IDataProducerModule<?> rpm;
    private volatile StateManager stateManager;
    private final ObservationHelper observationHelper = new ObservationHelper();
    Instant startTime = Instant.now();
    Instant endTime = Instant.now();
    ISensorHub hub;
    boolean wasAlarming = false;
    IObsStore rpmObs;
    Flow.Subscription dailyFileSubscription;
    Flow.Subscription occupancySubscription;
    List<String> fileNames = new ArrayList<>();
    private long cmdId = 0;
    private final Object lifecycleLock = new Object();
    private boolean started = false;
    private long lifecycleGeneration = 0;

    public OccupancyWrapper(ISensorHub hub) {
        this.hub = hub;
    }

    public OccupancyWrapper(ISensorHub hub, IDataProducerModule<?> rpm) {
        this(hub);
        setRpmSensor(rpm);
    }

    public OccupancyWrapper(ISensorHub hub, IDataProducerModule<?> rpm, FFMPEGSensorBase<?>... cameras) {
        this(hub);
        init(rpm, cameras);
    }

    public OccupancyWrapper(ISensorHub hub, IDataProducerModule<?> rpm, List<FFMPEGSensorBase<?>> cameras) {
        this(hub);
        init(rpm, cameras);
    }

    public void init(IDataProducerModule<?> rpm, FFMPEGSensorBase<?>... cameras) {
        setRpmSensor(rpm);
        setFFmpegSensors(cameras);
        //registerStateListener();
    }

    public void init(IDataProducerModule<?> rpm, List<FFMPEGSensorBase<?>> cameras) {
        setRpmSensor(rpm);
        setFFmpegSensors(cameras);
    }

    public void start() {
        final long generation;
        final IDataProducerModule<?> startedRpm;
        synchronized (lifecycleLock) {
            if (started) {
                logger.debug("Occupancy wrapper is already started; ignoring duplicate start request.");
                return;
            }
            if (!isInitialized()) {
                logger.warn("Cannot start; init this object first!");
                return;
            }

            started = true;
            generation = ++lifecycleGeneration;
            startedRpm = rpm;
            registerStateListener();
        }
        registerDailyFileListener(generation, startedRpm);

        try {
            var occOut = startedRpm.getOutputs().values().stream().filter((predicate) -> predicate instanceof OccupancyOutput).findFirst();
            if (occOut.isPresent()) {
                observationHelper.setOccupancyOutput((OccupancyOutput<?>) occOut.get());
            } else {
                logger.error("Could not find occupancy output; cannot add video paths to occupancy observations.");
            }
        } catch (Exception ignored) {
        }
    }

    public void stop() {
        Flow.Subscription dailySubscription;
        Flow.Subscription occupancyEventSubscription;
        synchronized (lifecycleLock) {
            started = false;
            lifecycleGeneration++;
            if (stateManager != null)
                stateManager.clearListeners();
            dailySubscription = dailyFileSubscription;
            dailyFileSubscription = null;
            occupancyEventSubscription = occupancySubscription;
            occupancySubscription = null;
        }

        if (dailySubscription != null)
            dailySubscription.cancel();
        if (occupancyEventSubscription != null)
            occupancyEventSubscription.cancel();

        observationHelper.stop();
    }

    private void registerDailyFileListener(long generation, IDataProducerModule<?> startedRpm) {
        hub.getEventBus().newSubscription().withEventType(ObsEvent.class)
                .withTopicID(EventUtils.getDataStreamDataTopicID(startedRpm.getUniqueIdentifier(), DAILYFILE_NAME))
                .subscribe((event) -> {
                    IDataProducerModule<?> currentRpm;
                    StateManager currentStateManager;
                    synchronized (lifecycleLock) {
                        if (!started || generation != lifecycleGeneration)
                            return;
                        currentRpm = rpm;
                        currentStateManager = stateManager;
                    }
                    if (currentRpm == null || currentStateManager == null)
                        return;

                    ObsEvent obsEvent = (ObsEvent) event;
                    var record = currentRpm.getOutputs().get(DAILYFILE_NAME).getRecordDescription();
                    var observations = obsEvent.getObservations();

                    for (var obs : observations) {
                        record.setData(obs.getResult());
                        currentStateManager.updateDailyFile(record);
                    }
                }).thenAccept(newSubscription -> {
                    synchronized (lifecycleLock) {
                        if (!started || generation != lifecycleGeneration || dailyFileSubscription != null) {
                            newSubscription.cancel();
                            return;
                        }

                        dailyFileSubscription = newSubscription;
                        newSubscription.request(Long.MAX_VALUE);
                        logger.info("Started subscription to rpm dailyfile event.");
                    }
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
                wasAlarming = false;
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
                    /*
                    if (wasAlarming) {
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
                    } else {
                        commandInterface.submitCommand(new CommandData(++cmdId, command.getData()));
                    }

                     */
                    observationHelper.addFfmpegOut(((FileControl)commandInterface).getFileName(), size);
                    commandInterface.submitCommand(new CommandData(++cmdId, command.getData()));
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

    public void setRpmSensor(IDataProducerModule<?> sensor) {
        Asserts.checkNotNull(sensor);
        // TODO For now, assuming the added sensor is an rpm. Need to figure out the correct way to check.
        if (setStateManager(sensor)) {
            rpm = sensor;
        }
        if (observationHelper != null)
            observationHelper.clear();
    }

    private boolean setStateManager(IDataProducerModule<?> sensor) {
        if (sensor instanceof AspectSensor) {
            stateManager = new AspectStateManager();
        } else if (sensor instanceof RapiscanSensor) {
            stateManager = new RapiscanStateManager();
        } else if (sensor instanceof Rs350OccupancyProcessModule){
            stateManager = new Rs350StateManager();
        } else {
            logger.error("Could not determine RPM type from provided module.");
            return false;
        }
        return true;
    }

    public void removeRpmSensor() {
        stop();
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
        private final OccupancyOutput.OccupancyCallback occupancyCallback = this::occupancyCallback;
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
                    this.occupancyOutput.removeOccupancyCallback(occupancyCallback);

                this.occupancyOutput = occupancyOutput;
                this.occupancyOutput.addOccupancyCallback(occupancyCallback);
            }
        }

        private void stop() {
            synchronized (lock) {
                if (occupancyOutput != null) {
                    occupancyOutput.removeOccupancyCallback(occupancyCallback);
                    occupancyOutput = null;
                }
                clear();
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
