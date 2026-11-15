/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.reports.helpers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Vector PDF layout engine backed by Apache PDFBox 3 with embedded subsetted Noto Sans fonts.
 * Supports mixed Portrait and Landscape pages, dashboard KPI cards, pagination, and footers.
 */
public class PdfDocumentBuilder implements Closeable {

    public enum Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    public record KpiCard(String label, String value, String sublabel, Color accentColor) {
        public KpiCard(String label, String value, Color accentColor) {
            this(label, value, null, accentColor);
        }
    }

    public record HeaderMetadata(
            String reportTitle,
            String siteOrNodeId,
            String eventTypeLine,
            String scopeSummary,
            Instant startUtc,
            Instant endUtc
    ) {}

    public static final float MARGIN_X = 36f;
    public static final float MARGIN_TOP = 34f;
    public static final float MARGIN_BOTTOM = 42f;
    private static final float FOOTER_RULE_Y = 28f;
    private static final float FOOTER_TEXT_Y = 17f;

    // Accessible dashboard color tokens
    public static final Color NAVY_HEADER = new Color(15, 23, 42);       // #0F172A
    public static final Color SLATE_900 = new Color(15, 23, 42);         // #0F172A
    public static final Color SLATE_700 = new Color(51, 65, 85);         // #334155
    public static final Color SLATE_600 = new Color(71, 85, 105);        // #475569
    public static final Color SLATE_500 = new Color(100, 116, 139);      // #64748B
    public static final Color SLATE_400 = new Color(148, 163, 184);      // #94A3B8
    public static final Color SLATE_300 = new Color(203, 213, 225);      // #CBD5E1
    public static final Color SLATE_200 = new Color(226, 232, 240);      // #E2E8F0
    public static final Color SLATE_100 = new Color(241, 245, 249);      // #F1F5F9
    public static final Color SLATE_50 = new Color(248, 250, 252);       // #F8FAFC

    public static final Color ACCENT_BLUE = new Color(37, 99, 235);      // #2563EB
    public static final Color ACCENT_SKY = new Color(2, 132, 199);       // #0284C7
    public static final Color ACCENT_RED = new Color(220, 38, 38);       // #DC2626
    public static final Color ACCENT_PURPLE = new Color(124, 58, 237);   // #7C3AED
    public static final Color ACCENT_TEAL = new Color(13, 148, 136);     // #0D9488
    public static final Color ACCENT_GREEN = new Color(22, 163, 74);     // #16A34A
    public static final Color ACCENT_AMBER = new Color(217, 119, 6);     // #D97706

    private static final String REGULAR_FONT_RESOURCE = "/com/botts/impl/service/oscar/reports/fonts/NotoSans-Regular.ttf";
    private static final String BOLD_FONT_RESOURCE = "/com/botts/impl/service/oscar/reports/fonts/NotoSans-Bold.ttf";

    public static final DateTimeFormatter UTC_TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC);

    private final PDDocument document;
    private final PDType0Font regularFont;
    private final PDType0Font boldFont;
    private final Map<Integer, Boolean> regularGlyphCache = new HashMap<>();
    private final Map<Integer, Boolean> boldGlyphCache = new HashMap<>();

    private final String reportTitle;
    private final String nodeId;
    private final Instant generatedAt;

    private PDPage currentPage;
    private PDPageContentStream contentStream;
    private Orientation currentOrientation = Orientation.PORTRAIT;
    private float pageWidth;
    private float pageHeight;
    private float cursorY;
    private boolean closed = false;

    public PdfDocumentBuilder(String reportTitle, String nodeId) throws IOException {
        this(reportTitle, nodeId, Orientation.PORTRAIT);
    }

    public PdfDocumentBuilder(String reportTitle, String nodeId, Orientation initialOrientation) throws IOException {
        this.reportTitle = reportTitle != null ? reportTitle : "OSCAR Report";
        this.nodeId = (nodeId != null && !nodeId.isBlank()) ? nodeId : "oscar-node";
        this.generatedAt = Instant.now();
        this.document = new PDDocument();

        PDDocumentInformation info = this.document.getDocumentInformation();
        info.setTitle(this.reportTitle);
        info.setCreator("OSCAR Service Module");
        info.setProducer("Apache PDFBox 3 / Noto Sans");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        cal.setTimeInMillis(this.generatedAt.toEpochMilli());
        info.setCreationDate(cal);

        this.regularFont = loadFontResource(REGULAR_FONT_RESOURCE);
        this.boldFont = loadFontResource(BOLD_FONT_RESOURCE);

        startNewPage(initialOrientation);
    }

    private PDType0Font loadFontResource(String path) throws IOException {
        try (InputStream in = PdfDocumentBuilder.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Missing bundled report font resource: " + path);
            }
            return PDType0Font.load(document, in, true);
        }
    }

    public PDDocument getDocument() {
        return document;
    }

    public PDType0Font getRegularFont() {
        return regularFont;
    }

    public PDType0Font getBoldFont() {
        return boldFont;
    }

    public PDPageContentStream getContentStream() {
        return contentStream;
    }

    public Orientation getCurrentOrientation() {
        return currentOrientation;
    }

    public float getPageWidth() {
        return pageWidth;
    }

    public float getPageHeight() {
        return pageHeight;
    }

    public float getContentWidth() {
        return pageWidth - 2f * MARGIN_X;
    }

    public float getCursorY() {
        return cursorY;
    }

    public void setCursorY(float cursorY) {
        this.cursorY = cursorY;
    }

    public void moveCursorDown(float delta) {
        this.cursorY -= delta;
    }

    public float getBottomLimitY() {
        return MARGIN_BOTTOM;
    }

    public float getAvailableHeight() {
        return cursorY - MARGIN_BOTTOM;
    }

    public void startNewPage() throws IOException {
        startNewPage(this.currentOrientation);
    }

    public void startNewPage(Orientation orientation) throws IOException {
        if (contentStream != null) {
            contentStream.close();
            contentStream = null;
        }
        this.currentOrientation = orientation;
        PDRectangle mediaBox = orientation == Orientation.LANDSCAPE
                ? new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth())
                : PDRectangle.LETTER;
        this.pageWidth = mediaBox.getWidth();
        this.pageHeight = mediaBox.getHeight();
        this.currentPage = new PDPage(mediaBox);
        this.document.addPage(this.currentPage);
        this.contentStream = new PDPageContentStream(this.document, this.currentPage);
        this.cursorY = this.pageHeight - MARGIN_TOP;
    }

    public void ensureSpace(float neededHeight) throws IOException {
        if (cursorY - neededHeight < MARGIN_BOTTOM) {
            startNewPage(this.currentOrientation);
        }
    }

    public void switchOrientationIfNeeded(Orientation desiredOrientation, float minHeightNeeded) throws IOException {
        if (this.currentOrientation != desiredOrientation) {
            startNewPage(desiredOrientation);
        } else {
            ensureSpace(minHeightNeeded);
        }
    }

    /**
     * Renders the dark-navy top report header banner with full metadata and exact report title.
     */
    public void drawReportBanner(HeaderMetadata meta) throws IOException {
        float width = getContentWidth();
        float innerWidth = width - 28f;

        List<String> metaLines = new ArrayList<>();
        if (meta.siteOrNodeId() != null && !meta.siteOrNodeId().isBlank()) {
            metaLines.add("Site ID: " + meta.siteOrNodeId());
        }
        if (meta.eventTypeLine() != null && !meta.eventTypeLine().isBlank()) {
            metaLines.add(meta.eventTypeLine());
        }
        if (meta.scopeSummary() != null && !meta.scopeSummary().isBlank()) {
            metaLines.addAll(wrapText(regularFont, 8.5f, meta.scopeSummary(), innerWidth));
        }
        if (meta.startUtc() != null && meta.endUtc() != null) {
            String windowLine = "Requested Window: " + UTC_TIMESTAMP_FMT.format(meta.startUtc())
                    + "  to  " + UTC_TIMESTAMP_FMT.format(meta.endUtc())
                    + " (" + formatDurationSummary(meta.startUtc(), meta.endUtc()) + ")";
            metaLines.addAll(wrapText(regularFont, 8.5f, windowLine, innerWidth));
        }

        float lineStep = 12f;
        float bannerHeight = 36f + metaLines.size() * lineStep + 10f;
        ensureSpace(bannerHeight + 10f);

        float topY = cursorY;
        float bottomY = topY - bannerHeight;

        // Main dark navy banner fill
        contentStream.setNonStrokingColor(NAVY_HEADER);
        contentStream.addRect(MARGIN_X, bottomY, width, bannerHeight);
        contentStream.fill();

        // Left royal-blue accent strip
        contentStream.setNonStrokingColor(ACCENT_BLUE);
        contentStream.addRect(MARGIN_X, bottomY, 4.5f, bannerHeight);
        contentStream.fill();

        // Report Title (exact string preserved for semantic extraction)
        float titleBaselineY = topY - 22f;
        drawText(boldFont, 15.5f, Color.WHITE, MARGIN_X + 14f, titleBaselineY, meta.reportTitle());

        // Top-right badge & generation timestamp
        String generatedText = "Generated: " + UTC_TIMESTAMP_FMT.format(generatedAt);
        float genWidth = measureTextWidth(regularFont, 8f, generatedText);
        drawText(regularFont, 8f, SLATE_300, MARGIN_X + width - 12f - genWidth, titleBaselineY + 1f, generatedText);

        // Subtle divider line inside banner
        float dividerY = titleBaselineY - 8f;
        contentStream.setStrokingColor(new Color(30, 41, 59));
        contentStream.setLineWidth(0.6f);
        contentStream.moveTo(MARGIN_X + 14f, dividerY);
        contentStream.lineTo(MARGIN_X + width - 12f, dividerY);
        contentStream.stroke();

        // Metadata lines
        float lineY = dividerY - 12f;
        for (String line : metaLines) {
            boolean isEventType = line.startsWith("Event Type:")
                    || line.startsWith("Site ID:")
                    || line.startsWith("Vehicle ID:");
            PDType0Font lineFont = isEventType ? boldFont : regularFont;
            Color lineColor = isEventType ? Color.WHITE : SLATE_300;
            drawText(lineFont, 8.5f, lineColor, MARGIN_X + 14f, lineY, line);
            lineY -= lineStep;
        }

        cursorY = bottomY - 12f;
    }

    private static String formatDurationSummary(Instant start, Instant end) {
        long seconds = Math.max(0L, Duration.between(start, end).getSeconds());
        long days = seconds / 86_400L;
        long hours = (seconds % 86_400L) / 3_600L;
        if (days > 0 && hours == 0) {
            return days == 1 ? "1 day" : days + " days";
        }
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours == 1 ? "1 hour" : hours + " hours";
        }
        long mins = Math.max(1L, seconds / 60L);
        return mins + " min";
    }

    /**
     * Renders a horizontal row of dashboard KPI summary cards.
     */
    public void drawKpiCards(List<KpiCard> cards) throws IOException {
        if (cards == null || cards.isEmpty()) {
            return;
        }
        float gap = 8f;
        float totalWidth = getContentWidth();
        int n = cards.size();
        float cardWidth = (totalWidth - gap * (n - 1)) / n;
        float cardHeight = 52f;

        ensureSpace(cardHeight + 12f);
        float topY = cursorY;
        float bottomY = topY - cardHeight;

        for (int i = 0; i < n; i++) {
            KpiCard card = cards.get(i);
            float x = MARGIN_X + i * (cardWidth + gap);

            // Card background & border
            contentStream.setNonStrokingColor(SLATE_50);
            contentStream.setStrokingColor(SLATE_200);
            contentStream.setLineWidth(0.75f);
            contentStream.addRect(x, bottomY, cardWidth, cardHeight);
            contentStream.fillAndStroke();

            // Top accent strip
            Color accent = card.accentColor() != null ? card.accentColor() : ACCENT_BLUE;
            contentStream.setNonStrokingColor(accent);
            contentStream.addRect(x, topY - 2.5f, cardWidth, 2.5f);
            contentStream.fill();

            float innerMaxW = cardWidth - 14f;

            // Label
            String labelText = truncateToWidth(boldFont, 7.5f, card.label(), innerMaxW);
            drawText(boldFont, 7.5f, SLATE_600, x + 7f, topY - 14f, labelText);

            // Value (auto-fit font size so large counts never clip)
            float valFontSize = 14f;
            while (valFontSize > 9f && measureTextWidth(boldFont, valFontSize, card.value()) > innerMaxW) {
                valFontSize -= 1f;
            }
            String valText = truncateToWidth(boldFont, valFontSize, card.value(), innerMaxW);
            drawText(boldFont, valFontSize, SLATE_900, x + 7f, topY - 31f, valText);

            // Optional sublabel
            if (card.sublabel() != null && !card.sublabel().isBlank()) {
                String sub = truncateToWidth(regularFont, 7.2f, card.sublabel(), innerMaxW);
                drawText(regularFont, 7.2f, SLATE_500, x + 7f, bottomY + 7f, sub);
            }
        }

        cursorY = bottomY - 14f;
    }

    /**
     * Renders a section heading with an accent bar and prevents orphan headings near page bottoms.
     */
    public void drawSectionHeader(String title, String subtitle, float minFollowingSpace) throws IOException {
        float headerHeight = (subtitle != null && !subtitle.isBlank()) ? 28f : 19f;
        ensureSpace(headerHeight + Math.max(40f, minFollowingSpace));

        float topY = cursorY;

        // Vertical accent bar
        contentStream.setNonStrokingColor(ACCENT_BLUE);
        contentStream.addRect(MARGIN_X, topY - 13f, 3f, 13f);
        contentStream.fill();

        drawText(boldFont, 11f, SLATE_900, MARGIN_X + 8f, topY - 11f, title);

        float ruleY = topY - 16f;
        if (subtitle != null && !subtitle.isBlank()) {
            String clippedSub = truncateToWidth(regularFont, 8f, subtitle, getContentWidth() - 10f);
            drawText(regularFont, 8f, SLATE_500, MARGIN_X + 8f, topY - 23f, clippedSub);
            ruleY = topY - 27f;
        }

        contentStream.setStrokingColor(SLATE_200);
        contentStream.setLineWidth(0.6f);
        contentStream.moveTo(MARGIN_X, ruleY);
        contentStream.lineTo(MARGIN_X + getContentWidth(), ruleY);
        contentStream.stroke();

        cursorY = ruleY - 8f;
    }

    public void drawSectionHeader(String title) throws IOException {
        drawSectionHeader(title, null, 60f);
    }

    /**
     * Renders an informational callout box (for empty states or explanatory notes).
     */
    public void drawCalloutBox(String message) throws IOException {
        float width = getContentWidth();
        List<String> lines = wrapText(regularFont, 8.5f, message, width - 24f);
        float lineStep = 12f;
        float boxHeight = Math.max(26f, 12f + lines.size() * lineStep);

        ensureSpace(boxHeight + 8f);
        float topY = cursorY;
        float bottomY = topY - boxHeight;

        contentStream.setNonStrokingColor(SLATE_50);
        contentStream.setStrokingColor(SLATE_300);
        contentStream.setLineWidth(0.6f);
        contentStream.addRect(MARGIN_X, bottomY, width, boxHeight);
        contentStream.fillAndStroke();

        contentStream.setNonStrokingColor(SLATE_400);
        contentStream.addRect(MARGIN_X, bottomY, 3f, boxHeight);
        contentStream.fill();

        float textY = topY - 15f;
        for (String line : lines) {
            drawText(regularFont, 8.5f, SLATE_600, MARGIN_X + 12f, textY, line);
            textY -= lineStep;
        }

        cursorY = bottomY - 10f;
    }

    /**
     * Draws a single line of sanitized text at (x, y).
     */
    public void drawText(PDType0Font font, float fontSize, Color color, float x, float y, String text) throws IOException {
        String safe = sanitizeSingleLine(font, text);
        if (safe.isEmpty()) {
            return;
        }
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(color);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(safe);
        contentStream.endText();
    }

    /**
     * Draws rotated single-line text anchored at (tx, ty) by angleRadians.
     */
    public void drawRotatedText(PDType0Font font, float fontSize, Color color,
                                float tx, float ty, double angleRadians, String text) throws IOException {
        String safe = sanitizeSingleLine(font, text);
        if (safe.isEmpty()) {
            return;
        }
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(color);
        contentStream.setTextMatrix(Matrix.getRotateInstance(angleRadians, tx, ty));
        contentStream.showText(safe);
        contentStream.endText();
    }

    /**
     * Draws a pure vector filled circle with an optional stroke.
     */
    public void drawFilledCircle(float cx, float cy, float r, Color fillColor, Color strokeColor, float strokeWidth)
            throws IOException {
        final float k = 0.55228475f * r;
        contentStream.moveTo(cx - r, cy);
        contentStream.curveTo(cx - r, cy + k, cx - k, cy + r, cx, cy + r);
        contentStream.curveTo(cx + k, cy + r, cx + r, cy + k, cx + r, cy);
        contentStream.curveTo(cx + r, cy - k, cx + k, cy - r, cx, cy - r);
        contentStream.curveTo(cx - k, cy - r, cx - r, cy - k, cx - r, cy);
        contentStream.closePath();

        if (fillColor != null && strokeColor != null && strokeWidth > 0f) {
            contentStream.setNonStrokingColor(fillColor);
            contentStream.setStrokingColor(strokeColor);
            contentStream.setLineWidth(strokeWidth);
            contentStream.fillAndStroke();
        } else if (fillColor != null) {
            contentStream.setNonStrokingColor(fillColor);
            contentStream.fill();
        } else if (strokeColor != null && strokeWidth > 0f) {
            contentStream.setStrokingColor(strokeColor);
            contentStream.setLineWidth(strokeWidth);
            contentStream.stroke();
        }
    }

    public float measureTextWidth(PDType0Font font, float fontSize, String text) {
        String safe = sanitizeSingleLine(font, text);
        if (safe.isEmpty()) {
            return 0f;
        }
        try {
            return (font.getStringWidth(safe) / 1000f) * fontSize;
        } catch (IOException e) {
            return safe.length() * fontSize * 0.52f;
        }
    }

    public String truncateToWidth(PDType0Font font, float fontSize, String text, float maxWidth) {
        String safe = sanitizeSingleLine(font, text);
        if (safe.isEmpty() || maxWidth <= 0f) {
            return "";
        }
        if (measureTextWidth(font, fontSize, safe) <= maxWidth) {
            return safe;
        }
        String ellipsis = "...";
        float ellipsisW = measureTextWidth(font, fontSize, ellipsis);
        if (ellipsisW >= maxWidth) {
            return "";
        }
        int lo = 0;
        int hi = safe.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            String candidate = safe.substring(0, mid) + ellipsis;
            if (measureTextWidth(font, fontSize, candidate) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return safe.substring(0, lo) + ellipsis;
    }

    /**
     * Wraps multi-paragraph text within maxWidth, breaking long unspaced tokens (URNs, UUIDs, paths)
     * cleanly at punctuation boundaries first and character boundaries if needed.
     */
    public List<String> wrapText(PDType0Font font, float fontSize, String rawText, float maxWidth) {
        List<String> out = new ArrayList<>();
        if (rawText == null || rawText.isEmpty()) {
            out.add("");
            return out;
        }
        float effectiveMax = Math.max(8f, maxWidth);
        String normalized = rawText.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "  ");
        String[] paragraphs = normalized.split("\n", -1);

        for (String paragraph : paragraphs) {
            String safePara = sanitizeSingleLine(font, paragraph);
            if (safePara.isEmpty()) {
                out.add("");
                continue;
            }
            String[] words = safePara.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
                if (currentLine.length() == 0) {
                    if (measureTextWidth(font, fontSize, word) <= effectiveMax) {
                        currentLine.append(word);
                    } else {
                        List<String> pieces = breakLongToken(font, fontSize, word, effectiveMax);
                        for (int i = 0; i < pieces.size() - 1; i++) {
                            out.add(pieces.get(i));
                        }
                        currentLine.append(pieces.get(pieces.size() - 1));
                    }
                } else {
                    String candidate = currentLine + " " + word;
                    if (measureTextWidth(font, fontSize, candidate) <= effectiveMax) {
                        currentLine.append(' ').append(word);
                    } else {
                        out.add(currentLine.toString());
                        currentLine.setLength(0);
                        if (measureTextWidth(font, fontSize, word) <= effectiveMax) {
                            currentLine.append(word);
                        } else {
                            List<String> pieces = breakLongToken(font, fontSize, word, effectiveMax);
                            for (int i = 0; i < pieces.size() - 1; i++) {
                                out.add(pieces.get(i));
                            }
                            currentLine.append(pieces.get(pieces.size() - 1));
                        }
                    }
                }
            }
            if (currentLine.length() > 0) {
                out.add(currentLine.toString());
            }
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
    }

    private List<String> breakLongToken(PDType0Font font, float fontSize, String token, float maxWidth) {
        List<String> chunks = new ArrayList<>();
        int idx = 0;
        int len = token.length();
        while (idx < len) {
            int fitEnd = idx + 1;
            int lastDelimiterBreak = -1;
            while (fitEnd <= len) {
                String slice = token.substring(idx, fitEnd);
                if (measureTextWidth(font, fontSize, slice) > maxWidth) {
                    break;
                }
                char lastChar = token.charAt(fitEnd - 1);
                if (lastChar == ':' || lastChar == '/' || lastChar == '-' || lastChar == '_' || lastChar == ',' || lastChar == '.') {
                    lastDelimiterBreak = fitEnd;
                }
                fitEnd++;
            }
            int chosenEnd = fitEnd - 1;
            if (chosenEnd <= idx) {
                chosenEnd = idx + 1;
            } else if (chosenEnd < len && lastDelimiterBreak > idx + 2) {
                chosenEnd = lastDelimiterBreak;
            }
            chunks.add(token.substring(idx, chosenEnd));
            idx = chosenEnd;
        }
        return chunks;
    }

    public String sanitizeSingleLine(PDType0Font font, String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Map<Integer, Boolean> glyphCache = (font == boldFont) ? boldGlyphCache : regularGlyphCache;
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); ) {
            int cp = input.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '\n' || cp == '\r' || cp == '\t') {
                sb.append(' ');
                continue;
            }
            if (cp < 0x20 || (cp >= 0x7F && cp <= 0x9F)) {
                continue;
            }
            if (cp >= 0x20 && cp <= 0x7E) {
                sb.append((char) cp);
                continue;
            }
            Boolean supported = glyphCache.get(cp);
            if (supported == null) {
                String glyphStr = new String(Character.toChars(cp));
                try {
                    font.encode(glyphStr);
                    supported = Boolean.TRUE;
                } catch (Exception e) {
                    supported = Boolean.FALSE;
                }
                glyphCache.put(cp, supported);
            }
            if (supported) {
                sb.appendCodePoint(cp);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    /**
     * Stamps "Page X of Y" footers on every page, writes the completed PDF to out, and closes the PDDocument.
     */
    public void finishAndSave(OutputStream out) throws IOException {
        if (closed) {
            return;
        }
        if (contentStream != null) {
            contentStream.close();
            contentStream = null;
        }

        int totalPages = document.getNumberOfPages();
        for (int pageIdx = 0; pageIdx < totalPages; pageIdx++) {
            PDPage page = document.getPage(pageIdx);
            float pw = page.getMediaBox().getWidth();
            try (PDPageContentStream footerStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                footerStream.setStrokingColor(SLATE_200);
                footerStream.setLineWidth(0.5f);
                footerStream.moveTo(MARGIN_X, FOOTER_RULE_Y);
                footerStream.lineTo(pw - MARGIN_X, FOOTER_RULE_Y);
                footerStream.stroke();

                String leftFooter = sanitizeSingleLine(regularFont, "OSCAR  |  " + reportTitle + "  |  Node: " + nodeId);
                String rightFooter = "Page " + (pageIdx + 1) + " of " + totalPages;
                float rightWidth = measureTextWidth(regularFont, 7.8f, rightFooter);

                footerStream.beginText();
                footerStream.setFont(regularFont, 7.8f);
                footerStream.setNonStrokingColor(SLATE_500);
                footerStream.newLineAtOffset(MARGIN_X, FOOTER_TEXT_Y);
                footerStream.showText(leftFooter);
                footerStream.endText();

                footerStream.beginText();
                footerStream.setFont(regularFont, 7.8f);
                footerStream.setNonStrokingColor(SLATE_500);
                footerStream.newLineAtOffset(pw - MARGIN_X - rightWidth, FOOTER_TEXT_Y);
                footerStream.showText(rightFooter);
                footerStream.endText();
            }
        }

        document.save(out);
        close();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (contentStream != null) {
            try {
                contentStream.close();
            } finally {
                contentStream = null;
            }
        }
        document.close();
    }
}
