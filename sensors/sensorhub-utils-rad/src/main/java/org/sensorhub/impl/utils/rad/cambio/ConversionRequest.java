package org.sensorhub.impl.utils.rad.cambio;

import gov.sandia.specutils.ParserType;
import gov.sandia.specutils.SaveSpectrumAsType;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class ConversionRequest {

    private File inputFile;
    private InputStream inputStream;
    private String inputFilename;
    private ParserType inputFormat;
    private SaveSpectrumAsType outputFormat;
    private File outputFile;
    private List<Integer> sampleNumbers;
    private List<Integer> detectorNumbers;

    private ConversionRequest() {}

    public File getInputFile() {
        return inputFile;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getInputFilename() {
        return inputFilename;
    }

    public ParserType getInputFormat() {
        return inputFormat;
    }

    public SaveSpectrumAsType getOutputFormat() {
        return outputFormat;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public List<Integer> getSampleNumbers() {
        return sampleNumbers;
    }

    public List<Integer> getDetectorNumbers() {
        return detectorNumbers;
    }

    public static class Builder {

        private final ConversionRequest instance;

        public Builder() {
            this.instance = new ConversionRequest();
            this.instance.inputFormat = ParserType.Auto;
            this.instance.outputFormat = SaveSpectrumAsType.N42_2012;
        }

        public Builder inputFile(File inputFile) {
            this.instance.inputFile = inputFile;
            return this;
        }

        public Builder inputFile(String inputFilePath) {
            this.instance.inputFile = new File(inputFilePath);
            return this;
        }

        public Builder inputStream(InputStream inputStream, String filename) {
            this.instance.inputStream = inputStream;
            this.instance.inputFilename = filename;
            return this;
        }

        public Builder inputFormat(ParserType inputFormat) {
            this.instance.inputFormat = inputFormat;
            return this;
        }

        public Builder outputFormat(SaveSpectrumAsType outputFormat) {
            this.instance.outputFormat = outputFormat;
            return this;
        }

        public Builder outputFile(File outputFile) {
            this.instance.outputFile = outputFile;
            return this;
        }

        public Builder outputFile(String outputFilePath) {
            this.instance.outputFile = new File(outputFilePath);
            return this;
        }

        public Builder sampleNumbers(List<Integer> sampleNumbers) {
            this.instance.sampleNumbers = sampleNumbers;
            return this;
        }

        public Builder detectorNumbers(List<Integer> detectorNumbers) {
            this.instance.detectorNumbers = detectorNumbers;
            return this;
        }

        public ConversionRequest build() {
            if (this.instance.inputFile == null && this.instance.inputStream == null) {
                throw new IllegalStateException("Either inputFile or inputStream is required");
            }
            if (this.instance.inputFile != null && this.instance.inputStream != null) {
                throw new IllegalStateException("Cannot specify both inputFile and inputStream");
            }
            if (this.instance.outputFormat == null) {
                throw new IllegalStateException("outputFormat is required");
            }
            return this.instance;
        }
    }
}
