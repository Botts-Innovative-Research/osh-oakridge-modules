package com.botts.impl.service.bucket;

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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.botts.impl.service.bucket.AbstractBucketStoreTest.TEST_BUCKET;
import static org.junit.Assert.*;

public class BucketServiceTest {

    BucketService service;
    ISensorHub hub;
    ModuleRegistry reg;
    IHttpServer<?> httpServer;

    @Before
    public void setup() throws SensorHubException {
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();
        httpServer = loadHttpServer();
        httpServer.waitForState(ModuleEvent.ModuleState.STARTED, 2000);
        service = loadBucketService();
        service.waitForState(ModuleEvent.ModuleState.STARTED, 1000);
    }

    private BucketService loadBucketService() throws SensorHubException {
        BucketServiceConfig config = new BucketServiceConfig();
        config.initialBuckets = List.of(TEST_BUCKET);
        config.fileStoreRootDir = "src/test/resources/test-root";
        config.autoStart = true;
        return (BucketService) reg.loadModule(config);
    }

    private IHttpServer<?> loadHttpServer() throws SensorHubException {
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.autoStart = true;
        httpConfig.moduleClass = HttpServer.class.getCanonicalName();
        httpConfig.id = UUID.randomUUID().toString();
        return (IHttpServer<?>) reg.loadModule(httpConfig);
    }

    @Test
    public void testVerifyBucketService() {
        assertNotNull(service.getBucketStore());
        var url = service.getPublicEndpointUrl();
        System.out.println(url);
        assertNotNull(url);
        assertFalse(url.isBlank());
        assertNotNull(service.getThreadPool());
        var objHandler = service.getObjectHandler(TEST_BUCKET, "test");
        assertNotNull(objHandler);
        System.out.println(objHandler.getObjectPattern());
    }

    @Test
    public void testCustomObjectHandler() throws IOException {
        var defaultHandler = service.getObjectHandler(TEST_BUCKET, "test");
        assertNotNull(defaultHandler);
        var handler = service.getObjectHandler(TEST_BUCKET, "item.test");
        assertEquals(defaultHandler, handler);
        var expectedHandler = new TestObjectHandler(service.getBucketStore());
        service.registerObjectHandler(expectedHandler);
        handler = service.getObjectHandler(TEST_BUCKET, "item.test");
        assertEquals(expectedHandler, handler);
        System.out.println(handler.getObjectPattern());
        service.unregisterObjectHandler(expectedHandler);
        handler = service.getObjectHandler(TEST_BUCKET, "item.test");
        assertEquals(defaultHandler, handler);
    }

    @After
    public void cleanup() {
        if (hub != null) {
            hub.stop();
            hub = null;
        }
    }

}
