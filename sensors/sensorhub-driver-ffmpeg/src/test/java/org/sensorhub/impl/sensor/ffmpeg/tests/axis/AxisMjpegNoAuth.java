package org.sensorhub.impl.sensor.ffmpeg.tests.axis;

import org.junit.Ignore;
import org.sensorhub.impl.sensor.ffmpeg.ConnectionTest;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;


public class AxisMjpegNoAuth extends ConnectionTest {

    private final static String AXIS_MJPEG_NO_AUTH_IP = "AXIS_MJPEG_NO_AUTH_IP";

    @Override
    protected void populateConfig(FFMPEGConfig config) {
        config.connection.useTCP = true;
        config.connection.fps = 24;
        config.name = " Axis Test without Auth";
        config.serialNumber = "test_mjpeg_axis_no_auth";
        config.autoStart = false;
        config.connection.connectionString = System.getenv(AXIS_MJPEG_NO_AUTH_IP);
        config.moduleClass = FFMPEGSensor.class.getCanonicalName();
        config.connectionConfig.connectTimeout = 5000;
        config.connectionConfig.reconnectAttempts = 10;
    }
}
