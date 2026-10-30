package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** One direct-addressed retained-media set that must be reconciled periodically. */
public record MediaCatalogJob(int schemaVersion, long sequence, String eventId, String laneUid,
                              String cameraUid, Instant createdAt) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public MediaCatalogJob {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || sequence < 0)
            throw new IllegalArgumentException("Invalid media catalog queue job");
        Objects.requireNonNull(createdAt, "createdAt");
        new MediaSetStore.Identity(eventId, laneUid, cameraUid);
    }

    public MediaSetStore.Identity identity() {
        return new MediaSetStore.Identity(eventId, laneUid, cameraUid);
    }

    public MediaCatalogJob withSequence(long replacement) {
        return new MediaCatalogJob(schemaVersion, replacement, eventId, laneUid, cameraUid,
                createdAt);
    }
}
