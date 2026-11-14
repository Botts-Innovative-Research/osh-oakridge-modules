/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.domain;

import com.botts.impl.process.ocr.engine.OcrRead;
import org.junit.jupiter.api.Test;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CandidateAggregatorTest {

    @Test
    void concatenatesSameLineSplitStencilBoxesAndSuppressesPlateFragments() {
        CandidateAggregator aggregator = new CandidateAggregator(0.55);

        List<OcrRead> frameReads = List.of(
                new OcrRead("CSQU", 0.94, new Rectangle(100, 200, 90, 36)),
                new OcrRead("305438", 0.93, new Rectangle(210, 202, 130, 35)),
                new OcrRead("3", 0.91, new Rectangle(355, 201, 28, 36)),
                new OcrRead("CSQU305", 0.72, new Rectangle(100, 200, 180, 36)),
                new OcrRead("TN849B2", 0.89, new Rectangle(420, 520, 120, 34))
        );

        var results = aggregator.aggregate(List.of(
                new CandidateAggregator.FrameInput(0, 1280, 720, frameReads)
        ));

        assertEquals(2, results.size());
        assertEquals(VehicleOcrResult.ID_TYPE_CONTAINER, results.get(0).idType());
        assertEquals("CSQU3054383", results.get(0).normalizedValue());
        assertTrue(results.get(0).checksumValid());

        assertEquals(VehicleOcrResult.ID_TYPE_PLATE, results.get(1).idType());
        assertEquals("TN849B2", results.get(1).normalizedValue());
    }

    @Test
    void suppressesRepeatedStaticEdgeOsdAcrossFrames() {
        CandidateAggregator aggregator = new CandidateAggregator(0.55, 0.05, 0.05);

        OcrRead staticOsd = new OcrRead("CAM2A19", 0.96, new Rectangle(40, 80, 140, 28));
        OcrRead realPlateF0 = new OcrRead("ABC1234", 0.88, new Rectangle(520, 410, 120, 34));
        OcrRead realPlateF1 = new OcrRead("ABC1234", 0.91, new Rectangle(610, 420, 122, 35));

        var results = aggregator.aggregate(List.of(
                new CandidateAggregator.FrameInput(0, 1280, 720, List.of(staticOsd, realPlateF0)),
                new CandidateAggregator.FrameInput(1, 1280, 720, List.of(staticOsd, realPlateF1)),
                new CandidateAggregator.FrameInput(2, 1280, 720, List.of(staticOsd))
        ));

        assertEquals(1, results.size());
        assertEquals("ABC1234", results.get(0).normalizedValue());
        assertEquals(2, results.get(0).readCount());
    }

    @Test
    void suppressesRepairedContainerShadowsCorrugatedFragmentsAndClippedPlateSubstrings() {
        CandidateAggregator aggregator = new CandidateAggregator(0.55);

        var results = aggregator.aggregate(List.of(
                new CandidateAggregator.FrameInput(0, 640, 480, List.of(
                        new OcrRead("EGHU9674700", 0.97, new Rectangle(180, 120, 210, 34)),
                        new OcrRead("SRG195", 0.95, new Rectangle(240, 320, 96, 28))
                )),
                new CandidateAggregator.FrameInput(1, 640, 480, List.of(
                        // Single-frame blurred misread G->C where check digit 0 gets repaired to 3 (ECHU9674703)
                        new OcrRead("ECHU9674700", 0.78, new Rectangle(182, 122, 208, 34)),
                        // Corrugated-wall partial read of EGHU9674700 + clipped tractor plate shadow RG195
                        new OcrRead("U9674101", 0.81, new Rectangle(220, 122, 140, 32)),
                        new OcrRead("RG195", 0.79, new Rectangle(248, 320, 80, 28))
                )),
                new CandidateAggregator.FrameInput(2, 640, 480, List.of(
                        new OcrRead("EGHU9674700", 0.96, new Rectangle(185, 124, 210, 34)),
                        new OcrRead("SRG195", 0.93, new Rectangle(242, 322, 96, 28))
                ))
        ));

        assertEquals(2, results.size());
        assertEquals(VehicleOcrResult.ID_TYPE_CONTAINER, results.get(0).idType());
        assertEquals("EGHU9674700", results.get(0).normalizedValue());
        assertTrue(results.get(0).checksumValid());
        assertFalse(results.get(0).checkDigitRepaired());

        assertEquals(VehicleOcrResult.ID_TYPE_PLATE, results.get(1).idType());
        assertEquals("SRG195", results.get(1).normalizedValue());
    }
}
