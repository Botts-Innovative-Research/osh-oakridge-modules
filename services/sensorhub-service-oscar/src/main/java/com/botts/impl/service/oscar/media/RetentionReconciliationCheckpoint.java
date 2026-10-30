package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable state for one frozen historical uncatalogued-media reconciliation sweep. */
public record RetentionReconciliationCheckpoint(
        int schemaVersion,
        Instant windowStart,
        Instant processedThrough,
        Instant windowEnd,
        Instant lastCompletedAt,
        Instant updatedAt) {

    /** Version 1 contained only processedThrough/updatedAt and had no frozen sweep bound. */
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public RetentionReconciliationCheckpoint {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "Unsupported retention reconciliation checkpoint schema: " + schemaVersion);
        Objects.requireNonNull(windowStart, "windowStart");
        Objects.requireNonNull(processedThrough, "processedThrough");
        Objects.requireNonNull(windowEnd, "windowEnd");
        Objects.requireNonNull(lastCompletedAt, "lastCompletedAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (processedThrough.isBefore(windowStart) || processedThrough.isAfter(windowEnd))
            throw new IllegalArgumentException(
                    "processedThrough must be inside the reconciliation sweep");
        if (lastCompletedAt.isAfter(updatedAt))
            throw new IllegalArgumentException("lastCompletedAt must not follow updatedAt");
    }

    public static RetentionReconciliationCheckpoint at(Instant processedThrough, Instant updatedAt) {
        return initial(processedThrough, updatedAt);
    }

    /** Creates a newly initialized cursor whose first full sweep is immediately due. */
    public static RetentionReconciliationCheckpoint initial(Instant cursor, Instant updatedAt) {
        return new RetentionReconciliationCheckpoint(CURRENT_SCHEMA_VERSION, cursor, cursor,
                cursor, Instant.EPOCH, updatedAt);
    }

    public RetentionReconciliationCheckpoint begin(Instant start, Instant end, Instant now) {
        return new RetentionReconciliationCheckpoint(CURRENT_SCHEMA_VERSION, start, start, end,
                earlier(lastCompletedAt, now), now);
    }

    public RetentionReconciliationCheckpoint advance(Instant cursor, Instant now) {
        return new RetentionReconciliationCheckpoint(CURRENT_SCHEMA_VERSION, windowStart, cursor,
                windowEnd, earlier(lastCompletedAt, now), now);
    }

    public RetentionReconciliationCheckpoint complete(Instant now) {
        return new RetentionReconciliationCheckpoint(CURRENT_SCHEMA_VERSION, windowStart,
                windowEnd, windowEnd, now, now);
    }

    public boolean active() {
        return processedThrough.isBefore(windowEnd);
    }

    private static Instant earlier(Instant left, Instant right) {
        return left.compareTo(right) <= 0 ? left : right;
    }
}
