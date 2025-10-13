package com.botts.impl.service.oscar;

import com.botts.api.service.bucket.IBucketService;
import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServiceConfig;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.botts.impl.service.oscar.stats.StatisticsControl;
import com.botts.impl.service.oscar.stats.StatisticsOutput;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
    public void testOutputCanRestart() {
        for (int i = 0; i < 10; i++) {
            statsOutput.start();
            statsOutput.stop();
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
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());
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
    public void testControlWithNoTimes() throws ExecutionException, InterruptedException {
        var cmdDesc = statsControl.getCommandDescription().copy();
        var cmdData = cmdDesc.createDataBlock();
        cmdData.setTimeStamp(0, null);
        cmdData.setTimeStamp(1, null);
        int id = 1;
        var res = statsControl.submitCommand(new CommandData(id++, cmdData)).get();
        assertNull(res.getMessage());
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());
        var resObsIds = res.getResult().getObservationIDs().stream().toList();
        assertFalse(resObsIds.isEmpty());
        var resObsId = resObsIds.get(0);
        System.out.println(hub.getIdEncoders().getObsIdEncoder().encodeID(resObsId));
        assertNotNull(resObsId);
        var obs = hub.getDatabaseRegistry().getFederatedDatabase().getObservationStore().get(resObsId);
        assertNotNull(obs);
        System.out.println(obs);
    }

    @Test
    public void testVerifyOutputLatestObsFromCommand() throws ExecutionException, InterruptedException {
        // Step 1: Publish 100 stats records
        Random random = new Random();
        var max = random.nextInt(500);
        for (int i = 0; i < max; i++)
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
            assertNotEquals(beforeId.toString(), afterId, "Command should create a new observation, not reuse previous one");
    }

    @Test
    public void testRaceCondition() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 500; i++)
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
