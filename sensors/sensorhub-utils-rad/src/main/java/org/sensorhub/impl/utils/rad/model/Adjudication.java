package org.sensorhub.impl.utils.rad.model;

public class Adjudication {

    private String feedback;
    private int adjudicationCode;
    private String isotopes;
    // TODO: Enum??
    private SecondaryInspectionStatus secondaryInspectionStatus;
    private String filePaths;
    private String occupancyId;
    private String vehicleId;

    public enum SecondaryInspectionStatus {
        NONE, REQUESTED, COMPLETED
    }

    public static class Builder {

        Adjudication instance;

        public Builder() {
            this.instance = new Adjudication();
        }

        public Builder feedback(String feedback) {
            instance.feedback = feedback;
            return this;
        }

        public Builder adjudicationCode(int code) {
            instance.adjudicationCode = code;
            return this;
        }

        public Builder isotopes(String isotopes) {
            instance.isotopes = isotopes;
            return this;
        }

        public Builder filePaths(String filePaths) {
            instance.filePaths = filePaths;
            return this;
        }

        public Builder occupancyId(String occupancyId) {
            instance.occupancyId = occupancyId;
            return this;
        }

        public Builder vehicleId(String vehicleId) {
            instance.vehicleId = vehicleId;
            return this;
        }

        public Builder secondaryInspectionStatus(SecondaryInspectionStatus secondaryInspectionStatus) {
            instance.secondaryInspectionStatus = secondaryInspectionStatus;
            return this;
        }

        public Adjudication build() {
            return instance;
        }

    }

    public String getFeedback() {
        return feedback;
    }

    public int getAdjudicationCode() {
        return adjudicationCode;
    }

    public String getIsotopes() {
        return isotopes;
    }

    public SecondaryInspectionStatus getSecondaryInspectionStatus() {
        return secondaryInspectionStatus;
    }

    public String getFilePaths() {
        return filePaths;
    }

    public String getOccupancyId() {
        return occupancyId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

}
