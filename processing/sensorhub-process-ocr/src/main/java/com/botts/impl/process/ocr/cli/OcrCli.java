/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.cli;

import com.botts.impl.process.ocr.domain.CandidateAggregator;
import com.botts.impl.process.ocr.engine.OcrEngine;
import com.botts.impl.process.ocr.engine.OcrRead;
import com.botts.impl.process.ocr.engine.OnnxOcrEngine;
import com.botts.impl.process.ocr.pipeline.FrameSampler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Standalone diagnostic CLI for evaluating container/plate OCR against an image or MP4 clip.
 */
public final class OcrCli {

    private OcrCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: OcrCli <modelDir> <image-or-mp4-path> [maxFrames]");
            System.exit(2);
        }
        Path modelDir = Paths.get(args[0]);
        Path input = Paths.get(args[1]);
        int maxFrames = args.length >= 3 ? Integer.parseInt(args[2]) : 5;

        if (!Files.isReadable(input)) {
            System.err.println("Input file is not readable: " + input);
            System.exit(1);
        }

        try (OcrEngine engine = OnnxOcrEngine.acquireShared(modelDir)) {
            List<CandidateAggregator.FrameInput> frames = new ArrayList<>();
            String lower = input.getFileName().toString().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov")
                    || lower.endsWith(".ts") || lower.endsWith(".avi") || lower.endsWith(".webm")) {
                FrameSampler sampler = new FrameSampler();
                for (FrameSampler.SampledFrame sf : sampler.sampleFrames(input, maxFrames)) {
                    try {
                        List<OcrRead> reads = engine.recognize(sf.image());
                        frames.add(new CandidateAggregator.FrameInput(
                                sf.index(), sf.image().getWidth(), sf.image().getHeight(), reads));
                    } finally {
                        sf.image().flush();
                    }
                }
            } else {
                BufferedImage img = ImageIO.read(input.toFile());
                if (img != null) {
                    try {
                        List<OcrRead> reads = engine.recognize(img);
                        frames.add(new CandidateAggregator.FrameInput(0, img.getWidth(), img.getHeight(), reads));
                    } finally {
                        img.flush();
                    }
                }
            }

            CandidateAggregator aggregator = new CandidateAggregator(0.45);
            for (CandidateAggregator.AggregatedCandidate c : aggregator.aggregate(frames)) {
                System.out.printf(Locale.ROOT,
                        "%s\t%s\tconf=%.3f\tchecksum=%s\thits=%d\traw=\"%s\"%n",
                        c.idType(), c.normalizedValue(), c.confidence(),
                        c.checksumValid(), c.readCount(), c.rawText());
            }
        }
    }
}
