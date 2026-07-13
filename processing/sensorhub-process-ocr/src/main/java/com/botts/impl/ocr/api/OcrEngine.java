package com.botts.impl.ocr.api;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * A scene-text OCR engine: finds text regions in an image and recognizes them.
 * The default implementation runs ONNX models in-process; alternatives
 * (Tesseract, an HTTP sidecar) can be swapped in behind this interface.
 */
public interface OcrEngine extends AutoCloseable {

    /** Loads models from the given directory. Must be called once before read(). */
    void init(Path modelDir) throws OcrEngineException;

    /** Detects and recognizes all text regions in the image. */
    List<OcrRead> read(BufferedImage image) throws OcrEngineException;

    @Override
    void close();
}
