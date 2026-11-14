package com.botts.impl.service.oscar;

import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServiceConfig;
import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.reports.helpers.*;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import com.botts.impl.system.lane.AdjudicationControl;
import com.botts.impl.system.lane.LaneSystem;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.CommandResult;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.CommandStreamInfo;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.database.system.SystemDriverDatabaseConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseDescriptor;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.service.consys.ConSysApiService;
import org.sensorhub.impl.service.consys.ConSysApiServiceConfig;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.eml.EmlEvaluationResult;
import org.sensorhub.impl.utils.rad.eml.EmlSourceBreakdown;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.sensorhub.impl.sensor.SensorSystem;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static org.junit.Assert.*;

public class ReportTests {
    @Rule
    public TemporaryFolder tempBucketDir = new TemporaryFolder();

    static SensorHub hub;
    static ModuleRegistry reg;

    SystemDriverDatabase systemDriverDatabase;

    OSCARServiceModule oscarServiceModule;
    RequestReportControl reportControl;
    OSCARSystem system;
    BucketService bucketService;

    // CSAPI
    ConSysApiService api;
    String apiRootUrl;
    IHttpServer httpServer;
    HttpClient httpClient;

    String reportControlApiID;

    public static Instant now = Instant.now();
    public static Instant begin = now.minus(365, ChronoUnit.DAYS);
    public static Instant end = now;


    @Before
    public void setup() throws SensorHubException, InterruptedException {
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();

        loadHTTPServer();
        loadConSysApiService();

//        var systemDatabaseConfig = createSystemDataBaseConfig();
//        loadSystemDatabase(systemDatabaseConfig);

        var bucketServiceConfig = createBucketServiceConfig();
        var serviceConfig = createOscarServiceConfig();

        loadAndStartBucketService(bucketServiceConfig);
        loadAndStartOscarService(serviceConfig);

        httpClient = HttpClient.newHttpClient();
        // Sleep to wait for event bus
        Thread.sleep(200);
        var commandStreamKey = hub.getDatabaseRegistry().getFederatedDatabase().getCommandStreamStore().getLatestVersionKey(system.getUniqueIdentifier(), RequestReportControl.NAME);
        assertNotNull(commandStreamKey);
        reportControlApiID = hub.getIdEncoders().getCommandStreamIdEncoder().encodeID(commandStreamKey.getInternalID());
        System.out.println(reportControlApiID);
    }

    private void loadHTTPServer() throws SensorHubException {
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.autoStart = true;
        httpConfig.moduleClass = HttpServer.class.getCanonicalName();
        httpConfig.id = UUID.randomUUID().toString();
        httpConfig.httpPort = freePort();
        httpServer = (IHttpServer) reg.loadModule(httpConfig);
        assertTrue(httpServer.waitForState(ModuleEvent.ModuleState.STARTED, 5000));
    }

    private static int freePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadConSysApiService() throws SensorHubException {
        ConSysApiServiceConfig swaCfg = new ConSysApiServiceConfig();
        swaCfg.endPoint = "/api";
        swaCfg.name = "ConSys API Service";
        swaCfg.autoStart = true;
        api = (ConSysApiService)reg.loadModule(swaCfg);
        apiRootUrl = httpServer.getPublicEndpointUrl(swaCfg.endPoint);
    }

    private void loadAndStartBucketService(BucketServiceConfig config) throws SensorHubException {
        bucketService = (BucketService) reg.loadModule(config);

        reg.startModule(bucketService.getLocalID());
        var isStarted = bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        assertTrue(isStarted);
    }

    private SystemDriverDatabaseConfig createSystemDataBaseConfig() throws SensorHubException {
//        MVObsSystemDatabaseConfig mvObsSystemDatabaseConfig = (MVObsSystemDatabaseConfig) reg.createModuleConfig(new Descriptor());
//        mvObsSystemDatabaseConfig.storagePath = "my-test.db";

        SystemDriverDatabaseConfig databaseConfig = new SystemDriverDatabaseConfig();

        databaseConfig.name = "System Driver Database";
        databaseConfig.autoStart = true;
        databaseConfig.databaseNum = 6;
        databaseConfig.moduleClass = SystemDriverDatabase.class.getCanonicalName();
        var config = new MVObsSystemDatabaseConfig();
        config.storagePath = "test.db";
        config.databaseNum = 7;
        databaseConfig.dbConfig = config;

        return databaseConfig;
    }

    private void loadSystemDatabase(SystemDriverDatabaseConfig databaseConfig) throws SensorHubException {
        systemDriverDatabase = (SystemDriverDatabase) reg.loadModule(databaseConfig);

        reg.startModule(systemDriverDatabase.getLocalID());
        var isStarted = systemDriverDatabase.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        assertTrue("system database should be started", isStarted);
    }

    private void loadAndStartOscarService(OSCARServiceConfig config) throws SensorHubException {
        oscarServiceModule = (OSCARServiceModule) reg.loadModule(config);
        reportControl = (RequestReportControl) oscarServiceModule.getOSCARSystem().getCommandInputs().get(RequestReportControl.NAME);
        system = oscarServiceModule.getOSCARSystem();

        reg.startModule(oscarServiceModule.getLocalID());
        var isStarted = oscarServiceModule.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        assertTrue("oscar service module should be started", isStarted);
    }

    private BucketServiceConfig createBucketServiceConfig() throws SensorHubException {
        BucketServiceConfig bucketServiceConfig = (BucketServiceConfig) reg.createModuleConfig(new com.botts.impl.service.bucket.Descriptor());
        bucketServiceConfig.fileStoreRootDir = tempBucketDir.getRoot().getAbsolutePath();

        List<String> buckets = new ArrayList<>();
        buckets.add("reports");
        buckets.add("videos");
        buckets.add("sitemap");

        bucketServiceConfig.initialBuckets = buckets;
        return bucketServiceConfig;
    }

    private OSCARServiceConfig createOscarServiceConfig() throws SensorHubException {
        OSCARServiceConfig serviceConfig = (OSCARServiceConfig) reg.createModuleConfig(new Descriptor());
        serviceConfig.autoStart = true;
        serviceConfig.siteDiagramConfig = new SiteDiagramConfig();
        serviceConfig.nodeId = "test-node-id";

        AdjudicationCodeConfig customHold = new AdjudicationCodeConfig();
        customHold.code = 15;
        customHold.label = "Port Authority Hold";
        serviceConfig.customAdjudicationCodes.add(customHold);

//        serviceConfig.databaseID = systemDriverDatabase.getLocalID();

        return serviceConfig;
    }

    @Test
    public void generateLaneReport() throws Exception {
        DataComponent commandDesc =  reportControl.getCommandDescription().copy();

        DataBlock commandData;
        commandData = commandDesc.createDataBlock();
        commandData.setStringValue(0, ReportCmdType.LANE.name());
        commandData.setTimeStamp(1, begin);
        commandData.setTimeStamp(2, end);
        commandData.setStringValue(3, "urn:osh:system:lane1");
        commandData.setStringValue(4, EventReportType.ALARMS.name());

        var res = reportControl.submitCommand(new CommandData(1, commandData)).get();

        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());

        var results = res.getResult().getInlineRecords().stream().toList();
        assertNotNull(results);

        String resPath = results.get(0).getStringValue();
        assertNotNull(resPath);
        assertFalse(resPath.contains(":"));

        assertFalse(resPath.isEmpty());
        assertTrue(oscarServiceModule.getBucketService().getBucketStore().objectExists(resPath));

    }

    @Test
    public void generateSiteReport() throws Exception {
        DataComponent commandDesc =  reportControl.getCommandDescription().copy();

        DataBlock commandData;
        commandData = commandDesc.createDataBlock();
        commandData.setStringValue(0, ReportCmdType.RDS_SITE.name());
        commandData.setTimeStamp(1, begin);
        commandData.setTimeStamp(2, end);
        commandData.setStringValue(3, "urn:osh:system:lane1");
        commandData.setStringValue(4, EventReportType.ALARMS.name());

        var res = reportControl.submitCommand(new CommandData(1, commandData)).get();
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());

        var results = res.getResult().getInlineRecords().stream().toList();
        assertNotNull(results);

        String resPath = results.get(0).getStringValue();
        assertNotNull(resPath);
        assertFalse(resPath.contains(":"));

        assertFalse(resPath.isEmpty());
        assertTrue(oscarServiceModule.getBucketService().getBucketStore().objectExists(resPath));
    }

    @Test
    public void scopedReportsUseDistinctCachePaths() throws Exception {
        DataComponent commandDesc = reportControl.getCommandDescription().copy();

        DataBlock firstCommand = commandDesc.createDataBlock();
        firstCommand.setStringValue(0, ReportCmdType.RDS_SITE.name());
        firstCommand.setTimeStamp(1, begin);
        firstCommand.setTimeStamp(2, end);
        firstCommand.setStringValue(3, "urn:osh:system:lane1");
        firstCommand.setStringValue(4, EventReportType.NONE.name());

        DataBlock secondCommand = commandDesc.createDataBlock();
        secondCommand.setStringValue(0, ReportCmdType.RDS_SITE.name());
        secondCommand.setTimeStamp(1, begin);
        secondCommand.setTimeStamp(2, end);
        secondCommand.setStringValue(3, "urn:osh:system:lane2");
        secondCommand.setStringValue(4, EventReportType.NONE.name());

        var first = reportControl.submitCommand(new CommandData(1, firstCommand)).get();
        var second = reportControl.submitCommand(new CommandData(2, secondCommand)).get();
        String firstPath = first.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
        String secondPath = second.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();

        assertNotEquals(firstPath, secondPath);
    }

    @Test
    public void generateAdjudicationReport() throws Exception {
        DataComponent commandDesc =  reportControl.getCommandDescription().copy();

        DataBlock commandData;
        commandData = commandDesc.createDataBlock();
        commandData.setStringValue(0, ReportCmdType.ADJUDICATION.name());
        commandData.setTimeStamp(1, begin);
        commandData.setTimeStamp(2, end);
        commandData.setStringValue(3, "urn:osh:system:lane1");
        commandData.setStringValue(4, EventReportType.ALARMS.name());

        var res = reportControl.submitCommand(new CommandData(1, commandData)).get();
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());

        var results = res.getResult().getInlineRecords().stream().toList();
        assertNotNull(results);

        String resPath = results.get(0).getStringValue();
        assertNotNull(resPath);
        assertFalse(resPath.contains(":"));

        assertFalse(resPath.isEmpty());
        assertTrue(oscarServiceModule.getBucketService().getBucketStore().objectExists(resPath));
    }

    @Test
    public void generateEventReport() throws Exception {
        DataComponent commandDesc =  reportControl.getCommandDescription().copy();

        DataBlock commandData;
        commandData = commandDesc.createDataBlock();
        commandData.setStringValue(0, ReportCmdType.EVENT.name());
        commandData.setTimeStamp(1, begin);
        commandData.setTimeStamp(2, end);
        commandData.setStringValue(3, "urn:osh:system:lane1");
        commandData.setStringValue(4, EventReportType.ALARMS_OCCUPANCIES.name());

        var res = reportControl.submitCommand(new CommandData(1, commandData)).get();
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());

        var results = res.getResult().getInlineRecords().stream().toList();
        assertNotNull(results);

        String resPath = results.get(0).getStringValue();
        assertNotNull(resPath);
        assertFalse(resPath.contains(":"));

        assertFalse(resPath.isEmpty());
        assertTrue(oscarServiceModule.getBucketService().getBucketStore().objectExists(resPath));
    }

    @Test
    public void compareCounts() throws Exception {
        long gammaCount1 = iterateCount();
        long gammaCount2 = predicateCount();

        assertEquals("Counts should be equal", gammaCount1, gammaCount2);
    }

    public long iterateCount() {
        var query = oscarServiceModule.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(RADHelper.DEF_OCCUPANCY)
                        .withValidTimeDuring(begin, end)
                        .build())
                .build()).iterator();

        var gammaCount = 0;
        while (query.hasNext()) {
            var entry  = query.next();

            var result = entry.getResult();

            var gamma = result.getBooleanValue(5);
            var neutron = result.getBooleanValue(6);

            if(gamma && !neutron){
                gammaCount++;
            }
        }
        return gammaCount;
    }

    public long predicateCount(){
        return Utils.countObservations(oscarServiceModule, Utils.gammaAlarmCQL, begin, end, RADHelper.DEF_OCCUPANCY);
    }


    // Connected Systems request tests
    private HttpRequest createSiteReportControlRequest() throws URISyntaxException {
        System.out.println(apiRootUrl);
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(
                        createCommandParams(ReportCmdType.RDS_SITE, "2025-08-30T17:37:14.753Z", "2026-10-10T17:17:14.753Z", "hey", EventReportType.ALARMS)))
                .uri(new URI(apiRootUrl + "/controlstreams/" + reportControlApiID + "/commands"))
                .header("Content-Type", "application/json")
                .build();
        return req;
    }

    private HttpRequest createLaneReportControlRequest() throws URISyntaxException {
        System.out.println(apiRootUrl);
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(
                        createCommandParams(ReportCmdType.LANE, "2025-08-30T17:37:14.753Z", "2026-10-01T17:17:14.753Z", "hey", EventReportType.ALARMS)))
                .uri(new URI(apiRootUrl + "/controlstreams/" + reportControlApiID + "/commands"))
                .header("Content-Type", "application/json")
                .build();
        return req;
    }

    private HttpRequest createEventReportControlRequest() throws URISyntaxException {
        System.out.println(apiRootUrl);
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(
                        createCommandParams(ReportCmdType.EVENT, "2025-08-30T17:37:14.753Z", "2026-10-01T17:17:14.753Z", "hey", EventReportType.ALARMS)))
                .uri(new URI(apiRootUrl + "/controlstreams/" + reportControlApiID + "/commands"))
                .header("Content-Type", "application/json")
                .build();
        return req;
    }

    private HttpRequest createAdjReportControlRequest() throws URISyntaxException {
        System.out.println(apiRootUrl);
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(
                        createCommandParams(ReportCmdType.ADJUDICATION, "2025-08-30T17:37:14.753Z", "2026-10-01T17:17:14.753Z", "hey", EventReportType.ALARMS)))
                .uri(new URI(apiRootUrl + "/controlstreams/" + reportControlApiID + "/commands"))
                .header("Content-Type", "application/json")
                .build();
        return req;
    }

    private String createCommandParams(ReportCmdType cmdType, String start, String end, String laneUID, EventReportType eventType) {
        return
        "{\n" +
                "    \"parameters\": {\n" +
                "        \"reportType\": \"" + cmdType.name() + "\",\n" +
                "        \"startDateTime\": \"" + start + "\",\n" +
                "        \"endDateTime\": \"" + end + "\",\n" +
                "        \"laneUID\": \"" + laneUID +"\",\n" +
                "        \"eventType\": \"" + eventType.name() + "\"\n" +
                "    }\n" +
                "}";
    }

    @Test
    public void testSendSiteReportCommand() throws URISyntaxException, IOException, InterruptedException {
        var resp = httpClient.send(createSiteReportControlRequest(), HttpResponse.BodyHandlers.ofString());
        System.out.println(resp.body());
        assertEquals(resp.statusCode(), 200);
    }

    @Test
    public void testSendLaneReportCommand() throws URISyntaxException, IOException, InterruptedException {
        var resp = httpClient.send(createLaneReportControlRequest(), HttpResponse.BodyHandlers.ofString());
        assertEquals(resp.statusCode(), 200);
    }

    @Test
    public void testSendEventReportCommand() throws URISyntaxException, IOException, InterruptedException {
        var resp = httpClient.send(createEventReportControlRequest(), HttpResponse.BodyHandlers.ofString());
        assertEquals(resp.statusCode(), 200);
    }
    @Test
    public void testSendAdjudicationReportCommand() throws URISyntaxException, IOException, InterruptedException {
        var resp = httpClient.send(createAdjReportControlRequest(), HttpResponse.BodyHandlers.ofString());
        assertEquals(resp.statusCode(), 200);
    }

    @Test
    public void testSendAdjudicationDetailsExtractCommands() throws Exception {
        for (ReportCmdType cmdType : List.of(
                ReportCmdType.ADJUDICATION_DETAILS,
                ReportCmdType.ADJUDICATION_DETAILS_CSV,
                ReportCmdType.ADJUDICATION_DETAILS_JSON,
                ReportCmdType.ADJUDICATION_DETAILS_XLSX
        )) {
            HttpRequest req = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(
                            createCommandParams(cmdType, "2025-08-30T17:37:14.753Z", "2026-10-01T17:17:14.753Z", "hey", EventReportType.NONE)))
                    .uri(new URI(apiRootUrl + "/controlstreams/" + reportControlApiID + "/commands"))
                    .header("Content-Type", "application/json")
                    .build();
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals("Expected HTTP 200 for " + cmdType, 200, resp.statusCode());
        }
    }

    private static class TestAdjudicationControl extends AdjudicationControl {
        TestAdjudicationControl(LaneSystem lane) {
            super(lane);
        }
    }

    private String readBucketPdfText(String relativePath) throws Exception {
        String key = relativePath.startsWith("reports/") ? relativePath.substring("reports/".length()) : relativePath;
        try (InputStream in = oscarServiceModule.getBucketService().getBucketStore().getObject("reports", key);
             PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    public void eventSubtypesUseDistinctPathsAndPreserveEventTypeHeader() throws Exception {
        Instant windowStart = Instant.parse("2026-11-01T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-11-03T00:00:00Z");
        DataComponent commandDesc = reportControl.getCommandDescription().copy();

        Map<EventReportType, String> paths = new EnumMap<>(EventReportType.class);
        int cmdId = 100;
        for (EventReportType subtype : List.of(EventReportType.ALARMS, EventReportType.SOH, EventReportType.ALARMS_OCCUPANCIES)) {
            DataBlock cmd = commandDesc.createDataBlock();
            cmd.setStringValue(0, ReportCmdType.EVENT.name());
            cmd.setTimeStamp(1, windowStart);
            cmd.setTimeStamp(2, windowEnd);
            cmd.setStringValue(3, "urn:osh:system:lane:lane-50");
            cmd.setStringValue(4, subtype.name());

            var status = reportControl.submitCommand(new CommandData(cmdId++, cmd)).get();
            assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, status.getStatusCode());
            String path = status.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
            assertTrue(path.endsWith(".pdf"));
            paths.put(subtype, path);

            String text = readBucketPdfText(path);
            assertTrue(text.contains("Event Report"));
            assertTrue(text.contains("Event Type: " + subtype.name()));
        }

        assertEquals("Each EventReportType subtype must write to a distinct PDF path", 3, new HashSet<>(paths.values()).size());
    }

    @Test
    public void populatedReportsRenderExactCountsDecimalRatesCustomCodesAndOverwriteFreshly() throws Exception {
        Path dbFile = Files.createTempFile("oscar-report-test-", ".dat");
        try {
            var dbConfig = (MVObsSystemDatabaseConfig) reg.createModuleConfig(new MVObsSystemDatabaseDescriptor());
            dbConfig.id = UUID.randomUUID().toString();
            dbConfig.databaseNum = 42;
            dbConfig.storagePath = dbFile.toString();
            var dbModule = reg.loadModule(dbConfig);
            dbModule.init();
            dbModule.start();
            IObsSystemDatabase db = (IObsSystemDatabase) dbModule;

            String laneUid = "urn:osh:system:lane:lane-50";
            var sml = new SMLHelper();
            var sysProcess = sml.createPhysicalSystem()
                    .uniqueID(laneUid)
                    .name("Lane 50")
                    .build();
            BigId laneSysId = db.getSystemDescStore().add(new SystemWrapper(sysProcess)).getInternalID();

            @SuppressWarnings({"rawtypes", "unchecked"})
            DataComponent occStruct = new OccupancyOutput(new SensorSystem()).getRecordDescription();
            BigId occDsId = db.getObservationStore().getDataStreams().add(new DataStreamInfo.Builder()
                    .withSystem(new FeatureId(laneSysId, laneUid))
                    .withName("occupancy")
                    .withRecordDescription(occStruct)
                    .withRecordEncoding(new TextEncodingImpl())
                    .build()).getInternalID();

            LaneSystem dummyLane = new LaneSystem();
            dummyLane.setParentHub(hub);
            TestAdjudicationControl adjControl = new TestAdjudicationControl(dummyLane);

            BigId cmdStreamId = db.getCommandStore().getCommandStreams().add(new CommandStreamInfo.Builder()
                    .withSystem(new FeatureId(laneSysId, laneUid))
                    .withName(AdjudicationControl.NAME)
                    .withRecordDescription(adjControl.getCommandDescription())
                    .withRecordEncoding(new TextEncodingImpl())
                    .withResultDescription(adjControl.getResultDescription())
                    .withResultEncoding(new TextEncodingImpl())
                    .build()).getInternalID();

            Instant windowStart = Instant.parse("2026-11-05T00:00:00Z");
            Instant windowEnd = Instant.parse("2026-11-06T00:00:00Z");

            // Insert 5 occupancies: 2 Gamma alarms (1 EML-suppressed via adjudicatedIds), 3 non-alarming -> 40.00% Alarm Rate, 50.00% EML Alarm Rate
            for (int i = 0; i < 5; i++) {
                Instant t = windowStart.plusSeconds(600L * (i + 1));
                Occupancy occ = new Occupancy.Builder()
                        .samplingTime(t.getEpochSecond())
                        .occupancyCount(i + 1)
                        .startTime(t.getEpochSecond() - 5)
                        .endTime(t.getEpochSecond())
                        .gammaAlarm(i < 2)
                        .neutronAlarm(false)
                        .maxGammaCount(i < 2 ? 2400 : 120)
                        .adjudicatedIds(i == 1 ? List.of(Adjudication.EML_NON_THREAT_MARKER) : List.of())
                        .build();
                db.getObservationStore().add(new ObsData.Builder()
                        .withDataStream(occDsId)
                        .withPhenomenonTime(t)
                        .withResultTime(t)
                        .withResult(Occupancy.fromOccupancy(occ))
                        .build());
            }

            // Record 2 completed adjudications: 1 custom code (15) + 1 EML automated release (9999)
            var createResultMethod = AdjudicationControl.class.getDeclaredMethod("createResultData", Adjudication.class, String.class);
            createResultMethod.setAccessible(true);

            Adjudication customAdj = new Adjudication.Builder()
                    .feedback("Referred to port supervisor")
                    .adjudicationCode(15)
                    .isotopes(List.of("Cesium137"))
                    .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.COMPLETED)
                    .filePaths(List.of("/buckets/reports/spec1.n42"))
                    .occupancyObsId("obs-custom-15")
                    .vehicleId("TRK-150")
                    .build();
            BigId cmdKey1 = db.getCommandStore().add(new CommandData.Builder()
                    .withCommandStream(cmdStreamId)
                    .withIssueTime(windowStart.plusSeconds(900))
                    .withSender("operator1")
                    .withParams(Adjudication.fromAdjudication(customAdj))
                    .build());
            DataBlock resBlock1 = (DataBlock) createResultMethod.invoke(adjControl, customAdj, "operator1");
            db.getCommandStatusStore().add(new CommandStatus.Builder()
                    .withCommand(cmdKey1)
                    .withReportTime(windowStart.plusSeconds(905))
                    .withStatusCode(ICommandStatus.CommandStatusCode.COMPLETED)
                    .withResult(CommandResult.withData(resBlock1))
                    .build());

            EmlEvaluationResult emlEval = new EmlEvaluationResult(
                    "1.0.2", "ERNIE", 0.50, "PORT-1", 50L, windowStart.plusSeconds(1500).toEpochMilli(), 2L,
                    "ALARM", true, false, false, "RELEASE", false, "",
                    0.02, 0.98, false, false, 5, 18.2, "",
                    List.of(),
                    new EmlSourceBreakdown("NORM", "ERNIE", 0, 0, 0, 0, 0.03, 0.95, 0.02)
            );
            Adjudication emlAdj = new Adjudication.Builder()
                    .feedback(emlEval.toAdjudicationFeedback())
                    .adjudicationCode(Adjudication.EML_NON_THREAT_CODE)
                    .isotopes(List.of("Potassium"))
                    .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.NONE)
                    .filePaths(List.of())
                    .occupancyObsId("obs-eml-9999")
                    .vehicleId("TRK-999")
                    .build();
            BigId cmdKey2 = db.getCommandStore().add(new CommandData.Builder()
                    .withCommandStream(cmdStreamId)
                    .withIssueTime(windowStart.plusSeconds(1500))
                    .withSender("EML")
                    .withParams(Adjudication.fromAdjudication(emlAdj))
                    .build());
            DataBlock resBlock2 = (DataBlock) createResultMethod.invoke(adjControl, emlAdj, "EML");
            db.getCommandStatusStore().add(new CommandStatus.Builder()
                    .withCommand(cmdKey2)
                    .withReportTime(windowStart.plusSeconds(1502))
                    .withStatusCode(ICommandStatus.CommandStatusCode.COMPLETED)
                    .withResult(CommandResult.withData(resBlock2))
                    .build());

            DataComponent commandDesc = reportControl.getCommandDescription().copy();

            // 1. Verify Lane Report renders decimal rate 40.00%, then overwrites to 33.33% after a 6th occupancy arrives
            DataBlock laneCmd = commandDesc.createDataBlock();
            laneCmd.setStringValue(0, ReportCmdType.LANE.name());
            laneCmd.setTimeStamp(1, windowStart);
            laneCmd.setTimeStamp(2, windowEnd);
            laneCmd.setStringValue(3, laneUid);
            laneCmd.setStringValue(4, EventReportType.NONE.name());

            var laneRes1 = reportControl.submitCommand(new CommandData(201, laneCmd)).get();
            String lanePdfPath = laneRes1.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
            String laneText1 = readBucketPdfText(lanePdfPath);
            assertTrue(laneText1.contains("Lane Report"));
            assertTrue(laneText1.contains("lane-50"));
            assertTrue("Decimal alarm occupancy rate should be rendered instead of integer-truncated 0", laneText1.contains("40.00%"));

            // Add 3 more non-alarming occupancies (total 8 occupancies, 2 alarms -> 25.00%) and re-run same report
            for (int i = 5; i < 8; i++) {
                Instant t = windowStart.plusSeconds(600L * (i + 1));
                Occupancy occ = new Occupancy.Builder()
                        .samplingTime(t.getEpochSecond())
                        .occupancyCount(i + 1)
                        .startTime(t.getEpochSecond() - 5)
                        .endTime(t.getEpochSecond())
                        .gammaAlarm(false)
                        .neutronAlarm(false)
                        .maxGammaCount(110)
                        .build();
                db.getObservationStore().add(new ObsData.Builder()
                        .withDataStream(occDsId)
                        .withPhenomenonTime(t)
                        .withResultTime(t)
                        .withResult(Occupancy.fromOccupancy(occ))
                        .build());
            }

            var laneRes2 = reportControl.submitCommand(new CommandData(202, laneCmd)).get();
            String lanePdfPath2 = laneRes2.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
            assertEquals(lanePdfPath, lanePdfPath2);
            String laneText2 = readBucketPdfText(lanePdfPath2);
            assertTrue("Re-generated report must overwrite cached PDF with updated 25.00% rate", laneText2.contains("25.00%"));

            // 2. Verify Adjudication Report is Portrait summary-only and omits zero-count isotopes
            DataBlock adjCmd = commandDesc.createDataBlock();
            adjCmd.setStringValue(0, ReportCmdType.ADJUDICATION.name());
            adjCmd.setTimeStamp(1, windowStart);
            adjCmd.setTimeStamp(2, windowEnd);
            adjCmd.setStringValue(3, laneUid);
            adjCmd.setStringValue(4, EventReportType.NONE.name());

            var adjRes = reportControl.submitCommand(new CommandData(203, adjCmd)).get();
            String adjPdfPath = adjRes.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
            String adjKey = adjPdfPath.startsWith("reports/") ? adjPdfPath.substring("reports/".length()) : adjPdfPath;
            try (InputStream in = oscarServiceModule.getBucketService().getBucketStore().getObject("reports", adjKey);
                 PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
                var lastPage = doc.getPage(doc.getNumberOfPages() - 1);
                assertEquals("Summary Adjudication Report pages should stay Portrait (612 pt wide)", 612f, lastPage.getMediaBox().getWidth(), 0.5f);

                String adjText = new PDFTextStripper().getText(doc);
                assertTrue(adjText.contains("Adjudication Report"));
                assertTrue(adjText.contains("Disposition - All Selected Lanes"));
                assertTrue(adjText.contains("Isotope Results - All Selected Lanes"));
                assertTrue(adjText.contains("lane-50"));
                assertTrue(
                        "All Selected Lanes summary must appear before the first individual lane section",
                        adjText.indexOf("Disposition - All Selected Lanes") < adjText.indexOf("Disposition - " + laneUid)
                );
                assertTrue(adjText.contains("Code 15: Port Authority Hold"));
                assertTrue(adjText.contains("EML Non-Threat"));
                assertTrue(adjText.contains("Cs-137 (Cesium137)"));
                assertTrue(adjText.contains("K (Potassium)"));
                assertFalse("Zero-count isotopes should be excluded from summary plots and tables", adjText.contains("Np (Neptunium)"));
                assertFalse("Zero-count isotopes should be excluded from summary plots and tables", adjText.contains("Xe (Xenon)"));
                assertFalse("Adjudication Details must be in its own separate report", adjText.contains("TRK-150"));
            }

            // 2b. Verify separate Adjudication Details Report in all 4 formats: PDF, CSV, JSON, and native XLSX
            DataBlock adjDetailsPdfCmd = commandDesc.createDataBlock();
            adjDetailsPdfCmd.setStringValue(0, ReportCmdType.ADJUDICATION_DETAILS.name());
            adjDetailsPdfCmd.setTimeStamp(1, windowStart);
            adjDetailsPdfCmd.setTimeStamp(2, windowEnd);
            adjDetailsPdfCmd.setStringValue(3, laneUid);
            adjDetailsPdfCmd.setStringValue(4, EventReportType.NONE.name());
            var adjDetailsPdfRes = reportControl.submitCommand(new CommandData(207, adjDetailsPdfCmd)).get();
            String adjDetailsPdfPath = adjDetailsPdfRes.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
            assertTrue(adjDetailsPdfPath.endsWith(".pdf"));
            String adjDetailsPdfKey = adjDetailsPdfPath.startsWith("reports/") ? adjDetailsPdfPath.substring("reports/".length()) : adjDetailsPdfPath;
            try (InputStream in = oscarServiceModule.getBucketService().getBucketStore().getObject("reports", adjDetailsPdfKey);
                 PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
                assertEquals("Adjudication Details PDF should be Landscape (792 pt wide)", 792f, doc.getPage(0).getMediaBox().getWidth(), 0.5f);
                String detailsText = new PDFTextStripper().getText(doc);
                assertTrue(detailsText.contains("Adjudication Details Report"));
                assertTrue(detailsText.contains("TRK-150"));
                assertTrue(detailsText.contains("TRK-999"));
                assertTrue(detailsText.contains("Source: NORM"));
                assertFalse(detailsText.contains("\"probabilityNonEmitting\""));
            }

            DataBlock adjDetailsCsvCmd = commandDesc.createDataBlock();
            adjDetailsCsvCmd.setStringValue(0, ReportCmdType.ADJUDICATION_DETAILS_CSV.name());
            adjDetailsCsvCmd.setTimeStamp(1, windowStart);
            adjDetailsCsvCmd.setTimeStamp(2, windowEnd);
            adjDetailsCsvCmd.setStringValue(3, laneUid);
            adjDetailsCsvCmd.setStringValue(4, EventReportType.NONE.name());
            var adjDetailsCsvRes = reportControl.submitCommand(new CommandData(208, adjDetailsCsvCmd)).get();
            String csvPath = adjDetailsCsvRes.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
            assertTrue(csvPath.endsWith(".csv"));
            String csvKey = csvPath.startsWith("reports/") ? csvPath.substring("reports/".length()) : csvPath;
            try (InputStream in = oscarServiceModule.getBucketService().getBucketStore().getObject("reports", csvKey)) {
                String csv = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(csv.contains("Lane Name,Lane UID,Record #,Time (UTC)"));
                assertTrue(csv.contains("TRK-150"));
                assertTrue(csv.contains("TRK-999"));
                assertTrue(csv.contains("NORM"));
            }

            DataBlock adjDetailsJsonCmd = commandDesc.createDataBlock();
            adjDetailsJsonCmd.setStringValue(0, ReportCmdType.ADJUDICATION_DETAILS_JSON.name());
            adjDetailsJsonCmd.setTimeStamp(1, windowStart);
            adjDetailsJsonCmd.setTimeStamp(2, windowEnd);
            adjDetailsJsonCmd.setStringValue(3, laneUid);
            adjDetailsJsonCmd.setStringValue(4, EventReportType.NONE.name());
            var adjDetailsJsonRes = reportControl.submitCommand(new CommandData(209, adjDetailsJsonCmd)).get();
            String jsonPath = adjDetailsJsonRes.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
            assertTrue(jsonPath.endsWith(".json"));
            String jsonKey = jsonPath.startsWith("reports/") ? jsonPath.substring("reports/".length()) : jsonPath;
            try (InputStream in = oscarServiceModule.getBucketService().getBucketStore().getObject("reports", jsonKey)) {
                JsonObject root = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("ADJUDICATION_DETAILS", root.get("reportType").getAsString());
                assertEquals(2L, root.getAsJsonObject("summary").get("totalRecords").getAsLong());
                JsonArray records = root.getAsJsonArray("records");
                assertEquals(2, records.size());
                assertEquals("TRK-150", records.get(0).getAsJsonObject().get("vehicleId").getAsString());
                assertEquals("NORM", records.get(1).getAsJsonObject().getAsJsonObject("eml").get("sourceType").getAsString());
            }

            DataBlock adjDetailsXlsxCmd = commandDesc.createDataBlock();
            adjDetailsXlsxCmd.setStringValue(0, ReportCmdType.ADJUDICATION_DETAILS_XLSX.name());
            adjDetailsXlsxCmd.setTimeStamp(1, windowStart);
            adjDetailsXlsxCmd.setTimeStamp(2, windowEnd);
            adjDetailsXlsxCmd.setStringValue(3, laneUid);
            adjDetailsXlsxCmd.setStringValue(4, EventReportType.NONE.name());
            var adjDetailsXlsxRes = reportControl.submitCommand(new CommandData(210, adjDetailsXlsxCmd)).get();
            String xlsxPath = adjDetailsXlsxRes.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
            assertTrue(xlsxPath.endsWith(".xlsx"));
            String xlsxKey = xlsxPath.startsWith("reports/") ? xlsxPath.substring("reports/".length()) : xlsxPath;
            assertEquals(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    oscarServiceModule.getBucketService().getBucketStore().getObjectMimeType("reports", xlsxKey)
            );
            try (InputStream in = oscarServiceModule.getBucketService().getBucketStore().getObject("reports", xlsxKey);
                 ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(in.readAllBytes()), StandardCharsets.UTF_8)) {
                Map<String, String> zipParts = new HashMap<>();
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    zipParts.put(entry.getName(), new String(zis.readAllBytes(), StandardCharsets.UTF_8));
                }
                assertTrue(zipParts.containsKey("[Content_Types].xml"));
                assertTrue(zipParts.containsKey("xl/workbook.xml"));
                assertTrue(zipParts.containsKey("xl/styles.xml"));
                assertTrue(zipParts.containsKey("xl/worksheets/sheet1.xml"));
                assertTrue(zipParts.get("xl/worksheets/sheet1.xml").contains("TRK-150"));
                assertTrue(zipParts.get("xl/worksheets/sheet1.xml").contains("TRK-999"));
            }

            // 3. Verify populated RDS Site Report renders decimal alarm rate, 50.00% EML alarm rate (1 of 2 alarms), and storage volumes
            DataBlock siteCmd = commandDesc.createDataBlock();
            siteCmd.setStringValue(0, ReportCmdType.RDS_SITE.name());
            siteCmd.setTimeStamp(1, windowStart);
            siteCmd.setTimeStamp(2, windowEnd);
            siteCmd.setStringValue(3, laneUid);
            siteCmd.setStringValue(4, EventReportType.NONE.name());
            var siteRes = reportControl.submitCommand(new CommandData(204, siteCmd)).get();
            String siteText = readBucketPdfText(siteRes.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue());
            assertTrue(siteText.contains("RDS Site Report"));
            assertTrue(siteText.contains("25.00%"));
            assertTrue("EML Alarm Rate should reflect 1 EML-suppressed occupancy out of 2 alarms (50.00%)", siteText.contains("50.00%"));
            assertTrue(siteText.contains("Storage Availability"));

            // 4. Verify populated 1-day and 5-day Event Reports (ALARMS_OCCUPANCIES)
            DataBlock event1dCmd = commandDesc.createDataBlock();
            event1dCmd.setStringValue(0, ReportCmdType.EVENT.name());
            event1dCmd.setTimeStamp(1, windowStart);
            event1dCmd.setTimeStamp(2, windowEnd);
            event1dCmd.setStringValue(3, laneUid);
            event1dCmd.setStringValue(4, EventReportType.ALARMS_OCCUPANCIES.name());
            var event1dRes = reportControl.submitCommand(new CommandData(205, event1dCmd)).get();
            String event1dText = readBucketPdfText(event1dRes.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue());
            assertTrue(event1dText.contains("Event Report"));
            assertTrue(event1dText.contains("Event Type: ALARMS_OCCUPANCIES"));
            assertTrue(event1dText.contains("Occupancies: 8"));

            // Seed 4 prior days of varied traffic + alarms for multi-day dual-axis Event Report verification
            for (int dayOffset = 1; dayOffset <= 4; dayOffset++) {
                Instant dayBase = windowStart.minusSeconds(86400L * dayOffset);
                int dayOccCount = 40 + dayOffset * 15;
                for (int k = 0; k < dayOccCount; k++) {
                    Instant t = dayBase.plusSeconds(300L * (k + 1));
                    boolean g = (k < (dayOffset % 3) + 1);
                    boolean n = (k == 0 && dayOffset % 2 == 0);
                    Occupancy occ = new Occupancy.Builder()
                            .samplingTime(t.getEpochSecond())
                            .occupancyCount(k + 1)
                            .startTime(t.getEpochSecond() - 4)
                            .endTime(t.getEpochSecond())
                            .gammaAlarm(g)
                            .neutronAlarm(n)
                            .maxGammaCount(g ? 1950 : 105)
                            .build();
                    db.getObservationStore().add(new ObsData.Builder()
                            .withDataStream(occDsId)
                            .withPhenomenonTime(t)
                            .withResultTime(t)
                            .withResult(Occupancy.fromOccupancy(occ))
                            .build());
                }
            }

            DataBlock event5dCmd = commandDesc.createDataBlock();
            event5dCmd.setStringValue(0, ReportCmdType.EVENT.name());
            event5dCmd.setTimeStamp(1, windowStart.minusSeconds(86400L * 4));
            event5dCmd.setTimeStamp(2, windowEnd);
            event5dCmd.setStringValue(3, laneUid);
            event5dCmd.setStringValue(4, EventReportType.ALARMS_OCCUPANCIES.name());
            var event5dRes = reportControl.submitCommand(new CommandData(206, event5dCmd)).get();
            String event5dText = readBucketPdfText(event5dRes.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue());
            assertTrue(event5dText.contains("Event Report"));
            assertTrue(event5dText.contains("Total Occupancies"));

            // 5. Verify CSV, JSON, and XLSX generation across RDS_SITE, LANE, ADJUDICATION, and EVENT
            verifyMultiFormatReportExports(commandDesc, windowStart, windowEnd, laneUid);
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    private void verifyMultiFormatReportExports(
            DataComponent commandDesc,
            Instant windowStart,
            Instant windowEnd,
            String laneUid
    ) throws Exception {
        // RDS_SITE CSV / JSON / XLSX
        assertBucketTextContains(
                submitReportCommand(commandDesc, 301, ReportCmdType.RDS_SITE_CSV, windowStart, windowEnd, laneUid, EventReportType.NONE),
                ".csv",
                "Category,Metric / Volume,Value / Used",
                "Alarm Statistics,Total Occupancies,8,count"
        );
        JsonObject siteJson = readBucketJson(
                submitReportCommand(commandDesc, 302, ReportCmdType.RDS_SITE_JSON, windowStart, windowEnd, laneUid, EventReportType.NONE)
        );
        assertEquals("RDS_SITE", siteJson.get("reportType").getAsString());
        assertEquals(8L, siteJson.getAsJsonObject("summary").get("totalOccupancies").getAsLong());
        assertTrue(siteJson.getAsJsonArray("records").size() >= 13);
        assertBucketXlsxContains(
                submitReportCommand(commandDesc, 303, ReportCmdType.RDS_SITE_XLSX, windowStart, windowEnd, laneUid, EventReportType.NONE),
                "Alarm Occupancy Rate"
        );

        // LANE CSV / JSON / XLSX
        assertBucketTextContains(
                submitReportCommand(commandDesc, 304, ReportCmdType.LANE_CSV, windowStart, windowEnd, laneUid, EventReportType.NONE),
                ".csv",
                "Lane Name,Lane UID,Total Occupancies,Total Alarms",
                laneUid
        );
        JsonObject laneJson = readBucketJson(
                submitReportCommand(commandDesc, 305, ReportCmdType.LANE_JSON, windowStart, windowEnd, laneUid, EventReportType.NONE)
        );
        assertEquals("LANE", laneJson.get("reportType").getAsString());
        assertEquals(8L, laneJson.getAsJsonObject("summary").get("totalOccupancies").getAsLong());
        assertEquals(1, laneJson.getAsJsonArray("records").size());
        assertBucketXlsxContains(
                submitReportCommand(commandDesc, 306, ReportCmdType.LANE_XLSX, windowStart, windowEnd, laneUid, EventReportType.NONE),
                laneUid
        );

        // ADJUDICATION (summary) CSV / JSON / XLSX
        assertBucketTextContains(
                submitReportCommand(commandDesc, 307, ReportCmdType.ADJUDICATION_CSV, windowStart, windowEnd, laneUid, EventReportType.NONE),
                ".csv",
                "Lane Name,Lane UID,Section,Code / Isotope,Label,Group,Count,Percent (%)",
                "All Selected Lanes (1),ALL_LANES,Disposition",
                "Cs-137"
        );
        JsonObject adjJson = readBucketJson(
                submitReportCommand(commandDesc, 308, ReportCmdType.ADJUDICATION_JSON, windowStart, windowEnd, laneUid, EventReportType.NONE)
        );
        assertEquals("ADJUDICATION", adjJson.get("reportType").getAsString());
        assertEquals(2L, adjJson.getAsJsonObject("summary").get("totalRecords").getAsLong());
        assertTrue(adjJson.getAsJsonArray("records").size() > 5);
        assertEquals("ALL_LANES", adjJson.getAsJsonArray("records").get(0).getAsJsonObject().get("laneUid").getAsString());
        assertBucketXlsxContains(
                submitReportCommand(commandDesc, 309, ReportCmdType.ADJUDICATION_XLSX, windowStart, windowEnd, laneUid, EventReportType.NONE),
                "ALL_LANES"
        );

        // EVENT CSV / JSON / XLSX
        assertBucketTextContains(
                submitReportCommand(commandDesc, 310, ReportCmdType.EVENT_CSV, windowStart, windowEnd, laneUid, EventReportType.ALARMS_OCCUPANCIES),
                ".csv",
                "Date (UTC),Gamma,Gamma-Neutron,Neutron,EML-Suppressed,Total Alarms,Total Occupancy"
        );
        JsonObject eventJson = readBucketJson(
                submitReportCommand(commandDesc, 311, ReportCmdType.EVENT_JSON, windowStart, windowEnd, laneUid, EventReportType.ALARMS_OCCUPANCIES)
        );
        assertEquals("EVENT", eventJson.get("reportType").getAsString());
        assertEquals("ALARMS_OCCUPANCIES", eventJson.get("eventType").getAsString());
        assertEquals(8L, eventJson.getAsJsonObject("summary").get("totalOccupancies").getAsLong());
        assertFalse(eventJson.getAsJsonArray("records").isEmpty());
        assertBucketXlsxContains(
                submitReportCommand(commandDesc, 312, ReportCmdType.EVENT_XLSX, windowStart, windowEnd, laneUid, EventReportType.ALARMS_OCCUPANCIES),
                "Daily Event Breakdown"
        );
    }

    private String submitReportCommand(
            DataComponent commandDesc,
            int cmdId,
            ReportCmdType type,
            Instant start,
            Instant end,
            String laneUid,
            EventReportType eventType
    ) throws Exception {
        DataBlock block = commandDesc.createDataBlock();
        block.setStringValue(0, type.name());
        block.setTimeStamp(1, start);
        block.setTimeStamp(2, end);
        block.setStringValue(3, laneUid);
        block.setStringValue(4, eventType.name());
        var res = reportControl.submitCommand(new CommandData(cmdId, block)).get();
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());
        return res.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
    }

    private void assertBucketTextContains(String resPath, String expectedSuffix, String... expectedSubstrings) throws Exception {
        assertTrue("Expected " + resPath + " to end with " + expectedSuffix, resPath.endsWith(expectedSuffix));
        String key = resPath.startsWith("reports/") ? resPath.substring("reports/".length()) : resPath;
        try (InputStream in = oscarServiceModule.getBucketService().getBucketStore().getObject("reports", key)) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String expected : expectedSubstrings) {
                assertTrue("Expected report " + resPath + " to contain '" + expected + "'", content.contains(expected));
            }
        }
    }

    private JsonObject readBucketJson(String resPath) throws Exception {
        assertTrue(resPath.endsWith(".json"));
        String key = resPath.startsWith("reports/") ? resPath.substring("reports/".length()) : resPath;
        try (InputStream in = oscarServiceModule.getBucketService().getBucketStore().getObject("reports", key)) {
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private void assertBucketXlsxContains(String resPath, String expectedSubstring) throws Exception {
        assertTrue(resPath.endsWith(".xlsx"));
        String key = resPath.startsWith("reports/") ? resPath.substring("reports/".length()) : resPath;
        try (InputStream in = oscarServiceModule.getBucketService().getBucketStore().getObject("reports", key);
             ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(in.readAllBytes()), StandardCharsets.UTF_8)) {
            StringBuilder combinedXml = new StringBuilder();
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                combinedXml.append(new String(zis.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertTrue(
                    "Expected workbook " + resPath + " XML to contain '" + expectedSubstring + "'",
                    combinedXml.toString().contains(expectedSubstring)
            );
        }
    }


    @After
    public void cleanup() {
        if (hub != null)
            hub.stop();
        httpClient = null;
        httpServer = null;

    }
}
