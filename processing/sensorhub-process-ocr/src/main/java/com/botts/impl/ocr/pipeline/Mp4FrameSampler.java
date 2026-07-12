package com.botts.impl.ocr.pipeline;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

/**
 * Extracts N decoded keyframes, evenly spread across a recorded occupancy MP4.
 * The demux reuses {@link MpegTsProcessor} (the same reader VideoRetention
 * uses on these files); decoding uses the stream's codec parameters so
 * MP4/AVCC packets carry their SPS/PPS extradata.
 *
 * The MP4 trailer is written asynchronously when the occupancy ends, so the
 * first open attempts may race the file finalize — hence the retry backoff.
 */
public class Mp4FrameSampler {

    public static class Frame {
        public final BufferedImage image;
        public final double ptsSeconds;

        Frame(BufferedImage image, double ptsSeconds) {
            this.image = image;
            this.ptsSeconds = ptsSeconds;
        }
    }

    static final long[] RETRY_BACKOFF_MS = {500, 1000, 2000, 4000, 8000};
    static final int MAX_COLLECTED_KEYFRAMES = 512;

    private static final Logger logger = LoggerFactory.getLogger(Mp4FrameSampler.class);

    private final long[] retryBackoffMs;

    public Mp4FrameSampler() {
        this(RETRY_BACKOFF_MS);
    }

    Mp4FrameSampler(long[] retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public List<Frame> sample(String absolutePath, int maxFrames) throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (int attempt = 0; attempt <= retryBackoffMs.length; attempt++) {
            if (attempt > 0)
                Thread.sleep(retryBackoffMs[attempt - 1]);
            try {
                return sampleOnce(absolutePath, maxFrames);
            } catch (IOException e) {
                lastFailure = e;
                logger.debug("Attempt {} to sample {} failed: {}", attempt + 1, absolutePath, e.getMessage());
            }
        }
        throw new IOException("Could not sample frames from " + absolutePath + " after retries", lastFailure);
    }

    private List<Frame> sampleOnce(String absolutePath, int maxFrames) throws IOException, InterruptedException {
        MpegTsProcessor demux = new MpegTsProcessor(absolutePath);
        demux.setInjectExtradata(false);

        List<DataBufferRecord> keyframes = new ArrayList<>();
        try {
            if (!demux.openStream())
                throw new IOException("Failed to open " + absolutePath);

            demux.queryEmbeddedStreams();
            AVStream stream = demux.getAvStream();
            if (stream == null)
                throw new IOException("No video stream in " + absolutePath);

            demux.addVideoDataBufferListener(new DataBufferListener() {
                @Override
                public void onDataBuffer(DataBufferRecord record) {
                    if (record.isKeyFrame() && keyframes.size() < MAX_COLLECTED_KEYFRAMES)
                        keyframes.add(record);
                }

                @Override
                public boolean isWriting() {
                    return true;
                }

                @Override
                public String getName() {
                    return "OCR-FRAME-SAMPLER";
                }
            });

            demux.processStream();
            demux.join();

            if (keyframes.isEmpty())
                throw new IOException("No keyframes demuxed from " + absolutePath);

            // decode before closing: the AVStream's codec parameters are freed with the context
            return decode(stream, pickEvenlySpread(keyframes, maxFrames));
        } finally {
            try {
                demux.stopProcessingStream();
                demux.closeStream();
            } catch (Exception e) {
                logger.warn("Failed to close demuxer for {}", absolutePath, e);
            }
        }
    }

    static List<DataBufferRecord> pickEvenlySpread(List<DataBufferRecord> records, int maxFrames) {
        if (records.size() <= maxFrames)
            return records;
        List<DataBufferRecord> picked = new ArrayList<>(maxFrames);
        double step = records.size() / (double) maxFrames;
        for (int i = 0; i < maxFrames; i++)
            picked.add(records.get((int) Math.floor(i * step)));
        return picked;
    }

    private List<Frame> decode(AVStream stream, List<DataBufferRecord> records) throws IOException {
        List<Frame> frames = new ArrayList<>();

        AVCodec codec = avcodec_find_decoder(stream.codecpar().codec_id());
        if (codec == null)
            throw new IOException("No decoder for codec id " + stream.codecpar().codec_id());

        AVCodecContext ctx = avcodec_alloc_context3(codec);
        AVPacket packet = av_packet_alloc();
        AVFrame decoded = av_frame_alloc();
        AVFrame bgr = null;
        SwsContext sws = null;

        try {
            if (avcodec_parameters_to_context(ctx, stream.codecpar()) < 0)
                throw new IOException("Failed to copy codec parameters");
            if (avcodec_open2(ctx, codec, (AVDictionary) null) < 0)
                throw new IOException("Failed to open decoder");

            ArrayDeque<Double> pendingPts = new ArrayDeque<>();
            for (DataBufferRecord record : records) {
                byte[] data = record.getDataBuffer();
                if (av_new_packet(packet, data.length) < 0)
                    throw new IOException("Failed to allocate packet");
                packet.data().put(data);

                pendingPts.add(record.getPresentationTimestamp());
                int sendResult = avcodec_send_packet(ctx, packet);
                av_packet_unref(packet);
                if (sendResult < 0) {
                    pendingPts.pollLast();
                    logger.debug("Decoder rejected a keyframe packet ({})", sendResult);
                    continue;
                }

                while (avcodec_receive_frame(ctx, decoded) == 0) {
                    if (sws == null) {
                        sws = sws_getContext(decoded.width(), decoded.height(), decoded.format(),
                                decoded.width(), decoded.height(), AV_PIX_FMT_BGR24,
                                SWS_BILINEAR, null, null, (double[]) null);
                        bgr = av_frame_alloc();
                        av_image_alloc(bgr.data(), bgr.linesize(),
                                decoded.width(), decoded.height(), AV_PIX_FMT_BGR24, 1);
                    }
                    Double pts = pendingPts.poll();
                    frames.add(toFrame(decoded, bgr, sws, pts != null ? pts : 0));
                }
            }

            // drain the decoder's reorder buffer
            avcodec_send_packet(ctx, (AVPacket) null);
            while (sws != null && avcodec_receive_frame(ctx, decoded) == 0) {
                Double pts = pendingPts.poll();
                frames.add(toFrame(decoded, bgr, sws, pts != null ? pts : 0));
            }
        } finally {
            if (sws != null)
                sws_freeContext(sws);
            if (bgr != null) {
                av_freep(bgr.data());
                av_frame_free(bgr);
            }
            av_frame_free(decoded);
            av_packet_free(packet);
            avcodec_free_context(ctx);
        }

        if (frames.isEmpty())
            throw new IOException("Decoded no frames");
        return frames;
    }

    private Frame toFrame(AVFrame decoded, AVFrame bgr, SwsContext sws, double ptsSeconds) {
        int width = decoded.width();
        int height = decoded.height();
        sws_scale(sws, decoded.data(), decoded.linesize(), 0, height, bgr.data(), bgr.linesize());

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        bgr.data(0).get(pixels, 0, width * height * 3);
        return new Frame(image, ptsSeconds);
    }
}
