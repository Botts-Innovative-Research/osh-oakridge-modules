package org.sensorhub.impl.utils.rad.dailyfile;

import java.time.Instant;
import java.util.Objects;

/**
 * One exact RPM payload and the time it was received or generated.
 *
 * <p>The raw payload is deliberately kept separate from parsing and from the
 * legacy CSV rendering step. It may contain commas, quotes, or line breaks;
 * the journal stores its UTF-8 bytes without normalization.</p>
 */
public record DailyFileRecord(Instant timestamp, String rawLine) {
    public DailyFileRecord {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(rawLine, "rawLine");
    }
}
