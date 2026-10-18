package org.sensorhub.impl.utils.rad.dailyfile;

/**
 * Controls how the legacy {@code dailyFile} sensor output is retained while
 * the direct daily-file journal is authoritative for the archive artifact.
 */
public enum DailyFileObservationMode {
    /** Publish and retain every daily-file observation until its sealed day is purged. */
    FULL,
    /** Production compatibility label with the same safe completed-day cleanup as {@link #FULL}. */
    LATEST,
    /** Do not publish the legacy daily-file observation. */
    NONE
}
