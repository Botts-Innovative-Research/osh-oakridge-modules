package com.botts.impl.service.oscar.video;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;

// TODO Should this be moved to the ffmpeg driver package?
public class VideoKeyframeDecimator implements DataBufferListener {
    final String outputFileName;

    boolean isWriting = true;

    final int totalKeyframe;
    static final int frameTimeout = 5;
    long duration;
    long keyFrameDuration;
    long currentDecFrame = 0;

    AVFormatContext avFormatContext;
    AVStream avStream;
    AVRational timeBase;

    Runnable closeCallback;

    final Object lock = new Object();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> timeoutTask;

    VideoKeyframeDecimator(String outputFileName, int totalKeyframe, AVStream otherStream) {
        this.outputFileName = outputFileName;
        this.totalKeyframe = totalKeyframe;

        openOutputFile(outputFileName, otherStream);

        keyFrameDuration = duration / totalKeyframe;
    }

    public void openOutputFile(String fileName, AVStream otherStream) {
        avFormatContext = new AVFormatContext(null);
        avformat.avformat_alloc_output_context2(avFormatContext, null, "mp4", null);

        avStream = avformat.avformat_new_stream(avFormatContext, null);
        avcodec.avcodec_parameters_copy(avStream.codecpar(), otherStream.codecpar());
        avStream.codecpar().format(otherStream.codecpar().format());
        timeBase = otherStream.time_base();
        duration = otherStream.duration();
        avStream.time_base(timeBase);
        avStream.duration(duration);

        if ((avFormatContext.oformat().flags() & AVFMT_NOFILE) == 0) {
            AVIOContext avioContext = new AVIOContext(null);
            if (avio_open(avioContext, fileName, AVIO_FLAG_WRITE) < 0) {
                throw new RuntimeException("Could not open file.");
            }
            avFormatContext.pb(avioContext);
        }

        avformat.avformat_write_header(avFormatContext, (AVDictionary) null);
    }

    private void resetFrameTimeout() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }

        //timeoutTask = scheduler.schedule(this::closeFile, frameTimeout, TimeUnit.SECONDS);
    }

    @Override
    public void onDataBuffer(DataBufferRecord record) {
        synchronized (lock) {
            if (!isWriting)
                return;

            resetFrameTimeout();
            long timestamp = (long) (record.getPresentationTimestamp() * timeBase.den() / timeBase.num());


            if (record.isKeyFrame() && timestamp >= keyFrameDuration * currentDecFrame) {

                byte[] data = record.getDataBuffer();
                AVPacket avPacket = av_packet_alloc();

                av_new_packet(avPacket, data.length);
                avPacket.data().put(data);
                avPacket.stream_index(avStream.index());
                avPacket.duration(keyFrameDuration);
                avPacket.pts(keyFrameDuration * currentDecFrame);
                avPacket.dts(keyFrameDuration * currentDecFrame);
                avPacket.time_base(timeBase);
                avPacket.flags(avPacket.flags() | avcodec.AV_PKT_FLAG_KEY);

                avformat.av_interleaved_write_frame(avFormatContext, avPacket);

                av_packet_unref(avPacket);

                currentDecFrame++;
                if (currentDecFrame >= totalKeyframe) {
                    closeFile();
                }
            }
        }
    }

    public void closeFile() {
        synchronized (lock) {
            if (isWriting) {
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
