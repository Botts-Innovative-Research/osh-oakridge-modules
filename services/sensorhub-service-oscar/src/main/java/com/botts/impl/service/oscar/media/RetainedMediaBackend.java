package com.botts.impl.service.oscar.media;

import com.botts.api.service.bucket.IBucketStore;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Idempotent retained-media job runner, keyed by observation/lane/camera identity. */
public final class RetainedMediaBackend {
    private final IBucketStore bucketStore;
    private final MediaSetStore mediaStore;
    private final JpegFrameExtractor extractor;
    private final Duration rollbackGrace;
    private final Duration secondaryFrameRetention;
    private final Clock clock;
    private final Logger log;

    public RetainedMediaBackend(IBucketStore bucketStore, MediaSetStore mediaStore, int jpegQuality,
                                Duration rollbackGrace, Logger log) {
        this(bucketStore, mediaStore, new JpegFrameExtractor(jpegQuality), rollbackGrace,
                mediaStore.secondaryFrameRetention().orElse(null), Clock.systemUTC(), log);
    }

    public RetainedMediaBackend(IBucketStore bucketStore, MediaSetStore mediaStore, int jpegQuality,
                                Duration rollbackGrace, Duration secondaryFrameRetention,
                                Logger log) {
        this(bucketStore, mediaStore, new JpegFrameExtractor(jpegQuality), rollbackGrace,
                secondaryFrameRetention, Clock.systemUTC(), log);
    }

    RetainedMediaBackend(IBucketStore bucketStore, MediaSetStore mediaStore,
                         JpegFrameExtractor extractor, Duration rollbackGrace,
                         Clock clock, Logger log) {
        this(bucketStore, mediaStore, extractor, rollbackGrace,
                mediaStore.secondaryFrameRetention().orElse(null), clock, log);
    }

    RetainedMediaBackend(IBucketStore bucketStore, MediaSetStore mediaStore,
                         JpegFrameExtractor extractor, Duration rollbackGrace,
                         Duration secondaryFrameRetention, Clock clock, Logger log) {
        if (rollbackGrace == null || rollbackGrace.isNegative())
            throw new IllegalArgumentException("Rollback grace must not be negative");
        if (secondaryFrameRetention != null && secondaryFrameRetention.isNegative())
            throw new IllegalArgumentException("Secondary frame retention must not be negative");
        this.bucketStore = bucketStore;
        this.mediaStore = mediaStore;
        this.extractor = extractor;
        this.rollbackGrace = rollbackGrace;
        this.secondaryFrameRetention = secondaryFrameRetention;
        this.mediaStore.setSecondaryFrameRetention(secondaryFrameRetention);
        this.clock = clock;
        this.log = log;
    }

    private record AttemptOutcome(boolean succeeded, Optional<MediaSetStore.Identity> delegateToSibling) {
    }

    /**
     * Materializes a five-JPEG set or advances an existing set's source lifecycle.
     * The source is never removed on the creation attempt, regardless of success or failure.
     */
    public boolean process(MediaSetStore.Identity identity, String sourceObjectKey) {
        try {
            AttemptOutcome outcome = mediaStore.withIdentityLock(identity,
                    () -> processAttemptLocked(identity, sourceObjectKey));
            if (outcome.delegateToSibling().isPresent()) {
                MediaSetStore.Identity sibling = outcome.delegateToSibling().get();
                return mediaStore.withIdentityLock(sibling,
                        () -> processAttemptLocked(sibling, sourceObjectKey).succeeded());
            }
            return outcome.succeeded();
        } catch (IOException e) {
            log.error("Unable to acquire retained-media identity transaction for {}",
                    identity.mediaSetId(), e);
            return false;
        }
    }

    private AttemptOutcome processAttemptLocked(MediaSetStore.Identity identity, String sourceObjectKey) {
        try {
            if (mediaStore.isTerminallyDeleted(identity)) {
                mediaStore.finishTerminalDeletion(identity);
                return new AttemptOutcome(true, Optional.empty());
            }
            String normalizedSourceKey = MediaSetStore.normalizedSourceKey(sourceObjectKey);
            Optional<MediaManifest> existing = mediaStore.findManifest(identity);
            if (existing.isPresent()) {
                MediaManifest manifest = mediaStore.finalizeInitialPublication(
                        identity, existing.get(), rollbackGrace, clock);
                if (manifest.lifecycleState() == MediaManifest.LifecycleState.DELETING
                        || manifest.lifecycleState() == MediaManifest.LifecycleState.DELETED)
                    return new AttemptOutcome(true, Optional.empty());
                mediaStore.verifyPublishedMedia(manifest);
                // Repair the narrow crash window after manifest commit but before its lifecycle
                // index entry was durably published.
                mediaStore.ensureManifestMaintenance(identity, manifest);
                String catalogSourceKey = MediaSetStore.stripVideoBucket(manifest.source().objectKey());
                if (!catalogSourceKey.equals(normalizedSourceKey))
                    throw new IOException("Retained-media identity was reused for a different source object");
                repairLedger(identity, manifest);
                advanceSourceLifecycle(identity, manifest, catalogSourceKey);
                return new AttemptOutcome(true, Optional.empty());
            }

            Optional<MediaManifest> sibling = mediaStore.findSiblingManifestForSource(
                    identity, normalizedSourceKey);
            if (sibling.isPresent()) {
                mediaStore.clearUncommittedIdentity(identity);
                MediaManifest siblingManifest = sibling.get();
                return new AttemptOutcome(true, Optional.of(new MediaSetStore.Identity(
                        siblingManifest.eventId(), siblingManifest.laneUid(), siblingManifest.cameraUid())));
            }

            Optional<MediaJobLedger> previousLedger = mediaStore.readLedger(identity);
            if (previousLedger.isPresent()
                    && !MediaSetStore.stripVideoBucket(previousLedger.get().sourceObjectKey())
                    .equals(normalizedSourceKey))
                throw new IOException("Retained-media identity was reused for a different source object");
            MediaJobLedger ledger = previousLedger.orElse(new MediaJobLedger(
                    MediaJobLedger.CURRENT_SCHEMA_VERSION,
                    identity.eventId(), identity.laneUid(), identity.cameraUid(), sourceObjectKey,
                    MediaJobLedger.JobState.DISCOVERED, 0, Instant.now(clock), ""));
            ledger = new MediaJobLedger(ledger.schemaVersion(), ledger.eventId(), ledger.laneUid(),
                    ledger.cameraUid(), sourceObjectKey, ledger.state(), ledger.attempts(),
                    ledger.updatedAt(), ledger.failure()).nextAttempt(Instant.now(clock));
            mediaStore.writeLedger(identity, ledger);
            mediaStore.cleanupUncommittedFrames(identity);

            if (!bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, normalizedSourceKey))
                throw new IOException("Retained-media source is missing: " + sourceObjectKey);

            Path source = Path.of(bucketStore.getResourceURI(
                    MediaSetStore.VIDEO_BUCKET, normalizedSourceKey)).toAbsolutePath().normalize();
            long sourceBytes = Files.size(source);
            String sourceHash = MediaSetStore.sha256(source);
            JpegFrameExtractor.ExtractionResult extraction = extractor.extract(source);
            if (extraction.frames().size() != MediaManifest.RETAINED_FRAME_COUNT)
                throw new IOException("Decoder did not return exactly five retained frames");

            ledger = ledger.transition(MediaJobLedger.JobState.PUBLISHING, "", Instant.now(clock));
            mediaStore.writeLedger(identity, ledger);
            List<MediaManifest.Asset> assets = new ArrayList<>(MediaManifest.RETAINED_FRAME_COUNT);
            for (int index = 0; index < extraction.frames().size(); index++) {
                JpegFrameExtractor.RetainedFrame frame = extraction.frames().get(index);
                MediaSetStore.PublishedAsset published = mediaStore.publishFrame(
                        identity, index + 1, frame.jpeg());
                assets.add(new MediaManifest.Asset(frame.position(), published.objectKey(),
                        "image/jpeg", published.sha256(), published.bytes(), published.width(),
                        published.height(), frame.decodedOrdinal(), frame.sourcePts(), frame.sourcePtsSeconds(),
                        MediaManifest.AssetState.PUBLISHED));
            }

            Instant createdAt = Instant.now(clock);
            MediaManifest manifest = new MediaManifest(
                    MediaManifest.CURRENT_SCHEMA_VERSION,
                    identity.mediaSetId(), identity.eventId(), identity.laneUid(), identity.cameraUid(),
                    createdAt, extractor.jpegQuality(), MediaManifest.LifecycleState.AVAILABLE,
                    MediaManifest.Hold.none(),
                    new MediaManifest.Source(sourceObjectKey, sourceHash, sourceBytes,
                            extraction.sourceCodec(), extraction.durationMicros(),
                            Instant.MAX,
                            MediaManifest.SourceState.PRESERVED_FOR_ROLLBACK),
                    assets);
            manifest = mediaStore.publishInitialManifest(identity, manifest,
                    rollbackGrace, clock);
            mediaStore.writeLedger(identity,
                    ledger.transition(MediaJobLedger.JobState.PUBLISHED, "", Instant.now(clock)));
            log.info("Published retained media set {} for event {}, lane {}, camera {}",
                    identity.mediaSetId(), identity.eventId(), identity.laneUid(), identity.cameraUid());
            return new AttemptOutcome(true, Optional.empty());
        } catch (Exception failure) {
            // Frames without a final manifest are invisible to the API and are cleaned before the
            // next idempotent attempt. A failed lifecycle unlink remains recoverable from its
            // ELIGIBLE_FOR_DELETION manifest even when the source is already visibly absent.
            try {
                if (mediaStore.findManifest(identity).isEmpty())
                    mediaStore.cleanupUncommittedFrames(identity);
                MediaJobLedger failed = mediaStore.readLedger(identity).orElse(new MediaJobLedger(
                        MediaJobLedger.CURRENT_SCHEMA_VERSION, identity.eventId(), identity.laneUid(),
                        identity.cameraUid(), durableSourceKey(sourceObjectKey),
                        MediaJobLedger.JobState.DISCOVERED,
                        0, Instant.now(clock), ""));
                mediaStore.writeLedger(identity, failed.transition(MediaJobLedger.JobState.FAILED,
                        conciseFailure(failure), Instant.now(clock)));
            } catch (Exception ledgerFailure) {
                failure.addSuppressed(ledgerFailure);
            }
            log.error("Retained-media job failed for event {}, lane {}, camera {}; source reference retained for recovery: {}",
                    identity.eventId(), identity.laneUid(), identity.cameraUid(),
                    durableSourceKey(sourceObjectKey), failure);
            return new AttemptOutcome(false, Optional.empty());
        }
    }

    private void advanceSourceLifecycle(MediaSetStore.Identity identity, MediaManifest manifest,
                                        String normalizedSourceKey) throws IOException {
        if (manifest.isHeld() || manifest.lifecycleState() == MediaManifest.LifecycleState.HELD)
            return;
        MediaManifest current = manifest;
        if (current.source().state() != MediaManifest.SourceState.DELETED) {
            if (Instant.now(clock).isBefore(current.source().rollbackDeleteAfter()))
                return;

            if (!bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, normalizedSourceKey)) {
                // Visible absence is not sufficient after a failed unlink barrier. Force the nearest
                // existing parent before committing DELETED and clearing retryable lifecycle state.
                mediaStore.deleteVideoObjectDurably(normalizedSourceKey);
                current = markSourceDeleted(identity, current);
            } else {
                MediaManifest eligible = current.withSource(
                        current.source().withState(MediaManifest.SourceState.ELIGIBLE_FOR_DELETION));
                mediaStore.publishManifest(identity, eligible);
                mediaStore.deleteVideoObjectDurably(normalizedSourceKey);
                current = markSourceDeleted(identity, eligible);
            }
        }
        advanceSecondaryFrameLifecycle(identity, current);
    }

    private MediaManifest markSourceDeleted(MediaSetStore.Identity identity, MediaManifest manifest)
            throws IOException {
        MediaManifest updated = manifest.withSource(
                manifest.source().withState(MediaManifest.SourceState.DELETED));
        mediaStore.publishManifest(identity, updated);
        MediaJobLedger ledger = mediaStore.readLedger(identity).orElse(new MediaJobLedger(
                MediaJobLedger.CURRENT_SCHEMA_VERSION, identity.eventId(), identity.laneUid(),
                identity.cameraUid(), manifest.source().objectKey(), MediaJobLedger.JobState.PUBLISHED,
                1, Instant.now(clock), ""));
        mediaStore.writeLedger(identity,
                ledger.transition(MediaJobLedger.JobState.SOURCE_DELETED, "", Instant.now(clock)));
        return updated;
    }

    private void advanceSecondaryFrameLifecycle(MediaSetStore.Identity identity,
                                                MediaManifest manifest) throws IOException {
        if (manifest.isHeld() || manifest.lifecycleState() == MediaManifest.LifecycleState.HELD)
            return;
        if (manifest.source().state() != MediaManifest.SourceState.DELETED)
            return;
        if (!manifest.hasSecondaryFramesRemaining())
            return;
        boolean resumingEligibleFrames = manifest.hasSecondaryFramesEligibleForDeletion();
        if (secondaryFrameRetention == null && !resumingEligibleFrames)
            return;
        Instant deleteAfter = secondaryFrameRetention != null
                ? manifest.secondaryFramesDeleteAfter(secondaryFrameRetention)
                : manifest.createdAt();
        if (!resumingEligibleFrames && Instant.now(clock).isBefore(deleteAfter))
            return;

        MediaManifest.Asset primaryFrame = manifest.assets().stream()
                .filter(MediaManifest.Asset::isPrimaryFrame)
                .findFirst()
                .orElseThrow(() -> new IOException("Manifest is missing primary MIDDLE frame"));
        mediaStore.verifyPublishedAsset(primaryFrame);

        MediaManifest current = manifest;
        boolean anySecondaryPublished = current.assets().stream()
                .anyMatch(asset -> !asset.isPrimaryFrame()
                        && asset.state() == MediaManifest.AssetState.PUBLISHED);
        if (anySecondaryPublished) {
            List<MediaManifest.Asset> eligibleAssets = new ArrayList<>(current.assets().size());
            for (MediaManifest.Asset asset : current.assets()) {
                if (asset.isPrimaryFrame()
                        || asset.state() == MediaManifest.AssetState.DELETED) {
                    eligibleAssets.add(asset);
                } else {
                    eligibleAssets.add(asset.withState(
                            MediaManifest.AssetState.ELIGIBLE_FOR_DELETION));
                }
            }
            current = current.withAssets(eligibleAssets);
            mediaStore.publishManifest(identity, current);
        }

        for (MediaManifest.Asset asset : current.assets()) {
            if (!asset.isPrimaryFrame())
                mediaStore.deleteVideoObjectDurably(asset.objectKey());
        }

        List<MediaManifest.Asset> prunedAssets = new ArrayList<>(current.assets().size());
        for (MediaManifest.Asset asset : current.assets()) {
            prunedAssets.add(asset.isPrimaryFrame()
                    ? asset
                    : asset.withState(MediaManifest.AssetState.DELETED));
        }
        MediaManifest pruned = current.withAssets(prunedAssets);
        mediaStore.publishManifest(identity, pruned);
    }

    private void repairLedger(MediaSetStore.Identity identity, MediaManifest manifest) throws IOException {
        MediaJobLedger.JobState desired = manifest.source().state() == MediaManifest.SourceState.DELETED
                ? MediaJobLedger.JobState.SOURCE_DELETED : MediaJobLedger.JobState.PUBLISHED;
        Optional<MediaJobLedger> current = mediaStore.readLedger(identity);
        if (current.isPresent() && current.get().state() == desired)
            return;
        MediaJobLedger ledger = current.orElse(new MediaJobLedger(
                MediaJobLedger.CURRENT_SCHEMA_VERSION, identity.eventId(), identity.laneUid(),
                identity.cameraUid(), manifest.source().objectKey(), desired, 1,
                Instant.now(clock), ""));
        mediaStore.writeLedger(identity, current.isEmpty()
                ? ledger : ledger.transition(desired, "", Instant.now(clock)));
    }

    private static String conciseFailure(Throwable failure) {
        String value = failure.getClass().getSimpleName() + ": "
                + (failure.getMessage() == null ? "unspecified failure" : failure.getMessage());
        return value.length() <= 1024 ? value : value.substring(0, 1024);
    }

    private static String durableSourceKey(String sourceObjectKey) {
        if (sourceObjectKey == null || sourceObjectKey.isBlank())
            return "<invalid-source>";
        return sourceObjectKey.length() <= 4_096
                ? sourceObjectKey : sourceObjectKey.substring(0, 4_096);
    }
}
