package com.botts.impl.service.oscar.webid;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public class WebIdRequest extends HttpRequest {

    private InputStream foreground;
    private InputStream background;
    private String drf;
    private boolean synthesizeBackground;

    private WebIdRequest() {
    }

    @Override
    public Optional<BodyPublisher> bodyPublisher() {
        return Optional.empty();
    }

    @Override
    public String method() {
        return "POST";
    }

    @Override
    public Optional<Duration> timeout() {
        return Optional.empty();
    }

    @Override
    public boolean expectContinue() {
        return false;
    }

    @Override
    public URI uri() {
        return null;
    }

    @Override
    public Optional<HttpClient.Version> version() {
        return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
        return null;
    }

    public InputStream getForeground() {
        return foreground;
    }

    public InputStream getBackground() {
        return background;
    }

    public String getDrf() {
        return drf;
    }

    public boolean synthesizeBackground() {
        return synthesizeBackground;
    }

    public static class Builder {

        private final WebIdRequest instance;

        public Builder() {
            this.instance = new WebIdRequest();
        }

        public Builder drf(String drf) {
            this.instance.drf = drf;
            return Builder.this;
        }

        public Builder foreground(InputStream foreground) {
            this.instance.foreground = foreground;
            return Builder.this;
        }

        public Builder background(InputStream background) {
            this.instance.background = background;
            return Builder.this;
        }

        public Builder synthesizeBackground(boolean synthesizeBackground) {
            this.instance.synthesizeBackground = synthesizeBackground;
            return Builder.this;
        }

        public WebIdRequest build() {
            if (this.instance.foreground == null) {
                throw new IllegalStateException("Foreground spectrum is required");
            }
            if (this.instance.synthesizeBackground && this.instance.background != null) {
                throw new IllegalStateException("Cannot specify both synthesizeBackground=true and a background file");
            }
            return this.instance;
        }
    }

}
