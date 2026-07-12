package com.botts.impl.ocr.text;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlateHeuristicsTest {

    @Test
    public void acceptsTypicalPlates() {
        assertTrue(PlateHeuristics.looksLikePlate("ABC1234"));
        assertTrue(PlateHeuristics.looksLikePlate("7XYZ123"));
        assertTrue(PlateHeuristics.looksLikePlate("1234"));
    }

    @Test
    public void rejectsNonPlates() {
        assertFalse(PlateHeuristics.looksLikePlate("ABC"));          // too short
        assertFalse(PlateHeuristics.looksLikePlate("ABCDEFGHI"));    // too long
        assertFalse(PlateHeuristics.looksLikePlate("MAERSK"));       // no digit
        assertFalse(PlateHeuristics.looksLikePlate("AB 1234"));      // not normalized
        assertFalse(PlateHeuristics.looksLikePlate(null));
        assertFalse(PlateHeuristics.looksLikePlate(""));
    }
}
