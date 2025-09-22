package org.sensorhub.impl.sensor.ffmpeg.outputs;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

public class HLSOutput<FFMPEGConfigType extends FFMPEGConfig> extends AbstractSensorOutput<FFMPEGSensorBase<FFMPEGConfigType>> implements DataBufferListener {

    private static final String SENSOR_OUTPUT_NAME = "hlsFile";
    private static final String SENSOR_OUTPUT_LABEL = "HLS File";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "HLS playlist file generated using ffmpeg library";

    private static final Logger logger = LoggerFactory.getLogger(HLSOutput.class);

    private DataComponent dataStruct;
    private DataEncoding dataEncoding;

    protected HLSOutput(String name, FFMPEGSensorBase<FFMPEGConfigType> parentSensor) {
        super(name, parentSensor);
    }

    @Override
    public void onDataBuffer(DataBufferRecord record) {

    }

    @Override
    public DataComponent getRecordDescription() {
        return null;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return null;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
}
