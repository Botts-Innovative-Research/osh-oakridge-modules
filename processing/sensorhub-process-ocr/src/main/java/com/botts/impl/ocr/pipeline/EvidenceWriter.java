package com.botts.impl.ocr.pipeline;

import com.botts.api.service.bucket.IBucketStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Writes JPEG evidence crops of OCR reads into the "ocr" bucket. */
public class EvidenceWriter {

    public static final String OCR_BUCKET = "ocr";
    static final double CROP_PAD_RATIO = 0.25;

    private static final DateTimeFormatter KEY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withLocale(Locale.US).withZone(ZoneId.systemDefault());
    private static final Logger logger = LoggerFactory.getLogger(EvidenceWriter.class);

    private final IBucketStore bucketStore;

    public EvidenceWriter(IBucketStore bucketStore) {
        this.bucketStore = bucketStore;
    }

    /**
     * Crops the evidence box (with padding) out of the frame and stores it as
     * {@code ocr/crops/<lane>/<occStart>_<idType>_<seq>.jpg}.
     *
     * @return the bucket-relative path to store in the observation, or "" on failure
     */
    public String writeCrop(String laneUid, Instant occupancyStartTime, String idType, int sequence,
                            BufferedImage frame, Rectangle box) {
        try {
            if (!bucketStore.bucketExists(OCR_BUCKET))
                bucketStore.createBucket(OCR_BUCKET);

            BufferedImage crop = cropWithPadding(frame, box);
            String key = "crops/" + laneUid.replace(':', '-') + "/"
                    + KEY_TIME_FORMAT.format(occupancyStartTime) + "_" + idType + "_" + sequence + ".jpg";

            try (OutputStream out = bucketStore.putObject(OCR_BUCKET, key, Map.of("Content-Type", "image/jpeg"))) {
                ImageIO.write(crop, "jpg", out);
            }
            return bucketStore.getRelativeResourceURI(OCR_BUCKET, key);
        } catch (Exception e) {
            logger.warn("Failed to write OCR evidence crop for lane {}", laneUid, e);
            return "";
        }
    }

    static BufferedImage cropWithPadding(BufferedImage frame, Rectangle box) {
        int padX = (int) Math.round(box.width * CROP_PAD_RATIO);
        int padY = (int) Math.round(box.height * CROP_PAD_RATIO);
        int x = Math.max(0, box.x - padX);
        int y = Math.max(0, box.y - padY);
        int w = Math.min(frame.getWidth() - x, box.width + 2 * padX);
        int h = Math.min(frame.getHeight() - y, box.height + 2 * padY);

        // getSubimage shares the backing raster; copy so the JPEG encoder sees a standalone image
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        var g = copy.createGraphics();
        g.drawImage(frame.getSubimage(x, y, w, h), 0, 0, null);
        g.dispose();
        return copy;
    }
}
