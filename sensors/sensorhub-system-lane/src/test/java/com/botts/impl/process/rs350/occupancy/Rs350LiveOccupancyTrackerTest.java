package com.botts.impl.process.rs350.occupancy;

import org.junit.Test;

import static org.junit.Assert.*;

public class Rs350LiveOccupancyTrackerTest {

    private static final long START_NANOS = 10_000_000_000L;

    @Test
    public void foregroundIsNonAlarmingLiveOccupancyWithStableStart() {
        var tracker = new Rs350LiveOccupancyTracker();

        assertFalse(tracker.isKnown());
        assertFalse(tracker.isOccupied());

        tracker.onForeground(1_795_000_000.125, START_NANOS);
        assertTrue(tracker.isKnown());
        assertTrue(tracker.isOccupied());
        assertEquals(1_795_000_000.125, tracker.getOccupancyStartTime(), 0.000_001);

        tracker.onForeground(1_795_000_001.125, START_NANOS + 1_000_000_000L);
        assertTrue(tracker.isOccupied());
        assertEquals("Later foreground samples must not move the occupancy start",
                1_795_000_000.125, tracker.getOccupancyStartTime(), 0.000_001);
    }

    @Test
    public void backgroundOnlyBecomesKnownClearAfterDebounce() {
        var tracker = new Rs350LiveOccupancyTracker();

        tracker.onBackground(START_NANOS);
        tracker.onBackground(START_NANOS + 1_000_000_000L);
        assertFalse(tracker.isKnown());
        assertFalse(tracker.expire(START_NANOS + Rs350LiveOccupancyTracker.BACKGROUND_DEBOUNCE_NANOS - 1));
        assertTrue(tracker.expire(START_NANOS + Rs350LiveOccupancyTracker.BACKGROUND_DEBOUNCE_NANOS));
        assertTrue(tracker.isKnown());
        assertFalse(tracker.isOccupied());
        assertEquals(0.0, tracker.getOccupancyStartTime(), 0.0);
    }

    @Test
    public void immediateForegroundCancelsBackgroundClearFromCombinedReport() {
        var tracker = new Rs350LiveOccupancyTracker();
        tracker.onForeground(100.0, START_NANOS);

        long combinedReportTime = START_NANOS + 1_000_000_000L;
        tracker.onBackground(combinedReportTime);
        tracker.onForeground(101.0, combinedReportTime + 1_000_000L);

        assertFalse(tracker.expire(combinedReportTime + Rs350LiveOccupancyTracker.BACKGROUND_DEBOUNCE_NANOS));
        assertTrue(tracker.isOccupied());
        assertEquals("A combined background/foreground report must not reset the start",
                100.0, tracker.getOccupancyStartTime(), 0.0);
    }

    @Test
    public void missedForegroundHeartbeatsCloseOccupancyAfterThreeSeconds() {
        var tracker = new Rs350LiveOccupancyTracker();
        tracker.onForeground(100.0, START_NANOS);

        long heartbeatTime = START_NANOS + 1_000_000_000L;
        tracker.onForeground(101.0, heartbeatTime);
        assertFalse(tracker.expire(heartbeatTime + Rs350LiveOccupancyTracker.FOREGROUND_INACTIVITY_NANOS - 1));
        assertTrue(tracker.expire(heartbeatTime + Rs350LiveOccupancyTracker.FOREGROUND_INACTIVITY_NANOS));
        assertFalse(tracker.isOccupied());
        assertEquals(0.0, tracker.getOccupancyStartTime(), 0.0);

        tracker.onForeground(200.0, heartbeatTime + Rs350LiveOccupancyTracker.FOREGROUND_INACTIVITY_NANOS + 1);
        assertTrue(tracker.isOccupied());
        assertEquals(200.0, tracker.getOccupancyStartTime(), 0.0);
    }
}
