package com.botts.impl.service.oscar.retention;

import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StoragePressureRetentionTest {

    @Test
    public void pressureCleanupDeletesOnlyVideosAndAlwaysProtectsDailyFiles() throws Exception {
        Path root = Files.createTempDirectory("oscar-pressure-retention-");
        try {
            FileSystemBucketStore store = new FileSystemBucketStore(root);
            store.createBucket("videos");
            store.createBucket("dailyfiles");
            store.createBucket("adjudication");
            put(store, "videos", "old-video.mp4", "video");
            put(store, "dailyfiles", "daily.csv", "daily");
            put(store, "adjudication", "evidence.jpg", "evidence");

            StoragePressureRetentionConfig config = new StoragePressureRetentionConfig();
            config.triggerUsagePercent = 1;
            config.targetUsagePercent = 0;
            config.minimumObjectAgeMinutes = 0;
            config.storagePath = root.toString();
            StoragePressureRetention retention = new StoragePressureRetention(store, config,
                    LoggerFactory.getLogger(StoragePressureRetentionTest.class));
            retention.cleanupIfNeeded();

            assertFalse(store.objectExists("videos", "old-video.mp4"));
            assertTrue(store.objectExists("dailyfiles", "daily.csv"));
            assertTrue(store.objectExists("adjudication", "evidence.jpg"));
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> path.toFile().delete());
            }
        }
    }

    private static void put(FileSystemBucketStore store, String bucket, String key, String value) throws Exception {
        store.putObject(bucket, key,
                new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)), Map.of());
    }
}
