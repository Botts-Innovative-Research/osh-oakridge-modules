package com.botts.impl.service.oscar.cambio;

import java.io.IOException;

public class CambioException extends IOException {

    private final String sourceFormat;
    private final String targetFormat;
    private final String filePath;

    public CambioException(String message) {
        super(message);
        this.sourceFormat = null;
        this.targetFormat = null;
        this.filePath = null;
    }

    public CambioException(String message, Throwable cause) {
        super(message, cause);
        this.sourceFormat = null;
        this.targetFormat = null;
        this.filePath = null;
    }

    public CambioException(String message, String sourceFormat, String targetFormat, String filePath) {
        super(message);
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
        this.filePath = filePath;
    }

    public CambioException(String message, String sourceFormat, String targetFormat, String filePath, Throwable cause) {
        super(message, cause);
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
        this.filePath = filePath;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public String getFilePath() {
        return filePath;
    }
}
