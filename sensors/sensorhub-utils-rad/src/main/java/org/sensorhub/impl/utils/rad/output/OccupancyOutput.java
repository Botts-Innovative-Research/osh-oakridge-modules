package org.sensorhub.impl.utils.rad.output;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class OccupancyOutput<SensorType extends ISensorModule<?>> extends VarRateSensorOutput<SensorType> {

    private static final String NAME = "occupancy";
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
        var isAdjudicated = radHelper.createIsAdjudicated();
        var placeHolder = radHelper.createPlaceholder();
        placeHolder.setName("videoFile");
        placeHolder.setLabel("Video File");

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("occupancy"))
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
                .addField(isAdjudicated.getName(), isAdjudicated)
//                .addField(needsSecondaryInspection.getName(), needsSecondaryInspection)
                .addField(placeHolder.getName(), placeHolder) // TODO Set the definition here
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(Occupancy occupancy) {
        if (latestRecord == null)
            dataBlock = dataStruct.createDataBlock();
        else
            dataBlock = latestRecord.renew();

        int index = 0;
        dataBlock.setLongValue(index++, System.currentTimeMillis()/1000);
        dataBlock.setIntValue(index++, occupancy.getOccupancyCount());
        dataBlock.setLongValue(index++, occupancy.getStartTime());
        dataBlock.setLongValue(index++, occupancy.getEndTime());
        dataBlock.setDoubleValue(index++, occupancy.getNeutronBackground());
        dataBlock.setBooleanValue(index++, occupancy.hasGammaAlarm());
        dataBlock.setBooleanValue(index++, occupancy.hasNeutronAlarm());
        dataBlock.setIntValue(index++, occupancy.getMaxGammaCount());
        dataBlock.setIntValue(index++, occupancy.getMaxNeutronCount());
        dataBlock.setBooleanValue(index++, occupancy.isAdjudicated());
        dataBlock.setStringValue(index, "");
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
