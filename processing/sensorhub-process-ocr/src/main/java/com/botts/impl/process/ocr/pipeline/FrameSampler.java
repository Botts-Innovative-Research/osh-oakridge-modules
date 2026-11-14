/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.pipeline;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

/**
 * Memory-safe, single-threaded video frame sampler for occupancy MP4 clips.
 *
 * <p>Uses direct FFmpeg {@link AVFormatContext}/{@link AVCodecContext} demuxing with:</p>
 * <ul>
 *   <li>{@code decoderContext.max_pixels(16_777_216L)} before {@code avcodec_open2}.</li>
 *   <li>32-byte SIMD-aligned {@code av_frame_get_buffer(bgr, 32)} allocations and row-by-row
 *       {@code linesize(0)} copies (eliminating unpadded {@code av_image_alloc} buffer overruns).</li>
 *   <li>Automatic downscaling of >1080p frames (e.g. 4K {@code 3840x2160} -> {@code 1920x1080})
 *       inside {@code sws_scale} to cap per-frame heap footprint to ~6 MiB.</li>
 *   <li>Deterministic reverse-order native resource cleanup in {@code finally}.</li>
 * </ul>
 */
public class FrameSampler {

    private static final Logger logger = LoggerFactory.getLogger(FrameSampler.class);

    static {
        // Silence noisy FFmpeg/swscaler deprecation warnings (e.g. MJPEG yuvj420p range notices)
        av_log_set_level(AV_LOG_ERROR);
    }

    static final long MIN_VALID_FILE_BYTES = 512L;
    static final long MAX_DECODER_PIXELS = 4096L * 4096L;
    static final int MAX_DIMENSION = 4096;
    static final int MAX_OUTPUT_WIDTH = 1920;
    static final int MAX_OUTPUT_HEIGHT = 1080;
    static final int MAX_DECODED_FRAMES = 36_000; // ~10 min at 60 fps

    public record SampledFrame(int index, double timestampSeconds, BufferedImage image) {}

    /**
     * Samples up to {@code maxFrames} evenly spaced frames across {@code [10%, 90%]} of the video.
     * Returns an empty list (never throws) when the clip is still being finalized, unreadable,
     * truncated, or contains no valid video frames.
     */
    public List<SampledFrame> sampleFrames(Path videoFile, int maxFrames) {
        if (videoFile == null || maxFrames <= 0)
            return List.of();

        Path normalized = videoFile.toAbsolutePath().normalize();
        try {
            if (!Files.isRegularFile(normalized) || Files.size(normalized) < MIN_VALID_FILE_BYTES)
                return List.of();
        } catch (IOException e) {
            return List.of();
        }

        try {
            List<Double> ptsTimeline = scanTimeline(normalized);
            if (ptsTimeline.isEmpty())
                return List.of();

            int[] selectedOrdinals = selectOrdinals(ptsTimeline.size(), maxFrames);
            return decodeSelectedOrdinals(normalized, selectedOrdinals);
        } catch (Exception e) {
            logger.debug("Unable to sample frames from {}: {}", normalized, e.getMessage());
            return List.of();
        }
    }

    static int[] selectOrdinals(int totalFrames, int maxFrames) {
        if (totalFrames <= 0 || maxFrames <= 0)
            return new int[0];
        if (totalFrames <= maxFrames) {
            int[] all = new int[totalFrames];
            for (int i = 0; i < totalFrames; i++)
                all[i] = i;
            return all;
        }
        int count = Math.min(totalFrames, maxFrames);
        int[] selected = new int[count];
        if (count == 1) {
            selected[0] = totalFrames / 2;
            return selected;
        }
        // Sample across [10%, 90%] of the occupancy clip so entry/exit empty pavement is skipped
        double startIdx = (totalFrames - 1) * 0.10;
        double endIdx = (totalFrames - 1) * 0.90;
        int prev = -1;
        for (int i = 0; i < count; i++) {
            double frac = i / (double) (count - 1);
            int candidate = (int) Math.round(startIdx + (endIdx - startIdx) * frac);
            candidate = Math.max(prev + 1, Math.min(totalFrames - (count - i), candidate));
            selected[i] = candidate;
            prev = candidate;
        }
        return selected;
    }

    private List<Double> scanTimeline(Path source) throws IOException {
        List<Double> timeline = new ArrayList<>();
        decode(source, (ordinal, seconds, frame) -> {
            timeline.add(seconds);
            return timeline.size() < MAX_DECODED_FRAMES;
        });
        return timeline;
    }

    private List<SampledFrame> decodeSelectedOrdinals(Path source, int[] selectedOrdinals) throws IOException {
        List<SampledFrame> out = new ArrayList<>(selectedOrdinals.length);
        int lastTarget = selectedOrdinals[selectedOrdinals.length - 1];
        decode(source, (ordinal, seconds, frame) -> {
            int pos = Arrays.binarySearch(selectedOrdinals, ordinal);
            if (pos >= 0) {
                BufferedImage img = convertToBgrImage(frame);
                if (img != null) {
                    out.add(new SampledFrame(pos, seconds, img));
                }
            }
            return ordinal < lastTarget;
        });
        return out;
    }

    @FunctionalInterface
    private interface FrameVisitor {
        /** Returns {@code true} to continue decoding, {@code false} to stop early. */
        boolean visit(int ordinal, double seconds, AVFrame frame) throws IOException;
    }

    private void decode(Path source, FrameVisitor visitor) throws IOException {
        AVFormatContext format = new AVFormatContext(null);
        AVCodecContext decoderContext = null;
        AVPacket packet = null;
        AVFrame frame = null;

        try {
            int rc = avformat_open_input(format, source.toString(), null, null);
            if (rc < 0)
                throw new IOException("avformat_open_input failed (" + rc + ")");

            rc = avformat_find_stream_info(format, (PointerPointer<?>) null);
            if (rc < 0)
                throw new IOException("avformat_find_stream_info failed (" + rc + ")");

            int streamIndex = findVideoStream(format);
            if (streamIndex < 0)
                throw new IOException("No video stream found");

            AVStream stream = format.streams(streamIndex);
            AVCodec decoder = avcodec_find_decoder(stream.codecpar().codec_id());
            if (decoder == null || decoder.isNull())
                throw new IOException("Unsupported video codec");

            decoderContext = avcodec_alloc_context3(decoder);
            if (decoderContext == null || decoderContext.isNull()
                    || avcodec_parameters_to_context(decoderContext, stream.codecpar()) < 0)
                throw new IOException("Failed to initialize AVCodecContext");

            decoderContext.max_pixels(MAX_DECODER_PIXELS);
            decoderContext.thread_count(1);
            if (decoderContext.width() > MAX_DIMENSION || decoderContext.height() > MAX_DIMENSION)
                throw new IOException("Video dimensions exceed safety limit");

            if (avcodec_open2(decoderContext, decoder, (PointerPointer<?>) null) < 0)
                throw new IOException("avcodec_open2 failed");

            packet = av_packet_alloc();
            frame = av_frame_alloc();
            if (packet == null || packet.isNull() || frame == null || frame.isNull())
                throw new IOException("Failed to allocate packet/frame");

            int[] ordinalRef = {0};
            boolean[] keepGoing = {true};

            while (keepGoing[0] && av_read_frame(format, packet) >= 0) {
                try {
                    if (packet.stream_index() != streamIndex)
                        continue;
                    int sent = avcodec_send_packet(decoderContext, packet);
                    if (sent == AVERROR_EAGAIN()) {
                        keepGoing[0] = drainFrames(decoderContext, frame, stream, ordinalRef, visitor);
                        avcodec_send_packet(decoderContext, packet);
                    } else if (sent < 0) {
                        // Corrupt packet in clip: skip packet gracefully instead of crashing
                        continue;
                    }
                    if (keepGoing[0]) {
                        keepGoing[0] = drainFrames(decoderContext, frame, stream, ordinalRef, visitor);
                    }
                } finally {
                    av_packet_unref(packet);
                }
            }

            if (keepGoing[0]) {
                int flush = avcodec_send_packet(decoderContext, null);
                if (flush >= 0 || flush == AVERROR_EOF()) {
                    drainFrames(decoderContext, frame, stream, ordinalRef, visitor);
                }
            }
        } finally {
            if (packet != null && !packet.isNull())
                av_packet_free(packet);
            if (frame != null && !frame.isNull())
                av_frame_free(frame);
            if (decoderContext != null && !decoderContext.isNull())
                avcodec_free_context(decoderContext);
            if (format != null && !format.isNull())
                avformat_close_input(format);
        }
    }

    private static boolean drainFrames(AVCodecContext decoderContext,
                                       AVFrame frame,
                                       AVStream stream,
                                       int[] ordinalRef,
                                       FrameVisitor visitor) throws IOException {
        while (true) {
            int rc = avcodec_receive_frame(decoderContext, frame);
            if (rc == AVERROR_EAGAIN() || rc == AVERROR_EOF() || rc < 0)
                return true;
            try {
                int w = frame.width();
                int h = frame.height();
                if (w <= 0 || h <= 0 || w > MAX_DIMENSION || h > MAX_DIMENSION
                        || frame.data(0) == null || frame.data(0).isNull()) {
                    continue;
                }
                int ordinal = ordinalRef[0]++;
                if (ordinal >= MAX_DECODED_FRAMES)
                    return false;
                long pts = frame.best_effort_timestamp();
                if (pts == AV_NOPTS_VALUE)
                    pts = frame.pts();
                double seconds = ptsToSeconds(pts, stream.time_base(), ordinal);
                if (!visitor.visit(ordinal, seconds, frame))
                    return false;
            } finally {
                av_frame_unref(frame);
            }
        }
    }

    private static BufferedImage convertToBgrImage(AVFrame source) throws IOException {
        int srcW = source.width();
        int srcH = source.height();
        if (srcW <= 0 || srcH <= 0 || (long) srcW * srcH > MAX_DECODER_PIXELS)
            return null;

        double scale = Math.min(1.0, Math.min(
                MAX_OUTPUT_WIDTH / (double) srcW,
                MAX_OUTPUT_HEIGHT / (double) srcH));
        int dstW = Math.max(2, ((int) Math.round(srcW * scale)) & ~1);
        int dstH = Math.max(2, ((int) Math.round(srcH * scale)) & ~1);

        AVFrame bgr = av_frame_alloc();
        SwsContext scaler = null;
        try {
            if (bgr == null || bgr.isNull())
                return null;
            bgr.format(AV_PIX_FMT_BGR24);
            bgr.width(dstW);
            bgr.height(dstH);
            if (av_frame_get_buffer(bgr, 32) < 0)
                return null;

            scaler = sws_getContext(
                    srcW, srcH, source.format(),
                    dstW, dstH, AV_PIX_FMT_BGR24,
                    SWS_BILINEAR, null, null, (double[]) null);
            if (scaler == null || scaler.isNull())
                return null;

            int scaled = sws_scale(scaler, source.data(), source.linesize(), 0, srcH, bgr.data(), bgr.linesize());
            if (scaled != dstH)
                return null;

            BufferedImage image = new BufferedImage(dstW, dstH, BufferedImage.TYPE_3BYTE_BGR);
            byte[] dst = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            int rowBytes = Math.multiplyExact(dstW, 3);
            int stride = bgr.linesize(0);
            for (int row = 0; row < dstH; row++) {
                bgr.data(0).position((long) row * stride).get(dst, row * rowBytes, rowBytes);
            }
            bgr.data(0).position(0);
            return image;
        } finally {
            if (scaler != null && !scaler.isNull())
                sws_freeContext(scaler);
            if (bgr != null && !bgr.isNull())
                av_frame_free(bgr);
        }
    }

    private static int findVideoStream(AVFormatContext format) {
        int nbStreams = format.nb_streams();
        for (int i = 0; i < nbStreams; i++) {
            AVStream s = format.streams(i);
            if (s != null && s.codecpar() != null && s.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO)
                return i;
        }
        return -1;
    }

    private static double ptsToSeconds(long pts, AVRational tb, int fallbackOrdinal) {
        if (pts == AV_NOPTS_VALUE || tb == null || tb.num() <= 0 || tb.den() <= 0)
            return fallbackOrdinal / 15.0;
        double s = pts * (tb.num() / (double) tb.den());
        return Double.isFinite(s) && s >= 0.0 ? s : fallbackOrdinal / 15.0;
    }
}
