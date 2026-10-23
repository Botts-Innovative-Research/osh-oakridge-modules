package org.sensorhub.impl.utils.rad.dailyfile;

import java.io.IOException;
import java.time.Instant;

/**
 * Synchronous handle for one lane's durable daily-file journal.
 *
 * <p>Implementations must apply backpressure and report failures. Callers must
 * never assume that an unsuccessful append was queued for later delivery.</p>
 */
public interface DailyFileWriterHandle extends AutoCloseable {

    String getLaneId();

    void append(DailyFileRecord record) throws IOException;

    default void append(String rawLine, Instant timestamp) throws IOException {
        append(new DailyFileRecord(timestamp, rawLine));
    }

    /** Forces all records accepted by this handle to stable storage. */
    void flush() throws IOException;

    @Override
    void close() throws IOException;
}
