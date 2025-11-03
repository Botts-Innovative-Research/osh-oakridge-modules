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

        dataStruct = radHelper.createOccupancy();

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
