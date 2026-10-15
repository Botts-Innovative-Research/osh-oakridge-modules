package org.sensorhub.impl.sensor.ffmpeg.controls;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.sensorhub.api.command.*;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.controls.hls.HlsStreamHandler;
import org.sensorhub.impl.sensor.ffmpeg.outputs.FileOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

public class HLSControl<FFmpegConfigType extends FFMPEGConfig> extends AbstractSensorControl<FFMPEGSensorBase<FFmpegConfigType>> implements FFmpegControl, IStreamingControlInterfaceWithResult {
    public static final String SENSOR_CONTROL_NAME = "ffmpegHlsControl";
    private static final String SENSOR_CONTROL_LABEL = "FFmpeg HLS Control";
    public static final String CMD_START_STREAM = "startStream";
    public static final String CMD_END_STREAM = "endStream";
    public static final String STREAM_CONTROL = "streamControl";
    //public static final String VIDEO_BUCKET = Constants.VIDEO_BUCKET;
    public static final String VIDEO_BUCKET = "videos";

    private final IBucketStore bucketStore;
    private final HlsStreamHandler hlsHandler;
    private static final Map<IBucketService, WeakReference<HlsStreamHandler>> HLS_HANDLERS =
            new WeakHashMap<>();
    DataRecord commandData;
    DataRecord resultData;
    String fileName = "";
    Path filePath = Paths.get(VIDEO_BUCKET);
    private final FileOutput<?> fileOutput;

    private static final Logger logger = LoggerFactory.getLogger(HLSControl.class);

    public HLSControl(FFMPEGSensorBase<FFmpegConfigType> sensor, FileOutput<?> fileOutput) throws DataStoreException {
        super(SENSOR_CONTROL_NAME, sensor);

        this.fileOutput = fileOutput;


        IBucketService bucketService = sensor.getParentHub().getModuleRegistry()
                .getModuleByType(IBucketService.class);
        bucketStore = bucketService.getBucketStore();
        hlsHandler = resolveHlsHandler(bucketService);

        boolean videosBucketExists = bucketStore.bucketExists(VIDEO_BUCKET);
        if (!videosBucketExists) {
            bucketStore.createBucket(VIDEO_BUCKET);
        }
    }

    /**
     * Resolves exactly one handler per live bucket-service instance. Weak keys and values avoid
     * retaining a stopped hub/service through this process-wide registry, while synchronization
     * makes initialization atomic when many camera modules are constructed concurrently.
     */
    static HlsStreamHandler resolveHlsHandler(IBucketService service) {
        synchronized (HLS_HANDLERS) {
            WeakReference<HlsStreamHandler> reference = HLS_HANDLERS.get(service);
            HlsStreamHandler handler = reference != null ? reference.get() : null;
            if (handler == null) {
                handler = new HlsStreamHandler(service, VIDEO_BUCKET);
                service.registerObjectHandler(handler);
                HLS_HANDLERS.put(service, new WeakReference<>(handler));
            }
            return handler;
        }
    }

    @Override
    public FileOutput<?> getFileOutput() {
        return this.fileOutput;
    }

    public void init() {
        SWEHelper fac = new SWEHelper();

        commandData = fac.createRecord()
                .name(getName())
                .label(SENSOR_CONTROL_LABEL)
                .addField(STREAM_CONTROL, fac.createCategory()
                        .label("Stream Command")
                        .addAllowedValues(CMD_START_STREAM, CMD_END_STREAM)
                        .definition(SWEHelper.getPropertyUri("StreamControl"))
                        .build())
                .build();

        resultData = fac.createRecord().name("result")
                .addField("streamPath", fac.createText())
                .build();
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandData;
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        return CompletableFuture.supplyAsync(() -> executeCommand(command));
    }

    private synchronized ICommandStatus executeCommand(ICommandData command) {
            boolean commandStatus = true;
            boolean reportFileName = false;
            String failureMessage = null;
            //DataRecord commandData = this.commandData.copy();
            //commandData.setData(command.getParams());
            var selected = command.getParams().getStringValue();

            if (selected == null) {
                commandStatus = false;
                failureMessage = "No stream command was selected";
            }

            else if (selected.equals(CMD_START_STREAM)) {
                if (fileOutput.isWriting()) {
                    commandStatus = true;
                    reportFileName = true;
                }
                else if (!parentSensor.isStreamReady()) {
                    commandStatus = false;
                    failureMessage = "Video input stream is not available";
                }
                else {
                    filePath = Paths.get("streams", parentSensor.getUniqueIdentifier().replace(':', '-'), "live.m3u8");
                    fileName = filePath.toString();
                    try {
                        // Remove artifacts left by an interrupted prior stream, then prepare a
                        // validated filesystem target without creating a zero-byte manifest.
                        // The HLS muxer publishes its first real playlist atomically after it has
                        // completed a keyframe-aligned media segment.
                        cleanupHlsResources(fileName);
                        String uri = bucketStore.getWritableResourceURI(VIDEO_BUCKET, fileName);
                        this.fileOutput.openFile(uri);
                        hlsHandler.addControl(fileName, this);
                        this.parentSensor.reportStatus("Writing video stream: " + fileName);
                        reportFileName = true;
                        commandStatus = true;
                        //fileOutput.publish(uri);
                    } catch (Exception e) {
                        logger.error("Exception while opening HLS output", e);
                        cleanupHlsResources(fileName);
                        fileName = "";
                        commandStatus = false;
                        failureMessage = "Unable to start HLS stream: " + e.getMessage();
                    }
                }
            } else if (selected.equals(CMD_END_STREAM)) {
                String closingFileName = fileName;
                try {
                    if (!fileOutput.isWriting()) {
                        commandStatus = false;
                        failureMessage = "HLS stream is not active";
                    } else {
                        this.fileOutput.closeFile();
                        this.parentSensor.reportStatus("Closing video stream: " + closingFileName);
                        commandStatus = true;
                        reportFileName = false;
                    }
                } catch (Exception e) {
                    logger.error("Exception while closing HLS output", e);
                    commandStatus = false;
                    failureMessage = "Unable to stop HLS stream: " + e.getMessage();
                } finally {
                    // FileOutput has already stopped writing even when trailer/close reports an
                    // error. Always unregister the handler and remove manifest/segments so a
                    // failed close cannot leave a stale active stream or partial artifacts.
                    cleanupHlsResources(closingFileName);
                    fileName = "";
                }
            } else {
                commandStatus = false;
                failureMessage = "Unsupported stream command: " + selected;
            }

            CommandStatus.Builder status = new CommandStatus.Builder()
                    .withCommand(command.getID())
                    .withStatusCode(commandStatus ? ICommandStatus.CommandStatusCode.ACCEPTED : ICommandStatus.CommandStatusCode.FAILED);
            if (failureMessage != null)
                status.withMessage(failureMessage);
            if (commandStatus && reportFileName) {
                // The manifest is intentionally absent until FFmpeg completes its first media
                // segment, so an existence-checking URI lookup cannot be used here. The object
                // key was already validated by getWritableResourceURI before the stream opened.
                resultData.renewDataBlock();
                resultData.getData().setStringValue(Paths.get(VIDEO_BUCKET, fileName).toString());
                ICommandResult result = CommandResult.withData(resultData.getData());
                status.withResult(result);
            }
            return status.build();
    }

    private void cleanupHlsResources(String streamName) {
        if (streamName == null || streamName.isBlank()) {
            return;
        }

        hlsHandler.removeControl(streamName, this);
        try {
            int extensionIndex = streamName.lastIndexOf('.');
            String streamPrefix = extensionIndex > 0
                    ? streamName.substring(0, extensionIndex) : streamName;
            var matchingObjects = bucketStore.listObjects(VIDEO_BUCKET).stream()
                    .filter(name -> name.startsWith(streamPrefix))
                    .toList();
            for (String hlsFile : matchingObjects) {
                bucketStore.deleteObject(VIDEO_BUCKET, hlsFile);
            }
        } catch (Exception cleanupError) {
            logger.warn("Exception during HLS cleanup for {}", streamName, cleanupError);
        }
    }

    @Override
    public DataComponent getResultDescription() {
        return resultData;
    }
}
