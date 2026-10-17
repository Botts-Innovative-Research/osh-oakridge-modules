package com.botts.impl.sensor.rapiscan;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class RapiscanThreadPoolManagerTest {

    @Test
    public void blockingReadersRunConcurrently() throws Exception {
        var readersStarted = new CountDownLatch(2);
        var releaseReaders = new CountDownLatch(1);
        Runnable reader = () -> {
            readersStarted.countDown();
            try {
                releaseReaders.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        var pool = RapiscanThreadPoolManager.getInstance();
        var first = pool.submitMessageReader(reader);
        var second = pool.submitMessageReader(reader);

        try {
            assertTrue("message readers were serialized", readersStarted.await(2, TimeUnit.SECONDS));
        } finally {
            releaseReaders.countDown();
        }

        first.get(2, TimeUnit.SECONDS);
        second.get(2, TimeUnit.SECONDS);
    }
}
