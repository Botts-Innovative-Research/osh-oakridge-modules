package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable, sharded work item for retained-media retries and source lifecycle advancement. */
public record MediaMaintenanceJob(
        int schemaVersion,
        Kind kind,
        String eventId,
        String laneUid,
        String cameraUid,
        String sourceObjectKey,
        Instant dueAt,
        int attempts) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public MediaMaintenanceJob {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "Unsupported media maintenance job schema: " + schemaVersion);
        Objects.requireNonNull(kind, "kind");
        requireText(eventId, "eventId");
        requireText(laneUid, "laneUid");
        requireText(cameraUid, "cameraUid");
        requireText(sourceObjectKey, "sourceObjectKey");
        Objects.requireNonNull(dueAt, "dueAt");
        if (attempts < 0)
            throw new IllegalArgumentException("attempts must not be negative");
    }

    public MediaSetStore.Identity identity() {
        return new MediaSetStore.Identity(eventId, laneUid, cameraUid);
    }

    public enum Kind {
        SOURCE_LIFECYCLE,
        RETRY
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " is required");
    }
}
