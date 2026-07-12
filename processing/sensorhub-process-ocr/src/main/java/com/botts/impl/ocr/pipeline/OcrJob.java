package com.botts.impl.ocr.pipeline;

import java.time.Instant;
import java.util.List;

/** One OCR work item: the recorded clips of a single alarming occupancy. */
public class OcrJob {

    /** A recorded clip and the camera it came from. */
    public static class VideoRef {
        /** Bucket-relative path as carried in the occupancy record, e.g. videos/clips/... */
        public final String videoPath;
        public final String cameraUid;

        public VideoRef(String videoPath, String cameraUid) {
            this.videoPath = videoPath;
            this.cameraUid = cameraUid;
        }
    }

    public final String laneUid;
    public final String occupancyObsId;
    public final Instant occupancyStartTime;
    public final List<VideoRef> videos;
    public final OcrSettings settings;

    public OcrJob(String laneUid, String occupancyObsId, Instant occupancyStartTime,
                  List<VideoRef> videos, OcrSettings settings) {
        this.laneUid = laneUid;
        this.occupancyObsId = occupancyObsId;
        this.occupancyStartTime = occupancyStartTime;
        this.videos = videos;
        this.settings = settings;
    }
}
