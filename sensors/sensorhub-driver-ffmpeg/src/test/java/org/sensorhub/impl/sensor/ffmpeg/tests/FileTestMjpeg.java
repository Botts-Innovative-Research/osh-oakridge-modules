package org.sensorhub.impl.sensor.ffmpeg.tests;

import org.sensorhub.impl.sensor.ffmpeg.ConnectionTest;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;

public class FileTestMjpeg extends ConnectionTest {

    @Override
    protected void populateConfig(FFMPEGConfig config) {
        config.connection.useTCP = true;
        config.connection.fps = 24;
        config.name = "H264 File";
        config.serialNumber = "h264_file";
        config.autoStart = true;
        config.connection.connectionString = this.getClass().getResource("sample-stream.ts").toString();
        config.moduleClass = FFMPEGSensor.class.getCanonicalName();
        config.connectionConfig.connectTimeout = 5000;
        config.connectionConfig.reconnectAttempts = 10;
    }
}
