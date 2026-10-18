package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable head/tail for the direct-addressed storage-pressure queue. */
public record MediaPressureQueueState(int schemaVersion, long headSequence, long nextSequence,
                                      Instant updatedAt) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public MediaPressureQueueState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || headSequence < 0
                || nextSequence < headSequence)
            throw new IllegalArgumentException("Invalid media pressure queue state");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static MediaPressureQueueState initial(Instant at) {
        return new MediaPressureQueueState(CURRENT_SCHEMA_VERSION, 0, 0, at);
    }

    public MediaPressureQueueState reserve(Instant at) {
        if (nextSequence == Long.MAX_VALUE)
            throw new IllegalStateException("Media pressure queue sequence is exhausted");
        return new MediaPressureQueueState(schemaVersion, headSequence, nextSequence + 1, at);
    }

    public MediaPressureQueueState advance(Instant at) {
        if (headSequence >= nextSequence)
            throw new IllegalStateException("Cannot advance an empty media pressure queue");
        return new MediaPressureQueueState(schemaVersion, headSequence + 1, nextSequence, at);
    }
}
