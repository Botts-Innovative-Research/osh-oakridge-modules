package com.botts.impl.service.oscar.media;

import java.time.Instant;
import java.util.Objects;

/** Durable lower bound for bounded time-bucketed processed-observation reclamation. */
public record MediaMetadataReclamationCheckpoint(
        int schemaVersion,
        Long earliestProcessedObservationEpochHour,
        Instant updatedAt) {

    public static final int CURRENT_SCHEMA_VERSION = 2;
    private static final long MAX_PROCESSED_OBSERVATION_EPOCH_HOUR =
            Math.floorDiv(Instant.MAX.getEpochSecond(), 60L * 60L) - 1L;

    public MediaMetadataReclamationCheckpoint {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "Unsupported media metadata checkpoint schema: " + schemaVersion);
        if (earliestProcessedObservationEpochHour != null
                && (earliestProcessedObservationEpochHour < 0
                || earliestProcessedObservationEpochHour
                > MAX_PROCESSED_OBSERVATION_EPOCH_HOUR))
            throw new IllegalArgumentException(
                    "Processed-observation epoch hour is outside the supported Instant range");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static MediaMetadataReclamationCheckpoint initial(Instant at) {
        return new MediaMetadataReclamationCheckpoint(CURRENT_SCHEMA_VERSION, null, at);
    }

    public MediaMetadataReclamationCheckpoint withEarliestProcessedObservationEpochHour(
            Long epochHour, Instant at) {
        return new MediaMetadataReclamationCheckpoint(schemaVersion, epochHour, at);
    }
}
