package com.botts.impl.ocr.text;

import java.util.regex.Pattern;

/**
 * ISO 6346 shipping container number utilities: normalization, OCR-error
 * positional coercion, pattern matching and check-digit validation.
 * A container number is 11 characters: a 3-letter owner code, a category
 * identifier (U, J or Z), a 6-digit serial and a check digit.
 */
public final class Iso6346 {

    public static final int LENGTH = 11;

    private static final Pattern CONTAINER_PATTERN = Pattern.compile("^[A-Z]{3}[UJZ]\\d{7}$");
    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]");

    // ISO 6346 letter values count up from A=10 but never use multiples of 11
    // (11, 22 and 33 are unassigned), so the sequence jumps at K->L and U->V.
    private static final int[] LETTER_VALUES = {
            10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 23, 24,
            25, 26, 27, 28, 29, 30, 31, 32, 34, 35, 36, 37, 38
    };

    private Iso6346() {}

    /** Uppercases and strips every character outside [A-Z0-9]. */
    public static String normalize(String raw) {
        if (raw == null)
            return "";
        return NON_ALNUM.matcher(raw.toUpperCase()).replaceAll("");
    }

    /**
     * Fixes common OCR confusions using the fixed character classes of an
     * 11-char container number: positions 1-4 must be letters, 5-11 digits.
     * Returns the coerced string, or the input unchanged if not 11 chars.
     */
    public static String coerceByPosition(String candidate) {
        if (candidate == null || candidate.length() != LENGTH)
            return candidate;

        char[] chars = candidate.toCharArray();
        for (int i = 0; i < 4; i++)
            chars[i] = digitToLetter(chars[i]);
        for (int i = 4; i < LENGTH; i++)
            chars[i] = letterToDigit(chars[i]);
        return new String(chars);
    }

    private static char digitToLetter(char c) {
        switch (c) {
            case '0': return 'O';
            case '1': return 'I';
            case '2': return 'Z';
            case '5': return 'S';
            case '8': return 'B';
            default: return c;
        }
    }

    private static char letterToDigit(char c) {
        switch (c) {
            case 'O': return '0';
            case 'I': return '1';
            case 'Z': return '2';
            case 'S': return '5';
            case 'B': return '8';
            default: return c;
        }
    }

    /** True when the string is 11 chars of the ISO 6346 shape (no check-digit test). */
    public static boolean matchesPattern(String s) {
        return s != null && CONTAINER_PATTERN.matcher(s).matches();
    }

    /**
     * Computes the ISO 6346 check digit over the first 10 characters:
     * sum of character values weighted by 2^position, mod 11, mod 10.
     */
    public static int checkDigit(String first10) {
        int sum = 0;
        int weight = 1;
        for (int i = 0; i < 10; i++) {
            char c = first10.charAt(i);
            int value = Character.isDigit(c) ? c - '0' : LETTER_VALUES[c - 'A'];
            sum += value * weight;
            weight <<= 1;
        }
        return (sum % 11) % 10;
    }

    /** True when the string matches the ISO 6346 pattern and its check digit. */
    public static boolean isValid(String s) {
        if (!matchesPattern(s))
            return false;
        return checkDigit(s) == s.charAt(10) - '0';
    }

    /** A container-number match found inside a longer OCR string. */
    public static class Match {
        public final String value;
        public final boolean checksumValid;

        Match(String value, boolean checksumValid) {
            this.value = value;
            this.checksumValid = checksumValid;
        }
    }

    /**
     * Slides an 11-char window over a normalized string, applying positional
     * coercion to each window. Returns the first checksum-valid match, or the
     * first pattern-only match when nothing validates, or null.
     */
    public static Match scan(String normalized) {
        if (normalized == null || normalized.length() < LENGTH)
            return null;

        Match patternOnly = null;
        for (int i = 0; i + LENGTH <= normalized.length(); i++) {
            String window = coerceByPosition(normalized.substring(i, i + LENGTH));
            if (!matchesPattern(window))
                continue;
            if (isValid(window))
                return new Match(window, true);
            if (patternOnly == null)
                patternOnly = new Match(window, false);
        }
        return patternOnly;
    }
}
