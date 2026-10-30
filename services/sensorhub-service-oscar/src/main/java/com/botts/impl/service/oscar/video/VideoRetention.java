package com.botts.impl.service.oscar.video;

import com.botts.api.service.bucket.IBucketStore;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.PointerPointer;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref;

public class VideoRetention {

    private static final Logger logger = LoggerFactory.getLogger(VideoRetention.class);
    private static final int MAX_PROCESS_TIME_MULTIPLIER = 5;
    private static final int DEFAULT_VIDEO_DURATION_MS = 100_000;
    private static final long STOP_JOIN_TIMEOUT_MS = 5_000L;
    private static final int FFMPEG_EAGAIN = -11;

    final IBucketStore bucketStore;
    volatile boolean hasStarted = false;
    int frameCount;

    SlidingOccupancyQuery retentionQuery;

    public VideoRetention(ISensorHub hub, IBucketStore bucketStore, TemporalAmount queryPeriod,
                          TemporalAmount retentionTimeOffset, int frameCount) {
        this.bucketStore = bucketStore;
        this.frameCount = frameCount;

        SlidingOccupancyQuery.QueryAction queryAction = frameCount > 0
                ? this::decimateOccupancyVideo : this::deleteOccupancyVideo;

        retentionQuery = new SlidingOccupancyQuery(hub,
                queryPeriod,
                queryAction,
                retentionTimeOffset);
    }

    public synchronized void start() {
        if (hasStarted) {
            logger.warn("Video retention already started");
            return;
        }

        hasStarted = true;
        retentionQuery.start();
    }

    private boolean deleteOccupancyVideo(Occupancy occupancy) {
        for (String videoFile : occupancy.getVideoPaths()) {
            logger.info("deleting, {}", videoFile);
            if (!bucketStore.objectExists("", videoFile)) {
                logger.info("Video file {} does not exist or has already been deleted", videoFile);
                return false;
            }
            try {
                bucketStore.deleteObject("", videoFile);
            } catch (DataStoreException e) {
                logger.info("Failed to delete video file {}", videoFile, e);
                return false;
            }
        }
        return true;
    }

    private boolean decimateOccupancyVideo(Occupancy occupancy) {
        for (String videoFile : occupancy.getVideoPaths()) {
            logger.info("decimating, {}", videoFile);
            if (!bucketStore.objectExists("", videoFile)) {
                logger.info("Video file {} does not exist", videoFile);
                continue;
            }
            try {
                if (!decimate(bucketStore.getResourceURI("", videoFile))) {
                    logger.info("Video file was not decimated; it was already below one frame per second "
                            + "or processing failed: {}", videoFile);
                    return false;
                }
            } catch (DataStoreException e) {
                logger.warn("Failed to decimate video file {}", videoFile, e);
                continue;
            }
        }
        return true;
    }

    public synchronized void stop() {
        if (!hasStarted) {
            return;
        }

        hasStarted = false;
        retentionQuery.stop();
    }

    /**
     * Decimates a local MP4 without ever deleting the source first. The completed temporary file
     * is reopened and read through FFmpeg before a backup-protected replacement is attempted.
     *
     * @param fileName result of getResourceURI on an object from the bucket store
     * @return true when a validated replacement was installed; false for an already-decimated
     *         source or any processing/validation/replacement failure
     */
    public boolean decimate(String fileName) {
        Path original = Paths.get(fileName).toAbsolutePath().normalize();
        Path temporary = null;
        MpegTsProcessor videoInput = new MpegTsProcessor(original.toString());
        VideoKeyframeDecimator videoOutput = null;
        MpegTsProcessor.VideoStreamSnapshot stream = null;
        AtomicReference<Throwable> processingFailure = new AtomicReference<>();
        boolean processingStarted = false;

        try {
            if (!Files.isRegularFile(original)) {
                logger.warn("Video source does not exist or is not a regular file: {}", original);
                return false;
            }

            videoInput.setInjectExtradata(false);
            if (!videoInput.openStream()) {
                logger.warn("Could not open video source for decimation: {}", original);
                return false;
            }
            videoInput.queryEmbeddedStreams();
            stream = videoInput.getVideoStreamSnapshot();
            if (stream == null) {
                logger.warn("Video source metadata is unavailable: {}", original);
                return false;
            }

            // A valid 0/0 average frame rate means "unknown", not "already decimated". Only a
            // known, positive rate below one FPS is an already-decimated signal.
            if (isAlreadyDecimated(stream.averageFrameRateNumerator(),
                    stream.averageFrameRateDenominator())) {
                return false;
            }

            Path parent = original.getParent();
            if (parent == null) {
                parent = Paths.get(".").toAbsolutePath().normalize();
            }
            temporary = Files.createTempFile(parent,
                    safeTempPrefix(original.getFileName().toString()), ".decimated.mp4");

            videoOutput = new VideoKeyframeDecimator(temporary.toString(), frameCount, stream);
            videoOutput.setFileCloseCallback(videoInput::stopProcessingStream);
            // File input reaches EOF before processStream returns. Inline delivery guarantees the
            // final packet callback has completed before retention finalizes and validates output.
            videoInput.addVideoDataBufferListener(videoOutput, false);
            videoInput.setUncaughtExceptionHandler((thread, failure) -> processingFailure.set(failure));

            long maxDuration = maximumProcessingDurationMillis(stream.duration(),
                    stream.timeBaseNumerator(), stream.timeBaseDenominator());
            videoInput.processStream();
            processingStarted = true;
            videoInput.join(maxDuration);
            if (videoInput.isAlive()) {
                throw new TimeoutException("Video decimation timed out after " + maxDuration + " ms");
            }
            if (processingFailure.get() != null) {
                throw new IOException("Video input processing failed", processingFailure.get());
            }

            videoOutput.closeFile();
            videoOutput = null;
            forceFile(temporary);
            if (!validateDecimatedFile(temporary)) {
                throw new IOException("FFmpeg could not reopen and read the decimated video");
            }

            replaceOriginalWithValidatedTemp(original, temporary);
            temporary = null;
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while decimating {}", original, e);
            return false;
        } catch (Exception e) {
            logger.error("Could not decimate {}; the original was preserved", original, e);
            return false;
        } finally {
            videoInput.stopProcessingStream();
            if (processingStarted && videoInput.isAlive()) {
                try {
                    videoInput.join(STOP_JOIN_TIMEOUT_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (videoInput.isAlive()) {
                // Do not release native input/output state underneath a worker that did not honor
                // the bounded stop request. A daemon reaper owns all remaining cleanup and waits
                // for the processor to leave FFmpeg before closing or deleting anything.
                logger.warn("Video processor is still stopping; deferring native cleanup for {}", original);
                deferCleanup(videoInput, videoOutput, stream, temporary);
            } else {
                cleanupResources(videoInput, videoOutput, stream, temporary);
            }
        }
    }

    private static void deferCleanup(MpegTsProcessor videoInput,
                                     VideoKeyframeDecimator videoOutput,
                                     MpegTsProcessor.VideoStreamSnapshot stream,
                                     Path temporary) {
        Thread reaper = new Thread(() -> {
            boolean interrupted = false;
            while (videoInput.isAlive()) {
                try {
                    videoInput.join();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            cleanupResources(videoInput, videoOutput, stream, temporary);
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }, "OSCAR-VIDEO-RETENTION-CLEANUP");
        reaper.setDaemon(true);
        reaper.start();
    }

    private static void cleanupResources(MpegTsProcessor videoInput,
                                         VideoKeyframeDecimator videoOutput,
                                         MpegTsProcessor.VideoStreamSnapshot stream,
                                         Path temporary) {
        videoInput.closeStream();
        if (videoOutput != null) {
            try {
                videoOutput.closeFile();
            } catch (Exception e) {
                logger.debug("Unable to finalize failed decimated output {}", temporary, e);
            }
        }
        if (stream != null && stream.codecParameters() != null) {
            avcodec_parameters_free(stream.codecParameters());
        }
        if (temporary != null) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException e) {
                logger.warn("Failed to delete partial decimated file {}", temporary, e);
            }
        }
    }

    static boolean isAlreadyDecimated(int frameRateNumerator, int frameRateDenominator) {
        return frameRateNumerator > 0 && frameRateDenominator > 0
                && frameRateNumerator < frameRateDenominator;
    }

    static long maximumProcessingDurationMillis(long duration, int timeBaseNumerator,
                                                int timeBaseDenominator) {
        if (duration <= 0 || timeBaseNumerator <= 0 || timeBaseDenominator <= 0) {
            return (long) DEFAULT_VIDEO_DURATION_MS * MAX_PROCESS_TIME_MULTIPLIER;
        }
        double durationMillis = duration * (double) timeBaseNumerator
                / timeBaseDenominator * 1_000.0;
        double budgetMillis = Math.ceil(durationMillis * MAX_PROCESS_TIME_MULTIPLIER);
        if (!Double.isFinite(budgetMillis) || budgetMillis <= 0 || budgetMillis > Long.MAX_VALUE) {
            return (long) DEFAULT_VIDEO_DURATION_MS * MAX_PROCESS_TIME_MULTIPLIER;
        }
        return Math.max(1L, (long) budgetMillis);
    }

    private static String safeTempPrefix(String fileName) {
        String prefix = fileName.replaceAll("[^A-Za-z0-9._-]", "_") + ".";
        return prefix.length() >= 3 ? prefix : "oscar-video.";
    }

    private static void forceFile(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    static boolean validateDecimatedFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return false;
        }
        try {
            if (Files.size(file) <= 0) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        AVFormatContext context = new AVFormatContext(null);
        AVPacket packet = null;
        AVFrame frame = null;
        AVCodecContext decoderContext = null;
        try {
            if (avformat_open_input(context, file.toString(), null, null) < 0) {
                return false;
            }
            if (avformat_find_stream_info(context, (org.bytedeco.javacpp.PointerPointer<?>) null) < 0) {
                return false;
            }

            int videoStream = -1;
            for (int i = 0; i < context.nb_streams(); i++) {
                var codec = context.streams(i).codecpar();
                if (codec.codec_type() == AVMEDIA_TYPE_VIDEO && codec.codec_id() != AV_CODEC_ID_NONE
                        && codec.width() > 0 && codec.height() > 0) {
                    videoStream = i;
                    break;
                }
            }
            if (videoStream < 0) {
                return false;
            }

            var codecParameters = context.streams(videoStream).codecpar();
            AVCodec decoder = avcodec_find_decoder(codecParameters.codec_id());
            if (decoder == null || decoder.isNull()) {
                return false;
            }
            decoderContext = avcodec_alloc_context3(decoder);
            if (decoderContext == null || decoderContext.isNull()
                    || avcodec_parameters_to_context(decoderContext, codecParameters) < 0
                    || avcodec_open2(decoderContext, decoder, (PointerPointer<?>) null) < 0) {
                return false;
            }

            packet = av_packet_alloc();
            frame = av_frame_alloc();
            if (packet == null || packet.isNull() || frame == null || frame.isNull()) {
                return false;
            }
            int packetsInspected = 0;
            while (packetsInspected++ < 10_000 && av_read_frame(context, packet) >= 0) {
                try {
                    if (packet.stream_index() != videoStream || packet.size() <= 0
                            || packet.data() == null || packet.data().isNull()) {
                        continue;
                    }

                    int sendResult = avcodec_send_packet(decoderContext, packet);
                    if (sendResult == FFMPEG_EAGAIN) {
                        int drainResult = receiveDecodedFrame(decoderContext, frame);
                        if (drainResult > 0) {
                            return true;
                        }
                        if (drainResult < 0) {
                            return false;
                        }
                        sendResult = avcodec_send_packet(decoderContext, packet);
                    }
                    if (sendResult < 0) {
                        logger.debug("FFmpeg rejected a validation packet with error {}", sendResult);
                        return false;
                    }

                    int drainResult = receiveDecodedFrame(decoderContext, frame);
                    if (drainResult > 0) {
                        return true;
                    }
                    if (drainResult < 0) {
                        return false;
                    }
                } finally {
                    av_packet_unref(packet);
                }
            }

            // Delayed codecs may not expose their first frame until EOF is signaled. Drain any
            // pending output before deciding that an otherwise valid compacted clip is corrupt.
            for (int flushAttempt = 0; flushAttempt < 2; flushAttempt++) {
                int flushResult = avcodec_send_packet(decoderContext, null);
                if (flushResult == 0 || flushResult == avutil.AVERROR_EOF()) {
                    break;
                }
                if (flushResult != FFMPEG_EAGAIN) {
                    logger.debug("FFmpeg decoder flush failed with error {}", flushResult);
                    return false;
                }
                int drainResult = receiveDecodedFrame(decoderContext, frame);
                if (drainResult > 0) {
                    return true;
                }
                if (drainResult < 0) {
                    return false;
                }
            }
            return receiveDecodedFrame(decoderContext, frame) > 0;
        } catch (Exception e) {
            logger.debug("Decimated video validation failed for {}", file, e);
            return false;
        } finally {
            if (packet != null) {
                av_packet_free(packet);
            }
            if (frame != null) {
                av_frame_free(frame);
            }
            if (decoderContext != null) {
                avcodec_free_context(decoderContext);
            }
            if (context != null && !context.isNull()) {
                avformat_close_input(context);
            }
        }
    }

    /**
     * @return 1 after decoding a usable frame, 0 when the decoder needs more input or reached EOF,
     *         and -1 for an actual decoder failure
     */
    private static int receiveDecodedFrame(AVCodecContext decoderContext, AVFrame frame) {
        while (true) {
            int receiveResult = avcodec_receive_frame(decoderContext, frame);
            if (receiveResult == FFMPEG_EAGAIN || receiveResult == avutil.AVERROR_EOF()) {
                return 0;
            }
            if (receiveResult < 0) {
                logger.debug("FFmpeg validation decode failed with error {}", receiveResult);
                return -1;
            }

            boolean usable = frame.width() > 0 && frame.height() > 0
                    && frame.data(0) != null && !frame.data(0).isNull();
            av_frame_unref(frame);
            if (usable) {
                return 1;
            }
        }
    }

    /**
     * Replaces the original only after keeping a same-directory backup. Atomic replacement is
     * preferred; the backup also makes the non-atomic fallback recoverable on filesystems that do
     * not implement ATOMIC_MOVE.
     */
    static void replaceOriginalWithValidatedTemp(Path original, Path temporary) throws IOException {
        if (!Files.isRegularFile(original)) {
            throw new IOException("Original video is missing: " + original);
        }
        if (!Files.isRegularFile(temporary) || Files.size(temporary) <= 0) {
            throw new IOException("Validated temporary video is missing or empty: " + temporary);
        }

        Path parent = original.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IOException("Original video has no parent directory: " + original);
        }
        Path backup = Files.createTempFile(parent,
                safeTempPrefix(original.getFileName().toString()), ".retention-backup");
        Files.delete(backup);
        boolean backupExists = false;
        boolean replaced = false;
        try {
            try {
                Files.createLink(backup, original);
            } catch (UnsupportedOperationException | IOException linkFailure) {
                Files.copy(original, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
            backupExists = true;

            long expectedSize = Files.size(temporary);
            try {
                Files.move(temporary, original,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, original, StandardCopyOption.REPLACE_EXISTING);
            }
            replaced = Files.isRegularFile(original) && Files.size(original) == expectedSize;
            if (!replaced) {
                throw new IOException("Replacement video could not be verified after move");
            }
        } catch (Exception replacementFailure) {
            if (backupExists) {
                try {
                    Files.copy(backup, original,
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    if (!Files.isRegularFile(original) || Files.size(original) != Files.size(backup)) {
                        throw new IOException("Restored source video could not be verified");
                    }
                } catch (Exception restoreFailure) {
                    replacementFailure.addSuppressed(restoreFailure);
                    // Keep the backup on disk when restoration fails so the source remains
                    // recoverable even if its original pathname could not be restored.
                    backupExists = false;
                }
            }
            if (replacementFailure instanceof IOException) {
                throw (IOException) replacementFailure;
            }
            throw new IOException("Could not replace original video", replacementFailure);
        } finally {
            if (backupExists) {
                try {
                    Files.deleteIfExists(backup);
                } catch (IOException e) {
                    if (replaced) {
                        logger.warn("Replacement succeeded but retention backup could not be removed: {}",
                                backup, e);
                    }
                }
            }
        }
    }
}
