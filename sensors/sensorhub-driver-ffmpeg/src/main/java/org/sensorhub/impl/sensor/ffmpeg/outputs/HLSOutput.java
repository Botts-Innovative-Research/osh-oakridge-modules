package org.sensorhub.impl.sensor.ffmpeg.outputs;

import com.botts.api.service.bucket.IBucketService;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HLSOutput<FFMPEGConfigType extends FFMPEGConfig> extends AbstractSensorOutput<FFMPEGSensorBase<FFMPEGConfigType>> implements DataBufferListener {

    private static final String SENSOR_OUTPUT_NAME = "hlsFile";
    private static final String SENSOR_OUTPUT_LABEL = "HLS File";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "HLS playlist file generated using ffmpeg library";

    private static final Logger logger = LoggerFactory.getLogger(HLSOutput.class);

    private DataComponent dataStruct;
    private DataEncoding dataEncoding;

    String directory;

    private final FileOutput fileOutput;

    public HLSOutput(FFMPEGSensorBase<FFMPEGConfigType> parentSensor, String directory) throws SensorHubException {
        super(SENSOR_OUTPUT_NAME, parentSensor);
        this.directory = directory;

        fileOutput = new FileOutput(getParentProducer().getParentHub().getModuleRegistry().getModuleByType(IBucketService.class).getBucketStore());

        SWEHelper helper = new SWEHelper();
        dataStruct = helper.createText()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .value("").build();

        dataEncoding = helper.newTextEncoding(",", "\n");
    }

    public void initStream(AVFormatContext inputFormat, int videoStreamId) throws IOException {
        String fileName = directory
                + parentSensor.getUniqueIdentifier().replace(':', '-') + "/"
                + System.currentTimeMillis()
                + ".m3u8";

        String path = fileName;
        if (fileName.contains("/")) {
            if (fileName.contains("http:")) { // assuming we're writing to this osh node
                var url = parent.getParentHub().getModuleRegistry().getModuleByType(HttpServer.class).getServletsBaseUrl();
                path = fileName.replace(url, "./");
            }
            Files.createDirectories(Paths.get(path.substring(0, path.lastIndexOf("/") + 1)));
        }

        fileOutput.openFile(path, inputFormat, videoStreamId);

        if (latestRecord == null) {
            latestRecord = dataStruct.createDataBlock();
        }
        latestRecord.setStringValue(fileName);
        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, this, latestRecord));
    }

    public void stopStream() throws IOException {
        fileOutput.closeFile();
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

    @Override
    public void onDataBuffer(DataBufferRecord record) {
        fileOutput.onDataBuffer(record);
    }
}
