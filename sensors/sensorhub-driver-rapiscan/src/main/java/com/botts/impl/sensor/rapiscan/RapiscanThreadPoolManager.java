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

    // Thread pools
    private final ExecutorService messageReaderPool;
    private final ScheduledExecutorService heartbeatPool;
    private final ExecutorService processingPool;

    // Tracking
    private final AtomicInteger activeSensors = new AtomicInteger(0);

    private RapiscanThreadPoolManager() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        // Message Reader Pool: One thread per sensor (blocking I/O)
        // Since these threads block on I/O, we need one per active connection
        messageReaderPool = new ThreadPoolExecutor(
                0,                              // Core pool size (start with 0)
                100,                            // Max pool size (enough for 50 sensors)
                60L, TimeUnit.SECONDS,         // Keep-alive time for idle threads
                new LinkedBlockingQueue<>(),    // Unbounded queue
                new NamedThreadFactory("Rapiscan-MessageReader")
        );

        // Heartbeat Pool: Scheduled executor for periodic tasks
        // Can be small since heartbeats are quick and scheduled
        heartbeatPool = Executors.newScheduledThreadPool(
                Math.min(10, availableProcessors), // Max 10 threads or number of cores
                new NamedThreadFactory("Rapiscan-Heartbeat")
        );

        // Processing Pool: For CPU-intensive tasks (EML processing, etc.)
        processingPool = new ThreadPoolExecutor(
                availableProcessors,                    // Core = number of processors
                availableProcessors * 2,                // Max = 2x processors
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),        // Bounded queue
                new NamedThreadFactory("Rapiscan-Processing"),
                new ThreadPoolExecutor.CallerRunsPolicy() // Backpressure: run in caller thread
        );

        logger.info("RapiscanThreadPoolManager initialized with {} processors", availableProcessors);
        logger.info("Message reader pool: 0-100 threads");
        logger.info("Heartbeat pool: {} threads", Math.min(10, availableProcessors));
        logger.info("Processing pool: {}-{} threads", availableProcessors, availableProcessors * 2);
    }

    /**
     * Get singleton instance
     */
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

    /**
     * Submit a message reading task (blocking I/O)
     */
    public Future<?> submitMessageReader(Runnable task) {
        return messageReaderPool.submit(task);
    }

    /**
     * Schedule a heartbeat task at fixed rate
     */
    public ScheduledFuture<?> scheduleHeartbeat(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return heartbeatPool.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Submit a CPU-intensive processing task
     */
    public Future<?> submitProcessing(Runnable task) {
        return processingPool.submit(task);
    }

    /**
     * Register a sensor (for tracking)
     */
    public void registerSensor() {
        int count = activeSensors.incrementAndGet();
        logger.debug("Registered sensor. Active sensors: {}", count);
    }

    /**
     * Unregister a sensor (for tracking)
     */
    public void unregisterSensor() {
        int count = activeSensors.decrementAndGet();
        logger.debug("Unregistered sensor. Active sensors: {}", count);
    }

    /**
     * Get statistics
     */
    public String getStatistics() {
        ThreadPoolExecutor msgPool = (ThreadPoolExecutor) messageReaderPool;
        ThreadPoolExecutor procPool = (ThreadPoolExecutor) processingPool;

        return String.format(
                "Active Sensors: %d\n" +
                        "Message Reader Pool: active=%d, pool=%d, queue=%d\n" +
                        "Heartbeat Pool: active=%d, pool=%d, queue=%d\n" +
                        "Processing Pool: active=%d, pool=%d, queue=%d",
                activeSensors.get(),
                msgPool.getActiveCount(), msgPool.getPoolSize(), msgPool.getQueue().size(),
                ((ThreadPoolExecutor) heartbeatPool).getActiveCount(),
                ((ThreadPoolExecutor) heartbeatPool).getPoolSize(),
                ((ThreadPoolExecutor) heartbeatPool).getQueue().size(),
                procPool.getActiveCount(), procPool.getPoolSize(), procPool.getQueue().size()
        );
    }

    /**
     * Shutdown all pools (should be called on application shutdown)
     */
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