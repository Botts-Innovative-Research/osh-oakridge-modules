package com.botts.impl.ocr.api;

import java.awt.Rectangle;

/** A single text region read from one image: raw text, confidence and location. */
public class OcrRead {

    public final String text;
    public final float confidence;
    public final Rectangle bbox;

    public OcrRead(String text, float confidence, Rectangle bbox) {
        this.text = text;
        this.confidence = confidence;
        this.bbox = bbox;
    }

    @Override
    public String toString() {
        return "OcrRead{\"" + text + "\", conf=" + confidence + ", bbox=" + bbox + "}";
    }
}
