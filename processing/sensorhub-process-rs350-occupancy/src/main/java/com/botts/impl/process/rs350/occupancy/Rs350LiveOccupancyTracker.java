package com.botts.impl.process.rs350.occupancy;

import java.util.concurrent.TimeUnit;

/**
 * Tracks the RS350's live measurement class independently from its alarm
 * state. Foreground reports refresh a live vehicle-activity lease; background
 * reports are exit candidates because a completed N42 event can contain both
 * measurement classes.
 */
final class Rs350LiveOccupancyTracker {

    /*
     * The RS350 normally emits foreground reports about once per second while
     * a vehicle is present.  A completed event can contain both a background
     * and a foreground measurement, and those measurements are delivered on
     * separate event-bus topics.  Treating the background report as an
     * immediate exit therefore creates a false clear/occupied pair and resets
     * the extended-occupancy clock.
     */
    static final long BACKGROUND_DEBOUNCE_NANOS = TimeUnit.SECONDS.toNanos(2);
    static final long FOREGROUND_INACTIVITY_NANOS = TimeUnit.SECONDS.toNanos(3);
    static final long NO_DEADLINE = Long.MAX_VALUE;

    private boolean occupied;
    private double occupancyStartTime;
    private boolean known;
    private long backgroundDeadlineNanos = NO_DEADLINE;
    private long foregroundDeadlineNanos = NO_DEADLINE;

    boolean onForeground(double measurementStartTime) {
        return onForeground(measurementStartTime, System.nanoTime());
    }

    boolean onForeground(double measurementStartTime, long nowNanos) {
        boolean changed = !known || !occupied;
        if (!occupied)
            occupancyStartTime = measurementStartTime;
        occupied = true;
        known = true;
        backgroundDeadlineNanos = NO_DEADLINE;
        foregroundDeadlineNanos = addWithoutOverflow(nowNanos, FOREGROUND_INACTIVITY_NANOS);
        return changed;
    }

    boolean onBackground() {
        return onBackground(System.nanoTime());
    }

    boolean onBackground(long nowNanos) {
        // Do not move an existing deadline forward on every background
        // heartbeat; otherwise a clear instrument that reports once per second
        // would remain Unknown forever.
        if ((!known || occupied) && backgroundDeadlineNanos == NO_DEADLINE)
            backgroundDeadlineNanos = addWithoutOverflow(nowNanos, BACKGROUND_DEBOUNCE_NANOS);
        return false;
    }

    boolean expire(long nowNanos) {
        boolean backgroundExpired = backgroundDeadlineNanos != NO_DEADLINE
                && nowNanos >= backgroundDeadlineNanos;
        boolean foregroundExpired = foregroundDeadlineNanos != NO_DEADLINE
                && nowNanos >= foregroundDeadlineNanos;
        if (!backgroundExpired && !foregroundExpired)
            return false;

        boolean changed = !known || occupied;
        occupied = false;
        occupancyStartTime = 0.0;
        known = true;
        backgroundDeadlineNanos = NO_DEADLINE;
        foregroundDeadlineNanos = NO_DEADLINE;
        return changed;
    }

    long nanosUntilNextDeadline(long nowNanos) {
        long deadline = Math.min(backgroundDeadlineNanos, foregroundDeadlineNanos);
        if (deadline == NO_DEADLINE)
            return NO_DEADLINE;
        return Math.max(0L, deadline - nowNanos);
    }

    void reset() {
        occupied = false;
        occupancyStartTime = 0.0;
        known = false;
        backgroundDeadlineNanos = NO_DEADLINE;
        foregroundDeadlineNanos = NO_DEADLINE;
    }

    boolean isKnown() {
        return known;
    }

    boolean isOccupied() {
        return occupied;
    }

    double getOccupancyStartTime() {
        return occupancyStartTime;
    }

    private static long addWithoutOverflow(long value, long increment) {
        if (value > Long.MAX_VALUE - increment)
            return Long.MAX_VALUE - 1;
        return value + increment;
    }
}
