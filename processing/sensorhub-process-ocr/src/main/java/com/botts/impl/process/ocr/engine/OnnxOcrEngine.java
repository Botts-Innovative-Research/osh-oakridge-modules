/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.engine;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PP-OCRv3 CPU ONNX Runtime scene-text engine (DBNet detector + SVTR-LCNet CTC recognizer).
 *
 * <p>Hardened for 50-lane continuous operation:</p>
 * <ul>
 *   <li>Bounded native threads ({@code intraOp=2}, {@code interOp=1}).</li>
 *   <li>{@code setMemoryPatternOptimization(false)} and {@code setCPUArenaAllocator(false)} so
 *       variable-width text crops release off-heap native tensor memory immediately after each
 *       {@code session.run()} instead of growing an unbounded native arena.</li>
 *   <li>Process-wide reference-counted session sharing via {@link #acquireShared(Path)} so
 *       multiple OCR-enabled lanes share one native model allocation rather than duplicating
 *       {@code det.onnx}/{@code rec.onnx} in native RAM.</li>
 * </ul>
 */
public class OnnxOcrEngine implements OcrEngine {

    public static final String DET_MODEL = "det.onnx";
    public static final String REC_MODEL = "rec.onnx";
    public static final String DICT_FILE = "dict.txt";

    private static final Path CONTAINER_MODEL_FALLBACK = Paths.get("/opt/oscar/models/ocr");
    private static final Path DEV_MODEL_FALLBACK = Paths.get("dist/models/ocr");

    static final int DET_MAX_SIDE = OcrEngine.DEFAULT_DET_MAX_SIDE;
    static final int MIN_DET_MAX_SIDE = 320;
    static final int MAX_DET_MAX_SIDE = 2560;
    static final long MAX_FRAME_PIXELS = 4096L * 4096L;
    static final int MAX_BOXES_PER_FRAME = 64;
    static final float[] DET_MEAN = {0.485f, 0.456f, 0.406f};
    static final float[] DET_STD = {0.229f, 0.224f, 0.225f};
    static final int REC_DEFAULT_HEIGHT = 48;
    static final int REC_MIN_WIDTH = 16;
    static final int REC_MAX_WIDTH = 1920;

    private static final Logger logger = LoggerFactory.getLogger(OnnxOcrEngine.class);

    private static final Object SHARED_LOCK = new Object();
    private static final Map<Path, SharedEntry> SHARED_ENGINES = new HashMap<>();

    private static final class SharedEntry {
        final OnnxOcrEngine engine;
        int refCount;

        SharedEntry(OnnxOcrEngine engine) {
            this.engine = engine;
            this.refCount = 1;
        }
    }

    private final int intraOpThreads;
    private final Object inferenceLock = new Object();

    private OrtEnvironment env;
    private OrtSession detSession;
    private OrtSession recSession;
    private String detInputName;
    private String recInputName;
    private CtcGreedyDecoder decoder;
    private final DbNetPostProcessor postProcessor = new DbNetPostProcessor();
    private int recHeight = REC_DEFAULT_HEIGHT;
    private boolean closed = false;

    public OnnxOcrEngine() {
        this(2);
    }

    public OnnxOcrEngine(Path modelDir) {
        this(2);
        init(modelDir);
    }

    public OnnxOcrEngine(int intraOpThreads) {
        this.intraOpThreads = Math.max(1, Math.min(4, intraOpThreads));
    }

    /**
     * Resolves {@code requestedDir} against packaged fallbacks ({@code /opt/oscar/models/ocr},
     * {@code dist/models/ocr}) so existing deployments with persistent state volumes locate
     * bundled models automatically.
     */
    public static Path resolveModelDirectory(Path requestedDir) {
        List<Path> candidates = new ArrayList<>(3);
        if (requestedDir != null)
            candidates.add(requestedDir);
        candidates.add(CONTAINER_MODEL_FALLBACK);
        candidates.add(DEV_MODEL_FALLBACK);

        for (Path candidate : candidates) {
            if (hasCompleteModelFiles(candidate))
                return candidate.toAbsolutePath().normalize();
        }
        return requestedDir != null
                ? requestedDir.toAbsolutePath().normalize()
                : CONTAINER_MODEL_FALLBACK;
    }

    public static boolean hasCompleteModelFiles(Path dir) {
        if (dir == null)
            return false;
        return Files.isReadable(dir.resolve(DET_MODEL))
                && Files.isReadable(dir.resolve(REC_MODEL))
                && Files.isReadable(dir.resolve(DICT_FILE));
    }

    /**
     * Acquires a reference-counted shared {@link OcrEngine} for {@code modelDir}. Closing the
     * returned handle decrements the reference count and closes native ONNX sessions only when
     * the last lane releases it.
     */
    public static OcrEngine acquireShared(Path modelDir) {
        Path resolved = resolveModelDirectory(modelDir);
        synchronized (SHARED_LOCK) {
            SharedEntry existing = SHARED_ENGINES.get(resolved);
            if (existing != null) {
                existing.refCount++;
                return new LeaseHandle(resolved, existing.engine);
            }
            OnnxOcrEngine created = new OnnxOcrEngine(2);
            created.init(resolved);
            SHARED_ENGINES.put(resolved, new SharedEntry(created));
            return new LeaseHandle(resolved, created);
        }
    }

    private static void releaseShared(Path resolvedKey) {
        OnnxOcrEngine toClose = null;
        synchronized (SHARED_LOCK) {
            SharedEntry entry = SHARED_ENGINES.get(resolvedKey);
            if (entry == null)
                return;
            entry.refCount--;
            if (entry.refCount <= 0) {
                SHARED_ENGINES.remove(resolvedKey);
                toClose = entry.engine;
            }
        }
        if (toClose != null) {
            toClose.close();
        }
    }

    private static final class LeaseHandle implements OcrEngine {
        private final Path key;
        private final OnnxOcrEngine delegate;
        private boolean released = false;

        LeaseHandle(Path key, OnnxOcrEngine delegate) {
            this.key = key;
            this.delegate = delegate;
        }

        @Override
        public List<OcrRead> recognize(BufferedImage frame) {
            return delegate.recognize(frame);
        }

        @Override
        public List<OcrRead> recognize(BufferedImage frame, int detMaxSide) {
            return delegate.recognize(frame, detMaxSide);
        }

        @Override
        public synchronized void close() {
            if (released)
                return;
            released = true;
            releaseShared(key);
        }
    }

    public void init(Path requestedDir) {
        Path modelDir = resolveModelDirectory(requestedDir);
        Path detPath = modelDir.resolve(DET_MODEL);
        Path recPath = modelDir.resolve(REC_MODEL);
        Path dictPath = modelDir.resolve(DICT_FILE);
        for (Path p : List.of(detPath, recPath, dictPath)) {
            if (!Files.isReadable(p))
                throw new OcrEngineException("OCR model file not found or unreadable: " + p);
        }

        synchronized (inferenceLock) {
            try {
                env = OrtEnvironment.getEnvironment();
                try (OrtSession.SessionOptions options = createBoundedSessionOptions()) {
                    detSession = env.createSession(detPath.toString(), options);
                }
                try (OrtSession.SessionOptions options = createBoundedSessionOptions()) {
                    recSession = env.createSession(recPath.toString(), options);
                }

                detInputName = detSession.getInputNames().iterator().next();
                recInputName = recSession.getInputNames().iterator().next();
                recHeight = queryRecInputHeight(recSession, recInputName);

                List<String> charset = Files.readAllLines(dictPath, StandardCharsets.UTF_8);
                decoder = new CtcGreedyDecoder(charset);
                closed = false;

                logger.info("OCR ONNX engine initialized from {} (recHeight={}, intraOpThreads={})",
                        modelDir, recHeight, intraOpThreads);
            } catch (OrtException | IOException e) {
                close();
                throw new OcrEngineException("Failed to initialize ONNX OCR engine from " + modelDir, e);
            }
        }
    }

    private OrtSession.SessionOptions createBoundedSessionOptions() throws OrtException {
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        options.setIntraOpNumThreads(intraOpThreads);
        options.setInterOpNumThreads(1);
        options.setMemoryPatternOptimization(false);
        options.setCPUArenaAllocator(false);
        return options;
    }

    private int queryRecInputHeight(OrtSession session, String inputName) {
        try {
            var info = session.getInputInfo().get(inputName);
            if (info != null && info.getInfo() instanceof TensorInfo tensorInfo) {
                long[] shape = tensorInfo.getShape();
                if (shape.length == 4 && shape[2] > 0)
                    return (int) shape[2];
            }
        } catch (OrtException e) {
            logger.debug("Could not query rec model input shape, using default height", e);
        }
        return REC_DEFAULT_HEIGHT;
    }

    @Override
    public List<OcrRead> recognize(BufferedImage image) {
        return recognize(image, DET_MAX_SIDE);
    }

    @Override
    public List<OcrRead> recognize(BufferedImage image, int detMaxSide) {
        if (image == null)
            return List.of();
        int w = image.getWidth();
        int h = image.getHeight();
        if (w <= 1 || h <= 1 || (long) w * h > MAX_FRAME_PIXELS)
            throw new OcrEngineException("Invalid frame dimensions for OCR: " + w + "x" + h);

        int effectiveMaxSide = Math.max(MIN_DET_MAX_SIDE, Math.min(MAX_DET_MAX_SIDE, detMaxSide));

        synchronized (inferenceLock) {
            if (closed || detSession == null || recSession == null)
                throw new OcrEngineException("OCR engine is not initialized or already closed");

            List<Rectangle> boxes = detect(image, effectiveMaxSide);
            if (boxes.isEmpty())
                return List.of();

            int limit = Math.min(boxes.size(), MAX_BOXES_PER_FRAME);
            List<OcrRead> reads = new ArrayList<>(limit + 4);
            for (int i = 0; i < limit; i++) {
                Rectangle box = boxes.get(i);
                if (box.width <= 1 || box.height <= 1)
                    continue;
                BufferedImage crop = image.getSubimage(box.x, box.y, box.width, box.height);
                CtcGreedyDecoder.Decoded decoded = recognizeCrop(crop);
                if (decoded.text() != null && !decoded.text().isBlank()) {
                    reads.add(new OcrRead(decoded.text(), decoded.confidence(), box));
                }
            }
            recoverSplitAndBoxedCheckDigits(image, reads);
            return reads;
        }
    }

    /**
     * Recovers optically verified 11-character ISO 6346 reads when DBNet splits a stencil across a
     * vertical rear-door locking bar or drops a single-character boxed Modulo-11 check digit:
     * <ul>
     *   <li>Single-box 10-character prefixes ({@code [A-Z]{3}[UJZR]\d{6}}) are re-read with {@code ±15%}
     *       vertical margin and {@code 1.38x}/{@code 1.54x} rightward span to capture taller painted
     *       check-digit borders ({@code [0]}) across locking bars.</li>
     *   <li>Split {@code 4 + 6} door/corrugation stencils (owner prefix box left of or stacked above a
     *       6-digit serial box, or an isolated 6/7-digit serial box whose narrow 4-letter prefix across
     *       the locking bar fell below DBNet's blob threshold) run a targeted span/rightward re-read,
     *       committing strictly when {@link com.botts.impl.process.ocr.domain.Iso6346} confirms an
     *       unrepaired optical Modulo-11 check digit.</li>
     * </ul>
     */
    private void recoverSplitAndBoxedCheckDigits(BufferedImage image, List<OcrRead> reads) {
        int baseCount = reads.size();
        if (baseCount == 0)
            return;
        int imgW = image.getWidth();
        int imgH = image.getHeight();

        for (int i = 0; i < baseCount; i++) {
            OcrRead cur = reads.get(i);
            Rectangle box = cur.boundingBox();
            if (box == null || box.width <= 1 || box.height <= 1)
                continue;

            // 1. Single-box 10-char ISO prefix whose boxed 11th check digit was missed or clipped:
            //    try tight (0% y-pad, 1.38x) first so adjacent 22G1/45G1 stencils below are never grazed,
            //    falling back to ±12% y-pad / 1.52x across wider rear-door locking bars.
            if (box.width > box.height * 2 && box.x + box.width < imgW - 4) {
                var match = com.botts.impl.process.ocr.domain.Iso6346.extractBest(cur.text());
                if (match.isPresent() && match.get().checkDigitRepaired()) {
                    Rectangle[] candidates = new Rectangle[]{
                            new Rectangle(box.x, box.y,
                                    Math.min(imgW - box.x, Math.max(box.width + 8, (int) Math.round(box.width * 1.38))),
                                    box.height),
                            paddedSpanBox(box.x, box.y,
                                    Math.max(box.width + 8, (int) Math.round(box.width * 1.50)), box.height, imgW, imgH)
                    };
                    for (Rectangle extBox : candidates) {
                        if (extBox.width <= box.width + 4)
                            continue;
                        OcrRead upgraded = tryOpticalIsoCrop(image, extBox, cur.confidence());
                        if (upgraded != null) {
                            reads.add(upgraded);
                            break;
                        }
                    }
                    continue;
                }
            }

            // 2. Split 4 + 6 door/corrugation stencil where an explicit 4-char owner prefix box sits
            //    to the left of or directly above a 6-digit serial box.
            String norm = com.botts.impl.process.ocr.domain.Iso6346.normalize(cur.text());
            if (norm.length() == 6 && isAllDigits(norm) && box.width >= box.height * 1.4 && box.x + box.width < imgW - 4) {
                OcrRead leftOwner = findSameLineLeftOwner(reads, baseCount, box);
                if (leftOwner != null) {
                    Rectangle ob = leftOwner.boundingBox();
                    int spanRight = Math.min(imgW, box.x + (int) Math.round(box.width * 1.50));
                    int topY = Math.min(ob.y, box.y);
                    int botY = Math.max(ob.y + ob.height, box.y + box.height);
                    double minBaseConf = Math.min(leftOwner.confidence(), cur.confidence());
                    OcrRead fullHit = tryOpticalIsoCrop(image,
                            new Rectangle(ob.x, topY, spanRight - ob.x, botY - topY), minBaseConf);
                    if (fullHit == null) {
                        fullHit = tryOpticalIsoCrop(image,
                                paddedSpanBox(ob.x, topY, spanRight - ob.x, botY - topY, imgW, imgH), minBaseConf);
                    }
                    if (fullHit != null) {
                        reads.add(fullHit);
                        continue;
                    }
                    OcrRead extSerial = trySerialRightwardExtension(image, leftOwner.text(), box, cur.confidence(), imgW, imgH);
                    if (extSerial != null) {
                        reads.set(i, extSerial);
                    }
                    continue;
                }

                OcrRead topOwner = findStackedTopOwner(reads, baseCount, box);
                if (topOwner != null) {
                    OcrRead extSerial = trySerialRightwardExtension(image, topOwner.text(), box, cur.confidence(), imgW, imgH);
                    if (extSerial != null) {
                        reads.set(i, extSerial);
                    }
                }
            }
        }
    }

    private OcrRead tryOpticalIsoCrop(BufferedImage image, Rectangle extBox, double baseConf) {
        if (extBox.width <= 4 || extBox.height <= 4)
            return null;
        CtcGreedyDecoder.Decoded dec = recognizeCrop(image.getSubimage(extBox.x, extBox.y, extBox.width, extBox.height));
        if (dec.text() == null || dec.text().isBlank())
            return null;
        var m = com.botts.impl.process.ocr.domain.Iso6346.extractBest(dec.text());
        if (m.isPresent() && m.get().checksumValid() && !m.get().checkDigitRepaired()) {
            return new OcrRead(dec.text(), Math.max(dec.confidence(), baseConf * 0.96), extBox);
        }
        return null;
    }

    private OcrRead trySerialRightwardExtension(BufferedImage image,
                                                String ownerText,
                                                Rectangle serialBox,
                                                double baseConf,
                                                int imgW,
                                                int imgH) {
        Rectangle[] candidates = new Rectangle[]{
                new Rectangle(serialBox.x, serialBox.y,
                        Math.min(imgW - serialBox.x, Math.max(serialBox.width + 8, (int) Math.round(serialBox.width * 1.46))),
                        serialBox.height),
                paddedSpanBox(serialBox.x, serialBox.y,
                        Math.max(serialBox.width + 8, (int) Math.round(serialBox.width * 1.58)), serialBox.height, imgW, imgH)
        };
        for (Rectangle extBox : candidates) {
            if (extBox.width <= serialBox.width + 4)
                continue;
            CtcGreedyDecoder.Decoded dec = recognizeCrop(image.getSubimage(extBox.x, extBox.y, extBox.width, extBox.height));
            if (dec.text() == null || dec.text().isBlank())
                continue;
            var m = com.botts.impl.process.ocr.domain.Iso6346.extractBest(ownerText + " " + dec.text());
            if (m.isPresent() && m.get().checksumValid() && !m.get().checkDigitRepaired()) {
                return new OcrRead(dec.text(), Math.max(dec.confidence(), baseConf * 0.96), extBox);
            }
        }
        return null;
    }

    private static Rectangle paddedSpanBox(int x, int y, int width, int height, int imgW, int imgH) {
        int padY = Math.max(2, (int) Math.round(height * 0.12));
        int nx = Math.max(0, x);
        int ny = Math.max(0, y - padY);
        int nw = Math.min(imgW - nx, width);
        int nh = Math.min(imgH - ny, height + 2 * padY);
        return new Rectangle(nx, ny, Math.max(1, nw), Math.max(1, nh));
    }

    private static OcrRead findSameLineLeftOwner(List<OcrRead> reads, int count, Rectangle serialBox) {
        OcrRead best = null;
        int bestGap = Integer.MAX_VALUE;
        for (int j = 0; j < count; j++) {
            OcrRead cand = reads.get(j);
            Rectangle cb = cand.boundingBox();
            if (cb == null || !isOwnerPrefixToken(cand.text()))
                continue;
            double maxH = Math.max(cb.height, serialBox.height);
            int gapX = serialBox.x - (cb.x + cb.width);
            if (Math.abs(cb.getCenterY() - serialBox.getCenterY()) <= maxH * 0.65
                    && gapX >= -((int) (maxH * 0.5)) && gapX <= maxH * 4.5 && gapX < bestGap) {
                best = cand;
                bestGap = gapX;
            }
        }
        return best;
    }

    private static OcrRead findStackedTopOwner(List<OcrRead> reads, int count, Rectangle serialBox) {
        for (int j = 0; j < count; j++) {
            OcrRead cand = reads.get(j);
            Rectangle cb = cand.boundingBox();
            if (cb == null || !isOwnerPrefixToken(cand.text()))
                continue;
            double maxH = Math.max(cb.height, serialBox.height);
            int gapY = serialBox.y - (cb.y + cb.height);
            if (gapY >= -((int) (maxH * 0.35)) && gapY <= maxH * 1.85
                    && (Math.abs(serialBox.x - cb.x) <= maxH * 2.5
                    || Math.abs(serialBox.getCenterX() - cb.getCenterX()) <= maxH * 3.0)) {
                return cand;
            }
        }
        return null;
    }

    private static boolean isOwnerPrefixToken(String raw) {
        String s = com.botts.impl.process.ocr.domain.Iso6346.normalize(raw);
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

    private static boolean isAllDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i)))
                return false;
        }
        return !s.isEmpty();
    }

    private List<Rectangle> detect(BufferedImage image, int detMaxSide) {
        int ow = image.getWidth();
        int oh = image.getHeight();
        double ratio = Math.min(1.0, detMaxSide / (double) Math.max(ow, oh));
        int rw = roundToMultipleOf32(ow * ratio);
        int rh = roundToMultipleOf32(oh * ratio);

        BufferedImage resized = resize(image, rw, rh);
        try {
            float[] tensor = toNchw(resized, DET_MEAN, DET_STD);
            try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(tensor), new long[]{1, 3, rh, rw});
                 OrtSession.Result result = detSession.run(Collections.singletonMap(detInputName, input))) {
                float[][][][] output = (float[][][][]) result.get(0).getValue();
                float[][] probMap = output[0][0];
                double scaleX = ow / (double) probMap[0].length;
                double scaleY = oh / (double) probMap.length;
                return postProcessor.extractBoxes(probMap, scaleX, scaleY, ow, oh);
            } catch (OrtException e) {
                throw new OcrEngineException("Text detection failed", e);
            }
        } finally {
            if (resized != image)
                resized.flush();
        }
    }

    private CtcGreedyDecoder.Decoded recognizeCrop(BufferedImage crop) {
        int width = (int) Math.round(crop.getWidth() * (recHeight / (double) Math.max(1, crop.getHeight())));
        width = Math.max(REC_MIN_WIDTH, Math.min(REC_MAX_WIDTH, width));

        BufferedImage resized = resize(crop, width, recHeight);
        try {
            float[] tensor = toNchwCentered(resized);
            try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(tensor), new long[]{1, 3, recHeight, width});
                 OrtSession.Result result = recSession.run(Collections.singletonMap(recInputName, input))) {
                float[][][] output = (float[][][]) result.get(0).getValue();
                return decoder.decode(output[0]);
            } catch (OrtException e) {
                throw new OcrEngineException("Text recognition failed", e);
            }
        } finally {
            if (resized != crop)
                resized.flush();
        }
    }

    private static int roundToMultipleOf32(double v) {
        return Math.max(32, (int) Math.round(v / 32.0) * 32);
    }

    static BufferedImage resize(BufferedImage src, int width, int height) {
        if (src.getWidth() == width && src.getHeight() == height && src.getType() == BufferedImage.TYPE_INT_RGB)
            return src;
        BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    static float[] toNchw(BufferedImage image, float[] mean, float[] std) {
        int w = image.getWidth();
        int h = image.getHeight();
        float[] tensor = new float[3 * h * w];
        int planeSize = h * w;
        int[] row = new int[w];

        for (int y = 0; y < h; y++) {
            image.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int rgb = row[x];
                int i = y * w + x;
                tensor[i] = (((rgb >> 16) & 0xFF) / 255f - mean[0]) / std[0];
                tensor[planeSize + i] = (((rgb >> 8) & 0xFF) / 255f - mean[1]) / std[1];
                tensor[2 * planeSize + i] = ((rgb & 0xFF) / 255f - mean[2]) / std[2];
            }
        }
        return tensor;
    }

    static float[] toNchwCentered(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        float[] tensor = new float[3 * h * w];
        int planeSize = h * w;
        int[] row = new int[w];

        for (int y = 0; y < h; y++) {
            image.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int rgb = row[x];
                int i = y * w + x;
                tensor[i] = ((rgb >> 16) & 0xFF) / 127.5f - 1f;
                tensor[planeSize + i] = ((rgb >> 8) & 0xFF) / 127.5f - 1f;
                tensor[2 * planeSize + i] = (rgb & 0xFF) / 127.5f - 1f;
            }
        }
        return tensor;
    }

    @Override
    public void close() {
        synchronized (inferenceLock) {
            closed = true;
            try {
                if (detSession != null)
                    detSession.close();
            } catch (Exception e) {
                logger.warn("Error closing ONNX detection session", e);
            } finally {
                detSession = null;
            }
            try {
                if (recSession != null)
                    recSession.close();
            } catch (Exception e) {
                logger.warn("Error closing ONNX recognition session", e);
            } finally {
                recSession = null;
            }
        }
    }
}
