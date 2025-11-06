package org.sensorhub.impl.utils.rad.model;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.vast.data.DataArrayImpl;

import java.util.ArrayList;
import java.util.List;

public class Occupancy {

    private double samplingTime;
    private int occupancyCount;
    private double startTime;
    private double endTime;
    private double neutronBackground;
    private boolean hasGammaAlarm;
    private boolean hasNeutronAlarm;
    private int maxGammaCount;
    private int maxNeutronCount;
    private List<String> adjudicatedIds = new ArrayList<>();
    private List<String> videoPaths = new ArrayList<>();

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

    public List<String> getAdjudicatedIds() {
        return adjudicatedIds;
    }

    public List<String> getVideoPaths() {
        return videoPaths;
    }

    public double getSamplingTime() {
        return samplingTime;
    }

    public static class Builder {

        Occupancy instance;

        public Builder() {
            this.instance = new Occupancy();
        }

        public Builder samplingTime(double samplingTime) {
            instance.samplingTime = samplingTime;
            return this;
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

        public Builder adjudicatedIds(List<String> adjudicatedIds) {
            instance.adjudicatedIds = adjudicatedIds;
            return this;
        }

        public Builder videoPaths(List<String> videoPaths) {
            instance.videoPaths = videoPaths;
            return this;
        }

        public Occupancy build() {
            return instance;
        }

    }

    public void addAdjudicatedId(String adjudicatedId) {
        this.adjudicatedIds.add(adjudicatedId);
    }

    public void addVideoPath(String videoPath) {
        this.videoPaths.add(videoPath);
    }

    public static DataBlock fromOccupancy(Occupancy occupancy) {
        OccupancyOutput output = new OccupancyOutput(new SensorSystem());
        DataComponent resultStructure = output.getRecordDescription();
        DataBlock dataBlock = resultStructure.createDataBlock();
        dataBlock.updateAtomCount();
        resultStructure.setData(dataBlock);

        int index = 0;

        dataBlock.setDoubleValue(index++, occupancy.getSamplingTime());
        dataBlock.setIntValue(index++, occupancy.getOccupancyCount());
        dataBlock.setDoubleValue(index++, occupancy.getStartTime());
        dataBlock.setDoubleValue(index++, occupancy.getEndTime());
        dataBlock.setDoubleValue(index++, occupancy.getNeutronBackground());
        dataBlock.setBooleanValue(index++, occupancy.hasGammaAlarm());
        dataBlock.setBooleanValue(index++, occupancy.hasNeutronAlarm());
        dataBlock.setIntValue(index++, occupancy.getMaxGammaCount());
        dataBlock.setIntValue(index++, occupancy.getMaxNeutronCount());

        int cmdIdsCount = occupancy.getAdjudicatedIds().size();
        dataBlock.setDoubleValue(index++, cmdIdsCount);

        var adjIdsArray = ((DataArrayImpl) resultStructure.getComponent("adjudicatedIds"));

        if (cmdIdsCount > 0) {
            adjIdsArray.updateSize();
            dataBlock.updateAtomCount();

            for (int i = 0; i < occupancy.getAdjudicatedIds().size(); i++) {
                dataBlock.setStringValue(index++, occupancy.getAdjudicatedIds().get(i));
            }
        }

        int filePathsCount = occupancy.getVideoPaths().size();
        dataBlock.setDoubleValue(index++, filePathsCount);

        var filePathsArray = ((DataArrayImpl) resultStructure.getComponent("videoPaths"));

        if (filePathsCount > 0) {
            filePathsArray.updateSize();
            dataBlock.updateAtomCount();

            for (int i = 0; i < occupancy.getVideoPaths().size(); i++) {
                dataBlock.setStringValue(index++, occupancy.getVideoPaths().get(i));
            }
        }

        return dataBlock;
    }

    public static Occupancy toOccupancy(DataBlock dataBlock) {
        int index = 0;

        var samplingTime = dataBlock.getDoubleValue(index++);
        var occupancyCount = dataBlock.getIntValue(index++);
        var startTime = dataBlock.getDoubleValue(index++);
        var endTime = dataBlock.getDoubleValue(index++);
        var neutronBackground = dataBlock.getDoubleValue(index++);
        var gammaAlarm = dataBlock.getBooleanValue(index++);
        var neutronAlarm = dataBlock.getBooleanValue(index++);
        var maxGammaCount = dataBlock.getIntValue(index++);
        var maxNeutronCount = dataBlock.getIntValue(index++);
        var cmdIdsCount = dataBlock.getIntValue(index++);

        List<String> cmdIds = new ArrayList<>();
        for (int i = 0; i < cmdIdsCount; i++)
            cmdIds.add(dataBlock.getStringValue(index++));

        var videoPathCount = dataBlock.getIntValue(index++);

        List<String> videoPaths = new ArrayList<>();
        for (int i = 0; i < videoPathCount; i++)
            videoPaths.add(dataBlock.getStringValue(index++));

        return new Builder()
                .samplingTime(samplingTime)
                .occupancyCount(occupancyCount)
                .startTime(startTime)
                .endTime(endTime)
                .neutronBackground(neutronBackground)
                .maxGammaCount(maxGammaCount)
                .maxNeutronCount(maxNeutronCount)
                .gammaAlarm(gammaAlarm)
                .neutronAlarm(neutronAlarm)
                .adjudicatedIds(cmdIds)
                .videoPaths(videoPaths)
                .build();
    }
}
