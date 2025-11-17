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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OccupancyOutput<SensorType extends ISensorModule<?>> extends VarRateSensorOutput<SensorType> {

    public static final String NAME = "occupancy";
    private static final String LABEL = "Occupancy";

    private static final Logger logger = LoggerFactory.getLogger(OccupancyOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    protected final List<OccupancyCallback> occupancyCallbacks = new ArrayList<>();
    ExecutorService executor = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private static final int TIMEOUT = 10;

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
        var task = executor.submit(() -> augmentPublish(occupancy));

        scheduledExecutor.schedule(() -> {
            if (!task.isDone()) {
                logger.warn("Timeout waiting for occupancy publish task to complete. Canceling task.");
                task.cancel(true);
            }
        }, TIMEOUT, TimeUnit.SECONDS);
    }

    private void augmentPublish(Occupancy occupancy) {
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
