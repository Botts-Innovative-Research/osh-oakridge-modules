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

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
        assertFalse(store.isObservationProcessed(processed.observationId()));
        store.markObservationProcessed(processed);
        assertTrue("Processed IDs must survive cursor advancement and restart",
                new MediaSetStore(bucketStore).isObservationProcessed(processed.observationId()));

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
            public boolean isProcessed(String observationId) {
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
}
