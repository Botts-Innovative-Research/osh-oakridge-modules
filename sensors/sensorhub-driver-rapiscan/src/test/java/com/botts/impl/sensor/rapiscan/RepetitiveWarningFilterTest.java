package com.botts.impl.sensor.rapiscan;

import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepetitiveWarningFilterTest {
    @Test
    public void rateLimitsOnlyExpectedFallbackWarnings() {
        RepetitiveWarningFilter filter = new RepetitiveWarningFilter(null);

        assertTrue(filter.isLoggable(record("No setup information found")));
        assertFalse(filter.isLoggable(record("No setup information found")));
        assertTrue(filter.isLoggable(record("Unexpected setup failure")));
    }

    @Test
    public void preservesAnExistingFilter() {
        RepetitiveWarningFilter filter = new RepetitiveWarningFilter(record -> false);

        assertFalse(filter.isLoggable(record("An otherwise loggable warning")));
    }

    private static LogRecord record(String message) {
        return new LogRecord(Level.WARNING, message);
    }
}
