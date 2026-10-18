package com.botts.impl.service.oscar.media;

import com.botts.api.service.bucket.IBucketStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.CRC32C;

/**
 * Filesystem-backed catalog for retained media sets.
 *
 * <p>All writes use a same-directory partial, fsync the file, verify its contents, and then use an
 * atomic rename.  Frames are published before {@code manifest.json}; readers consequently treat a
 * manifest as the single commit marker for a complete set.</p>
 */
public final class MediaSetStore {
    public static final String VIDEO_BUCKET = "videos";
    public static final String ROOT_PREFIX = "retained-media/v1/events/";
    public static final String API_PREFIX = "media/";
    public static final String RETENTION_CHECKPOINT_KEY =
            "retained-media/v1/.retention-scan-checkpoint.json";
    public static final String MAINTENANCE_CHECKPOINT_KEY =
            "retained-media/v1/.maintenance-checkpoint.json";
    public static final String RECONCILIATION_CHECKPOINT_KEY =
            "retained-media/v1/.retention-reconciliation-checkpoint.json";
    public static final String PRESSURE_QUEUE_STATE_KEY =
            "retained-media/v1/.pressure-queue-state.json";
    public static final String CATALOG_QUEUE_STATE_KEY =
            "retained-media/v1/.catalog-queue-state.json";
    public static final String METADATA_RECLAMATION_CHECKPOINT_KEY =
            "retained-media/v1/.metadata-reclamation-checkpoint.json";
    private static final String MAINTENANCE_PREFIX = "retained-media/v1/maintenance/";
    private static final String PRESSURE_QUEUE_ITEM_PREFIX =
            "retained-media/v1/pressure-queue/items/";
    private static final String PRESSURE_QUEUE_MEMBER_PREFIX =
            "retained-media/v1/pressure-queue/members/";
    private static final String CATALOG_QUEUE_ITEM_PREFIX =
            "retained-media/v1/catalog-queue/items/";
    private static final String CATALOG_QUEUE_MEMBER_PREFIX =
            "retained-media/v1/catalog-queue/members/";
    private static final String PROCESSED_OBSERVATION_INDEX_PREFIX =
            "retained-media/v1/retention-processed-index/";
    private static final String PROCESSED_OBSERVATION_INDEX_LOCK_KEY =
            PROCESSED_OBSERVATION_INDEX_PREFIX + ".index.lock";
    private static final String DELETION_INDEX_PREFIX =
            "retained-media/v1/deletion-index/";
    private static final String MANIFEST_FILE = "manifest.json";
    private static final String LEDGER_FILE = ".job.json";
    private static final String PARTIAL_MARKER = ".partial-";
    private static final String BUCKET_PATH_ANCHOR = ".oscar-retained-media-path-anchor";
    private static final int MAX_INTERNAL_JSON_BYTES = 1024 * 1024;
    private static final byte[] DELETION_INDEX_HEADER = new byte[]{
            'O', 'S', 'C', 'A', 'R', 'M', 'D', 'I', 0, 0, 0, 1, 0, 0, 0, 36
    };
    private static final byte[] PROCESSED_OBSERVATION_INDEX_HEADER = new byte[]{
            'O', 'S', 'C', 'A', 'R', 'M', 'P', 'I', 0, 0, 0, 1, 0, 0, 0, 36
    };
    private static final int EXACT_INDEX_DIGEST_BYTES = 32;
    private static final int EXACT_INDEX_RECORD_BYTES = 36;
    private static final int EXACT_INDEX_CACHE_MAX_ENTRIES = 32;
    private static final int EXACT_INDEX_CACHE_MAX_RECORDS = 200_000;
    private static final int EXACT_INDEX_CACHE_MAX_RECORDS_PER_SNAPSHOT = 100_000;
    private static final long PROCESSED_OBSERVATION_BUCKET_SECONDS = 60L * 60L;
    /*
     * Keep reclamation/checkpointing hourly, but split a busy hour into deterministic five-minute
     * files.  A single cache snapshot therefore remains below 100,000 entries until a 50-lane
     * site sustains more than 6.6 completed occupancies per lane per second for five minutes
     * (1.2 million/hour).  That is far beyond a physically meaningful RPM occupancy rate and
     * removes the former 100,000-record/hour cache cliff without returning to per-event inodes.
     */
    private static final int PROCESSED_OBSERVATION_INDEX_FILES_PER_HOUR = 12;
    private static final long PROCESSED_OBSERVATION_INDEX_FILE_SECONDS =
            PROCESSED_OBSERVATION_BUCKET_SECONDS
                    / PROCESSED_OBSERVATION_INDEX_FILES_PER_HOUR;
    private static final Object[] INDEX_FILE_LOCKS = createLocks(256);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (value, type, context) -> context.serialize(value.toString()))
            .registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (value, type, context) -> Instant.parse(value.getAsString()))
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final IBucketStore bucketStore;
    private final Path videoBucketRoot;
    private final Object[] identityLocks = new Object[256];
    /**
     * A small per-store LRU avoids reparsing a busy exact-index file for every occupancy. Every hit is
     * guarded by a fresh filesystem stamp while the cross-process index lock is held, so an append
     * from another live {@code MediaSetStore} is observed before the cached set can be used.
     */
    private final Map<String, ExactIndexSnapshot> exactIndexCache =
            new LinkedHashMap<>(EXACT_INDEX_CACHE_MAX_ENTRIES + 1, 0.75f, true);
    private int exactIndexCacheRecordCount;

    public MediaSetStore(IBucketStore bucketStore) throws DataStoreException {
        this.bucketStore = Objects.requireNonNull(bucketStore, "bucketStore");
        for (int index = 0; index < identityLocks.length; index++)
            identityLocks[index] = new Object();
        if (!bucketStore.bucketExists(VIDEO_BUCKET))
            bucketStore.createBucket(VIDEO_BUCKET);
        try {
            Path anchor = Path.of(bucketStore.getWritableResourceURI(
                            VIDEO_BUCKET, BUCKET_PATH_ANCHOR))
                    .toAbsolutePath().normalize();
            this.videoBucketRoot = anchor.getParent();
            if (videoBucketRoot == null
                    || !Files.isDirectory(videoBucketRoot, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(videoBucketRoot))
                throw new DataStoreException("Videos bucket is not a regular directory");
        } catch (IllegalArgumentException e) {
            throw new DataStoreException("Unable to resolve the videos bucket path", e);
        }
    }

    public record Identity(String eventId, String laneUid, String cameraUid) {
        public Identity {
            validateIdentity(eventId, "eventId");
            validateIdentity(laneUid, "laneUid");
            validateIdentity(cameraUid, "cameraUid");
        }

        public String mediaSetId() {
            return sha256((eventId + "\u0000" + laneUid + "\u0000" + cameraUid)
                    .getBytes(StandardCharsets.UTF_8));
        }
    }

    public String baseKey(Identity identity) {
        return ROOT_PREFIX + encode(identity.eventId())
                + "/lanes/" + encode(identity.laneUid())
                + "/cameras/" + encode(identity.cameraUid());
    }

    public String manifestKey(Identity identity) {
        return baseKey(identity) + "/" + MANIFEST_FILE;
    }

    public String ledgerKey(Identity identity) {
        return baseKey(identity) + "/" + LEDGER_FILE;
    }

    public String frameKey(Identity identity, int oneBasedIndex) {
        if (oneBasedIndex < 1 || oneBasedIndex > MediaManifest.RETAINED_FRAME_COUNT)
            throw new IllegalArgumentException("Retained frame index must be 1 through 5");
        return baseKey(identity) + String.format(Locale.ROOT,
                "/frame-%02d.jpg", oneBasedIndex);
    }

    public Optional<MediaManifest> findManifest(Identity identity) throws IOException {
        String key = manifestKey(identity);
        return bucketStore.objectExists(VIDEO_BUCKET, key)
                ? Optional.of(readManifest(key)) : Optional.empty();
    }

    public List<ManifestEntry> findEvent(String eventId) throws IOException {
        return findEvent(eventId, Integer.MAX_VALUE);
    }

    /** Reads no more than {@code maximumEntries}; callers may request one extra to detect overflow. */
    public List<ManifestEntry> findEvent(String eventId, int maximumEntries) throws IOException {
        validateIdentity(eventId, "eventId");
        if (maximumEntries < 1)
            throw new IllegalArgumentException("maximumEntries must be positive");
        String prefix = ROOT_PREFIX + encode(eventId) + "/";
        List<ManifestEntry> result = new ArrayList<>();
        try {
            for (String key : bucketStore.listObjects(VIDEO_BUCKET, prefix,
                    "/" + MANIFEST_FILE, maximumEntries)) {
                String normalizedKey = key.replace('\\', '/');
                if (isFinalManifestKey(normalizedKey) && normalizedKey.startsWith(prefix)) {
                    MediaManifest manifest = readManifest(normalizedKey);
                    if (eventId.equals(manifest.eventId())
                            && (manifest.lifecycleState() == MediaManifest.LifecycleState.AVAILABLE
                            || manifest.lifecycleState() == MediaManifest.LifecycleState.HELD))
                        result.add(new ManifestEntry(normalizedKey, manifest));
                    if (result.size() >= maximumEntries)
                        break;
                }
            }
        } catch (DataStoreException e) {
            throw new IOException("Unable to search retained media catalog", e);
        }
        result.sort(Comparator.comparing((ManifestEntry entry) -> entry.manifest().laneUid())
                .thenComparing(entry -> entry.manifest().cameraUid()));
        return result;
    }

    public List<ManifestEntry> listManifests() throws IOException {
        List<ManifestEntry> result = new ArrayList<>();
        try {
            if (!bucketStore.bucketExists(VIDEO_BUCKET))
                return result;
            for (String key : bucketStore.listObjects(VIDEO_BUCKET)) {
                String normalizedKey = key.replace('\\', '/');
                if (isFinalManifestKey(normalizedKey))
                    result.add(new ManifestEntry(normalizedKey, readManifest(normalizedKey)));
            }
        } catch (DataStoreException e) {
            throw new IOException("Unable to list retained media catalog", e);
        }
        return result;
    }

    /** Returns the next stable, lexicographically ordered slice of the committed catalog. */
    public List<ManifestEntry> listManifests(String afterKey, int limit) throws IOException {
        if (limit < 1)
            throw new IllegalArgumentException("manifest listing limit must be positive");
        String resumeAfter = afterKey == null ? "" : afterKey;
        List<ManifestEntry> result = new ArrayList<>();
        try {
            for (String key : bucketStore.listObjects(VIDEO_BUCKET, ROOT_PREFIX).stream()
                    .map(value -> value.replace('\\', '/'))
                    .filter(MediaSetStore::isFinalManifestKey)
                    .filter(value -> value.compareTo(resumeAfter) > 0)
                    .sorted()
                    .limit(limit)
                    .toList())
                result.add(new ManifestEntry(key, readManifest(key)));
        } catch (DataStoreException e) {
            throw new IOException("Unable to scan retained media catalog", e);
        }
        return result;
    }

    public MediaManifest readManifest(String key) throws IOException {
        String normalizedKey = key == null ? null : key.replace('\\', '/');
        if (!isFinalManifestKey(normalizedKey))
            throw new IOException("Not a final media manifest key");
        try (Reader reader = new java.io.StringReader(readBoundedJson(normalizedKey))) {
            MediaManifest manifest = GSON.fromJson(reader, MediaManifest.class);
            validateManifestShape(manifest);
            validateManifestIdentityAndKeys(normalizedKey, manifest);
            return manifest;
        } catch (RuntimeException e) {
            throw new IOException("Invalid retained media manifest " + normalizedKey, e);
        }
    }

    public Optional<MediaJobLedger> readLedger(Identity identity) throws IOException {
        String key = ledgerKey(identity);
        if (!bucketStore.objectExists(VIDEO_BUCKET, key))
            return Optional.empty();
        try (Reader reader = new java.io.StringReader(readBoundedJson(key))) {
            MediaJobLedger ledger = GSON.fromJson(reader, MediaJobLedger.class);
            if (ledger == null
                    || !identity.eventId().equals(ledger.eventId())
                    || !identity.laneUid().equals(ledger.laneUid())
                    || !identity.cameraUid().equals(ledger.cameraUid()))
                throw new IOException("Retained media ledger identity does not match its key");
            return Optional.of(ledger);
        } catch (RuntimeException e) {
            throw new IOException("Invalid retained media job ledger " + key, e);
        }
    }

    public synchronized void writeLedger(Identity identity, MediaJobLedger ledger) throws IOException {
        if (isTerminallyDeleted(identity))
            throw new IOException("Retained media set is terminally deleted: " + identity.mediaSetId());
        if (!identity.eventId().equals(ledger.eventId())
                || !identity.laneUid().equals(ledger.laneUid())
                || !identity.cameraUid().equals(ledger.cameraUid()))
            throw new IOException("Ledger identity does not match its catalog key");
        atomicWriteJson(ledgerKey(identity), ledger, MediaJobLedger.class);
        if (ledger.state() == MediaJobLedger.JobState.DECODING
                || ledger.state() == MediaJobLedger.JobState.PUBLISHING
                || ledger.state() == MediaJobLedger.JobState.FAILED) {
            enqueueMaintenance(new MediaMaintenanceJob(
                    MediaMaintenanceJob.CURRENT_SCHEMA_VERSION,
                    MediaMaintenanceJob.Kind.RETRY,
                    ledger.eventId(), ledger.laneUid(), ledger.cameraUid(),
                    ledger.sourceObjectKey(), retryDueAt(ledger), ledger.attempts()));
        } else if (ledger.state() == MediaJobLedger.JobState.PUBLISHED
                || ledger.state() == MediaJobLedger.JobState.SOURCE_DELETED) {
            removeMaintenance(MediaMaintenanceJob.Kind.RETRY, identity);
        }
    }

    public PublishedAsset publishFrame(Identity identity, int index, byte[] jpeg) throws IOException {
        if (isTerminallyDeleted(identity))
            throw new IOException("Retained media set is terminally deleted: " + identity.mediaSetId());
        Objects.requireNonNull(jpeg, "jpeg");
        if (jpeg.length == 0 || jpeg.length > MediaManifest.MAX_JPEG_BYTES)
            throw new IOException("Retained JPEG exceeds the encoded-size safety limit");
        String key = frameKey(identity, index);
        Path target = writablePath(key);
        Path partial = partialSibling(target);
        try {
            writeAndForce(partial, jpeg);
            JpegFrameExtractor.verifyBaselineSrgbJpeg(partial);
            atomicReplace(partial, target);
            forceDirectory(target.getParent());
            JpegFrameExtractor.JpegInfo info = JpegFrameExtractor.verifyBaselineSrgbJpeg(target);
            return new PublishedAsset(key, sha256(target), Files.size(target), info.width(), info.height());
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    public synchronized void publishManifest(Identity identity, MediaManifest manifest) throws IOException {
        if (isTerminallyDeleted(identity))
            throw new IOException("Retained media set is terminally deleted: " + identity.mediaSetId());
        if (!identity.mediaSetId().equals(manifest.mediaSetId())
                || !identity.eventId().equals(manifest.eventId())
                || !identity.laneUid().equals(manifest.laneUid())
                || !identity.cameraUid().equals(manifest.cameraUid()))
            throw new IOException("Manifest identity does not match its catalog key");
        validateManifestIdentityAndKeys(manifestKey(identity), manifest);
        validateManifestAssetsExist(manifest);
        validateManifestSource(manifest);
        // Reserve both direct-addressed work queues before committing the manifest. Consumers use
        // the same synchronized store, so a live process cannot observe either entry before the
        // manifest. A crash before rename leaves harmless stale entries; a crash after rename can
        // leave neither lifecycle reconciliation nor pressure deletion without durable work.
        ensureCatalogCandidate(identity, manifest);
        ensurePressureCandidate(identity, manifest);
        atomicWriteJson(manifestKey(identity), manifest, MediaManifest.class);
        MediaManifest reopened = readManifest(manifestKey(identity));
        if (!reopened.equals(manifest))
            throw new IOException("Published media manifest did not round-trip exactly");
        ensureManifestMaintenance(identity, manifest);
    }

    /** Ensures a committed manifest has the matching durable source-lifecycle index state. */
    public synchronized void ensureManifestMaintenance(Identity identity, MediaManifest manifest)
            throws IOException {
        if (!identity.mediaSetId().equals(manifest.mediaSetId()))
            throw new IOException("Manifest identity does not match its maintenance key");
        ensureCatalogCandidate(identity, manifest);
        ensurePressureCandidate(identity, manifest);
        if ((manifest.lifecycleState() == MediaManifest.LifecycleState.AVAILABLE
                || manifest.lifecycleState() == MediaManifest.LifecycleState.HELD)
                && manifest.source().state() != MediaManifest.SourceState.DELETED) {
            enqueueMaintenance(new MediaMaintenanceJob(
                    MediaMaintenanceJob.CURRENT_SCHEMA_VERSION,
                    MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE,
                    manifest.eventId(), manifest.laneUid(), manifest.cameraUid(),
                    manifest.source().objectKey(), manifest.source().rollbackDeleteAfter(), 0));
        } else {
            removeMaintenance(MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, identity);
        }
    }

    public Optional<RetentionScanCheckpoint> readRetentionScanCheckpoint() throws IOException {
        return readInternalJson(RETENTION_CHECKPOINT_KEY, RetentionScanCheckpoint.class)
                .map(checkpoint -> {
                    validateRetentionCheckpoint(checkpoint);
                    return checkpoint;
                });
    }

    public synchronized void writeRetentionScanCheckpoint(RetentionScanCheckpoint checkpoint)
            throws IOException {
        validateRetentionCheckpoint(checkpoint);
        atomicWriteJson(RETENTION_CHECKPOINT_KEY, checkpoint, RetentionScanCheckpoint.class);
    }

    public Optional<RetentionReconciliationCheckpoint> readReconciliationCheckpoint()
            throws IOException {
        return readInternalJson(RECONCILIATION_CHECKPOINT_KEY,
                RetentionReconciliationCheckpoint.class);
    }

    public synchronized void writeReconciliationCheckpoint(
            RetentionReconciliationCheckpoint checkpoint) throws IOException {
        Objects.requireNonNull(checkpoint, "checkpoint");
        atomicWriteJson(RECONCILIATION_CHECKPOINT_KEY, checkpoint,
                RetentionReconciliationCheckpoint.class);
    }

    /** Exact time-bucketed lookup with no per-observation marker inode or stale cache. */
    public boolean isObservationProcessed(String observationId, Instant resultTime)
            throws IOException {
        String digest = processedObservationDigest(observationId);
        String key = processedObservationIndexKey(resultTime);
        return withIndexFileLock(PROCESSED_OBSERVATION_INDEX_LOCK_KEY, () -> {
            Path path = writablePath(key);
            if (!requireExistingOrConfirmedAbsent(path, "processed-observation index"))
                return false;
            return readExactIndex(path, key, PROCESSED_OBSERVATION_INDEX_HEADER,
                    ignored -> true, "processed-observation index", digest).containsTarget();
        });
    }

    /**
     * Appends one observation digest to its UTC epoch-hour bucket. The durable lower-bound state is
     * reserved first, so a crash can leave at most a harmless missing bucket, never an untracked
     * bucket that reclamation cannot eventually reach.
     */
    public void markObservationProcessed(RetentionProcessedObservation marker)
            throws IOException {
        Objects.requireNonNull(marker, "marker");
        String digest = processedObservationDigest(marker.observationId());
        long epochHour = processedObservationEpochHour(marker.resultTime());
        String key = processedObservationIndexKey(marker.resultTime());
        withIndexFileLock(PROCESSED_OBSERVATION_INDEX_LOCK_KEY, () -> {
            MediaMetadataReclamationCheckpoint checkpoint =
                    readMetadataReclamationCheckpoint();
            Long earliest = checkpoint.earliestProcessedObservationEpochHour();
            if (earliest == null || epochHour < earliest) {
                checkpoint = checkpoint.withEarliestProcessedObservationEpochHour(
                        epochHour, Instant.now());
                writeMetadataReclamationCheckpoint(checkpoint);
            }
            appendExactIndexDigest(writablePath(key), key,
                    PROCESSED_OBSERVATION_INDEX_HEADER, digest, ignored -> true,
                    "processed-observation index");
            return null;
        });
    }

    /**
     * Deletes at most {@code workLimit} deterministic hourly index buckets whose end is strictly
     * before the forward scanner's durable late-arrival boundary. Each hour has twelve
     * direct-addressed five-minute files; no object listing or directory traversal occurs, and a
     * missing hour consumes the same fixed twelve probes as a populated hour.
     */
    public ProcessedObservationPruneResult pruneProcessedObservations(
            Instant retainFromInclusive, int workLimit) throws IOException {
        Objects.requireNonNull(retainFromInclusive, "retainFromInclusive");
        if (workLimit < 1)
            throw new IllegalArgumentException("Processed-observation prune limit must be positive");

        return withIndexFileLock(PROCESSED_OBSERVATION_INDEX_LOCK_KEY, () -> {
            MediaMetadataReclamationCheckpoint checkpoint =
                    readMetadataReclamationCheckpoint();
            Long nextEpochHour = checkpoint.earliestProcessedObservationEpochHour();
            if (nextEpochHour == null)
                return new ProcessedObservationPruneResult(0, 0, null);

            int inspected = 0;
            int deleted = 0;
            while (inspected < workLimit
                    && processedObservationBucketEnd(nextEpochHour)
                    .isBefore(retainFromInclusive)) {
                boolean deletedAnyFile = false;
                for (int file = 0; file < PROCESSED_OBSERVATION_INDEX_FILES_PER_HOUR;
                     file++) {
                    String key = processedObservationIndexKey(nextEpochHour, file);
                    DurableDeleteResult result = deleteDurably(key, true);
                    if (result.existed())
                        deletedAnyFile = true;
                    invalidateExactIndex(key);
                }
                if (deletedAnyFile)
                    deleted++;
                inspected++;
                nextEpochHour = Math.addExact(nextEpochHour, 1L);
            }
            if (inspected > 0) {
                checkpoint = checkpoint.withEarliestProcessedObservationEpochHour(
                        nextEpochHour, Instant.now());
                writeMetadataReclamationCheckpoint(checkpoint);
            }
            return new ProcessedObservationPruneResult(inspected, deleted, nextEpochHour);
        });
    }

    public Optional<MediaMaintenanceCheckpoint> readMaintenanceCheckpoint() throws IOException {
        return readInternalJson(MAINTENANCE_CHECKPOINT_KEY, MediaMaintenanceCheckpoint.class)
                .map(checkpoint -> {
                    validateMaintenanceCheckpoint(checkpoint);
                    return checkpoint;
                });
    }

    public synchronized void writeMaintenanceCheckpoint(MediaMaintenanceCheckpoint checkpoint)
            throws IOException {
        validateMaintenanceCheckpoint(checkpoint);
        atomicWriteJson(MAINTENANCE_CHECKPOINT_KEY, checkpoint, MediaMaintenanceCheckpoint.class);
    }

    public synchronized void enqueueMaintenance(MediaMaintenanceJob job) throws IOException {
        validateMaintenanceJob(job);
        atomicWriteJson(maintenanceKey(job.kind(), job.identity()), job,
                MediaMaintenanceJob.class);
    }

    public List<MaintenanceEntry> listMaintenance(MediaMaintenanceJob.Kind kind, int shard,
                                                   String afterKey, int limit) throws IOException {
        if (shard < 0 || shard >= MediaMaintenanceCheckpoint.SHARD_COUNT)
            throw new IllegalArgumentException("maintenance shard must be from 0 through 255");
        if (limit < 1)
            throw new IllegalArgumentException("maintenance listing limit must be positive");
        String prefix = maintenanceShardPrefix(kind, shard);
        String resumeAfter = afterKey == null ? "" : afterKey;
        List<MaintenanceEntry> entries = new ArrayList<>();
        try {
            for (String key : bucketStore.listObjects(VIDEO_BUCKET, prefix).stream()
                    .map(value -> value.replace('\\', '/'))
                    .filter(value -> value.startsWith(prefix) && value.endsWith(".json"))
                    .filter(value -> value.compareTo(resumeAfter) > 0)
                    .sorted()
                    .limit(limit)
                    .toList()) {
                MediaMaintenanceJob job = readInternalJson(key, MediaMaintenanceJob.class)
                        .orElseThrow(() -> new IOException("Missing media maintenance job " + key));
                validateMaintenanceJob(job);
                if (job.kind() != kind || !key.equals(maintenanceKey(kind, job.identity())))
                    throw new IOException("Media maintenance job identity does not match " + key);
                entries.add(new MaintenanceEntry(key, job));
            }
        } catch (DataStoreException e) {
            throw new IOException("Unable to list media maintenance index " + prefix, e);
        }
        return entries;
    }

    public synchronized void removeMaintenance(MediaMaintenanceJob.Kind kind, Identity identity)
            throws IOException {
        deleteIfPresent(maintenanceKey(kind, identity));
    }

    public boolean maintenanceExists(MediaMaintenanceJob.Kind kind, Identity identity) {
        return bucketStore.objectExists(VIDEO_BUCKET, maintenanceKey(kind, identity));
    }

    public boolean isTerminallyDeleted(Identity identity) throws IOException {
        Objects.requireNonNull(identity, "identity");
        String mediaSetId = identity.mediaSetId();
        String key = deletionIndexKey(mediaSetId);
        return withIndexFileLock(deletionIndexLockKey(mediaSetId), () -> {
            Path path = writablePath(key);
            if (!requireExistingOrConfirmedAbsent(path, "media deletion index"))
                return false;
            return readExactIndex(path, key, DELETION_INDEX_HEADER,
                    digest -> deletionIndexKey(digest).equals(key),
                    "media deletion index", mediaSetId).containsTarget();
        });
    }

    public synchronized void clearMaintenance(Identity identity) throws IOException {
        removeMaintenance(MediaMaintenanceJob.Kind.RETRY, identity);
        removeMaintenance(MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, identity);
    }

    /**
     * Idempotently indexes a committed (or immediately-to-be-committed) set for bounded emergency
     * cleanup. Queue slots are addressed by a monotonic sequence, so consuming one slot never
     * lists or sorts the retained-media tree.
     */
    public synchronized void ensurePressureCandidate(Identity identity, MediaManifest manifest)
            throws IOException {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(manifest, "manifest");
        if (!identity.mediaSetId().equals(manifest.mediaSetId()))
            throw new IOException("Manifest identity does not match its pressure-index key");
        if (manifest.lifecycleState() == MediaManifest.LifecycleState.DELETED
                || isTerminallyDeleted(identity)) {
            clearPressureCandidate(identity);
            return;
        }

        MediaPressureQueueState state = readPressureQueueState();
        Optional<MediaPressureJob> existing = readInternalJson(pressureMemberKey(identity),
                MediaPressureJob.class);
        if (existing.isPresent()) {
            MediaPressureJob member = existing.get();
            validatePressureJob(member);
            Optional<MediaPressureJob> item = readInternalJson(
                    pressureItemKey(member.sequence()), MediaPressureJob.class);
            if (member.identity().equals(identity)
                    && member.sequence() >= state.headSequence()
                    && member.sequence() < state.nextSequence()
                    && item.isPresent() && member.equals(item.get()))
                return;
            deleteIfPresent(pressureMemberKey(identity));
        }
        reservePressureCandidate(identity, manifest.createdAt(), state);
    }

    /** Tail snapshot used to ensure one pressure cycle never revisits deferred work. */
    public synchronized long pressureQueueTail() throws IOException {
        return readPressureQueueState().nextSequence();
    }

    /** Reads exactly one direct-addressed queue slot without scanning a directory. */
    public synchronized Optional<PressureQueueEntry> inspectPressureHead() throws IOException {
        MediaPressureQueueState state = readPressureQueueState();
        if (state.headSequence() >= state.nextSequence())
            return Optional.empty();
        long sequence = state.headSequence();
        Optional<MediaPressureJob> job = readInternalJson(pressureItemKey(sequence),
                MediaPressureJob.class);
        boolean currentMembership = false;
        if (job.isPresent()) {
            validatePressureJob(job.get());
            if (job.get().sequence() != sequence)
                throw new IOException("Pressure queue item sequence does not match its key");
            Optional<MediaPressureJob> member = readInternalJson(
                    pressureMemberKey(job.get().identity()), MediaPressureJob.class);
            currentMembership = member.isPresent() && member.get().equals(job.get());
        }
        return Optional.of(new PressureQueueEntry(sequence, job, currentMembership));
    }

    /** Acknowledges a deleted, missing, or stale pressure entry and advances one durable slot. */
    public synchronized void completePressureEntry(PressureQueueEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        MediaPressureQueueState state = readPressureQueueState();
        if (state.headSequence() > entry.sequence())
            return; // Idempotent replay after the durable head was already advanced.
        if (state.headSequence() != entry.sequence())
            throw new IOException("Pressure queue entry is not at the durable head");
        if (entry.currentMembership() && entry.job().isPresent()) {
            MediaPressureJob job = entry.job().get();
            Optional<MediaPressureJob> member = readInternalJson(
                    pressureMemberKey(job.identity()), MediaPressureJob.class);
            if (member.isPresent() && member.get().equals(job))
                deleteIfPresent(pressureMemberKey(job.identity()));
        }
        deleteIfPresent(pressureItemKey(entry.sequence()));
        writePressureQueueState(state.advance(Instant.now()));
    }

    /** Moves an ineligible/held candidate to the tail without revisiting it in this cycle. */
    public synchronized void deferPressureEntry(PressureQueueEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        if (entry.job().isEmpty() || !entry.currentMembership()) {
            completePressureEntry(entry);
            return;
        }
        MediaPressureQueueState state = readPressureQueueState();
        if (state.headSequence() > entry.sequence())
            return;
        if (state.headSequence() != entry.sequence())
            throw new IOException("Pressure queue entry is not at the durable head");

        MediaPressureJob current = entry.job().get();
        Optional<MediaPressureJob> member = readInternalJson(
                pressureMemberKey(current.identity()), MediaPressureJob.class);
        if (member.isEmpty() || !member.get().equals(current)
                || isTerminallyDeleted(current.identity())) {
            completePressureEntry(entry);
            return;
        }
        MediaPressureJob replacement = current.withSequence(state.nextSequence());
        MediaPressureQueueState reserved = state.reserve(Instant.now());
        // Persist tail reservation first. A crash can create a bounded gap, never an invisible job.
        writePressureQueueState(reserved);
        atomicWriteJson(pressureItemKey(replacement.sequence()), replacement,
                MediaPressureJob.class);
        atomicWriteJson(pressureMemberKey(replacement.identity()), replacement,
                MediaPressureJob.class);
        deleteIfPresent(pressureItemKey(entry.sequence()));
        writePressureQueueState(reserved.advance(Instant.now()));
    }

    private MediaPressureQueueState reservePressureCandidate(Identity identity, Instant createdAt,
                                                              MediaPressureQueueState state)
            throws IOException {
        MediaPressureJob job = new MediaPressureJob(MediaPressureJob.CURRENT_SCHEMA_VERSION,
                state.nextSequence(), identity.eventId(), identity.laneUid(), identity.cameraUid(),
                createdAt);
        MediaPressureQueueState reserved = state.reserve(Instant.now());
        // State first makes a crash-visible gap; item then member makes membership the final commit.
        writePressureQueueState(reserved);
        atomicWriteJson(pressureItemKey(job.sequence()), job, MediaPressureJob.class);
        atomicWriteJson(pressureMemberKey(identity), job, MediaPressureJob.class);
        return reserved;
    }

    private MediaPressureQueueState readPressureQueueState() throws IOException {
        MediaPressureQueueState state = readInternalJson(PRESSURE_QUEUE_STATE_KEY,
                MediaPressureQueueState.class).orElseGet(() ->
                MediaPressureQueueState.initial(Instant.EPOCH));
        validatePressureQueueState(state);
        return state;
    }

    private void writePressureQueueState(MediaPressureQueueState state) throws IOException {
        validatePressureQueueState(state);
        atomicWriteJson(PRESSURE_QUEUE_STATE_KEY, state, MediaPressureQueueState.class);
    }

    private void clearPressureCandidate(Identity identity) throws IOException {
        Optional<MediaPressureJob> member = readInternalJson(pressureMemberKey(identity),
                MediaPressureJob.class);
        deleteIfPresent(pressureMemberKey(identity));
        if (member.isPresent())
            deleteIfPresent(pressureItemKey(member.get().sequence()));
    }

    /**
     * Ensures every manifest commit is represented in a direct-addressed, cyclic reconciliation
     * queue. This is separate from emergency pressure order: consuming or deferring either queue
     * cannot make the other lose its durable membership.
     */
    public synchronized void ensureCatalogCandidate(Identity identity, MediaManifest manifest)
            throws IOException {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(manifest, "manifest");
        if (!identity.mediaSetId().equals(manifest.mediaSetId()))
            throw new IOException("Manifest identity does not match its catalog-index key");
        if (manifest.lifecycleState() == MediaManifest.LifecycleState.DELETED) {
            clearCatalogCandidate(identity);
            return;
        }

        MediaCatalogQueueState state = readCatalogQueueState();
        Optional<MediaCatalogJob> existing = readInternalJson(catalogMemberKey(identity),
                MediaCatalogJob.class);
        if (existing.isPresent()) {
            MediaCatalogJob member = existing.get();
            validateCatalogJob(member);
            Optional<MediaCatalogJob> item = readInternalJson(
                    catalogItemKey(member.sequence()), MediaCatalogJob.class);
            if (member.identity().equals(identity)
                    && member.sequence() >= state.headSequence()
                    && member.sequence() < state.nextSequence()
                    && item.isPresent() && member.equals(item.get()))
                return;
            deleteIfPresent(catalogMemberKey(identity));
        }
        reserveCatalogCandidate(identity, manifest.createdAt(), state);
    }

    /** Tail snapshot prevents a cyclic catalog sweep from revisiting deferred work this cycle. */
    public synchronized long catalogQueueTail() throws IOException {
        return readCatalogQueueState().nextSequence();
    }

    /** Reads exactly one direct-addressed catalog slot without listing the retained-media tree. */
    public synchronized Optional<CatalogQueueEntry> inspectCatalogHead() throws IOException {
        MediaCatalogQueueState state = readCatalogQueueState();
        if (state.headSequence() >= state.nextSequence())
            return Optional.empty();
        long sequence = state.headSequence();
        Optional<MediaCatalogJob> job = readInternalJson(catalogItemKey(sequence),
                MediaCatalogJob.class);
        boolean currentMembership = false;
        if (job.isPresent()) {
            validateCatalogJob(job.get());
            if (job.get().sequence() != sequence)
                throw new IOException("Catalog queue item sequence does not match its key");
            Optional<MediaCatalogJob> member = readInternalJson(
                    catalogMemberKey(job.get().identity()), MediaCatalogJob.class);
            currentMembership = member.isPresent() && member.get().equals(job.get());
        }
        return Optional.of(new CatalogQueueEntry(sequence, job, currentMembership));
    }

    /** Acknowledges a deleted, missing, or stale catalog entry. */
    public synchronized void completeCatalogEntry(CatalogQueueEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        MediaCatalogQueueState state = readCatalogQueueState();
        if (state.headSequence() > entry.sequence())
            return;
        if (state.headSequence() != entry.sequence())
            throw new IOException("Catalog queue entry is not at the durable head");
        if (entry.currentMembership() && entry.job().isPresent()) {
            MediaCatalogJob job = entry.job().get();
            Optional<MediaCatalogJob> member = readInternalJson(
                    catalogMemberKey(job.identity()), MediaCatalogJob.class);
            if (member.isPresent() && member.get().equals(job))
                deleteIfPresent(catalogMemberKey(job.identity()));
        }
        deleteIfPresent(catalogItemKey(entry.sequence()));
        writeCatalogQueueState(state.advance(Instant.now()));
    }

    /** Moves a live catalog member to the tail for a later bounded reconciliation cycle. */
    public synchronized void deferCatalogEntry(CatalogQueueEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        if (entry.job().isEmpty() || !entry.currentMembership()) {
            completeCatalogEntry(entry);
            return;
        }
        MediaCatalogQueueState state = readCatalogQueueState();
        if (state.headSequence() > entry.sequence())
            return;
        if (state.headSequence() != entry.sequence())
            throw new IOException("Catalog queue entry is not at the durable head");

        MediaCatalogJob current = entry.job().get();
        Optional<MediaCatalogJob> member = readInternalJson(
                catalogMemberKey(current.identity()), MediaCatalogJob.class);
        if (member.isEmpty() || !member.get().equals(current)) {
            completeCatalogEntry(entry);
            return;
        }
        MediaCatalogJob replacement = current.withSequence(state.nextSequence());
        MediaCatalogQueueState reserved = state.reserve(Instant.now());
        // State first leaves a visible, one-slot gap after a crash; membership is committed last.
        writeCatalogQueueState(reserved);
        atomicWriteJson(catalogItemKey(replacement.sequence()), replacement,
                MediaCatalogJob.class);
        atomicWriteJson(catalogMemberKey(replacement.identity()), replacement,
                MediaCatalogJob.class);
        deleteIfPresent(catalogItemKey(entry.sequence()));
        writeCatalogQueueState(reserved.advance(Instant.now()));
    }

    private void reserveCatalogCandidate(Identity identity, Instant createdAt,
                                         MediaCatalogQueueState state) throws IOException {
        MediaCatalogJob job = new MediaCatalogJob(MediaCatalogJob.CURRENT_SCHEMA_VERSION,
                state.nextSequence(), identity.eventId(), identity.laneUid(), identity.cameraUid(),
                createdAt);
        MediaCatalogQueueState reserved = state.reserve(Instant.now());
        writeCatalogQueueState(reserved);
        atomicWriteJson(catalogItemKey(job.sequence()), job, MediaCatalogJob.class);
        atomicWriteJson(catalogMemberKey(identity), job, MediaCatalogJob.class);
    }

    private MediaCatalogQueueState readCatalogQueueState() throws IOException {
        MediaCatalogQueueState state = readInternalJson(CATALOG_QUEUE_STATE_KEY,
                MediaCatalogQueueState.class).orElseGet(() ->
                MediaCatalogQueueState.initial(Instant.EPOCH));
        validateCatalogQueueState(state);
        return state;
    }

    private void writeCatalogQueueState(MediaCatalogQueueState state) throws IOException {
        validateCatalogQueueState(state);
        atomicWriteJson(CATALOG_QUEUE_STATE_KEY, state, MediaCatalogQueueState.class);
    }

    private void clearCatalogCandidate(Identity identity) throws IOException {
        Optional<MediaCatalogJob> member = readInternalJson(catalogMemberKey(identity),
                MediaCatalogJob.class);
        deleteIfPresent(catalogMemberKey(identity));
        if (member.isPresent())
            deleteIfPresent(catalogItemKey(member.get().sequence()));
    }

    /**
     * Repairs the durable ledger/index state implied by an already committed manifest. Lifecycle
     * work is committed before a PUBLISHED ledger is allowed to remove the retry entry.
     */
    public void reconcileCommittedManifest(ManifestEntry entry, Instant at) throws IOException {
        MediaManifest manifest = entry.manifest();
        Identity identity = new Identity(manifest.eventId(), manifest.laneUid(), manifest.cameraUid());
        withIdentityLock(identity, () -> {
            if (isTerminallyDeleted(identity)) {
                finishTerminalDeletionLocked(identity);
                return null;
            }
            ensureManifestMaintenance(identity, manifest);
            MediaJobLedger.JobState desired = manifest.source().state()
                    == MediaManifest.SourceState.DELETED
                    ? MediaJobLedger.JobState.SOURCE_DELETED
                    : MediaJobLedger.JobState.PUBLISHED;
            Optional<MediaJobLedger> current = readLedger(identity);
            if (current.isEmpty()) {
                writeLedger(identity, new MediaJobLedger(MediaJobLedger.CURRENT_SCHEMA_VERSION,
                        identity.eventId(), identity.laneUid(), identity.cameraUid(),
                        manifest.source().objectKey(), desired, 1, at, ""));
            } else if (current.get().state() != desired) {
                writeLedger(identity, current.get().transition(desired, "", at));
            }
            return null;
        });
    }

    public void verifyPublishedMedia(MediaManifest manifest) throws IOException {
        validateManifestAssetsExist(manifest);
        validateManifestSource(manifest);
    }

    public void verifyPublishedAsset(MediaManifest.Asset asset) throws IOException {
        verifyAsset(asset);
    }

    /**
     * Opens an asset once, caps actual bytes before allocation growth, and verifies that same byte
     * array before it can be served. This closes the verify-path/reopen-path replacement window.
     */
    public byte[] readVerifiedAsset(MediaManifest.Asset asset) throws IOException {
        Objects.requireNonNull(asset, "asset");
        if (asset.bytes() <= 0 || asset.bytes() > MediaManifest.MAX_JPEG_BYTES)
            throw new IOException("Retained frame byte count exceeds the safety limit");
        int readLimit = Math.toIntExact(asset.bytes() + 1);
        try (var input = bucketStore.getObject(VIDEO_BUCKET, asset.objectKey())) {
            byte[] bytes = input.readNBytes(readLimit);
            if (bytes.length != asset.bytes())
                throw new IOException("Retained frame actual byte count does not match its manifest");
            if (!sha256(bytes).equals(asset.sha256()))
                throw new IOException("Retained frame actual hash does not match its manifest");
            JpegFrameExtractor.JpegInfo jpeg = JpegFrameExtractor.verifyBaselineSrgbJpeg(bytes);
            if (jpeg.width() != asset.width() || jpeg.height() != asset.height())
                throw new IOException("Retained frame actual dimensions do not match its manifest");
            return bytes;
        } catch (DataStoreException e) {
            throw new IOException("Unable to read retained frame " + asset.objectKey(), e);
        }
    }

    public void cleanupUncommittedFrames(Identity identity) throws IOException {
        String committedKey = manifestKey(identity);
        Path committedPath = objectPathWithoutCreatingParents(committedKey);
        if (requireRegularObjectOrDurableAbsence(committedPath, committedKey))
            return;
        String setKey = baseKey(identity);
        Path setDirectory = committedPath.getParent();
        if (setDirectory == null || Files.notExists(setDirectory, LinkOption.NOFOLLOW_LINKS))
            return;
        List<Path> candidates;
        try (var files = Files.list(setDirectory)) {
            candidates = files.filter(Files::isRegularFile).toList();
        }
        for (Path file : candidates) {
            String name = file.getFileName().toString();
            if (name.endsWith(".jpg") || name.contains(PARTIAL_MARKER))
                deleteIfPresent(setKey + "/" + name);
        }
    }

    /** Deletes a catalog set as a unit: deleting state first, commit marker last. */
    public long deleteMediaSet(ManifestEntry entry) throws IOException {
        MediaManifest supplied = entry.manifest();
        Identity identity = new Identity(supplied.eventId(), supplied.laneUid(), supplied.cameraUid());
        return withIdentityLock(identity, () -> deleteMediaSetLocked(entry, identity));
    }

    /**
     * Completes an interrupted terminal deletion, including removal of its durable queue work.
     * A terminal index entry is authoritative even if a pre-crash manifest unlink reappears.
     */
    public long finishTerminalDeletion(Identity identity) throws IOException {
        Objects.requireNonNull(identity, "identity");
        return withIdentityLock(identity, () -> {
            if (!isTerminallyDeleted(identity))
                throw new IOException("Retained media set is not terminally deleted: "
                        + identity.mediaSetId());
            return finishTerminalDeletionLocked(identity);
        });
    }

    private long finishTerminalDeletionLocked(Identity identity) throws IOException {
        String key = manifestKey(identity);
        Path path = objectPathWithoutCreatingParents(key);
        if (requireRegularObjectOrDurableAbsence(path, key)) {
            MediaManifest manifest = readManifest(key);
            return deleteMediaSetLocked(new ManifestEntry(key, manifest), identity);
        }
        clearMaintenance(identity);
        clearPressureCandidate(identity);
        clearCatalogCandidate(identity);
        return 0;
    }

    private long deleteMediaSetLocked(ManifestEntry entry, Identity identity) throws IOException {
        Path manifestPath = objectPathWithoutCreatingParents(entry.manifestKey());
        if (!requireRegularObjectOrDurableAbsence(manifestPath, entry.manifestKey())) {
            if (isTerminallyDeleted(identity)) {
                clearMaintenance(identity);
                clearPressureCandidate(identity);
                clearCatalogCandidate(identity);
            }
            return 0;
        }
        MediaManifest manifest = readManifest(entry.manifestKey());
        boolean deletionAlreadyStarted = manifest.lifecycleState()
                == MediaManifest.LifecycleState.DELETING || isTerminallyDeleted(identity);
        if (!deletionAlreadyStarted
                && (manifest.isHeld() || manifest.lifecycleState() == MediaManifest.LifecycleState.HELD))
            return 0;
        if (!deletionAlreadyStarted
                && manifest.source().state() == MediaManifest.SourceState.PRESERVED_FOR_ROLLBACK
                && Instant.now().isBefore(manifest.source().rollbackDeleteAfter()))
            return 0;

        if (!deletionAlreadyStarted) {
            manifest = manifest.withLifecycleState(MediaManifest.LifecycleState.DELETING);
            publishManifest(identity, manifest);
        }

        // The exact terminal index record is appended and fsynced before the first destructive
        // delete. It survives the set without one permanent JSON/inode per camera and prevents a
        // stale retry, overlapping scan, or restart from reconstructing intentionally deleted media.
        if (!isTerminallyDeleted(identity))
            recordTerminalDeletion(identity);
        clearMaintenance(identity);

        long deletedBytes = 0;
        try {
            for (MediaManifest.Asset asset : manifest.assets())
                deletedBytes += deleteIfPresent(asset.objectKey());
            // A terminal set delete verifies the source is absent even when an older manifest says
            // DELETED. This also removes a directory entry replayed after a pre-upgrade crash.
            deletedBytes += deleteIfPresent(stripVideoBucket(manifest.source().objectKey()));
            deletedBytes += deleteIfPresent(ledgerKey(identity));
            deletedBytes += deleteIfPresent(entry.manifestKey());
            clearMaintenance(identity);
            clearPressureCandidate(identity);
            clearCatalogCandidate(identity);
            return deletedBytes;
        } catch (IOException e) {
            // The terminal tombstone plus either the DELETING manifest or remaining catalog and
            // pressure work stay durable. A future cleanup pass retries every idempotent delete.
            throw e;
        }
    }

    /** Deletes a videos-bucket object and makes its absence durable before returning. */
    public long deleteVideoObjectDurably(String key) throws IOException {
        return deleteIfPresent(key);
    }

    public long mediaSetBytes(MediaManifest manifest) {
        long bytes = manifest.source().state() == MediaManifest.SourceState.DELETED
                ? 0 : manifest.source().bytes();
        for (MediaManifest.Asset asset : manifest.assets())
            bytes += asset.bytes();
        return bytes;
    }

    public IBucketStore bucketStore() {
        return bucketStore;
    }

    <T> T withIdentityLock(Identity identity, IOCallable<T> action) throws IOException {
        int lockIndex = (identity.mediaSetId().hashCode() & Integer.MAX_VALUE) % identityLocks.length;
        synchronized (identityLocks[lockIndex]) {
            return action.call();
        }
    }

    private MediaMetadataReclamationCheckpoint readMetadataReclamationCheckpoint()
            throws IOException {
        Path path = writablePath(METADATA_RECLAMATION_CHECKPOINT_KEY);
        if (!requireExistingOrConfirmedAbsent(path, "media metadata checkpoint"))
            return MediaMetadataReclamationCheckpoint.initial(Instant.EPOCH);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path))
            throw new IOException("Media metadata checkpoint is not a regular file");
        MediaMetadataReclamationCheckpoint checkpoint = readInternalJson(
                METADATA_RECLAMATION_CHECKPOINT_KEY,
                MediaMetadataReclamationCheckpoint.class).orElseThrow(() ->
                new IOException("Media metadata checkpoint disappeared during locked read"));
        // Invoke the canonical constructor for validation after deserialization.
        try {
            return new MediaMetadataReclamationCheckpoint(checkpoint.schemaVersion(),
                    checkpoint.earliestProcessedObservationEpochHour(), checkpoint.updatedAt());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IOException("Invalid media metadata checkpoint", e);
        }
    }

    private void writeMetadataReclamationCheckpoint(
            MediaMetadataReclamationCheckpoint checkpoint) throws IOException {
        Objects.requireNonNull(checkpoint, "checkpoint");
        atomicWriteJson(METADATA_RECLAMATION_CHECKPOINT_KEY, checkpoint,
                MediaMetadataReclamationCheckpoint.class);
    }

    /** Commits one exact digest to a bounded-shard, append-only, checksummed deletion index. */
    void recordTerminalDeletion(Identity identity) throws IOException {
        String mediaSetId = identity.mediaSetId();
        String key = deletionIndexKey(mediaSetId);
        withIndexFileLock(deletionIndexLockKey(mediaSetId), () -> {
            appendExactIndexDigest(writablePath(key), key, DELETION_INDEX_HEADER, mediaSetId,
                    digest -> deletionIndexKey(digest).equals(key), "media deletion index");
            return null;
        });
    }

    private void appendExactIndexDigest(Path path, String key, byte[] expectedHeader,
                                        String digestHex, Predicate<String> recordValidator,
                                        String description) throws IOException {
        ensureExactIndexHeader(path, expectedHeader);
        ExactIndexReadResult existing = readExactIndex(path, key, expectedHeader,
                recordValidator, description, digestHex);
        if (existing.containsTarget())
            return;

        byte[] digest = hexBytes(digestHex);
        ByteBuffer record = ByteBuffer.allocate(EXACT_INDEX_RECORD_BYTES);
        record.put(digest);
        CRC32C checksum = new CRC32C();
        checksum.update(digest, 0, digest.length);
        record.putInt((int) checksum.getValue());
        record.flip();

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            channel.position(channel.size());
            while (record.hasRemaining())
                channel.write(record);
            channel.force(true);
        }
        forceDirectory(path.getParent());
        if (existing.cacheableEntries() != null) {
            Set<String> updated = existing.cacheableEntries();
            // Remove the old accounting before mutating the shared cached set in place.
            invalidateExactIndex(key);
            updated.add(digestHex);
            cacheExactIndex(key, path, updated);
        } else {
            invalidateExactIndex(key);
        }
    }

    private ExactIndexReadResult readExactIndex(Path path, String key, byte[] expectedHeader,
                                                Predicate<String> recordValidator,
                                                String description, String targetDigest)
            throws IOException {
        BasicFileAttributes initialAttributes = readRegularFileAttributes(path, key, description);
        FileStamp initialStamp = FileStamp.from(initialAttributes);
        ExactIndexSnapshot cached = cachedExactIndex(key);
        if (cached != null && cached.stamp().equals(initialStamp)) {
            // A matching stamp is a performance hint, not authority to bypass current access
            // checks. An index made unreadable after it was cached must fail closed.
            verifyExactIndexReadable(path, key, description);
            return new ExactIndexReadResult(cached.entries().contains(targetDigest),
                    cached.entries());
        }

        Set<String> loaded = null;
        boolean containsTarget = false;
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.READ, StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            long size = channel.size();
            if (size < expectedHeader.length)
                throw new IOException(description + " header is truncated: " + key);
            ByteBuffer header = ByteBuffer.allocate(expectedHeader.length);
            readFully(channel, header, 0, description + " header");
            if (!java.util.Arrays.equals(header.array(), expectedHeader))
                throw new IOException(description + " header is invalid: " + key);

            long completeBytes = ((size - expectedHeader.length)
                    / EXACT_INDEX_RECORD_BYTES) * EXACT_INDEX_RECORD_BYTES;
            long validSize = expectedHeader.length + completeBytes;
            if (validSize != size) {
                // Only an interrupted final append can create a short tail. A complete record with
                // a bad checksum is never truncated, because that could resurrect deleted media.
                channel.truncate(validSize);
                channel.force(true);
                forceDirectory(path.getParent());
                size = validSize;
            }

            long recordCount = completeBytes / EXACT_INDEX_RECORD_BYTES;
            if (recordCount <= EXACT_INDEX_CACHE_MAX_RECORDS_PER_SNAPSHOT)
                loaded = new HashSet<>((int) Math.min(Integer.MAX_VALUE,
                        Math.max(16L, recordCount * 4L / 3L + 1L)));

            long position = expectedHeader.length;
            ByteBuffer record = ByteBuffer.allocate(EXACT_INDEX_RECORD_BYTES);
            byte[] digest = new byte[EXACT_INDEX_DIGEST_BYTES];
            while (position < size) {
                record.clear();
                readFully(channel, record, position, description + " record");
                record.flip();
                record.get(digest);
                int expectedChecksum = record.getInt();
                CRC32C checksum = new CRC32C();
                checksum.update(digest, 0, digest.length);
                if ((int) checksum.getValue() != expectedChecksum)
                    throw new IOException(description + " checksum is invalid: " + key);
                String digestHex = hex(digest);
                if (!recordValidator.test(digestHex))
                    throw new IOException(description + " record is in the wrong bucket: " + key);
                if (digestHex.equals(targetDigest))
                    containsTarget = true;
                if (loaded != null)
                    loaded.add(digestHex);
                position += EXACT_INDEX_RECORD_BYTES;
            }
        }
        if (loaded == null) {
            invalidateExactIndex(key);
            return new ExactIndexReadResult(containsTarget, null);
        }
        cacheExactIndex(key, path, loaded);
        return new ExactIndexReadResult(containsTarget, loaded);
    }

    private BasicFileAttributes readRegularFileAttributes(Path path, String key,
                                                          String description)
            throws IOException {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new IOException("Unable to inspect " + description + ": " + key, e);
        }
        if (!attributes.isRegularFile() || Files.isSymbolicLink(path))
            throw new IOException(description + " is not a regular file: " + key);
        return attributes;
    }

    private void verifyExactIndexReadable(Path path, String key, String description)
            throws IOException {
        try (FileChannel ignored = FileChannel.open(path, StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            // Opening is the check; no content read is required because this exact file stamp was
            // fully checksummed before entering the cache.
        } catch (IOException | UnsupportedOperationException e) {
            throw new IOException("Unable to read " + description + ": " + key, e);
        }
    }

    private void ensureExactIndexHeader(Path path, byte[] header) throws IOException {
        if (requireExistingOrConfirmedAbsent(path, "retained-media exact index"))
            return;
        Path partial = partialSibling(path);
        try {
            writeAndForce(partial, header);
            try {
                Files.move(partial, path, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                throw new IOException(
                        "Retained-media exact indexes require same-directory atomic rename", e);
            }
            forceDirectory(path.getParent());
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    private <T> T withIndexFileLock(String lockKey, IOCallable<T> action) throws IOException {
        Path lockPath = writablePath(lockKey);
        int stripe = (lockPath.toString().hashCode() & Integer.MAX_VALUE)
                % INDEX_FILE_LOCKS.length;
        synchronized (INDEX_FILE_LOCKS[stripe]) {
            boolean existed = requireExistingOrConfirmedAbsent(
                    lockPath, "retained-media index lock");
            if (existed)
                readRegularFileAttributes(
                        lockPath, lockKey, "retained-media index lock");
            try (FileChannel channel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS)) {
                // Revalidate after opening to close the check/create race. The open option rejects
                // a final symlink; this check also rejects directories and other irregular files.
                readRegularFileAttributes(lockPath, lockKey, "retained-media index lock");
                try (FileLock ignored = channel.lock()) {
                    return action.call();
                }
            }
        }
    }

    private static boolean requireExistingOrConfirmedAbsent(Path path, String description)
            throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS))
            return true;
        if (Files.notExists(path, LinkOption.NOFOLLOW_LINKS))
            return false;
        throw new IOException("Unable to determine whether " + description + " exists: " + path);
    }

    private ExactIndexSnapshot cachedExactIndex(String key) {
        synchronized (exactIndexCache) {
            return exactIndexCache.get(key);
        }
    }

    private void cacheExactIndex(String key, Path path, Set<String> entries) throws IOException {
        BasicFileAttributes attributes = readRegularFileAttributes(path, key, "retained-media index");
        synchronized (exactIndexCache) {
            ExactIndexSnapshot previous = exactIndexCache.remove(key);
            if (previous != null)
                exactIndexCacheRecordCount -= previous.entries().size();
            // One pathological high-rate index file must not consume unbounded heap. It remains fully
            // queryable from the checksummed file, but is deliberately not retained in memory.
            if (entries.size() > EXACT_INDEX_CACHE_MAX_RECORDS_PER_SNAPSHOT)
                return;
            ExactIndexSnapshot snapshot = new ExactIndexSnapshot(
                    FileStamp.from(attributes), entries);
            exactIndexCache.put(key, snapshot);
            exactIndexCacheRecordCount += snapshot.entries().size();
            Iterator<Map.Entry<String, ExactIndexSnapshot>> iterator =
                    exactIndexCache.entrySet().iterator();
            while ((exactIndexCache.size() > EXACT_INDEX_CACHE_MAX_ENTRIES
                    || exactIndexCacheRecordCount > EXACT_INDEX_CACHE_MAX_RECORDS)
                    && iterator.hasNext()) {
                Map.Entry<String, ExactIndexSnapshot> eldest = iterator.next();
                exactIndexCacheRecordCount -= eldest.getValue().entries().size();
                iterator.remove();
            }
        }
    }

    private void invalidateExactIndex(String key) {
        synchronized (exactIndexCache) {
            ExactIndexSnapshot removed = exactIndexCache.remove(key);
            if (removed != null)
                exactIndexCacheRecordCount -= removed.entries().size();
        }
    }

    ExactIndexCacheStats exactIndexCacheStats() {
        synchronized (exactIndexCache) {
            return new ExactIndexCacheStats(exactIndexCache.size(), exactIndexCacheRecordCount,
                    EXACT_INDEX_CACHE_MAX_ENTRIES, EXACT_INDEX_CACHE_MAX_RECORDS,
                    EXACT_INDEX_CACHE_MAX_RECORDS_PER_SNAPSHOT);
        }
    }

    private static void readFully(FileChannel channel, ByteBuffer target, long position,
                                  String description) throws IOException {
        while (target.hasRemaining()) {
            int read = channel.read(target, position + target.position());
            if (read < 0)
                throw new IOException("Unexpected end of " + description);
            if (read == 0)
                throw new IOException("Unable to make progress reading " + description);
        }
    }

    private <T> Optional<T> readInternalJson(String key, Class<T> type) throws IOException {
        if (!bucketStore.objectExists(VIDEO_BUCKET, key))
            return Optional.empty();
        try (Reader reader = new java.io.StringReader(readBoundedJson(key))) {
            T value = GSON.fromJson(reader, type);
            if (value == null)
                throw new IOException("Internal media JSON is empty: " + key);
            return Optional.of(value);
        } catch (RuntimeException e) {
            throw new IOException("Invalid internal media JSON " + key, e);
        }
    }

    private String readBoundedJson(String key) throws IOException {
        try (var input = bucketStore.getObject(VIDEO_BUCKET, key)) {
            byte[] bytes = input.readNBytes(MAX_INTERNAL_JSON_BYTES + 1);
            if (bytes.length > MAX_INTERNAL_JSON_BYTES)
                throw new IOException("Internal media JSON exceeds the safety limit: " + key);
            if (bytes.length == 0)
                throw new IOException("Internal media JSON is empty: " + key);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (DataStoreException e) {
            throw new IOException("Unable to read internal media JSON " + key, e);
        }
    }

    private static String maintenanceKey(MediaMaintenanceJob.Kind kind, Identity identity) {
        String mediaSetId = identity.mediaSetId();
        int shard = Integer.parseInt(mediaSetId.substring(0, 2), 16);
        return maintenanceShardPrefix(kind, shard) + mediaSetId + ".json";
    }

    private static String maintenanceShardPrefix(MediaMaintenanceJob.Kind kind, int shard) {
        return MAINTENANCE_PREFIX + kind.name().toLowerCase(java.util.Locale.ROOT) + "/"
                + String.format(Locale.ROOT, "%02x", shard) + "/";
    }

    private static String processedObservationDigest(String observationId) {
        if (observationId == null || observationId.isBlank())
            throw new IllegalArgumentException("observationId is required");
        return sha256(observationId.getBytes(StandardCharsets.UTF_8));
    }

    static String processedObservationIndexKey(Instant resultTime) {
        long epochHour = processedObservationEpochHour(resultTime);
        long secondsWithinHour = Math.floorMod(resultTime.getEpochSecond(),
                PROCESSED_OBSERVATION_BUCKET_SECONDS);
        int file = Math.toIntExact(secondsWithinHour
                / PROCESSED_OBSERVATION_INDEX_FILE_SECONDS);
        return processedObservationIndexKey(epochHour, file);
    }

    private static String processedObservationIndexKey(long epochHour, int file) {
        if (file < 0 || file >= PROCESSED_OBSERVATION_INDEX_FILES_PER_HOUR)
            throw new IllegalArgumentException("Processed-observation index file is invalid");
        return PROCESSED_OBSERVATION_INDEX_PREFIX
                + String.format(Locale.ROOT, "%019d-%02d.idx", epochHour, file);
    }

    private static long processedObservationEpochHour(Instant resultTime) {
        Objects.requireNonNull(resultTime, "resultTime");
        long epochHour = Math.floorDiv(resultTime.getEpochSecond(),
                PROCESSED_OBSERVATION_BUCKET_SECONDS);
        long maximumEpochHour = Math.floorDiv(Instant.MAX.getEpochSecond(),
                PROCESSED_OBSERVATION_BUCKET_SECONDS) - 1L;
        if (epochHour < 0 || epochHour > maximumEpochHour)
            throw new IllegalArgumentException(
                    "Processed-observation result time is outside the supported range");
        return epochHour;
    }

    private static Instant processedObservationBucketEnd(long epochHour) {
        try {
            return Instant.ofEpochSecond(Math.multiplyExact(Math.addExact(epochHour, 1L),
                    PROCESSED_OBSERVATION_BUCKET_SECONDS));
        } catch (ArithmeticException | java.time.DateTimeException e) {
            return epochHour >= 0 ? Instant.MAX : Instant.MIN;
        }
    }

    static String deletionIndexKey(Identity identity) {
        return deletionIndexKey(identity.mediaSetId());
    }

    private static String deletionIndexKey(String mediaSetId) {
        if (mediaSetId == null || !mediaSetId.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("mediaSetId must be a lowercase SHA-256 digest");
        return DELETION_INDEX_PREFIX + mediaSetId.substring(0, 2) + "/"
                + mediaSetId.substring(0, 4) + ".idx";
    }

    private static String deletionIndexLockKey(String mediaSetId) {
        if (mediaSetId == null || !mediaSetId.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("mediaSetId must be a lowercase SHA-256 digest");
        return DELETION_INDEX_PREFIX + mediaSetId.substring(0, 2) + "/.index.lock";
    }

    private static byte[] hexBytes(String value) {
        if (value == null || (value.length() & 1) != 0)
            throw new IllegalArgumentException("Hex value must have an even length");
        byte[] result = new byte[value.length() / 2];
        for (int index = 0; index < result.length; index++) {
            int high = Character.digit(value.charAt(index * 2), 16);
            int low = Character.digit(value.charAt(index * 2 + 1), 16);
            if (high < 0 || low < 0)
                throw new IllegalArgumentException("Hex value contains a non-hexadecimal character");
            result[index] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private static Instant retryDueAt(MediaJobLedger ledger) {
        int exponent = Math.min(Math.max(ledger.attempts() - 1, 0), 8);
        long delayMinutes = Math.min(24L * 60L, 5L << exponent);
        return ledger.updatedAt().plusSeconds(delayMinutes * 60L);
    }

    private static String pressureItemKey(long sequence) {
        if (sequence < 0)
            throw new IllegalArgumentException("pressure queue sequence must not be negative");
        return PRESSURE_QUEUE_ITEM_PREFIX
                + String.format(Locale.ROOT, "%019d.json", sequence);
    }

    private static String pressureMemberKey(Identity identity) {
        String mediaSetId = identity.mediaSetId();
        return PRESSURE_QUEUE_MEMBER_PREFIX + mediaSetId.substring(0, 2) + "/"
                + mediaSetId + ".json";
    }

    private static String catalogItemKey(long sequence) {
        if (sequence < 0)
            throw new IllegalArgumentException("catalog queue sequence must not be negative");
        return CATALOG_QUEUE_ITEM_PREFIX
                + String.format(Locale.ROOT, "%019d.json", sequence);
    }

    private static String catalogMemberKey(Identity identity) {
        String mediaSetId = identity.mediaSetId();
        return CATALOG_QUEUE_MEMBER_PREFIX + mediaSetId.substring(0, 2) + "/"
                + mediaSetId + ".json";
    }

    private static void validateRetentionCheckpoint(RetentionScanCheckpoint checkpoint) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (checkpoint.schemaVersion() != RetentionScanCheckpoint.CURRENT_SCHEMA_VERSION
                || checkpoint.processedThrough() == null || checkpoint.updatedAt() == null
                || checkpoint.updatedAt().isBefore(checkpoint.processedThrough()))
            throw new IllegalArgumentException("Invalid retained-media scan checkpoint");
    }

    private static void validateMaintenanceCheckpoint(MediaMaintenanceCheckpoint checkpoint) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        new MediaMaintenanceCheckpoint(checkpoint.schemaVersion(), checkpoint.lifecycleShard(),
                checkpoint.lifecycleAfterKey(), checkpoint.retryShard(), checkpoint.retryAfterKey(),
                checkpoint.catalogAfterKey(), checkpoint.updatedAt());
    }

    private static void validateMaintenanceJob(MediaMaintenanceJob job) {
        Objects.requireNonNull(job, "job");
        new MediaMaintenanceJob(job.schemaVersion(), job.kind(), job.eventId(), job.laneUid(),
                job.cameraUid(), job.sourceObjectKey(), job.dueAt(), job.attempts());
    }

    private static void validatePressureQueueState(MediaPressureQueueState state) {
        Objects.requireNonNull(state, "state");
        new MediaPressureQueueState(state.schemaVersion(), state.headSequence(),
                state.nextSequence(), state.updatedAt());
    }

    private static void validatePressureJob(MediaPressureJob job) {
        Objects.requireNonNull(job, "job");
        new MediaPressureJob(job.schemaVersion(), job.sequence(), job.eventId(), job.laneUid(),
                job.cameraUid(), job.createdAt());
    }

    private static void validateCatalogQueueState(MediaCatalogQueueState state) {
        Objects.requireNonNull(state, "state");
        new MediaCatalogQueueState(state.schemaVersion(), state.headSequence(),
                state.nextSequence(), state.updatedAt());
    }

    private static void validateCatalogJob(MediaCatalogJob job) {
        Objects.requireNonNull(job, "job");
        new MediaCatalogJob(job.schemaVersion(), job.sequence(), job.eventId(), job.laneUid(),
                job.cameraUid(), job.createdAt());
    }

    public static boolean isInternalMediaKey(String key) {
        return key != null && key.startsWith("retained-media/");
    }

    public static boolean isApiKey(String key) {
        return key != null && (key.equals("media") || key.startsWith(API_PREFIX));
    }

    public static String apiCameraToken(String cameraUid) {
        validateIdentity(cameraUid, "cameraUid");
        return encode(cameraUid);
    }

    public static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(Path path) throws IOException {
        MessageDigest digest = sha256Digest();
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0)
                    digest.update(buffer, 0, count);
            }
        }
        return hex(digest.digest());
    }

    public static String sha256(byte[] bytes) {
        return hex(sha256Digest().digest(bytes));
    }

    public static String stripVideoBucket(String objectKey) {
        String normalized = objectKey.replace('\\', '/');
        while (normalized.startsWith("/"))
            normalized = normalized.substring(1);
        return normalized.startsWith(VIDEO_BUCKET + "/")
                ? normalized.substring(VIDEO_BUCKET.length() + 1) : normalized;
    }

    static String normalizedSourceKey(String objectKey) throws IOException {
        if (objectKey == null)
            throw new IOException("Retained-media source object key is required");
        if (objectKey.length() > 4_096)
            throw new IOException("Retained-media source object key exceeds the safety limit");
        String normalized = stripVideoBucket(objectKey).replace('\\', '/');
        if (normalized.isBlank() || normalized.contains(PARTIAL_MARKER)
                || isInternalMediaKey(normalized) || isApiKey(normalized))
            throw new IOException("Retained-media source object key is not a published video");
        for (String component : normalized.split("/", -1)) {
            if (component.isEmpty() || component.equals(".") || component.equals(".."))
                throw new IOException("Retained-media source object key is not confined");
        }
        return normalized;
    }

    private void validateManifestAssetsExist(MediaManifest manifest) throws IOException {
        EnumSet<MediaManifest.FramePosition> positions = EnumSet.noneOf(MediaManifest.FramePosition.class);
        for (MediaManifest.Asset asset : manifest.assets()) {
            if (!positions.add(asset.position()))
                throw new IOException("Manifest repeats retained frame position " + asset.position());
            verifyAsset(asset);
        }
        if (positions.size() != MediaManifest.RETAINED_FRAME_COUNT)
            throw new IOException("Manifest must contain each of the five retained frame positions");
    }

    private void verifyAsset(MediaManifest.Asset asset) throws IOException {
        if (asset.objectKey().contains(PARTIAL_MARKER)
                || !bucketStore.objectExists(VIDEO_BUCKET, asset.objectKey()))
            throw new IOException("Manifest references an unpublished asset: " + asset.objectKey());
        try {
            if (bucketStore.getObjectSize(VIDEO_BUCKET, asset.objectKey()) != asset.bytes())
                throw new IOException("Manifest asset byte count does not match " + asset.objectKey());
            Path path = Path.of(bucketStore.getResourceURI(VIDEO_BUCKET, asset.objectKey()));
            if (!sha256(path).equals(asset.sha256()))
                throw new IOException("Manifest asset hash does not match " + asset.objectKey());
            JpegFrameExtractor.JpegInfo jpeg = JpegFrameExtractor.verifyBaselineSrgbJpeg(path);
            if (jpeg.width() != asset.width() || jpeg.height() != asset.height())
                throw new IOException("Manifest asset dimensions do not match " + asset.objectKey());
        } catch (DataStoreException e) {
            throw new IOException("Unable to verify manifest asset " + asset.objectKey(), e);
        }
    }

    private void validateManifestSource(MediaManifest manifest) throws IOException {
        if (manifest.source().state() == MediaManifest.SourceState.DELETED)
            return;
        String key = normalizedSourceKey(manifest.source().objectKey());
        if (!bucketStore.objectExists(VIDEO_BUCKET, key)) {
            if (manifest.source().state() == MediaManifest.SourceState.ELIGIBLE_FOR_DELETION)
                return; // Recover a crash after source deletion but before the DELETED commit.
            throw new IOException("Manifest references an unpublished source: " + key);
        }
        try {
            if (bucketStore.getObjectSize(VIDEO_BUCKET, key) != manifest.source().bytes())
                throw new IOException("Manifest source byte count does not match " + key);
            Path path = Path.of(bucketStore.getResourceURI(VIDEO_BUCKET, key));
            if (!sha256(path).equals(manifest.source().sha256()))
                throw new IOException("Manifest source hash does not match " + key);
        } catch (DataStoreException e) {
            throw new IOException("Unable to verify manifest source " + key, e);
        }
    }

    private void atomicWriteJson(String key, Object value, Class<?> expectedClass) throws IOException {
        byte[] bytes = GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
        Path target = writablePath(key);
        Path partial = partialSibling(target);
        try {
            writeAndForce(partial, bytes);
            try (Reader reader = Files.newBufferedReader(partial, StandardCharsets.UTF_8)) {
                Object reopened = GSON.fromJson(reader, expectedClass);
                if (reopened == null)
                    throw new IOException("Serialized JSON could not be parsed");
                if (!value.equals(reopened))
                    throw new IOException("Serialized JSON did not round-trip exactly");
            } catch (RuntimeException e) {
                throw new IOException("Serialized JSON could not be verified", e);
            }
            atomicReplace(partial, target);
            forceDirectory(target.getParent());
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    private Path writablePath(String key) throws IOException {
        try {
            return Path.of(bucketStore.getWritableResourceURI(VIDEO_BUCKET, key))
                    .toAbsolutePath().normalize();
        } catch (DataStoreException e) {
            throw new IOException("Unable to prepare retained media object " + key, e);
        }
    }

    private long deleteIfPresent(String key) throws IOException {
        return deleteDurably(key, false).bytes();
    }

    /**
     * Deletes one regular object and durably records its absence. Reclamation deliberately issues
     * its bounded direct delete even when the file is already absent, then requires a fresh parent
     * barrier before its checkpoint may advance.
     */
    private DurableDeleteResult deleteDurably(String key, boolean deleteWhenAbsent)
            throws IOException {
        Path path = objectPathWithoutCreatingParents(key);
        validateExistingDeleteParentPrefix(path);
        boolean existed = requireExistingOrConfirmedAbsent(path, "retained media object");
        BasicFileAttributes attributes = null;
        if (existed)
            attributes = readRegularFileAttributes(path, key, "retained media object");
        if (!existed && !deleteWhenAbsent) {
            forceDurableAbsence(path, key);
            return new DurableDeleteResult(false, 0);
        }

        long bytes = attributes == null ? 0 : attributes.size();
        try {
            bucketStore.deleteObject(VIDEO_BUCKET, key);
        } catch (DataStoreException e) {
            throw new IOException("Unable to delete retained media object " + key, e);
        }
        // The queue/member state that makes this delete retryable is cleared only after the
        // nearest existing parent has durably recorded this unlink (or a missing parent).
        forceDurableAbsence(path, key);
        return new DurableDeleteResult(existed, bytes);
    }

    private record DurableDeleteResult(boolean existed, long bytes) {
    }

    private Path objectPathWithoutCreatingParents(String key) throws IOException {
        try {
            if (key == null || key.isBlank())
                throw new IllegalArgumentException("Object key is required");
            Path relative = Path.of(key.replace('\\', '/'));
            if (relative.isAbsolute() || relative.getRoot() != null || relative.getNameCount() == 0)
                throw new IllegalArgumentException("Object key must be relative");
            for (Path component : relative) {
                if (component.toString().isBlank() || component.toString().equals(".")
                        || component.toString().equals(".."))
                    throw new IllegalArgumentException("Object key must remain inside the bucket");
            }
            Path resolved = videoBucketRoot.resolve(relative).normalize();
            if (resolved.equals(videoBucketRoot) || !resolved.startsWith(videoBucketRoot))
                throw new IllegalArgumentException("Object key must remain inside the bucket");
            return resolved;
        } catch (RuntimeException e) {
            throw new IOException("Invalid retained media object key " + key, e);
        }
    }

    private boolean requireRegularObjectOrDurableAbsence(Path path, String key)
            throws IOException {
        validateExistingDeleteParentPrefix(path);
        if (requireExistingOrConfirmedAbsent(path, "retained media object")) {
            readRegularFileAttributes(path, key, "retained media object");
            return true;
        }
        forceDurableAbsence(path, key);
        return false;
    }

    /**
     * Makes a visible absence durable without creating the missing object's parent. If one or more
     * parent directories are also absent, forcing the nearest existing ancestor durably records
     * the first missing directory entry and therefore the complete descendant absence.
     */
    private void forceDurableAbsence(Path path, String key) throws IOException {
        validateExistingDeleteParentPrefix(path);
        if (!Files.notExists(path, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Retained media object is not confirmed absent: " + key);

        Path candidate = path.getParent();
        while (candidate != null && candidate.startsWith(videoBucketRoot)) {
            if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
                validateExistingDirectoryChain(candidate);
                forceDirectory(candidate);
                if (!Files.notExists(path, LinkOption.NOFOLLOW_LINKS))
                    throw new IOException(
                            "Retained media object appeared during durable deletion: " + key);
                return;
            }
            if (!Files.notExists(candidate, LinkOption.NOFOLLOW_LINKS))
                throw new IOException(
                        "Unable to confirm retained media parent absence: " + candidate);
            candidate = candidate.getParent();
        }
        throw new IOException("Unable to find a durable parent for retained media object " + key);
    }

    private void validateExistingDeleteParentPrefix(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent == null || !parent.startsWith(videoBucketRoot))
            throw new IOException("Retained media object has no confined parent: " + path);
        Path current = videoBucketRoot;
        requireRegularDirectory(current);
        for (Path component : videoBucketRoot.relativize(parent)) {
            current = current.resolve(component);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                requireRegularDirectory(current);
            } else if (Files.notExists(current, LinkOption.NOFOLLOW_LINKS)) {
                return;
            } else {
                throw new IOException(
                        "Unable to inspect retained media parent directory: " + current);
            }
        }
    }

    private void validateExistingDirectoryChain(Path directory) throws IOException {
        Path current = videoBucketRoot;
        requireRegularDirectory(current);
        for (Path component : videoBucketRoot.relativize(directory)) {
            current = current.resolve(component);
            requireRegularDirectory(current);
        }
    }

    private static void requireRegularDirectory(Path directory) throws IOException {
        final BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(directory, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new IOException("Unable to inspect retained media parent directory: "
                    + directory, e);
        }
        if (!attributes.isDirectory() || Files.isSymbolicLink(directory))
            throw new IOException(
                    "Retained media parent is not a regular directory: " + directory);
    }

    private static void validateManifestShape(MediaManifest manifest) throws IOException {
        if (manifest == null
                || manifest.schemaVersion() != MediaManifest.CURRENT_SCHEMA_VERSION
                || manifest.assets().size() != MediaManifest.RETAINED_FRAME_COUNT)
            throw new IOException("Unsupported or incomplete retained media manifest");
    }

    private void validateManifestIdentityAndKeys(String key, MediaManifest manifest)
            throws IOException {
        final Identity identity;
        try {
            identity = new Identity(manifest.eventId(), manifest.laneUid(), manifest.cameraUid());
        } catch (IllegalArgumentException e) {
            throw new IOException("Manifest contains an invalid retained-media identity", e);
        }
        if (!identity.mediaSetId().equals(manifest.mediaSetId()))
            throw new IOException("Manifest media-set identity digest does not match its fields");
        if (!manifestKey(identity).equals(key.replace('\\', '/')))
            throw new IOException("Manifest identity does not match its catalog path");
        for (MediaManifest.Asset asset : manifest.assets()) {
            int frameIndex = asset.position().ordinal() + 1;
            if (!frameKey(identity, frameIndex).equals(asset.objectKey().replace('\\', '/')))
                throw new IOException("Manifest asset does not match its retained-media set path");
        }
        normalizedSourceKey(manifest.source().objectKey());
    }

    private static boolean isFinalManifestKey(String key) {
        return key != null && key.startsWith(ROOT_PREFIX)
                && key.endsWith("/" + MANIFEST_FILE) && !key.contains(PARTIAL_MARKER);
    }

    private static Path partialSibling(Path target) {
        return target.resolveSibling(target.getFileName() + PARTIAL_MARKER + UUID.randomUUID());
    }

    private static void writeAndForce(Path path, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining())
                channel.write(buffer);
            channel.force(true);
        }
    }

    private static void atomicReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            throw new IOException("Retained media storage must support same-directory atomic rename", e);
        }
    }

    private static void forceDirectory(Path directory) throws IOException {
        if (directory == null)
            return;
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (UnsupportedOperationException e) {
            // Directory fsync is not available on every supported filesystem/JDK combination.
        } catch (IOException e) {
            // Windows commonly rejects opening a directory as a FileChannel even though the
            // same-directory atomic rename has already completed. Ignore only that precise
            // Windows limitation; POSIX and all other I/O failures remain fatal so durability is
            // never silently weakened on filesystems that support directory fsync.
            if (!isIgnorableWindowsDirectoryFsyncFailure(e, isWindows()))
                throw e;
        }
    }

    static boolean isIgnorableWindowsDirectoryFsyncFailure(IOException failure,
                                                             boolean windows) {
        return windows && failure instanceof AccessDeniedException;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
                .startsWith("windows");
    }

    private static Object[] createLocks(int count) {
        Object[] locks = new Object[count];
        for (int index = 0; index < locks.length; index++)
            locks[index] = new Object();
        return locks;
    }

    private static void validateIdentity(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 512
                || value.indexOf('\u0000') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)
            throw new IllegalArgumentException(field + " is not a valid retained-media identity");
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String hex(byte[] bytes) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] result = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            result[index * 2] = digits[value >>> 4];
            result[index * 2 + 1] = digits[value & 0x0f];
        }
        return new String(result);
    }

    public record ManifestEntry(String manifestKey, MediaManifest manifest) {}

    public record PublishedAsset(String objectKey, String sha256, long bytes, int width, int height) {}

    public record MaintenanceEntry(String key, MediaMaintenanceJob job) {}

    public record ProcessedObservationPruneResult(int inspected, int deleted, Long nextEpochHour) {
        public ProcessedObservationPruneResult {
            if (inspected < 0 || deleted < 0 || deleted > inspected)
                throw new IllegalArgumentException("Invalid processed-observation prune counts");
        }
    }

    private record FileStamp(long size, FileTime lastModifiedTime, Object fileKey) {
        private static FileStamp from(BasicFileAttributes attributes) {
            return new FileStamp(attributes.size(), attributes.lastModifiedTime(),
                    attributes.fileKey());
        }
    }

    private record ExactIndexSnapshot(FileStamp stamp, Set<String> entries) {
        private ExactIndexSnapshot {
            Objects.requireNonNull(stamp, "stamp");
            Objects.requireNonNull(entries, "entries");
        }
    }

    private record ExactIndexReadResult(boolean containsTarget,
                                        Set<String> cacheableEntries) {}

    record ExactIndexCacheStats(int entries, int records, int maximumEntries,
                                int maximumRecords, int maximumRecordsPerSnapshot) {}

    public record PressureQueueEntry(long sequence, Optional<MediaPressureJob> job,
                                     boolean currentMembership) {
        public PressureQueueEntry {
            if (sequence < 0)
                throw new IllegalArgumentException("pressure queue sequence must not be negative");
            Objects.requireNonNull(job, "job");
        }
    }

    public record CatalogQueueEntry(long sequence, Optional<MediaCatalogJob> job,
                                    boolean currentMembership) {
        public CatalogQueueEntry {
            if (sequence < 0)
                throw new IllegalArgumentException("catalog queue sequence must not be negative");
            Objects.requireNonNull(job, "job");
        }
    }

    @FunctionalInterface
    interface IOCallable<T> {
        T call() throws IOException;
    }
}
