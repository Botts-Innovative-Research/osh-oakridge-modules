package com.botts.impl.system.lane.helpers.occupancy;

import com.botts.impl.process.rs350.occupancy.DailyFileStruct;
import com.botts.impl.process.rs350.occupancy.Rs350OccupancyProcessModule;
import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.LaneSystem;
import net.opengis.swe.v20.DataComponent;
import org.junit.Test;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.impl.utils.rad.RADHelper;

import java.time.Instant;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class OccupancyStatusOutputTest {

    private static final Instant CLEAR_TIME = Instant.parse("2026-09-24T12:00:00Z");
    private static final Instant OCCUPIED_TIME = Instant.parse("2026-09-24T12:00:10.250Z");
    private static final Instant RS350_SOURCE_START = Instant.parse("2026-09-24T12:00:09Z");
    private static final Instant ALARM_TIME = Instant.parse("2026-09-24T12:00:20Z");
    private static final Instant END_TIME = Instant.parse("2026-09-24T12:00:30Z");

    @Test
    public void rapiscanPublishesCanonicalOccupancyLifecycle() {
        assertCanonicalLifecycle(
                new RapiscanSensor(),
                () -> textDailyFile("GB,0"),
                () -> textDailyFile("GS,1"),
                () -> textDailyFile("GA,1"),
                () -> textDailyFile("GB,0"));
    }

    @Test
    public void aspectPublishesCanonicalOccupancyLifecycle() {
        assertCanonicalLifecycle(
                new AspectSensor(),
                () -> textDailyFile("0,0,0,0,0,0,0,0,0,0"),
                () -> textDailyFile("0,1,0,0,0,0,0,0,0,0"),
                () -> textDailyFile("0,1,1,0,0,0,0,0,0,0"),
                () -> textDailyFile("0,0,0,0,0,0,0,0,0,0"));
    }

    @Test
    public void rs350PublishesCanonicalOccupancyLifecycle() {
        var output = newTestOutput();
        var wrapper = new OccupancyWrapper(null, new Rs350OccupancyProcessModule(), output);

        assertNull("Unknown state must not be represented as clear", output.getLatestRecord());

        assertTrue(wrapper.processDailyFile(rs350DailyFile(false, false, null), CLEAR_TIME));
        assertStatus(output, false, 0.0, CLEAR_TIME);

        // A foreground report without an alarm is still a real RS350 occupancy.
        assertTrue(wrapper.processDailyFile(
                rs350DailyFile(true, false, RS350_SOURCE_START), OCCUPIED_TIME));
        assertStatus(output, true, epochSeconds(RS350_SOURCE_START), OCCUPIED_TIME);

        assertTrue(wrapper.processDailyFile(
                rs350DailyFile(true, true, RS350_SOURCE_START), ALARM_TIME));
        assertStatus(output, true, epochSeconds(RS350_SOURCE_START), OCCUPIED_TIME);

        assertTrue(wrapper.processDailyFile(rs350DailyFile(false, false, null), END_TIME));
        assertStatus(output, false, 0.0, END_TIME);
    }

    @Test
    public void malformedSampleDoesNotTurnUnknownIntoClear() {
        var output = newTestOutput();
        var wrapper = new OccupancyWrapper(null, new RapiscanSensor(), output);

        assertFalse(wrapper.processDailyFile(textDailyFile("unrecognized"), CLEAR_TIME));
        assertNull(output.getLatestRecord());
    }

    @Test
    public void outputAdvertisesStableDiscoveryContract() {
        var output = newTestOutput();
        var record = output.getRecordDescription();

        assertEquals(OccupancyStatusOutput.NAME, output.getName());
        assertEquals(OccupancyStatusOutput.OCCUPANCY_STATUS_DEFINITION,
                record.getComponent("isOccupied").getDefinition());
        assertEquals(OccupancyStatusOutput.OCCUPANCY_START_TIME_DEFINITION,
                record.getComponent("occupancyStartTime").getDefinition());
    }

    private static void assertCanonicalLifecycle(
            IDataProducerModule<?> rpm,
            Supplier<DataComponent> clearSample,
            Supplier<DataComponent> occupiedSample,
            Supplier<DataComponent> alarmSample,
            Supplier<DataComponent> endSample) {
        var output = newTestOutput();
        var wrapper = new OccupancyWrapper(null, rpm, output);

        assertNull("Unknown state must not be represented as clear", output.getLatestRecord());

        assertTrue(wrapper.processDailyFile(clearSample.get(), CLEAR_TIME));
        assertStatus(output, false, 0.0, CLEAR_TIME);

        assertTrue(wrapper.processDailyFile(occupiedSample.get(), OCCUPIED_TIME));
        assertStatus(output, true, epochSeconds(OCCUPIED_TIME), OCCUPIED_TIME);

        assertTrue(wrapper.processDailyFile(alarmSample.get(), ALARM_TIME));
        assertStatus(output, true, epochSeconds(OCCUPIED_TIME), OCCUPIED_TIME);

        assertTrue(wrapper.processDailyFile(endSample.get(), END_TIME));
        assertStatus(output, false, 0.0, END_TIME);
    }

    private static void assertStatus(
            OccupancyStatusOutput output,
            boolean occupied,
            double expectedStart,
            Instant expectedPublishTime) {
        var record = output.getLatestRecord();
        assertNotNull(record);
        assertEquals(occupied, record.getBooleanValue(1));
        assertEquals(expectedStart, record.getDoubleValue(2), 0.000_001);
        assertEquals(expectedPublishTime.toEpochMilli(), output.getLatestRecordTime());
    }

    private static DataComponent textDailyFile(String message) {
        var radHelper = new RADHelper();
        var record = radHelper.createRecord()
                .name("dailyFile")
                .addField("samplingTime", radHelper.createPrecisionTimeStamp())
                .addField("message", radHelper.createText().name("message").build())
                .build();
        record.renewDataBlock();
        record.getData().setDoubleValue(0, epochSeconds(CLEAR_TIME));
        record.getData().setStringValue(1, message);
        return record;
    }

    private static DataComponent rs350DailyFile(boolean occupied, boolean alarming, Instant occupancyStart) {
        var record = DailyFileStruct.getRecordDescription();
        record.renewDataBlock();
        record.getComponent("samplingTime").getData().setDoubleValue(epochSeconds(CLEAR_TIME));
        record.getComponent(DailyFileStruct.OCCUPIED_NAME).getData().setBooleanValue(occupied);
        record.getComponent(DailyFileStruct.OCCUPANCY_START_TIME_NAME).getData()
                .setDoubleValue(occupancyStart == null ? 0.0 : epochSeconds(occupancyStart));
        record.getComponent(DailyFileStruct.ALARM_NAME).getData().setBooleanValue(alarming);
        return record;
    }

    private static double epochSeconds(Instant instant) {
        return instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
    }

    private static OccupancyStatusOutput newTestOutput() {
        return new OccupancyStatusOutput(new LaneSystem() {
            @Override
            public String getUniqueIdentifier() {
                return "urn:osh:system:lane:occupancy-status-test";
            }
        });
    }
}
