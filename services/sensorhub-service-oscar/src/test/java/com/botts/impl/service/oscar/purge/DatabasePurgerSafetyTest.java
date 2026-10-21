package com.botts.impl.service.oscar.purge;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.ICancellableDataStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabase;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileObservationMode;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileWriterHandle;
import org.sensorhub.impl.utils.rad.dailyfile.IDailyFileJournalService;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLHelper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DatabasePurgerSafetyTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    private MVObsSystemDatabase database;
    private Path databasePath;
    private Path bucketRoot;
    private IBucketStore bucketStore;

    @Before
    public void setUp() throws Exception {
        databasePath = Files.createTempFile("purger-safety-", ".dat");
        Files.delete(databasePath);
        database = new MVObsSystemDatabase();
        MVObsSystemDatabaseConfig config = new MVObsSystemDatabaseConfig();
        config.storagePath = databasePath.toString();
        config.databaseNum = 14;
        database.setConfiguration(config);
        database.init();
        database.start();

        bucketRoot = Files.createTempDirectory("purger-safety-bucket-");
        bucketStore = new FileSystemBucketStore(bucketRoot);
        bucketStore.createBucket("dailyfiles");
    }

    @After
    public void tearDown() throws Exception {
        if (database != null)
            database.stop();
        if (databasePath != null)
            Files.deleteIfExists(databasePath);
        if (bucketRoot != null && Files.exists(bucketRoot)) {
            try (var paths = Files.walk(bucketRoot)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test
    public void midnightDelayNeverTruncatesToAnEarlyRun() {
        DatabasePurger purger = purger(null, ZoneOffset.UTC);
        Instant at235914 = Instant.parse("2026-10-17T23:59:14.900Z");
        assertEquals(45_100L, purger.calculateDelayUntilMidnightMillis(at235914));
        Instant fractionalMillisecond = Instant.parse("2026-10-17T23:59:59.000000001Z");
        assertEquals(1_000L, purger.calculateDelayUntilMidnightMillis(fractionalMillisecond));
    }

    @Test
    public void previousDayWindowIsCapturedOnceAndDstAware() {
        ZoneId zone = ZoneId.of("America/New_York");
        Instant captured = Instant.parse("2026-03-09T12:00:00Z");
        DatabasePurger.DayWindow window = DatabasePurger.previousCompletedDayWindow(captured, zone);
        assertEquals(LocalDate.of(2026, 3, 8), window.day());
        assertEquals(Instant.parse("2026-03-08T05:00:00Z"), window.startInclusive());
        assertEquals(Instant.parse("2026-03-09T04:00:00Z"), window.endExclusive());
    }

    @Test
    public void purgesExactHalfOpenWindowOnlyAfterJournalManifestExists() throws Exception {
        BigId system = addSystem(null, "urn:test:lane:daily");
        BigId daily = addDataStream(system, "urn:test:lane:daily", "dailyFile", dailyFileStructure());
        DatabasePurger.DayWindow window = DatabasePurger.previousCompletedDayWindow(NOW, ZoneOffset.UTC);
        addStringObservation(daily, window.startInclusive().plusSeconds(1), "inside");
        addStringObservation(daily, window.endExclusive(), "next-day-boundary");

        ToggleJournal journal = new ToggleJournal(ZoneOffset.UTC);
        DatabasePurger purger = purger(journal, ZoneOffset.UTC);
        purger.exportAndPurgeDailyFileData(window);
        assertEquals(2, observations(daily).size());

        journal.published.set(true);
        purger.exportAndPurgeDailyFileData(window);
        List<IObsData> remaining = observations(daily);
        assertEquals(1, remaining.size());
        assertEquals(window.endExclusive(), remaining.get(0).getPhenomenonTime());
        assertEquals("Immutable [start,end) deletion must leave the next day's midnight record",
                "next-day-boundary", remaining.get(0).getResult().getStringValue(1));
    }

    @Test
    public void deletesLargeDailyWindowInBoundedBatches() throws Exception {
        BigId system = addSystem(null, "urn:test:lane:batched");
        BigId daily = addDataStream(system, "urn:test:lane:batched", "dailyFile",
                dailyFileStructure());
        DatabasePurger.DayWindow window = DatabasePurger.previousCompletedDayWindow(NOW, ZoneOffset.UTC);
        for (int i = 0; i <= DatabasePurger.DELETE_BATCH_SIZE; i++)
            addStringObservation(daily, window.startInclusive().plusNanos(i + 1L), "record-" + i);
        addStringObservation(daily, window.endExclusive(), "next-day-boundary");

        ToggleJournal journal = new ToggleJournal(ZoneOffset.UTC);
        journal.published.set(true);
        purger(journal, ZoneOffset.UTC).exportAndPurgeDailyFileData(window);

        List<IObsData> remaining = observations(daily);
        assertEquals(1, remaining.size());
        assertEquals(window.endExclusive(), remaining.get(0).getPhenomenonTime());
    }

    @Test
    public void deletionRevalidatesEligibilityAfterConcurrentTimestampMutation() throws Exception {
        BigId system = addSystem(null, "urn:test:lane:delete-race");
        BigId daily = addDataStream(system, "urn:test:lane:delete-race", "dailyFile",
                dailyFileStructure());
        DatabasePurger.DayWindow window = DatabasePurger.previousCompletedDayWindow(
                NOW, ZoneOffset.UTC);
        addStringObservation(daily, window.startInclusive().plusSeconds(1), "moved-before-delete");

        Map.Entry<BigId, IObsData> selected;
        try (var entries = database.getObservationStore().selectEntries(new ObsFilter.Builder()
                .withDataStreams(daily).build())) {
            selected = entries.findFirst().orElseThrow();
        }
        // Move exactly onto the excluded upper bound. This catches stores that
        // accidentally collapse Range.closedOpen into an inclusive SQL range
        // during the final predicate-guarded delete.
        Instant movedTime = window.endExclusive();
        IObsData moved = ObsData.Builder.from(selected.getValue())
                .withPhenomenonTime(movedTime)
                .withResultTime(movedTime)
                .build();
        AtomicBoolean mutationRan = new AtomicBoolean();
        IObsSystemDatabase racingDatabase = databaseThatMutatesBeforeFirstDelete(
                selected.getKey(), moved, mutationRan);

        ToggleJournal journal = new ToggleJournal(ZoneOffset.UTC);
        journal.published.set(true);
        new DatabasePurger(racingDatabase, bucketStore, 0, journal, ZoneOffset.UTC,
                Clock.fixed(NOW, ZoneOffset.UTC)).exportAndPurgeDailyFileData(window);

        assertTrue("The fixture must mutate after selection and immediately before deletion",
                mutationRan.get());
        List<IObsData> remaining = observations(daily);
        assertEquals("A row moved outside the immutable window must survive guarded deletion",
                1, remaining.size());
        assertEquals(movedTime, remaining.get(0).getPhenomenonTime());
    }

    @Test
    public void deletionRevalidatesEligibilityAfterConcurrentDataStreamMutation() throws Exception {
        BigId system = addSystem(null, "urn:test:lane:delete-stream-race");
        BigId daily = addDataStream(system, "urn:test:lane:delete-stream-race", "dailyFile",
                dailyFileStructure());
        BigId replacementSystem = addSystem(null,
                "urn:test:lane:delete-stream-race-replacement");
        BigId replacementStream = addDataStream(replacementSystem,
                "urn:test:lane:delete-stream-race-replacement", "dailyFile",
                dailyFileStructure());
        DatabasePurger.DayWindow window = DatabasePurger.previousCompletedDayWindow(
                NOW, ZoneOffset.UTC);
        addStringObservation(daily, window.startInclusive().plusSeconds(1),
                "moved-before-delete");

        Map.Entry<BigId, IObsData> selected;
        try (var entries = database.getObservationStore().selectEntries(new ObsFilter.Builder()
                .withDataStreams(daily).build())) {
            selected = entries.findFirst().orElseThrow();
        }
        IObsData moved = ObsData.Builder.from(selected.getValue())
                .withDataStream(replacementStream)
                .build();
        AtomicBoolean mutationRan = new AtomicBoolean();
        IObsSystemDatabase racingDatabase = databaseThatMutatesBeforeFirstDelete(
                selected.getKey(), moved, mutationRan);

        ToggleJournal journal = new ToggleJournal(ZoneOffset.UTC);
        journal.published.set(true);
        new DatabasePurger(racingDatabase, bucketStore, 0, journal, ZoneOffset.UTC,
                Clock.fixed(NOW, ZoneOffset.UTC)).exportAndPurgeDailyFileData(window);

        assertTrue("The fixture must move the observation after selection and before deletion",
                mutationRan.get());
        IObsData remaining = database.getObservationStore().get(selected.getKey());
        assertTrue("A row moved to another stream must survive guarded deletion",
                remaining != null);
        assertEquals(replacementStream, remaining.getDataStreamID());
        assertEquals(window.startInclusive().plusSeconds(1), remaining.getPhenomenonTime());
    }

    @Test
    public void hourlyDailyFileCatchUpIsDurableBoundedAndOldestFirstAcrossRestart() throws Exception {
        BigId system = addSystem(null, "urn:test:lane:catch-up");
        BigId daily = addDataStream(system, "urn:test:lane:catch-up", "dailyFile",
                dailyFileStructure());
        LocalDate first = LocalDate.of(2026, 10, 13);
        addStringObservation(daily, DatabasePurger.dayWindow(first, ZoneOffset.UTC)
                .startInclusive().plusSeconds(1), "day-1");
        addStringObservation(daily, DatabasePurger.dayWindow(first.plusDays(1), ZoneOffset.UTC)
                .startInclusive().plusSeconds(1), "day-2");
        addStringObservation(daily, DatabasePurger.dayWindow(first.plusDays(2), ZoneOffset.UTC)
                .startInclusive().plusSeconds(1), "day-3");

        ToggleJournal journal = new ToggleJournal(ZoneOffset.UTC);
        journal.published.set(true);
        journal.earliestJournalDay = first;
        String feedId = "urn:test:lane:catch-up#dailyFile";
        DatabasePurger firstProcess = new DatabasePurger(database, bucketStore, 0, journal,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true, 2);
        firstProcess.executeHourlyPurge();

        assertEquals(List.of(first, first.plusDays(1)), journal.requestedDays);
        assertEquals(Optional.of(first.plusDays(1)), journal.checkpoint(feedId));
        assertEquals("One old day must remain for the next bounded cycle", 1,
                observations(daily).size());

        // A new purger instance proves that progress lives in the journal rather than memory.
        journal.requestedDays.clear();
        DatabasePurger restarted = new DatabasePurger(database, bucketStore, 0, journal,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true, 2);
        restarted.executeHourlyPurge();

        assertEquals(List.of(first.plusDays(2)), journal.requestedDays);
        assertEquals(Optional.of(LocalDate.of(2026, 10, 16)), journal.checkpoint(feedId));
        assertTrue(observations(daily).isEmpty());
    }

    @Test
    public void hourlyCatchUpRetriesAfterPublicationWithoutDeletingEarly() throws Exception {
        BigId system = addSystem(null, "urn:test:lane:blocked");
        BigId daily = addDataStream(system, "urn:test:lane:blocked", "dailyFile",
                dailyFileStructure());
        LocalDate day = LocalDate.of(2026, 10, 15);
        addStringObservation(daily, DatabasePurger.dayWindow(day, ZoneOffset.UTC)
                .startInclusive().plusSeconds(1), "not-yet-published");

        ToggleJournal journal = new ToggleJournal(ZoneOffset.UTC, DailyFileObservationMode.LATEST);
        journal.earliestJournalDay = day;
        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, journal,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true, 10);
        purger.executeHourlyPurge();

        assertEquals(1, observations(daily).size());
        assertEquals(Optional.empty(), journal.checkpoint("urn:test:lane:blocked#dailyFile"));
        assertEquals(List.of(day), journal.requestedDays);

        journal.published.set(true);
        journal.requestedDays.clear();
        purger.executeHourlyPurge();

        assertTrue("The next hourly cycle must remove the completed day after publication",
                observations(daily).isEmpty());
        assertEquals(Optional.of(LocalDate.of(2026, 10, 16)),
                journal.checkpoint("urn:test:lane:blocked#dailyFile"));
        assertEquals(List.of(day), journal.requestedDays);
    }

    @Test
    public void hourlyCatchUpDefersWithoutNoiseUntilJournalIsReady() throws Exception {
        BigId system = addSystem(null, "urn:test:lane:journal-startup");
        BigId daily = addDataStream(system, "urn:test:lane:journal-startup", "dailyFile",
                dailyFileStructure());
        LocalDate day = LocalDate.of(2026, 10, 16);
        addStringObservation(daily, DatabasePurger.dayWindow(day, ZoneOffset.UTC)
                .startInclusive().plusSeconds(1), "wait-for-journal");

        ToggleJournal journal = new ToggleJournal(ZoneOffset.UTC);
        journal.ready.set(false);
        journal.earliestJournalDay = day;
        journal.published.set(true);
        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, journal,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true, 10);

        purger.executeHourlyPurge();

        assertEquals("An undiscoverable manifest must never permit deletion", 1,
                observations(daily).size());
        assertTrue("A not-yet-started journal must not be queried per feed",
                journal.requestedDays.isEmpty());
        assertEquals(Optional.empty(),
                journal.checkpoint("urn:test:lane:journal-startup#dailyFile"));

        journal.ready.set(true);
        purger.executeHourlyPurge();

        assertTrue("The next hourly cycle must resume after journal startup",
                observations(daily).isEmpty());
        assertEquals(Optional.of(day),
                journal.checkpoint("urn:test:lane:journal-startup#dailyFile"));
    }

    @Test
    public void hourlyCatchUpStillRunsWhenAnotherMaintenancePhaseFails() throws Exception {
        BigId system = addSystem(null, "urn:test:lane:phase-failure");
        BigId daily = addDataStream(system, "urn:test:lane:phase-failure", "dailyFile",
                dailyFileStructure());
        LocalDate day = LocalDate.of(2026, 10, 16);
        addStringObservation(daily, DatabasePurger.dayWindow(day, ZoneOffset.UTC)
                .startInclusive().plusSeconds(1), "published");

        ToggleJournal journal = new ToggleJournal(ZoneOffset.UTC);
        journal.earliestJournalDay = day;
        journal.published.set(true);
        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, journal,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true, 10) {
            @Override
            void purgeOldConnectionStatusWithinScope() {
                throw new IllegalStateException("fixture connection-status failure");
            }
        };

        purger.executeHourlyPurge();

        assertTrue("An unrelated hourly failure must not starve Daily File catch-up",
                observations(daily).isEmpty());
        assertEquals(Optional.of(day),
                journal.checkpoint("urn:test:lane:phase-failure#dailyFile"));
    }

    @Test
    public void occupancyGatingIsScopedToEachLane() throws Exception {
        BigId lane1 = addSystem(null, "urn:test:lane:1");
        BigId lane2 = addSystem(null, "urn:test:lane:2");
        BigId lane1Sensor = addSystem(lane1, "urn:test:sensor:1");
        BigId lane2Sensor = addSystem(lane2, "urn:test:sensor:2");
        BigId lane1Process = addSystem(lane1, "urn:test:process:1");
        BigId lane2Process = addSystem(lane2, "urn:test:process:2");

        BigId lane1Gamma = addDataStream(lane1Sensor, "urn:test:sensor:1", "gammaCounts", scalarStructure("gammaCounts"));
        BigId lane2Gamma = addDataStream(lane2Sensor, "urn:test:sensor:2", "gammaCounts", scalarStructure("gammaCounts"));
        BigId lane1Occupancy = addDataStream(lane1Process, "urn:test:process:1", "occupancy", occupancyStructure());
        BigId lane2Occupancy = addDataStream(lane2Process, "urn:test:process:2", "occupancy", occupancyStructure());

        Instant lane1Start = NOW.minusSeconds(3 * 3600);
        Instant lane1End = NOW.minusSeconds(2 * 3600);
        Instant lane2Start = NOW.minusSeconds(90 * 60);
        Instant lane2End = NOW.minusSeconds(60 * 60);
        addOccupancy(lane1Occupancy, lane1Start, lane1End);
        addOccupancy(lane2Occupancy, lane2Start, lane2End);

        Instant duringLane1Only = lane1Start.plusSeconds(10 * 60);
        addScalarObservation(lane1Gamma, duringLane1Only, 101);
        addScalarObservation(lane2Gamma, duringLane1Only, 202);

        purger(null, ZoneOffset.UTC).purgeNonOccupancyData();

        assertEquals("Lane 1 activity must retain lane 1 data", 1, observations(lane1Gamma).size());
        assertTrue("Lane 1 activity must not retain lane 2 data", observations(lane2Gamma).isEmpty());
    }

    @Test
    public void delegatedLifecycleSkipsOnlyLegacyHighVolumePurge() throws Exception {
        BigId lane = addSystem(null, "urn:test:lane:delegated");
        BigId sensor = addSystem(lane, "urn:test:sensor:delegated");
        BigId process = addSystem(lane, "urn:test:process:delegated");
        BigId gamma = addDataStream(sensor, "urn:test:sensor:delegated", "gammaCounts",
                scalarStructure("gammaCounts"));
        BigId occupancy = addDataStream(process, "urn:test:process:delegated", "occupancy",
                occupancyStructure());

        Instant occupiedStart = NOW.minusSeconds(3 * 3600);
        addOccupancy(occupancy, occupiedStart, occupiedStart.plusSeconds(300));
        addScalarObservation(gamma, NOW.minusSeconds(2 * 3600), 42);

        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, null,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true);
        purger.executeHourlyPurge();

        assertEquals("The tiered database owns deletion of high-volume observations", 1,
                observations(gamma).size());
    }

    @Test
    public void latestModeNeverDeletesUnsealedCurrentDayObservations() throws Exception {
        BigId system = addSystem(null, "urn:test:lane:latest");
        BigId daily = addDataStream(system, "urn:test:lane:latest", "dailyFile",
                dailyFileStructure());
        addStringObservation(daily, NOW.minusSeconds(30), "oldest");
        addStringObservation(daily, NOW.minusSeconds(20), "middle");
        addStringObservation(daily, NOW.minusSeconds(10), "newest");

        ToggleJournal journal = new ToggleJournal(ZoneOffset.UTC, DailyFileObservationMode.LATEST);
        purger(journal, ZoneOffset.UTC).executeHourlyPurge();

        List<IObsData> remaining = observations(daily);
        assertEquals("LATEST cleanup must wait for a completed, published day", 3,
                remaining.size());
    }

    @Test
    public void activeImmediatePurgeIsCancelledBeforeStopSucceedsAndCanRestart() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch cancellationReleasedWorker = new CountDownLatch(1);
        CountDownLatch firstExited = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        AtomicInteger runCount = new AtomicInteger();
        AtomicInteger cancellationCount = new AtomicInteger();
        AtomicInteger postCancelMutations = new AtomicInteger();

        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, null,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true) {
            @Override
            ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
                return new TestCancellationOperation(cancellationCount,
                        cancellationReleasedWorker);
            }

            @Override
            void purgeOldConnectionStatusWithinScope() {
                int run = runCount.incrementAndGet();
                if (run > 1) {
                    secondStarted.countDown();
                    return;
                }

                firstStarted.countDown();
                awaitIgnoringInterrupts(cancellationReleasedWorker);
                if (cancellationCount.get() == 0)
                    postCancelMutations.incrementAndGet();
                firstExited.countDown();
            }
        };

        purger.start();
        assertTrue("The initial-delay-zero purge must be active", firstStarted.await(2, TimeUnit.SECONDS));
        long stopStarted = System.nanoTime();
        purger.stop();
        long stopMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stopStarted);

        assertTrue("Stop must stay comfortably inside the core 10 second budget",
                stopMillis < DatabasePurger.STOP_TIMEOUT_MILLIS + 1_000L);
        assertTrue(firstExited.await(1, TimeUnit.SECONDS));
        assertEquals(1, cancellationCount.get());
        assertEquals("No mutation may run after cancellation and successful stop",
                0, postCancelMutations.get());

        purger.stop(); // idempotent
        assertEquals(1, cancellationCount.get());

        purger.start();
        assertTrue("A fully stopped purger must be restartable",
                secondStarted.await(2, TimeUnit.SECONDS));
        purger.stop();
        assertTrue(runCount.get() >= 2);
    }

    @Test
    public void interruptResistantWorkerCausesTruthfulBoundedStopFailure() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch allowExit = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        AtomicInteger mutationCount = new AtomicInteger();

        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, null,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true) {
            @Override
            ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
                return new ICancellableDataStore.CancellableOperation() {
                    @Override public void cancel() { }
                    @Override public boolean isCancellationRequested() { return false; }
                    @Override public void close() { }
                };
            }

            @Override
            void purgeOldConnectionStatusWithinScope() {
                started.countDown();
                awaitIgnoringInterrupts(allowExit);
                mutationCount.incrementAndGet();
                exited.countDown();
            }
        };

        purger.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        long stopStarted = System.nanoTime();
        assertThrows(org.sensorhub.api.common.SensorHubException.class,
                () -> purger.stop(100, TimeUnit.MILLISECONDS));
        assertTrue("A non-cancellable store must fail before the registry deadline",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stopStarted) < 1_000L);
        assertFalse("The blocked worker is deliberately still active", exited.await(50, TimeUnit.MILLISECONDS));

        allowExit.countDown();
        assertTrue(exited.await(1, TimeUnit.SECONDS));
        purger.stop(1, TimeUnit.SECONDS);
        int mutationsAtSuccessfulStop = mutationCount.get();
        Thread.sleep(50L);
        assertEquals("A successful stop must leave no mutating worker behind",
                mutationsAtSuccessfulStop, mutationCount.get());
    }

    @Test
    public void datastoreCancellationFailureCanNeverBeReportedAsSuccessfulStop() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        AtomicInteger cancellationAttempts = new AtomicInteger();

        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, null,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true) {
            @Override
            ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
                return new ICancellableDataStore.CancellableOperation() {
                    @Override
                    public void cancel() {
                        cancellationAttempts.incrementAndGet();
                        throw new IllegalStateException("fixture datastore cancellation failure");
                    }

                    @Override public boolean isCancellationRequested() { return false; }
                    @Override public void close() { }
                };
            }

            @Override
            void purgeOldConnectionStatusWithinScope() {
                started.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException expected) {
                    Thread.currentThread().interrupt();
                } finally {
                    exited.countDown();
                }
            }
        };

        purger.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertThrows(org.sensorhub.api.common.SensorHubException.class, purger::stop);
        assertTrue("Task interruption may terminate the worker, but cannot erase a failed query cancellation",
                exited.await(1, TimeUnit.SECONDS));
        assertTrue(cancellationAttempts.get() >= 1);
        assertThrows("A repeated stop must not convert the prior failure into success",
                org.sensorhub.api.common.SensorHubException.class, purger::stop);
        assertThrows("The unsafe purger instance must be recreated rather than restarted",
                org.sensorhub.api.common.SensorHubException.class, purger::start);
    }

    @Test
    public void everyDirectMaintenanceEntryOwnsExactlyOneCancellableScope() {
        AtomicInteger opened = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, null,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true) {
            @Override
            ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
                opened.incrementAndGet();
                return new ICancellableDataStore.CancellableOperation() {
                    @Override public void cancel() { }
                    @Override public boolean isCancellationRequested() { return false; }
                    @Override public void close() { closed.incrementAndGet(); }
                };
            }
        };

        purger.purgeOldConnectionStatus();
        purger.exportAndPurgeDailyFileData();
        purger.purgeNonOccupancyData();
        purger.exportAndPurgeDailyFileData(
                DatabasePurger.previousCompletedDayWindow(NOW, ZoneOffset.UTC));

        assertEquals("Nested daily-file helpers must reuse the owning scope", 4, opened.get());
        assertEquals(4, closed.get());
    }

    @Test
    public void stopCancelsAndWaitsForDirectAdministrativeMaintenance() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch cancellationReleasedWorker = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        AtomicInteger cancellationCount = new AtomicInteger();
        AtomicInteger mutations = new AtomicInteger();
        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, null,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true) {
            @Override
            ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
                return new TestCancellationOperation(cancellationCount,
                        cancellationReleasedWorker);
            }

            @Override
            void purgeOldConnectionStatusWithinScope() {
                started.countDown();
                awaitIgnoringInterrupts(cancellationReleasedWorker);
                if (cancellationCount.get() == 0)
                    mutations.incrementAndGet();
                exited.countDown();
            }
        };

        Thread adminCall = new Thread(purger::purgeOldConnectionStatus,
                "fixture-direct-purge");
        adminCall.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        purger.stop(1, TimeUnit.SECONDS);
        adminCall.join(1_000L);

        assertFalse("Successful stop must leave no direct maintenance thread", adminCall.isAlive());
        assertTrue(exited.await(1, TimeUnit.SECONDS));
        assertEquals(1, cancellationCount.get());
        assertEquals(0, mutations.get());
        int stableMutations = mutations.get();
        Thread.sleep(50L);
        assertEquals(stableMutations, mutations.get());
    }

    @Test
    public void cancellationAndWorkerShareOneMonotonicStopDeadline() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, null,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true) {
            @Override
            ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
                return new ICancellableDataStore.CancellableOperation() {
                    @Override
                    public void cancel() {
                        sleepIgnoringInterrupts(70L);
                        releaseWorker.countDown();
                    }

                    @Override public boolean isCancellationRequested() { return false; }
                    @Override public void close() { }
                };
            }

            @Override
            void purgeOldConnectionStatusWithinScope() {
                started.countDown();
                awaitIgnoringInterrupts(releaseWorker);
                sleepIgnoringInterrupts(80L);
                exited.countDown();
            }
        };

        Thread adminCall = new Thread(purger::purgeOldConnectionStatus,
                "fixture-deadline-purge");
        adminCall.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        long stopStarted = System.nanoTime();
        assertThrows(org.sensorhub.api.common.SensorHubException.class,
                () -> purger.stop(100, TimeUnit.MILLISECONDS));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stopStarted);
        assertTrue("Cancellation must not receive a second full timeout: " + elapsedMillis,
                elapsedMillis < 250L);

        assertTrue(exited.await(1, TimeUnit.SECONDS));
        adminCall.join(1_000L);
        purger.stop(1, TimeUnit.SECONDS);
    }

    @Test
    public void scopeCloseFailureIsStickyAndCanNeverBecomeSuccessfulStop() {
        AtomicInteger closeAttempts = new AtomicInteger();
        DatabasePurger purger = new DatabasePurger(database, bucketStore, 0, null,
                ZoneOffset.UTC, Clock.fixed(NOW, ZoneOffset.UTC), true) {
            @Override
            ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
                return new ICancellableDataStore.CancellableOperation() {
                    @Override public void cancel() { }
                    @Override public boolean isCancellationRequested() { return false; }

                    @Override
                    public void close() {
                        closeAttempts.incrementAndGet();
                        throw new IllegalStateException("fixture scope-close failure");
                    }
                };
            }
        };

        assertThrows(IllegalStateException.class, purger::purgeOldConnectionStatus);
        assertEquals(1, closeAttempts.get());
        assertThrows(org.sensorhub.api.common.SensorHubException.class,
                () -> purger.stop(50, TimeUnit.MILLISECONDS));
        assertThrows("Repeated stop cannot erase the failed close",
                org.sensorhub.api.common.SensorHubException.class,
                () -> purger.stop(50, TimeUnit.MILLISECONDS));
        assertThrows("A failed scope owner must be recreated",
                org.sensorhub.api.common.SensorHubException.class, purger::start);
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

    private static void sleepIgnoringInterrupts(long millis) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        boolean interrupted = false;
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0)
                break;
            try {
                TimeUnit.NANOSECONDS.sleep(remaining);
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }
        if (interrupted)
            Thread.interrupted();
    }

    private static final class TestCancellationOperation
            implements ICancellableDataStore.CancellableOperation {
        private final AtomicInteger cancellationCount;
        private final CountDownLatch release;
        private final AtomicBoolean cancelled = new AtomicBoolean();

        TestCancellationOperation(AtomicInteger cancellationCount, CountDownLatch release) {
            this.cancellationCount = cancellationCount;
            this.release = release;
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                cancellationCount.incrementAndGet();
                release.countDown();
            }
        }

        @Override
        public boolean isCancellationRequested() {
            return cancelled.get();
        }

        @Override
        public void close() { }
    }

    private DatabasePurger purger(IDailyFileJournalService journal, ZoneId zone) {
        return new DatabasePurger(database, bucketStore, 0, journal, zone, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private IObsSystemDatabase databaseThatMutatesBeforeFirstDelete(
            BigId observationId, IObsData replacement, AtomicBoolean mutationRan) {
        IObsStore delegateStore = database.getObservationStore();
        IObsStore observationStore = (IObsStore) Proxy.newProxyInstance(
                IObsStore.class.getClassLoader(), new Class<?>[]{IObsStore.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("removeEntries")
                            && mutationRan.compareAndSet(false, true)) {
                        delegateStore.put(observationId, replacement);
                    }
                    return invokeDelegate(method, delegateStore, args);
                });
        return (IObsSystemDatabase) Proxy.newProxyInstance(
                IObsSystemDatabase.class.getClassLoader(),
                new Class<?>[]{IObsSystemDatabase.class},
                (proxy, method, args) -> method.getName().equals("getObservationStore")
                        && method.getParameterCount() == 0
                        ? observationStore : invokeDelegate(method, database, args));
    }

    private static Object invokeDelegate(java.lang.reflect.Method method, Object delegate,
                                         Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private BigId addSystem(BigId parent, String uid) throws DataStoreException {
        AbstractProcess process = new SMLHelper().createPhysicalComponent().uniqueID(uid).name(uid).build();
        SystemWrapper wrapper = new SystemWrapper(process);
        return parent == null
                ? database.getSystemDescStore().add(wrapper).getInternalID()
                : database.getSystemDescStore().add(parent, wrapper).getInternalID();
    }

    private BigId addDataStream(BigId systemId, String systemUid, String outputName, DataComponent structure)
            throws DataStoreException {
        DataStreamInfo info = new DataStreamInfo.Builder()
                .withName(outputName)
                .withSystem(new FeatureId(systemId, systemUid))
                .withRecordDescription(structure)
                .withRecordEncoding(new TextEncodingImpl())
                .build();
        return database.getDataStreamStore().add(info).getInternalID();
    }

    private DataComponent dailyFileStructure() {
        RADHelper helper = new RADHelper();
        var time = helper.createPrecisionTimeStamp();
        var message = helper.createRapiscanMessage();
        return helper.createRecord().name("dailyFile").addField(time.getName(), time)
                .addField(message.getName(), message).build();
    }

    private DataComponent scalarStructure(String name) {
        RADHelper helper = new RADHelper();
        var time = helper.createPrecisionTimeStamp();
        var value = helper.createCount().name("value").build();
        return helper.createRecord().name(name).addField(time.getName(), time)
                .addField(value.getName(), value).build();
    }

    private DataComponent occupancyStructure() {
        return new OccupancyOutput<>(new SensorSystem()).getRecordDescription();
    }

    private void addStringObservation(BigId dataStream, Instant time, String value) {
        DataComponent structure = dailyFileStructure();
        var block = structure.createDataBlock();
        block.setTimeStamp(0, time);
        block.setStringValue(1, value);
        addObservation(dataStream, time, block);
    }

    private void addScalarObservation(BigId dataStream, Instant time, int value) {
        DataComponent structure = scalarStructure("gammaCounts");
        var block = structure.createDataBlock();
        block.setTimeStamp(0, time);
        block.setIntValue(1, value);
        addObservation(dataStream, time, block);
    }

    private void addOccupancy(BigId dataStream, Instant start, Instant end) {
        Occupancy occupancy = new Occupancy.Builder()
                .samplingTime(end.getEpochSecond())
                .occupancyCount(1)
                .startTime(start.getEpochSecond())
                .endTime(end.getEpochSecond())
                .build();
        addObservation(dataStream, end, Occupancy.fromOccupancy(occupancy));
    }

    private void addObservation(BigId dataStream, Instant time, net.opengis.swe.v20.DataBlock block) {
        database.getObservationStore().add(new ObsData.Builder()
                .withDataStream(dataStream)
                .withPhenomenonTime(time)
                .withResult(block)
                .build());
    }

    private List<IObsData> observations(BigId dataStream) {
        return database.getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(dataStream).build()).toList();
    }

    private static final class ToggleJournal implements IDailyFileJournalService {
        private final AtomicBoolean ready = new AtomicBoolean(true);
        private final AtomicBoolean published = new AtomicBoolean();
        private final ZoneId zone;
        private final DailyFileObservationMode observationMode;
        private final Map<String, LocalDate> checkpoints = new HashMap<>();
        private final List<LocalDate> requestedDays = new ArrayList<>();
        private LocalDate earliestJournalDay;

        private ToggleJournal(ZoneId zone) {
            this(zone, DailyFileObservationMode.FULL);
        }

        private ToggleJournal(ZoneId zone, DailyFileObservationMode observationMode) {
            this.zone = zone;
            this.observationMode = observationMode;
        }

        @Override
        public boolean isJournalReady() {
            return ready.get();
        }

        @Override
        public DailyFileWriterHandle openWriter(String laneId) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public DailyFileObservationMode getObservationMode() {
            return observationMode;
        }

        @Override
        public ZoneId getJournalZoneId() {
            return zone;
        }

        @Override
        public boolean isPublished(String laneId, Instant startInclusive, Instant endExclusive) {
            requestedDays.add(LocalDate.ofInstant(startInclusive, zone));
            return published.get();
        }

        @Override
        public Optional<LocalDate> readObservationPurgeCheckpoint(String feedId) {
            return checkpoint(feedId);
        }

        @Override
        public void writeObservationPurgeCheckpoint(String feedId, LocalDate processedThrough) {
            checkpoints.compute(feedId, (ignored, existing) -> existing == null
                    || processedThrough.isAfter(existing) ? processedThrough : existing);
        }

        @Override
        public Optional<LocalDate> findEarliestJournalDay(String laneId) {
            return Optional.ofNullable(earliestJournalDay);
        }

        private Optional<LocalDate> checkpoint(String feedId) {
            return Optional.ofNullable(checkpoints.get(feedId));
        }
    }
}
