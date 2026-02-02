package com.botts.impl.service.oscar.purge;

import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.resource.ResourceKey;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.TimeExtent;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DatabasePurger {
    private static final Logger log = LoggerFactory.getLogger(DatabasePurger.class);

    private final IObsSystemDatabase database;
    private final IBucketStore bucketStore;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> hourlyPurgeTask;
    private ScheduledFuture<?> dailyExportTask;

    private final int occupancyBufferSeconds;

    private static final String DAILY_FILES_BUCKET = "dailyfiles";

    private static final int CONNECTION_STATUS_RETENTION_HOURS = 1;

    // Data stream output names
    private static final String OUTPUT_DAILY_FILE = "dailyFile";
    private static final String OUTPUT_SPEED = "speed";
    private static final String OUTPUT_CONNECTION_STATUS = "connectionStatus";
    private static final String OUTPUT_GAMMA_COUNTS = "gammaCounts";
    private static final String OUTPUT_NEUTRON_COUNTS = "neutronCounts";
    private static final String OUTPUT_GAMMA_THRESHOLD = "gammaThreshold";
    private static final String OUTPUT_OCCUPANCY = OccupancyOutput.NAME;

    public DatabasePurger(IObsSystemDatabase database, IBucketStore bucketStore, int occupancyBufferSeconds) {
        this.database = database;
        this.bucketStore = bucketStore;
        this.occupancyBufferSeconds = occupancyBufferSeconds;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OSCAR-database-purger");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() throws SensorHubException {
        try {
            if (!bucketStore.bucketExists(DAILY_FILES_BUCKET)) {
                bucketStore.createBucket(DAILY_FILES_BUCKET);
                log.info("Created bucket for daily file exports: {}", DAILY_FILES_BUCKET);
            }

            hourlyPurgeTask = scheduler.scheduleAtFixedRate(
                this::executeHourlyPurge,
                0,
                1,
                TimeUnit.HOURS
            );
            log.info("Scheduled hourly purge task (every 1 hour)");

            // Schedule daily midnight export and purge
            long initialDelayMinutes = calculateDelayUntilMidnight();
            dailyExportTask = scheduler.scheduleAtFixedRate(
                this::executeDailyExportAndPurge,
                initialDelayMinutes,
                TimeUnit.DAYS.toMinutes(1),
                TimeUnit.MINUTES
            );
            log.info("Scheduled daily export task (next run in {} minutes at midnight)", initialDelayMinutes);

        } catch (DataStoreException e) {
            throw new SensorHubException("Failed to create daily files bucket", e);
        }
    }

    public void stop() throws SensorHubException {
        log.info("Stopping database purger...");

        if (hourlyPurgeTask != null)
            hourlyPurgeTask.cancel(false);
        if (dailyExportTask != null)
            dailyExportTask.cancel(false);

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

    private void executeDailyExportAndPurge() {
        try {
            log.info("Starting daily export and purge task...");
            exportAndPurgeDailyFileData();
            log.info("Daily export and purge task completed successfully");
        } catch (Exception e) {
            log.error("Error during daily export and purge task", e);
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

    private Map<BigId, IDataStreamInfo> getDataStreamEntriesByOutputNames(String... outputNames) {
        var filter = new DataStreamFilter.Builder()
            .withOutputNames(outputNames)
            .build();

        Map<BigId, IDataStreamInfo> result = new HashMap<>();
        database.getDataStreamStore()
            .selectEntries(filter)
            .forEach(entry -> {
                DataStreamKey key = entry.getKey();
                IDataStreamInfo value = entry.getValue();
                result.put(key.getInternalID(), value);
            });
        return result;
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

    public void exportAndPurgeDailyFileData() {
        log.info("Exporting and purging Daily File data...");

        // Export daily files
        exportDailyFileOutputToCSV();
        // Purge daily files
        Set<BigId> ids = getDataStreamIdsByOutputNames(OUTPUT_DAILY_FILE);
        if (ids.isEmpty()) {
            log.debug("No Daily File data streams found");
            return;
        }

        // Purge data that was exported (yesterday and before) - using UTC
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant cutoff = today.atStartOfDay(ZoneOffset.UTC).toInstant();

        long deleted = deleteObservationsBeforeTime(ids, cutoff);
        log.info("Purged {} Daily File observations", deleted);
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

        // Get dataStream IDs for high-volume outputs (including Speed)
        Set<BigId> dataStreamIds = getDataStreamIdsByOutputNames(
            OUTPUT_GAMMA_COUNTS,
            OUTPUT_NEUTRON_COUNTS,
            OUTPUT_GAMMA_THRESHOLD,
            OUTPUT_SPEED
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

    private void exportDailyFileOutputToCSV() {
        // Use UTC for consistent daily file exports
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        String dateStr = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE);

        Instant startOfYesterday = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfYesterday = yesterday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Get daily file dataStream info for name lookup
        Map<BigId, IDataStreamInfo> dataStreamInfo = getDataStreamEntriesByOutputNames(OUTPUT_DAILY_FILE);

        if (dataStreamInfo.isEmpty()) {
            log.info("No Daily File dataStreams found, nothing to export");
            return;
        }

        log.info("Exporting DailyFileOutput records for {} lanes...", dataStreamInfo.size());

        // Export each dataStream to its own CSV file
        for (var entry : dataStreamInfo.entrySet()) {
            BigId dataStreamId = entry.getKey();
            IDataStreamInfo dsInfo = entry.getValue();

            // Use system ID to create a unique filename per lane
            String systemId = dsInfo.getSystemID().getUniqueID();
            // Extract lane identifier from system ID (e.g., "urn:osh:sensor:rapiscan:lane1" -> "lane1")
            String laneId = systemId.contains(":") ? systemId.substring(systemId.lastIndexOf(':') + 1) : systemId;
            String objectKey = String.format("%s_%s.csv", laneId, dateStr);

            exportDataStreamToCSV(dataStreamId, objectKey, startOfYesterday, endOfYesterday);
        }
    }

    private void exportDataStreamToCSV(BigId dataStreamId, String objectKey,
                                       Instant startTime, Instant endTime) {
        var filter = new ObsFilter.Builder()
            .withDataStreams(Set.of(dataStreamId))
            .withPhenomenonTimeDuring(startTime, endTime)
            .build();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", "text/csv");

        try (OutputStream outputStream = bucketStore.putObject(DAILY_FILES_BUCKET, objectKey, metadata);
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

            // Write CSV header
            writer.write("message,timestamp\n");

            long count = database.getObservationStore()
                .select(filter)
                .peek(obs -> {
                    try {
                        String timestamp = obs.getPhenomenonTime().toString();

                        // Get the message content from the observation
                        String message = "";
                        if (obs.getResult() != null && obs.getResult().getAtomCount() > 1) {
                            message = obs.getResult().getStringValue(1);
                            // Escape quotes and wrap in quotes for CSV
                            message = "\"" + message.replace("\"", "\"\"") + "\"";
                        }

                        writer.write(String.format("%s,%s\n", message, timestamp));
                    } catch (IOException e) {
                        log.warn("Failed to write observation to CSV: {}", e.getMessage());
                    }
                })
                .count();

            log.info("Exported {} DailyFileOutput records to bucket {}/{}", count, DAILY_FILES_BUCKET, objectKey);
        } catch (IOException | DataStoreException e) {
            log.error("Failed to export DailyFileOutput to bucket {}/{}: {}", DAILY_FILES_BUCKET, objectKey, e.getMessage(), e);
        }
    }

    private long calculateDelayUntilMidnight() {
        // Schedule based on UTC midnight for consistent behavior
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration duration = Duration.between(now, nextMidnight);
        return duration.toMinutes();
    }
}
