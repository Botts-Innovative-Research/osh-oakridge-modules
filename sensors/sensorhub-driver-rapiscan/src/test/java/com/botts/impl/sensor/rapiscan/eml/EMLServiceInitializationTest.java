package com.botts.impl.sensor.rapiscan.eml;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EMLServiceInitializationTest {

    @Test
    public void concurrentLaneInitializationIsSerialized() throws Exception {
        int laneCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(laneCount);
        CountDownLatch ready = new CountDownLatch(laneCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger activeInitializers = new AtomicInteger();
        AtomicInteger maximumConcurrentInitializers = new AtomicInteger();
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int lane = 0; lane < laneCount; lane++) {
                final int laneNumber = lane;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return EMLService.initializeErnie(() -> {
                        int active = activeInitializers.incrementAndGet();
                        maximumConcurrentInitializers.accumulateAndGet(active, Math::max);
                        try {
                            Thread.sleep(2);
                            return laneNumber;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted while simulating ERNIE initialization", e);
                        } finally {
                            activeInitializers.decrementAndGet();
                        }
                    });
                }));
            }

            assertTrue("All lane initializers should reach the start barrier", ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (int lane = 0; lane < laneCount; lane++)
                assertEquals(lane, futures.get(lane).get(10, TimeUnit.SECONDS).intValue());
            assertEquals("The legacy ERNIE registry must have one initializer at a time",
                    1, maximumConcurrentInitializers.get());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue("Initialization test executor did not terminate",
                    executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
