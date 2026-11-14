/*******************************************************************************

  The contents of this file are subject to the Mozilla Public License, v. 2.0.
  If a copy of the MPL was not distributed with this file, You can obtain one
  at http://mozilla.org/MPL/2.0/.

  Software distributed under the License is distributed on an "AS IS" basis,
  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  for the specific language governing rights and limitations under the License.

  The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
  Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.system.lane.helpers.ocr;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.process.ocr.engine.OcrEngine;
import com.botts.impl.process.ocr.engine.OnnxOcrEngine;
import com.botts.impl.process.ocr.pipeline.OcrJob;
import com.botts.impl.process.ocr.pipeline.OcrSettings;
import com.botts.impl.process.ocr.pipeline.VehicleOcrProcessor;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.VehicleOcrOutput;
import com.botts.impl.system.lane.config.OcrConfig;
import com.botts.impl.system.lane.helpers.occupancy.OccupancyWrapper;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IObsData;
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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Non-blocking per-lane OCR coordinator. Subscribes to the lane's {@code occupancy} output and
 * dispatches asynchronous, resource-governed {@link OcrJob}s onto the shared low-priority
 * {@link VehicleOcrProcessor} worker without ever sleeping or querying PostgreSQL on SensorHub's
 * {@code EventBus} threads.
 */
public final class OcrHelper implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(OcrHelper.class);
    private static final String OCCUPANCY_NAME = OccupancyOutput.NAME;
    private static final String VIDEO_BUCKET = "videos";
    private static final long MAX_OCCUPANCY_PRODUCER_WAIT_MS = 10_000L;
    private static final long PHENOMENON_TIME_TOLERANCE_MS = 5L;
    private static final int MAX_STASHED_TRANSIENT_CLIPS = 32;
    private static final long TRANSIENT_CLIP_TTL_MS = 15_000L;

    private final LaneSystem laneSystem;
    private final ISensorHub hub;
    private final IDataProducerModule<?> occupancyProducer;
    private final VehicleOcrOutput output;
    private final OcrConfig config;
    private final AtomicBoolean laneActive = new AtomicBoolean(true);
    private final Object stashLock = new Object();
    private final LinkedHashMap<OccupancyMatchKey, StashedClips> transientClipStash = new LinkedHashMap<>();

    private volatile OcrEngine engineLease;
    private volatile VehicleOcrProcessor processorLease;
    private volatile IBucketService bucketService;
    private volatile IBucketStore bucketStore;
    private volatile IdEncoder obsIdEncoder;
    private volatile IObsStore occupancyObsStore;
    private volatile Flow.Subscription occupancySub;
    private volatile Thread startupThread;

    public OcrHelper(LaneSystem laneSystem,
                     IDataProducerModule<?> occupancyProducer,
                     VehicleOcrOutput output,
                     OcrConfig config) {
        this.laneSystem = laneSystem;
        this.hub = laneSystem.getParentHub();
        this.occupancyProducer = occupancyProducer;
        this.output = output;
        this.config = config;

        try {
            String rawModelDir = (config.modelDir == null || config.modelDir.isBlank())
                    ? "models/ocr"
                    : config.modelDir.trim();
            this.engineLease = OnnxOcrEngine.acquireShared(Paths.get(rawModelDir));
            resolveBucketService();
            this.processorLease = VehicleOcrProcessor.acquireShared(bucketStore, bucketService);
        } catch (Throwable t) {
            logger.error("Failed to initialize Vehicle OCR for lane {} (lane telemetry unaffected): {}",
                    laneSystem.getUniqueIdentifier(), t.getMessage(), t);
            stop();
            return;
        }

        if (!config.alarmsOnly) {
            OccupancyWrapper wrapper = laneSystem.getOccupancyWrapper();
            if (wrapper != null) {
                wrapper.setTransientNonAlarmClipConsumer(this::onTransientNonAlarmClips);
            }
        }

        if (occupancyProducer.getCurrentState() == ModuleEvent.ModuleState.STARTED) {
            createSubscription();
        } else {
            Thread initThread = new Thread(this::waitAndSubscribe,
                    "ocr-helper-init-" + laneSystem.getConfiguration().name);
            initThread.setDaemon(true);
            this.startupThread = initThread;
            initThread.start();
        }
    }

    private void resolveBucketService() {
        try {
            var services = hub.getModuleRegistry().getLoadedModules(IBucketService.class);
            if (services != null && !services.isEmpty()) {
                IBucketService svc = services.iterator().next();
                this.bucketService = svc;
                this.bucketStore = svc.getBucketStore();
            }
        } catch (Exception e) {
            logger.debug("Bucket service not yet available for OCR helper on lane {}: {}",
                    laneSystem.getUniqueIdentifier(), e.getMessage());
        }
    }

    private void waitAndSubscribe() {
        try {
            occupancyProducer.waitForState(ModuleEvent.ModuleState.STARTED, MAX_OCCUPANCY_PRODUCER_WAIT_MS);
            if (laneActive.get()) {
                createSubscription();
            }
        } catch (SensorHubException e) {
            if (laneActive.get()) {
                logger.warn("Occupancy producer did not reach STARTED within {} ms; OCR inactive on lane {}",
                        MAX_OCCUPANCY_PRODUCER_WAIT_MS, laneSystem.getUniqueIdentifier());
            }
        } catch (Throwable t) {
            logger.warn("Unexpected error waiting for occupancy producer on lane {}: {}",
                    laneSystem.getUniqueIdentifier(), t.getMessage());
        } finally {
            this.startupThread = null;
        }
    }

    private void createSubscription() {
        if (!laneActive.get())
            return;
        try {
            String producerUid = occupancyProducer.getUniqueIdentifier();
            if (producerUid == null || producerUid.isBlank())
                return;

            this.obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();
            var db = hub.getSystemDriverRegistry().getDatabase(producerUid);
            if (db == null) {
                db = hub.getSystemDriverRegistry().getDatabase(laneSystem.getUniqueIdentifier());
            }
            if (db != null) {
                this.occupancyObsStore = db.getObservationStore();
            }

            hub.getEventBus().newSubscription(ObsEvent.class)
                    .withTopicID(EventUtils.getDataStreamDataTopicID(producerUid, OCCUPANCY_NAME))
                    .subscribe(this::onOccupancyEvent)
                    .thenAccept(sub -> {
                        if (!laneActive.get()) {
                            sub.cancel();
                            return;
                        }
                        this.occupancySub = sub;
                        sub.request(Long.MAX_VALUE);
                        logger.info("Started asynchronous Vehicle OCR subscription on lane {}",
                                laneSystem.getUniqueIdentifier());
                    });
        } catch (Throwable t) {
            logger.error("Failed to subscribe Vehicle OCR to occupancy events on lane {}",
                    laneSystem.getUniqueIdentifier(), t);
            stop();
        }
    }

    private void onTransientNonAlarmClips(Occupancy occupancy,
                                          List<String> videoPaths,
                                          Runnable discardCallback) {
        StashedClips incoming = new StashedClips(
                videoPaths != null ? List.copyOf(videoPaths) : List.of(),
                discardCallback);
        if (!laneActive.get()
                || occupancy == null
                || incoming.videoPaths.isEmpty()
                || config.alarmsOnly
                || (!config.readContainerIds && !config.readPlates)) {
            incoming.discardOnce();
            return;
        }

        OccupancyMatchKey key = OccupancyMatchKey.from(occupancy);
        List<StashedClips> evicted = new ArrayList<>();
        synchronized (stashLock) {
            if (!laneActive.get()) {
                evicted.add(incoming);
            } else {
                StashedClips prev = transientClipStash.put(key, incoming);
                if (prev != null) {
                    evicted.add(prev);
                }
                while (transientClipStash.size() > MAX_STASHED_TRANSIENT_CLIPS) {
                    Iterator<Map.Entry<OccupancyMatchKey, StashedClips>> it =
                            transientClipStash.entrySet().iterator();
                    if (!it.hasNext())
                        break;
                    evicted.add(it.next().getValue());
                    it.remove();
                }
            }
        }
        for (StashedClips old : evicted) {
            old.discardOnce();
        }
        if (evicted.contains(incoming)) {
            return;
        }
        CompletableFuture.delayedExecutor(TRANSIENT_CLIP_TTL_MS, TimeUnit.MILLISECONDS)
                .execute(() -> expireStashedClips(key, incoming));
    }

    private void expireStashedClips(OccupancyMatchKey key, StashedClips expected) {
        boolean removed = false;
        synchronized (stashLock) {
            if (transientClipStash.get(key) == expected) {
                transientClipStash.remove(key);
                removed = true;
            }
        }
        if (removed) {
            expected.discardOnce();
        }
    }

    private StashedClips takeStashedClips(Occupancy occupancy) {
        if (occupancy == null)
            return null;
        OccupancyMatchKey exactKey = OccupancyMatchKey.from(occupancy);
        synchronized (stashLock) {
            StashedClips exact = transientClipStash.remove(exactKey);
            if (exact != null)
                return exact;
            // Fallback within +-5 ms tolerance if text serialization rounded samplingTime
            Iterator<Map.Entry<OccupancyMatchKey, StashedClips>> it =
                    transientClipStash.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<OccupancyMatchKey, StashedClips> entry = it.next();
                if (entry.getKey().occupancyCount == exactKey.occupancyCount
                        && Math.abs(entry.getKey().samplingTimeMillis - exactKey.samplingTimeMillis) <= PHENOMENON_TIME_TOLERANCE_MS) {
                    StashedClips found = entry.getValue();
                    it.remove();
                    return found;
                }
            }
        }
        return null;
    }

    private void clearAllStashedClips() {
        List<StashedClips> drained;
        synchronized (stashLock) {
            drained = new ArrayList<>(transientClipStash.values());
            transientClipStash.clear();
        }
        for (StashedClips stashed : drained) {
            stashed.discardOnce();
        }
    }

    private void onOccupancyEvent(ObsEvent event) {
        if (!laneActive.get() || event == null || event.getObservations() == null)
            return;

        try {
            if (bucketStore == null) {
                resolveBucketService();
            }

            OcrSettings settings = toSettings(config);
            String producerUid = occupancyProducer.getUniqueIdentifier();
            for (IObsData obs : event.getObservations()) {
                if (obs == null || obs.getResult() == null)
                    continue;

                Occupancy occupancy = readOccupancy(obs);
                if (occupancy == null)
                    continue;

                StashedClips stashed = takeStashedClips(occupancy);
                Runnable cleanupCallback = stashed != null ? stashed::discardOnce : () -> {};

                try {
                    if (!laneActive.get() || (!settings.readContainerIds() && !settings.readPlates())) {
                        cleanupCallback.run();
                        continue;
                    }

                    if (config.alarmsOnly && !occupancy.hasGammaAlarm() && !occupancy.hasNeutronAlarm()) {
                        cleanupCallback.run();
                        continue;
                    }

                    List<String> sourcePaths = (occupancy.getVideoPaths() != null && !occupancy.getVideoPaths().isEmpty())
                            ? occupancy.getVideoPaths()
                            : (stashed != null ? stashed.videoPaths : List.of());

                    List<OcrJob.CameraClip> clips = resolveCameraClips(sourcePaths);
                    if (clips.isEmpty()) {
                        cleanupCallback.run();
                        logger.debug("Occupancy on lane {} has no matching camera clips for OCR",
                                laneSystem.getUniqueIdentifier());
                        continue;
                    }

                    AtomicInteger resolverAttempt = new AtomicInteger(0);
                    Instant phenomenonTime = obs.getPhenomenonTime();
                    int targetOccupancyCount = occupancy.getOccupancyCount();
                    double resultTs = obs.getResultTime() != null
                            ? (obs.getResultTime().toEpochMilli() / 1000.0)
                            : occupancy.getSamplingTime();

                    OcrJob job = new OcrJob(
                            laneSystem.getUniqueIdentifier(),
                            null,
                            () -> resolvePersistedOccupancyObsId(
                                    producerUid,
                                    phenomenonTime,
                                    targetOccupancyCount,
                                    resolverAttempt.getAndIncrement() > 0),
                            occupancy.getStartTime(),
                            occupancy.getEndTime(),
                            resultTs,
                            clips,
                            settings,
                            result -> {
                                if (laneActive.get()) {
                                    output.publishData(result);
                                }
                            },
                            cleanupCallback);

                    OcrEngine eng = this.engineLease;
                    VehicleOcrProcessor proc = this.processorLease;
                    if (eng != null && proc != null) {
                        proc.submit(job, eng, laneActive);
                    } else {
                        cleanupCallback.run();
                    }
                } catch (Throwable jobErr) {
                    cleanupCallback.run();
                    throw jobErr;
                }
            }
        } catch (Throwable t) {
            logger.warn("Isolated error queueing Vehicle OCR event on lane {}: {}",
                    laneSystem.getUniqueIdentifier(), t.getMessage());
        }
    }

    private Occupancy readOccupancy(IObsData obs) {
        try {
            DataComponent struct = resolveRecordStructure(obs);
            return OccupancyExtended.readOccupancy(struct, obs.getResult());
        } catch (Exception e) {
            logger.debug("Could not decode occupancy observation for OCR on lane {}: {}",
                    laneSystem.getUniqueIdentifier(), e.getMessage());
            return null;
        }
    }

    private DataComponent resolveRecordStructure(IObsData obs) {
        IObsStore store = this.occupancyObsStore;
        if (store == null || obs == null || obs.getDataStreamID() == null)
            return null;
        try {
            var ds = store.getDataStreams().get(new DataStreamKey(obs.getDataStreamID()));
            return ds != null ? ds.getRecordStructure() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolvePersistedOccupancyObsId(String producerUid,
                                                  Instant phenomenonTime,
                                                  int targetOccupancyCount,
                                                  boolean commitBatchFirst) {
        IObsStore store = this.occupancyObsStore;
        IdEncoder encoder = this.obsIdEncoder;
        if (!laneActive.get() || store == null || encoder == null || producerUid == null)
            return null;

        try {
            if (commitBatchFirst) {
                try {
                    store.commit();
                } catch (Exception e) {
                    logger.debug("Batch commit while resolving OCR occupancy ID on {} deferred: {}",
                            producerUid, e.getMessage());
                }
            }

            var timeFilter = new ObsFilter.Builder()
                    .withSystems()
                        .withUniqueIDs(producerUid)
                        .done()
                    .withDataStreams()
                        .withOutputNames(OCCUPANCY_NAME)
                        .done()
                    .withPhenomenonTime();
            ObsFilter filter = (phenomenonTime != null
                    ? timeFilter.withRange(
                            phenomenonTime.minusMillis(PHENOMENON_TIME_TOLERANCE_MS),
                            phenomenonTime.plusMillis(PHENOMENON_TIME_TOLERANCE_MS))
                    : timeFilter.withAllTimes())
                    .done()
                    .build();

            List<Map.Entry<BigId, IObsData>> entries;
            try (var stream = store.selectEntries(filter)) {
                entries = stream.toList();
            }
            if (entries.isEmpty())
                return null;

            for (var entry : entries) {
                DataComponent struct = resolveRecordStructure(entry.getValue());
                Occupancy candidate = OccupancyExtended.readOccupancy(struct, entry.getValue().getResult());
                if (candidate.getOccupancyCount() == targetOccupancyCount) {
                    return encoder.encodeID(entry.getKey());
                }
            }
            if (entries.size() == 1) {
                return encoder.encodeID(entries.get(0).getKey());
            }
        } catch (Exception e) {
            logger.debug("Unable to resolve persisted occupancy observation ID on {}: {}",
                    producerUid, e.getMessage());
        }
        return null;
    }

    private List<OcrJob.CameraClip> resolveCameraClips(List<String> videoPaths) {
        if (videoPaths == null || videoPaths.isEmpty())
            return List.of();

        List<String> laneCameraUids = new ArrayList<>();
        for (var member : laneSystem.getMembers().values()) {
            if (member instanceof FFMPEGSensorBase<?> cam && cam.getUniqueIdentifier() != null) {
                laneCameraUids.add(cam.getUniqueIdentifier());
            }
        }

        List<OcrJob.CameraClip> resolved = new ArrayList<>();
        for (String rawPath : videoPaths) {
            if (rawPath == null || rawPath.isBlank())
                continue;

            String normalized = rawPath.replace('\\', '/').trim();
            String cameraUid = laneCameraUids.stream()
                    .filter(uid -> normalized.contains(uid.replace(':', '-'))
                            || normalized.contains(uid.replace(':', '_')))
                    .findFirst()
                    .orElse(laneCameraUids.size() == 1 ? laneCameraUids.get(0) : "");

            if (config.cameraUids != null && !config.cameraUids.isEmpty() && !config.cameraUids.contains(cameraUid)) {
                continue;
            }

            Path fsPath = resolveVideoFilesystemPath(normalized, rawPath);
            if (fsPath != null) {
                resolved.add(new OcrJob.CameraClip(cameraUid, fsPath));
            }
        }
        return resolved;
    }

    private Path resolveVideoFilesystemPath(String normalizedForwardSlash, String originalPath) {
        try {
            String key = normalizedForwardSlash;
            while (key.startsWith("/")) {
                key = key.substring(1);
            }
            if (key.startsWith(VIDEO_BUCKET + "/")) {
                key = key.substring((VIDEO_BUCKET + "/").length());
            }
            IBucketStore store = this.bucketStore;
            if (store != null && !key.isBlank()) {
                String rawUri = null;
                try {
                    rawUri = store.getResourceURI(VIDEO_BUCKET, key);
                } catch (Exception notYetWritten) {
                    // MP4 clip may still be finalizing on the FFmpeg recording thread; resolve its
                    // confined target path so VehicleOcrProcessor's timer wheel can retry once ready.
                    try {
                        rawUri = store.getWritableResourceURI(VIDEO_BUCKET, key);
                    } catch (Exception ignored) {
                    }
                }
                if (rawUri != null && !rawUri.isBlank()) {
                    return rawUri.startsWith("file:") ? Paths.get(URI.create(rawUri)) : Paths.get(rawUri);
                }
            }
        } catch (Exception ignored) {
        }
        try {
            Path candidate = Paths.get(originalPath);
            if (Files.exists(candidate) || this.bucketStore == null) {
                return candidate;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static OcrSettings toSettings(OcrConfig cfg) {
        String rawDir = (cfg.modelDir == null || cfg.modelDir.isBlank()) ? "models/ocr" : cfg.modelDir.trim();
        return new OcrSettings(
                Paths.get(rawDir),
                cfg.readContainerIds,
                cfg.readPlates,
                cfg.framesPerVideo,
                cfg.containerMinConfidence,
                cfg.plateMinConfidence,
                cfg.ignoreTopFraction,
                cfg.ignoreBottomFraction);
    }

    public void stop() {
        laneActive.set(false);

        try {
            OccupancyWrapper wrapper = laneSystem.getOccupancyWrapper();
            if (wrapper != null) {
                wrapper.setTransientNonAlarmClipConsumer(null);
            }
        } catch (Exception ignored) {
        }
        clearAllStashedClips();

        Thread init = this.startupThread;
        this.startupThread = null;
        if (init != null) {
            init.interrupt();
        }

        Flow.Subscription sub = this.occupancySub;
        this.occupancySub = null;
        if (sub != null) {
            try {
                sub.cancel();
            } catch (Exception ignored) {
            }
        }

        VehicleOcrProcessor proc = this.processorLease;
        this.processorLease = null;
        if (proc != null) {
            try {
                proc.close();
            } catch (Exception ignored) {
            }
        }

        OcrEngine eng = this.engineLease;
        this.engineLease = null;
        if (eng != null) {
            try {
                eng.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void close() {
        stop();
    }

    private record OccupancyMatchKey(long samplingTimeMillis, int occupancyCount) {
        static OccupancyMatchKey from(Occupancy occupancy) {
            double ts = occupancy.getSamplingTime() > 0 ? occupancy.getSamplingTime() : occupancy.getStartTime();
            return new OccupancyMatchKey(Math.round(ts * 1000.0), occupancy.getOccupancyCount());
        }
    }

    private static final class StashedClips {
        final List<String> videoPaths;
        private final Runnable discardCallback;
        private final AtomicBoolean discarded = new AtomicBoolean(false);

        StashedClips(List<String> videoPaths, Runnable discardCallback) {
            this.videoPaths = videoPaths;
            this.discardCallback = discardCallback;
        }

        void discardOnce() {
            if (discarded.compareAndSet(false, true) && discardCallback != null) {
                try {
                    discardCallback.run();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
