package org.sensorhub.impl.utils.rad.cambio;

import gov.sandia.specutils.SaveSpectrumAsType;

import java.io.File;
import java.util.List;

public class ConversionResult {

    private boolean successful;
    private File outputFile;
    private byte[] outputBytes;
    private SaveSpectrumAsType outputFormat;
    private SpectrumInfo spectrumInfo;
    private String errorMessage;
    private List<String> warnings;

    private ConversionResult() {}

    public boolean isSuccessful() {
        return successful;
    }

    public boolean hasError() {
        return !successful;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public byte[] getOutputBytes() {
        return outputBytes;
    }

    public SaveSpectrumAsType getOutputFormat() {
        return outputFormat;
    }

    public SpectrumInfo getSpectrumInfo() {
        return spectrumInfo;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public static class Builder {

        private final ConversionResult instance;

        public Builder() {
            this.instance = new ConversionResult();
        }

        public Builder successful(boolean successful) {
            this.instance.successful = successful;
            return this;
        }

        public Builder outputFile(File outputFile) {
            this.instance.outputFile = outputFile;
            return this;
        }

        public Builder outputBytes(byte[] outputBytes) {
            this.instance.outputBytes = outputBytes;
            return this;
        }

        public Builder outputFormat(SaveSpectrumAsType outputFormat) {
            this.instance.outputFormat = outputFormat;
            return this;
        }

        public Builder spectrumInfo(SpectrumInfo spectrumInfo) {
            this.instance.spectrumInfo = spectrumInfo;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.instance.errorMessage = errorMessage;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.instance.warnings = warnings;
            return this;
        }

        public ConversionResult build() {
            return this.instance;
        }
    }
}
