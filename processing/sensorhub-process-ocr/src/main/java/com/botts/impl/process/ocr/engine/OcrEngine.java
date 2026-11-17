/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.engine;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Scene-text detection and recognition engine contract.
 */
@FunctionalInterface
public interface OcrEngine extends AutoCloseable {

    int DEFAULT_DET_MAX_SIDE = 960;
    int HIGH_RES_DET_MAX_SIDE = 1600;

    /** Detects and recognizes text boxes in {@code frame} at {@link #DEFAULT_DET_MAX_SIDE}. Never returns {@code null}. */
    List<OcrRead> recognize(BufferedImage frame);

    /**
     * Detects and recognizes text boxes in {@code frame} resizing the longest side to at most
     * {@code detMaxSide} pixels prior to DBNet inference. Implementations that do not support
     * multi-scale detection fall back to {@link #recognize(BufferedImage)}.
     */
    default List<OcrRead> recognize(BufferedImage frame, int detMaxSide) {
        return recognize(frame);
    }

    @Override
    default void close() {
    }
}
