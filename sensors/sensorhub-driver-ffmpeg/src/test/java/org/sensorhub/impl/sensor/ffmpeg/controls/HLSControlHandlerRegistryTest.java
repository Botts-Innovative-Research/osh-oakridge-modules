package org.sensorhub.impl.sensor.ffmpeg.controls;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.api.service.bucket.IObjectHandler;
import org.junit.Test;
import org.sensorhub.impl.sensor.ffmpeg.controls.hls.HlsStreamHandler;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class HLSControlHandlerRegistryTest {

    @Test
    public void concurrentControlsShareOneHandlerPerBucketService() throws Exception {
        TestBucketService firstService = new TestBucketService();
        TestBucketService recreatedService = new TestBucketService();
        ExecutorService callers = Executors.newFixedThreadPool(50);

        try {
            CountDownLatch ready = new CountDownLatch(50);
            CountDownLatch start = new CountDownLatch(1);
            Set<HlsStreamHandler> resolvedHandlers = ConcurrentHashMap.newKeySet();
            @SuppressWarnings("unchecked")
            Future<HlsStreamHandler>[] futures = new Future[50];

            for (int index = 0; index < futures.length; index++) {
                futures[index] = callers.submit(() -> {
                    ready.countDown();
                    start.await();
                    HlsStreamHandler handler = HLSControl.resolveHlsHandler(firstService);
                    resolvedHandlers.add(handler);
                    return handler;
                });
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            for (Future<HlsStreamHandler> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }

            assertEquals(1, resolvedHandlers.size());
            assertEquals(1, firstService.registrationCount.get());

            HlsStreamHandler replacement = HLSControl.resolveHlsHandler(recreatedService);
            assertNotSame(resolvedHandlers.iterator().next(), replacement);
            assertEquals(1, recreatedService.registrationCount.get());
        } finally {
            callers.shutdownNow();
            firstService.shutdown();
            recreatedService.shutdown();
        }
    }

    private static final class TestBucketService implements IBucketService {
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final AtomicInteger registrationCount = new AtomicInteger();
        private final IBucketStore store = (IBucketStore) Proxy.newProxyInstance(
                IBucketStore.class.getClassLoader(),
                new Class<?>[]{IBucketStore.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));

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
            registrationCount.incrementAndGet();
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
