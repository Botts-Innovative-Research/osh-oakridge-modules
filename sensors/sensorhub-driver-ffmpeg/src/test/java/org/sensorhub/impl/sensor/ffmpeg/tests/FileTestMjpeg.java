package org.sensorhub.impl.sensor.ffmpeg.tests;

import org.junit.Ignore;
import org.sensorhub.impl.sensor.ffmpeg.ConnectionTest;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;


public class FileTestMjpeg extends ConnectionTest {

    public FileTestMjpeg() throws Exception {
    }

    @Override
    protected void populateConfig(FFMPEGConfig config) {
        config.connection.useTCP = true;
        config.connection.fps = 24;
        config.name = "H264 File";
        config.serialNumber = "h264_file";
        config.autoStart = false;
        config.connection.connectionString = FFMPEGSensor.class.getResource("sample-stream.ts").getPath().substring(1);
        //config.connection.connectionString = "http://192.171.163.3/axis-cgi/media.cgi?audiocodec=aac&audiosamplerate=16000&audiobitrate=32000&camera=1&videoframeskipmode=empty&videozprofile=classic&resolution=1920x1080&audiodeviceid=0&audioinputid=0&timestamp=1&videocodec=h264&container=mp4";
        config.moduleClass = FFMPEGSensor.class.getCanonicalName();
        config.connectionConfig.connectTimeout = 5000;
        config.connectionConfig.reconnectAttempts = 10;
    }
}
