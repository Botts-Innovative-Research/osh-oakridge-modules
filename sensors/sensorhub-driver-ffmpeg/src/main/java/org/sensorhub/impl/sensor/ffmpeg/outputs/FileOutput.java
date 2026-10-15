package org.sensorhub.impl.sensor.ffmpeg.outputs;

//import com.botts.impl.service.oscar.Constants; TODO Circular dependency, maybe move the constant to some other package?
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.TextEncoding;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.outputs.util.ByteArraySeekableBuffer;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.sensorhub.mpegts.DeliveryMode;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;


public class FileOutput<FFMPEGConfigType extends FFMPEGConfig> extends AbstractSensorOutput<FFMPEGSensorBase<FFMPEGConfigType>> implements DataBufferListener {

    public String outputName = "FileNameOutput";
    final DataComponent outputStruct;
    final TextEncoding outputEncoding;

    private static final Logger logger = LoggerFactory.getLogger(FileOutput.class);
    private final AtomicBoolean doFileWrite = new AtomicBoolean(false);
    //private static final String BUCKET_NAME = Constants.VIDEO_BUCKET;
    private static final String BUCKET_NAME = "videos";
    //private String outputFile = "";
    private AVFormatContext outputContext;
    private final Object contextLock = new Object();

    AVStream outputVideoStream;
    OutputStream outputStream;
    AVRational timeBase = new AVRational();
    AVRational inputTimeBase = new AVRational();

    final int MAX_PACKET_QUEUE_SIZE = 1000;
    private final TimestampNormalizer timestampNormalizer = new TimestampNormalizer();
    private long packetsWritten;
    private volatile long packetsDroppedBeforeInitialKeyFrame;
    private boolean waitingForInitialKeyFrame;
    private boolean headerWritten;
    private volatile IOException writeFailure;
    private WriteCallback writeCallback;
    private SeekCallback seekCallback;
    BytePointer buffer;
    ByteArraySeekableBuffer seekableBuffer;
    String fileName;
    private final DeliveryMode deliveryMode;

    /**
     * Creates an output that receives packets as they are demuxed.
     */
    public FileOutput(FFMPEGSensorBase<FFMPEGConfigType> parentSensor, String name) throws SensorHubException {
        this(parentSensor, name, DeliveryMode.LIVE_ONLY);
    }

    /**
     * @param deliveryMode how this output wants packets delivered. Use
     *                     {@link DeliveryMode#BUFFERED} for recordings that should include the
     *                     pre-roll frames leading up to the record command, and
     *                     {@link DeliveryMode#LIVE_ONLY} for anything that has to stay current.
     */
    public FileOutput(FFMPEGSensorBase<FFMPEGConfigType> parentSensor, String name, DeliveryMode deliveryMode) throws SensorHubException {
        super(name, parentSensor);
        this.outputName = name;
        this.deliveryMode = deliveryMode;
        var helper = new SWEHelper();

        outputStruct = helper.createText()
                .name(outputName)
                .label(outputName)
                .build();

        outputEncoding = helper.newTextEncoding();
    }

    @Override
    public boolean isWriting() {
        return doFileWrite.get();
    }

    @Override
    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    @Override
    public void onDataBuffer(DataBufferRecord record) {
        if (!isWriting() || writeFailure != null) {
            return;
        }
        var data = record.getDataBuffer();

        if (data == null || data.length == 0) {
            recordWriteFailure(new IOException("Video packet has no data"));
            return;
        }

        AVPacket avPacket = av_packet_alloc();
        if (avPacket == null || avPacket.isNull()) {
            recordWriteFailure(new IOException("FFmpeg could not allocate an output packet"));
            return;
        }

        int ret = av_new_packet(avPacket, data.length);
        if (ret < 0) {
            recordWriteFailure(ffmpegIOException("Could not allocate output packet data", ret));
            av_packet_free(avPacket);
            return;
        }

        try {
            avPacket.data().position(0).put(data, 0, data.length);
            if (record.isKeyFrame())
                avPacket.flags(avPacket.flags() | AV_PKT_FLAG_KEY);

            synchronized (contextLock) {
                if (!isWriting() || outputContext == null) {
                    logger.debug("Output closed before packet could be written. Packet dropped.");
                    return;
                }
                if (writeFailure != null) {
                    return;
                }

                if (waitingForInitialKeyFrame && !record.isKeyFrame()) {
                    packetsDroppedBeforeInitialKeyFrame++;
                    return;
                }
                if (waitingForInitialKeyFrame) {
                    waitingForInitialKeyFrame = false;
                }

                AVRational packetTimeBase = populatePacketTiming(avPacket, record);
                normalizeAndRescalePacket(avPacket, packetTimeBase);
                avPacket.stream_index(outputVideoStream.index());
                avPacket.pos(-1);

                int writeRet = av_interleaved_write_frame(outputContext, avPacket);
                if (writeRet < 0) {
                    recordWriteFailure(ffmpegIOException("Could not write video packet", writeRet));
                } else {
                    packetsWritten++;
                }
            }
        } catch (IOException e) {
            recordWriteFailure(e);
        } finally {
            av_packet_free(avPacket);
        }
    }

    public void publish() {
        if (this.fileName != null && !this.fileName.isBlank()) {
            this.outputStruct.renewDataBlock();
            this.outputStruct.getData().setStringValue(this.fileName);
            this.eventHandler.publish(new DataEvent(System.currentTimeMillis(), this, outputStruct.getData().clone()));
        }
    }

    public void publish(String fileNameOverride) {
        if (fileNameOverride != null && !fileNameOverride.isBlank()) {
            this.outputStruct.renewDataBlock();
            this.outputStruct.getData().setStringValue(fileNameOverride);
            this.eventHandler.publish(new DataEvent(System.currentTimeMillis(), this, outputStruct.getData().clone()));
        }
    }

    /**
     * Write video data to a file. Format determined by file name suffix.
     * @param fileName
     * @throws IOException
     */
    public void openFile(String fileName) throws IOException {
        AVDictionary options = null;
        AVCodecParameters avCodecParameters = null;

        synchronized (contextLock) {
            try {
                if (doFileWrite.get()) {
                    throw new IOException("Already writing to file " + this.fileName);
                }

                this.outputStream = null;
                this.fileName = fileName;
                this.writeFailure = null;
                this.headerWritten = false;
                this.packetsWritten = 0;
                this.packetsDroppedBeforeInitialKeyFrame = 0;
                this.waitingForInitialKeyFrame = deliveryMode == DeliveryMode.LIVE_ONLY
                        && fileName.endsWith(".m3u8");
                this.timestampNormalizer.reset();

                seekableBuffer = null;
                writeCallback = null;
                seekCallback = null;

                var processor = this.parentSensor.getProcessor();
                if (processor == null || !processor.isStreamOpen() || !processor.isAlive()) {
                    throw new IOException("Video input stream is not available");
                }

                MpegTsProcessor.VideoStreamSnapshot streamSnapshot = processor.getVideoStreamSnapshot();
                if (streamSnapshot == null || streamSnapshot.codecParameters() == null) {
                    throw new IOException("Video input codec parameters are not available");
                }
                avCodecParameters = streamSnapshot.codecParameters();
                if (avCodecParameters.codec_type() != AVMEDIA_TYPE_VIDEO
                        || avCodecParameters.codec_id() == AV_CODEC_ID_NONE
                        || avCodecParameters.width() <= 0
                        || avCodecParameters.height() <= 0) {
                    throw new IOException("Video input codec parameters do not contain valid dimensions and codec metadata");
                }

                if (streamSnapshot.timeBaseNumerator() <= 0
                        || streamSnapshot.timeBaseDenominator() <= 0) {
                    throw new IOException("Video input stream does not contain a valid time base");
                }
                inputTimeBase = av_make_q(
                        streamSnapshot.timeBaseNumerator(), streamSnapshot.timeBaseDenominator());

                initCtx(avCodecParameters);

                // confirm outputContext and oformat are valid
                if (outputContext == null || outputContext.oformat() == null) {
                    throw new IOException("avformat_alloc_output_context2 failed to create an output context.");
                }

                if ((outputContext.oformat().flags() & AVFMT_NOFILE) == 0) { // IMPORTANT! HLS does not have a pb!
                    AVIOContext avioContext = new AVIOContext(null);
                    int openRet = avio_open(avioContext, this.fileName, AVIO_FLAG_WRITE);
                    if (openRet < 0) {
                        throw ffmpegIOException("Could not open file for writing: " + this.fileName, openRet);
                    }
                    outputContext.pb(avioContext);
                }

                if (this.fileName.endsWith(".m3u8")) {
                    options = hlsOptions();
                }

                int ret;
                if ((ret = avformat.avformat_write_header(outputContext, options)) < 0) {
                    throw ffmpegIOException("Could not write header to file", ret);
                }
                headerWritten = true;

                AVRational muxerTimeBase = outputVideoStream.time_base();
                if (muxerTimeBase == null || muxerTimeBase.num() <= 0 || muxerTimeBase.den() <= 0) {
                    throw new IOException("Output muxer did not provide a valid video time base");
                }
                timeBase = av_make_q(muxerTimeBase.num(), muxerTimeBase.den());

                // FFmpeg's log level is process-wide. Do not change it for one recording and
                // inadvertently hide (or amplify) diagnostics from the other camera streams.
                doFileWrite.set(true);

            } catch (Exception e) {
                doFileWrite.set(false);
                IOException cleanupFailure = cleanupOutput(headerWritten);
                IOException openFailure = e instanceof IOException
                        ? (IOException) e
                        : new IOException("Unexpected error while opening output", e);
                if (cleanupFailure != null) {
                    openFailure.addSuppressed(cleanupFailure);
                }
                throw new IOException("Could not open output file " + fileName, openFailure);
            } finally {
                if (options != null) {
                    av_dict_free(options);
                }

                if (avCodecParameters != null) {
                    avcodec_parameters_free(avCodecParameters);
                }
            }
        }
    }

    private void initCtx(AVCodecParameters codecParams) throws IOException {
        int ret;

        //AVFormatContext inputContext = this.parentSensor.getProcessor().getAvFormatContext();
        outputContext = new AVFormatContext(null);

        if ((ret = avformat_alloc_output_context2(outputContext, null, null, this.fileName)) < 0) {
            throw ffmpegIOException("Could not open output context for file " + this.fileName, ret);
        }

        outputVideoStream = avformat.avformat_new_stream(outputContext, null);
        if (outputVideoStream == null || outputVideoStream.isNull()) {
            throw new IOException("Could not allocate an output video stream for file " + this.fileName);
        }

        if ((ret = avcodec.avcodec_parameters_copy(outputVideoStream.codecpar(), codecParams)) < 0) {
            throw ffmpegIOException("Could not copy codec parameters for file " + this.fileName, ret);
        }

        // We're transcoding, need to override some of the copied values
        outputVideoStream.codecpar().codec_id(AV_CODEC_ID_H264);
        outputVideoStream.codecpar().codec_tag(0);

        timeBase = av_make_q(inputTimeBase.num(), inputTimeBase.den());
        outputVideoStream.time_base(timeBase);
        outputVideoStream.duration(AV_NOPTS_VALUE);

        //outputContext.flags(outputContext.flags() | AVFormatContext.AVFMT_FLAG_GENPTS);
        outputContext.flags(outputContext.flags() | AVFormatContext.AVFMT_FLAG_AUTO_BSF);

    }

    private String ffmpegErrorString(int retCode) {
        try (BytePointer buf = new BytePointer(AV_ERROR_MAX_STRING_SIZE)) {
            av_strerror(retCode, buf, buf.capacity());
            byte[] bytes = new byte[AV_ERROR_MAX_STRING_SIZE];
            buf.position(0).get(bytes, 0, bytes.length);
            int length = 0;
            while (length < bytes.length && bytes[length] != 0) {
                length++;
            }
            return new String(bytes, 0, length, StandardCharsets.UTF_8);
        }
    }

    private IOException ffmpegIOException(String message, int retCode) {
        return new IOException(message + ": " + ffmpegErrorString(retCode)
                + " (FFmpeg error " + retCode + ")");
    }

    private void recordWriteFailure(IOException failure) {
        synchronized (contextLock) {
            if (writeFailure == null) {
                writeFailure = failure;
                logger.error("Video output failed for {}: {}", fileName, failure.getMessage());
            }
        }
    }

    private AVRational populatePacketTiming(AVPacket avPacket, DataBufferRecord record) throws IOException {
        if (record.hasPacketTiming()) {
            avPacket.pts(record.getPacketPts());
            avPacket.dts(record.getPacketDts());
            avPacket.duration(record.getPacketDuration());
            return av_make_q(record.getTimeBaseNumerator(), record.getTimeBaseDenominator());
        }

        // Compatibility for callers created before packet-level timing was carried by
        // DataBufferRecord. This path cannot recover B-frame DTS, so native demuxed packets must
        // use the metadata-bearing constructor above.
        double presentationTimestamp = record.getPresentationTimestamp();
        if (!Double.isFinite(presentationTimestamp)) {
            throw new IOException("Video packet does not contain usable timestamp metadata");
        }
        long timestamp = (long) (presentationTimestamp * inputTimeBase.den() / inputTimeBase.num());
        avPacket.pts(timestamp);
        avPacket.dts(timestamp);
        avPacket.duration(0);
        return inputTimeBase;
    }

    private void normalizeAndRescalePacket(AVPacket avPacket, AVRational packetTimeBase) throws IOException {
        if (packetTimeBase == null || packetTimeBase.num() <= 0 || packetTimeBase.den() <= 0) {
            throw new IOException("Video packet does not contain a valid time base");
        }

        // Scale each packet independently first. This permits a reconnect or transcoder to use a
        // different source time base without mixing units in the recording timeline.
        av_packet_rescale_ts(avPacket, packetTimeBase, timeBase);
        NormalizedTimestamps normalized = timestampNormalizer.normalize(
                avPacket.pts(), avPacket.dts(), avPacket.duration());
        avPacket.pts(normalized.pts());
        avPacket.dts(normalized.dts());
        avPacket.duration(normalized.duration());
        avPacket.time_base(timeBase);
        if (normalized.discontinuity()) {
            logger.warn("Video timestamp discontinuity detected; rebasing the output timeline");
        }
    }

    record NormalizedTimestamps(long pts, long dts, long duration, boolean discontinuity) {}

    /**
     * Rebases packet timestamps onto one strictly increasing decode timeline. A timestamp reset or
     * wrap is corrected by adding the same offset to PTS and DTS, preserving B-frame composition
     * offsets instead of collapsing DTS onto PTS.
     */
    static final class TimestampNormalizer {
        private long timestampOffset;
        private long lastDecodeTimestamp = AV_NOPTS_VALUE;
        private long lastDuration;

        void reset() {
            timestampOffset = 0;
            lastDecodeTimestamp = AV_NOPTS_VALUE;
            lastDuration = 0;
        }

        NormalizedTimestamps normalize(long pts, long dts, long duration) throws IOException {
            long orderingTimestamp = dts != AV_NOPTS_VALUE ? dts : pts;
            if (orderingTimestamp == AV_NOPTS_VALUE) {
                throw new IOException("Video packet has neither DTS nor PTS");
            }

            try {
                if (lastDecodeTimestamp == AV_NOPTS_VALUE) {
                    timestampOffset = Math.negateExact(orderingTimestamp);
                }

                long normalizedOrderingTimestamp = Math.addExact(orderingTimestamp, timestampOffset);
                boolean discontinuity = lastDecodeTimestamp != AV_NOPTS_VALUE
                        && normalizedOrderingTimestamp <= lastDecodeTimestamp;
                if (discontinuity) {
                    long step = duration > 0 ? duration : (lastDuration > 0 ? lastDuration : 1L);
                    long desiredTimestamp = Math.addExact(lastDecodeTimestamp, step);
                    timestampOffset = Math.addExact(
                            timestampOffset,
                            Math.subtractExact(desiredTimestamp, normalizedOrderingTimestamp));
                    normalizedOrderingTimestamp = desiredTimestamp;
                }

                long normalizedPts = pts == AV_NOPTS_VALUE
                        ? AV_NOPTS_VALUE : Math.addExact(pts, timestampOffset);
                long normalizedDts = dts == AV_NOPTS_VALUE
                        ? AV_NOPTS_VALUE : Math.addExact(dts, timestampOffset);
                lastDecodeTimestamp = normalizedOrderingTimestamp;
                if (duration > 0) {
                    lastDuration = duration;
                }
                return new NormalizedTimestamps(
                        normalizedPts, normalizedDts, duration, discontinuity);
            } catch (ArithmeticException e) {
                throw new IOException("Video timestamp overflow while rebasing the output timeline", e);
            }
        }
    }

    private static AVDictionary hlsOptions() {
        AVDictionary options = new AVDictionary();
        av_dict_set(options, "hls_list_size", "2", 0);
        av_dict_set(options, "hls_time", "1", 0);
        //avutil.av_dict_set(options, "hls_segment_type", "ts", 0);
        //avutil.av_dict_set(options, "movflags", "faststart+default_base_moof", 0);
        // Publish only complete playlists/segments and promise independent segments only after
        // FileOutput has admitted the first keyframe. Removing append_list also prevents a stale
        // manifest from carrying media into a restarted stream.
        av_dict_set(options, "hls_flags", "delete_segments+independent_segments+temp_file", 0);
        av_dict_set(options, "hls_segment_type", "mpegts", 0);
        av_dict_set(options, "use_localtime", "1", 0);
        av_dict_set(options, "max_delay", "0", 0);
        av_dict_set(options, "muxdelay", "0", 0);
        av_dict_set(options, "muxpreload", "0", 0);
        //av_dict_set(options, "hls_fmp4_init_filename", "init.mp4", 0);
        return options;
    }

    /** Number of live packets intentionally skipped while aligning a new HLS stream to an IDR. */
    public long getPacketsDroppedBeforeInitialKeyFrame() {
        return packetsDroppedBeforeInitialKeyFrame;
    }

    public void closeFile() throws IOException {
        synchronized (contextLock) {
            if (!isWriting()) {
                return;
            }

            doFileWrite.set(false);
            IOException failure = writeFailure;
            if (packetsWritten == 0 && failure == null) {
                failure = new IOException("No video packets were received while recording");
            }

            IOException cleanupFailure = cleanupOutput(headerWritten);
            if (cleanupFailure != null) {
                if (failure == null) {
                    failure = cleanupFailure;
                } else {
                    failure.addSuppressed(cleanupFailure);
                }
            }

            writeFailure = null;
            if (failure != null) {
                throw new IOException("Could not finalize output file " + this.fileName, failure);
            }
        }
    }

    /**
     * Releases a partially or fully initialized output context. AVStream instances are owned by
     * AVFormatContext, so outputVideoStream is deliberately not closed separately.
     */
    private IOException cleanupOutput(boolean writeTrailer) {
        IOException failure = null;

        if (outputContext != null && !outputContext.isNull()) {
            if (writeTrailer) {
                try {
                    int ret = av_write_trailer(outputContext);
                    if (ret < 0) {
                        failure = ffmpegIOException("Could not write output trailer", ret);
                    }
                } catch (Exception e) {
                    failure = new IOException("Could not write output trailer", e);
                }
            }

            try {
                if (outputContext.pb() != null && !outputContext.pb().isNull()) {
                    avio_flush(outputContext.pb());
                    int ret = avio_closep(outputContext.pb());
                    if (ret < 0) {
                        IOException closeFailure = ffmpegIOException("Could not close output file", ret);
                        if (failure == null) {
                            failure = closeFailure;
                        } else {
                            failure.addSuppressed(closeFailure);
                        }
                    }
                    outputContext.pb(null);
                }
            } catch (Exception e) {
                IOException closeFailure = new IOException("Could not close output file", e);
                if (failure == null) {
                    failure = closeFailure;
                } else {
                    failure.addSuppressed(closeFailure);
                }
            }
        }

        // Flush to a caller-provided stream if a seekable in-memory buffer is in use.
        if (outputStream != null) {
            try {
                if (seekableBuffer != null) {
                    outputStream.write(seekableBuffer.getData());
                }
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                IOException streamFailure = new IOException("Could not flush buffered video output", e);
                if (failure == null) {
                    failure = streamFailure;
                } else {
                    failure.addSuppressed(streamFailure);
                }
            } finally {
                outputStream = null;
            }
        }

        if (writeCallback != null) {
            try {
                writeCallback.close();
            } catch (Exception e) {
                logger.debug("Error closing video write callback", e);
            } finally {
                writeCallback = null;
            }
        }
        if (seekCallback != null) {
            try {
                seekCallback.close();
            } catch (Exception e) {
                logger.debug("Error closing video seek callback", e);
            } finally {
                seekCallback = null;
            }
        }

        outputVideoStream = null;
        if (outputContext != null) {
            try {
                if (!outputContext.isNull()) {
                    avformat_free_context(outputContext);
                }
            } catch (Exception e) {
                IOException contextFailure = new IOException("Could not release output context", e);
                if (failure == null) {
                    failure = contextFailure;
                } else {
                    failure.addSuppressed(contextFailure);
                }
            } finally {
                outputContext = null;
            }
        }

        headerWritten = false;
        waitingForInitialKeyFrame = false;
        return failure;
    }

    @Override
    public DataComponent getRecordDescription() {
        return outputStruct.copy();
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return outputEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return Double.NaN;
    }

    // Used to write ffmpeg output to buffer instead of a file
    private static class WriteCallback extends Write_packet_Pointer_BytePointer_int {
        private ByteArraySeekableBuffer buffer;

        public WriteCallback(ByteArraySeekableBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int call(Pointer opaque, BytePointer buf, int buf_size) {
            byte[] b = new byte[buf_size];
            buf.get(b, 0, buf_size);
            return buffer.write(b, 0, buf_size);
        }

        public void close() {
            buffer = null;
            super.close();
        }
    }

    private static class SeekCallback extends org.bytedeco.ffmpeg.avformat.Seek_Pointer_long_int {
        private ByteArraySeekableBuffer buffer;

        public SeekCallback(ByteArraySeekableBuffer buffer) {
            this.buffer = buffer;
        }

        public void close() {
            buffer = null;
            super.close();
        }

        @Override
        public long call(Pointer opaque, long offset, int whence) {
            return buffer.seek(offset, whence);
        }
    }
}
