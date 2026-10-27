package com.botts.impl.service.oscar;

import com.botts.impl.system.lane.AdjudicationControl;
import com.botts.impl.system.lane.LaneSystem;
import net.opengis.swe.v20.Count;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.utils.rad.model.Adjudication;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class AdjudicationCodeTests {
    private SensorHub hub;
    private TestControl control;

    /** Exercise the production registration lifecycle without unrelated services. */
    public static class CodeService extends OSCARServiceModule {
        @Override
        protected void doInit() {
            config.getCustomAdjudicationCodeValues();
            system = new OSCARSystem(config.nodeId);
            adjudicationCodesOutput = new AdjudicationCodesOutput(system);
            system.addOutput(adjudicationCodesOutput, false);
            system.updateSensorDescription();
        }

        @Override
        protected void doStart() throws org.sensorhub.api.common.SensorHubException {
            registerSystemAndPublishCodes();
        }

        @Override
        protected void doStop() throws org.sensorhub.api.common.SensorHubException {
            unregisterSystem();
        }
    }

    private static class TestControl extends AdjudicationControl {
        TestControl(LaneSystem lane) {
            super(lane);
        }

        boolean accepts(int code) {
            return isAdjudicationCodeSupported(code);
        }
    }

    @Before
    public void setUp() throws Exception {
        hub = new SensorHub();
        hub.start();
        var lane = new LaneSystem();
        lane.setParentHub(hub);
        control = new TestControl(lane);
    }

    @After
    public void tearDown() {
        if (hub != null)
            hub.stop();
    }

    private AdjudicationCodeConfig entry(int code, String label) {
        var entry = new AdjudicationCodeConfig();
        entry.code = code;
        entry.label = label;
        return entry;
    }

    private OSCARServiceModule loadService(AdjudicationCodeConfig... entries) throws Exception {
        var config = new OSCARServiceConfig();
        config.moduleClass = OSCARServiceModule.class.getName();
        config.customAdjudicationCodes.addAll(Arrays.asList(entries));
        return (OSCARServiceModule) hub.getModuleRegistry().loadModule(config);
    }

    @Test
    public void builtInCodesWorkWithoutService() {
        for (int code = 0; code <= 11; code++)
            assertTrue(control.accepts(code));
        assertFalse(control.accepts(-1));
        assertFalse(control.accepts(12));
    }

    @Test
    public void acceptsOnlyConfiguredCustomCodesAndSeesUpdates() throws Exception {
        var service = loadService(entry(12, "Local disposition"), entry(100, "Additional review"));
        assertTrue(control.accepts(12));
        assertTrue(control.accepts(100));
        assertFalse(control.accepts(13));
        assertFalse(control.accepts(99));

        var updated = new OSCARServiceConfig();
        updated.customAdjudicationCodes.add(entry(42, "New disposition"));
        service.setConfiguration(updated);
        assertFalse(control.accepts(12));
        assertTrue(control.accepts(42));
        assertTrue(control.accepts(11));
    }

    @Test
    public void commandAndResultSchemasAllowCustomCodes() {
        for (var record : List.of(control.getCommandDescription(), control.getResultDescription())) {
            var code = (Count) record.getComponent("adjudicationCode");
            assertArrayEquals(new double[]{0, Integer.MAX_VALUE},
                    code.getConstraint().getIntervalList().get(0), 0);
        }
    }

    @Test
    public void submissionRejectsUnknownCodeBeforeUpdatingOccupancy() throws Exception {
        loadService(entry(100, "Additional review"));
        var status = submit(99);
        assertEquals(ICommandStatus.CommandStatusCode.FAILED, status.getStatusCode());
        assertTrue(status.getMessage().contains("Unknown adjudication code: 99"));

        // A configured code passes disposition validation and proceeds to the
        // independent occupancy validation, even on an already-created lane.
        assertEquals("Occupancy Observation ID field must not be blank.", submit(100).getMessage());
    }

    private ICommandStatus submit(int code) throws Exception {
        var adjudication = new Adjudication.Builder()
                .feedback("Review")
                .adjudicationCode(code)
                .isotopes(List.of())
                .filePaths(List.of())
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.NONE)
                .occupancyObsId("")
                .vehicleId("")
                .build();
        var data = Adjudication.fromAdjudication(adjudication);
        assertEquals(code, Adjudication.toAdjudication(data).getAdjudicationCode());
        return control.submitCommand(new CommandData.Builder()
                .withId(BigId.fromLong(1, 1))
                .withCommandStream(BigId.fromLong(1, 2))
                .withParams(data)
                .build()).get();
    }

    @Test
    public void configurationUpdatesPublishObservationsAfterReregistration() throws Exception {
        var service = new CodeService();
        service.setParentHub(hub);
        var config = new OSCARServiceConfig();
        config.nodeId = "code-lifecycle-test";
        config.customAdjudicationCodes.add(entry(12, "Initial"));
        service.init(config);
        service.start();
        try {
            assertTrue(service.isStarted());
            assertPublishedCode(hub.getDatabaseRegistry().getFederatedDatabase(), 12);
            var oldSystem = service.system;

            var updated = new OSCARServiceConfig();
            updated.nodeId = config.nodeId;
            updated.customAdjudicationCodes.add(entry(42, "Added later"));
            service.updateConfig(updated);
            assertTrue("Configuration update must restart the service", service.isStarted());
            assertNotSame(oldSystem, service.system);
            assertPublishedCode(hub.getDatabaseRegistry().getFederatedDatabase(), 42);

            updated = new OSCARServiceConfig();
            updated.nodeId = config.nodeId;
            service.updateConfig(updated);
            assertTrue(service.isStarted());
            assertPublishedCode(hub.getDatabaseRegistry().getFederatedDatabase(), null);
        } finally {
            service.stop();
        }
        assertFalse(hub.getSystemDriverRegistry().isRegistered(service.system.getUniqueIdentifier()));
    }

    @Test
    public void mapsDatabaseBeforePublishingInitialCodeObservations() throws Exception {
        var dbConfig = new org.sensorhub.impl.datastore.mem.InMemorySystemStateDbConfig();
        dbConfig.id = "code-database";
        dbConfig.databaseNum = 7;
        var database = (org.sensorhub.impl.datastore.mem.InMemorySystemStateDatabase)
                hub.getModuleRegistry().loadModule(dbConfig);
        database.init();
        database.start();
        var service = new CodeService();
        service.setParentHub(hub);
        var config = new OSCARServiceConfig();
        config.nodeId = "code-database-test";
        config.databaseID = dbConfig.id;
        config.customAdjudicationCodes.add(entry(100, "Database choice"));
        service.init(config);
        service.start();
        try {
            assertTrue(service.isStarted());
            assertSame(database, hub.getSystemDriverRegistry().getDatabase(service.system.getUniqueIdentifier()));
            assertPublishedCode(database, 100);
        } finally {
            service.stop();
        }
    }

    private void assertPublishedCode(org.sensorhub.api.database.IObsSystemDatabase database, Integer code) {
        var output = AdjudicationCodesOutput.NAME;
        var filter = new org.sensorhub.api.datastore.obs.ObsFilter.Builder()
                .withDataStreams().withOutputNames(output).done().withLatestResult().build();
        var observations = database.getObservationStore().select(filter).toList();
        assertEquals("An observation must be persisted for " + output, 1, observations.size());
        var data = observations.get(0).getResult();
        assertEquals(code == null ? 0 : 1, data.getIntValue(1));
        if (code != null)
            assertEquals(code.intValue(), data.getIntValue(2));
    }

    @Test
    public void publishesCustomChoicesAndClearsRemovedCodes() throws Exception {
        var output = new AdjudicationCodesOutput(new OSCARSystem("test"));
        output.publish(List.of(entry(12, "Local review"), entry(100, "Additional review")));
        var data = output.getLatestRecord();
        assertEquals(2, data.getIntValue(1));
        assertEquals(12, data.getIntValue(2));
        assertEquals("Local review", data.getStringValue(3));
        assertEquals(100, data.getIntValue(4));
        assertEquals("Additional review", data.getStringValue(5));
        var buffer = new java.io.ByteArrayOutputStream();
        var writer = new org.vast.swe.fast.JsonDataWriterGson();
        writer.setDataComponents(output.getRecordDescription().copy());
        writer.setOutput(buffer);
        writer.startStream(false);
        writer.write(data);
        writer.endStream();
        writer.flush();
        var json = com.google.gson.JsonParser.parseString(buffer.toString(java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("codes");
        assertEquals(2, json.size());
        assertEquals(12, json.get(0).getAsJsonObject().get("code").getAsInt());
        assertEquals("Local review", json.get(0).getAsJsonObject().get("label").getAsString());
        var filter = new org.sensorhub.api.datastore.obs.DataStreamFilter.Builder()
                .withObservedProperties(AdjudicationCodesOutput.DEFINITION).build();
        assertTrue(filter.hasObservable(output.getRecordDescription()));

        output.publish(List.of());
        assertEquals(0, output.getLatestRecord().getIntValue(1));
        assertEquals(2, output.getLatestRecord().getAtomCount());
        output.publish(List.of(entry(42, "Replacement")));
        assertEquals(42, output.getLatestRecord().getIntValue(2));
        assertEquals(4, output.getLatestRecord().getAtomCount());
    }

    @Test
    public void missingCustomCodesAreBackwardCompatible() {
        var config = new OSCARServiceConfig();
        assertTrue(config.getCustomAdjudicationCodeValues().isEmpty());
        config.customAdjudicationCodes = null;
        assertTrue(config.getCustomAdjudicationCodeValues().isEmpty());
    }

    @Test
    public void rejectsReservedNegativeDuplicateAndUnlabeledCodes() {
        for (int code : new int[]{-1, 0, 11})
            assertInvalid(entry(code, "Reserved"));
        assertInvalid(entry(12, null));
        assertInvalid(entry(12, "  "));
        assertInvalid((AdjudicationCodeConfig) null);
        assertInvalid(entry(12, "First"), entry(12, "Duplicate"));
    }

    private void assertInvalid(AdjudicationCodeConfig... entries) {
        var config = new OSCARServiceConfig();
        config.customAdjudicationCodes.addAll(Arrays.asList(entries));
        try {
            config.getCustomAdjudicationCodeValues();
            fail("Expected invalid custom disposition configuration to be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
