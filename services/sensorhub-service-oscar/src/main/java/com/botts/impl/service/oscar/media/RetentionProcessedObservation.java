package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable idempotency marker for an occupancy accepted by the retained-media job system. */
public record RetentionProcessedObservation(
        int schemaVersion,
        String observationId,
        Instant resultTime,
        Instant processedAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public RetentionProcessedObservation {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "Unsupported processed-observation schema: " + schemaVersion);
        if (observationId == null || observationId.isBlank())
            throw new IllegalArgumentException("observationId is required");
        Objects.requireNonNull(resultTime, "resultTime");
        Objects.requireNonNull(processedAt, "processedAt");
    }

    public static RetentionProcessedObservation at(String observationId, Instant resultTime,
                                                    Instant processedAt) {
        return new RetentionProcessedObservation(CURRENT_SCHEMA_VERSION, observationId,
                resultTime, processedAt);
    }
}
