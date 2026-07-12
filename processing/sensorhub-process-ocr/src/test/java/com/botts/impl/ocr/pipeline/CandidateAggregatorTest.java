package com.botts.impl.ocr.pipeline;

import com.botts.impl.ocr.api.OcrRead;
import org.junit.Test;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CandidateAggregatorTest {

    private final BufferedImage frame = new BufferedImage(64, 64, BufferedImage.TYPE_3BYTE_BGR);

    private CandidateAggregator.FrameRead read(String text, float confidence) {
        return read(text, confidence, new Rectangle(0, 0, 10, 10));
    }

    private CandidateAggregator.FrameRead read(String text, float confidence, Rectangle bbox) {
        return new CandidateAggregator.FrameRead(
                new OcrRead(text, confidence, bbox),
                frame, "urn:osh:sensor:ffmpeg:cam1", Instant.now());
    }

    private OcrSettings settings() {
        OcrSettings settings = new OcrSettings();
        settings.containerMinConfidence = 0.5;
        settings.plateMinConfidence = 0.6;
        return settings;
    }

    @Test
    public void votesAcrossFramesByNormalizedValue() {
        List<CandidateAggregator.FrameRead> reads = List.of(
                read("CSQU 305438 3", 0.7f),
                read("csqu3054383", 0.9f),
                read("CSQU3054383", 0.8f));

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings()).aggregate(reads);

        assertEquals(1, candidates.size());
        CandidateAggregator.Candidate candidate = candidates.get(0);
        assertEquals(VehicleOcrResult.ID_TYPE_CONTAINER, candidate.idType);
        assertEquals("CSQU3054383", candidate.normalizedValue);
        assertTrue(candidate.checksumValid);
        assertEquals(3, candidate.readCount);
        assertEquals(0.9f, candidate.confidence, 1e-6);
    }

    @Test
    public void keepsContainerAndPlateSeparately() {
        List<CandidateAggregator.FrameRead> reads = List.of(
                read("CSQU3054383", 0.8f),
                read("XYZ7890", 0.9f));

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings()).aggregate(reads);

        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().anyMatch(c -> VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType)));
        assertTrue(candidates.stream().anyMatch(c -> VehicleOcrResult.ID_TYPE_PLATE.equals(c.idType)));
    }

    @Test
    public void dropsPlateFragmentsOfContainerNumbers() {
        List<CandidateAggregator.FrameRead> reads = List.of(
                read("CSQU3054383", 0.8f),
                // the serial read on its own in another frame is not a plate
                read("3054383", 0.95f));

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings()).aggregate(reads);

        assertEquals(1, candidates.size());
        assertEquals(VehicleOcrResult.ID_TYPE_CONTAINER, candidates.get(0).idType);
    }

    @Test
    public void checksumValidContainerSurvivesLowConfidence() {
        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings())
                .aggregate(List.of(read("CSQU3054383", 0.3f)));
        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).checksumValid);
    }

    @Test
    public void lowConfidenceUnvalidatedReadsAreDropped() {
        List<CandidateAggregator.FrameRead> reads = List.of(
                read("CSQU3054384", 0.3f),   // pattern-only container below floor
                read("AB1234", 0.4f));       // plate below floor

        assertTrue(new CandidateAggregator(settings()).aggregate(reads).isEmpty());
    }

    @Test
    public void sortsChecksumValidFirstThenConfidence() {
        List<CandidateAggregator.FrameRead> reads = List.of(
                read("CSQU3054384", 0.95f),  // pattern-only, above floor
                read("TEMU1234565", 0.6f));  // checksum valid, lower confidence

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings()).aggregate(reads);

        assertEquals(2, candidates.size());
        assertEquals("TEMU1234565", candidates.get(0).normalizedValue);
    }

    @Test
    public void capsCandidatesPerType() {
        List<CandidateAggregator.FrameRead> reads = new ArrayList<>();
        String[] plates = {"AAA1111", "BBB2222", "CCC3333", "DDD4444", "EEE5555"};
        for (int i = 0; i < plates.length; i++)
            reads.add(read(plates[i], 0.7f + i * 0.01f));

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings()).aggregate(reads);
        assertEquals(CandidateAggregator.MAX_CANDIDATES_PER_TYPE, candidates.size());
    }

    @Test
    public void assemblesContainerFromSplitReadsOnSameLine() {
        // real containers box the check digit separately, so the detector often
        // yields "owner+serial" and the digit as two reads on one line
        List<CandidateAggregator.FrameRead> reads = List.of(
                read("CSQU 305438", 0.9f, new Rectangle(10, 50, 200, 30)),
                read("3", 0.8f, new Rectangle(230, 52, 20, 28)));

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings()).aggregate(reads);

        assertEquals(1, candidates.size());
        CandidateAggregator.Candidate candidate = candidates.get(0);
        assertEquals(VehicleOcrResult.ID_TYPE_CONTAINER, candidate.idType);
        assertEquals("CSQU3054383", candidate.normalizedValue);
        assertTrue(candidate.checksumValid);
    }

    @Test
    public void doesNotConcatenateAcrossLines() {
        // same reads but vertically far apart: no line concatenation
        List<CandidateAggregator.FrameRead> reads = List.of(
                read("CSQU 305438", 0.9f, new Rectangle(10, 50, 200, 30)),
                read("3", 0.8f, new Rectangle(10, 300, 20, 28)));

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings()).aggregate(reads);
        assertTrue(candidates.stream().noneMatch(c -> "CSQU3054383".equals(c.normalizedValue)));
    }

    @Test
    public void respectsTypeToggles() {
        OcrSettings noPlates = settings();
        noPlates.readPlates = false;
        assertTrue(new CandidateAggregator(noPlates).aggregate(List.of(read("XYZ7890", 0.9f))).isEmpty());

        OcrSettings noContainers = settings();
        noContainers.readContainerIds = false;
        List<CandidateAggregator.Candidate> candidates =
                new CandidateAggregator(noContainers).aggregate(List.of(read("CSQU3054383", 0.9f)));
        // with container reading off, an 11-char read is not plate-shaped either
        assertTrue(candidates.isEmpty());
    }
}
