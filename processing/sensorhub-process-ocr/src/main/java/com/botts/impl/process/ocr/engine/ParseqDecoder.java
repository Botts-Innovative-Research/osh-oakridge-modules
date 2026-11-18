/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.engine;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Decoder and aspect-preserving {@code 32x128} tensor preprocessor for Apache-2.0 PARSeq
 * 2D-attention scene-text recognition models ({@code parseq_rec_int8.onnx}).
 */
public final class ParseqDecoder {

    public static final int INPUT_HEIGHT = 32;
    public static final int INPUT_WIDTH = 128;

    private static final float[] MEAN = {0.694f, 0.695f, 0.693f};
    private static final float[] STD = {0.299f, 0.296f, 0.301f};

    private final String vocab;

    public ParseqDecoder(String vocab) {
        this.vocab = vocab == null ? "" : vocab;
    }

    public CtcGreedyDecoder.Decoded decode(float[][] logits) {
        if (logits == null || logits.length == 0 || vocab.isEmpty())
            return new CtcGreedyDecoder.Decoded("", 0f);

        int eosIndex = vocab.length();
        StringBuilder sb = new StringBuilder(logits.length);
        double probSum = 0.0;
        int count = 0;

        for (float[] step : logits) {
            if (step == null || step.length == 0)
                break;
            int bestIdx = 0;
            float bestVal = step[0];
            for (int c = 1; c < step.length; c++) {
                if (step[c] > bestVal) {
                    bestVal = step[c];
                    bestIdx = c;
                }
            }
            if (bestIdx >= eosIndex)
                break;

            sb.append(vocab.charAt(bestIdx));
            probSum += softmaxAt(step, bestIdx);
            count++;
        }

        float conf = count > 0 ? (float) (probSum / count) : 0f;
        return new CtcGreedyDecoder.Decoded(sb.toString(), conf);
    }

    /**
     * Aspect-ratio-preserving bilinear resize into {@code 32x128} with right/bottom zero-padding
     * prior to {@code (px/255 - mean) / std} normalization (matching docTR PARSeq preprocessing).
     */
    public static float[] toInputTensor(BufferedImage crop) {
        int h = Math.max(1, crop.getHeight());
        int w = Math.max(1, crop.getWidth());
        double ratio = Math.min(INPUT_HEIGHT / (double) h, INPUT_WIDTH / (double) w);
        int nh = Math.max(1, Math.min(INPUT_HEIGHT, (int) Math.round(h * ratio)));
        int nw = Math.max(1, Math.min(INPUT_WIDTH, (int) Math.round(w * ratio)));

        BufferedImage resized = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(crop, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }

        int plane = INPUT_HEIGHT * INPUT_WIDTH;
        float[] tensor = new float[3 * plane];
        float padR = -MEAN[0] / STD[0];
        float padG = -MEAN[1] / STD[1];
        float padB = -MEAN[2] / STD[2];
        for (int i = 0; i < plane; i++) {
            tensor[i] = padR;
            tensor[plane + i] = padG;
            tensor[2 * plane + i] = padB;
        }

        int[] row = new int[nw];
        for (int y = 0; y < nh; y++) {
            resized.getRGB(0, y, nw, 1, row, 0, nw);
            int base = y * INPUT_WIDTH;
            for (int x = 0; x < nw; x++) {
                int rgb = row[x];
                int idx = base + x;
                tensor[idx] = (((rgb >> 16) & 0xFF) / 255f - MEAN[0]) / STD[0];
                tensor[plane + idx] = (((rgb >> 8) & 0xFF) / 255f - MEAN[1]) / STD[1];
                tensor[2 * plane + idx] = ((rgb & 0xFF) / 255f - MEAN[2]) / STD[2];
            }
        }
        resized.flush();
        return tensor;
    }

    private static double softmaxAt(float[] step, int idx) {
        double max = step[idx];
        for (float v : step) {
            if (v > max)
                max = v;
        }
        double sum = 0.0;
        for (float v : step) {
            sum += Math.exp(v - max);
        }
        return sum > 0.0 ? Math.exp(step[idx] - max) / sum : 0.0;
    }
}
