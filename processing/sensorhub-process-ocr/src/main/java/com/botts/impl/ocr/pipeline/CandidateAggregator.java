package com.botts.impl.ocr.pipeline;

import com.botts.impl.ocr.api.OcrRead;
import com.botts.impl.ocr.text.Iso6346;
import com.botts.impl.ocr.text.PlateHeuristics;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns raw per-frame OCR reads into deduplicated vehicle-ID candidates:
 * classify each read as a container number or plate, vote across frames and
 * cameras by normalized value, keep the best-confidence frame as evidence.
 */
public class CandidateAggregator {

    public static final int MAX_CANDIDATES_PER_TYPE = 3;

    /** One OCR read in the context of the frame it came from. */
    public static class FrameRead {
        public final OcrRead read;
        public final BufferedImage frameImage;
        public final String cameraUid;
        public final Instant frameTime;

        public FrameRead(OcrRead read, BufferedImage frameImage, String cameraUid, Instant frameTime) {
            this.read = read;
            this.frameImage = frameImage;
            this.cameraUid = cameraUid;
            this.frameTime = frameTime;
        }
    }

    /** An aggregated candidate plus the evidence frame/box it was best read from. */
    public static class Candidate {
        public final String idType;
        public String value;
        public String normalizedValue;
        public boolean checksumValid;
        public float confidence;
        public int readCount;
        public String cameraUid;
        public Instant frameTime;
        public BufferedImage evidenceFrame;
        public Rectangle evidenceBox;

        Candidate(String idType) {
            this.idType = idType;
        }
    }

    private final OcrSettings settings;

    public CandidateAggregator(OcrSettings settings) {
        this.settings = settings;
    }

    public List<Candidate> aggregate(List<FrameRead> frameReads) {
        Map<String, Candidate> byKey = new LinkedHashMap<>();

        for (FrameRead frameRead : frameReads) {
            String normalized = Iso6346.normalize(frameRead.read.text);
            if (normalized.isEmpty())
                continue;

            Iso6346.Match container = settings.readContainerIds ? Iso6346.scan(normalized) : null;
            if (container != null)
                merge(byKey, frameRead, VehicleOcrResult.ID_TYPE_CONTAINER,
                        container.value, container.checksumValid);

            if (settings.readPlates && container == null && PlateHeuristics.looksLikePlate(normalized))
                merge(byKey, frameRead, VehicleOcrResult.ID_TYPE_PLATE, normalized, false);
        }

        if (settings.readContainerIds)
            scanLineConcatenations(byKey, frameReads);

        List<Candidate> candidates = new ArrayList<>(byKey.values());
        // a "plate" that is just a fragment of a read container number is OCR noise
        candidates.removeIf(candidate ->
                VehicleOcrResult.ID_TYPE_PLATE.equals(candidate.idType)
                        && candidates.stream().anyMatch(other ->
                        VehicleOcrResult.ID_TYPE_CONTAINER.equals(other.idType)
                                && other.normalizedValue.contains(candidate.normalizedValue)));

        candidates.removeIf(candidate -> {
            double floor = VehicleOcrResult.ID_TYPE_CONTAINER.equals(candidate.idType)
                    ? settings.containerMinConfidence
                    : settings.plateMinConfidence;
            return !candidate.checksumValid && candidate.confidence < floor;
        });

        candidates.sort(Comparator
                .comparing((Candidate c) -> c.checksumValid).reversed()
                .thenComparing(Comparator.comparingDouble((Candidate c) -> c.confidence).reversed()));

        List<Candidate> capped = new ArrayList<>();
        int containers = 0, plates = 0;
        for (Candidate candidate : candidates) {
            if (VehicleOcrResult.ID_TYPE_CONTAINER.equals(candidate.idType)) {
                if (containers++ < MAX_CANDIDATES_PER_TYPE)
                    capped.add(candidate);
            } else {
                if (plates++ < MAX_CANDIDATES_PER_TYPE)
                    capped.add(candidate);
            }
        }
        return capped;
    }

    /**
     * A container number often detects as several boxes (the check digit is
     * printed in its own box on real containers), so also scan the x-ordered
     * concatenation of the reads on each text line of each frame.
     */
    private void scanLineConcatenations(Map<String, Candidate> byKey, List<FrameRead> frameReads) {
        Map<BufferedImage, List<FrameRead>> byFrame = new LinkedHashMap<>();
        for (FrameRead frameRead : frameReads)
            byFrame.computeIfAbsent(frameRead.frameImage, key -> new ArrayList<>()).add(frameRead);

        for (List<FrameRead> frame : byFrame.values()) {
            for (List<FrameRead> line : groupIntoLines(frame)) {
                if (line.size() < 2)
                    continue;
                // fallback only: a read that already validated on its own was
                // counted in the per-read pass above
                boolean alreadyValid = line.stream().anyMatch(read -> {
                    Iso6346.Match match = Iso6346.scan(Iso6346.normalize(read.read.text));
                    return match != null && match.checksumValid;
                });
                if (alreadyValid)
                    continue;
                line.sort(Comparator.comparingInt(read -> read.read.bbox.x));

                String joinedRaw = line.stream().map(read -> read.read.text)
                        .collect(java.util.stream.Collectors.joining(" "));
                Iso6346.Match match = Iso6346.scan(Iso6346.normalize(joinedRaw));
                if (match == null)
                    continue;

                float confidence = (float) line.stream()
                        .mapToDouble(read -> read.read.confidence).average().orElse(0);
                Rectangle union = new Rectangle(line.get(0).read.bbox);
                for (FrameRead read : line)
                    union = union.union(read.read.bbox);

                FrameRead synthetic = new FrameRead(new OcrRead(joinedRaw, confidence, union),
                        line.get(0).frameImage, line.get(0).cameraUid, line.get(0).frameTime);
                merge(byKey, synthetic, VehicleOcrResult.ID_TYPE_CONTAINER, match.value, match.checksumValid);
            }
        }
    }

    private static List<List<FrameRead>> groupIntoLines(List<FrameRead> frame) {
        List<List<FrameRead>> lines = new ArrayList<>();
        for (FrameRead read : frame) {
            List<FrameRead> target = null;
            for (List<FrameRead> line : lines) {
                if (verticallyOverlaps(line.get(0).read.bbox, read.read.bbox)) {
                    target = line;
                    break;
                }
            }
            if (target != null)
                target.add(read);
            else {
                List<FrameRead> line = new ArrayList<>();
                line.add(read);
                lines.add(line);
            }
        }
        return lines;
    }

    private static boolean verticallyOverlaps(Rectangle a, Rectangle b) {
        int overlap = Math.min(a.y + a.height, b.y + b.height) - Math.max(a.y, b.y);
        return overlap >= Math.min(a.height, b.height) * 0.5;
    }

    private void merge(Map<String, Candidate> byKey, FrameRead frameRead,
                       String idType, String normalizedValue, boolean checksumValid) {
        Candidate candidate = byKey.computeIfAbsent(idType + ":" + normalizedValue,
                key -> new Candidate(idType));
        candidate.readCount++;
        candidate.checksumValid |= checksumValid;

        float confidence = frameRead.read.confidence;
        if (candidate.value == null || confidence > candidate.confidence) {
            candidate.value = frameRead.read.text;
            candidate.normalizedValue = normalizedValue;
            candidate.confidence = confidence;
            candidate.cameraUid = frameRead.cameraUid;
            candidate.frameTime = frameRead.frameTime;
            candidate.evidenceFrame = frameRead.frameImage;
            candidate.evidenceBox = frameRead.read.bbox;
        }
    }
}
