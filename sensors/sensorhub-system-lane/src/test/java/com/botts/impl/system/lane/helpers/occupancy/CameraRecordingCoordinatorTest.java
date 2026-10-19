package com.botts.impl.system.lane.helpers.occupancy;

import org.junit.Test;
import org.sensorhub.api.command.CommandResult;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.vast.data.DataBlockString;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CameraRecordingCoordinatorTest {
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(2);

    @Test
    public void callbackBeforeEndAndBackToBackNonAlarmCannotContaminateAlarm() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        OccupancyRecordingRegistry registry = new OccupancyRecordingRegistry();
        FakeCamera camera = new FakeCamera("urn:osh:camera:lane-01");
        Instant firstStart = Instant.parse("2026-10-18T08:09:41.315Z");
        Instant secondStart = Instant.parse("2026-10-18T08:09:42.315Z");

        CameraRecordingCoordinator.Recording first = coordinator.begin(
                firstStart, "2026-10-18_08-09-41_315", List.of(camera));
        Invocation firstOpen = camera.take();
        assertEquals("2026-10-18_08-09-41_315-g1.mp4", firstOpen.fileName);
        firstOpen.complete(accepted());
        registry.register(first, registry.epoch());

        OccupancyRecordingRegistry.Resolution firstResolution = registry.resolve(
                occupancy(firstStart, false), TEST_TIMEOUT, coordinator::finish);
        assertNotNull(firstResolution);
        Invocation firstCancel = camera.take();
        assertFalse(firstCancel.save);

        CameraRecordingCoordinator.Recording second = coordinator.begin(
                secondStart, "2026-10-18_08-09-42_315", List.of(camera));
        assertNull("The next open must wait for the real prior cancellation",
                camera.calls.poll(100, TimeUnit.MILLISECONDS));
        firstCancel.complete(acceptedWithPath(camera.path(first.fileName())));
        assertFalse(registry.complete(firstResolution, false));

        Invocation secondOpen = camera.take();
        assertEquals("2026-10-18_08-09-42_315-g2.mp4", secondOpen.fileName);
        secondOpen.complete(accepted());
        registry.register(second, registry.epoch());
        CompletableFuture<OccupancyRecordingRegistry.Resolution> alarmResolution =
                CompletableFuture.supplyAsync(() -> registry.resolve(
                        occupancy(secondStart, true), TEST_TIMEOUT, coordinator::finish));
        Invocation secondClose = camera.take();
        assertTrue(secondClose.save);
        String currentPath = camera.path(second.fileName());
        secondClose.complete(acceptedWithPath(currentPath));

        OccupancyRecordingRegistry.Resolution resolved = alarmResolution.get(2, TimeUnit.SECONDS);
        assertEquals(List.of(currentPath), resolved.paths());
        assertTrue(registry.complete(resolved, true));
        assertTrue(first.videoPaths().join().isEmpty());
        assertEquals(0, registry.pendingCount());
    }

    @Test
    public void delayedMultiCameraResultsKeepSnapshotOrderAndAssociation() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        FakeCamera front = new FakeCamera("urn:osh:camera:front");
        FakeCamera rear = new FakeCamera("urn:osh:camera:rear");
        Instant start = Instant.parse("2026-10-18T04:50:07.890Z");

        CameraRecordingCoordinator.Recording recording = coordinator.begin(start,
                "2026-10-18_04-50-07_890", List.of(front, rear));
        front.take().complete(accepted());
        rear.take().complete(accepted());
        recording.decide(true);
        coordinator.finishActive(true);
        Invocation frontClose = front.take();
        Invocation rearClose = rear.take();

        String rearPath = rear.path(recording.fileName());
        rearClose.complete(completedWithPath(rearPath));
        assertFalse(recording.videoPaths().isDone());
        String frontPath = front.path(recording.fileName());
        frontClose.complete(acceptedWithPath(frontPath));

        assertEquals(List.of(frontPath, rearPath),
                recording.videoPaths().get(2, TimeUnit.SECONDS));
        assertTrue(recording.markPublished());
    }

    @Test
    public void cameraMembershipChangesDoNotAlterActiveGenerationSnapshot() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        FakeCamera original = new FakeCamera("urn:osh:camera:original");
        FakeCamera addedLater = new FakeCamera("urn:osh:camera:added-later");
        List<CameraRecordingCoordinator.CameraTarget> mutableTargets = new ArrayList<>();
        mutableTargets.add(original);

        CameraRecordingCoordinator.Recording recording = coordinator.begin(Instant.EPOCH,
                "snapshot", mutableTargets);
        original.take().complete(accepted());
        mutableTargets.clear();
        mutableTargets.add(addedLater);
        recording.decide(true);
        coordinator.finishActive(true);

        original.take().complete(acceptedWithPath(original.path(recording.fileName())));
        assertNull(addedLater.calls.poll(100, TimeUnit.MILLISECONDS));
        assertEquals(List.of(original.path(recording.fileName())), recording.videoPaths().join());
        recording.markPublished();
    }

    @Test
    public void oneMalformedCameraCannotHideAnotherSuccessfulCamera() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        FakeCamera good = new FakeCamera("urn:osh:camera:good");
        FakeCamera openFailed = new FakeCamera("urn:osh:camera:open-failed");
        FakeCamera closeFailed = new FakeCamera("urn:osh:camera:close-failed");
        FakeCamera closeException = new FakeCamera("urn:osh:camera:close-exception");
        FakeCamera nullStatus = new FakeCamera("urn:osh:camera:null-status");
        FakeCamera blank = new FakeCamera("urn:osh:camera:blank");
        FakeCamera stale = new FakeCamera("urn:osh:camera:stale");
        FakeCamera wrongCamera = new FakeCamera("urn:osh:camera:wrong-camera");
        FakeCamera malformed = new FakeCamera("urn:osh:camera:malformed");
        malformed.throwOnOwnsPath = true;
        List<FakeCamera> cameras = List.of(good, openFailed, closeFailed, closeException,
                nullStatus, blank, stale, wrongCamera, malformed);

        CameraRecordingCoordinator.Recording recording = coordinator.begin(
                Instant.parse("2026-10-18T04:50:07.890Z"),
                "2026-10-18_04-50-07_890", new ArrayList<>(cameras));
        for (FakeCamera camera : cameras) {
            camera.take().complete(camera == openFailed
                    ? CommandStatus.failed(BigId.NONE, "open failed") : accepted());
        }
        recording.decide(true);
        coordinator.finishActive(true);

        String validPath = good.path(recording.fileName());
        good.take().complete(acceptedWithPath(validPath));
        Invocation openFailedClose = openFailed.take();
        assertFalse(openFailedClose.save);
        openFailedClose.complete(acceptedWithPath(openFailed.path(recording.fileName())));
        String failedAfterPublishPath = closeFailed.path(recording.fileName());
        closeFailed.take().complete(failedWithPath(failedAfterPublishPath));
        closeException.take().fail(new IllegalStateException("camera close crashed"));
        nullStatus.take().complete(null);
        blank.take().complete(acceptedWithPath("   "));
        stale.take().complete(acceptedWithPath(stale.path("prior-g7.mp4")));
        wrongCamera.take().complete(acceptedWithPath(
                "videos/clips/urn-osh-camera-someone-else/" + recording.fileName()));
        malformed.take().complete(acceptedWithPath(malformed.path(recording.fileName())));

        assertEquals(List.of(validPath), recording.videoPaths().get(2, TimeUnit.SECONDS));
        assertEquals("A failed post-link close must clean its unpublished final object",
                List.of(failedAfterPublishPath), closeFailed.discarded);
        assertTrue(recording.markPublished());
    }

    @Test
    public void reverseCallbacksWithinOneSecondMatchExactGenerations() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        FakeCamera firstCamera = new FakeCamera("urn:osh:camera:first");
        FakeCamera secondCamera = new FakeCamera("urn:osh:camera:second");
        Instant firstStart = Instant.parse("2026-10-18T10:00:00.100Z");
        Instant secondStart = Instant.parse("2026-10-18T10:00:00.900Z");

        CameraRecordingCoordinator.Recording first = finishedRecording(
                coordinator, firstCamera, firstStart, "first");
        CameraRecordingCoordinator.Recording second = finishedRecording(
                coordinator, secondCamera, secondStart, "second");

        OccupancyRecordingRegistry registry = new OccupancyRecordingRegistry();
        long epoch = registry.epoch();
        registry.register(first, epoch);
        registry.register(second, epoch);

        OccupancyRecordingRegistry.Resolution secondResolution = registry.resolve(
                occupancy(secondStart, true), TEST_TIMEOUT);
        OccupancyRecordingRegistry.Resolution firstResolution = registry.resolve(
                occupancy(firstStart, true), TEST_TIMEOUT);
        assertEquals(List.of(secondCamera.path(second.fileName())), secondResolution.paths());
        assertEquals(List.of(firstCamera.path(first.fileName())), firstResolution.paths());
        assertTrue(registry.complete(secondResolution, true));
        assertTrue(registry.complete(firstResolution, true));
    }

    @Test
    public void callbackWaitsForDelayedRegistrationWithoutStrandingRecording() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        OccupancyRecordingRegistry registry = new OccupancyRecordingRegistry();
        FakeCamera camera = new FakeCamera("urn:osh:camera:delayed-register");
        Instant start = Instant.parse("2026-10-18T11:00:00.123Z");
        CompletableFuture<OccupancyRecordingRegistry.Resolution> callback =
                CompletableFuture.supplyAsync(() -> registry.resolve(
                        occupancy(start, true), TEST_TIMEOUT));

        CameraRecordingCoordinator.Recording recording = coordinator.begin(start, "delayed",
                List.of(camera));
        camera.take().complete(accepted());
        coordinator.finishActive(true);
        registry.register(recording, registry.epoch());
        camera.take().complete(acceptedWithPath(camera.path(recording.fileName())));

        OccupancyRecordingRegistry.Resolution resolution = callback.get(2, TimeUnit.SECONDS);
        assertNotNull(resolution);
        assertEquals(List.of(camera.path(recording.fileName())), resolution.paths());
        assertTrue(registry.complete(resolution, true));
    }

    @Test
    public void clearAfterCloseBeforeAttachInvalidatesAndDeletesUnpublishedFile() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        OccupancyRecordingRegistry registry = new OccupancyRecordingRegistry();
        FakeCamera camera = new FakeCamera("urn:osh:camera:stop-race");
        Instant start = Instant.parse("2026-10-18T12:00:00.123Z");
        CameraRecordingCoordinator.Recording recording = finishedRecording(
                coordinator, camera, start, "stop-race");
        registry.register(recording, registry.epoch());

        OccupancyRecordingRegistry.Resolution resolution = registry.resolve(
                occupancy(start, true), TEST_TIMEOUT);
        assertNotNull(resolution);
        registry.clear();
        assertFalse(registry.complete(resolution, true));
        assertEquals(List.of(camera.path(recording.fileName())), camera.discarded);
    }

    @Test
    public void coordinatorStillOwnsCompletedFileUntilRegistryPublishesIt() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        FakeCamera camera = new FakeCamera("urn:osh:camera:unpublished");
        CameraRecordingCoordinator.Recording recording = finishedRecording(coordinator, camera,
                Instant.parse("2026-10-18T12:10:00Z"), "unpublished");

        assertTrue(recording.videoPaths().isDone());
        coordinator.cancelAll();
        assertEquals(List.of(camera.path(recording.fileName())), camera.discarded);
    }

    @Test
    public void authoritativeLateNonAlarmDeletesFallbackSavedFile() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator(
                Duration.ofSeconds(2), Duration.ofMillis(50));
        OccupancyRecordingRegistry registry = new OccupancyRecordingRegistry();
        FakeCamera camera = new FakeCamera("urn:osh:camera:fallback-mismatch");
        Instant start = Instant.parse("2026-10-18T12:15:00Z");
        CameraRecordingCoordinator.Recording recording = coordinator.begin(start, "fallback",
                List.of(camera));
        camera.take().complete(accepted());
        registry.register(recording, registry.epoch());

        // Simulate a daily-file alarm state whose occupancy output is delayed and ultimately says
        // non-alarm. The bounded fallback may close/save, but it must never leave an orphan.
        coordinator.finishActive(true);
        Invocation close = camera.take();
        assertTrue(close.save);
        String path = camera.path(recording.fileName());
        close.complete(acceptedWithPath(path));
        assertEquals(List.of(path), recording.videoPaths().get(2, TimeUnit.SECONDS));

        OccupancyRecordingRegistry.Resolution resolution = registry.resolve(
                occupancy(start, false), TEST_TIMEOUT);
        assertNotNull(resolution);
        assertFalse(registry.complete(resolution, false));
        assertEquals(List.of(path), camera.discarded);
    }

    @Test
    public void staleLifecycleRegistrationIsRejectedAndCleaned() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        OccupancyRecordingRegistry registry = new OccupancyRecordingRegistry();
        FakeCamera camera = new FakeCamera("urn:osh:camera:old-lifecycle");
        CameraRecordingCoordinator.Recording recording = finishedRecording(coordinator, camera,
                Instant.parse("2026-10-18T12:20:00Z"), "old-lifecycle");
        long oldEpoch = registry.epoch();
        registry.clear();
        registry.register(recording, oldEpoch);

        assertEquals(0, registry.pendingCount());
        assertEquals(List.of(camera.path(recording.fileName())), camera.discarded);
    }

    @Test
    public void pendingRegistryCapInvalidatesEvictedGeneration() {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        OccupancyRecordingRegistry registry = new OccupancyRecordingRegistry();
        CameraRecordingCoordinator.Recording oldest = null;
        long epoch = registry.epoch();
        for (int i = 0; i < 65; i++) {
            CameraRecordingCoordinator.Recording recording = coordinator.begin(
                    Instant.ofEpochMilli(1_000L + i), "pending-" + i, List.of());
            if (oldest == null)
                oldest = recording;
            recording.decide(true);
            coordinator.finishActive(true);
            registry.register(recording, epoch);
        }

        assertEquals(64, registry.pendingCount());
        assertFalse("Evicted recording must no longer be publishable", oldest.markPublished());
        registry.clear();
    }

    @Test
    public void overlappingBeginCancelsPriorGenerationBeforeOpeningNext() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        FakeCamera camera = new FakeCamera("urn:osh:camera:overlap");
        CameraRecordingCoordinator.Recording first = coordinator.begin(Instant.EPOCH,
                "first", List.of(camera));
        camera.take().complete(accepted());

        CameraRecordingCoordinator.Recording second = coordinator.begin(Instant.EPOCH.plusSeconds(1),
                "second", List.of(camera));
        Invocation cancel = camera.take();
        assertFalse(cancel.save);
        assertNull(camera.calls.poll(100, TimeUnit.MILLISECONDS));
        cancel.complete(accepted());
        assertEquals(Operation.OPEN, camera.take().operation);
        assertTrue(first.videoPaths().join().isEmpty());
        assertNotNull(second);
        coordinator.cancelAll();
    }

    @Test
    public void zeroCamerasCompletesWithoutBlankPath() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator();
        CameraRecordingCoordinator.Recording recording = coordinator.begin(Instant.EPOCH,
                "no-camera", List.of());
        recording.decide(true);
        coordinator.finishActive(true);
        assertEquals(List.of(), recording.videoPaths().get(2, TimeUnit.SECONDS));
        recording.invalidate();
    }

    @Test
    public void hungCameraCommandTimesOutWithoutReleasingFifoBarrier() throws Exception {
        CameraRecordingCoordinator coordinator =
                new CameraRecordingCoordinator(Duration.ofMillis(100));
        OccupancyRecordingRegistry registry = new OccupancyRecordingRegistry();
        FakeCamera camera = new FakeCamera("urn:osh:camera:hung");
        Instant start = Instant.parse("2026-10-18T12:30:00Z");
        CameraRecordingCoordinator.Recording recording = coordinator.begin(start,
                "hung", List.of(camera));
        camera.take(); // deliberately never complete the real open
        registry.register(recording, registry.epoch());

        assertNull(registry.resolve(occupancy(start, true), Duration.ofMillis(250),
                coordinator::finish));
        assertNull("The close must remain behind the real unfinished open",
                camera.calls.poll(150, TimeUnit.MILLISECONDS));
        for (int i = 0; i < 100; i++) {
            CameraRecordingCoordinator.Recording later = coordinator.begin(
                    Instant.EPOCH.plusSeconds(i + 1L), "later-" + i, List.of(camera));
            later.decide(false);
            coordinator.finishActive(false);
            later.invalidate();
        }
        assertTrue("A quarantined camera must retain only a finite cleanup chain",
                coordinator.pendingCommandCount(camera.serializationKey()) <= 4);
        assertNull(camera.calls.poll(150, TimeUnit.MILLISECONDS));
        recording.invalidate();
    }

    @Test
    public void queuedCommandGetsFullExecutionTimeoutWithoutCrossGenerationContamination()
            throws Exception {
        Duration operationTimeout = Duration.ofMillis(400);
        CameraRecordingCoordinator coordinator =
                new CameraRecordingCoordinator(operationTimeout, Duration.ofSeconds(2));
        FakeCamera camera = new FakeCamera("urn:osh:camera:keyframe-gated");
        Instant firstStart = Instant.parse("2026-10-18T12:35:00Z");

        CameraRecordingCoordinator.Recording first = coordinator.begin(firstStart,
                "first-keyframe-gated", List.of(camera));
        Invocation firstOpen = camera.take();
        first.decide(true);
        assertTrue(coordinator.finish(first, true));

        // Model a successful first-keyframe wait that consumes most of the per-operation bound.
        // The close has already been enqueued, but its own deadline must not start yet.
        Thread.sleep(250);
        firstOpen.complete(accepted());
        Invocation firstClose = camera.take();

        // Cross the close's old enqueue-relative deadline while keeping its actual execution
        // below the same bound. Before the fix this quarantined the healthy camera and rejected
        // the next generation's open.
        Thread.sleep(200);
        CameraRecordingCoordinator.Recording second = coordinator.begin(
                firstStart.plusSeconds(1), "second-keyframe-gated", List.of(camera));

        String firstPath = camera.path(first.fileName());
        firstClose.complete(acceptedWithPath(firstPath));
        assertEquals(List.of(firstPath), first.videoPaths().get(2, TimeUnit.SECONDS));

        Invocation secondOpen = camera.take();
        assertEquals(Operation.OPEN, secondOpen.operation);
        assertEquals(second.fileName(), secondOpen.fileName);
        secondOpen.complete(accepted());
        second.decide(true);
        assertTrue(coordinator.finish(second, true));

        Invocation secondClose = camera.take();
        assertEquals(Operation.CLOSE, secondClose.operation);
        String secondPath = camera.path(second.fileName());
        secondClose.complete(acceptedWithPath(secondPath));
        assertEquals(List.of(secondPath), second.videoPaths().get(2, TimeUnit.SECONDS));
        assertFalse("Each generation must retain only its own output",
                second.videoPaths().getNow(List.of()).contains(firstPath));

        first.invalidate();
        second.invalidate();
    }

    @Test
    public void lateCloseAfterRegistryTimeoutIsObservedAndDiscarded() throws Exception {
        CameraRecordingCoordinator coordinator = new CameraRecordingCoordinator(
                Duration.ofMillis(50), Duration.ofSeconds(2));
        OccupancyRecordingRegistry registry = new OccupancyRecordingRegistry();
        FakeCamera camera = new FakeCamera("urn:osh:camera:late-close");
        Instant start = Instant.parse("2026-10-18T12:40:00Z");
        CameraRecordingCoordinator.Recording recording = coordinator.begin(start,
                "late-close", List.of(camera));
        camera.take().complete(accepted());
        registry.register(recording, registry.epoch());

        CompletableFuture<OccupancyRecordingRegistry.Resolution> callback =
                CompletableFuture.supplyAsync(() -> registry.resolve(occupancy(start, true),
                        Duration.ofMillis(200), coordinator::finish));
        Invocation delayedClose = camera.take();
        assertTrue(delayedClose.save);
        assertNull(callback.get(1, TimeUnit.SECONDS));

        String path = camera.path(recording.fileName());
        delayedClose.complete(acceptedWithPath(path));
        assertEquals(List.of(), recording.videoPaths().get(2, TimeUnit.SECONDS));
        assertEquals(List.of(path), camera.discarded);
    }

    @Test
    public void strictPathOwnershipAcceptsOnlyExactRelativeCameraObject() {
        String token = "urn-osh-camera-lane-01";
        String file = "2026-10-18_04-50-07_890-g2.mp4";
        assertTrue(OccupancyWrapper.isExpectedCameraPath("clips/" + token + "/" + file,
                token, file));
        assertTrue(OccupancyWrapper.isExpectedCameraPath("videos/clips/" + token + "/" + file,
                token, file));
        assertFalse(OccupancyWrapper.isExpectedCameraPath("/videos/clips/" + token + "/" + file,
                token, file));
        assertFalse(OccupancyWrapper.isExpectedCameraPath("file://clips/" + token + "/" + file,
                token, file));
        assertFalse(OccupancyWrapper.isExpectedCameraPath("prefix/clips/" + token + "/" + file,
                token, file));
        assertFalse(OccupancyWrapper.isExpectedCameraPath("clips/../" + token + "/" + file,
                token, file));
        assertFalse(OccupancyWrapper.isExpectedCameraPath("clips//" + token + "/" + file,
                token, file));
        assertFalse(OccupancyWrapper.isExpectedCameraPath(
                "clips/urn-osh-camera-other/" + file, token, file));
        assertFalse(OccupancyWrapper.isExpectedCameraPath(
                "clips/" + token + "/old-g1.mp4", token, file));
    }

    @Test
    public void occupancyPathAttachmentDeduplicatesWithoutReordering() {
        Occupancy occupancy = new Occupancy.Builder()
                .videoPaths(new ArrayList<>(List.of("videos/clips/camera/first.mp4")))
                .build();
        OccupancyWrapper.appendDistinctVideoPaths(occupancy, List.of(
                "videos/clips/camera/first.mp4",
                "videos/clips/camera/second.mp4"));

        assertEquals(List.of("videos/clips/camera/first.mp4",
                "videos/clips/camera/second.mp4"), occupancy.getVideoPaths());
    }

    private static CameraRecordingCoordinator.Recording finishedRecording(
            CameraRecordingCoordinator coordinator, FakeCamera camera, Instant start,
            String prefix) throws Exception {
        CameraRecordingCoordinator.Recording recording = coordinator.begin(start, prefix,
                List.of(camera));
        camera.take().complete(accepted());
        recording.decide(true);
        coordinator.finishActive(true);
        camera.take().complete(acceptedWithPath(camera.path(recording.fileName())));
        recording.videoPaths().get(2, TimeUnit.SECONDS);
        return recording;
    }

    private static Occupancy occupancy(Instant start, boolean alarm) {
        return new Occupancy.Builder()
                .startTime(start.getEpochSecond() + start.getNano() / 1_000_000_000.0)
                .gammaAlarm(alarm)
                .build();
    }

    private static ICommandStatus accepted() {
        return new CommandStatus.Builder()
                .withCommand(BigId.NONE)
                .withStatusCode(ICommandStatus.CommandStatusCode.ACCEPTED)
                .build();
    }

    private static ICommandStatus acceptedWithPath(String path) {
        DataBlockString block = new DataBlockString(1);
        block.setStringValue(0, path);
        return new CommandStatus.Builder()
                .withCommand(BigId.NONE)
                .withStatusCode(ICommandStatus.CommandStatusCode.ACCEPTED)
                .withResult(CommandResult.withData(block))
                .build();
    }

    private static ICommandStatus completedWithPath(String path) {
        DataBlockString block = new DataBlockString(1);
        block.setStringValue(0, path);
        return new CommandStatus.Builder()
                .withCommand(BigId.NONE)
                .withStatusCode(ICommandStatus.CommandStatusCode.COMPLETED)
                .withResult(CommandResult.withData(block))
                .build();
    }

    private static ICommandStatus failedWithPath(String path) {
        DataBlockString block = new DataBlockString(1);
        block.setStringValue(0, path);
        return new CommandStatus.Builder()
                .withCommand(BigId.NONE)
                .withStatusCode(ICommandStatus.CommandStatusCode.FAILED)
                .withMessage("final publication durability step failed")
                .withResult(CommandResult.withData(block))
                .build();
    }

    private enum Operation { OPEN, CLOSE }

    private static final class Invocation {
        private final Operation operation;
        private final String fileName;
        private final boolean save;
        private final CompletableFuture<ICommandStatus> result = new CompletableFuture<>();

        private Invocation(Operation operation, String fileName, boolean save) {
            this.operation = operation;
            this.fileName = fileName;
            this.save = save;
        }

        private void complete(ICommandStatus status) {
            result.complete(status);
        }

        private void fail(Throwable failure) {
            result.completeExceptionally(failure);
        }
    }

    private static final class FakeCamera implements CameraRecordingCoordinator.CameraTarget {
        private final String uid;
        private final String token;
        private final LinkedBlockingQueue<Invocation> calls = new LinkedBlockingQueue<>();
        private final List<String> discarded = new CopyOnWriteArrayList<>();
        private boolean throwOnOwnsPath;

        private FakeCamera(String uid) {
            this.uid = uid;
            this.token = uid.replace(':', '-');
        }

        @Override
        public String cameraId() {
            return uid;
        }

        @Override
        public Object serializationKey() {
            return this;
        }

        @Override
        public CompletableFuture<ICommandStatus> open(String fileName) {
            Invocation invocation = new Invocation(Operation.OPEN, fileName, false);
            calls.add(invocation);
            return invocation.result;
        }

        @Override
        public CompletableFuture<ICommandStatus> close(boolean save) {
            Invocation invocation = new Invocation(Operation.CLOSE, null, save);
            calls.add(invocation);
            return invocation.result;
        }

        @Override
        public CompletableFuture<Void> discard(String path) {
            discarded.add(path);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean ownsPath(String path, String requestedFileName) {
            if (throwOnOwnsPath)
                throw new IllegalArgumentException("malformed result");
            return OccupancyWrapper.isExpectedCameraPath(path, token, requestedFileName);
        }

        private String path(String requestedFileName) {
            return "videos/clips/" + token + "/" + requestedFileName;
        }

        private Invocation take() throws InterruptedException {
            Invocation invocation = calls.poll(2, TimeUnit.SECONDS);
            assertNotNull("Timed out waiting for camera invocation for " + uid, invocation);
            return invocation;
        }
    }
}
