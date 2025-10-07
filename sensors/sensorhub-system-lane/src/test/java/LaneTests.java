/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.
 ******************************************************************************/

import com.botts.impl.sensor.rapiscan.EMLConfig;
import com.botts.impl.system.lane.Descriptor;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.*;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alex Almanza
 * @since May 01 2025
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LaneTests {
    private static final Logger log = LoggerFactory.getLogger(LaneTests.class);
    // Constants
    private String RPM_UID = "rpm1";
    private String RPM_NAME = "My RPM";
    private String LANE_UID = "1";
    private String LANE_NAME = "Test Lane";

    private static final String RPM_HOST = System.getenv("RPM_HOST");//"192.168.1.211";
    private static final int RAPISCAN_PORT = 1601;
    private static final int ASPECT_PORT = 502;

    // Used for tests
    private SensorHub hub;
    private ModuleRegistry reg;
    private LaneSystem lane;
    private List<LaneSystem> multipleLanes;

    @Before
    public void setUp() throws Exception {
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();
    }

    private LaneConfig createLaneConfig(boolean isRapiscan, String rpmHost, int rpmPort) throws SensorHubException {
        LaneConfig config = (LaneConfig) reg.createModuleConfig(new Descriptor());
        config.laneOptionsConfig = new LaneOptionsConfig();
        config.laneOptionsConfig.rpmConfig = isRapiscan ? new RapiscanRPMConfig() : new AspectRPMConfig();
        config.laneOptionsConfig.rpmConfig.remoteHost = rpmHost;
        config.laneOptionsConfig.rpmConfig.remotePort = rpmPort;
        config.uniqueID = rpmHost + rpmPort;
        config.name = "Lane " + rpmPort;

        List<FFMpegConfig> ffmpegList = new ArrayList();
        ffmpegList.add(new CustomCameraConfig());

//        http://66.27.116.187/mjpg/video.mjpg
        config.laneOptionsConfig.ffmpegConfig = ffmpegList;
        config.laneOptionsConfig.ffmpegConfig.get(0).remoteHost = "66.27.116.187";
        ((CustomCameraConfig) config.laneOptionsConfig.ffmpegConfig.get(0)).streamPath = "/mjpg/video.mjpg";

        return config;
    }

    private void testLoadAndStart(LaneConfig config) throws SensorHubException {
        LaneSystem lane = (LaneSystem) reg.loadModule(config);
        reg.startModule(lane.getLocalID());
        boolean isStarted = lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        Assert.assertTrue(isStarted);
    }

    @Test
    public void testSingleRapiscan() throws SensorHubException {
        var config = createLaneConfig(true, RPM_HOST, RAPISCAN_PORT);
        testLoadAndStart(config);
    }

    @Test
    public void testSingleAspect() throws SensorHubException {
        var config = createLaneConfig(false, RPM_HOST, ASPECT_PORT);
        testLoadAndStart(config);
    }

    @Test
    public void test25RapiscanEML() throws SensorHubException {

        for (int i = 1; i < 25; i++) {
            int port = 1600 + i;
            var config = createLaneConfig(true, RPM_HOST, port);

            ((RapiscanRPMConfig) config.laneOptionsConfig.rpmConfig).emlConfig = new RapiscanRPMConfig.EMLConfig();
            ((RapiscanRPMConfig) config.laneOptionsConfig.rpmConfig).emlConfig.emlEnabled = true;
            testLoadAndStart(config);
        }
    }

    @Test
    public void test25Rapiscan() throws SensorHubException {
        for (int i = 1; i < 25; i++) {
            int port = 1600 + i;
            var config = createLaneConfig(true, RPM_HOST, port);
            testLoadAndStart(config);
        }
    }


    @After
    public void cleanup() {
        if (hub != null)
            hub.stop();
    }

}
