package com.botts.impl.service.oscar.video;

import com.botts.api.service.bucket.IBucketStore;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Collections;

public class VideoRetention extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(VideoRetention.class);
    IObsStore obsStore;
    final TemporalAmount decimTimeOffset, deletionTimeOffset;
    final IBucketStore bucketStore;
    final int frameCount;

    public VideoRetention(IObsStore obsStore, IBucketStore bucketStore, TemporalAmount decimTimeOffset, TemporalAmount deletionTimeOffset, int frameCount) {
        super();
        threadSettings();
        this.obsStore = obsStore;
        this.decimTimeOffset = decimTimeOffset;
        this.deletionTimeOffset = deletionTimeOffset;
        this.bucketStore = bucketStore;
        this.frameCount = frameCount;
    }

    private void threadSettings() {
        this.setDaemon(true);
        this.setPriority(Thread.MIN_PRIORITY);
    }

    // TODO Consider adding delay between each query/decimation iteration
    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            var observations = obsStore.select(new ObsFilter.Builder()
                    .withResultTimeDuring(Instant.now().minus(deletionTimeOffset), Instant.now().minus(decimTimeOffset))
                    .withDataStreams(new DataStreamFilter.Builder().withOutputNames(OccupancyOutput.NAME).build())
                    .build()).toList();

            for (var obs : observations) {
                var occupancy = Occupancy.toOccupancy(obs.getResult());
                for (String videoFile : occupancy.getVideoPaths()) {
                    decimate(videoFile);
                }
            }
        }
    }

    public void decimate(String fileName) {

        String originalMp4 = fileName;
        String decimatedMp4 = fileName + ".tmp";

        MpegTsProcessor videoInput = new MpegTsProcessor(originalMp4);
        videoInput.openStream();
        videoInput.queryEmbeddedStreams();

        VideoKeyframeDecimator videoOutput = new VideoKeyframeDecimator(decimatedMp4, frameCount, videoInput.getAvStream());
        videoInput.addVideoDataBufferListener(videoOutput);

        videoOutput.setFileCloseCallback(() -> {
            videoInput.stopProcessingStream();
            try {
                videoInput.join();
            } catch (Exception ignored) {}
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
    }
}
