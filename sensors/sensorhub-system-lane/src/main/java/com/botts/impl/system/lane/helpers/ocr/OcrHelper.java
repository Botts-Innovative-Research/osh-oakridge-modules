package com.botts.impl.system.lane.helpers.ocr;

import com.botts.impl.ocr.pipeline.OcrJob;
import com.botts.impl.ocr.pipeline.OcrSettings;
import com.botts.impl.ocr.pipeline.VehicleOcrProcessor;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.VehicleOcrOutput;
import com.botts.impl.system.lane.config.OcrConfig;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.OccupancyExtended;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

/**
 * Subscribes to a lane's occupancy datastream and, on gamma/neutron alarms,
 * queues OCR of the occupancy's recorded clips. Results are published on the
 * lane's {@link VehicleOcrOutput}, correlated by {@code occupancyObsId}.
 * Mirrors the WebIdHelper subscription/correlation shape; created only when
 * the lane's OCR config is enabled.
 */
public class OcrHelper {

    private static final String OCCUPANCY_NAME = OccupancyOutput.NAME;
    private static final Logger logger = LoggerFactory.getLogger(OcrHelper.class);
    private static final long MAX_OCCUPANCY_PRODUCER_WAIT_MS = 5000;
    private static final long TIME_TIL_OBS_CHECK_MS = 300; // Wait before checking the obs store

    private final LaneSystem laneSystem;
    private final ISensorHub hub;
    private final IDataProducerModule<?> occupancyProducer;
    private final VehicleOcrOutput output;
    private final OcrConfig config;

    private IdEncoder obsIdEncoder;
    private IObsStore occupancyObsStore;
    private Flow.Subscription occupancySub;
    private Thread startupThread;

    public OcrHelper(LaneSystem laneSystem, IDataProducerModule<?> occupancyProducer,
                     VehicleOcrOutput output, OcrConfig config) {
        this.laneSystem = laneSystem;
        this.hub = laneSystem.getParentHub();
        this.occupancyProducer = occupancyProducer;
        this.output = output;
        this.config = config;
        startupThread = new Thread(this::init, "ocr-helper-init");
        startupThread.start();
    }

    private void init() {
        try {
            occupancyProducer.waitForState(ModuleEvent.ModuleState.STARTED, MAX_OCCUPANCY_PRODUCER_WAIT_MS);
        } catch (SensorHubException e) {
            logger.error("Occupancy producer not started in time; OCR disabled for lane {}",
                    laneSystem.getUniqueIdentifier(), e);
            return;
        }
        createSubscription();
    }

    private void createSubscription() {
        try {
            obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();
            occupancyObsStore = hub.getSystemDriverRegistry()
                    .getDatabase(occupancyProducer.getUniqueIdentifier()).getObservationStore();

            hub.getEventBus().newSubscription(ObsEvent.class)
                    .withTopicID(EventUtils.getDataStreamDataTopicID(occupancyProducer.getUniqueIdentifier(), OCCUPANCY_NAME))
                    .subscribe(this::processOccupancyEvent)
                    .thenAccept(subscription -> {
                        this.occupancySub = subscription;
                        subscription.request(Long.MAX_VALUE);
                        logger.info("Started OCR subscription to {} for lane {}",
                                OCCUPANCY_NAME, laneSystem.getUniqueIdentifier());
                    });
        } catch (Exception e) {
            logger.error("Failed to create OCR occupancy subscription for lane {}",
                    laneSystem.getUniqueIdentifier(), e);
            this.stop();
        }
    }

    private void processOccupancyEvent(ObsEvent event) {
        try {
            Thread.sleep(TIME_TIL_OBS_CHECK_MS);
        } catch (InterruptedException e) {
            return;
        }

        for (var obs : event.getObservations()) {
            Occupancy occupancy = readOccupancy((ObsData) obs);
            if (occupancy == null)
                continue;

            if (!occupancy.hasGammaAlarm() && !occupancy.hasNeutronAlarm())
                continue;

            List<String> videoPaths = occupancy.getVideoPaths();
            if (videoPaths == null || videoPaths.isEmpty()) {
                logger.debug("Alarming occupancy on lane {} has no recorded clips; skipping OCR",
                        laneSystem.getUniqueIdentifier());
                continue;
            }

            BigId obsId = findObsId((ObsData) obs);
            if (obsId == null)
                continue;

            List<OcrJob.VideoRef> videos = resolveVideos(videoPaths);
            if (videos.isEmpty())
                continue;

            Instant occupancyStart = Instant.ofEpochMilli((long) (occupancy.getStartTime() * 1000));
            OcrJob job = new OcrJob(laneSystem.getUniqueIdentifier(), obsIdEncoder.encodeID(obsId),
                    occupancyStart, videos, toSettings(config));
            VehicleOcrProcessor.submit(hub, job, output::publishData);
        }
    }

    private Occupancy readOccupancy(ObsData obs) {
        try {
            DataComponent recordStructure = null;
            var dsInfo = occupancyObsStore.getDataStreams().get(new DataStreamKey(obs.getDataStreamID()));
            if (dsInfo != null)
                recordStructure = dsInfo.getRecordStructure();
            return OccupancyExtended.readOccupancy(recordStructure, obs.getResult());
        } catch (Exception e) {
            logger.warn("Failed to parse occupancy observation for OCR on lane {}",
                    laneSystem.getUniqueIdentifier(), e);
            return null;
        }
    }

    private BigId findObsId(ObsData obs) {
        ObsFilter filter = new ObsFilter.Builder()
                .withSystems()
                    .withUniqueIDs(occupancyProducer.getUniqueIdentifier())
                    .done()
                .withDataStreams()
                    .withOutputNames(OCCUPANCY_NAME)
                    .done()
                .withPhenomenonTime()
                    .withSingleValue(obs.getPhenomenonTime())
                    .done()
                .build();
        try {
            return occupancyObsStore.selectKeys(filter).findFirst().orElse(null);
        } catch (Exception e) {
            logger.error("Could not resolve occupancy observation id for OCR", e);
            return null;
        }
    }

    /**
     * Maps each recorded clip path back to its camera by the sanitized system
     * UID embedded in the path, applying the config's camera filter.
     */
    private List<OcrJob.VideoRef> resolveVideos(List<String> videoPaths) {
        List<String> cameraUids = new ArrayList<>();
        for (var member : laneSystem.getMembers().values()) {
            if (member instanceof FFMPEGSensorBase<?> camera && camera.getUniqueIdentifier() != null)
                cameraUids.add(camera.getUniqueIdentifier());
        }

        List<OcrJob.VideoRef> videos = new ArrayList<>();
        for (String videoPath : videoPaths) {
            if (videoPath == null || videoPath.isBlank())
                continue;

            String normalizedPath = videoPath.replace('\\', '/');
            String cameraUid = cameraUids.stream()
                    .filter(uid -> normalizedPath.contains(uid.replace(':', '-')))
                    .findFirst()
                    .orElse("");

            if (!config.cameraUids.isEmpty() && !config.cameraUids.contains(cameraUid))
                continue;

            videos.add(new OcrJob.VideoRef(videoPath, cameraUid));
        }
        return videos;
    }

    private static OcrSettings toSettings(OcrConfig config) {
        OcrSettings settings = new OcrSettings();
        settings.modelDir = config.modelDir;
        settings.readContainerIds = config.readContainerIds;
        settings.readPlates = config.readPlates;
        settings.framesPerVideo = config.framesPerVideo;
        settings.containerMinConfidence = config.containerMinConfidence;
        settings.plateMinConfidence = config.plateMinConfidence;
        return settings;
    }

    public void stop() {
        if (startupThread != null) {
            startupThread.interrupt();
            try {
                startupThread.join();
            } catch (InterruptedException ignored) {}
            startupThread = null;
        }

        if (occupancySub != null) {
            occupancySub.cancel();
            occupancySub = null;
        }
    }
}
