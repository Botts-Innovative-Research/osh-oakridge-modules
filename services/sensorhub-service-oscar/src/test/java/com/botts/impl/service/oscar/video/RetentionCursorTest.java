package com.botts.impl.service.oscar.video;

import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import com.botts.impl.service.oscar.media.MediaMaintenanceCheckpoint;
import com.botts.impl.service.oscar.media.MediaMaintenanceJob;
import com.botts.impl.service.oscar.media.MediaJobLedger;
import com.botts.impl.service.oscar.media.MediaSetStore;
import com.botts.impl.service.oscar.media.RetentionScanCheckpoint;
import com.botts.impl.service.oscar.media.RetentionProcessedObservation;
import com.botts.impl.service.oscar.media.RetentionReconciliationCheckpoint;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.database.IFederatedDatabase;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.ICancellableDataStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.impl.utils.rad.model.Occupancy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class RetentionCursorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void catchUpPlanNeverExceedsConfiguredSpanOrEligibilityCutoff() {
        Instant cursor = Instant.parse("2026-01-01T00:00:00Z");
        SlidingOccupancyQuery.ScanPlan bounded = SlidingOccupancyQuery.plan(cursor,
                Instant.parse("2026-01-03T00:00:00Z"), Duration.ofHours(6));
        assertTrue(bounded.hasWork());
        assertEquals(cursor, bounded.startInclusive());
        assertEquals(Instant.parse("2026-01-01T06:00:00Z"), bounded.endInclusive());

        SlidingOccupancyQuery.ScanPlan caughtUp = SlidingOccupancyQuery.plan(cursor,
                Instant.parse("2026-01-01T02:00:00Z"), Duration.ofHours(6));
        assertEquals(Instant.parse("2026-01-01T02:00:00Z"), caughtUp.endInclusive());

        assertFalse(SlidingOccupancyQuery.plan(cursor, cursor, Duration.ofHours(6)).hasWork());
        assertFalse(SlidingOccupancyQuery.plan(cursor, cursor.minusSeconds(1),
                Duration.ofHours(6)).hasWork());

        assertEquals(Instant.parse("2025-12-31T00:00:00Z"),
                SlidingOccupancyQuery.overlapStart(cursor, Duration.ofHours(24)));
        assertEquals(Instant.EPOCH, SlidingOccupancyQuery.overlapStart(
                Instant.EPOCH.plusSeconds(10), Duration.ofHours(24)));
    }

    @Test
    public void retentionAndMaintenanceCheckpointsRoundTripAndReplaceAtomically() throws Exception {
        Path root = temporaryFolder.newFolder("checkpoint-store").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        Instant second = first.plus(Duration.ofHours(6));

        store.writeRetentionScanCheckpoint(RetentionScanCheckpoint.at(first, second));
        assertEquals(first, store.readRetentionScanCheckpoint().orElseThrow().processedThrough());
        store.writeRetentionScanCheckpoint(
                RetentionScanCheckpoint.at(second, second.plusSeconds(1)));
        assertEquals(second, store.readRetentionScanCheckpoint().orElseThrow().processedThrough());

        RetentionReconciliationCheckpoint reconciliation =
                RetentionReconciliationCheckpoint.at(first, second);
        store.writeReconciliationCheckpoint(reconciliation);
        assertEquals(reconciliation, store.readReconciliationCheckpoint().orElseThrow());

        RetentionProcessedObservation processed = RetentionProcessedObservation.at(
                "observation-id", first, second);
        assertFalse(store.isObservationProcessed(
                processed.observationId(), processed.resultTime()));
        store.markObservationProcessed(processed);
        assertTrue("Processed IDs must survive cursor advancement and restart",
                new MediaSetStore(bucketStore).isObservationProcessed(
                        processed.observationId(), processed.resultTime()));

        MediaMaintenanceCheckpoint maintenance = MediaMaintenanceCheckpoint.initial(second)
                .withLifecycle(new MediaMaintenanceCheckpoint.Cursor(17, "lifecycle-key"),
                        second.plusSeconds(1))
                .withRetry(new MediaMaintenanceCheckpoint.Cursor(42, "retry-key"),
                        second.plusSeconds(2));
        store.writeMaintenanceCheckpoint(maintenance);
        assertEquals(maintenance, store.readMaintenanceCheckpoint().orElseThrow());

        try (var files = Files.walk(root)) {
            assertFalse("Atomic checkpoint publication must not leave partials",
                    files.anyMatch(path -> path.getFileName().toString().contains(".partial-")));
        }
    }

    @Test
    public void hourlyProcessedIndexesArePrunedOnlyBehindLateArrivalBoundary()
            throws Exception {
        Path root = temporaryFolder.newFolder("processed-marker-prune").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant cutoff = Instant.parse("2026-01-02T00:00:00Z");
        RetentionProcessedObservation old = RetentionProcessedObservation.at(
                "old-observation", cutoff.minusSeconds(61 * 60), cutoff);
        RetentionProcessedObservation boundary = RetentionProcessedObservation.at(
                "boundary-observation", cutoff.minusSeconds(60 * 60), cutoff);
        RetentionProcessedObservation recent = RetentionProcessedObservation.at(
                "recent-observation", cutoff, cutoff.plusSeconds(1));
        store.markObservationProcessed(old);
        store.markObservationProcessed(boundary);
        store.markObservationProcessed(recent);

        MediaSetStore.ProcessedObservationPruneResult result =
                store.pruneProcessedObservations(cutoff, 1);
        assertEquals(1, result.inspected());
        assertEquals(1, result.deleted());
        assertFalse(store.isObservationProcessed(old.observationId(), old.resultTime()));
        assertTrue("The inclusive overlap boundary must remain deduplicated",
                store.isObservationProcessed(boundary.observationId(), boundary.resultTime()));
        assertTrue(store.isObservationProcessed(recent.observationId(), recent.resultTime()));

        MediaSetStore restarted = new MediaSetStore(bucketStore);
        assertTrue("Metadata reclamation state and retained markers must survive restart",
                restarted.isObservationProcessed(
                        boundary.observationId(), boundary.resultTime()));
        try (var files = Files.walk(root)) {
            long markerCount = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".idx"))
                    .filter(path -> path.toString().contains("retention-processed-index"))
                    .count();
            assertEquals(2, markerCount);
        }
    }

    @Test
    public void processedIndexCacheRefreshesAfterAnotherLiveStoreAppends()
            throws Exception {
        Path root = temporaryFolder.newFolder("processed-index-cache").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore first = new MediaSetStore(bucketStore);
        MediaSetStore second = new MediaSetStore(bucketStore);
        Instant hour = Instant.parse("2026-01-02T12:15:00Z");
        RetentionProcessedObservation one = RetentionProcessedObservation.at(
                "first-observation", hour, hour);
        RetentionProcessedObservation two = RetentionProcessedObservation.at(
                "second-observation", hour.plusSeconds(60), hour.plusSeconds(60));
        RetentionProcessedObservation three = RetentionProcessedObservation.at(
                "third-observation", hour.plusSeconds(120), hour.plusSeconds(120));
        RetentionProcessedObservation four = RetentionProcessedObservation.at(
                "fourth-observation", hour.plusSeconds(180), hour.plusSeconds(180));

        first.markObservationProcessed(one);
        // A second local append is a cache hit on the same hour and must remain mutable internally.
        first.markObservationProcessed(three);
        assertTrue(first.isObservationProcessed(three.observationId(), three.resultTime()));
        assertFalse("The first store must not cache an absent digest indefinitely",
                first.isObservationProcessed(two.observationId(), two.resultTime()));
        second.markObservationProcessed(two);
        assertTrue("A stamped cache must refresh after an external append",
                first.isObservationProcessed(two.observationId(), two.resultTime()));
        first.markObservationProcessed(four);
        assertTrue(first.isObservationProcessed(four.observationId(), four.resultTime()));

        try (var files = Files.walk(root)) {
            assertEquals("Every observation in one hour belongs to one compact index inode", 1,
                    files.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".idx"))
                            .filter(path -> path.toString().contains(
                                    "retention-processed-index"))
                            .count());
        }
    }

    @Test
    public void processedIndexReclamationHasARealDirectIoWorkBound() throws Exception {
        Path root = temporaryFolder.newFolder("processed-index-bounded").toPath();
        InstrumentedBucketStore bucketStore = new InstrumentedBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant old = Instant.parse("2026-01-01T01:05:00Z");
        Instant sameHourDifferentFile = Instant.parse("2026-01-01T01:55:00Z");
        store.markObservationProcessed(RetentionProcessedObservation.at(
                "old", old, old));
        store.markObservationProcessed(RetentionProcessedObservation.at(
                "old-same-hour", sameHourDifferentFile, sameHourDifferentFile));

        MediaSetStore.ProcessedObservationPruneResult first =
                store.pruneProcessedObservations(
                        Instant.parse("2026-01-01T05:00:00Z"), 1);
        assertEquals(1, first.inspected());
        assertEquals(1, first.deleted());
        assertFalse(store.isObservationProcessed("old", old));
        assertFalse(store.isObservationProcessed("old-same-hour", sameHourDifferentFile));
        assertEquals(0, bucketStore.listCalls.get());
        assertEquals("One reclaimed hour performs exactly twelve direct file probes",
                12, bucketStore.deleteCalls.get());

        MediaSetStore.ProcessedObservationPruneResult missingHours =
                store.pruneProcessedObservations(
                        Instant.parse("2026-01-01T05:00:00Z"), 2);
        assertEquals(2, missingHours.inspected());
        assertEquals(0, missingHours.deleted());
        assertEquals("Missing buckets are direct-addressed and consume the same bounded budget",
                36, bucketStore.deleteCalls.get());
        assertEquals(0, bucketStore.listCalls.get());
    }

    @Test
    public void absentProcessedIndexMustCrossDurabilityBarrierBeforeCheckpointAdvances()
            throws Exception {
        org.junit.Assume.assumeFalse("Open-file unlink injection is POSIX-only",
                System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
                        .contains("win"));
        Path root = temporaryFolder.newFolder("processed-index-durable-absence").toPath();
        Instant old = Instant.parse("2026-01-01T01:00:00Z");
        long epochHour = Math.floorDiv(old.getEpochSecond(), 60L * 60L);
        String prefix = "retained-media/v1/retention-processed-index/";
        String firstKey = prefix + String.format(java.util.Locale.ROOT,
                "%019d-00.idx", epochHour);
        String finalKey = prefix + String.format(java.util.Locale.ROOT,
                "%019d-11.idx", epochHour);
        DeleteIndexParentAfterObjectStore bucketStore =
                new DeleteIndexParentAfterObjectStore(root, finalKey);
        MediaSetStore store = new MediaSetStore(bucketStore);
        store.markObservationProcessed(RetentionProcessedObservation.at("old", old, old));

        // Simulate an unlink whose directory entry has not yet crossed a durability barrier. The
        // final absent direct probe then loses its containing directory before it can be fsynced.
        Files.delete(Path.of(bucketStore.getResourceURI(MediaSetStore.VIDEO_BUCKET, firstKey)));
        assertThrows(IOException.class, () -> store.pruneProcessedObservations(
                old.plus(Duration.ofHours(3)), 1));
        Path blockedParent = bucketStore.blockedParent();
        assertNotNull(blockedParent);
        assertTrue(Files.isRegularFile(blockedParent));

        FileSystemBucketStore restartedBucketStore = new FileSystemBucketStore(root);
        MediaSetStore restarted = new MediaSetStore(restartedBucketStore);
        assertThrows("A restart must not advance through an absence without a parent barrier",
                IOException.class, () -> restarted.pruneProcessedObservations(
                        old.plus(Duration.ofHours(3)), 1));

        Files.delete(blockedParent);
        MediaSetStore.ProcessedObservationPruneResult recovered =
                restarted.pruneProcessedObservations(old.plus(Duration.ofHours(3)), 1);
        assertEquals("The failed pass must leave the original hour checkpointed", 1,
                recovered.inspected());
        assertEquals(Long.valueOf(epochHour + 1L), recovered.nextEpochHour());
    }

    @Test
    public void legacyRetentionModeStillReclaimsProcessedMetadata() throws Exception {
        Path root = temporaryFolder.newFolder("legacy-metadata-reclamation").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant old = Instant.parse("2026-01-01T00:05:00Z");
        store.markObservationProcessed(RetentionProcessedObservation.at("old", old, old));
        store.writeRetentionScanCheckpoint(RetentionScanCheckpoint.at(
                Instant.parse("2026-01-01T03:00:00Z"),
                Instant.parse("2026-01-01T03:00:00Z")));

        VideoRetention retention = retention(bucketStore, 0, 10);
        retention.runMaintenanceCycle();

        assertFalse("frameCount=0 disables frame conversion, not metadata reclamation",
                store.isObservationProcessed("old", old));
    }

    @Test
    public void legacyRetentionDeletesVideosThroughTheConfiguredBucket() throws Exception {
        Path root = temporaryFolder.newFolder("legacy-video-delete").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        VideoRetention retention = retention(bucketStore, 0, 10);
        String firstKey = "clips/legacy/first.mp4";
        String secondKey = "clips/legacy/second.mp4";
        Files.write(Path.of(bucketStore.getWritableResourceURI(
                MediaSetStore.VIDEO_BUCKET, firstKey)), new byte[]{1});
        Files.write(Path.of(bucketStore.getWritableResourceURI(
                MediaSetStore.VIDEO_BUCKET, secondKey)), new byte[]{2});
        Occupancy occupancy = new Occupancy.Builder().videoPaths(List.of(
                "videos/" + firstKey, "/videos/" + secondKey)).build();

        assertTrue(retention.deleteOccupancyVideo(
                new SlidingOccupancyQuery.OccupancyRecord(
                        "event", "lane", null, occupancy)));
        assertFalse(bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, firstKey));
        assertFalse(bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, secondKey));
    }

    @Test
    public void legacyRetentionRequiresDurableUnlinkBeforeReportingSuccess() throws Exception {
        Path root = temporaryFolder.newFolder("legacy-video-durable-delete").toPath();
        String key = "clips/legacy-durable/source.mp4";
        DeleteParentAfterObjectStore bucketStore = new DeleteParentAfterObjectStore(root, key);
        VideoRetention retention = retention(bucketStore, 0, 10);
        Path source = Path.of(bucketStore.getWritableResourceURI(
                MediaSetStore.VIDEO_BUCKET, key));
        Files.write(source, new byte[]{1});
        Occupancy occupancy = new Occupancy.Builder()
                .videoPaths(List.of("videos/" + key)).build();
        SlidingOccupancyQuery.OccupancyRecord record =
                new SlidingOccupancyQuery.OccupancyRecord("event", "lane", null, occupancy);

        assertFalse("The observation must remain unprocessed when parent fsync cannot complete",
                retention.deleteOccupancyVideo(record));
        assertFalse(bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, key));
        assertTrue(Files.isRegularFile(source.getParent(),
                java.nio.file.LinkOption.NOFOLLOW_LINKS));

        FileSystemBucketStore restartedBucketStore = new FileSystemBucketStore(root);
        VideoRetention restarted = retention(restartedBucketStore, 0, 10);
        assertFalse("Restart must not accept a merely visible absence with an irregular parent",
                restarted.deleteOccupancyVideo(record));
        Files.delete(source.getParent());
        assertTrue(restarted.deleteOccupancyVideo(record));
    }

    @Test
    public void corruptProcessedBucketDoesNotStarveOtherMaintenance() throws Exception {
        Path root = temporaryFolder.newFolder("corrupt-processed-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant old = Instant.parse("2026-01-01T00:05:00Z");
        store.markObservationProcessed(RetentionProcessedObservation.at("old", old, old));
        store.writeRetentionScanCheckpoint(RetentionScanCheckpoint.at(
                old, old));
        Path index;
        try (var files = Files.walk(root)) {
            index = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".idx"))
                    .filter(path -> path.toString().contains("retention-processed-index"))
                    .findFirst().orElseThrow();
        }
        byte[] corrupt = Files.readAllBytes(index);
        corrupt[corrupt.length - 1] ^= 1;
        Files.write(index, corrupt, StandardOpenOption.TRUNCATE_EXISTING);

        VideoRetention retention = retention(bucketStore, 5, 10);
        retention.runMaintenanceCycle();

        assertTrue("Independent maintenance must continue after reclamation fails visibly",
                bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET,
                        MediaSetStore.MAINTENANCE_CHECKPOINT_KEY));
        IOException failure = assertThrows(IOException.class,
                () -> new MediaSetStore(bucketStore)
                        .isObservationProcessed("old", old));
        assertTrue(failure.getMessage().contains("checksum"));
    }

    @Test
    public void corruptOutOfRangeMetadataCheckpointFailsClosed() throws Exception {
        Path root = temporaryFolder.newFolder("negative-metadata-checkpoint").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Path checkpoint = Path.of(bucketStore.getWritableResourceURI(
                MediaSetStore.VIDEO_BUCKET,
                MediaSetStore.METADATA_RECLAMATION_CHECKPOINT_KEY));
        store.writeRetentionScanCheckpoint(RetentionScanCheckpoint.at(
                Instant.parse("2026-01-01T03:00:00Z"),
                Instant.parse("2026-01-01T03:00:00Z")));
        for (long invalid : new long[]{-1L, Long.MAX_VALUE}) {
            Files.writeString(checkpoint, ("""
                    {"schemaVersion":2,"earliestProcessedObservationEpochHour":%d,
                     "updatedAt":"2026-01-01T00:00:00Z"}
                    """).formatted(invalid));
            IOException failure = assertThrows(IOException.class,
                    () -> store.pruneProcessedObservations(Instant.now(), 1));
            assertTrue(failure.getMessage().contains("checkpoint"));

            retention(bucketStore, 5, 10).runMaintenanceCycle();
            assertTrue("Malformed reclamation state must not starve independent maintenance",
                    bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET,
                            MediaSetStore.MAINTENANCE_CHECKPOINT_KEY));
        }
    }

    @Test
    public void checkpointRejectsAnImpossibleUpdateTime() {
        Instant processed = Instant.parse("2026-01-01T00:00:00Z");
        try {
            RetentionScanCheckpoint.at(processed, processed.minusSeconds(1));
            fail("Checkpoint update time before its high-water mark must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("updatedAt"));
        }
    }

    @Test
    public void nonterminalLedgerHasOneStableRetryIndexEntryUntilPublished() throws Exception {
        Path root = temporaryFolder.newFolder("retry-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity("event", "lane", "camera");
        Instant firstAttempt = Instant.parse("2026-01-01T00:00:00Z");
        MediaJobLedger decoding = new MediaJobLedger(MediaJobLedger.CURRENT_SCHEMA_VERSION,
                identity.eventId(), identity.laneUid(), identity.cameraUid(),
                "videos/source.mp4", MediaJobLedger.JobState.DECODING, 1, firstAttempt, "");
        store.writeLedger(identity, decoding);
        assertTrue(store.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));

        MediaJobLedger failedAgain = new MediaJobLedger(MediaJobLedger.CURRENT_SCHEMA_VERSION,
                identity.eventId(), identity.laneUid(), identity.cameraUid(),
                "videos/source.mp4", MediaJobLedger.JobState.FAILED, 2,
                firstAttempt.plusSeconds(60), "temporary failure");
        store.writeLedger(identity, failedAgain);
        int shard = Integer.parseInt(identity.mediaSetId().substring(0, 2), 16);
        var jobs = store.listMaintenance(MediaMaintenanceJob.Kind.RETRY, shard, "", 10);
        assertEquals("Stable identity key must replace rather than duplicate retry work", 1,
                jobs.size());
        assertEquals(2, jobs.get(0).job().attempts());
        assertEquals(firstAttempt.plusSeconds(11 * 60), jobs.get(0).job().dueAt());

        store.writeLedger(identity, failedAgain.transition(
                MediaJobLedger.JobState.PUBLISHED, "", firstAttempt.plusSeconds(120)));
        assertFalse(store.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));
    }

    @Test
    public void scheduledScannerCanRestartAfterAnOrdinaryStop() throws Exception {
        ISensorHub hub = (ISensorHub) Proxy.newProxyInstance(
                ISensorHub.class.getClassLoader(), new Class<?>[]{ISensorHub.class},
                (proxy, method, arguments) -> null);
        RetentionScanCheckpoint atEpoch = RetentionScanCheckpoint.at(Instant.EPOCH, Instant.EPOCH);
        SlidingOccupancyQuery.CursorStore cursorStore = new SlidingOccupancyQuery.CursorStore() {
            @Override
            public Optional<RetentionScanCheckpoint> read() {
                return Optional.of(atEpoch);
            }

            @Override
            public void write(RetentionScanCheckpoint checkpoint) {
            }

            @Override
            public Optional<RetentionReconciliationCheckpoint> readReconciliation() {
                return Optional.empty();
            }

            @Override
            public void writeReconciliation(RetentionReconciliationCheckpoint checkpoint) {
            }

            @Override
            public boolean isProcessed(String observationId, Instant resultTime) {
                return false;
            }

            @Override
            public void markProcessed(RetentionProcessedObservation marker) {
            }
        };
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch firstRun = new CountDownLatch(1);
        CountDownLatch secondRun = new CountDownLatch(1);
        SlidingOccupancyQuery query = new SlidingOccupancyQuery(hub, Duration.ofDays(1),
                ignored -> true, Duration.ZERO, Duration.ofDays(1), cursorStore, () -> {
                    if (runs.incrementAndGet() == 1)
                        firstRun.countDown();
                    else
                        secondRun.countDown();
                }, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), Duration.ZERO,
                Duration.ofDays(1));

        try {
            query.start();
            assertTrue("First scheduler generation did not run",
                    firstRun.await(5, TimeUnit.SECONDS));
            query.stop();
            query.start();
            assertTrue("Restarted scheduler generation did not run",
                    secondRun.await(5, TimeUnit.SECONDS));
        } finally {
            query.stop();
        }
    }

    @Test
    public void timedOutStopIsTruthfulAndSuccessfulRetryLeavesNoWorker() throws Exception {
        ISensorHub hub = (ISensorHub) Proxy.newProxyInstance(
                ISensorHub.class.getClassLoader(), new Class<?>[]{ISensorHub.class},
                (proxy, method, arguments) -> null);
        RetentionScanCheckpoint atEpoch = RetentionScanCheckpoint.at(Instant.EPOCH, Instant.EPOCH);
        SlidingOccupancyQuery.CursorStore cursorStore = new SlidingOccupancyQuery.CursorStore() {
            @Override public Optional<RetentionScanCheckpoint> read() { return Optional.of(atEpoch); }
            @Override public void write(RetentionScanCheckpoint checkpoint) { }
            @Override public Optional<RetentionReconciliationCheckpoint> readReconciliation() {
                return Optional.empty();
            }
            @Override public void writeReconciliation(RetentionReconciliationCheckpoint checkpoint) { }
            @Override public boolean isProcessed(String observationId, Instant resultTime) {
                return false;
            }
            @Override public void markProcessed(RetentionProcessedObservation marker) { }
        };
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        AtomicInteger mutations = new AtomicInteger();
        SlidingOccupancyQuery query = new SlidingOccupancyQuery(hub, Duration.ofDays(1),
                ignored -> true, Duration.ZERO, Duration.ofDays(1), cursorStore, () -> {
                    started.countDown();
                    awaitIgnoringInterrupts(release);
                    mutations.incrementAndGet();
                    exited.countDown();
                }, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), Duration.ZERO,
                Duration.ofDays(1));

        query.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertThrows(IllegalStateException.class,
                () -> query.stop(100, TimeUnit.MILLISECONDS));
        assertThrows("An unverified old generation must prevent restart",
                IllegalStateException.class, query::start);

        release.countDown();
        assertTrue(exited.await(1, TimeUnit.SECONDS));
        query.stop(1, TimeUnit.SECONDS);
        int stableMutations = mutations.get();
        Thread.sleep(50L);
        assertEquals(stableMutations, mutations.get());
    }

    @Test
    public void stopCancelsActiveDatastoreQueryBeforeReportingTermination() throws Exception {
        CountDownLatch queryStarted = new CountDownLatch(1);
        CountDownLatch queryReleased = new CountDownLatch(1);
        CountDownLatch cancellationStarted = new CountDownLatch(1);
        CountDownLatch releaseCancellation = new CountDownLatch(1);
        CountDownLatch scopeClosed = new CountDownLatch(1);
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        ICancellableDataStore.CancellableOperation operation =
                new ICancellableDataStore.CancellableOperation() {
                    private volatile boolean cancelled;

                    @Override
                    public void cancel() {
                        if (!cancelled) {
                            cancelled = true;
                            cancellations.incrementAndGet();
                            cancellationStarted.countDown();
                            // Let the scheduler worker terminate while this driver-cancellation
                            // helper deliberately remains in flight.
                            queryReleased.countDown();
                            awaitIgnoringInterrupts(releaseCancellation);
                        }
                    }

                    @Override
                    public boolean isCancellationRequested() {
                        return cancelled;
                    }

                    @Override
                    public void close() {
                        closes.incrementAndGet();
                        scopeClosed.countDown();
                    }
                };

        IDataStreamStore dataStreams = (IDataStreamStore) Proxy.newProxyInstance(
                IDataStreamStore.class.getClassLoader(), new Class<?>[]{IDataStreamStore.class},
                (proxy, method, arguments) -> method.getName().equals("select")
                        ? Stream.empty() : defaultProxyValue(method.getReturnType()));
        IObsStore observations = (IObsStore) Proxy.newProxyInstance(
                IObsStore.class.getClassLoader(),
                new Class<?>[]{IObsStore.class, ICancellableDataStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "beginCancellableOperation" -> operation;
                    case "getDataStreams" -> dataStreams;
                    case "selectEntries" -> {
                        queryStarted.countDown();
                        awaitIgnoringInterrupts(queryReleased);
                        yield Stream.empty();
                    }
                    default -> defaultProxyValue(method.getReturnType());
                });
        IObsSystemDatabase localDatabase = (IObsSystemDatabase) Proxy.newProxyInstance(
                IObsSystemDatabase.class.getClassLoader(),
                new Class<?>[]{IObsSystemDatabase.class},
                (proxy, method, arguments) -> method.getName().equals("getObservationStore")
                        ? observations : defaultProxyValue(method.getReturnType()));
        IFederatedDatabase federated = (IFederatedDatabase) Proxy.newProxyInstance(
                IFederatedDatabase.class.getClassLoader(),
                new Class<?>[]{IFederatedDatabase.class},
                (proxy, method, arguments) -> method.getName().equals("getObservationStore")
                        ? observations : defaultProxyValue(method.getReturnType()));
        IDatabaseRegistry registry = (IDatabaseRegistry) Proxy.newProxyInstance(
                IDatabaseRegistry.class.getClassLoader(),
                new Class<?>[]{IDatabaseRegistry.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getFederatedDatabase" -> federated;
                    case "getObsSystemDatabases" -> List.of(localDatabase);
                    default -> defaultProxyValue(method.getReturnType());
                });
        ISensorHub hub = (ISensorHub) Proxy.newProxyInstance(
                ISensorHub.class.getClassLoader(), new Class<?>[]{ISensorHub.class},
                (proxy, method, arguments) -> method.getName().equals("getDatabaseRegistry")
                        ? registry : defaultProxyValue(method.getReturnType()));

        RetentionScanCheckpoint atEpoch = RetentionScanCheckpoint.at(Instant.EPOCH, Instant.EPOCH);
        SlidingOccupancyQuery.CursorStore cursorStore = new SlidingOccupancyQuery.CursorStore() {
            @Override public Optional<RetentionScanCheckpoint> read() { return Optional.of(atEpoch); }
            @Override public void write(RetentionScanCheckpoint checkpoint) { }
            @Override public Optional<RetentionReconciliationCheckpoint> readReconciliation() {
                return Optional.empty();
            }
            @Override public void writeReconciliation(RetentionReconciliationCheckpoint checkpoint) { }
            @Override public boolean isProcessed(String observationId, Instant resultTime) {
                return false;
            }
            @Override public void markProcessed(RetentionProcessedObservation marker) { }
        };
        SlidingOccupancyQuery query = new SlidingOccupancyQuery(hub, Duration.ofDays(1),
                ignored -> true, Duration.ZERO, Duration.ofDays(1), cursorStore, () -> { },
                Clock.fixed(Instant.EPOCH.plusSeconds(10), ZoneOffset.UTC), Duration.ZERO,
                Duration.ofDays(1));

        query.start();
        try {
            assertTrue("Retention query did not enter the datastore",
                    queryStarted.await(2, TimeUnit.SECONDS));
            long requestStarted = System.nanoTime();
            query.requestStop();
            long requestMillis = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - requestStarted);
            assertTrue("Video cancellation must not block the OSCAR stop request phase",
                    requestMillis < 250L);
            assertTrue(cancellationStarted.await(2, TimeUnit.SECONDS));
            assertTrue("The scheduler worker did not close its datastore scope",
                    scopeClosed.await(2, TimeUnit.SECONDS));

            assertThrows("Stop must remain truthful while the cancellation helper is active",
                    IllegalStateException.class,
                    () -> query.awaitStop(100, TimeUnit.MILLISECONDS));
            assertTrue("The fixture must isolate the old helper after scheduler termination",
                    scheduler(query).isTerminated());
            assertThrows("A terminated scheduler cannot restart over its old cancellation helper",
                    IllegalStateException.class, query::start);

            releaseCancellation.countDown();
            query.awaitStop(2, TimeUnit.SECONDS);
            assertEquals("Stop must cancel the scoped datastore query", 1, cancellations.get());
            assertEquals("The worker must close its own datastore scope", 1, closes.get());
        } finally {
            releaseCancellation.countDown();
            try {
                query.requestStop();
                query.awaitStop(2, TimeUnit.SECONDS);
            } catch (RuntimeException ignored) {
                // Preserve the primary assertion if fixture setup failed.
            }
        }
    }

    private static ScheduledExecutorService scheduler(SlidingOccupancyQuery query)
            throws ReflectiveOperationException {
        Field field = SlidingOccupancyQuery.class.getDeclaredField("executor");
        field.setAccessible(true);
        return (ScheduledExecutorService) field.get(query);
    }

    private static Object defaultProxyValue(Class<?> type) {
        if (!type.isPrimitive())
            return null;
        if (type == boolean.class)
            return false;
        if (type == char.class)
            return '\0';
        if (type == byte.class)
            return (byte) 0;
        if (type == short.class)
            return (short) 0;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == float.class)
            return 0F;
        if (type == double.class)
            return 0D;
        return null;
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

    private static VideoRetention retention(FileSystemBucketStore bucketStore, int frameCount,
                                            int metadataWorkLimit) {
        ISensorHub hub = (ISensorHub) Proxy.newProxyInstance(
                ISensorHub.class.getClassLoader(), new Class<?>[]{ISensorHub.class},
                (proxy, method, arguments) -> null);
        return new VideoRetention(hub, bucketStore, Duration.ofMinutes(1), Duration.ZERO,
                frameCount, 85, Duration.ZERO, Duration.ofHours(1), 3, Duration.ZERO,
                Duration.ofHours(1), metadataWorkLimit);
    }

    private static final class InstrumentedBucketStore extends FileSystemBucketStore {
        private final AtomicInteger listCalls = new AtomicInteger();
        private final AtomicInteger deleteCalls = new AtomicInteger();

        private InstrumentedBucketStore(Path root) throws IOException {
            super(root);
        }

        @Override
        public List<String> listObjects(String bucketName) throws DataStoreException {
            listCalls.incrementAndGet();
            throw new AssertionError("Processed-index reclamation must not list the bucket");
        }

        @Override
        public List<String> listObjects(String bucketName, String keyPrefix)
                throws DataStoreException {
            listCalls.incrementAndGet();
            throw new AssertionError("Processed-index reclamation must not list a prefix");
        }

        @Override
        public List<String> listObjects(String bucketName, String keyPrefix, String keySuffix,
                                        int limit) throws DataStoreException {
            listCalls.incrementAndGet();
            throw new AssertionError("Processed-index reclamation must not page a prefix");
        }

        @Override
        public void deleteObject(String bucketName, String key) throws DataStoreException {
            if (key.contains("retention-processed-index/") && key.endsWith(".idx"))
                deleteCalls.incrementAndGet();
            super.deleteObject(bucketName, key);
        }
    }

    /** Replaces an index directory after its final direct delete but before the caller can fsync. */
    private static final class DeleteIndexParentAfterObjectStore extends FileSystemBucketStore {
        private final String targetKey;
        private boolean armed = true;
        private Path blockedParent;

        private DeleteIndexParentAfterObjectStore(Path root, String targetKey) throws IOException {
            super(root);
            this.targetKey = targetKey;
        }

        @Override
        public void deleteObject(String bucketName, String key) throws DataStoreException {
            Path target = null;
            if (armed && MediaSetStore.VIDEO_BUCKET.equals(bucketName) && targetKey.equals(key))
                target = Path.of(getWritableResourceURI(bucketName, key));
            super.deleteObject(bucketName, key);
            if (target == null)
                return;
            try {
                Path parent = target.getParent();
                Files.delete(parent.resolve(".index.lock"));
                Files.delete(parent);
                Files.write(parent, new byte[]{1}, StandardOpenOption.CREATE_NEW);
                blockedParent = parent;
                armed = false;
            } catch (IOException e) {
                throw new DataStoreException("Unable to inject processed-index barrier failure", e);
            }
        }

        private Path blockedParent() {
            return blockedParent;
        }
    }

    /** Simulates a successful legacy unlink followed by a failed parent-directory barrier. */
    private static final class DeleteParentAfterObjectStore extends FileSystemBucketStore {
        private final String targetKey;
        private boolean armed = true;

        private DeleteParentAfterObjectStore(Path root, String targetKey) throws IOException {
            super(root);
            this.targetKey = targetKey;
        }

        @Override
        public void deleteObject(String bucketName, String key) throws DataStoreException {
            Path parent = null;
            if (armed && MediaSetStore.VIDEO_BUCKET.equals(bucketName) && targetKey.equals(key))
                parent = Path.of(getWritableResourceURI(bucketName, key)).getParent();
            super.deleteObject(bucketName, key);
            if (parent == null)
                return;
            try {
                Files.delete(parent);
                Files.write(parent, new byte[]{1}, StandardOpenOption.CREATE_NEW);
                armed = false;
            } catch (IOException e) {
                throw new DataStoreException("Unable to inject legacy delete barrier failure", e);
            }
        }
    }
}
