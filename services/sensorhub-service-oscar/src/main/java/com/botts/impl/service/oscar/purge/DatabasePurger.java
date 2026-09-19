package com.botts.impl.service.oscar.purge;

import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.resource.ResourceKey;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.TimeExtent;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DatabasePurger {
    private static final Logger log = LoggerFactory.getLogger(DatabasePurger.class);

    private final IObsSystemDatabase database;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> hourlyPurgeTask;
    private ScheduledFuture<?> dailyPurgeTask;

    private final int occupancyBufferSeconds;

    private static final int CONNECTION_STATUS_RETENTION_HOURS = 1;

    // Data stream output names
    private static final String OUTPUT_DAILY_FILE = "dailyFile";
    private static final String OUTPUT_SPEED = "speed";
    private static final String OUTPUT_CONNECTION_STATUS = "connectionStatus";
    private static final String OUTPUT_GAMMA_COUNTS = "gammaCounts";
    private static final String OUTPUT_NEUTRON_COUNTS = "neutronCounts";
    private static final String OUTPUT_GAMMA_THRESHOLD = "gammaThreshold";
    private static final String OUTPUT_OCCUPANCY = OccupancyOutput.NAME;

    // RS350 data stream output names
    private static final String OUTPUT_BACKGROUND_REPORT = "backgroundReport";
    private static final String OUTPUT_FOREGROUND_REPORT = "foregroundReport";
    private static final String OUTPUT_STATUS = "status";

    public DatabasePurger(IObsSystemDatabase database, int occupancyBufferSeconds) {
        this.database = database;
        this.occupancyBufferSeconds = occupancyBufferSeconds;
    }

    public void start() throws SensorHubException {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OSCAR-database-purger");
            t.setDaemon(true);
            return t;
        });

        hourlyPurgeTask = scheduler.scheduleAtFixedRate(
            this::executeHourlyPurge,
            0,
            1,
            TimeUnit.HOURS
        );
        log.info("Scheduled hourly purge task (every 1 hour)");

        // Schedule daily purge at local midnight. Daily files themselves are written by the RPM drivers
        // as messages arrive (see DailyFileAppender); the observations are only kept for live consumers.
        long initialDelayMinutes = calculateDelayUntilMidnight();
        dailyPurgeTask = scheduler.scheduleAtFixedRate(
            this::executeDailyPurge,
            initialDelayMinutes,
            TimeUnit.DAYS.toMinutes(1),
            TimeUnit.MINUTES
        );
        log.info("Scheduled daily purge task (next run in {} minutes at midnight)", initialDelayMinutes);
    }

    public void stop() throws SensorHubException {
        log.info("Stopping database purger...");

        if (hourlyPurgeTask != null)
            hourlyPurgeTask.cancel(false);
        if (dailyPurgeTask != null)
            dailyPurgeTask.cancel(false);

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Scheduler did not terminate");
                }
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Database purger stopped");
    }

    private void executeHourlyPurge() {
        try {
            log.info("Starting hourly purge task...");

            purgeOldConnectionStatus();

            purgeNonOccupancyData();

            log.info("Hourly purge task completed successfully");
        } catch (Exception e) {
            // Log but don't throw - we don't want to kill the scheduler
            log.error("Error during hourly purge task", e);
        }
    }

    private void executeDailyPurge() {
        try {
            log.info("Starting daily purge task...");
            purgeDailyFileData();
            log.info("Daily purge task completed successfully");
        } catch (Exception e) {
            log.error("Error during daily purge task", e);
        }
    }

    // Utility methods

    private Set<BigId> getDataStreamIdsByOutputNames(String... outputNames) {
        var filter = new DataStreamFilter.Builder()
            .withOutputNames(outputNames)
            .build();

        return database.getDataStreamStore()
            .selectKeys(filter)
            .map(ResourceKey::getInternalID)
            .collect(Collectors.toSet());
    }

    private List<TimeExtent> getOccupancyWindows() {
        var filter = new ObsFilter.Builder()
            .withDataStreams(new DataStreamFilter.Builder()
                .withOutputNames(OUTPUT_OCCUPANCY)
                .build())
            .build();

        return database.getObservationStore()
            .select(filter)
            .map(obs -> {
                try {
                    Occupancy occ = Occupancy.toOccupancy(obs.getResult());
                    // Convert epoch seconds to Instant
                    Instant startTime = Instant.ofEpochSecond((long) occ.getStartTime());
                    Instant endTime = Instant.ofEpochSecond((long) occ.getEndTime());
                    return TimeExtent.period(startTime, endTime);
                } catch (Exception e) {
                    log.warn("Failed to parse occupancy record: {}", e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(TimeExtent::begin))
            .toList();
    }

    private List<TimeExtent> mergeAndBufferWindows(List<TimeExtent> windows, Duration buffer) {
        if (windows.isEmpty()) {
            return List.of();
        }

        // First apply buffer to each window
        List<TimeExtent> bufferedWindows = windows.stream()
            .map(w -> TimeExtent.period(w.begin().minus(buffer), w.end().plus(buffer)))
            .sorted(Comparator.comparing(TimeExtent::begin))
            .toList();

        // Merge overlapping windows
        List<TimeExtent> merged = new ArrayList<>();
        TimeExtent current = bufferedWindows.get(0);

        for (int i = 1; i < bufferedWindows.size(); i++) {
            TimeExtent next = bufferedWindows.get(i);
            if (current.intersects(next) || !current.end().isBefore(next.begin())) {
                // Windows overlap or are adjacent, merge them using span
                current = TimeExtent.span(current, next);
            } else {
                // No overlap, add current and move to next
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }

    public void purgeOldConnectionStatus() {
        Set<BigId> ids = getDataStreamIdsByOutputNames(OUTPUT_CONNECTION_STATUS);

        if (ids.isEmpty()) {
            log.debug("No Connection Status dataStreams found");
            return;
        }

        Instant cutoff = Instant.now().minus(Duration.ofHours(CONNECTION_STATUS_RETENTION_HOURS));
        long deleted = deleteObservationsBeforeTime(ids, cutoff);
        log.info("Purged {} Connection Status observations older than {}", deleted, cutoff);
    }

    /**
     * Purges dailyFile observations from before today's local midnight. The daily files in the
     * bucket store are written directly by the drivers, so these observations only need to
     * live long enough for live consumers (lane state tracking).
     */
    public void purgeDailyFileData() {
        log.info("Purging Daily File data...");

        Set<BigId> ids = getDataStreamIdsByOutputNames(OUTPUT_DAILY_FILE);
        if (ids.isEmpty()) {
            log.debug("No Daily File data streams found");
            return;
        }

        // Same day boundary as the daily files: local midnight
        ZoneId zone = ZoneId.systemDefault();
        Instant cutoff = LocalDate.now(zone).atStartOfDay(zone).toInstant();

        long deleted = deleteObservationsBeforeTime(ids, cutoff);
        log.info("Purged {} Daily File observations before {}", deleted, cutoff);
    }

    public void purgeNonOccupancyData() {
        log.info("Purging data outside occupancy windows...");

        // Get occupancy windows
        List<TimeExtent> windows = getOccupancyWindows();
        if (windows.isEmpty()) {
            log.warn("No occupancy windows found - skipping non-occupancy purge to avoid data loss");
            return;
        }

        log.info("Found {} occupancy windows", windows.size());

        // Merge overlapping windows and add buffer
        Duration buffer = Duration.ofSeconds(occupancyBufferSeconds);
        List<TimeExtent> mergedWindows = mergeAndBufferWindows(windows, buffer);
        log.info("Merged into {} non-overlapping windows with {}s buffer", mergedWindows.size(), occupancyBufferSeconds);

        // Get dataStream IDs for high-volume outputs (Aspect/Rapiscan and RS350)
        Set<BigId> dataStreamIds = getDataStreamIdsByOutputNames(
            OUTPUT_GAMMA_COUNTS,
            OUTPUT_NEUTRON_COUNTS,
            OUTPUT_GAMMA_THRESHOLD,
            OUTPUT_SPEED,
            OUTPUT_BACKGROUND_REPORT,
            OUTPUT_FOREGROUND_REPORT,
            OUTPUT_STATUS
        );

        if (dataStreamIds.isEmpty()) {
            log.debug("No high-volume dataStreams found");
            return;
        }

        log.info("Processing {} dataStreams for occupancy-gated purge", dataStreamIds.size());

        // Delete data in the gaps between occupancy windows
        long totalDeleted = purgeGapsBetweenWindows(dataStreamIds, mergedWindows);
        log.info("Purged {} observations outside occupancy windows", totalDeleted);
    }

    private long purgeGapsBetweenWindows(Set<BigId> dataStreamIds, List<TimeExtent> windows) {
        long totalDeleted = 0;

        // Delete data before the first occupancy
        if (!windows.isEmpty()) {
            TimeExtent firstWindow = windows.get(0);
            // Only delete data from a reasonable past (e.g., 30 days ago) to avoid deleting historical data
            Instant earliestPurge = Instant.now().minus(Duration.ofDays(30));
            if (firstWindow.begin().isAfter(earliestPurge)) {
                long deleted = deleteObservationsInTimeRange(dataStreamIds, earliestPurge, firstWindow.begin());
                totalDeleted += deleted;
                log.debug("Deleted {} observations before first occupancy window", deleted);
            }
        }

        // Delete data in gaps between occupancy windows
        Instant previousEnd = windows.isEmpty() ? Instant.EPOCH : windows.get(0).end();
        for (int i = 1; i < windows.size(); i++) {
            TimeExtent window = windows.get(i);
            if (previousEnd.isBefore(window.begin())) {
                // There's a gap between windows - delete data in this gap
                long deleted = deleteObservationsInTimeRange(dataStreamIds, previousEnd, window.begin());
                totalDeleted += deleted;
                log.debug("Deleted {} observations in gap between {} and {}", deleted, previousEnd, window.begin());
            }
            previousEnd = window.end();
        }

        // Delete data after the last occupancy up to current time minus a safety margin
        // Keep recent data (last hour) in case new occupancies are being recorded
        if (!windows.isEmpty()) {
            TimeExtent lastWindow = windows.get(windows.size() - 1);
            Instant safetyMargin = Instant.now().minus(Duration.ofHours(1));
            if (lastWindow.end().isBefore(safetyMargin)) {
                long deleted = deleteObservationsInTimeRange(dataStreamIds, lastWindow.end(), safetyMargin);
                totalDeleted += deleted;
                log.debug("Deleted {} observations after last occupancy window", deleted);
            }
        }

        return totalDeleted;
    }

    private long deleteObservationsBeforeTime(Set<BigId> dataStreamIds, Instant cutoff) {
        if (dataStreamIds.isEmpty()) {
            return 0;
        }

        // Use a reasonable earliest time to avoid querying from epoch
        Instant earliestTime = Instant.now().minus(Duration.ofDays(365));

        var filter = new ObsFilter.Builder()
            .withDataStreams(dataStreamIds)
            .withPhenomenonTimeDuring(earliestTime, cutoff)
            .build();

        return database.getObservationStore().removeEntries(filter);
    }

    private long deleteObservationsInTimeRange(Set<BigId> dataStreamIds, Instant start, Instant end) {
        if (dataStreamIds.isEmpty() || !start.isBefore(end)) {
            return 0;
        }

        var filter = new ObsFilter.Builder()
            .withDataStreams(dataStreamIds)
            .withPhenomenonTimeDuring(start, end)
            .build();

        return database.getObservationStore().removeEntries(filter);
    }

    private long calculateDelayUntilMidnight() {
        // Local midnight, matching the daily file day boundary
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration duration = Duration.between(now, nextMidnight);
        return duration.toMinutes();
    }
}
