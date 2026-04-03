package org.sensorhub.impl.utils.rad.cambio;

import gov.sandia.specutils.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class CambioConverter {

    static {
        SpecUtilsNativeLoader.load();
    }

    public CambioConverter() {
    }

    public Set<String> getSupportedInputFormats() {
        Set<String> formats = new LinkedHashSet<>();
        formats.add("n42");
        formats.add("N42_2006");
        formats.add("N42_2012");
        formats.add("Spc");
        formats.add("Exploranium");
        formats.add("Pcf");
        formats.add("Chn");
        formats.add("SpeIaea");
        formats.add("TxtOrCsv");
        formats.add("Cnf");
        formats.add("TracsMps");
        formats.add("Aram");
        formats.add("SPMDailyFile");
        formats.add("AmptekMca");
        formats.add("MicroRaider");
        formats.add("RadiaCode");
        formats.add("OrtecListMode");
        formats.add("LsrmSpe");
        formats.add("Tka");
        formats.add("MultiAct");
        formats.add("Phd");
        formats.add("Lzs");
        formats.add("ScanDataXml");
        formats.add("Json");
        formats.add("CaenHexagonGXml");
        return formats;
    }

    public Set<String> getSupportedOutputFormats() {
        Set<String> formats = new LinkedHashSet<>();
        formats.add("Txt");
        formats.add("Csv");
        formats.add("Pcf");
        formats.add("n42");
        formats.add("N42_2006");
        formats.add("N42_2012");
        formats.add("Chn");
        formats.add("SpcBinaryInt");
        formats.add("SpcBinaryFloat");
        formats.add("SpcAscii");
        formats.add("ExploraniumGr130v0");
        formats.add("ExploraniumGr135v2");
        formats.add("SpeIaea");
        formats.add("Cnf");
        formats.add("Tka");
        formats.add("HtmlD3");
        return formats;
    }

    public SpectrumInfo readSpectrumInfo(File inputFile) throws CambioException {
        return readSpectrumInfo(inputFile, ParserType.Auto);
    }

    public SpectrumInfo readSpectrumInfo(File inputFile, ParserType parserType) throws CambioException {
        SpecFile specFile = new SpecFile();
        try {
            if (!specFile.load_file(inputFile.getAbsolutePath(), parserType, "")) {
                throw new CambioException("Failed to load spectrum file: " + inputFile.getAbsolutePath());
            }
            return extractSpectrumInfo(specFile, inputFile.getName());
        } finally {
            specFile.delete();
        }
    }

    public SpectrumInfo readSpectrumInfo(InputStream inputStream, String filename) throws CambioException {
        return readSpectrumInfo(inputStream, filename, ParserType.Auto);
    }

    public SpectrumInfo readSpectrumInfo(InputStream inputStream, String filename, ParserType parserType) throws CambioException {
        File tempFile = null;
        try {
            tempFile = createTempFile(inputStream, filename);
            return readSpectrumInfo(tempFile, parserType);
        } catch (IOException e) {
            throw new CambioException("Failed to read spectrum from stream", e);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    public ConversionResult convert(ConversionRequest request) throws CambioException {
        File tempInputFile = null;
        SpecFile specFile = new SpecFile();

        try {
            File inputFile;
            if (request.getInputFile() != null) {
                inputFile = request.getInputFile();
            } else {
                tempInputFile = createTempFile(request.getInputStream(), request.getInputFilename());
                inputFile = tempInputFile;
            }

            if (!specFile.load_file(inputFile.getAbsolutePath(), request.getInputFormat(), "")) {
                return new ConversionResult.Builder()
                        .successful(false)
                        .errorMessage("Failed to load spectrum file: " + inputFile.getAbsolutePath())
                        .outputFormat(request.getOutputFormat())
                        .build();
            }

            SpectrumInfo spectrumInfo = extractSpectrumInfo(specFile, inputFile.getName());
            List<String> warnings = extractWarnings(specFile);

            if (request.getOutputFile() != null) {
                writeToFile(specFile, request);
                return new ConversionResult.Builder()
                        .successful(true)
                        .outputFile(request.getOutputFile())
                        .outputFormat(request.getOutputFormat())
                        .spectrumInfo(spectrumInfo)
                        .warnings(warnings)
                        .build();
            } else {
                byte[] outputBytes = writeToBytes(specFile, request);
                return new ConversionResult.Builder()
                        .successful(true)
                        .outputBytes(outputBytes)
                        .outputFormat(request.getOutputFormat())
                        .spectrumInfo(spectrumInfo)
                        .warnings(warnings)
                        .build();
            }
        } catch (Exception e) {
            throw new CambioException("Conversion failed: " + e.getMessage(), e);
        } finally {
            specFile.delete();
            deleteTempFile(tempInputFile);
        }
    }

    public byte[] convertToN42(File inputFile) throws CambioException {
        return convertToN42(inputFile, ParserType.Auto);
    }

    public byte[] convertToN42(File inputFile, ParserType inputFormat) throws CambioException {
        ConversionRequest request = new ConversionRequest.Builder()
                .inputFile(inputFile)
                .inputFormat(inputFormat)
                .outputFormat(SaveSpectrumAsType.N42_2012)
                .build();

        ConversionResult result = convert(request);
        if (!result.isSuccessful()) {
            throw new CambioException("Conversion to N42 failed: " + result.getErrorMessage());
        }
        return result.getOutputBytes();
    }

    public byte[] convertToN42(InputStream inputStream, String filename) throws CambioException {
        return convertToN42(inputStream, filename, ParserType.Auto);
    }

    public byte[] convertToN42(InputStream inputStream, String filename, ParserType inputFormat) throws CambioException {
        ConversionRequest request = new ConversionRequest.Builder()
                .inputStream(inputStream, filename)
                .inputFormat(inputFormat)
                .outputFormat(SaveSpectrumAsType.N42_2012)
                .build();

        ConversionResult result = convert(request);
        if (!result.isSuccessful()) {
            throw new CambioException("Conversion to N42 failed: " + result.getErrorMessage());
        }
        return result.getOutputBytes();
    }

    public void convertToN42File(File inputFile, File outputFile) throws CambioException {
        convertToN42File(inputFile, outputFile, ParserType.Auto);
    }

    public void convertToN42File(File inputFile, File outputFile, ParserType inputFormat) throws CambioException {
        ConversionRequest request = new ConversionRequest.Builder()
                .inputFile(inputFile)
                .inputFormat(inputFormat)
                .outputFormat(SaveSpectrumAsType.N42_2012)
                .outputFile(outputFile)
                .build();

        ConversionResult result = convert(request);
        if (!result.isSuccessful()) {
            throw new CambioException("Conversion to N42 file failed: " + result.getErrorMessage());
        }
    }

    private SpectrumInfo extractSpectrumInfo(SpecFile specFile, String filename) {
        List<String> detectorNames = new ArrayList<>();
        StringVector names = specFile.detector_names();
        for (int i = 0; i < names.size(); i++) {
            detectorNames.add(names.get(i));
        }

        List<Integer> detectorNumbers = new ArrayList<>();
        IntVector numbers = specFile.detector_numbers();
        for (int i = 0; i < numbers.size(); i++) {
            detectorNumbers.add(numbers.get(i));
        }

        List<String> remarks = new ArrayList<>();
        StringVector remarksVector = specFile.remarks();
        for (int i = 0; i < remarksVector.size(); i++) {
            remarks.add(remarksVector.get(i));
        }

        List<String> parseWarnings = new ArrayList<>();
        StringVector warningsVector = specFile.parse_warnings();
        for (int i = 0; i < warningsVector.size(); i++) {
            parseWarnings.add(warningsVector.get(i));
        }

        return new SpectrumInfo.Builder()
                .uuid(specFile.uuid())
                .filename(filename)
                .instrumentType(specFile.instrument_type())
                .manufacturer(specFile.manufacturer())
                .instrumentModel(specFile.instrument_model())
                .instrumentId(specFile.instrument_id())
                .detectorType(specFile.detector_type().toString())
                .locationName(specFile.measurement_location_name())
                .latitude(specFile.mean_latitude())
                .longitude(specFile.mean_longitude())
                .hasGpsInfo(specFile.has_gps_info())
                .gammaLiveTime(specFile.gamma_live_time())
                .gammaRealTime(specFile.gamma_real_time())
                .gammaCountSum(specFile.gamma_count_sum())
                .neutronCountSum(specFile.neutron_counts_sum())
                .containsNeutrons(specFile.contained_neutron())
                .numMeasurements(specFile.num_measurements())
                .numGammaChannels(specFile.num_gamma_channels())
                .detectorNames(detectorNames)
                .detectorNumbers(detectorNumbers)
                .remarks(remarks)
                .parseWarnings(parseWarnings)
                .build();
    }

    private List<String> extractWarnings(SpecFile specFile) {
        List<String> warnings = new ArrayList<>();
        StringVector warningsVector = specFile.parse_warnings();
        for (int i = 0; i < warningsVector.size(); i++) {
            warnings.add(warningsVector.get(i));
        }
        return warnings;
    }

    private void writeToFile(SpecFile specFile, ConversionRequest request) throws CambioException {
        try {
            if (request.getSampleNumbers() != null && request.getDetectorNumbers() != null) {
                IntVector sampleNums = new IntVector();
                for (Integer num : request.getSampleNumbers()) {
                    sampleNums.add(num);
                }
                IntVector detNums = new IntVector();
                for (Integer num : request.getDetectorNumbers()) {
                    detNums.add(num);
                }
                specFile.write_to_file(request.getOutputFile().getAbsolutePath(), sampleNums, detNums, request.getOutputFormat());
            } else {
                specFile.write_to_file(request.getOutputFile().getAbsolutePath(), request.getOutputFormat());
            }
        } catch (Exception e) {
            throw new CambioException("Failed to write output file: " + e.getMessage(), e);
        }
    }

    private byte[] writeToBytes(SpecFile specFile, ConversionRequest request) throws CambioException {
        File tempOutputFile = null;
        try {
            String extension = getExtensionForFormat(request.getOutputFormat());
            tempOutputFile = File.createTempFile("cambio_output_", extension);
            // SpecUtils won't overwrite existing files, so delete the temp file first
            tempOutputFile.delete();

            if (request.getSampleNumbers() != null && request.getDetectorNumbers() != null) {
                IntVector sampleNums = new IntVector();
                for (Integer num : request.getSampleNumbers()) {
                    sampleNums.add(num);
                }
                IntVector detNums = new IntVector();
                for (Integer num : request.getDetectorNumbers()) {
                    detNums.add(num);
                }
                specFile.write_to_file(tempOutputFile.getAbsolutePath(), sampleNums, detNums, request.getOutputFormat());
            } else {
                specFile.write_to_file(tempOutputFile.getAbsolutePath(), request.getOutputFormat());
            }

            return Files.readAllBytes(tempOutputFile.toPath());
        } catch (IOException e) {
            throw new CambioException("Failed to write output to bytes: " + e.getMessage(), e);
        } finally {
            deleteTempFile(tempOutputFile);
        }
    }

    private String getExtensionForFormat(SaveSpectrumAsType format) {
        if (format == SaveSpectrumAsType.N42_2006 || format == SaveSpectrumAsType.N42_2012) {
            return ".n42";
        } else if (format == SaveSpectrumAsType.Pcf) {
            return ".pcf";
        } else if (format == SaveSpectrumAsType.Csv) {
            return ".csv";
        } else if (format == SaveSpectrumAsType.Txt) {
            return ".txt";
        } else if (format == SaveSpectrumAsType.Chn) {
            return ".chn";
        } else if (format == SaveSpectrumAsType.SpcBinaryInt || format == SaveSpectrumAsType.SpcBinaryFloat || format == SaveSpectrumAsType.SpcAscii) {
            return ".spc";
        } else if (format == SaveSpectrumAsType.SpeIaea) {
            return ".spe";
        } else if (format == SaveSpectrumAsType.Cnf) {
            return ".cnf";
        } else if (format == SaveSpectrumAsType.Tka) {
            return ".tka";
        } else if (format == SaveSpectrumAsType.HtmlD3) {
            return ".html";
        } else {
            return ".dat";
        }
    }

    private File createTempFile(InputStream inputStream, String filename) throws IOException {
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."));
        }
        File tempFile = File.createTempFile("cambio_input_", extension);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    private void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
}
