package com.botts.impl.service.oscar.retention;

import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.datastore.DataStoreException;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StoragePressureRetention {
    private static final String VIDEO_BUCKET = "videos";

    private final IBucketStore bucketStore;
    private final StoragePressureRetentionConfig config;
    private final Logger log;
    private final Path storagePath;
    private ScheduledExecutorService scheduler;

    public StoragePressureRetention(IBucketStore bucketStore, StoragePressureRetentionConfig config, Logger log) {
        if (config.triggerUsagePercent <= config.targetUsagePercent || config.triggerUsagePercent > 100
                || config.targetUsagePercent < 0)
            throw new IllegalArgumentException("Storage trigger must be greater than target and no more than 100 percent");
        if (config.checkPeriodMinutes <= 0 || config.minimumObjectAgeMinutes < 0)
            throw new IllegalArgumentException("Storage retention intervals must be valid");
        if (config.storagePath == null || config.storagePath.isBlank())
            throw new IllegalArgumentException("Storage path is required");

        this.bucketStore = bucketStore;
        this.config = config;
        this.log = log;
        this.storagePath = Path.of(config.storagePath).toAbsolutePath().normalize();
    }

    public synchronized void start() {
        if (scheduler != null)
            return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "OSCAR-storage-pressure-retention");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::runSafely, 0, config.checkPeriodMinutes, TimeUnit.MINUTES);
    }

    public synchronized void stop() {
        if (scheduler == null)
            return;
        scheduler.shutdownNow();
        scheduler = null;
    }

    void cleanupIfNeeded() throws IOException, DataStoreException {
        Files.createDirectories(storagePath);
        FileStore fileStore = Files.getFileStore(storagePath);
        StorageUsage usage = readUsage(fileStore);
        if (usage.percentUsed() < config.triggerUsagePercent)
            return;

        long bytesToFree = Math.max(0, usage.usedBytes() - usage.targetBytes(config.targetUsagePercent));
        log.warn("Storage usage is {}%; deleting eligible data until usage reaches {}% ({} bytes required)",
                usage.percentUsed(), config.targetUsagePercent, bytesToFree);

        long cutoffMillis = Instant.now().minus(Duration.ofMinutes(config.minimumObjectAgeMinutes)).toEpochMilli();
        List<Candidate> candidates = loadCandidates(cutoffMillis);
        long freedBytes = 0;
        int deletedObjects = 0;
        for (Candidate candidate : candidates) {
            if (freedBytes >= bytesToFree)
                break;
            bucketStore.deleteObject(candidate.bucket(), candidate.key());
            freedBytes += candidate.size();
            deletedObjects++;
            log.info("Pressure cleanup deleted {}/{} ({} bytes)", candidate.bucket(), candidate.key(), candidate.size());
        }

        StorageUsage finalUsage = readUsage(fileStore);
        if (finalUsage.percentUsed() > config.targetUsagePercent) {
            log.error("Storage remains at {}% after deleting {} objects ({} bytes). Daily-file CSVs remain protected.",
                    finalUsage.percentUsed(), deletedObjects, freedBytes);
        } else {
            log.info("Storage pressure cleanup completed at {}% after deleting {} objects ({} bytes)",
                    finalUsage.percentUsed(), deletedObjects, freedBytes);
        }
    }

    private void runSafely() {
        try {
            cleanupIfNeeded();
        } catch (Exception e) {
            log.error("Storage pressure cleanup failed", e);
        }
    }

    private List<Candidate> loadCandidates(long cutoffMillis) throws DataStoreException {
        List<Candidate> candidates = new ArrayList<>();
        if (!bucketStore.bucketExists(VIDEO_BUCKET))
            return candidates;

        for (String key : bucketStore.listObjects(VIDEO_BUCKET)) {
            try {
                Path path = Path.of(bucketStore.getResourceURI(VIDEO_BUCKET, key));
                long modifiedAt = Files.getLastModifiedTime(path).toMillis();
                if (modifiedAt <= cutoffMillis)
                    candidates.add(new Candidate(VIDEO_BUCKET, key, bucketStore.getObjectSize(VIDEO_BUCKET, key),
                            modifiedAt, mediaPriority(key)));
            } catch (IOException e) {
                log.warn("Cannot inspect retention candidate {}/{}", VIDEO_BUCKET, key, e);
            }
        }

        candidates.sort(Comparator.comparingInt(Candidate::priority)
                .thenComparingLong(Candidate::modifiedAt)
                .thenComparing(Comparator.comparingLong(Candidate::size).reversed()));
        return candidates;
    }

    private static int mediaPriority(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".mp4") || lower.endsWith(".mjpeg") || lower.endsWith(".mjpg")
                || lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".ts"))
            return 0;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp"))
            return 1;
        return 2;
    }

    private static StorageUsage readUsage(FileStore fileStore) throws IOException {
        long total = fileStore.getTotalSpace();
        long used = Math.max(0, total - fileStore.getUsableSpace());
        int percent = total == 0 ? 0 : (int) Math.ceil((used * 100.0) / total);
        return new StorageUsage(total, used, percent);
    }

    private record Candidate(String bucket, String key, long size, long modifiedAt, int priority) {}

    private record StorageUsage(long totalBytes, long usedBytes, int percentUsed) {
        long targetBytes(int targetPercent) {
            return (long) Math.floor(totalBytes * (targetPercent / 100.0));
        }
    }
}
