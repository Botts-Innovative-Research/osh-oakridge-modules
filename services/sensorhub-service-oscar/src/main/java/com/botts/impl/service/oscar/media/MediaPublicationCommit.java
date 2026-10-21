package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable intent/commit record used to anchor source rollback grace after manifest commit. */
public record MediaPublicationCommit(
        int schemaVersion,
        String mediaSetId,
        long rollbackGraceSeconds,
        State state,
        Instant committedAt,
        Instant rollbackDeleteAfter) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public MediaPublicationCommit {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported media publication commit version");
        if (mediaSetId == null || !mediaSetId.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("mediaSetId must be a lowercase SHA-256 digest");
        if (rollbackGraceSeconds < 0)
            throw new IllegalArgumentException("rollbackGraceSeconds must not be negative");
        Objects.requireNonNull(state, "state");
        if (state == State.RESERVED) {
            if (committedAt != null || rollbackDeleteAfter != null)
                throw new IllegalArgumentException("A reserved publication has no commit time");
        } else {
            Objects.requireNonNull(committedAt, "committedAt");
            Objects.requireNonNull(rollbackDeleteAfter, "rollbackDeleteAfter");
            if (!rollbackDeleteAfter.equals(committedAt.plusSeconds(rollbackGraceSeconds)))
                throw new IllegalArgumentException("Rollback deadline must equal commit time plus grace");
        }
    }

    public static MediaPublicationCommit reserved(String mediaSetId, long graceSeconds) {
        return new MediaPublicationCommit(CURRENT_SCHEMA_VERSION, mediaSetId, graceSeconds,
                State.RESERVED, null, null);
    }

    public MediaPublicationCommit committed(Instant at) {
        Objects.requireNonNull(at, "at");
        return new MediaPublicationCommit(schemaVersion, mediaSetId, rollbackGraceSeconds,
                State.COMMITTED, at, at.plusSeconds(rollbackGraceSeconds));
    }

    public enum State {
        RESERVED,
        COMMITTED
    }
}

