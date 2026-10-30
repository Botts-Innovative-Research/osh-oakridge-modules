package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable, identity-aware state for one event/lane/camera retention job. */
public record MediaJobLedger(
        int schemaVersion,
        String eventId,
        String laneUid,
        String cameraUid,
        String sourceObjectKey,
        JobState state,
        int attempts,
        Instant updatedAt,
        String failure) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public MediaJobLedger {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported media job ledger schema: " + schemaVersion);
        requireText(eventId, "eventId");
        requireText(laneUid, "laneUid");
        requireText(cameraUid, "cameraUid");
        requireText(sourceObjectKey, "sourceObjectKey");
        Objects.requireNonNull(state, "state");
        if (attempts < 0)
            throw new IllegalArgumentException("attempts must not be negative");
        Objects.requireNonNull(updatedAt, "updatedAt");
        failure = Objects.requireNonNull(failure, "failure");
        if (failure.length() > 1024)
            throw new IllegalArgumentException("failure must be at most 1024 characters");
    }

    public enum JobState {
        DISCOVERED,
        DECODING,
        PUBLISHING,
        PUBLISHED,
        SOURCE_DELETED,
        FAILED
    }

    public MediaJobLedger transition(JobState replacement, String failureMessage, Instant at) {
        return new MediaJobLedger(schemaVersion, eventId, laneUid, cameraUid, sourceObjectKey,
                replacement, attempts, at, failureMessage == null ? "" : failureMessage);
    }

    public MediaJobLedger nextAttempt(Instant at) {
        return new MediaJobLedger(schemaVersion, eventId, laneUid, cameraUid, sourceObjectKey,
                JobState.DECODING, attempts + 1, at, "");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " is required");
    }
}
