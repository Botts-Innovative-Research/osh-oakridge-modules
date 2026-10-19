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
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class FileControl<FFmpegConfigType extends FFMPEGConfig> extends AbstractSensorControl<FFMPEGSensorBase<FFmpegConfigType>> implements FFmpegControl {
    public static final String SENSOR_CONTROL_NAME = "ffmpegFileControl";
    private static final String SENSOR_CONTROL_LABEL = "FFmpeg File Control";
    public static final String CMD_OPEN_FILE = "startFile";
    public static final String CMD_CLOSE_FILE = "endFile";
    public static final String FILE_IO = "fileIO";
    //public static final String VIDEO_BUCKET = Constants.VIDEO_BUCKET;
    public static final String VIDEO_BUCKET = "videos";
    // The lane coordinator exposes an eight-second command deadline. Five seconds accommodates a
    // slow camera GOP/startup while reserving three seconds for trailer flush, durable publication,
    // and command propagation before that outer deadline.
    static final Duration RECORDING_READINESS_TIMEOUT = Duration.ofSeconds(5);
    // Five independently muxed video packets are the minimum input from which OSCAR can retain
    // FIRST/QUARTER/MIDDLE/THREE_QUARTERS/LAST evidence. A short occupancy therefore receives a
    // bounded post-roll instead of publishing an MP4 that can never enter five-frame retention.
    static final int MINIMUM_RETAINABLE_VIDEO_PACKETS = 5;
    static final Duration RECORDING_COMPLETENESS_TIMEOUT = Duration.ofSeconds(5);

    private final IBucketStore bucketStore;
    DataRecord commandData;
    DataRecord resultData;
    String fileName = "";
    String workingFileName = "";
    private final FileOutput<?> fileOutput;
    private final AsyncCommandQueue<ICommandStatus> commandQueue = new AsyncCommandQueue<>();

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

    /**
     * Removes a completed output that lost its occupancy publication race during stop/restart.
     * Only this control's exact relative camera directory is accepted.
     */
    public CompletableFuture<Void> discardCompletedFile(String relativePath) {
        final String objectKey;
        try {
            String normalized = Objects.requireNonNull(relativePath, "relativePath")
                    .replace('\\', '/');
            if (normalized.startsWith("/") || normalized.contains("://")
                    || normalized.indexOf('\0') >= 0)
                throw new IllegalArgumentException("Completed video path must be relative");
            if (normalized.startsWith(VIDEO_BUCKET + "/"))
                normalized = normalized.substring(VIDEO_BUCKET.length() + 1);
            String cameraPrefix = "clips/"
                    + parentSensor.getUniqueIdentifier().replace(':', '-') + "/";
            String leaf = normalized.startsWith(cameraPrefix)
                    ? normalized.substring(cameraPrefix.length()) : "";
            if (leaf.isBlank() || leaf.contains("/") || leaf.equals(".") || leaf.equals("..")
                    || leaf.contains(".partial-"))
                throw new IllegalArgumentException("Completed video path is outside this camera");
            objectKey = normalized;
        } catch (RuntimeException invalid) {
            return CompletableFuture.failedFuture(invalid);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                if (bucketStore.objectExists(VIDEO_BUCKET, objectKey))
                    bucketStore.deleteObject(VIDEO_BUCKET, objectKey);
            } catch (Exception failure) {
                throw new CompletionException(failure);
            }
        });
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
        // A synchronized execute method prevents simultaneous mutations, but common-pool tasks
        // can acquire that monitor in a different order than submitCommand was called. The FIFO
        // preserves the required open -> close/cancel -> next open camera lifecycle.
        return commandQueue.submit(() -> CompletableFuture.supplyAsync(() -> executeCommand(command)));
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
                    final String requestedName;
                    try {
                        requestedName = validateRequestedFileName(
                                selected.getData().getStringValue());
                    } catch (IllegalArgumentException invalidName) {
                        commandStatus = false;
                        failureMessage = invalidName.getMessage();
                        return status(command, commandStatus, failureMessage, false, "");
                    }
                    fileName = Paths.get("clips", parentSensor.getUniqueIdentifier().replace(':', '-'),
                            requestedName).toString();

                    if (!requestedName.endsWith(".mp4")) {
                        fileName += ".mp4";
                    }
                    workingFileName = partialFileName(fileName);

                    try {
                        this.fileOutput.openFile(bucketStore.getWritableResourceURI(
                                VIDEO_BUCKET, workingFileName));
                        this.fileOutput.awaitRecordingReady(RECORDING_READINESS_TIMEOUT);
                        this.parentSensor.getLogger().trace("Writing hidden partial for file: {}", fileName);
                    } catch (Exception e) {
                        getLogger().error("Exception while opening MP4 output", e);
                        if (fileOutput.isWriting()) {
                            try {
                                fileOutput.cancelFile();
                            } catch (Exception cleanupError) {
                                e.addSuppressed(cleanupError);
                            }
                        }
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
                boolean saveFile = ((Boolean) selected).getValue();
                if (!fileOutput.isWriting()) {
                    if (saveFile) {
                        commandStatus = false;
                        failureMessage = "Video recording is not active";
                    } else {
                        // Discard is an idempotent cancellation. In particular, an open that could
                        // not receive a packet must not turn a non-alarm occupancy into an error.
                        try {
                            discardInactivePartial(workingFileName,
                                    () -> bucketStore.deleteObject(VIDEO_BUCKET, workingFileName));
                        } catch (Exception cleanupError) {
                            getLogger().error("Unable to remove canceled MP4 output {}",
                                    workingFileName, cleanupError);
                            commandStatus = false;
                            failureMessage = "Unable to remove canceled video recording: "
                                    + cleanupError.getMessage();
                        }
                        fileName = "";
                        workingFileName = "";
                    }
                } else {
                    try {
                        if (saveFile) {
                            fileOutput.awaitRecordingPacketCount(
                                    MINIMUM_RETAINABLE_VIDEO_PACKETS,
                                    RECORDING_COMPLETENESS_TIMEOUT);
                        }
                        var processor = this.parentSensor.getProcessor();
                        if (processor == null) {
                            finishOutput(saveFile, this.fileOutput::closeFile,
                                    this.fileOutput::cancelFile);
                        } else {
                            processor.executeAfterPendingVideoData(fileOutput, () -> {
                                finishOutput(saveFile, fileOutput::closeFile,
                                        fileOutput::cancelFile);
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
                        // publishCompletedPath deliberately never removes a final hard link after
                        // it has been created. If a later durability/partial-cleanup step failed,
                        // include that final object in the failed status so the occupancy owner can
                        // validate and remove the unpublished file rather than leaking it.
                        if (saveFile && e instanceof FinalObjectCreatedException) {
                            fileNameTemp = fileName;
                            reportFileName = true;
                        }
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
                        getLogger().error("Exception while finalizing MP4 output", e);
                        try {
                            if (!workingFileName.isBlank())
                                bucketStore.deleteObject(VIDEO_BUCKET, workingFileName);
                        } catch (Exception cleanupError) {
                            getLogger().error("Unable to remove incomplete MP4 output {}", workingFileName, cleanupError);
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

            return status(command, commandStatus, failureMessage, reportFileName, fileNameTemp);
    }

    private ICommandStatus status(ICommandData command, boolean commandStatus,
                                  String failureMessage, boolean reportFileName,
                                  String completedFileName) {
        CommandStatus.Builder status = new CommandStatus.Builder()
                .withCommand(command.getID())
                .withStatusCode(commandStatus ? ICommandStatus.CommandStatusCode.ACCEPTED
                        : ICommandStatus.CommandStatusCode.FAILED);
        if (failureMessage != null)
            status.withMessage(failureMessage);

        if (reportFileName) {
            try {
                resultData.renewDataBlock();
                resultData.getData().setStringValue(
                        bucketStore.getRelativeResourceURI(VIDEO_BUCKET, completedFileName));
                ICommandResult result = CommandResult.withData(resultData.getData());
                status.withResult(result);
            } catch (Exception pathFailure) {
                // Completed recording keys are created exclusively from a validated safe leaf.
                // Preserve cleanup observability even if a bucket implementation cannot format
                // its relative URI after publication.
                getLogger().error("Unable to format completed video URI {}; using canonical key",
                        completedFileName, pathFailure);
                resultData.renewDataBlock();
                resultData.getData().setStringValue(
                        Paths.get(VIDEO_BUCKET, completedFileName).toString());
                status.withResult(CommandResult.withData(resultData.getData()));
            }
        }
        return status.build();
    }

    static String validateRequestedFileName(String requestedName) {
        if (requestedName == null || requestedName.isBlank()
                || !requestedName.equals(requestedName.trim()))
            throw new IllegalArgumentException("Video file name must not be blank or padded");
        boolean safeStem = requestedName.matches("[A-Za-z0-9][A-Za-z0-9_-]*");
        boolean safeMp4 = requestedName.matches("[A-Za-z0-9][A-Za-z0-9_-]*\\.mp4");
        if (requestedName.length() > 200 || (!safeStem && !safeMp4))
            throw new IllegalArgumentException("Video file name must be one safe relative filename");
        return requestedName;
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

    /** Selects strict saved-recording finalization or tolerant discard finalization. */
    static void finishOutput(boolean saveFile, OutputFinalizer close,
                             OutputFinalizer cancel) throws Exception {
        if (saveFile)
            close.run();
        else
            cancel.run();
    }

    /**
     * Removes a partial left by an output that stopped before receiving its first packet.
     * Absence is idempotent success, while a real lookup/delete failure is surfaced to the
     * command status so cancellation cannot silently leak hidden media.
     */
    static void discardInactivePartial(String partialKey, OutputFinalizer delete) throws Exception {
        if (partialKey == null || partialKey.isBlank())
            return;
        // Bucket stores intentionally hide partial objects from objectExists/listObjects. Their
        // delete operation is idempotent, so address the known private key directly.
        delete.run();
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
        try {
            if (!Files.isRegularFile(completed) || Files.size(completed) <= 0)
                throw new IOException("Published MP4 could not be verified");
            directoryForcer.force(completed.getParent());
            Files.delete(partial);
            directoryForcer.force(completed.getParent());
        } catch (IOException postLinkFailure) {
            // Distinguish a final object created by this attempt from createLink failing because
            // an immutable object already existed. Only the former may be handed to the occupancy
            // coordinator for unpublished-output cleanup.
            throw new FinalObjectCreatedException(postLinkFailure);
        }
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

    static final class FinalObjectCreatedException extends IOException {
        private FinalObjectCreatedException(IOException cause) {
            super(cause.getMessage(), cause);
        }
    }

    @FunctionalInterface
    interface OutputFinalizer {
        void run() throws Exception;
    }
}
