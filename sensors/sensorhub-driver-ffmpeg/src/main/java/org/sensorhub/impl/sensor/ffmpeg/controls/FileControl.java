package org.sensorhub.impl.sensor.ffmpeg.controls;

import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.*;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.outputs.FileOutput;
import org.vast.swe.SWEHelper;
//import com.botts.impl.service.oscar.Constants;
import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class FileControl<FFmpegConfigType extends FFMPEGConfig> extends AbstractSensorControl<FFMPEGSensorBase<FFmpegConfigType>> implements FFmpegControl {
    public static final String SENSOR_CONTROL_NAME = "ffmpegFileControl";
    private static final String SENSOR_CONTROL_LABEL = "FFmpeg File Control";
    public static final String CMD_OPEN_FILE = "startFile";
    public static final String CMD_CLOSE_FILE = "endFile";
    public static final String FILE_IO = "fileIO";
    //public static final String VIDEO_BUCKET = Constants.VIDEO_BUCKET;
    public static final String VIDEO_BUCKET = "videos";

    private final IBucketStore bucketStore;
    DataRecord commandData;
    DataRecord resultData;
    String fileName = "";
    private final FileOutput fileOutput;

    public FileControl(FFMPEGSensorBase<FFmpegConfigType> sensor, FileOutput fileOutput) throws DataStoreException {
        super(SENSOR_CONTROL_NAME, sensor);

        this.fileOutput = fileOutput;

        var bucketService = sensor.getParentHub().getModuleRegistry().getModuleByType(IBucketService.class);
        bucketStore = bucketService.getBucketStore();

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
                .addField(FILE_IO, fac.createChoice()
                        .label("File I/O")
                        .addItem(CMD_OPEN_FILE, fac.createText()
                                .label("Start File")
                                .description("Start recording video to a file.")
                                .definition(SWEHelper.getPropertyUri("VideoStart"))
                                .build())
                        .addItem(CMD_CLOSE_FILE, fac.createBoolean()
                                .value(true)
                                .label("Save File")
                                .label("Stop recording. Save file if true, discard if false.")
                                .definition(SWEHelper.getPropertyUri("VideoStop"))
                                .build())
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
            String fileNameTemp = "";
            DataRecord commandData = this.commandData.copy();
            commandData.setData(command.getParams());
            var selected = ((DataChoice) commandData.getComponent(0)).getSelectedItem();
            if (selected == null)
                commandStatus = false;

            else if (selected.getName().equals(CMD_OPEN_FILE)) {
                if (!fileName.isEmpty())
                    commandStatus = false;

                else {
                    fileName = Paths.get("clips", parentSensor.getUniqueIdentifier().replace(':', '-'),
                            selected.getData().getStringValue()).toString();

                    if (!fileName.contains(".")) {
                        fileName += ".mp4";
                    }

                    try {
                        //this.parentSensor.getProcessor().openFile(fileName);
                        //var outputStream = this.bucketStore.putObject(VIDEO_BUCKET, fileName, Collections.emptyMap());
                        this.bucketStore.putObject(VIDEO_BUCKET, fileName, Collections.emptyMap()).close();
                        this.fileOutput.openFile(bucketStore.getResourceURI(VIDEO_BUCKET, fileName));
                        this.parentSensor.reportStatus("Writing to file: " + fileName);
                    } catch (Exception e) {
                        commandStatus = false;
                    }
                }
            } else if (selected.getName().equals(CMD_CLOSE_FILE)) {
                if (fileName.isEmpty())
                    commandStatus = false;
                else {
                    boolean saveFile = ((Boolean) selected).getValue();
                    try {
                        this.fileOutput.closeFile();

                        // Delete file if we do not want to save
                        if (!saveFile) {
                            bucketStore.deleteObject(VIDEO_BUCKET, fileName);
                            this.parentSensor.reportStatus("Discarded file: " + fileName);
                        } else {
                            this.parentSensor.reportStatus("Saved file: " + fileName);
                            fileNameTemp = fileName;
                            reportFileName = true;
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
                    resultData.getData().setStringValue(bucketStore.getRelativeResourceURI(VIDEO_BUCKET, fileNameTemp));
                    ICommandResult result = CommandResult.withData(resultData.getData());
                    status.withResult(result);
                } catch (Exception ignored) {}
            }
            return status.build();
        });
    }
}
