package org.sensorhub.impl.sensor.ffmpeg.outputs;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
import static org.bytedeco.ffmpeg.global.avutil.AV_ERROR_MAX_STRING_SIZE;
import static org.bytedeco.ffmpeg.global.avutil.av_strerror;

public class FileOutput implements DataBufferListener {

    private static final Logger logger = LoggerFactory.getLogger(FileOutput.class);
    private volatile boolean doFileWrite = false;
    private String outputFile = "";
    private AVFormatContext outputContext;
    private final Object contextLock = new Object();
    private ArrayDeque<AVPacket> framesSinceKey = new ArrayDeque<>();
    AVStream outputVideoStream;
    int ptsInc;
    double timeBase;
    int filePts;
    boolean hasHadKey = false;

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

            if (doFileWrite) {
                if (outputContext != null) {
                    packetTiming(avPacket);
                    av_write_frame(outputContext, avPacket);
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

    public void openFile(String outputFile, AVFormatContext inputFormat, int videoStreamId) throws IOException {
        try {
            if (doFileWrite) {
                throw new IOException("Already writing to file " + this.outputFile);
            }

            /*
            if (!framesSinceKey.isEmpty()) {
                filePts = framesSinceKey.peek().pts();
            } else {
                filePts = 0;
            }

             */

            filePts = 0;
            int ret;

            outputContext = avformat.avformat_alloc_context();
            this.outputFile = outputFile;
            avformat.avformat_alloc_output_context2(outputContext, null, null, outputFile);

            //AVCodec videoCodec = new AVCodec(inputFormat.video_codec());
            //videoCodec.type(AVMEDIA_TYPE_VIDEO);

            outputVideoStream = avformat.avformat_new_stream(outputContext, null);
            AVStream inputStream = inputFormat.streams(videoStreamId);

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

            // TODO Make sure this is correct
            if ((outputContext.oformat().flags() & AVFMT_NOFILE) == 0) {
                AVIOContext avioContext = new AVIOContext(null);
                if (avio_open(avioContext, outputFile, AVIO_FLAG_WRITE) < 0) {
                    throw new IOException("Could not open file.");
                }
                outputContext.pb(avioContext);
            }

            outputContext.start_time(0);
            outputContext.position(0);
            //outputContext.pb().position(0);

            AVDictionary options = null;
            if (outputFile.endsWith(".m3u8"))
                options = hlsOptions();

            if ((ret = avformat.avformat_write_header(outputContext, options)) < 0) {
                logFFmpeg(ret);
                throw new IOException("Could not write header to file.");
            }

            while (!framesSinceKey.isEmpty()) {
                AVPacket packet = framesSinceKey.pop();
                packetTiming(packet);
                av_write_frame(outputContext, packet);
                av_packet_unref(packet);
            }

            doFileWrite = true;
        } catch (Exception e) {
            throw new IOException("Could not open output file " + outputFile, e);
        }
    }

    private void logFFmpeg(int retCode) {
        BytePointer buf = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
        av_strerror(retCode, buf, buf.capacity());
        logger.warn("FFmpeg returned error code {}: {}", retCode, buf.getString());
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

    public void closeFile() throws IOException {
        if (doFileWrite) {
            doFileWrite = false;
            hasHadKey = false;
            try {
                synchronized (contextLock) {
                    avformat.av_write_trailer(outputContext);
                    avio_close(outputContext.pb());
                    avformat.avformat_free_context(outputContext);
                    outputContext = null;
                }
            } catch (Exception e) {
                throw new IOException("Could not close output file " + outputFile + ".", e);
            }
        }
    }
}
