package org.sensorhub.impl.sensor.ffmpeg.tests.axis;

import org.sensorhub.impl.sensor.ffmpeg.ConnectionTest;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;


public class AxisH264NoAuth extends ConnectionTest {

    private final static String AXIS_H264_NO_AUTH_IP = "AXIS_H264_NO_AUTH_IP";

    @Override
    protected void populateConfig(FFMPEGConfig config) {
        config.connection.useTCP = true;
        config.connection.fps = 24;
        config.name = " Axis Test without Auth";
        config.serialNumber = "test_h264_axis_no_auth";
        config.autoStart = true;
        config.connection.connectionString = System.getenv(AXIS_H264_NO_AUTH_IP);
        config.moduleClass = FFMPEGSensor.class.getCanonicalName();
        config.connectionConfig.connectTimeout = 5000;
        config.connectionConfig.reconnectAttempts = 10;
    }
}
