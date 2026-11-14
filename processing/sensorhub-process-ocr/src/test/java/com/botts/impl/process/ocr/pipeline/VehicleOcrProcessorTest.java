/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.pipeline;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.process.ocr.engine.OcrEngine;
import com.botts.impl.process.ocr.engine.OcrRead;
import org.junit.jupiter.api.Test;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VehicleOcrProcessorTest {

    @Test
    void defersUnderHighCpuLoadResolvesObsIdAsynchronouslyAndReusesRetainedJpegs() throws Exception {
        InMemoryBucketStore store = new InMemoryBucketStore();
        AtomicInteger registeredCount = new AtomicInteger();
        IBucketService bucketService = createBucketServiceSpy(store, registeredCount);

        // Seed RetainedMedia committed 5-frame JPEG set in videos bucket
        String obsId = "obs-ret-001";
        String encObs = URLEncoder.encode(obsId, StandardCharsets.UTF_8);
        String dir = "retained-media/v1/events/" + encObs + "/urn%3Aosh%3Asystem%3Alane%3Alane-01/urn%3Aosh%3Asensor%3Affmpeg%3Acam-01";
        store.createBucket("videos");
        store.putBytes("videos", dir + "/manifest.json", "{}".getBytes(StandardCharsets.UTF_8));
        store.putBytes("videos", dir + "/frame-01.jpg", makeJpegBytes(320, 180));
        store.putBytes("videos", dir + "/frame-02.jpg", makeJpegBytes(320, 180));

        AtomicReference<Double> fakeCpu = new AtomicReference<>(0.92); // initially saturated
        OcrResourceGovernor governor = new OcrResourceGovernor(new OcrResourceGovernor.TelemetrySource() {
            @Override public double systemCpuLoad() { return fakeCpu.get(); }
            @Override public double processCpuLoad() { return fakeCpu.get(); }
            @Override public double heapUsedRatio() { return 0.35; }
            @Override public long freeAllocatableBytes() { return 512L * 1024 * 1024; }
        }, 0.85, 0.80, 0.85, 96L * 1024 * 1024);

        OcrEngine stubEngine = image -> List.of(
                new OcrRead("CSQU 305438 3", 0.96, new Rectangle(40, 50, 180, 40))
        );

        AtomicInteger resolveCalls = new AtomicInteger();
        CountDownLatch publishedLatch = new CountDownLatch(1);
        List<VehicleOcrResult> published = new CopyOnWriteArrayList<>();

        try (VehicleOcrProcessor proc = new VehicleOcrProcessor(
                new FrameSampler(),
                store,
                bucketService,
                governor,
                8,
                20L,
                new long[]{30L, 40L},
                new long[]{30L},
                40L)) {

            OcrJob job = new OcrJob(
                    "urn:osh:system:lane:lane-01",
                    null,
                    () -> resolveCalls.incrementAndGet() >= 2 ? obsId : null,
                    1_700_000_000.0,
                    1_700_000_004.0,
                    1_700_000_004.5,
                    List.of(),
                    new OcrSettings(Paths.get("models/ocr"), 2, 0.50, 0.0, 0.0),
                    res -> {
                        published.add(res);
                        publishedLatch.countDown();
                    });

            long t0 = System.nanoTime();
            proc.submit(job, stubEngine, new AtomicBoolean(true));
            long submitMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            assertTrue(submitMs < 100L, "submit() must never block caller/EventBus thread");

            // Allow one CPU deferral, then drop CPU load so the background worker proceeds
            Thread.sleep(90L);
            assertTrue(proc.getJobsDeferred() >= 1, "Expected job to defer while CPU load is 92%");
            fakeCpu.set(0.25);

            assertTrue(publishedLatch.await(5, TimeUnit.SECONDS), "Expected async OCR result after CPU subsided");
            assertEquals(1, published.size());
            VehicleOcrResult r = published.get(0);
            assertEquals(obsId, r.getOccupancyObsId());
            assertEquals(VehicleOcrResult.ID_TYPE_CONTAINER, r.getIdType());
            assertEquals("CSQU3054383", r.getNormalizedValue());
            assertTrue(r.isChecksumValid());
            assertEquals("urn:osh:sensor:ffmpeg:cam-01", r.getCameraUid());
            assertTrue(r.getEvidenceImagePath().startsWith("ocr/crops/"));
            assertTrue(registeredCount.get() >= 1, "Expected IBucketService.ensureBucketRegistered(\"ocr\") call");
        }
    }

    @Test
    void shedsUnderCriticalHeapDropsOldestOnOverflowAndSurvivesEngineErrors() throws Exception {
        AtomicReference<Double> heapRatio = new AtomicReference<>(0.95); // critical heap pressure
        OcrResourceGovernor governor = new OcrResourceGovernor(new OcrResourceGovernor.TelemetrySource() {
            @Override public double systemCpuLoad() { return 0.20; }
            @Override public double processCpuLoad() { return 0.15; }
            @Override public double heapUsedRatio() { return heapRatio.get(); }
            @Override public long freeAllocatableBytes() {
                return heapRatio.get() > 0.90 ? 16L * 1024 * 1024 : 512L * 1024 * 1024;
            }
        }, 0.85, 0.80, 0.85, 96L * 1024 * 1024);

        try (VehicleOcrProcessor proc = new VehicleOcrProcessor(
                new FrameSampler(),
                null,
                null,
                governor,
                2,
                0L,
                new long[]{10L},
                new long[]{10L},
                15L)) {

            OcrEngine throwingEngine = image -> {
                throw new AssertionError("simulated fatal OCR engine fault");
            };

            OcrJob shedJob = new OcrJob(
                    "urn:osh:system:lane:lane-02",
                    "obs-heap",
                    () -> "obs-heap",
                    100.0, 102.0, 102.0,
                    List.of(),
                    new OcrSettings(Paths.get("models/ocr"), 2, 0.50, 0.0, 0.0),
                    r -> {});

            proc.submit(shedJob, throwingEngine, new AtomicBoolean(true));
            long deadline = System.currentTimeMillis() + 3_000L;
            while (System.currentTimeMillis() < deadline && proc.getJobsShedHeap() == 0) {
                Thread.sleep(20L);
            }
            assertEquals(1L, proc.getJobsShedHeap(), "Expected job to be safely shed under sustained critical heap pressure");

            // Now clear heap pressure, block worker briefly, overflow queue, and prove worker survives AssertionError
            heapRatio.set(0.30);
            InMemoryBucketStore store = new InMemoryBucketStore();
            store.createBucket("videos");
            String dir = "retained-media/v1/events/obs-ok/lane/cam";
            store.putBytes("videos", dir + "/manifest.json", "{}".getBytes(StandardCharsets.UTF_8));
            store.putBytes("videos", dir + "/frame-01.jpg", makeJpegBytes(160, 120));

            CountDownLatch blockerStart = new CountDownLatch(1);
            CountDownLatch blockerRelease = new CountDownLatch(1);
            CountDownLatch finalSuccess = new CountDownLatch(1);

            try (VehicleOcrProcessor proc2 = new VehicleOcrProcessor(
                    new FrameSampler(), store, null, governor, 2, 0L, new long[0], new long[0], 10L)) {

                OcrEngine blockingThenThrowing = img -> {
                    blockerStart.countDown();
                    try {
                        blockerRelease.await(3, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                    }
                    throw new AssertionError("boom inside worker");
                };

                OcrJob j1 = new OcrJob("lane", "obs-ok", null, 1, 2, 2, List.of(),
                        new OcrSettings(Paths.get("models/ocr"), 1, 0.5, 0, 0), r -> {});
                proc2.submit(j1, blockingThenThrowing, new AtomicBoolean(true));
                assertTrue(blockerStart.await(2, TimeUnit.SECONDS));

                // Push 4 more jobs into queue of capacity 2 -> drops 2 oldest
                for (int k = 0; k < 4; k++) {
                    proc2.submit(j1, img -> List.of(), new AtomicBoolean(true));
                }
                assertTrue(proc2.getJobsDroppedOverflow() >= 2, "Expected drop-oldest overflow backpressure");

                // Submit one final valid job after releasing the throwing job to prove worker survived AssertionError
                blockerRelease.countDown();
                proc2.submit(
                        new OcrJob("lane", "obs-ok", null, 1, 2, 2, List.of(),
                                new OcrSettings(Paths.get("models/ocr"), 1, 0.5, 0, 0),
                                r -> finalSuccess.countDown()),
                        img -> List.of(new OcrRead("CSQU 305438 3", 0.95, new Rectangle(10, 10, 80, 25))),
                        new AtomicBoolean(true));

                assertTrue(finalSuccess.await(4, TimeUnit.SECONDS),
                        "Worker thread must survive Throwable/AssertionError fault barrier and process subsequent jobs");
            }
        }
    }

    @Test
    void prunesOldestEvidenceCropsBeyondMaxRetainedCeiling() throws Exception {
        InMemoryBucketStore store = new InMemoryBucketStore();
        EvidenceWriter writer = new EvidenceWriter(store, 10);
        BufferedImage img = new BufferedImage(200, 120, BufferedImage.TYPE_3BYTE_BGR);

        for (int i = 0; i < 30; i++) {
            String key = writer.writeCrop(
                    img,
                    new Rectangle(20, 20, 80, 40),
                    Instant.parse("2026-11-01T00:00:00Z").plusSeconds(i * 86_400L));
            assertFalse(key.isBlank());
        }
        writer.pruneOldestIfNeeded();
        assertEquals(10, store.listObjects("ocr").size());
    }

    @Test
    void invokesCleanupCallbackExactlyOnceAcrossCompletionSheddingOverflowAndClose() throws Exception {
        AtomicReference<Double> heapRatio = new AtomicReference<>(0.30);
        OcrResourceGovernor governor = new OcrResourceGovernor(new OcrResourceGovernor.TelemetrySource() {
            @Override public double systemCpuLoad() { return 0.20; }
            @Override public double processCpuLoad() { return 0.15; }
            @Override public double heapUsedRatio() { return heapRatio.get(); }
            @Override public long freeAllocatableBytes() {
                return heapRatio.get() > 0.90 ? 16L * 1024 * 1024 : 512L * 1024 * 1024;
            }
        }, 0.85, 0.80, 0.85, 96L * 1024 * 1024);

        InMemoryBucketStore store = new InMemoryBucketStore();
        store.createBucket("videos");
        String dir = "retained-media/v1/events/obs-clean/lane/cam";
        store.putBytes("videos", dir + "/manifest.json", "{}".getBytes(StandardCharsets.UTF_8));
        store.putBytes("videos", dir + "/frame-01.jpg", makeJpegBytes(160, 120));

        // 1. Normal completion invokes cleanupCallback exactly once
        AtomicInteger normalCleanup = new AtomicInteger();
        CountDownLatch normalDone = new CountDownLatch(1);
        try (VehicleOcrProcessor proc = new VehicleOcrProcessor(
                new FrameSampler(), store, null, governor, 2, 0L, new long[0], new long[0], 10L)) {
            OcrJob normalJob = new OcrJob(
                    "lane", "obs-clean", null, 1, 2, 2, List.of(),
                    new OcrSettings(Paths.get("models/ocr"), 1, 0.5, 0, 0),
                    r -> {},
                    () -> {
                        normalCleanup.incrementAndGet();
                        normalDone.countDown();
                    });
            proc.submit(normalJob,
                    img -> List.of(new OcrRead("EGHU 397269 4", 0.99, new Rectangle(10, 10, 80, 25))),
                    new AtomicBoolean(true));
            assertTrue(normalDone.await(3, TimeUnit.SECONDS));
            assertEquals(1, normalCleanup.get());

            // 2. Critical heap shedding invokes cleanupCallback exactly once
            heapRatio.set(0.95);
            AtomicInteger shedCleanup = new AtomicInteger();
            CountDownLatch shedDone = new CountDownLatch(1);
            OcrJob shedJob = new OcrJob(
                    "lane", "obs-clean", null, 1, 2, 2, List.of(),
                    new OcrSettings(Paths.get("models/ocr"), 1, 0.5, 0, 0),
                    r -> {},
                    () -> {
                        shedCleanup.incrementAndGet();
                        shedDone.countDown();
                    });
            proc.submit(shedJob, img -> List.of(), new AtomicBoolean(true));
            assertTrue(shedDone.await(3, TimeUnit.SECONDS));
            assertEquals(1, shedCleanup.get());

            // 3. Queue overflow eviction + close() invoke every job's cleanupCallback once
            heapRatio.set(0.30);
            CountDownLatch workerBlocked = new CountDownLatch(1);
            CountDownLatch releaseWorker = new CountDownLatch(1);
            AtomicInteger batchCleanups = new AtomicInteger();

            OcrJob blocker = new OcrJob(
                    "lane", "obs-clean", null, 1, 2, 2, List.of(),
                    new OcrSettings(Paths.get("models/ocr"), 1, 0.5, 0, 0),
                    r -> {},
                    batchCleanups::incrementAndGet);
            proc.submit(blocker, img -> {
                workerBlocked.countDown();
                try {
                    releaseWorker.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                return List.of();
            }, new AtomicBoolean(true));
            assertTrue(workerBlocked.await(2, TimeUnit.SECONDS));

            for (int i = 0; i < 5; i++) {
                proc.submit(new OcrJob(
                        "lane", "obs-clean", null, 1, 2, 2, List.of(),
                        new OcrSettings(Paths.get("models/ocr"), 1, 0.5, 0, 0),
                        r -> {},
                        batchCleanups::incrementAndGet),
                        img -> List.of(),
                        new AtomicBoolean(true));
            }
            // Queue capacity is 2, so at least 3 of the 5 queued jobs were evicted and cleaned up immediately
            assertTrue(batchCleanups.get() >= 3);
            proc.close();
            releaseWorker.countDown();
            // After close() + worker exit, all 6 jobs (1 blocker + 5 queued) must have cleaned up once each
            long waitDeadline = System.currentTimeMillis() + 2_000L;
            while (System.currentTimeMillis() < waitDeadline && batchCleanups.get() < 6) {
                Thread.sleep(15L);
            }
            assertEquals(6, batchCleanups.get());
        }
    }

    private static byte[] makeJpegBytes(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, w, h);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private static IBucketService createBucketServiceSpy(IBucketStore store, AtomicInteger registerCount) {
        return (IBucketService) Proxy.newProxyInstance(
                IBucketService.class.getClassLoader(),
                new Class<?>[]{IBucketService.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getBucketStore" -> store;
                        case "ensureBucketRegistered" -> {
                            registerCount.incrementAndGet();
                            yield null;
                        }
                        default -> null;
                    };
                });
    }

    private static final class InMemoryBucketStore implements IBucketStore {
        private final Map<String, Map<String, byte[]>> buckets = new ConcurrentHashMap<>();

        void putBytes(String bucket, String key, byte[] data) {
            buckets.computeIfAbsent(bucket, k -> new ConcurrentHashMap<>()).put(key, data);
        }

        @Override public void createBucket(String bucketName) {
            buckets.computeIfAbsent(bucketName, k -> new ConcurrentHashMap<>());
        }
        @Override public boolean bucketExists(String bucketName) { return buckets.containsKey(bucketName); }
        @Override public void deleteBucket(String bucketName) { buckets.remove(bucketName); }
        @Override public List<String> listBuckets() { return new ArrayList<>(buckets.keySet()); }
        @Override public long getNumBuckets() { return buckets.size(); }
        @Override public boolean objectExists(String bucketName, String key) {
            return buckets.getOrDefault(bucketName, Map.of()).containsKey(key);
        }
        @Override public boolean objectExists(String relativePath) { return false; }
        @Override public String createObject(String bucketName, InputStream data, Map<String, String> metadata) throws DataStoreException { return ""; }
        @Override public void putObject(String bucketName, String key, InputStream data, Map<String, String> metadata) throws DataStoreException {
            try {
                buckets.computeIfAbsent(bucketName, k -> new ConcurrentHashMap<>()).put(key, data.readAllBytes());
            } catch (Exception e) {
                throw new DataStoreException("putObject failed", e);
            }
        }
        @Override public OutputStream putObject(String bucketName, String key, Map<String, String> metadata) { return OutputStream.nullOutputStream(); }
        @Override public OutputStream appendObject(String bucketName, String key, Map<String, String> metadata) { return OutputStream.nullOutputStream(); }
        @Override public InputStream getObject(String bucketName, String key) throws DataStoreException {
            byte[] b = buckets.getOrDefault(bucketName, Map.of()).get(key);
            if (b == null)
                throw new DataStoreException("not found: " + key);
            return new ByteArrayInputStream(b);
        }
        @Override public long getObjectSize(String bucketName, String key) {
            byte[] b = buckets.getOrDefault(bucketName, Map.of()).get(key);
            return b == null ? 0L : b.length;
        }
        @Override public String getObjectMimeType(String bucketName, String key) { return "image/jpeg"; }
        @Override public void deleteObject(String bucketName, String key) {
            buckets.getOrDefault(bucketName, Map.of()).remove(key);
        }
        @Override public List<String> listObjects(String bucketName) {
            return buckets.getOrDefault(bucketName, Map.of()).keySet().stream()
                    .sorted()
                    .toList();
        }
        @Override public List<String> listObjects(String bucketName, String prefix) {
            String p = prefix == null ? "" : prefix;
            return buckets.getOrDefault(bucketName, Map.of()).keySet().stream()
                    .filter(k -> k.startsWith(p))
                    .sorted()
                    .toList();
        }
        @Override public long getNumObjects(String bucketName) {
            return buckets.getOrDefault(bucketName, Map.of()).size();
        }
        @Override public String getResourceURI(String bucketName, String key) { return "file:///tmp/" + bucketName + "/" + key; }
        @Override public String getRelativeResourceURI(String bucketName, String key) { return bucketName + "/" + key; }
    }
}
