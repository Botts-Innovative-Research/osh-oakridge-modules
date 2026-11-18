/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.engine;

import com.botts.impl.process.ocr.domain.CandidateAggregator;
import org.junit.jupiter.api.Test;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OnnxOcrEngineTest {

    public static Path locateBundledModelDir() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        for (Path cur = cwd; cur != null; cur = cur.getParent()) {
            Path candidate = cur.resolve("dist/models/ocr");
            if (Files.isRegularFile(candidate.resolve("det.onnx"))
                    && Files.isRegularFile(candidate.resolve("rec.onnx"))) {
                return candidate;
            }
        }
        fail("Bundled PP-OCRv3 ONNX models not found under dist/models/ocr from " + cwd);
        return null;
    }

    public static BufferedImage renderContainerFrame(int width, int height, String containerLabel) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Corrugated dark container panel background
            g.setColor(new Color(45, 58, 72));
            g.fillRect(0, 0, width, height);

            // High-contrast white container stencil placard
            int placardX = width / 6;
            int placardY = height / 3;
            int placardW = (width * 2) / 3;
            int placardH = Math.max(96, height / 5);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(placardX, placardY, placardW, placardH);

            g.setColor(new Color(18, 22, 26));
            int fontSize = Math.max(42, placardH / 2);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
            g.drawString(containerLabel, placardX + 32, placardY + (placardH * 2) / 3);
        } finally {
            g.dispose();
        }
        return img;
    }

    @Test
    void recognizesSyntheticIso6346ContainerPlacardViaSharedOnnxLease() {
        Path modelDir = locateBundledModelDir();

        try (OcrEngine lease1 = OnnxOcrEngine.acquireShared(modelDir);
             OcrEngine lease2 = OnnxOcrEngine.acquireShared(modelDir)) {

            BufferedImage frame = renderContainerFrame(1280, 720, "CSQU 305438 3");
            List<OcrRead> reads = lease1.recognize(frame);
            assertFalse(reads.isEmpty(), "Expected PP-OCRv3 detector + recognizer to return at least one read");

            CandidateAggregator aggregator = new CandidateAggregator(0.50);
            var candidates = aggregator.aggregate(List.of(
                    new CandidateAggregator.FrameInput(0, frame.getWidth(), frame.getHeight(), reads)
            ));

            assertFalse(candidates.isEmpty(), "Expected aggregated ISO 6346 candidate from reads: " + reads);
            assertEquals(VehicleOcrResult.ID_TYPE_CONTAINER, candidates.get(0).idType());
            assertEquals("CSQU3054383", candidates.get(0).normalizedValue());
            assertTrue(candidates.get(0).checksumValid());

            // Closing lease1 while lease2 is still active must keep lease2 usable
            lease1.close();
            List<OcrRead> reads2 = lease2.recognize(frame);
            assertFalse(reads2.isEmpty());
        }
    }

    @Test
    void recoversSplitDoorStencilAndBoxedCheckDigitAcrossLockingBar() {
        Path modelDir = locateBundledModelDir();
        BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(44, 62, 88));
            g.fillRect(0, 0, 1280, 720);

            // Vertical dark door locking bar separating owner prefix and 6-digit serial + boxed check digit
            g.setColor(new Color(22, 28, 36));
            g.fillRect(545, 80, 26, 560);

            g.setColor(new Color(244, 246, 248));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
            g.drawString("SEGU", 395, 260);
            g.drawString("464776", 595, 260);

            // Taller painted check-digit border around '1'
            g.drawRect(782, 214, 38, 56);
            g.drawString("1", 790, 260);
        } finally {
            g.dispose();
        }

        try (OcrEngine engine = OnnxOcrEngine.acquireShared(modelDir)) {
            List<OcrRead> reads = engine.recognize(img);
            CandidateAggregator aggregator = new CandidateAggregator(0.50);
            var candidates = aggregator.aggregate(List.of(
                    new CandidateAggregator.FrameInput(0, img.getWidth(), img.getHeight(), reads)
            ));
            assertFalse(candidates.isEmpty(), "Expected split door stencil with boxed check digit to resolve: " + reads);
            assertEquals("SEGU4647761", candidates.get(0).normalizedValue());
            assertTrue(candidates.get(0).checksumValid());
            assertFalse(candidates.get(0).checkDigitRepaired());
        }
    }
}
