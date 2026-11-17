/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.pipeline;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.process.ocr.domain.CandidateAggregator;
import com.botts.impl.process.ocr.engine.OcrEngine;
import com.botts.impl.process.ocr.engine.OcrRead;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Asynchronous, resource-governed background processor for per-lane container and license-plate OCR.
 *
 * <p>Key operational guarantees:</p>
 * <ul>
 *   <li><strong>Zero blocking on caller/EventBus threads:</strong> {@link #submit(OcrJob, OcrEngine, AtomicBoolean)}
 *       schedules onto a non-blocking 1-thread timer wheel and returns immediately.</li>
 *   <li><strong>Resource-governed execution:</strong> Consults {@link OcrResourceGovernor} before
 *       starting each job and between frames, deferring non-blockingly under elevated CPU/heap load
 *       and shedding safely under critical heap pressure.</li>
 *   <li><strong>Bounded queue with drop-oldest backpressure:</strong> Burst occupancies shed the
 *       oldest queued OCR task when the queue reaches capacity so fresh alarms are processed first
 *       and memory cannot grow without bound.</li>
 *   <li><strong>Complete fault barrier:</strong> All timer and worker callbacks catch
 *       {@link Throwable} so OCR errors never impact lane telemetry, cameras, or SensorHub.</li>
 * </ul>
 */
public class VehicleOcrProcessor implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(VehicleOcrProcessor.class);

    public static final int DEFAULT_QUEUE_CAPACITY = 32;
    public static final long DEFAULT_INITIAL_SETTLE_DELAY_MS = 300L;
    public static final long DEFAULT_RESOURCE_DEFER_DELAY_MS = 1_500L;
    public static final int MAX_RESOURCE_DEFERRALS = 8;
    public static final int MAX_PUBLISHED_CANDIDATES = 6;

    private static final long[] DEFAULT_OBS_ID_RETRY_DELAYS_MS = {250L, 500L, 1_000L, 1_500L, 2_500L, 4_000L};
    private static final long[] DEFAULT_CLIP_RETRY_DELAYS_MS = {500L, 1_500L, 3_000L, 6_000L};

    private static final String VIDEO_BUCKET = "videos";
    private static final String RETAINED_EVENTS_PREFIX = "retained-media/v1/events/";

    private static final Object SHARED_LOCK = new Object();
    private static VehicleOcrProcessor sharedInstance;
    private static int sharedRefCount = 0;

    private final FrameSampler frameSampler;
    private volatile IBucketStore bucketStore;
    private volatile IBucketService bucketService;
    private volatile EvidenceWriter cachedEvidenceWriter;
    private final OcrResourceGovernor governor;
    private final int queueCapacity;
    private final long initialSettleDelayMs;
    private final long[] obsIdRetryDelaysMs;
    private final long[] clipRetryDelaysMs;
    private final long resourceDeferDelayMs;

    private final ArrayBlockingQueue<Runnable> workQueue;
    private final ThreadPoolExecutor workerPool;
    private final ScheduledExecutorService timerWheel;
    private final Set<TaskState> inFlightStates = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final AtomicLong jobsSubmitted = new AtomicLong();
    private final AtomicLong jobsCompleted = new AtomicLong();
    private final AtomicLong jobsDeferred = new AtomicLong();
    private final AtomicLong jobsDroppedOverflow = new AtomicLong();
    private final AtomicLong jobsShedHeap = new AtomicLong();
    private final AtomicLong totalInferenceMillis = new AtomicLong();

    public static VehicleOcrProcessor acquireShared(IBucketStore bucketStore) {
        return acquireShared(bucketStore, null);
    }

    /**
     * Acquires a reference-counted process-wide shared {@link VehicleOcrProcessor} so multiple
     * OCR-enabled lanes share one bounded low-priority background worker rather than spawning
     * competing thread pools.
     */
    public static VehicleOcrProcessor acquireShared(IBucketStore bucketStore, IBucketService bucketService) {
        synchronized (SHARED_LOCK) {
            if (sharedInstance == null || sharedInstance.closed.get()) {
                sharedInstance = new VehicleOcrProcessor(
                        new FrameSampler(),
                        bucketStore,
                        bucketService,
                        new OcrResourceGovernor(),
                        DEFAULT_QUEUE_CAPACITY,
                        DEFAULT_INITIAL_SETTLE_DELAY_MS,
                        DEFAULT_OBS_ID_RETRY_DELAYS_MS,
                        DEFAULT_CLIP_RETRY_DELAYS_MS,
                        DEFAULT_RESOURCE_DEFER_DELAY_MS);
                sharedRefCount = 0;
            } else {
                boolean updated = false;
                if (bucketStore != null && sharedInstance.bucketStore == null) {
                    sharedInstance.bucketStore = bucketStore;
                    updated = true;
                }
                if (bucketService != null && sharedInstance.bucketService == null) {
                    sharedInstance.bucketService = bucketService;
                    updated = true;
                }
                if (updated) {
                    sharedInstance.cachedEvidenceWriter = null;
                }
            }
            sharedRefCount++;
            final VehicleOcrProcessor target = sharedInstance;
            return new SharedLease(target);
        }
    }

    private static void releaseShared(VehicleOcrProcessor target) {
        VehicleOcrProcessor toClose = null;
        synchronized (SHARED_LOCK) {
            if (sharedInstance == target) {
                sharedRefCount--;
                if (sharedRefCount <= 0) {
                    sharedRefCount = 0;
                    toClose = sharedInstance;
                    sharedInstance = null;
                }
            }
        }
        if (toClose != null) {
            toClose.close();
        }
    }

    private static final class SharedLease extends VehicleOcrProcessor {
        private final VehicleOcrProcessor delegate;
        private final AtomicBoolean leaseClosed = new AtomicBoolean(false);

        SharedLease(VehicleOcrProcessor delegate) {
            super(true);
            this.delegate = delegate;
        }

        @Override
        public void submit(OcrJob job, OcrEngine engine, AtomicBoolean laneActive) {
            if (!leaseClosed.get()) {
                delegate.submit(job, engine, laneActive);
            } else if (job != null) {
                runCleanupQuietly(job);
            }
        }

        @Override
        public long getJobsSubmitted() { return delegate.getJobsSubmitted(); }
        @Override
        public long getJobsCompleted() { return delegate.getJobsCompleted(); }
        @Override
        public long getJobsDeferred() { return delegate.getJobsDeferred(); }
        @Override
        public long getJobsDroppedOverflow() { return delegate.getJobsDroppedOverflow(); }
        @Override
        public long getJobsShedHeap() { return delegate.getJobsShedHeap(); }
        @Override
        public long getTotalInferenceMillis() { return delegate.getTotalInferenceMillis(); }
        @Override
        public int getQueueDepth() { return delegate.getQueueDepth(); }

        @Override
        public void close() {
            if (leaseClosed.compareAndSet(false, true)) {
                releaseShared(delegate);
            }
        }
    }

    /** Private no-allocation constructor used only by {@link SharedLease}. */
    private VehicleOcrProcessor(boolean leaseStub) {
        this.frameSampler = null;
        this.bucketStore = null;
        this.bucketService = null;
        this.governor = null;
        this.queueCapacity = 0;
        this.initialSettleDelayMs = 0L;
        this.obsIdRetryDelaysMs = new long[0];
        this.clipRetryDelaysMs = new long[0];
        this.resourceDeferDelayMs = 0L;
        this.workQueue = null;
        this.workerPool = null;
        this.timerWheel = null;
    }

    public VehicleOcrProcessor(FrameSampler frameSampler,
                               IBucketStore bucketStore,
                               OcrResourceGovernor governor,
                               int queueCapacity,
                               long initialSettleDelayMs,
                               long[] obsIdRetryDelaysMs,
                               long[] clipRetryDelaysMs,
                               long resourceDeferDelayMs) {
        this(frameSampler, bucketStore, null, governor, queueCapacity,
                initialSettleDelayMs, obsIdRetryDelaysMs, clipRetryDelaysMs, resourceDeferDelayMs);
    }

    public VehicleOcrProcessor(FrameSampler frameSampler,
                               IBucketStore bucketStore,
                               IBucketService bucketService,
                               OcrResourceGovernor governor,
                               int queueCapacity,
                               long initialSettleDelayMs,
                               long[] obsIdRetryDelaysMs,
                               long[] clipRetryDelaysMs,
                               long resourceDeferDelayMs) {
        this.frameSampler = frameSampler != null ? frameSampler : new FrameSampler();
        this.bucketStore = bucketStore;
        this.bucketService = bucketService;
        this.governor = governor != null ? governor : new OcrResourceGovernor();
        this.queueCapacity = Math.max(1, queueCapacity);
        this.initialSettleDelayMs = Math.max(0L, initialSettleDelayMs);
        this.obsIdRetryDelaysMs = obsIdRetryDelaysMs != null ? obsIdRetryDelaysMs.clone() : DEFAULT_OBS_ID_RETRY_DELAYS_MS;
        this.clipRetryDelaysMs = clipRetryDelaysMs != null ? clipRetryDelaysMs.clone() : DEFAULT_CLIP_RETRY_DELAYS_MS;
        this.resourceDeferDelayMs = Math.max(10L, resourceDeferDelayMs);

        this.workQueue = new ArrayBlockingQueue<>(this.queueCapacity);
        this.workerPool = new ThreadPoolExecutor(
                1,
                1,
                60L,
                TimeUnit.SECONDS,
                workQueue,
                r -> {
                    Thread t = new Thread(r, "oscar-ocr-worker");
                    t.setDaemon(true);
                    t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.MIN_PRIORITY + 1));
                    return t;
                });
        this.workerPool.allowCoreThreadTimeOut(true);

        this.timerWheel = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "oscar-ocr-scheduler");
            t.setDaemon(true);
            t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 2));
            return t;
        });
    }

    /**
     * Non-blockingly schedules {@code job} for asynchronous execution as system resources allow.
     * Safe to invoke directly from SensorHub {@code EventBus} subscriber callbacks.
     */
    public void submit(OcrJob job, OcrEngine engine, AtomicBoolean laneActive) {
        if (job == null)
            return;
        if (engine == null || closed.get() || (laneActive != null && !laneActive.get())) {
            runCleanupQuietly(job);
            return;
        }

        jobsSubmitted.incrementAndGet();
        TaskState state = new TaskState(job, engine, laneActive, inFlightStates);
        inFlightStates.add(state);
        if (closed.get()) {
            state.runCleanupOnce();
            return;
        }
        scheduleOnTimer(state, initialSettleDelayMs);
    }

    private void scheduleOnTimer(TaskState state, long delayMs) {
        if (closed.get() || !state.isActive()) {
            state.runCleanupOnce();
            return;
        }
        try {
            if (delayMs <= 0L) {
                enqueueWorker(state);
            } else {
                timerWheel.schedule(() -> {
                    try {
                        enqueueWorker(state);
                    } catch (Throwable t) {
                        state.runCleanupOnce();
                        logger.warn("Unexpected error enqueueing OCR job for lane {}", state.job.laneUid(), t);
                    }
                }, delayMs, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            state.runCleanupOnce();
            logger.debug("OCR scheduler rejected job for lane {}: {}", state.job.laneUid(), t.getMessage());
        }
    }

    private void enqueueWorker(TaskState state) {
        if (closed.get() || !state.isActive()) {
            state.runCleanupOnce();
            return;
        }
        QueuedTask runner = new QueuedTask(state, () -> executeSafely(state));
        while (!workQueue.offer(runner)) {
            Runnable evicted = workQueue.poll();
            if (evicted != null) {
                if (evicted instanceof QueuedTask qt) {
                    qt.state.runCleanupOnce();
                }
                jobsDroppedOverflow.incrementAndGet();
                logger.warn("OCR background queue full ({}); dropped oldest pending OCR job to keep up with live occupancies",
                        queueCapacity);
            }
        }
        if (closed.get()) {
            drainAndCleanupWorkQueue();
            return;
        }
        try {
            if (workerPool.getPoolSize() == 0) {
                workerPool.prestartCoreThread();
            }
        } catch (Throwable ignored) {
        }
    }

    private void executeSafely(TaskState state) {
        boolean rescheduled = false;
        try {
            if (closed.get() || !state.isActive())
                return;

            // Step 1: Check CPU & JVM heap headroom before doing any heavy native decode or inference
            OcrResourceGovernor.Decision decision =
                    governor.evaluateBeforeJob(state.resourceDefers, MAX_RESOURCE_DEFERRALS);
            if (decision == OcrResourceGovernor.Decision.DEFER) {
                state.resourceDefers++;
                jobsDeferred.incrementAndGet();
                logger.debug("Deferring OCR job for lane {} (deferral {}/{}) while CPU/heap is busy",
                        state.job.laneUid(), state.resourceDefers, MAX_RESOURCE_DEFERRALS);
                rescheduled = true;
                scheduleOnTimer(state, resourceDeferDelayMs);
                return;
            }
            if (decision == OcrResourceGovernor.Decision.SHED_HEAP_PRESSURE) {
                jobsShedHeap.incrementAndGet();
                logger.warn("Shedding OCR job for lane {} due to sustained critical JVM heap pressure",
                        state.job.laneUid());
                return;
            }

            // Step 2: Resolve durable occupancy observation ID asynchronously (zero EventBus sleep)
            if (state.resolvedObsId == null || state.resolvedObsId.isBlank()) {
                String resolved = state.job.occupancyObsIdResolver() != null
                        ? state.job.occupancyObsIdResolver().get()
                        : null;
                if (resolved != null && !resolved.isBlank()) {
                    state.resolvedObsId = resolved;
                } else if (state.obsIdRetries < obsIdRetryDelaysMs.length) {
                    long delay = obsIdRetryDelaysMs[state.obsIdRetries++];
                    rescheduled = true;
                    scheduleOnTimer(state, delay);
                    return;
                } else {
                    logger.debug("Skipping OCR job for lane {}: occupancy observation ID not found after {} retries",
                            state.job.laneUid(), state.obsIdRetries);
                    return;
                }
            }

            // Step 3: Sample frames (prefer committed RetainedMedia 5-frame JPEGs if already present,
            // otherwise decode directly from finalized MP4 clips).
            boolean ecoMode = state.resourceDefers >= MAX_RESOURCE_DEFERRALS;
            int maxFrames = ecoMode
                    ? Math.min(2, state.job.settings().framesPerVideo())
                    : state.job.settings().framesPerVideo();

            long tStartNs = System.nanoTime();
            List<LoadedFrame> loadedFrames = loadFrames(state, maxFrames);
            if (loadedFrames.isEmpty()) {
                if (state.clipRetries < clipRetryDelaysMs.length) {
                    long delay = clipRetryDelaysMs[state.clipRetries++];
                    rescheduled = true;
                    scheduleOnTimer(state, delay);
                    return;
                }
                logger.debug("No decodable video frames available for lane {} occupancy {} after {} retries",
                        state.job.laneUid(), state.resolvedObsId, state.clipRetries);
                return;
            }

            // Step 4: Run ONNX detection + recognition frame-by-frame with cooperative resource checks
            try {
                runInferenceAndPublish(state, loadedFrames, ecoMode);
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tStartNs);
                long totalMs = totalInferenceMillis.addAndGet(Math.max(0L, elapsedMs));
                long completed = jobsCompleted.incrementAndGet();
                if (completed == 1L || completed % 10L == 0L) {
                    logger.info("OCR governor telemetry: submitted={} completed={} deferred={} droppedOverflow={} shedHeap={} queueDepth={} lastJobMs={} avgJobMs={}",
                            jobsSubmitted.get(), completed, jobsDeferred.get(),
                            jobsDroppedOverflow.get(), jobsShedHeap.get(),
                            workQueue.size(), elapsedMs, totalMs / Math.max(1L, completed));
                }
            } finally {
                for (LoadedFrame lf : loadedFrames) {
                    if (lf.image != null) {
                        lf.image.flush();
                    }
                }
            }
        } catch (Throwable t) {
            logger.warn("Isolated OCR fault on lane {} (system unaffected): {}",
                    state.job.laneUid(), t.toString(), t);
        } finally {
            if (!rescheduled) {
                state.runCleanupOnce();
            }
        }
    }

    private void runInferenceAndPublish(TaskState state,
                                        List<LoadedFrame> loadedFrames,
                                        boolean ecoMode) throws InterruptedException {
        CandidateAggregator aggregator = new CandidateAggregator(state.job.settings());
        List<BufferedImage> images = new ArrayList<>(loadedFrames.size());
        for (LoadedFrame lf : loadedFrames) {
            images.add(lf.image);
        }

        AtomicBoolean criticalHeapLogged = new AtomicBoolean(false);
        List<CandidateAggregator.FrameInput> frameInputs = TwoTierFrameScanner.scanFrames(
                state.engine,
                images,
                aggregator,
                () -> {
                    if (closed.get() || !state.isActive())
                        return true;
                    if (governor.isCriticalHeapPressure()) {
                        if (criticalHeapLogged.compareAndSet(false, true)) {
                            logger.warn("Stopping remaining OCR frame scans early on lane {} due to critical heap pressure",
                                    state.job.laneUid());
                        }
                        return true;
                    }
                    return false;
                },
                () -> ecoMode || governor.isElevatedLoad(),
                () -> !ecoMode
                        && state.job.settings().readContainerIds()
                        && !governor.isElevatedLoad()
                        && (workQueue == null || workQueue.size() < 4),
                ecoMode);

        if (closed.get() || !state.isActive() || frameInputs.isEmpty())
            return;

        List<CandidateAggregator.AggregatedCandidate> candidates = aggregator.aggregate(frameInputs);
        if (candidates.isEmpty())
            return;

        EvidenceWriter evidenceWriter = resolveEvidenceWriter();
        double baseTs = state.job.resultTimestamp() > 0
                ? state.job.resultTimestamp()
                : (System.currentTimeMillis() / 1000.0);
        double occStartSec = state.job.occupancyStartTime() > 0
                ? state.job.occupancyStartTime()
                : baseTs;
        Instant cropTime = Instant.ofEpochMilli((long) (baseTs * 1000.0));

        int count = Math.min(MAX_PUBLISHED_CANDIDATES, candidates.size());
        for (int idx = 0; idx < count; idx++) {
            if (closed.get() || !state.isActive())
                return;
            CandidateAggregator.AggregatedCandidate c = candidates.get(idx);
            LoadedFrame bestFrame = loadedFrames.get(
                    Math.max(0, Math.min(loadedFrames.size() - 1, c.bestFrameIndex())));

            String cropKey = "";
            if (evidenceWriter != null && c.bestBoundingBox() != null) {
                cropKey = evidenceWriter.writeCrop(
                        bestFrame.image,
                        c.bestBoundingBox(),
                        cropTime,
                        state.resolvedObsId);
            }

            // Offset by +1 ms per additional candidate so multiple candidates on one occupancy
            // never collide on identical millisecond timestamps in ConSys datastreams.
            Instant sampleTime = Instant.ofEpochMilli((long) (baseTs * 1000.0) + idx);
            Instant frameTime = Instant.ofEpochMilli(
                    (long) ((occStartSec + Math.max(0.0, bestFrame.timestampSeconds)) * 1000.0));

            VehicleOcrResult result = new VehicleOcrResult.Builder()
                    .sampleTime(sampleTime)
                    .occupancyObsId(state.resolvedObsId)
                    .idType(c.idType())
                    .value(c.rawText())
                    .normalizedValue(c.normalizedValue())
                    .checksumValid(c.checksumValid())
                    .confidence((float) c.confidence())
                    .readCount(c.readCount())
                    .cameraUid(bestFrame.cameraUid)
                    .frameTime(frameTime)
                    .evidenceImagePath(cropKey)
                    .build();

            state.job.resultConsumer().accept(result);
        }
    }

    private EvidenceWriter resolveEvidenceWriter() {
        IBucketStore store = this.bucketStore;
        if (store == null)
            return null;
        EvidenceWriter existing = this.cachedEvidenceWriter;
        if (existing != null)
            return existing;
        EvidenceWriter created = new EvidenceWriter(store, this.bucketService);
        this.cachedEvidenceWriter = created;
        return created;
    }

    private List<LoadedFrame> loadFrames(TaskState state, int maxFramesPerCamera) {
        // Fast path: reuse RetainedMedia 5-frame sRGB JPEGs when already published in videos bucket
        List<LoadedFrame> retained = tryLoadRetainedJpegs(
                state.resolvedObsId,
                maxFramesPerCamera,
                Math.max(0.0, state.job.occupancyEndTime() - state.job.occupancyStartTime()));
        if (!retained.isEmpty())
            return retained;

        if (state.job.clips() == null || state.job.clips().isEmpty())
            return List.of();

        List<LoadedFrame> decoded = new ArrayList<>();
        for (OcrJob.CameraClip clip : state.job.clips()) {
            if (clip == null || clip.videoPath() == null)
                continue;
            List<FrameSampler.SampledFrame> sampled =
                    frameSampler.sampleFrames(clip.videoPath(), maxFramesPerCamera);
            for (FrameSampler.SampledFrame sf : sampled) {
                if (sf.image() != null) {
                    decoded.add(new LoadedFrame(
                            clip.cameraUid() != null ? clip.cameraUid() : "",
                            sf.timestampSeconds(),
                            sf.image()));
                }
            }
        }
        return decoded;
    }

    private List<LoadedFrame> tryLoadRetainedJpegs(String occupancyObsId,
                                                   int maxFramesPerCamera,
                                                   double durationSec) {
        if (bucketStore == null || occupancyObsId == null || occupancyObsId.isBlank())
            return List.of();
        try {
            if (!bucketStore.bucketExists(VIDEO_BUCKET))
                return List.of();
            String encodedEvent = URLEncoder.encode(occupancyObsId, StandardCharsets.UTF_8);
            String prefix = RETAINED_EVENTS_PREFIX + encodedEvent + "/";
            List<String> keys = bucketStore.listObjects(VIDEO_BUCKET, prefix);
            if (keys == null || keys.isEmpty())
                return List.of();

            Set<String> committedDirs = new HashSet<>();
            Map<String, List<String>> jpegsByDir = new HashMap<>();
            for (String rawKey : keys) {
                String key = rawKey.replace('\\', '/');
                int slash = key.lastIndexOf('/');
                if (slash <= 0)
                    continue;
                String dir = key.substring(0, slash);
                String name = key.substring(slash + 1);
                if ("manifest.json".equals(name)) {
                    committedDirs.add(dir);
                } else if (name.startsWith("frame-") && name.endsWith(".jpg")) {
                    jpegsByDir.computeIfAbsent(dir, k -> new ArrayList<>()).add(key);
                }
            }
            if (committedDirs.isEmpty())
                return List.of();

            List<LoadedFrame> out = new ArrayList<>();
            for (String dir : committedDirs) {
                List<String> frameKeys = jpegsByDir.getOrDefault(dir, List.of());
                if (frameKeys.isEmpty())
                    continue;
                Collections.sort(frameKeys);
                String cameraUid = extractCameraSegment(dir);
                int take = Math.min(maxFramesPerCamera, frameKeys.size());
                for (int i = 0; i < take; i++) {
                    double frac = take <= 1 ? 0.5 : (i / (double) (take - 1));
                    String fk = frameKeys.get((int) Math.round(frac * (frameKeys.size() - 1)));
                    try (InputStream in = bucketStore.getObject(VIDEO_BUCKET, fk)) {
                        if (in != null) {
                            BufferedImage img = ImageIO.read(in);
                            if (img != null) {
                                out.add(new LoadedFrame(cameraUid, frac * durationSec, img));
                            }
                        }
                    }
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String extractCameraSegment(String dir) {
        int slash = dir.lastIndexOf('/');
        if (slash < 0 || slash + 1 >= dir.length())
            return "";
        try {
            return java.net.URLDecoder.decode(dir.substring(slash + 1), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return dir.substring(slash + 1);
        }
    }

    public long getJobsSubmitted() { return jobsSubmitted.get(); }
    public long getJobsCompleted() { return jobsCompleted.get(); }
    public long getJobsDeferred() { return jobsDeferred.get(); }
    public long getJobsDroppedOverflow() { return jobsDroppedOverflow.get(); }
    public long getJobsShedHeap() { return jobsShedHeap.get(); }
    public long getTotalInferenceMillis() { return totalInferenceMillis.get(); }
    public int getQueueDepth() { return workQueue != null ? workQueue.size() : 0; }

    private void drainAndCleanupWorkQueue() {
        if (workQueue == null)
            return;
        List<Runnable> drained = new ArrayList<>();
        workQueue.drainTo(drained);
        for (Runnable r : drained) {
            if (r instanceof QueuedTask qt) {
                qt.state.runCleanupOnce();
            }
        }
    }

    private static void runCleanupQuietly(OcrJob job) {
        if (job == null)
            return;
        try {
            job.cleanupCallback().run();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true))
            return;
        if (timerWheel != null)
            timerWheel.shutdownNow();
        if (workerPool != null) {
            drainAndCleanupWorkQueue();
            workerPool.shutdownNow();
        }
        for (TaskState state : List.copyOf(inFlightStates)) {
            state.runCleanupOnce();
        }
    }

    private record LoadedFrame(String cameraUid, double timestampSeconds, BufferedImage image) {}

    private static final class QueuedTask implements Runnable {
        final TaskState state;
        final Runnable delegate;

        QueuedTask(TaskState state, Runnable delegate) {
            this.state = state;
            this.delegate = delegate;
        }

        @Override
        public void run() {
            delegate.run();
        }
    }

    private static final class TaskState {
        final OcrJob job;
        final OcrEngine engine;
        final AtomicBoolean laneActive;
        final Set<TaskState> ownerSet;
        final AtomicBoolean cleanedUp = new AtomicBoolean(false);
        String resolvedObsId;
        int obsIdRetries = 0;
        int clipRetries = 0;
        int resourceDefers = 0;

        TaskState(OcrJob job, OcrEngine engine, AtomicBoolean laneActive, Set<TaskState> ownerSet) {
            this.job = job;
            this.engine = engine;
            this.laneActive = laneActive;
            this.ownerSet = ownerSet;
            this.resolvedObsId = job.initialOccupancyObsId();
        }

        boolean isActive() {
            return laneActive == null || laneActive.get();
        }

        void runCleanupOnce() {
            if (cleanedUp.compareAndSet(false, true)) {
                if (ownerSet != null) {
                    ownerSet.remove(this);
                }
                runCleanupQuietly(job);
            }
        }
    }
}
