import com.botts.impl.sensor.aspect.AspectConfig;
import com.botts.impl.sensor.aspect.AspectDescriptor;
import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.comm.ModbusTCPCommProviderConfig;
import com.botts.impl.sensor.aspect.comm.ModbusTCPConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;

public class TestAspectSensor {

    private static final String REMOTE_HOST = "192.168.1.211";
    private static final int REMOTE_PORT = 502;
    AspectConfig config;
    ISensorHub hub;
    ModuleRegistry reg;

    @Before
    public void setup() throws SensorHubException {
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
