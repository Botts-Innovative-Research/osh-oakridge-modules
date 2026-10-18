package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable cursor for the slow historical uncatalogued-media reconciliation sweep. */
public record RetentionReconciliationCheckpoint(
        int schemaVersion,
        Instant processedThrough,
        Instant updatedAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public RetentionReconciliationCheckpoint {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "Unsupported retention reconciliation checkpoint schema: " + schemaVersion);
        Objects.requireNonNull(processedThrough, "processedThrough");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(processedThrough))
            throw new IllegalArgumentException("updatedAt must not precede processedThrough");
    }

    public static RetentionReconciliationCheckpoint at(Instant processedThrough, Instant updatedAt) {
        return new RetentionReconciliationCheckpoint(CURRENT_SCHEMA_VERSION, processedThrough,
                updatedAt);
    }
}
