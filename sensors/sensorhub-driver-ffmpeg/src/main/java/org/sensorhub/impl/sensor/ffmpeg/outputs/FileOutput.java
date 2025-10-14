package org.sensorhub.impl.sensor.ffmpeg.outputs;

//import com.botts.impl.service.oscar.Constants; TODO Circular dependency, maybe move the constant to some other package?
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.TextEncoding;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
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

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
import static org.bytedeco.ffmpeg.global.avutil.*;


public class FileOutput<FFMPEGConfigType extends FFMPEGConfig> extends AbstractSensorOutput<FFMPEGSensorBase<FFMPEGConfigType>> implements DataBufferListener {

    public static final String OUTPUT_NAME = "FileNameOutput";
    public static final String OUTPUT_LABEL = "FFmpeg file output name";
    final DataComponent outputStruct;
    final TextEncoding outputEncoding;

    private static final Logger logger = LoggerFactory.getLogger(FileOutput.class);
    private volatile boolean doFileWrite = false;
    //private static final String BUCKET_NAME = Constants.VIDEO_BUCKET;
    private static final String BUCKET_NAME = "videos";
    //private String outputFile = "";
    private AVFormatContext outputContext;
    private final Object contextLock = new Object();
    private final ArrayDeque<AVPacket> framesSinceKey = new ArrayDeque<>();
    AVStream outputVideoStream;
    OutputStream outputStream;
    int ptsInc;
    double timeBase;
    int filePts;
    boolean hasHadKey = false;
    private WriteCallback writeCallback;
    private SeekCallback seekCallback;
    BytePointer buffer;
    ByteArraySeekableBuffer seekableBuffer;
    String fileName;

    public FileOutput(FFMPEGSensorBase<FFMPEGConfigType> parentSensor) throws SensorHubException {
        super(OUTPUT_NAME, parentSensor);
        var helper = new SWEHelper();

        outputStruct = helper.createText()
                .name(OUTPUT_NAME)
                .label(OUTPUT_LABEL)
                .build();

        outputEncoding = helper.newTextEncoding();
    }

    @Override
    public void onDataBuffer(DataBufferRecord record) {
        synchronized (contextLock) {
            var data = record.getDataBuffer();
            AVPacket avPacket = av_packet_alloc();
            av_new_packet(avPacket, data.length);
            avPacket.data().put(data);
            boolean isKeyFrame = record.isKeyFrame();

            if (isKeyFrame) {
                hasHadKey = true;
                avPacket.flags(avPacket.flags() | avcodec.AV_PKT_FLAG_KEY);
            }

            synchronized (contextLock) {
                if (doFileWrite) {
                    if (outputContext != null) {
                        packetTiming(avPacket);
                        av_write_frame(outputContext, avPacket);
                        av_packet_free(avPacket);
                    } else {
                        logger.error("Cannot write to file; output context is null");
                    }
                } else if (hasHadKey) {
                    if (isKeyFrame) {
                        framesSinceKey.clear();
                    }
                    framesSinceKey.add(avPacket);
                }
            }
        }
    }

    public void openFile(OutputStream outputStream, String fileName) throws IOException {
        avutil.av_log_set_level(avutil.AV_LOG_VERBOSE);
        try {
            if (doFileWrite) {
                throw new IOException("Already writing to file " + this.fileName);
            }

            this.outputStream = outputStream;
            this.fileName = fileName;

            seekableBuffer = new ByteArraySeekableBuffer(8 * 1024 * 1024); // 8 MB initial size
            writeCallback = new WriteCallback(seekableBuffer).retainReference();
            seekCallback = new SeekCallback(seekableBuffer).retainReference();

            filePts = 0;
            int ret;

            //AVFormatContext inputContext = this.parentSensor.getProcessor().getAvFormatContext();
            AVStream inputStream = this.parentSensor.getProcessor().getAvStream();

            outputContext = new AVFormatContext(null);
            avformat_alloc_output_context2(outputContext, null, "mp4", null); // Assuming always mp4 output

            outputVideoStream = avformat.avformat_new_stream(outputContext, null);

            avcodec.avcodec_parameters_copy(outputVideoStream.codecpar(), inputStream.codecpar());

            // We're transcoding, need to override some of the copied values
            outputVideoStream.codecpar().codec_id(AV_CODEC_ID_H264);
            outputVideoStream.codecpar().codec_tag(0);

            if (inputStream.time_base().num() == 0 || inputStream.time_base().den() == 0) {
                outputVideoStream.time_base(new AVRational());
                outputVideoStream.time_base().num(1);
                outputVideoStream.time_base().den(90000);
            } else {
                outputVideoStream.time_base(new AVRational(inputStream.time_base()));
            }

            if (inputStream.avg_frame_rate().num() == 0 || inputStream.avg_frame_rate().den() == 0) {
                outputVideoStream.avg_frame_rate(new AVRational());
                outputVideoStream.avg_frame_rate().num(30);
                outputVideoStream.avg_frame_rate().den(1);
            } else {
                outputVideoStream.avg_frame_rate(new AVRational(inputStream.avg_frame_rate()));
            }
            
            outputVideoStream.start_time(0);
            //outputVideoStream.position(0);
            outputVideoStream.r_frame_rate(new AVRational(inputStream.r_frame_rate()));

            ptsInc = outputVideoStream.time_base().den() / (outputVideoStream.avg_frame_rate().num());
            //timeBase = (double) inputStream.time_base().num() / inputStream.time_base().den();

            this.buffer = new BytePointer(avutil.av_malloc(4096)).capacity(4096);
            var avio = avio_alloc_context(buffer, 4096, 1, null, null, writeCallback, seekCallback);
            outputContext.pb(avio);
            outputContext.url(new BytePointer(av_malloc(1)).putString(""));

            outputContext.start_time(0);
            outputContext.position(0);
            //outputContext.pb().position(0);

            AVDictionary options = null;

            if ((ret = avformat.avformat_write_header(outputContext, options)) < 0) {
                logFFmpeg(ret);
                throw new IOException("Could not write header to file.");
            }

            while (!framesSinceKey.isEmpty()) {
                AVPacket packet = framesSinceKey.pop();
                packetTiming(packet);
                av_write_frame(outputContext, packet);
                av_packet_free(packet);
            }

            doFileWrite = true;
        } catch (Exception e) {
            throw new IOException("Could not open output file " + fileName, e);
        }
    }

    private void logFFmpeg(int retCode) {
        BytePointer buf = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
        av_strerror(retCode, buf, buf.capacity());
        logger.error("FFmpeg returned error code {}: {}", retCode, buf.getString());
    }

    private void packetTiming(AVPacket avPacket) {
        filePts += ptsInc;
        avPacket.pts(filePts);
        avPacket.dts(filePts);
        avPacket.duration(ptsInc);
    }

    private static AVDictionary hlsOptions() {
        AVDictionary options = new AVDictionary();
        avutil.av_dict_set(options, "hls_list_size", "5", 0);
        avutil.av_dict_set(options, "hls_flags", "delete_segments", 0);
        avutil.av_dict_set(options, "hls_segment_type", "fmp4", 0);
        avutil.av_dict_set(options, "hls_time", "4", 0);
        avutil.av_dict_set(options, "movflags", "empty_moov+default_base_moof", 0);
        return options;
    }

    public void closeFile(boolean doFilenameOutput) throws IOException {
        if (doFileWrite) {
            doFileWrite = false;
            hasHadKey = false;
            try {
                synchronized (contextLock) {
                    avformat.av_write_trailer(outputContext);
                    avio_flush(outputContext.pb());


                    outputStream.write(seekableBuffer.getData());
                    outputStream.flush();
                    outputStream.close();

                    avio_close(outputContext.pb());
                    outputContext.pb(null);

                    writeCallback.free();
                    seekCallback.free();

                    avformat_free_context(outputContext);
                    outputContext = null;
                }
                if (doFilenameOutput) {
                    this.outputStruct.renewDataBlock();
                    this.outputStruct.getData().setStringValue(this.fileName);
                    this.eventHandler.publish(new DataEvent(System.currentTimeMillis(), this, outputStruct.getData().clone()));
                }
            } catch (Exception e) {
                throw new IOException("Could not close output file " + this.fileName + ".", e);
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


