package org.sensorhub.impl.sensor.ffmpeg;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FFMPEGSensorReconnectTest {

    private FFMPEGSensor sensor;

    @Before
    public void setUp() {
        sensor = new FFMPEGSensor();
        FFMPEGConfig config = new FFMPEGConfig();
        config.connectionConfig.reconnectAttempts = 10;
        config.connectionConfig.reconnectPeriod = 60_000;
        sensor.setConfiguration(config);
        sensor.enableReconnect();
    }

    @After
    public void tearDown() {
        sensor.disableReconnect();
    }

    @Test
    public void duplicateReconnectRequestsShareOneDelayedAttempt() {
        sensor.scheduleReconnect(new Exception("stream unavailable"));
        sensor.scheduleReconnect(new Exception("duplicate failure"));

        assertEquals(1, sensor.getReconnectAttemptCount());
        assertTrue(sensor.isReconnectPending());
    }
}
