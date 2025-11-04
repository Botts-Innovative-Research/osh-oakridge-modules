package com.botts.impl.service.oscar.video;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;

import static org.bytedeco.ffmpeg.global.avcodec.av_new_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;

// TODO Should this be moved to the ffmpeg driver package?
public class VideoKeyframeDecimator implements DataBufferListener {
    final String outputFileName;

    boolean isWriting = true;

    final int totalKeyframe;
    long duration;
    long keyFrameDuration;
    long currentDecFrame = 0;

    AVFormatContext avFormatContext;
    AVRational timeBase;

    Runnable closeCallback;

    VideoKeyframeDecimator(String outputFileName, int totalKeyframe, AVStream otherStream) {
        this.outputFileName = outputFileName;
        this.totalKeyframe = totalKeyframe;

        openOutputFile(outputFileName, otherStream);

        keyFrameDuration = duration / totalKeyframe;
    }

    public void openOutputFile(String fileName, AVStream otherStream) {
        avFormatContext = new AVFormatContext(null);
        avformat.avformat_alloc_output_context2(avFormatContext, null, null, fileName);

        var avStream = avformat.avformat_new_stream(avFormatContext, null);
        avcodec.avcodec_parameters_copy(avStream.codecpar(), otherStream.codecpar());
        timeBase = otherStream.time_base();
        duration = otherStream.duration();

        if ((avFormatContext.oformat().flags() & AVFMT_NOFILE) == 0) {
            AVIOContext avioContext = new AVIOContext(null);
            if (avio_open(avioContext, fileName, AVIO_FLAG_WRITE) < 0) {
                throw new RuntimeException("Could not open file.");
            }
            avFormatContext.pb(avioContext);
        }

        avformat.avformat_write_header(avFormatContext, (AVDictionary) null);
    }


    @Override
    public void onDataBuffer(DataBufferRecord record) {
        long timestamp = (long)(record.getPresentationTimestamp() * timeBase.den() / timeBase.num());

        if (record.isKeyFrame() && timestamp > keyFrameDuration * currentDecFrame) {
            currentDecFrame++;

            byte[] data = record.getDataBuffer();
            AVPacket avPacket = av_packet_alloc();

            av_new_packet(avPacket, data.length);
            avPacket.data().put(data);
            avPacket.duration(keyFrameDuration);
            avPacket.time_base(timeBase);

            avformat.av_write_frame(avFormatContext, avPacket);

            if (currentDecFrame >= totalKeyframe) {
                isWriting = false;
                avformat.av_write_trailer(avFormatContext);
                avformat.avio_close(avFormatContext.pb());

                if (closeCallback != null) {
                    closeCallback.run();
                }
            }
        }
    }

    @Override
    public boolean isWriting() {
        return isWriting;
    }

    public void setFileCloseCallback(Runnable closeCallback) {
        this.closeCallback = closeCallback;
    }
}
