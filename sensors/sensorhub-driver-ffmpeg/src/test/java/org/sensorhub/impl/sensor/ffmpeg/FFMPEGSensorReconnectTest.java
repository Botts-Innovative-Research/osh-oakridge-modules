package org.sensorhub.impl.sensor.ffmpeg;

import net.opengis.swe.v20.DataBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.utils.rad.RADHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FFMPEGSensorReconnectTest {

    private static final String CONNECTION_STATUS_OUTPUT = "connectionStatus";

    private TestFFMPEGSensor sensor;

    @Before
    public void setUp() {
        sensor = new TestFFMPEGSensor();
        FFMPEGConfig config = new FFMPEGConfig();
        config.connection.connectionString = "test://camera";
        config.output.useVideoFrames = true;
        config.connectionConfig.reconnectAttempts = 10;
        config.connectionConfig.reconnectPeriod = 60_000;
        sensor.setConfiguration(config);
    }

    @After
    public void tearDown() throws SensorHubException {
        sensor.doStop();
    }

    @Test
    public void duplicateReconnectRequestsShareOneDelayedAttempt() {
        sensor.enableReconnect();
        sensor.scheduleReconnect(new Exception("stream unavailable"));
        sensor.scheduleReconnect(new Exception("duplicate failure"));

        assertEquals(1, sensor.getReconnectAttemptCount());
        assertTrue(sensor.isReconnectPending());
    }

    @Test
    public void initialOpenFailureStillInitializesWithExplicitOfflineStatus() throws SensorHubException {
        sensor.failOpen = true;

        sensor.doInit();

        IStreamingDataInterface output = sensor.getOutputs().get(CONNECTION_STATUS_OUTPUT);
        assertNotNull("Connection-status output must be registered before opening the source", output);
        assertSame(sensor.connectionStatusOutput, output);
        assertEquals(RADHelper.getRadUri("ConnectionStatus"),
                output.getRecordDescription().getComponent("isConnected").getDefinition());
        assertTrue("Stream controls must be configured even while the camera is Offline",
                sensor.streamControlConfigurationAttempted);
        assertConnectionStatus(false);
    }

    @Test
    public void successfulStartClearsStaleOnlineBeforeOpenThenPublishesOnline() throws SensorHubException {
        initializeWithUnavailableSource();
        sensor.publishConnectionStatus(true);
        assertConnectionStatus(true);

        sensor.failOpen = false;
        sensor.doStart();

        assertEquals(Boolean.FALSE, sensor.connectionStatusObservedAtOpen);
        assertEquals(Boolean.FALSE, sensor.connectionStatusObservedAtStart);
        assertTrue("Video outputs must be configured after the recovered source opens",
                sensor.videoOutputConfigurationAttempted);
        assertTrue("Stream controls must be configured after the recovered source opens",
                sensor.streamControlConfigurationAttempted);
        assertTrue("Outputs created after recovery must refresh the system registry",
                sensor.recoveredInterfacesRegistered);
        assertConnectionStatus(true);
    }

    @Test
    public void failedStartReplacesStaleOnlineWithOffline() throws SensorHubException {
        initializeWithUnavailableSource();
        sensor.publishConnectionStatus(true);
        assertConnectionStatus(true);

        sensor.failOpen = true;
        sensor.doStart();

        assertEquals(Boolean.FALSE, sensor.connectionStatusObservedAtOpen);
        assertConnectionStatus(false);
        assertTrue(sensor.isReconnectPending());
    }

    @Test
    public void streamStartupFailureNeverPublishesOnline() throws SensorHubException {
        initializeWithUnavailableSource();
        sensor.publishConnectionStatus(true);

        sensor.failOpen = false;
        sensor.failStart = true;
        sensor.doStart();

        assertEquals(Boolean.FALSE, sensor.connectionStatusObservedAtOpen);
        assertEquals(Boolean.FALSE, sensor.connectionStatusObservedAtStart);
        assertConnectionStatus(false);
        assertTrue(sensor.isReconnectPending());
    }

    @Test
    public void reconnectAndStopBothReplaceOnlineWithOffline() throws SensorHubException {
        initializeWithUnavailableSource();

        sensor.publishConnectionStatus(true);
        sensor.enableReconnect();
        sensor.scheduleReconnect(new Exception("stream lost"));
        assertConnectionStatus(false);

        sensor.publishConnectionStatus(true);
        sensor.doStop();
        assertConnectionStatus(false);
    }

    private void initializeWithUnavailableSource() throws SensorHubException {
        sensor.failOpen = true;
        sensor.doInit();
        assertConnectionStatus(false);
    }

    private void assertConnectionStatus(boolean expected) {
        IStreamingDataInterface output = sensor.getOutputs().get(CONNECTION_STATUS_OUTPUT);
        assertNotNull("Connection-status output is not registered", output);
        DataBlock latestRecord = output.getLatestRecord();
        assertNotNull("Connection-status output has no current observation", latestRecord);
        assertEquals(expected, latestRecord.getBooleanValue(1));
    }

    private static class TestFFMPEGSensor extends FFMPEGSensor {
        boolean failOpen;
        boolean failStart;
        Boolean connectionStatusObservedAtOpen;
        Boolean connectionStatusObservedAtStart;
        boolean videoOutputConfigurationAttempted;
        boolean streamControlConfigurationAttempted;
        boolean recoveredInterfacesRegistered;

        @Override
        protected void openStream() throws SensorHubException {
            connectionStatusObservedAtOpen = currentConnectionStatus();
            if (failOpen)
                throw new SensorHubException("source unavailable");
        }

        @Override
        protected void startStream() throws SensorHubException {
            connectionStatusObservedAtStart = currentConnectionStatus();
            if (failStart)
                throw new SensorHubException("stream startup failed");
        }

        @Override
        protected void configureVideoOutputAfterOpen() {
            if (!failOpen)
                videoOutputConfigurationAttempted = true;
        }

        @Override
        protected void configureStreamControlsAfterOpen() {
            streamControlConfigurationAttempted = true;
        }

        @Override
        protected void registerRecoveredInterfaces() {
            recoveredInterfacesRegistered = true;
        }

        @Override
        protected void stopStream() {
            // No native FFmpeg processor is used by these lifecycle tests.
        }

        private Boolean currentConnectionStatus() {
            if (connectionStatusOutput == null || connectionStatusOutput.getLatestRecord() == null)
                return null;
            return connectionStatusOutput.getLatestRecord().getBooleanValue(1);
        }
    }
}
