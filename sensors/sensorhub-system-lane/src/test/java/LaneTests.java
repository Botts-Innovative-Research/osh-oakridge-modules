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

import com.botts.impl.system.lane.AdjudicationControl;
import com.botts.impl.system.lane.Descriptor;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.*;
import com.botts.impl.system.lane.helpers.occupancy.OccupancyStatusOutput;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.BigIdLong;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.database.system.SystemDriverDatabaseConfig;
import org.sensorhub.impl.database.system.SystemDriverDatabaseDescriptor;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseDescriptor;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockString;
import org.vast.sensorML.SMLHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Alex Almanza
 * @since May 01, 2025
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LaneTests {
    private static final Logger log = LoggerFactory.getLogger(LaneTests.class);
    private static final String RUN_EXTERNAL_TESTS_ENV = "OSCAR_RUN_RPM_HARDWARE_TESTS";
    private static final String HARDWARE_TIMEOUT_ENV = "OSCAR_EXTERNAL_HARDWARE_TEST_TIMEOUT_MS";
    private static final long DEFAULT_HARDWARE_TIMEOUT_MS = 600_000L;
    private static final String RPM_HOST_ENV = "RPM_HOST";

    @Rule
    public final Timeout externalHardwareTimeout =
            new Timeout(hardwareTimeoutMillis(), TimeUnit.MILLISECONDS);
    // Constants
    private String RPM_UID = "rpm1";
    private String RPM_NAME = "My RPM";
    private String LANE_UID = "1";
    private String LANE_NAME = "Test Lane";

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

        // RPM integration tests do not need a camera. Keeping this empty also
        // prevents the tests from depending on a public MJPEG endpoint.
        config.laneOptionsConfig.ffmpegConfig = new ArrayList<>();

        return config;
    }

    private static void requireExternalIntegration() {
        Assume.assumeTrue(
                "Requires an explicitly configured RPM integration environment; see the lane module README",
                Boolean.parseBoolean(System.getenv(RUN_EXTERNAL_TESTS_ENV)));
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

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank())
            throw new IllegalStateException(name + " must be set when live lane integration tests are enabled");
        return value;
    }

    private LaneSystem testLoadAndStart(LaneConfig config) throws SensorHubException {
        LaneSystem lane = (LaneSystem) reg.loadModule(config);
        reg.startModule(lane.getLocalID());
        boolean isStarted = lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        Assert.assertTrue(isStarted);
        return lane;
    }

    @Test
    public void testOperationalViewKeywordsArePublished() throws SensorHubException {
        LaneConfig config = (LaneConfig) reg.createModuleConfig(new Descriptor());
        config.name = "View Lane";
        config.uniqueID = "view-test";
        config.operationalViewKeys = List.of("secondary", "north-gate", "north-gate");

        LaneSystem lane = (LaneSystem) reg.loadModule(config);
        lane.init();

        List<String> keywords = new ArrayList<>();
        for (var keywordList : lane.getCurrentDescription().getKeywordsList())
            keywords.addAll(keywordList.getKeywordList());

        assertEquals(List.of("north-gate", "secondary"), config.operationalViewKeys);
        assertTrue(keywords.contains("oscar:view:north-gate"));
        assertTrue(keywords.contains("oscar:view:secondary"));
        assertSame(lane.getOccupancyStatusOutput(), lane.getOutputs().get(OccupancyStatusOutput.NAME));
    }

    @Test
    public void testInvalidOperationalViewKeyIsRejected() throws SensorHubException {
        LaneConfig config = (LaneConfig) reg.createModuleConfig(new Descriptor());
        config.name = "Bad View";
        config.uniqueID = "bad-view-test";
        config.operationalViewKeys = List.of("North Gate");

        LaneSystem lane = (LaneSystem) reg.loadModule(config);
        lane.init();
        assertEquals(ModuleEvent.ModuleState.LOADED, lane.getCurrentState());
        assertNotNull(lane.getCurrentError());
        assertNotNull(lane.getCurrentError().getCause());
        assertTrue(lane.getCurrentError().getCause().getMessage().contains("Operational view keys"));
    }

    @Test
    public void testSingleRapiscan() throws SensorHubException {
        requireExternalIntegration();
        var config = createLaneConfig(true, requiredEnvironment(RPM_HOST_ENV), RAPISCAN_PORT);
        testLoadAndStart(config);
    }

    @Test
    public void testSingleAspect() throws SensorHubException {
        requireExternalIntegration();
        var config = createLaneConfig(false, requiredEnvironment(RPM_HOST_ENV), ASPECT_PORT);
        testLoadAndStart(config);
    }

    @Test
    public void test25RapiscanEML() throws SensorHubException {
        requireExternalIntegration();
        String rpmHost = requiredEnvironment(RPM_HOST_ENV);

        for (int i = 1; i < 25; i++) {
            int port = 1600 + i;
            var config = createLaneConfig(true, rpmHost, port);

            ((RapiscanRPMConfig) config.laneOptionsConfig.rpmConfig).emlConfig = new RapiscanRPMConfig.EMLConfig();
            ((RapiscanRPMConfig) config.laneOptionsConfig.rpmConfig).emlConfig.emlEnabled = true;
            testLoadAndStart(config);
        }
    }

    @Test
    public void test25Rapiscan() throws SensorHubException {
        requireExternalIntegration();
        String rpmHost = requiredEnvironment(RPM_HOST_ENV);

        for (int i = 1; i < 25; i++) {
            int port = 1600 + i;
            var config = createLaneConfig(true, rpmHost, port);
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

    private BigId addOccupancy(SystemDriverDatabase database, LaneSystem lane) throws Exception {
        long start = Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli();
        long end = Instant.now().toEpochMilli();

        var process = new SMLHelper().createPhysicalComponent()
                .uniqueID(lane.getUniqueIdentifier())
                .name(lane.getName())
                .build();
        BigId systemId = database.getSystemDescStore().add(new SystemWrapper(process)).getInternalID();

        var occupancyOutput = new OccupancyOutput<>(new SensorSystem());
        var dataStream = new DataStreamInfo.Builder()
                .withName(OccupancyOutput.NAME)
                .withSystem(new FeatureId(systemId, lane.getUniqueIdentifier()))
                .withRecordDescription(occupancyOutput.getRecordDescription())
                .withRecordEncoding(occupancyOutput.getRecommendedEncoding())
                .build();
        BigId dataStreamId = database.getObservationStore().getDataStreams().add(dataStream).getInternalID();

        var occ = new Occupancy.Builder()
                .samplingTime(end / 1000d)
                .occupancyCount(1)
                .startTime(start/1000d)
                .endTime(end/1000d)
                .neutronBackground(100)
                .gammaAlarm(true)
                .neutronAlarm(true)
                .maxGammaCount(1000)
                .maxNeutronCount(1000)
                .build();

        return database.getObservationStore().add(new ObsData.Builder()
                .withDataStream(dataStreamId)
                .withPhenomenonTime(Instant.ofEpochMilli(end))
                .withResult(Occupancy.fromOccupancy(occ))
                .build());
    }

    @Test
    public void testAdjudication() throws Exception {
        // Adjudication logic does not require physical hardware. Initialize a
        // lane without devices and insert a real occupancy into the local H2
        // system-driver database so the default suite keeps this coverage.
        var config = (LaneConfig) reg.createModuleConfig(new Descriptor());
        config.uniqueID = "adjudication-test";
        config.name = "Adj Lane";
        var lane = (LaneSystem) reg.loadModule(config);
        lane.init();

        var database = loadAndStartDB();
        assertTrue(database.waitForState(ModuleEvent.ModuleState.STARTED, 10_000));
        var obsId = addOccupancy(database, lane);

        var obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();
        var cmdIdEncoder = hub.getIdEncoders().getCommandIdEncoder();
        var obsStore = database.getObservationStore();
        var encodedObsId = obsIdEncoder.encodeID(obsId);

        var control = (AdjudicationControl) lane.getCommandInputs().get(AdjudicationControl.NAME);
        control.setObsStore(obsStore);
        var adjData = Adjudication.fromAdjudication(new Adjudication.Builder()
                .feedback("hey where did u get that")
                .adjudicationCode(0)
                .isotopes(List.of("Xenon", "Uranium-238"))
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.NONE)
                .filePaths(List.of("test.txt", "hello.ts", "hello.p", "f.p", "ruby.sheila"))
                .occupancyObsId(encodedObsId)
                .vehicleId("ABC123")
                .build());
        var cmd = new CommandData.Builder()
                .withId(new BigIdLong(1, 1))
                .withParams(adjData)
                .withCommandStream(new BigIdLong(1, 2))
                .withSender("Alex")
                .build();
        var res = control.submitCommand(cmd).get();
        System.out.println("Status: " + res.getStatusCode());
        System.out.println("Message: " + res.getMessage());
        assertNull(res.getMessage());
        assertEquals(res.getStatusCode(), ICommandStatus.CommandStatusCode.COMPLETED);
        // Adjudication ID got added to occupancy observation
        assertEquals(cmdIdEncoder.encodeID(cmd.getID()), obsStore.get(obsId).getResult().getStringValue(10));
        // TODO: add more assertions

        assertNotNull(res.getResult().getInlineRecords());
        var data = res.getResult().getInlineRecords().stream().toList().get(0);
        for (int i = 0; i < data.getAtomCount(); i++) {
            try {
                var obj = ((DataBlockMixed) data).getUnderlyingObject()[i];
                int objAtomCount = obj.getAtomCount();
                if (objAtomCount > 1) {
                    for (int j = 0; j < objAtomCount; j++) {
                        System.out.println(((DataBlockString) obj).getUnderlyingObject()[j].toString());
                    }
                } else {
                    System.out.println(obj.getStringValue());
                }
            } catch (Exception e) {
                break;
            }
        }

        System.out.println("-------------------------------------------------");

        // check command id is inserted correctly into occupancy record

        var adjData2 = Adjudication.fromAdjudication(new Adjudication.Builder()
                .feedback("second adjudication feedback")
                .adjudicationCode(2)
                .isotopes(List.of("Cobalt-60"))
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.REQUESTED)
                .filePaths(List.of("report2.txt"))
                .occupancyObsId(encodedObsId)
                .vehicleId("XYZ789")
                .build());

        var cmd2 = new CommandData.Builder()
                .withId(new BigIdLong(1, 4))
                .withParams(adjData2)
                .withCommandStream(new BigIdLong(1, 3))
                .withSender("Kalyn")
                .build();

        var res2 = control.submitCommand(cmd2).get();

        System.out.println("Status 2: " + res2.getStatusCode());
        System.out.println("Message 2: " + res2.getMessage());
        assertNull(res2.getMessage());
        assertEquals(res2.getStatusCode(), ICommandStatus.CommandStatusCode.COMPLETED);

        BigId decodedObsId = obsIdEncoder.decodeID(encodedObsId);
        var obs = obsStore.get(decodedObsId);

        int numAdjs = obs.getResult().getIntValue(9);
        System.out.println("Adjudication IDs");
        for (int i = 0; i < numAdjs; i++) {
            var id = obs.getResult().getStringValue(10+i);
            assertNotNull(id);
            assertFalse(id.isBlank());
            if (i == 0)
                assertEquals(cmdIdEncoder.encodeID(cmd.getID()), id);
            else if (i == 1)
                assertEquals(cmdIdEncoder.encodeID(cmd2.getID()), id);
            else if (i == obs.getResult().getAtomCount())
                fail("Index out of bounds! This will not serialize properly");
            System.out.println(id);
        }
    }


    @After
    public void cleanup() throws IOException {
        if (hub != null)
            hub.stop();
        Files.deleteIfExists(Path.of("test.dat"));
    }

}
