import com.botts.impl.sensor.aspect.AspectConfig;
import com.botts.impl.sensor.aspect.AspectDescriptor;
import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.comm.ModbusTCPCommProviderConfig;
import com.botts.impl.sensor.aspect.comm.ModbusTCPConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;

public class TestAspectSensor {

    private static final String RUN_HARDWARE_TESTS_ENV = "OSCAR_RUN_RPM_HARDWARE_TESTS";
    private static final String HARDWARE_TIMEOUT_ENV = "OSCAR_EXTERNAL_HARDWARE_TEST_TIMEOUT_MS";
    private static final long DEFAULT_HARDWARE_TIMEOUT_MS = 600_000L;

    @Rule
    public final Timeout externalHardwareTimeout =
            new Timeout(hardwareTimeoutMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

    private static final String REMOTE_HOST = System.getenv("RPM_HOST");//"192.168.1.211";
    private static final int REMOTE_PORT = 502;
    AspectConfig config;
    ISensorHub hub;
    ModuleRegistry reg;

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

    @Before
    public void setup() throws SensorHubException {
        org.junit.Assume.assumeTrue(
                "Requires an explicitly enabled external Aspect RPM; see the repository README",
                Boolean.parseBoolean(System.getenv(RUN_HARDWARE_TESTS_ENV)));
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();


        config = (AspectConfig) reg.createModuleConfig(new AspectDescriptor());
        config.commSettings = new ModbusTCPCommProviderConfig();
        var commConfig = new ModbusTCPConfig();
        commConfig.addressRange = new ModbusTCPConfig.AddressRange();
        commConfig.addressRange.from = 1;
        commConfig.addressRange.to = 4;
        commConfig.remoteHost = REMOTE_HOST;
        commConfig.remotePort = REMOTE_PORT;
        config.commSettings.protocol = commConfig;
    }

    @Test
    public void testConnectAndStart() throws SensorHubException {
        AspectSensor sensor = (AspectSensor) reg.loadModule(config);
        reg.startModule(sensor.getLocalID());
        boolean isStarted = sensor.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        Assert.assertTrue(isStarted);
    }

}
