package com.botts.impl.service.oscar.video;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.javacpp.BytePointer;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

// TODO Should this be moved to the ffmpeg driver package?
public class VideoKeyframeDecimator implements DataBufferListener {
    private static final Logger logger = LoggerFactory.getLogger(VideoKeyframeDecimator.class);
    static final String LISTENER_NAME = "KEYFRAME-DECIMATOR";

    private final String outputFileName;
    private final int totalKeyframes;
    private final Object lock = new Object();

    private volatile boolean writing;
    private boolean closed;
    private boolean headerWritten;
    private long packetsWritten;
    private long outputDuration;
    private long keyframeDuration;
    private long currentDecimatedFrame;
    private long firstInputTimestamp = AV_NOPTS_VALUE;
    private IOException failure;

    private AVFormatContext formatContext;
    private AVStream outputStream;
    private AVRational outputTimeBase;
    private Runnable closeCallback;

    VideoKeyframeDecimator(String outputFileName, int totalKeyframes,
                           MpegTsProcessor.VideoStreamSnapshot inputStream) throws IOException {
        this.outputFileName = Objects.requireNonNull(outputFileName, "outputFileName");
        if (totalKeyframes <= 0) {
            throw new IOException("Decimated video must contain at least one keyframe");
        }
        this.totalKeyframes = totalKeyframes;

        try {
            openOutputFile(inputStream);
            writing = true;
        } catch (Exception e) {
            IOException openFailure = asIOException("Could not open decimated output " + outputFileName, e);
            IOException cleanupFailure = cleanupOutput(false);
            if (cleanupFailure != null) {
                openFailure.addSuppressed(cleanupFailure);
            }
            closed = true;
            throw openFailure;
        }
    }

    private void openOutputFile(MpegTsProcessor.VideoStreamSnapshot inputStream) throws IOException {
        if (inputStream == null || inputStream.codecParameters() == null) {
            throw new IOException("Input video metadata is not available");
        }
        AVCodecParameters inputCodec = inputStream.codecParameters();
        if (inputCodec.codec_type() != AVMEDIA_TYPE_VIDEO || inputCodec.codec_id() == AV_CODEC_ID_NONE
                || inputCodec.width() <= 0 || inputCodec.height() <= 0) {
            throw new IOException("Input video codec metadata is incomplete");
        }
        if (inputStream.timeBaseNumerator() <= 0 || inputStream.timeBaseDenominator() <= 0) {
            throw new IOException("Input video time base is invalid");
        }
        formatContext = new AVFormatContext(null);
        int ret = avformat_alloc_output_context2(formatContext, null, "mp4", outputFileName);
        if (ret < 0 || formatContext == null || formatContext.isNull()
                || formatContext.oformat() == null || formatContext.oformat().isNull()) {
            throw ffmpegIOException("Could not allocate MP4 output context", ret);
        }

        outputStream = avformat_new_stream(formatContext, null);
        if (outputStream == null || outputStream.isNull()) {
            throw new IOException("Could not allocate decimated video stream");
        }
        ret = avcodec_parameters_copy(outputStream.codecpar(), inputCodec);
        if (ret < 0) {
            throw ffmpegIOException("Could not copy input video codec parameters", ret);
        }
        outputStream.codecpar().codec_tag(0);

        AVRational inputTimeBase = av_make_q(
                inputStream.timeBaseNumerator(), inputStream.timeBaseDenominator());
        outputStream.time_base(inputTimeBase);

        long inputDuration = inputStream.duration();
        if (inputDuration <= 0) {
            // MpegTsProcessor already falls back to the container duration. If neither duration is
            // known, guessing can select only one frame from a short clip and destroy useful data.
            throw new IOException("Input video duration is not available; preserving the source");
        }

        double durationSeconds = inputDuration
                * (double) inputStream.timeBaseNumerator() / inputStream.timeBaseDenominator();
        double decimatedFrameRate = totalKeyframes / durationSeconds;
        if (!Double.isFinite(decimatedFrameRate) || decimatedFrameRate <= 0.0) {
            throw new IOException("Could not calculate decimated video frame rate");
        }
        AVRational frameRate = av_d2q(decimatedFrameRate, 1_000_000);
        if (frameRate == null || frameRate.num() <= 0 || frameRate.den() <= 0) {
            throw new IOException("Could not represent decimated video frame rate");
        }
        outputStream.avg_frame_rate(frameRate);
        outputStream.r_frame_rate(frameRate);

        if ((formatContext.oformat().flags() & AVFMT_NOFILE) == 0) {
            AVIOContext ioContext = new AVIOContext(null);
            ret = avio_open(ioContext, outputFileName, AVIO_FLAG_WRITE);
            if (ret < 0) {
                throw ffmpegIOException("Could not open decimated output file", ret);
            }
            formatContext.pb(ioContext);
        }

        ret = avformat_write_header(formatContext, (AVDictionary) null);
        if (ret < 0) {
            throw ffmpegIOException("Could not write decimated output header", ret);
        }
        headerWritten = true;

        AVRational muxerTimeBase = outputStream.time_base();
        if (muxerTimeBase == null || muxerTimeBase.num() <= 0 || muxerTimeBase.den() <= 0) {
            throw new IOException("MP4 muxer did not provide a valid time base");
        }
        outputTimeBase = av_make_q(muxerTimeBase.num(), muxerTimeBase.den());
        outputDuration = av_rescale_q(inputDuration, inputTimeBase, outputTimeBase);
        if (outputDuration <= 0) {
            throw new IOException("Could not rescale input video duration");
        }
        keyframeDuration = Math.max(1L, outputDuration / totalKeyframes);
    }

    @Override
    public void onDataBuffer(DataBufferRecord record) {
        synchronized (lock) {
            if (!writing || failure != null || !record.isKeyFrame()) {
                return;
            }

            AVPacket packet = null;
            try {
                long inputTimestamp = inputTimestamp(record);
                if (firstInputTimestamp == AV_NOPTS_VALUE) {
                    firstInputTimestamp = inputTimestamp;
                }
                long relativeTimestamp = Math.max(0L, inputTimestamp - firstInputTimestamp);
                long nextOutputTimestamp = Math.multiplyExact(keyframeDuration, currentDecimatedFrame);
                if (relativeTimestamp < nextOutputTimestamp) {
                    return;
                }

                byte[] data = record.getDataBuffer();
                if (data == null || data.length == 0) {
                    throw new IOException("Input keyframe has no packet data");
                }
                packet = av_packet_alloc();
                if (packet == null || packet.isNull()) {
                    throw new IOException("Could not allocate decimated video packet");
                }
                int ret = av_new_packet(packet, data.length);
                if (ret < 0) {
                    throw ffmpegIOException("Could not allocate decimated packet data", ret);
                }
                packet.data().position(0).put(data, 0, data.length);
                packet.stream_index(outputStream.index());
                packet.duration(keyframeDuration);
                packet.pts(nextOutputTimestamp);
                packet.dts(nextOutputTimestamp);
                packet.pos(-1);
                packet.flags(packet.flags() | AV_PKT_FLAG_KEY);
                packet.time_base(outputTimeBase);

                ret = av_interleaved_write_frame(formatContext, packet);
                if (ret < 0) {
                    throw ffmpegIOException("Could not write decimated video packet", ret);
                }
                packetsWritten++;
                currentDecimatedFrame++;
                if (currentDecimatedFrame >= totalKeyframes) {
                    closeFile();
                }
            } catch (Exception e) {
                recordFailure(asIOException("Decimated video write failed", e));
            } finally {
                if (packet != null) {
                    av_packet_free(packet);
                }
            }
        }
    }

    private long inputTimestamp(DataBufferRecord record) throws IOException {
        if (record.hasPacketTiming()) {
            long timestamp = record.getPacketPts() != AV_NOPTS_VALUE
                    ? record.getPacketPts() : record.getPacketDts();
            if (timestamp == AV_NOPTS_VALUE) {
                throw new IOException("Input keyframe has no timestamp");
            }
            AVRational inputTimeBase = av_make_q(
                    record.getTimeBaseNumerator(), record.getTimeBaseDenominator());
            return av_rescale_q(timestamp, inputTimeBase, outputTimeBase);
        }

        double presentationTimestamp = record.getPresentationTimestamp();
        if (!Double.isFinite(presentationTimestamp)) {
            throw new IOException("Input keyframe has no presentation timestamp");
        }
        double scaledTimestamp = presentationTimestamp * outputTimeBase.den() / outputTimeBase.num();
        if (!Double.isFinite(scaledTimestamp) || scaledTimestamp > Long.MAX_VALUE
                || scaledTimestamp < Long.MIN_VALUE) {
            throw new IOException("Input keyframe timestamp is outside the supported range");
        }
        return Math.round(scaledTimestamp);
    }

    public void closeFile() throws IOException {
        synchronized (lock) {
            if (closed) {
                if (failure != null) {
                    throw new IOException("Could not finalize decimated video " + outputFileName, failure);
                }
                return;
            }

            writing = false;
            IOException closeFailure = failure;
            if (packetsWritten == 0 && closeFailure == null) {
                closeFailure = new IOException("No keyframes were written to decimated video");
            }
            closeFailure = appendFailure(closeFailure, cleanupOutput(headerWritten));

            closed = true;
            if (closeCallback != null) {
                try {
                    closeCallback.run();
                } catch (RuntimeException e) {
                    closeFailure = appendFailure(closeFailure,
                            new IOException("Decimated video close callback failed", e));
                }
            }
            failure = closeFailure;
            if (failure != null) {
                throw new IOException("Could not finalize decimated video " + outputFileName, failure);
            }
        }
    }

    private IOException cleanupOutput(boolean writeTrailer) {
        IOException cleanupFailure = null;
        if (formatContext != null && !formatContext.isNull()) {
            if (writeTrailer) {
                try {
                    int ret = av_write_trailer(formatContext);
                    if (ret < 0) {
                        cleanupFailure = ffmpegIOException("Could not write decimated output trailer", ret);
                    }
                } catch (Exception e) {
                    cleanupFailure = new IOException("Could not write decimated output trailer", e);
                }
            }

            try {
                if (formatContext.pb() != null && !formatContext.pb().isNull()) {
                    avio_flush(formatContext.pb());
                    int ret = avio_closep(formatContext.pb());
                    if (ret < 0) {
                        cleanupFailure = appendFailure(cleanupFailure,
                                ffmpegIOException("Could not close decimated output file", ret));
                    }
                    formatContext.pb(null);
                }
            } catch (Exception e) {
                cleanupFailure = appendFailure(cleanupFailure,
                        new IOException("Could not close decimated output file", e));
            }

            try {
                avformat_free_context(formatContext);
            } catch (Exception e) {
                cleanupFailure = appendFailure(cleanupFailure,
                        new IOException("Could not release decimated output context", e));
            }
        }
        formatContext = null;
        outputStream = null;
        outputTimeBase = null;
        headerWritten = false;
        return cleanupFailure;
    }

    private void recordFailure(IOException writeFailure) {
        if (failure == null) {
            failure = writeFailure;
            logger.error("Decimated video output failed for {}: {}", outputFileName,
                    writeFailure.getMessage());
        } else {
            failure.addSuppressed(writeFailure);
        }
        writing = false;
    }

    private static IOException appendFailure(IOException current, IOException additional) {
        if (additional == null) {
            return current;
        }
        if (current == null) {
            return additional;
        }
        current.addSuppressed(additional);
        return current;
    }

    private static IOException asIOException(String message, Exception cause) {
        return cause instanceof IOException ? (IOException) cause : new IOException(message, cause);
    }

    private static IOException ffmpegIOException(String message, int errorCode) {
        String errorText = "unknown error";
        try (BytePointer buffer = new BytePointer(AV_ERROR_MAX_STRING_SIZE)) {
            if (av_strerror(errorCode, buffer, buffer.capacity()) >= 0) {
                byte[] bytes = new byte[AV_ERROR_MAX_STRING_SIZE];
                buffer.position(0).get(bytes, 0, bytes.length);
                int length = 0;
                while (length < bytes.length && bytes[length] != 0) {
                    length++;
                }
                errorText = new String(bytes, 0, length, StandardCharsets.UTF_8);
            }
        }
        return new IOException(message + ": " + errorText + " (FFmpeg error " + errorCode + ")");
    }

    @Override
    public boolean isWriting() {
        return writing;
    }

    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    public void setFileCloseCallback(Runnable closeCallback) {
        synchronized (lock) {
            this.closeCallback = closeCallback;
        }
    }
}
