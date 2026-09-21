/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.utils.rad.dailyfile;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.module.ModuleEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Appends radiation portal monitor (RPM) messages to a per-day "daily file" stored in the
 * {@value #BUCKET} bucket of the node's bucket store, one message per line.
 * <p>
 * The output matches legacy CAS daily files: the message fields are followed by the computer's
 * local time and UTC time, both in {@code HH-MM-SS.sss} format, with no header, no quoting, and CRLF
 * line endings. Files are named {@code <fileId>_<YYYY-MM-DD>.csv} and roll over at local midnight
 * (per the supplied clock). Every line is flushed immediately so a crash loses at most the line
 * being written.
 * <p>
 * The bucket store may not be available when a driver starts (OSH starts modules concurrently), so
 * lines are queued in memory (bounded) until the store future completes, then written in order.
 * {@link #append(String)} never throws; write failures are counted and logged at a limited rate.
 */
public class DailyFileAppender implements AutoCloseable {

    public static final String BUCKET = "dailyfiles";
    public static final String CONTENT_TYPE = "text/csv";
    public static final String FILE_EXTENSION = ".csv";
    static final String LINE_END = "\r\n";
    static final int MAX_QUEUED_LINES = 20_000;
    static final long ERROR_LOG_INTERVAL_MS = 60_000;
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH-mm-ss.SSS");

    private final CompletableFuture<IBucketStore> storeFuture;
    private final String fileId;
    private final Clock clock;
    private final Logger log;

    private final Deque<DailyFileLine> pending = new ArrayDeque<>();
    private IBucketStore store;
    private OutputStream out;
    private LocalDate currentDate;
    private String currentKey;
    private boolean closed;
    private boolean queueOverflowWarned;
    private long droppedLines;
    private long lastErrorLogMillis;

    /**
     * @param storeFuture completes with the bucket store to write into (may already be complete)
     * @param fileId      identifier used as the file name prefix (e.g. the RPM serial number)
     * @param clock       clock used to determine the local date of each line
     * @param log         driver logger
     */
    public DailyFileAppender(CompletableFuture<IBucketStore> storeFuture, String fileId, Clock clock, Logger log) {
        this.storeFuture = Objects.requireNonNull(storeFuture, "storeFuture");
        this.fileId = sanitizeFileId(fileId);
        this.clock = Objects.requireNonNull(clock, "clock");
        this.log = Objects.requireNonNull(log, "log");

        storeFuture.whenComplete((s, err) -> {
            synchronized (this) {
                if (closed)
                    return;
                if (err != null || s == null) {
                    log.error("Daily file '{}' has no bucket store available; {} queued line(s) will be dropped",
                            this.fileId, pending.size(), err);
                    return;
                }
                store = s;
                drainPending();
            }
        });
    }

    /**
     * Creates an appender bound to the hub's bucket service. The lookup is asynchronous, so the driver
     * can start (and buffer messages) before the bucket service is up.
     *
     * @param hub    the parent hub
     * @param fileId identifier used as the file name prefix (e.g. the RPM serial number)
     * @param log    driver logger
     */
    public static DailyFileAppender forHub(ISensorHub hub, String fileId, Logger log) {
        CompletableFuture<IBucketStore> storeFuture = hub.getModuleRegistry()
                .waitForModuleType(IBucketService.class, ModuleEvent.ModuleState.STARTED)
                .thenApply(IBucketService::getBucketStore);
        return new DailyFileAppender(storeFuture, fileId, Clock.systemDefaultZone(), log);
    }

    /**
     * Appends one message line. Never throws.
     *
     * @param line the message fields exactly as received (without timestamps or line terminator);
     *             null or empty lines are ignored
     */
    public synchronized void append(String line) {
        if (closed || line == null || line.isEmpty())
            return;

        Instant timestamp = clock.instant();
        DailyFileLine dailyFileLine = new DailyFileLine(
                LocalDate.ofInstant(timestamp, clock.getZone()),
                line + "," + TIME_FORMAT.withZone(clock.getZone()).format(timestamp)
                        + "," + TIME_FORMAT.withZone(ZoneOffset.UTC).format(timestamp));

        if (store == null) {
            if (storeFuture.isDone() && !storeFuture.isCompletedExceptionally()) {
                store = storeFuture.getNow(null);
            }
            if (store == null) {
                enqueue(dailyFileLine);
                return;
            }
            drainPending();
        }

        writeLine(dailyFileLine);
    }

    /**
     * Flushes and closes the current file. Subsequent calls to {@link #append(String)} are ignored.
     */
    @Override
    public synchronized void close() {
        if (closed)
            return;
        closed = true;
        if (store != null)
            drainPending();
        closeStream();
        if (!pending.isEmpty()) {
            log.warn("Daily file '{}' closed with {} unwritten line(s)", fileId, pending.size());
            pending.clear();
        }
    }

    /** @return the bucket object key currently being written, or null if none is open */
    synchronized String currentKey() {
        return currentKey;
    }

    /** @return number of lines waiting for the bucket store */
    synchronized int queuedLines() {
        return pending.size();
    }

    /** @return number of lines dropped because of write failures */
    synchronized long droppedLines() {
        return droppedLines;
    }

    static String keyFor(String fileId, LocalDate date) {
        return sanitizeFileId(fileId) + "_" + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + FILE_EXTENSION;
    }

    static String sanitizeFileId(String fileId) {
        if (fileId == null || fileId.isBlank())
            return "rpm";
        return fileId.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void enqueue(DailyFileLine line) {
        if (pending.size() >= MAX_QUEUED_LINES) {
            pending.pollFirst();
            if (!queueOverflowWarned) {
                queueOverflowWarned = true;
                log.warn("Daily file '{}' queue is full ({} lines) while waiting for the bucket store; dropping oldest lines",
                        fileId, MAX_QUEUED_LINES);
            }
        }
        pending.addLast(line);
    }

    private void drainPending() {
        DailyFileLine line;
        while ((line = pending.pollFirst()) != null)
            writeLine(line);
    }

    private void writeLine(DailyFileLine line) {
        try {
            if (out == null || !line.date().equals(currentDate))
                openStream(line.date());

            out.write((line.text() + LINE_END).getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            closeStream();
            droppedLines++;
            long now = clock.millis();
            if (now - lastErrorLogMillis >= ERROR_LOG_INTERVAL_MS) {
                lastErrorLogMillis = now;
                log.error("Failed to write to daily file '{}' ({} line(s) dropped so far): {}",
                        currentKey != null ? currentKey : fileId, droppedLines, e.toString());
            }
        }
    }

    private void openStream(LocalDate date) throws Exception {
        closeStream();
        if (!store.bucketExists(BUCKET))
            store.createBucket(BUCKET);
        String key = keyFor(fileId, date);
        out = store.appendObject(BUCKET, key, Map.of("Content-Type", CONTENT_TYPE));
        currentDate = date;
        currentKey = key;
        log.info("Writing daily file {}/{}", BUCKET, key);
    }

    private void closeStream() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                log.warn("Error closing daily file {}: {}", currentKey, e.toString());
            }
            out = null;
        }
        currentDate = null;
    }

    private record DailyFileLine(LocalDate date, String text) {
    }
}
