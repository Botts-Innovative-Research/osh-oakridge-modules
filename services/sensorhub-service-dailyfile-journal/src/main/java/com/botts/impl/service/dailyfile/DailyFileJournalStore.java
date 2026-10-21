package com.botts.impl.service.dailyfile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileObservationMode;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileRecord;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileWriterHandle;
import org.sensorhub.impl.utils.rad.dailyfile.IDailyFileJournalService;
import org.slf4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.CRC32C;

/**
 * Filesystem implementation of the direct daily-file journal.
 *
 * <p>Each accepted record is synchronously appended to a per-lane {@code .open}
 * segment as {@code length + CRC32C + payload}. Periodic and volume-based
 * forces bound the amount of data exposed to a power loss. Completed days are
 * rendered into the legacy CSV syntax and published by atomic rename, with a
 * versioned SHA-256 manifest published last.</p>
 */
public final class DailyFileJournalStore implements IDailyFileJournalService, AutoCloseable {

    static final int SEGMENT_MAGIC = 0x444A4E4C; // DJNL
    static final int SEGMENT_VERSION = 1;
    static final int SEGMENT_HEADER_BYTES = Integer.BYTES * 2 + Long.BYTES;
    static final int FRAME_HEADER_BYTES = Integer.BYTES * 2;
    static final int RECORD_TIMESTAMP_BYTES = Long.BYTES + Integer.BYTES;
    static final int MANIFEST_VERSION = 1;
    static final int LIFECYCLE_INDEX_VERSION = 1;
    static final int LIFECYCLE_MAINTENANCE_DAY_LIMIT = 16;
    static final int LIFECYCLE_QUEUE_SHARDS = 256;
    static final long LIFECYCLE_MAINTENANCE_INTERVAL_MILLIS = 60_000;
    static final long CLOSE_TIMEOUT_MILLIS = 5_000;
    static final int MAX_METADATA_BYTES = 64 * 1024;
    static final int MAX_CONFIRMATION_SET_BYTES = 64 * 1024 * 1024;
    static final int MAX_CONFIRMATION_SET_RECEIPTS = 250_000;
    static final int MAX_IDENTIFIER_BYTES = 4 * 1024;
    static final String DELETION_STAGING_PREFIX = ".deleting-";
    static final String DELETION_PENDING_PREFIX = ".delete-pending-";
    static final String LEGACY_MIGRATION_MARKER = ".lifecycle-index-migration.v1.json";
    static final DateTimeFormatter LEGACY_TIME = DateTimeFormatter
            .ofPattern("HH-mm-ss.SSS", Locale.ROOT);

    private final Path rootDirectory;
    private final Path journalDirectory;
    private final Path segmentsDirectory;
    private final Path ledgerDirectory;
    private final Path backupConfirmationDirectory;
    private final Path backupConfirmationSetDirectory;
    private final Path observationPurgeCheckpointDirectory;
    private final Path lifecycleQueueDirectory;
    private final Path lifecycleCursorPath;
    private final Path publicationDirectory;
    private final ZoneId zoneId;
    private final DailyFileObservationMode observationMode;
    private final long fsyncIntervalMillis;
    private final int maxUnforcedRecords;
    private final long maxUnforcedBytes;
    private final long maxSegmentBytes;
    private final Duration maxSegmentAge;
    private final int maxRecordBytes;
    private final int sealedJournalRetentionDays;
    private final boolean requireBackupBeforeSealedJournalDeletion;
    private final boolean trackWriterLeases;
    private final Clock clock;
    private final Logger log;
    private final Map<String, LaneState> lanes = new ConcurrentHashMap<>();
    private final Map<String, ConfirmationSetIndex> confirmationSetIndexes =
            new ConcurrentHashMap<>();
    private final Set<Path> durableDirectoryEntries = ConcurrentHashMap.newKeySet();
    private final AtomicInteger confirmationSetMarkerLoads = new AtomicInteger();
    private final ScheduledExecutorService forceScheduler;
    private final ScheduledExecutorService lifecycleScheduler;
    private final ReentrantLock closeLock = new ReentrantLock();
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final ReentrantLock writerHandleLock = new ReentrantLock();
    private final Condition noOpenWriterHandles = writerHandleLock.newCondition();
    private int openWriterHandles;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile Runnable deletionFreezeHook = () -> { };
    private boolean closeComplete;
    private int recoveryMaintenanceShard;
    private int purgeMaintenanceShard;
    private int deletionMaintenanceShard;

    public DailyFileJournalStore(Path rootDirectory, DailyFileJournalServiceConfig config, Logger log)
            throws IOException {
        this(rootDirectory, config, Clock.systemUTC(), log, true, true);
    }

    /**
     * Offline, restartable migration for journals created before the durable lifecycle index.
     * OSCAR must be stopped while this method runs. The journal metadata is the final commit: a
     * crash before it is published leaves startup blocked and a rerun safely recreates any missing
     * idempotent work markers.
     *
     * @return the number of newly created lifecycle work markers
     */
    public static long migrateLegacyJournalIndex(Path configuredRoot,
                                                 DailyFileJournalServiceConfig config)
            throws IOException {
        Objects.requireNonNull(config, "config");
        Path root = initializeRoot(configuredRoot);
        ZoneId configuredZone = ZoneId.of(Objects.requireNonNull(config.zoneId, "zoneId"));
        Path journal = resolveChild(root, config.journalDirectory, "journalDirectory");
        Path segments = journal.resolve("segments");
        Path ledgers = journal.resolve("ledger");
        Path lifecycleQueue = journal.resolve("lifecycle-queue-v1");
        Set<Path> durableEntries = new HashSet<>();
        durableEntries.add(root);
        createConfinedDirectories(root, journal, durableEntries,
                DailyFileJournalStore::forceDirectory);
        createConfinedDirectories(root, segments, durableEntries,
                DailyFileJournalStore::forceDirectory);
        createConfinedDirectories(root, ledgers, durableEntries,
                DailyFileJournalStore::forceDirectory);
        createConfinedDirectories(root, lifecycleQueue, durableEntries,
                DailyFileJournalStore::forceDirectory);

        Path lockPath = journal.resolve(".lifecycle-index-migration.lock");
        try (FileChannel lockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)) {
            FileLock acquiredLock;
            try {
                acquiredLock = lockChannel.tryLock();
            } catch (OverlappingFileLockException e) {
                acquiredLock = null;
            }
            if (acquiredLock == null)
                throw new IOException("Another daily-file lifecycle-index migration is running");
            final FileLock migrationLock = acquiredLock;
            try (migrationLock) {
                Path metadata = journal.resolve("journal-metadata.v1.json");
                Path migrationMarker = journal.resolve(LEGACY_MIGRATION_MARKER);
                boolean replaceLegacyMetadata = Files.exists(metadata,
                        LinkOption.NOFOLLOW_LINKS);
                if (replaceLegacyMetadata) {
                    JsonObject existing = readJsonObject(metadata, "journal metadata");
                    if (requiredLong(existing, "schemaVersion") != 1
                            || !configuredZone.getId().equals(requiredText(existing, "zoneId"))) {
                        throw new IOException("Configured daily-file zone " + configuredZone
                                + " does not match the existing journal metadata");
                    }
                    if (lifecycleIndexVersion(existing) == LIFECYCLE_INDEX_VERSION) {
                        if (Files.deleteIfExists(migrationMarker))
                            forceDirectory(journal);
                        return 0L;
                    }
                }

                String migrationJson = "{\n"
                        + "  \"schemaVersion\":1,\n"
                        + "  \"zoneId\":\"" + jsonEscape(configuredZone.getId()) + "\",\n"
                        + "  \"startedAt\":\"" + Instant.now() + "\"\n"
                        + "}\n";
                if (Files.exists(migrationMarker, LinkOption.NOFOLLOW_LINKS)) {
                    JsonObject existing = readJsonObject(migrationMarker,
                            "daily-file lifecycle-index migration marker");
                    if (requiredLong(existing, "schemaVersion") != 1
                            || !configuredZone.getId().equals(requiredText(existing, "zoneId"))) {
                        throw new IOException("Interrupted daily-file lifecycle-index migration "
                                + "uses a different time zone");
                    }
                } else {
                    atomicWriteNew(migrationMarker,
                            migrationJson.getBytes(StandardCharsets.UTF_8));
                }

                validateLegacyLedgerZones(ledgers, configuredZone);
                long indexed = indexLegacySegmentDirectories(segments, lifecycleQueue,
                        durableEntries);

                // Indexed metadata is deliberately the last durable commit. Until the versioned
                // index claim is present, ordinary startup refuses the legacy journal and cannot
                // observe a partial index.
                String metadataJson = "{\n"
                        + "  \"schemaVersion\":1,\n"
                        + "  \"zoneId\":\"" + jsonEscape(configuredZone.getId()) + "\",\n"
                        + "  \"lifecycleIndexVersion\":" + LIFECYCLE_INDEX_VERSION + "\n"
                        + "}\n";
                if (replaceLegacyMetadata)
                    atomicWrite(metadata, metadataJson.getBytes(StandardCharsets.UTF_8));
                else
                    atomicWriteNew(metadata, metadataJson.getBytes(StandardCharsets.UTF_8));
                if (Files.deleteIfExists(migrationMarker))
                    forceDirectory(journal);
                return indexed;
            }
        }
    }

    /** Offline/test constructor; callers must close all handles before closing the store. */
    DailyFileJournalStore(Path rootDirectory, DailyFileJournalServiceConfig config, Clock clock,
                          Logger log, boolean startScheduler) throws IOException {
        this(rootDirectory, config, clock, log, startScheduler, false);
    }

    DailyFileJournalStore(Path rootDirectory, DailyFileJournalServiceConfig config, Clock clock,
                          Logger log, boolean startScheduler, boolean trackWriterLeases)
            throws IOException {
        this.rootDirectory = initializeRoot(rootDirectory);
        durableDirectoryEntries.add(this.rootDirectory);
        Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.log = Objects.requireNonNull(log, "log");
        this.trackWriterLeases = trackWriterLeases;
        this.zoneId = ZoneId.of(Objects.requireNonNull(config.zoneId, "zoneId"));
        this.observationMode = config.dailyFileObservationMode != null
                ? config.dailyFileObservationMode : DailyFileObservationMode.FULL;
        this.fsyncIntervalMillis = requirePositive(config.fsyncIntervalMillis, "fsyncIntervalMillis");
        this.maxUnforcedRecords = (int) requirePositive(config.maxUnforcedRecords, "maxUnforcedRecords");
        this.maxUnforcedBytes = requirePositive(config.maxUnforcedBytes, "maxUnforcedBytes");
        this.maxSegmentBytes = requirePositive(config.maxSegmentBytes, "maxSegmentBytes");
        this.maxRecordBytes = (int) requirePositive(config.maxRecordBytes, "maxRecordBytes");
        if (config.sealedJournalRetentionDays < 0)
            throw new IllegalArgumentException("sealedJournalRetentionDays must not be negative");
        this.sealedJournalRetentionDays = config.sealedJournalRetentionDays;
        this.requireBackupBeforeSealedJournalDeletion =
                config.requireBackupBeforeSealedJournalDeletion;
        this.maxSegmentAge = Duration.ofSeconds(requirePositive(config.maxSegmentAgeSeconds,
                "maxSegmentAgeSeconds"));
        if (maxSegmentBytes <= SEGMENT_HEADER_BYTES + FRAME_HEADER_BYTES + RECORD_TIMESTAMP_BYTES)
            throw new IllegalArgumentException("maxSegmentBytes is too small for a journal record");

        this.journalDirectory = resolveChild(this.rootDirectory, config.journalDirectory, "journalDirectory");
        this.publicationDirectory = resolveChild(this.rootDirectory, config.publicationDirectory,
                "publicationDirectory");
        if (journalDirectory.startsWith(publicationDirectory)
                || publicationDirectory.startsWith(journalDirectory))
            throw new IllegalArgumentException(
                    "journalDirectory and publicationDirectory must not overlap");
        this.segmentsDirectory = journalDirectory.resolve("segments");
        this.ledgerDirectory = journalDirectory.resolve("ledger");
        this.backupConfirmationDirectory = journalDirectory.resolve("backup-confirmations");
        this.backupConfirmationSetDirectory = backupConfirmationDirectory.resolve(".sets");
        this.observationPurgeCheckpointDirectory = journalDirectory.resolve(
                "observation-purge-checkpoints");
        this.lifecycleQueueDirectory = journalDirectory.resolve("lifecycle-queue-v1");
        this.lifecycleCursorPath = journalDirectory.resolve("lifecycle-cursors.v1.json");

        createConfinedDirectories(this.rootDirectory, segmentsDirectory);
        createConfinedDirectories(this.rootDirectory, ledgerDirectory);
        createConfinedDirectories(this.rootDirectory, backupConfirmationDirectory);
        createConfinedDirectories(backupConfirmationDirectory, backupConfirmationSetDirectory);
        createConfinedDirectories(this.rootDirectory, observationPurgeCheckpointDirectory);
        createConfinedDirectories(this.rootDirectory, lifecycleQueueDirectory);
        createConfinedDirectories(this.rootDirectory, publicationDirectory);
        validateOrInitializeJournalMetadata();
        readLifecycleCursors();

        // Historical recovery can require reading and rendering millions of records. Running it
        // here delays the module's STARTED event and can make every required RPM client time out,
        // after which SensorHub does not retry those stopped modules. Current-day crash recovery is
        // already performed lazily by LaneState.openDay before the first new append. Older days and
        // interrupted retention work are represented by durable sharded work items and handled by
        // the bounded lifecycle pass below, after live writers have had an opportunity to attach.

        if (startScheduler) {
            forceScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "daily-file-journal-force");
                thread.setDaemon(true);
                return thread;
            });
            forceScheduler.scheduleAtFixedRate(this::periodicMaintenance,
                    fsyncIntervalMillis, fsyncIntervalMillis, TimeUnit.MILLISECONDS);
            lifecycleScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "daily-file-journal-lifecycle");
                thread.setDaemon(true);
                return thread;
            });
            lifecycleScheduler.scheduleWithFixedDelay(this::periodicLifecycleMaintenance,
                    LIFECYCLE_MAINTENANCE_INTERVAL_MILLIS,
                    LIFECYCLE_MAINTENANCE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        } else {
            forceScheduler = null;
            lifecycleScheduler = null;
        }
    }

    @Override
    public DailyFileWriterHandle openWriter(String laneId) throws IOException {
        String checkedLaneId = requireLaneId(laneId);
        writerHandleLock.lock();
        try {
            if (closing.get() || closed.get())
                throw new IOException("Daily-file journal is closing");
            lifecycleLock.lock();
            try {
                ensureOpen();
                LaneState state;
                try {
                    state = lanes.computeIfAbsent(checkedLaneId, id -> {
                        try {
                            return new LaneState(id);
                        } catch (IOException e) {
                            throw new LaneInitializationException(e);
                        }
                    });
                } catch (LaneInitializationException e) {
                    throw e.ioException;
                }
                if (trackWriterLeases)
                    openWriterHandles++;
                return new WriterHandle(state, trackWriterLeases);
            } finally {
                lifecycleLock.unlock();
            }
        } finally {
            writerHandleLock.unlock();
        }
    }

    @Override
    public DailyFileObservationMode getObservationMode() {
        return observationMode;
    }

    @Override
    public ZoneId getJournalZoneId() {
        return zoneId;
    }

    @Override
    public boolean isPublished(String laneId, Instant startInclusive, Instant endExclusive) {
        try {
            String checkedLaneId = requireLaneId(laneId);
            LocalDate day = LocalDate.ofInstant(startInclusive, zoneId);
            DayWindow window = DayWindow.forDate(day, zoneId);
            if (!window.startInclusive.equals(startInclusive) || !window.endExclusive.equals(endExclusive))
                return false;
            Path ledger = ledgerPath(checkedLaneId, day);
            String csvName = publicationFileId(checkedLaneId) + "_" + day + ".csv";
            String manifestName = csvName + ".manifest.v" + MANIFEST_VERSION + ".json";
            Path csv = publicationDirectory.resolve(csvName);
            Path manifest = publicationDirectory.resolve(manifestName);
            if (!isRegularFileNoFollow(csv) || !isRegularFileNoFollow(manifest))
                return false;

            JsonObject ledgerJson = readJsonObject(ledger, "publication ledger");
            JsonObject manifestJson = readJsonObject(manifest, "daily-file manifest");
            String checksum = sha256(csv);
            long byteCount = fileSizeNoFollow(csv);
            long ledgerRecords = requiredLong(ledgerJson, "recordCount");
            long manifestRecords = requiredLong(manifestJson, "recordCount");
            // Retention is anchored to this immutable timestamp, so malformed publication time
            // must fail the same validation that protects compatibility-row and journal deletion.
            Instant.parse(requiredText(manifestJson, "publishedAt"));
            return requiredLong(ledgerJson, "schemaVersion") == 1
                    && requiredLong(manifestJson, "schemaVersion") == MANIFEST_VERSION
                    && "PUBLISHED".equals(requiredText(ledgerJson, "state"))
                    && checkedLaneId.equals(requiredText(ledgerJson, "laneId"))
                    && checkedLaneId.equals(requiredText(manifestJson, "laneId"))
                    && day.toString().equals(requiredText(ledgerJson, "day"))
                    && zoneId.getId().equals(requiredText(ledgerJson, "zoneId"))
                    && zoneId.getId().equals(requiredText(manifestJson, "zoneId"))
                    && startInclusive.toString().equals(requiredText(ledgerJson,
                    "windowStartInclusive"))
                    && startInclusive.toString().equals(requiredText(manifestJson,
                    "windowStartInclusive"))
                    && endExclusive.toString().equals(requiredText(ledgerJson,
                    "windowEndExclusive"))
                    && endExclusive.toString().equals(requiredText(manifestJson,
                    "windowEndExclusive"))
                    && csvName.equals(requiredText(ledgerJson, "dataFile"))
                    && csvName.equals(requiredText(manifestJson, "dataFile"))
                    && manifestName.equals(requiredText(ledgerJson, "manifestFile"))
                    && "OSCAR_LEGACY_DAILY_CSV".equals(requiredText(manifestJson, "format"))
                    && "SHA-256".equals(requiredText(manifestJson, "checksumAlgorithm"))
                    && checksum.equals(requiredText(ledgerJson, "sha256"))
                    && checksum.equals(requiredText(manifestJson, "sha256"))
                    && ledgerRecords >= 0 && ledgerRecords == manifestRecords
                    && requiredLong(ledgerJson, "byteCount") == byteCount
                    && requiredLong(manifestJson, "byteCount") == byteCount;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void confirmPublishedBackup(String laneId, Instant startInclusive, Instant endExclusive,
                                       String publishedSha256, String backupReference)
            throws IOException {
        String checkedLaneId = requireLaneId(laneId);
        LocalDate day = LocalDate.ofInstant(startInclusive, zoneId);
        confirmPublishedBackup(checkedLaneId, startInclusive, endExclusive, publishedSha256,
                backupReference, sealedSegmentInventory(segmentDayDirectory(checkedLaneId, day)));
    }

    void confirmPublishedBackup(String laneId, Instant startInclusive, Instant endExclusive,
                                String publishedSha256, String backupReference,
                                List<SealedSegmentIdentity> backedUpSegments)
            throws IOException {
        PreparedBackupConfirmation prepared = preparePublishedBackupConfirmation(laneId,
                startInclusive, endExclusive, publishedSha256, backupReference,
                backedUpSegments);
        commitPreparedBackupConfirmations("direct-" + UUID.randomUUID(), List.of(prepared));
    }

    PreparedBackupConfirmation preparePublishedBackupConfirmation(
            String laneId, Instant startInclusive, Instant endExclusive,
            String publishedSha256, String backupReference,
            List<SealedSegmentIdentity> backedUpSegments) throws IOException {
        String checkedLaneId = requireLaneId(laneId);
        if (publishedSha256 == null || !publishedSha256.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("publishedSha256 must be a lowercase SHA-256 digest");
        if (backupReference == null || backupReference.isBlank())
            throw new IllegalArgumentException("backupReference is required");
        LocalDate day = LocalDate.ofInstant(startInclusive, zoneId);
        DayWindow window = DayWindow.forDate(day, zoneId);
        if (!window.startInclusive.equals(startInclusive)
                || !window.endExclusive.equals(endExclusive))
            throw new IOException("Backup confirmation does not match an immutable journal day");
        if (!isPublished(checkedLaneId, startInclusive, endExclusive))
            throw new IOException("Cannot confirm backup before immutable daily-file publication");

        Path csv = publicationDirectory.resolve(
                publicationFileId(checkedLaneId) + "_" + day + ".csv");
        String actualSha256 = sha256(csv);
        if (!actualSha256.equals(publishedSha256))
            throw new IOException("Backup confirmation checksum does not match the publication");
        List<SealedSegmentIdentity> liveSegments = sealedSegmentInventory(
                segmentDayDirectory(checkedLaneId, day));
        if (!liveSegments.equals(Objects.requireNonNull(backedUpSegments, "backedUpSegments")))
            throw new IOException("Backup sealed-segment inventory does not match live lane/day");
        if (liveSegments.isEmpty())
            throw new IOException("Backup confirmation requires at least one sealed segment");
        return new PreparedBackupConfirmation(backupConfirmationPath(checkedLaneId, day),
                checkedLaneId, day, startInclusive, endExclusive, publishedSha256,
                backupReference, List.copyOf(liveSegments));
    }

    void commitPreparedBackupConfirmations(String confirmationSet,
                                            List<PreparedBackupConfirmation> confirmations)
            throws IOException {
        if (confirmationSet == null
                || !confirmationSet.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
            throw new IOException("Backup confirmation set ID is invalid");
        Objects.requireNonNull(confirmations, "confirmations");
        if (confirmations.isEmpty())
            return;
        List<PreparedBackupConfirmation> ordered = new ArrayList<>(confirmations);
        Set<Path> uniqueTargets = new HashSet<>();
        for (PreparedBackupConfirmation prepared : ordered) {
            Path target = prepared.target().toAbsolutePath().normalize();
            if (!target.startsWith(backupConfirmationDirectory)
                    || target.startsWith(backupConfirmationSetDirectory)
                    || !target.getFileName().toString().endsWith(".json")
                    || !uniqueTargets.add(target))
                throw new IOException("Backup confirmation set has an invalid or duplicate target");
        }
        ordered.sort(Comparator.comparing(value -> confirmationRelativePath(value.target())));
        if (ordered.size() > MAX_CONFIRMATION_SET_RECEIPTS)
            throw new IOException("Backup confirmation set has too many receipts");
        Path marker = confirmationSetMarkerPath(confirmationSet);
        if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Backup confirmation set is already committed: "
                    + confirmationSet);
        Instant confirmedAt = clock.instant();
        List<CommittedConfirmation> committed = new ArrayList<>(ordered.size());
        for (PreparedBackupConfirmation prepared : ordered) {
            byte[] receipt = prepared.json(confirmationSet, confirmedAt)
                    .getBytes(StandardCharsets.UTF_8);
            committed.add(new CommittedConfirmation(prepared.target(),
                    confirmationRelativePath(prepared.target()), sha256(receipt), receipt));
        }
        StringBuilder markerJson = new StringBuilder("{\n")
                .append("  \"schemaVersion\":1,\n")
                .append("  \"format\":\"oscar-dailyfile-backup-confirmation-set\",\n")
                .append("  \"confirmationSet\":\"").append(jsonEscape(confirmationSet))
                .append("\",\n")
                .append("  \"receiptCount\":").append(ordered.size()).append(",\n")
                .append("  \"committedAt\":\"").append(confirmedAt).append("\",\n")
                .append("  \"receipts\":[\n");
        for (int index = 0; index < committed.size(); index++) {
            CommittedConfirmation receipt = committed.get(index);
            markerJson.append("    {\"path\":\"").append(jsonEscape(receipt.path()))
                    .append("\",\"sha256\":\"").append(receipt.sha256()).append("\"}");
            if (index + 1 < committed.size())
                markerJson.append(',');
            markerJson.append('\n');
        }
        markerJson.append("  ]\n}\n");
        byte[] markerBytes = markerJson.toString().getBytes(StandardCharsets.UTF_8);
        if (markerBytes.length > MAX_CONFIRMATION_SET_BYTES)
            throw new IOException("Backup confirmation set marker exceeds its safety limit");
        // Every receipt is durable before the one atomic marker that makes the complete set
        // visible to retention. A crash or write failure before that marker is therefore safe:
        // uncommitted receipts cannot authorize deletion.
        for (CommittedConfirmation receipt : committed)
            atomicWrite(receipt.target(), receipt.contents());
        atomicWriteNew(marker, markerBytes);
    }

    private String confirmationRelativePath(Path target) {
        return backupConfirmationDirectory.relativize(target.toAbsolutePath().normalize())
                .toString().replace('\\', '/');
    }

    private Path confirmationSetMarkerPath(String confirmationSet) {
        return backupConfirmationSetDirectory.resolve(confirmationSet + ".json");
    }

    private boolean isConfirmationSetCommitted(Path confirmation, JsonObject receipt)
            throws IOException {
        if (!receipt.has("confirmationSet"))
            return false;
        String confirmationSet = requiredText(receipt, "confirmationSet");
        if (!confirmationSet.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
            return false;
        ConfirmationSetIndex index = confirmationSetIndex(confirmationSet);
        String relative = confirmationRelativePath(confirmation);
        String expectedSha256 = index.receiptSha256().get(relative);
        return expectedSha256 != null && expectedSha256.equals(sha256(confirmation));
    }

    ConfirmationSetIndex confirmationSetIndex(String confirmationSet) throws IOException {
        Path marker = confirmationSetMarkerPath(confirmationSet);
        ConfirmationSetIndex cached = confirmationSetIndexes.get(confirmationSet);
        if (cached != null) {
            if (cached.markerIdentity().equals(confirmationSetMarkerIdentity(marker)))
                return cached;
            confirmationSetIndexes.remove(confirmationSet, cached);
            throw new IOException("Immutable backup confirmation set marker changed");
        }
        synchronized (confirmationSetIndexes) {
            cached = confirmationSetIndexes.get(confirmationSet);
            if (cached != null) {
                if (cached.markerIdentity().equals(confirmationSetMarkerIdentity(marker)))
                    return cached;
                confirmationSetIndexes.remove(confirmationSet, cached);
                throw new IOException("Immutable backup confirmation set marker changed");
            }
            ConfirmationSetIndex loaded = readConfirmationSetIndex(confirmationSet, marker);
            confirmationSetIndexes.put(confirmationSet, loaded);
            confirmationSetMarkerLoads.incrementAndGet();
            return loaded;
        }
    }

    private ConfirmationSetIndex readConfirmationSetIndex(String confirmationSet, Path markerPath)
            throws IOException {
        ConfirmationSetMarkerIdentity before = confirmationSetMarkerIdentity(markerPath);
        JsonObject marker = readJsonObject(markerPath,
                "backup confirmation set marker", MAX_CONFIRMATION_SET_BYTES);
        if (requiredLong(marker, "schemaVersion") != 1
                || !"oscar-dailyfile-backup-confirmation-set".equals(
                requiredText(marker, "format"))
                || !confirmationSet.equals(requiredText(marker, "confirmationSet")))
            throw new IOException("Backup confirmation set marker identity is invalid");
        JsonElement receiptList = marker.get("receipts");
        if (receiptList == null || !receiptList.isJsonArray())
            throw new IOException("Backup confirmation set receipt index is missing");
        JsonArray receipts = receiptList.getAsJsonArray();
        long advertisedCount = requiredLong(marker, "receiptCount");
        if (advertisedCount <= 0 || advertisedCount != receipts.size()
                || receipts.size() > MAX_CONFIRMATION_SET_RECEIPTS)
            throw new IOException("Backup confirmation set receipt count is invalid");
        Map<String, String> receiptSha256 = new HashMap<>(receipts.size());
        String previousPath = null;
        for (JsonElement element : receipts) {
            if (!element.isJsonObject())
                throw new IOException("Backup confirmation set receipt entry is invalid");
            JsonObject member = element.getAsJsonObject();
            String relative = requiredText(member, "path");
            String expectedSha256 = requiredText(member, "sha256");
            if (!relative.matches("[A-Za-z0-9._-]+/[0-9]{4}-[0-9]{2}-[0-9]{2}\\.json")
                    || !expectedSha256.matches("[0-9a-f]{64}")
                    || (previousPath != null && previousPath.compareTo(relative) >= 0))
                throw new IOException("Backup confirmation set receipt index is invalid at "
                        + relative);
            if (receiptSha256.put(relative, expectedSha256) != null)
                throw new IOException("Backup confirmation set contains a duplicate receipt");
            previousPath = relative;
        }
        ConfirmationSetMarkerIdentity after = confirmationSetMarkerIdentity(markerPath);
        if (!before.equals(after))
            throw new IOException("Backup confirmation set marker changed while being read");
        return new ConfirmationSetIndex(Map.copyOf(receiptSha256), after);
    }

    private static ConfirmationSetMarkerIdentity confirmationSetMarkerIdentity(Path marker)
            throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(marker,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink()
                || Files.isSymbolicLink(marker))
            throw new IOException("Backup confirmation set marker is linked or non-regular");
        Object key = attributes.fileKey();
        return new ConfirmationSetMarkerIdentity(key == null ? "" : key.toString(),
                attributes.size(), attributes.lastModifiedTime());
    }

    int confirmationSetMarkerLoadCount() {
        return confirmationSetMarkerLoads.get();
    }

    private static String sha256(byte[] contents) {
        return HexFormat.of().formatHex(sha256Digest().digest(contents));
    }

    record CommittedConfirmation(Path target, String path, String sha256, byte[] contents) {
        CommittedConfirmation {
            contents = contents.clone();
        }

        @Override
        public byte[] contents() {
            return contents.clone();
        }
    }

    record ConfirmationSetIndex(Map<String, String> receiptSha256,
                                ConfirmationSetMarkerIdentity markerIdentity) {
    }

    record ConfirmationSetMarkerIdentity(String fileKey, long size, FileTime lastModifiedTime) {
    }

    record PreparedBackupConfirmation(Path target, String laneId, LocalDate day,
                                      Instant startInclusive, Instant endExclusive,
                                      String publishedSha256, String backupReference,
                                      List<SealedSegmentIdentity> sealedSegments) {
        PreparedBackupConfirmation {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(laneId, "laneId");
            Objects.requireNonNull(day, "day");
            Objects.requireNonNull(startInclusive, "startInclusive");
            Objects.requireNonNull(endExclusive, "endExclusive");
            Objects.requireNonNull(publishedSha256, "publishedSha256");
            Objects.requireNonNull(backupReference, "backupReference");
            sealedSegments = List.copyOf(sealedSegments);
        }

        String json(String confirmationSet, Instant confirmedAt) {
            return "{\n"
                    + "  \"schemaVersion\":1,\n"
                    + "  \"laneId\":\"" + jsonEscape(laneId) + "\",\n"
                    + "  \"day\":\"" + day + "\",\n"
                    + "  \"windowStartInclusive\":\"" + startInclusive + "\",\n"
                    + "  \"windowEndExclusive\":\"" + endExclusive + "\",\n"
                    + "  \"sha256\":\"" + publishedSha256 + "\",\n"
                    + "  \"backupReference\":\"" + jsonEscape(backupReference) + "\",\n"
                    + "  \"confirmationSet\":\"" + jsonEscape(confirmationSet) + "\",\n"
                    + "  \"sealedSegments\":"
                    + sealedSegmentInventoryJson(sealedSegments) + ",\n"
                    + "  \"confirmedAt\":\"" + confirmedAt + "\"\n"
                    + "}\n";
        }
    }

    @Override
    public boolean isBackupConfirmed(String laneId, Instant startInclusive,
                                     Instant endExclusive) {
        try {
            String checkedLaneId = requireLaneId(laneId);
            LocalDate day = LocalDate.ofInstant(startInclusive, zoneId);
            return isBackupConfirmed(checkedLaneId, startInclusive, endExclusive,
                    segmentDayDirectory(checkedLaneId, day));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBackupConfirmed(String laneId, Instant startInclusive,
                                      Instant endExclusive, Path sealedDayDirectory) {
        try {
            String checkedLaneId = requireLaneId(laneId);
            LocalDate day = LocalDate.ofInstant(startInclusive, zoneId);
            DayWindow window = DayWindow.forDate(day, zoneId);
            if (!window.startInclusive.equals(startInclusive)
                    || !window.endExclusive.equals(endExclusive))
                return false;
            Path confirmation = backupConfirmationPath(checkedLaneId, day);
            if (!isRegularFileNoFollow(confirmation))
                return false;
            // Most historical days are intentionally not backup-confirmed. Check the tiny
            // receipt first so a retention sweep does not hash every published CSV.
            if (!isPublished(checkedLaneId, startInclusive, endExclusive))
                return false;
            Path csv = publicationDirectory.resolve(
                    publicationFileId(checkedLaneId) + "_" + day + ".csv");
            String checksum = sha256(csv);
            JsonObject json = readJsonObject(confirmation, "backup confirmation");
            List<SealedSegmentIdentity> confirmedSegments = parseSealedSegmentInventory(json);
            List<SealedSegmentIdentity> liveSegments = sealedSegmentInventory(sealedDayDirectory);
            return isConfirmationSetCommitted(confirmation, json)
                    && checkedLaneId.equals(requiredText(json, "laneId"))
                    && startInclusive.toString().equals(requiredText(json, "windowStartInclusive"))
                    && endExclusive.toString().equals(requiredText(json, "windowEndExclusive"))
                    && checksum.equals(requiredText(json, "sha256"))
                    && !requiredText(json, "backupReference").isBlank()
                    && !confirmedSegments.isEmpty()
                    && confirmedSegments.equals(liveSegments);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<LocalDate> readObservationPurgeCheckpoint(String feedId) throws IOException {
        String checkedFeedId = requireFeedId(feedId);
        Path checkpoint = observationPurgeCheckpointPath(checkedFeedId);
        if (!Files.exists(checkpoint, LinkOption.NOFOLLOW_LINKS))
            return Optional.empty();
        JsonObject json = readJsonObject(checkpoint, "observation purge checkpoint");
        if (requiredLong(json, "schemaVersion") != 1
                || !checkedFeedId.equals(requiredText(json, "feedId")))
            throw new IOException("Daily-file observation purge checkpoint feed identity mismatch");
        try {
            return Optional.of(LocalDate.parse(requiredText(json, "processedThrough")));
        } catch (RuntimeException e) {
            throw new IOException("Invalid daily-file observation purge checkpoint", e);
        }
    }

    @Override
    public synchronized void writeObservationPurgeCheckpoint(String feedId,
                                                              LocalDate processedThrough)
            throws IOException {
        String checkedFeedId = requireFeedId(feedId);
        Objects.requireNonNull(processedThrough, "processedThrough");
        Optional<LocalDate> existing = readObservationPurgeCheckpoint(checkedFeedId);
        if (existing.isPresent() && processedThrough.isBefore(existing.get()))
            throw new IOException("Daily-file purge checkpoint cannot move backwards");
        String json = "{\n"
                + "  \"schemaVersion\":1,\n"
                + "  \"feedId\":\"" + jsonEscape(checkedFeedId) + "\",\n"
                + "  \"processedThrough\":\"" + processedThrough + "\",\n"
                + "  \"updatedAt\":\"" + clock.instant() + "\"\n"
                + "}\n";
        atomicWrite(observationPurgeCheckpointPath(checkedFeedId),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<LocalDate> findEarliestJournalDay(String laneId) throws IOException {
        String checkedLaneId = requireLaneId(laneId);
        Path laneDirectory = segmentsDirectory.resolve(directoryId(checkedLaneId));
        if (!Files.isDirectory(laneDirectory, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(laneDirectory))
            return Optional.empty();
        try (var paths = Files.list(laneDirectory)) {
            return paths.filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isSymbolicLink(path)).map(path -> {
                try {
                    return LocalDate.parse(path.getFileName().toString());
                } catch (RuntimeException ignored) {
                    return null;
                }
            }).filter(Objects::nonNull).min(Comparator.naturalOrder());
        }
    }

    /** Visible for tests and for an explicit administrative flush. */
    void sealCompletedDays(Instant now) throws IOException {
        IOException failure = maintainActiveLanes(now);

        lifecycleLock.lock();
        try {
            // A recovered lane need not have been opened by a driver yet. Active lanes are also
            // safe here because recoverHistoricalDay coordinates with their per-lane lock and
            // skips only the exact day currently accepting records.
            try (DirectoryStream<Path> laneDirs = Files.newDirectoryStream(segmentsDirectory)) {
                for (Path laneDir : laneDirs) {
                    if (!Files.isDirectory(laneDir, LinkOption.NOFOLLOW_LINKS))
                        continue;
                    String laneId = readLaneId(laneDir);
                    try {
                        sealRecoveredCompletedDays(laneId, laneDir,
                                LocalDate.ofInstant(now, zoneId));
                    } catch (IOException e) {
                        failure = merge(failure, e);
                    }
                }
            }
        } finally {
            lifecycleLock.unlock();
        }
        if (failure != null)
            throw failure;
    }

    private IOException maintainActiveLanes(Instant now) {
        IOException failure = null;
        for (LaneState lane : lanes.values()) {
            try {
                lane.maintenance(now);
            } catch (IOException e) {
                failure = merge(failure, e);
            }
        }
        return failure;
    }

    private boolean isSealedDayDeletionCandidate(String laneId, DayWindow window, Instant now)
            throws IOException {
        Duration retention = Duration.ofDays(sealedJournalRetentionDays);
        // Avoid publication hashing or segment replay until the original day-close retention has
        // elapsed. A later publication time below can only move this boundary forward.
        if (now.isBefore(window.endExclusive.plus(retention)))
            return false;

        boolean publicationAndPolicyValid = isPublished(
                laneId, window.startInclusive, window.endExclusive);
        if (!publicationAndPolicyValid)
            return false;

        Instant publishedAt = publishedAt(laneId, window.day);
        Instant retentionAnchor = publishedAt.isAfter(window.endExclusive)
                ? publishedAt : window.endExclusive;
        if (now.isBefore(retentionAnchor.plus(retention)))
            return false;

        return true;
    }

    private Instant publishedAt(String laneId, LocalDate day) throws IOException {
        String csvName = publicationFileId(laneId) + "_" + day + ".csv";
        Path manifest = publicationDirectory.resolve(
                csvName + ".manifest.v" + MANIFEST_VERSION + ".json");
        try {
            return Instant.parse(requiredText(
                    readJsonObject(manifest, "daily-file manifest"), "publishedAt"));
        } catch (RuntimeException e) {
            throw new IOException("Invalid immutable daily-file publication timestamp", e);
        }
    }

    private boolean sealedJournalMatchesPublishedCsv(String laneId, Path dayDirectory,
                                                      DayWindow window) throws IOException {
        validateStagedDeletionDirectory(dayDirectory);
        MessageDigest digest = sha256Digest();
        long[] renderedBytes = {0L};
        for (Path segment : listSegments(dayDirectory, ".seg")) {
            scanRecords(segment, record -> {
                if (record.timestamp().isBefore(window.startInclusive)
                        || !record.timestamp().isBefore(window.endExclusive)) {
                    throw new IOException("Journal record " + record.timestamp()
                            + " is outside immutable day window " + window);
                }
                byte[] line = renderLegacyLine(record);
                digest.update(line);
                renderedBytes[0] = Math.addExact(renderedBytes[0], line.length);
            });
        }
        Path csv = publicationDirectory.resolve(publicationFileId(laneId)
                + "_" + window.day + ".csv");
        return renderedBytes[0] == fileSizeNoFollow(csv)
                && HexFormat.of().formatHex(digest.digest()).equals(sha256(csv));
    }

    private boolean freezeAuthorizeAndDelete(LifecycleWork purgeWork, Path dayDirectory,
                                             DayWindow window) throws IOException {
        validateStagedDeletionDirectory(dayDirectory);
        Path pending = dayDirectory.resolveSibling(deletionPendingName(purgeWork.day));
        Path committed = dayDirectory.resolveSibling(deletionStagingName(purgeWork.day));
        if (Files.exists(pending, LinkOption.NOFOLLOW_LINKS)
                || Files.exists(committed, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Stale journal deletion state requires bounded recovery for "
                    + purgeWork.laneId + "/" + purgeWork.day);
        }

        LifecycleWork deletionWork = enqueueLifecycleWork(
                LifecycleWorkKind.DELETION, purgeWork.laneId, purgeWork.day);
        atomicMoveNew(dayDirectory, pending);
        forceDirectory(pending.getParent());
        deletionFreezeHook.run();

        boolean authorized = requireBackupBeforeSealedJournalDeletion
                ? isBackupConfirmed(purgeWork.laneId, window.startInclusive,
                window.endExclusive, pending)
                : isPublished(purgeWork.laneId, window.startInclusive, window.endExclusive)
                && sealedJournalMatchesPublishedCsv(purgeWork.laneId, pending, window);
        if (!authorized) {
            restorePendingDeletion(purgeWork, pending, dayDirectory, deletionWork);
            return false;
        }

        // This second atomic rename is the destructive commit point. A crash while PENDING is
        // fail-safe and restores the frozen day; a crash after DELETING only resumes removal.
        atomicMoveNew(pending, committed);
        forceDirectory(committed.getParent());
        deleteStagedDay(committed);
        removeLifecycleWork(deletionWork);

        // A misconfigured second writer can recreate the original pathname after it was frozen.
        // Never consume those late additions as part of the authorized snapshot. Keep them on
        // disk and queue explicit recovery, where immutable-publication mismatch fails closed.
        if (Files.exists(dayDirectory, LinkOption.NOFOLLOW_LINKS)) {
            enqueueLifecycleWork(LifecycleWorkKind.RECOVERY,
                    purgeWork.laneId, purgeWork.day);
        } else {
            removeLifecycleWork(purgeWork);
        }
        return true;
    }

    private void restorePendingDeletion(LifecycleWork purgeWork, Path pending,
                                        Path dayDirectory, LifecycleWork deletionWork)
            throws IOException {
        if (Files.exists(dayDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("A late journal day appeared while the frozen source was being "
                    + "validated; both copies were preserved for administrator reconciliation: "
                    + dayDirectory);
        }
        atomicMoveNew(pending, dayDirectory);
        forceDirectory(dayDirectory.getParent());
        removeLifecycleWork(deletionWork);
    }

    private void resumeStagedDeletion(LifecycleWork work) throws IOException {
        Path dayDirectory = segmentDayDirectory(work.laneId, work.day);
        Path pending = dayDirectory.resolveSibling(deletionPendingName(work.day));
        Path committed = dayDirectory.resolveSibling(deletionStagingName(work.day));
        if (Files.exists(committed, LinkOption.NOFOLLOW_LINKS)) {
            deleteStagedDay(committed);
            removeLifecycleWork(work);
            if (!Files.exists(dayDirectory, LinkOption.NOFOLLOW_LINKS))
                removeLifecycleWork(LifecycleWorkKind.PURGE, work.laneId, work.day);
            log.info("Completed interrupted sealed-journal deletion {}", committed);
            return;
        }
        if (Files.exists(pending, LinkOption.NOFOLLOW_LINKS)) {
            restorePendingDeletion(work, pending, dayDirectory, work);
            log.info("Restored uncommitted sealed-journal deletion {}", dayDirectory);
            return;
        }
        // The durable marker is written before the first rename. No staged path means either the
        // rename never occurred or the idempotent removal completed before its marker cleanup.
        removeLifecycleWork(work);
    }

    private void deleteStagedDay(Path stagedDirectory) throws IOException {
        validateStagedDeletionDirectory(stagedDirectory);
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(stagedDirectory)) {
            for (Path entry : entries)
                Files.delete(entry);
        }
        Files.delete(stagedDirectory);
        forceDirectory(stagedDirectory.getParent());
    }

    private void validateStagedDeletionDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(directory)) {
            throw new IOException("Refusing to purge a linked or non-directory journal day: "
                    + directory);
        }
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(entry)
                        || !entry.getFileName().toString().endsWith(".seg")) {
                    throw new IOException(
                            "Refusing to purge a journal day containing unsealed or unknown files: "
                                    + directory);
                }
            }
        }
    }

    static String deletionStagingName(LocalDate day) {
        return DELETION_STAGING_PREFIX + day;
    }

    static String deletionPendingName(LocalDate day) {
        return DELETION_PENDING_PREFIX + day;
    }

    private static boolean isDeletionStagingDirectory(Path path) {
        String name = path.getFileName().toString();
        if (!name.startsWith(DELETION_STAGING_PREFIX))
            return false;
        try {
            LocalDate.parse(name.substring(DELETION_STAGING_PREFIX.length()));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Path segmentDayDirectory(String laneId, LocalDate day) {
        return segmentsDirectory.resolve(directoryId(laneId)).resolve(day.toString());
    }

    static List<SealedSegmentIdentity> sealedSegmentInventory(Path dayDirectory)
            throws IOException {
        if (!Files.isDirectory(dayDirectory) || Files.isSymbolicLink(dayDirectory))
            throw new IOException("Sealed journal day is missing or is a symbolic link: "
                    + dayDirectory);
        List<Path> files;
        try (var paths = Files.list(dayDirectory)) {
            files = paths.sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        List<SealedSegmentIdentity> identities = new ArrayList<>(files.size());
        for (Path file : files) {
            String name = file.getFileName().toString();
            if (!name.endsWith(".seg") || !Files.isRegularFile(file)
                    || Files.isSymbolicLink(file))
                throw new IOException("Journal day contains an unsealed, unknown, or linked entry: "
                        + file);
            identities.add(new SealedSegmentIdentity(name, Files.size(file), sha256(file)));
        }
        return List.copyOf(identities);
    }

    private static String sealedSegmentInventoryJson(List<SealedSegmentIdentity> segments) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0)
                json.append(',');
            SealedSegmentIdentity segment = segments.get(i);
            json.append("{\"name\":\"").append(jsonEscape(segment.name()))
                    .append("\",\"byteCount\":").append(segment.byteCount())
                    .append(",\"sha256\":\"").append(segment.sha256()).append("\"}");
        }
        return json.append(']').toString();
    }

    private static List<SealedSegmentIdentity> parseSealedSegmentInventory(JsonObject json)
            throws IOException {
        JsonElement value = json.get("sealedSegments");
        if (value == null || !value.isJsonArray())
            throw new IOException("Backup confirmation has no sealed-segment inventory");
        JsonArray array = value.getAsJsonArray();
        List<SealedSegmentIdentity> identities = new ArrayList<>(array.size());
        String previous = null;
        for (JsonElement element : array) {
            if (!element.isJsonObject())
                throw new IOException("Invalid sealed-segment identity");
            JsonObject object = element.getAsJsonObject();
            String name = requiredText(object, "name");
            String checksum = requiredText(object, "sha256");
            long byteCount;
            try {
                byteCount = object.get("byteCount").getAsLong();
            } catch (RuntimeException e) {
                throw new IOException("Invalid sealed-segment byte count", e);
            }
            if (!name.matches("[A-Za-z0-9._-]+\\.seg") || checksum.length() != 64
                    || !checksum.matches("[0-9a-f]{64}") || byteCount < 0
                    || (previous != null && previous.compareTo(name) >= 0))
                throw new IOException("Invalid or unsorted sealed-segment identity");
            identities.add(new SealedSegmentIdentity(name, byteCount, checksum));
            previous = name;
        }
        return List.copyOf(identities);
    }

    private static String requiredText(JsonObject object, String field) throws IOException {
        try {
            JsonElement value = object.get(field);
            if (value == null || value.isJsonNull())
                throw new IOException("Missing field " + field);
            return value.getAsString();
        } catch (RuntimeException e) {
            throw new IOException("Invalid field " + field, e);
        }
    }

    private static long requiredLong(JsonObject object, String field) throws IOException {
        try {
            JsonElement value = object.get(field);
            if (value == null || value.isJsonNull() || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isNumber())
                throw new IOException("Missing or non-numeric field " + field);
            return Long.parseLong(value.getAsString());
        } catch (NumberFormatException | UnsupportedOperationException e) {
            throw new IOException("Invalid integer field " + field, e);
        }
    }

    private static JsonObject readJsonObject(Path path, String description) throws IOException {
        return readJsonObject(path, description, MAX_METADATA_BYTES);
    }

    private static JsonObject readJsonObject(Path path, String description, int maxBytes)
            throws IOException {
        if (!isRegularFileNoFollow(path))
            throw new IOException("Missing, linked, or non-regular " + description + ": " + path);
        long advertisedSize = fileSizeNoFollow(path);
        if (advertisedSize <= 0 || advertisedSize > maxBytes)
            throw new IOException("Invalid " + description + " size: " + advertisedSize);
        byte[] contents;
        try (InputStream input = Files.newInputStream(path, StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            contents = input.readNBytes(maxBytes + 1);
        }
        if (contents.length == 0 || contents.length > maxBytes)
            throw new IOException("Invalid " + description + " size: " + contents.length);
        try {
            JsonElement parsed = JsonParser.parseString(new String(contents,
                    StandardCharsets.UTF_8));
            if (!parsed.isJsonObject())
                throw new IOException("Invalid " + description + ": JSON object required");
            return parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Invalid " + description, e);
        }
    }

    private static boolean isRegularFileNoFollow(Path path) {
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(path);
    }

    private static long fileSizeNoFollow(Path path) throws IOException {
        return Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS).size();
    }

    record SealedSegmentIdentity(String name, long byteCount, String sha256) {
        SealedSegmentIdentity {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(sha256, "sha256");
        }
    }

    @Override
    public void close() throws IOException {
        closeWithin(CLOSE_TIMEOUT_MILLIS);
    }

    void closeWithin(long timeoutMillis) throws IOException {
        if (timeoutMillis < 0)
            throw new IllegalArgumentException("timeoutMillis must not be negative");
        long deadlineNanos = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        boolean closeLocked;
        try {
            closeLocked = tryLockUntil(closeLock, deadlineNanos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while closing the daily-file journal", e);
        }
        if (!closeLocked)
            throw new IOException("Timed out waiting for another daily-file close attempt");
        try {
            if (closeComplete)
                return;
            boolean writerGateLocked;
            try {
                writerGateLocked = tryLockUntil(writerHandleLock, deadlineNanos);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while closing daily-file writer admission", e);
            }
            if (!writerGateLocked)
                throw new IOException("Timed out closing daily-file writer admission");
            try {
                closing.set(true);
            } finally {
                writerHandleLock.unlock();
            }
            try {
                if (!awaitWriterHandlesUntil(deadlineNanos))
                    throw new IOException("Daily-file journal writers did not close within "
                            + timeoutMillis + " ms");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for daily-file writers", e);
            }
            // Keep periodic force and lifecycle maintenance alive while admitted writers
            // drain. If that drain times out, the remaining handles are still usable and
            // retain their configured time-based fsync guarantee even though no new handle
            // can enter through the closing gate.
            if (!shutdownSchedulersUntil(lifecycleScheduler, forceScheduler, deadlineNanos))
                throw new IOException("Daily-file journal schedulers did not terminate within "
                        + timeoutMillis + " ms");
            closed.set(true);

            IOException failure = null;
            boolean lifecycleLocked;
            try {
                lifecycleLocked = tryLockUntil(lifecycleLock, deadlineNanos);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while closing the daily-file journal", e);
            }
            if (!lifecycleLocked)
                throw new IOException(
                        "Timed out waiting for daily-file lifecycle maintenance to stop");
            try {
                for (LaneState lane : lanes.values()) {
                    try {
                        lane.closeForShutdown(deadlineNanos);
                    } catch (IOException e) {
                        failure = merge(failure, e);
                    }
                }
                if (failure == null) {
                    lanes.clear();
                    closeComplete = true;
                }
            } finally {
                lifecycleLock.unlock();
            }
            if (failure != null)
                throw failure;
        } finally {
            closeLock.unlock();
        }
    }

    static boolean shutdownSchedulersBounded(ScheduledExecutorService first,
                                             ScheduledExecutorService second,
                                             long timeoutMillis) {
        if (timeoutMillis < 0)
            throw new IllegalArgumentException("timeoutMillis must not be negative");
        long deadlineNanos = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        return shutdownSchedulersUntil(first, second, deadlineNanos);
    }

    private static boolean shutdownSchedulersUntil(ScheduledExecutorService first,
                                                   ScheduledExecutorService second,
                                                   long deadlineNanos) {
        if (first != null)
            first.shutdown();
        if (second != null)
            second.shutdown();
        long now = System.nanoTime();
        long gracefulDeadline = now + Math.max(0L, deadlineNanos - now) / 2L;
        try {
            boolean firstStopped = awaitUntil(first, gracefulDeadline);
            boolean secondStopped = awaitUntil(second, gracefulDeadline);
            if (!firstStopped && first != null)
                first.shutdownNow();
            if (!secondStopped && second != null)
                second.shutdownNow();
            return awaitUntil(first, deadlineNanos) && awaitUntil(second, deadlineNanos);
        } catch (InterruptedException e) {
            if (first != null)
                first.shutdownNow();
            if (second != null)
                second.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean awaitUntil(ScheduledExecutorService scheduler, long deadlineNanos)
            throws InterruptedException {
        if (scheduler == null || scheduler.isTerminated())
            return true;
        long remaining = deadlineNanos - System.nanoTime();
        return remaining > 0 && scheduler.awaitTermination(remaining, TimeUnit.NANOSECONDS);
    }

    private static boolean tryLockUntil(ReentrantLock lock, long deadlineNanos)
            throws InterruptedException {
        long remaining = deadlineNanos - System.nanoTime();
        return remaining > 0 && lock.tryLock(remaining, TimeUnit.NANOSECONDS);
    }

    private boolean awaitWriterHandlesUntil(long deadlineNanos) throws InterruptedException {
        if (!tryLockUntil(writerHandleLock, deadlineNanos))
            return false;
        try {
            while (openWriterHandles != 0) {
                long remaining = deadlineNanos - System.nanoTime();
                if (remaining <= 0)
                    return false;
                noOpenWriterHandles.awaitNanos(remaining);
            }
            return true;
        } finally {
            writerHandleLock.unlock();
        }
    }

    boolean maintenanceSchedulersRunning() {
        return forceScheduler != null && lifecycleScheduler != null
                && !forceScheduler.isShutdown() && !lifecycleScheduler.isShutdown();
    }

    private void periodicMaintenance() {
        try {
            IOException failure = maintainActiveLanes(clock.instant());
            if (failure != null)
                throw failure;
        } catch (Exception e) {
            log.error("Daily-file journal periodic force/rollover failed", e);
        }
    }

    private void periodicLifecycleMaintenance() {
        try {
            maintainBoundedLifecycle(clock.instant());
        } catch (Exception e) {
            log.error("Daily-file journal bounded lifecycle maintenance failed", e);
        }
    }

    private void validateOrInitializeJournalMetadata() throws IOException {
        Path metadata = journalDirectory.resolve("journal-metadata.v1.json");
        Path migrationMarker = journalDirectory.resolve(LEGACY_MIGRATION_MARKER);
        if (Files.exists(migrationMarker, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Daily-file lifecycle-index migration is incomplete. Stop "
                    + "OSCAR and run com.botts.impl.service.dailyfile."
                    + "DailyFileJournalIndexMigration for " + rootDirectory + " before restart");
        }
        if (Files.exists(metadata, LinkOption.NOFOLLOW_LINKS)) {
            JsonObject json = readJsonObject(metadata, "journal metadata");
            if (requiredLong(json, "schemaVersion") != 1
                    || !zoneId.getId().equals(requiredText(json, "zoneId"))) {
                throw new IOException("Configured daily-file zone " + zoneId
                        + " does not match the existing journal metadata");
            }
            if (lifecycleIndexVersion(json) != LIFECYCLE_INDEX_VERSION) {
                throw new IOException("Existing daily-file data requires the offline lifecycle-index "
                        + "migration. Stop OSCAR and run com.botts.impl.service.dailyfile."
                        + "DailyFileJournalIndexMigration for " + rootDirectory);
            }
            return;
        }

        // Never turn startup into an O(total-history) migration. An early prototype journal that
        // predates this metadata must be indexed by the explicit offline migration procedure,
        // which validates every ledger before installing metadata and lifecycle work items.
        if (hasDirectoryEntries(ledgerDirectory) || hasDirectoryEntries(segmentsDirectory))
            throw new IOException("Existing daily-file data requires the offline lifecycle-index "
                    + "migration. Stop OSCAR and run com.botts.impl.service.dailyfile."
                    + "DailyFileJournalIndexMigration for " + rootDirectory);
        String json = "{\n"
                + "  \"schemaVersion\":1,\n"
                + "  \"zoneId\":\"" + jsonEscape(zoneId.getId()) + "\",\n"
                + "  \"lifecycleIndexVersion\":" + LIFECYCLE_INDEX_VERSION + "\n"
                + "}\n";
        atomicWrite(metadata, json.getBytes(StandardCharsets.UTF_8));
    }

    private static int lifecycleIndexVersion(JsonObject metadata) throws IOException {
        JsonElement value = metadata.get("lifecycleIndexVersion");
        if (value == null || value.isJsonNull())
            return 0;
        long version = requiredLong(metadata, "lifecycleIndexVersion");
        if (version != LIFECYCLE_INDEX_VERSION)
            throw new IOException("Unsupported daily-file lifecycle-index metadata version "
                    + version);
        return (int) version;
    }

    private static boolean hasDirectoryEntries(Path directory) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            return entries.iterator().hasNext();
        }
    }

    private static void validateLegacyLedgerZones(Path ledgerDirectory, ZoneId configuredZone)
            throws IOException {
        try (DirectoryStream<Path> laneDirectories = Files.newDirectoryStream(ledgerDirectory)) {
            for (Path laneDirectory : laneDirectories) {
                if (!Files.isDirectory(laneDirectory, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(laneDirectory))
                    continue;
                try (DirectoryStream<Path> ledgers = Files.newDirectoryStream(laneDirectory,
                        "*.json")) {
                    for (Path ledger : ledgers) {
                        JsonObject json = readJsonObject(ledger, "publication ledger");
                        if (!configuredZone.getId().equals(requiredText(json, "zoneId"))) {
                            throw new IOException("Configured daily-file zone " + configuredZone
                                    + " does not match existing ledger " + ledger);
                        }
                    }
                }
            }
        }
    }

    private static long indexLegacySegmentDirectories(Path segmentsDirectory,
                                                      Path lifecycleQueueDirectory,
                                                      Set<Path> durableEntries)
            throws IOException {
        long indexed = 0L;
        try (DirectoryStream<Path> laneDirectories = Files.newDirectoryStream(segmentsDirectory)) {
            for (Path laneDirectory : laneDirectories) {
                if (!Files.isDirectory(laneDirectory, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(laneDirectory)) {
                    throw new IOException("Legacy journal segment root contains a linked or "
                            + "unexpected lane entry: " + laneDirectory);
                }
                String laneId = readLaneIdFromDirectory(laneDirectory);
                if (!laneDirectory.getFileName().toString().equals(directoryId(laneId))) {
                    throw new IOException("Legacy journal lane identity is not bound to its "
                            + "directory: " + laneDirectory);
                }
                try (DirectoryStream<Path> dayDirectories = Files.newDirectoryStream(
                        laneDirectory)) {
                    for (Path dayDirectory : dayDirectories) {
                        if (dayDirectory.getFileName().toString().equals("lane.id"))
                            continue;
                        if (!Files.isDirectory(dayDirectory, LinkOption.NOFOLLOW_LINKS)
                                || Files.isSymbolicLink(dayDirectory)) {
                            throw new IOException("Legacy journal lane contains a linked or "
                                    + "unexpected day entry: " + dayDirectory);
                        }
                        String name = dayDirectory.getFileName().toString();
                        LifecycleWorkKind kind;
                        LocalDate day;
                        try {
                            if (name.startsWith(DELETION_PENDING_PREFIX)) {
                                kind = LifecycleWorkKind.DELETION;
                                day = LocalDate.parse(name.substring(
                                        DELETION_PENDING_PREFIX.length()));
                            } else if (name.startsWith(DELETION_STAGING_PREFIX)) {
                                kind = LifecycleWorkKind.DELETION;
                                day = LocalDate.parse(name.substring(
                                        DELETION_STAGING_PREFIX.length()));
                            } else {
                                kind = LifecycleWorkKind.RECOVERY;
                                day = LocalDate.parse(name);
                            }
                        } catch (RuntimeException invalidDay) {
                            throw new IOException("Legacy journal lane contains an unrecognized "
                                    + "day directory: " + dayDirectory, invalidDay);
                        }
                        validateLegacyDayEntries(dayDirectory);
                        if (writeMigrationLifecycleMarker(lifecycleQueueDirectory,
                                durableEntries, kind, laneId, day))
                            indexed++;
                    }
                }
            }
        }
        return indexed;
    }

    private static void validateLegacyDayEntries(Path dayDirectory) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dayDirectory)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (!isRegularFileNoFollow(entry) || Files.isSymbolicLink(entry)
                        || !name.matches("[0-9]{6}\\.(?:open|seg)")) {
                    throw new IOException("Legacy journal day contains a linked or unexpected "
                            + "segment entry: " + entry);
                }
                sequence(entry);
            }
        }
    }

    private static boolean writeMigrationLifecycleMarker(Path lifecycleQueueDirectory,
                                                         Set<Path> durableEntries,
                                                         LifecycleWorkKind kind,
                                                         String laneId, LocalDate day)
            throws IOException {
        String digest = HexFormat.of().formatHex(sha256Digest().digest(
                (laneId + "\u0000" + day).getBytes(StandardCharsets.UTF_8)));
        Path kindDirectory = lifecycleQueueDirectory.resolve(kind.directory);
        createConfinedDirectories(lifecycleQueueDirectory, kindDirectory, durableEntries,
                DailyFileJournalStore::forceDirectory);
        Path shardDirectory = kindDirectory.resolve(digest.substring(0, 2));
        createConfinedDirectories(kindDirectory, shardDirectory, durableEntries,
                DailyFileJournalStore::forceDirectory);
        Path marker = shardDirectory.resolve(digest + ".json");
        if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
            JsonObject existing = readJsonObject(marker, "daily-file lifecycle work item");
            if (requiredLong(existing, "schemaVersion") != 1
                    || !kind.directory.equals(requiredText(existing, "kind"))
                    || !laneId.equals(requiredText(existing, "laneId"))
                    || !day.toString().equals(requiredText(existing, "day"))) {
                throw new IOException("Daily-file lifecycle work identity collision: " + marker);
            }
            return false;
        }
        String json = "{\n"
                + "  \"schemaVersion\":1,\n"
                + "  \"kind\":\"" + kind.directory + "\",\n"
                + "  \"laneId\":\"" + jsonEscape(laneId) + "\",\n"
                + "  \"day\":\"" + day + "\",\n"
                + "  \"createdAt\":\"" + Instant.now() + "\"\n"
                + "}\n";
        atomicWriteNew(marker, json.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    private void sealRecoveredCompletedDays(String laneId, Path laneDir, LocalDate today) throws IOException {
        try (DirectoryStream<Path> dayDirs = Files.newDirectoryStream(laneDir)) {
            for (Path dayDir : dayDirs) {
                if (!Files.isDirectory(dayDir, LinkOption.NOFOLLOW_LINKS))
                    continue;
                LocalDate day;
                try {
                    day = LocalDate.parse(dayDir.getFileName().toString());
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (!day.isBefore(today))
                    continue;
                recoverHistoricalDay(laneId, day, dayDir);
            }
        }
    }

    /**
     * Recovers one completed day without racing a live writer for the same lane.
     *
     * <p>A lane can be active for today's records while older, crash-interrupted days still need
     * publication. The lane lock is held only long enough to compare the monotonically increasing
     * active day. Once the recovered day is older, its directory can be processed without pausing
     * live appends. A lightweight exact-day recovery claim remains set while the lane lock is
     * released, so even a concurrent rollover that resets the active day cannot race a late append
     * into the directory being published. Only an append targeting that claimed historical day
     * waits. Complete historical publications take the exact validated metadata fast path and are
     * never replayed merely because the process restarted.</p>
     */
    private void recoverHistoricalDay(String laneId, LocalDate day, Path dayDirectory)
            throws IOException {
        LaneState activeLane = lanes.get(laneId);
        boolean recoveryClaimed = false;
        if (activeLane != null) {
            activeLane.lock.lock();
            try {
                if (activeLane.historicalRecoveryDay != null)
                    return;
                if (activeLane.activeDay != null && !day.isBefore(activeLane.activeDay))
                    return;
                activeLane.historicalRecoveryDay = day;
                recoveryClaimed = true;
            } finally {
                activeLane.lock.unlock();
            }
        }

        try {
            List<Path> openSegments = listSegments(dayDirectory, ".open");
            for (Path segment : openSegments) {
                recoverOpenSegment(segment);
                sealSegmentFile(segment);
            }
            if (!openSegments.isEmpty() || !isPublishedDay(laneId, day))
                publishDay(laneId, day, dayDirectory);
            else
                completePublishedDayLifecycle(laneId, day);
        } finally {
            if (recoveryClaimed) {
                activeLane.lock.lock();
                try {
                    if (day.equals(activeLane.historicalRecoveryDay)) {
                        activeLane.historicalRecoveryDay = null;
                        activeLane.historicalRecoveryComplete.signalAll();
                    }
                } finally {
                    activeLane.lock.unlock();
                }
            }
        }
    }

    /**
     * Performs a fixed amount of historical work independently of the fsync cadence. Durable,
     * 256-way sharded work items make both enumeration and recovery proportional to this cycle's
     * work limit rather than to total retained history.
     */
    void maintainBoundedLifecycle(Instant now) throws IOException {
        lifecycleLock.lock();
        try {
            IOException failure = null;
            LocalDate today = LocalDate.ofInstant(now, zoneId);
            List<LifecycleWork> deletionWork = nextLifecycleWork(
                    LifecycleWorkKind.DELETION, LIFECYCLE_MAINTENANCE_DAY_LIMIT);
            for (LifecycleWork work : deletionWork) {
                try {
                    resumeStagedDeletion(work);
                } catch (IOException | RuntimeException e) {
                    failure = merge(failure,
                            new IOException("Unable to recover staged journal deletion "
                                    + work.laneId + "/" + work.day, e));
                }
            }

            List<LifecycleWork> recoveryWork = nextLifecycleWork(
                    LifecycleWorkKind.RECOVERY, LIFECYCLE_MAINTENANCE_DAY_LIMIT);
            for (LifecycleWork work : recoveryWork) {
                Path dayDirectory = segmentDayDirectory(work.laneId, work.day);
                if (!Files.exists(dayDirectory, LinkOption.NOFOLLOW_LINKS)) {
                    removeLifecycleWork(work);
                    continue;
                }
                if (!work.day.isBefore(today))
                    continue;
                try {
                    recoverHistoricalDay(work.laneId, work.day, dayDirectory);
                } catch (IOException | RuntimeException e) {
                    failure = merge(failure,
                            new IOException("Unable to maintain recovered journal day "
                                    + dayDirectory, e));
                }
            }

            if (sealedJournalRetentionDays != 0) {
                List<LifecycleWork> purgeWork = nextLifecycleWork(
                        LifecycleWorkKind.PURGE, LIFECYCLE_MAINTENANCE_DAY_LIMIT);
                for (LifecycleWork work : purgeWork) {
                    Path dayDirectory = segmentDayDirectory(work.laneId, work.day);
                    if (!Files.exists(dayDirectory, LinkOption.NOFOLLOW_LINKS)) {
                        removeLifecycleWork(work);
                        continue;
                    }
                    try {
                        DayWindow window = DayWindow.forDate(work.day, zoneId);
                        if (!isSealedDayDeletionCandidate(work.laneId, window, now))
                            continue;
                        if (freezeAuthorizeAndDelete(work, dayDirectory, window)) {
                            log.info("Deleted eligible sealed journal segments for lane {} day {}",
                                    work.laneId, work.day);
                        }
                    } catch (IOException | RuntimeException e) {
                        failure = merge(failure,
                                new IOException("Unable to purge sealed journal "
                                        + dayDirectory, e));
                    }
                }
            }
            writeLifecycleCursors();
            if (failure != null)
                throw failure;
        } finally {
            lifecycleLock.unlock();
        }
    }

    private boolean isPublishedDay(String laneId, LocalDate day) {
        DayWindow window = DayWindow.forDate(day, zoneId);
        return isPublished(laneId, window.startInclusive, window.endExclusive);
    }

    private void completePublishedDayLifecycle(String laneId, LocalDate day) throws IOException {
        if (sealedJournalRetentionDays != 0)
            enqueueLifecycleWork(LifecycleWorkKind.PURGE, laneId, day);
        removeLifecycleWork(LifecycleWorkKind.RECOVERY, laneId, day);
    }

    /** Package-visible hook for an explicit, bounded legacy-index migration tool or test. */
    void indexJournalDayForRecovery(String laneId, LocalDate day) throws IOException {
        enqueueLifecycleWork(LifecycleWorkKind.RECOVERY, requireLaneId(laneId),
                Objects.requireNonNull(day, "day"));
    }

    /** Package-visible crash-fixture hook; production creates this marker before staging. */
    void indexStagedDeletionForRecovery(String laneId, LocalDate day) throws IOException {
        enqueueLifecycleWork(LifecycleWorkKind.DELETION, requireLaneId(laneId),
                Objects.requireNonNull(day, "day"));
    }

    void setDeletionFreezeHookForTest(Runnable hook) {
        deletionFreezeHook = Objects.requireNonNull(hook, "hook");
    }

    private LifecycleWork enqueueLifecycleWork(LifecycleWorkKind kind, String laneId,
                                               LocalDate day) throws IOException {
        String checkedLaneId = requireLaneId(laneId);
        String digest = HexFormat.of().formatHex(sha256Digest().digest(
                (checkedLaneId + "\u0000" + day).getBytes(StandardCharsets.UTF_8)));
        List<Path> existingMarkers = lifecycleWorkMarkers(kind, digest);
        if (!existingMarkers.isEmpty()) {
            LifecycleWork existing = readLifecycleWork(kind, existingMarkers.get(0));
            if (!existing.laneId.equals(checkedLaneId) || !existing.day.equals(day))
                throw new IOException("Daily-file lifecycle work identity collision: "
                        + existing.marker);
            for (int index = 1; index < existingMarkers.size(); index++) {
                LifecycleWork duplicate = readLifecycleWork(kind, existingMarkers.get(index));
                if (!duplicate.laneId.equals(checkedLaneId) || !duplicate.day.equals(day))
                    throw new IOException("Daily-file lifecycle work identity collision: "
                            + duplicate.marker);
            }
            return existing;
        }
        Path kindDirectory = lifecycleQueueDirectory.resolve(kind.directory);
        createConfinedDirectories(lifecycleQueueDirectory, kindDirectory);
        Path shardDirectory = kindDirectory.resolve(digest.substring(0, 2));
        createConfinedDirectories(kindDirectory, shardDirectory);
        Path marker = shardDirectory.resolve(digest + ".json");
        LifecycleWork work = new LifecycleWork(kind, checkedLaneId, day, marker);
        String json = "{\n"
                + "  \"schemaVersion\":1,\n"
                + "  \"kind\":\"" + kind.directory + "\",\n"
                + "  \"laneId\":\"" + jsonEscape(checkedLaneId) + "\",\n"
                + "  \"day\":\"" + day + "\",\n"
                + "  \"createdAt\":\"" + clock.instant() + "\"\n"
                + "}\n";
        try {
            atomicWriteNew(marker, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // An equivalent marker written by another process is idempotent; any different or
            // malformed occupant still fails closed through the identity validation.
            if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS))
                throw e;
            LifecycleWork existing = readLifecycleWork(kind, marker);
            if (!existing.laneId.equals(checkedLaneId) || !existing.day.equals(day))
                throw e;
            return existing;
        }
        return work;
    }

    private LifecycleWork readLifecycleWork(LifecycleWorkKind expectedKind, Path marker)
            throws IOException {
        JsonObject json = readJsonObject(marker, "daily-file lifecycle work item");
        if (requiredLong(json, "schemaVersion") != 1
                || !expectedKind.directory.equals(requiredText(json, "kind")))
            throw new IOException("Daily-file lifecycle work type mismatch: " + marker);
        String laneId = requireLaneId(requiredText(json, "laneId"));
        LocalDate day;
        try {
            day = LocalDate.parse(requiredText(json, "day"));
            Instant.parse(requiredText(json, "createdAt"));
        } catch (RuntimeException e) {
            throw new IOException("Invalid daily-file lifecycle work item: " + marker, e);
        }
        String digest = HexFormat.of().formatHex(sha256Digest().digest(
                (laneId + "\u0000" + day).getBytes(StandardCharsets.UTF_8)));
        if (!marker.getFileName().toString().equals(digest + ".json"))
            throw new IOException("Daily-file lifecycle work key mismatch: " + marker);
        return new LifecycleWork(expectedKind, laneId, day, marker);
    }

    private List<LifecycleWork> nextLifecycleWork(LifecycleWorkKind kind, int limit)
            throws IOException {
        if (limit <= 0)
            return List.of();
        int startingShard = lifecycleCursor(kind);
        List<LifecycleWork> result = new ArrayList<>(limit);
        Set<String> offeredMarkers = new HashSet<>();
        Path kindDirectory = lifecycleQueueDirectory.resolve(kind.directory);
        int inspected = 0;
        for (int offset = 0; offset < LIFECYCLE_QUEUE_SHARDS && inspected < limit; offset++) {
            int shard = (startingShard + offset) % LIFECYCLE_QUEUE_SHARDS;
            Path shardDirectory = kindDirectory.resolve(String.format(Locale.ROOT, "%02x", shard));
            if (!Files.isDirectory(shardDirectory, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(shardDirectory))
                continue;
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(shardDirectory,
                    "*.json")) {
                for (Path marker : entries) {
                    if (inspected++ >= limit)
                        break;
                    if (!offeredMarkers.add(marker.getFileName().toString()))
                        continue;
                    try {
                        LifecycleWork work = readLifecycleWork(kind, marker);
                        result.add(rotateLifecycleWork(work, shard));
                    } catch (IOException malformed) {
                        quarantineLifecycleWork(kind, marker, malformed);
                    }
                }
            }
            // If the bounded page filled inside this shard, resume the same shard next cycle.
            // Selected entries were already rotated elsewhere, so the next page exposes work
            // that had been hidden behind them without rescanning total retained history.
            setLifecycleCursor(kind, inspected >= limit
                    ? shard : (shard + 1) % LIFECYCLE_QUEUE_SHARDS);
        }
        if (inspected == 0)
            setLifecycleCursor(kind, (startingShard + 1) % LIFECYCLE_QUEUE_SHARDS);
        return result;
    }

    private void removeLifecycleWork(LifecycleWork work) throws IOException {
        removeLifecycleWork(work.kind, work.laneId, work.day);
    }

    private void removeLifecycleWork(LifecycleWorkKind kind, String laneId, LocalDate day)
            throws IOException {
        String digest = HexFormat.of().formatHex(sha256Digest().digest(
                (laneId + "\u0000" + day).getBytes(StandardCharsets.UTF_8)));
        for (Path marker : lifecycleWorkMarkers(kind, digest)) {
            if (Files.deleteIfExists(marker))
                forceDirectory(marker.getParent());
        }
    }

    /**
     * Moves selected work behind the durable shard cursor before returning it. This is the queue's
     * retry rotation: an ineligible or failing item remains durable, but cannot occupy the first
     * slot in a shard forever and hide later work. The destination is the shard immediately behind
     * the scan cursor, so every marker left in the source shard is observed before this item is
     * offered again. Enumeration and identity lookup remain bounded by the fixed 256-shard fanout.
     */
    private LifecycleWork rotateLifecycleWork(LifecycleWork work, int sourceShard)
            throws IOException {
        int targetShard = (sourceShard + LIFECYCLE_QUEUE_SHARDS - 1)
                % LIFECYCLE_QUEUE_SHARDS;
        Path kindDirectory = lifecycleQueueDirectory.resolve(work.kind.directory);
        Path targetDirectory = kindDirectory.resolve(String.format(Locale.ROOT, "%02x",
                targetShard));
        createConfinedDirectories(kindDirectory, targetDirectory);
        Path target = targetDirectory.resolve(work.marker.getFileName());
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            LifecycleWork duplicate = readLifecycleWork(work.kind, target);
            if (!duplicate.laneId.equals(work.laneId) || !duplicate.day.equals(work.day))
                throw new IOException("Daily-file lifecycle retry identity collision: " + target);
            if (Files.deleteIfExists(work.marker))
                forceDirectory(work.marker.getParent());
            return duplicate;
        }
        atomicMoveNew(work.marker, target);
        forceDirectory(work.marker.getParent());
        forceDirectory(targetDirectory);
        return new LifecycleWork(work.kind, work.laneId, work.day, target);
    }

    private void quarantineLifecycleWork(LifecycleWorkKind kind, Path marker,
                                         IOException failure) throws IOException {
        Path kindDirectory = lifecycleQueueDirectory.resolve(kind.directory);
        Path quarantine = kindDirectory.resolve("quarantine");
        createConfinedDirectories(kindDirectory, quarantine);
        Path target = quarantine.resolve(marker.getFileName() + ".invalid-" + UUID.randomUUID());
        atomicMoveNew(marker, target);
        forceDirectory(marker.getParent());
        forceDirectory(quarantine);
        log.error("Quarantined malformed daily-file lifecycle marker {} as {}",
                marker, target, failure);
    }

    private List<Path> lifecycleWorkMarkers(LifecycleWorkKind kind, String digest)
            throws IOException {
        List<Path> markers = new ArrayList<>(1);
        Path kindDirectory = lifecycleQueueDirectory.resolve(kind.directory);
        if (!Files.exists(kindDirectory, LinkOption.NOFOLLOW_LINKS))
            return markers;
        if (!Files.isDirectory(kindDirectory, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(kindDirectory))
            throw new IOException("Daily-file lifecycle queue is linked or not a directory: "
                    + kindDirectory);
        for (int shard = 0; shard < LIFECYCLE_QUEUE_SHARDS; shard++) {
            Path marker = kindDirectory.resolve(String.format(Locale.ROOT, "%02x", shard))
                    .resolve(digest + ".json");
            if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS))
                markers.add(marker);
        }
        return markers;
    }

    private void readLifecycleCursors() throws IOException {
        if (!Files.exists(lifecycleCursorPath, LinkOption.NOFOLLOW_LINKS))
            return;
        JsonObject json = readJsonObject(lifecycleCursorPath,
                "daily-file lifecycle cursor");
        if (requiredLong(json, "schemaVersion") != 1)
            throw new IOException("Unsupported daily-file lifecycle cursor version");
        recoveryMaintenanceShard = checkedLifecycleShard(json, "recoveryShard");
        purgeMaintenanceShard = checkedLifecycleShard(json, "purgeShard");
        deletionMaintenanceShard = checkedLifecycleShard(json, "deletionShard");
    }

    private int checkedLifecycleShard(JsonObject json, String field) throws IOException {
        long value = requiredLong(json, field);
        if (value < 0 || value >= LIFECYCLE_QUEUE_SHARDS)
            throw new IOException("Invalid daily-file lifecycle cursor " + field);
        return (int) value;
    }

    private void writeLifecycleCursors() throws IOException {
        String json = "{\n"
                + "  \"schemaVersion\":1,\n"
                + "  \"recoveryShard\":" + recoveryMaintenanceShard + ",\n"
                + "  \"purgeShard\":" + purgeMaintenanceShard + ",\n"
                + "  \"deletionShard\":" + deletionMaintenanceShard + ",\n"
                + "  \"updatedAt\":\"" + clock.instant() + "\"\n"
                + "}\n";
        atomicWrite(lifecycleCursorPath, json.getBytes(StandardCharsets.UTF_8));
    }

    private int lifecycleCursor(LifecycleWorkKind kind) {
        return switch (kind) {
            case RECOVERY -> recoveryMaintenanceShard;
            case PURGE -> purgeMaintenanceShard;
            case DELETION -> deletionMaintenanceShard;
        };
    }

    private void setLifecycleCursor(LifecycleWorkKind kind, int value) {
        switch (kind) {
            case RECOVERY -> recoveryMaintenanceShard = value;
            case PURGE -> purgeMaintenanceShard = value;
            case DELETION -> deletionMaintenanceShard = value;
        }
    }

    private void publishDay(String laneId, LocalDate day, Path dayDirectory) throws IOException {
        DayWindow window = DayWindow.forDate(day, zoneId);
        List<Path> segments = listSegments(dayDirectory, ".seg");
        if (segments.isEmpty())
            return;

        String fileId = publicationFileId(laneId);
        String csvName = fileId + "_" + day + ".csv";
        String manifestName = csvName + ".manifest.v" + MANIFEST_VERSION + ".json";
        Path csv = publicationDirectory.resolve(csvName);
        Path manifest = publicationDirectory.resolve(manifestName);
        Path csvPartial = publicationDirectory.resolve("." + csvName + ".partial-" + UUID.randomUUID());

        MessageDigest digest = sha256Digest();
        long recordCount = 0;
        try (FileChannel out = FileChannel.open(csvPartial, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            for (Path segment : segments) {
                recordCount += scanRecords(segment, record -> {
                    if (record.timestamp().isBefore(window.startInclusive)
                            || !record.timestamp().isBefore(window.endExclusive)) {
                        throw new IOException("Journal record " + record.timestamp()
                                + " is outside immutable day window " + window);
                    }
                    byte[] line = renderLegacyLine(record);
                    digest.update(line);
                    writeFully(out, ByteBuffer.wrap(line));
                });
            }
            out.force(true);
        } catch (Exception e) {
            Files.deleteIfExists(csvPartial);
            if (e instanceof IOException io)
                throw io;
            throw new IOException("Unable to publish daily file " + csvName, e);
        }

        String checksum = HexFormat.of().formatHex(digest.digest());
        long byteCount = Files.size(csvPartial);
        if (Files.exists(csv, LinkOption.NOFOLLOW_LINKS)) {
            if (!isRegularFileNoFollow(csv)) {
                Files.deleteIfExists(csvPartial);
                throw new IOException("Refusing linked or non-regular immutable daily file " + csv);
            }
            String existingChecksum = sha256(csv);
            if (fileSizeNoFollow(csv) != byteCount || !existingChecksum.equals(checksum)) {
                Files.deleteIfExists(csvPartial);
                throw new IOException("Refusing to replace immutable daily file " + csv
                        + ": existing checksum does not match recovered journal");
            }
            Files.delete(csvPartial);
        } else {
            try {
                atomicMoveNew(csvPartial, csv);
            } catch (IOException e) {
                Files.deleteIfExists(csvPartial);
                throw e;
            }
            forceDirectory(publicationDirectory);
        }

        String manifestJson = manifestJson(laneId, window, csvName, recordCount, byteCount, checksum,
                clock.instant());
        if (Files.exists(manifest, LinkOption.NOFOLLOW_LINKS)) {
            JsonObject existing = readJsonObject(manifest, "daily-file manifest");
            if (requiredLong(existing, "schemaVersion") != MANIFEST_VERSION
                    || !"OSCAR_LEGACY_DAILY_CSV".equals(requiredText(existing, "format"))
                    || !checksum.equals(requiredText(existing, "sha256"))
                    || requiredLong(existing, "recordCount") != recordCount
                    || requiredLong(existing, "byteCount") != byteCount
                    || !laneId.equals(requiredText(existing, "laneId"))
                    || !zoneId.getId().equals(requiredText(existing, "zoneId"))
                    || !csvName.equals(requiredText(existing, "dataFile"))
                    || !"SHA-256".equals(requiredText(existing, "checksumAlgorithm"))
                    || !window.startInclusive.toString().equals(requiredText(existing,
                    "windowStartInclusive"))
                    || !window.endExclusive.toString().equals(requiredText(existing,
                    "windowEndExclusive"))) {
                throw new IOException("Existing manifest does not match immutable daily file " + csv);
            }
        } else {
            atomicWrite(manifest, manifestJson.getBytes(StandardCharsets.UTF_8));
            forceDirectory(publicationDirectory);
        }

        writeLedger(laneId, window, "PUBLISHED", recordCount, csvName, manifestName, checksum, byteCount);
        completePublishedDayLifecycle(laneId, day);
        log.info("Published daily file {} with {} record(s), SHA-256 {}", csv, recordCount, checksum);
    }

    private byte[] renderLegacyLine(DailyFileRecord record) {
        String localTime = LEGACY_TIME.withZone(zoneId).format(record.timestamp());
        String utcTime = LEGACY_TIME.withZone(ZoneOffset.UTC).format(record.timestamp());
        return (record.rawLine() + "," + localTime + "," + utcTime + "\r\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Validates and visits one sealed segment without retaining decoded records.  Capturing the
     * size once makes the scan finite even if a misconfigured second process appends to the file;
     * the final comparison then rejects that mutation instead of publishing a partial snapshot.
     */
    private long scanRecords(Path segment, RecordVisitor visitor) throws IOException {
        if (!isRegularFileNoFollow(segment))
            throw new IOException("Refusing linked or non-regular journal segment: " + segment);
        try (FileChannel channel = FileChannel.open(segment, StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            long segmentSize = channel.size();
            validateHeader(channel, segment);
            channel.position(SEGMENT_HEADER_BYTES);
            long recordCount = 0;
            ByteBuffer frameHeader = ByteBuffer.allocate(FRAME_HEADER_BYTES);
            while (channel.position() < segmentSize) {
                long frameStart = channel.position();
                if (segmentSize - frameStart < FRAME_HEADER_BYTES)
                    throw new IOException("Journal segment has a truncated frame header: "
                            + segment + " at offset " + frameStart);
                frameHeader.clear();
                readFully(channel, frameHeader);
                frameHeader.flip();
                int length = frameHeader.getInt();
                int expectedCrc = frameHeader.getInt();
                validatePayloadLength(length, segment, frameStart);
                if (segmentSize - channel.position() < length)
                    throw new IOException("Journal segment has a truncated frame payload: "
                            + segment + " at offset " + frameStart);
                ByteBuffer payload = ByteBuffer.allocate(length);
                readFully(channel, payload);
                byte[] bytes = payload.array();
                if (crc32c(bytes) != expectedCrc)
                    throw new IOException("CRC mismatch in " + segment + " at offset " + frameStart);
                visitor.visit(decodeRecord(bytes));
                recordCount++;
            }
            if (channel.size() != segmentSize)
                throw new IOException("Journal segment changed while it was being scanned: " + segment);
            return recordCount;
        }
    }

    @FunctionalInterface
    private interface RecordVisitor {
        void visit(DailyFileRecord record) throws IOException;
    }

    private RecoveryResult recoverOpenSegment(Path segment) throws IOException {
        try (FileChannel channel = FileChannel.open(segment, StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            validateHeader(channel, segment);
            long size = channel.size();
            long validEnd = SEGMENT_HEADER_BYTES;
            long records = 0;
            channel.position(SEGMENT_HEADER_BYTES);
            while (channel.position() < size) {
                long frameStart = channel.position();
                if (size - frameStart < FRAME_HEADER_BYTES) {
                    truncatePartialTail(channel, segment, validEnd, size);
                    break;
                }
                ByteBuffer frameHeader = ByteBuffer.allocate(FRAME_HEADER_BYTES);
                readFully(channel, frameHeader);
                frameHeader.flip();
                int length = frameHeader.getInt();
                int expectedCrc = frameHeader.getInt();
                validatePayloadLength(length, segment, frameStart);
                if (size - channel.position() < length) {
                    truncatePartialTail(channel, segment, validEnd, size);
                    break;
                }
                ByteBuffer payload = ByteBuffer.allocate(length);
                readFully(channel, payload);
                if (crc32c(payload.array()) != expectedCrc)
                    throw new IOException("CRC mismatch in complete frame " + segment
                            + " at offset " + frameStart);
                decodeRecord(payload.array());
                validEnd = channel.position();
                records++;
            }
            return new RecoveryResult(validEnd, records, readHeaderCreatedAt(channel, segment));
        }
    }

    private void truncatePartialTail(FileChannel channel, Path segment, long validEnd, long oldSize)
            throws IOException {
        channel.truncate(validEnd);
        channel.force(true);
        log.warn("Recovered truncated daily-file journal tail in {} ({} -> {} bytes)",
                segment, oldSize, validEnd);
    }

    private DailyFileRecord decodeRecord(byte[] payload) throws IOException {
        if (payload.length < RECORD_TIMESTAMP_BYTES)
            throw new IOException("Journal payload is shorter than its timestamp header");
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        long epochSecond = buffer.getLong();
        int nano = buffer.getInt();
        if (nano < 0 || nano > 999_999_999)
            throw new IOException("Invalid journal timestamp nanoseconds: " + nano);
        byte[] raw = new byte[buffer.remaining()];
        buffer.get(raw);
        return new DailyFileRecord(Instant.ofEpochSecond(epochSecond, nano),
                new String(raw, StandardCharsets.UTF_8));
    }

    private byte[] encodeRecord(DailyFileRecord record) throws IOException {
        byte[] raw = record.rawLine().getBytes(StandardCharsets.UTF_8);
        if (raw.length > maxRecordBytes)
            throw new IOException("Daily-file record is " + raw.length + " bytes; configured maximum is "
                    + maxRecordBytes);
        ByteBuffer payload = ByteBuffer.allocate(RECORD_TIMESTAMP_BYTES + raw.length);
        payload.putLong(record.timestamp().getEpochSecond());
        payload.putInt(record.timestamp().getNano());
        payload.put(raw);
        return payload.array();
    }

    private void validatePayloadLength(int length, Path segment, long offset) throws IOException {
        if (length < RECORD_TIMESTAMP_BYTES || length > RECORD_TIMESTAMP_BYTES + maxRecordBytes)
            throw new IOException("Invalid frame length " + length + " in " + segment + " at offset " + offset);
    }

    private void validateHeader(FileChannel channel, Path segment) throws IOException {
        if (channel.size() < SEGMENT_HEADER_BYTES)
            throw new IOException("Journal segment has a truncated header: " + segment);
        ByteBuffer header = ByteBuffer.allocate(SEGMENT_HEADER_BYTES);
        channel.position(0);
        readFully(channel, header);
        header.flip();
        if (header.getInt() != SEGMENT_MAGIC)
            throw new IOException("Journal segment has invalid magic: " + segment);
        int version = header.getInt();
        if (version != SEGMENT_VERSION)
            throw new IOException("Unsupported journal segment version " + version + ": " + segment);
    }

    private Instant readHeaderCreatedAt(FileChannel channel, Path segment) throws IOException {
        validateHeader(channel, segment);
        ByteBuffer created = ByteBuffer.allocate(Long.BYTES);
        channel.position(Integer.BYTES * 2L);
        readFully(channel, created);
        created.flip();
        return Instant.ofEpochMilli(created.getLong());
    }

    private void writeSegmentHeader(FileChannel channel, Instant createdAt) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(SEGMENT_HEADER_BYTES);
        header.putInt(SEGMENT_MAGIC);
        header.putInt(SEGMENT_VERSION);
        header.putLong(createdAt.toEpochMilli());
        header.flip();
        writeFully(channel, header);
        channel.force(true);
    }

    private void sealSegmentFile(Path openSegment) throws IOException {
        if (!openSegment.getFileName().toString().endsWith(".open"))
            throw new IOException("Not an open journal segment: " + openSegment);
        String fileName = openSegment.getFileName().toString();
        Path sealed = openSegment.resolveSibling(fileName.substring(0, fileName.length() - 5) + ".seg");
        atomicMoveNew(openSegment, sealed);
        forceDirectory(openSegment.getParent());
    }

    private List<Path> listSegments(Path directory, String suffix) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(directory))
            return List.of();
        try (var paths = Files.list(directory)) {
            List<Path> matching = paths.filter(path -> path.getFileName().toString()
                            .endsWith(suffix))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
            for (Path path : matching) {
                if (!isRegularFileNoFollow(path))
                    throw new IOException("Refusing linked or non-regular journal segment: " + path);
            }
            return matching;
        }
    }

    private void writeLedger(String laneId, DayWindow window, String state, long recordCount,
                             String dataFile, String manifestFile, String checksum, long byteCount)
            throws IOException {
        String json = "{\n"
                + "  \"schemaVersion\":1,\n"
                + "  \"laneId\":\"" + jsonEscape(laneId) + "\",\n"
                + "  \"day\":\"" + window.day + "\",\n"
                + "  \"zoneId\":\"" + jsonEscape(zoneId.getId()) + "\",\n"
                + "  \"windowStartInclusive\":\"" + window.startInclusive + "\",\n"
                + "  \"windowEndExclusive\":\"" + window.endExclusive + "\",\n"
                + "  \"state\":\"" + state + "\",\n"
                + "  \"recordCount\":" + recordCount + ",\n"
                + "  \"byteCount\":" + byteCount + ",\n"
                + "  \"sha256\":" + nullableJson(checksum) + ",\n"
                + "  \"dataFile\":" + nullableJson(dataFile) + ",\n"
                + "  \"manifestFile\":" + nullableJson(manifestFile) + ",\n"
                + "  \"updatedAt\":\"" + clock.instant() + "\"\n"
                + "}\n";
        atomicWrite(ledgerPath(laneId, window.day), json.getBytes(StandardCharsets.UTF_8));
    }

    private String manifestJson(String laneId, DayWindow window, String dataFile, long recordCount,
                                long byteCount, String checksum, Instant publishedAt) {
        return "{\n"
                + "  \"schemaVersion\":" + MANIFEST_VERSION + ",\n"
                + "  \"format\":\"OSCAR_LEGACY_DAILY_CSV\",\n"
                + "  \"laneId\":\"" + jsonEscape(laneId) + "\",\n"
                + "  \"zoneId\":\"" + jsonEscape(zoneId.getId()) + "\",\n"
                + "  \"windowStartInclusive\":\"" + window.startInclusive + "\",\n"
                + "  \"windowEndExclusive\":\"" + window.endExclusive + "\",\n"
                + "  \"recordCount\":" + recordCount + ",\n"
                + "  \"byteCount\":" + byteCount + ",\n"
                + "  \"checksumAlgorithm\":\"SHA-256\",\n"
                + "  \"sha256\":\"" + checksum + "\",\n"
                + "  \"dataFile\":\"" + jsonEscape(dataFile) + "\",\n"
                + "  \"publishedAt\":\"" + publishedAt + "\"\n"
                + "}\n";
    }

    private Path ledgerPath(String laneId, LocalDate day) throws IOException {
        Path laneLedgerDirectory = ledgerDirectory.resolve(directoryId(laneId));
        createConfinedDirectories(ledgerDirectory, laneLedgerDirectory);
        return laneLedgerDirectory.resolve(day + ".json");
    }

    private Path backupConfirmationPath(String laneId, LocalDate day) throws IOException {
        Path laneBackupDirectory = backupConfirmationDirectory.resolve(directoryId(laneId));
        createConfinedDirectories(backupConfirmationDirectory, laneBackupDirectory);
        return laneBackupDirectory.resolve(day + ".json");
    }

    private Path observationPurgeCheckpointPath(String feedId) {
        String digest = shortHash(requireFeedId(feedId));
        return observationPurgeCheckpointDirectory.resolve(digest + ".json");
    }

    private Path laneDirectory(String laneId) throws IOException {
        Path directory = segmentsDirectory.resolve(directoryId(laneId));
        createConfinedDirectories(segmentsDirectory, directory);
        Path idFile = directory.resolve("lane.id");
        if (Files.exists(idFile, LinkOption.NOFOLLOW_LINKS)) {
            if (!isRegularFileNoFollow(idFile) || fileSizeNoFollow(idFile) > MAX_IDENTIFIER_BYTES)
                throw new IOException("Journal lane identity is linked, non-regular, or oversized: "
                        + idFile);
            String existing = readBoundedUtf8(idFile, MAX_IDENTIFIER_BYTES,
                    "journal lane identity");
            if (!existing.equals(laneId))
                throw new IOException("Journal lane directory collision between '" + laneId
                        + "' and '" + existing + "'");
        } else {
            atomicWrite(idFile, laneId.getBytes(StandardCharsets.UTF_8));
        }
        return directory;
    }

    private String readLaneId(Path laneDirectory) throws IOException {
        return readLaneIdFromDirectory(laneDirectory);
    }

    private static String readLaneIdFromDirectory(Path laneDirectory) throws IOException {
        Path idFile = laneDirectory.resolve("lane.id");
        if (!Files.exists(idFile, LinkOption.NOFOLLOW_LINKS))
            return requireLaneId(laneDirectory.getFileName().toString());
        if (!isRegularFileNoFollow(idFile) || fileSizeNoFollow(idFile) > MAX_IDENTIFIER_BYTES)
            throw new IOException("Journal lane identity is linked, non-regular, or oversized: "
                    + idFile);
        return requireLaneId(readBoundedUtf8(idFile, MAX_IDENTIFIER_BYTES,
                "journal lane identity"));
    }

    private static String requireLaneId(String laneId) {
        if (laneId == null || laneId.isBlank() || laneId.equals(".") || laneId.equals("..")
                || laneId.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("laneId is invalid");
        if (laneId.getBytes(StandardCharsets.UTF_8).length > MAX_IDENTIFIER_BYTES)
            throw new IllegalArgumentException("laneId is too long");
        return laneId;
    }

    private static String requireFeedId(String feedId) {
        if (feedId == null || feedId.isBlank()
                || feedId.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("feedId is invalid");
        if (feedId.getBytes(StandardCharsets.UTF_8).length > MAX_IDENTIFIER_BYTES)
            throw new IllegalArgumentException("feedId is too long");
        return feedId;
    }

    static String directoryId(String laneId) {
        String safe = laneId.replaceAll("[^A-Za-z0-9._-]", "_");
        boolean needsHash = !safe.equals(laneId);
        if (safe.isBlank() || safe.equals(".") || safe.equals("..")) {
            safe = "lane";
            needsHash = true;
        }
        if (safe.length() > 120) {
            safe = safe.substring(0, 120);
            needsHash = true;
        }
        if (needsHash)
            safe += "-" + shortHash(laneId);
        return safe;
    }

    static String publicationFileId(String laneId) {
        String suffix = laneId.contains(":") ? laneId.substring(laneId.lastIndexOf(':') + 1) : laneId;
        String safe = suffix.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank())
            safe = "lane";
        if (safe.length() > 120)
            safe = safe.substring(0, 120);
        // The human-readable suffix alone is not unique (different full UIDs commonly end in the
        // same lane name). Always bind the published name to the complete stable UID.
        return safe + "-" + shortHash(laneId);
    }

    private static String shortHash(String value) {
        return HexFormat.of().formatHex(sha256Digest().digest(value.getBytes(StandardCharsets.UTF_8)), 0, 6);
    }

    static String sha256(Path path) throws IOException {
        if (!isRegularFileNoFollow(path))
            throw new IOException("Refusing to hash linked or non-regular file: " + path);
        MessageDigest digest = sha256Digest();
        try (var input = Files.newInputStream(path, StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0)
                    digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
        }
    }

    private static int crc32c(byte[] payload) {
        CRC32C crc = new CRC32C();
        crc.update(payload, 0, payload.length);
        return (int) crc.getValue();
    }

    private static void atomicWrite(Path target, byte[] contents) throws IOException {
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(parent))
            throw new IOException("Refusing to write through a linked or missing directory: "
                    + parent);
        Path partial = target.resolveSibling("." + target.getFileName() + ".partial-" + UUID.randomUUID());
        try (FileChannel channel = FileChannel.open(partial, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            writeFully(channel, ByteBuffer.wrap(contents));
            channel.force(true);
        } catch (IOException e) {
            Files.deleteIfExists(partial);
            throw e;
        }
        try {
            atomicReplace(partial, target);
        } catch (IOException e) {
            Files.deleteIfExists(partial);
            throw e;
        }
        forceDirectory(target.getParent());
    }

    private static void atomicWriteNew(Path target, byte[] contents) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Refusing to replace existing immutable journal artifact "
                    + target);
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(parent))
            throw new IOException("Refusing to write through a linked or missing directory: "
                    + parent);
        Path partial = target.resolveSibling("." + target.getFileName() + ".partial-"
                + UUID.randomUUID());
        try (FileChannel channel = FileChannel.open(partial, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            writeFully(channel, ByteBuffer.wrap(contents));
            channel.force(true);
        } catch (IOException e) {
            Files.deleteIfExists(partial);
            throw e;
        }
        try {
            atomicMoveNew(partial, target);
        } catch (IOException e) {
            Files.deleteIfExists(partial);
            throw e;
        }
        forceDirectory(parent);
    }

    private static void atomicMoveNew(Path source, Path target) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Refusing to replace existing immutable journal artifact " + target);
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            throw new IOException("Filesystem does not support the atomic rename required by the daily-file journal: "
                    + source + " -> " + target, e);
        }
    }

    private static void atomicReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            throw new IOException("Filesystem does not support the atomic replacement required by the daily-file journal: "
                    + source + " -> " + target, e);
        }
    }

    private static void forceDirectory(Path directory) throws IOException {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (UnsupportedOperationException e) {
            // Directory fsync is unavailable on some supported filesystem/JDK combinations.
        } catch (IOException e) {
            // Windows commonly rejects opening a directory as a FileChannel. Ignore only that
            // platform-specific limitation; permission and all other failures remain visible on
            // filesystems where directory fsync is supported.
            if (!isIgnorableWindowsDirectoryFsyncFailure(e, isWindows()))
                throw e;
        }
    }

    static boolean isIgnorableWindowsDirectoryFsyncFailure(IOException failure,
                                                             boolean windows) {
        return windows && failure instanceof AccessDeniedException;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
                .startsWith("windows");
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (channel.write(buffer) < 0)
                throw new EOFException("Unexpected end of file while writing journal");
        }
    }

    private static void readFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0)
                throw new EOFException("Unexpected end of journal segment");
        }
    }

    private static Path resolveChild(Path root, String child, String fieldName) {
        if (child == null || child.isBlank())
            throw new IllegalArgumentException(fieldName + " must not be blank");
        Path configured = Path.of(child);
        if (configured.isAbsolute())
            throw new IllegalArgumentException(fieldName + " must be relative to rootDirectory");
        Path resolved = root.resolve(configured).normalize();
        if (!resolved.startsWith(root))
            throw new IllegalArgumentException(fieldName + " must stay inside rootDirectory");
        return resolved;
    }

    private static Path initializeRoot(Path root) throws IOException {
        Path configured = Objects.requireNonNull(root, "rootDirectory")
                .toAbsolutePath().normalize();
        createDirectoryTreeDurably(configured);
        if (!Files.isDirectory(configured))
            throw new IOException("Daily-file storage root is not a directory: " + configured);
        return configured.toRealPath();
    }

    private void createConfinedDirectories(Path confinementRoot, Path directory)
            throws IOException {
        createConfinedDirectories(confinementRoot, directory, durableDirectoryEntries,
                DailyFileJournalStore::forceDirectory);
    }

    static void createConfinedDirectories(Path confinementRoot, Path directory,
                                          Set<Path> durableEntries,
                                          DirectoryForcer directoryForcer)
            throws IOException {
        Objects.requireNonNull(durableEntries, "durableEntries");
        Objects.requireNonNull(directoryForcer, "directoryForcer");
        Path root = confinementRoot.toAbsolutePath().normalize();
        Path target = directory.toAbsolutePath().normalize();
        if (!target.startsWith(root))
            throw new IOException("Directory escapes daily-file storage root: " + target);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root))
            throw new IOException("Daily-file storage root is linked or not a directory: " + root);
        Path current = root;
        for (Path component : root.relativize(target)) {
            current = current.resolve(component);
            boolean created = false;
            try {
                Files.createDirectory(current);
                created = true;
            } catch (FileAlreadyExistsException ignored) {
                // Validated below. This also handles a concurrent creator safely.
            }
            if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(current))
                throw new IOException("Refusing linked or non-directory storage path: " + current);
            if (created || !durableEntries.contains(current)) {
                directoryForcer.force(current.getParent());
                durableEntries.add(current);
            }
        }
    }

    private static void createDirectoryTreeDurably(Path directory) throws IOException {
        List<Path> missing = new ArrayList<>();
        Path existing = directory;
        while (existing != null && Files.notExists(existing, LinkOption.NOFOLLOW_LINKS)) {
            missing.add(existing);
            existing = existing.getParent();
        }
        if (existing == null || !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(existing, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(existing))
            throw new IOException("Daily-file storage root has no regular existing ancestor: "
                    + directory);

        // Re-forcing the nearest visible ancestor closes the retry window after a prior create
        // succeeded but its parent-directory barrier failed.
        if (existing.getParent() != null)
            forceDirectory(existing.getParent());
        for (int index = missing.size() - 1; index >= 0; index--) {
            Path component = missing.get(index);
            try {
                Files.createDirectory(component);
            } catch (FileAlreadyExistsException ignored) {
                // A concurrent creator is acceptable only after the no-follow validation below.
            }
            if (!Files.isDirectory(component, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(component))
                throw new IOException("Refusing linked or non-directory storage path: " + component);
            forceDirectory(component.getParent());
        }
    }

    @FunctionalInterface
    interface DirectoryForcer {
        void force(Path directory) throws IOException;
    }

    private static String readBoundedUtf8(Path path, int maxBytes, String description)
            throws IOException {
        byte[] contents;
        try (InputStream input = Files.newInputStream(path, StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            contents = input.readNBytes(maxBytes + 1);
        }
        if (contents.length > maxBytes)
            throw new IOException("Oversized " + description + ": " + path);
        return new String(contents, StandardCharsets.UTF_8);
    }

    private static long requirePositive(long value, String name) {
        if (value <= 0)
            throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20)
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    else
                        escaped.append(character);
                }
            }
        }
        return escaped.toString();
    }

    private static String nullableJson(String value) {
        return value == null ? "null" : "\"" + jsonEscape(value) + "\"";
    }

    private static IOException merge(IOException first, IOException next) {
        if (first == null)
            return next;
        first.addSuppressed(next);
        return first;
    }

    private void ensureOpen() throws IOException {
        if (closed.get())
            throw new IOException("Daily-file journal is closed");
    }

    private final class LaneState {
        private final String laneId;
        private final Path laneDirectory;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition historicalRecoveryComplete = lock.newCondition();
        private LocalDate activeDay;
        private LocalDate historicalRecoveryDay;
        private Path dayDirectory;
        private Path openSegment;
        private FileChannel channel;
        private Instant segmentCreatedAt;
        private int segmentSequence;
        private long segmentRecords;
        private long dayRecords;
        private int unforcedRecords;
        private long unforcedBytes;
        private IOException terminalFailure;
        private IOException shutdownFailure;

        private LaneState(String laneId) throws IOException {
            this.laneId = laneId;
            this.laneDirectory = laneDirectory(laneId);
        }

        void append(DailyFileRecord record) throws IOException {
            lock.lock();
            try {
                ensureOpen();
                if (terminalFailure != null)
                    throw new IOException("Daily-file journal lane is failed: " + laneId,
                            terminalFailure);

                LocalDate recordDay = LocalDate.ofInstant(record.timestamp(), zoneId);
                while (recordDay.equals(historicalRecoveryDay)) {
                    try {
                        historicalRecoveryComplete.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while waiting for historical daily-file "
                                + "recovery for " + laneId + " on " + recordDay, e);
                    }
                    ensureOpen();
                }
                byte[] payload = encodeRecord(record);
                int frameBytes = FRAME_HEADER_BYTES + payload.length;
                if (SEGMENT_HEADER_BYTES + frameBytes > maxSegmentBytes)
                    throw new IOException("Daily-file record requires "
                            + (SEGMENT_HEADER_BYTES + frameBytes)
                            + " segment bytes; configured maximum is " + maxSegmentBytes);
                if (activeDay == null) {
                    try {
                        openDay(recordDay, record.timestamp());
                    } catch (IOException e) {
                        terminalFailure = e;
                        throw e;
                    }
                } else if (!recordDay.equals(activeDay)) {
                    if (recordDay.isBefore(activeDay))
                        throw new IOException("Out-of-order daily-file record for " + laneId + ": "
                                + record.timestamp() + " belongs to " + recordDay
                                + " after " + activeDay);
                    try {
                        sealActiveDay();
                        openDay(recordDay, record.timestamp());
                    } catch (IOException e) {
                        terminalFailure = e;
                        throw e;
                    }
                }

                try {
                    if (segmentRecords > 0 && (channel.size() + frameBytes > maxSegmentBytes
                            || Duration.between(segmentCreatedAt, record.timestamp())
                            .compareTo(maxSegmentAge) >= 0)) {
                        rotateSegment(record.timestamp());
                    }

                    ByteBuffer frame = ByteBuffer.allocate(frameBytes);
                    frame.putInt(payload.length);
                    frame.putInt(crc32c(payload));
                    frame.put(payload);
                    frame.flip();
                    writeFully(channel, frame);
                    segmentRecords++;
                    dayRecords++;
                    unforcedRecords++;
                    unforcedBytes += frameBytes;
                    if (unforcedRecords >= maxUnforcedRecords || unforcedBytes >= maxUnforcedBytes)
                        force();
                } catch (IOException e) {
                    terminalFailure = e;
                    throw e;
                }
            } finally {
                lock.unlock();
            }
        }

        void maintenance(Instant now) throws IOException {
            lock.lock();
            try {
                if (terminalFailure != null)
                    throw new IOException("Daily-file journal lane is failed: " + laneId,
                            terminalFailure);
                try {
                    if (channel != null)
                        force();
                    LocalDate currentDay = LocalDate.ofInstant(now, zoneId);
                    if (activeDay != null && activeDay.isBefore(currentDay))
                        sealActiveDay();
                } catch (IOException e) {
                    terminalFailure = e;
                    throw e;
                }
            } finally {
                lock.unlock();
            }
        }

        void force() throws IOException {
            lock.lock();
            try {
                if (channel == null || (unforcedRecords == 0 && unforcedBytes == 0))
                    return;
                channel.force(false);
                unforcedRecords = 0;
                unforcedBytes = 0;
                writeLedger(laneId, DayWindow.forDate(activeDay, zoneId), "OPEN", dayRecords,
                        null, null, null, 0);
            } finally {
                lock.unlock();
            }
        }

        void closeForShutdown(long deadlineNanos) throws IOException {
            boolean locked;
            try {
                locked = tryLockUntil(lock, deadlineNanos);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while closing daily-file lane " + laneId, e);
            }
            if (!locked)
                throw new IOException("Timed out while closing daily-file lane " + laneId);
            try {
                if (shutdownFailure != null)
                    throw new IOException("Daily-file lane previously failed to close: " + laneId,
                            shutdownFailure);
                if (channel == null)
                    return;
                IOException failure = null;
                try {
                    force();
                } catch (IOException e) {
                    failure = e;
                }
                try {
                    channel.close();
                } catch (IOException e) {
                    failure = merge(failure, e);
                } finally {
                    channel = null;
                    openSegment = null;
                }
                if (failure != null) {
                    shutdownFailure = failure;
                    throw failure;
                }
            } finally {
                lock.unlock();
            }
        }

        private void openDay(LocalDate day, Instant now) throws IOException {
            DayWindow window = DayWindow.forDate(day, zoneId);
            if (isPublished(laneId, window.startInclusive, window.endExclusive))
                throw new IOException("Refusing to append to already published daily-file window " + window);

            // Reserve recovery before creating the first segment. A power loss can therefore
            // never leave an unindexed historical day that startup would have to discover by a
            // complete tree walk.
            enqueueLifecycleWork(LifecycleWorkKind.RECOVERY, laneId, day);
            activeDay = day;
            dayDirectory = laneDirectory.resolve(day.toString());
            createConfinedDirectories(laneDirectory, dayDirectory);
            List<Path> opens = listSegments(dayDirectory, ".open");
            if (opens.size() > 1) {
                for (int i = 0; i < opens.size() - 1; i++) {
                    // Every crash-interrupted tail must be validated and truncated before the
                    // filename is allowed to claim that it is a sealed, replayable segment.
                    recoverOpenSegment(opens.get(i));
                    sealSegmentFile(opens.get(i));
                }
                opens = listSegments(dayDirectory, ".open");
            }

            dayRecords = countRecords(listSegments(dayDirectory, ".seg"));
            if (!opens.isEmpty()) {
                openSegment = opens.get(0);
                RecoveryResult recovered = recoverOpenSegment(openSegment);
                segmentSequence = sequence(openSegment);
                segmentCreatedAt = recovered.createdAt;
                segmentRecords = recovered.recordCount;
                dayRecords += segmentRecords;
                channel = FileChannel.open(openSegment, StandardOpenOption.READ, StandardOpenOption.WRITE);
                channel.position(recovered.validEnd);
            } else {
                List<Path> sealed = listSegments(dayDirectory, ".seg");
                segmentSequence = sealed.isEmpty() ? 0 : sequence(sealed.get(sealed.size() - 1)) + 1;
                openNewSegment(now);
            }
            writeLedger(laneId, window, "OPEN", dayRecords, null, null, null, 0);
        }

        private void openNewSegment(Instant now) throws IOException {
            openSegment = dayDirectory.resolve(String.format(Locale.ROOT, "%06d.open", segmentSequence));
            channel = FileChannel.open(openSegment, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
            segmentCreatedAt = now;
            segmentRecords = 0;
            writeSegmentHeader(channel, now);
            forceDirectory(dayDirectory);
        }

        private void rotateSegment(Instant now) throws IOException {
            force();
            channel.close();
            channel = null;
            sealSegmentFile(openSegment);
            segmentSequence++;
            openNewSegment(now);
        }

        private void sealActiveDay() throws IOException {
            LocalDate day = activeDay;
            Path completedDayDirectory = dayDirectory;
            if (channel != null) {
                force();
                channel.close();
                channel = null;
                sealSegmentFile(openSegment);
            }
            publishDay(laneId, day, completedDayDirectory);
            openSegment = null;
            activeDay = null;
            dayDirectory = null;
            segmentRecords = 0;
            unforcedRecords = 0;
            unforcedBytes = 0;
            dayRecords = 0;
        }

        private long countRecords(List<Path> segments) throws IOException {
            long count = 0;
            for (Path segment : segments)
                count += scanRecords(segment, record -> { });
            return count;
        }
    }

    private final class WriterHandle implements DailyFileWriterHandle {
        private final LaneState lane;
        private final boolean shutdownLease;
        private final AtomicBoolean handleClosed = new AtomicBoolean();

        private WriterHandle(LaneState lane, boolean shutdownLease) {
            this.lane = lane;
            this.shutdownLease = shutdownLease;
        }

        @Override
        public String getLaneId() {
            return lane.laneId;
        }

        @Override
        public synchronized void append(DailyFileRecord record) throws IOException {
            if (handleClosed.get())
                throw new IOException("Daily-file writer handle is closed");
            lane.append(Objects.requireNonNull(record, "record"));
        }

        @Override
        public synchronized void flush() throws IOException {
            if (handleClosed.get())
                throw new IOException("Daily-file writer handle is closed");
            lane.force();
        }

        @Override
        public synchronized void close() throws IOException {
            if (!handleClosed.compareAndSet(false, true))
                return;
            try {
                lane.force();
            } finally {
                if (shutdownLease) {
                    writerHandleLock.lock();
                    try {
                        openWriterHandles--;
                        if (openWriterHandles == 0)
                            noOpenWriterHandles.signalAll();
                    } finally {
                        writerHandleLock.unlock();
                    }
                }
            }
        }
    }

    private static int sequence(Path segment) throws IOException {
        String name = segment.getFileName().toString();
        int dot = name.indexOf('.');
        try {
            return Integer.parseInt(dot >= 0 ? name.substring(0, dot) : name);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid journal segment name: " + segment, e);
        }
    }

    private record RecoveryResult(long validEnd, long recordCount, Instant createdAt) {
    }

    private enum LifecycleWorkKind {
        RECOVERY("recovery"),
        PURGE("purge"),
        DELETION("deletion");

        private final String directory;

        LifecycleWorkKind(String directory) {
            this.directory = directory;
        }
    }

    private record LifecycleWork(LifecycleWorkKind kind, String laneId, LocalDate day,
                                 Path marker) {
    }

    static record DayWindow(LocalDate day, Instant startInclusive, Instant endExclusive) {
        static DayWindow forDate(LocalDate day, ZoneId zoneId) {
            return new DayWindow(day, day.atStartOfDay(zoneId).toInstant(),
                    day.plusDays(1).atStartOfDay(zoneId).toInstant());
        }
    }

    private static final class LaneInitializationException extends RuntimeException {
        private final IOException ioException;

        private LaneInitializationException(IOException ioException) {
            super(ioException);
            this.ioException = ioException;
        }
    }
}
