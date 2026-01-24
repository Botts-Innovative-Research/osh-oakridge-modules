package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.cambio.*;
import gov.sandia.specutils.ParserType;
import gov.sandia.specutils.SaveSpectrumAsType;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.*;

public class CambioTests {

    private CambioConverter converter;

    @Before
    public void setup() {
        // You can set -Djava.library.path when running tests
        converter = new CambioConverter();
    }

    @Test
    public void testGetSupportedFormats() {
        var inputFormats = converter.getSupportedInputFormats();
        var outputFormats = converter.getSupportedOutputFormats();

        assertFalse("Should have input formats", inputFormats.isEmpty());
        assertFalse("Should have output formats", outputFormats.isEmpty());

        System.out.println("Supported input formats: " + inputFormats);
        System.out.println("Supported output formats: " + outputFormats);

        assertTrue(inputFormats.contains("N42_2006"));
        assertTrue(inputFormats.contains("N42_2012"));
        assertTrue(inputFormats.contains("Pcf"));
        assertTrue(inputFormats.contains("Chn"));

        assertTrue(outputFormats.contains("N42_2012"));
        assertTrue(outputFormats.contains("Csv"));
    }

    @Test
    public void testReadSpectrumInfo() throws Exception {
        URL resource = getClass().getClassLoader().getResource("webid/n42/Annex_E.n42");
        assertNotNull("Test file should exist", resource);

        File testFile = new File(resource.toURI());
        SpectrumInfo info = converter.readSpectrumInfo(testFile);

        assertNotNull(info);
        System.out.println("UUID: " + info.getUuid());
        System.out.println("Instrument Type: " + info.getInstrumentType());
        System.out.println("Manufacturer: " + info.getManufacturer());
        System.out.println("Model: " + info.getInstrumentModel());
        System.out.println("Detector Type: " + info.getDetectorType());
        System.out.println("Num Measurements: " + info.getNumMeasurements());
        System.out.println("Gamma Live Time: " + info.getGammaLiveTime());
        System.out.println("Gamma Count Sum: " + info.getGammaCountSum());
        System.out.println("Detector Names: " + info.getDetectorNames());

        assertTrue(info.getNumMeasurements() > 0);
    }

    @Test
    public void testReadSpectrumInfoFromStream() throws Exception {
        URL resource = getClass().getClassLoader().getResource("webid/n42/Annex_E.n42");
        assertNotNull("Test file should exist", resource);

        try (InputStream is = resource.openStream()) {
            SpectrumInfo info = converter.readSpectrumInfo(is, "Annex_E.n42");

            assertNotNull(info);
            assertTrue(info.getNumMeasurements() > 0);
        }
    }

    @Test
    public void testConvertN42ToCsv() throws Exception {
        URL resource = getClass().getClassLoader().getResource("webid/n42/Annex_E.n42");
        assertNotNull("Test file should exist", resource);

        File testFile = new File(resource.toURI());

        ConversionRequest request = new ConversionRequest.Builder()
                .inputFile(testFile)
                .inputFormat(ParserType.Auto)
                .outputFormat(SaveSpectrumAsType.Csv)
                .build();

        ConversionResult result = converter.convert(request);

        assertTrue("Conversion should succeed", result.isSuccessful());
        assertNotNull( "Should have output bytes", result.getOutputBytes());
        assertNotNull("Should have spectrum info", result.getSpectrumInfo());

        String csvContent = new String(result.getOutputBytes());
        System.out.println("CSV output (first 500 chars):");
        System.out.println(csvContent.substring(0, Math.min(500, csvContent.length())));
    }

    @Test
    public void testConvertN42ToPcf() throws Exception {
        URL resource = getClass().getClassLoader().getResource("webid/n42/Annex_E.n42");

        File testFile = new File(resource.toURI());
        File outputFile = File.createTempFile("test_output_", ".pcf");
        outputFile.delete(); // SpecUtils won't overwrite existing files
        outputFile.deleteOnExit();

        ConversionRequest request = new ConversionRequest.Builder()
                .inputFile(testFile)
                .inputFormat(ParserType.Auto)
                .outputFormat(SaveSpectrumAsType.Pcf)
                .outputFile(outputFile)
                .build();

        ConversionResult result = converter.convert(request);

        assertTrue("Conversion should succeed", result.isSuccessful());
        assertTrue("Output file should exist", outputFile.exists());
        assertTrue("Output file should not be empty", outputFile.length() > 0);

        System.out.println("PCF output written to: " + outputFile.getAbsolutePath());
        System.out.println("File size: " + outputFile.length() + " bytes");
    }

    @Test
    public void testConvertToN42Shortcut() throws Exception {
        URL resource = getClass().getClassLoader().getResource("webid/n42/Annex_E.n42");
        assertNotNull("Test file should exist", resource);

        File testFile = new File(resource.toURI());
        byte[] n42Bytes = converter.convertToN42(testFile);

        assertNotNull(n42Bytes);
        assertTrue(n42Bytes.length > 0);

        String n42Content = new String(n42Bytes);
        assertTrue("Output should be N42 XML", n42Content.contains("<?xml") || n42Content.contains("<RadInstrumentData"));
    }

    @Test
    public void testConvertToHtmlVisualization() throws Exception {
        URL resource = getClass().getClassLoader().getResource("webid/n42/Annex_E.n42");
        assertNotNull("Test file should exist", resource);

        File testFile = new File(resource.toURI());

        ConversionRequest request = new ConversionRequest.Builder()
                .inputFile(testFile)
                .outputFormat(SaveSpectrumAsType.HtmlD3)
                .build();

        ConversionResult result = converter.convert(request);

        assertTrue("Conversion should succeed", result.isSuccessful());
        assertNotNull(result.getOutputBytes());

        String htmlContent = new String(result.getOutputBytes());
        assertTrue("Output should be HTML", htmlContent.contains("<html") || htmlContent.contains("<!DOCTYPE"));

        System.out.println("Generated D3.js HTML visualization");
    }

    @Test
    public void testConversionWithWarnings() throws Exception {
        URL resource = getClass().getClassLoader().getResource("webid/n42/Annex_E.n42");
        assertNotNull("Test file should exist", resource);

        File testFile = new File(resource.toURI());

        ConversionRequest request = new ConversionRequest.Builder()
                .inputFile(testFile)
                .outputFormat(SaveSpectrumAsType.N42_2012)
                .build();

        ConversionResult result = converter.convert(request);

        assertTrue(result.isSuccessful());
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            System.out.println("Parse warnings:");
            for (String warning : result.getWarnings()) {
                System.out.println("  - " + warning);
            }
        }
    }
}
