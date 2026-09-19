package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.purge.DatabasePurger;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabase;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link DatabasePurger} daily file observation purging.
 *
 * <p>Daily files themselves are written by the RPM drivers as messages arrive
 * (see {@code DailyFileAppender}); the purger only removes {@code dailyFile}
 * observations from before today's local midnight so the database does not
 * grow without bound.</p>
 */
public class DatabasePurgerTests {

    private static final String SYSTEM_UID = "urn:osh:test:sensor:lane1";
    private static final String OUTPUT_DAILY_FILE = "dailyFile";
    private static final String OUTPUT_GAMMA_COUNTS = "gammaCounts";
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private MVObsSystemDatabase database;
    private Path dbFile;

    @Before
    public void setUp() throws Exception {
        // H2 obs system database backed by a temp file
        dbFile = Files.createTempFile("purger-test-", ".dat");
        Files.deleteIfExists(dbFile);

        database = new MVObsSystemDatabase();
        MVObsSystemDatabaseConfig config = new MVObsSystemDatabaseConfig();
        config.storagePath = dbFile.toString();
        config.databaseNum = 3;
        database.setConfiguration(config);
        database.init();
        database.start();
    }

    @After
    public void tearDown() throws Exception {
        if (database != null) {
            try {
                database.stop();
            } catch (Exception ignore) {}
        }
        if (dbFile != null) {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void testPurgeDailyFileRemovesYesterdayKeepsToday() throws Exception {
        BigId sysId = addSystem();
        BigId dsId = addDataStream(sysId, OUTPUT_DAILY_FILE);

        Instant todayStart = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant();
        Instant yesterdayStart = LocalDate.now(ZONE).minusDays(1).atStartOfDay(ZONE).toInstant();

        addObservations(dsId, List.of(
                yesterdayStart.plusSeconds(3600),
                yesterdayStart.plusSeconds(7200),
                yesterdayStart.plusSeconds(10800),
                todayStart.plusSeconds(60),
                Instant.now()));
        assertEquals(5, countObs(dsId));

        DatabasePurger purger = new DatabasePurger(database, 30);
        purger.purgeDailyFileData();

        assertEquals("only today's observations should remain", 2, countObs(dsId));
        database.getObservationStore()
                .select(new ObsFilter.Builder().withDataStreams(dsId).build())
                .forEach(obs -> assertFalse("observation from before today survived purge: " + obs.getPhenomenonTime(),
                        obs.getPhenomenonTime().isBefore(todayStart)));
    }

    @Test
    public void testPurgeDailyFileDoesNotTouchOtherOutputs() throws Exception {
        BigId sysId = addSystem();
        BigId dailyId = addDataStream(sysId, OUTPUT_DAILY_FILE);
        BigId gammaId = addDataStream(sysId, OUTPUT_GAMMA_COUNTS);

        Instant yesterdayStart = LocalDate.now(ZONE).minusDays(1).atStartOfDay(ZONE).toInstant();
        addObservations(dailyId, List.of(yesterdayStart.plusSeconds(3600)));
        addObservations(gammaId, List.of(yesterdayStart.plusSeconds(3600), yesterdayStart.plusSeconds(7200)));

        DatabasePurger purger = new DatabasePurger(database, 30);
        purger.purgeDailyFileData();

        assertEquals(0, countObs(dailyId));
        assertEquals("other outputs must not be purged by the daily file purge", 2, countObs(gammaId));
    }

    @Test
    public void testPurgeDailyFileNoopWhenNoDataStreams() throws Exception {
        DatabasePurger purger = new DatabasePurger(database, 30);
        purger.purgeDailyFileData();
        assertTrue(database.getDataStreamStore().isEmpty());
    }

    // ==================== Helper Methods ====================

    private BigId addSystem() throws DataStoreException {
        AbstractProcess p = new SMLHelper().createPhysicalComponent()
                .uniqueID(SYSTEM_UID)
                .name("Test Lane")
                .build();
        SystemWrapper sysWrapper = new SystemWrapper(p);
        return database.getSystemDescStore().add(sysWrapper).getInternalID();
    }

    private DataComponent createMessageStruct(String outputName) {
        RADHelper radHelper = new RADHelper();
        var samplingTime = radHelper.createPrecisionTimeStamp();
        var message = radHelper.createAspectMessageFile();
        return radHelper.createRecord()
                .name(outputName)
                .label(outputName)
                .updatable(true)
                .definition(RADHelper.getRadUri("DailyFile"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(message.getName(), message)
                .build();
    }

    private BigId addDataStream(BigId sysId, String outputName) throws DataStoreException {
        DataComponent recordStruct = createMessageStruct(outputName);
        var dsInfo = new DataStreamInfo.Builder()
                .withName(outputName)
                .withSystem(new FeatureId(sysId, SYSTEM_UID))
                .withRecordDescription(recordStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .build();
        return database.getDataStreamStore().add(dsInfo).getInternalID();
    }

    private void addObservations(BigId dsId, List<Instant> times) {
        DataComponent recordStruct = createMessageStruct(OUTPUT_DAILY_FILE);
        for (Instant ts : times) {
            DataBlock block = recordStruct.createDataBlock();
            block.setTimeStamp(0, ts);
            block.setStringValue(1, "GB,000188,000206,000262,000243");
            IObsData obs = new ObsData.Builder()
                    .withDataStream(dsId)
                    .withPhenomenonTime(ts)
                    .withResult(block)
                    .build();
            database.getObservationStore().add(obs);
        }
    }

    private long countObs(BigId dsId) {
        return database.getObservationStore()
                .countMatchingEntries(new ObsFilter.Builder().withDataStreams(Set.of(dsId)).build());
    }
}
