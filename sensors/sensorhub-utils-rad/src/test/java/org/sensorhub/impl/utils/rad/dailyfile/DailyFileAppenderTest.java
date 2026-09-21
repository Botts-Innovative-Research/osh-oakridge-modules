package org.sensorhub.impl.utils.rad.dailyfile;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.datastore.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DailyFileAppenderTest {

    private static final Logger LOG = LoggerFactory.getLogger(DailyFileAppenderTest.class);
    private static final ZoneId ZONE = ZoneId.of("America/New_York");
    private static final String FILE_ID = "rpm001";

    private Path root;
    private IBucketStore store;
    private MutableClock clock;

    @Before
    public void setUp() throws Exception {
        root = Files.createTempDirectory("dailyfile-test-");
        store = new FileSystemBucketStore(root);
        clock = new MutableClock(ZonedDateTime.of(2026, 9, 16, 12, 0, 0, 0, ZONE).toInstant());
    }

    @After
    public void tearDown() throws Exception {
        if (root != null && Files.exists(root)) {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test
    public void writesCasLinesWithLocalAndUtcTimestamps() throws Exception {
        clock.set(ZonedDateTime.of(2026, 9, 16, 12, 0, 0, 961_000_000, ZONE).toInstant());
        try (DailyFileAppender appender = newAppender(store)) {
            appender.append("GB,000188,000206,000262,000243");
            appender.append("SG1,000660,000090,05,10,07.0,P");
            appender.append("SP,0.0136,50.134,080.68,000000");
        }

        assertEquals("GB,000188,000206,000262,000243,12-00-00.961,16-00-00.961\r\n"
                        + "SG1,000660,000090,05,10,07.0,P,12-00-00.961,16-00-00.961\r\n"
                        + "SP,0.0136,50.134,080.68,000000,12-00-00.961,16-00-00.961\r\n",
                read(key(LocalDate.of(2026, 9, 16))));
    }

    @Test
    public void preservesMessageFieldsBeforeTimestamps() throws Exception {
        try (DailyFileAppender appender = newAppender(store)) {
            appender.append("\"a\",b,\"c\"\"d\"");
        }
        assertEquals("\"a\",b,\"c\"\"d\",12-00-00.000,16-00-00.000\r\n", read(key(LocalDate.of(2026, 9, 16))));
    }

    @Test
    public void skipsNullAndEmptyLines() throws Exception {
        try (DailyFileAppender appender = newAppender(store)) {
            appender.append(null);
            appender.append("");
            appender.append("NB,000000,000000,000000,000000");
        }
        assertEquals("NB,000000,000000,000000,000000,12-00-00.000,16-00-00.000\r\n",
                read(key(LocalDate.of(2026, 9, 16))));
    }

    @Test
    public void rollsOverAtLocalMidnight() throws Exception {
        // 23:59:59 local on 09-16 is already 03:59:59Z on 09-17: proves the LOCAL date is used
        clock.set(ZonedDateTime.of(2026, 9, 16, 23, 59, 59, 0, ZONE).toInstant());
        try (DailyFileAppender appender = newAppender(store)) {
            appender.append("A");
            assertEquals(key(LocalDate.of(2026, 9, 16)), appender.currentKey());

            clock.set(ZonedDateTime.of(2026, 9, 17, 0, 0, 0, 0, ZONE).toInstant());
            appender.append("B");
            assertEquals(key(LocalDate.of(2026, 9, 17)), appender.currentKey());
        }

        assertEquals("A,23-59-59.000,03-59-59.000\r\n", read(key(LocalDate.of(2026, 9, 16))));
        assertEquals("B,00-00-00.000,04-00-00.000\r\n", read(key(LocalDate.of(2026, 9, 17))));
    }

    @Test
    public void reopenAppendsRatherThanTruncates() throws Exception {
        try (DailyFileAppender first = newAppender(store)) {
            first.append("A");
        }
        try (DailyFileAppender second = newAppender(store)) {
            second.append("B");
        }
        assertEquals("A,12-00-00.000,16-00-00.000\r\nB,12-00-00.000,16-00-00.000\r\n",
                read(key(LocalDate.of(2026, 9, 16))));
    }

    @Test
    public void buffersUntilStoreReadyThenWritesInOrder() throws Exception {
        CompletableFuture<IBucketStore> future = new CompletableFuture<>();
        try (DailyFileAppender appender = new DailyFileAppender(future, FILE_ID, clock, LOG)) {
            appender.append("A");
            appender.append("B");
            assertEquals(2, appender.queuedLines());
            assertNull(appender.currentKey());

            future.complete(store);
            appender.append("C");

            assertEquals(0, appender.queuedLines());
        }
        assertEquals("A,12-00-00.000,16-00-00.000\r\nB,12-00-00.000,16-00-00.000\r\n"
                        + "C,12-00-00.000,16-00-00.000\r\n",
                read(key(LocalDate.of(2026, 9, 16))));
    }

    @Test
    public void boundedQueueDropsOldest() throws Exception {
        CompletableFuture<IBucketStore> future = new CompletableFuture<>();
        try (DailyFileAppender appender = new DailyFileAppender(future, FILE_ID, clock, LOG)) {
            for (int i = 0; i <= DailyFileAppender.MAX_QUEUED_LINES; i++)
                appender.append("L" + i);
            assertEquals(DailyFileAppender.MAX_QUEUED_LINES, appender.queuedLines());
            future.complete(store);
        }

        String content = read(key(LocalDate.of(2026, 9, 16)));
        assertFalse("oldest line should have been dropped", content.startsWith("L0,"));
        assertTrue(content.startsWith("L1,12-00-00.000,16-00-00.000\r\n"));
        assertTrue(content.endsWith("L" + DailyFileAppender.MAX_QUEUED_LINES
                + ",12-00-00.000,16-00-00.000\r\n"));
    }

    @Test
    public void ioErrorDoesNotThrowAndRecovers() throws Exception {
        IBucketStore flaky = mock(IBucketStore.class);
        when(flaky.bucketExists(anyString())).thenReturn(true);
        when(flaky.appendObject(anyString(), anyString(), any()))
                .thenThrow(new DataStoreException("disk on fire"))
                .thenAnswer(inv -> store.appendObject(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
        store.createBucket(DailyFileAppender.BUCKET);

        try (DailyFileAppender appender = newAppender(flaky)) {
            appender.append("A");   // fails to open, must not throw
            assertEquals(1, appender.droppedLines());
            appender.append("B");   // reopens successfully
            assertEquals(1, appender.droppedLines());
        }
        assertEquals("B,12-00-00.000,16-00-00.000\r\n", read(key(LocalDate.of(2026, 9, 16))));
    }

    @Test
    public void createsBucketIfMissing() throws Exception {
        assertFalse(store.bucketExists(DailyFileAppender.BUCKET));
        try (DailyFileAppender appender = newAppender(store)) {
            appender.append("A");
        }
        assertTrue(store.bucketExists(DailyFileAppender.BUCKET));
    }

    @Test
    public void sanitizesFileId() throws Exception {
        try (DailyFileAppender appender = new DailyFileAppender(CompletableFuture.completedFuture(store), "rpm/00 1", clock, LOG)) {
            appender.append("A");
            assertEquals("rpm_00_1_2026-09-16.csv", appender.currentKey());
        }
        assertTrue(store.objectExists(DailyFileAppender.BUCKET, "rpm_00_1_2026-09-16.csv"));
    }

    @Test
    public void appendAfterCloseIsIgnored() throws Exception {
        DailyFileAppender appender = newAppender(store);
        appender.append("A");
        appender.close();
        appender.append("B");
        assertEquals("A,12-00-00.000,16-00-00.000\r\n", read(key(LocalDate.of(2026, 9, 16))));
    }

    // ---- helpers ----

    private DailyFileAppender newAppender(IBucketStore s) {
        return new DailyFileAppender(CompletableFuture.completedFuture(s), FILE_ID, clock, LOG);
    }

    private static String key(LocalDate date) {
        return FILE_ID + "_" + date + ".csv";
    }

    private String read(String key) throws Exception {
        try (InputStream in = store.getObject(DailyFileAppender.BUCKET, key)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Clock with a settable instant, fixed zone. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
