package com.botts.impl.system.lane.helpers.occupancy;

import com.botts.impl.process.rs350.occupancy.Rs350OccupancyProcessModule;
import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.config.LaneConfig;
import com.botts.impl.system.lane.helpers.occupancy.state.*;
import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.controls.FileControl;
import org.sensorhub.impl.utils.rad.eml.PendingEmlAutoAdjudicationRegistry;
import org.sensorhub.impl.utils.rad.eml.PendingExtendedOccupancyAutoAdjudicationRegistry;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.OccupancyExtended;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Asserts;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OccupancyWrapper {
    public static final String DAILYFILE_NAME = "dailyFile";

    @FunctionalInterface
    public interface TransientNonAlarmClipConsumer {
        void accept(Occupancy occupancy, List<String> videoPaths, Runnable discardCallback);
    }

    private static final Logger logger = LoggerFactory.getLogger(OccupancyWrapper.class);
    private static final ScheduledExecutorService EXTENDED_OCCUPANCY_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "oscar-extended-occupancy-watchdog");
                t.setDaemon(true);
                return t;
            });

    private final CopyOnWriteArrayList<FFMPEGSensorBase<?>> cameras = new CopyOnWriteArrayList<>();
    private IDataProducerModule<?> rpm;
    private volatile StateManager stateManager;
    private final OccupancyStatusOutput occupancyStatusOutput;
    private volatile TransientNonAlarmClipConsumer transientNonAlarmClipConsumer;
    private final CameraRecordingCoordinator recordingCoordinator = new CameraRecordingCoordinator();
    private final ObservationHelper observationHelper = new ObservationHelper(
            recordingCoordinator,
            this::evaluateCompletedOccupancy,
            () -> this.transientNonAlarmClipConsumer);
    Instant startTime = Instant.now();
    Instant endTime = Instant.now();
    ISensorHub hub;
    boolean wasAlarming = false;
    IObsStore rpmObs;
    Flow.Subscription dailyFileSubscription;
    Flow.Subscription occupancySubscription;
    List<String> fileNames = new ArrayList<>();
    private final AtomicLong cmdId = new AtomicLong();
    private final Object lifecycleLock = new Object();
    private boolean started = false;
    private long lifecycleGeneration = 0;
    private boolean hasValidOccupancyState = false;
    private boolean occupancyActive = false;
    private Instant occupancyStartedAt = null;
    private boolean extendedOccupancyFaultLogged = false;
    private long occupancyEpoch = 0;
    private ScheduledFuture<?> extendedOccupancyFaultTimer = null;
    private volatile int extendedOccupancyThresholdMinutes = LaneConfig.DEFAULT_EXTENDED_OCCUPANCY_THRESHOLD_MINUTES;

    public OccupancyWrapper(ISensorHub hub) {
        this(hub, (OccupancyStatusOutput) null);
    }

    public OccupancyWrapper(ISensorHub hub, OccupancyStatusOutput occupancyStatusOutput) {
        this.hub = hub;
        this.occupancyStatusOutput = occupancyStatusOutput;
        if (occupancyStatusOutput != null) {
            this.extendedOccupancyThresholdMinutes = occupancyStatusOutput.getExtendedOccupancyThresholdMinutes();
        }
    }

    public OccupancyWrapper(ISensorHub hub, IDataProducerModule<?> rpm) {
        this(hub, (OccupancyStatusOutput) null);
        setRpmSensor(rpm);
    }

    public OccupancyWrapper(ISensorHub hub, IDataProducerModule<?> rpm, OccupancyStatusOutput occupancyStatusOutput) {
        this(hub, occupancyStatusOutput);
        setRpmSensor(rpm);
    }

    public OccupancyWrapper(ISensorHub hub, IDataProducerModule<?> rpm, FFMPEGSensorBase<?>... cameras) {
        this(hub, (OccupancyStatusOutput) null);
        init(rpm, cameras);
    }

    public OccupancyWrapper(ISensorHub hub, IDataProducerModule<?> rpm, List<FFMPEGSensorBase<?>> cameras) {
        this(hub, (OccupancyStatusOutput) null);
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
            observationHelper.resume();
            registerStateListener(generation);
        }
        registerDailyFileListener(generation, startedRpm);

        try {
            var occOut = startedRpm.getOutputs().values().stream().filter((predicate) -> predicate instanceof OccupancyOutput).findFirst();
            if (occOut.isPresent()) {
                synchronized (lifecycleLock) {
                    if (started && generation == lifecycleGeneration)
                        observationHelper.setOccupancyOutput((OccupancyOutput<?>) occOut.get());
                }
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
            occupancyEpoch++;
            cancelExtendedOccupancyFaultTimerLocked();
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
        recordingCoordinator.cancelAll();
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
                        var observationTime = obs.getPhenomenonTime() != null
                                ? obs.getPhenomenonTime()
                                : Instant.now();
                        processDailyFile(currentStateManager, record, observationTime);
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

    private void registerStateListener(long generation) {
        if (stateManager == null) {
            if (rpm == null) {
                logger.error("State manager is null. Add an rpm before setting the state listener.");
                return;
            } else {
                stateManager = stateManagerFor(rpm);
                if (stateManager == null) {
                    logger.error("Could not determine RPM type from provided module.");
                    return;
                }
            }
        }
        final StateManager listenerManager = stateManager;
        listenerManager.clearListeners();

        listenerManager.addListener((from, to) -> {
            synchronized (lifecycleLock) {
                if (!started || generation != lifecycleGeneration || stateManager != listenerManager)
                    return;
                // Start recording if leaving non-occupancy state. Beginning and registration stay
                // under the lifecycle lock, so stop cannot cancel and then be followed by a late
                // open from an already-dispatched transition.
                if (from == StateManager.State.NON_OCCUPANCY) {
                    wasAlarming = false;
                    startTime = Instant.now();
                    Instant correlatedStart = listenerManager.getOccupancyStartTimeHint();
                    if (correlatedStart == null)
                        correlatedStart = startTime;
                    String fileNamePrefix = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS")
                            .withLocale(Locale.US).withZone(ZoneId.systemDefault()).format(startTime);
                    CameraRecordingCoordinator.Recording recording = recordingCoordinator.begin(
                            correlatedStart, fileNamePrefix, cameraTargets());
                    observationHelper.register(recording);
                }

                if (to == StateManager.State.ALARMING_OCCUPANCY) {
                    wasAlarming = true;
                } else if (to == StateManager.State.NON_OCCUPANCY) {
                    endTime = Instant.now();
                    recordingCoordinator.finishActive(wasAlarming);
                    wasAlarming = false;
                }
            }
        });
    }

    private List<CameraRecordingCoordinator.CameraTarget> cameraTargets() {
        List<CameraRecordingCoordinator.CameraTarget> targets = new ArrayList<>(cameras.size());
        for (FFMPEGSensorBase<?> camera : List.copyOf(cameras)) {
            FileControl<?> fileControl = camera.getCommandInputs().values().stream()
                    .filter(FileControl.class::isInstance)
                    .map(FileControl.class::cast)
                    .findFirst()
                    .orElse(null);
            if (fileControl == null) {
                logger.warn("Camera {} has no FFmpeg file control; skipping occupancy recording",
                        camera.getUniqueIdentifier());
                continue;
            }
            targets.add(new FileControlTarget(camera.getUniqueIdentifier(), fileControl));
        }
        return targets;
    }

    private final class FileControlTarget implements CameraRecordingCoordinator.CameraTarget {
        private final String cameraUid;
        private final String pathToken;
        private final FileControl<?> control;

        private FileControlTarget(String cameraUid, FileControl<?> control) {
            this.cameraUid = cameraUid;
            this.pathToken = cameraUid.replace(':', '-');
            this.control = control;
        }

        @Override
        public String cameraId() {
            return cameraUid;
        }

        @Override
        public Object serializationKey() {
            return control;
        }

        @Override
        public CompletableFuture<ICommandStatus> open(String fileName) {
            DataComponent command = newCommand(FileControl.CMD_OPEN_FILE);
            ((DataChoice) command.getComponent(0)).getSelectedItem().getData()
                    .setStringValue(fileName);
            return submit(command);
        }

        @Override
        public CompletableFuture<ICommandStatus> close(boolean save) {
            DataComponent command = newCommand(FileControl.CMD_CLOSE_FILE);
            ((DataChoice) command.getComponent(0)).getSelectedItem().getData()
                    .setBooleanValue(save);
            return submit(command);
        }

        @Override
        public CompletableFuture<Void> discard(String path) {
            return control.discardCompletedFile(path);
        }

        private DataComponent newCommand(String selection) {
            DataComponent command = control.getCommandDescription().clone();
            command.renewDataBlock();
            DataChoice fileIO = (DataChoice) command.getComponent(0);
            fileIO.setSelectedItem(selection);
            if (!fileIO.getSelectedItem().hasData())
                fileIO.getSelectedItem().renewDataBlock();
            return command;
        }

        private CompletableFuture<ICommandStatus> submit(DataComponent command) {
            return control.submitCommand(new CommandData(cmdId.incrementAndGet(), command.getData()));
        }

        @Override
        public boolean ownsPath(String path, String requestedFileName) {
            return isExpectedCameraPath(path, pathToken, requestedFileName);
        }
    }

    static boolean isExpectedCameraPath(String path, String cameraToken, String requestedFileName) {
        if (path == null || cameraToken == null || requestedFileName == null
                || cameraToken.isBlank() || requestedFileName.isBlank())
            return false;
        if (!cameraToken.matches("[A-Za-z0-9._-]+")
                || !requestedFileName.matches("[A-Za-z0-9][A-Za-z0-9._-]*"))
            return false;
        String normalized = path.replace('\\', '/');
        if (normalized.indexOf('\0') >= 0 || normalized.startsWith("/")
                || normalized.contains("://"))
            return false;
        List<String> segments = Arrays.asList(normalized.split("/", -1));
        if (segments.contains("") || segments.contains(".") || segments.contains(".."))
            return false;
        String expected = "clips/" + cameraToken + "/" + requestedFileName;
        return normalized.equals(expected) || normalized.equals("videos/" + expected);
    }

    public boolean isInitialized() {
        return (rpm != null);
    }

    public void setFFmpegSensors(FFMPEGSensorBase<?>... sensors) {
        Arrays.stream(sensors).forEach(cameras::addIfAbsent);
    }

    public void setFFmpegSensors(List<FFMPEGSensorBase<?>> sensors) {
        sensors.forEach(cameras::addIfAbsent);
    }

    public void addFFmpegSensor(FFMPEGSensorBase<?> sensor) {
        cameras.addIfAbsent(sensor);
    }

    public void setRpmSensor(IDataProducerModule<?> sensor) {
        Asserts.checkNotNull(sensor);
        StateManager nextStateManager = stateManagerFor(sensor);
        if (nextStateManager == null) {
            logger.error("Could not determine RPM type from provided module.");
            return;
        }

        final boolean restart;
        synchronized (lifecycleLock) {
            restart = started;
        }
        // A live RPM replacement must replace both event-bus subscriptions and the occupancy
        // callback. Reusing the old subscription would parse old-topic samples with the new RPM's
        // record schema and could open recordings in the wrong lifecycle.
        if (restart)
            stop();

        StateManager previousStateManager;
        synchronized (lifecycleLock) {
            previousStateManager = stateManager;
            stateManager = nextStateManager;
            rpm = sensor;
            hasValidOccupancyState = false;
            occupancyActive = false;
            occupancyStartedAt = null;
            extendedOccupancyFaultLogged = false;
            occupancyEpoch++;
            cancelExtendedOccupancyFaultTimerLocked();
        }
        if (previousStateManager != null)
            previousStateManager.clearListeners();
        observationHelper.clear();
        recordingCoordinator.cancelAll();
        if (restart)
            start();
    }

    private StateManager stateManagerFor(IDataProducerModule<?> sensor) {
        if (sensor instanceof AspectSensor) {
            return new AspectStateManager();
        } else if (sensor instanceof RapiscanSensor) {
            return new RapiscanStateManager();
        } else if (sensor instanceof Rs350OccupancyProcessModule){
            return new Rs350StateManager();
        }
        return null;
    }

    public void removeRpmSensor() {
        stop();
        IDataProducerModule<?> prevRpm = rpm;
        rpm = null;
        rpmObs = null;
        stateManager = null;
        if (prevRpm != null && prevRpm.getUniqueIdentifier() != null) {
            PendingExtendedOccupancyAutoAdjudicationRegistry.clearProducer(prevRpm.getUniqueIdentifier());
        }
        synchronized (lifecycleLock) {
            hasValidOccupancyState = false;
            occupancyActive = false;
            occupancyStartedAt = null;
            extendedOccupancyFaultLogged = false;
            occupancyEpoch++;
            cancelExtendedOccupancyFaultTimerLocked();
        }
    }

    public void removeFFmpegSensor(FFMPEGSensorBase<?> sensor) {
        cameras.remove(sensor);
    }

    public void clearFFmpegSensors() {
        cameras.clear();
    }

    public void setTransientNonAlarmClipConsumer(TransientNonAlarmClipConsumer consumer) {
        this.transientNonAlarmClipConsumer = consumer;
    }

    public int getExtendedOccupancyThresholdMinutes() {
        return extendedOccupancyThresholdMinutes;
    }

    public void setExtendedOccupancyThresholdMinutes(int thresholdMinutes) {
        int clamped = Math.max(
                LaneConfig.MIN_EXTENDED_OCCUPANCY_THRESHOLD_MINUTES,
                Math.min(LaneConfig.MAX_EXTENDED_OCCUPANCY_THRESHOLD_MINUTES, thresholdMinutes));
        this.extendedOccupancyThresholdMinutes = clamped;
        if (occupancyStatusOutput != null) {
            occupancyStatusOutput.setExtendedOccupancyThresholdMinutes(clamped);
        }
    }

    private Duration getExtendedOccupancyThreshold() {
        return Duration.ofMinutes(Math.max(
                LaneConfig.MIN_EXTENDED_OCCUPANCY_THRESHOLD_MINUTES,
                extendedOccupancyThresholdMinutes));
    }

    private boolean hasExceededExtendedOccupancyThreshold(Instant start, Instant sampleTime) {
        if (start == null || sampleTime == null)
            return false;
        return !sampleTime.isBefore(start.plus(getExtendedOccupancyThreshold()));
    }

    private void cancelExtendedOccupancyFaultTimerLocked() {
        if (extendedOccupancyFaultTimer != null) {
            extendedOccupancyFaultTimer.cancel(false);
            extendedOccupancyFaultTimer = null;
        }
    }

    private void scheduleExtendedOccupancyFaultTimerLocked(long expectedOccupancyEpoch, Instant activeStart, Instant observationTime) {
        cancelExtendedOccupancyFaultTimerLocked();
        if (!started || activeStart == null)
            return;
        Duration threshold = getExtendedOccupancyThreshold();
        Duration elapsed = observationTime != null && observationTime.isAfter(activeStart)
                ? Duration.between(activeStart, observationTime)
                : Duration.ZERO;
        long delayMillis = Math.max(1L, threshold.minus(elapsed).toMillis());
        extendedOccupancyFaultTimer = EXTENDED_OCCUPANCY_SCHEDULER.schedule(
                () -> onExtendedOccupancyDeadlineReached(expectedOccupancyEpoch, activeStart),
                delayMillis,
                TimeUnit.MILLISECONDS);
    }

    private void onExtendedOccupancyDeadlineReached(long expectedOccupancyEpoch, Instant expectedStart) {
        Instant faultSampleTime;
        synchronized (lifecycleLock) {
            if (!started || !occupancyActive || extendedOccupancyFaultLogged || occupancyEpoch != expectedOccupancyEpoch)
                return;
            extendedOccupancyFaultLogged = true;
            extendedOccupancyFaultTimer = null;
            Instant thresholdInstant = expectedStart.plus(getExtendedOccupancyThreshold());
            Instant now = Instant.now();
            faultSampleTime = now.isAfter(thresholdInstant) ? now : thresholdInstant;
        }
        if (occupancyStatusOutput != null) {
            occupancyStatusOutput.publish(faultSampleTime, true, expectedStart, true);
        }
    }

    boolean processDailyFile(DataComponent dailyFile, Instant observationTime) {
        return processDailyFile(stateManager, dailyFile, observationTime);
    }

    private boolean processDailyFile(StateManager manager, DataComponent dailyFile, Instant observationTime) {
        if (manager == null || dailyFile == null || observationTime == null
                || !manager.updateDailyFile(dailyFile, observationTime))
            return false;

        boolean shouldPublish = false;
        boolean publishFault = false;
        boolean isOccupied = manager.hasActiveOccupancy();
        Instant activeStart;
        synchronized (lifecycleLock) {
            if (manager != stateManager)
                return false;

            boolean transition = !hasValidOccupancyState || isOccupied != occupancyActive;
            if (transition) {
                shouldPublish = true;
                if (isOccupied) {
                    occupancyStartedAt = manager.getOccupancyStartTimeHint() != null
                            ? manager.getOccupancyStartTimeHint()
                            : observationTime;
                    occupancyActive = true;
                    hasValidOccupancyState = true;
                    long currentEpoch = ++occupancyEpoch;
                    if (hasExceededExtendedOccupancyThreshold(occupancyStartedAt, observationTime)) {
                        extendedOccupancyFaultLogged = true;
                        publishFault = true;
                        cancelExtendedOccupancyFaultTimerLocked();
                    } else {
                        extendedOccupancyFaultLogged = false;
                        publishFault = false;
                        scheduleExtendedOccupancyFaultTimerLocked(currentEpoch, occupancyStartedAt, observationTime);
                    }
                } else {
                    occupancyStartedAt = null;
                    occupancyActive = false;
                    hasValidOccupancyState = true;
                    extendedOccupancyFaultLogged = false;
                    publishFault = false;
                    occupancyEpoch++;
                    cancelExtendedOccupancyFaultTimerLocked();
                }
            } else if (isOccupied && !extendedOccupancyFaultLogged
                    && hasExceededExtendedOccupancyThreshold(occupancyStartedAt, observationTime)) {
                extendedOccupancyFaultLogged = true;
                shouldPublish = true;
                publishFault = true;
                cancelExtendedOccupancyFaultTimerLocked();
            }
            activeStart = occupancyStartedAt;
        }

        if (shouldPublish && occupancyStatusOutput != null)
            occupancyStatusOutput.publish(observationTime, isOccupied, activeStart, publishFault);
        return true;
    }

    void evaluateCompletedOccupancy(Occupancy occupancy) {
        if (occupancy == null)
            return;

        double thresholdSeconds = getExtendedOccupancyThreshold().toSeconds();
        double durationSeconds = occupancy.getEndTime() - occupancy.getStartTime();
        boolean isExtendedBySpan = Double.isFinite(durationSeconds) && durationSeconds >= thresholdSeconds;

        boolean isExtended;
        boolean publishLateFault = false;
        Instant lateFaultSampleTime = null;
        Instant lateFaultStart = null;
        String producerUid;

        synchronized (lifecycleLock) {
            producerUid = rpm != null ? rpm.getUniqueIdentifier() : null;
            boolean isExtendedByActiveStart = !isExtendedBySpan
                    && occupancyStartedAt != null
                    && occupancy.getSamplingTime() > 0
                    && (occupancy.getSamplingTime() - (occupancyStartedAt.toEpochMilli() / 1000.0)) >= thresholdSeconds;

            isExtended = extendedOccupancyFaultLogged || isExtendedBySpan || isExtendedByActiveStart;
            if (isExtended && !extendedOccupancyFaultLogged) {
                extendedOccupancyFaultLogged = true;
                cancelExtendedOccupancyFaultTimerLocked();
                publishLateFault = true;
                lateFaultStart = occupancy.getStartTime() > 0
                        ? Instant.ofEpochMilli(Math.round(occupancy.getStartTime() * 1000.0))
                        : (occupancyStartedAt != null ? occupancyStartedAt : Instant.now());
                double endEpochSec = occupancy.getEndTime() > 0
                        ? occupancy.getEndTime()
                        : occupancy.getSamplingTime();
                lateFaultSampleTime = endEpochSec > 0
                        ? Instant.ofEpochMilli(Math.round(endEpochSec * 1000.0))
                        : Instant.now();
            }
        }

        if (publishLateFault && occupancyStatusOutput != null) {
            occupancyStatusOutput.publish(lateFaultSampleTime, true, lateFaultStart, true);
        }

        if (isExtended && isGammaOnlyAlarm(occupancy)) {
            double effectiveDuration = isExtendedBySpan ? durationSeconds : Math.max(thresholdSeconds, durationSeconds);
            var eval = PendingExtendedOccupancyAutoAdjudicationRegistry.ExtendedOccupancyEvaluation.create(
                    effectiveDuration,
                    extendedOccupancyThresholdMinutes,
                    occupancy.getMaxGammaCount());

            if (occupancy.getAdjudicatedIds() != null) {
                occupancy.getAdjudicatedIds().removeIf(Adjudication.EML_NON_THREAT_MARKER::equals);
            }
            if (producerUid != null) {
                PendingEmlAutoAdjudicationRegistry.remove(
                        producerUid,
                        occupancy.getSamplingTime(),
                        occupancy.getOccupancyCount());
            }
            if (occupancy.getAdjudicatedIds() == null
                    || !occupancy.getAdjudicatedIds().contains(Adjudication.EXTENDED_OCCUPANCY_GAMMA_MARKER)) {
                occupancy.addAdjudicatedId(Adjudication.EXTENDED_OCCUPANCY_GAMMA_MARKER);
            }
            occupancy.recordAdjudicationCode(Adjudication.EXTENDED_OCCUPANCY_GAMMA_CODE);

            if (producerUid != null && !producerUid.isBlank()) {
                PendingExtendedOccupancyAutoAdjudicationRegistry.register(
                        producerUid,
                        occupancy.getSamplingTime(),
                        occupancy.getOccupancyCount(),
                        eval);
            }
        }
    }

    private static boolean isGammaOnlyAlarm(Occupancy occupancy) {
        if (occupancy == null || !occupancy.hasGammaAlarm() || occupancy.hasNeutronAlarm())
            return false;
        if (occupancy instanceof OccupancyExtended extended) {
            String category = extended.getAlarmCategory();
            if (category != null && category.toLowerCase(Locale.ROOT).contains("neutron"))
                return false;
        }
        return true;
    }



    private static class ObservationHelper {
        private static final Duration FILE_TIMEOUT = Duration.ofSeconds(8);
        private OccupancyOutput<?> occupancyOutput;
        private final OccupancyOutput.OccupancyCallback occupancyCallback = this::occupancyCallback;
        private final Object lock = new Object();
        private final OccupancyRecordingRegistry recordings = new OccupancyRecordingRegistry();
        private final CameraRecordingCoordinator coordinator;
        private final Consumer<Occupancy> completedOccupancyEvaluator;
        private final Supplier<TransientNonAlarmClipConsumer> transientClipConsumerSupplier;
        private boolean active;
        private long lifecycleEpoch;

        private ObservationHelper(
                CameraRecordingCoordinator coordinator,
                Consumer<Occupancy> completedOccupancyEvaluator,
                Supplier<TransientNonAlarmClipConsumer> transientClipConsumerSupplier) {
            this.coordinator = coordinator;
            this.completedOccupancyEvaluator = completedOccupancyEvaluator;
            this.transientClipConsumerSupplier = transientClipConsumerSupplier;
        }

        public void register(CameraRecordingCoordinator.Recording recording) {
            synchronized (lock) {
                if (!active) {
                    recording.invalidate();
                    return;
                }
                recordings.register(recording, recordings.epoch());
            }
        }

        public void resume() {
            synchronized (lock) {
                active = true;
                lifecycleEpoch++;
            }
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
                active = false;
                lifecycleEpoch++;
                if (occupancyOutput != null) {
                    occupancyOutput.removeOccupancyCallback(occupancyCallback);
                    occupancyOutput = null;
                }
                clear();
            }
        }

        private void occupancyCallback(Occupancy occupancy) {
            final long callbackEpoch;
            synchronized (lock) {
                if (!active)
                    return;
                callbackEpoch = lifecycleEpoch;
            }

            if (completedOccupancyEvaluator != null) {
                completedOccupancyEvaluator.accept(occupancy);
            }

            boolean alarm = occupancy.hasGammaAlarm() || occupancy.hasNeutronAlarm();
            TransientNonAlarmClipConsumer transientConsumer =
                    !alarm && transientClipConsumerSupplier != null
                            ? transientClipConsumerSupplier.get()
                            : null;
            boolean retainNonAlarm = !alarm && transientConsumer != null;

            OccupancyRecordingRegistry.Resolution resolution =
                    recordings.resolve(occupancy, FILE_TIMEOUT, retainNonAlarm, coordinator::finish);
            if (resolution == null)
                return;

            boolean handoffTransient = false;
            synchronized (lock) {
                boolean valid = active && callbackEpoch == lifecycleEpoch && !resolution.paths().isEmpty();
                if (alarm) {
                    if (!recordings.complete(resolution, valid))
                        return;
                    appendDistinctVideoPaths(occupancy, resolution.paths());
                } else {
                    boolean retain = valid && retainNonAlarm;
                    if (!recordings.complete(resolution, retain))
                        return;
                    handoffTransient = true;
                }
            }

            if (handoffTransient) {
                Runnable discardCallback = resolution.recording()::discardSavedPaths;
                try {
                    transientConsumer.accept(occupancy, resolution.paths(), discardCallback);
                } catch (Throwable t) {
                    discardCallback.run();
                    logger.warn("Transient non-alarm clip consumer failed; discarded temporary clips immediately", t);
                }
            }
        }

        private void clear() {
            synchronized (lock) {
                lifecycleEpoch++;
                recordings.clear();
            }
        }
    }

    static void appendDistinctVideoPaths(Occupancy occupancy, List<String> paths) {
        // Preserve existing/source camera order while avoiding duplicates from an upstream
        // callback or a replayed augmentation pass.
        Set<String> existing = new LinkedHashSet<>(occupancy.getVideoPaths());
        for (String path : paths) {
            if (path != null && !path.isBlank() && existing.add(path))
                occupancy.addVideoPath(path);
        }
    }
}
