package com.botts.impl.system.lane.helpers.occupancy;

import com.botts.impl.system.lane.LaneSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

import java.time.Instant;

/**
 * Lane-level, RPM-independent view of the current occupancy state.
 *
 * <p>The output intentionally belongs to the lane rather than to a specific RPM
 * driver.  Consumers can therefore discover one stable stream and use the same
 * result contract for Rapiscan, Aspect, and RS350 lanes.</p>
 */
public class OccupancyStatusOutput extends VarRateSensorOutput<LaneSystem> {

    public static final String NAME = "occupancyStatus";
    public static final String OCCUPANCY_STATUS_DEFINITION = RADHelper.getRadUri("OccupancyStatus");
    public static final String OCCUPANCY_START_TIME_DEFINITION = RADHelper.getRadUri("OccupancyStartTime");

    private final DataRecord recordStruct;
    private final DataEncoding recordEncoding;

    public OccupancyStatusOutput(LaneSystem parentSensor) {
        super(NAME, parentSensor, 0);

        var radHelper = new RADHelper();
        recordStruct = radHelper.createRecord()
                .name(NAME)
                .label("Occupancy Status")
                .description("Current lane occupancy state and the start time of the active occupancy")
                .updatable(true)
                .addField("samplingTime", radHelper.createPrecisionTimeStamp())
                .addField("isOccupied", radHelper.createBoolean()
                        .name("isOccupied")
                        .label("Is Occupied")
                        .definition(OCCUPANCY_STATUS_DEFINITION)
                        .description("True while an occupancy or alarming occupancy is active"))
                .addField("occupancyStartTime", radHelper.createTime()
                        .asPhenomenonTimeIsoUTC()
                        .name("occupancyStartTime")
                        .label("Occupancy Start Time")
                        .definition(OCCUPANCY_START_TIME_DEFINITION)
                        .description("Start of the active occupancy; zero when the lane is clear"))
                .build();
        recordEncoding = new TextEncodingImpl(",", "\n");
    }

    public void publish(Instant sampleTime, boolean isOccupied, Instant occupancyStartTime) {
        var dataBlock = latestRecord == null ? recordStruct.createDataBlock() : latestRecord.renew();
        long eventTime = sampleTime.toEpochMilli();

        dataBlock.setDoubleValue(0, toEpochSeconds(sampleTime));
        dataBlock.setBooleanValue(1, isOccupied);
        dataBlock.setDoubleValue(2, isOccupied && occupancyStartTime != null
                ? toEpochSeconds(occupancyStartTime)
                : 0.0);

        latestRecord = dataBlock;
        latestRecordTime = eventTime;
        eventHandler.publish(new DataEvent(eventTime, this, dataBlock));
    }

    private static double toEpochSeconds(Instant instant) {
        return instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
    }

    @Override
    public DataComponent getRecordDescription() {
        return recordStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return recordEncoding;
    }
}
