package org.sensorhub.impl.sensor.ffmpeg.tests;

import org.sensorhub.impl.sensor.ffmpeg.ConnectionTest;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;


public class FileTestH264 extends ConnectionTest {

    private static final Logger logger = LoggerFactory.getLogger(FileTestH264.class);

    public FileTestH264() throws Exception {
    }

    @Override
    protected void populateConfig(FFMPEGConfig config) {
        config.connection.useTCP = true;
        config.connection.fps = 30;
        config.name = "H264 File";
        config.serialNumber = "h264_file";
        config.autoStart = false;
        try {
            config.connection.transportStreamPath = Paths.get(FFMPEGSensor.class.getResource("sample-stream.ts").toURI()).toString();
        } catch (Exception e) {
            logger.error("Could not parse file path for test!");
        }
        config.moduleClass = FFMPEGSensor.class.getCanonicalName();
        config.connectionConfig.connectTimeout = 5000;
        config.connectionConfig.reconnectAttempts = 10;
        config.output.useVideoFrames = false;
        config.output.useHLS = true;
        config.connection.loop = true;
    }
}
