package com.botts.impl.service.oscar.video;

import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.TemporalAmount;

public class VideoRetention {

    private static final Logger logger = LoggerFactory.getLogger(VideoRetention.class);
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
                return false;
            } else {
                try {
                    if (!decimate(bucketStore.getResourceURI("", videoFile))) {
                        logger.info("Video file was already decimated {}", videoFile);
                        return false;
                    }
                } catch (DataStoreException e) {
                    logger.warn("Failed to decimate video file {}", videoFile, e);
                    return false;
                }
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
        videoInput.setInjectExtradata(false);
        videoInput.openStream();
        videoInput.queryEmbeddedStreams();
        var stream = videoInput.getAvStream();
        if (stream.avg_frame_rate() == null || stream.avg_frame_rate().num() < stream.avg_frame_rate().den()) {
            return false; // if fps < 1, assume this file has already been decimated and we lost track somehow
        }

        VideoKeyframeDecimator videoOutput = new VideoKeyframeDecimator(decimatedMp4, frameCount, videoInput.getAvStream());
        videoInput.addVideoDataBufferListener(videoOutput);

        videoOutput.setFileCloseCallback(() -> {
            videoInput.stopProcessingStream();
            videoInput.closeStream();
        });

        videoInput.processStream();

        try {
            videoInput.join();
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for video processing to finish. Writing output early, stopping thread.", e);
            videoOutput.closeFile();
            Thread.currentThread().interrupt();
        }

        // TODO May be nice to rename objects through the bucket store
        var decimatedFile = Paths.get(decimatedMp4).toFile();
        var originalFile = Paths.get(originalMp4).toFile();
        originalFile.delete();
        decimatedFile.renameTo(originalFile);
        return true;
    }
}