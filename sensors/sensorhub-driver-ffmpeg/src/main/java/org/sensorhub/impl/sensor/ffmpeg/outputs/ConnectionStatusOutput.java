package org.sensorhub.impl.sensor.ffmpeg.outputs;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.vast.swe.SWEHelper;

/**
 * Reports whether the FFmpeg driver currently has an open upstream stream.
 */
public class ConnectionStatusOutput<FFMPEGConfigType extends FFMPEGConfig>
        extends AbstractSensorOutput<FFMPEGSensorBase<FFMPEGConfigType>> {

    private static final String SENSOR_OUTPUT_NAME = "connectionStatus";
    private static final String SENSOR_OUTPUT_LABEL = "Connection Status";
    private static final String CONNECTION_STATUS_DEFINITION =
            "http://www.opengis.net/def/property/OGC/0/ConnectionStatus";

    private DataComponent dataStruct;
    private DataEncoding dataEncoding;

    public ConnectionStatusOutput(FFMPEGSensorBase<FFMPEGConfigType> parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        SWEHelper swe = new SWEHelper();

        dataStruct = swe.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .addField("samplingTime", swe.createTime()
                        .name("samplingTime")
                        .label("Sampling Time")
                        .asSamplingTimeIsoUTC())
                .addField("isConnected", swe.createBoolean()
                        .name("isConnected")
                        .label("Is Connected")
                        .definition(CONNECTION_STATUS_DEFINITION)
                        .description("Whether the camera stream is connected"))
                .build();
        dataEncoding = swe.newTextEncoding(",", "\n");
    }

    public void publish(boolean isConnected) {
        DataBlock dataBlock = latestRecord == null
                ? dataStruct.createDataBlock()
                : latestRecord.renew();
        long timestamp = System.currentTimeMillis();

        dataBlock.setDoubleValue(0, timestamp / 1000.0);
        dataBlock.setBooleanValue(1, isConnected);
        latestRecord = dataBlock;
        latestRecordTime = timestamp;
        eventHandler.publish(new DataEvent(timestamp, this, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
}
