package com.botts.impl.ocr.cli;

import com.botts.impl.ocr.api.OcrRead;
import com.botts.impl.ocr.onnx.OnnxOcrEngine;
import com.botts.impl.ocr.pipeline.CandidateAggregator;
import com.botts.impl.ocr.pipeline.Mp4FrameSampler;
import com.botts.impl.ocr.pipeline.OcrSettings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Standalone accuracy-spike / field-debugging tool: runs the OCR pipeline over
 * still images or recorded MP4s and prints raw reads plus aggregated
 * container/plate candidates.
 *
 * Usage: OcrCli &lt;modelDir&gt; &lt;image.jpg|clip.mp4&gt; [more files...]
 */
public class OcrCli {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: OcrCli <modelDir> <image.jpg|clip.mp4> [more files...]");
            System.exit(2);
        }

        OcrSettings settings = new OcrSettings();
        settings.modelDir = args[0];
        // the CLI is for eyeballing accuracy, so don't filter anything out
        settings.containerMinConfidence = 0;
        settings.plateMinConfidence = 0;

        try (OnnxOcrEngine engine = new OnnxOcrEngine()) {
            engine.init(Paths.get(args[0]));

            for (int i = 1; i < args.length; i++) {
                File file = new File(args[i]);
                System.out.println("=== " + file);

                List<CandidateAggregator.FrameRead> frameReads = new ArrayList<>();
                List<BufferedImage> images = new ArrayList<>();

                if (file.getName().toLowerCase(Locale.US).endsWith(".mp4")) {
                    for (Mp4FrameSampler.Frame frame : new Mp4FrameSampler().sample(file.getAbsolutePath(), settings.framesPerVideo))
                        images.add(frame.image);
                } else {
                    BufferedImage image = ImageIO.read(file);
                    if (image == null) {
                        System.err.println("  unreadable image, skipping");
                        continue;
                    }
                    images.add(image);
                }

                long start = System.nanoTime();
                for (BufferedImage image : images) {
                    for (OcrRead read : engine.read(image)) {
                        System.out.printf(Locale.US, "  read: %-24s conf=%.2f  %s%n", '"' + read.text + '"', read.confidence, read.bbox);
                        frameReads.add(new CandidateAggregator.FrameRead(read, image, "cli", Instant.now()));
                    }
                }
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;

                System.out.printf(Locale.US, "  -- %d frame(s) in %d ms --%n", images.size(), elapsedMs);
                for (CandidateAggregator.Candidate candidate : new CandidateAggregator(settings).aggregate(frameReads)) {
                    System.out.printf(Locale.US, "  candidate: %-9s %-14s conf=%.2f reads=%d checksum=%s%n",
                            candidate.idType, candidate.normalizedValue, candidate.confidence,
                            candidate.readCount, candidate.checksumValid ? "VALID" : "-");
                }
            }
        }
    }
}
