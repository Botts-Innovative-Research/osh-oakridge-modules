package org.sensorhub.impl.utils.rad.model;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.impl.utils.rad.RADHelper;

import java.time.Instant;

/**
 * One OCR candidate read from a lane camera during an alarming occupancy:
 * either a shipping-container number (ISO 6346) or a license plate.
 * One observation is published per candidate; results are correlated to the
 * occupancy through {@code occupancyObsId}, same as {@link WebIdAnalysis}.
 */
public class VehicleOcrResult {

    public static final String ID_TYPE_CONTAINER = "container";
    public static final String ID_TYPE_PLATE = "plate";

    private Instant sampleTime;
    private String occupancyObsId;
    private String idType;
    private String value;
    private String normalizedValue;
    private boolean checksumValid;
    private float confidence;
    private int readCount;
    private String cameraUid;
    private Instant frameTime;
    private String evidenceImagePath;

    private VehicleOcrResult() {}

    public Instant getSampleTime() {
        return sampleTime;
    }

    public String getOccupancyObsId() {
        return occupancyObsId;
    }

    public String getIdType() {
        return idType;
    }

    public String getValue() {
        return value;
    }

    public String getNormalizedValue() {
        return normalizedValue;
    }

    public boolean isChecksumValid() {
        return checksumValid;
    }

    public float getConfidence() {
        return confidence;
    }

    public int getReadCount() {
        return readCount;
    }

    public String getCameraUid() {
        return cameraUid;
    }

    public Instant getFrameTime() {
        return frameTime;
    }

    public String getEvidenceImagePath() {
        return evidenceImagePath;
    }

    public void setOccupancyObsId(String occupancyObsId) {
        this.occupancyObsId = occupancyObsId;
    }

    @Override
    public String toString() {
        return "VehicleOcrResult{" + idType + "=" + normalizedValue
                + ", checksumValid=" + checksumValid
                + ", confidence=" + confidence
                + ", readCount=" + readCount + "}";
    }

    public static class Builder {

        private final VehicleOcrResult instance;

        public Builder() {
            this.instance = new VehicleOcrResult();
        }

        public Builder sampleTime(Instant sampleTime) {
            this.instance.sampleTime = sampleTime;
            return this;
        }

        public Builder occupancyObsId(String occupancyObsId) {
            this.instance.occupancyObsId = occupancyObsId;
            return this;
        }

        public Builder idType(String idType) {
            this.instance.idType = idType;
            return this;
        }

        public Builder value(String value) {
            this.instance.value = value;
            return this;
        }

        public Builder normalizedValue(String normalizedValue) {
            this.instance.normalizedValue = normalizedValue;
            return this;
        }

        public Builder checksumValid(boolean checksumValid) {
            this.instance.checksumValid = checksumValid;
            return this;
        }

        public Builder confidence(float confidence) {
            this.instance.confidence = confidence;
            return this;
        }

        public Builder readCount(int readCount) {
            this.instance.readCount = readCount;
            return this;
        }

        public Builder cameraUid(String cameraUid) {
            this.instance.cameraUid = cameraUid;
            return this;
        }

        public Builder frameTime(Instant frameTime) {
            this.instance.frameTime = frameTime;
            return this;
        }

        public Builder evidenceImagePath(String evidenceImagePath) {
            this.instance.evidenceImagePath = evidenceImagePath;
            return this;
        }

        public VehicleOcrResult build() {
            return this.instance;
        }
    }

    public static DataBlock fromVehicleOcrResult(VehicleOcrResult result) {
        RADHelper radHelper = new RADHelper();
        DataComponent recordStructure = radHelper.createVehicleOcrRecord();
        DataBlock dataBlock = recordStructure.createDataBlock();
        recordStructure.setData(dataBlock);

        int index = 0;

        dataBlock.setTimeStamp(index++, result.getSampleTime());
        dataBlock.setStringValue(index++, emptyIfNull(result.getOccupancyObsId()));
        dataBlock.setStringValue(index++, emptyIfNull(result.getIdType()));
        dataBlock.setStringValue(index++, emptyIfNull(result.getValue()));
        dataBlock.setStringValue(index++, emptyIfNull(result.getNormalizedValue()));
        dataBlock.setBooleanValue(index++, result.isChecksumValid());
        dataBlock.setFloatValue(index++, result.getConfidence());
        dataBlock.setIntValue(index++, result.getReadCount());
        dataBlock.setStringValue(index++, emptyIfNull(result.getCameraUid()));
        dataBlock.setTimeStamp(index++, result.getFrameTime() != null ? result.getFrameTime() : result.getSampleTime());
        dataBlock.setStringValue(index, emptyIfNull(result.getEvidenceImagePath()));

        return dataBlock;
    }

    public static VehicleOcrResult toVehicleOcrResult(DataBlock dataBlock) {
        int index = 0;

        var sampleTime = dataBlock.getTimeStamp(index++);
        var occupancyObsId = dataBlock.getStringValue(index++);
        var idType = dataBlock.getStringValue(index++);
        var value = dataBlock.getStringValue(index++);
        var normalizedValue = dataBlock.getStringValue(index++);
        var checksumValid = dataBlock.getBooleanValue(index++);
        var confidence = dataBlock.getFloatValue(index++);
        var readCount = dataBlock.getIntValue(index++);
        var cameraUid = dataBlock.getStringValue(index++);
        var frameTime = dataBlock.getTimeStamp(index++);
        var evidenceImagePath = dataBlock.getStringValue(index);

        return new Builder()
                .sampleTime(sampleTime)
                .occupancyObsId(occupancyObsId)
                .idType(idType)
                .value(value)
                .normalizedValue(normalizedValue)
                .checksumValid(checksumValid)
                .confidence(confidence)
                .readCount(readCount)
                .cameraUid(cameraUid)
                .frameTime(frameTime)
                .evidenceImagePath(evidenceImagePath)
                .build();
    }

    private static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }
}
