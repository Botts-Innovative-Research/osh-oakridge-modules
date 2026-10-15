/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.PointerPointer;
import org.sensorhub.impl.process.video.transcoder.coders.Coder;
import org.sensorhub.impl.process.video.transcoder.coders.Decoder;
import org.sensorhub.impl.process.video.transcoder.coders.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

/**
 * The class provides a wrapper to bytedeco.org JavaCpp-Platform.
 * "Bytedeco makes native libraries available to the Java platform
 * by offering ready-to-use bindings generated with the codeveloped
 * JavaCPP technology."
 * <p>
 * Of particular interest is the platform support for ffmpeg, specifically
 * avutils which is used to demux the MPEG-TS streams into h264 video packets
 * and MISB STANAG 4609 Metadata packets.
 * <p>
 * The MpegTsProcessor allows for easy interface and management of the
 * logical stream, allowing client applications to register callbacks
 * for video and metadata buffers for further domain specific processing.
 * <p>
 * <H1>Example Usage:</H1>
 * MpegTsProcessor mpegTsProcessor =
 *      new MpegTsProcessor("src/test/resources/Flight_20190327_018.ts");
 * <p>
 * mpegTsProcessor.openStream();
 * <p>
 * mpegTsProcessor.queryEmbeddedStreams();
 * <p>
 * if (mpegTsProcessor.hasVideoStream()) {
 * <p>
 *      mpegTsProcessor.setVideoDataBufferListener(new DataBufferListener() {
 *
 *          public void onDataBuffer(byte[] dataBuffer) {
 *                 ...
 *              }
 *          });
 * }
 * <p>
 * if (mpegTsProcessor.hasDataStream()) {
 * <p>
 *      mpegTsProcessor.setDataDataBufferListener(new DataBufferListener() {
 *
 *          public void onDataBuffer(byte[] dataBuffer) {
 *                  ...
 *              }
 *          });
 * }
 * <p>
 * mpegTsProcessor.processStream();
 * <p>
 * ...
 * ...
 * ...
 * <p>
 * mpegTsProcessor.stopProcessingStream();
 *
 * try {
 *
 *      mpegTsProcessor.join();
 *
 * } catch (InterruptedException e) {
 *
 *      e.printStackTrace();
 * }
 *
 * mpegTsProcessor.closeStream();
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class MpegTsProcessor extends Thread {

    /**
     * Logging utility
     */
    private static final Logger logger = LoggerFactory.getLogger(MpegTsProcessor.class);


    private static class ListenerThreadFactory implements ThreadFactory {
        private final String name;

        ListenerThreadFactory(String prefix, String listenerType) {
            this.name = prefix + "-" + listenerType;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name);
        }
    }

    /**
     * A registered listener together with everything needed to dispatch to it: the delivery mode
     * it asked for, and the executor its callbacks run on. A null executor means the listener is
     * invoked inline on the stream processing thread.
     * <p>
     * Keeping the executor here rather than in a side map means it can never go missing while a
     * packet is being dispatched to the listener it belongs to.
     */
    private record Subscription(DataBufferListener listener, DeliveryMode mode, ExecutorService executor) {

        void deliver(DataBufferRecord record) {
            if (executor == null) {
                listener.onDataBuffer(record);
            } else {
                executor.submit(() -> listener.onDataBuffer(record));
            }
        }

        void shutdown() {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Dispatch targets, pre-built so the per-packet path never allocates a set.
     */
    private static final Set<DeliveryMode> ALL_MODES =
            Collections.unmodifiableSet(EnumSet.allOf(DeliveryMode.class));
    private static final Set<DeliveryMode> LIVE_MODES =
            Collections.unmodifiableSet(EnumSet.of(DeliveryMode.LIVE_ONLY));
    private static final Set<DeliveryMode> BUFFERED_MODES =
            Collections.unmodifiableSet(EnumSet.of(DeliveryMode.BUFFERED));

    boolean injectExtradata = true;

    protected final Object contextLock = new Object();

    /** Guards every transition of the buffered-packet queue and replay state. */
    private final Object frameQueueLock = new Object();

    /** Each packet in this queue has exactly one native owner: the queue. */
    private Queue<AVPacket> fileFrameQueue;

    private int fileFrameQueueMax;

    private boolean isSubmittingFrameQueue;

    private ExecutorService frameQueueSubmitter;

    /**
     * Name of thread
     */
    private static final String WORKER_THREAD_NAME = "STREAM-PROCESSOR";

    /** Upper bounds for probing. The actual duration is capped by the configured connect timeout. */
    private static final long MAX_ANALYZE_DURATION_MICROS = 15_000_000L;
    private static final long MAX_PROBE_SIZE_BYTES = 10_000_000L;
    private static final long PROBE_BYTES_PER_SECOND = 1_000_000L;
    private static final long MIN_PROBE_SIZE_BYTES = 32_768L;

    /** Upper bound for one asynchronous decoder/encoder handoff on an unpaced file source. */
    private static final long TRANSCODE_OUTPUT_WAIT_NANOS = TimeUnit.MILLISECONDS.toNanos(500L);
    private static final long TRANSCODER_FLUSH_TIMEOUT_MILLIS = 5_000L;

    /**
     * Id of invalid sub streams within actual stream
     */
    protected static final int INVALID_STREAM_ID = -1;

    /**
     * Context used by underlying ffmpeg library to decode stream
     */
    private AVFormatContext avFormatContext;

    /**
     * Container for possible video sub stream id
     */
    private int videoStreamId = INVALID_STREAM_ID;

    /**
     * Listeners for video buffers extracted from the transport stream, each paired with the
     * delivery mode it registered under and the executor its callbacks run on.
     * <p>
     * Copy-on-write because this is read once per demuxed packet and written only when the stream
     * starts or stops, which keeps the dispatch path free of locks and allocations.
     */
    private final CopyOnWriteArrayList<Subscription> videoSubscriptions = new CopyOnWriteArrayList<>();

    /**
     * Flag indicating if processing of the transport stream should be terminated
     */
    private volatile boolean terminateProcessing = false;

    /**
     * Flag indicating whether or not the stream has been opened or connected to successfully
     */
    private volatile boolean streamOpened = false;

    /**
     * A string representation of the file or url to use as the source of the transport stream to
     * demux
     */
    private final String streamSource;

    /**
     * Time base units for video stream timing used to compute a timestamp for each packet extracted
     */
    private int videoStreamTimeBaseNumerator;

    private int videoStreamTimeBaseDenominator;

    /**
     * Time base units for data stream timing used to compute a timestamp for each packet extracted
     */
    private double dataStreamTimeBase;

    /**
     * FPS to enforce when playing back from file.
     * 0 means the file will be played back as fast as possible
     */
    int fps;

    /**
     * If true, play the video file continuously in a loop
     */
    volatile boolean loop;

    private long timeout = 5000000;

    byte[] spsPpsHeader = null;

    private boolean useTCP;

    private Decoder decoder;

    private Encoder encoder;

    private boolean doTranscode = false;

    /** Decoder input crosses the demux and decoder threads; poll is an ownership transfer. */
    private final Queue<AVPacket> decodeQueue = new ConcurrentLinkedQueue<>();

    /** A native codec-parameter copy plus an immutable snapshot of the selected stream time base. */
    public record VideoStreamSnapshot(AVCodecParameters codecParameters,
                                      int timeBaseNumerator,
                                      int timeBaseDenominator,
                                      long duration,
                                      int averageFrameRateNumerator,
                                      int averageFrameRateDenominator) {}


    /**
     * Constructor
     *
     * @param source A string representation of the file or url to use as the source of the transport stream to
     *               demux
     */
    public MpegTsProcessor(String source) {

        this(source, 0, false, false);
    }


    /**
     * Constructor with more options when playing back from file
     *
     * @param source A string representation of the file or url to use as the source of the transport stream to
     *               demux
     * @param fps The desired playback FPS (use 0 for decoding the TS file as fast as possible)
     * @param loop
     */
    public MpegTsProcessor(String source, int fps, boolean loop, boolean useTCP) {

        super(WORKER_THREAD_NAME);

        this.streamSource = source;
        this.fps = fps;
        this.loop = loop;
        this.useTCP = useTCP;
    }

    /**
     * Sets the frame buffer size for the frame queue. The frame count provided determines
     * the maximum number of frames that can be stored in the buffer. If the frame count is less
     * than or equal to zero, the frame buffer will be set to null.
     *
     * <p>Increase the size of the frame buffer when the trigger for the
     * {@link org.sensorhub.impl.sensor.ffmpeg.controls.FileControl} file save command is late.</p>
     *
     * <p>E.g., if the file save command is typically two seconds late
     * and the stream is 30 fps, a buffer of 60 frames should be sufficient.</p>
     *
     * @param frameCount The number of frames to allocate space for in the buffer. A value of 0 or
     *                   less will set the buffer to null.
     */
    // TODO: Convert from number of frames to seconds, convert to queue size using fps
    public void setFrameBuffer(int frameCount) {
        if (frameCount < 0) {
            frameCount = 0;
        }

        synchronized (frameQueueLock) {
            isSubmittingFrameQueue = false;
            drainAndFreeFrameQueueLocked();
            fileFrameQueueMax = frameCount;
            fileFrameQueue = frameCount > 0 ? new ArrayDeque<>(frameCount) : null;
        }
    }

    /**
     * Attempts to open the stream given the {@link MpegTsProcessor#streamSource}
     * Opening the stream entails establishing appropriate connection and ability to
     * extract stream information.
     *
     * @return true if the stream is opened succesfully, false otherwise.
     */
    public boolean openStream() {
        AVDictionary options = new AVDictionary();
        synchronized (contextLock) {
            try {
                logger.debug("openStream");
                streamOpened = false;
                videoStreamId = INVALID_STREAM_ID;

                avformat.avformat_network_init();

                if (useTCP) {
                    avutil.av_dict_set(options, "rtsp_transport", "tcp", 0);
                }

                long analyzeDuration = getAnalyzeDurationMicros();
                avutil.av_dict_set(options, "timeout", Long.toString(timeout), 0);
                avutil.av_dict_set(options, "rw_timeout", Long.toString(timeout), 0);
                avutil.av_dict_set(options, "analyzeduration", Long.toString(analyzeDuration), 0);
                avutil.av_dict_set(options, "probesize", Long.toString(getProbeSizeBytes(analyzeDuration)), 0);

                // Create a new AV Format Context for I/O
                avFormatContext = new AVFormatContext(null);

                int returnCode = avformat.avformat_open_input(avFormatContext, streamSource, null, options);

                // Attempt to open the stream, streamPath can be a file or URL
                if (returnCode == 0) {

                    returnCode = avformat.avformat_find_stream_info(avFormatContext, (PointerPointer<?>) null);

                    if (returnCode < 0) {

                        logger.error("Failed to find stream info");

                    } else if ((videoStreamId = findUsableVideoStreamId()) != INVALID_STREAM_ID) {

                        streamOpened = true;
                        logger.debug("Configured video stream opened");

                    } else {
                        logger.warn("Video stream metadata is incomplete; waiting for a reconnect instead of "
                                + "starting with invalid codec parameters");
                    }
                } else {

                    logger.error("Failed to open configured video stream");
                }
            } catch (Exception e) {
                logger.error("Exception when opening stream", e);
            } finally {
                if (!streamOpened) {
                    closeInputContext();
                }
                av_dict_free(options);
            }
        }

        return streamOpened;
    }

    private int findUsableVideoStreamId() {
        for (int streamId = 0; streamId < avFormatContext.nb_streams(); ++streamId) {
            AVStream stream = avFormatContext.streams(streamId);
            AVCodecParameters codecParameters = stream.codecpar();
            if (codecParameters.codec_type() != AVMEDIA_TYPE_VIDEO)
                continue;

            AVRational streamTimeBase = stream.time_base();
            if (codecParameters.codec_id() != avcodec.AV_CODEC_ID_NONE
                    && codecParameters.width() > 0
                    && codecParameters.height() > 0
                    && streamTimeBase != null
                    && streamTimeBase.num() > 0
                    && streamTimeBase.den() > 0) {
                return streamId;
            }
        }
        return INVALID_STREAM_ID;
    }

    long getAnalyzeDurationMicros() {
        if (timeout <= 0) {
            return MAX_ANALYZE_DURATION_MICROS;
        }
        return Math.min(timeout, MAX_ANALYZE_DURATION_MICROS);
    }

    long getProbeSizeBytes(long analyzeDurationMicros) {
        long durationSecondsRoundedUp = Math.max(1L, (analyzeDurationMicros + 999_999L) / 1_000_000L);
        long timeoutBoundedProbeSize = durationSecondsRoundedUp * PROBE_BYTES_PER_SECOND;
        return Math.max(MIN_PROBE_SIZE_BYTES, Math.min(MAX_PROBE_SIZE_BYTES, timeoutBoundedProbeSize));
    }

    private void closeInputContext() {
        if (avFormatContext == null)
            return;

        try {
            if (!avFormatContext.isNull()) {
                avformat.avformat_close_input(avFormatContext);
            }
        } finally {
            avFormatContext = null;
        }
    }

    /**
     * @return {@code true} when the native input context is currently open.
     */
    public boolean isStreamOpen() {
        return streamOpened;
    }

    public void setInjectExtradata(boolean injectExtradata) { this.injectExtradata = injectExtradata; }

    public AVCodecParameters getCodecParams() {
        VideoStreamSnapshot snapshot = getVideoStreamSnapshot();
        return snapshot == null ? null : snapshot.codecParameters();
    }

    /**
     * Copies all metadata needed by an output while holding {@link #contextLock}. No borrowed
     * AVStream or AVRational pointer escapes the input context, so a concurrent close cannot
     * invalidate metadata after this method returns. The caller owns and must free codecParameters.
     */
    public VideoStreamSnapshot getVideoStreamSnapshot() {
        synchronized (contextLock) {
            AVCodecParameters dst = null;
            try {
                if (!streamOpened || avFormatContext == null || avFormatContext.isNull()
                        || videoStreamId < 0 || videoStreamId >= avFormatContext.nb_streams()) {
                    return null;
                }
                AVCodecParameters src = avFormatContext.streams(videoStreamId).codecpar();
                if (src == null) {
                    logger.error("Video stream codec parameters are not available");
                    return null;
                }
                dst = avcodec.avcodec_parameters_alloc();
                if (dst == null) {
                    logger.error("Could not allocate AVCodecParameters");
                    return null;
                }
                int ret = avcodec.avcodec_parameters_copy(dst, src);
                if (ret < 0) {
                    avcodec.avcodec_parameters_free(dst);
                    logger.error("AVCodecParameters failed: {}", ret);
                    return null;
                }
                AVRational timeBase = avFormatContext.streams(videoStreamId).time_base();
                if (timeBase == null || timeBase.num() <= 0 || timeBase.den() <= 0) {
                    avcodec.avcodec_parameters_free(dst);
                    logger.error("Video stream time base is not available");
                    return null;
                }
                AVRational averageFrameRate = avFormatContext.streams(videoStreamId).avg_frame_rate();
                int frameRateNumerator = averageFrameRate == null || averageFrameRate.isNull()
                        ? 0 : averageFrameRate.num();
                int frameRateDenominator = averageFrameRate == null || averageFrameRate.isNull()
                        ? 0 : averageFrameRate.den();
                long duration = resolveVideoDuration(
                        avFormatContext.streams(videoStreamId).duration(),
                        avFormatContext.duration(),
                        timeBase.num(),
                        timeBase.den());
                return new VideoStreamSnapshot(
                        dst,
                        timeBase.num(),
                        timeBase.den(),
                        duration,
                        frameRateNumerator,
                        frameRateDenominator);
            } catch (Exception e) {
                if (dst != null && !dst.isNull()) {
                    avcodec.avcodec_parameters_free(dst);
                }
                logger.error("Exception when copying video stream metadata", e);
                return null;
            }
        }
    }

    /**
     * Uses the stream duration when available, otherwise converts FFmpeg's container duration from
     * microseconds into the selected stream's time base. Returning a non-positive value tells
     * callers that duration-dependent processing must preserve the source rather than guess.
     */
    static long resolveVideoDuration(long streamDuration, long containerDuration,
                                     int timeBaseNumerator, int timeBaseDenominator) {
        if (streamDuration > 0) {
            return streamDuration;
        }
        if (containerDuration <= 0 || timeBaseNumerator <= 0 || timeBaseDenominator <= 0) {
            return streamDuration;
        }
        long resolved = av_rescale_q(containerDuration,
                av_make_q(1, AV_TIME_BASE),
                av_make_q(timeBaseNumerator, timeBaseDenominator));
        return resolved > 0 ? resolved : streamDuration;
    }

    /**
     * Required to identify if the transport stream contains a video stream and/or a data stream.
     * Should be invoked after {@link MpegTsProcessor#openStream()} and used in conjunction
     * with {@link MpegTsProcessor#hasDataStream()} {@link MpegTsProcessor#hasVideoStream()}
     * to determine what streams are available for consumption.
     */
    public void queryEmbeddedStreams() {

        logger.debug("queryAvailableStreams");

        if (!streamOpened) {

            String message = "Stream is not opened, stream must be open to query available sub-streams";

            logger.error(message);

            throw new IllegalStateException(message);
        }

        synchronized (contextLock) {
            if (avFormatContext == null || videoStreamId < 0
                    || videoStreamId >= avFormatContext.nb_streams()) {
                throw new IllegalStateException("Stream does not contain usable video metadata");
            }

            AVStream videoStream = avFormatContext.streams(videoStreamId);
            AVCodecParameters codecParameters = videoStream.codecpar();
            AVRational timeBase = videoStream.time_base();
            logger.debug("Video stream present with id: {}", videoStreamId);

            doTranscode = codecParameters.codec_id() != avcodec.AV_CODEC_ID_H264;
            videoStreamTimeBaseNumerator = timeBase.num();
            videoStreamTimeBaseDenominator = timeBase.den();

            // Injecting extradata as-is only works for the exact selected H264 video stream.
            int extraSize = codecParameters.extradata_size();
            if (injectExtradata && extraSize > 0
                    && codecParameters.codec_id() == avcodec.AV_CODEC_ID_H264) {
                spsPpsHeader = new byte[extraSize];
                codecParameters.extradata().get(spsPpsHeader);
            } else {
                spsPpsHeader = null;
            }
        }
    }

    /**
     * Required to identify if the transport stream contains a video stream stream.
     * Should be invoked after {@link MpegTsProcessor#queryEmbeddedStreams()} and used in conjunction
     * with {@link MpegTsProcessor#setVideoDataBufferListener(DataBufferListener)}
     * to register callbacks for appropriate buffers.
     */
    public boolean hasVideoStream() {

        boolean hasVideoStream = videoStreamId != INVALID_STREAM_ID;

        logger.debug("hasVideoStream: {}", hasVideoStream);

        return hasVideoStream;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Required to identify if the transport stream contains a data stream.
     * Should be invoked after {@link MpegTsProcessor#queryEmbeddedStreams()} and used in conjunction
     * with {@link MpegTsProcessor#setMetaDataDataBufferListener(DataBufferListener)}
     * to register callbacks for appropriate buffers.
     */
//    public boolean hasDataStream() {
//
//        boolean hasDataStream = dataStreamId != INVALID_STREAM_ID;
//
//        logger.debug("hasDataStream: {}", hasDataStream);
//
//        return hasDataStream;
//    }

    // This is a very unsophisticated way of getting the name
    // Good enough for the reverted driver
    public String getCodecName() {
        int codecId = avFormatContext.streams(videoStreamId).codecpar().codec_id();
        if (codecId == avcodec.AV_CODEC_ID_H264) {
            return "h264";
        } else if (codecId == avcodec.AV_CODEC_ID_MJPEG) {
            return "mjpeg";
        } else {
            return "other";
        }
    }

    /**
     * Retrieves the average frame rate for the embedded video if there is one
     * Should be invoked after {@link MpegTsProcessor#hasVideoStream()}
     * to retrieve the video average frame rate.
     *
     * @return average frame rate for video
     *
     * @throws IllegalStateException if there is no video stream embedded
     */
    public double getVideoStreamAvgFrameRate() throws IllegalStateException {

        if (INVALID_STREAM_ID == videoStreamId) {

            String message = "Stream does not contain video frames";

            logger.error(message);

            throw new IllegalStateException(message);
        }

        AVRational rational = avFormatContext.streams(videoStreamId).avg_frame_rate();
        double num = rational.num();
        double den = rational.den();
        return num/den;
    }

    /**
     * Required to identify the width and height of the video frames.
     * Should be invoked after {@link MpegTsProcessor#queryEmbeddedStreams()} and
     * {@link MpegTsProcessor#hasVideoStream()} to retrieve the video frame dimensions.
     *
     * @return an int[] where index 0 is the width and index 1 is the height of the frames.
     *
     * @throws IllegalStateException if there is no video stream embedded
     */
    public int[] getVideoStreamFrameDimensions() {

        final int WIDTH_IDX = 0;
        final int HEIGHT_IDX = 1;

        logger.debug("getVideoStreamFrameDimensions");

        if (INVALID_STREAM_ID == videoStreamId) {

            String message = "Stream does not contain video frames";

            logger.error(message);

            throw new IllegalStateException(message);
        }

        AVCodecParameters codecParameters = avFormatContext.streams(videoStreamId).codecpar();

        int[] dimensions = {codecParameters.width(), codecParameters.height()};



        logger.debug("Frame [width, height] = [ {}, {} ]", dimensions[WIDTH_IDX], dimensions[HEIGHT_IDX]);

        return dimensions;
    }

    /**
     * Registers a data buffer listener to callback if clients are interested in demuxed
     * data buffers
     *
     * @param metadataDataBufferListener the listener to invoke when a data buffer is retrieved from
     *                               the transport stream.
     *
     * @throws NullPointerException if the data buffer listener is null
     */
//    public void setMetaDataDataBufferListener(DataBufferListener metadataDataBufferListener) throws NullPointerException {
//
//        if (null == metadataDataBufferListener) {
//
//            throw new NullPointerException("Attempt to set null metadataDataBufferListener");
//        }
//
//        this.metadataDataBufferListener = metadataDataBufferListener;
//    }

    /**
     * Clears all video buffer listeners then adds this listener.
     * Registers a video buffer listener to callback if clients are interested in demuxed
     * video buffers
     *
     * @param videoDataBufferListener the listener to invoke when a video buffer is retrieved from
     *                                the transport stream.
     *
     * @throws NullPointerException if the data buffer listener is null
     */
    public void setVideoDataBufferListener(DataBufferListener videoDataBufferListener) throws NullPointerException {
        clearVideoDataBufferListeners();
        addVideoDataBufferListener(videoDataBufferListener);
    }

    /**
     * Registers a listener to be called back on its own thread, under the delivery mode it
     * reports from {@link DataBufferListener#getDeliveryMode()}.
     *
     * @param videoDataBufferListener the listener to invoke when a video buffer is retrieved from
     *                                the transport stream
     *
     * @throws NullPointerException if the data buffer listener is null
     */
    public void addVideoDataBufferListener(DataBufferListener videoDataBufferListener) throws NullPointerException {
        addVideoDataBufferListener(videoDataBufferListener, true);
    }

    /**
     * Registers a listener under the delivery mode it reports from
     * {@link DataBufferListener#getDeliveryMode()}.
     * <p>
     * Registering a listener that is already registered replaces its existing subscription.
     *
     * @param videoDataBufferListener the listener to invoke when a video buffer is retrieved from
     *                                the transport stream
     * @param async true to call the listener back on a dedicated thread, false to call it inline
     *              on the stream processing thread. Inline listeners must return promptly, since
     *              they hold up demuxing for everyone.
     *
     * @throws NullPointerException if the data buffer listener is null
     */
    public void addVideoDataBufferListener(DataBufferListener videoDataBufferListener, boolean async) throws NullPointerException {
        if (null == videoDataBufferListener) {

            throw new NullPointerException("Attempt to set null videoStreamPacketListener");
        }

        removeVideoDataBufferListener(videoDataBufferListener);

        ExecutorService executor = async
                ? Executors.newSingleThreadExecutor(new ListenerThreadFactory(
                        WORKER_THREAD_NAME, videoDataBufferListener.getName()))
                : null;

        videoSubscriptions.add(new Subscription(
                videoDataBufferListener, videoDataBufferListener.getDeliveryMode(), executor));
    }

    public Set<DataBufferListener> getVideoDataBufferListeners() {
        Set<DataBufferListener> out = new HashSet<>();
        for (var subscription : videoSubscriptions) {
            out.add(subscription.listener());
        }
        return out;
    }

    public void clearVideoDataBufferListeners() {
        for (var subscription : videoSubscriptions) {
            subscription.shutdown();
        }
        videoSubscriptions.clear();
    }

    /**
     * Unregisters a listener and shuts down its callback thread. Removing a listener that was
     * never registered is a no-op.
     */
    public void removeVideoDataBufferListener(DataBufferListener videoDataBufferListener) {
        videoSubscriptions.removeIf(subscription -> {
            if (subscription.listener() != videoDataBufferListener) {
                return false;
            }
            subscription.shutdown();
            return true;
        });
    }

    /**
     * Hands a record to every registered listener that asked for one of the given delivery modes
     * and is currently writing.
     * <p>
     * The record is cloned once and shared across the matching listeners, which only read it.
     *
     * @param dataRecord the record to deliver
     * @param targetModes the delivery modes to deliver to
     */
    protected void notifyVideoDataBufferListeners(DataBufferRecord dataRecord, Set<DeliveryMode> targetModes) {
        DataBufferRecord dataClone = null;
        for (var subscription : videoSubscriptions) {
            if (!targetModes.contains(subscription.mode()) || !subscription.listener().isWriting()) {
                continue;
            }
            if (dataClone == null) {
                dataClone = dataRecord.clone();
            }
            subscription.deliver(dataClone);
        }
    }

    /**
     * Starts the threaded process for demuxing the transport stream
     * Should only be invoked if stream is successfully opened.
     * <p>
     * Call this method instead of invoke {@link MpegTsProcessor#start()} directly,
     * as this method will ensure the codec context is setup for use with the transport stream.
     *
     * @throws IllegalStateException if the stream has not been opened or failed to open.
     */
    public void processStream() throws IllegalStateException {

        logger.debug("processStream");

        if (streamOpened) {

            if (doTranscode) {
                var settings = new HashMap<String, Integer>();
                var dims = getVideoStreamFrameDimensions();
                settings.put("width", dims[0]);
                settings.put("height", dims[1]);
                settings.put("pix_fmt", avutil.AV_PIX_FMT_YUV420P);
                int[] frameRate = getTranscoderFrameRate();
                settings.put("fps_num", frameRate[0]);
                settings.put("fps_den", frameRate[1]);
                decoder = new Decoder(getCodecId(), settings);
                encoder = new Encoder(avcodec.AV_CODEC_ID_H264, settings);
                decoder.setInQueue(decodeQueue);
                encoder.setInQueue(decoder.getOutQueue());
                decoder.start();
                encoder.start();
            }

            start();

        } else {

            String message = "Stream has not been opened or failed to open";

            logger.error(message);

            throw new IllegalStateException(message);
        }
    }

    private int[] getTranscoderFrameRate() {
        synchronized (contextLock) {
            AVRational averageFrameRate = avFormatContext.streams(videoStreamId).avg_frame_rate();
            int numerator = averageFrameRate == null || averageFrameRate.isNull()
                    ? 0 : averageFrameRate.num();
            int denominator = averageFrameRate == null || averageFrameRate.isNull()
                    ? 0 : averageFrameRate.den();
            return selectTranscoderFrameRate(numerator, denominator, fps);
        }
    }

    static int[] selectTranscoderFrameRate(int numerator, int denominator, int configuredFps) {
        double rate = denominator == 0 ? Double.NaN : (double) numerator / denominator;
        if (numerator > 0 && denominator > 0 && Double.isFinite(rate)
                && rate >= 1.0 && rate <= 240.0) {
            return new int[] {numerator, denominator};
        }
        return new int[] {configuredFps > 0 ? Math.min(configuredFps, 240) : 30, 1};
    }

    /**
     * @return true if any registered listener, in any delivery mode, is currently writing
     */
    public boolean hasWritingListener() {
        for (var subscription : videoSubscriptions) {
            if (subscription.listener().isWriting()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if any listener registered under the given delivery mode is currently writing
     */
    public boolean hasWritingListener(DeliveryMode mode) {
        for (var subscription : videoSubscriptions) {
            if (subscription.mode() == mode && subscription.listener().isWriting()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        try {
            do {
                processStreamPackets();
                throwIfTranscoderFailed();
                logger.info("End of MISB TS stream");
                if (loop) {
                    avformat.av_seek_frame(avFormatContext, 0, 0, avformat.AVSEEK_FLAG_ANY);
                }
            }
            while (loop);
        } finally {
            shutdownExecutors();
        }
    }

    private void submitPacketToListeners(AVPacket avPacket, Set<DeliveryMode> targetModes) {

        boolean isKeyFrame = (avPacket.flags() & avcodec.AV_PKT_FLAG_KEY) != 0;

        // Process video packet
        byte[] dataBuffer = new byte[avPacket.size()];
        avPacket.data().get(dataBuffer);

        // Prepend the SPS/PPS header to key frames so each one can be decoded standalone
        if (isKeyFrame && spsPpsHeader != null) {
            byte[] combined = new byte[spsPpsHeader.length + dataBuffer.length];
            System.arraycopy(spsPpsHeader, 0, combined, 0, spsPpsHeader.length);
            System.arraycopy(dataBuffer, 0, combined, spsPpsHeader.length, dataBuffer.length);
            dataBuffer = combined;
        }

        // Pass data buffer to interested listeners
        AVRational packetTimeBase = avPacket.time_base();
        int packetTimeBaseNumerator = videoStreamTimeBaseNumerator;
        int packetTimeBaseDenominator = videoStreamTimeBaseDenominator;
        if (packetTimeBase != null && packetTimeBase.num() > 0 && packetTimeBase.den() > 0) {
            packetTimeBaseNumerator = packetTimeBase.num();
            packetTimeBaseDenominator = packetTimeBase.den();
        }
        double packetTimeBaseUnits = (double) packetTimeBaseNumerator / packetTimeBaseDenominator;
        double presentationTimestamp = presentationTimestamp(
                avPacket.pts(), avPacket.dts(), packetTimeBaseUnits);
        notifyVideoDataBufferListeners(new DataBufferRecord(
                presentationTimestamp,
                dataBuffer,
                isKeyFrame,
                avPacket.pts(),
                avPacket.dts(),
                avPacket.duration(),
                packetTimeBaseNumerator,
                packetTimeBaseDenominator), targetModes);
    }

    private void deliverVideoPacket(AVPacket packet) {
        boolean isWriting = hasWritingListener();
        boolean bufferedListenerIsWriting = hasWritingListener(DeliveryMode.BUFFERED);
        if (bufferedListenerIsWriting) {
            if (!hasBufferedFramesOrReplay()) {
                submitPacketToListeners(packet, ALL_MODES);
            } else {
                // BUFFERED listeners receive both pre-roll and subsequent packets in order from
                // the replay queue. LIVE listeners remain current.
                submitPacketToListeners(packet, LIVE_MODES);
                submitPacketToFrameQueue(packet);
                submitFrameQueueToListenersAsync();
            }
        } else {
            if (isWriting) {
                submitPacketToListeners(packet, LIVE_MODES);
            }
            submitPacketToFrameQueue(packet);
        }
    }

    static double presentationTimestamp(long pts, long dts, double timeBaseUnits) {
        long presentationTime = pts != AV_NOPTS_VALUE ? pts : dts;
        return presentationTime == AV_NOPTS_VALUE
                ? Double.NaN
                : presentationTime * timeBaseUnits;
    }

    private void submitFrameQueueToListenersAsync() {
        synchronized (frameQueueLock) {
            if (isSubmittingFrameQueue || fileFrameQueue == null || fileFrameQueue.isEmpty()) {
                return;
            }
            if (!hasWritingListener(DeliveryMode.BUFFERED)) {
                return;
            }

            if (frameQueueSubmitter == null || frameQueueSubmitter.isShutdown()) {
                frameQueueSubmitter = Executors.newSingleThreadExecutor(
                        new ListenerThreadFactory(WORKER_THREAD_NAME, "FrameQueueSubmit"));
            }
            isSubmittingFrameQueue = true;
            try {
                frameQueueSubmitter.submit(this::drainFrameQueueToListeners);
            } catch (RejectedExecutionException e) {
                isSubmittingFrameQueue = false;
                logger.debug("Frame buffer replay executor is shutting down", e);
            }
        }
    }

    /**
     * Polling transfers native ownership from the queue to this worker. The packet is removed while
     * holding frameQueueLock and is always freed exactly once by this method.
     */
    private void drainFrameQueueToListeners() {
        try {
            while (true) {
                AVPacket packet;
                synchronized (frameQueueLock) {
                    if (!isSubmittingFrameQueue || fileFrameQueue == null
                            || !hasWritingListener(DeliveryMode.BUFFERED)) {
                        isSubmittingFrameQueue = false;
                        return;
                    }
                    packet = fileFrameQueue.poll();
                    if (packet == null) {
                        isSubmittingFrameQueue = false;
                        return;
                    }
                }

                try {
                    submitPacketToListeners(packet, BUFFERED_MODES);
                } finally {
                    av_packet_free(packet);
                }
            }
        } catch (RuntimeException e) {
            logger.warn("Frame buffer replay stopped after a listener rejected a packet", e);
            synchronized (frameQueueLock) {
                isSubmittingFrameQueue = false;
            }
        }
    }

    private void shutdownExecutors() {
        // Stop the producer and consumer before joining either one. Stopping the encoder first
        // while the decoder is still producing leaves orphaned native frames in their shared
        // queue. Interrupt also releases an idle worker immediately.
        if (decoder != null) {
            decoder.doRun.set(false);
            decoder.interrupt();
        }
        if (encoder != null) {
            encoder.doRun.set(false);
            encoder.interrupt();
        }

        if (decoder != null) {
            try {
                decoder.join(5_000L);
                if (decoder.isAlive()) {
                    logger.error("Timed out waiting for decoder worker to stop");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (encoder != null) {
            try {
                encoder.join(5_000L);
                if (encoder.isAlive()) {
                    logger.error("Timed out waiting for encoder worker to stop");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Coder cleanup normally drains these queues. Polling again is safe and covers a worker
        // that failed before its packet allocation phase.
        drainPacketQueue(decodeQueue);
        if (decoder != null && !decoder.isAlive()) {
            drainFrameQueue(decoder.getOutQueue());
        }
        if (encoder != null && !encoder.isAlive()) {
            drainPacketQueue(encoder.getOutQueue());
        }

        ExecutorService submitter;
        synchronized (frameQueueLock) {
            isSubmittingFrameQueue = false;
            submitter = frameQueueSubmitter;
        }
        if (submitter != null) {
            submitter.shutdownNow();
            try {
                if (!submitter.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Timed out waiting for frame buffer replay to stop");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        clearVideoDataBufferListeners();

        synchronized (frameQueueLock) {
            frameQueueSubmitter = null;
            drainAndFreeFrameQueueLocked();
            fileFrameQueue = null;
        }
    }

    private void submitPacketToFrameQueue(AVPacket avPacket) {
        AVPacket newPacket = avcodec.av_packet_clone(avPacket);
        if (newPacket == null || newPacket.isNull()) {
            logger.error("Could not clone a packet for the frame buffer");
            return;
        }

        synchronized (frameQueueLock) {
            if (fileFrameQueue == null || fileFrameQueueMax <= 0) {
                av_packet_free(newPacket);
                return;
            }
            while (fileFrameQueue.size() >= fileFrameQueueMax) {
                AVPacket evictedPacket = fileFrameQueue.poll();
                if (evictedPacket != null) {
                    av_packet_free(evictedPacket);
                }
            }
            fileFrameQueue.add(newPacket);
        }
    }

    private boolean isFrameBufferEnabled() {
        synchronized (frameQueueLock) {
            return fileFrameQueue != null;
        }
    }

    private boolean hasBufferedFramesOrReplay() {
        synchronized (frameQueueLock) {
            return isSubmittingFrameQueue || (fileFrameQueue != null && !fileFrameQueue.isEmpty());
        }
    }

    private void drainAndFreeFrameQueueLocked() {
        if (fileFrameQueue == null) {
            return;
        }
        AVPacket packet;
        while ((packet = fileFrameQueue.poll()) != null) {
            av_packet_free(packet);
        }
    }

    private static void drainPacketQueue(Queue<AVPacket> queue) {
        AVPacket packet;
        while ((packet = queue.poll()) != null) {
            av_packet_free(packet);
        }
    }

    private static void drainFrameQueue(Queue<AVFrame> queue) {
        AVFrame frame;
        while ((frame = queue.poll()) != null) {
            av_frame_free(frame);
        }
    }

    private void throwIfTranscoderFailed() {
        Throwable failure = decoder == null ? null : decoder.getFailure();
        if (failure == null && encoder != null) {
            failure = encoder.getFailure();
        }
        if (failure != null) {
            throw new IllegalStateException("Video transcoder failed", failure);
        }
    }

    private AVPacket awaitTranscodedPacket() {
        long deadline = System.nanoTime() + TRANSCODE_OUTPUT_WAIT_NANOS;
        Queue<AVPacket> outQueue = encoder.getOutQueue();
        do {
            AVPacket packet = outQueue.poll();
            if (packet != null) {
                return packet;
            }
            throwIfTranscoderFailed();
            if (!decoder.isAlive() || !encoder.isAlive() || Thread.currentThread().isInterrupted()) {
                return null;
            }
            LockSupport.parkNanos(500_000L);
        } while (System.nanoTime() < deadline);
        return null;
    }

    private void flushTranscoderAtEndOfStream() {
        decoder.finishInput();
        awaitCoderFlush(decoder, "decoder");

        // The decoder can emit delayed frames while flushing. Do not finish the encoder until the
        // decoder has stopped producing and the encoder has consumed everything in their shared
        // queue.
        encoder.finishInput();
        awaitCoderFlush(encoder, "encoder");
        throwIfTranscoderFailed();

        AVPacket packet;
        while ((packet = encoder.getOutQueue().poll()) != null) {
            try {
                deliverVideoPacket(packet);
            } finally {
                av_packet_free(packet);
            }
        }
    }

    private void awaitCoderFlush(Coder<?, ?> coder, String name) {
        try {
            coder.join(TRANSCODER_FLUSH_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while flushing video " + name, e);
        }
        if (coder.isAlive()) {
            throw new IllegalStateException("Timed out flushing video " + name);
        }
        if (coder.getFailure() != null) {
            throw new IllegalStateException("Video " + name + " failed while flushing", coder.getFailure());
        }
        if (!coder.isOutputComplete()) {
            throw new IllegalStateException("Video " + name + " stopped before end-of-input was flushed");
        }
    }

    private int readPacket(AVPacket avPacket) {
        synchronized (contextLock) {
            if (terminateProcessing || !streamOpened) {
                return AVERROR_EOF;
            }
            return av_read_frame(avFormatContext, avPacket);
        }
    }

    /**
     * Stream processing, where the demuxing is invoked on underlying ffmpeg libraries
     * and callbacks, if registered, are invoked for appropriate buffers.
     */
    private void processStreamPackets() {
        // This container remains owned by the demux loop. Transcoded output is held separately.
        AVPacket demuxPacket = avcodec.av_packet_alloc();
        if (demuxPacket == null || demuxPacket.isNull()) {
            logger.error("Could not allocate a packet for the video demuxer");
            return;
        }

        // Read frames
        long startTime = System.currentTimeMillis();
        long frameCount = 0;

        try {
            while (!terminateProcessing) {
                int readResult = readPacket(demuxPacket);
                if (readResult == AVERROR_EAGAIN()) {
                    LockSupport.parkNanos(500_000L);
                    continue;
                }
                if (readResult == AVERROR_EOF) {
                    if (doTranscode && !loop) {
                        flushTranscoderAtEndOfStream();
                    }
                    break;
                }
                if (readResult < 0) {
                    throw new IllegalStateException(
                            "Could not read video input (FFmpeg error " + readResult + ")");
                }

                AVPacket deliveryPacket = demuxPacket;
                try {
                    // Discard the frame if no listeners are writing and no pre-roll is configured.
                    boolean isWriting = hasWritingListener();
                    boolean frameBufferEnabled = isFrameBufferEnabled();
                    boolean skipProcessing = !isWriting && !frameBufferEnabled;

                    if (skipProcessing) {
                        drainPacketQueue(decodeQueue);
                        if (decoder != null) {
                            drainFrameQueue(decoder.getOutQueue());
                        }
                        if (encoder != null) {
                            drainPacketQueue(encoder.getOutQueue());
                        }
                        continue;
                    }

                    // Only the selected, metadata-validated video stream is delivered.
                    if (demuxPacket.stream_index() != videoStreamId) {
                        continue;
                    }

                    // if FPS is set, we may have to wait a little
                    if (fps > 0) {
                        var now = System.currentTimeMillis();
                        var sleepDuration = frameCount * 1000 / fps - (now - startTime);
                        if (sleepDuration > 0) {
                            try {
                                Thread.sleep(sleepDuration);
                            } catch (InterruptedException e) {
                                logger.error("Interrupted waiting for stream processor to stop", e);
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                    // Pacing follows selected input packets, not encoder output. The asynchronous
                    // encoder often has no packet ready for the first few inputs; incrementing only
                    // after output made file inputs race to EOF and shut the coders down untouched.
                    frameCount++;

                    if (doTranscode) {
                        throwIfTranscoderFailed();
                        AVPacket decoderInput = avcodec.av_packet_clone(demuxPacket);
                        if (decoderInput == null || decoderInput.isNull()) {
                            logger.error("Could not clone a packet for video transcoding");
                            continue;
                        }
                        decodeQueue.add(decoderInput);
                        // Wait for the asynchronous pipeline instead of racing a zero-FPS file to
                        // EOF before either worker can produce output. The wait is bounded and both
                        // worker failures and shutdown interrupt it promptly.
                        deliveryPacket = awaitTranscodedPacket();
                        if (deliveryPacket == null) {
                            throwIfTranscoderFailed();
                            continue;
                        }
                    }

                    deliverVideoPacket(deliveryPacket);
                } finally {
                    releaseIterationPackets(demuxPacket, deliveryPacket);
                }
            }
        } finally {
            av_packet_free(demuxPacket);
        }
    }

    /**
     * Releases ownership acquired during one demux-loop iteration. The demux container is reused;
     * a distinct encoder-output packet is a one-shot native allocation and is freed here.
     */
    static void releaseIterationPackets(AVPacket demuxPacket, AVPacket deliveryPacket) {
        if (deliveryPacket != null && deliveryPacket != demuxPacket) {
            av_packet_free(deliveryPacket);
        }
        if (demuxPacket != null && !demuxPacket.isNull()) {
            av_packet_unref(demuxPacket);
        }
    }

    /**
     * Closes the transport stream, releasing allocated resources including the codec context.
     */
    public void closeStream() {

        logger.debug("closeStream");

        synchronized (contextLock) {
            closeInputContext();
            streamOpened = false;
        }
    }

    /**
     * Indicate to processor to stop processing packets from the stream
     */
    public void stopProcessingStream() {

        logger.debug("stopProcessingStream");

        loop = false;
        terminateProcessing = true;
    }

    public int getCodecId() {
        logger.debug("getCodecId");

        if (INVALID_STREAM_ID == videoStreamId) {

            String message = "Stream does not contain video frames";

            logger.error(message);

            throw new IllegalStateException(message);
        }

        return avFormatContext.streams(videoStreamId).codecpar().codec_id();
    }
}
