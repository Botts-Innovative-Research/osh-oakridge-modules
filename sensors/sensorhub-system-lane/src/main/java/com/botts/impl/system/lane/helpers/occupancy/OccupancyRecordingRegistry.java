package com.botts.impl.system.lane.helpers.occupancy;

import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/** Matches an occupancy output to the exact recording generation created for it. */
final class OccupancyRecordingRegistry {
    private static final Logger logger = LoggerFactory.getLogger(OccupancyRecordingRegistry.class);
    private static final int MAX_PENDING_RECORDINGS = 64;

    record Resolution(CameraRecordingCoordinator.Recording recording, List<String> paths,
                      long lifecycleEpoch) {}

    private final Object lock = new Object();
    private final Deque<CameraRecordingCoordinator.Recording> pending = new ArrayDeque<>();
    private final Set<CameraRecordingCoordinator.Recording> checkedOut =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private long lifecycleEpoch;

    long epoch() {
        synchronized (lock) {
            return lifecycleEpoch;
        }
    }

    void register(CameraRecordingCoordinator.Recording recording, long expectedEpoch) {
        if (recording == null)
            return;
        CameraRecordingCoordinator.Recording evicted = null;
        boolean stale;
        synchronized (lock) {
            stale = lifecycleEpoch != expectedEpoch;
            if (!stale) {
                pending.addLast(recording);
                if (pending.size() > MAX_PENDING_RECORDINGS) {
                    evicted = pending.removeFirst();
                    logger.warn("Discarding unmatched occupancy recording generation {} after the "
                            + "pending-generation safety bound was reached", evicted.generation());
                }
                lock.notifyAll();
            }
        }
        if (stale)
            recording.invalidate();
        if (evicted != null)
            evicted.invalidate();
    }

    Resolution resolve(Occupancy occupancy, Duration timeout) {
        return resolve(occupancy, timeout, (recording, alarm) -> { });
    }

    Resolution resolve(Occupancy occupancy, Duration timeout,
                       BiConsumer<CameraRecordingCoordinator.Recording, Boolean> matched) {
        if (timeout == null || timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException("timeout must be positive");
        long timeoutNanos = timeout.toNanos();
        long now = System.nanoTime();
        long deadline = now > Long.MAX_VALUE - timeoutNanos ? Long.MAX_VALUE : now + timeoutNanos;
        final long expectedEpoch = epoch();

        CameraRecordingCoordinator.Recording recording = takeMatching(
                occupancy, deadline, expectedEpoch);
        if (recording == null)
            return null;
        boolean alarm = occupancy.hasGammaAlarm() || occupancy.hasNeutronAlarm();
        recording.decide(alarm);
        try {
            matched.accept(recording, alarm);
        } catch (RuntimeException failure) {
            logger.error("Failed to finish matched camera recording generation {}",
                    recording.generation(), failure);
            abandon(recording);
            return null;
        }
        if (!alarm)
            return new Resolution(recording, List.of(), expectedEpoch);

        try {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0)
                throw new TimeoutException("No time remained after matching the occupancy");
            List<String> paths = recording.videoPaths().get(remaining, TimeUnit.NANOSECONDS);
            synchronized (lock) {
                if (lifecycleEpoch != expectedEpoch) {
                    checkedOut.remove(recording);
                    recording.invalidate();
                    return null;
                }
            }
            List<String> validated = new ArrayList<>();
            if (paths != null) {
                for (String path : paths) {
                    if (path != null && !path.isBlank())
                        validated.add(path.trim());
                }
            }
            return new Resolution(recording, List.copyOf(validated), expectedEpoch);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for video files for occupancy generation {}",
                    recording.generation());
        } catch (TimeoutException e) {
            logger.error("Timed out waiting for video files for occupancy generation {}",
                    recording.generation());
        } catch (ExecutionException e) {
            logger.error("Failed to obtain video files for occupancy generation {}",
                    recording.generation(), e.getCause() == null ? e : e.getCause());
        }
        abandon(recording);
        return null;
    }

    /** Caller must serialize this with the owner lifecycle lock before appending the paths. */
    boolean complete(Resolution resolution, boolean publish) {
        if (resolution == null)
            return false;
        boolean valid;
        synchronized (lock) {
            valid = lifecycleEpoch == resolution.lifecycleEpoch
                    && checkedOut.remove(resolution.recording);
        }
        if (valid && publish && !resolution.paths.isEmpty()
                && resolution.recording.markPublished())
            return true;
        resolution.recording.invalidate();
        return false;
    }

    private CameraRecordingCoordinator.Recording takeMatching(
            Occupancy occupancy, long deadline, long expectedEpoch) {
        synchronized (lock) {
            while (true) {
                if (lifecycleEpoch != expectedEpoch)
                    return null;
                CameraRecordingCoordinator.Recording match = findExactMatch(occupancy);
                if (match != null) {
                    pending.remove(match);
                    checkedOut.add(match);
                    return match;
                }
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    logger.warn("No camera recording generation exactly matched occupancy start {}",
                            occupancy.getStartTime());
                    return null;
                }
                try {
                    long millis = TimeUnit.NANOSECONDS.toMillis(remaining);
                    int nanos = (int) (remaining - TimeUnit.MILLISECONDS.toNanos(millis));
                    lock.wait(millis, nanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }

    private CameraRecordingCoordinator.Recording findExactMatch(Occupancy occupancy) {
        if (pending.isEmpty())
            return null;
        double occupancyStart = occupancy.getStartTime();
        if (!Double.isFinite(occupancyStart) || occupancyStart <= 0.0)
            return null;
        // All supported RPM occupancy records are millisecond-quantized. Converting their double
        // epoch seconds through nanoseconds can land one millisecond low because values such as
        // .100 are not exact in IEEE-754. Round at the source precision instead.
        long requestedStartMillis = Math.round(occupancyStart * 1_000.0);
        return pending.stream()
                .filter(recording -> recording.occupancyStartedAt().toEpochMilli()
                        == requestedStartMillis)
                .findFirst()
                .orElse(null);
    }

    void clear() {
        List<CameraRecordingCoordinator.Recording> invalidated;
        synchronized (lock) {
            lifecycleEpoch++;
            invalidated = new ArrayList<>(pending.size() + checkedOut.size());
            invalidated.addAll(pending);
            invalidated.addAll(checkedOut);
            pending.clear();
            checkedOut.clear();
            lock.notifyAll();
        }
        invalidated.forEach(CameraRecordingCoordinator.Recording::invalidate);
    }

    int pendingCount() {
        synchronized (lock) {
            return pending.size() + checkedOut.size();
        }
    }

    private void abandon(CameraRecordingCoordinator.Recording recording) {
        synchronized (lock) {
            checkedOut.remove(recording);
        }
        recording.invalidate();
    }

}
