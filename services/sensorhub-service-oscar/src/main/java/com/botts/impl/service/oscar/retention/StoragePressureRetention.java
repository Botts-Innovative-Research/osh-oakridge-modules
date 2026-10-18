package com.botts.impl.service.oscar.retention;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.oscar.media.MediaManifest;
import com.botts.impl.service.oscar.media.MediaSetStore;
import org.sensorhub.api.datastore.DataStoreException;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StoragePressureRetention {
    private static final long DEFAULT_STOP_TIMEOUT_MILLIS = 5_000L;
    private final StoragePressureRetentionConfig config;
    private final Logger log;
    private final Path storagePath;
    private final MediaSetStore mediaStore;
    private final Object lifecycleMonitor = new Object();
    private final Object cleanupMonitor = new Object();
    private ScheduledExecutorService scheduler;

    public StoragePressureRetention(IBucketStore bucketStore, StoragePressureRetentionConfig config, Logger log) {
        this(bucketStore, createMediaStore(bucketStore), config, log);
    }

    public StoragePressureRetention(IBucketStore bucketStore, MediaSetStore mediaStore,
                                    StoragePressureRetentionConfig config, Logger log) {
        if (config.triggerUsagePercent <= config.targetUsagePercent || config.triggerUsagePercent > 100
                || config.targetUsagePercent < 0)
            throw new IllegalArgumentException("Storage trigger must be greater than target and no more than 100 percent");
        if (config.checkPeriodMinutes <= 0 || config.minimumObjectAgeMinutes < 0
                || config.maximumMediaSetsPerCycle <= 0)
            throw new IllegalArgumentException("Storage retention intervals must be valid");
        if (config.storagePath == null || config.storagePath.isBlank())
            throw new IllegalArgumentException("Storage path is required");

        this.config = config;
        this.log = log;
        this.mediaStore = mediaStore;
        this.storagePath = Path.of(config.storagePath).toAbsolutePath().normalize();
    }

    public void start() {
        synchronized (lifecycleMonitor) {
            if (scheduler != null && !scheduler.isTerminated())
                return;
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "OSCAR-storage-pressure-retention");
                thread.setDaemon(true);
                return thread;
            });
            scheduler.scheduleWithFixedDelay(this::runSafely, 0,
                    config.checkPeriodMinutes, TimeUnit.MINUTES);
        }
    }

    public void stop() {
        stop(DEFAULT_STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void stop(long timeout, TimeUnit unit) {
        java.util.Objects.requireNonNull(unit, "unit");
        if (timeout <= 0)
            throw new IllegalArgumentException("Storage-pressure stop timeout must be positive");
        synchronized (lifecycleMonitor) {
            ScheduledExecutorService stopping = scheduler;
            if (stopping == null)
                return;
            stopping.shutdownNow();
            boolean terminated;
            try {
                terminated = stopping.awaitTermination(timeout, unit);
            } catch (InterruptedException error) {
                stopping.shutdownNow();
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Interrupted while waiting for storage-pressure retention to stop", error);
            }
            if (!terminated)
                throw new IllegalStateException(
                        "Storage-pressure retention worker did not terminate within the stop deadline");
            scheduler = null;
        }
    }

    void cleanupIfNeeded() throws IOException, DataStoreException {
        synchronized (cleanupMonitor) {
            cleanupIfNeededWithinLock();
        }
    }

    private void cleanupIfNeededWithinLock() throws IOException, DataStoreException {
        Files.createDirectories(storagePath);
        FileStore fileStore = Files.getFileStore(storagePath);
        StorageUsage usage = readUsage(fileStore);
        if (usage.percentUsed() < config.triggerUsagePercent)
            return;

        long bytesToFree = Math.max(0, usage.usedBytes() - usage.targetBytes(config.targetUsagePercent));
        log.warn("Storage usage is {}%; deleting eligible data until usage reaches {}% ({} bytes required)",
                usage.percentUsed(), config.targetUsagePercent, bytesToFree);

        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofMinutes(config.minimumObjectAgeMinutes));
        // Freeze the tail so a held or not-yet-old entry deferred to the tail is inspected no more
        // than once in this cycle. Both work and memory therefore remain bounded regardless of the
        // number of retained sets on disk.
        long cycleTail = mediaStore.pressureQueueTail();
        long freedBytes = 0;
        int deletedObjects = 0;
        int inspected = 0;
        while (!Thread.currentThread().isInterrupted()
                && inspected < config.maximumMediaSetsPerCycle && freedBytes < bytesToFree) {
            Optional<MediaSetStore.PressureQueueEntry> next = mediaStore.inspectPressureHead();
            if (next.isEmpty() || next.get().sequence() >= cycleTail)
                break;
            MediaSetStore.PressureQueueEntry queueEntry = next.get();
            inspected++;
            if (queueEntry.job().isEmpty() || !queueEntry.currentMembership()) {
                mediaStore.completePressureEntry(queueEntry);
                continue;
            }

            MediaSetStore.Identity identity = queueEntry.job().get().identity();
            Optional<MediaManifest> found = mediaStore.findManifest(identity);
            if (found.isEmpty()) {
                mediaStore.completePressureEntry(queueEntry);
                continue;
            }
            MediaManifest manifest = found.get();
            boolean deletionAlreadyStarted = manifest.lifecycleState()
                    == MediaManifest.LifecycleState.DELETING;
            boolean protectedByHold = manifest.isHeld()
                    || manifest.lifecycleState() == MediaManifest.LifecycleState.HELD;
            boolean protectedByRollback = manifest.source().state()
                    == MediaManifest.SourceState.PRESERVED_FOR_ROLLBACK
                    && now.isBefore(manifest.source().rollbackDeleteAfter());
            boolean tooNew = manifest.createdAt().isAfter(cutoff);
            if (!deletionAlreadyStarted && (protectedByHold || protectedByRollback || tooNew)) {
                mediaStore.deferPressureEntry(queueEntry);
                continue;
            }

            long deleted = mediaStore.deleteMediaSet(new MediaSetStore.ManifestEntry(
                    mediaStore.manifestKey(identity), manifest));
            freedBytes += deleted;
            if (deleted > 0) {
                deletedObjects++;
                log.info("Pressure cleanup deleted media set {} ({} bytes)",
                        manifest.mediaSetId(), deleted);
            }
            // deleteMediaSet clears membership only after the manifest is gone. Advancing the
            // direct queue is idempotent if a restart observes the now-missing queue item.
            if (deleted > 0 || mediaStore.findManifest(identity).isEmpty())
                mediaStore.completePressureEntry(queueEntry);
            else
                // A hold or rollback-grace update may race the pre-delete read. deleteMediaSet
                // rechecks the live manifest and returns zero; retain the candidate at the tail.
                mediaStore.deferPressureEntry(queueEntry);
        }

        StorageUsage finalUsage = readUsage(fileStore);
        if (finalUsage.percentUsed() > config.targetUsagePercent) {
            log.error("Storage remains at {}% after inspecting {} and deleting {} media sets "
                            + "({} bytes). Daily-file CSVs remain protected; bounded cleanup will "
                            + "resume next cycle.", finalUsage.percentUsed(), inspected,
                    deletedObjects, freedBytes);
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

    private static StorageUsage readUsage(FileStore fileStore) throws IOException {
        long total = fileStore.getTotalSpace();
        long used = Math.max(0, total - fileStore.getUsableSpace());
        int percent = total == 0 ? 0 : (int) Math.ceil((used * 100.0) / total);
        return new StorageUsage(total, used, percent);
    }

    private static MediaSetStore createMediaStore(IBucketStore bucketStore) {
        try {
            return new MediaSetStore(bucketStore);
        } catch (DataStoreException e) {
            throw new IllegalStateException("Unable to initialize retained-media storage", e);
        }
    }

    private record StorageUsage(long totalBytes, long usedBytes, int percentUsed) {
        long targetBytes(int targetPercent) {
            return (long) Math.floor(totalBytes * (targetPercent / 100.0));
        }
    }
}
