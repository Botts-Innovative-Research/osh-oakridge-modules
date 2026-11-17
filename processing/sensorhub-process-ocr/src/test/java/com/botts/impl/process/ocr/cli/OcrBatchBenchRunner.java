/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.cli;

import com.botts.impl.process.ocr.domain.CandidateAggregator;
import com.botts.impl.process.ocr.engine.OcrRead;
import com.botts.impl.process.ocr.engine.OnnxOcrEngine;
import com.botts.impl.process.ocr.pipeline.FrameSampler;
import com.botts.impl.process.ocr.pipeline.TwoTierFrameScanner;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-worker warm-JVM benchmark harness executing the exact production two-tier OCR pipeline
 * ({@link FrameSampler} + {@link TwoTierFrameScanner} + {@link OnnxOcrEngine} +
 * {@link CandidateAggregator}) across a JSONL manifest of MP4 clips.
 */
public final class OcrBatchBenchRunner {

    private static final Pattern STR_FIELD = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");

    private static String extractJsonString(String jsonLine, String key) {
        Matcher m = STR_FIELD.matcher(jsonLine);
        while (m.find()) {
            if (key.equals(m.group(1))) {
                return m.group(2);
            }
        }
        return "";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: OcrBatchBenchRunner <modelDir> <datasetDir> <manifestJsonl> <outJsonl> [maxFrames=6] [workers=4]");
            System.exit(2);
        }
        Path modelDir = Paths.get(args[0]);
        Path datasetDir = Paths.get(args[1]);
        Path manifestPath = Paths.get(args[2]);
        Path outPath = Paths.get(args[3]);
        int maxFrames = args.length >= 5 ? Integer.parseInt(args[4]) : 6;
        int workers = args.length >= 6 ? Integer.parseInt(args[5]) : 4;

        List<String> lines = Files.readAllLines(manifestPath, StandardCharsets.UTF_8)
                .stream().filter(l -> !l.isBlank()).toList();

        java.util.Map<String, String> existingByClipId = new java.util.HashMap<>();
        if (Files.isReadable(outPath)) {
            for (String el : Files.readAllLines(outPath, StandardCharsets.UTF_8)) {
                if (!el.isBlank()) {
                    String cid = extractJsonString(el, "clip_id");
                    if (!cid.isEmpty()) {
                        existingByClipId.put(cid, el);
                    }
                }
            }
        }
        System.out.printf(Locale.ROOT, "[BENCH] Loaded %d clips (%d cached) | maxFrames=%d | workers=%d%n",
                lines.size(), existingByClipId.size(), maxFrames, workers);

        ThreadLocal<OnnxOcrEngine> tlEngine = ThreadLocal.withInitial(() -> new OnnxOcrEngine(modelDir));
        List<OnnxOcrEngine> createdEngines = java.util.Collections.synchronizedList(new ArrayList<>());

        ExecutorService pool = Executors.newFixedThreadPool(workers);
        AtomicInteger doneCount = new AtomicInteger(0);
        long tStart = System.nanoTime();

        List<Callable<String>> tasks = new ArrayList<>(lines.size());
        for (String line : lines) {
            tasks.add(() -> {
                String clipId = extractJsonString(line, "clip_id");
                String cached = existingByClipId.get(clipId);
                if (cached != null) {
                    doneCount.incrementAndGet();
                    return cached;
                }
                OnnxOcrEngine engine = tlEngine.get();
                synchronized (createdEngines) {
                    if (!createdEngines.contains(engine)) {
                        createdEngines.add(engine);
                    }
                }
                String relClip = extractJsonString(line, "clip_path");
                String relCrop = extractJsonString(line, "crop_path");
                String gtCode = extractJsonString(line, "container_number");
                String mode = extractJsonString(line, "transport_mode");

                Path clipFile = datasetDir.resolve(relClip);
                Path cropFile = datasetDir.resolve(relCrop);

                long c0 = System.nanoTime();
                FrameSampler sampler = new FrameSampler();
                CandidateAggregator aggregator = new CandidateAggregator(0.45);
                List<FrameSampler.SampledFrame> sampled = sampler.sampleFrames(clipFile, maxFrames);
                List<BufferedImage> images = new ArrayList<>(sampled.size());
                for (FrameSampler.SampledFrame sf : sampled) {
                    if (sf.image() != null) {
                        images.add(sf.image());
                    }
                }
                List<CandidateAggregator.FrameInput> frameInputs;
                try {
                    frameInputs = TwoTierFrameScanner.scanFrames(engine, images, aggregator);
                } finally {
                    for (BufferedImage img : images) {
                        img.flush();
                    }
                }
                List<CandidateAggregator.AggregatedCandidate> videoCands = aggregator.aggregate(frameInputs);
                double videoMs = (System.nanoTime() - c0) / 1_000_000.0;

                List<CandidateAggregator.AggregatedCandidate> videoContainers = videoCands.stream()
                        .filter(c -> VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType()))
                        .toList();

                String top1Video = videoContainers.isEmpty() ? "" : videoContainers.get(0).normalizedValue();
                double top1Conf = videoContainers.isEmpty() ? 0.0 : videoContainers.get(0).confidence();
                boolean top1Checksum = !videoContainers.isEmpty() && videoContainers.get(0).checksumValid();
                boolean top1Repaired = !videoContainers.isEmpty() && videoContainers.get(0).checkDigitRepaired();
                int top1Hits = videoContainers.isEmpty() ? 0 : videoContainers.get(0).readCount();
                boolean top3Match = videoContainers.stream().anyMatch(c -> gtCode.equals(c.normalizedValue()));

                String top1Crop = "";
                double cropConf = 0.0;
                if (Files.isReadable(cropFile)) {
                    BufferedImage img = ImageIO.read(cropFile.toFile());
                    if (img != null) {
                        try {
                            List<CandidateAggregator.FrameInput> cf =
                                    TwoTierFrameScanner.scanFrames(engine, List.of(img), aggregator);
                            List<CandidateAggregator.AggregatedCandidate> cropCands = aggregator.aggregate(cf).stream()
                                    .filter(c -> VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType()))
                                    .toList();
                            if (!cropCands.isEmpty()) {
                                top1Crop = cropCands.get(0).normalizedValue();
                                cropConf = cropCands.get(0).confidence();
                            }
                        } finally {
                            img.flush();
                        }
                    }
                }

                int cur = doneCount.incrementAndGet();
                if (cur % 50 == 0 || cur == lines.size()) {
                    double elapsedS = (System.nanoTime() - tStart) / 1_000_000_000.0;
                    System.out.printf(Locale.ROOT, "  [OcrCli] %d / %d clips (%.1f clips/s)%n",
                            cur, lines.size(), cur / Math.max(0.1, elapsedS));
                }

                return String.format(Locale.ROOT,
                        "{\"clip_id\":\"%s\",\"transport_mode\":\"%s\",\"gt\":\"%s\","
                                + "\"video_top1\":\"%s\",\"video_top1_match\":%s,\"video_top3_match\":%s,"
                                + "\"video_conf\":%.4f,\"video_checksum_valid\":%s,\"video_repaired\":%s,"
                                + "\"video_hits\":%d,\"sampled_frames\":%d,\"video_ms\":%.2f,"
                                + "\"crop_top1\":\"%s\",\"crop_match\":%s,\"crop_conf\":%.4f}",
                        esc(clipId), esc(mode), esc(gtCode),
                        esc(top1Video), gtCode.equals(top1Video), top3Match,
                        top1Conf, top1Checksum, top1Repaired,
                        top1Hits, frameInputs.size(), videoMs,
                        esc(top1Crop), gtCode.equals(top1Crop), cropConf);
            });
        }

        List<Future<String>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        if (outPath.getParent() != null) {
            Files.createDirectories(outPath.getParent());
        }
        try (BufferedWriter w = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
            for (Future<String> f : futures) {
                w.write(f.get());
                w.newLine();
            }
        }
        for (OnnxOcrEngine e : createdEngines) {
            try { e.close(); } catch (Exception ignored) {}
        }
        System.out.println("[BENCH] Saved results to " + outPath);
    }
}
