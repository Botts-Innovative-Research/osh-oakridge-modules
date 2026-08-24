package com.botts.impl.service.oscar.video;

import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoRetention {

    private static final Logger logger = LoggerFactory.getLogger(VideoRetention.class);
    private static final int MAX_PROCESS_TIME_MULTIPLIER = 5;
    private static final int DEFAULT_VIDEO_DURATION_MS = 100_000;

    final IBucketStore bucketStore;
    volatile boolean hasStarted = false;
    int frameCount;

    SlidingOccupancyQuery retentionQuery;

    public VideoRetention(ISensorHub hub, IBucketStore bucketStore, TemporalAmount queryPeriod, TemporalAmount retentionTimeOffset, int frameCount) {
        this.bucketStore = bucketStore;
        this.frameCount = frameCount;

        SlidingOccupancyQuery.QueryAction queryAction = frameCount > 0 ? this::decimateOccupancyVideo : this::deleteOccupancyVideo;

        retentionQuery = new SlidingOccupancyQuery(hub,
                queryPeriod,
                queryAction,
                retentionTimeOffset);
    }

    public synchronized void start() {
        if (hasStarted) {
            logger.warn("Video retention already started");
            return;
        }

        hasStarted = true;
        retentionQuery.start();
    }

    private boolean deleteOccupancyVideo(Occupancy occupancy) {
        for (String videoFile : occupancy.getVideoPaths()) {
            logger.info("deleting, {}", videoFile);
            if (!bucketStore.objectExists("", videoFile)) {
                logger.info("Video file {} does not exist or has already been deleted", videoFile);
                return false;
            } else {
                try {
                    bucketStore.deleteObject("", videoFile);
                } catch (DataStoreException e) {
                    logger.info("Failed to delete video file {}", videoFile, e);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean decimateOccupancyVideo(Occupancy occupancy) {
        for (String videoFile : occupancy.getVideoPaths()) {
            logger.info("decimating, {}", occupancy.getVideoPaths().get(0));
            if (!bucketStore.objectExists("", videoFile)) {
                logger.info("Video file {} does not exist", videoFile);
                continue;
            }
            try {
                if (!decimate(bucketStore.getResourceURI("", videoFile))) {
                    logger.info("Video file was already decimated {}", videoFile);
                    return false;
                }
            } catch (DataStoreException e) {
                logger.warn("Failed to decimate video file {}", videoFile, e);
                continue;
            }
        }
        return true;
    }

    public synchronized void stop() {
        if (!hasStarted)
            return;

        hasStarted = false;
        retentionQuery.stop();
    }


    /**
     *
     * @param fileName Result of getResourceURI on an object from the bucket store
     * @return true if input was not already decimated (fps > 1)
     */
    public boolean decimate(String fileName) {

        String originalMp4 = fileName;
        String decimatedMp4 = fileName.substring(0, fileName.lastIndexOf('.')) + "_decimated.mp4";

        MpegTsProcessor videoInput = new MpegTsProcessor(originalMp4);
        VideoKeyframeDecimator videoOutput = null;
        boolean success = true;
        final AtomicBoolean decimateFinished = new AtomicBoolean(false);

        videoInput.setInjectExtradata(false);
        videoInput.openStream();
        videoInput.queryEmbeddedStreams();
        var stream = videoInput.getAvStream();
        if (stream == null || stream.avg_frame_rate() == null || stream.avg_frame_rate().num() < stream.avg_frame_rate().den()) {
            return false; // if fps < 1, assume this file has already been decimated and we lost track somehow
        }

        videoOutput = new VideoKeyframeDecimator(decimatedMp4, frameCount, stream);
        videoInput.addVideoDataBufferListener(videoOutput);

        long endTime;
        long maxDuration;
        if (stream.duration() > 0 && stream.time_base() != null && !stream.time_base().isNull()) {
            maxDuration = (long) (stream.duration() * (double) stream.time_base().num() / stream.time_base().den() * 100_000) * MAX_PROCESS_TIME_MULTIPLIER;
            endTime = System.currentTimeMillis() + maxDuration;
        } else {
            maxDuration = DEFAULT_VIDEO_DURATION_MS * MAX_PROCESS_TIME_MULTIPLIER;
            endTime = System.currentTimeMillis() + maxDuration;
        }

        // Decimated file will be written/closed automatically.
        videoOutput.setFileCloseCallback(() -> decimateFinished.set(true));

        // Process video. Proceed after the decimated file is written.
        try {
            videoInput.processStream();
            videoInput.join(maxDuration);
            videoInput.stopProcessingStream();
            while (!decimateFinished.get() && checkTimeDuringDecimation(endTime) && checkInterruptedDuringDecimation()) {
                Thread.onSpinWait();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for {} video processing to finish. Writing output early.", fileName, e);
            success = false;
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warn("Exception while waiting for {} video processing to finish. Writing output early.", fileName, e);
            success = false;
        } finally {
            videoInput.closeStream();
            videoOutput.closeFile();
        }

        if (!success) {
            var partial = Paths.get(decimatedMp4).toFile();
            if (partial.exists() && !partial.delete()) {
                logger.warn("Failed to delete partial decimated file {}", decimatedMp4);
            }
        }

        // TODO May be nice to rename objects through the bucket store
        var decimatedFile = Paths.get(decimatedMp4).toFile();
        var originalFile = Paths.get(originalMp4).toFile();
        originalFile.delete();
        decimatedFile.renameTo(originalFile);
        return true;
    }

    private static boolean checkInterruptedDuringDecimation() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Interrupted during video decimation");
        }
        return true;
    }

    private static boolean checkTimeDuringDecimation(long maxTime) throws TimeoutException {
        if (System.currentTimeMillis() > maxTime) {
            throw new TimeoutException("Timeout waiting for video decimation");
        }
        return true;
    }
}