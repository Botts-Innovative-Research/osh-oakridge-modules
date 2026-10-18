package com.botts.impl.system.lane.helpers.occupancy;

import org.sensorhub.api.command.ICommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Orders file commands per camera and keeps their results scoped to one occupancy generation.
 *
 * <p>{@link org.sensorhub.impl.sensor.ffmpeg.controls.FileControl#submitCommand} is asynchronous.
 * Submitting an open followed by a close therefore does not, by itself, prove that the commands
 * will execute in that order. This coordinator gives every camera a completion chain and gives
 * every occupancy its own immutable result future. A late result can consequently complete only
 * the generation that submitted it; it can never populate the next occupancy.</p>
 */
final class CameraRecordingCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(CameraRecordingCoordinator.class);
    private static final Duration DEFAULT_DECISION_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration DEFAULT_COMMAND_TIMEOUT = Duration.ofSeconds(8);
    private static final int MAX_PENDING_COMMANDS_PER_CAMERA = 4;

    interface CameraTarget {
        String cameraId();

        /** Stable identity used to serialize commands for this physical camera control. */
        Object serializationKey();

        CompletableFuture<ICommandStatus> open(String fileName);

        CompletableFuture<ICommandStatus> close(boolean save);

        CompletableFuture<Void> discard(String path);

        /**
         * Confirms that an authoritative close result belongs to this camera and requested file.
         */
        boolean ownsPath(String path, String requestedFileName);
    }

    static final class Recording {
        private final long generation;
        private final Instant occupancyStartedAt;
        private final String fileName;
        private final List<Attempt> attempts;
        private final CompletableFuture<Boolean> saveDecision = new CompletableFuture<>();
        private final CompletableFuture<List<String>> videoPaths = new CompletableFuture<>();
        private final List<SavedPath> savedPaths = new ArrayList<>();
        private final Consumer<Recording> terminalCallback;
        private boolean invalidated;
        private boolean published;

        private Recording(long generation, Instant occupancyStartedAt, String fileName,
                          List<Attempt> attempts, Consumer<Recording> terminalCallback) {
            this.generation = generation;
            this.occupancyStartedAt = occupancyStartedAt;
            this.fileName = fileName;
            this.attempts = attempts;
            this.terminalCallback = terminalCallback;
        }

        long generation() {
            return generation;
        }

        Instant occupancyStartedAt() {
            return occupancyStartedAt;
        }

        String fileName() {
            return fileName;
        }

        CompletableFuture<List<String>> videoPaths() {
            return videoPaths;
        }

        /** The first decision wins; the occupancy callback is authoritative when it arrives. */
        boolean decide(boolean save) {
            return saveDecision.complete(save);
        }

        boolean markPublished() {
            synchronized (savedPaths) {
                if (invalidated)
                    return false;
                if (published)
                    return true;
                published = true;
            }
            terminalCallback.accept(this);
            return true;
        }

        void invalidate() {
            List<SavedPath> cleanup;
            synchronized (savedPaths) {
                if (published || invalidated)
                    return;
                invalidated = true;
                cleanup = List.copyOf(savedPaths);
            }
            decide(false);
            cleanup.forEach(Recording::discardQuietly);
            terminalCallback.accept(this);
        }

        private Optional<String> retain(CameraTarget target, String path) {
            SavedPath saved = new SavedPath(target, path);
            synchronized (savedPaths) {
                if (!invalidated) {
                    savedPaths.add(saved);
                    return Optional.of(path);
                }
            }
            discardQuietly(saved);
            return Optional.empty();
        }

        private static void discardQuietly(SavedPath saved) {
            try {
                CompletableFuture<Void> cleanup = saved.target.discard(saved.path);
                if (cleanup != null) {
                    cleanup.exceptionally(failure -> {
                        logger.error("Failed to remove unpublished camera output {}: {}",
                                saved.path, rootMessage(failure));
                        return null;
                    });
                }
            } catch (RuntimeException failure) {
                logger.error("Failed to schedule removal of unpublished camera output {}",
                        saved.path, failure);
            }
        }
    }

    private record SavedPath(CameraTarget target, String path) {}

    private static final class Attempt {
        private final CameraTarget target;
        private final CompletableFuture<ICommandStatus> openStatus;

        private Attempt(CameraTarget target, CompletableFuture<ICommandStatus> openStatus) {
            this.target = target;
            this.openStatus = openStatus;
        }
    }

    private static final class CameraQueue {
        private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
        private int pending;
        private boolean quarantined;
        private int cleanupPending;
    }

    private final Object stateLock = new Object();
    private final Object queueLock = new Object();
    private final Map<Object, CameraQueue> cameraQueues = new IdentityHashMap<>();
    private final Set<Recording> inFlight = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Duration commandTimeout;
    private final Duration decisionTimeout;
    private long nextGeneration;
    private Recording active;

    CameraRecordingCoordinator() {
        this(DEFAULT_COMMAND_TIMEOUT, DEFAULT_DECISION_TIMEOUT);
    }

    CameraRecordingCoordinator(Duration commandTimeout) {
        this(commandTimeout, DEFAULT_DECISION_TIMEOUT);
    }

    CameraRecordingCoordinator(Duration commandTimeout, Duration decisionTimeout) {
        if (commandTimeout == null || commandTimeout.isNegative() || commandTimeout.isZero())
            throw new IllegalArgumentException("commandTimeout must be positive");
        if (decisionTimeout == null || decisionTimeout.isNegative() || decisionTimeout.isZero())
            throw new IllegalArgumentException("decisionTimeout must be positive");
        this.commandTimeout = commandTimeout;
        this.decisionTimeout = decisionTimeout;
    }

    Recording begin(Instant occupancyStartedAt, String fileNamePrefix, List<CameraTarget> targets) {
        Objects.requireNonNull(occupancyStartedAt, "occupancyStartedAt");
        if (fileNamePrefix == null || fileNamePrefix.isBlank())
            throw new IllegalArgumentException("Recording file name prefix must not be blank");
        Objects.requireNonNull(targets, "targets");

        synchronized (stateLock) {
            if (active != null) {
                logger.warn("Occupancy generation {} was still active when a new occupancy began; "
                        + "ending it before opening generation {}", active.generation,
                        nextGeneration + 1);
                Recording superseded = active;
                active = null;
                superseded.invalidate();
                finishLocked(superseded, false);
            }

            long generation = ++nextGeneration;
            // The generation suffix prevents two fast/back-to-back occupancies from requesting
            // the same immutable object even when the wall clock has millisecond resolution.
            String fileName = fileNamePrefix + "-g" + generation + ".mp4";
            List<Attempt> attempts = new ArrayList<>(targets.size());
            for (CameraTarget target : targets) {
                if (target == null)
                    continue;
                CompletableFuture<ICommandStatus> open = enqueue(target,
                        () -> target.open(fileName), false);
                attempts.add(new Attempt(target, open));
            }

            active = new Recording(generation, occupancyStartedAt, fileName,
                    Collections.unmodifiableList(attempts), this::recordingTerminated);
            inFlight.add(active);
            return active;
        }
    }

    Recording finishActive(boolean save) {
        synchronized (stateLock) {
            if (active == null)
                return null;
            Recording finished = active;
            active = null;
            finishLocked(finished, save);
            return finished;
        }
    }

    /** Finishes only the exact generation named by an occupancy callback. */
    boolean finish(Recording recording, boolean fallbackSave) {
        synchronized (stateLock) {
            if (recording == null || active != recording)
                return false;
            active = null;
            finishLocked(recording, fallbackSave);
            return true;
        }
    }

    /** Cancels every active, ended, or decision-pending generation without waiting on camera I/O. */
    void cancelAll() {
        synchronized (stateLock) {
            if (active != null) {
                Recording canceled = active;
                active = null;
                finishLocked(canceled, false);
            }
            for (Recording recording : List.copyOf(inFlight))
                recording.invalidate();
        }
    }

    private void finishLocked(Recording recording, boolean fallbackSave) {
        List<CompletableFuture<Optional<String>>> paths = new ArrayList<>(recording.attempts.size());
        for (Attempt attempt : recording.attempts) {
            CompletableFuture<ICommandStatus> closeStatus = enqueue(attempt.target,
                    () -> recording.saveDecision.thenCompose(save -> {
                        // If the exposed open failed or timed out, a late underlying open may still
                        // complete. Its ordered close must be a discard, never an orphan-producing
                        // save based on stale lane state.
                        boolean openSucceeded = !attempt.openStatus.isCompletedExceptionally()
                                && isSuccessful(attempt.openStatus.getNow(null));
                        return attempt.target.close(save && openSucceeded);
                    }), true);
            CompletableFuture<Optional<String>> path = attempt.openStatus
                    .handle((status, error) -> error == null && isSuccessful(status))
                    .thenCombine(closeStatus.handle((status, error) -> {
                        if (error != null) {
                            logger.warn("Camera {} failed to close occupancy generation {}: {}",
                                    attempt.target.cameraId(), recording.generation,
                                    rootMessage(error));
                            return null;
                        }
                        return status;
                    }), (opened, closed) -> {
                        if (!recording.saveDecision.join() || !opened)
                            return Optional.<String>empty();
                        Optional<String> authoritative = hasInlineResult(closed)
                                ? validatedPath(attempt.target, recording, closed)
                                : Optional.empty();
                        // A failed close may still report a final object created before a later
                        // durability step failed. Delete it immediately; it must never become one
                        // of the paths protected by a partially successful multi-camera publish.
                        if (!isSuccessful(closed)) {
                            authoritative.ifPresent(failedPath -> Recording.discardQuietly(
                                    new SavedPath(attempt.target, failedPath)));
                            return Optional.<String>empty();
                        }
                        return authoritative.flatMap(savedPath ->
                                recording.retain(attempt.target, savedPath));
                    });
            paths.add(path);
        }

        CompletableFuture.allOf(paths.toArray(CompletableFuture[]::new))
                .whenComplete((ignored, failure) -> {
                    if (failure != null) {
                        recording.videoPaths.completeExceptionally(unwrap(failure));
                        return;
                    }

                    LinkedHashSet<String> uniquePaths = new LinkedHashSet<>();
                    for (CompletableFuture<Optional<String>> path : paths)
                        path.join().ifPresent(uniquePaths::add);
                    recording.videoPaths.complete(List.copyOf(uniquePaths));
                });
        // The occupancy result normally supplies the decision immediately. A missing result must
        // not leave a native recorder open forever, so retain the lane-state decision only as a
        // bounded fallback. stop()/RPM replacement bypasses this delay via cancelAll().
        CompletableFuture.delayedExecutor(decisionTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> recording.decide(fallbackSave));
    }

    /**
     * A completed file remains owned by the coordinator until the matching occupancy atomically
     * publishes it or invalidates it. Merely completing camera I/O is not terminal: stop/restart
     * must still be able to delete a completed but not-yet-attached file.
     */
    private void recordingTerminated(Recording recording) {
        synchronized (stateLock) {
            inFlight.remove(recording);
        }
    }

    private Optional<String> validatedPath(CameraTarget target, Recording recording,
                                           ICommandStatus closeStatus) {
        final Iterable<? extends net.opengis.swe.v20.DataBlock> records;
        try {
            if (closeStatus.getResult() == null || closeStatus.getResult().getInlineRecords() == null) {
                logger.warn("Camera {} returned no saved-video result for occupancy generation {}",
                        target.cameraId(), recording.generation);
                return Optional.empty();
            }
            records = closeStatus.getResult().getInlineRecords();
        } catch (RuntimeException malformed) {
            logger.warn("Camera {} returned a malformed saved-video result for occupancy generation {}",
                    target.cameraId(), recording.generation, malformed);
            return Optional.empty();
        }

        if (records == null) {
            logger.warn("Camera {} returned no saved-video result for occupancy generation {}",
                    target.cameraId(), recording.generation);
            return Optional.empty();
        }

        for (var result : records) {
            if (result == null)
                continue;
            final String candidate;
            try {
                candidate = result.getStringValue();
            } catch (RuntimeException e) {
                logger.warn("Camera {} returned an unreadable saved-video result for occupancy "
                        + "generation {}", target.cameraId(), recording.generation, e);
                continue;
            }
            if (candidate == null || candidate.isBlank()) {
                logger.warn("Camera {} returned a blank saved-video result for occupancy generation {}",
                        target.cameraId(), recording.generation);
                continue;
            }
            String path = candidate.trim();
            final boolean owned;
            try {
                owned = target.ownsPath(path, recording.fileName);
            } catch (RuntimeException malformed) {
                logger.warn("Camera {} could not validate its saved-video result for occupancy "
                        + "generation {}", target.cameraId(), recording.generation, malformed);
                continue;
            }
            if (!owned) {
                logger.warn("Camera {} returned a stale or mismatched saved-video result for "
                                + "occupancy generation {}: {}", target.cameraId(),
                        recording.generation, path);
                continue;
            }
            return Optional.of(path);
        }
        return Optional.empty();
    }

    private CompletableFuture<ICommandStatus> enqueue(
            CameraTarget target, Supplier<CompletableFuture<ICommandStatus>> command,
            boolean cleanup) {
        CompletableFuture<ICommandStatus> result = new CompletableFuture<>();
        // The serialization barrier is released only by the real camera future. The surrounding
        // occupancy wait is bounded; keeping this result attached to the real future lets a late
        // successful close reveal and delete any final file after that wait has been invalidated.
        CompletableFuture<Void> operationDone = new CompletableFuture<>();
        CompletableFuture<Void> tail = operationDone.handle((ignored, failure) -> null);
        CompletableFuture<Void> predecessor;
        Object key = Objects.requireNonNull(target.serializationKey(), "serializationKey");
        CameraQueue queue;
        synchronized (queueLock) {
            queue = cameraQueues.computeIfAbsent(key, ignored -> new CameraQueue());
            if ((queue.quarantined && (!cleanup || queue.cleanupPending > 0))
                    || queue.pending >= MAX_PENDING_COMMANDS_PER_CAMERA) {
                result.completeExceptionally(new RejectedExecutionException(
                        "Camera " + target.cameraId() + " command queue is quarantined"));
                return result;
            }
            predecessor = queue.tail;
            queue.tail = tail;
            queue.pending++;
            if (cleanup)
                queue.cleanupPending++;
        }

        CompletableFuture.delayedExecutor(commandTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> {
                    if (!result.isDone()) {
                        // Quarantine bounds the chain, but keep observing the real result. A late
                        // successful CLOSE can publish a final object; invalidated Recording.retain
                        // must see that authoritative path so it can delete the orphan candidate.
                        synchronized (queueLock) {
                            queue.quarantined = true;
                        }
                        logger.error("Timed out waiting for camera {} command; quarantining its "
                                + "queue until the real operation completes", target.cameraId());
                    }
                });

        // Register the chain before invoking user/camera code. Even if another thread queues the
        // next command immediately, it will wait for this result.
        predecessor.whenComplete((ignored, predecessorFailure) -> {
            final CompletableFuture<ICommandStatus> submitted;
            try {
                submitted = Objects.requireNonNull(command.get(),
                        "Camera command returned a null future");
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
                operationDone.complete(null);
                return;
            }
            submitted.whenComplete((status, failure) -> {
                if (failure == null)
                    result.complete(status);
                else
                    result.completeExceptionally(unwrap(failure));
                operationDone.complete(null);
            });
        });

        tail.whenComplete((ignored, failure) -> {
            synchronized (queueLock) {
                queue.pending--;
                if (cleanup)
                    queue.cleanupPending--;
                if (queue.pending == 0 && cameraQueues.get(key) == queue)
                    cameraQueues.remove(key);
            }
        });
        return result;
    }

    int pendingCommandCount(Object serializationKey) {
        synchronized (queueLock) {
            CameraQueue queue = cameraQueues.get(serializationKey);
            return queue == null ? 0 : queue.pending;
        }
    }

    private static boolean isSuccessful(ICommandStatus status) {
        if (status == null || status.getStatusCode() == null)
            return false;
        return status.getStatusCode() == ICommandStatus.CommandStatusCode.ACCEPTED
                || status.getStatusCode() == ICommandStatus.CommandStatusCode.COMPLETED;
    }

    private static boolean hasInlineResult(ICommandStatus status) {
        try {
            return status != null && status.getResult() != null
                    && status.getResult().getInlineRecords() != null;
        } catch (RuntimeException malformed) {
            return false;
        }
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException && current.getCause() != null)
            current = current.getCause();
        return current;
    }

    private static String rootMessage(Throwable failure) {
        Throwable root = unwrap(failure);
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
