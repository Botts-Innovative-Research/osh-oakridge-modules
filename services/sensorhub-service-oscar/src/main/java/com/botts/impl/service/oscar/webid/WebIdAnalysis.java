package com.botts.impl.service.oscar.webid;

import java.io.InputStream;

public class WebIdAnalysis {

    private String isotopeName;
    private String isotopeType;
    private double confidenceLevel;
    private double estimatedDose;
    private double chi2;
    private String analysisError;


    public WebIdAnalysis() {}

    public static class Builder {

        private final WebIdAnalysis instance;

        public Builder() {
            this.instance = new WebIdAnalysis();
        }

        public WebIdAnalysis.Builder isotopeName(String isotopeName) {
            this.instance.isotopeName = isotopeName;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder isotopeType(String isotopeType) {
            this.instance.isotopeType = isotopeType;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder confidenceLevel(double confidenceLevel) {
            this.instance.confidenceLevel = confidenceLevel;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder estimatedDose(double estimatedDose) {
            this.instance.estimatedDose = estimatedDose;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder chi2(double chi2) {
            this.instance.chi2 = chi2;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder analysisError(String analysisError) {
            this.instance.analysisError = analysisError;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis build() {
            return this.instance;
        }
    }

}
