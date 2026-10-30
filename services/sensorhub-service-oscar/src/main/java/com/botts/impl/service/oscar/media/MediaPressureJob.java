package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** One durable, direct-addressed candidate for emergency retained-media cleanup. */
public record MediaPressureJob(int schemaVersion, long sequence, String eventId, String laneUid,
                               String cameraUid, Instant createdAt) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public MediaPressureJob {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || sequence < 0)
            throw new IllegalArgumentException("Invalid media pressure queue job");
        Objects.requireNonNull(createdAt, "createdAt");
        // Reuse the canonical retained-media identity validation.
        new MediaSetStore.Identity(eventId, laneUid, cameraUid);
    }

    public MediaSetStore.Identity identity() {
        return new MediaSetStore.Identity(eventId, laneUid, cameraUid);
    }

    public MediaPressureJob withSequence(long replacement) {
        return new MediaPressureJob(schemaVersion, replacement, eventId, laneUid, cameraUid,
                createdAt);
    }
}
