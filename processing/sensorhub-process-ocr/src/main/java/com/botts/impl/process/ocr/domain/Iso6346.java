/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * ISO 6346 freight container identification code parser, check-digit validator, and
 * OCR-ambiguity repair utility.
 *
 * <p>Format: 3 uppercase owner letters + 1 equipment category identifier ({@code U},
 * {@code J}, {@code Z}, {@code R}) + 6 serial digits + 1 check digit (11 characters total,
 * compact normalized form {@code CSQU3054383}).</p>
 */
public final class Iso6346 {

    public record Match(String compact, String formatted, boolean checksumValid, boolean checkDigitRepaired) {}

    public static final int CODE_LENGTH = 11;

    private static final Pattern CONTAINER_PATTERN = Pattern.compile("^[A-Z]{3}[UJZR]\\d{7}$");
    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]");

    /**
     * Burned-in camera OSD timestamps, phone/web markings, door weight labels, and standalone ISO
     * size/type codes stripped prior to scanning 11-character windows so non-ID tokens never fuse
     * with adjacent text into bogus container codes (e.g. {@code CAM 2026-03-28}, {@code MAX.GROSS},
     * {@code Tel:+31(0)10-8200700}, or trailing {@code 45G1}).
     */
    private static final Pattern NON_ID_STENCILS = Pattern.compile(
            "(?i)(?:TEL|FAX|PHONE)\\s*[:.]?\\s*\\+?[0-9()\\s.\\-]{6,}"
                    + "|WWW\\.[A-Z0-9.\\-]+"
                    + "|\\+\\d{1,3}\\s*\\(?\\d+\\)?[\\s.\\-]*\\d[\\d\\s.\\-]{4,}"
                    + "|MAX[.\\s]*(?:GROSS|CROSS|G\\.?\\s*W\\.?)"
                    + "|\\b(?:TARE|PAYLOAD|NET\\s*WT|CU\\.?\\s*CAP)\\b"
                    + "|\\b(?:12|20|22|25|40|42|45|L2|L5)[CGHPRUTVBSK06][0-91I]\\b"
                    + "|\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}"
                    + "|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4}"
                    + "|\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b");

    private static final Set<String> KNOWN_BIC_PREFIXES = Set.of(
            "ABEU", "ACLU", "AGMU", "AGNU", "AMFU", "AMHU", "ANBU", "APDU", "APHU", "APRU",
            "APXU", "APZU", "AXIU", "BEAU", "BGBU", "BGFU", "BHCU", "BHOU", "BIDU", "BMOU",
            "BOLU", "BRTU", "BSIU", "BTEU", "BTLU", "CAAU", "CAIU", "CANU", "CARU", "CAXU",
            "CBHU", "CCLU", "CDAU", "CEXU", "CFOU", "CGMU", "CHAU", "CICU", "CIMU", "CIQU",
            "CLDU", "CLHU", "CMAU", "CNEU", "CNRU", "CNSU", "COCU", "COLU", "COSU", "CPPU",
            "CPVU", "CRIU", "CRXU", "CSFU", "CSLU", "CSNU", "CSQU", "CTDU", "CXDU", "CXRU",
            "CXSU", "CXTU", "DAYU", "DFOU", "DFSU", "DHAU", "DHDU", "DHIU", "DILU", "DJDU",
            "DOCU", "DOLU", "DRYU", "DYPU", "ECBU", "ECHU", "ECMU", "ECSU", "EGHU", "EGSU",
            "EISU", "EITU", "EKCU", "ELTU", "EMCU", "EMHU", "EMOU", "ENHU", "EUCU", "EURU",
            "EUVU", "EXFU", "EXPU", "EXSU", "FANU", "FBIU", "FBLU", "FCIU", "FDXU", "FFAU",
            "FICU", "FOTU", "FSCU", "FTAU", "FWRU", "GAOU", "GATU", "GCAU", "GCNU", "GCXU",
            "GESU", "GILU", "GLDU", "GSTU", "GVCU", "GYCU", "HAMU", "HAPU", "HARU", "HASU",
            "HDMU", "HGIU", "HGTU", "HHHU", "HIBU", "HLBU", "HLXU", "HMCU", "HMMU", "HOYU",
            "HUKU", "IAMU", "IBRU", "ICKU", "ICLU", "IDEU", "ILBU", "IMAU", "IMTU", "ITNU",
            "JBHU", "JBNU", "JZPU", "KKFU", "KKTU", "KMTU", "KOCU", "LTIU", "LYGU", "LYSU",
            "MAGU", "MATU", "MBSU", "MCLU", "MEBU", "MEDU", "MEKU", "MEMU", "MERU", "MMAU",
            "MNBU", "MOAU", "MOCU", "MOIU", "MORU", "MOTU", "MRKU", "MRSU", "MSAU", "MSBU",
            "MSCU", "MSDU", "MSGU", "MSKU", "MSMU", "MSNU", "MSWU", "MWCU", "MXMU", "NESU",
            "NEWU", "NFWU", "NIDU", "NPOU", "NRSU", "NYKU", "OCVU", "OERU", "ONAU", "ONEU",
            "ONRU", "OOCU", "OOLU", "OPTU", "ORYU", "OULU", "PCVU", "PFAU", "PGHU", "PGRU",
            "PONU", "POTU", "PSSU", "PVDU", "RANU", "RATU", "RCLU", "RLTU", "RROU", "RRSU",
            "RSTU", "SAIU", "SANU", "SBKU", "SEGU", "SEKU", "SELU", "SENU", "SESU", "SEZU",
            "SIMU", "SITU", "SKLU", "SKYU", "SLLU", "SLZU", "SNEU", "SNLU", "SNTU", "SPTU",
            "STBU", "SUDU", "SWRU", "SZLU", "TANU", "TCIU", "TCKU", "TCLU", "TCNU", "TCVU",
            "TDTU", "TEGU", "TEMU", "TENU", "TEXU", "TFAU", "TGBU", "TGHU", "TIFU", "TIIU",
            "TKRU", "TLLU", "TRHU", "TRIU", "TRLU", "TSLU", "TTNU", "TXGU", "UACU", "UAEU",
            "UASU", "UBHU", "UETU", "UMEU", "UMXU", "UNIU", "UNXU", "UPSU", "UTCU", "VENU",
            "VODU", "VTGU", "WABU", "WEDU", "WHLU", "WHSU", "WILU", "XGRU", "XINU", "XPCU",
            "XPOU", "XXLU", "XYLU", "YDHU", "YMLU", "YMMU", "ZCSU", "ZIMU", "ZMOU"
    );

    // ISO 6346 letter values count up from A=10 skipping multiples of 11 (11, 22, 33 unused)
    private static final int[] LETTER_VALUES = {
            10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 23, 24,
            25, 26, 27, 28, 29, 30, 31, 32, 34, 35, 36, 37, 38
    };

    private Iso6346() {
    }

    public static String stripTimestamps(String raw) {
        if (raw == null)
            return "";
        return NON_ID_STENCILS.matcher(raw).replaceAll(" ");
    }

    public static String normalize(String raw) {
        if (raw == null)
            return "";
        return NON_ALNUM.matcher(stripTimestamps(raw).toUpperCase(Locale.ROOT)).replaceAll("");
    }

    public static boolean matchesPattern(String compact) {
        return compact != null && CONTAINER_PATTERN.matcher(compact).matches();
    }

    public static boolean isKnownBicPrefix(String prefix4) {
        return prefix4 != null
                && prefix4.length() >= 4
                && KNOWN_BIC_PREFIXES.contains(prefix4.substring(0, 4).toUpperCase(Locale.ROOT));
    }

    public static int computeCheckDigit(CharSequence first10) {
        if (first10 == null || first10.length() < 10)
            throw new IllegalArgumentException("Expected at least 10 characters");
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = Character.toUpperCase(first10.charAt(i));
            int val;
            if (i < 4) {
                if (c < 'A' || c > 'Z')
                    throw new IllegalArgumentException("Expected letter at index " + i + ": " + first10);
                if (i == 3 && !isCategory(c))
                    throw new IllegalArgumentException("Invalid category letter: " + c);
                val = LETTER_VALUES[c - 'A'];
            } else {
                if (c < '0' || c > '9')
                    throw new IllegalArgumentException("Expected digit at index " + i + ": " + first10);
                val = c - '0';
            }
            sum += val * (1 << i);
        }
        return (sum % 11) % 10;
    }

    public static boolean isValid(String candidate) {
        if (candidate == null)
            return false;
        String clean = NON_ALNUM.matcher(candidate.toUpperCase(Locale.ROOT)).replaceAll("");
        if (!matchesPattern(clean))
            return false;
        return computeCheckDigit(clean) == (clean.charAt(10) - '0');
    }

    public static String repairCheckDigit(String first10) {
        return first10 + (char) ('0' + computeCheckDigit(first10));
    }

    public static String format(String compact11) {
        if (compact11 == null || compact11.length() != CODE_LENGTH)
            return compact11;
        return compact11.substring(0, 4) + " " + compact11.substring(4, 10) + " " + compact11.substring(10);
    }

    /**
     * Scans {@code rawText} for the best ISO 6346 container match, preferring in order:
     * <ol>
     *   <li>An 11-char window (with positional glyph coercion) whose read check digit validates.</li>
     *   <li>A cleanly-read 10-char owner+category+serial prefix whose boxed check digit was
     *       misread or missed by the detector, repaired deterministically from characters 1..10.</li>
     *   <li>A structural pattern-only match whose category slot was genuinely read as a letter.</li>
     * </ol>
     */
    public static Optional<Match> extractBest(String rawText) {
        String normalized = normalize(rawText);
        if (normalized.length() < CODE_LENGTH - 1)
            return Optional.empty();

        Match repaired = null;
        Match patternOnly = null;

        for (int i = 0; i + CODE_LENGTH <= normalized.length(); i++) {
            String source = normalized.substring(i, i + CODE_LENGTH);
            String coerced = coerceByPosition(source);
            String validVariant = findChecksumValidVariant(source, coerced);
            if (validVariant != null) {
                return Optional.of(new Match(validVariant, format(validVariant), true, false));
            }
            // Rear-door vertical locking bar right after the 4-letter prefix is occasionally read
            // as an extra '1'/'I'/'L' before the 7-digit serial+check (e.g. 'ECBU 1457135 7' -> 'ECBU4571357').
            if (i + CODE_LENGTH + 1 <= normalized.length()) {
                char barGlyph = normalized.charAt(i + 4);
                if (barGlyph == '1' || barGlyph == 'I' || barGlyph == 'L') {
                    String withoutBar = normalized.substring(i, i + 4) + normalized.substring(i + 5, i + CODE_LENGTH + 1);
                    String withoutBarCoerced = coerceByPosition(withoutBar);
                    String barVariant = findChecksumValidVariant(withoutBar, withoutBarCoerced);
                    if (barVariant != null) {
                        return Optional.of(new Match(barVariant, format(barVariant), true, false));
                    }
                }
            }
            // Never overwrite an optically read 11th digit: if a 7th digit was read immediately
            // after the 6 serial digits and failed Modulo-11 (even after 1-char BIC prefix repair),
            // an earlier serial/prefix character was misread, so synthesizing a new check digit
            // would mask the optical error.
            if (repaired == null && prefixReadCleanly(source) && !Character.isDigit(source.charAt(10))) {
                String fixed = repairCheckDigit(source.substring(0, 10));
                repaired = new Match(fixed, format(fixed), true, true);
            }
            // Reject pattern-only candidates whose category identifier was manufactured from a digit
            if (patternOnly == null && matchesPattern(coerced) && !Character.isDigit(source.charAt(3))) {
                patternOnly = new Match(coerced, format(coerced), false, false);
            }
        }

        // Check-digit box missed altogether leaving a clean 10-character owner+category+serial read
        if (repaired == null) {
            for (int i = 0; i + CODE_LENGTH - 1 <= normalized.length(); i++) {
                String source = normalized.substring(i, i + CODE_LENGTH - 1);
                boolean hasEleventhDigit = (i + CODE_LENGTH - 1 < normalized.length())
                        && Character.isDigit(normalized.charAt(i + CODE_LENGTH - 1));
                if (!hasEleventhDigit && prefixReadCleanly(source)) {
                    String fixed = repairCheckDigit(source);
                    repaired = new Match(fixed, format(fixed), true, true);
                    break;
                }
            }
        }

        return Optional.ofNullable(repaired != null ? repaired : patternOnly);
    }

    private static boolean prefixReadCleanly(String source) {
        if (source == null || source.length() < 10)
            return false;
        for (int i = 0; i < 3; i++) {
            char c = source.charAt(i);
            if (c < 'A' || c > 'Z')
                return false;
        }
        if (!isCategory(source.charAt(3)))
            return false;
        for (int i = 4; i < 10; i++) {
            char c = source.charAt(i);
            if (c < '0' || c > '9')
                return false;
        }
        return true;
    }

    static String coerceByPosition(String window11) {
        char[] out = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            char c = window11.charAt(i);
            if (i < 4) {
                out[i] = digitToLetter(c);
            } else {
                out[i] = letterToDigit(c);
            }
        }
        return new String(out);
    }

    /**
     * Evaluates primary positional coercion first (resolving {@code WSKU -> MSKU} Modulo-11 weight
     * collisions where {@code W=35} and {@code M=24} differ by {@code 11}), then checks ambiguous
     * {@code 0}/{@code O} -> {@code O}/{@code Q}/{@code D} owner letters, and finally checks a
     * unique single-letter corrugated-wall optical confusion into {@link #KNOWN_BIC_PREFIXES}
     * strictly when the optically read 11th Modulo-11 check digit validates.
     */
    private static String findChecksumValidVariant(String source, String coerced) {
        if (coerced.startsWith("WSKU")) {
            String msku = "MSKU" + coerced.substring(4);
            if (matchesPattern(msku) && isValid(msku)) {
                return msku;
            }
        }
        if (matchesPattern(coerced) && isValid(coerced))
            return coerced;

        String serialAndCheck = coerced.substring(4);
        for (int i = 0; i < serialAndCheck.length(); i++) {
            char d = serialAndCheck.charAt(i);
            if (d < '0' || d > '9')
                return null;
        }

        char[][] choices = new char[4][];
        for (int i = 0; i < 3; i++) {
            char s = source.charAt(i);
            char c = coerced.charAt(i);
            if (s == '0' || s == 'O') {
                choices[i] = new char[]{'O', 'Q', 'D'};
            } else if (c >= 'A' && c <= 'Z') {
                choices[i] = new char[]{c};
            } else {
                return null;
            }
        }
        char s3 = source.charAt(3);
        char c3 = coerced.charAt(3);
        if (isCategory(c3)) {
            choices[3] = new char[]{c3};
        } else if (s3 == '0' || s3 == 'O') {
            choices[3] = new char[]{'U'};
        } else {
            return null;
        }

        for (char c0 : choices[0]) {
            for (char c1 : choices[1]) {
                for (char c2 : choices[2]) {
                    for (char cat : choices[3]) {
                        String candidate = "" + c0 + c1 + c2 + cat + serialAndCheck;
                        if (isValid(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        // Single-character BIC owner-prefix optical confusion validated against an optically read 11th check digit
        if (isCategory(c3) && Character.isDigit(source.charAt(10))) {
            List<String> bicMatches = new ArrayList<>(2);
            for (int pos = 0; pos < 3; pos++) {
                char orig = coerced.charAt(pos);
                for (char nb : ownerGlyphNeighbors(orig)) {
                    String prefix4 = coerced.substring(0, pos) + nb + coerced.substring(pos + 1, 4);
                    if (KNOWN_BIC_PREFIXES.contains(prefix4)) {
                        String candidate = prefix4 + serialAndCheck;
                        if (isValid(candidate) && !bicMatches.contains(candidate)) {
                            bicMatches.add(candidate);
                        }
                    }
                }
            }
            if (bicMatches.size() == 1) {
                return bicMatches.get(0);
            }
        }
        return null;
    }

    private static char[] ownerGlyphNeighbors(char c) {
        return switch (c) {
            case 'C' -> new char[]{'G', 'D'};
            case 'G' -> new char[]{'C', 'O', 'Q'};
            case 'D' -> new char[]{'O', 'C', 'B', 'Q'};
            case 'O' -> new char[]{'D', 'Q', 'G', 'C'};
            case 'Q' -> new char[]{'O', 'G', 'D'};
            case 'A' -> new char[]{'R', 'H', 'M'};
            case 'R' -> new char[]{'A', 'P', 'K', 'B'};
            case 'M' -> new char[]{'N', 'W', 'H'};
            case 'N' -> new char[]{'M', 'H', 'W'};
            case 'W' -> new char[]{'M', 'N'};
            case 'E' -> new char[]{'G', 'F', 'C', 'B'};
            case 'I' -> new char[]{'L', 'T', 'U', 'J'};
            case 'L' -> new char[]{'T', 'I', 'U', 'J'};
            case 'H' -> new char[]{'N', 'M', 'K'};
            case 'B' -> new char[]{'D', 'P', 'R', 'G'};
            case 'P' -> new char[]{'R', 'B', 'F'};
            default -> new char[0];
        };
    }

    private static char digitToLetter(char c) {
        return switch (c) {
            case '0' -> 'O';
            case '1' -> 'I';
            case '2' -> 'Z';
            case '5' -> 'S';
            case '6' -> 'G';
            case '8' -> 'B';
            default -> c;
        };
    }

    private static char letterToDigit(char c) {
        return switch (c) {
            case 'O', 'Q', 'D' -> '0';
            case 'I', 'L' -> '1';
            case 'Z' -> '2';
            case 'S' -> '5';
            case 'G' -> '6';
            case 'B' -> '8';
            default -> c;
        };
    }

    private static boolean isCategory(char c) {
        return c == 'U' || c == 'J' || c == 'Z' || c == 'R';
    }
}
