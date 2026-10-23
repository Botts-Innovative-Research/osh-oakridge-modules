package org.sensorhub.impl.sensor.ffmpeg;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * Opt-in base class for connection tests that require a physical or network camera.
 * Self-contained file and emulator tests continue to extend {@link ConnectionTest} directly.
 */
public abstract class ExternalCameraConnectionTest extends ConnectionTest {

    private static final String RUN_HARDWARE_TESTS_ENV = "OSCAR_RUN_CAMERA_HARDWARE_TESTS";
    private static final String HARDWARE_TIMEOUT_ENV = "OSCAR_EXTERNAL_HARDWARE_TEST_TIMEOUT_MS";
    private static final long DEFAULT_HARDWARE_TIMEOUT_MS = 600_000L;

    @Rule
    public final Timeout externalHardwareTimeout =
            new Timeout(hardwareTimeoutMillis(), TimeUnit.MILLISECONDS);

    @Before
    @Override
    public void init() throws Exception {
        org.junit.Assume.assumeTrue(
                "Requires explicitly enabled external Axis/Sony camera hardware; see the repository README",
                Boolean.parseBoolean(System.getenv(RUN_HARDWARE_TESTS_ENV)));
        super.init();
    }

    private static long hardwareTimeoutMillis() {
        String configured = System.getenv(HARDWARE_TIMEOUT_ENV);
        if (configured == null || configured.isBlank()) {
            return DEFAULT_HARDWARE_TIMEOUT_MS;
        }

        try {
            long timeout = Long.parseLong(configured);
            if (timeout <= 0) {
                throw new IllegalArgumentException(HARDWARE_TIMEOUT_ENV + " must be greater than zero");
            }
            return timeout;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(HARDWARE_TIMEOUT_ENV + " must be a positive integer", e);
        }
    }
}
