package org.sensorhub.impl.sensor.ffmpeg.controls.hls;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.api.service.bucket.IObjectHandler;
import com.botts.impl.service.bucket.util.InvalidRequestException;
import com.botts.impl.service.bucket.util.RequestContext;
import org.junit.After;
import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HlsStreamHandlerTest {

    private static final String STREAM_KEY = "streams/camera/live.m3u8";
    private final List<TestBucketService> services = new ArrayList<>();

    @After
    public void stopSchedulers() {
        services.forEach(TestBucketService::shutdown);
    }

    @Test
    public void authorizedMissingManifestRefreshesOnlyTheVideoStream() throws Exception {
        TestStore store = new TestStore();
        store.exists = false;
        TestBucketService service = service(store);
        TestableHlsStreamHandler handler = new TestableHlsStreamHandler(service, "videos");
        // Disable wall-clock cleanup; this test drives timeout boundaries explicitly.
        service.shutdown();
        handler.addControl(STREAM_KEY, null);
        assertTrue(handler.refreshManifestActivity("videos", STREAM_KEY, 1L));

        InvalidRequestException missing = expectInvalidRequest(() ->
                handler.doGet(context("videos", STREAM_KEY, acceptingResponse())));
        assertEquals(InvalidRequestException.ErrorCode.NOT_FOUND, missing.getErrorCode());
        assertEquals(1, handler.permissionChecks);
        long refreshedAt = handler.getLastAccessMillis(STREAM_KEY);
        assertTrue(refreshedAt > 1L);

        InvalidRequestException otherBucketMissing = expectInvalidRequest(() ->
                handler.doGet(context("archive", STREAM_KEY, acceptingResponse())));
        assertEquals(InvalidRequestException.ErrorCode.NOT_FOUND, otherBucketMissing.getErrorCode());
        assertEquals(2, handler.permissionChecks);
        assertEquals("A request for the same key in another bucket must not refresh HLS activity",
                refreshedAt, handler.getLastAccessMillis(STREAM_KEY));

        handler.cleanupInactiveStreams(refreshedAt + HlsStreamHandler.STREAM_TIMEOUT_MS - 1);
        assertTrue(handler.isActiveStream(STREAM_KEY));

        // Model a manifest request winning a race with a cleanup pass based on the previous
        // deadline: the refreshed activity record must survive that stale cleanup deadline.
        long racedRefresh = refreshedAt + HlsStreamHandler.STREAM_TIMEOUT_MS + 1;
        assertTrue(handler.refreshManifestActivity("videos", STREAM_KEY, racedRefresh));
        handler.cleanupInactiveStreams(refreshedAt + HlsStreamHandler.STREAM_TIMEOUT_MS);
        assertTrue(handler.isActiveStream(STREAM_KEY));
        assertEquals(racedRefresh, handler.getLastAccessMillis(STREAM_KEY));

        handler.cleanupInactiveStreams(racedRefresh + HlsStreamHandler.STREAM_TIMEOUT_MS);
        assertFalse("An unrequested stream must still age out after 30 seconds",
                handler.isActiveStream(STREAM_KEY));
    }

    @Test
    public void closesObjectInputStreamWhenResponseTransferFails() throws Exception {
        TestStore store = new TestStore();
        store.exists = true;
        store.data = new CloseTrackingInputStream(new byte[]{1, 2, 3});
        TestBucketService service = service(store);
        TestableHlsStreamHandler handler = new TestableHlsStreamHandler(service, "videos");
        service.shutdown();

        try {
            handler.doGet(context("videos", STREAM_KEY, failingResponse()));
            fail("Expected the client response write to fail");
        } catch (IOException expected) {
            assertEquals("client disconnected", expected.getMessage());
        }

        assertTrue("The bucket object stream must close when transferTo fails", store.data.closed);
    }

    @Test
    public void emptyManifestIsNotServedAsAReadyPlaylist() throws Exception {
        TestStore store = new TestStore();
        store.exists = true;
        store.data = new CloseTrackingInputStream(new byte[0]);
        TestBucketService service = service(store);
        TestableHlsStreamHandler handler = new TestableHlsStreamHandler(service, "videos");
        service.shutdown();
        handler.addControl(STREAM_KEY, null);

        InvalidRequestException empty = expectInvalidRequest(() ->
                handler.doGet(context("videos", STREAM_KEY, acceptingResponse())));

        assertEquals(InvalidRequestException.ErrorCode.NOT_FOUND, empty.getErrorCode());
        assertEquals(1, handler.permissionChecks);
        assertTrue("An empty-manifest poll should still keep the live stream active",
                handler.isActiveStream(STREAM_KEY));
    }

    @Test
    public void cleanupTaskEndsWithBucketServiceExecutor() throws Exception {
        TestBucketService service = service(new TestStore());

        new TestableHlsStreamHandler(service, "videos");

        assertEquals(1, service.scheduledTaskCount());
        service.shutdown();
        assertTrue(service.awaitTermination());
        assertEquals("The service scheduler must not retain a stopped HLS handler",
                0, service.scheduledTaskCount());
    }

    private TestBucketService service(TestStore store) {
        TestBucketService service = new TestBucketService(store.proxy());
        services.add(service);
        return service;
    }

    private static RequestContext context(String bucket, String key,
                                          HttpServletResponse response) throws IOException {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getHeaderNames".equals(method.getName())) {
                        return Collections.emptyEnumeration();
                    }
                    if ("getPathInfo".equals(method.getName())) {
                        return "/" + bucket + "/" + key;
                    }
                    return defaultValue(method.getReturnType());
                });
        return new RequestContext(request, response, null);
    }

    private static HttpServletResponse acceptingResponse() {
        return response(new ServletOutputStream() {
            @Override
            public void write(int value) {
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {
            }
        });
    }

    private static HttpServletResponse failingResponse() {
        return response(new ServletOutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("client disconnected");
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {
            }
        });
    }

    private static HttpServletResponse response(ServletOutputStream output) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> "getOutputStream".equals(method.getName())
                        ? output : defaultValue(method.getReturnType()));
    }

    private static InvalidRequestException expectInvalidRequest(IORunnable runnable) throws Exception {
        try {
            runnable.run();
            fail("Expected an InvalidRequestException");
            return null;
        } catch (InvalidRequestException expected) {
            return expected;
        }
    }

    @FunctionalInterface
    private interface IORunnable {
        void run() throws Exception;
    }

    private static final class TestableHlsStreamHandler extends HlsStreamHandler {
        private int permissionChecks;

        private TestableHlsStreamHandler(IBucketService service, String streamBucketName) {
            super(service, streamBucketName);
        }

        @Override
        protected void checkGetPermission(RequestContext ctx, String bucketName) {
            permissionChecks++;
        }
    }

    private static final class TestBucketService implements IBucketService {
        private final IBucketStore store;
        private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

        private TestBucketService(IBucketStore store) {
            this.store = store;
            scheduler.setRemoveOnCancelPolicy(true);
        }

        @Override
        public IBucketStore getBucketStore() {
            return store;
        }

        @Override
        public String getPublicEndpointUrl() {
            return "/bucket";
        }

        @Override
        public ExecutorService getThreadPool() {
            return scheduler;
        }

        @Override
        public void registerObjectHandler(IObjectHandler handler) {
        }

        @Override
        public void unregisterObjectHandler(IObjectHandler handler) {
        }

        @Override
        public IObjectHandler getObjectHandler(String bucketName, String objectKey) {
            return null;
        }

        @Override
        public IObjectHandler getObjectHandler(String bucketName, String objectKey,
                                               HttpServletRequest request) {
            return null;
        }

        private void shutdown() {
            scheduler.shutdown();
        }

        private int scheduledTaskCount() {
            return scheduler.getQueue().size();
        }

        private boolean awaitTermination() throws InterruptedException {
            return scheduler.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static final class TestStore implements InvocationHandler {
        private boolean exists;
        private CloseTrackingInputStream data;

        private IBucketStore proxy() {
            return (IBucketStore) Proxy.newProxyInstance(
                    IBucketStore.class.getClassLoader(),
                    new Class<?>[]{IBucketStore.class}, this);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            switch (method.getName()) {
                case "objectExists":
                    return exists;
                case "getObjectSize":
                    return data != null ? (long) data.available() : 1L;
                case "getObjectMimeType":
                    return "application/vnd.apple.mpegurl";
                case "getObject":
                    return data;
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        private CloseTrackingInputStream(byte[] data) {
            super(data);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return '\0';
    }
}
