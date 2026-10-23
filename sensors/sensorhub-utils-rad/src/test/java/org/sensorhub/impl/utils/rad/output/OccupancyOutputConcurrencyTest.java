package org.sensorhub.impl.utils.rad.output;

import org.junit.Test;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.impl.utils.rad.model.Occupancy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class OccupancyOutputConcurrencyTest {

    @Test
    public void callbackRemovalDuringAugmentationUsesStableSnapshot() {
        TestOutput output = new TestOutput(mock(IDataProducer.class));
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        AtomicReference<OccupancyOutput.OccupancyCallback> first = new AtomicReference<>();
        AtomicReference<OccupancyOutput.OccupancyCallback> second = new AtomicReference<>();
        first.set(occupancy -> {
            firstCalls.incrementAndGet();
            output.removeOccupancyCallback(first.get());
            output.removeOccupancyCallback(second.get());
        });
        second.set(occupancy -> secondCalls.incrementAndGet());
        output.addOccupancyCallback(first.get());
        output.addOccupancyCallback(second.get());

        output.invokeCallbacks(new Occupancy.Builder().startTime(1.0).build());
        assertEquals(1, firstCalls.get());
        assertEquals("Copy-on-write iteration must finish the dispatched snapshot",
                1, secondCalls.get());

        output.invokeCallbacks(new Occupancy.Builder().startTime(2.0).build());
        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }

    private static final class TestOutput extends OccupancyOutput<IDataProducer> {
        private TestOutput(IDataProducer parentSensor) {
            super(parentSensor);
        }

        private void invokeCallbacks(Occupancy occupancy) {
            augmentOccupancy(occupancy);
        }
    }
}
