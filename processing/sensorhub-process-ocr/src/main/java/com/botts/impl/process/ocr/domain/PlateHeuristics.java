/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Heuristics for identifying plausible license-plate tokens while rejecting common shipping
 * container stencils, weight markings, and camera on-screen display (OSD) overlays.
 */
public final class PlateHeuristics {

    private static final int MIN_LEN = 5;
    private static final int MAX_LEN = 8;

    private static final Pattern ALLOWED_RAW = Pattern.compile("^[A-Z0-9\\s\\-]{5,12}$");

    private static final Set<String> REJECT_EXACT = Set.of(
            "TARE", "MAXGROSS", "PAYLOAD", "NETWT", "CUFT", "CUM",
            "KGS", "LBS", "ISO", "HEAVY", "CAUTION", "HIGH", "CUBE"
    );

    private static final Pattern OSD_TOKEN = Pattern.compile(
            "^(?:CAM|CAMERA|CH|CHANNEL|LANE|GATE|PORTAL|RPM|FPS|KBPS|MBPS|NVR|DVR|IPC|RTSP|UTC|GMT|REC)\\d*$");

    /** Clipped right-hand container serial fragments (e.g. 6 digits + check char, or 1 trailing prefix letter + 6 serial digits like K514097). */
    private static final Pattern CONTAINER_SERIAL_FRAGMENT = Pattern.compile(
            "^\\d{5,7}[A-Z0-9]$|^[A-Z]\\d{6}$");

    /** ISO 6346 size/type stencil (e.g. 22G1, 45G1, 45R1) plus optional trailing border/corrugation artifacts (e.g. 45C1P, 45G181). */
    private static final Pattern ISO_SIZE_TYPE_STENCIL = Pattern.compile(
            "^(?:20|22|25|40|42|45|L2|L5)[CGHPRUTVBSK][0-9][A-Z0-9]{0,2}$");

    /** Roadside highway and terminal gate signage substrings (e.g. S101 Ring, Terminal 10, Danger) that are not vehicle plates. */
    private static final Pattern SIGNAGE_TOKEN = Pattern.compile(
            "(?:RING|TERMIN|ERMIN|RMINA|ERMUL|MINA\\d|DANGER|EURO|GROSS|CROSS|PAYLOAD)|^\\d{1,3}ING$");

    private PlateHeuristics() {
    }

    public static Optional<String> normalizePlate(String rawText) {
        if (rawText == null)
            return Optional.empty();
        String trimmed = rawText.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_RAW.matcher(trimmed).matches())
            return Optional.empty();

        StringBuilder sb = new StringBuilder(trimmed.length());
        int letters = 0;
        int digits = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.append(c);
                letters++;
            } else if (c >= '0' && c <= '9') {
                sb.append(c);
                digits++;
            }
        }

        String compact = sb.toString();
        if (compact.length() < MIN_LEN || compact.length() > MAX_LEN)
            return Optional.empty();

        // Require at least one letter and one digit so pure words (MAERSK, TRITON) and pure
        // numbers (tare weights, years, bitrate counters) never masquerade as plates.
        if (letters == 0 || digits == 0)
            return Optional.empty();

        if (REJECT_EXACT.contains(compact))
            return Optional.empty();
        if (OSD_TOKEN.matcher(compact).matches())
            return Optional.empty();
        if (CONTAINER_SERIAL_FRAGMENT.matcher(compact).matches())
            return Optional.empty();
        if (ISO_SIZE_TYPE_STENCIL.matcher(compact).matches())
            return Optional.empty();
        if (SIGNAGE_TOKEN.matcher(compact).find())
            return Optional.empty();

        // Reject 4-letter ISO 6346 owner+category prefix fragments (e.g. MSCU1)
        if (compact.length() == 5 && letters == 4
                && (compact.charAt(3) == 'U' || compact.charAt(3) == 'J'
                || compact.charAt(3) == 'Z' || compact.charAt(3) == 'R')
                && Character.isLetter(compact.charAt(0))
                && Character.isLetter(compact.charAt(1))
                && Character.isLetter(compact.charAt(2))) {
            return Optional.empty();
        }

        return Optional.of(compact);
    }
}
