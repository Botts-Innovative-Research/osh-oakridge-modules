package org.sensorhub.impl.utils.rad.output;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class OccupancyOutput<SensorType extends ISensorModule<?>> extends VarRateSensorOutput<SensorType> {

    public static final String NAME = "occupancy";
    private static final String LABEL = "Occupancy";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public OccupancyOutput(SensorType parentSensor) {
        super(NAME, parentSensor, 4);

        RADHelper radHelper = new RADHelper();
        var samplingTime = radHelper.createPrecisionTimeStamp();
        var occupancyCount = radHelper.createOccupancyCount();
        var occupancyStart = radHelper.createOccupancyStartTime();
        var occupancyEnd = radHelper.createOccupancyEndTime();
        var neutronBackground = radHelper.createNeutronBackground();
        var gammaAlarm = radHelper.createGammaAlarm();
        var neutronAlarm = radHelper.createNeutronAlarm();
        var maxGamma = radHelper.createMaxGamma();
        var maxNeutron = radHelper.createMaxNeutron();
        var adjudicationIdsCount = radHelper.createAdjudicatedIdCount();
        var adjudicatedIds = radHelper.createAdjudicatedIdsArray();
        var videoPathCount = radHelper.createVideoPathCount();
        var videoPaths = radHelper.createVideoPathsArray();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("Occupancy"))
                .description("System occupancy count since midnight each day")
                .addField(samplingTime.getName(), samplingTime)
                .addField(occupancyCount.getName(), occupancyCount)
                .addField(occupancyStart.getName(), occupancyStart)
                .addField(occupancyEnd.getName(), occupancyEnd)
                .addField(neutronBackground.getName(), neutronBackground)
                .addField(gammaAlarm.getName(), gammaAlarm)
                .addField(neutronAlarm.getName(), neutronAlarm)
                .addField(maxGamma.getName(), maxGamma)
                .addField(maxNeutron.getName(), maxNeutron)
                .addField(adjudicationIdsCount.getName(), adjudicationIdsCount)
                .addField(adjudicatedIds.getName(), adjudicatedIds)
                .addField(videoPathCount.getName(), videoPathCount)
                .addField(videoPaths.getName(), videoPaths)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void setData(Occupancy occupancy) {
        dataBlock = dataStruct.createDataBlock();

        dataStruct.setData(dataBlock);

        int index = 0;
        dataBlock.setDoubleValue(index++, System.currentTimeMillis()/1000);
        dataBlock.setIntValue(index++, occupancy.getOccupancyCount());
        dataBlock.setDoubleValue(index++, occupancy.getStartTime());
        dataBlock.setDoubleValue(index++, occupancy.getEndTime());
        dataBlock.setDoubleValue(index++, occupancy.getNeutronBackground());
        dataBlock.setBooleanValue(index++, occupancy.hasGammaAlarm());
        dataBlock.setBooleanValue(index++, occupancy.hasNeutronAlarm());
        dataBlock.setIntValue(index++, occupancy.getMaxGammaCount());
        dataBlock.setIntValue(index++, occupancy.getMaxNeutronCount());

        dataBlock.setIntValue(index++, 0); // adj id count
        dataBlock.setIntValue(index, 0); // video path count

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), OccupancyOutput.this, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }
}
