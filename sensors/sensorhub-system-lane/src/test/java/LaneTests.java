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
import org.sensorhub.utils.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    private SystemDriverDatabase database;
    private File dbFile;

    @Before
    public void setUp() throws Exception {
        if (dbFile == null) {
            // Create temp file to use with System Driver Database
            dbFile = File.createTempFile("testlanedb", ".dat");
            dbFile.deleteOnExit();
        }

        if (hub == null) {
            hub = new SensorHub();
            hub.start();
        }

        if (database == null) {
            database = loadSystemDriverDatabase(hub);
            assertNotNull(database);
            assertNull(database.getCurrentError());
            assertEquals(database.getCurrentState(), ModuleEvent.ModuleState.STARTED);
        }

        if (lane == null) {
            lane = loadLaneModule(hub);
            assertNotNull(lane);
            assertNull(lane.getCurrentError());
            assertEquals(lane.getCurrentState(), ModuleEvent.ModuleState.LOADED);
            startLaneAndWaitForStarted();
            assertEquals(lane.getCurrentState(), ModuleEvent.ModuleState.STARTED);
        }
    }

    @Test
    public void testLaneCreatesRPMModule() throws SensorHubException {
        assertFalse(lane.getMembers().isEmpty());
    }

    @Test
    public void testLaneAddedToDatabase() throws InterruptedException, TimeoutException {
        Async.waitForCondition(() -> database.getConfiguration().systemUIDs.contains(lane.getUniqueIdentifier()), 500, 10000);
    }

    @Test
    public void testProcessStarted() throws SensorHubException, TimeoutException {
        AbstractProcessModule<?> processModule = hub.getModuleRegistry().getModuleByType(OccupancyProcessModule.class);
        Async.waitForCondition(() -> processModule.getCurrentState() == ModuleEvent.ModuleState.STARTED, 500, 20000);
        System.out.println("Process started");
    }

    private void startLaneAndWaitForStarted() throws SensorHubException {
        hub.getModuleRegistry().startModule(lane.getLocalID());
        lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
    }

    private SystemDriverDatabase loadSystemDriverDatabase(ISensorHub hub) throws SensorHubException {
        SystemDriverDatabaseConfig dbModuleConfig = new SystemDriverDatabaseConfig();
        dbModuleConfig.id = LANE_DATABASE_ID;
        dbModuleConfig.name = "Lane DB";
        dbModuleConfig.autoStart = true;
        dbModuleConfig.databaseNum = 1;
        MVObsSystemDatabaseConfig dbConfig = (MVObsSystemDatabaseConfig) (dbModuleConfig.dbConfig = new MVObsSystemDatabaseConfig());
        dbConfig.storagePath = dbFile.getAbsolutePath();
        dbConfig.readOnly = false;
        return (SystemDriverDatabase) hub.getModuleRegistry().loadModule(dbModuleConfig);
    }

    private LaneSystem loadLaneModule(ISensorHub hub) throws SensorHubException {
        // This is what you should configure in the Admin UI
        LaneConfig laneConfig = (LaneConfig) hub.getModuleRegistry().createModuleConfig(new Descriptor());
        laneConfig.uniqueID = LANE_UID;
        laneConfig.name = LANE_NAME;
        var opts = laneConfig.laneOptionsConfig = new LaneOptionsConfig();
        var db = opts.laneDatabaseConfig = new LaneDatabaseConfig();
        db.purgePeriodMinutes = 5;
        db.laneDatabaseId = LANE_DATABASE_ID;
        db.autoPurgeVideoData = true;
        opts.createProcess = true;
        var rpm = opts.rpmConfig = new RPMConfig();
        rpm.rpmUniqueId = RPM_UID;
        rpm.rpmLabel = RPM_NAME;
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
