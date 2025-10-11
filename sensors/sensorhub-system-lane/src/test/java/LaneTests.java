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
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.system.lane.AdjudicationControl;
import com.botts.impl.system.lane.Descriptor;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.*;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigIdLong;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.database.system.SystemDriverDatabaseConfig;
import org.sensorhub.impl.database.system.SystemDriverDatabaseDescriptor;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabase;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseDescriptor;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

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

    @Before
    public void setUp() throws Exception {
        Files.deleteIfExists(Path.of("test.dat"));
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

    private LaneSystem testLoadAndStart(LaneConfig config) throws SensorHubException {
        LaneSystem lane = (LaneSystem) reg.loadModule(config);
        reg.startModule(lane.getLocalID());
        boolean isStarted = lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        Assert.assertTrue(isStarted);
        return lane;
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

    // Adjudication

    private SystemDriverDatabase loadAndStartDB() throws SensorHubException {
        var config = (SystemDriverDatabaseConfig) reg.createModuleConfig(new SystemDriverDatabaseDescriptor());
        var dbConfig = (MVObsSystemDatabaseConfig) reg.createModuleConfig(new MVObsSystemDatabaseDescriptor());
        dbConfig.storagePath = "test.dat";
        config.systemUIDs = Set.of("*");
        config.dbConfig = dbConfig;
        config.autoStart = true;
        config.databaseNum = 2;
        return (SystemDriverDatabase) reg.loadModule(config);
    }

    private void generateOccupancy(LaneSystem lane) throws SensorHubException {
        long start = Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli();
        long end = Instant.now().toEpochMilli();

        AtomicReference<OccupancyOutput> occOutput = new AtomicReference<>();
        lane.getMembers().values().forEach((member) -> {
            if (member.getOutputs().containsKey(OccupancyOutput.NAME))
                occOutput.set((OccupancyOutput) member.getOutputs().get(OccupancyOutput.NAME));
        });

        var occ = new Occupancy.Builder()
                .occupancyCount(1)
                .startTime(start/1000d)
                .endTime(end/1000d)
                .neutronBackground(100)
                .gammaAlarm(true)
                .neutronAlarm(true)
                .maxGammaCount(1000)
                .maxNeutronCount(1000)
                .build();

        occOutput.get().setData(occ);
    }

    @Test
    public void testAdjudication() throws ExecutionException, InterruptedException, SensorHubException {
        // Start database
        var database = loadAndStartDB();

        // Start lane
        var config = createLaneConfig(true, RPM_HOST, RAPISCAN_PORT);
        var lane = testLoadAndStart(config);

        // Gen occ
        generateOccupancy(lane);

        var obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();
        var cmdIdEncoder = hub.getIdEncoders().getCommandIdEncoder();
        var obsStore = hub.getDatabaseRegistry().getFederatedDatabase().getObservationStore();
        var filter = obsStore.selectAllFilter();
        var obsId = obsStore.selectKeys(filter).toList().get(0);
        var encodedObsId = obsIdEncoder.encodeID(obsId);

        var control = (AdjudicationControl) lane.getCommandInputs().get(AdjudicationControl.NAME);
        var adjData = Adjudication.fromAdjudication(new Adjudication.Builder()
                .feedback("hey where did u get that")
                .adjudicationCode(3)
                .isotopes("U-235,U-238")
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.NONE)
                .filePaths("")
                .occupancyId(encodedObsId)
                .vehicleId("ABC123")
                .build());
        var cmd = new CommandData.Builder()
                .withId(new BigIdLong(1, 1))
                .withParams(adjData)
                .withCommandStream(new BigIdLong(1, 2))
                .build();
        var res = control.submitCommand(cmd).get();
        System.out.println(res.getStatusCode());
        System.out.println(res.getMessage());
        assertEquals(res.getStatusCode(), ICommandStatus.CommandStatusCode.ACCEPTED);
        // Adjudication ID got added to occupancy observation
        assertEquals(cmdIdEncoder.encodeID(cmd.getID()), obsStore.get(obsId).getResult().getStringValue(10));
        // TODO: add more assertions
    }


    @After
    public void cleanup() throws IOException {
        if (hub != null)
            hub.stop();
        Files.deleteIfExists(Path.of("test.dat"));
    }

}
