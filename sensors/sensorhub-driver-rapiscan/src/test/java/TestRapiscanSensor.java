import com.botts.impl.sensor.rapiscan.EMLConfig;
import com.botts.impl.sensor.rapiscan.RapiscanConfig;
import com.botts.impl.sensor.rapiscan.RapiscanDescriptor;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import gov.llnl.ernie.api.ERNIE_lane;
import gov.llnl.ernie.api.Results;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.comm.TCPCommProviderConfig;
import org.sensorhub.impl.comm.TCPConfig;
import org.sensorhub.impl.module.ModuleRegistry;

public class TestRapiscanSensor {

    private static final String REMOTE_HOST = System.getenv("RPM_HOST");//"192.168.1.211";
    private static final int REMOTE_PORT = 1601;
    ISensorHub hub;
    ModuleRegistry reg;

    @Before
    public void setup() throws SensorHubException {
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();
    }

    private RapiscanConfig createConfig(String host, int port) throws SensorHubException {
        RapiscanConfig config = (RapiscanConfig) reg.createModuleConfig(new RapiscanDescriptor());
        config.commSettings = new TCPCommProviderConfig();
        var commConfig = new TCPConfig();
        commConfig.remoteHost = host;
        commConfig.remotePort = port;
        config.commSettings.protocol = commConfig;
        return config;
    }

    private void testLoadAndStart(RapiscanConfig config) throws SensorHubException {
        RapiscanSensor sensor = (RapiscanSensor) reg.loadModule(config);
        reg.startModule(sensor.getLocalID());
        boolean isStarted = sensor.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        Assert.assertTrue(isStarted);
    }

    @Test
    public void testSingleRPM() throws SensorHubException {
        var config = createConfig(REMOTE_HOST, REMOTE_PORT);
        testLoadAndStart(config);
    }

    @Test
    public void test25RPMsConnectedWithEML() throws SensorHubException {
        for (int i = 1; i < 25; i++) {
            int port = 1600 + i;
            var config = createConfig(REMOTE_HOST, port);
            config.serialNumber = String.valueOf(port);
            config.emlConfig = new EMLConfig();
            config.emlConfig.emlEnabled = true;
            testLoadAndStart(config);
        }
    }

    @Test
    public void test25RPMsConnectedNoEML() throws SensorHubException {
        for (int i = 1; i < 25; i++) {
            int port = 1600 + i;
            var config = createConfig(REMOTE_HOST, port);
            config.serialNumber = String.valueOf(port);
            testLoadAndStart(config);
        }
    }

    @After
    public void cleanup() {
        if (hub != null)
            hub.stop();
    }

}
