package com.botts.impl.ocr.pipeline;

/**
 * Pipeline settings for one lane, mapped from the lane's OCR config. Kept
 * separate from the lane config class so this module has no lane dependency.
 */
public class OcrSettings {

    public String modelDir = "models/ocr";
    public boolean readContainerIds = true;
    public boolean readPlates = true;
    public int framesPerVideo = 8;
    public double containerMinConfidence = 0.5;
    public double plateMinConfidence = 0.6;
}
