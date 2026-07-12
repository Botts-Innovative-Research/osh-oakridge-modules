package com.botts.impl.ocr.onnx;

import org.junit.Test;

import java.awt.Rectangle;
import java.util.List;

import static org.junit.Assert.*;

public class DbNetPostProcessorTest {

    private float[][] emptyMap(int h, int w) {
        return new float[h][w];
    }

    private void fill(float[][] map, int x, int y, int w, int h) {
        for (int yy = y; yy < y + h; yy++)
            for (int xx = x; xx < x + w; xx++)
                map[yy][xx] = 0.9f;
    }

    @Test
    public void extractsScaledPaddedBox() {
        float[][] map = emptyMap(100, 100);
        fill(map, 20, 40, 30, 10);

        List<Rectangle> boxes = new DbNetPostProcessor().extractBoxes(map, 2.0, 2.0, 200, 200);

        assertEquals(1, boxes.size());
        Rectangle box = boxes.get(0);
        // 30x10 blob at (20,40) scaled by 2 = 60x20 at (40,80), plus the unclip
        // expansion of ~11px on every side
        assertTrue(box.contains(new Rectangle(40, 80, 60, 20)));
        assertTrue(box.x >= 20 && box.y >= 60);
    }

    @Test
    public void ignoresTinyBlobs() {
        float[][] map = emptyMap(100, 100);
        fill(map, 10, 10, 4, 4); // 16 px < MIN_COMPONENT_AREA

        assertTrue(new DbNetPostProcessor().extractBoxes(map, 1, 1, 100, 100).isEmpty());
    }

    @Test
    public void mergesBoxesOnSameLine() {
        float[][] map = emptyMap(100, 200);
        fill(map, 10, 50, 40, 10);   // "CSQU"
        fill(map, 60, 51, 50, 10);   // "305438 3", small gap on the same line

        List<Rectangle> boxes = new DbNetPostProcessor().extractBoxes(map, 1, 1, 200, 100);

        assertEquals(1, boxes.size());
        assertTrue(boxes.get(0).width >= 100);
    }

    @Test
    public void keepsSeparateLinesApart() {
        float[][] map = emptyMap(100, 200);
        fill(map, 10, 20, 60, 10);
        fill(map, 10, 70, 60, 10);

        assertEquals(2, new DbNetPostProcessor().extractBoxes(map, 1, 1, 200, 100).size());
    }
}
