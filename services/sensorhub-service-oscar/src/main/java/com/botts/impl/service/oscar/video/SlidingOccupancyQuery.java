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
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SlidingOccupancyQuery {
    private static final Logger logger = LoggerFactory.getLogger(SlidingOccupancyQuery.class);
    private final String OCCUPANCY_NAME = OccupancyOutput.NAME;

    TemporalAmount batchLength;
    TemporalAmount queryRangeStart;
    TemporalAmount queryRangeEnd;
    //TemporalAmount queryDelta = Duration.ZERO;
    Instant lastQueryTime = Instant.now();
    final TemporalAmount queryError = Duration.ofSeconds(5);
    QueryAction afterQueryAction;

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    ScheduledFuture future;

    ISensorHub hub;

    public SlidingOccupancyQuery(ISensorHub hub, TemporalAmount batchLength, QueryAction afterQueryAction, TemporalAmount queryRangeStart, TemporalAmount queryRangeEnd) {
        this.batchLength = batchLength;
        this.afterQueryAction = afterQueryAction;
        this.queryRangeStart = queryRangeStart;
        this.queryRangeEnd = queryRangeEnd;
        this.hub = hub;
    }

    public void start() {
        future = executor.scheduleAtFixedRate(this::queryOccupancies, 0, batchLength.get(TimeUnit.SECONDS.toChronoUnit()), TimeUnit.SECONDS);
    }

    private void queryOccupancies() {
        try {
            logger.warn("Querying occupancies"); // TODO THIS IS DEBUG, REMOVE

            List<IObsData> decimObs;
            TemporalAmount queryDelta;
            Temporal lastQueryTime;

            // batch start and batch end are in reverse. start is closest to now, end is further.
            Instant batchStart = Instant.now().minus(this.queryRangeStart);
            Instant batchEnd = batchStart.minus(batchLength).minus(queryError);
            boolean continueQuery = true;

            do {
                lastQueryTime = Instant.now();

                decimObs = hub.getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                        .withDataStreams(new DataStreamFilter.Builder().withOutputNames(OccupancyOutput.NAME).build())
                        .withResultTimeDuring(batchEnd, batchStart)
                        .build()).sorted(Comparator.comparing(IObsData::getResultTime, Comparator.reverseOrder())).toList();

                for (IObsData obs : decimObs) {
                    if (!this.afterQueryAction.call(Occupancy.toOccupancy(obs.getResult()))) {
                        continueQuery = false;
                        break;
                    };
                }

                if (continueQuery) {
                    queryDelta = Duration.between(lastQueryTime, Instant.now());
                    batchStart = batchEnd.minus(queryDelta);
                    batchEnd = batchStart.minus(batchLength).minus(queryError);
                    continueQuery = batchStart.isBefore(Instant.now().minus(this.queryRangeEnd));
                }

            } while(continueQuery);

            logger.warn("Finished querying occupancies"); // TODO THIS IS DEBUG, REMOVE

        } catch (Exception e) {
            logger.warn("Exception while querying occupancies", e);
        }

    }


    @FunctionalInterface
    public interface QueryAction { public boolean call (Occupancy queryResult); }
}
