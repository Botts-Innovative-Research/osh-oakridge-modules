package com.botts.impl.ocr.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import com.botts.impl.ocr.api.OcrEngine;
import com.botts.impl.ocr.api.OcrEngineException;
import com.botts.impl.ocr.api.OcrRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PP-OCR-family engine on ONNX Runtime (CPU): a DBNet text-detection model
 * finds text regions, a CTC recognition model reads each region. Expects
 * {@code det.onnx}, {@code rec.onnx} and {@code dict.txt} in the model dir.
 */
public class OnnxOcrEngine implements OcrEngine {

    public static final String DET_MODEL = "det.onnx";
    public static final String REC_MODEL = "rec.onnx";
    public static final String DICT_FILE = "dict.txt";

    // DBNet input constraints and PP-OCR normalization constants
    static final int DET_MAX_SIDE = 960;
    static final float[] DET_MEAN = {0.485f, 0.456f, 0.406f};
    static final float[] DET_STD = {0.229f, 0.224f, 0.225f};
    static final int REC_DEFAULT_HEIGHT = 48;
    static final int REC_MIN_WIDTH = 16;
    // generous cap: squeezing a long line horizontally mangles glyphs, and the
    // rec model is dynamic-width, so only truly extreme crops get clamped
    static final int REC_MAX_WIDTH = 1920;

    private static final Logger logger = LoggerFactory.getLogger(OnnxOcrEngine.class);

    private final int intraOpThreads;

    OrtEnvironment env;
    OrtSession detSession;
    OrtSession recSession;
    String detInputName;
    String recInputName;
    CtcGreedyDecoder decoder;
    final DbNetPostProcessor postProcessor = new DbNetPostProcessor();
    int recHeight = REC_DEFAULT_HEIGHT;

    public OnnxOcrEngine() {
        this(2);
    }

    public OnnxOcrEngine(int intraOpThreads) {
        this.intraOpThreads = intraOpThreads;
    }

    @Override
    public synchronized void init(Path modelDir) throws OcrEngineException {
        Path detPath = modelDir.resolve(DET_MODEL);
        Path recPath = modelDir.resolve(REC_MODEL);
        Path dictPath = modelDir.resolve(DICT_FILE);
        for (Path p : List.of(detPath, recPath, dictPath)) {
            if (!Files.isReadable(p))
                throw new OcrEngineException("OCR model file not found or unreadable: " + p);
        }

        try {
            env = OrtEnvironment.getEnvironment();
            try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
                options.setIntraOpNumThreads(intraOpThreads);
                options.setInterOpNumThreads(1);
                detSession = env.createSession(detPath.toString(), options);
            }
            try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
                options.setIntraOpNumThreads(intraOpThreads);
                options.setInterOpNumThreads(1);
                recSession = env.createSession(recPath.toString(), options);
            }

            detInputName = detSession.getInputNames().iterator().next();
            recInputName = recSession.getInputNames().iterator().next();
            recHeight = queryRecInputHeight(recSession, recInputName);

            List<String> charset = Files.readAllLines(dictPath, StandardCharsets.UTF_8);
            decoder = new CtcGreedyDecoder(charset);

            logger.info("OCR engine initialized from {} (rec input height {})", modelDir, recHeight);
        } catch (OrtException | IOException e) {
            close();
            throw new OcrEngineException("Failed to initialize ONNX OCR engine from " + modelDir, e);
        }
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
    public List<OcrRead> read(BufferedImage image) throws OcrEngineException {
        if (detSession == null || recSession == null)
            throw new OcrEngineException("OCR engine not initialized");

        List<Rectangle> boxes = detect(image);
        List<OcrRead> reads = new ArrayList<>();
        for (Rectangle box : boxes) {
            BufferedImage crop = image.getSubimage(box.x, box.y, box.width, box.height);
            CtcGreedyDecoder.Decoded decoded = recognize(crop);
            if (decoded.text != null && !decoded.text.isBlank())
                reads.add(new OcrRead(decoded.text, decoded.confidence, box));
        }
        return reads;
    }

    private List<Rectangle> detect(BufferedImage image) throws OcrEngineException {
        int ow = image.getWidth();
        int oh = image.getHeight();
        double ratio = Math.min(1.0, DET_MAX_SIDE / (double) Math.max(ow, oh));
        int rw = roundToMultipleOf32(ow * ratio);
        int rh = roundToMultipleOf32(oh * ratio);

        BufferedImage resized = resize(image, rw, rh);
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
    }

    private CtcGreedyDecoder.Decoded recognize(BufferedImage crop) throws OcrEngineException {
        int width = (int) Math.round(crop.getWidth() * (recHeight / (double) crop.getHeight()));
        width = Math.max(REC_MIN_WIDTH, Math.min(REC_MAX_WIDTH, width));

        BufferedImage resized = resize(crop, width, recHeight);
        float[] tensor = toNchwCentered(resized);

        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(tensor), new long[]{1, 3, recHeight, width});
             OrtSession.Result result = recSession.run(Collections.singletonMap(recInputName, input))) {

            float[][][] output = (float[][][]) result.get(0).getValue();
            return decoder.decode(output[0]);
        } catch (OrtException e) {
            throw new OcrEngineException("Text recognition failed", e);
        }
    }

    private static int roundToMultipleOf32(double v) {
        return Math.max(32, (int) Math.round(v / 32.0) * 32);
    }

    static BufferedImage resize(BufferedImage src, int width, int height) {
        if (src.getWidth() == width && src.getHeight() == height && src.getType() == BufferedImage.TYPE_INT_RGB)
            return src;
        BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return dst;
    }

    /** RGB image to NCHW float tensor, normalized per channel with mean/std. */
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

    /** RGB image to NCHW float tensor normalized to [-1, 1] (PP-OCR recognition input). */
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
    public synchronized void close() {
        try {
            if (detSession != null)
                detSession.close();
            if (recSession != null)
                recSession.close();
        } catch (Exception e) {
            logger.warn("Error closing ONNX sessions", e);
        } finally {
            detSession = null;
            recSession = null;
        }
    }
}
