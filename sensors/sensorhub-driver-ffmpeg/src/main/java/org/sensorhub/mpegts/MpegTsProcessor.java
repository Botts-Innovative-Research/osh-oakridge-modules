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
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.PointerPointer;
import org.sensorhub.impl.process.video.transcoder.coders.Decoder;
import org.sensorhub.impl.process.video.transcoder.coders.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avformat.*;

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

    boolean injectExtradata = true;

    public final Object contextLock = new Object();

    /**
     * Name of thread
     */
    private static final String WORKER_THREAD_NAME = "STREAM-PROCESSOR";

    /**
     * Id of invalid sub streams within actual stream
     */
    protected static final int INVALID_STREAM_ID = -1;

    /**
     * Context used by underlying ffmpeg library to decode stream
     */
    private AVFormatContext avFormatContext;

    /**
     * Codec context, holds information about the codec used in transport stream
     */
    private AVCodecContext avCodecContext;

    /**
     * Container for possible video sub stream id
     */
    private int videoStreamId = INVALID_STREAM_ID;

    /**
     * Container for possible data sub stream id
     */
    private int dataStreamId = INVALID_STREAM_ID;

    /**
     * Listener for video buffers extracted from the transport stream
     */
    private final List<DataBufferListener> videoDataBufferListeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Flag indicating if processing of the transport stream should be terminated
     */
    private final AtomicBoolean terminateProcessing = new AtomicBoolean(false);

    /**
     * Flag indicating whether or not the stream has been opened or connected to successfully
     */
    private boolean streamOpened = false;

    /**
     * A string representation of the file or url to use as the source of the transport stream to
     * demux
     */
    private final String streamSource;

    /**
     * Time base units for video stream timing used to compute a timestamp for each packet extracted
     */
    private double videoStreamTimeBase;

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

    private final Queue<AVPacket> decodeQueue = new ArrayDeque<>();


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
     * Attempts to open the stream given the {@link MpegTsProcessor#streamSource}
     * Opening the stream entails establishing appropriate connection and ability to
     * extract stream information.
     *
     * @return true if the stream is opened succesfully, false otherwise.
     */
    public boolean openStream() {
        synchronized (contextLock) {

            try {
                logger.debug("openStream");

                avformat.avformat_network_init();

                AVDictionary options = new AVDictionary();


                if (useTCP) {
                    avutil.av_dict_set(options, "rtsp_transport", "tcp", 0);
                }

                avutil.av_dict_set(options, "timeout", Long.toString(timeout), 0);

                // Create a new AV Format Context for I/O
                avFormatContext = new AVFormatContext(null);

                int returnCode = avformat.avformat_open_input(avFormatContext, streamSource, null, options);

                // Attempt to open the stream, streamPath can be a file or URL
                if (returnCode == 0) {

                    returnCode = avformat.avformat_find_stream_info(avFormatContext, (PointerPointer<?>) null);

                    if (returnCode < 0) {

                        logger.error("Failed to find stream info");

                    } else {

                        streamOpened = true;
                        logger.debug("Stream opened {}", streamSource);
                    }
                } else {

                    logger.error("Failed to open stream: {}", streamSource);
                }
            } catch (Exception e) { logger.error("Exception when opening stream", e); }
        }

        return streamOpened;
    }

    public void setInjectExtradata(boolean injectExtradata) {this.injectExtradata = injectExtradata;}

    public AVCodecParameters getCodecParams() {
        synchronized (contextLock) {
            try {
                AVCodecParameters src = avFormatContext.streams(videoStreamId).codecpar();
                AVCodecParameters dst = avcodec.avcodec_parameters_alloc();
                if (src == null) {
                    logger.error("Could not allocate AVCodecParameters");
                    return null;
                }
                if (dst == null) {
                    logger.error("Could not allocate AVCodecParameters");
                    return null;
                }
                int ret = avcodec.avcodec_parameters_copy(dst, src);
                if (ret < 0) {
                    avcodec.avcodec_parameters_free(dst);
                    logger.error("AVCodecParameters failed: {}", ret);
                }
                return dst;
            } catch (Exception e) { return null; }
        }
    }

    public AVStream getAvStream() {
        synchronized (contextLock) {
            return avFormatContext.streams(videoStreamId);
        }
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

        for (int streamId = 0; streamId < avFormatContext.nb_streams(); ++streamId) {

            int codecType = avFormatContext.streams(streamId).codecpar().codec_type();

            AVRational timeBase = avFormatContext.streams(streamId).time_base();

            double num = timeBase.num();
            double den = timeBase.den();

            double timeBaseUnits = num / den;

            if (INVALID_STREAM_ID == videoStreamId && avutil.AVMEDIA_TYPE_VIDEO == codecType) {

                logger.debug("Video stream present with id: {}", streamId);

                videoStreamId = streamId;

                // Transcode if not h264
                doTranscode = avFormatContext.streams(streamId).codecpar().codec_id() != avcodec.AV_CODEC_ID_H264;

                videoStreamTimeBase = timeBaseUnits;
            }

            // Injecting extradata as-is only works for receiving video streams
            // For file h264 containers (like mp4) we will want to convert the
            // PPS/SPS extradata from AVCC to Annex B.
            // TODO: Detect AVCC, convert to Annex B
            int extraSize = avFormatContext.streams(streamId).codecpar().extradata_size();
            if (injectExtradata && extraSize > 0 && avFormatContext.streams(streamId).codecpar().codec_id() == avcodec.AV_CODEC_ID_H264) {
                spsPpsHeader = new byte[extraSize];
                avFormatContext.streams(streamId).codecpar().extradata().get(spsPpsHeader);
            } else {
                spsPpsHeader = null;
            }


//            if (INVALID_STREAM_ID == dataStreamId && avutil.AVMEDIA_TYPE_DATA == codecType) {
//
//                logger.debug("Data stream present with id: {}", streamId);
//
//                dataStreamId = streamId;
//
//                dataStreamTimeBase = timeBaseUnits;
//            }
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

    public void addVideoDataBufferListener(DataBufferListener videoDataBufferListener) throws NullPointerException {
        synchronized (videoDataBufferListeners) {
            if (null == videoDataBufferListener) {

                throw new NullPointerException("Attempt to set null videoStreamPacketListener");
            }

            this.videoDataBufferListeners.add(videoDataBufferListener);
        }
    }

    public void clearVideoDataBufferListeners() {
        synchronized (videoDataBufferListeners) {
            this.videoDataBufferListeners.clear();
        }
    }

    public void removeVideoDataBufferListener(DataBufferListener videoDataBufferListener) throws NullPointerException {
        synchronized (videoDataBufferListeners) {
            this.videoDataBufferListeners.remove(videoDataBufferListener);
        }
    }

    protected void notifyVideoDataBufferListeners(DataBufferRecord dataRecord) {
        for (var videoBuffers : videoDataBufferListeners) {
            videoBuffers.onDataBuffer(dataRecord);
        }
    }

    public <T extends DataBufferListener> List<T> getVideoDataBufferListenersByType(Class<T> dataBufferListenerType) {
        List<T> out = new ArrayList<>();
        synchronized (videoDataBufferListeners) {
            for (var listener : videoDataBufferListeners) {
                if (dataBufferListenerType.isInstance(listener)) {
                    out.add((T) listener);
                }
            }
        }
        return out;
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
            //avutil.av_log_set_level(avutil.AV_LOG_DEBUG);
            // Allocate the codec contexts and attempt to open them
            openCodecContext();

            if (doTranscode) {
                var settings = new HashMap<String, Integer>();
                var dims = getVideoStreamFrameDimensions();
                settings.put("width", dims[0]);
                settings.put("height", dims[1]);
                settings.put("pix_fmt", avutil.AV_PIX_FMT_YUV420P);
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

    @Override
    public void run() {

        do {
            processStreamPackets();
            logger.info("End of MISB TS stream");
            if (loop) {
                avformat.av_seek_frame(avFormatContext, 0, 0, avformat.AVSEEK_FLAG_ANY);
            }
        }
        while (loop);

        if (encoder != null) {
            encoder.doRun.set(false);
            try {
                encoder.join();
            } catch (Exception ignored) {}
        }
        if (decoder != null) {
            decoder.doRun.set(false);
            try {
                decoder.join();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Stream processing, where the demuxing is invoked on underlying ffmpeg libraries
     * and callbacks, if registered, are invoked for appropriate buffers.
     */
    private void processStreamPackets() {

        AVPacket avPacket = null;

        try {
            // Create an AV packet container to pass data to demuxer
            avPacket = new AVPacket();

            // Read frames
            long startTime = System.currentTimeMillis();
            long frameCount = 0;
            int retCode;
            boolean doProcessing = true;

            while (!terminateProcessing.get() && (retCode = av_read_frame(avFormatContext, avPacket)) >= 0) {
                boolean skipProcessing = true;
                for (DataBufferListener listener : videoDataBufferListeners) {
                    if (listener.isWriting()) {
                        skipProcessing = false;
                        break;
                    }
                }
                if (skipProcessing) {
                    if (decodeQueue != null) {
                        decodeQueue.clear();
                    }
                    if (decoder != null) {
                        decoder.getOutQueue().clear();
                    }
                    if (encoder != null) {
                        encoder.getOutQueue().clear();
                    }
                    continue;
                }

                // If it is a video or data frame
                if ((avPacket.stream_index() == videoStreamId)) {

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

                    if (doTranscode) {
                        decodeQueue.add(avcodec.av_packet_clone(avPacket));
                        var outQueue = encoder.getOutQueue();

                        if (outQueue.isEmpty())
                            continue;
                        else
                            avPacket = outQueue.poll();
                    }

                    boolean isKeyFrame = (avPacket.flags() & avcodec.AV_PKT_FLAG_KEY) != 0;

                    // Process video packet
                    byte[] dataBuffer = new byte[avPacket.size()];
                    avPacket.data().get(dataBuffer);

                    // Pass data buffer to interested listeners
                    frameCount++;

                    if (isKeyFrame && spsPpsHeader != null) {
                        byte[] combined = new byte[spsPpsHeader.length + dataBuffer.length];
                        System.arraycopy(spsPpsHeader, 0, combined, 0, spsPpsHeader.length);
                        System.arraycopy(dataBuffer, 0, combined, spsPpsHeader.length, dataBuffer.length);

                        notifyVideoDataBufferListeners(new DataBufferRecord(avPacket.pts() * videoStreamTimeBase, combined, isKeyFrame));
                    } else {
                        notifyVideoDataBufferListeners(new DataBufferRecord(avPacket.pts() * videoStreamTimeBase, dataBuffer, isKeyFrame));
                    }
                }
                // clear packet
                avcodec.av_packet_unref(avPacket);
            }
        }
        finally {
            // fully deallocate packet
            if (avPacket != null)
            {
                avcodec.av_packet_unref(avPacket);
                avPacket.deallocate();
            }
        }
    }

    /**
     * Closes the codec context and cleans up its associated resources.  This method is invoked
     * by {@link MpegTsProcessor#closeStream()} to ensure cleanup is neat and orderly.
     */
    private void closeCodecContext() {

        if (null != avCodecContext) {

            avcodec.avcodec_close(avCodecContext);

            avcodec.avcodec_free_context(avCodecContext);

            avCodecContext = null;
        }
    }

    /**
     * Closes the transport stream, releasing allocated resources including the codec context.
     */
    public void closeStream() {

        logger.debug("closeStream");

        if (streamOpened) {

            closeCodecContext();

            if (null != avFormatContext) {
                avformat.avformat_close_input(avFormatContext);
            }

            streamOpened = false;
        }
    }

    /**
     * Indicate to processor to stop processing packets from the stream
     */
    public void stopProcessingStream() {

        logger.debug("stopProcessingStream");

        if (streamOpened) {
            loop = false;
            terminateProcessing.set(true);
        }
    }

    /**
     * Opens the codec context, and sets it up according to the {@link MpegTsProcessor#videoStreamId}.
     * This method is invoked by {@link MpegTsProcessor#openStream()} to ensure resources are allocated
     * and codec context is setup according to contents of the transport stream.
     *
     * @throws IllegalStateException if the codec is unsupported.
     */
    private void openCodecContext() throws IllegalStateException {

        avCodecContext = avcodec.avcodec_alloc_context3(null);

        // Store the codec parameters in the codec context
        avcodec.avcodec_parameters_to_context(avCodecContext, avFormatContext.streams(videoStreamId).codecpar());

        // Get the associated codec from the id stored in the context
        AVCodec codec = avcodec.avcodec_find_decoder(avCodecContext.codec_id());
        if (null == codec) {

            String message = "Unsupported codec";

            logger.error(message);

            throw new IllegalStateException(message);
        }

        // Attempt to open the codec
        int returnCode = avcodec.avcodec_open2(avCodecContext, codec, (PointerPointer<?>) null);

        // If codec could not be opened
        if (returnCode < 0) {

            String message = "Cannot open codec";

            logger.error(message);

            throw new IllegalStateException(message);
        }
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