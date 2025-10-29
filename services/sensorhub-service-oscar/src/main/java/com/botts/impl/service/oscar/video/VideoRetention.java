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
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Collections;

public class VideoRetention extends Thread {

    IObsStore obsStore;
    final TemporalAmount decimTimeOffset, deletionTimeOffset;
    final static RADHelper radHelper = new RADHelper();
    final DataRecord occupancyRecord;
    final IBucketStore bucketStore;

    VideoRetention(IObsStore obsStore, IBucketStore bucketStore, TemporalAmount decimTimeOffset, TemporalAmount deletionTimeOffset, int frameCount) {
        super();
        threadSettings();
        this.obsStore = obsStore;
        this.decimTimeOffset = decimTimeOffset;
        this.deletionTimeOffset = deletionTimeOffset;
        this.occupancyRecord = radHelper.createOccupancy();
        this.bucketStore = bucketStore;
    }

    private void threadSettings() {
        this.setDaemon(true);
        this.setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            var observations = obsStore.select(new ObsFilter.Builder()
                    .withResultTimeDuring(Instant.now().minus(deletionTimeOffset), Instant.now().minus(decimTimeOffset))
                    .withDataStreams(new DataStreamFilter.Builder().withOutputNames(OccupancyOutput.NAME).build())
                    .build()).toList();

            for (var obs : observations) {
                occupancyRecord.renewDataBlock();
                occupancyRecord.setData(obs.getResult());

                DataArray files = (DataArray) occupancyRecord.getField(radHelper.createVideoPathsArray().getName());
                for (int i = 0; i < files.getArraySizeComponent().getValue(); i++) {
                    String fileName = files.getData().getStringValue(i);

                    try {
                        originalMp4 = bucketStore.getObject("", fileName);
                    } catch (DataStoreException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        decimatedMp4 = bucketStore.putObject("", fileName + ".tmp", Collections.emptyMap());
                    } catch (DataStoreException e) {
                        throw new RuntimeException(e);
                    }



                }
            }
        }
    }
}
