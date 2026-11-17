/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.pipeline;

import com.botts.impl.process.ocr.domain.CandidateAggregator;
import com.botts.impl.process.ocr.engine.OcrEngine;
import com.botts.impl.process.ocr.engine.OcrRead;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Shared two-tier multi-frame OCR execution schedule used by {@link VehicleOcrProcessor},
 * {@link com.botts.impl.process.ocr.cli.OcrCli}, and offline dataset benchmarks.
 *
 * <ol>
 *   <li><strong>Pass 1 ({@code 960px} default scale + early clean-checksum lock-in):</strong>
 *       Scans sampled frames at {@link OcrEngine#DEFAULT_DET_MAX_SIDE} ({@code 960px}) and stops
 *       early once an optically Modulo-11 verified ({@code !checkDigitRepaired}) container ID is
 *       confirmed across {@code >= 2} frames at {@code confidence >= 0.88} (or on a single frame
 *       at {@code confidence >= 0.85} under elevated load).</li>
 *   <li><strong>Pass 2 ({@code 1600px} conditional high-resolution fallback):</strong>
 *       Only when Pass 1 produces zero {@code checksumValid} container candidates and resource
 *       headroom permits, re-runs {@link OcrEngine#HIGH_RES_DET_MAX_SIDE} ({@code 1600px}) on up
 *       to 3 center/transit frames (stopping immediately once an unrepaired checksum-valid
 *       container is locked in) and merges detections per physical frame index.</li>
 * </ol>
 */
public final class TwoTierFrameScanner {

    public static final double NORMAL_LOCK_IN_CONFIDENCE = 0.88;
    public static final double ELEVATED_LOAD_LOCK_IN_CONFIDENCE = 0.85;
    public static final double HIGH_RES_EARLY_LOCK_CONFIDENCE = 0.85;
    public static final int MAX_HIGH_RES_FALLBACK_FRAMES = 3;

    private TwoTierFrameScanner() {
    }

    public static List<CandidateAggregator.FrameInput> scanFrames(
            OcrEngine engine,
            List<BufferedImage> images,
            CandidateAggregator aggregator
    ) throws InterruptedException {
        return scanFrames(
                engine,
                images,
                aggregator,
                () -> false,
                () -> false,
                () -> true,
                false);
    }

    public static List<CandidateAggregator.FrameInput> scanFrames(
            OcrEngine engine,
            List<BufferedImage> images,
            CandidateAggregator aggregator,
            BooleanSupplier stopRequested,
            BooleanSupplier elevatedLoad,
            BooleanSupplier allowHighResFallback,
            boolean ecoMode
    ) throws InterruptedException {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(aggregator, "aggregator");
        if (images == null || images.isEmpty())
            return List.of();

        int n = images.size();
        List<CandidateAggregator.FrameInput> frameInputs = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            if (stopRequested.getAsBoolean())
                break;

            BufferedImage img = images.get(i);
            if (img == null)
                continue;

            List<OcrRead> reads = engine.recognize(img, OcrEngine.DEFAULT_DET_MAX_SIDE);
            frameInputs.add(new CandidateAggregator.FrameInput(
                    i, img.getWidth(), img.getHeight(), reads));

            if (i < n - 1) {
                boolean busy = ecoMode || elevatedLoad.getAsBoolean();
                if (busy || i >= 1) {
                    List<CandidateAggregator.AggregatedCandidate> interim = aggregator.aggregate(frameInputs);
                    boolean locked = busy
                            ? interim.stream().anyMatch(c ->
                                    VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType())
                                            && c.checksumValid()
                                            && c.confidence() >= ELEVATED_LOAD_LOCK_IN_CONFIDENCE)
                            : interim.stream().anyMatch(c ->
                                    VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType())
                                            && c.checksumValid()
                                            && !c.checkDigitRepaired()
                                            && c.readCount() >= 2
                                            && c.confidence() >= NORMAL_LOCK_IN_CONFIDENCE);
                    if (locked) {
                        break;
                    }
                }
                if (ecoMode) {
                    Thread.sleep(25L);
                }
            }
        }

        if (stopRequested.getAsBoolean() || frameInputs.isEmpty() || !allowHighResFallback.getAsBoolean()) {
            return frameInputs;
        }

        List<CandidateAggregator.AggregatedCandidate> after960 = aggregator.aggregate(frameInputs);
        boolean hasValidContainer = after960.stream().anyMatch(c ->
                VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType()) && c.checksumValid());
        if (hasValidContainer) {
            return frameInputs;
        }

        List<Integer> fallbackSlots = selectHighResFallbackSlots(frameInputs);
        for (int slot : fallbackSlots) {
            if (stopRequested.getAsBoolean())
                break;

            CandidateAggregator.FrameInput base = frameInputs.get(slot);
            int physIndex = base.frameIndex();
            BufferedImage img = images.get(physIndex);
            if (img == null)
                continue;

            List<OcrRead> hiReads = engine.recognize(img, OcrEngine.HIGH_RES_DET_MAX_SIDE);
            if (hiReads.isEmpty())
                continue;

            List<OcrRead> merged = new ArrayList<>(base.reads().size() + hiReads.size());
            merged.addAll(base.reads());
            merged.addAll(hiReads);
            frameInputs.set(slot, new CandidateAggregator.FrameInput(
                    physIndex, base.frameWidth(), base.frameHeight(), merged));

            List<CandidateAggregator.AggregatedCandidate> updated = aggregator.aggregate(frameInputs);
            boolean cleanHighResLock = updated.stream().anyMatch(c ->
                    VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType())
                            && c.checksumValid()
                            && !c.checkDigitRepaired()
                            && c.confidence() >= HIGH_RES_EARLY_LOCK_CONFIDENCE);
            if (cleanHighResLock) {
                break;
            }
        }

        return frameInputs;
    }

    static List<Integer> selectHighResFallbackSlots(List<CandidateAggregator.FrameInput> frameInputs) {
        int m = frameInputs.size();
        if (m == 0)
            return List.of();
        if (m <= 2) {
            List<Integer> all = new ArrayList<>(m);
            for (int i = 0; i < m; i++)
                all.add(i);
            return all;
        }

        Set<Integer> ordered = new LinkedHashSet<>();
        int midLeft = (m / 2) - 1;
        int midRight = m / 2;
        ordered.add(midLeft);
        ordered.add(midRight);

        // Choose the 3rd fallback frame between the late rear-door transit slot (m - 2) and
        // early entry slot (1), preferring whichever had partial 960px text box activity.
        int lateSlot = Math.min(m - 1, midRight + 1);
        int earlySlot = Math.max(0, midLeft - 1);
        int lateReads = frameInputs.get(lateSlot).reads() != null ? frameInputs.get(lateSlot).reads().size() : 0;
        int earlyReads = frameInputs.get(earlySlot).reads() != null ? frameInputs.get(earlySlot).reads().size() : 0;
        if (lateReads >= earlyReads) {
            ordered.add(lateSlot);
            ordered.add(earlySlot);
        } else {
            ordered.add(earlySlot);
            ordered.add(lateSlot);
        }

        List<Integer> result = new ArrayList<>(Math.min(MAX_HIGH_RES_FALLBACK_FRAMES, ordered.size()));
        for (int idx : ordered) {
            if (result.size() >= MAX_HIGH_RES_FALLBACK_FRAMES)
                break;
            result.add(idx);
        }
        return result;
    }
}
