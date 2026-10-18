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
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
    private static final String MAINTENANCE_PREFIX = "retained-media/v1/maintenance/";
    private static final String PRESSURE_QUEUE_ITEM_PREFIX =
            "retained-media/v1/pressure-queue/items/";
    private static final String PRESSURE_QUEUE_MEMBER_PREFIX =
            "retained-media/v1/pressure-queue/members/";
    private static final String CATALOG_QUEUE_ITEM_PREFIX =
            "retained-media/v1/catalog-queue/items/";
    private static final String CATALOG_QUEUE_MEMBER_PREFIX =
            "retained-media/v1/catalog-queue/members/";
    private static final String PROCESSED_OBSERVATION_PREFIX =
            "retained-media/v1/retention-processed/";
    private static final String MANIFEST_FILE = "manifest.json";
    private static final String LEDGER_FILE = ".job.json";
    private static final String DELETION_FILE = ".deleted.json";
    private static final String PARTIAL_MARKER = ".partial-";
    private static final int MAX_INTERNAL_JSON_BYTES = 1024 * 1024;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (value, type, context) -> context.serialize(value.toString()))
            .registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (value, type, context) -> Instant.parse(value.getAsString()))
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final IBucketStore bucketStore;
    private final Object[] identityLocks = new Object[256];

    public MediaSetStore(IBucketStore bucketStore) throws DataStoreException {
        this.bucketStore = Objects.requireNonNull(bucketStore, "bucketStore");
        for (int index = 0; index < identityLocks.length; index++)
            identityLocks[index] = new Object();
        if (!bucketStore.bucketExists(VIDEO_BUCKET))
            bucketStore.createBucket(VIDEO_BUCKET);
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

    public String deletionMarkerKey(Identity identity) {
        return baseKey(identity) + "/" + DELETION_FILE;
    }

    public String frameKey(Identity identity, int oneBasedIndex) {
        if (oneBasedIndex < 1 || oneBasedIndex > MediaManifest.RETAINED_FRAME_COUNT)
            throw new IllegalArgumentException("Retained frame index must be 1 through 5");
        return baseKey(identity) + String.format("/frame-%02d.jpg", oneBasedIndex);
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

    public boolean isObservationProcessed(String observationId) {
        return bucketStore.objectExists(VIDEO_BUCKET, processedObservationKey(observationId));
    }

    public synchronized void markObservationProcessed(RetentionProcessedObservation marker)
            throws IOException {
        Objects.requireNonNull(marker, "marker");
        atomicWriteJson(processedObservationKey(marker.observationId()), marker,
                RetentionProcessedObservation.class);
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

    public boolean isTerminallyDeleted(Identity identity) {
        return bucketStore.objectExists(VIDEO_BUCKET, deletionMarkerKey(identity));
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
                clearMaintenance(identity);
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
        if (bucketStore.objectExists(VIDEO_BUCKET, manifestKey(identity)))
            return;
        Path setDirectory = writablePath(baseKey(identity) + "/.cleanup-anchor").getParent();
        if (setDirectory == null || !Files.exists(setDirectory))
            return;
        try (var files = Files.list(setDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".jpg") || name.contains(PARTIAL_MARKER))
                    Files.deleteIfExists(file);
            }
        }
    }

    /** Deletes a catalog set as a unit: deleting state first, commit marker last. */
    public long deleteMediaSet(ManifestEntry entry) throws IOException {
        MediaManifest supplied = entry.manifest();
        Identity identity = new Identity(supplied.eventId(), supplied.laneUid(), supplied.cameraUid());
        return withIdentityLock(identity, () -> deleteMediaSetLocked(entry, identity));
    }

    private long deleteMediaSetLocked(ManifestEntry entry, Identity identity) throws IOException {
        if (!bucketStore.objectExists(VIDEO_BUCKET, entry.manifestKey())) {
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

        // The terminal marker is committed before the first destructive delete. It survives the
        // media set and prevents a stale retry, overlapping scan, or restart from reconstructing it.
        if (!isTerminallyDeleted(identity))
            atomicWriteJson(deletionMarkerKey(identity),
                    MediaDeletionMarker.at(identity, Instant.now()), MediaDeletionMarker.class);
        clearMaintenance(identity);

        long deletedBytes = 0;
        try {
            for (MediaManifest.Asset asset : manifest.assets())
                deletedBytes += deleteIfPresent(asset.objectKey());
            if (manifest.source().state() != MediaManifest.SourceState.DELETED)
                deletedBytes += deleteIfPresent(stripVideoBucket(manifest.source().objectKey()));
            deletedBytes += deleteIfPresent(ledgerKey(identity));
            deletedBytes += deleteIfPresent(entry.manifestKey());
            clearMaintenance(identity);
            clearPressureCandidate(identity);
            clearCatalogCandidate(identity);
            return deletedBytes;
        } catch (IOException e) {
            // The DELETING manifest and terminal tombstone remain durable recovery markers. A
            // future cleanup pass can retry every idempotent delete without media recreation.
            throw e;
        }
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
                + String.format("%02x", shard) + "/";
    }

    private static String processedObservationKey(String observationId) {
        if (observationId == null || observationId.isBlank())
            throw new IllegalArgumentException("observationId is required");
        String digest = sha256(observationId.getBytes(StandardCharsets.UTF_8));
        return PROCESSED_OBSERVATION_PREFIX + digest.substring(0, 2) + "/" + digest + ".json";
    }

    private static Instant retryDueAt(MediaJobLedger ledger) {
        int exponent = Math.min(Math.max(ledger.attempts() - 1, 0), 8);
        long delayMinutes = Math.min(24L * 60L, 5L << exponent);
        return ledger.updatedAt().plusSeconds(delayMinutes * 60L);
    }

    private static String pressureItemKey(long sequence) {
        if (sequence < 0)
            throw new IllegalArgumentException("pressure queue sequence must not be negative");
        return PRESSURE_QUEUE_ITEM_PREFIX + String.format("%019d.json", sequence);
    }

    private static String pressureMemberKey(Identity identity) {
        String mediaSetId = identity.mediaSetId();
        return PRESSURE_QUEUE_MEMBER_PREFIX + mediaSetId.substring(0, 2) + "/"
                + mediaSetId + ".json";
    }

    private static String catalogItemKey(long sequence) {
        if (sequence < 0)
            throw new IllegalArgumentException("catalog queue sequence must not be negative");
        return CATALOG_QUEUE_ITEM_PREFIX + String.format("%019d.json", sequence);
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
        try {
            if (!bucketStore.objectExists(VIDEO_BUCKET, key))
                return 0;
            long bytes = bucketStore.getObjectSize(VIDEO_BUCKET, key);
            bucketStore.deleteObject(VIDEO_BUCKET, key);
            return bytes;
        } catch (DataStoreException e) {
            throw new IOException("Unable to delete retained media object " + key, e);
        }
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
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes)
            result.append(String.format("%02x", value));
        return result.toString();
    }

    public record ManifestEntry(String manifestKey, MediaManifest manifest) {}

    public record PublishedAsset(String objectKey, String sha256, long bytes, int width, int height) {}

    public record MaintenanceEntry(String key, MediaMaintenanceJob job) {}

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
