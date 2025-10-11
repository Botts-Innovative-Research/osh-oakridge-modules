package org.sensorhub.impl.utils.rad.model;

import net.opengis.swe.v20.DataBlock;
import org.sensorhub.impl.utils.rad.RADHelper;

public class Adjudication {

    private String feedback;
    private int adjudicationCode;
    private String isotopes;
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

    public static DataBlock fromAdjudication(Adjudication adjudication) {
        RADHelper fac = new RADHelper();
        DataBlock dataBlock = fac.createAdjudicationRecord().createDataBlock();
        dataBlock.setStringValue(0, adjudication.getFeedback());
        dataBlock.setIntValue(1, adjudication.getAdjudicationCode());
        // TODO: Use array
        dataBlock.setStringValue(2, adjudication.getIsotopes());
        dataBlock.setStringValue(3, adjudication.getSecondaryInspectionStatus().toString());
        // TODO: Use array
        dataBlock.setStringValue(4, adjudication.getFilePaths());
        dataBlock.setStringValue(5, adjudication.getOccupancyId());
        dataBlock.setStringValue(6, adjudication.getVehicleId());
        return dataBlock;
    }

    public static Adjudication toAdjudication(DataBlock dataBlock) {
        return new Adjudication.Builder()
                .feedback(dataBlock.getStringValue(0))
                .adjudicationCode(dataBlock.getIntValue(1))
                .isotopes(dataBlock.getStringValue(2))
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.valueOf(dataBlock.getStringValue(3)))
                .filePaths(dataBlock.getStringValue(4))
                .occupancyId(dataBlock.getStringValue(5))
                .vehicleId(dataBlock.getStringValue(6))
                .build();
    }

}
