package com.botts.impl.service.oscar.webid;

import java.util.List;

public class WebIdAnalysis {

    private List<IsotopeAnalysis> isotopes;
    private String isotopeString;
    private String drf;

    private double estimatedDose;
    private double chi2;

    private int analysisError;
    private String errorMessage;
    private List<String> analysisWarnings;

    private String foregroundDescription;
    private String backgroundDescription;

    private WebIdAnalysis() {}

    public boolean isSuccessful() {
        return analysisError == 0;
    }

    public boolean hasError() {
        return analysisError != 0;
    }

    public List<IsotopeAnalysis> getIsotopes() {
        return isotopes;
    }

    public String getIsotopeString() {
        return isotopeString;
    }

    public String getDrf() {
        return drf;
    }

    public double getEstimatedDose() {
        return estimatedDose;
    }

    public double getChi2() {
        return chi2;
    }

    public int getAnalysisError() {
        return analysisError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getAnalysisWarnings() {
        return analysisWarnings;
    }

    public String getForegroundDescription() {
        return foregroundDescription;
    }

    public String getBackgroundDescription() {
        return backgroundDescription;
    }

    public static class Builder {

        private final WebIdAnalysis instance;

        public Builder() {
            this.instance = new WebIdAnalysis();
        }

        public WebIdAnalysis.Builder isotopes(List<IsotopeAnalysis> isotopes) {
            this.instance.isotopes = isotopes;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder isotopeString(String isotopeString) {
            this.instance.isotopeString = isotopeString;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder drf(String drf) {
            this.instance.drf = drf;
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

        public WebIdAnalysis.Builder analysisError(int analysisError) {
            this.instance.analysisError = analysisError;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder errorMessage(String errorMessage) {
            this.instance.errorMessage = errorMessage;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder analysisWarnings(List<String> analysisWarnings) {
            this.instance.analysisWarnings = analysisWarnings;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder foregroundDescription(String foregroundDescription) {
            this.instance.foregroundDescription = foregroundDescription;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis.Builder backgroundDescription(String backgroundDescription) {
            this.instance.backgroundDescription = backgroundDescription;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis build() {
            return this.instance;
        }
    }
}
