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
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.AccessDeniedException;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
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
    String workingFileName = "";
    private final FileOutput<?> fileOutput;

    public FileControl(FFMPEGSensorBase<FFmpegConfigType> sensor, FileOutput<?> fileOutput) throws DataStoreException {
        super(SENSOR_CONTROL_NAME, sensor);

        this.fileOutput = fileOutput;

        var bucketService = sensor.getParentHub().getModuleRegistry().getModuleByType(IBucketService.class);
        bucketStore = bucketService.getBucketStore();

        boolean videosBucketExists = bucketStore.bucketExists(VIDEO_BUCKET);
        if (!videosBucketExists) {
            bucketStore.createBucket(VIDEO_BUCKET);
        }
    }

    public String getFileName() {
        if (fileName.isBlank()) {
            getLogger().debug("Blank video URI ");
            return "";
        }
        // This method is used while the recorder still owns a hidden partial. Return only the
        // eventual final key; callers can persist the legacy videoPaths value without exposing the
        // in-progress object.
        return Paths.get(VIDEO_BUCKET, fileName).toString();
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
        return CompletableFuture.supplyAsync(() -> executeCommand(command));
    }

    private synchronized ICommandStatus executeCommand(ICommandData command) {
            boolean commandStatus = true;
            boolean reportFileName = false;
            String fileNameTemp = "";
            String failureMessage = null;
            DataRecord commandData = this.commandData.copy();
            commandData.setData(command.getParams());
            var selected = ((DataChoice) commandData.getComponent(0)).getSelectedItem();
            if (selected == null) {
                commandStatus = false;
                failureMessage = "No file command was selected";
            }

            else if (selected.getName().equals(CMD_OPEN_FILE)) {
                if (fileOutput.isWriting()) {
                    commandStatus = false;
                    failureMessage = "Video recording is already active";
                }
                else if (!parentSensor.isStreamReady()) {
                    commandStatus = false;
                    failureMessage = "Video input stream is not available";
                }

                else {
                    fileName = Paths.get("clips", parentSensor.getUniqueIdentifier().replace(':', '-'),
                            selected.getData().getStringValue()).toString();

                    if (!fileName.contains(".")) {
                        fileName += ".mp4";
                    }
                    workingFileName = partialFileName(fileName);

                    try {
                        this.fileOutput.openFile(bucketStore.getWritableResourceURI(
                                VIDEO_BUCKET, workingFileName));
                        this.parentSensor.getLogger().trace("Writing hidden partial for file: {}", fileName);
                    } catch (Exception e) {
                        getLogger().error("Exception while opening MP4 output", e);
                        try {
                            if (!workingFileName.isBlank())
                                bucketStore.deleteObject(VIDEO_BUCKET, workingFileName);
                        } catch (Exception cleanupError) {
                            getLogger().debug("Unable to remove incomplete MP4 output {}", workingFileName, cleanupError);
                        }
                        fileName = "";
                        workingFileName = "";
                        commandStatus = false;
                        failureMessage = "Unable to start video recording: " + e.getMessage();
                    }
                }
            } else if (selected.getName().equals(CMD_CLOSE_FILE)) {
                if (!fileOutput.isWriting()) {
                    commandStatus = false;
                    failureMessage = "Video recording is not active";
                }
                else {
                    boolean saveFile = ((Boolean) selected).getValue();
                    try {
                        var processor = this.parentSensor.getProcessor();
                        if (processor == null) {
                            this.fileOutput.closeFile();
                        } else {
                            processor.executeAfterPendingVideoData(fileOutput, () -> {
                                fileOutput.closeFile();
                                return null;
                            });
                        }

                        // Delete file if we do not want to save
                        if (!saveFile) {
                            bucketStore.deleteObject(VIDEO_BUCKET, workingFileName);
                            this.parentSensor.getLogger().trace("Discarded file: {}", fileName);
                        } else {
                            publishCompletedFile(workingFileName, fileName);
                            this.parentSensor.getLogger().trace("Saved file: {}", fileName);
                            fileNameTemp = fileName;
                            reportFileName = true;
                        }
                        fileName = "";
                        workingFileName = "";
                    } catch (Exception e) {
                        // A concurrent processor shutdown can reject or cancel the ordered close
                        // boundary. Always release the native output context before deleting its
                        // incomplete bucket object.
                        if (fileOutput.isWriting()) {
                            try {
                                fileOutput.closeFile();
                            } catch (Exception cleanupError) {
                                e.addSuppressed(cleanupError);
                            }
                        }
                        getLogger().error("Exception while closing MP4 output", e);
                        try {
                            if (!workingFileName.isBlank())
                                bucketStore.deleteObject(VIDEO_BUCKET, workingFileName);
                        } catch (Exception cleanupError) {
                            getLogger().debug("Unable to remove incomplete MP4 output {}", workingFileName, cleanupError);
                        }
                        fileName = "";
                        workingFileName = "";
                        commandStatus = false;
                        failureMessage = "Unable to stop video recording: " + e.getMessage();
                    }
                }
            } else {
                commandStatus = false;
                failureMessage = "Unsupported file command: " + selected.getName();
            }

            CommandStatus.Builder status = new CommandStatus.Builder()
                    .withCommand(command.getID())
                    .withStatusCode(commandStatus ? ICommandStatus.CommandStatusCode.ACCEPTED : ICommandStatus.CommandStatusCode.FAILED);
            if (failureMessage != null)
                status.withMessage(failureMessage);

            if (commandStatus && reportFileName) {
                try {
                    resultData.renewDataBlock();
                    resultData.getData().setStringValue(bucketStore.getRelativeResourceURI(VIDEO_BUCKET, fileNameTemp));
                    ICommandResult result = CommandResult.withData(resultData.getData());
                    status.withResult(result);
                } catch (Exception ignored) {}
            }
            return status.build();
    }

    private void publishCompletedFile(String partialKey, String finalKey) throws Exception {
        if (partialKey == null || partialKey.isBlank() || finalKey == null || finalKey.isBlank())
            throw new IOException("Recording paths are unavailable");
        Path partial = Path.of(bucketStore.getWritableResourceURI(VIDEO_BUCKET, partialKey))
                .toAbsolutePath().normalize();
        Path completed = Path.of(bucketStore.getWritableResourceURI(VIDEO_BUCKET, finalKey))
                .toAbsolutePath().normalize();
        publishCompletedPath(partial, completed, FileControl::forceDirectory);
    }

    /**
     * Atomically exposes a completed recording without ever replacing an existing final object.
     * A hard link is used because {@code ATOMIC_MOVE} is allowed to replace a target on some
     * providers even when {@code REPLACE_EXISTING} is absent. The partial name is removed only
     * after the final name and its directory have been forced. Once the final link exists, no
     * failure path removes it; a caller may safely clean up only the hidden partial name.
     */
    static void publishCompletedPath(Path partial, Path completed,
                                     DirectoryForcer directoryForcer) throws IOException {
        if (!Files.isRegularFile(partial) || Files.size(partial) <= 0)
            throw new IOException("Completed MP4 partial is missing or empty");
        if (!Objects.equals(partial.getParent(), completed.getParent()))
            throw new IOException("Completed MP4 must be published in the partial's directory");
        try (FileChannel channel = FileChannel.open(partial, StandardOpenOption.WRITE)) {
            channel.force(true);
        }

        // CREATE_LINK is an atomic, no-replace namespace operation: FileAlreadyExistsException is
        // raised rather than silently replacing an immutable completed recording.
        Files.createLink(completed, partial);
        if (!Files.isRegularFile(completed) || Files.size(completed) <= 0)
            throw new IOException("Published MP4 could not be verified");
        directoryForcer.force(completed.getParent());
        Files.delete(partial);
        directoryForcer.force(completed.getParent());
    }

    private static String partialFileName(String finalName) {
        int separator = Math.max(finalName.lastIndexOf('/'), finalName.lastIndexOf('\\'));
        int extension = finalName.lastIndexOf('.');
        String marker = ".partial-" + UUID.randomUUID();
        return extension > separator
                ? finalName.substring(0, extension) + marker + finalName.substring(extension)
                : finalName + marker;
    }

    private static void forceDirectory(Path directory) throws IOException {
        if (directory == null)
            return;
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (UnsupportedOperationException ignored) {
            // Some supported filesystems do not expose directory channels.
        } catch (IOException failure) {
            // Windows does not expose directories as readable FileChannels and commonly reports
            // AccessDeniedException here even though the same-directory atomic rename succeeded.
            // Do not turn that platform limitation into deletion of an otherwise complete MP4.
            // POSIX errors still propagate because they can indicate a real durability failure.
            if (!isIgnorableWindowsDirectoryFsyncFailure(failure, isWindows()))
                throw failure;
        }
    }

    static boolean isIgnorableWindowsDirectoryFsyncFailure(IOException failure,
                                                             boolean windows) {
        return windows && failure instanceof AccessDeniedException;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
                .startsWith("windows");
    }

    @FunctionalInterface
    interface DirectoryForcer {
        void force(Path directory) throws IOException;
    }
}
