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
import org.bytedeco.ffmpeg.global.avutil;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.io.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    AVRational inputFrameRate = new AVRational();
    final Queue<AVPacket> packetQueue = new ConcurrentLinkedQueue<>();

    final int MAX_PACKET_QUEUE_SIZE = 1000;
    volatile long filePts;
    volatile long curPts;
    private WriteCallback writeCallback;
    private SeekCallback seekCallback;
    BytePointer buffer;
    ByteArraySeekableBuffer seekableBuffer;
    String fileName;
    private boolean isLive = false;

    public FileOutput(FFMPEGSensorBase<FFMPEGConfigType> parentSensor, String name) throws SensorHubException {
        super(name, parentSensor);
        this.outputName = name;
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
    public void onDataBuffer(DataBufferRecord record) {
        if (!isWriting()) {
            return;
        }
        var data = record.getDataBuffer();

        long timestamp = (long)(record.getPresentationTimestamp() * inputTimeBase.den() / inputTimeBase.num());

        AVPacket avPacket = av_packet_alloc();
        av_new_packet(avPacket, data.length);
        avPacket.data().put(data);
        avPacket.pts(timestamp);
        avPacket.dts(timestamp);

        if (record.isKeyFrame())
            avPacket.flags(avPacket.flags() | AV_PKT_FLAG_KEY);

        //packetQueue.add(avPacket);
        packetTiming(avPacket);
        av_interleaved_write_frame(outputContext, avPacket);
        av_packet_free(avPacket);
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
        try {
            synchronized (contextLock) {
                if (doFileWrite.get()) {
                    throw new IOException("Already writing to file " + this.fileName);
                }

                this.outputStream = null;
                this.fileName = fileName;

                seekableBuffer = null;
                writeCallback = null;
                seekCallback = null;

                AVCodecParameters avCodecParameters = this.parentSensor.getProcessor().getCodecParams();
                if (avCodecParameters == null) {
                    logger.error("Could not open file output; this stream's codec parameters are null");
                    return;
                }

                var inStream = parentSensor.getProcessor().getAvStream();
                if (inStream != null) {
                    if ((inputTimeBase = inStream.time_base()) == null) {
                        inputTimeBase = av_make_q(1, 90000);
                        logger.warn("Could not get input timebase; using default timebase of 1/90k");
                    }
                    if ((inputFrameRate = inStream.avg_frame_rate()) == null) {
                        inputFrameRate = av_make_q(30, 1);
                        logger.warn("Could not get input framerate; using default framerate of 30fps");
                    }
                } else {
                    inputTimeBase = av_make_q(1, 90000);
                    inputFrameRate = av_make_q(30, 1);
                    logger.warn("Could not get input stream; using default timebase of 1/90k and framerate of 30fps");
                }

                initCtx(avCodecParameters);

                avcodec_parameters_free(avCodecParameters);

                if ((outputContext.oformat().flags() & AVFMT_NOFILE) == 0) { // IMPORTANT! HLS does not have a pb!
                    AVIOContext avioContext = new AVIOContext(null);
                    if (avio_open(avioContext, this.fileName, AVIO_FLAG_WRITE) < 0) {
                        throw new IOException("Could not open file.");
                    }
                    outputContext.pb(avioContext);
                }

                if (this.fileName.endsWith(".m3u8")) {
                    options = hlsOptions();
                    isLive = true;
                    //filePts = System.currentTimeMillis();
                    //inputTimeBase = av_make_q(1, 1000);
                } else {
                    isLive = false;
                }

                int ret;
                if ((ret = avformat.avformat_write_header(outputContext, options)) < 0) {
                    logFFmpeg(ret);
                    throw new IOException("Could not write header to file.");
                }

                timeBase = outputVideoStream.time_base();
                //avPacket = av_packet_alloc();

                packetQueue.clear();
                //startWriterThread();
                doFileWrite.set(true);
                av_log_set_level(AV_LOG_QUIET);
            }
        } catch (Exception e) {
            throw new IOException("Could not open output file " + fileName, e);
        } finally {
            if (options != null) {
                av_dict_free(options);
            }
        }
    }

    private void initCtx(AVCodecParameters codecParams) {
        filePts = 0;
        curPts = 0;

        //AVFormatContext inputContext = this.parentSensor.getProcessor().getAvFormatContext();
        outputContext = new AVFormatContext(null);
        avformat_alloc_output_context2(outputContext, null, null, this.fileName);

        outputVideoStream = avformat.avformat_new_stream(outputContext, null);

        avcodec.avcodec_parameters_copy(outputVideoStream.codecpar(), codecParams);
        //FFmpegUtils.cleanCodecParameters(outputVideoStream.codecpar());

        timeBase = av_make_q(1, 90000);

        // We're transcoding, need to override some of the copied values
        outputVideoStream.codecpar().codec_id(AV_CODEC_ID_H264);
        outputVideoStream.codecpar().codec_tag(0);

        outputVideoStream.time_base(timeBase);
        outputVideoStream.duration(AV_NOPTS_VALUE);

        //outputContext.flags(outputContext.flags() | AVFormatContext.AVFMT_FLAG_GENPTS);
        outputContext.flags(outputContext.flags() | AVFormatContext.AVFMT_FLAG_AUTO_BSF);

        outputVideoStream.start_time(0);
        outputVideoStream.position(0);

        outputContext.start_time(0);
        outputContext.position(0);
    }

    private void logFFmpeg(int retCode) {
        BytePointer buf = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
        try {
            av_strerror(retCode, buf, buf.capacity());
            logger.error("FFmpeg returned error code {}: {}", retCode, buf.getString());
        } finally {
            buf.deallocate();
            buf.close();
        }
    }

    private void packetTiming(AVPacket avPacket) {
        if (filePts <= 0) {
            filePts = avPacket.pts();
        }

        long newPts = avutil.av_rescale_q(avPacket.pts() - filePts, inputTimeBase, timeBase);
        if (isLive && newPts <= curPts) {
            newPts = curPts + 1;
        }
        avPacket.pts(newPts);
        avPacket.dts(newPts);

        //avPacket.duration(av_rescale_q(1, inputFrameRate, timeBase));
        //avPacket.time_base(timeBase);
        curPts = newPts;
    }

    private static AVDictionary hlsOptions() {
        AVDictionary options = new AVDictionary();
        av_dict_set(options, "hls_list_size", "2", 0);
        av_dict_set(options, "hls_time", "1", 0);
        //avutil.av_dict_set(options, "hls_segment_type", "ts", 0);
        //avutil.av_dict_set(options, "movflags", "faststart+default_base_moof", 0);
        av_dict_set(options, "hls_flags", "delete_segments+append_list+split_by_time+program_date_time", 0);
        av_dict_set(options, "hls_segment_type", "mpegts", 0);
        av_dict_set(options, "use_localtime", "1", 0);
        av_dict_set(options, "max_delay", "0", 0);
        av_dict_set(options, "muxdelay", "0", 0);
        av_dict_set(options, "muxpreload", "0", 0);
        //av_dict_set(options, "hls_fmp4_init_filename", "init.mp4", 0);
        return options;
    }

    public void closeFile() throws IOException {
        synchronized (contextLock) {
            if (isWriting()) {
                doFileWrite.set(false);

                try {
                    if (outputContext.pb() != null) {
                        avio_flush(outputContext.pb());
                    }

                    avformat.av_write_trailer(outputContext);

                    if (outputContext.pb() != null) {
                        avio_flush(outputContext.pb());
                        avio_closep(outputContext.pb());
                        outputContext.pb(null);
                    }

                    if (outputStream != null) {
                        outputStream.write(seekableBuffer.getData());
                        outputStream.flush();
                        outputStream.close();
                        outputStream = null;
                    }

                    if (writeCallback != null) {
                        writeCallback.free();
                        writeCallback = null;
                    }
                    if (seekCallback != null) {
                        seekCallback.free();
                        seekCallback = null;
                    }

                    if (outputVideoStream != null) {
                        outputVideoStream.close();
                        outputVideoStream = null;
                    }

                    avformat_free_context(outputContext);
                    outputContext = null;
                } catch (Exception e) {
                    throw new IOException("Could not close output file " + this.fileName + ".", e);
                }
            }
        }
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

        public void free() {
            buffer = null;
        }
    }

    private static class SeekCallback extends org.bytedeco.ffmpeg.avformat.Seek_Pointer_long_int {
        private ByteArraySeekableBuffer buffer;

        public SeekCallback(ByteArraySeekableBuffer buffer) {
            this.buffer = buffer;
        }

        public void free() {
            buffer = null;
        }

        @Override
        public long call(Pointer opaque, long offset, int whence) {
            return buffer.seek(offset, whence);
        }
    }
}


