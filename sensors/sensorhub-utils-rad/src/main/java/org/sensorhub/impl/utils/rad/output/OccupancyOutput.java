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

import java.util.ArrayList;
import java.util.List;

public class OccupancyOutput<SensorType extends ISensorModule<?>> extends VarRateSensorOutput<SensorType> {

    public static final String NAME = "occupancy";
    private static final String LABEL = "Occupancy";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    protected final List<OccupancyCallback> occupancyCallbacks = new ArrayList<>();

    public OccupancyOutput(SensorType parentSensor) {
        super(NAME, parentSensor, 4);

        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createOccupancy();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void setData(Occupancy occupancy) {

        augmentOccupancy(occupancy);

        dataBlock = Occupancy.fromOccupancy(occupancy);

        dataStruct.setData(dataBlock);

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), OccupancyOutput.this, dataBlock));
    }

    /**
     * Use callbacks to modify occupancy data before publishing. (Ex: Add video paths)
     * @param occupancy
     */
    private void augmentOccupancy(Occupancy occupancy) {
        for (OccupancyCallback callback : occupancyCallbacks) {
            callback.call(occupancy);
        }
    }

    public void addOccupancyCallback(OccupancyCallback callback) { this.occupancyCallbacks.add(callback); }

    public void removeOccupancyCallback(OccupancyCallback callback) { this.occupancyCallbacks.remove(callback); }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @FunctionalInterface
    public interface OccupancyCallback { public void call(Occupancy occupancy); }
}
