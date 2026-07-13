package com.botts.impl.ocr.onnx;

import com.botts.impl.ocr.api.OcrRead;
import com.botts.impl.ocr.pipeline.CandidateAggregator;
import com.botts.impl.ocr.pipeline.OcrSettings;
import com.botts.impl.ocr.text.Iso6346;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Golden tests against the real ONNX models. Skipped when the model directory
 * is absent (e.g. bare CI checkout); on the build node the models live in
 * dist/models/ocr. Override with -DocrModelDir=/path/to/models.
 */
public class OnnxOcrEngineTest {

    // compact print form; the widely-spaced boxed check digit variant is
    // exercised via CandidateAggregator's line-concatenation unit tests and
    // belongs to the real-imagery accuracy spike
    static final String CONTAINER_NUMBER = "CSQU 3054383";
    static final String PLATE_NUMBER = "XYZ 7890";

    static OnnxOcrEngine engine;

    @BeforeClass
    public static void setUp() throws Exception {
        Path modelDir = Paths.get(System.getProperty("ocrModelDir", "../../../../dist/models/ocr"));
        assumeTrue("OCR models not present, skipping golden tests",
                Files.isReadable(modelDir.resolve(OnnxOcrEngine.DET_MODEL)));

        engine = new OnnxOcrEngine();
        engine.init(modelDir);
    }

    @AfterClass
    public static void tearDown() {
        if (engine != null)
            engine.close();
    }

    @Test
    public void readsRenderedContainerNumber() throws Exception {
        BufferedImage image = renderText(CONTAINER_NUMBER, 900, 220, Color.WHITE, new Color(0x37, 0x41, 0x51));

        List<OcrRead> reads = engine.read(image);
        assertFalse("engine found no text at all", reads.isEmpty());

        String allText = Iso6346.normalize(String.join(" ", reads.stream().map(r -> r.text).toList()));
        Iso6346.Match match = Iso6346.scan(allText);
        assertNotNull("no container number found in: " + reads, match);
        assertEquals("CSQU3054383", match.value);
        assertTrue("check digit failed for read: " + match.value, match.checksumValid);
    }

    @Test
    public void readsRenderedPlate() throws Exception {
        BufferedImage image = renderText(PLATE_NUMBER, 500, 200, Color.BLACK, Color.WHITE);

        List<OcrRead> reads = engine.read(image);
        assertFalse("engine found no text at all", reads.isEmpty());

        String normalized = Iso6346.normalize(String.join("", reads.stream().map(r -> r.text).toList()));
        assertTrue("expected XYZ7890 in " + reads, normalized.contains("XYZ7890"));
    }

    @Test
    public void endToEndAggregationProducesValidCandidate() throws Exception {
        BufferedImage image = renderText(CONTAINER_NUMBER, 900, 220, Color.WHITE, new Color(0x37, 0x41, 0x51));

        OcrSettings settings = new OcrSettings();
        List<CandidateAggregator.FrameRead> frameReads = engine.read(image).stream()
                .map(read -> new CandidateAggregator.FrameRead(read, image, "test-cam", Instant.now()))
                .toList();

        List<CandidateAggregator.Candidate> candidates = new CandidateAggregator(settings).aggregate(frameReads);

        assertTrue("no candidates from " + frameReads.size() + " reads",
                candidates.stream().anyMatch(c ->
                        VehicleOcrResult.ID_TYPE_CONTAINER.equals(c.idType)
                                && "CSQU3054383".equals(c.normalizedValue)
                                && c.checksumValid));
    }

    static BufferedImage renderText(String text, int width, int height, Color fg, Color bg) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(bg);
        g.fillRect(0, 0, width, height);
        g.setColor(fg);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, height / 2));
        var metrics = g.getFontMetrics();
        int x = (width - metrics.stringWidth(text)) / 2;
        int y = (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(text, x, y);
        g.dispose();
        return image;
    }
}
