package com.botts.impl.service.oscar.video;

import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class SlidingOccupancyQuery {
    private static final Logger logger = LoggerFactory.getLogger(SlidingOccupancyQuery.class);
    private final String OCCUPANCY_NAME = OccupancyOutput.NAME;

    TemporalAmount batchWindowLength;
    TemporalAmount queryRangeStart;
    TemporalAmount queryRangeEnd;
    static final TemporalAmount adjustableWindowIncreaseOnMiss = Duration.ofMinutes(30); // TODO Test different values
    QueryAction afterQueryAction;

    static final ThreadFactory minPriorityThreadFac = r -> {
        Thread newThread = new Thread(r);
        newThread.setPriority(Thread.MIN_PRIORITY);
        return newThread;
    };
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, minPriorityThreadFac);
    ScheduledFuture<?> future;

    ISensorHub hub;

    public SlidingOccupancyQuery(ISensorHub hub, TemporalAmount batchWindowLength, QueryAction afterQueryAction, TemporalAmount queryRangeStart, TemporalAmount queryRangeEnd) {
        this.batchWindowLength = batchWindowLength;
        this.afterQueryAction = afterQueryAction;
        this.queryRangeStart = queryRangeStart;
        this.queryRangeEnd = queryRangeEnd;
        this.hub = hub;
    }

    public SlidingOccupancyQuery(ISensorHub hub, TemporalAmount batchWindowLength, QueryAction afterQueryAction, TemporalAmount queryRangeStart) {
        this(hub, batchWindowLength, afterQueryAction, queryRangeStart, null);
    }

    public void start() {
        // Should generally result in one or two queries per batch
        future = executor.scheduleAtFixedRate(this::queryOccupancies, 0, batchWindowLength.get(TimeUnit.SECONDS.toChronoUnit()), TimeUnit.SECONDS);
    }

    private void queryOccupancies() {
        try {

            List<IObsData> decimObs;

            // batch start and batch end are in reverse. start is closest to now (larger), end is further (smaller).
            Instant batchStart = Instant.now().minus(this.queryRangeStart);
            Instant batchEnd = batchStart.minus(batchWindowLength);
            Instant queryEnd = queryRangeEnd == null ? Instant.EPOCH : Instant.now().minus(queryRangeEnd);
            TemporalAmount adjustableBatchLength = batchWindowLength;
            boolean continueQuery = true;

            do {
                decimObs = hub.getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                        .withDataStreams(new DataStreamFilter.Builder().withOutputNames(OccupancyOutput.NAME).build())
                        .withResultTimeDuring(batchEnd, batchStart)
                        .build()).sorted(Comparator.comparing(IObsData::getResultTime, Comparator.reverseOrder())).toList();

                for (IObsData obs : decimObs) {
                    if (!this.afterQueryAction.call(Occupancy.toOccupancy(obs.getResult()))) {
                        continueQuery = false;
                        break;
                    }
                }

                if (continueQuery) {
                    if (decimObs.isEmpty()) {
                        // If our list of observations is empty, increase the window size
                        adjustableBatchLength = Duration.between(Instant.now(),
                                Instant.now().plus(adjustableBatchLength).plus(adjustableWindowIncreaseOnMiss));
                    } else {
                        // Reset the window size if we're getting observations
                        adjustableBatchLength = batchWindowLength;
                    }
                    batchStart = batchEnd;
                    batchEnd = batchStart.minus(adjustableBatchLength);
                    continueQuery = !batchStart.isBefore(queryEnd);
                }

            } while(continueQuery);

        } catch (Exception e) {
            logger.warn("Exception while querying occupancies", e);
        }

    }

    public void stop() {
        if (future != null) {
            future.cancel(false);
            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for occupancy query thread to terminate", e);
                Thread.currentThread().interrupt();
            }
        }
    }


    @FunctionalInterface
    public interface QueryAction { public boolean call (Occupancy queryResult); }
}
