package com.botts.impl.service.oscar;

import com.botts.api.service.bucket.IBucketService;
import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServiceConfig;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.botts.impl.service.oscar.stats.StatisticsControl;
import com.botts.impl.service.oscar.stats.StatisticsOutput;
import com.botts.impl.service.oscar.stats.Statistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.ICancellableDataStore;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.utils.Async;
import org.vast.data.DataBlockMixed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class StatisticsTests {

    OSCARServiceModule oscarServiceModule;
    BucketService bucketService;
    ModuleRegistry reg;
    OSCARSystem system;
    ISensorHub hub;
    IHttpServer httpServer;

    StatisticsOutput statsOutput;
    StatisticsControl statsControl;

    @Before
    public void setup() throws SensorHubException, TimeoutException {
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();

        loadHTTPServer();

        var bucketConfig = createBucketServiceConfig();
        loadAndStartBucketService(bucketConfig);
        assertTrue(bucketService.isStarted());

        var oscarConfig = createOscarServiceConfig();
        loadAndStartOscarService(oscarConfig);
        assertTrue(oscarServiceModule.isStarted());

        statsOutput = (StatisticsOutput) system.getOutputs().get(StatisticsOutput.NAME);
        statsControl = (StatisticsControl) system.getCommandInputs().get(StatisticsControl.NAME);

        Async.waitForCondition(() -> hub.getSystemDriverRegistry().isRegistered(system.getUniqueIdentifier()), 10000);
    }

    @Test
    public void testOutputAndControlExists() throws SensorHubException {
        assertNotNull(statsOutput);
        assertNotNull(statsControl);
    }

    @Test
    public void testOutput() {
        statsOutput.publishLatestStatistics();
        var latestRecord = statsOutput.getLatestRecord();
        assertNotNull(latestRecord);
    }

    @Test
    public void legacyStatisticsCutoffDoesNotDoubleCountExistingTotals() {
        statsOutput.stop();
        var federatedDatabase = hub.getDatabaseRegistry().getFederatedDatabase();
        var dataStreamIds = federatedDatabase.getDataStreamStore().selectKeys(
                new DataStreamFilter.Builder()
                        .withSystems().withUniqueIDs(system.getUniqueIdentifier()).done()
                        .withOutputNames(StatisticsOutput.NAME)
                        .build()
        ).map(key -> key.getInternalID()).toList();
        assertEquals(1, dataStreamIds.size());

        BigId statisticsDataStream = dataStreamIds.get(0);
        var writableDatabase = hub.getDatabaseRegistry()
                .getObsDatabaseByNum(statisticsDataStream.getScope());
        assertNotNull(writableDatabase);
        writableDatabase.getObservationStore().removeEntries(new ObsFilter.Builder()
                .withDataStreams(statisticsDataStream)
                .build());

        Instant intendedCutoff = Instant.parse("2026-10-22T12:34:56Z");
        var legacyRecord = statsOutput.getRecordDescription().createDataBlock();
        legacyRecord.setLongValue(0, intendedCutoff.getEpochSecond());
        legacyRecord.setLongValue(1, 7L);
        writableDatabase.getObservationStore().add(new ObsData.Builder()
                .withDataStream(statisticsDataStream)
                .withPhenomenonTime(legacyRecord.getTimeStamp(0))
                .withResult(legacyRecord)
                .build());

        AtomicReference<Instant> incrementalStart = new AtomicReference<>();
        StatisticsOutput compatibilityOutput = new LegacyCompatibilityStatisticsOutput(
                system, federatedDatabase, incrementalStart);

        compatibilityOutput.publishLatestStatistics();

        assertEquals("The incremental query must begin at the intended legacy cutoff",
                intendedCutoff, incrementalStart.get());
        assertEquals("No new source observations means the prior all-time total is unchanged",
                7L, compatibilityOutput.getLatestRecord().getLongValue(1));
    }

    private static final class LegacyCompatibilityStatisticsOutput extends StatisticsOutput {
        private final AtomicReference<Instant> incrementalStart;

        private LegacyCompatibilityStatisticsOutput(OSCARSystem parent,
                                                    IObsSystemDatabase database,
                                                    AtomicReference<Instant> incrementalStart) {
            super(parent, database, 60);
            this.incrementalStart = incrementalStart;
        }

        @Override
        protected Statistics getStatsAfter(Instant exclusiveStart, Instant inclusiveEnd,
                                           Collection<String> laneUIDs) {
            incrementalStart.set(exclusiveStart);
            return Statistics.zero();
        }

        @Override
        protected Statistics getStats(Instant start, Instant end,
                                      Collection<String> laneUIDs) {
            return Statistics.zero();
        }
    }

    @Test
    public void testOutputCanRestart() {
        for (int i = 0; i < 10; i++) {
            statsOutput.start();
            statsOutput.stop();
        }
    }

    @Test
    public void statisticsStopIsTruthfulAndLeavesNoPublishingWorker() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        AtomicInteger publications = new AtomicInteger();
        StatisticsOutput blocking = new BlockingStatisticsOutput(
                (OSCARSystem) statsOutput.getParentProducer(),
                hub.getDatabaseRegistry().getFederatedDatabase(), started, release, exited,
                publications);

        blocking.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertThrows(IllegalStateException.class,
                () -> blocking.stop(100, TimeUnit.MILLISECONDS));
        release.countDown();
        assertTrue(exited.await(1, TimeUnit.SECONDS));
        blocking.stop(1, TimeUnit.SECONDS);
        int stablePublications = publications.get();
        Thread.sleep(50L);
        assertEquals(stablePublications, publications.get());
    }

    @Test
    public void statisticsStopCancelsDatabaseWorkWithoutBlockingTheRequestPhase() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch cancellationReleasedQuery = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        StatisticsOutput blocking = new CancellableBlockingStatisticsOutput(
                (OSCARSystem) statsOutput.getParentProducer(),
                hub.getDatabaseRegistry().getFederatedDatabase(), started,
                cancellationReleasedQuery, exited, cancellations, closes);

        blocking.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        long requestStarted = System.nanoTime();
        blocking.requestStop();
        long requestMillis = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - requestStarted);
        assertTrue("Cancellation delivery must not block the module-registry thread",
                requestMillis < 250L);

        blocking.awaitStop(2, TimeUnit.SECONDS);
        assertTrue(exited.await(1, TimeUnit.SECONDS));
        assertEquals(1, cancellations.get());
        assertEquals(1, closes.get());
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
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0)
                return;
            try {
                TimeUnit.NANOSECONDS.sleep(remaining);
            } catch (InterruptedException ignored) {
                // Model a JDBC cancellation request which does not promptly honor interruption.
            }
        }
    }

    private static final class BlockingStatisticsOutput extends StatisticsOutput {
        private final CountDownLatch started;
        private final CountDownLatch release;
        private final CountDownLatch exited;
        private final AtomicInteger publications;

        private BlockingStatisticsOutput(OSCARSystem parent,
                                         IObsSystemDatabase database,
                                         CountDownLatch started,
                                         CountDownLatch release,
                                         CountDownLatch exited,
                                         AtomicInteger publications) {
            super(parent, database, 60);
            this.started = started;
            this.release = release;
            this.exited = exited;
            this.publications = publications;
        }

        @Override
        protected void publishLatestStatisticsWithinScope() {
            started.countDown();
            awaitIgnoringInterrupts(release);
            publications.incrementAndGet();
            exited.countDown();
        }
    }

    private static final class CancellableBlockingStatisticsOutput extends StatisticsOutput {
        private final CountDownLatch started;
        private final CountDownLatch cancellationReleasedQuery;
        private final CountDownLatch exited;
        private final AtomicInteger cancellations;
        private final AtomicInteger closes;

        private CancellableBlockingStatisticsOutput(OSCARSystem parent,
                                                    IObsSystemDatabase database,
                                                    CountDownLatch started,
                                                    CountDownLatch cancellationReleasedQuery,
                                                    CountDownLatch exited,
                                                    AtomicInteger cancellations,
                                                    AtomicInteger closes) {
            super(parent, database, 60);
            this.started = started;
            this.cancellationReleasedQuery = cancellationReleasedQuery;
            this.exited = exited;
            this.cancellations = cancellations;
            this.closes = closes;
        }

        @Override
        protected ICancellableDataStore.CancellableOperation beginCancellableDataStoreOperation() {
            return new ICancellableDataStore.CancellableOperation() {
                private volatile boolean cancelled;

                @Override
                public void cancel() {
                    if (!cancelled) {
                        cancelled = true;
                        sleepIgnoringInterrupts(500L);
                        cancellations.incrementAndGet();
                        cancellationReleasedQuery.countDown();
                    }
                }

                @Override
                public boolean isCancellationRequested() {
                    return cancelled;
                }

                @Override
                public void close() {
                    closes.incrementAndGet();
                }
            };
        }

        @Override
        protected void publishLatestStatisticsWithinScope() {
            started.countDown();
            awaitIgnoringInterrupts(cancellationReleasedQuery);
            exited.countDown();
        }
    }

    @Test
    public void testControlWithTimes() throws ExecutionException, InterruptedException {
        var cmdDesc = statsControl.getCommandDescription().copy();
        var cmdData = cmdDesc.createDataBlock();
        Instant before = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant now = Instant.now();
        cmdData.setTimeStamp(0, before);
        cmdData.setTimeStamp(1, now);
        int id = 1;
        var res = statsControl.submitCommand(new CommandData(id++, cmdData)).get();
        assertNull(res.getMessage());
        assertEquals(ICommandStatus.CommandStatusCode.COMPLETED, res.getStatusCode());
        var resDataBlocks = res.getResult().getInlineRecords().stream().toList();
        assertFalse(resDataBlocks.isEmpty());
        var resData = resDataBlocks.get(0);
        assertNotNull(resData);

        // Null end field
        cmdData.setTimeStamp(1, null);
        res = statsControl.submitCommand(new CommandData(id++, cmdData)).get();
        assertEquals(ICommandStatus.CommandStatusCode.REJECTED, res.getStatusCode());

        // Null start field
        cmdData.setTimeStamp(0, null);
        cmdData.setTimeStamp(1, now);
        res = statsControl.submitCommand(new CommandData(id, cmdData)).get();
        assertEquals(ICommandStatus.CommandStatusCode.REJECTED, res.getStatusCode());
    }

    @Test
    public void testControlScopesStatisticsToLaneUIDs() throws ExecutionException, InterruptedException {
        var cmdData = statsControl.getCommandDescription().copy().createDataBlock();
        cmdData.setTimeStamp(0, Instant.EPOCH);
        cmdData.setTimeStamp(1, Instant.now());
        cmdData.setStringValue(2, "urn:osh:system:lane:not-present");

        var status = statsControl.submitCommand(new CommandData(1, cmdData)).get();
        assertEquals(ICommandStatus.CommandStatusCode.COMPLETED, status.getStatusCode());

        var result = status.getResult().getInlineRecords().stream().findFirst().orElseThrow();
        for (int i = 0; i < result.getAtomCount(); i++)
            assertEquals(0, result.getLongValue(i));
    }

    @Test
    public void testControlWithNoTimes() throws ExecutionException, InterruptedException {
        var db = hub.getDatabaseRegistry().getFederatedDatabase().getObservationStore();
        var latestBefore = db.selectKeys(
                new ObsFilter.Builder()
                        .withLatestResult()
                        .withSystems().withUniqueIDs(statsOutput.getParentProducer().getUniqueIdentifier())
                        .done()
                        .withDataStreams().withOutputNames(statsOutput.getName())
                        .done()
                        .withLimit(1)
                        .build()
        ).toList();
        BigId beforeId = latestBefore.isEmpty() ? null : latestBefore.get(0);

        var cmdDesc = statsControl.getCommandDescription().copy();
        var cmdData = cmdDesc.createDataBlock();
        cmdData.setTimeStamp(0, null);
        cmdData.setTimeStamp(1, null);
        int id = 1;
        var res = statsControl.submitCommand(new CommandData(id++, cmdData)).get();
        assertNull(res.getMessage());
        assertEquals(ICommandStatus.CommandStatusCode.COMPLETED, res.getStatusCode());
        var resObsIds = res.getResult().getObservationIDs().stream().toList();
        assertFalse(resObsIds.isEmpty());
        var resObsId = resObsIds.get(0);
        System.out.println(hub.getIdEncoders().getObsIdEncoder().encodeID(resObsId));
        assertNotNull(resObsId);
        if (beforeId != null)
            assertNotEquals("Command must return a newly persisted observation", beforeId, resObsId);
        var obs = db.get(resObsId);
        assertNotNull(obs);
        System.out.println(obs);
    }

    @Test
    public void testVerifyOutputLatestObsFromCommand() throws ExecutionException, InterruptedException {
        // Seed a deterministic baseline record which the command must not reuse.
        statsOutput.publishLatestStatistics();

        var db = hub.getDatabaseRegistry().getFederatedDatabase().getObservationStore();
        var latestBefore = db.selectKeys(
                new ObsFilter.Builder()
                        .withLatestResult()
                        .withSystems().withUniqueIDs(statsOutput.getParentProducer().getUniqueIdentifier())
                        .done()
                        .withDataStreams().withOutputNames(statsOutput.getName())
                        .done()
                        .withLimit(1)
                        .build()
        ).toList();

        BigId beforeId = latestBefore.isEmpty() ? null : latestBefore.get(0);

        var cmdDesc = statsControl.getCommandDescription().copy();
        var cmdData = cmdDesc.createDataBlock();
        cmdData.setTimeStamp(0, null);
        cmdData.setTimeStamp(1, null);

        var res = statsControl.submitCommand(new CommandData(1, cmdData)).get();
        assertEquals(ICommandStatus.CommandStatusCode.COMPLETED, res.getStatusCode());
        var resObsIds = res.getResult().getObservationIDs().stream().toList();
        assertFalse("Command should return at least one observation ID", resObsIds.isEmpty());
        var resObsId = resObsIds.get(0);

        var latestAfter = db.selectKeys(
                new ObsFilter.Builder()
                        .withLatestResult()
                        .withSystems().withUniqueIDs(statsOutput.getParentProducer().getUniqueIdentifier())
                        .done()
                        .withDataStreams().withOutputNames(statsOutput.getName())
                        .done()
                        .withLimit(1)
                        .build()
        ).toList();

        assertFalse("Database should have at least one observation", latestAfter.isEmpty());
        var afterId = latestAfter.get(0);

        // The latest ID after the command should match the command’s observation ID
        assertEquals(afterId, resObsId);

        if (beforeId != null)
            assertNotEquals("Command should create a new observation, not reuse previous one", beforeId, afterId);
    }

    @Test
    public void testRaceCondition() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 200; i++)
            testVerifyOutputLatestObsFromCommand();
    }

    private OSCARServiceConfig createOscarServiceConfig() throws SensorHubException {
        OSCARServiceConfig serviceConfig = (OSCARServiceConfig) reg.createModuleConfig(new Descriptor());
        serviceConfig.autoStart = true;
        serviceConfig.siteDiagramConfig = new SiteDiagramConfig();
        serviceConfig.nodeId = "test-node-id";

        return serviceConfig;
    }

    private void loadAndStartOscarService(OSCARServiceConfig config) throws SensorHubException {
        oscarServiceModule = (OSCARServiceModule) reg.loadModule(config);
        system = oscarServiceModule.getOSCARSystem();

        reg.startModule(oscarServiceModule.getLocalID());
        var isStarted = oscarServiceModule.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        assertTrue("oscar service module should be started", isStarted);
    }

    private BucketServiceConfig createBucketServiceConfig() throws SensorHubException {
        BucketServiceConfig bucketServiceConfig = (BucketServiceConfig) reg.createModuleConfig(new com.botts.impl.service.bucket.Descriptor());
        bucketServiceConfig.fileStoreRootDir = "oscar-test";

        List<String> buckets = new ArrayList<>();
        buckets.add("reports");
        buckets.add("videos");
        buckets.add("sitemap");

        bucketServiceConfig.initialBuckets = buckets;
        return bucketServiceConfig;
    }

    private void loadAndStartBucketService(BucketServiceConfig config) throws SensorHubException {
        bucketService = (BucketService) reg.loadModule(config);

        reg.startModule(bucketService.getLocalID());
        var isStarted = bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        assertTrue(isStarted);
    }

    private void loadHTTPServer() throws SensorHubException {
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.autoStart = true;
        httpConfig.moduleClass = HttpServer.class.getCanonicalName();
        httpConfig.id = UUID.randomUUID().toString();
        httpServer = (IHttpServer<?>) reg.loadModule(httpConfig);
    }

    @After
    public void cleanup() {
        if (hub != null) {
            hub.stop();
            hub = null;
        }
    }

}
