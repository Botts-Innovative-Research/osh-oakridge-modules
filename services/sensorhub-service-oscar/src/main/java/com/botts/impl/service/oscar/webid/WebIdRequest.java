package com.botts.impl.service.oscar.webid;

import java.io.InputStream;
import java.util.Set;

public class WebIdRequest {

    private InputStream foreground;
    private InputStream background;
    private String drf;
    private boolean synthesizeBackground;

    private WebIdRequest() {

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
            return this.instance;
        }
    }

}
