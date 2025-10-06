package org.sensorhub.impl.utils.rad.model;

public class Occupancy {

    private int occupancyCount;
    private double startTime;
    private double endTime;
    private double neutronBackground;
    private boolean hasGammaAlarm;
    private boolean hasNeutronAlarm;
    private int maxGammaCount;
    private int maxNeutronCount;
    private boolean isAdjudicated = false;
    private String videoPaths;

    public int getOccupancyCount() {
        return occupancyCount;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public double getNeutronBackground() {
        return neutronBackground;
    }

    public boolean hasGammaAlarm() {
        return hasGammaAlarm;
    }

    public boolean hasNeutronAlarm() {
        return hasNeutronAlarm;
    }

    public int getMaxGammaCount() {
        return maxGammaCount;
    }

    public int getMaxNeutronCount() {
        return maxNeutronCount;
    }

    public boolean isAdjudicated() {
        return isAdjudicated;
    }

    public String getVideoPaths() {
        return videoPaths;
    }

    public static class Builder {

        Occupancy instance;

        public Builder() {
            this.instance = new Occupancy();
        }

        public Builder occupancyCount(int occupancyCount) {
            instance.occupancyCount = occupancyCount;
            return this;
        }

        public Builder startTime(double startTime) {
            instance.startTime = startTime;
            return this;
        }

        public Builder endTime(double endTime) {
            instance.endTime = endTime;
            return this;
        }

        public Builder neutronBackground(double neutronBackground) {
            instance.neutronBackground = neutronBackground;
            return this;
        }

        public Builder gammaAlarm(boolean hasGammaAlarm) {
            instance.hasGammaAlarm = hasGammaAlarm;
            return this;
        }

        public Builder neutronAlarm(boolean hasNeutronAlarm) {
            instance.hasNeutronAlarm = hasNeutronAlarm;
            return this;
        }

        public Builder maxGammaCount(int maxGammaCount) {
            instance.maxGammaCount = maxGammaCount;
            return this;
        }

        public Builder maxNeutronCount(int maxNeutronCount) {
            instance.maxNeutronCount = maxNeutronCount;
            return this;
        }

        public Builder adjudicated(boolean isAdjudicated) {
            instance.isAdjudicated = isAdjudicated;
            return this;
        }

        public Builder videoPaths(String videoPaths) {
            instance.videoPaths = videoPaths;
            return this;
        }

        public Occupancy build() {
            return instance;
        }

    }

}
