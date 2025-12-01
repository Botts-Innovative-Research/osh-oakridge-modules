package com.botts.impl.sensor.rapiscan;

import org.sensorhub.utils.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RapiscanThreadPoolManager {

    private static final Logger logger = LoggerFactory.getLogger(RapiscanThreadPoolManager.class);
    private static volatile RapiscanThreadPoolManager instance;
    private static final Object lock = new Object();

    private final ExecutorService messageReaderPool;
    private final ScheduledExecutorService heartbeatPool;
    private final ExecutorService processingPool;

    private final AtomicInteger activeSensors = new AtomicInteger(0);

    private RapiscanThreadPoolManager() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        messageReaderPool = new ThreadPoolExecutor(
                0,
                50,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("Rapiscan-MessageReader")
        );

        heartbeatPool = Executors.newScheduledThreadPool(
                Math.min(10, availableProcessors),
                new NamedThreadFactory("Rapiscan-Heartbeat")
        );

        processingPool = new ThreadPoolExecutor(
                availableProcessors,
                availableProcessors * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new NamedThreadFactory("Rapiscan-Processing"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        logger.info("RapiscanThreadPoolManager initialized with {} processors", availableProcessors);
        logger.info("Message reader pool: 0-100 threads");
        logger.info("Heartbeat pool: {} threads", Math.min(10, availableProcessors));
        logger.info("Processing pool: {}-{} threads", availableProcessors, availableProcessors * 2);
    }

    public static RapiscanThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new RapiscanThreadPoolManager();
                }
            }
        }
        return instance;
    }

    public Future<?> submitMessageReader(Runnable task) {
        return messageReaderPool.submit(task);
    }

    public ScheduledFuture<?> scheduleHeartbeat(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return heartbeatPool.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public Future<?> submitProcessing(Runnable task) {
        return processingPool.submit(task);
    }

    public void registerSensor() {
        int count = activeSensors.incrementAndGet();
        logger.debug("Registered sensor. Active sensors: {}", count);
    }

    public void unregisterSensor() {
        int count = activeSensors.decrementAndGet();
        logger.debug("Unregistered sensor. Active sensors: {}", count);
    }

    public void shutdown() {
        logger.info("Shutting down RapiscanThreadPoolManager...");

        messageReaderPool.shutdown();
        heartbeatPool.shutdown();
        processingPool.shutdown();

        try {
            if (!messageReaderPool.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Message reader pool did not terminate gracefully");
                messageReaderPool.shutdownNow();
            }

            if (!heartbeatPool.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Heartbeat pool did not terminate gracefully");
                heartbeatPool.shutdownNow();
            }

            if (!processingPool.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Processing pool did not terminate gracefully");
                processingPool.shutdownNow();
            }

            logger.info("RapiscanThreadPoolManager shut down successfully");
        } catch (InterruptedException e) {
            logger.error("Interrupted during shutdown", e);
            messageReaderPool.shutdownNow();
            heartbeatPool.shutdownNow();
            processingPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}