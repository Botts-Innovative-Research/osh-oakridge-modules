/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.pipeline;

import com.botts.impl.process.ocr.domain.CandidateAggregator;
import com.botts.impl.process.ocr.engine.OcrEngine;
import com.botts.impl.process.ocr.engine.OcrRead;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TwoTierFrameScannerTest {

    @Test
    void locksInEarlyAfterTwoCleanUnrepairedChecksumFramesWithoutRunningRemainingFramesOr1600Fallback() throws Exception {
        List<String> calls = new ArrayList<>();
        OcrEngine engine = new OcrEngine() {
            @Override
            public List<OcrRead> recognize(BufferedImage frame) {
                return recognize(frame, DEFAULT_DET_MAX_SIDE);
            }

            @Override
            public List<OcrRead> recognize(BufferedImage frame, int detMaxSide) {
                calls.add(detMaxSide + ":" + frame.getWidth());
                return List.of(new OcrRead("CSQU 305438 3", 0.94, new Rectangle(40, 40, 160, 34)));
            }
        };

        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            frames.add(new BufferedImage(100 + i, 80, BufferedImage.TYPE_3BYTE_BGR));
        }

        CandidateAggregator agg = new CandidateAggregator(0.45);
        var inputs = TwoTierFrameScanner.scanFrames(engine, frames, agg);

        // Must break after frames 0 and 1 (readCount == 2, conf == 0.94 >= 0.88, !checkDigitRepaired)
        assertEquals(2, inputs.size());
        assertEquals(List.of("960:100", "960:101"), calls);
    }

    @Test
    void triggersConditional1600FallbackOnlyWhen960FindsZeroChecksumValidContainersAndStopsOnFirstCleanHit() throws Exception {
        List<String> calls = new ArrayList<>();
        OcrEngine engine = new OcrEngine() {
            @Override
            public List<OcrRead> recognize(BufferedImage frame) {
                return recognize(frame, DEFAULT_DET_MAX_SIDE);
            }

            @Override
            public List<OcrRead> recognize(BufferedImage frame, int detMaxSide) {
                int idx = frame.getWidth() - 200;
                calls.add(detMaxSide + ":" + idx);
                if (detMaxSide == HIGH_RES_DET_MAX_SIDE && idx == 2) {
                    return List.of(new OcrRead("YMMU 615485 0", 0.91, new Rectangle(60, 50, 170, 28)));
                }
                return List.of();
            }
        };

        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            frames.add(new BufferedImage(200 + i, 120, BufferedImage.TYPE_3BYTE_BGR));
        }

        CandidateAggregator agg = new CandidateAggregator(0.45);
        var inputs = TwoTierFrameScanner.scanFrames(engine, frames, agg);
        var candidates = agg.aggregate(inputs);

        assertEquals(6, inputs.size(), "Physical frame count must remain 6");
        // 6 calls at 960px + 1 call on center slot 2 at 1600px (early lock-in stops remaining 1600px slots)
        assertEquals(List.of("960:0", "960:1", "960:2", "960:3", "960:4", "960:5", "1600:2"), calls);
        assertEquals(1, candidates.size());
        assertEquals("YMMU6154850", candidates.get(0).normalizedValue());
        assertEquals(2, candidates.get(0).bestFrameIndex());
    }
}
