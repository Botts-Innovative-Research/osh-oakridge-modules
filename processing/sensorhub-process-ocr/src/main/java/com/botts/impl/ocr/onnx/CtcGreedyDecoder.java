package com.botts.impl.ocr.onnx;

import java.util.List;

/**
 * Greedy CTC decoding of a recognition model output [timesteps][classes]:
 * argmax per timestep, collapse repeats, drop blanks (class 0). Confidence is
 * the mean softmax probability of the emitted characters.
 */
public class CtcGreedyDecoder {

    private final List<String> charset;

    /** @param charset dictionary entries; class index i maps to charset[i-1], class 0 is CTC blank */
    public CtcGreedyDecoder(List<String> charset) {
        this.charset = charset;
    }

    public static class Decoded {
        public final String text;
        public final float confidence;

        Decoded(String text, float confidence) {
            this.text = text;
            this.confidence = confidence;
        }
    }

    public Decoded decode(float[][] logits) {
        StringBuilder text = new StringBuilder();
        double confidenceSum = 0;
        int emitted = 0;
        int prevClass = 0;
        boolean isProbabilities = looksLikeProbabilities(logits);

        for (float[] step : logits) {
            int best = 0;
            for (int c = 1; c < step.length; c++) {
                if (step[c] > step[best])
                    best = c;
            }

            if (best != 0 && best != prevClass) {
                text.append(classToChar(best));
                confidenceSum += isProbabilities ? step[best] : softmaxProbability(step, best);
                emitted++;
            }
            prevClass = best;
        }

        float confidence = emitted > 0 ? (float) (confidenceSum / emitted) : 0f;
        return new Decoded(text.toString(), confidence);
    }

    /** PP-OCR rec exports usually end in a softmax; raw-logit models do not. */
    private static boolean looksLikeProbabilities(float[][] logits) {
        if (logits.length == 0)
            return false;
        double sum = 0;
        for (float v : logits[0]) {
            if (v < 0 || v > 1.0001f)
                return false;
            sum += v;
        }
        return Math.abs(sum - 1.0) < 0.01;
    }

    private String classToChar(int classIndex) {
        int dictIndex = classIndex - 1;
        if (dictIndex < charset.size())
            return charset.get(dictIndex);
        // PP-OCR rec models append a space class after the dictionary
        return " ";
    }

    private double softmaxProbability(float[] step, int index) {
        double max = step[index];
        double sum = 0;
        for (float v : step)
            sum += Math.exp(v - max);
        return 1.0 / sum;
    }
}
