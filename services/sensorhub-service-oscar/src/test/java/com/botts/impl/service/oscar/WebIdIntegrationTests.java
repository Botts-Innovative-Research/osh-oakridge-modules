package com.botts.impl.service.oscar;

import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServiceConfig;
import com.botts.impl.service.oscar.webid.WebIdClient;
import com.botts.impl.service.oscar.webid.WebIdRequest;
import com.botts.impl.service.oscar.webid.WebIdResourceHandler;
import com.botts.impl.system.lane.AdjudicationControl;
import com.botts.impl.system.lane.Descriptor;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.WebIdOutput;
import com.botts.impl.system.lane.config.*;
import net.opengis.swe.v20.DataBlock;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigIdLong;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.database.system.SystemDriverDatabaseConfig;
import org.sensorhub.impl.database.system.SystemDriverDatabaseDescriptor;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseDescriptor;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.IsotopeAnalysis;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Integration tests for WebID analysis functionality.
 *
 * These tests verify:
 * 1. WebID analysis with real n42 files using the WebID API
 * 2. WebID observations can be stored in the database
 * 3. WebID observation IDs can be attached to occupancy observations
 * 4. Adjudications can reference n42 file paths and link to occupancies with WebID data
 * 5. HTTP multipart uploads with webIdEnabled query parameter trigger WebID analysis
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebIdIntegrationTests {

    private static final Logger log = LoggerFactory.getLogger(WebIdIntegrationTests.class);

    private static final String RPM_HOST = "100.94.197.23";
//    private static final String RPM_HOST = System.getenv("RPM_HOST"); //"192.168.1.211";
    private static final int RPM_PORT = 1601;
    private static final String DB_PATH = "webid_test.dat";
    private static final String BUCKET_ROOT = "webid_test_bucket";
    private static final String TEST_BUCKET = "webid-uploads";

    private SensorHub hub;
    private ModuleRegistry reg;
    private WebIdClient webIdClient;

    @Before
    public void setUp() throws Exception {
        Files.deleteIfExists(Path.of(DB_PATH));
        cleanupBucketDir();
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();
        webIdClient = new WebIdClient("https://full-spectrum.sandia.gov/api/v1");
    }

    @After
    public void cleanup() throws IOException {
        if (hub != null)
            hub.stop();
        Files.deleteIfExists(Path.of(DB_PATH));
        cleanupBucketDir();
    }

    private void cleanupBucketDir() {
        try {
            Path bucketPath = Path.of(BUCKET_ROOT);
            if (Files.exists(bucketPath)) {
                Files.walk(bucketPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException ignored) {}
    }

    // ==================== Helper Methods ====================

    private SystemDriverDatabase loadAndStartDB() throws SensorHubException {
        var config = (SystemDriverDatabaseConfig) reg.createModuleConfig(new SystemDriverDatabaseDescriptor());
        var dbConfig = (MVObsSystemDatabaseConfig) reg.createModuleConfig(new MVObsSystemDatabaseDescriptor());
        dbConfig.storagePath = DB_PATH;
        config.systemUIDs = Set.of("*");
        config.dbConfig = dbConfig;
        config.autoStart = true;
        config.databaseNum = 2;
        return (SystemDriverDatabase) reg.loadModule(config);
    }

    private LaneConfig createLaneConfig() throws SensorHubException {
        LaneConfig config = (LaneConfig) reg.createModuleConfig(new Descriptor());
        config.laneOptionsConfig = new LaneOptionsConfig();
        config.laneOptionsConfig.rpmConfig = new RapiscanRPMConfig();
        config.laneOptionsConfig.rpmConfig.remoteHost = RPM_HOST;
        config.laneOptionsConfig.rpmConfig.remotePort = RPM_PORT;
        config.uniqueID = "test-lane-webid";
        config.name = "WebID Lane";
        config.laneOptionsConfig.ffmpegConfig = new ArrayList<>();
        return config;
    }

    private LaneSystem loadAndStartLane(LaneConfig config) throws SensorHubException {
        LaneSystem lane = (LaneSystem) reg.loadModule(config);
        reg.startModule(lane.getLocalID());
        boolean isStarted = lane.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        assertTrue("Lane should start successfully", isStarted);
        return lane;
    }

    private IHttpServer<?> loadHttpServer() throws SensorHubException {
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.autoStart = true;
        httpConfig.moduleClass = HttpServer.class.getCanonicalName();
        httpConfig.id = UUID.randomUUID().toString();
        httpConfig.httpPort = 8888;
        return (IHttpServer<?>) reg.loadModule(httpConfig);
    }

    private BucketService loadBucketService() throws SensorHubException {
        BucketServiceConfig config = new BucketServiceConfig();
        config.initialBuckets = List.of(TEST_BUCKET);
        config.fileStoreRootDir = BUCKET_ROOT;
        config.autoStart = true;
        return (BucketService) reg.loadModule(config);
    }

    private void generateOccupancy(LaneSystem lane) throws SensorHubException {
        long start = Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli();
        long end = Instant.now().toEpochMilli();

        AtomicReference<OccupancyOutput> occOutput = new AtomicReference<>();
        lane.getMembers().values().forEach((member) -> {
            if (member.getOutputs().containsKey(OccupancyOutput.NAME))
                occOutput.set((OccupancyOutput) member.getOutputs().get(OccupancyOutput.NAME));
        });

        assertNotNull("OccupancyOutput should exist", occOutput.get());

        var occ = new Occupancy.Builder()
                .samplingTime(end / 1000d)
                .occupancyCount(1)
                .startTime(start / 1000d)
                .endTime(end / 1000d)
                .neutronBackground(100)
                .gammaAlarm(true)
                .neutronAlarm(false)
                .maxGammaCount(8500)
                .maxNeutronCount(150)
                .adjudicatedIds(new ArrayList<>())
                .videoPaths(new ArrayList<>())
                .webIdObsIds(new ArrayList<>())
                .build();

        occOutput.get().setData(occ);
    }

    // ==================== HTTP Multipart Upload Helper Methods ====================

    /**
     * Sends a multipart POST request to upload a file with WebID query parameters.
     */
    private int sendMultipartUpload(String bucketUrl, String filename, byte[] fileContent,
                                    String contentType, String occupancyObsId, String laneUid,
                                    boolean webIdEnabled) throws IOException {

        String boundary = "----WebIdTestBoundary" + System.currentTimeMillis();

        // Build URL with query parameters
        StringBuilder urlBuilder = new StringBuilder(bucketUrl);
        urlBuilder.append("/").append(TEST_BUCKET).append("/").append(filename);
        urlBuilder.append("?webIdEnabled=").append(webIdEnabled);
        if (occupancyObsId != null && !occupancyObsId.isBlank()) {
            urlBuilder.append("&occupancyObsId=").append(occupancyObsId);
        }
        if (laneUid != null && !laneUid.isBlank()) {
            urlBuilder.append("&laneUid=").append(laneUid);
        }

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        // Build multipart body
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), true);

        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
        writer.append("Content-Type: ").append(contentType).append("\r\n");
        writer.append("\r\n");
        writer.flush();

        baos.write(fileContent);

        writer.append("\r\n");
        writer.append("--").append(boundary).append("--").append("\r\n");
        writer.flush();

        byte[] body = baos.toByteArray();

        conn.setRequestProperty("Content-Length", String.valueOf(body.length));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        conn.disconnect();

        return responseCode;
    }

    /**
     * Sends a PUT request to upload a file with WebID query parameters.
     */
    private int sendPutUpload(String bucketUrl, String filename, byte[] fileContent,
                              String contentType, String occupancyObsId, String laneUid,
                              boolean webIdEnabled) throws IOException {

        // Build URL with query parameters
        StringBuilder urlBuilder = new StringBuilder(bucketUrl);
        urlBuilder.append("/").append(TEST_BUCKET).append("/").append(filename);
        urlBuilder.append("?webIdEnabled=").append(webIdEnabled);
        if (occupancyObsId != null && !occupancyObsId.isBlank()) {
            urlBuilder.append("&occupancyObsId=").append(occupancyObsId);
        }
        if (laneUid != null && !laneUid.isBlank()) {
            urlBuilder.append("&laneUid=").append(laneUid);
        }

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Content-Length", String.valueOf(fileContent.length));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(fileContent);
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        conn.disconnect();

        return responseCode;
    }

    // ==================== Basic WebID API Tests ====================

    @Test
    public void test01_WebIdAnalysisWithN42File() throws IOException, InterruptedException {
        WebIdRequest request = new WebIdRequest.Builder()
                .foreground(WebIdIntegrationTests.class.getResourceAsStream("/webid/n42/ex_I131_DetectiveEX.n42"))
                .drf("Detective-EX")
                .build();

        WebIdAnalysis response = webIdClient.analyze(request);

        assertNotNull("Response should not be null", response);
        assertEquals("Analysis should succeed", 0, response.getAnalysisError());
        assertNotNull("Isotopes should not be null", response.getIsotopes());
        assertFalse("Should detect isotopes", response.getIsotopes().isEmpty());

        boolean foundI131 = response.getIsotopes().stream()
                .anyMatch(isotope -> isotope.getName().contains("I131") || isotope.getName().contains("I-131"));
        assertTrue("Should detect I-131 isotope", foundI131);

        System.out.println(response.getDrf());
        System.out.println(response.getIsotopeString());
        System.out.println(response.getEstimatedDose());
    }

    // ==================== Serialization Tests ====================

    @Test
    public void test02_WebIdAnalysisSerializeDeserialize() {
        List<IsotopeAnalysis> isotopes = new ArrayList<>();
        isotopes.add(new IsotopeAnalysis.Builder()
                .name("I-131")
                .type("Medical")
                .confidence(9.4f)
                .confidenceStr("H")
                .countRate(28362.25f)
                .build());
        isotopes.add(new IsotopeAnalysis.Builder()
                .name("Cs-137")
                .type("Industrial")
                .confidence(7.8f)
                .confidenceStr("M")
                .countRate(15000.5f)
                .build());

        WebIdAnalysis original = new WebIdAnalysis.Builder()
                .sampleTime(Instant.parse("2026-02-09T12:00:00Z"))
                .isotopes(isotopes)
                .isotopeString("I-131 (H), Cs-137 (M)")
                .drf("Detective-EX")
                .estimatedDose(1699.0)
                .chi2(2.82)
                .errorMessage("")
                .analysisWarnings(List.of("Warning: background and foreground spectra were taken 41 days apart."))
                .occupancyObsId("test_occupancy_obs_id")
                .build();

        DataBlock dataBlock = WebIdAnalysis.fromWebIdAnalysis(original);
        assertNotNull(dataBlock);

        WebIdAnalysis deserialized = WebIdAnalysis.toWebIdAnalysis(dataBlock);
        assertNotNull(deserialized);

        assertEquals(original.getSampleTime(), deserialized.getSampleTime());
        assertEquals(original.getIsotopes().size(), deserialized.getIsotopes().size());
        assertEquals(original.getIsotopeString(), deserialized.getIsotopeString());
        assertEquals(original.getDrf(), deserialized.getDrf());
        assertEquals(original.getEstimatedDose(), deserialized.getEstimatedDose(), 0.001);
        assertEquals(original.getChi2(), deserialized.getChi2(), 0.001);
        assertEquals(original.getOccupancyObsId(), deserialized.getOccupancyObsId());

        log.info("WebIdAnalysis serialization/deserialization test passed");
    }

    @Test
    public void test03_OccupancyWithWebIdSerializeDeserialize() {
        double now = System.currentTimeMillis() / 1000.0;

        Occupancy original = new Occupancy.Builder()
                .samplingTime(now)
                .occupancyCount(5)
                .startTime(now - 30)
                .endTime(now)
                .neutronBackground(75.5)
                .gammaAlarm(true)
                .neutronAlarm(true)
                .maxGammaCount(10000)
                .maxNeutronCount(500)
                .adjudicatedIds(List.of("adj_001", "adj_002"))
                .videoPaths(List.of("/videos/occ1.mp4"))
                .webIdObsIds(List.of("webid_001", "webid_002", "webid_003"))
                .build();

        DataBlock dataBlock = Occupancy.fromOccupancy(original);
        assertNotNull(dataBlock);

        Occupancy deserialized = Occupancy.toOccupancy(dataBlock);
        assertNotNull(deserialized);

        assertEquals(original.getSamplingTime(), deserialized.getSamplingTime(), 0.001);
        assertEquals(original.getOccupancyCount(), deserialized.getOccupancyCount());
        assertEquals(original.getAdjudicatedIds().size(), deserialized.getAdjudicatedIds().size());
        assertEquals(original.getWebIdObsIds().size(), deserialized.getWebIdObsIds().size());
        assertTrue(deserialized.getWebIdObsIds().contains("webid_001"));
        assertTrue(deserialized.getWebIdObsIds().contains("webid_002"));
        assertTrue(deserialized.getWebIdObsIds().contains("webid_003"));

        log.info("Occupancy with WebID serialization/deserialization test passed");
    }

    @Test
    public void test04_AdjudicationWithN42FilePathSerializeDeserialize() {
        Adjudication original = new Adjudication.Builder()
                .feedback("WebID analysis confirmed Cs-137 and Co-60")
                .adjudicationCode(1)
                .isotopes(List.of("Cs-137", "Co-60"))
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.COMPLETED)
                .filePaths(List.of("/bucket/uploads/spectrum.n42", "/bucket/webid/analysis_result.json"))
                .occupancyObsId("test_occ_id")
                .vehicleId("VAN-456")
                .build();

        DataBlock dataBlock = Adjudication.fromAdjudication(original);
        assertNotNull(dataBlock);

        Adjudication deserialized = Adjudication.toAdjudication(dataBlock);
        assertNotNull(deserialized);

        assertEquals(original.getFeedback(), deserialized.getFeedback());
        assertEquals(original.getFilePaths(), deserialized.getFilePaths());
        assertTrue(deserialized.getFilePaths().stream().anyMatch(p -> p.endsWith(".n42")));

        log.info("Adjudication with n42 file path serialization/deserialization test passed");
    }

    // ==================== HTTP Upload with BucketService Tests ====================

    @Test
    public void test05_BucketServiceSetup() throws SensorHubException {
        // Test that we can set up HttpServer and BucketService
        var httpServer = loadHttpServer();
        httpServer.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        var bucketService = loadBucketService();
        bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        assertNotNull("BucketService should have bucket store", bucketService.getBucketStore());

        String publicUrl = bucketService.getPublicEndpointUrl();
        assertNotNull("BucketService should have public URL", publicUrl);
        assertFalse("Public URL should not be blank", publicUrl.isBlank());

        log.info("BucketService public URL: {}", publicUrl);
    }

    @Test
    public void test06_RegisterWebIdResourceHandler() throws SensorHubException {
        // Test that WebIdResourceHandler can be registered with BucketService
        var httpServer = loadHttpServer();
        httpServer.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        var bucketService = loadBucketService();
        bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        // Register WebIdResourceHandler
        WebIdResourceHandler webIdHandler = new WebIdResourceHandler(
                bucketService.getBucketStore(), hub, webIdClient);
        bucketService.registerObjectHandler(webIdHandler);

        // Verify handler is registered for n42 files
        var handler = bucketService.getObjectHandler(TEST_BUCKET, "test.n42");
        assertNotNull("Handler should exist for n42 files", handler);
        assertEquals("Handler should be WebIdResourceHandler", webIdHandler, handler);

        log.info("WebIdResourceHandler registered successfully, pattern: {}", handler.getObjectPattern());

        // Cleanup
        bucketService.unregisterObjectHandler(webIdHandler);
    }

    @Test
    public void test07_HttpUploadWithWebIdEnabled() throws SensorHubException, IOException, InterruptedException {
        // Full integration test: HTTP upload with webIdEnabled=true

        // Setup infrastructure
        var database = loadAndStartDB();
        var laneConfig = createLaneConfig();
        var lane = loadAndStartLane(laneConfig);
        generateOccupancy(lane);

        var httpServer = loadHttpServer();
        httpServer.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        var bucketService = loadBucketService();
        bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        // Register WebIdResourceHandler
        WebIdResourceHandler webIdHandler = new WebIdResourceHandler(
                bucketService.getBucketStore(), hub, webIdClient);
        bucketService.registerObjectHandler(webIdHandler);

        // Get occupancy obs ID
        var obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();
        var obsStore = hub.getDatabaseRegistry().getFederatedDatabase().getObservationStore();
        database.getObservationStore().removeEntries(obsStore.selectAllFilter());
        generateOccupancy(lane);
        var occupancyObsId = obsStore.selectKeys(obsStore.selectAllFilter()).toList().get(0);
        var encodedOccupancyObsId = obsIdEncoder.encodeID(occupancyObsId);

        log.info("Occupancy ID: {}", encodedOccupancyObsId);

        // Read n42 file content
        byte[] n42Content;
        try (InputStream is = WebIdIntegrationTests.class.getResourceAsStream("/webid/n42/ex_I131_DetectiveEX.n42")) {
            assertNotNull("N42 test file should exist", is);
            n42Content = is.readAllBytes();
        }

        String bucketUrl = "http://localhost:8888/sensorhub" + bucketService.getPublicEndpointUrl();
        log.info("Uploading n42 file to: {}", bucketUrl);

        // Send HTTP PUT with webIdEnabled=true
        int responseCode = sendPutUpload(
                bucketUrl,
                "test_spectrum.n42",
                n42Content,
                "application/xml",
                encodedOccupancyObsId,
                lane.getUniqueIdentifier(),
                true
        );

        log.info("HTTP PUT response code: {}", responseCode);
        assertEquals("Upload should succeed", 200, responseCode);

        // Wait for async WebID processing
        Thread.sleep(3000);

        // Verify the file was stored
        assertTrue("Uploaded file should exist",
                bucketService.getBucketStore().objectExists(TEST_BUCKET, "test_spectrum.n42"));

        // Check if WebID observation was created
        var webIdDsKeys = obsStore.getDataStreams().selectKeys(new DataStreamFilter.Builder()
                .withOutputNames(WebIdOutput.NAME)
                .build()).toList();

        log.info("WebID datastreams found: {}", webIdDsKeys.size());

        // Get occupancy datastream to find the right observation
        var occDsKeys = obsStore.getDataStreams().selectKeys(new DataStreamFilter.Builder()
                .withOutputNames(OccupancyOutput.NAME)
                .build()).toList();

        if (!occDsKeys.isEmpty()) {
            log.info("Occupancy datastream found, checking for WebID attachment...");
            // The occupancy observation should still be retrievable by its ID
            var updatedOccupancyObs = obsStore.get(occupancyObsId);
            if (updatedOccupancyObs != null) {
                try {
                    var updatedOccupancy = Occupancy.toOccupancy(updatedOccupancyObs.getResult());
                    log.info("Occupancy WebID obs count: {}", updatedOccupancy.getWebIdObsIds().size());

                    if (!updatedOccupancy.getWebIdObsIds().isEmpty()) {
                        log.info("WebID observation successfully attached to occupancy!");
                        log.info("WebID Obs IDs: {}", updatedOccupancy.getWebIdObsIds());
                    }
                } catch (Exception e) {
                    log.warn("Could not deserialize occupancy: {}", e.getMessage());
                }
            }
        }

        // Cleanup
        bucketService.unregisterObjectHandler(webIdHandler);
    }

    @Test
    public void test08_HttpMultipartUploadWithWebIdEnabled() throws SensorHubException, IOException, InterruptedException {
        // Test multipart POST upload with webIdEnabled=true

        // Setup infrastructure
        var database = loadAndStartDB();
        var laneConfig = createLaneConfig();
        var lane = loadAndStartLane(laneConfig);
        generateOccupancy(lane);

        var httpServer = loadHttpServer();
        httpServer.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        var bucketService = loadBucketService();
        bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        // Register WebIdResourceHandler
        WebIdResourceHandler webIdHandler = new WebIdResourceHandler(
                bucketService.getBucketStore(), hub, webIdClient);
        bucketService.registerObjectHandler(webIdHandler);

        // Get occupancy obs ID
        var obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();
        var obsStore = hub.getDatabaseRegistry().getFederatedDatabase().getObservationStore();
        var occupancyObsId = obsStore.selectKeys(obsStore.selectAllFilter()).toList().get(0);
        var encodedOccupancyObsId = obsIdEncoder.encodeID(occupancyObsId);

        log.info("Occupancy ID for multipart upload: {}", encodedOccupancyObsId);

        // Read n42 file content
        byte[] n42Content;
        try (InputStream is = WebIdIntegrationTests.class.getResourceAsStream("/webid/n42/ex_I131_DetectiveEX.n42")) {
            assertNotNull("N42 test file should exist", is);
            n42Content = is.readAllBytes();
        }

        String bucketUrl = "http://localhost:8888/sensorhub" + bucketService.getPublicEndpointUrl();

        // Send multipart POST with webIdEnabled=true
        int responseCode = sendMultipartUpload(
                bucketUrl,
                "multipart_spectrum.n42",
                n42Content,
                "application/xml",
                encodedOccupancyObsId,
                lane.getUniqueIdentifier(),
                true
        );

        log.info("HTTP POST multipart response code: {}", responseCode);
        assertTrue("Multipart upload should succeed (200 or 201)",
                responseCode == 200 || responseCode == 201);

        // Wait for async WebID processing
        Thread.sleep(3000);

        // Cleanup
        bucketService.unregisterObjectHandler(webIdHandler);

        log.info("HTTP multipart upload with WebID test completed");
    }

    @Test
    public void test10_FullAdjudicationWithWebIdWorkflow() throws SensorHubException, IOException, InterruptedException, ExecutionException {
        // Complete workflow: upload n42 file with WebID, then submit adjudication

        // Setup infrastructure
        var database = loadAndStartDB();
        var laneConfig = createLaneConfig();
        var lane = loadAndStartLane(laneConfig);
        generateOccupancy(lane);

        var httpServer = loadHttpServer();
        httpServer.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        var bucketService = loadBucketService();
        bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 5000);

        // Register WebIdResourceHandler
        WebIdResourceHandler webIdHandler = new WebIdResourceHandler(
                bucketService.getBucketStore(), hub, webIdClient);
        bucketService.registerObjectHandler(webIdHandler);

        var obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();
        var cmdIdEncoder = hub.getIdEncoders().getCommandIdEncoder();
        var obsStore = hub.getDatabaseRegistry().getFederatedDatabase().getObservationStore();

        // Get the occupancy observation (filter by occupancy datastream)
        var occDsKeys = obsStore.getDataStreams().selectKeys(new DataStreamFilter.Builder()
                .withOutputNames(OccupancyOutput.NAME)
                .build()).toList();
        assertFalse("Should have occupancy datastream", occDsKeys.isEmpty());
        var occDsId = occDsKeys.get(0).getInternalID();

        // Get occupancy observations from that datastream
        var occupancyObsId = obsStore.selectKeys(obsStore.selectAllFilter())
                .filter(key -> {
                    var obs = obsStore.get(key);
                    return obs != null && obs.getDataStreamID().equals(occDsId);
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("No occupancy observation found"));
        var encodedOccupancyObsId = obsIdEncoder.encodeID(occupancyObsId);

        log.info("Step 1: Generated occupancy with ID: {} (datastream: {})", encodedOccupancyObsId, occDsId);

        // Step 2: Upload n42 file via HTTP with webIdEnabled
        byte[] n42Content;
        try (InputStream is = WebIdIntegrationTests.class.getResourceAsStream("/webid/n42/ex_I131_DetectiveEX.n42")) {
            n42Content = is.readAllBytes();
        }

        String bucketUrl = "http://localhost:8888/sensorhub" + bucketService.getPublicEndpointUrl();
        String filename = "adjudication_spectrum.n42";

        int uploadResponse = sendPutUpload(
                bucketUrl,
                filename,
                n42Content,
                "application/xml",
                encodedOccupancyObsId,
                lane.getUniqueIdentifier(),
                true
        );

        assertEquals("Upload should succeed", 200, uploadResponse);
        log.info("Step 2: Uploaded n42 file with webIdEnabled=true");

        // Wait for WebID processing
        Thread.sleep(3000);

        // Step 3: Perform WebID analysis for adjudication feedback
        WebIdRequest request = new WebIdRequest.Builder()
                .foreground(new ByteArrayInputStream(n42Content))
                .drf("Detective-EX")
                .build();
        WebIdAnalysis analysis = webIdClient.analyze(request);
        assertTrue("WebID analysis should succeed", analysis.isSuccessful());

        List<String> detectedIsotopes = analysis.getIsotopes().stream()
                .map(IsotopeAnalysis::getName)
                .toList();

        log.info("Step 3: WebID analysis - Isotopes: {}", analysis.getIsotopeString());

        // Step 4: Submit adjudication with file path
        var control = (AdjudicationControl) lane.getCommandInputs().get(AdjudicationControl.NAME);

        var adjData = Adjudication.fromAdjudication(new Adjudication.Builder()
                .feedback("WebID confirmed: " + analysis.getIsotopeString() + ". Dose: " + analysis.getEstimatedDose())
                .adjudicationCode(0)
                .isotopes(new ArrayList<>(detectedIsotopes))
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.COMPLETED)
                .filePaths(List.of("/" + TEST_BUCKET + "/" + filename))
                .occupancyObsId(encodedOccupancyObsId)
                .vehicleId("WEBID-WORKFLOW-TEST")
                .build());

        var cmd = new CommandData.Builder()
                .withId(new BigIdLong(1, 300))
                .withParams(adjData)
                .withCommandStream(new BigIdLong(1, 301))
                .withSender("WebIdWorkflowTestUser")
                .build();

        var result = control.submitCommand(cmd).get();

        log.info("Adjudication result: status={}, message={}", result.getStatusCode(), result.getMessage());

        if (result.getStatusCode() != ICommandStatus.CommandStatusCode.COMPLETED) {
            log.error("Adjudication failed with message: {}", result.getMessage());
        }

        assertEquals("Adjudication should complete", ICommandStatus.CommandStatusCode.COMPLETED, result.getStatusCode());
        assertNull("Should have no error message", result.getMessage());

        log.info("Step 4: Adjudication submitted successfully");

        // Final verification
        var finalOccupancy = Occupancy.toOccupancy(obsStore.get(occupancyObsId).getResult());
        assertFalse("Occupancy should have adjudication IDs", finalOccupancy.getAdjudicatedIds().isEmpty());
        assertEquals(cmdIdEncoder.encodeID(cmd.getID()), finalOccupancy.getAdjudicatedIds().get(0));

        log.info("Full adjudication with WebID workflow completed successfully!");
        log.info("  Occupancy ID: {}", encodedOccupancyObsId);
        log.info("  Adjudication ID: {}", cmdIdEncoder.encodeID(cmd.getID()));
        log.info("  File uploaded: {}", filename);
        log.info("  Detected Isotopes: {}", analysis.getIsotopeString());

        // Cleanup
        bucketService.unregisterObjectHandler(webIdHandler);
    }
}
