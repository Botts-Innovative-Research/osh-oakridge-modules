package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable head/tail for the direct-addressed retained-media reconciliation queue. */
public record MediaCatalogQueueState(int schemaVersion, long headSequence, long nextSequence,
                                     Instant updatedAt) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public MediaCatalogQueueState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || headSequence < 0
                || nextSequence < headSequence)
            throw new IllegalArgumentException("Invalid media catalog queue state");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static MediaCatalogQueueState initial(Instant at) {
        return new MediaCatalogQueueState(CURRENT_SCHEMA_VERSION, 0, 0, at);
    }

    public MediaCatalogQueueState reserve(Instant at) {
        if (nextSequence == Long.MAX_VALUE)
            throw new IllegalStateException("Media catalog queue sequence is exhausted");
        return new MediaCatalogQueueState(schemaVersion, headSequence, nextSequence + 1, at);
    }

    public MediaCatalogQueueState advance(Instant at) {
        if (headSequence >= nextSequence)
            throw new IllegalStateException("Cannot advance an empty media catalog queue");
        return new MediaCatalogQueueState(schemaVersion, headSequence + 1, nextSequence, at);
    }
}
