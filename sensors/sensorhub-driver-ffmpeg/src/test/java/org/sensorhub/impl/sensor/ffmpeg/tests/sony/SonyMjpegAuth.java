package org.sensorhub.impl.sensor.ffmpeg.tests.sony;

import org.sensorhub.impl.sensor.ffmpeg.ConnectionTest;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;


public class SonyMjpegAuth extends ConnectionTest {

    private final static String SONY_MJPEG_AUTH_IP = "SONY_MJPEG_AUTH_IP";

    @Override
    protected void populateConfig(FFMPEGConfig config) {
        config.connection.useTCP = true;
        config.connection.fps = 24;
        config.name = "MJPEG Sony Test with Auth";
        config.serialNumber = "test_mjpeg_sony_auth";
        config.autoStart = false;
        config.connection.connectionString = System.getenv(SONY_MJPEG_AUTH_IP);
        config.moduleClass = FFMPEGSensor.class.getCanonicalName();
        config.connectionConfig.connectTimeout = 5000;
        config.connectionConfig.reconnectAttempts = 10;
        config.output.useVideoFrames = false;
        config.output.useHLS = true;
    }
}
