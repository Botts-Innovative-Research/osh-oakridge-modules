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

import com.botts.impl.system.lane.Descriptor;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.*;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.database.system.SystemDriverDatabaseConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;

import static org.junit.Assert.*;

/**
 * @author Alex Almanza
 * @since May 01 2025
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LaneTests {
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
    
    @Before
    public void setUp() throws Exception {
        hub = new SensorHub();
        hub.start();

        database = loadSystemDriverDatabase();
        assertNotNull(database);
        assertNull(database.getCurrentError());
        assertEquals(database.getCurrentState(), ModuleEvent.ModuleState.STARTED);
        lane = loadLaneModule();
        assertNotNull(lane);
        assertNull(lane.getCurrentError());
        assertEquals(lane.getCurrentState(), ModuleEvent.ModuleState.LOADED);
    }

    @Test
    public void _1testSystemDriverDatabaseLoads() throws SensorHubException {
        database = loadSystemDriverDatabase();
    }

    @Test
    public void _2testLaneLoads() throws SensorHubException {
        lane = loadLaneModule();
    }

    @Test
    public void _3testLaneCreatesRPMModule() throws SensorHubException {
        hub.getModuleRegistry().startModuleAsync(lane);
        assertFalse(lane.getMembers().isEmpty());
    }

    @Test
    public void _4testProcessLoaded() {
    }

    @Test
    public void _5testProcessInitialized() {
    }

    @Test
    public void _6testProcessStarted() {
    }

    private SystemDriverDatabase loadSystemDriverDatabase() throws SensorHubException {
        SystemDriverDatabaseConfig dbModuleConfig = new SystemDriverDatabaseConfig();
        dbModuleConfig.name = "Lane DB";
        dbModuleConfig.autoStart = true;
        dbModuleConfig.databaseNum = 1;
        MVObsSystemDatabaseConfig dbConfig = (MVObsSystemDatabaseConfig) (dbModuleConfig.dbConfig = new MVObsSystemDatabaseConfig());
        dbConfig.storagePath = "unitTestDB.db";
        dbConfig.readOnly = false;
        return (SystemDriverDatabase) hub.getModuleRegistry().loadModule(dbModuleConfig);
    }

    private LaneSystem loadLaneModule() throws SensorHubException {
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

}
