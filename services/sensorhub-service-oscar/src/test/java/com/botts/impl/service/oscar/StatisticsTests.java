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
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.vast.data.DataBlockMixed;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    public void setup() throws SensorHubException {
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
    }

    @Test
    public void testOutputAndControlExists() throws SensorHubException {
        assertNotNull(statsOutput);
        assertNotNull(statsControl);
    }

    @Test
    public void testOutput() {
        statsOutput.publishLatestStatistics();
        // TODO: Verify accuracy
        var latestRecord = statsOutput.getLatestRecord();
        assertNotNull(latestRecord);
    }

    @Test
    public void testControlWithTimes() {
        // TODO: send command and verify result
    }

    @Test
    public void testControlWithNoTimes() {
        // TODO: send command with null time and verify result
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
        httpServer = (IHttpServer) reg.loadModule(httpConfig);
    }

    @After
    public void cleanup() {
        if (hub != null) {
            hub.stop();
            hub = null;
        }
    }

}
