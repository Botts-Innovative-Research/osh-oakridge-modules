package com.botts.impl.service.oscar.media;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.PointerPointer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

/** Decodes a source clip twice and emits exactly five independent baseline sRGB JPEG images. */
public final class JpegFrameExtractor {
    private static final int MAX_DECODED_FRAMES = 1_000_000;
    private static final double[] TARGETS = {0.0, 0.25, 0.50, 0.75, 1.0};

    private final int jpegQuality;

    public JpegFrameExtractor(int jpegQuality) {
        if (jpegQuality < 1 || jpegQuality > 100)
            throw new IllegalArgumentException("JPEG quality must be between 1 and 100");
        this.jpegQuality = jpegQuality;
    }

    int jpegQuality() {
        return jpegQuality;
    }

    public ExtractionResult extract(Path source) throws IOException {
        Path normalized = source.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized) || Files.size(normalized) <= 0)
            throw new IOException("Source video is missing or empty: " + normalized);

        List<FramePoint> timeline = new ArrayList<>();
        StreamMetadata metadata = decode(normalized, (ordinal, pts, seconds, frame) -> {
            if (timeline.size() >= MAX_DECODED_FRAMES)
                throw new IOException("Source exceeds the retained-media decoded-frame safety limit");
            timeline.add(new FramePoint(ordinal, pts, seconds));
        });

        if (timeline.size() < MediaManifest.RETAINED_FRAME_COUNT)
            throw new IOException("Source contains fewer than five distinct decoded frames");

        long[] selectedOrdinals = selectDistinctOrdinals(timeline);
        CapturedFrame[] captured = new CapturedFrame[MediaManifest.RETAINED_FRAME_COUNT];
        decode(normalized, (ordinal, pts, seconds, frame) -> {
            int selectedIndex = Arrays.binarySearch(selectedOrdinals, ordinal);
            if (selectedIndex >= 0)
                captured[selectedIndex] = capture(frame, pts, seconds);
        });

        List<RetainedFrame> frames = new ArrayList<>(MediaManifest.RETAINED_FRAME_COUNT);
        for (int i = 0; i < captured.length; i++) {
            CapturedFrame frame = captured[i];
            if (frame == null)
                throw new IOException("Decoder did not reproduce selected frame " + selectedOrdinals[i]);
            frames.add(new RetainedFrame(position(i), frame.jpeg(), frame.width(), frame.height(),
                    frame.sourcePts(), frame.sourcePtsSeconds(), selectedOrdinals[i]));
        }

        long durationMicros = metadata.durationMicros();
        if (durationMicros <= 0) {
            double decodedDuration = Math.max(0.0,
                    timeline.get(timeline.size() - 1).seconds() - timeline.get(0).seconds());
            durationMicros = Math.round(decodedDuration * 1_000_000.0);
        }
        return new ExtractionResult(metadata.codec(), durationMicros, List.copyOf(frames));
    }

    static long[] selectDistinctOrdinals(List<FramePoint> timeline) {
        if (timeline.size() < MediaManifest.RETAINED_FRAME_COUNT)
            throw new IllegalArgumentException("At least five decoded frames are required");

        long[] result = new long[MediaManifest.RETAINED_FRAME_COUNT];
        result[0] = timeline.get(0).ordinal();
        result[result.length - 1] = timeline.get(timeline.size() - 1).ordinal();
        double first = timeline.get(0).seconds();
        double last = timeline.get(timeline.size() - 1).seconds();
        boolean usableTimeline = Double.isFinite(first) && Double.isFinite(last) && last > first;

        int previousIndex = 0;
        for (int slot = 1; slot < result.length - 1; slot++) {
            int minIndex = previousIndex + 1;
            int maxIndex = timeline.size() - (result.length - slot);
            double target = usableTimeline
                    ? first + (last - first) * TARGETS[slot]
                    : (timeline.size() - 1) * TARGETS[slot];
            int bestIndex = minIndex;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int index = minIndex; index <= maxIndex; index++) {
                double value = usableTimeline ? timeline.get(index).seconds() : index;
                double distance = Math.abs(value - target);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = index;
                }
            }
            result[slot] = timeline.get(bestIndex).ordinal();
            previousIndex = bestIndex;
        }
        return result;
    }

    public static JpegInfo verifyBaselineSrgbJpeg(Path jpeg) throws IOException {
        if (!Files.isRegularFile(jpeg, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(jpeg))
            throw new IOException("Retained JPEG is not a regular file");
        long encodedBytes = Files.readAttributes(jpeg,
                java.nio.file.attribute.BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS).size();
        if (encodedBytes <= 0 || encodedBytes > MediaManifest.MAX_JPEG_BYTES)
            throw new IOException("Retained JPEG exceeds the encoded-size safety limit");
        // A separate size check followed by readAllBytes permits a concurrent replacement to force
        // an unbounded allocation. Read at most one byte beyond the supported envelope instead.
        int limit = Math.toIntExact(MediaManifest.MAX_JPEG_BYTES + 1);
        byte[] bytes;
        try (var input = Files.newInputStream(jpeg, StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readNBytes(limit);
        }
        if (bytes.length != encodedBytes)
            throw new IOException("Retained JPEG changed while it was being verified");
        return verifyBaselineSrgbJpeg(bytes);
    }

    /** Verifies the exact byte array that will be served, avoiding verify-then-reopen races. */
    public static JpegInfo verifyBaselineSrgbJpeg(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > MediaManifest.MAX_JPEG_BYTES)
            throw new IOException("Retained JPEG exceeds the encoded-size safety limit");
        if (bytes.length < 4 || (bytes[0] & 0xff) != 0xff || (bytes[1] & 0xff) != 0xd8
                || (bytes[bytes.length - 2] & 0xff) != 0xff
                || (bytes[bytes.length - 1] & 0xff) != 0xd9)
            throw new IOException("Retained frame is not a complete JPEG image");
        JpegInfo encoded = baselineJpegInfo(bytes);
        if (encoded == null)
            throw new IOException("Retained frame is not a baseline (SOF0) JPEG image");
        validateDimensions(encoded.width(), encoded.height());

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        if (decoded == null || decoded.getWidth() <= 0 || decoded.getHeight() <= 0)
            throw new IOException("Retained JPEG cannot be decoded independently");
        validateDimensions(decoded.getWidth(), decoded.getHeight());
        if (decoded.getWidth() != encoded.width() || decoded.getHeight() != encoded.height())
            throw new IOException("Retained JPEG decoded dimensions do not match its SOF0 header");
        ColorSpace colorSpace = decoded.getColorModel().getColorSpace();
        if (decoded.getColorModel().hasAlpha() || !colorSpace.isCS_sRGB())
            throw new IOException("Retained JPEG is not opaque sRGB");
        return new JpegInfo(decoded.getWidth(), decoded.getHeight());
    }

    private StreamMetadata decode(Path source, DecodedFrameConsumer consumer) throws IOException {
        AVFormatContext format = new AVFormatContext(null);
        AVCodecContext decoderContext = null;
        AVPacket packet = null;
        AVFrame frame = null;
        try {
            int result = avformat_open_input(format, source.toString(), null, null);
            if (result < 0)
                throw new IOException("FFmpeg could not open source video (error " + result + ")");
            result = avformat_find_stream_info(format, (PointerPointer<?>) null);
            if (result < 0)
                throw new IOException("FFmpeg could not read source stream metadata (error " + result + ")");

            int streamIndex = findVideoStream(format);
            if (streamIndex < 0)
                throw new IOException("Source has no decodable video stream");
            AVStream stream = format.streams(streamIndex);
            AVCodec decoder = avcodec_find_decoder(stream.codecpar().codec_id());
            if (decoder == null || decoder.isNull())
                throw new IOException("No FFmpeg decoder is available for the source codec");
            decoderContext = avcodec_alloc_context3(decoder);
            if (decoderContext == null || decoderContext.isNull()
                    || avcodec_parameters_to_context(decoderContext, stream.codecpar()) < 0)
                throw new IOException("FFmpeg could not initialize the source decoder");
            // Bound decoder-owned frame allocations before opening the codec. A post-decode
            // dimension check is too late for an oversized malicious stream header.
            decoderContext.max_pixels(MediaManifest.MAX_JPEG_PIXELS);
            if (decoderContext.width() > 0 || decoderContext.height() > 0)
                validateDimensions(decoderContext.width(), decoderContext.height());
            if (avcodec_open2(decoderContext, decoder, (PointerPointer<?>) null) < 0)
                throw new IOException("FFmpeg could not initialize the source decoder");

            packet = av_packet_alloc();
            frame = av_frame_alloc();
            if (packet == null || packet.isNull() || frame == null || frame.isNull())
                throw new IOException("FFmpeg could not allocate decode buffers");

            DecodeCounter counter = new DecodeCounter();
            while (av_read_frame(format, packet) >= 0) {
                try {
                    if (packet.stream_index() != streamIndex)
                        continue;
                    sendAndDrain(decoderContext, packet, frame, stream, counter, consumer);
                } finally {
                    av_packet_unref(packet);
                }
            }
            int flush = avcodec_send_packet(decoderContext, null);
            if (flush >= 0 || flush == AVERROR_EOF())
                drain(decoderContext, frame, stream, counter, consumer);
            else if (!isAgain(flush))
                throw new IOException("FFmpeg decoder flush failed (error " + flush + ")");

            String codec = avcodec_get_name(stream.codecpar().codec_id()).getString();
            long durationMicros = durationMicros(format, stream);
            return new StreamMetadata(codec == null ? "unknown" : codec, durationMicros);
        } finally {
            if (packet != null)
                av_packet_free(packet);
            if (frame != null)
                av_frame_free(frame);
            if (decoderContext != null)
                avcodec_free_context(decoderContext);
            if (format != null && !format.isNull())
                avformat_close_input(format);
        }
    }

    private static void sendAndDrain(AVCodecContext decoderContext, AVPacket packet, AVFrame frame,
                                     AVStream stream, DecodeCounter counter,
                                     DecodedFrameConsumer consumer) throws IOException {
        int sent = avcodec_send_packet(decoderContext, packet);
        if (isAgain(sent)) {
            drain(decoderContext, frame, stream, counter, consumer);
            sent = avcodec_send_packet(decoderContext, packet);
        }
        if (sent < 0)
            throw new IOException("FFmpeg rejected a source video packet (error " + sent + ")");
        drain(decoderContext, frame, stream, counter, consumer);
    }

    private static void drain(AVCodecContext decoderContext, AVFrame frame, AVStream stream,
                              DecodeCounter counter, DecodedFrameConsumer consumer) throws IOException {
        while (true) {
            int received = avcodec_receive_frame(decoderContext, frame);
            if (isAgain(received) || received == AVERROR_EOF())
                return;
            if (received < 0)
                throw new IOException("FFmpeg failed to decode a source frame (error " + received + ")");
            try {
                if (frame.width() <= 0 || frame.height() <= 0
                        || frame.data(0) == null || frame.data(0).isNull())
                    continue;
                validateDimensions(frame.width(), frame.height());
                if (counter.value >= MAX_DECODED_FRAMES)
                    throw new IOException(
                            "Source exceeds the retained-media decoded-frame safety limit");
                long ordinal = counter.value++;
                long pts = frame.best_effort_timestamp();
                if (pts == AV_NOPTS_VALUE)
                    pts = frame.pts();
                if (pts == AV_NOPTS_VALUE)
                    pts = ordinal;
                double seconds = ptsSeconds(pts, stream, ordinal);
                consumer.accept(ordinal, pts, seconds, frame);
            } finally {
                av_frame_unref(frame);
            }
        }
    }

    private CapturedFrame capture(AVFrame source, long pts, double ptsSeconds) throws IOException {
        int width = source.width();
        int height = source.height();
        // Validate with long arithmetic before FFmpeg's BGR buffer or BufferedImage allocation.
        validateDimensions(width, height);
        AVFrame bgr = av_frame_alloc();
        SwsContext scaler = null;
        try {
            if (bgr == null || bgr.isNull())
                throw new IOException("FFmpeg could not allocate a retained-frame conversion buffer");
            bgr.format(AV_PIX_FMT_BGR24);
            bgr.width(width);
            bgr.height(height);
            int allocation = av_frame_get_buffer(bgr, 32);
            if (allocation < 0)
                throw new IOException("FFmpeg could not allocate a retained-frame image (error " + allocation + ")");
            scaler = sws_getContext(width, height, source.format(), width, height, AV_PIX_FMT_BGR24,
                    SWS_BICUBIC, null, null, (double[]) null);
            if (scaler == null || scaler.isNull())
                throw new IOException("FFmpeg could not create the retained-frame sRGB converter");
            int scaled = sws_scale(scaler, source.data(), source.linesize(), 0, height,
                    bgr.data(), bgr.linesize());
            if (scaled != height)
                throw new IOException("FFmpeg did not convert the complete retained frame");

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            byte[] destination = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            int rowBytes = Math.multiplyExact(width, 3);
            int stride = bgr.linesize(0);
            for (int row = 0; row < height; row++) {
                bgr.data(0).position((long) row * stride).get(destination, row * rowBytes, rowBytes);
            }
            bgr.data(0).position(0);
            return new CapturedFrame(encodeJpeg(image), width, height, pts, ptsSeconds);
        } finally {
            if (scaler != null)
                sws_freeContext(scaler);
            if (bgr != null)
                av_frame_free(bgr);
        }
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext())
            throw new IOException("No JPEG ImageIO writer is installed");
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(jpegQuality / 100.0f);
            if (parameters.canWriteProgressive())
                parameters.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
            writer.write(null, new IIOImage(image, null, null), parameters);
            imageOutput.flush();
            byte[] jpeg = output.toByteArray();
            if (jpeg.length == 0 || jpeg.length > MediaManifest.MAX_JPEG_BYTES)
                throw new IOException("Encoded retained JPEG exceeds the encoded-size safety limit");
            if (baselineJpegInfo(jpeg) == null)
                throw new IOException("JPEG encoder did not produce baseline output");
            return jpeg;
        } finally {
            writer.dispose();
        }
    }

    static void validateDimensions(int width, int height) throws IOException {
        if (width <= 0 || height <= 0)
            throw new IOException("Retained JPEG dimensions must be positive");
        if (width > MediaManifest.MAX_JPEG_DIMENSION
                || height > MediaManifest.MAX_JPEG_DIMENSION)
            throw new IOException("Retained JPEG dimensions exceed the safety limit");
        long pixels = (long) width * (long) height;
        if (pixels > MediaManifest.MAX_JPEG_PIXELS)
            throw new IOException("Retained JPEG pixel count exceeds the safety limit");
    }

    private static JpegInfo baselineJpegInfo(byte[] jpeg) {
        int offset = 2;
        while (offset + 1 < jpeg.length) {
            if ((jpeg[offset] & 0xff) != 0xff) {
                offset++;
                continue;
            }
            while (offset < jpeg.length && (jpeg[offset] & 0xff) == 0xff)
                offset++;
            if (offset >= jpeg.length)
                break;
            int marker = jpeg[offset++] & 0xff;
            if (marker == 0xc2 || marker == 0xda)
                return null;
            if (marker == 0xd8 || marker == 0xd9 || (marker >= 0xd0 && marker <= 0xd7) || marker == 0x01)
                continue;
            if (offset + 1 >= jpeg.length)
                return null;
            int length = ((jpeg[offset] & 0xff) << 8) | (jpeg[offset + 1] & 0xff);
            if (length < 2 || offset + length > jpeg.length)
                return null;
            if (marker == 0xc0) {
                if (length < 8)
                    return null;
                int height = ((jpeg[offset + 3] & 0xff) << 8) | (jpeg[offset + 4] & 0xff);
                int width = ((jpeg[offset + 5] & 0xff) << 8) | (jpeg[offset + 6] & 0xff);
                return new JpegInfo(width, height);
            }
            offset += length;
        }
        return null;
    }

    private static int findVideoStream(AVFormatContext format) {
        for (int index = 0; index < format.nb_streams(); index++) {
            if (format.streams(index).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO)
                return index;
        }
        return -1;
    }

    private static long durationMicros(AVFormatContext format, AVStream stream) {
        long streamDuration = stream.duration();
        if (streamDuration > 0 && streamDuration != AV_NOPTS_VALUE
                && stream.time_base().num() > 0 && stream.time_base().den() > 0) {
            double micros = streamDuration * (double) stream.time_base().num()
                    / stream.time_base().den() * 1_000_000.0;
            if (Double.isFinite(micros) && micros > 0 && micros <= Long.MAX_VALUE)
                return Math.round(micros);
        }
        long containerDuration = format.duration();
        return containerDuration > 0 && containerDuration != AV_NOPTS_VALUE ? containerDuration : 0;
    }

    private static double ptsSeconds(long pts, AVStream stream, long ordinal) {
        if (stream.time_base().num() > 0 && stream.time_base().den() > 0) {
            double seconds = pts * (double) stream.time_base().num() / stream.time_base().den();
            if (Double.isFinite(seconds))
                return seconds;
        }
        return ordinal;
    }

    private static boolean isAgain(int value) {
        // FFmpeg encodes errno values with the host platform's EAGAIN constant (-35 on macOS,
        // -11 on Linux), so the library helper must be used instead of a Linux-only literal.
        return value == AVERROR_EAGAIN();
    }

    private static MediaManifest.FramePosition position(int index) {
        return switch (index) {
            case 0 -> MediaManifest.FramePosition.FIRST;
            case 1 -> MediaManifest.FramePosition.QUARTER;
            case 2 -> MediaManifest.FramePosition.MIDDLE;
            case 3 -> MediaManifest.FramePosition.THREE_QUARTERS;
            case 4 -> MediaManifest.FramePosition.LAST;
            default -> throw new IllegalArgumentException("Retained frame index must be 0 through 4");
        };
    }

    @FunctionalInterface
    private interface DecodedFrameConsumer {
        void accept(long ordinal, long pts, double seconds, AVFrame frame) throws IOException;
    }

    private static final class DecodeCounter {
        long value;
    }

    record FramePoint(long ordinal, long pts, double seconds) {}

    private record StreamMetadata(String codec, long durationMicros) {}

    private record CapturedFrame(byte[] jpeg, int width, int height, long sourcePts,
                                 double sourcePtsSeconds) {}

    public record RetainedFrame(MediaManifest.FramePosition position, byte[] jpeg, int width,
                                int height, long sourcePts, double sourcePtsSeconds,
                                long decodedOrdinal) {}

    public record ExtractionResult(String sourceCodec, long durationMicros,
                                   List<RetainedFrame> frames) {}

    public record JpegInfo(int width, int height) {}
}
