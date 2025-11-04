package org.sensorhub.impl.sensor.ffmpeg.controls;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServlet;
import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.sensorhub.api.command.*;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.controls.hls.HlsStreamHandler;
import org.sensorhub.impl.sensor.ffmpeg.outputs.FileOutput;
import org.sensorhub.impl.service.HttpServer;
import org.vast.swe.SWEHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class HLSControl<FFmpegConfigType extends FFMPEGConfig> extends AbstractSensorControl<FFMPEGSensorBase<FFmpegConfigType>> implements FFmpegControl, IStreamingControlInterfaceWithResult {
    public static final String SENSOR_CONTROL_NAME = "ffmpegHlsControl";
    private static final String SENSOR_CONTROL_LABEL = "FFmpeg HLS Control";
    public static final String CMD_START_STREAM = "startStream";
    public static final String CMD_END_STREAM = "endStream";
    public static final String STREAM_CONTROL = "streamControl";
    //public static final String VIDEO_BUCKET = Constants.VIDEO_BUCKET;
    public static final String VIDEO_BUCKET = "videos";

    private final IBucketStore bucketStore;
    private final IBucketService bucketService;
    private final HttpServer httpServer;
    private static final AtomicReference<HlsStreamHandler> hlsHandler = new AtomicReference<>();
    DataRecord commandData;
    DataRecord resultData;
    String fileName = "";
    Path filePath = Paths.get(VIDEO_BUCKET);
    private final FileOutput fileOutput;

    public HLSControl(FFMPEGSensorBase<FFmpegConfigType> sensor, FileOutput fileOutput) throws DataStoreException {
        super(SENSOR_CONTROL_NAME, sensor);

        this.fileOutput = fileOutput;


        httpServer = sensor.getParentHub().getModuleRegistry().getModuleByType(HttpServer.class);
        bucketService = sensor.getParentHub().getModuleRegistry().getModuleByType(IBucketService.class);
        bucketStore = bucketService.getBucketStore();

        if (hlsHandler.get() == null) {
            hlsHandler.set(new HlsStreamHandler(bucketService));
            bucketService.registerObjectHandler(hlsHandler.get());
        }

        boolean videosBucketExists = bucketStore.bucketExists(VIDEO_BUCKET);
        if (!videosBucketExists) {
            bucketStore.createBucket(VIDEO_BUCKET);
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

        return CompletableFuture.supplyAsync(() -> {
            boolean commandStatus = true;
            boolean reportFileName = false;
            //DataRecord commandData = this.commandData.copy();
            //commandData.setData(command.getParams());
            var selected = command.getParams().getStringValue();

            if (selected == null)
                commandStatus = false;

            else if (selected.equals(CMD_START_STREAM)) {
                if (!fileName.isEmpty()) {
                    commandStatus = true;
                    reportFileName = true;
                }
                else {
                    filePath = Paths.get("streams", parentSensor.getUniqueIdentifier().replace(':', '-'), "live.m3u8");
                    fileName = filePath.toString();
                    try {
                        //this.parentSensor.getProcessor().openFile(fileName);
                        this.bucketStore.putObject(VIDEO_BUCKET, fileName, Collections.emptyMap()).close();
                        String uri = bucketStore.getResourceURI(VIDEO_BUCKET, fileName);
                        //bucketStore.deleteObject(VIDEO_BUCKET, fileName);
                        this.fileOutput.openFile(uri);
                        hlsHandler.get().addControl(fileName, this);
                        this.parentSensor.reportStatus("Writing video stream: " + fileName);
                        reportFileName = true;
                        fileOutput.publish(bucketStore.getRelativeResourceURI(VIDEO_BUCKET, fileName));
                    } catch (Exception e) {
                        fileName = null;
                        commandStatus = false;
                    }
                }
            } else if (selected.equals(CMD_END_STREAM)) {
                if (fileName.isEmpty())
                    commandStatus = false;
                else {
                    try {
                        this.fileOutput.closeFile();
                        this.parentSensor.reportStatus("Closing video stream: " + fileName);
                        hlsHandler.get().removeControl(fileName, this);

                        // File cleanup
                        var fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));
                        var filteredNames = bucketStore.listObjects(VIDEO_BUCKET).stream().filter(name -> {
                            return name.contains(fileNameNoExt);
                        }).toList();
                        for (String hlsFile : filteredNames) {
                            bucketStore.deleteObject(VIDEO_BUCKET, hlsFile);
                        }

                        fileName = "";
                    } catch (Exception e) {
                        commandStatus = false;
                    }
                }
            } else {
                commandStatus = false;
            }

            CommandStatus.Builder status = new CommandStatus.Builder()
                    .withCommand(command.getID())
                    .withStatusCode(commandStatus ? ICommandStatus.CommandStatusCode.ACCEPTED : ICommandStatus.CommandStatusCode.FAILED);
            if (commandStatus && reportFileName) {
                try {
                    resultData.renewDataBlock();
                    resultData.getData().setStringValue(bucketStore.getRelativeResourceURI(VIDEO_BUCKET, fileName));
                    ICommandResult result = CommandResult.withData(resultData.getData());
                    status.withResult(result);
                } catch (Exception ignored) {}
            }
            return status.build();
        });
    }

    @Override
    public DataComponent getResultDescription() {
        return resultData;
    }
}
