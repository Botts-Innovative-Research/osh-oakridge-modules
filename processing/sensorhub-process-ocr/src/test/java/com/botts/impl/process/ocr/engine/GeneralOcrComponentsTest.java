/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.engine;

import com.botts.impl.process.ocr.domain.CandidateAggregator;
import com.botts.impl.process.ocr.pipeline.TwoTierFrameScanner;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GeneralOcrComponentsTest {

    @Test
    void parseqDecoderStopsAtEosAndComputesMeanConfidence() {
        ParseqDecoder decoder = new ParseqDecoder("0123ABC");
        float[][] logits = new float[][]{
                {0f, 0f, 0f, 0f, 6f, 0f, 0f, 0f}, // 'A' (idx 4)
                {0f, 5f, 0f, 0f, 0f, 0f, 0f, 0f}, // '1' (idx 1)
                {0f, 0f, 0f, 0f, 0f, 0f, 0f, 7f}, // <eos> (idx 7 == vocab.length())
                {0f, 0f, 0f, 0f, 0f, 8f, 0f, 0f}  // ignored after <eos>
        };
        CtcGreedyDecoder.Decoded dec = decoder.decode(logits);
        assertEquals("A1", dec.text());
        assertTrue(dec.confidence() > 0.90f);
    }

    @Test
    void orientedPostProcessorDecodesPeaksAndDeskewsTiltedCrop() {
        BufferedImage frame = new BufferedImage(1920, 1080, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = frame.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(880, 500, 160, 48);
        g.dispose();

        OrientedStencilPostProcessor.LetterboxInput lb = OrientedStencilPostProcessor.preprocess(frame);
        assertEquals(3 * 384 * 640, lb.tensor().length);

        float[][][] outMap = new float[8][96][160];
        outMap[0][48][80] = 0.92f;
        outMap[2][48][80] = 0.25f;
        outMap[3][48][80] = 0.25f;
        outMap[4][48][80] = (float) Math.log(54.0);
        outMap[5][48][80] = (float) Math.log(18.0);
        outMap[6][48][80] = (float) Math.sin(Math.toRadians(30.0)); // 2*theta = 30 deg -> theta = 15 deg
        outMap[7][48][80] = (float) Math.cos(Math.toRadians(30.0));

        List<OrientedStencilPostProcessor.OrientedBox> peaks =
                OrientedStencilPostProcessor.decodePeaks(outMap, lb, 1920, 1080, 0.20, 0.20);
        assertEquals(1, peaks.size());
        OrientedStencilPostProcessor.OrientedBox box = peaks.get(0);
        assertEquals(0, box.classId());
        assertEquals(Math.toRadians(15.0), box.thetaRadians(), 1e-3);

        BufferedImage deskewed = OrientedStencilPostProcessor.extractDeskewedCrop(frame, box, 1.1, 1.1);
        assertNotNull(deskewed);
        assertTrue(deskewed.getWidth() >= 16 && deskewed.getHeight() >= 10);
    }

    @Test
    void tierZeroFastRoiLocksInWithoutInvokingSlowDbNetPass() throws InterruptedException {
        AtomicInteger fastCalls = new AtomicInteger(0);
        AtomicInteger slowCalls = new AtomicInteger(0);

        OcrEngine mockFastEngine = new OcrEngine() {
            @Override
            public boolean supportsFastRoiPass() {
                return true;
            }

            @Override
            public List<OcrRead> recognizeFastRoi(BufferedImage frame) {
                int step = fastCalls.getAndIncrement();
                return List.of(
                        new OcrRead("CSQU3054383", 0.96, new Rectangle(240 + step * 18, 120, 180, 40)),
                        new OcrRead("TN849B2", 0.91, new Rectangle(280 + step * 18, 220, 110, 32))
                );
            }

            @Override
            public List<OcrRead> recognize(BufferedImage frame) {
                slowCalls.incrementAndGet();
                return List.of();
            }
        };

        List<BufferedImage> imgs = List.of(
                new BufferedImage(640, 360, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(640, 360, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(640, 360, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(640, 360, BufferedImage.TYPE_3BYTE_BGR)
        );
        var res = TwoTierFrameScanner.scanFrames(mockFastEngine, imgs, new CandidateAggregator(0.45));
        assertEquals(2, fastCalls.get(), "Expected early Tier-0 lock-in after 2 clean frames");
        assertEquals(0, slowCalls.get(), "Expected slow DBNet pass to be skipped completely");
        assertEquals(2, res.size());
    }
}
