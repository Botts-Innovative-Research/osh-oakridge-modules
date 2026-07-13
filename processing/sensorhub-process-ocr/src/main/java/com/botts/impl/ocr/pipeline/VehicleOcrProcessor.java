package com.botts.impl.ocr.pipeline;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.ocr.api.OcrEngine;
import com.botts.impl.ocr.api.OcrEngineException;
import com.botts.impl.ocr.api.OcrRead;
import com.botts.impl.ocr.onnx.OnnxOcrEngine;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.impl.sensor.ffmpeg.controls.FileControl;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Process-wide OCR work queue: one worker thread, bounded queue, lazily
 * initialized ONNX engine shared by all enabled lanes. A node with OCR
 * disabled on every lane never touches this class, so ONNX Runtime and the
 * models are only loaded when the first enabled lane submits a job.
 */
public class VehicleOcrProcessor {

    static final int QUEUE_CAPACITY = 8;

    private static final Logger logger = LoggerFactory.getLogger(VehicleOcrProcessor.class);

    private static volatile ThreadPoolExecutor executor;
    private static OcrEngine engine;
    private static Path engineModelDir;
    private static final Object engineLock = new Object();

    private VehicleOcrProcessor() {}

    /**
     * Queues OCR of one occupancy's recorded clips. Results are delivered on
     * the worker thread, one call per candidate. Never throws: failures are
     * logged and produce no results.
     */
    public static void submit(ISensorHub hub, OcrJob job, Consumer<VehicleOcrResult> onResult) {
        getExecutor().execute(() -> {
            try {
                process(hub, job, onResult);
            } catch (Throwable t) {
                logger.error("OCR job failed for lane {} occupancy {}", job.laneUid, job.occupancyObsId, t);
            }
        });
    }

    private static ThreadPoolExecutor getExecutor() {
        ThreadPoolExecutor local = executor;
        if (local == null) {
            synchronized (VehicleOcrProcessor.class) {
                if (executor == null) {
                    executor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
                            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                            runnable -> {
                                Thread thread = new Thread(runnable, "vehicle-ocr");
                                thread.setDaemon(true);
                                return thread;
                            },
                            (runnable, pool) -> {
                                logger.warn("OCR queue full; dropping oldest job");
                                pool.getQueue().poll();
                                pool.execute(runnable);
                            });
                    executor.allowCoreThreadTimeOut(true);
                }
                local = executor;
            }
        }
        return local;
    }

    private static OcrEngine getEngine(Path modelDir) throws OcrEngineException {
        synchronized (engineLock) {
            if (engine != null) {
                if (!modelDir.equals(engineModelDir))
                    logger.warn("OCR engine already initialized from {}; ignoring different modelDir {}",
                            engineModelDir, modelDir);
                return engine;
            }
            OcrEngine newEngine = new OnnxOcrEngine();
            newEngine.init(modelDir);
            engine = newEngine;
            engineModelDir = modelDir;
            return engine;
        }
    }

    private static void process(ISensorHub hub, OcrJob job, Consumer<VehicleOcrResult> onResult) throws Exception {
        long startedAt = System.nanoTime();
        OcrEngine ocrEngine = getEngine(Paths.get(job.settings.modelDir).toAbsolutePath());

        IBucketStore bucketStore = getBucketStore(hub);
        if (bucketStore == null) {
            logger.error("No bucket service available; cannot resolve occupancy videos");
            return;
        }

        Mp4FrameSampler sampler = new Mp4FrameSampler();
        List<CandidateAggregator.FrameRead> frameReads = new ArrayList<>();

        for (OcrJob.VideoRef video : job.videos) {
            String key = bucketRelativeToKey(video.videoPath);
            List<Mp4FrameSampler.Frame> frames;
            try {
                String absolutePath = bucketStore.getResourceURI(FileControl.VIDEO_BUCKET, key);
                frames = sampler.sample(absolutePath, job.settings.framesPerVideo);
            } catch (Exception e) {
                logger.warn("Skipping clip {} for lane {}: {}", video.videoPath, job.laneUid, e.getMessage());
                continue;
            }

            for (Mp4FrameSampler.Frame frame : frames) {
                Instant frameTime = job.occupancyStartTime
                        .plus(Duration.ofMillis((long) (frame.ptsSeconds * 1000)));
                List<OcrRead> reads;
                try {
                    reads = ocrEngine.read(frame.image);
                } catch (OcrEngineException e) {
                    logger.warn("OCR failed on a frame of {}: {}", video.videoPath, e.getMessage());
                    continue;
                }
                for (OcrRead read : reads)
                    frameReads.add(new CandidateAggregator.FrameRead(read, frame.image, video.cameraUid, frameTime));
            }
        }

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(job.settings).aggregate(frameReads);

        EvidenceWriter evidenceWriter = new EvidenceWriter(bucketStore);
        int sequence = 0;
        for (CandidateAggregator.Candidate candidate : candidates) {
            String evidencePath = evidenceWriter.writeCrop(job.laneUid, job.occupancyStartTime,
                    candidate.idType, sequence++, candidate.evidenceFrame, candidate.evidenceBox);

            onResult.accept(new VehicleOcrResult.Builder()
                    .sampleTime(Instant.now())
                    .occupancyObsId(job.occupancyObsId)
                    .idType(candidate.idType)
                    .value(candidate.value)
                    .normalizedValue(candidate.normalizedValue)
                    .checksumValid(candidate.checksumValid)
                    .confidence(candidate.confidence)
                    .readCount(candidate.readCount)
                    .cameraUid(candidate.cameraUid)
                    .frameTime(candidate.frameTime)
                    .evidenceImagePath(evidencePath)
                    .build());
        }

        logger.info("OCR of occupancy {} on lane {} produced {} candidate(s) in {} ms",
                job.occupancyObsId, job.laneUid, candidates.size(),
                (System.nanoTime() - startedAt) / 1_000_000);
    }

    private static IBucketStore getBucketStore(ISensorHub hub) {
        try {
            IBucketService bucketService = hub.getModuleRegistry().getModuleByType(IBucketService.class);
            return bucketService != null ? bucketService.getBucketStore() : null;
        } catch (Exception e) {
            logger.error("Failed to look up bucket service", e);
            return null;
        }
    }

    /** Strips the leading bucket segment from a bucket-relative video path. */
    static String bucketRelativeToKey(String videoPath) {
        String normalized = videoPath.replace('\\', '/').trim();
        String prefix = FileControl.VIDEO_BUCKET + "/";
        return normalized.startsWith(prefix) ? normalized.substring(prefix.length()) : normalized;
    }
}
