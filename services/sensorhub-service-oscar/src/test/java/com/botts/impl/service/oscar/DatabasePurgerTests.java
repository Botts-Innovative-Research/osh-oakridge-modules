package com.botts.impl.service.oscar;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import com.botts.impl.service.oscar.purge.DatabasePurger;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabase;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link DatabasePurger} CSV export.
 *
 * <p>These tests cover the regression where {@code dailyFile} observations were
 * being exported as CSV files containing only the {@code message,timestamp}
 * header with no data rows beneath. The original implementation used
 * {@code stream.peek(...).count()}, which is allowed to short-circuit when the
 * source's size is known (Java 9+), skipping the per-element side effect that
 * actually wrote each row. The fix uses {@code forEach} with an external
 * counter; these tests guard against a regression.</p>
 */
public class DatabasePurgerTests {

    private static final String SYSTEM_UID = "urn:osh:test:sensor:lane1";
    private static final String EXPECTED_LANE_ID = "lane1";
    private static final String OUTPUT_DAILY_FILE = "dailyFile";
    private static final String DAILY_FILES_BUCKET = "dailyfiles";

    private MVObsSystemDatabase database;
    private IBucketStore bucketStore;
    private Path dbFile;
    private Path bucketRoot;

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

        // File-system-backed bucket store in a temp dir
        bucketRoot = Files.createTempDirectory("purger-bucket-");
        bucketStore = new FileSystemBucketStore(bucketRoot);
        bucketStore.createBucket(DAILY_FILES_BUCKET);
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
        if (bucketRoot != null && Files.exists(bucketRoot)) {
            try (Stream<Path> paths = Files.walk(bucketRoot)) {
                paths.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    public void testDailyFileExportContainsDataRows() throws Exception {
        BigId sysId = addSystem();
        BigId dsId = addDailyFileDataStream(sysId);

        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        Instant yesterdayStart = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant();

        // Use content that stresses CSV quoting logic: commas and embedded quotes
        List<String> messages = List.of(
                "msg,with,commas",
                "msg with \"quotes\"",
                "plain-msg-3"
        );
        addDailyFileObservations(dsId, yesterdayStart, messages);

        DatabasePurger purger = new DatabasePurger(database, bucketStore, 30);
        purger.exportAndPurgeDailyFileData();

        String dateStr = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String objectKey = EXPECTED_LANE_ID + "_" + dateStr + ".csv";

        assertTrue("Exported CSV should exist in bucket",
                bucketStore.objectExists(DAILY_FILES_BUCKET, objectKey));

        String csvContent = readBucketObjectAsString(objectKey);

        // Split on newlines; default split strips trailing empties, so we keep only
        // non-blank lines. We expect 1 header line + 3 data rows.
        String[] lines = csvContent.split("\n");
        assertTrue("CSV should have header as first line",
                lines.length > 0 && "message,timestamp".equals(lines[0]));

        // The regression: previously only the header was written. Assert strictly
        // that data rows are present.
        assertTrue("CSV must contain data rows beneath the header (regression guard)",
                lines.length > 1);
        assertEquals("CSV should contain header + 3 data rows", 4, lines.length);

        // Each message should appear, CSV-escaped
        for (String msg : messages) {
            String escaped = "\"" + msg.replace("\"", "\"\"") + "\"";
            assertTrue("CSV should contain escaped message: " + escaped,
                    csvContent.contains(escaped));
        }
    }

    @Test
    public void testDailyFileExportHeaderOnlyWhenNoObservations() throws Exception {
        BigId sysId = addSystem();
        addDailyFileDataStream(sysId);
        // Intentionally add no observations

        DatabasePurger purger = new DatabasePurger(database, bucketStore, 30);
        purger.exportAndPurgeDailyFileData();

        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        String dateStr = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String objectKey = EXPECTED_LANE_ID + "_" + dateStr + ".csv";

        assertTrue("Exported CSV should exist even with no observations",
                bucketStore.objectExists(DAILY_FILES_BUCKET, objectKey));

        String csvContent = readBucketObjectAsString(objectKey);
        assertEquals("CSV should contain only the header line",
                "message,timestamp\n", csvContent);
    }

    @Test
    public void testDailyFileExportDoesNothingWhenNoDataStreams() throws Exception {
        // No system, no datastream. Export should be a no-op, and no CSV file
        // should appear in the bucket.
        DatabasePurger purger = new DatabasePurger(database, bucketStore, 30);
        purger.exportAndPurgeDailyFileData();

        assertEquals("No files should be written when no dailyFile datastreams exist",
                0, bucketStore.getNumObjects(DAILY_FILES_BUCKET));
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

    private DataComponent createDailyFileStruct() {
        RADHelper radHelper = new RADHelper();
        var samplingTime = radHelper.createPrecisionTimeStamp();
        var aspectMessage = radHelper.createAspectMessageFile();
        return radHelper.createRecord()
                .name(OUTPUT_DAILY_FILE)
                .label("Daily File")
                .updatable(true)
                .definition(RADHelper.getRadUri("DailyFile"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(aspectMessage.getName(), aspectMessage)
                .build();
    }

    private BigId addDailyFileDataStream(BigId sysId) throws DataStoreException {
        DataComponent recordStruct = createDailyFileStruct();
        var dsInfo = new DataStreamInfo.Builder()
                .withName(OUTPUT_DAILY_FILE)
                .withSystem(new FeatureId(sysId, SYSTEM_UID))
                .withRecordDescription(recordStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .build();
        return database.getDataStreamStore().add(dsInfo).getInternalID();
    }

    private void addDailyFileObservations(BigId dsId, Instant yesterdayStart, List<String> messages) {
        DataComponent recordStruct = createDailyFileStruct();
        for (int i = 0; i < messages.size(); i++) {
            // Offset by one hour per obs to keep all timestamps well within yesterday UTC
            Instant ts = yesterdayStart.plusSeconds(3600L * (i + 1));
            DataBlock block = recordStruct.createDataBlock();
            block.setTimeStamp(0, ts);
            block.setStringValue(1, messages.get(i));
            ObsData obs = new ObsData.Builder()
                    .withDataStream(dsId)
                    .withPhenomenonTime(ts)
                    .withResult(block)
                    .build();
            database.getObservationStore().add(obs);
        }
    }

    private String readBucketObjectAsString(String objectKey) throws Exception {
        try (InputStream in = bucketStore.getObject(DAILY_FILES_BUCKET, objectKey);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            return sb.toString();
        }
    }
}
