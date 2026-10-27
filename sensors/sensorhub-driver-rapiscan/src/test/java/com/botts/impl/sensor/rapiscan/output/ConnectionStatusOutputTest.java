package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import org.junit.Test;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.event.IEventListener;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionStatusOutputTest {

    @Test
    public void throttlesUnchangedHeartbeatsToThirtySecondsWhilePublishingTransitionsImmediately() {
        RapiscanSensor sensor = new RapiscanSensor() {
            @Override
            public String getUniqueIdentifier() {
                return "urn:osh:sensor:rapiscan:test";
            }
        };
        ConnectionStatusOutput output = new ConnectionStatusOutput(sensor);
        output.init();

        List<Boolean> published = new ArrayList<>();
        IEventListener listener = event -> {
            if (event instanceof DataEvent dataEvent) {
                published.add(dataEvent.getRecords()[0].getBooleanValue(1));
            }
        };
        output.registerListener(listener);

        long baseMillis = 1_700_000_000_000L;
        output.onNewMessage(true, baseMillis);
        assertEquals(1, published.size());
        assertTrue(published.get(0));

        // Unchanged status within 30s is suppressed.
        output.onNewMessage(true, baseMillis + 1_000L);
        output.onNewMessage(true, baseMillis + 29_999L);
        assertEquals(1, published.size());

        // Unchanged status at 30s publishes a heartbeat.
        output.onNewMessage(true, baseMillis + 30_000L);
        assertEquals(2, published.size());
        assertTrue(published.get(1));

        // State transition publishes immediately without waiting 30s.
        output.onNewMessage(false, baseMillis + 31_000L);
        assertEquals(3, published.size());
        assertFalse(published.get(2));

        // Transition back to connected also publishes immediately.
        output.onNewMessage(true, baseMillis + 32_000L);
        assertEquals(4, published.size());
        assertTrue(published.get(3));
    }
}
