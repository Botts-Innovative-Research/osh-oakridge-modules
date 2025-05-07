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

import com.botts.impl.process.occupancy.OccupancyProcessModule;
import com.botts.impl.system.lane.Descriptor;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.*;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.database.system.SystemDriverDatabaseConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.processing.AbstractProcessModule;
//import org.sensorhub.impl.sensor.fakeweather.FakeWeatherConfig;
//import org.sensorhub.impl.sensor.fakeweather.FakeWeatherSensor;
import org.sensorhub.utils.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * @author Alex Almanza
 * @since May 01 2025
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LaneTests {
    private static final Logger log = LoggerFactory.getLogger(LaneTests.class);
    // Constants
    private String LANE_DATABASE_ID = "lanedb1";
    private String RPM_UID = "rpm1";
    private String RPM_NAME = "My RPM";
    private String LANE_UID = "1";
    private String LANE_NAME = "Test Lane";
    private String RAPISCAN_HOST = "localhost";
    private int RAPISCAN_PORT = 1600;

    // Used for tests
    private SensorHub hub;
    private LaneSystem lane;
    private List<LaneSystem> multipleLanes;
    private SystemDriverDatabase database;
    private File dbFile;

    // For specific process test
    private Flow.Subscription processSub;
    private int numOutputsBefore;

    @Before
    public void setUp() throws Exception {
        // Create temp file to use with System Driver Database
        dbFile = File.createTempFile("testlanedb", ".dat");
        dbFile.deleteOnExit();

        hub = new SensorHub();
        hub.start();

        database = loadSystemDriverDatabase(hub);
        assertNotNull(database);
        assertNull(database.getCurrentError());
        assertEquals(database.getCurrentState(), ModuleEvent.ModuleState.STARTED);

        lane = loadLaneModule(hub, "x");
        assertNotNull(lane);
        assertNull(lane.getCurrentError());
        assertEquals(lane.getCurrentState(), ModuleEvent.ModuleState.LOADED);
        startLaneAndWaitForStarted();
        assertEquals(lane.getCurrentState(), ModuleEvent.ModuleState.STARTED);

        multipleLanes = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            multipleLanes.add(loadLaneModule(hub, "" + i));
        }

        for (LaneSystem lane : multipleLanes) {
            hub.getModuleRegistry().initModule(lane.getLocalID());
            lane.waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
            hub.getModuleRegistry().startModuleAsync(lane);
        }

        for (LaneSystem lane : multipleLanes) {
            lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        }
    }

    @Test
    public void testLaneCreatesRPMModule() throws SensorHubException {
        assertFalse(lane.getMembers().isEmpty());
    }

    @Test
    public void testMultipleLanesCreateRPMModules() throws SensorHubException {
        boolean failed = false;
        for (LaneSystem lane : multipleLanes) {
            lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        }

        for (LaneSystem lane : multipleLanes) {
            if (lane.getMembers().isEmpty())
                failed = true;
            break;
        }
        assertFalse(failed);
    }

    @Test
    public void testProcessStarted() throws SensorHubException, TimeoutException {
        AbstractProcessModule<?> processModule = hub.getModuleRegistry().getModuleByType(OccupancyProcessModule.class);
        Async.waitForCondition(() -> processModule.getCurrentState() == ModuleEvent.ModuleState.STARTED, 500, 20000);
        System.out.println("Process started");
    }

    @Test
    public void testAllProcessesStarted() throws TimeoutException {
        List<OccupancyProcessModule> processModules = hub.getModuleRegistry().getLoadedModules(OccupancyProcessModule.class).stream().toList();
        System.out.println(processModules.size());
        for (OccupancyProcessModule processModule : processModules) {
            Async.waitForCondition(() -> processModule.getCurrentState() == ModuleEvent.ModuleState.STARTED, 500, 20000);
            System.out.println("Process " + processModule.getName() + " started");
        }
        System.out.println("All processes started");
    }

    @Test
    public void testAllProcessesCreated() throws SensorHubException {
        var processModules = hub.getModuleRegistry().getLoadedModules(OccupancyProcessModule.class);
        var lanes = hub.getModuleRegistry().getLoadedModules(LaneSystem.class);
        System.out.println("Processes: " + processModules.size());
        System.out.println("Lanes: " + lanes.size());
        assertEquals(processModules.size(), lanes.size());
    }

    // NOTE: Uncomment all related simulated weather driver code if you want to run this test, or perform test with different module
//    @Test
//    public void testProcessContainsNewOutputs() throws TimeoutException, SensorHubException {
//        // Check lane is started
//        lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
//        var processes = hub.getModuleRegistry().getLoadedModules(OccupancyProcessModule.class).stream().toList();
//        OccupancyProcessModule process = processes.stream().filter(p -> p.getConfiguration().systemUID.equals(lane.getUniqueIdentifier())).findFirst().orElse(null);
//        assertNotNull(process);
//        // Check process is started
//        process.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
//        // Track number of outputs of only one lane submodule (should be ~10)
//        numOutputsBefore = process.getOutputs().size();
//        // Add submodule to lane
//        var weatherSubmodule = lane.addSubsystem(createWeatherSubmodule(hub, "1"));
//        System.out.println("Weather submodule loaded");
//        System.out.println(weatherSubmodule.getCurrentState());
//        // Wait for autostart to kick in
//        weatherSubmodule.init();
//        weatherSubmodule.waitForState(ModuleEvent.ModuleState.INITIALIZED, 10000);
//        weatherSubmodule.start();
//        var weatherStarted = weatherSubmodule.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
//        assertTrue(weatherStarted);
//        System.out.println("Weather submodule started");
//        System.out.println(weatherSubmodule.getCurrentState());
//        // Check process adds submodule's outputs
//        System.out.println("Num outputs before: " + numOutputsBefore);
//        System.out.println("Num outputs after: " + process.getOutputs().size());
//        Async.waitForCondition(() -> numOutputsBefore != process.getOutputs().size(), 20000);
//        // Check process is started with new outputs
//        System.out.println("Num outputs before: " + numOutputsBefore);
//        System.out.println("Num outputs after: " + process.getOutputs().size());
//        System.out.println(process.getCurrentState());
//        boolean isStarted = process.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
//        System.out.println("Process: " + process.getCurrentState());
//        assertTrue(isStarted);
//    }

    @Test
    public void testProcessStopsAndStartsWithLane() throws SensorHubException {
        var processes = hub.getModuleRegistry().getLoadedModules(OccupancyProcessModule.class).stream().toList();
        OccupancyProcessModule process = processes.stream().filter(p -> p.getConfiguration().systemUID.equals(lane.getUniqueIdentifier())).findFirst().orElse(null);
        // Check lane is started
        lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        // Check process is started
        process.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        // Add listener to process module
        new Thread(() -> {
            try {
                Async.waitForCondition(() -> process.getCurrentState() == ModuleEvent.ModuleState.STARTING, 500, 20000);
                System.out.println("Process has been restarted..");
                System.out.println(process.getCurrentState());
            } catch (TimeoutException e) {
                fail();
                throw new RuntimeException(e);
            }
        }).start();
        // Stop lane
        lane.stop();
        lane.waitForState(ModuleEvent.ModuleState.STOPPED, 5000);
        // Start lane
        lane.start();
        lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
    }

    private void startLaneAndWaitForStarted() throws SensorHubException {
        hub.getModuleRegistry().startModule(lane.getLocalID());
        lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
    }

//    private SensorSystemConfig.SystemMember createWeatherSubmodule(ISensorHub hub, String id) throws SensorHubException {
//        FakeWeatherConfig config = (FakeWeatherConfig) hub.getModuleRegistry().createModuleConfig(hub.getModuleRegistry().getInstalledModuleTypes(FakeWeatherSensor.class).stream().toList().get(0));
//        assertNotNull(config);
//        config.autoStart = true;
//        config.name = "Weather Driver " + id;
//        config.serialNumber = id;
//        var newMember = new SensorSystemConfig.SystemMember();
//        newMember.config = config;
//        return newMember;
//    }

    private SystemDriverDatabase loadSystemDriverDatabase(ISensorHub hub) throws SensorHubException {
        SystemDriverDatabaseConfig dbModuleConfig = new SystemDriverDatabaseConfig();
        dbModuleConfig.id = LANE_DATABASE_ID;
        dbModuleConfig.name = "Lane DB";
        dbModuleConfig.autoStart = true;
        dbModuleConfig.databaseNum = 1;
        dbModuleConfig.systemUIDs = new HashSet<>(List.of("urn:osh:system:lane:*", "urn:osh:process:occupancy:*"));
        MVObsSystemDatabaseConfig dbConfig = (MVObsSystemDatabaseConfig) (dbModuleConfig.dbConfig = new MVObsSystemDatabaseConfig());
        dbConfig.storagePath = dbFile.getAbsolutePath();
        dbConfig.readOnly = false;
        return (SystemDriverDatabase) hub.getModuleRegistry().loadModule(dbModuleConfig);
    }

    private LaneSystem loadLaneModule(ISensorHub hub, String id) throws SensorHubException {
        // This is what you should configure in the Admin UI
        LaneConfig laneConfig = (LaneConfig) hub.getModuleRegistry().createModuleConfig(new Descriptor());
        laneConfig.uniqueID = id;
        laneConfig.name = "Lane " + id;
        var opts = laneConfig.laneOptionsConfig = new LaneOptionsConfig();
        var db = opts.laneDatabaseConfig = new LaneDatabaseConfig();
        db.laneDatabaseId = LANE_DATABASE_ID;
        opts.createProcess = true;
        var rpm = opts.rpmConfig = new RPMConfig();
        rpm.rpmUniqueId = id;
        rpm.rpmLabel = "RPM " + id;
        rpm.rpmType = RPMType.RAPISCAN;
        rpm.remoteHost = RAPISCAN_HOST;
        rpm.remotePort = RAPISCAN_PORT;
        return (LaneSystem) hub.getModuleRegistry().loadModule(laneConfig);
    }

    @After
    public void cleanup()
    {
        try
        {
            if (hub != null)
                hub.stop();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (dbFile != null)
                dbFile.delete();
        }
    }

}
