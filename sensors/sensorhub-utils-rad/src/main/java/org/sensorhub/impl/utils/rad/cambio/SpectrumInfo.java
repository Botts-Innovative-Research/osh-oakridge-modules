package org.sensorhub.impl.utils.rad.cambio;

import java.util.List;

public class SpectrumInfo {

    private String uuid;
    private String filename;
    private String instrumentType;
    private String manufacturer;
    private String instrumentModel;
    private String instrumentId;
    private String detectorType;
    private String locationName;
    private double latitude;
    private double longitude;
    private boolean hasGpsInfo;

    private float gammaLiveTime;
    private float gammaRealTime;
    private double gammaCountSum;
    private double neutronCountSum;
    private boolean containsNeutrons;

    private long numMeasurements;
    private long numGammaChannels;
    private List<String> detectorNames;
    private List<Integer> detectorNumbers;
    private List<String> remarks;
    private List<String> parseWarnings;

    private SpectrumInfo() {}

    public String getUuid() {
        return uuid;
    }

    public String getFilename() {
        return filename;
    }

    public String getInstrumentType() {
        return instrumentType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getInstrumentModel() {
        return instrumentModel;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public String getDetectorType() {
        return detectorType;
    }

    public String getLocationName() {
        return locationName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean hasGpsInfo() {
        return hasGpsInfo;
    }

    public float getGammaLiveTime() {
        return gammaLiveTime;
    }

    public float getGammaRealTime() {
        return gammaRealTime;
    }

    public double getGammaCountSum() {
        return gammaCountSum;
    }

    public double getNeutronCountSum() {
        return neutronCountSum;
    }

    public boolean containsNeutrons() {
        return containsNeutrons;
    }

    public long getNumMeasurements() {
        return numMeasurements;
    }

    public long getNumGammaChannels() {
        return numGammaChannels;
    }

    public List<String> getDetectorNames() {
        return detectorNames;
    }

    public List<Integer> getDetectorNumbers() {
        return detectorNumbers;
    }

    public List<String> getRemarks() {
        return remarks;
    }

    public List<String> getParseWarnings() {
        return parseWarnings;
    }

    public static class Builder {

        private final SpectrumInfo instance;

        public Builder() {
            this.instance = new SpectrumInfo();
        }

        public Builder uuid(String uuid) {
            this.instance.uuid = uuid;
            return this;
        }

        public Builder filename(String filename) {
            this.instance.filename = filename;
            return this;
        }

        public Builder instrumentType(String instrumentType) {
            this.instance.instrumentType = instrumentType;
            return this;
        }

        public Builder manufacturer(String manufacturer) {
            this.instance.manufacturer = manufacturer;
            return this;
        }

        public Builder instrumentModel(String instrumentModel) {
            this.instance.instrumentModel = instrumentModel;
            return this;
        }

        public Builder instrumentId(String instrumentId) {
            this.instance.instrumentId = instrumentId;
            return this;
        }

        public Builder detectorType(String detectorType) {
            this.instance.detectorType = detectorType;
            return this;
        }

        public Builder locationName(String locationName) {
            this.instance.locationName = locationName;
            return this;
        }

        public Builder latitude(double latitude) {
            this.instance.latitude = latitude;
            return this;
        }

        public Builder longitude(double longitude) {
            this.instance.longitude = longitude;
            return this;
        }

        public Builder hasGpsInfo(boolean hasGpsInfo) {
            this.instance.hasGpsInfo = hasGpsInfo;
            return this;
        }

        public Builder gammaLiveTime(float gammaLiveTime) {
            this.instance.gammaLiveTime = gammaLiveTime;
            return this;
        }

        public Builder gammaRealTime(float gammaRealTime) {
            this.instance.gammaRealTime = gammaRealTime;
            return this;
        }

        public Builder gammaCountSum(double gammaCountSum) {
            this.instance.gammaCountSum = gammaCountSum;
            return this;
        }

        public Builder neutronCountSum(double neutronCountSum) {
            this.instance.neutronCountSum = neutronCountSum;
            return this;
        }

        public Builder containsNeutrons(boolean containsNeutrons) {
            this.instance.containsNeutrons = containsNeutrons;
            return this;
        }

        public Builder numMeasurements(long numMeasurements) {
            this.instance.numMeasurements = numMeasurements;
            return this;
        }

        public Builder numGammaChannels(long numGammaChannels) {
            this.instance.numGammaChannels = numGammaChannels;
            return this;
        }

        public Builder detectorNames(List<String> detectorNames) {
            this.instance.detectorNames = detectorNames;
            return this;
        }

        public Builder detectorNumbers(List<Integer> detectorNumbers) {
            this.instance.detectorNumbers = detectorNumbers;
            return this;
        }

        public Builder remarks(List<String> remarks) {
            this.instance.remarks = remarks;
            return this;
        }

        public Builder parseWarnings(List<String> parseWarnings) {
            this.instance.parseWarnings = parseWarnings;
            return this;
        }

        public SpectrumInfo build() {
            return this.instance;
        }
    }
}
