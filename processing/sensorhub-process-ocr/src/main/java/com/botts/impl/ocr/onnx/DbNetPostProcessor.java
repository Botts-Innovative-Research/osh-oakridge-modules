package com.botts.impl.ocr.onnx;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns a DBNet text-detection probability map into axis-aligned boxes in
 * original-image coordinates: binarize, connected components, filter tiny
 * blobs, pad, then merge horizontally adjacent boxes on the same text line
 * (container owner codes and serials often detect as separate boxes).
 */
public class DbNetPostProcessor {

    static final float BINARIZE_THRESHOLD = 0.3f;
    static final int MIN_COMPONENT_AREA = 30;
    // DBNet is trained on shrunk text kernels, so detected regions are ~40%
    // smaller than the glyphs; expand by area/perimeter like PP-OCR's unclip
    static final double UNCLIP_RATIO = 1.5;
    static final double SAME_LINE_MIN_VERTICAL_OVERLAP = 0.5;
    static final double SAME_LINE_MAX_GAP_HEIGHTS = 1.5;

    /**
     * @param probMap detection probabilities, row-major [mapHeight][mapWidth]
     * @param scaleX  originalWidth / mapWidth
     * @param scaleY  originalHeight / mapHeight
     * @param imageWidth original image width, for clamping
     * @param imageHeight original image height, for clamping
     */
    public List<Rectangle> extractBoxes(float[][] probMap, double scaleX, double scaleY,
                                        int imageWidth, int imageHeight) {
        int h = probMap.length;
        int w = h > 0 ? probMap[0].length : 0;

        boolean[][] mask = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                mask[y][x] = probMap[y][x] > BINARIZE_THRESHOLD;

        List<Rectangle> mapBoxes = connectedComponentBoxes(mask, h, w);

        List<Rectangle> boxes = new ArrayList<>();
        for (Rectangle box : mapBoxes) {
            if (box.width * box.height < MIN_COMPONENT_AREA)
                continue;

            int x = (int) Math.round(box.x * scaleX);
            int y = (int) Math.round(box.y * scaleY);
            int bw = (int) Math.round(box.width * scaleX);
            int bh = (int) Math.round(box.height * scaleY);

            int unclip = (int) Math.round(UNCLIP_RATIO * (bw * (double) bh) / (2.0 * (bw + bh)));
            x -= unclip;
            y -= unclip;
            bw += 2 * unclip;
            bh += 2 * unclip;

            x = Math.max(0, x);
            y = Math.max(0, y);
            bw = Math.min(bw, imageWidth - x);
            bh = Math.min(bh, imageHeight - y);
            if (bw <= 1 || bh <= 1)
                continue;

            boxes.add(new Rectangle(x, y, bw, bh));
        }

        return mergeSameLine(boxes);
    }

    private List<Rectangle> connectedComponentBoxes(boolean[][] mask, int h, int w) {
        List<Rectangle> boxes = new ArrayList<>();
        int[] queue = new int[h * w];

        for (int sy = 0; sy < h; sy++) {
            for (int sx = 0; sx < w; sx++) {
                if (!mask[sy][sx])
                    continue;

                int head = 0, tail = 0;
                queue[tail++] = sy * w + sx;
                mask[sy][sx] = false;
                int minX = sx, maxX = sx, minY = sy, maxY = sy;

                while (head < tail) {
                    int cell = queue[head++];
                    int cy = cell / w, cx = cell % w;
                    minX = Math.min(minX, cx);
                    maxX = Math.max(maxX, cx);
                    minY = Math.min(minY, cy);
                    maxY = Math.max(maxY, cy);

                    if (cy > 0 && mask[cy - 1][cx]) { mask[cy - 1][cx] = false; queue[tail++] = cell - w; }
                    if (cy < h - 1 && mask[cy + 1][cx]) { mask[cy + 1][cx] = false; queue[tail++] = cell + w; }
                    if (cx > 0 && mask[cy][cx - 1]) { mask[cy][cx - 1] = false; queue[tail++] = cell - 1; }
                    if (cx < w - 1 && mask[cy][cx + 1]) { mask[cy][cx + 1] = false; queue[tail++] = cell + 1; }
                }

                boxes.add(new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1));
            }
        }
        return boxes;
    }

    /** Merges boxes that sit on the same text line with a small horizontal gap. */
    List<Rectangle> mergeSameLine(List<Rectangle> boxes) {
        List<Rectangle> sorted = new ArrayList<>(boxes);
        sorted.sort(Comparator.comparingInt((Rectangle r) -> r.y).thenComparingInt(r -> r.x));

        List<Rectangle> merged = new ArrayList<>();
        for (Rectangle box : sorted) {
            Rectangle target = null;
            for (Rectangle existing : merged) {
                if (isSameLine(existing, box)) {
                    target = existing;
                    break;
                }
            }
            if (target != null)
                target.setBounds(target.union(box));
            else
                merged.add(new Rectangle(box));
        }
        return merged;
    }

    private boolean isSameLine(Rectangle a, Rectangle b) {
        int overlapTop = Math.max(a.y, b.y);
        int overlapBottom = Math.min(a.y + a.height, b.y + b.height);
        int overlap = overlapBottom - overlapTop;
        int minHeight = Math.min(a.height, b.height);
        if (minHeight <= 0 || overlap < minHeight * SAME_LINE_MIN_VERTICAL_OVERLAP)
            return false;

        int gap = Math.max(a.x, b.x) - Math.min(a.x + a.width, b.x + b.width);
        return gap <= minHeight * SAME_LINE_MAX_GAP_HEIGHTS;
    }
}
