package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable high-water mark for the occupancy-to-media retention scan. */
public record RetentionScanCheckpoint(
        int schemaVersion,
        Instant processedThrough,
        Instant updatedAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public RetentionScanCheckpoint {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "Unsupported retention scan checkpoint schema: " + schemaVersion);
        Objects.requireNonNull(processedThrough, "processedThrough");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(processedThrough))
            throw new IllegalArgumentException("updatedAt must not precede processedThrough");
    }

    public static RetentionScanCheckpoint at(Instant processedThrough, Instant updatedAt) {
        return new RetentionScanCheckpoint(CURRENT_SCHEMA_VERSION, processedThrough, updatedAt);
    }
}
