package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable terminal tombstone preventing an emergency-deleted media set from being recreated. */
public record MediaDeletionMarker(
        int schemaVersion,
        String mediaSetId,
        String eventId,
        String laneUid,
        String cameraUid,
        Instant deletedAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public MediaDeletionMarker {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "Unsupported media deletion marker schema: " + schemaVersion);
        Objects.requireNonNull(mediaSetId, "mediaSetId");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(laneUid, "laneUid");
        Objects.requireNonNull(cameraUid, "cameraUid");
        Objects.requireNonNull(deletedAt, "deletedAt");
    }

    public static MediaDeletionMarker at(MediaSetStore.Identity identity, Instant deletedAt) {
        return new MediaDeletionMarker(CURRENT_SCHEMA_VERSION, identity.mediaSetId(),
                identity.eventId(), identity.laneUid(), identity.cameraUid(), deletedAt);
    }
}
