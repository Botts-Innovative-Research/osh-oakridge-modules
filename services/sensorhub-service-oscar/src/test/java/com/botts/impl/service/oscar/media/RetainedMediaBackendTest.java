package com.botts.impl.service.oscar.media;

import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import org.sensorhub.api.datastore.DataStoreException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.CRC32C;

import static org.junit.Assert.*;

public class RetainedMediaBackendTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void directoryFsyncFallbackIsLimitedToWindowsAccessDenied() {
        AccessDeniedException accessDenied = new AccessDeniedException("catalog-directory");
        assertTrue(MediaSetStore.isIgnorableWindowsDirectoryFsyncFailure(accessDenied, true));
        assertFalse(MediaSetStore.isIgnorableWindowsDirectoryFsyncFailure(accessDenied, false));
        assertFalse(MediaSetStore.isIgnorableWindowsDirectoryFsyncFailure(
                new java.io.IOException("disk failure"), true));
    }

    @Test
    public void extractorProducesExactFiveDistinctBaselineSrgbJpegs() throws Exception {
        JpegFrameExtractor.ExtractionResult extraction = new JpegFrameExtractor(85)
                .extract(bundledH264Fixture());

        assertEquals(5, extraction.frames().size());
        assertFalse(extraction.sourceCodec().isBlank());
        assertTrue(extraction.durationMicros() > 0);
        assertEquals(List.of(
                        MediaManifest.FramePosition.FIRST,
                        MediaManifest.FramePosition.QUARTER,
                        MediaManifest.FramePosition.MIDDLE,
                        MediaManifest.FramePosition.THREE_QUARTERS,
                        MediaManifest.FramePosition.LAST),
                extraction.frames().stream().map(JpegFrameExtractor.RetainedFrame::position).toList());
        assertEquals(5, extraction.frames().stream()
                .map(JpegFrameExtractor.RetainedFrame::decodedOrdinal).distinct().count());

        long previousOrdinal = -1;
        for (int index = 0; index < extraction.frames().size(); index++) {
            JpegFrameExtractor.RetainedFrame frame = extraction.frames().get(index);
            assertTrue(frame.decodedOrdinal() > previousOrdinal);
            previousOrdinal = frame.decodedOrdinal();
            Path jpeg = temporaryFolder.newFile("frame-" + index + ".jpg").toPath();
            Files.write(jpeg, frame.jpeg());
            JpegFrameExtractor.JpegInfo info = JpegFrameExtractor.verifyBaselineSrgbJpeg(jpeg);
            assertEquals(frame.width(), info.width());
            assertEquals(frame.height(), info.height());
        }
    }

    @Test
    public void fewerThanFiveDecodedFramesCannotSelectFivePositions() {
        List<JpegFrameExtractor.FramePoint> shortTimeline = List.of(
                new JpegFrameExtractor.FramePoint(0, 0, 0),
                new JpegFrameExtractor.FramePoint(1, 1, 1),
                new JpegFrameExtractor.FramePoint(2, 2, 2),
                new JpegFrameExtractor.FramePoint(3, 3, 3));
        try {
            JpegFrameExtractor.selectDistinctOrdinals(shortTimeline);
            fail("Four decoded frames must not produce a retained-media set");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("five"));
        }
    }

    @Test
    public void retainedJpegLimitsMatchViewerAndUseOverflowSafePixelMath() throws Exception {
        JpegFrameExtractor.validateDimensions(6_000, 3_333);
        assertThrows(IOException.class,
                () -> JpegFrameExtractor.validateDimensions(6_001, 1));
        assertThrows(IOException.class,
                () -> JpegFrameExtractor.validateDimensions(6_000, 3_334));
        assertThrows(IOException.class,
                () -> JpegFrameExtractor.validateDimensions(Integer.MAX_VALUE, Integer.MAX_VALUE));

        String digest = "0".repeat(64);
        new MediaManifest.Asset(MediaManifest.FramePosition.FIRST, "frame.jpg", "image/jpeg",
                digest, MediaManifest.MAX_JPEG_BYTES, 6_000, 3_333, 0, 0, 0,
                MediaManifest.AssetState.PUBLISHED);
        assertThrows(IllegalArgumentException.class, () -> new MediaManifest.Asset(
                MediaManifest.FramePosition.FIRST, "frame.jpg", "image/jpeg", digest,
                MediaManifest.MAX_JPEG_BYTES + 1, 1, 1, 0, 0, 0,
                MediaManifest.AssetState.PUBLISHED));
        assertThrows(IllegalArgumentException.class, () -> new MediaManifest.Asset(
                MediaManifest.FramePosition.FIRST, "frame.jpg", "image/jpeg", digest,
                1, 6_000, 3_334, 0, 0, 0, MediaManifest.AssetState.PUBLISHED));
    }

    @Test
    public void oversizedJpegIsRejectedBeforeReadOrPublication() throws Exception {
        Path oversized = temporaryFolder.newFile("oversized.jpg").toPath();
        try (FileChannel channel = FileChannel.open(oversized, StandardOpenOption.WRITE)) {
            channel.position(MediaManifest.MAX_JPEG_BYTES);
            channel.write(ByteBuffer.wrap(new byte[]{0}));
        }
        IOException verificationFailure = assertThrows(IOException.class,
                () -> JpegFrameExtractor.verifyBaselineSrgbJpeg(oversized));
        assertTrue(verificationFailure.getMessage().contains("encoded-size"));

        Path root = temporaryFolder.newFolder("oversized-publication").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        bucketStore.createBucket(MediaSetStore.VIDEO_BUCKET);
        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "oversized-event", "lane", "camera");
        byte[] bytes = new byte[(int) MediaManifest.MAX_JPEG_BYTES + 1];
        assertThrows(IOException.class, () -> store.publishFrame(identity, 1, bytes));
        assertFalse(bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET,
                store.frameKey(identity, 1)));
    }

    @Test
    public void jpegVerifierDoesNotFollowSymbolicLinks() throws Exception {
        Path target = temporaryFolder.newFile("jpeg-target.jpg").toPath();
        Files.write(target, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9});
        Path link = target.resolveSibling("jpeg-link.jpg");
        Files.createSymbolicLink(link, target);

        IOException failure = assertThrows(IOException.class,
                () -> JpegFrameExtractor.verifyBaselineSrgbJpeg(link));
        assertTrue(failure.getMessage().contains("regular file"));
    }

    @Test
    public void manifestMustBindIdentityAssetPathsAndExternalSource() throws Exception {
        Path root = temporaryFolder.newFolder("manifest-confinement").toPath();
        MediaSetStore store = new MediaSetStore(new FileSystemBucketStore(root));
        MediaSetStore.Identity expected = new MediaSetStore.Identity("event", "lane", "camera");
        MediaSetStore.Identity other = new MediaSetStore.Identity("other", "lane", "camera");

        IOException crossSet = assertThrows(IOException.class, () -> store.publishManifest(expected,
                dummyManifest(expected, other, "clips/camera/source.mp4")));
        assertTrue(crossSet.getMessage().contains("retained-media set path"));

        IOException internalSource = assertThrows(IOException.class,
                () -> store.publishManifest(expected, dummyManifest(expected, expected,
                        store.frameKey(expected, 1))));
        assertTrue(internalSource.getMessage().contains("not a published video"));

        IOException traversalSource = assertThrows(IOException.class,
                () -> store.publishManifest(expected, dummyManifest(expected, expected,
                        "clips/camera/../../outside.mp4")));
        assertTrue(traversalSource.getMessage().contains("not confined"));

        IOException oversizedSource = assertThrows(IOException.class,
                () -> store.publishManifest(expected, dummyManifest(expected, expected,
                        "x".repeat(4_097))));
        assertTrue(oversizedSource.getMessage().contains("safety limit"));
    }

    @Test
    public void oversizedManifestIsRejectedByBoundedReader() throws Exception {
        Path root = temporaryFolder.newFolder("oversized-manifest").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity("event", "lane", "camera");
        Path manifest = Path.of(bucketStore.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET,
                store.manifestKey(identity)));
        try (FileChannel channel = FileChannel.open(manifest, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE_NEW)) {
            channel.position(1024L * 1024L);
            channel.write(ByteBuffer.wrap(new byte[]{'}'}));
        }

        IOException failure = assertThrows(IOException.class,
                () -> store.readManifest(store.manifestKey(identity)));
        assertTrue(failure.getMessage().contains("exceeds"));
    }

    @Test
    public void backendPublishesFramesThenManifestAndPreservesSourceForGrace() throws Exception {
        Path root = temporaryFolder.newFolder("media-store").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        bucketStore.createBucket(MediaSetStore.VIDEO_BUCKET);
        String sourceKey = "clips/urn-osh-sensor-ffmpeg-lane-Lane1-0/source.ts";
        Path source = Path.of(bucketStore.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET, sourceKey));
        Files.copy(bundledH264Fixture(), source);

        MediaSetStore store = new MediaSetStore(bucketStore);
        RetainedMediaBackend backend = new RetainedMediaBackend(bucketStore, store, 85,
                Duration.ofHours(24), LoggerFactory.getLogger(getClass()));
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "event-public-id", "urn:osh:system:lane:Lane1",
                "urn:osh:sensor:ffmpeg:lane:Lane1:0");

        assertTrue(backend.process(identity, "videos/" + sourceKey));
        assertTrue("Source must survive the rollback grace",
                bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, sourceKey));
        MediaManifest manifest = store.findManifest(identity).orElseThrow();
        assertEquals(MediaManifest.CURRENT_SCHEMA_VERSION, manifest.schemaVersion());
        assertEquals(MediaManifest.LifecycleState.AVAILABLE, manifest.lifecycleState());
        assertEquals(MediaManifest.SourceState.PRESERVED_FOR_ROLLBACK, manifest.source().state());
        assertEquals(5, manifest.assets().size());
        assertEquals(5, manifest.assets().stream()
                .map(MediaManifest.Asset::sourceFrameOrdinal).distinct().count());
        for (MediaManifest.Asset asset : manifest.assets()) {
            assertTrue(bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, asset.objectKey()));
            assertEquals(asset.bytes(), bucketStore.getObjectSize(MediaSetStore.VIDEO_BUCKET, asset.objectKey()));
        }
        assertEquals(MediaJobLedger.JobState.PUBLISHED, store.readLedger(identity).orElseThrow().state());
        assertFalse("Successful publication must clear the retry index",
                store.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));
        assertTrue("Published source must have a durable lifecycle work item",
                store.maintenanceExists(MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, identity));

        // A restart/retry repairs the narrow crash window between manifest commit and lifecycle
        // index publication without decoding the source again. The simulated PUBLISHING ledger
        // keeps the durable retry alive until the lifecycle entry has been repaired.
        store.removeMaintenance(MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, identity);
        MediaJobLedger publishing = store.readLedger(identity).orElseThrow().transition(
                MediaJobLedger.JobState.PUBLISHING, "", manifest.createdAt().plusSeconds(1));
        store.writeLedger(identity, publishing);
        assertFalse(store.maintenanceExists(MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, identity));
        assertTrue(store.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));
        store.reconcileCommittedManifest(new MediaSetStore.ManifestEntry(
                store.manifestKey(identity), manifest), manifest.createdAt().plusSeconds(2));
        assertTrue(store.maintenanceExists(MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, identity));
        assertFalse("Retry may be removed only after lifecycle recovery is durable",
                store.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));

        // A repeated sliding query is an idempotent lookup, not a second transcode.
        assertTrue(backend.process(identity, "videos/" + sourceKey));
        assertEquals(5, store.findManifest(identity).orElseThrow().assets().size());

        // Only a later durable retry, after the catalogued grace deadline, may remove the source.
        Clock afterGrace = Clock.fixed(manifest.source().rollbackDeleteAfter().plusSeconds(1),
                ZoneOffset.UTC);
        RetainedMediaBackend lifecycleBackend = new RetainedMediaBackend(bucketStore, store,
                new JpegFrameExtractor(85), Duration.ofHours(24), afterGrace,
                LoggerFactory.getLogger(getClass()));
        assertTrue(lifecycleBackend.process(identity, "videos/" + sourceKey));
        assertFalse(bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, sourceKey));
        assertEquals(MediaManifest.SourceState.DELETED,
                store.findManifest(identity).orElseThrow().source().state());
        assertEquals(MediaJobLedger.JobState.SOURCE_DELETED,
                store.readLedger(identity).orElseThrow().state());
        assertFalse("Deleted source must leave no lifecycle work item",
                store.maintenanceExists(MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, identity));

        // Emergency deletion commits a durable tombstone before removing the manifest. Stale
        // maintenance entries that survive a crash cannot reconstruct the media on restart.
        MediaManifest beforeEmergencyDelete = store.findManifest(identity).orElseThrow();
        MediaSetStore.ManifestEntry entry = new MediaSetStore.ManifestEntry(
                store.manifestKey(identity), beforeEmergencyDelete);
        assertTrue(store.deleteMediaSet(entry) > 0);
        assertTrue(store.isTerminallyDeleted(identity));
        assertFalse("New deletion state must not leave one permanent JSON file per media set",
                bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET,
                        store.baseKey(identity) + "/.deleted.json"));
        String deletionIndexKey = MediaSetStore.deletionIndexKey(identity);
        assertTrue(bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, deletionIndexKey));
        assertTrue(store.findManifest(identity).isEmpty());
        assertFalse(store.readLedger(identity).isPresent());

        Instant staleDueAt = manifest.createdAt().plusSeconds(2);
        store.enqueueMaintenance(new MediaMaintenanceJob(
                MediaMaintenanceJob.CURRENT_SCHEMA_VERSION, MediaMaintenanceJob.Kind.RETRY,
                identity.eventId(), identity.laneUid(), identity.cameraUid(),
                "videos/" + sourceKey, staleDueAt, 2));
        store.enqueueMaintenance(new MediaMaintenanceJob(
                MediaMaintenanceJob.CURRENT_SCHEMA_VERSION,
                MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE,
                identity.eventId(), identity.laneUid(), identity.cameraUid(),
                "videos/" + sourceKey, staleDueAt, 0));

        // Simulate a crash during the next append. Restart recovery truncates only the incomplete
        // tail and must retain the already-forced exact tombstone.
        Path deletionIndex = Path.of(bucketStore.getResourceURI(
                MediaSetStore.VIDEO_BUCKET, deletionIndexKey));
        long committedIndexBytes = Files.size(deletionIndex);
        Files.write(deletionIndex, new byte[]{1, 2, 3, 4, 5, 6, 7},
                StandardOpenOption.APPEND);
        MediaSetStore restartedStore = new MediaSetStore(bucketStore);
        assertTrue(restartedStore.isTerminallyDeleted(identity));
        assertEquals(committedIndexBytes, Files.size(deletionIndex));
        RetainedMediaBackend restartedBackend = new RetainedMediaBackend(bucketStore,
                restartedStore, 85, Duration.ZERO, LoggerFactory.getLogger(getClass()));
        assertTrue(restartedBackend.process(identity, "videos/" + sourceKey));
        assertTrue(restartedStore.findManifest(identity).isEmpty());
        assertFalse(restartedStore.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));
        assertFalse(restartedStore.maintenanceExists(
                MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, identity));
    }

    @Test
    public void sourceDeletionCommitsOnlyAfterDurableUnlinkAndRecoversAfterRestart()
            throws Exception {
        Path root = temporaryFolder.newFolder("durable-source-delete").toPath();
        String sourceKey = "clips/durable-source/source.ts";
        DeleteParentAfterObjectStore bucketStore =
                new DeleteParentAfterObjectStore(root, sourceKey);
        bucketStore.createBucket(MediaSetStore.VIDEO_BUCKET);
        Path source = Path.of(bucketStore.getWritableResourceURI(
                MediaSetStore.VIDEO_BUCKET, sourceKey));
        Files.copy(bundledH264Fixture(), source);

        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "durable-source-event", "lane", "camera");
        RetainedMediaBackend publisher = new RetainedMediaBackend(bucketStore, store, 85,
                Duration.ofHours(1), LoggerFactory.getLogger(getClass()));
        assertTrue(publisher.process(identity, "videos/" + sourceKey));

        MediaManifest published = store.findManifest(identity).orElseThrow();
        Clock afterGrace = Clock.fixed(published.source().rollbackDeleteAfter().plusSeconds(1),
                ZoneOffset.UTC);
        RetainedMediaBackend lifecycle = new RetainedMediaBackend(bucketStore, store,
                new JpegFrameExtractor(85), Duration.ofHours(1), afterGrace,
                LoggerFactory.getLogger(getClass()));
        assertFalse("A failed directory durability barrier must not commit DELETED",
                lifecycle.process(identity, "videos/" + sourceKey));
        assertFalse(bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, sourceKey));
        assertEquals(MediaManifest.SourceState.ELIGIBLE_FOR_DELETION,
                store.findManifest(identity).orElseThrow().source().state());
        assertTrue("The interrupted lifecycle must remain durably retryable",
                store.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));

        FileSystemBucketStore restartedBucketStore = new FileSystemBucketStore(root);
        MediaSetStore restartedStore = new MediaSetStore(restartedBucketStore);
        RetainedMediaBackend restarted = new RetainedMediaBackend(restartedBucketStore,
                restartedStore, new JpegFrameExtractor(85), Duration.ofHours(1), afterGrace,
                LoggerFactory.getLogger(getClass()));
        assertTrue(Files.isRegularFile(source.getParent(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertFalse("A restart must not commit a merely visible source absence",
                restarted.process(identity, "videos/" + sourceKey));
        assertEquals(MediaManifest.SourceState.ELIGIBLE_FOR_DELETION,
                restartedStore.findManifest(identity).orElseThrow().source().state());
        assertTrue(restartedStore.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));

        Files.delete(source.getParent());
        assertTrue(restarted.process(identity, "videos/" + sourceKey));
        assertEquals(MediaManifest.SourceState.DELETED,
                restartedStore.findManifest(identity).orElseThrow().source().state());
        assertEquals(MediaJobLedger.JobState.SOURCE_DELETED,
                restartedStore.readLedger(identity).orElseThrow().state());
        assertFalse(restartedStore.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));
    }

    @Test
    public void terminalDeletionKeepsRecoveryWorkUntilUnlinksAreDurable() throws Exception {
        Path root = temporaryFolder.newFolder("durable-terminal-delete").toPath();
        String sourceKey = "clips/durable-terminal/source.ts";
        DeleteParentAfterObjectStore bucketStore =
                new DeleteParentAfterObjectStore(root, sourceKey);
        bucketStore.createBucket(MediaSetStore.VIDEO_BUCKET);
        Path source = Path.of(bucketStore.getWritableResourceURI(
                MediaSetStore.VIDEO_BUCKET, sourceKey));
        Files.copy(bundledH264Fixture(), source);

        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "durable-terminal-event", "lane", "camera");
        RetainedMediaBackend publisher = new RetainedMediaBackend(bucketStore, store, 85,
                Duration.ZERO, LoggerFactory.getLogger(getClass()));
        assertTrue(publisher.process(identity, "videos/" + sourceKey));
        MediaManifest manifest = store.findManifest(identity).orElseThrow();
        MediaSetStore.ManifestEntry entry = new MediaSetStore.ManifestEntry(
                store.manifestKey(identity), manifest);

        assertThrows("An irregular containing path must fail the durability barrier",
                IOException.class, () -> store.deleteMediaSet(entry));
        assertTrue(store.isTerminallyDeleted(identity));
        assertTrue("The manifest must remain as a restart recovery marker",
                store.findManifest(identity).isPresent());
        assertFalse(bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, sourceKey));
        assertTrue(store.inspectPressureHead().orElseThrow().currentMembership());
        assertTrue(store.inspectCatalogHead().orElseThrow().currentMembership());

        FileSystemBucketStore restartedBucketStore = new FileSystemBucketStore(root);
        MediaSetStore restartedStore = new MediaSetStore(restartedBucketStore);
        RetainedMediaBackend restarted = new RetainedMediaBackend(restartedBucketStore,
                restartedStore, 85, Duration.ZERO, LoggerFactory.getLogger(getClass()));
        assertFalse("A restart must retain work until an absent source has a durable parent",
                restarted.process(identity, "videos/" + sourceKey));
        assertTrue(restartedStore.findManifest(identity).isPresent());
        assertTrue(restartedStore.inspectPressureHead().orElseThrow().currentMembership());
        assertTrue(restartedStore.inspectCatalogHead().orElseThrow().currentMembership());

        Files.delete(source.getParent());
        assertTrue("A terminal tombstone must resume deletion of a reappeared manifest",
                restarted.process(identity, "videos/" + sourceKey));
        assertTrue(restartedStore.findManifest(identity).isEmpty());
        assertTrue(restartedStore.isTerminallyDeleted(identity));
        assertFalse(restartedStore.inspectPressureHead().orElseThrow().currentMembership());
        assertFalse(restartedStore.inspectCatalogHead().orElseThrow().currentMembership());
    }

    @Test
    public void absentTerminalManifestMustHaveDurableParentBeforeQueuesAreCleared()
            throws Exception {
        Path root = temporaryFolder.newFolder("durable-absent-manifest").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        bucketStore.createBucket(MediaSetStore.VIDEO_BUCKET);
        String sourceKey = "clips/durable-absent-manifest/source.ts";
        Path source = Path.of(bucketStore.getWritableResourceURI(
                MediaSetStore.VIDEO_BUCKET, sourceKey));
        Files.copy(bundledH264Fixture(), source);

        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "durable-absent-manifest-event", "lane", "camera");
        RetainedMediaBackend publisher = new RetainedMediaBackend(bucketStore, store, 85,
                Duration.ZERO, LoggerFactory.getLogger(getClass()));
        assertTrue(publisher.process(identity, "videos/" + sourceKey));
        MediaManifest manifest = store.findManifest(identity).orElseThrow();
        store.recordTerminalDeletion(identity);

        Path manifestPath = Path.of(bucketStore.getResourceURI(
                MediaSetStore.VIDEO_BUCKET, store.manifestKey(identity)));
        Path setDirectory = manifestPath.getParent();
        for (MediaManifest.Asset asset : manifest.assets())
            Files.delete(Path.of(bucketStore.getResourceURI(
                    MediaSetStore.VIDEO_BUCKET, asset.objectKey())));
        Files.delete(source);
        Files.delete(Path.of(bucketStore.getResourceURI(
                MediaSetStore.VIDEO_BUCKET, store.ledgerKey(identity))));
        Files.delete(manifestPath);
        Files.delete(setDirectory);
        Files.write(setDirectory, new byte[]{1}, StandardOpenOption.CREATE_NEW);

        assertThrows(IOException.class, () -> store.finishTerminalDeletion(identity));
        assertTrue(store.inspectPressureHead().orElseThrow().currentMembership());
        assertTrue(store.inspectCatalogHead().orElseThrow().currentMembership());

        Files.delete(setDirectory);
        assertEquals(0, store.finishTerminalDeletion(identity));
        assertFalse(store.inspectPressureHead().orElseThrow().currentMembership());
        assertFalse(store.inspectCatalogHead().orElseThrow().currentMembership());
    }

    @Test
    public void orphanFrameAndPartialCleanupRequireDurableDirectoryUnlinks() throws Exception {
        assertOrphanCleanupRequiresDurability("frame-01.jpg", "jpeg");
        assertOrphanCleanupRequiresDurability("frame-01.jpg.partial-crash", "partial");
    }

    @Test
    public void multipleLiveStoresSerializeAppendsAndRefreshStampedCaches() throws Exception {
        Path root = temporaryFolder.newFolder("concurrent-deletion-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore firstStore = new MediaSetStore(bucketStore);
        MediaSetStore secondStore = new MediaSetStore(bucketStore);
        List<MediaSetStore.Identity> identities = identitiesInOneDeletionIndex(5);
        MediaSetStore.Identity seed = identities.get(0);
        MediaSetStore.Identity sequential = identities.get(1);
        MediaSetStore.Identity left = identities.get(2);
        MediaSetStore.Identity right = identities.get(3);
        MediaSetStore.Identity finalExternal = identities.get(4);
        firstStore.recordTerminalDeletion(seed);
        firstStore.recordTerminalDeletion(sequential);
        assertTrue("Sequential appends must update the same cached index in place",
                firstStore.isTerminallyDeleted(sequential));
        assertFalse("Prime a positive index cache without the later digest",
                firstStore.isTerminallyDeleted(left));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> firstAppend = executor.submit(() -> {
                ready.countDown();
                start.await();
                firstStore.recordTerminalDeletion(left);
                return null;
            });
            Future<?> secondAppend = executor.submit(() -> {
                ready.countDown();
                start.await();
                secondStore.recordTerminalDeletion(right);
                return null;
            });
            ready.await();
            start.countDown();
            firstAppend.get();
            secondAppend.get();
        } finally {
            executor.shutdownNow();
        }

        assertTrue("The first live store must refresh after the second store appends",
                firstStore.isTerminallyDeleted(right));
        secondStore.recordTerminalDeletion(finalExternal);
        assertTrue(firstStore.isTerminallyDeleted(finalExternal));
        MediaSetStore restarted = new MediaSetStore(bucketStore);
        assertTrue(restarted.isTerminallyDeleted(seed));
        assertTrue(restarted.isTerminallyDeleted(sequential));
        assertTrue(restarted.isTerminallyDeleted(left));
        assertTrue(restarted.isTerminallyDeleted(right));
        assertTrue(restarted.isTerminallyDeleted(finalExternal));
        Path index = Path.of(bucketStore.getResourceURI(MediaSetStore.VIDEO_BUCKET,
                MediaSetStore.deletionIndexKey(seed)));
        assertEquals("Concurrent writers must preserve five exact records without interleaving",
                16L + 5L * 36L, Files.size(index));
    }

    @Test
    public void completeFinalDeletionIndexRecordWithBadChecksumFailsClosed() throws Exception {
        Path root = temporaryFolder.newFolder("corrupt-deletion-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "checksum-deleted-event", "lane", "camera");
        store.recordTerminalDeletion(identity);

        Path index = Path.of(bucketStore.getResourceURI(MediaSetStore.VIDEO_BUCKET,
                MediaSetStore.deletionIndexKey(identity)));
        byte[] committed = Files.readAllBytes(index);
        committed[committed.length - 1] ^= 1;
        Files.write(index, committed, StandardOpenOption.TRUNCATE_EXISTING);
        long corruptSize = Files.size(index);

        IOException failure = assertThrows(IOException.class,
                () -> new MediaSetStore(bucketStore).isTerminallyDeleted(identity));
        assertTrue(failure.getMessage().contains("checksum"));
        assertEquals("A complete corrupt record must never be truncated into a false negative",
                corruptSize, Files.size(index));
    }

    @Test
    public void oversizedFiveMinuteIndexIsNotRetainedInBoundedHeapCache() throws Exception {
        Path root = temporaryFolder.newFolder("bounded-exact-index-cache").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant hour = Instant.parse("2026-01-01T12:00:00Z");
        String key = MediaSetStore.processedObservationIndexKey(hour);
        Path index = Path.of(bucketStore.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET, key));
        int records = store.exactIndexCacheStats().maximumRecordsPerSnapshot() + 1;
        writeProcessedIndex(index, records, 0);

        assertFalse(store.isObservationProcessed("not-present", hour));
        MediaSetStore.ExactIndexCacheStats stats = store.exactIndexCacheStats();
        assertEquals("A snapshot over the per-file cap must not be retained", 0, stats.entries());
        assertEquals(0, stats.records());
        assertTrue(stats.records() <= stats.maximumRecords());
    }

    @Test
    public void processedIndexPathsAreLocaleStableAndRemoveTheHundredThousandPerHourCliff()
            throws Exception {
        Instant hour = Instant.parse("2026-01-01T12:00:00Z");
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"));
            Set<String> keys = new java.util.HashSet<>();
            for (int fiveMinuteFile = 0; fiveMinuteFile < 12; fiveMinuteFile++) {
                String key = MediaSetStore.processedObservationIndexKey(
                        hour.plusSeconds(fiveMinuteFile * 5L * 60L));
                assertTrue("Index paths must use locale-independent ASCII digits: " + key,
                        key.matches("retained-media/v1/retention-processed-index/"
                                + "[0-9]{19}-[0-9]{2}\\.idx"));
                keys.add(key);
            }
            assertEquals("One hour must be divided into twelve bounded five-minute files",
                    12, keys.size());
            assertEquals(MediaSetStore.processedObservationIndexKey(hour),
                    MediaSetStore.processedObservationIndexKey(hour.plusSeconds(299)));
            assertNotEquals(MediaSetStore.processedObservationIndexKey(hour),
                    MediaSetStore.processedObservationIndexKey(hour.plusSeconds(300)));
        } finally {
            Locale.setDefault(original);
        }

        Path root = temporaryFolder.newFolder("processed-index-capacity").toPath();
        MediaSetStore store = new MediaSetStore(new FileSystemBucketStore(root));
        long cacheableRecordsPerHour = 12L
                * store.exactIndexCacheStats().maximumRecordsPerSnapshot();
        assertEquals("Five-minute partitioning moves the streamed-file threshold to 1.2M/h",
                1_200_000L, cacheableRecordsPerHour);
        assertTrue("At 50 lanes this is over 6.6 completed occupancies/second/lane",
                cacheableRecordsPerHour / 3_600.0 / 50.0 > 6.6);
    }

    @Test
    public void moreThanHundredThousandRecordsInOneHourRemainCacheableWhenPartitioned()
            throws Exception {
        Path root = temporaryFolder.newFolder("partitioned-high-rate-hour").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant hour = Instant.parse("2026-01-01T12:00:00Z");
        int recordsPerFile = 8_334;

        for (int file = 0; file < 12; file++) {
            Instant resultTime = hour.plusSeconds(file * 5L * 60L);
            String key = MediaSetStore.processedObservationIndexKey(resultTime);
            Path index = Path.of(bucketStore.getWritableResourceURI(
                    MediaSetStore.VIDEO_BUCKET, key));
            writeProcessedIndex(index, recordsPerFile, file * recordsPerFile);
            assertFalse(store.isObservationProcessed("not-present-" + file, resultTime));
        }

        MediaSetStore.ExactIndexCacheStats stats = store.exactIndexCacheStats();
        assertEquals(12, stats.entries());
        assertEquals("100,008 records/hour must stay on the bounded O(1) cached path",
                100_008, stats.records());
        assertTrue(stats.records() <= stats.maximumRecords());
    }

    @Test
    public void irregularDeletionIndexDirectoryFailsClosed() throws Exception {
        Path root = temporaryFolder.newFolder("directory-deletion-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "directory-deleted-event", "lane", "camera");
        Path index = Path.of(bucketStore.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET,
                MediaSetStore.deletionIndexKey(identity)));
        Files.createDirectory(index);

        IOException failure = assertThrows(IOException.class,
                () -> store.isTerminallyDeleted(identity));
        assertTrue(failure.getMessage().contains("not a regular file"));
    }

    @Test
    public void irregularProcessedIndexDirectoryFailsClosed() throws Exception {
        Path root = temporaryFolder.newFolder("directory-processed-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant resultTime = Instant.parse("2026-01-01T12:01:00Z");
        Path index = Path.of(bucketStore.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET,
                MediaSetStore.processedObservationIndexKey(resultTime)));
        Files.createDirectory(index);

        IOException failure = assertThrows(IOException.class,
                () -> store.isObservationProcessed("directory-processed", resultTime));
        assertTrue(failure.getMessage().contains("not a regular file"));
    }

    @Test
    public void irregularDeletionIndexSymlinkFailsClosed() throws Exception {
        Path root = temporaryFolder.newFolder("symlink-deletion-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "symlink-deleted-event", "lane", "camera");
        Path index = Path.of(bucketStore.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET,
                MediaSetStore.deletionIndexKey(identity)));
        Path target = temporaryFolder.newFile("index-target").toPath();
        try {
            Files.createSymbolicLink(index, target);
        } catch (UnsupportedOperationException | IOException e) {
            org.junit.Assume.assumeNoException("Symbolic links are unavailable", e);
        }

        IOException failure = assertThrows(IOException.class,
                () -> store.isTerminallyDeleted(identity));
        assertTrue(failure.getMessage().contains("Unable to prepare retained media object"));
    }

    @Test
    public void irregularProcessedIndexSymlinkFailsClosed() throws Exception {
        Path root = temporaryFolder.newFolder("symlink-processed-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant resultTime = Instant.parse("2026-01-01T12:02:00Z");
        Path index = Path.of(bucketStore.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET,
                MediaSetStore.processedObservationIndexKey(resultTime)));
        Path target = temporaryFolder.newFile("processed-index-target").toPath();
        try {
            Files.createSymbolicLink(index, target);
        } catch (UnsupportedOperationException | IOException e) {
            org.junit.Assume.assumeNoException("Symbolic links are unavailable", e);
        }

        assertThrows(IOException.class,
                () -> store.isObservationProcessed("symlink-processed", resultTime));
    }

    @Test
    public void irregularDeletionIndexLockDirectoryFailsClosed() throws Exception {
        Path root = temporaryFolder.newFolder("directory-deletion-lock").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "directory-lock-event", "lane", "camera");
        Path lock = deletionIndexLockPath(bucketStore, identity);
        Files.createDirectory(lock);

        assertThrows(IOException.class, () -> store.recordTerminalDeletion(identity));
    }

    @Test
    public void irregularDeletionIndexLockSymlinkFailsClosed() throws Exception {
        Path root = temporaryFolder.newFolder("symlink-deletion-lock").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "symlink-lock-event", "lane", "camera");
        Path lock = deletionIndexLockPath(bucketStore, identity);
        Path target = temporaryFolder.newFile("lock-target").toPath();
        try {
            Files.createSymbolicLink(lock, target);
        } catch (UnsupportedOperationException | IOException e) {
            org.junit.Assume.assumeNoException("Symbolic links are unavailable", e);
        }

        assertThrows(IOException.class, () -> store.recordTerminalDeletion(identity));
    }

    @Test
    public void unreadableDeletionIndexFailsClosedWherePermissionsAreEnforced() throws Exception {
        Path root = temporaryFolder.newFolder("unreadable-deletion-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "unreadable-deleted-event", "lane", "camera");
        store.recordTerminalDeletion(identity);
        Path index = Path.of(bucketStore.getResourceURI(MediaSetStore.VIDEO_BUCKET,
                MediaSetStore.deletionIndexKey(identity)));
        org.junit.Assume.assumeTrue("POSIX permissions are unavailable",
                Files.getFileStore(index).supportsFileAttributeView("posix"));
        Set<PosixFilePermission> original = Files.getPosixFilePermissions(index);
        try {
            Files.setPosixFilePermissions(index, Set.of());
            org.junit.Assume.assumeFalse("This runtime bypasses mode-bit read restrictions",
                    Files.isReadable(index));
            assertThrows("A live store must not trust a cache after access is revoked",
                    IOException.class, () -> store.isTerminallyDeleted(identity));
            assertThrows(IOException.class,
                    () -> new MediaSetStore(bucketStore).isTerminallyDeleted(identity));
        } finally {
            Files.setPosixFilePermissions(index, original);
        }
    }

    @Test
    public void unreadableProcessedIndexFailsClosedWherePermissionsAreEnforced() throws Exception {
        Path root = temporaryFolder.newFolder("unreadable-processed-index").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        MediaSetStore store = new MediaSetStore(bucketStore);
        Instant resultTime = Instant.parse("2026-01-01T12:03:00Z");
        RetentionProcessedObservation marker = RetentionProcessedObservation.at(
                "unreadable-processed", resultTime, resultTime);
        store.markObservationProcessed(marker);
        Path index = Path.of(bucketStore.getResourceURI(MediaSetStore.VIDEO_BUCKET,
                MediaSetStore.processedObservationIndexKey(resultTime)));
        org.junit.Assume.assumeTrue("POSIX permissions are unavailable",
                Files.getFileStore(index).supportsFileAttributeView("posix"));
        Set<PosixFilePermission> original = Files.getPosixFilePermissions(index);
        try {
            Files.setPosixFilePermissions(index, Set.of());
            org.junit.Assume.assumeFalse("This runtime bypasses mode-bit read restrictions",
                    Files.isReadable(index));
            assertThrows("A live processed-index cache must not bypass revoked access",
                    IOException.class,
                    () -> store.isObservationProcessed(marker.observationId(), resultTime));
            assertThrows(IOException.class,
                    () -> new MediaSetStore(bucketStore)
                            .isObservationProcessed(marker.observationId(), resultTime));
        } finally {
            Files.setPosixFilePermissions(index, original);
        }
    }

    @Test
    public void decodeFailureLeavesSourceAndDurableFailureState() throws Exception {
        Path root = temporaryFolder.newFolder("failed-media-store").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        bucketStore.createBucket(MediaSetStore.VIDEO_BUCKET);
        String sourceKey = "clips/camera/corrupt.mp4";
        Path source = Path.of(bucketStore.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET, sourceKey));
        Files.write(source, "not a video".getBytes(StandardCharsets.UTF_8));

        MediaSetStore store = new MediaSetStore(bucketStore);
        RetainedMediaBackend backend = new RetainedMediaBackend(bucketStore, store, 85,
                Duration.ZERO, LoggerFactory.getLogger(getClass()));
        MediaSetStore.Identity identity = new MediaSetStore.Identity("failed-event", "lane", "camera");

        assertFalse(backend.process(identity, "videos/" + sourceKey));
        assertTrue("Any failure must preserve the source",
                bucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, sourceKey));
        assertTrue(store.findManifest(identity).isEmpty());
        assertEquals(MediaJobLedger.JobState.FAILED, store.readLedger(identity).orElseThrow().state());
        assertTrue("A failed transcode must be durably indexed for bounded retry",
                store.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));
    }

    @Test
    public void missingSourceIsRecordedAsRetryableDurableFailure() throws Exception {
        Path root = temporaryFolder.newFolder("missing-source-store").toPath();
        FileSystemBucketStore bucketStore = new FileSystemBucketStore(root);
        bucketStore.createBucket(MediaSetStore.VIDEO_BUCKET);
        MediaSetStore store = new MediaSetStore(bucketStore);
        RetainedMediaBackend backend = new RetainedMediaBackend(bucketStore, store, 85,
                Duration.ofHours(24), LoggerFactory.getLogger(getClass()));
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "missing-event", "lane", "camera");

        assertFalse(backend.process(identity, "videos/clips/camera/not-yet-published.mp4"));
        MediaJobLedger ledger = store.readLedger(identity).orElseThrow();
        assertEquals(MediaJobLedger.JobState.FAILED, ledger.state());
        assertEquals(1, ledger.attempts());
        assertTrue(ledger.failure().contains("missing"));
        assertTrue(store.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));
        int retryShard = Integer.parseInt(identity.mediaSetId().substring(0, 2), 16);
        List<MediaSetStore.MaintenanceEntry> retry = store.listMaintenance(
                MediaMaintenanceJob.Kind.RETRY, retryShard, "", 10);
        assertEquals(1, retry.size());
        assertEquals(identity, retry.get(0).job().identity());
        assertTrue(retry.get(0).job().dueAt().isAfter(ledger.updatedAt()));
    }

    @Test
    public void selectionUsesFirstQuartilesAndLastWithoutDuplicateFrames() {
        List<JpegFrameExtractor.FramePoint> timeline = java.util.stream.LongStream.range(0, 9)
                .mapToObj(index -> new JpegFrameExtractor.FramePoint(index, index * 10, index))
                .toList();
        assertArrayEquals(new long[]{0, 2, 4, 6, 8},
                JpegFrameExtractor.selectDistinctOrdinals(timeline));
    }

    private static List<MediaSetStore.Identity> identitiesInOneDeletionIndex(int count) {
        Map<String, List<MediaSetStore.Identity>> byIndex = new HashMap<>();
        for (int candidate = 0; candidate < 100_000; candidate++) {
            MediaSetStore.Identity identity = new MediaSetStore.Identity(
                    "concurrent-deleted-event-" + candidate, "lane", "camera");
            List<MediaSetStore.Identity> matching = byIndex.computeIfAbsent(
                    MediaSetStore.deletionIndexKey(identity), ignored -> new ArrayList<>());
            matching.add(identity);
            if (matching.size() == count)
                return List.copyOf(matching);
        }
        throw new AssertionError("Unable to find identities sharing one deletion-index shard");
    }

    private static void writeProcessedIndex(Path index, int records, int firstValue)
            throws IOException {
        try (FileChannel channel = FileChannel.open(index, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(new byte[]{
                    'O', 'S', 'C', 'A', 'R', 'M', 'P', 'I', 0, 0, 0, 1, 0, 0, 0, 36
            }));
            ByteBuffer record = ByteBuffer.allocate(36);
            byte[] digest = new byte[32];
            for (int offset = 0; offset < records; offset++) {
                java.util.Arrays.fill(digest, (byte) 0);
                ByteBuffer.wrap(digest).putInt(28, firstValue + offset);
                CRC32C checksum = new CRC32C();
                checksum.update(digest, 0, digest.length);
                record.clear();
                record.put(digest).putInt((int) checksum.getValue()).flip();
                while (record.hasRemaining())
                    channel.write(record);
            }
            channel.force(true);
        }
    }

    private static Path deletionIndexLockPath(FileSystemBucketStore bucketStore,
                                              MediaSetStore.Identity identity)
            throws Exception {
        String indexKey = MediaSetStore.deletionIndexKey(identity);
        String lockKey = indexKey.substring(0, indexKey.lastIndexOf('/') + 1) + ".index.lock";
        return Path.of(bucketStore.getWritableResourceURI(MediaSetStore.VIDEO_BUCKET, lockKey));
    }

    private static Path bundledH264Fixture() throws URISyntaxException {
        var resource = RetainedMediaBackendTest.class.getResource(
                "/org/sensorhub/impl/sensor/ffmpeg/sample-stream.ts");
        assertNotNull("Bundled H.264 retention fixture is missing", resource);
        assertEquals("file", resource.getProtocol());
        return Path.of(resource.toURI());
    }

    private static MediaManifest dummyManifest(MediaSetStore.Identity identity,
                                                MediaSetStore.Identity assetIdentity,
                                                String sourceKey) {
        String digest = "0".repeat(64);
        List<MediaManifest.Asset> assets = new ArrayList<>();
        for (MediaManifest.FramePosition position : MediaManifest.FramePosition.values()) {
            int index = position.ordinal() + 1;
            assets.add(new MediaManifest.Asset(position,
                    "retained-media/v1/events/" + MediaSetStore.encode(assetIdentity.eventId())
                            + "/lanes/" + MediaSetStore.encode(assetIdentity.laneUid())
                            + "/cameras/" + MediaSetStore.encode(assetIdentity.cameraUid())
                            + String.format("/frame-%02d.jpg", index),
                    "image/jpeg", digest, 1, 1, 1, position.ordinal(),
                    position.ordinal(), position.ordinal(), MediaManifest.AssetState.PUBLISHED));
        }
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new MediaManifest(MediaManifest.CURRENT_SCHEMA_VERSION, identity.mediaSetId(),
                identity.eventId(), identity.laneUid(), identity.cameraUid(), now, 85,
                MediaManifest.LifecycleState.AVAILABLE, MediaManifest.Hold.none(),
                new MediaManifest.Source(sourceKey, digest, 1, "h264", 1,
                        now.plusSeconds(60), MediaManifest.SourceState.PRESERVED_FOR_ROLLBACK),
                assets);
    }

    private void assertOrphanCleanupRequiresDurability(String fileName, String suffix)
            throws Exception {
        Path root = temporaryFolder.newFolder("durable-orphan-" + suffix).toPath();
        FileSystemBucketStore seedBucketStore = new FileSystemBucketStore(root);
        MediaSetStore seedStore = new MediaSetStore(seedBucketStore);
        MediaSetStore.Identity identity = new MediaSetStore.Identity(
                "durable-orphan-event-" + suffix, "lane", "camera");
        String key = seedStore.baseKey(identity) + "/" + fileName;
        Path orphan = Path.of(seedBucketStore.getWritableResourceURI(
                MediaSetStore.VIDEO_BUCKET, key));
        Files.write(orphan, new byte[]{1, 2, 3});

        DeleteParentAfterObjectStore failingBucketStore =
                new DeleteParentAfterObjectStore(root, key);
        MediaSetStore failingStore = new MediaSetStore(failingBucketStore);
        assertThrows("Cleanup must not report success before the unlink directory is durable",
                IOException.class, () -> failingStore.cleanupUncommittedFrames(identity));
        assertFalse(failingBucketStore.objectExists(MediaSetStore.VIDEO_BUCKET, key));
        assertTrue(Files.isRegularFile(orphan.getParent(),
                java.nio.file.LinkOption.NOFOLLOW_LINKS));

        MediaSetStore restarted = new MediaSetStore(new FileSystemBucketStore(root));
        assertThrows("Restart must not treat an irregular parent as durable orphan absence",
                IOException.class, () -> restarted.cleanupUncommittedFrames(identity));
        Files.delete(orphan.getParent());
        restarted.cleanupUncommittedFrames(identity);
    }

    /** Simulates a crash boundary where unlink succeeds but its parent cannot be fsynced. */
    private static final class DeleteParentAfterObjectStore extends FileSystemBucketStore {
        private final String targetKey;
        private boolean armed = true;

        private DeleteParentAfterObjectStore(Path root, String targetKey) throws IOException {
            super(root);
            this.targetKey = targetKey;
        }

        @Override
        public void deleteObject(String bucketName, String key) throws DataStoreException {
            Path parent = null;
            if (armed && MediaSetStore.VIDEO_BUCKET.equals(bucketName) && targetKey.equals(key))
                parent = Path.of(getWritableResourceURI(bucketName, key)).getParent();
            super.deleteObject(bucketName, key);
            if (parent == null)
                return;
            try {
                Files.delete(parent);
                Files.write(parent, new byte[]{1}, StandardOpenOption.CREATE_NEW);
                armed = false;
            } catch (IOException e) {
                throw new DataStoreException("Unable to inject irregular delete parent", e);
            }
        }
    }

}
