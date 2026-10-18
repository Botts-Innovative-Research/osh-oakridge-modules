package com.botts.impl.service.dailyfile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileRecord;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileWriterHandle;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DailyFileJournalStoreTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private Path root;
    private MutableClock clock;
    private DailyFileJournalServiceConfig config;

    @Before
    public void setUp() throws IOException {
        root = Files.createTempDirectory("dailyfile-journal-test-");
        clock = new MutableClock(Instant.parse("2026-10-16T16:00:00.961Z"));
        config = new DailyFileJournalServiceConfig();
        config.zoneId = NEW_YORK.getId();
        config.fsyncIntervalMillis = 60_000;
        config.maxUnforcedRecords = 128;
        config.maxUnforcedBytes = 1024 * 1024;
        config.maxSegmentBytes = 1024 * 1024;
        config.maxSegmentAgeSeconds = 3600;
        config.maxRecordBytes = 1024 * 1024;
    }

    @After
    public void tearDown() throws IOException {
        if (root != null && Files.exists(root)) {
            try (var paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Test
    public void publicationIdAlwaysIncludesHashOfTheFullLaneUid() {
        String first = DailyFileJournalStore.publicationFileId("urn:site:east:Lane01");
        String second = DailyFileJournalStore.publicationFileId("urn:site:west:Lane01");

        assertTrue(first.matches("Lane01-[0-9a-f]{12}"));
        assertTrue(second.matches("Lane01-[0-9a-f]{12}"));
        assertNotEquals("Equal readable suffixes must not collide", first, second);
        assertTrue(DailyFileJournalStore.directoryId("lane".repeat(100)).length() < 255);
    }

    @Test
    public void pathLikeLaneIdentifiersCannotAliasJournalDirectories() throws Exception {
        try (DailyFileJournalStore store = newStore()) {
            assertThrows(IllegalArgumentException.class, () -> store.openWriter("."));
            assertThrows(IllegalArgumentException.class, () -> store.openWriter(".."));
        }
    }

    @Test
    public void publishesExactLegacyBytesAndVersionedChecksumManifest() throws Exception {
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        try (DailyFileJournalStore store = newStore()) {
            try (DailyFileWriterHandle writer = store.openWriter("urn:oscar:lane:Lane01")) {
                writer.append(new DailyFileRecord(timestamp, "GB,000188,000206,000262,000243"));
                writer.append(new DailyFileRecord(timestamp, "\"a\",b,\"c\"\"d\""));
            }

            clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
            store.sealCompletedDays(clock.instant());

            Path csv = publishedFile("urn:oscar:lane:Lane01", day);
            assertEquals("GB,000188,000206,000262,000243,12-00-00.961,16-00-00.961\r\n"
                            + "\"a\",b,\"c\"\"d\",12-00-00.961,16-00-00.961\r\n",
                    Files.readString(csv, StandardCharsets.UTF_8));

            Path manifest = Path.of(csv + ".manifest.v1.json");
            String json = Files.readString(manifest, StandardCharsets.UTF_8);
            assertTrue(json.contains("\"schemaVersion\":1"));
            assertTrue(json.contains("\"checksumAlgorithm\":\"SHA-256\""));
            assertTrue(json.contains("\"recordCount\":2"));
            DailyFileJournalStore.DayWindow window = DailyFileJournalStore.DayWindow.forDate(day, NEW_YORK);
            assertTrue(store.isPublished("urn:oscar:lane:Lane01", window.startInclusive(), window.endExclusive()));
        }
    }

    @Test
    public void usesImmutableDstAwareHalfOpenDayWindow() throws Exception {
        LocalDate springForward = LocalDate.of(2026, 3, 8);
        Instant noon = springForward.atTime(12, 0).atZone(NEW_YORK).toInstant();
        clock.set(noon);
        try (DailyFileJournalStore store = newStore()) {
            store.openWriter("lane-dst").append("A", noon);
            clock.set(springForward.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusMillis(1));
            store.sealCompletedDays(clock.instant());
        }

        String manifest = Files.readString(Path.of(
                publishedFile("lane-dst", springForward) + ".manifest.v1.json"));
        assertTrue(manifest.contains("\"windowStartInclusive\":\"2026-03-08T05:00:00Z\""));
        assertTrue(manifest.contains("\"windowEndExclusive\":\"2026-03-09T04:00:00Z\""));
        assertEquals(Duration.ofHours(23), Duration.between(
                springForward.atStartOfDay(NEW_YORK).toInstant(),
                springForward.plusDays(1).atStartOfDay(NEW_YORK).toInstant()));
    }

    @Test
    public void recoversAndTruncatesPartialTailBeforeAppending() throws Exception {
        Instant first = clock.instant();
        DailyFileJournalStore initial = newStore();
        initial.openWriter("lane-recovery").append("A", first);
        initial.close();

        Path open = onlyOpenSegment("lane-recovery", LocalDate.ofInstant(first, NEW_YORK));
        long validSize = Files.size(open);
        Files.write(open, ByteBuffer.allocate(Integer.BYTES).putInt(500).array(), StandardOpenOption.APPEND);
        assertEquals(validSize + Integer.BYTES, Files.size(open));

        try (DailyFileJournalStore recovered = newStore()) {
            assertEquals(validSize, Files.size(open));
            recovered.openWriter("lane-recovery").append("B", first.plusSeconds(1));
            clock.set(LocalDate.ofInstant(first, NEW_YORK).plusDays(1)
                    .atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
            recovered.sealCompletedDays(clock.instant());
        }

        assertEquals("A,12-00-00.961,16-00-00.961\r\n"
                        + "B,12-00-01.961,16-00-01.961\r\n",
                Files.readString(publishedFile("lane-recovery", LocalDate.of(2026, 10, 16))));
    }

    @Test
    public void refusesCompleteFrameWithBadCrc() throws Exception {
        Instant timestamp = clock.instant();
        DailyFileJournalStore initial = newStore();
        initial.openWriter("lane-crc").append("payload", timestamp);
        initial.close();

        Path open = onlyOpenSegment("lane-crc", LocalDate.ofInstant(timestamp, NEW_YORK));
        byte[] bytes = Files.readAllBytes(open);
        bytes[bytes.length - 1] ^= 0x01;
        Files.write(open, bytes);

        IOException failure = assertThrows(IOException.class, this::newStore);
        assertTrue(failure.getMessage().contains("CRC mismatch"));
    }

    @Test
    public void rotatesSegmentsBySizeWithoutDroppingRecords() throws Exception {
        config.maxSegmentBytes = 60;
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        try (DailyFileJournalStore store = newStore()) {
            DailyFileWriterHandle writer = store.openWriter("lane-rotation");
            writer.append("12345678901234567890", timestamp);
            writer.append("abcdefghijklmnopqrst", timestamp.plusMillis(1));

            Path dayDir = laneDirectory("lane-rotation").resolve(day.toString());
            try (var paths = Files.list(dayDir)) {
                assertEquals(1, paths.filter(path -> path.toString().endsWith(".seg")).count());
            }
            clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
            store.sealCompletedDays(clock.instant());
        }

        List<String> lines = Files.readAllLines(publishedFile("lane-rotation", day));
        assertEquals(2, lines.size());
    }

    @Test
    public void streamingPublisherCrcValidatesEverySealedFrame() throws Exception {
        config.maxSegmentBytes = 60;
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        try (DailyFileJournalStore store = newStore()) {
            DailyFileWriterHandle writer = store.openWriter("lane-streaming-crc");
            writer.append("12345678901234567890", timestamp);
            writer.append("abcdefghijklmnopqrst", timestamp.plusMillis(1));

            Path dayDirectory = laneDirectory("lane-streaming-crc").resolve(day.toString());
            Path sealed;
            try (var paths = Files.list(dayDirectory)) {
                sealed = paths.filter(path -> path.toString().endsWith(".seg"))
                        .findFirst().orElseThrow();
            }
            try (var segment = java.nio.channels.FileChannel.open(sealed,
                    StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                long payloadByte = DailyFileJournalStore.SEGMENT_HEADER_BYTES
                        + DailyFileJournalStore.FRAME_HEADER_BYTES
                        + DailyFileJournalStore.RECORD_TIMESTAMP_BYTES;
                ByteBuffer corrupted = ByteBuffer.allocate(1);
                segment.read(corrupted, payloadByte);
                corrupted.flip();
                corrupted.put(0, (byte) (corrupted.get(0) ^ 0x01));
                segment.write(corrupted, payloadByte);
                segment.force(true);
            }

            clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
            IOException failure = assertThrows(IOException.class,
                    () -> store.sealCompletedDays(clock.instant()));
            assertTrue(failure.getMessage().contains("CRC mismatch"));
            assertFalse(Files.exists(publishedFile("lane-streaming-crc", day)));
        }
    }

    @Test
    public void rotatesSegmentsByAgeWithoutDroppingRecords() throws Exception {
        config.maxSegmentAgeSeconds = 1;
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        try (DailyFileJournalStore store = newStore()) {
            DailyFileWriterHandle writer = store.openWriter("lane-age-rotation");
            writer.append("first", timestamp);
            writer.append("second", timestamp.plusSeconds(1));

            Path dayDir = laneDirectory("lane-age-rotation").resolve(day.toString());
            try (var paths = Files.list(dayDir)) {
                assertEquals(1, paths.filter(path -> path.toString().endsWith(".seg")).count());
            }
            clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
            store.sealCompletedDays(clock.instant());
        }

        assertEquals(2, Files.readAllLines(
                publishedFile("lane-age-rotation", day)).size());
    }

    @Test
    public void oversizedRecordIsRejectedAndNeverQueuedOrDroppedSilently() throws Exception {
        config.maxRecordBytes = 3;
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        try (DailyFileJournalStore store = newStore()) {
            DailyFileWriterHandle writer = store.openWriter("lane-backpressure");
            IOException failure = assertThrows(IOException.class,
                    () -> writer.append("four", timestamp));
            assertTrue(failure.getMessage().contains("configured maximum"));
            writer.append("ok", timestamp.plusMillis(1));
            clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
            store.sealCompletedDays(clock.instant());
        }
        String csv = Files.readString(publishedFile("lane-backpressure", day));
        assertFalse(csv.contains("four"));
        assertTrue(csv.startsWith("ok,"));
    }

    @Test
    public void recordThatCannotFitAnEmptySegmentIsRejectedExplicitly() throws Exception {
        config.maxSegmentBytes = 40;
        Instant timestamp = clock.instant();
        try (DailyFileJournalStore store = newStore()) {
            DailyFileWriterHandle writer = store.openWriter("lane-segment-bound");
            IOException failure = assertThrows(IOException.class,
                    () -> writer.append("payload-that-cannot-fit", timestamp));
            assertTrue(failure.getMessage().contains("segment bytes"));
        }
    }

    @Test
    public void serializesConcurrentWritersPerLaneAndKeepsLanesIndependent() throws Exception {
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        try (DailyFileJournalStore store = newStore()) {
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch start = new CountDownLatch(1);
            try {
                Future<?> laneA1 = executor.submit(() -> appendRange(store, "lane-A", "a", 0, 50, timestamp, start));
                Future<?> laneA2 = executor.submit(() -> appendRange(store, "lane-A", "b", 0, 50, timestamp, start));
                Future<?> laneB1 = executor.submit(() -> appendRange(store, "lane-B", "c", 0, 50, timestamp, start));
                start.countDown();
                laneA1.get();
                laneA2.get();
                laneB1.get();
            } finally {
                executor.shutdownNow();
            }
            clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
            store.sealCompletedDays(clock.instant());
        }

        assertEquals(100, Files.readAllLines(publishedFile("lane-A", day)).size());
        assertEquals(50, Files.readAllLines(publishedFile("lane-B", day)).size());
    }

    @Test
    public void immutablePublicationMismatchFailsRecovery() throws Exception {
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore store = newStore();
        store.openWriter("lane-immutable").append("original", timestamp);
        clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
        store.sealCompletedDays(clock.instant());
        store.close();

        Path csv = publishedFile("lane-immutable", day);
        Files.writeString(csv, "tampered\r\n", StandardOpenOption.TRUNCATE_EXISTING);
        IOException failure = assertThrows(IOException.class, this::newStore);
        assertTrue(failure.getMessage().contains("Refusing to replace immutable daily file"));
    }

    @Test
    public void publicationStatusVerifiesPersistedLedgerManifestAndFileBytes() throws Exception {
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore.DayWindow window = DailyFileJournalStore.DayWindow.forDate(day, NEW_YORK);
        DailyFileJournalStore first = newStore();
        first.openWriter("lane-verification").append("verified", timestamp);
        clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
        first.sealCompletedDays(clock.instant());
        first.close();

        try (DailyFileJournalStore recovered = newStore()) {
            assertTrue(recovered.isPublished("lane-verification",
                    window.startInclusive(), window.endExclusive()));
            Path manifest = Path.of(publishedFile("lane-verification", day)
                    + ".manifest.v1.json");
            String originalManifest = Files.readString(manifest, StandardCharsets.UTF_8);
            Files.writeString(manifest, originalManifest.replace("\"recordCount\":1",
                    "\"recordCount\":999"), StandardOpenOption.TRUNCATE_EXISTING);
            assertFalse("Manifest counts must be parsed and matched, not substring-tested",
                    recovered.isPublished("lane-verification",
                            window.startInclusive(), window.endExclusive()));
            Files.writeString(manifest, originalManifest, StandardOpenOption.TRUNCATE_EXISTING);
            assertTrue(recovered.isPublished("lane-verification",
                    window.startInclusive(), window.endExclusive()));
            Files.writeString(publishedFile("lane-verification", day),
                    "tampered\r\n", StandardOpenOption.TRUNCATE_EXISTING);
            assertFalse("A persisted ledger alone must not authorize database deletion",
                    recovered.isPublished("lane-verification",
                            window.startInclusive(), window.endExclusive()));
        }
    }

    @Test
    public void rejectsSymlinkedJournalDirectories() throws Exception {
        Path actual = Files.createDirectory(root.resolve("journal-outside"));
        Files.createSymbolicLink(root.resolve(config.journalDirectory), actual);

        IOException failure = assertThrows(IOException.class, this::newStore);
        assertTrue(failure.getMessage().contains("linked or non-directory"));
    }

    @Test
    public void rejectsOverlappingPrivateJournalAndPublicArtifactDirectories() {
        config.journalDirectory = "dailyfiles/private-journal";

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                this::newStore);
        assertTrue(failure.getMessage().contains("must not overlap"));
    }

    @Test
    public void restartRejectsAZoneChangeThatWouldRepartitionJournalDays() throws Exception {
        try (DailyFileJournalStore ignored = newStore()) {
            // Initial creation durably binds the calendar zone before accepting records.
        }
        config.zoneId = "UTC";

        IOException failure = assertThrows(IOException.class, this::newStore);
        assertTrue(failure.getMessage().contains("does not match the existing journal metadata"));
    }

    @Test
    public void sealedJournalRetentionRequiresMatchingBackupConfirmation() throws Exception {
        config.sealedJournalRetentionDays = 1;
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore.DayWindow window =
                DailyFileJournalStore.DayWindow.forDate(day, NEW_YORK);

        try (DailyFileJournalStore store = newStore()) {
            store.openWriter("lane-backed-up").append("backed-up", timestamp);
            store.openWriter("lane-not-backed-up").append("not-backed-up", timestamp);
            clock.set(window.endExclusive().plusSeconds(1));
            store.sealCompletedDays(clock.instant());

            Path backedUpCsv = publishedFile("lane-backed-up", day);
            store.confirmPublishedBackup("lane-backed-up", window.startInclusive(),
                    window.endExclusive(), DailyFileJournalStore.sha256(backedUpCsv),
                    "s3://verified-backup/site/day");
            assertTrue(store.isBackupConfirmed("lane-backed-up", window.startInclusive(),
                    window.endExclusive()));

            clock.set(window.endExclusive().plus(Duration.ofDays(1)).plusSeconds(1));
            Path backedUpDay = laneDirectory("lane-backed-up").resolve(day.toString());
            Path sealed;
            try (var paths = Files.list(backedUpDay)) {
                sealed = paths.filter(path -> path.toString().endsWith(".seg"))
                        .findFirst().orElseThrow();
            }
            Path lateSegment = backedUpDay.resolve("late-commit.seg");
            Files.copy(sealed, lateSegment);
            assertFalse("A late segment absent from the confirmed backup invalidates the proof",
                    store.isBackupConfirmed("lane-backed-up", window.startInclusive(),
                            window.endExclusive()));
            store.sealCompletedDays(clock.instant());
            assertTrue("Mismatched segment inventory must block journal pruning",
                    Files.isDirectory(backedUpDay));

            Files.delete(lateSegment);
            assertTrue(store.isBackupConfirmed("lane-backed-up", window.startInclusive(),
                    window.endExclusive()));
            store.sealCompletedDays(clock.instant());

            assertFalse("Confirmed sealed segments may be removed after retention",
                    Files.exists(backedUpDay));
            assertTrue("Unconfirmed sealed segments must be retained indefinitely",
                    Files.isDirectory(laneDirectory("lane-not-backed-up").resolve(day.toString())));
            assertTrue("Public CSV remains immutable after private segment expiry",
                    Files.isRegularFile(backedUpCsv));
        }
    }

    @Test
    public void confirmationSetWriteFailureCannotPartiallyAuthorizePruning() throws Exception {
        String firstLane = "lane-a-confirmation-set";
        String secondLane = "lane-z-confirmation-set";
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore.DayWindow window =
                DailyFileJournalStore.DayWindow.forDate(day, NEW_YORK);

        try (DailyFileJournalStore store = newStore()) {
            store.openWriter(firstLane).append("first", timestamp);
            store.openWriter(secondLane).append("second", timestamp.plusMillis(1));
            clock.set(window.endExclusive().plusSeconds(1));
            store.sealCompletedDays(clock.instant());

            Path firstDay = laneDirectory(firstLane).resolve(day.toString());
            Path secondDay = laneDirectory(secondLane).resolve(day.toString());
            DailyFileJournalStore.PreparedBackupConfirmation first =
                    store.preparePublishedBackupConfirmation(firstLane,
                            window.startInclusive(), window.endExclusive(),
                            DailyFileJournalStore.sha256(publishedFile(firstLane, day)),
                            "oscar-backup:verified-first",
                            DailyFileJournalStore.sealedSegmentInventory(firstDay));
            DailyFileJournalStore.PreparedBackupConfirmation second =
                    store.preparePublishedBackupConfirmation(secondLane,
                            window.startInclusive(), window.endExclusive(),
                            DailyFileJournalStore.sha256(publishedFile(secondLane, day)),
                            "oscar-backup:verified-first",
                            DailyFileJournalStore.sealedSegmentInventory(secondDay));

            Path blockedParent = second.target().getParent();
            Files.delete(blockedParent);
            Files.writeString(blockedParent, "not-a-directory", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            assertThrows(IOException.class, () -> store.commitPreparedBackupConfirmations(
                    "verified-first", List.of(first, second)));

            assertTrue("The fixture must fail after at least one receipt was made durable",
                    Files.isRegularFile(first.target()));
            assertFalse(Files.exists(root.resolve(
                    ".dailyfile-journal/backup-confirmations/.sets/verified-first.json")));
            assertFalse("An uncommitted receipt must never authorize pruning",
                    store.isBackupConfirmed(firstLane,
                            window.startInclusive(), window.endExclusive()));
            assertFalse(store.isBackupConfirmed(secondLane,
                    window.startInclusive(), window.endExclusive()));
        }
    }

    @Test
    public void confirmationSetBindsEveryReceiptAndRejectsUncommittedFormats() throws Exception {
        String firstLane = "lane-bound-first";
        String secondLane = "lane-bound-second";
        String forgedLane = "lane-bound-forged";
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore.DayWindow window =
                DailyFileJournalStore.DayWindow.forDate(day, NEW_YORK);

        try (DailyFileJournalStore store = newStore()) {
            for (String lane : List.of(firstLane, secondLane, forgedLane))
                store.openWriter(lane).append(lane, timestamp);
            clock.set(window.endExclusive().plusSeconds(1));
            store.sealCompletedDays(clock.instant());

            DailyFileJournalStore.PreparedBackupConfirmation first = prepareConfirmation(
                    store, firstLane, day, window, "oscar-backup:bound-set");
            DailyFileJournalStore.PreparedBackupConfirmation second = prepareConfirmation(
                    store, secondLane, day, window, "oscar-backup:bound-set");
            DailyFileJournalStore.PreparedBackupConfirmation forged = prepareConfirmation(
                    store, forgedLane, day, window, "oscar-backup:bound-set");
            store.commitPreparedBackupConfirmations("bound-set", List.of(first, second));
            assertTrue(store.isBackupConfirmed(firstLane,
                    window.startInclusive(), window.endExclusive()));
            assertTrue(store.isBackupConfirmed(secondLane,
                    window.startInclusive(), window.endExclusive()));

            String oldFormat = forged.json("unused-set", clock.instant())
                    .replace("  \"confirmationSet\":\"unused-set\",\n", "");
            Files.writeString(forged.target(), oldFormat, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            assertFalse("A receipt without a committed confirmation set is never trusted",
                    store.isBackupConfirmed(forgedLane,
                            window.startInclusive(), window.endExclusive()));

            Files.writeString(forged.target(), forged.json("bound-set", clock.instant()),
                    StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            assertFalse("Copying a valid set ID cannot forge set membership",
                    store.isBackupConfirmed(forgedLane,
                            window.startInclusive(), window.endExclusive()));

            Files.delete(second.target());
            assertTrue("An immutable set index keeps an independently valid member usable",
                    store.isBackupConfirmed(firstLane,
                            window.startInclusive(), window.endExclusive()));
            assertFalse(store.isBackupConfirmed(secondLane,
                    window.startInclusive(), window.endExclusive()));

            Files.delete(first.target().getParent().getParent()
                    .resolve(".sets/bound-set.json"));
            assertFalse("A cached index must fail closed when its commit marker disappears",
                    store.isBackupConfirmed(firstLane,
                            window.startInclusive(), window.endExclusive()));
        }
    }

    private DailyFileJournalStore.PreparedBackupConfirmation prepareConfirmation(
            DailyFileJournalStore store, String lane, LocalDate day,
            DailyFileJournalStore.DayWindow window, String backupReference) throws IOException {
        return store.preparePublishedBackupConfirmation(lane,
                window.startInclusive(), window.endExclusive(),
                DailyFileJournalStore.sha256(publishedFile(lane, day)), backupReference,
                DailyFileJournalStore.sealedSegmentInventory(
                        laneDirectory(lane).resolve(day.toString())));
    }

    @Test
    public void largeConfirmationSetIndexIsParsedOnlyOncePerStore() throws Exception {
        String lane = "lane-scale-confirmation-set";
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore.DayWindow window =
                DailyFileJournalStore.DayWindow.forDate(day, NEW_YORK);

        try (DailyFileJournalStore store = newStore()) {
            store.openWriter(lane).append("scale", timestamp);
            clock.set(window.endExclusive().plusSeconds(1));
            store.sealCompletedDays(clock.instant());
            DailyFileJournalStore.PreparedBackupConfirmation prepared = prepareConfirmation(
                    store, lane, day, window, "oscar-backup:scale-set");
            store.commitPreparedBackupConfirmations("scale-set", List.of(prepared));

            Path confirmationRoot = prepared.target().getParent().getParent();
            String currentPath = confirmationRoot.relativize(prepared.target())
                    .toString().replace('\\', '/');
            String currentSha = DailyFileJournalStore.sha256(prepared.target());
            List<String> members = new java.util.ArrayList<>(18_000);
            for (int index = 0; index < 17_999; index++) {
                members.add(String.format("bulk%05d/2026-01-01.json|%s", index,
                        "a".repeat(64)));
            }
            members.add(currentPath + "|" + currentSha);
            members.sort(String::compareTo);
            StringBuilder marker = new StringBuilder("{\n")
                    .append("  \"schemaVersion\":1,\n")
                    .append("  \"format\":\"oscar-dailyfile-backup-confirmation-set\",\n")
                    .append("  \"confirmationSet\":\"scale-set\",\n")
                    .append("  \"receiptCount\":18000,\n")
                    .append("  \"committedAt\":\"").append(clock.instant()).append("\",\n")
                    .append("  \"receipts\":[\n");
            for (int index = 0; index < members.size(); index++) {
                String[] member = members.get(index).split("\\|", 2);
                marker.append("    {\"path\":\"").append(member[0])
                        .append("\",\"sha256\":\"").append(member[1]).append("\"}");
                if (index + 1 < members.size())
                    marker.append(',');
                marker.append('\n');
            }
            marker.append("  ]\n}\n");
            Files.writeString(confirmationRoot.resolve(".sets/scale-set.json"), marker,
                    StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            assertEquals(18_000,
                    store.confirmationSetIndex("scale-set").receiptSha256().size());
            for (int pass = 0; pass < 32; pass++) {
                assertTrue(store.isBackupConfirmed(lane,
                        window.startInclusive(), window.endExclusive()));
            }
            assertEquals("A large immutable marker must not be reparsed for every lane/day",
                    1, store.confirmationSetMarkerLoadCount());
        }
    }

    @Test
    public void interruptedSealedJournalDeletionResumesWithoutRepublishingPartialDay()
            throws Exception {
        config.sealedJournalRetentionDays = 1;
        config.maxSegmentBytes = 60;
        String laneId = "lane-purge-recovery";
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore.DayWindow window =
                DailyFileJournalStore.DayWindow.forDate(day, NEW_YORK);

        try (DailyFileJournalStore store = newStore()) {
            DailyFileWriterHandle writer = store.openWriter(laneId);
            writer.append("12345678901234567890", timestamp);
            writer.append("abcdefghijklmnopqrst", timestamp.plusMillis(1));
            clock.set(window.endExclusive().plusSeconds(1));
            store.sealCompletedDays(clock.instant());
            Path csv = publishedFile(laneId, day);
            store.confirmPublishedBackup(laneId, window.startInclusive(), window.endExclusive(),
                    DailyFileJournalStore.sha256(csv), "s3://verified-backup/purge-recovery");
        }

        Path dayDirectory = laneDirectory(laneId).resolve(day.toString());
        Path staged = dayDirectory.resolveSibling(
                DailyFileJournalStore.deletionStagingName(day));
        Files.move(dayDirectory, staged, StandardCopyOption.ATOMIC_MOVE);
        List<Path> sealed;
        try (var paths = Files.list(staged)) {
            sealed = paths.filter(path -> path.toString().endsWith(".seg")).toList();
        }
        assertTrue("Fixture must exercise a partially deleted multi-segment day",
                sealed.size() >= 2);
        Files.delete(sealed.get(0));

        try (DailyFileJournalStore ignored = newStore()) {
            assertFalse("Restart must finish the durable deletion state",
                    Files.exists(staged));
            assertFalse(Files.exists(dayDirectory));
            assertTrue("Immutable publication must survive private journal deletion",
                    Files.isRegularFile(publishedFile(laneId, day)));
        }
    }

    @Test
    public void boundedLifecycleDoesNotReprocessCompleteHistoricalPublications()
            throws Exception {
        String laneId = "lane-bounded-maintenance";
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore.DayWindow window =
                DailyFileJournalStore.DayWindow.forDate(day, NEW_YORK);
        try (DailyFileJournalStore initial = newStore()) {
            initial.openWriter(laneId).append("published-once", timestamp);
            clock.set(window.endExclusive().plusSeconds(1));
            initial.sealCompletedDays(clock.instant());
        }

        try (DailyFileJournalStore recovered = newStore()) {
            Path dayDirectory = laneDirectory(laneId).resolve(day.toString());
            Path segment;
            try (var paths = Files.list(dayDirectory)) {
                segment = paths.filter(path -> path.toString().endsWith(".seg"))
                        .findFirst().orElseThrow();
            }
            Files.write(segment, new byte[]{0x01}, StandardOpenOption.APPEND);

            // The startup recovery above already verified the immutable CSV against the journal.
            // A bounded periodic pass must use the durable publication record rather than reading
            // and rendering the complete historical segment again.
            recovered.maintainBoundedLifecycle(clock.instant());
            assertTrue(Files.readString(publishedFile(laneId, day))
                    .startsWith("published-once,"));
        }
    }

    @Test
    public void boundedLifecycleCapsAndAdvancesHistoricalDayWork() throws Exception {
        String laneId = "lane-bounded-work";
        LocalDate today = LocalDate.ofInstant(clock.instant(), NEW_YORK);
        Path builderRoot = root.resolve("maintenance-builder");

        try (DailyFileJournalStore live = newStore()) {
            try (DailyFileJournalStore builder = new DailyFileJournalStore(builderRoot, config,
                    clock, LoggerFactory.getLogger(getClass()), false)) {
                DailyFileWriterHandle writer = builder.openWriter(laneId);
                for (int daysAgo = 17; daysAgo >= 1; daysAgo--) {
                    Instant timestamp = today.minusDays(daysAgo).atTime(12, 0)
                            .atZone(NEW_YORK).toInstant();
                    writer.append("day-" + daysAgo, timestamp);
                }
                builder.sealCompletedDays(clock.instant());
            }

            Path sourceLane = builderRoot.resolve(".dailyfile-journal/segments")
                    .resolve(DailyFileJournalStore.directoryId(laneId));
            Path destinationLane = root.resolve(".dailyfile-journal/segments")
                    .resolve(DailyFileJournalStore.directoryId(laneId));
            copyTree(sourceLane, destinationLane);

            live.maintainBoundedLifecycle(clock.instant());
            try (var paths = Files.list(root.resolve("dailyfiles"))) {
                assertEquals(DailyFileJournalStore.LIFECYCLE_MAINTENANCE_DAY_LIMIT,
                        paths.filter(path -> path.getFileName().toString()
                                        .startsWith(DailyFileJournalStore.publicationFileId(laneId) + "_"))
                                .filter(path -> path.getFileName().toString().endsWith(".csv"))
                                .count());
            }

            live.maintainBoundedLifecycle(clock.instant());
            try (var paths = Files.list(root.resolve("dailyfiles"))) {
                assertEquals(17, paths.filter(path -> path.getFileName().toString()
                                        .startsWith(DailyFileJournalStore.publicationFileId(laneId) + "_"))
                                .filter(path -> path.getFileName().toString().endsWith(".csv"))
                                .count());
            }
        }
    }

    @Test
    public void observationPurgeCheckpointIsDurableMonotonicAndFeedBound() throws Exception {
        String feedId = "urn:oscar:lane:checkpoint#dailyFile";
        LocalDate processedThrough = LocalDate.of(2026, 10, 14);
        try (DailyFileJournalStore store = newStore()) {
            assertEquals(java.util.Optional.empty(),
                    store.readObservationPurgeCheckpoint(feedId));
            store.writeObservationPurgeCheckpoint(feedId, processedThrough);
            assertEquals(java.util.Optional.of(processedThrough),
                    store.readObservationPurgeCheckpoint(feedId));
            assertThrows(IOException.class, () -> store.writeObservationPurgeCheckpoint(
                    feedId, processedThrough.minusDays(1)));
        }

        try (DailyFileJournalStore restarted = newStore()) {
            assertEquals(java.util.Optional.of(processedThrough),
                    restarted.readObservationPurgeCheckpoint(feedId));
        }
    }

    @Test
    public void retriesParentBarrierForEveryCreatedDirectoryComponent() throws Exception {
        Path confinement = Files.createDirectories(root.resolve("durable-directory-root"));
        Path target = confinement.resolve("first/second/third");
        Set<Path> durableEntries = new HashSet<>();
        List<Path> forcedDirectories = new ArrayList<>();
        AtomicBoolean failSecondBarrier = new AtomicBoolean(true);

        IOException failure = assertThrows(IOException.class,
                () -> DailyFileJournalStore.createConfinedDirectories(
                        confinement, target, durableEntries, directory -> {
                            forcedDirectories.add(directory);
                            if (directory.equals(confinement.resolve("first"))
                                    && failSecondBarrier.getAndSet(false)) {
                                throw new IOException("injected directory barrier failure");
                            }
                        }));
        assertTrue(failure.getMessage().contains("injected"));
        assertTrue(Files.isDirectory(confinement.resolve("first/second")));
        assertFalse(Files.exists(target));

        DailyFileJournalStore.createConfinedDirectories(
                confinement, target, durableEntries, forcedDirectories::add);

        assertEquals(List.of(
                confinement,
                confinement.resolve("first"),
                confinement.resolve("first"),
                confinement.resolve("first/second")), forcedDirectories);
        assertTrue(Files.isDirectory(target));
    }

    @Test
    public void accessDeniedDirectoryBarrierIsVisibleOnPosixAndIgnoredOnWindows() {
        AccessDeniedException denied = new AccessDeniedException("directory");

        assertFalse(DailyFileJournalStore.isIgnorableWindowsDirectoryFsyncFailure(
                denied, false));
        assertTrue(DailyFileJournalStore.isIgnorableWindowsDirectoryFsyncFailure(
                denied, true));
        assertFalse(DailyFileJournalStore.isIgnorableWindowsDirectoryFsyncFailure(
                new IOException("other failure"), true));
    }

    @Test
    public void schedulerShutdownReturnsWithinItsBoundAndCanBeRetried()
            throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        scheduler.execute(() -> {
            started.countDown();
            while (release.getCount() != 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    // Deliberately model filesystem work that does not respond to interruption.
                }
            }
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));

        long startedAt = System.nanoTime();
        try {
            assertFalse(DailyFileJournalStore.shutdownSchedulersBounded(
                    scheduler, null, 100));
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startedAt);
            assertTrue("Shutdown exceeded its bounded wait: " + elapsedMillis + " ms",
                    elapsedMillis < 2_000);
        } finally {
            release.countDown();
            assertTrue(DailyFileJournalStore.shutdownSchedulersBounded(
                    scheduler, null, 2_000));
        }
    }

    @Test
    public void serviceShutdownDrainsActiveWriterHandlesBeforeClosingLanes() throws Exception {
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore store = new DailyFileJournalStore(
                root, config, LoggerFactory.getLogger(getClass()));
        DailyFileWriterHandle writer = store.openWriter("lane-shutdown-drain");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> close = executor.submit(() -> {
            try {
                store.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            boolean closingObserved = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (System.nanoTime() < deadline) {
                try (DailyFileWriterHandle probe = store.openWriter("lane-shutdown-probe")) {
                    // Closing has not claimed the writer gate yet; release the probe immediately.
                } catch (IOException e) {
                    closingObserved = e.getMessage().contains("closing");
                    if (closingObserved)
                        break;
                    throw e;
                }
                Thread.yield();
            }
            assertTrue("The service close must stop admitting new writers", closingObserved);
            assertFalse("The store must wait for the active sensor writer", close.isDone());

            writer.append("last-record-before-stop", timestamp);
            writer.close();
            close.get(2, TimeUnit.SECONDS);
        } finally {
            try {
                writer.close();
            } finally {
                executor.shutdownNow();
                if (!close.isDone())
                    close.get(2, TimeUnit.SECONDS);
            }
        }

        clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
        try (DailyFileJournalStore recovered = newStore()) {
            recovered.sealCompletedDays(clock.instant());
        }
        assertTrue(Files.readString(publishedFile("lane-shutdown-drain", day))
                .startsWith("last-record-before-stop,"));
    }

    @Test
    public void timedOutWriterDrainCanBeRetriedWithoutLosingFinalRecord() throws Exception {
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore store = new DailyFileJournalStore(
                root, config, LoggerFactory.getLogger(getClass()));
        DailyFileWriterHandle writer = store.openWriter("lane-shutdown-retry");

        IOException timeout = assertThrows(IOException.class, () -> store.closeWithin(50));
        assertTrue(timeout.getMessage().contains("writers did not close"));
        assertTrue("A timed-out writer drain must retain periodic fsync",
                store.maintenanceSchedulersRunning());
        writer.append("record-after-first-close-attempt", timestamp);
        writer.close();
        store.closeWithin(2_000);
        assertThrows(IOException.class, () -> store.openWriter("lane-shutdown-retry"));

        clock.set(day.plusDays(1).atStartOfDay(NEW_YORK).toInstant().plusSeconds(1));
        try (DailyFileJournalStore recovered = newStore()) {
            recovered.sealCompletedDays(clock.instant());
        }
        assertTrue(Files.readString(publishedFile("lane-shutdown-retry", day))
                .startsWith("record-after-first-close-attempt,"));
    }

    @Test
    public void writerAdmissionContentionRespectsCloseDeadline() throws Exception {
        DailyFileJournalStore store = new DailyFileJournalStore(
                root, config, LoggerFactory.getLogger(getClass()));
        Field field = DailyFileJournalStore.class.getDeclaredField("writerHandleLock");
        field.setAccessible(true);
        ReentrantLock writerGate = (ReentrantLock) field.get(store);
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> holder = executor.submit(() -> {
            writerGate.lock();
            try {
                acquired.countDown();
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writerGate.unlock();
            }
        });
        try {
            assertTrue(acquired.await(1, TimeUnit.SECONDS));
            long started = System.nanoTime();
            IOException timeout = assertThrows(IOException.class,
                    () -> store.closeWithin(75));
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertTrue(timeout.getMessage().contains("writer admission"));
            assertTrue("Close must not wait indefinitely for the writer admission lock",
                    elapsedMillis < 1_000L);
        } finally {
            release.countDown();
            holder.get(1, TimeUnit.SECONDS);
            executor.shutdownNow();
            store.closeWithin(2_000);
        }
    }

    @Test
    public void verifiedSnapshotCliRejectsSymlinkedStorageAncestor() throws Exception {
        Path liveState = Files.createDirectories(root.resolve("cli-link-live"));
        Path liveStorage = liveState.resolve("nested/files");
        try (DailyFileJournalStore ignored = new DailyFileJournalStore(liveStorage, config,
                clock, LoggerFactory.getLogger(getClass()), false)) {
            // Initialize a valid, empty live journal tree.
        }
        Files.writeString(liveState.resolve("config.json"), """
                [{
                  "objClass":"com.botts.impl.service.dailyfile.DailyFileJournalServiceConfig",
                  "rootDirectory":"nested/files",
                  "journalDirectory":".dailyfile-journal",
                  "publicationDirectory":"dailyfiles",
                  "zoneId":"America/New_York",
                  "dailyFileObservationMode":"FULL",
                  "fsyncIntervalMillis":60000,
                  "maxUnforcedRecords":128,
                  "maxUnforcedBytes":1048576,
                  "maxSegmentBytes":67108864,
                  "maxSegmentAgeSeconds":900,
                  "maxRecordBytes":4194304,
                  "sealedJournalRetentionDays":0,
                  "moduleClass":"com.botts.impl.service.dailyfile.DailyFileJournalService"
                }]
                """, StandardCharsets.UTF_8);

        Path snapshotState = Files.createDirectories(root.resolve("cli-link-snapshot"));
        Path outside = Files.createDirectories(root.resolve("cli-link-outside/files"))
                .getParent();
        try {
            Files.createSymbolicLink(snapshotState.resolve("nested"), outside);
        } catch (UnsupportedOperationException | IOException e) {
            return;
        }

        IOException failure = assertThrows(IOException.class,
                () -> DailyFileBackupConfirmationCli.confirmVerifiedSnapshot(
                        liveState, snapshotState, "20261017T120000Z-0123abcd",
                        "a".repeat(64)));
        assertTrue(failure.getMessage().contains("symbolic link"));
        assertTrue(Files.isDirectory(outside.resolve("files")));
    }

    @Test
    public void verifiedSnapshotCliConfirmsOnlyPublishedDaysWithSealedSegments() throws Exception {
        Path liveState = Files.createDirectories(root.resolve("cli-live"));
        Path liveStorage = Files.createDirectories(liveState.resolve("files"));
        DailyFileJournalServiceConfig cliConfig = new DailyFileJournalServiceConfig();
        cliConfig.zoneId = NEW_YORK.getId();
        cliConfig.fsyncIntervalMillis = 60_000;
        cliConfig.sealedJournalRetentionDays = 0;
        Files.writeString(liveState.resolve("config.json"), """
                [{
                  "objClass":"com.botts.impl.service.dailyfile.DailyFileJournalServiceConfig",
                  "rootDirectory":"files",
                  "journalDirectory":".dailyfile-journal",
                  "publicationDirectory":"dailyfiles",
                  "zoneId":"America/New_York",
                  "dailyFileObservationMode":"FULL",
                  "fsyncIntervalMillis":60000,
                  "maxUnforcedRecords":128,
                  "maxUnforcedBytes":1048576,
                  "maxSegmentBytes":67108864,
                  "maxSegmentAgeSeconds":900,
                  "maxRecordBytes":4194304,
                  "sealedJournalRetentionDays":0,
                  "moduleClass":"com.botts.impl.service.dailyfile.DailyFileJournalService"
                }]
                """, StandardCharsets.UTF_8);

        String laneId = "urn:oscar:lane:backup-cli";
        String secondLaneId = "urn:oscar:lane:backup-cli-second";
        Instant timestamp = clock.instant();
        LocalDate day = LocalDate.ofInstant(timestamp, NEW_YORK);
        DailyFileJournalStore.DayWindow window =
                DailyFileJournalStore.DayWindow.forDate(day, NEW_YORK);
        try (DailyFileJournalStore live = new DailyFileJournalStore(liveStorage, cliConfig,
                clock, LoggerFactory.getLogger(getClass()), false)) {
            live.openWriter(laneId).append("captured", timestamp);
            live.openWriter(secondLaneId).append("captured-second", timestamp.plusMillis(1));
            clock.set(window.endExclusive().plusSeconds(1));
            live.sealCompletedDays(clock.instant());
        }

        Path snapshotState = root.resolve("cli-snapshot");
        copyTree(liveState, snapshotState);
        String archiveHash = "a".repeat(64);
        Path liveDay = liveStorage.resolve(".dailyfile-journal/segments")
                .resolve(DailyFileJournalStore.directoryId(laneId)).resolve(day.toString());
        Path existingSegment;
        try (var paths = Files.list(liveDay)) {
            existingSegment = paths.filter(path -> path.toString().endsWith(".seg"))
                    .findFirst().orElseThrow();
        }
        Path lateSegment = liveDay.resolve("late-after-snapshot.seg");
        Files.write(lateSegment, java.util.Arrays.copyOf(Files.readAllBytes(existingSegment),
                DailyFileJournalStore.SEGMENT_HEADER_BYTES), StandardOpenOption.CREATE_NEW);
        IOException mismatch = assertThrows(IOException.class,
                () -> DailyFileBackupConfirmationCli.confirmVerifiedSnapshot(
                        liveState, snapshotState, "20261017T120000Z-0123abcd", archiveHash));
        assertTrue(mismatch.getMessage().contains("inventory differs"));
        Path confirmationRoot = liveStorage.resolve(
                ".dailyfile-journal/backup-confirmations");
        Path confirmation = confirmationRoot.resolve(DailyFileJournalStore.directoryId(laneId))
                .resolve(day + ".json");
        Path secondConfirmation = confirmationRoot.resolve(
                DailyFileJournalStore.directoryId(secondLaneId)).resolve(day + ".json");
        Path confirmationSet = confirmationRoot.resolve(
                ".sets/20261017T120000Z-0123abcd.json");
        assertFalse("A mismatch in any lane must leave every confirmation uncommitted",
                Files.exists(confirmation));
        assertFalse(Files.exists(secondConfirmation));
        assertFalse(Files.exists(confirmationSet));
        Files.delete(lateSegment);

        assertEquals(2, DailyFileBackupConfirmationCli.confirmVerifiedSnapshot(
                liveState, snapshotState, "20261017T120000Z-0123abcd", archiveHash));
        try (DailyFileJournalStore reopened = new DailyFileJournalStore(liveStorage, cliConfig,
                clock, LoggerFactory.getLogger(getClass()), false)) {
            assertTrue(reopened.isBackupConfirmed(laneId,
                    window.startInclusive(), window.endExclusive()));
            assertTrue(reopened.isBackupConfirmed(secondLaneId,
                    window.startInclusive(), window.endExclusive()));
        }

        assertTrue(Files.isRegularFile(confirmationSet));
        assertTrue(Files.readString(confirmation).contains(
                "oscar-backup:20261017T120000Z-0123abcd/oscar-state.tar.gz#sha256="
                        + archiveHash));
        Files.delete(confirmationSet);
        try (DailyFileJournalStore reopened = new DailyFileJournalStore(liveStorage, cliConfig,
                clock, LoggerFactory.getLogger(getClass()), false)) {
            assertFalse("Receipts cannot authorize pruning without the atomic set marker",
                    reopened.isBackupConfirmed(laneId,
                            window.startInclusive(), window.endExclusive()));
            assertFalse(reopened.isBackupConfirmed(secondLaneId,
                    window.startInclusive(), window.endExclusive()));
        }
        assertEquals(2, DailyFileBackupConfirmationCli.confirmVerifiedSnapshot(
                liveState, snapshotState, "20261017T120000Z-0123abcd", archiveHash));

        Files.delete(confirmation);
        Files.delete(secondConfirmation);
        Path snapshotDay = snapshotState.resolve("files/.dailyfile-journal/segments")
                .resolve(DailyFileJournalStore.directoryId(laneId)).resolve(day.toString());
        try (var paths = Files.walk(snapshotDay)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Path secondSnapshotDay = snapshotState.resolve("files/.dailyfile-journal/segments")
                .resolve(DailyFileJournalStore.directoryId(secondLaneId)).resolve(day.toString());
        try (var paths = Files.walk(secondSnapshotDay)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertEquals(0, DailyFileBackupConfirmationCli.confirmVerifiedSnapshot(
                liveState, snapshotState, "20261017T120000Z-0123abce", archiveHash));
        assertFalse("A backup without the sealed source segments must not authorize pruning",
                Files.exists(confirmation));
    }

    private void appendRange(DailyFileJournalStore store, String lane, String prefix, int from, int to,
                             Instant timestamp, CountDownLatch start) {
        try {
            start.await();
            DailyFileWriterHandle writer = store.openWriter(lane);
            for (int i = from; i < to; i++)
                writer.append(prefix + i, timestamp.plusNanos(i));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DailyFileJournalStore newStore() throws IOException {
        return new DailyFileJournalStore(root, config, clock,
                LoggerFactory.getLogger(DailyFileJournalStoreTest.class), false);
    }

    private Path publishedFile(String laneId, LocalDate day) {
        return root.resolve("dailyfiles/" + DailyFileJournalStore.publicationFileId(laneId)
                + "_" + day + ".csv");
    }

    private Path onlyOpenSegment(String lane, LocalDate day) throws IOException {
        try (var paths = Files.list(laneDirectory(lane).resolve(day.toString()))) {
            return paths.filter(path -> path.toString().endsWith(".open")).findFirst().orElseThrow();
        }
    }

    private Path laneDirectory(String lane) throws IOException {
        Path segments = root.resolve(".dailyfile-journal/segments");
        try (var paths = Files.list(segments)) {
            return paths.filter(Files::isDirectory)
                    .filter(path -> {
                        try {
                            return Files.readString(path.resolve("lane.id")).equals(lane);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .findFirst().orElseThrow();
        }
    }

    private static void copyTree(Path source, Path destination) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path));
                if (Files.isDirectory(path))
                    Files.createDirectories(target);
                else
                    Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
    }

    private static final class MutableClock extends Clock {
        private volatile Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
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
