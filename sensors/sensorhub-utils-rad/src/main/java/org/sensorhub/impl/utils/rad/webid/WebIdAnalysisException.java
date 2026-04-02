package org.sensorhub.impl.utils.rad.webid;

import java.io.IOException;

public class WebIdAnalysisException extends IOException {

    private final int httpStatusCode;
    private final String responseBody;

    public WebIdAnalysisException(String message, int httpStatusCode, String responseBody) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.responseBody = responseBody;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
