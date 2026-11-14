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
        config.security.requireAuth = false;
        config.autoStart = true;
        return (BucketService) reg.loadModule(config);
    }

    private IHttpServer<?> loadHttpServer() throws SensorHubException {
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.autoStart = true;
        httpConfig.moduleClass = HttpServer.class.getCanonicalName();
        httpConfig.id = UUID.randomUUID().toString();
        httpConfig.httpPort = freePort();
        return (IHttpServer<?>) reg.loadModule(httpConfig);
    }

    private static int freePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    @Test
    public void testDownloadDispositionAndXlsxMimeType() throws Exception {
        var store = service.getBucketStore();
        try {
            try (var out = store.putObject(TEST_BUCKET, "sample_report.pdf", java.util.Collections.emptyMap())) {
                out.write("%PDF-1.4\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            try (var out = store.putObject(TEST_BUCKET, "sample_extract.xlsx", java.util.Collections.emptyMap())) {
                out.write(new byte[]{'P', 'K', 3, 4});
            }
            assertEquals(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    store.getObjectMimeType(TEST_BUCKET, "sample_extract.xlsx")
            );

            var client = java.net.http.HttpClient.newHttpClient();
            String base = httpServer.getPublicEndpointUrl(service.getPublicEndpointUrl());
            if (!base.endsWith("/")) base += "/";

            var inlineResp = client.send(
                    java.net.http.HttpRequest.newBuilder().GET()
                            .uri(java.net.URI.create(base + TEST_BUCKET + "/sample_report.pdf")).build(),
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray()
            );
            assertEquals(200, inlineResp.statusCode());
            assertTrue(inlineResp.headers().firstValue("Content-Disposition").isEmpty());

            var dlResp = client.send(
                    java.net.http.HttpRequest.newBuilder().GET()
                            .uri(java.net.URI.create(base + TEST_BUCKET + "/sample_report.pdf?download=1")).build(),
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray()
            );
            assertEquals(200, dlResp.statusCode());
            assertEquals(
                    "attachment; filename=\"sample_report.pdf\"",
                    dlResp.headers().firstValue("Content-Disposition").orElse("")
            );

            var xlsxResp = client.send(
                    java.net.http.HttpRequest.newBuilder().GET()
                            .uri(java.net.URI.create(base + TEST_BUCKET + "/sample_extract.xlsx")).build(),
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray()
            );
            assertEquals(200, xlsxResp.statusCode());
            assertEquals(
                    "attachment; filename=\"sample_extract.xlsx\"",
                    xlsxResp.headers().firstValue("Content-Disposition").orElse("")
            );
        } finally {
            try { store.deleteObject(TEST_BUCKET, "sample_report.pdf"); } catch (Exception ignored) {}
            try { store.deleteObject(TEST_BUCKET, "sample_extract.xlsx"); } catch (Exception ignored) {}
        }
    }

    @After
    public void cleanup() {
        if (hub != null) {
            hub.stop();
            hub = null;
        }
    }

}
