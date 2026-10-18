package com.botts.impl.service.oscar.media;

import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
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
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

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

        MediaSetStore restartedStore = new MediaSetStore(bucketStore);
        RetainedMediaBackend restartedBackend = new RetainedMediaBackend(bucketStore,
                restartedStore, 85, Duration.ZERO, LoggerFactory.getLogger(getClass()));
        assertTrue(restartedBackend.process(identity, "videos/" + sourceKey));
        assertTrue(restartedStore.findManifest(identity).isEmpty());
        assertFalse(restartedStore.maintenanceExists(MediaMaintenanceJob.Kind.RETRY, identity));
        assertFalse(restartedStore.maintenanceExists(
                MediaMaintenanceJob.Kind.SOURCE_LIFECYCLE, identity));
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

}
