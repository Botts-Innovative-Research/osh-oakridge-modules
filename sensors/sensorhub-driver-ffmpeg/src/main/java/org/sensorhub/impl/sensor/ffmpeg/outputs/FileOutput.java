package org.sensorhub.impl.sensor.ffmpeg.outputs;

//import com.botts.impl.service.oscar.Constants; TODO Circular dependency, maybe move the constant to some other package?
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.TextEncoding;
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
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
    volatile long filePts;
    private WriteCallback writeCallback;
    private SeekCallback seekCallback;
    BytePointer buffer;
    ByteArraySeekableBuffer seekableBuffer;
    String fileName;

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
        synchronized (contextLock) {
            var data = record.getDataBuffer();
            long timestamp = (long)(record.getPresentationTimestamp() * inputTimeBase.den() / inputTimeBase.num());

            AVPacket avPacket = av_packet_alloc();
            av_new_packet(avPacket, data.length);
            avPacket.data().put(data);
            avPacket.pts(timestamp);
            avPacket.dts(timestamp);

            if (record.isKeyFrame()) {
                avPacket.flags(avPacket.flags() | avcodec.AV_PKT_FLAG_KEY);
            }

            if (doFileWrite.get()) {
                if (outputContext != null) {
                    packetTiming(avPacket);
                    av_write_frame(outputContext, avPacket);
                    av_packet_free(avPacket);
                } else {
                    logger.error("Cannot write to file; output context is null");
                }
            }
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

    // TODO Reduce reused code
    /**
     * Write video data to an OutputStream. A file name is still required for determining file format and
     * broadcasting the file name with {@link #publish()}.
     * @param outputStream
     * @param fileName
     * @throws IOException
     */
    public void openFile(OutputStream outputStream, String fileName) throws IOException {
        try {
            if (doFileWrite.get()) {
                throw new IOException("Already writing to file " + this.fileName);
            }

            this.outputStream = outputStream;
            this.fileName = fileName;

            seekableBuffer = new ByteArraySeekableBuffer(8 * 1024 * 1024); // 8 MB initial size
            writeCallback = new WriteCallback(seekableBuffer).retainReference();
            seekCallback = new SeekCallback(seekableBuffer).retainReference();

            /*
            filePts = 0;

            //AVFormatContext inputContext = this.parentSensor.getProcessor().getAvFormatContext();
            AVStream inputStream = this.parentSensor.getProcessor().getAvStream();

            outputContext = new AVFormatContext(null);
            avformat_alloc_output_context2(outputContext, null, null, this.fileName); // Assuming always mp4 output

            outputVideoStream = avformat.avformat_new_stream(outputContext, null);

            avcodec.avcodec_parameters_copy(outputVideoStream.codecpar(), inputStream.codecpar());

            // We're transcoding, need to override some of the copied values
            outputVideoStream.codecpar().codec_id(AV_CODEC_ID_H264);
            outputVideoStream.codecpar().codec_tag(0);

            outputVideoStream.time_base(timeBase);
            outputVideoStream.duration(AV_NOPTS_VALUE);

            outputVideoStream.start_time(0);
            outputVideoStream.position(0);

             */
            initCtx();

            //timeBase = (double) inputStream.time_base().num() / inputStream.time_base().den();

            this.buffer = new BytePointer(avutil.av_malloc(4096)).capacity(4096);
            var avio = avio_alloc_context(buffer, 4096, 1, null, null, writeCallback, seekCallback);
            outputContext.pb(avio);
            outputContext.url(new BytePointer(av_malloc(1)).putString(""));

            AVDictionary options = null;
            if (this.fileName.endsWith(".m3u8")) {
                options = hlsOptions();
            }

            int ret;
            if ((ret = avformat.avformat_write_header(outputContext, options)) < 0) {
                logFFmpeg(ret);
                throw new IOException("Could not write header to file.");
            }

            /*
            while (!framesSinceKey.isEmpty()) {
                AVPacket packet = framesSinceKey.pop();
                packetTiming(packet);
                av_write_frame(outputContext, packet);
                av_packet_free(packet);
            }

             */

            timeBase = outputVideoStream.time_base();
            doFileWrite.set(true);
        } catch (Exception e) {
            throw new IOException("Could not open output file " + fileName, e);
        }
    }

    /**
     * Write video data to a file. Format determined by file name suffix.
     * @param fileName
     * @throws IOException
     */
    public void openFile(String fileName) throws IOException {
        try {
            if (doFileWrite.get()) {
                throw new IOException("Already writing to file " + this.fileName);
            }
            //avutil.av_log_set_level(AV_LOG_DEBUG);

            // TODO REMOVE REPEAT CODE
            this.outputStream = null;
            this.fileName = fileName;

            seekableBuffer = null;
            writeCallback = null;
            seekCallback = null;

            /*
            filePts = 0;
            int ret;

            //AVFormatContext inputContext = this.parentSensor.getProcessor().getAvFormatContext();
            AVStream inputStream = this.parentSensor.getProcessor().getAvStream();

            outputContext = new AVFormatContext(null);
            avformat_alloc_output_context2(outputContext, null, null, this.fileName);

            outputVideoStream = avformat.avformat_new_stream(outputContext, null);

            avcodec.avcodec_parameters_copy(outputVideoStream.codecpar(), inputStream.codecpar());

            // We're transcoding, need to override some of the copied values
            outputVideoStream.codecpar().codec_id(AV_CODEC_ID_H264);
            outputVideoStream.codecpar().codec_tag(0);

            outputVideoStream.time_base(timeBase);

            outputVideoStream.start_time(0);
            outputVideoStream.position(0);

             */
            initCtx();

            //timeBase = (double) inputStream.time_base().num() / inputStream.time_base().den();

            if ((outputContext.oformat().flags() & AVFMT_NOFILE) == 0) { // IMPORTANT! HLS does not have a pb!
                AVIOContext avioContext = new AVIOContext(null);
                if (avio_open(avioContext, this.fileName, AVIO_FLAG_WRITE) < 0) {
                    throw new IOException("Could not open file.");
                }
                outputContext.pb(avioContext);
            }

            AVDictionary options = null;
            if (this.fileName.endsWith(".m3u8")) {
                options = hlsOptions();
            }

            int ret;
            if ((ret = avformat.avformat_write_header(outputContext, options)) < 0) {
                logFFmpeg(ret);
                throw new IOException("Could not write header to file.");
            }

            /*
            while (!framesSinceKey.isEmpty()) {
                AVPacket packet = framesSinceKey.pop();
                packetTiming(packet);
                av_write_frame(outputContext, packet);
                av_packet_free(packet);
            }

             */

            timeBase = outputVideoStream.time_base();
            doFileWrite.set(true);
        } catch (Exception e) {
            throw new IOException("Could not open output file " + fileName, e);
        }
    }

    private void initCtx() {
        filePts = 0;

        //AVFormatContext inputContext = this.parentSensor.getProcessor().getAvFormatContext();
        AVStream inputStream = this.parentSensor.getProcessor().getAvStream();

        outputContext = new AVFormatContext(null);
        avformat_alloc_output_context2(outputContext, null, null, this.fileName);

        outputVideoStream = avformat.avformat_new_stream(outputContext, null);

        avcodec.avcodec_parameters_copy(outputVideoStream.codecpar(), inputStream.codecpar());
        inputTimeBase = inputStream.time_base();

        // We're transcoding, need to override some of the copied values
        outputVideoStream.codecpar().codec_id(AV_CODEC_ID_H264);
        outputVideoStream.codecpar().codec_tag(0);

        //outputVideoStream.time_base(timeBase);
        outputVideoStream.duration(AV_NOPTS_VALUE);

        outputVideoStream.start_time(0);
        outputVideoStream.position(0);

        outputContext.start_time(0);
        outputContext.position(0);
    }

    private void logFFmpeg(int retCode) {
        BytePointer buf = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
        av_strerror(retCode, buf, buf.capacity());
        logger.error("FFmpeg returned error code {}: {}", retCode, buf.getString());
    }

    private void packetTiming(AVPacket avPacket) {
        if (filePts <= 0) {
            filePts = avPacket.pts();
        }
        long newPts = avutil.av_rescale_q(avPacket.pts() - filePts, timeBase, inputTimeBase);
        avPacket.pts(newPts);
        avPacket.dts(newPts);
        avPacket.time_base(timeBase);
    }

    private static AVDictionary hlsOptions() {
        AVDictionary options = new AVDictionary();
        avutil.av_dict_set(options, "hls_list_size", "5", 0);
        avutil.av_dict_set(options, "hls_time", "4", 0);
        //avutil.av_dict_set(options, "hls_segment_type", "ts", 0);
        //avutil.av_dict_set(options, "movflags", "faststart+default_base_moof", 0);
        avutil.av_dict_set(options, "hls_flags", "delete_segments+append_list", 0);
        //av_dict_set(options, "hls_fmp4_init_filename", "init.mp4", 0);
        return options;
    }

    public void closeFile() throws IOException {
        synchronized (contextLock) {
            if (doFileWrite.get()) {
                doFileWrite.set(false);
                try {
                    if (outputContext.pb() != null) {
                        avio_flush(outputContext.pb());
                    }

                    avformat.av_write_trailer(outputContext);

                    if (outputContext.pb() != null) {
                        avio_flush(outputContext.pb());
                        avio_close(outputContext.pb());
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


