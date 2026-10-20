package com.botts.impl.sensor.rapiscan;

import gov.llnl.ernie.vm250.ErnieVM250Package;
import gov.llnl.ernie.vm250.VM250MotionProfiler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Rate-limits expected per-scan fallback warnings without hiding other EML diagnostics. */
final class RepetitiveWarningFilter implements Filter {
    private static final long INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(10);
    private static final String MISSING_SETUP = "No setup information found";
    private static final String MISSING_VELOCITY = "No velocity reading for scan, assuming ";
    private static final String UNRELIABLE_VELOCITY = "No reliable velocity reading for scan, assuming ";

    private final Filter delegate;
    private final AtomicLong nextSetupAllowedNanos = new AtomicLong();
    private final AtomicLong nextVelocityAllowedNanos = new AtomicLong();

    RepetitiveWarningFilter(Filter delegate) {
        this.delegate = delegate;
    }

    static void install() {
        install(ErnieVM250Package.LOGGER);
        install(Logger.getLogger(VM250MotionProfiler.class.getName()));
        installHandlers(Logger.getLogger(""));
    }

    private static void install(Logger logger) {
        synchronized (logger) {
            Filter current = logger.getFilter();
            if (!(current instanceof RepetitiveWarningFilter)) {
                logger.setFilter(new RepetitiveWarningFilter(current));
            }
        }
    }

    /**
     * Also protect the active JUL-to-SLF4J bridge. Some EML initialization paths
     * replace a named logger's filter after a lane starts; the root handler is
     * the final JUL boundary and therefore keeps the rate limit effective.
     */
    static void installHandlers(Logger logger) {
        for (Handler handler : logger.getHandlers()) {
            synchronized (handler) {
                Filter current = handler.getFilter();
                if (!(current instanceof RepetitiveWarningFilter)) {
                    handler.setFilter(new RepetitiveWarningFilter(current));
                }
            }
        }
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        if (delegate != null && !delegate.isLoggable(record)) {
            return false;
        }
        String message = record.getMessage();
        AtomicLong nextAllowedNanos;
        if (MISSING_SETUP.equals(message)) {
            nextAllowedNanos = nextSetupAllowedNanos;
        } else if (message != null && (message.startsWith(MISSING_VELOCITY)
                || message.startsWith(UNRELIABLE_VELOCITY))) {
            nextAllowedNanos = nextVelocityAllowedNanos;
        } else {
            return true;
        }

        long now = System.nanoTime();
        long next = nextAllowedNanos.get();
        return now >= next && nextAllowedNanos.compareAndSet(next, now + INTERVAL_NANOS);
    }
}
