package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avcodec.AV_FIELD_PROGRESSIVE;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class FFmpegUtils {
    private static final Logger logger = LoggerFactory.getLogger(FFmpegUtils.class);

    public static void cleanCodecParameters(AVCodecParameters codecpar) {
        Objects.requireNonNull(codecpar);

        // Ensure valid codec type
        int codecType = codecpar.codec_type();
        if (codecType == AVMEDIA_TYPE_UNKNOWN) {
            logger.warn("Codec type not set, defaulting to video");
            codecpar.codec_type(AVMEDIA_TYPE_VIDEO);  // default to video
        }

        // Codec
        if (codecpar.codec_id() == AV_CODEC_ID_NONE) {
            logger.warn("Codec not set, defaulting to H264");
            codecpar.codec_id(AV_CODEC_ID_H264);
        }

        if (codecpar.framerate() == null) {
            logger.warn("Frame rate not set, defaulting to 30 fps");
            codecpar.framerate(av_make_q(30, 1));
        }

        // Width/height
        if (codecpar.width() <= 0) {
            logger.warn("Width not set, defaulting to 1920");
            codecpar.width(1920);
        }
        if (codecpar.height() <= 0) {
            logger.warn("Height not set, defaulting to 1080");
            codecpar.height(1080);
        }

        // Pixel fmt
        if (codecpar.format() < 0) {
            logger.warn("Pixel format not set, defaulting to YUV420P");
            codecpar.format(AV_PIX_FMT_YUV420P);
        }

        if (codecpar.field_order() == AV_FIELD_UNKNOWN) {
            logger.warn("Field order not set, defaulting to progressive");
            codecpar.field_order(AV_FIELD_PROGRESSIVE);
        }

    }
}
