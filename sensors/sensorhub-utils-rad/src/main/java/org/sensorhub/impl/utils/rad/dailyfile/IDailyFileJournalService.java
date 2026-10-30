package org.sensorhub.impl.utils.rad.dailyfile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/** Service capability exposed by the durable daily-file journal module. */
public interface IDailyFileJournalService {

    /**
     * Returns {@code true} once the durable journal store can serve maintenance reads.
     *
     * <p>The OSCAR service and journal module can be started concurrently.  Maintenance
     * callers use this signal to defer a bounded catch-up cycle instead of treating normal
     * module-start ordering as a per-feed I/O failure.  Custom implementations that are ready
     * as soon as they are discoverable retain the historical behavior through this default.</p>
     */
    default boolean isJournalReady() {
        return true;
    }

    /**
     * Opens a synchronous writer for a stable lane identifier. Repeated calls
     * for the same lane share the same ordered durable stream.
     */
    DailyFileWriterHandle openWriter(String laneId) throws IOException;

    DailyFileObservationMode getObservationMode();

    ZoneId getJournalZoneId();

    /**
     * Returns true only after both the legacy CSV and its checksum manifest
     * have been durably published for this exact half-open window.
     */
    boolean isPublished(String laneId, Instant startInclusive, Instant endExclusive);

    /**
     * Records an external backup system's durable confirmation for an immutable published day.
     * Implementations must verify the publication checksum before accepting the confirmation.
     */
    default void confirmPublishedBackup(String laneId, Instant startInclusive, Instant endExclusive,
                                        String publishedSha256, String backupReference)
            throws IOException {
        throw new UnsupportedOperationException("Backup confirmation is not supported");
    }

    default boolean isBackupConfirmed(String laneId, Instant startInclusive,
                                      Instant endExclusive) {
        return false;
    }

    /** Last calendar day whose compatibility observations were fully purged for one stable feed. */
    default Optional<LocalDate> readObservationPurgeCheckpoint(String feedId) throws IOException {
        return Optional.empty();
    }

    /** Atomically advances the compatibility-observation purge checkpoint. */
    default void writeObservationPurgeCheckpoint(String feedId, LocalDate processedThrough)
            throws IOException {
        // Older/custom implementations may rely on oldest-record discovery instead.
    }

    /** Earliest on-disk journal day for a lane, used to seed first-run catch-up. */
    default Optional<LocalDate> findEarliestJournalDay(String laneId) throws IOException {
        return Optional.empty();
    }
}
