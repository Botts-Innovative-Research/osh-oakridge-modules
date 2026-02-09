package org.sensorhub.impl.utils.rad.model;

import net.opengis.swe.v20.DataArray;import net.opengis.swe.v20.DataBlock;import net.opengis.swe.v20.DataComponent;import org.sensorhub.impl.utils.rad.RADHelper;

import java.time.Instant;import java.util.ArrayList;import java.util.List;

public class WebIdAnalysis {

    private Instant sampleTime;

    private List<IsotopeAnalysis> isotopes;
    private String isotopeString;
    private String drf;

    private double estimatedDose;
    private double chi2;

    private int analysisError;
    private String errorMessage;
    private List<String> analysisWarnings;

    private String occupancyObsId;

    private String rawJson;

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

    public String getOccupancyObsId() {
        return occupancyObsId;
    }

    public Instant getSampleTime() {
        return sampleTime;
    }

    public void setSampleTime(Instant sampleTime) {
        this.sampleTime = sampleTime;
    }

    public void setOccupancyObsId(String occupancyObsId) {
        this.occupancyObsId = occupancyObsId;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    @Override
    public String toString() {
        return this.rawJson;
    }

    public static class Builder {

        private final WebIdAnalysis instance;

        public Builder() {
            this.instance = new WebIdAnalysis();
        }

        public WebIdAnalysis.Builder sampleTime(Instant sampleTime) {
            this.instance.sampleTime = sampleTime;
            return WebIdAnalysis.Builder.this;
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

        public WebIdAnalysis.Builder occupancyObsId(String occupancyObsId) {
            this.instance.occupancyObsId = occupancyObsId;
            return WebIdAnalysis.Builder.this;
        }

        public WebIdAnalysis build() {
            return this.instance;
        }
    }

    public static DataBlock fromWebIdAnalysis(WebIdAnalysis analysis) {
        RADHelper radHelper = new RADHelper();
        DataComponent recordStructure = radHelper.createWebIdRecord();
        DataBlock dataBlock = recordStructure.createDataBlock();
        recordStructure.setData(dataBlock);

        int index = 0;

        dataBlock.setTimeStamp(index++, analysis.getSampleTime());

        // isotopes
        int numIsotopes = analysis.getIsotopes().size();
        dataBlock.setIntValue(index++, numIsotopes);

        // update isotope array size
        var isotopesArray = ((DataArray) recordStructure.getComponent("isotopes"));
        isotopesArray.updateSize();
        dataBlock.updateAtomCount();

        // populate isotope analyses
        for (int i = 0; i < numIsotopes; i++) {
            var isotope = analysis.getIsotopes().get(i);
            dataBlock.setStringValue(index++, isotope.getName());
            dataBlock.setStringValue(index++, isotope.getType());
            dataBlock.setFloatValue(index++, isotope.getConfidence());
            dataBlock.setStringValue(index++, isotope.getConfidenceStr());
            dataBlock.setFloatValue(index++, isotope.getCountRate());
        }

        // populate other fields
        dataBlock.setStringValue(index++, analysis.getIsotopeString() == null || analysis.getIsotopeString().isBlank() ? "" : analysis.getIsotopeString());
        dataBlock.setStringValue(index++, analysis.getDrf() == null || analysis.getDrf().isBlank() ? "" : analysis.getDrf());
        dataBlock.setDoubleValue(index++, analysis.getEstimatedDose());
        dataBlock.setDoubleValue(index++, analysis.getChi2());
        dataBlock.setStringValue(index++, analysis.getErrorMessage() == null || analysis.getErrorMessage().isBlank() ? "" : analysis.getErrorMessage());

        // analysis warnings
        int numWarnings = analysis.getAnalysisWarnings().size();
        dataBlock.setIntValue(index++, numWarnings);

        // update analysis warnings array size
        var warningsArray = ((DataArray) recordStructure.getComponent("analysisWarnings"));
        warningsArray.updateSize();
        dataBlock.updateAtomCount();

        // populate warnings
        for (int i = 0; i < numWarnings; i++) {
            var warning = analysis.getAnalysisWarnings().get(i);
            dataBlock.setStringValue(index++, warning);
        }

        dataBlock.setStringValue(index, analysis.getOccupancyObsId().isBlank() ? "" : analysis.getOccupancyObsId());

        return dataBlock;
    }

    public static WebIdAnalysis toWebIdAnalysis(DataBlock dataBlock) {
        int index = 0;

        var timestamp = dataBlock.getTimeStamp(index++);

        // isotopes
        int numIsotopes = dataBlock.getIntValue(index++);
        List<IsotopeAnalysis> isotopes = new ArrayList<>();
        for (int i = 0; i < numIsotopes; i++) {
            var name = dataBlock.getStringValue(index++);
            var type = dataBlock.getStringValue(index++);
            var confidence = dataBlock.getFloatValue(index++);
            var confidenceStr = dataBlock.getStringValue(index++);
            var countRate = dataBlock.getFloatValue(index++);
            IsotopeAnalysis isotope = new IsotopeAnalysis.Builder()
                    .name(name)
                    .type(type)
                    .confidence(confidence)
                    .confidenceStr(confidenceStr)
                    .countRate(countRate)
                    .build();

            isotopes.add(isotope);
        }

        // other fields
        var isotopeStr = dataBlock.getStringValue(index++);
        var drf = dataBlock.getStringValue(index++);
        var estimatedDose = dataBlock.getDoubleValue(index++);
        var chi2 = dataBlock.getDoubleValue(index++);
        var errorMessage = dataBlock.getStringValue(index++);

        // warnings
        int numWarnings = dataBlock.getIntValue(index++);
        List<String> warnings = new ArrayList<>();
        for (int i = 0; i < numWarnings; i++) {
            var warning = dataBlock.getStringValue(index++);
            warnings.add(warning);
        }

        var occupancyObsId = dataBlock.getStringValue(index);

        return new WebIdAnalysis.Builder()
                .sampleTime(timestamp)
                .isotopes(isotopes)
                .isotopeString(isotopeStr)
                .drf(drf)
                .estimatedDose(estimatedDose)
                .chi2(chi2)
                .errorMessage(errorMessage)
                .analysisWarnings(warnings)
                .occupancyObsId(occupancyObsId)
                .build();
    }
}
