package com.botts.impl.system.lane.helpers.occupancy.state;

import net.opengis.swe.v20.DataComponent;
import org.junit.Test;
import org.sensorhub.impl.utils.rad.RADHelper;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class StateManagerConcurrencyTest {

    @Test
    public void listenerRemovalDuringTransitionUsesStableSnapshot() {
        TestManager manager = new TestManager();
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        AtomicReference<IStateListener> first = new AtomicReference<>();
        first.set((from, to) -> {
            firstCalls.incrementAndGet();
            manager.removeListener(first.get());
            manager.clearListeners();
        });
        manager.addListener(first.get());
        manager.addListener((from, to) -> secondCalls.incrementAndGet());

        manager.occupied = true;
        Instant start = Instant.parse("2026-10-18T13:00:00.123Z");
        manager.updateDailyFile(sample(), start);

        assertEquals(1, firstCalls.get());
        assertEquals("Copy-on-write iteration must finish the dispatched snapshot",
                1, secondCalls.get());
        assertEquals(start, manager.getOccupancyStartTimeHint());

        manager.occupied = false;
        manager.updateDailyFile(sample(), start.plusSeconds(1));
        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }

    private static DataComponent sample() {
        var record = new RADHelper().createRecord()
                .name("dailyFile")
                .addField("message", new RADHelper().createText().name("message").build())
                .build();
        record.renewDataBlock();
        record.getData().setStringValue(0, "sample");
        return record;
    }

    private static final class TestManager extends StateManager {
        private boolean occupied;

        @Override
        protected boolean isAlarming() {
            return false;
        }

        @Override
        protected boolean isOccupied() {
            return occupied;
        }

        @Override
        protected boolean isNonOccupied() {
            return !occupied;
        }

        @Override
        protected boolean parseDailyFile() {
            return dailyFile != null && dailyFile.hasData();
        }
    }
}
