package com.botts.impl.ocr.text;

import java.util.regex.Pattern;

/**
 * Loose license-plate shape heuristics for v1: US plates vary too much by
 * state for strict formats, so we accept short alphanumeric tokens with at
 * least one digit and rely on the recognition confidence threshold plus
 * multi-frame voting to reject noise.
 */
public final class PlateHeuristics {

    public static final int MIN_LENGTH = 4;
    public static final int MAX_LENGTH = 8;

    private static final Pattern ALNUM = Pattern.compile("^[A-Z0-9]+$");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

    private PlateHeuristics() {}

    public static boolean looksLikePlate(String normalized) {
        if (normalized == null)
            return false;
        int len = normalized.length();
        if (len < MIN_LENGTH || len > MAX_LENGTH)
            return false;
        if (!ALNUM.matcher(normalized).matches())
            return false;
        return HAS_DIGIT.matcher(normalized).matches();
    }
}
