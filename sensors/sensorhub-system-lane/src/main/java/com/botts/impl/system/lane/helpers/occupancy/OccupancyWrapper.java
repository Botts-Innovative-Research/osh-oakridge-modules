package com.botts.impl.system.lane.helpers.occupancy;

import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.helpers.occupancy.state.*;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Text;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.controls.FileControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.process.DataConnectionList;
import org.vast.util.Asserts;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.BiConsumer;

public class OccupancyWrapper {
    public static final String OCCUPANCY = "occupancy";
    public static final String SYSTEM_INPUT_PARAM = "systemInput";
    public static final String VIDEO_PREFIX = "videoPrefix";
    public static final String DAILYFILE_NAME = "dailyFile";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyWrapper.class);
    private List<FFMPEGSensorBase<?>> cameras = new ArrayList<>();
    private AbstractSensorModule<?> rpm;
    private volatile StateManager stateManager;
    static final int OBS_BUFFER_SECONDS = 1;
    static final int DATA_FILE_NAME_INDEX = 10;
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
            rpmObs = hub.getSystemDriverRegistry().getDatabase(rpm.getParentSystemUID()).getObservationStore();
        } catch (Exception ignored) {

        }
    }

    public void registerDailyFileListener() {
        hub.getEventBus().newSubscription().withEventType(ObsEvent.class)
                .withTopicID(EventUtils.getDataStreamDataTopicID(rpm.getUniqueIdentifier(), DAILYFILE_NAME))
                .subscribe((event) -> {
                    ObsEvent obsEvent = (ObsEvent) event;
                    var record = rpm.getOutputs().get(DAILYFILE_NAME).getRecordDescription();
                    // Assuming only one daily file per event. May need to be changed if this causes any issues.
                    record.setData(obsEvent.getObservations()[0].getResult());
                    stateManager.updateDailyFile(record);
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
                    String fileName = System.currentTimeMillis() + ".mp4";
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
                    commandInterface.submitCommand(new CommandData(++cmdId, command.getData()));
                }

                //doPublish = true;
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
                                .withDataStreams(new DataStreamFilter.Builder()
                                        .withOutputNames(OCCUPANCY).build())
                                .withResultTimeDuring(startTime.minusSeconds(OBS_BUFFER_SECONDS), endTime.plusSeconds(OBS_BUFFER_SECONDS))
                                .withLimit(1)
                                .build()).toList();

                        if (occObs.isEmpty()) {
                            logger.error("SOMETHING WENT WRONG! OCCUPANCY TRIGGERED WITHOUT A CORRESPONDING OCCUPANCY RECORD!");
                        } else {
                            occObs.getFirst().getResult().setStringValue(DATA_FILE_NAME_INDEX, csvFileNames.toString());
                        }
                    }
                }
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
}