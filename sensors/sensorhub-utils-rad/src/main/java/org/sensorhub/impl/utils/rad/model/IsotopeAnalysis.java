package org.sensorhub.impl.utils.rad.model;

public class IsotopeAnalysis {

    private String name;
    private String type;
    private float confidence;
    private String confidenceStr;
    private float countRate;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getConfidenceStr() {
        return confidenceStr;
    }

    public float getCountRate() {
        return countRate;
    }

    private IsotopeAnalysis() {}

    public static class Builder {

        private final IsotopeAnalysis instance;

        public Builder() {
            this.instance = new IsotopeAnalysis();
        }

        public IsotopeAnalysis.Builder name(String name) {
            this.instance.name = name;
            return IsotopeAnalysis.Builder.this;
        }

        public IsotopeAnalysis.Builder type(String type) {
            this.instance.type = type;
            return IsotopeAnalysis.Builder.this;
        }

        public IsotopeAnalysis.Builder confidence(float confidence) {
            this.instance.confidence = confidence;
            return IsotopeAnalysis.Builder.this;
        }

        public IsotopeAnalysis.Builder confidenceStr(String confidenceStr) {
            this.instance.confidenceStr = confidenceStr;
            return IsotopeAnalysis.Builder.this;
        }

        public IsotopeAnalysis.Builder countRate(float countRate) {
            this.instance.countRate = countRate;
            return IsotopeAnalysis.Builder.this;
        }

        public IsotopeAnalysis build() {
            return this.instance;
        }
    }
}
