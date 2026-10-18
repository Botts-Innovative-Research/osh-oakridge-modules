package com.botts.impl.service.oscar.retention;

import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import com.botts.impl.service.oscar.media.MediaManifest;
import com.botts.impl.service.oscar.media.MediaMaintenanceJob;
import com.botts.impl.service.oscar.media.MediaSetStore;
import org.junit.Test;
import org.sensorhub.api.datastore.DataStoreException;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class StoragePressureRetentionTest {

    @Test
    public void pressureCleanupDeletesCompleteCatalogSetsAndRespectsHolds() throws Exception {
        Path root = Files.createTempDirectory("oscar-pressure-retention-");
        try {
            FileSystemBucketStore store = new FileSystemBucketStore(root);
            store.createBucket("videos");
            store.createBucket("dailyfiles");
            store.createBucket("adjudication");
            put(store, "videos", "legacy-video.mp4", "legacy");
            put(store, "dailyfiles", "daily.csv", "daily");
            put(store, "adjudication", "evidence.jpg", "evidence");
            MediaSetStore mediaStore = new MediaSetStore(store);
            MediaSetStore.Identity deletable = createMediaSet(store, mediaStore,
                    "event-delete", "camera-delete", false);
            MediaSetStore.Identity held = createMediaSet(store, mediaStore,
                    "event-held", "camera-held", true);
            MediaSetStore.Identity inGrace = createMediaSet(store, mediaStore,
                    "event-grace", "camera-grace", false);
            MediaManifest graceManifest = mediaStore.findManifest(inGrace).orElseThrow();
            MediaManifest.Source source = graceManifest.source();
            mediaStore.publishManifest(inGrace, graceManifest.withSource(new MediaManifest.Source(
                    source.objectKey(), source.sha256(), source.bytes(), source.codec(),
                    source.durationMicros(), Instant.now().plusSeconds(3_600),
                    MediaManifest.SourceState.PRESERVED_FOR_ROLLBACK)));

            StoragePressureRetentionConfig config = new StoragePressureRetentionConfig();
            config.triggerUsagePercent = 1;
            config.targetUsagePercent = 0;
            config.minimumObjectAgeMinutes = 0;
            config.storagePath = root.toString();
            StoragePressureRetention retention = new StoragePressureRetention(store, mediaStore, config,
                    LoggerFactory.getLogger(StoragePressureRetentionTest.class));
            retention.cleanupIfNeeded();

            assertTrue("Uncataloged legacy video must not be deleted",
                    store.objectExists("videos", "legacy-video.mp4"));
            assertTrue(mediaStore.findManifest(deletable).isEmpty());
            assertTrue("Held media set must remain cataloged", mediaStore.findManifest(held).isPresent());
            assertTrue("A media set in source rollback grace must remain cataloged",
                    mediaStore.findManifest(inGrace).isPresent());
            assertTrue(store.objectExists("dailyfiles", "daily.csv"));
            assertTrue(store.objectExists("adjudication", "evidence.jpg"));
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> path.toFile().delete());
            }
        }
    }

    @Test
    public void deletingManifestMakesInterruptedSetDeletionRetryable() throws Exception {
        Path root = Files.createTempDirectory("oscar-pressure-retry-");
        try {
            FileSystemBucketStore store = new FileSystemBucketStore(root);
            store.createBucket("videos");
            MediaSetStore mediaStore = new MediaSetStore(store);
            MediaSetStore.Identity identity = createMediaSet(store, mediaStore,
                    "event-retry", "camera-retry", false);
            MediaManifest manifest = mediaStore.findManifest(identity).orElseThrow();
            MediaManifest deleting = manifest.withLifecycleState(MediaManifest.LifecycleState.DELETING);
            mediaStore.publishManifest(identity, deleting);

            // Simulate a process failure after the first asset was removed but before the commit
            // marker. The next catalog pass must be able to finish without republishing a manifest
            // that references the now-missing frame.
            store.deleteObject("videos", deleting.assets().get(0).objectKey());
            assertTrue(mediaStore.deleteMediaSet(new MediaSetStore.ManifestEntry(
                    mediaStore.manifestKey(identity), deleting)) > 0);
            assertTrue(mediaStore.findManifest(identity).isEmpty());
            assertFalse(store.objectExists("videos",
                    MediaSetStore.stripVideoBucket(deleting.source().objectKey())));
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> path.toFile().delete());
            }
        }
    }

    @Test
    public void boundedDirectQueueResumesAfterRestartAndNeverListsCatalog() throws Exception {
        Path root = Files.createTempDirectory("oscar-pressure-bounded-");
        try {
            NoListingFileSystemBucketStore store = new NoListingFileSystemBucketStore(root);
            store.createBucket("videos");
            MediaSetStore firstStore = new MediaSetStore(store);
            MediaSetStore.Identity held = createMediaSet(store, firstStore,
                    "event-held-head", "camera-held-head", true);
            MediaSetStore.Identity eligible = createMediaSet(store, firstStore,
                    "event-after-held", "camera-after-held", false);

            StoragePressureRetentionConfig config = new StoragePressureRetentionConfig();
            config.triggerUsagePercent = 1;
            config.targetUsagePercent = 0;
            config.minimumObjectAgeMinutes = 0;
            config.maximumMediaSetsPerCycle = 1;
            config.storagePath = root.toString();
            new StoragePressureRetention(store, firstStore, config,
                    LoggerFactory.getLogger(StoragePressureRetentionTest.class)).cleanupIfNeeded();

            assertTrue("One bounded cycle may inspect only the held head", firstStore.findManifest(held).isPresent());
            assertTrue("The following eligible set must be left for the durable next cycle",
                    firstStore.findManifest(eligible).isPresent());

            MediaSetStore restartedStore = new MediaSetStore(store);
            new StoragePressureRetention(store, restartedStore, config,
                    LoggerFactory.getLogger(StoragePressureRetentionTest.class)).cleanupIfNeeded();

            assertTrue("Deferred held media remains protected", restartedStore.findManifest(held).isPresent());
            assertTrue("Restart resumes at the next durable direct-addressed slot",
                    restartedStore.findManifest(eligible).isEmpty());
            assertTrue("Pressure cleanup must never enumerate the retained-media catalog",
                    store.listCalls == 0);
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> path.toFile().delete());
            }
        }
    }

    @Test
    public void missingReservedQueueSlotIsSkippedOneAtATimeAfterRestart() throws Exception {
        Path root = Files.createTempDirectory("oscar-pressure-gap-");
        try {
            FileSystemBucketStore store = new FileSystemBucketStore(root);
            store.createBucket("videos");
            MediaSetStore firstStore = new MediaSetStore(store);
            MediaSetStore.Identity first = createMediaSet(store, firstStore,
                    "event-gap", "camera-gap", false);
            MediaSetStore.Identity second = createMediaSet(store, firstStore,
                    "event-after-gap", "camera-after-gap", false);
            store.deleteObject("videos",
                    "retained-media/v1/pressure-queue/items/0000000000000000000.json");

            MediaSetStore restarted = new MediaSetStore(store);
            MediaSetStore.PressureQueueEntry gap = restarted.inspectPressureHead().orElseThrow();
            assertTrue(gap.job().isEmpty());
            restarted.completePressureEntry(gap);
            MediaSetStore.PressureQueueEntry next = restarted.inspectPressureHead().orElseThrow();
            assertTrue(next.currentMembership());
            assertTrue(next.job().orElseThrow().identity().equals(second));

            // The orphan membership from the simulated pre-item crash is repaired idempotently.
            restarted.ensurePressureCandidate(first, restarted.findManifest(first).orElseThrow());
            assertTrue(restarted.pressureQueueTail() >= 3);
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> path.toFile().delete());
            }
        }
    }

    @Test
    public void failedDestructiveDeleteRemainsQueuedAndCompletesAfterRestart() throws Exception {
        Path root = Files.createTempDirectory("oscar-pressure-delete-restart-");
        try {
            FailingDeleteFileSystemBucketStore store =
                    new FailingDeleteFileSystemBucketStore(root);
            store.createBucket("videos");
            MediaSetStore firstStore = new MediaSetStore(store);
            MediaSetStore.Identity identity = createMediaSet(store, firstStore,
                    "event-delete-restart", "camera-delete-restart", false);

            StoragePressureRetentionConfig config = new StoragePressureRetentionConfig();
            config.triggerUsagePercent = 1;
            config.targetUsagePercent = 0;
            config.minimumObjectAgeMinutes = 0;
            config.maximumMediaSetsPerCycle = 1;
            config.storagePath = root.toString();

            store.failNextFrameDeletion = true;
            boolean failed = false;
            try {
                new StoragePressureRetention(store, firstStore, config,
                        LoggerFactory.getLogger(StoragePressureRetentionTest.class))
                        .cleanupIfNeeded();
            } catch (IOException expected) {
                failed = true;
            }
            assertTrue("The injected destructive delete must fail", failed);
            assertTrue("A terminal marker must prevent the partially deleted set from returning",
                    firstStore.isTerminallyDeleted(identity));
            assertTrue("The manifest is the durable retry boundary until deletion completes",
                    firstStore.findManifest(identity).isPresent());

            MediaSetStore restarted = new MediaSetStore(store);
            new StoragePressureRetention(store, restarted, config,
                    LoggerFactory.getLogger(StoragePressureRetentionTest.class)).cleanupIfNeeded();

            assertTrue("A restart must finish the queued terminal deletion",
                    restarted.findManifest(identity).isEmpty());
            assertTrue("Completed deletion must consume the pressure queue",
                    restarted.inspectPressureHead().isEmpty());
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> path.toFile().delete());
            }
        }
    }

    @Test
    public void catalogReconciliationQueueIsDirectAddressedCyclicAndRestartSafe() throws Exception {
        Path root = Files.createTempDirectory("oscar-catalog-bounded-");
        try {
            NoListingFileSystemBucketStore store = new NoListingFileSystemBucketStore(root);
            store.createBucket("videos");
            MediaSetStore firstStore = new MediaSetStore(store);
            MediaSetStore.Identity first = createMediaSet(store, firstStore,
                    "event-catalog-first", "camera-catalog-first", false);
            MediaSetStore.Identity second = createMediaSet(store, firstStore,
                    "event-catalog-second", "camera-catalog-second", false);

            long frozenTail = firstStore.catalogQueueTail();
            assertTrue(frozenTail == 2);
            MediaSetStore.CatalogQueueEntry head = firstStore.inspectCatalogHead().orElseThrow();
            assertTrue(head.currentMembership());
            assertTrue(head.job().orElseThrow().identity().equals(first));
            firstStore.deferCatalogEntry(head);

            MediaSetStore restarted = new MediaSetStore(store);
            MediaSetStore.CatalogQueueEntry resumed =
                    restarted.inspectCatalogHead().orElseThrow();
            assertTrue("Restart must resume with the next durable catalog member",
                    resumed.job().orElseThrow().identity().equals(second));
            restarted.removeMaintenance(MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, second);
            assertFalse(restarted.maintenanceExists(
                    MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, second));
            MediaManifest secondManifest = restarted.findManifest(second).orElseThrow();
            restarted.reconcileCommittedManifest(new MediaSetStore.ManifestEntry(
                    restarted.manifestKey(second), secondManifest), Instant.now());
            assertTrue("The queue repairs a manifest committed before lifecycle enqueue",
                    restarted.maintenanceExists(MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, second));
            assertTrue("The queue also repairs the manifest's durable ledger",
                    restarted.readLedger(second).isPresent());
            restarted.deferCatalogEntry(resumed);
            MediaSetStore.CatalogQueueEntry wrapped =
                    restarted.inspectCatalogHead().orElseThrow();
            assertTrue("A reconciled live member cycles to the tail for periodic repair",
                    wrapped.job().orElseThrow().identity().equals(first));
            assertTrue("Catalog reconciliation must not enumerate retained-media paths",
                    store.listCalls == 0);
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> path.toFile().delete());
            }
        }
    }

    @Test
    public void stopFailsTruthfullyUntilActivePressureCleanupTerminates() throws Exception {
        Path root = Files.createTempDirectory("oscar-pressure-stop-");
        try {
            FileSystemBucketStore store = new FileSystemBucketStore(root);
            MediaSetStore mediaStore = new MediaSetStore(store);
            StoragePressureRetentionConfig config = new StoragePressureRetentionConfig();
            config.triggerUsagePercent = 95;
            config.targetUsagePercent = 90;
            config.minimumObjectAgeMinutes = 0;
            config.maximumMediaSetsPerCycle = 1;
            config.checkPeriodMinutes = 60;
            config.storagePath = root.toString();
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            CountDownLatch exited = new CountDownLatch(1);
            AtomicInteger mutations = new AtomicInteger();
            StoragePressureRetention retention = new StoragePressureRetention(
                    store, mediaStore, config,
                    LoggerFactory.getLogger(StoragePressureRetentionTest.class)) {
                @Override
                void cleanupIfNeeded() {
                    started.countDown();
                    awaitIgnoringInterrupts(release);
                    mutations.incrementAndGet();
                    exited.countDown();
                }
            };

            retention.start();
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertThrows(IllegalStateException.class,
                    () -> retention.stop(100, TimeUnit.MILLISECONDS));
            release.countDown();
            assertTrue(exited.await(1, TimeUnit.SECONDS));
            retention.stop(1, TimeUnit.SECONDS);
            int stableMutations = mutations.get();
            Thread.sleep(50L);
            assertEquals(stableMutations, mutations.get());
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((left, right) -> right.compareTo(left))
                        .forEach(path -> path.toFile().delete());
            }
        }
    }

    private static void awaitIgnoringInterrupts(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }
        if (interrupted)
            Thread.interrupted();
    }

    private static void put(FileSystemBucketStore store, String bucket, String key, String value) throws Exception {
        store.putObject(bucket, key,
                new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)), Map.of());
    }

    private static MediaSetStore.Identity createMediaSet(FileSystemBucketStore store,
                                                         MediaSetStore mediaStore,
                                                         String eventId, String cameraUid,
                                                         boolean held) throws Exception {
        MediaSetStore.Identity identity = new MediaSetStore.Identity(eventId, "lane", cameraUid);
        String sourceKey = "clips/" + cameraUid + "/source.mp4";
        put(store, "videos", sourceKey, "source-video");
        List<MediaManifest.Asset> assets = new ArrayList<>();
        MediaManifest.FramePosition[] positions = MediaManifest.FramePosition.values();
        for (int index = 0; index < 5; index++) {
            BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_3BYTE_BGR);
            image.setRGB(index % 4, index % 4, 0xff000000 | (index + 1) * 0x00111111);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", bytes);
            MediaSetStore.PublishedAsset published = mediaStore.publishFrame(
                    identity, index + 1, bytes.toByteArray());
            assets.add(new MediaManifest.Asset(positions[index], published.objectKey(),
                    "image/jpeg", published.sha256(), published.bytes(), published.width(),
                    published.height(), index, index, index, MediaManifest.AssetState.PUBLISHED));
        }
        Path sourcePath = Path.of(store.getResourceURI("videos", sourceKey));
        Instant created = Instant.now().minusSeconds(3600);
        MediaManifest manifest = new MediaManifest(MediaManifest.CURRENT_SCHEMA_VERSION,
                identity.mediaSetId(), identity.eventId(), identity.laneUid(), identity.cameraUid(),
                created, 85,
                held ? MediaManifest.LifecycleState.HELD : MediaManifest.LifecycleState.AVAILABLE,
                held ? new MediaManifest.Hold(true, "legal", created) : MediaManifest.Hold.none(),
                new MediaManifest.Source("videos/" + sourceKey, MediaSetStore.sha256(sourcePath),
                        Files.size(sourcePath), "h264", 1_000_000,
                        created.plusSeconds(60), MediaManifest.SourceState.ELIGIBLE_FOR_DELETION),
                assets);
        mediaStore.publishManifest(identity, manifest);
        return identity;
    }

    private static final class NoListingFileSystemBucketStore extends FileSystemBucketStore {
        private int listCalls;

        private NoListingFileSystemBucketStore(Path root) throws Exception {
            super(root);
        }

        @Override
        public List<String> listObjects(String bucketName) throws DataStoreException {
            listCalls++;
            throw new DataStoreException("Unexpected full bucket listing");
        }

        @Override
        public List<String> listObjects(String bucketName, String keyPrefix)
                throws DataStoreException {
            listCalls++;
            throw new DataStoreException("Unexpected prefix listing");
        }
    }

    private static final class FailingDeleteFileSystemBucketStore extends FileSystemBucketStore {
        private boolean failNextFrameDeletion;

        private FailingDeleteFileSystemBucketStore(Path root) throws Exception {
            super(root);
        }

        @Override
        public void deleteObject(String bucketName, String key) throws DataStoreException {
            if (failNextFrameDeletion && key.endsWith(".jpg")) {
                failNextFrameDeletion = false;
                throw new DataStoreException("Injected retained-frame deletion failure");
            }
            super.deleteObject(bucketName, key);
        }
    }
}
