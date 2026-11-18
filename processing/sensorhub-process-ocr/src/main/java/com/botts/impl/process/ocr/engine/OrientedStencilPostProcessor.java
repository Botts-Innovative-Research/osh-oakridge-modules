/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.engine;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Letterbox preprocessor, stride-4 CenterNet peak decoder, and {@link AffineTransform} native-resolution
 * deskew cropper for {@code stencil_det.onnx} ({@code [1, 8, 96, 160]} on {@code 640x384} input).
 */
public final class OrientedStencilPostProcessor {

    public static final int INPUT_WIDTH = 640;
    public static final int INPUT_HEIGHT = 384;
    public static final int STRIDE = 4;
    public static final int MAX_CONTAINER_PEAKS = 3;
    public static final int MAX_PLATE_PEAKS = 2;

    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    public record OrientedBox(
            int classId,
            double score,
            double cx,
            double cy,
            double width,
            double height,
            double thetaRadians
    ) {
        public Rectangle axisAlignedBounds(int imgW, int imgH) {
            double cos = Math.abs(Math.cos(thetaRadians));
            double sin = Math.abs(Math.sin(thetaRadians));
            int bw = Math.max(4, (int) Math.round(width * cos + height * sin));
            int bh = Math.max(4, (int) Math.round(width * sin + height * cos));
            int x = Math.max(0, Math.min(imgW - 2, (int) Math.round(cx - bw * 0.5)));
            int y = Math.max(0, Math.min(imgH - 2, (int) Math.round(cy - bh * 0.5)));
            bw = Math.min(bw, imgW - x);
            bh = Math.min(bh, imgH - y);
            return new Rectangle(x, y, Math.max(2, bw), Math.max(2, bh));
        }
    }

    public record LetterboxInput(float[] tensor, double scale, int padX, int padY) {}

    private OrientedStencilPostProcessor() {
    }

    public static LetterboxInput preprocess(BufferedImage src) {
        int fw = Math.max(1, src.getWidth());
        int fh = Math.max(1, src.getHeight());
        double scale = Math.min(INPUT_WIDTH / (double) fw, INPUT_HEIGHT / (double) fh);
        int rw = Math.max(1, Math.min(INPUT_WIDTH, (int) Math.round(fw * scale)));
        int rh = Math.max(1, Math.min(INPUT_HEIGHT, (int) Math.round(fh * scale)));
        int padX = (INPUT_WIDTH - rw) / 2;
        int padY = (INPUT_HEIGHT - rh) / 2;

        BufferedImage canvas = new BufferedImage(INPUT_WIDTH, INPUT_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, INPUT_WIDTH, INPUT_HEIGHT);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, padX, padY, rw, rh, null);
        } finally {
            g.dispose();
        }

        float[] tensor = OnnxOcrEngine.toNchw(canvas, MEAN, STD);
        canvas.flush();
        return new LetterboxInput(tensor, scale, padX, padY);
    }

    public static List<OrientedBox> decodePeaks(float[][][] outMap,
                                                LetterboxInput lb,
                                                int origW,
                                                int origH,
                                                double containerMinScore,
                                                double plateMinScore) {
        if (outMap == null || outMap.length < 8)
            return List.of();
        int gh = outMap[0].length;
        int gw = outMap[0][0].length;

        List<OrientedBox> containers = extractClassPeaks(outMap, 0, gh, gw, lb, origW, origH, containerMinScore, MAX_CONTAINER_PEAKS);
        List<OrientedBox> plates = extractClassPeaks(outMap, 1, gh, gw, lb, origW, origH, plateMinScore, MAX_PLATE_PEAKS);

        List<OrientedBox> combined = new ArrayList<>(containers.size() + plates.size());
        combined.addAll(containers);
        combined.addAll(plates);
        return combined;
    }

    private static List<OrientedBox> extractClassPeaks(float[][][] outMap,
                                                       int cls,
                                                       int gh,
                                                       int gw,
                                                       LetterboxInput lb,
                                                       int origW,
                                                       int origH,
                                                       double minScore,
                                                       int maxKeep) {
        float[][] hm = outMap[cls];
        List<OrientedBox> raw = new ArrayList<>();
        for (int y = 1; y < gh - 1; y++) {
            float[] row = hm[y];
            for (int x = 1; x < gw - 1; x++) {
                float s = row[x];
                if (s < minScore)
                    continue;
                if (s < hm[y - 1][x - 1] || s < hm[y - 1][x] || s < hm[y - 1][x + 1]
                        || s < row[x - 1] || s < row[x + 1]
                        || s < hm[y + 1][x - 1] || s < hm[y + 1][x] || s < hm[y + 1][x + 1]) {
                    continue;
                }
                double icx = (x + outMap[2][y][x]) * STRIDE;
                double icy = (y + outMap[3][y][x]) * STRIDE;
                double iw = Math.exp(Math.max(1.0, Math.min(6.5, outMap[4][y][x])));
                double ih = Math.exp(Math.max(1.0, Math.min(6.0, outMap[5][y][x])));
                double theta = 0.5 * Math.atan2(outMap[6][y][x], outMap[7][y][x]);

                double ocx = (icx - lb.padX()) / lb.scale();
                double ocy = (icy - lb.padY()) / lb.scale();
                double ow = iw / lb.scale();
                double oh = ih / lb.scale();
                if (ocx < 2 || ocx > origW - 2 || ocy < 2 || ocy > origH - 2)
                    continue;

                raw.add(new OrientedBox(cls, s, ocx, ocy, ow, oh, theta));
            }
        }

        raw.sort(Comparator.comparingDouble(OrientedBox::score).reversed());
        List<OrientedBox> nms = new ArrayList<>(Math.min(maxKeep, raw.size()));
        for (OrientedBox cand : raw) {
            boolean suppressed = false;
            for (OrientedBox kept : nms) {
                double dx = Math.abs(cand.cx() - kept.cx());
                double dy = Math.abs(cand.cy() - kept.cy());
                if (dx < Math.min(cand.width(), kept.width()) * 0.55
                        && dy < Math.min(cand.height(), kept.height()) * 0.65) {
                    suppressed = true;
                    break;
                }
            }
            if (!suppressed) {
                nms.add(cand);
                if (nms.size() >= maxKeep)
                    break;
            }
        }
        return nms;
    }

    /**
     * Extracts a native-resolution deskewed crop around {@code box} by counter-rotating {@code -theta}
     * around {@code (cx, cy)} via {@link AffineTransform}.
     */
    public static BufferedImage extractDeskewedCrop(BufferedImage src,
                                                    OrientedBox box,
                                                    double widthScale,
                                                    double heightScale) {
        int outW = Math.max(16, Math.min(src.getWidth(), (int) Math.round(box.width() * widthScale)));
        int outH = Math.max(10, Math.min(src.getHeight(), (int) Math.round(box.height() * heightScale)));

        if (Math.abs(box.thetaRadians()) < 0.035) {
            int x = Math.max(0, Math.min(src.getWidth() - 2, (int) Math.round(box.cx() - outW * 0.5)));
            int y = Math.max(0, Math.min(src.getHeight() - 2, (int) Math.round(box.cy() - outH * 0.5)));
            int w = Math.min(outW, src.getWidth() - x);
            int h = Math.min(outH, src.getHeight() - y);
            return src.getSubimage(x, y, Math.max(2, w), Math.max(2, h));
        }

        BufferedImage dst = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            AffineTransform at = new AffineTransform();
            at.translate(outW * 0.5, outH * 0.5);
            at.rotate(-box.thetaRadians());
            at.translate(-box.cx(), -box.cy());
            g.drawImage(src, at, null);
        } finally {
            g.dispose();
        }
        return dst;
    }
}
