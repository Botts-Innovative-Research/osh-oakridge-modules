package com.botts.impl.service.oscar.video;

import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import com.botts.impl.service.oscar.media.MediaMaintenanceCheckpoint;
import com.botts.impl.service.oscar.media.MediaMaintenanceJob;
import com.botts.impl.service.oscar.media.MediaJobLedger;
import com.botts.impl.service.oscar.media.MediaSetStore;
import com.botts.impl.service.oscar.media.RetentionLateArrivalCheckpoint;
import com.botts.impl.service.oscar.media.RetentionScanCheckpoint;
import com.botts.impl.service.oscar.media.RetentionProcessedObservation;
import com.botts.impl.service.oscar.media.RetentionReconciliationCheckpoint;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.database.IFederatedDatabase;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.ICancellableDataStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.vast.util.TimeExtent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

        Instant completed = Instant.parse("2026-01-01T00:00:00Z");
        assertFalse(SlidingOccupancyQuery.isDue(completed,
                completed.plus(Duration.ofHours(5)), Duration.ofHours(6)));
        assertTrue(SlidingOccupancyQuery.isDue(completed,
                completed.plus(Duration.ofHours(6)), Duration.ofHours(6)));
        assertTrue("A backwards wall-clock correction must not suppress a sweep indefinitely",
                SlidingOccupancyQuery.isDue(completed, completed.minusSeconds(1),
                        Duration.ofHours(6)));
    }

    @Test
    public void checkpointTransitionsNormalizeBackwardsClockCorrections() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(4));
        Instant future = start.plus(Duration.ofDays(2));
        Instant corrected = start.plus(Duration.ofDays(1));

        RetentionLateArrivalCheckpoint completedLate =
                RetentionLateArrivalCheckpoint.idle(start, start)
                        .begin(start, end, start)
                        .complete(future);
        RetentionLateArrivalCheckpoint restartedLate = completedLate.begin(
                start, end, corrected);
        assertEquals(corrected, restartedLate.lastCompletedAt());
        RetentionLateArrivalCheckpoint activeLate = completedLate.begin(
                start, end, future.plusSeconds(1));
        assertEquals(corrected, activeLate.advance(
                start.plusSeconds(1), corrected).lastCompletedAt());

        RetentionReconciliationCheckpoint completedHistorical =
                RetentionReconciliationCheckpoint.initial(start, start)
                        .begin(start, end, start)
                        .complete(future);
        RetentionReconciliationCheckpoint restartedHistorical = completedHistorical.begin(
                start, end, corrected);
        assertEquals(corrected, restartedHistorical.lastCompletedAt());
        RetentionReconciliationCheckpoint activeHistorical = completedHistorical.begin(
                start, end, future.plusSeconds(1));
        assertEquals(corrected, activeHistorical.advance(
                start.plusSeconds(1), corrected).lastCompletedAt());
    }

    @Test
    public void productionVideoRetentionDefaultsEnableImmediateAsyncFiveJpegReduction() {
        VideoRetentionConfig config = new VideoRetentionConfig();
        assertEquals(0, config.timeToRetention);
        assertTrue(config.enableFrameRetention);
        assertEquals(5, config.frameRetentionCount);
        assertEquals(24, config.sourceRollbackGraceHours);
        assertEquals(5, config.videoQueryPeriod);
        assertEquals(24, config.lateArrivalOverlapHours);
        assertEquals(360, config.lateArrivalScanPeriodMinutes);
        assertEquals(2, config.lateArrivalCatchUpHours);
        assertEquals(24, config.reconciliationPeriodHours);
        assertEquals(15, config.occupancyStreamRefreshMinutes);
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

        RetentionLateArrivalCheckpoint lateArrival =
                RetentionLateArrivalCheckpoint.idle(first, second)
                        .begin(first, second, second)
                        .advance(first.plusSeconds(60), second.plusSeconds(1));
        store.writeLateArrivalCheckpoint(lateArrival);
        assertEquals(lateArrival, store.readLateArrivalCheckpoint().orElseThrow());

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
    public void legacyHistoricalCursorIsMigratedToAnImmediatelyDueFrozenSweep()
            throws Exception {
        Path root = temporaryFolder.newFolder("legacy-reconciliation-checkpoint").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant cursor = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = cursor.plus(Duration.ofDays(1));
        Path checkpointPath = Path.of(bucketStore.getWritableResourceURI(
                MediaSetStore.VIDEO_BUCKET, MediaSetStore.RECONCILIATION_CHECKPOINT_KEY));
        Files.writeString(checkpointPath, "{\"schemaVersion\":1,\"processedThrough\":\""
                + cursor + "\",\"updatedAt\":\"" + updated + "\"}");

        RetentionReconciliationCheckpoint migrated = store.readReconciliationCheckpoint()
                .orElseThrow();
        assertEquals(RetentionReconciliationCheckpoint.CURRENT_SCHEMA_VERSION,
                migrated.schemaVersion());
        assertEquals(cursor, migrated.processedThrough());
        assertEquals(Instant.EPOCH, migrated.lastCompletedAt());
        assertFalse(migrated.active());
        assertTrue(SlidingOccupancyQuery.isDue(migrated.lastCompletedAt(), updated,
                Duration.ofHours(24)));
    }

    @Test
    public void historicalSweepKeepsFrozenEndAcrossRestartThenStartsANewCycle() {
        BigId occupancyStreamId = BigId.fromLong(7, 43);
        AtomicReference<Instant> earliest = new AtomicReference<>();
        List<QueryWindow> windows = new ArrayList<>();
        ISensorHub hub = recordingHub(occupancyStreamId, earliest, windows,
                new AtomicInteger());
        MutableClock clock = new MutableClock(Instant.parse("2026-01-10T00:00:00Z"));
        Instant now = clock.instant();
        Instant historicalStart = now.minus(Duration.ofHours(20));
        Instant frozenEnd = now.minus(Duration.ofHours(10));
        InMemoryCursorStore cursors = new InMemoryCursorStore();
        cursors.forward.set(RetentionScanCheckpoint.at(now, now));
        Instant safeThrough = now.minus(Duration.ofHours(2));
        cursors.late.set(RetentionLateArrivalCheckpoint.idle(safeThrough, now)
                .complete(now));
        cursors.historical.set(RetentionReconciliationCheckpoint
                .initial(historicalStart, now.minus(Duration.ofDays(1)))
                .begin(historicalStart, frozenEnd, now.minus(Duration.ofDays(1)))
                .advance(historicalStart.plus(Duration.ofHours(2)),
                        now.minus(Duration.ofDays(1))));

        newScanner(hub, cursors, clock, Duration.ofHours(2)).runCycle();
        assertEquals(historicalStart.plus(Duration.ofHours(4)),
                cursors.historical.get().processedThrough());
        assertEquals(frozenEnd, cursors.historical.get().windowEnd());

        // Recreate the scanner to model a process restart. The active sweep resumes immediately,
        // even though its last completion time would otherwise be inside the 24-hour period.
        clock.set(now.plus(Duration.ofHours(1)));
        newScanner(hub, cursors, clock, Duration.ofHours(2)).runCycle();
        assertEquals(historicalStart.plus(Duration.ofHours(6)),
                cursors.historical.get().processedThrough());
        assertEquals("The moving forward boundary must not extend an active historical sweep",
                frozenEnd, cursors.historical.get().windowEnd());

        newScanner(hub, cursors, clock, Duration.ofHours(2)).runCycle();
        newScanner(hub, cursors, clock, Duration.ofHours(2)).runCycle();
        assertFalse(cursors.historical.get().active());
        assertEquals(frozenEnd, cursors.historical.get().processedThrough());
        Instant completedAt = cursors.historical.get().lastCompletedAt();

        clock.set(completedAt.plus(Duration.ofHours(23)));
        newScanner(hub, cursors, clock, Duration.ofHours(2)).runCycle();
        assertEquals("A completed full sweep must wait for its configured period", completedAt,
                cursors.historical.get().lastCompletedAt());

        clock.set(completedAt.plus(Duration.ofHours(24)));
        newScanner(hub, cursors, clock, Duration.ofHours(2)).runCycle();
        assertTrue("A new full-history cycle must be recorded after the period",
                cursors.historical.get().lastCompletedAt().isAfter(completedAt));
        assertFalse(cursors.historical.get().active());
    }

    @Test
    public void oldObservationCommittedAfterLateSliceIsQueriedByNextHistoricalSweep() {
        BigId occupancyStreamId = BigId.fromLong(7, 44);
        Instant firstNow = Instant.parse("2026-01-10T00:00:00Z");
        Instant oldResultTime = firstNow.minus(Duration.ofHours(3));
        AtomicReference<Instant> earliest = new AtomicReference<>();
        List<QueryWindow> windows = new ArrayList<>();
        AtomicInteger oldObservationQueries = new AtomicInteger();
        ISensorHub hub = recordingHub(occupancyStreamId, earliest, windows,
                oldObservationQueries);
        MutableClock clock = new MutableClock(firstNow);
        InMemoryCursorStore cursors = new InMemoryCursorStore();
        cursors.forward.set(RetentionScanCheckpoint.at(firstNow, firstNow));
        Instant finalizedLateThrough = firstNow.minus(Duration.ofHours(2));
        cursors.late.set(RetentionLateArrivalCheckpoint.idle(finalizedLateThrough, firstNow)
                .complete(firstNow));
        cursors.historical.set(RetentionReconciliationCheckpoint
                .initial(finalizedLateThrough, firstNow.minus(Duration.ofDays(1)))
                .begin(finalizedLateThrough, finalizedLateThrough,
                        firstNow.minus(Duration.ofDays(1)))
                .complete(firstNow));

        // The old result is committed only after its late-arrival event-time slice was finalized.
        earliest.set(oldResultTime);
        clock.set(firstNow.plus(Duration.ofHours(24)));
        newScanner(hub, cursors, clock, Duration.ofHours(48)).runCycle();

        assertTrue("The next frozen historical sweep must query the newly visible old result",
                oldObservationQueries.get() > 0);
        assertTrue(windows.stream().anyMatch(window -> window.limit() != 1
                && !oldResultTime.isBefore(window.start())
                && !oldResultTime.isAfter(window.end())));
        assertFalse(cursors.historical.get().active());
    }

    @Test
    public void clockRollbackPausesActiveFrozenSweepsAtCurrentSafeBoundary() {
        BigId occupancyStreamId = BigId.fromLong(7, 45);
        List<QueryWindow> windows = new ArrayList<>();
        ISensorHub hub = recordingHub(occupancyStreamId, new AtomicReference<>(), windows,
                new AtomicInteger());
        Instant now = Instant.parse("2026-01-10T00:00:00Z");
        Instant futureForward = now.plus(Duration.ofHours(12));
        Instant currentSafe = now.minus(Duration.ofHours(2));
        InMemoryCursorStore cursors = new InMemoryCursorStore();
        cursors.forward.set(RetentionScanCheckpoint.at(futureForward, futureForward));
        cursors.late.set(RetentionLateArrivalCheckpoint
                .idle(now.minus(Duration.ofHours(4)), now.minus(Duration.ofHours(1)))
                .begin(now.minus(Duration.ofHours(4)),
                        futureForward.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1))));
        cursors.historical.set(RetentionReconciliationCheckpoint
                .initial(now.minus(Duration.ofHours(6)), now.minus(Duration.ofHours(1)))
                .begin(now.minus(Duration.ofHours(6)),
                        futureForward.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1))));

        SlidingOccupancyQuery query = new SlidingOccupancyQuery(hub, Duration.ofHours(1),
                ignored -> true, Duration.ZERO, Duration.ofHours(24), cursors, () -> { },
                Clock.fixed(now, ZoneOffset.UTC), Duration.ofHours(2), Duration.ofHours(6),
                Duration.ofHours(24), Duration.ofHours(24), Duration.ofHours(24),
                Duration.ofMinutes(15));
        query.runCycle();

        assertFalse(cursors.late.get().processedThrough().isAfter(currentSafe));
        assertFalse(cursors.historical.get().processedThrough().isAfter(currentSafe));
        assertTrue("The original frozen late bound must survive rollback",
                cursors.late.get().windowEnd().isAfter(currentSafe));
        assertTrue("The original frozen historical bound must survive rollback",
                cursors.historical.get().windowEnd().isAfter(currentSafe));
        assertTrue(cursors.late.get().active());
        assertTrue(cursors.historical.get().active());
        assertTrue(windows.stream().allMatch(window -> !window.end().isAfter(currentSafe)));

        int pausedQueryCount = windows.size();
        query.runCycle();
        assertEquals("A frozen sweep at the rollback clamp must pause without replaying",
                pausedQueryCount, windows.size());
    }

    @Test
    public void zeroOverlapHistoricalSweepStillHonorsRollbackEligibilityCutoff() {
        BigId occupancyStreamId = BigId.fromLong(7, 46);
        List<QueryWindow> windows = new ArrayList<>();
        ISensorHub hub = recordingHub(occupancyStreamId, new AtomicReference<>(), windows,
                new AtomicInteger());
        Instant now = Instant.parse("2026-01-10T00:00:00Z");
        Instant futureForward = now.plus(Duration.ofHours(12));
        InMemoryCursorStore cursors = new InMemoryCursorStore();
        cursors.forward.set(RetentionScanCheckpoint.at(futureForward, futureForward));
        cursors.historical.set(RetentionReconciliationCheckpoint
                .initial(now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)))
                .begin(now.minus(Duration.ofHours(2)), futureForward,
                        now.minus(Duration.ofHours(1))));

        SlidingOccupancyQuery query = new SlidingOccupancyQuery(hub, Duration.ofHours(1),
                ignored -> true, Duration.ZERO, Duration.ofHours(24), cursors, () -> { },
                Clock.fixed(now, ZoneOffset.UTC), Duration.ZERO, Duration.ofHours(6),
                Duration.ofHours(24), Duration.ofHours(24), Duration.ofHours(24),
                Duration.ofMinutes(15));
        query.runCycle();

        assertEquals(now, cursors.historical.get().processedThrough());
        assertEquals(futureForward, cursors.historical.get().windowEnd());
        assertTrue(cursors.historical.get().active());
        assertTrue(windows.stream().allMatch(window -> !window.end().isAfter(now)));
    }

    @Test
    public void emptyRangeLessOccupancyStreamUsesKnownEarliestRange() {
        BigId rangedStream = BigId.fromLong(7, 47);
        BigId rangeLessStream = BigId.fromLong(7, 48);
        Instant knownBegin = Instant.EPOCH.plusSeconds(50 * 3600);
        IDataStreamStore dataStreams = (IDataStreamStore) Proxy.newProxyInstance(
                IDataStreamStore.class.getClassLoader(), new Class<?>[]{IDataStreamStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "selectKeys" -> Stream.of(
                            new DataStreamKey(rangedStream), new DataStreamKey(rangeLessStream));
                    case "selectEntries" -> Stream.of(
                            dataStreamEntry(rangedStream, TimeExtent.instant(knownBegin)),
                            dataStreamEntry(rangeLessStream, null));
                    default -> defaultProxyValue(method.getReturnType());
                });
        IObsStore observations = emptyObservationStore(dataStreams);
        ISensorHub hub = hubWithObservationStore(observations);
        InMemoryCursorStore cursors = new InMemoryCursorStore();
        Instant now = Instant.EPOCH.plusSeconds(100 * 3600);
        SlidingOccupancyQuery query = new SlidingOccupancyQuery(hub, Duration.ofHours(1),
                ignored -> true, Duration.ZERO, Duration.ofHours(1), cursors, () -> { },
                Clock.fixed(now, ZoneOffset.UTC), Duration.ZERO, Duration.ofHours(6),
                Duration.ofHours(2), Duration.ofHours(1), Duration.ofHours(24),
                Duration.ofMinutes(15));

        query.runCycle();

        assertEquals("An empty unknown stream must not force a Unix-epoch backfill",
                knownBegin.minusNanos(1).plus(Duration.ofHours(1)),
                cursors.forward.get().processedThrough());
    }

    @Test
    public void emptyPostgisSentinelRangeUsesFreshInitializationWindow() {
        BigId emptyStream = BigId.fromLong(7, 49);
        IDataStreamStore dataStreams = (IDataStreamStore) Proxy.newProxyInstance(
                IDataStreamStore.class.getClassLoader(), new Class<?>[]{IDataStreamStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "selectKeys" -> Stream.of(new DataStreamKey(emptyStream));
                    case "selectEntries" -> Stream.of(dataStreamEntry(emptyStream,
                            TimeExtent.period(Instant.MIN, Instant.MAX)));
                    default -> defaultProxyValue(method.getReturnType());
                });
        IObsStore observations = emptyObservationStore(dataStreams);
        InMemoryCursorStore cursors = new InMemoryCursorStore();
        Instant now = Instant.EPOCH.plusSeconds(100 * 3600);
        SlidingOccupancyQuery query = new SlidingOccupancyQuery(
                hubWithObservationStore(observations), Duration.ofHours(1), ignored -> true,
                Duration.ZERO, Duration.ofHours(1), cursors, () -> { },
                Clock.fixed(now, ZoneOffset.UTC), Duration.ZERO, Duration.ofHours(6),
                Duration.ofHours(2), Duration.ofHours(1), Duration.ofHours(24),
                Duration.ofMinutes(15));

        query.runCycle();

        assertEquals("PostGIS's empty MIN/MAX sentinel must not trigger an epoch backfill",
                now, cursors.forward.get().processedThrough());
    }

    @Test
    public void rangeLessOccupancyStreamWithEligibleRowForcesEpochFallback() {
        BigId rangedStream = BigId.fromLong(7, 50);
        BigId rangeLessStream = BigId.fromLong(7, 51);
        IDataStreamStore dataStreams = (IDataStreamStore) Proxy.newProxyInstance(
                IDataStreamStore.class.getClassLoader(), new Class<?>[]{IDataStreamStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "selectKeys" -> Stream.of(
                            new DataStreamKey(rangedStream), new DataStreamKey(rangeLessStream));
                    case "selectEntries" -> Stream.of(
                            dataStreamEntry(rangedStream, TimeExtent.instant(
                                    Instant.EPOCH.plusSeconds(50 * 3600))),
                            dataStreamEntry(rangeLessStream, null));
                    default -> defaultProxyValue(method.getReturnType());
                });
        IObsData eligibleObservation = (IObsData) Proxy.newProxyInstance(
                IObsData.class.getClassLoader(), new Class<?>[]{IObsData.class},
                (proxy, method, arguments) -> defaultProxyValue(method.getReturnType()));
        AtomicInteger unknownStreamProbes = new AtomicInteger();
        IObsStore observations = (IObsStore) Proxy.newProxyInstance(
                IObsStore.class.getClassLoader(), new Class<?>[]{IObsStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getDataStreams" -> dataStreams;
                    case "selectEntries" -> {
                        ObsFilter filter = (ObsFilter) arguments[0];
                        if (filter.getLimit() == 1
                                && Set.of(rangeLessStream).equals(
                                filter.getDataStreamFilter().getInternalIDs())) {
                            unknownStreamProbes.incrementAndGet();
                            yield Stream.of(Map.entry(rangeLessStream, eligibleObservation));
                        }
                        yield Stream.empty();
                    }
                    default -> defaultProxyValue(method.getReturnType());
                });
        InMemoryCursorStore cursors = new InMemoryCursorStore();
        Instant now = Instant.EPOCH.plusSeconds(100 * 3600);
        SlidingOccupancyQuery query = new SlidingOccupancyQuery(
                hubWithObservationStore(observations), Duration.ofHours(1), ignored -> true,
                Duration.ZERO, Duration.ofHours(1), cursors, () -> { },
                Clock.fixed(now, ZoneOffset.UTC), Duration.ZERO, Duration.ofHours(6),
                Duration.ofHours(2), Duration.ofHours(1), Duration.ofHours(24),
                Duration.ofMinutes(15));

        query.runCycle();

        assertTrue("Only the range-less stream must receive the bounded existence probe",
                unknownStreamProbes.get() > 0);
        assertEquals("A proven eligible row behind an unknown range needs gap-free backfill",
                Instant.EPOCH.plus(Duration.ofHours(1)),
                cursors.forward.get().processedThrough());
    }

    @Test
    public void metadataReclamationNeverPassesDurableDelayedCursor() throws Exception {
        Path root = temporaryFolder.newFolder("delayed-cursor-marker-prune").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant base = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        Instant alreadyDelayed = base.minus(Duration.ofHours(10)).plusSeconds(60);
        Instant delayedBacklog = base.minus(Duration.ofHours(6)).plusSeconds(60);
        RetentionProcessedObservation oldMarker = RetentionProcessedObservation.at(
                "old-delayed-marker", alreadyDelayed, base);
        RetentionProcessedObservation backlogMarker = RetentionProcessedObservation.at(
                "unscanned-delayed-marker", delayedBacklog, base);
        store.markObservationProcessed(oldMarker);
        store.markObservationProcessed(backlogMarker);
        store.writeRetentionScanCheckpoint(RetentionScanCheckpoint.at(base, base));
        VideoRetention retention = retention(bucketStore, 0, 50, Duration.ofHours(4));

        retention.runMaintenanceCycle();
        assertTrue("No marker is reclaimable before the delayed cursor is initialized",
                store.isObservationProcessed(oldMarker.observationId(), oldMarker.resultTime()));
        assertTrue(store.isObservationProcessed(
                backlogMarker.observationId(), backlogMarker.resultTime()));

        Instant delayedCursor = base.minus(Duration.ofHours(8));
        store.writeLateArrivalCheckpoint(RetentionLateArrivalCheckpoint
                .idle(delayedCursor, base).complete(base));
        retention.runMaintenanceCycle();
        assertFalse(store.isObservationProcessed(oldMarker.observationId(), oldMarker.resultTime()));
        assertTrue("A marker in the delayed cursor backlog must remain deduplicated",
                store.isObservationProcessed(
                        backlogMarker.observationId(), backlogMarker.resultTime()));

        Instant safeThrough = base.minus(Duration.ofHours(4));
        store.writeLateArrivalCheckpoint(RetentionLateArrivalCheckpoint
                .idle(safeThrough, base).complete(base));
        retention.runMaintenanceCycle();
        assertFalse("The marker becomes reclaimable only after delayed scan success",
                store.isObservationProcessed(
                        backlogMarker.observationId(), backlogMarker.resultTime()));
    }

    @Test
    public void completedLateArrivalSweepIsRateLimitedAndUsesCachedStreamIds() {
        BigId occupancyStreamId = BigId.fromLong(7, 42);
        AtomicInteger streamResolutions = new AtomicInteger();
        AtomicInteger observationQueries = new AtomicInteger();
        IDataStreamStore dataStreams = (IDataStreamStore) Proxy.newProxyInstance(
                IDataStreamStore.class.getClassLoader(), new Class<?>[]{IDataStreamStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "selectKeys" -> {
                        streamResolutions.incrementAndGet();
                        yield Stream.of(new DataStreamKey(occupancyStreamId));
                    }
                    case "selectEntries" -> Stream.empty();
                    default -> defaultProxyValue(method.getReturnType());
                });
        IObsStore observations = (IObsStore) Proxy.newProxyInstance(
                IObsStore.class.getClassLoader(), new Class<?>[]{IObsStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getDataStreams" -> dataStreams;
                    case "selectEntries" -> {
                        ObsFilter filter = (ObsFilter) arguments[0];
                        assertEquals("Every observation query must use pre-resolved stream IDs",
                                java.util.Set.of(occupancyStreamId),
                                filter.getDataStreamFilter().getInternalIDs());
                        observationQueries.incrementAndGet();
                        yield Stream.empty();
                    }
                    default -> defaultProxyValue(method.getReturnType());
                });
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
                    case "getObsSystemDatabases" -> List.of();
                    default -> defaultProxyValue(method.getReturnType());
                });
        ISensorHub hub = (ISensorHub) Proxy.newProxyInstance(
                ISensorHub.class.getClassLoader(), new Class<?>[]{ISensorHub.class},
                (proxy, method, arguments) -> method.getName().equals("getDatabaseRegistry")
                        ? registry : defaultProxyValue(method.getReturnType()));

        AtomicReference<RetentionScanCheckpoint> forward = new AtomicReference<>();
        AtomicReference<RetentionLateArrivalCheckpoint> late = new AtomicReference<>();
        AtomicReference<RetentionReconciliationCheckpoint> historical = new AtomicReference<>();
        SlidingOccupancyQuery.CursorStore cursorStore = new SlidingOccupancyQuery.CursorStore() {
            @Override public Optional<RetentionScanCheckpoint> read() {
                return Optional.ofNullable(forward.get());
            }
            @Override public void write(RetentionScanCheckpoint checkpoint) {
                forward.set(checkpoint);
            }
            @Override public Optional<RetentionReconciliationCheckpoint> readReconciliation() {
                return Optional.ofNullable(historical.get());
            }
            @Override public void writeReconciliation(
                    RetentionReconciliationCheckpoint checkpoint) {
                historical.set(checkpoint);
            }
            @Override public Optional<RetentionLateArrivalCheckpoint> readLateArrival() {
                return Optional.ofNullable(late.get());
            }
            @Override public void writeLateArrival(RetentionLateArrivalCheckpoint checkpoint) {
                late.set(checkpoint);
            }
            @Override public boolean isProcessed(String observationId, Instant resultTime) {
                return false;
            }
            @Override public void markProcessed(RetentionProcessedObservation marker) {
            }
        };
        Instant now = Instant.parse("2026-01-03T00:00:00Z");
        SlidingOccupancyQuery query = new SlidingOccupancyQuery(hub, Duration.ofHours(1),
                ignored -> true, Duration.ZERO, Duration.ofHours(1), cursorStore, () -> { },
                Clock.fixed(now, ZoneOffset.UTC), Duration.ofHours(2), Duration.ofHours(6),
                Duration.ofHours(2), Duration.ofHours(24), Duration.ofHours(24),
                Duration.ofMinutes(15));

        query.runCycle();
        int firstCycleQueries = observationQueries.get();
        assertTrue("The first cycle must perform bounded initialization/forward/sweep queries",
                firstCycleQueries > 0);
        assertNotNull(late.get());
        assertFalse("The two-hour sweep must complete within its two-hour cycle budget",
                late.get().active());

        query.runCycle();
        assertEquals("A completed overlap must not be replayed before its six-hour period",
                firstCycleQueries, observationQueries.get());

        // Simulate a crash after one half of a sweep was checkpointed.  The recent completion
        // timestamp would normally rate-limit a new sweep, but active work must resume at once.
        late.set(new RetentionLateArrivalCheckpoint(
                RetentionLateArrivalCheckpoint.CURRENT_SCHEMA_VERSION,
                now.minus(Duration.ofHours(4)), now.minus(Duration.ofHours(3)),
                now.minus(Duration.ofHours(2)),
                now, now));
        query.runCycle();
        assertTrue("An interrupted durable sweep must resume without waiting six hours",
                observationQueries.get() > firstCycleQueries);
        assertFalse(late.get().active());
        assertEquals("Occupancy output names must be resolved once per cache period", 1,
                streamResolutions.get());
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
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "selectKeys" -> Stream.of(new DataStreamKey(BigId.fromLong(1, 1)));
                    case "selectEntries" -> Stream.empty();
                    default -> defaultProxyValue(method.getReturnType());
                });
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

    private static SlidingOccupancyQuery newScanner(
            ISensorHub hub, InMemoryCursorStore cursors, Clock clock,
            Duration reconciliationCatchUp) {
        return new SlidingOccupancyQuery(hub, Duration.ofHours(1), ignored -> true,
                Duration.ZERO, Duration.ofHours(24), cursors, () -> { }, clock,
                Duration.ofHours(2), Duration.ofHours(6), Duration.ofHours(24),
                reconciliationCatchUp, Duration.ofHours(24), Duration.ofMinutes(15));
    }

    private static ISensorHub recordingHub(
            BigId occupancyStreamId, AtomicReference<Instant> earliest,
            List<QueryWindow> windows, AtomicInteger trackedTimeQueries) {
        IDataStreamStore dataStreams = (IDataStreamStore) Proxy.newProxyInstance(
                IDataStreamStore.class.getClassLoader(), new Class<?>[]{IDataStreamStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "selectKeys" -> Stream.of(new DataStreamKey(occupancyStreamId));
                    case "selectEntries" -> {
                        Instant value = earliest.get();
                        if (value == null)
                            yield Stream.empty();
                        IDataStreamInfo info = (IDataStreamInfo) Proxy.newProxyInstance(
                                IDataStreamInfo.class.getClassLoader(),
                                new Class<?>[]{IDataStreamInfo.class},
                                (infoProxy, infoMethod, infoArguments) ->
                                        infoMethod.getName().equals("getResultTimeRange")
                                                ? TimeExtent.instant(value)
                                                : defaultProxyValue(infoMethod.getReturnType()));
                        yield Stream.of(Map.entry(
                                new DataStreamKey(occupancyStreamId), info));
                    }
                    default -> defaultProxyValue(method.getReturnType());
                });
        IObsStore observations = (IObsStore) Proxy.newProxyInstance(
                IObsStore.class.getClassLoader(), new Class<?>[]{IObsStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getDataStreams" -> dataStreams;
                    case "selectEntries" -> {
                        ObsFilter filter = (ObsFilter) arguments[0];
                        if (filter.getResultTime() != null) {
                            Instant start = filter.getResultTime().getMin();
                            Instant end = filter.getResultTime().getMax();
                            windows.add(new QueryWindow(start, end, filter.getLimit()));
                            Instant tracked = earliest.get();
                            if (tracked != null && filter.getLimit() != 1
                                    && !tracked.isBefore(start) && !tracked.isAfter(end))
                                trackedTimeQueries.incrementAndGet();
                        }
                        yield Stream.empty();
                    }
                    default -> defaultProxyValue(method.getReturnType());
                });
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
                    case "getObsSystemDatabases" -> List.of();
                    default -> defaultProxyValue(method.getReturnType());
                });
        return (ISensorHub) Proxy.newProxyInstance(ISensorHub.class.getClassLoader(),
                new Class<?>[]{ISensorHub.class},
                (proxy, method, arguments) -> method.getName().equals("getDatabaseRegistry")
                        ? registry : defaultProxyValue(method.getReturnType()));
    }

    private static IDataStreamInfo dataStreamInfo(TimeExtent resultTimeRange) {
        return (IDataStreamInfo) Proxy.newProxyInstance(IDataStreamInfo.class.getClassLoader(),
                new Class<?>[]{IDataStreamInfo.class},
                (proxy, method, arguments) -> method.getName().equals("getResultTimeRange")
                        ? resultTimeRange : defaultProxyValue(method.getReturnType()));
    }

    private static Map.Entry<DataStreamKey, IDataStreamInfo> dataStreamEntry(
            BigId streamId, TimeExtent resultTimeRange) {
        return Map.entry(new DataStreamKey(streamId), dataStreamInfo(resultTimeRange));
    }

    private static IObsStore emptyObservationStore(IDataStreamStore dataStreams) {
        return (IObsStore) Proxy.newProxyInstance(IObsStore.class.getClassLoader(),
                new Class<?>[]{IObsStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getDataStreams" -> dataStreams;
                    case "selectEntries" -> Stream.empty();
                    default -> defaultProxyValue(method.getReturnType());
                });
    }

    private static ISensorHub hubWithObservationStore(IObsStore observations) {
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
                    case "getObsSystemDatabases" -> List.of();
                    default -> defaultProxyValue(method.getReturnType());
                });
        return (ISensorHub) Proxy.newProxyInstance(ISensorHub.class.getClassLoader(),
                new Class<?>[]{ISensorHub.class},
                (proxy, method, arguments) -> method.getName().equals("getDatabaseRegistry")
                        ? registry : defaultProxyValue(method.getReturnType()));
    }

    private record QueryWindow(Instant start, Instant end, long limit) {
    }

    private static final class InMemoryCursorStore implements SlidingOccupancyQuery.CursorStore {
        private final AtomicReference<RetentionScanCheckpoint> forward = new AtomicReference<>();
        private final AtomicReference<RetentionLateArrivalCheckpoint> late = new AtomicReference<>();
        private final AtomicReference<RetentionReconciliationCheckpoint> historical =
                new AtomicReference<>();

        @Override public Optional<RetentionScanCheckpoint> read() {
            return Optional.ofNullable(forward.get());
        }

        @Override public void write(RetentionScanCheckpoint checkpoint) {
            forward.set(checkpoint);
        }

        @Override public Optional<RetentionReconciliationCheckpoint> readReconciliation() {
            return Optional.ofNullable(historical.get());
        }

        @Override public void writeReconciliation(RetentionReconciliationCheckpoint checkpoint) {
            historical.set(checkpoint);
        }

        @Override public Optional<RetentionLateArrivalCheckpoint> readLateArrival() {
            return Optional.ofNullable(late.get());
        }

        @Override public void writeLateArrival(RetentionLateArrivalCheckpoint checkpoint) {
            late.set(checkpoint);
        }

        @Override public boolean isProcessed(String observationId, Instant resultTime) {
            return false;
        }

        @Override public void markProcessed(RetentionProcessedObservation marker) {
        }
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        private MutableClock(Instant now) {
            this.now = new AtomicReference<>(now);
        }

        private void set(Instant value) {
            now.set(value);
        }

        @Override public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone))
                throw new IllegalArgumentException("Test clock supports UTC only");
            return this;
        }

        @Override public Instant instant() {
            return now.get();
        }
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
        return retention(bucketStore, frameCount, metadataWorkLimit, Duration.ZERO);
    }

    private static VideoRetention retention(FileSystemBucketStore bucketStore, int frameCount,
                                            int metadataWorkLimit, Duration lateArrivalOverlap) {
        ISensorHub hub = (ISensorHub) Proxy.newProxyInstance(
                ISensorHub.class.getClassLoader(), new Class<?>[]{ISensorHub.class},
                (proxy, method, arguments) -> null);
        return new VideoRetention(hub, bucketStore, Duration.ofMinutes(1), Duration.ZERO,
                frameCount, 85, Duration.ZERO, Duration.ofHours(1), 3, lateArrivalOverlap,
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
