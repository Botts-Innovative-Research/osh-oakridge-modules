/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.domain;

import com.botts.impl.process.ocr.engine.OcrRead;
import com.botts.impl.process.ocr.pipeline.OcrSettings;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.awt.Rectangle;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Aggregates raw per-frame OCR detections across cameras and frames into ranked
 * {@link VehicleOcrResult#ID_TYPE_CONTAINER} and {@link VehicleOcrResult#ID_TYPE_PLATE} candidates.
 */
public final class CandidateAggregator {

    public static final int MAX_CANDIDATES_PER_TYPE = 3;
    public static final double SINGLE_FRAME_REPAIRED_MIN_CONFIDENCE = 0.82;
    public static final double SINGLE_FRAME_UNKNOWN_BIC_REPAIRED_MIN_CONFIDENCE = 0.93;
    private static final double STATIC_EDGE_BAND_FRACTION = 0.18;

    public record FrameInput(int frameIndex, int frameWidth, int frameHeight, List<OcrRead> reads) {
        public FrameInput(int frameIndex, List<OcrRead> reads) {
            this(frameIndex, 0, 0, reads);
        }
    }

    public record AggregatedCandidate(
            String idType,
            String normalizedValue,
            String rawText,
            double confidence,
            boolean checksumValid,
            boolean checkDigitRepaired,
            int bestFrameIndex,
            Rectangle bestBoundingBox,
            int readCount,
            double effectiveScore
    ) {}

    private final boolean readContainerIds;
    private final boolean readPlates;
    private final double containerMinConfidence;
    private final double plateMinConfidence;
    private final double ignoreTopFraction;
    private final double ignoreBottomFraction;

    public CandidateAggregator(double minConfidence) {
        this(new OcrSettings(Paths.get("models/ocr"), true, true, 6, minConfidence, minConfidence, 0.0, 0.0));
    }

    public CandidateAggregator(double minConfidence, double ignoreTopFraction, double ignoreBottomFraction) {
        this(new OcrSettings(Paths.get("models/ocr"), true, true, 6, minConfidence, minConfidence,
                ignoreTopFraction, ignoreBottomFraction));
    }

    public CandidateAggregator(OcrSettings settings) {
        this.readContainerIds = settings.readContainerIds();
        this.readPlates = settings.readPlates();
        this.containerMinConfidence = settings.containerMinConfidence();
        this.plateMinConfidence = settings.plateMinConfidence();
        this.ignoreTopFraction = settings.ignoreTopFraction();
        this.ignoreBottomFraction = settings.ignoreBottomFraction();
    }

    public List<AggregatedCandidate> aggregate(List<FrameInput> frames) {
        if (frames == null || frames.isEmpty())
            return List.of();

        Set<String> staticEdgeKeys = findStaticEdgePositions(frames);
        Map<String, Accumulator> byKey = new LinkedHashMap<>();

        for (FrameInput frame : frames) {
            if (frame == null || frame.reads() == null || frame.reads().isEmpty())
                continue;

            List<OcrRead> usable = new ArrayList<>();
            for (OcrRead read : frame.reads()) {
                if (read == null || read.text() == null || read.text().isBlank())
                    continue;
                if (isInIgnoredBand(read, frame.frameHeight()))
                    continue;
                if (isStaticEdgeOsd(read, frame, staticEdgeKeys))
                    continue;
                usable.add(read);
            }
            if (usable.isEmpty())
                continue;

            // 1. Per-box evaluation
            for (OcrRead read : usable) {
                evaluateAndRecord(byKey, frame.frameIndex(), read.text(), read.confidence(), read.boundingBox());
            }

            // 2. Same-line horizontal & 2-line vertical stacked box concatenation for split container stencils
            if (readContainerIds) {
                for (LineRead merged : buildSameLineConcatenations(usable)) {
                    Optional<Iso6346.Match> iso = Iso6346.extractBest(merged.text);
                    if (iso.isPresent()) {
                        Iso6346.Match m = iso.get();
                        recordCandidate(
                                byKey,
                                VehicleOcrResult.ID_TYPE_CONTAINER,
                                m.compact(),
                                merged.text,
                                merged.confidence,
                                m.checksumValid(),
                                m.checkDigitRepaired(),
                                frame.frameIndex(),
                                merged.unionBox);
                    }
                }
                for (LineRead stacked : buildStackedContainerConcatenations(usable)) {
                    Optional<Iso6346.Match> iso = Iso6346.extractBest(stacked.text);
                    if (iso.isPresent()) {
                        Iso6346.Match m = iso.get();
                        if (m.checksumValid() && (!m.checkDigitRepaired() || Iso6346.isKnownBicPrefix(m.compact()))) {
                            recordCandidate(
                                    byKey,
                                    VehicleOcrResult.ID_TYPE_CONTAINER,
                                    m.compact(),
                                    stacked.text,
                                    stacked.confidence,
                                    m.checksumValid(),
                                    m.checkDigitRepaired(),
                                    frame.frameIndex(),
                                    stacked.unionBox);
                        }
                    }
                }
            }
        }

        if (byKey.isEmpty())
            return List.of();

        List<AggregatedCandidate> all = new ArrayList<>(byKey.size());
        for (Accumulator acc : byKey.values()) {
            AggregatedCandidate c = acc.toCandidate();
            double floor = VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType())
                    ? containerMinConfidence
                    : plateMinConfidence;
            if (!c.checksumValid() && c.confidence() < floor)
                continue;
            // Gate single-frame check-digit-repaired candidates (where the 11th Modulo-11 check digit
            // was synthesized from a 10-character prefix rather than optically verified) behind a
            // stricter confidence floor, requiring >= 0.93 when the 4-letter prefix is not a registered BIC code.
            if (VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType())
                    && c.checkDigitRepaired()
                    && c.readCount() == 1) {
                double repairedFloor = Iso6346.isKnownBicPrefix(c.normalizedValue())
                        ? SINGLE_FRAME_REPAIRED_MIN_CONFIDENCE
                        : SINGLE_FRAME_UNKNOWN_BIC_REPAIRED_MIN_CONFIDENCE;
                if (c.confidence() < Math.max(floor, repairedFloor))
                    continue;
            }
            all.add(c);
        }

        // 1. Suppress repaired/unverified container shadows when a stronger direct-valid container
        //    with a near-identical 10-character prefix (<= 2 char edits) is already present.
        List<AggregatedCandidate> snapshotForContainerShadows = List.copyOf(all);
        all.removeIf(c -> VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType())
                && (!c.checksumValid() || c.checkDigitRepaired())
                && snapshotForContainerShadows.stream().anyMatch(other ->
                other != c
                        && VehicleOcrResult.ID_TYPE_CONTAINER.equals(other.idType())
                        && other.checksumValid()
                        && !other.checkDigitRepaired()
                        && other.effectiveScore() > c.effectiveScore()
                        && prefixHammingDistance(other.normalizedValue(), c.normalizedValue(), 10) <= 2));

        // 2. Remove any "plate" that is a substring, corrugated-rib subsequence, or 1-2 char misread
        //    window of a detected container number.
        List<AggregatedCandidate> snapshotForPlateFragments = List.copyOf(all);
        all.removeIf(c -> VehicleOcrResult.ID_TYPE_PLATE.equals(c.idType())
                && snapshotForPlateFragments.stream().anyMatch(other ->
                VehicleOcrResult.ID_TYPE_CONTAINER.equals(other.idType())
                        && isContainerFragment(c.normalizedValue(), other.normalizedValue())));

        // 3. Suppress clipped edge-frame plate shadows (e.g. S2103 alongside TS2103, ET313 alongside JET313).
        List<AggregatedCandidate> snapshotForPlateShadows = List.copyOf(all);
        all.removeIf(c -> VehicleOcrResult.ID_TYPE_PLATE.equals(c.idType())
                && snapshotForPlateShadows.stream().anyMatch(other ->
                other != c
                        && VehicleOcrResult.ID_TYPE_PLATE.equals(other.idType())
                        && !other.normalizedValue().equals(c.normalizedValue())
                        && other.normalizedValue().contains(c.normalizedValue())
                        && other.effectiveScore() >= c.effectiveScore()));

        all.sort(Comparator.comparingDouble(AggregatedCandidate::effectiveScore).reversed());

        List<AggregatedCandidate> capped = new ArrayList<>();
        int containers = 0;
        int plates = 0;
        for (AggregatedCandidate c : all) {
            if (VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType())) {
                if (containers++ < MAX_CANDIDATES_PER_TYPE)
                    capped.add(c);
            } else if (plates++ < MAX_CANDIDATES_PER_TYPE) {
                capped.add(c);
            }
        }
        return capped;
    }

    private void evaluateAndRecord(Map<String, Accumulator> byKey,
                                   int frameIndex,
                                   String rawText,
                                   double confidence,
                                   Rectangle box) {
        if (readContainerIds) {
            Optional<Iso6346.Match> iso = Iso6346.extractBest(rawText);
            if (iso.isPresent()) {
                Iso6346.Match m = iso.get();
                recordCandidate(
                        byKey,
                        VehicleOcrResult.ID_TYPE_CONTAINER,
                        m.compact(),
                        rawText,
                        confidence,
                        m.checksumValid(),
                        m.checkDigitRepaired(),
                        frameIndex,
                        box);
                return;
            }
        }

        if (readPlates) {
            Optional<String> plate = PlateHeuristics.normalizePlate(rawText);
            if (plate.isPresent()) {
                recordCandidate(
                        byKey,
                        VehicleOcrResult.ID_TYPE_PLATE,
                        plate.get(),
                        rawText,
                        confidence,
                        false,
                        false,
                        frameIndex,
                        box);
            }
        }
    }

    private void recordCandidate(Map<String, Accumulator> byKey,
                                 String idType,
                                 String normalized,
                                 String raw,
                                 double confidence,
                                 boolean checksumValid,
                                 boolean checkDigitRepaired,
                                 int frameIndex,
                                 Rectangle box) {
        String key = idType + ":" + normalized;
        Accumulator acc = byKey.computeIfAbsent(key, k -> new Accumulator(idType, normalized));
        acc.observe(raw, confidence, checksumValid, checkDigitRepaired, frameIndex, box);
    }

    private record LineRead(String text, double confidence, Rectangle unionBox) {}

    private List<LineRead> buildSameLineConcatenations(List<OcrRead> reads) {
        if (reads.size() < 2)
            return List.of();
        List<OcrRead> sorted = new ArrayList<>(reads);
        sorted.sort(Comparator.comparingInt(r -> r.boundingBox() != null ? r.boundingBox().x : 0));

        List<LineRead> merged = new ArrayList<>();
        boolean[] usedAsSeed = new boolean[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            if (usedAsSeed[i])
                continue;
            OcrRead first = sorted.get(i);
            if (first.boundingBox() == null)
                continue;

            List<OcrRead> line = new ArrayList<>();
            line.add(first);
            Rectangle union = new Rectangle(first.boundingBox());
            double centerY = first.boundingBox().getCenterY();
            double height = Math.max(1.0, first.boundingBox().getHeight());

            for (int j = i + 1; j < sorted.size(); j++) {
                OcrRead next = sorted.get(j);
                if (next.boundingBox() == null)
                    continue;
                double dy = Math.abs(next.boundingBox().getCenterY() - centerY);
                double nextH = Math.max(1.0, next.boundingBox().getHeight());
                double maxH = Math.max(height, nextH);
                int gapX = next.boundingBox().x - (union.x + union.width);
                if (dy <= maxH * 0.65 && gapX >= -((int) maxH) && gapX <= maxH * 5.0) {
                    line.add(next);
                    union = union.union(next.boundingBox());
                    usedAsSeed[j] = true;
                }
            }

            if (line.size() >= 2) {
                boolean anyStandaloneValid = line.stream().anyMatch(r -> {
                    Optional<Iso6346.Match> m = Iso6346.extractBest(r.text());
                    return m.isPresent() && m.get().checksumValid() && !m.get().checkDigitRepaired();
                });
                if (anyStandaloneValid)
                    continue;

                StringBuilder sb = new StringBuilder();
                double confSum = 0.0;
                for (int k = 0; k < line.size(); k++) {
                    if (k > 0)
                        sb.append(' ');
                    sb.append(line.get(k).text().trim());
                    confSum += line.get(k).confidence();
                }
                merged.add(new LineRead(sb.toString(), confSum / line.size(), union));
            }
        }
        return merged;
    }

    /**
     * Merges vertically stacked 2-line rear-door and corner container stencils where the 4-character
     * owner+category prefix ({@code [A-Z]{3}[UJZR]}) is painted directly above the 6/7-digit serial
     * (optionally followed on the bottom row by a separate 1-digit boxed check-digit box).
     */
    private List<LineRead> buildStackedContainerConcatenations(List<OcrRead> reads) {
        if (reads.size() < 2)
            return List.of();
        List<LineRead> stacked = new ArrayList<>();
        for (int i = 0; i < reads.size(); i++) {
            OcrRead top = reads.get(i);
            Rectangle tb = top.boundingBox();
            if (tb == null || !isStackedOwnerPrefix(top.text()))
                continue;

            for (int j = 0; j < reads.size(); j++) {
                if (i == j)
                    continue;
                OcrRead bottom = reads.get(j);
                Rectangle bb = bottom.boundingBox();
                if (bb == null)
                    continue;

                double maxH = Math.max(1.0, Math.max(tb.height, bb.height));
                double hRatio = bb.height / (double) Math.max(1, tb.height);
                int gapY = bb.y - (tb.y + tb.height);
                boolean verticallyBelow = gapY >= -((int) (maxH * 0.35)) && gapY <= maxH * 1.85
                        && hRatio >= 0.45 && hRatio <= 2.2;
                boolean horizontallyAligned = Math.abs(bb.x - tb.x) <= maxH * 2.5
                        || Math.abs(bb.getCenterX() - tb.getCenterX()) <= maxH * 3.0;
                if (!verticallyBelow || !horizontallyAligned)
                    continue;

                String bNorm = Iso6346.normalize(bottom.text());
                if (bNorm.length() < 6 || bNorm.length() > 8)
                    continue;

                Rectangle union = tb.union(bb);
                String mergedText = top.text().trim() + " " + bottom.text().trim();
                double conf = 0.5 * (top.confidence() + bottom.confidence());

                if (bNorm.length() == 6) {
                    // Check for a separate 1-digit boxed check digit immediately to the right of bottom
                    for (int k = 0; k < reads.size(); k++) {
                        if (k == i || k == j)
                            continue;
                        OcrRead chk = reads.get(k);
                        Rectangle cb = chk.boundingBox();
                        if (cb == null)
                            continue;
                        String cNorm = Iso6346.normalize(chk.text());
                        int gapX = cb.x - (bb.x + bb.width);
                        if (cNorm.length() == 1
                                && Math.abs(cb.getCenterY() - bb.getCenterY()) <= maxH * 0.65
                                && gapX >= -((int) (maxH * 0.5)) && gapX <= maxH * 4.0) {
                            stacked.add(new LineRead(
                                    mergedText + " " + chk.text().trim(),
                                    (top.confidence() + bottom.confidence() + chk.confidence()) / 3.0,
                                    union.union(cb)));
                        }
                    }
                }
                stacked.add(new LineRead(mergedText, conf, union));
            }
        }
        return stacked;
    }

    private static boolean isStackedOwnerPrefix(String raw) {
        String s = Iso6346.normalize(raw);
        if (s.length() != 4)
            return false;
        char cat = s.charAt(3);
        if (cat != 'U' && cat != 'J' && cat != 'Z' && cat != 'R')
            return false;
        int letters = 0;
        for (int i = 0; i < 3; i++) {
            if (Character.isLetter(s.charAt(i)))
                letters++;
        }
        return letters >= 2;
    }

    private boolean isInIgnoredBand(OcrRead read, int frameHeight) {
        if (frameHeight <= 0 || read.boundingBox() == null)
            return false;
        double cy = read.boundingBox().getCenterY() / frameHeight;
        if (ignoreTopFraction > 0 && cy <= ignoreTopFraction)
            return true;
        return ignoreBottomFraction > 0 && cy >= (1.0 - ignoreBottomFraction);
    }

    private Set<String> findStaticEdgePositions(List<FrameInput> frames) {
        if (frames.size() < 2)
            return Set.of();
        Map<String, Set<Integer>> seenFrames = new HashMap<>();
        for (FrameInput f : frames) {
            if (f == null || f.frameHeight() <= 0 || f.frameWidth() <= 0 || f.reads() == null)
                continue;
            for (OcrRead r : f.reads()) {
                if (r == null || r.boundingBox() == null || r.text() == null)
                    continue;
                String edgeKey = edgeKeyFor(r, f);
                if (edgeKey != null) {
                    seenFrames.computeIfAbsent(edgeKey, k -> new HashSet<>()).add(f.frameIndex());
                }
            }
        }
        Set<String> repeated = new HashSet<>();
        for (Map.Entry<String, Set<Integer>> e : seenFrames.entrySet()) {
            // Static edge text appearing in identical quantized slot on >= 3 frames (or all frames when >=2)
            int threshold = Math.max(2, (int) Math.ceil(frames.size() * 0.5));
            if (e.getValue().size() >= threshold) {
                repeated.add(e.getKey());
            }
        }
        return repeated;
    }

    private boolean isStaticEdgeOsd(OcrRead read, FrameInput frame, Set<String> staticEdgeKeys) {
        if (staticEdgeKeys.isEmpty())
            return false;
        String key = edgeKeyFor(read, frame);
        return key != null && staticEdgeKeys.contains(key);
    }

    private static String edgeKeyFor(OcrRead read, FrameInput frame) {
        if (frame.frameWidth() <= 0 || frame.frameHeight() <= 0 || read.boundingBox() == null)
            return null;
        double cy = read.boundingBox().getCenterY();
        boolean inEdge = cy <= frame.frameHeight() * STATIC_EDGE_BAND_FRACTION
                || cy >= frame.frameHeight() * (1.0 - STATIC_EDGE_BAND_FRACTION);
        if (!inEdge)
            return null;

        int qx = (int) Math.round((read.boundingBox().getCenterX() / frame.frameWidth()) * 25.0);
        int qy = (int) Math.round((cy / frame.frameHeight()) * 25.0);
        String normText = read.text().trim().toUpperCase(Locale.ROOT);
        return qx + ":" + qy + ":" + normText;
    }

    private static int prefixHammingDistance(String a, String b, int prefixLen) {
        if (a == null || b == null || a.length() < prefixLen || b.length() < prefixLen)
            return Integer.MAX_VALUE;
        int dist = 0;
        for (int i = 0; i < prefixLen; i++) {
            if (a.charAt(i) != b.charAt(i))
                dist++;
        }
        return dist;
    }

    private static boolean isContainerFragment(String plate, String container) {
        if (plate == null || container == null || plate.isEmpty() || plate.length() > container.length())
            return false;
        if (container.contains(plate))
            return true;
        if (isSubsequence(plate, container))
            return true;
        int maxAllowedWindowDist = plate.length() >= 6 ? 2 : 1;
        return minWindowHammingDistance(plate, container) <= maxAllowedWindowDist;
    }

    private static boolean isSubsequence(String sub, String target) {
        int si = 0;
        for (int ti = 0; ti < target.length() && si < sub.length(); ti++) {
            if (sub.charAt(si) == target.charAt(ti))
                si++;
        }
        return si == sub.length();
    }

    private static int minWindowHammingDistance(String needle, String haystack) {
        if (needle.length() > haystack.length())
            return Integer.MAX_VALUE;
        int best = Integer.MAX_VALUE;
        int maxStart = haystack.length() - needle.length();
        for (int start = 0; start <= maxStart; start++) {
            int dist = 0;
            for (int i = 0; i < needle.length(); i++) {
                if (needle.charAt(i) != haystack.charAt(start + i)) {
                    dist++;
                    if (dist >= best)
                        break;
                }
            }
            if (dist < best)
                best = dist;
        }
        return best;
    }

    private static final class Accumulator {
        private final String idType;
        private final String normalizedValue;
        private String bestRawText = "";
        private double bestConfidence = -1.0;
        private boolean checksumValid = false;
        private boolean checkDigitRepaired = true;
        private int bestFrameIndex = 0;
        private Rectangle bestBox = null;
        private final Set<Integer> framesSeen = new HashSet<>();

        Accumulator(String idType, String normalizedValue) {
            this.idType = idType;
            this.normalizedValue = normalizedValue;
        }

        void observe(String raw,
                     double conf,
                     boolean validChecksum,
                     boolean repaired,
                     int frameIndex,
                     Rectangle box) {
            boolean first = framesSeen.isEmpty();
            framesSeen.add(frameIndex);
            this.checksumValid |= validChecksum;
            this.checkDigitRepaired = first ? repaired : (this.checkDigitRepaired && repaired);
            if (conf > bestConfidence) {
                this.bestConfidence = conf;
                this.bestRawText = raw;
                this.bestFrameIndex = frameIndex;
                this.bestBox = box == null ? null : new Rectangle(box);
            }
        }

        AggregatedCandidate toCandidate() {
            int hits = framesSeen.size();
            double bonus = 0.0;
            if (VehicleOcrResult.ID_TYPE_CONTAINER.equals(idType)) {
                // Weighted checksum bonus (+0.22 for direct Modulo-11 valid, +0.18 for multi-frame
                // repaired check digit, +0.08 for single-frame repaired check digit) so optically
                // checksum-verified stencils outrank synthesized single-frame prefixes.
                if (checksumValid) {
                    if (!checkDigitRepaired) {
                        bonus += 0.22;
                    } else {
                        bonus += (hits >= 2) ? 0.18 : 0.08;
                    }
                } else {
                    bonus -= 0.10;
                }
            }
            bonus += Math.min(0.15, 0.05 * Math.max(0, hits - 1));
            double effective = bestConfidence + bonus;
            return new AggregatedCandidate(
                    idType,
                    normalizedValue,
                    bestRawText,
                    bestConfidence,
                    checksumValid,
                    checksumValid && checkDigitRepaired,
                    bestFrameIndex,
                    bestBox,
                    hits,
                    effective);
        }
    }
}
