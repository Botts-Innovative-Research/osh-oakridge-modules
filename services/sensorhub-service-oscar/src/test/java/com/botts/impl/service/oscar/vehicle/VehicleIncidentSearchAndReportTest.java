package com.botts.impl.service.oscar.vehicle;

import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServiceConfig;
import com.botts.impl.service.oscar.Descriptor;
import com.botts.impl.service.oscar.OSCARServiceConfig;
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.OSCARSystem;
import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.reports.helpers.EventReportType;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.botts.impl.system.lane.AdjudicationControl;
import com.botts.impl.system.lane.LaneSystem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
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
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseDescriptor;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.security.BasicSecurityRealmConfig;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.IsotopeAnalysis;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.OccupancyExtended;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

public class VehicleIncidentSearchAndReportTest {

    private static final String USER = "incident-operator";
    private static final String PASSWORD = "test-password";

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private SensorHub hub;
    private ModuleRegistry reg;
    private BucketService bucketService;
    private OSCARServiceModule oscarModule;
    private RequestReportControl reportControl;
    private IObsSystemDatabase db;
    private Path dbFile;
    private String bucketEndpoint;

    @Before
    public void setUp() throws Exception {
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();

        BasicSecurityRealmConfig realmConfig = new BasicSecurityRealmConfig();
        realmConfig.id = UUID.randomUUID().toString();
        realmConfig.autoStart = true;
        BasicSecurityRealmConfig.UserConfig user = new BasicSecurityRealmConfig.UserConfig();
        user.userID = USER;
        user.name = USER;
        user.password = PASSWORD;
        realmConfig.users.add(user);
        reg.loadModule(realmConfig).waitForState(ModuleEvent.ModuleState.STARTED, 5_000);

        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.id = UUID.randomUUID().toString();
        httpConfig.autoStart = true;
        httpConfig.httpPort = freePort();
        httpConfig.authMethod = HttpServerConfig.AuthMethod.BASIC;
        IHttpServer<?> httpServer = (HttpServer) reg.loadModule(httpConfig);
        assertTrue(httpServer.waitForState(ModuleEvent.ModuleState.STARTED, 5_000));

        dbFile = tempDir.newFile("vehicle-incident-test.db").toPath();
        Files.deleteIfExists(dbFile);
        MVObsSystemDatabaseConfig dbConfig = (MVObsSystemDatabaseConfig) reg.createModuleConfig(new MVObsSystemDatabaseDescriptor());
        dbConfig.id = UUID.randomUUID().toString();
        dbConfig.databaseNum = 43;
        dbConfig.storagePath = dbFile.toString();
        var dbModule = reg.loadModule(dbConfig);
        dbModule.init();
        dbModule.start();
        db = (IObsSystemDatabase) dbModule;

        BucketServiceConfig bucketConfig = (BucketServiceConfig) reg.createModuleConfig(new com.botts.impl.service.bucket.Descriptor());
        bucketConfig.fileStoreRootDir = tempDir.newFolder("buckets").getAbsolutePath();
        bucketConfig.initialBuckets = new ArrayList<>(List.of("reports", "videos", "sitemap", "adjudication"));
        bucketConfig.security.requireAuth = true;
        bucketConfig.security.enableAccessControl = false;
        bucketService = (BucketService) reg.loadModule(bucketConfig);
        reg.startModule(bucketService.getLocalID());
        assertTrue(bucketService.waitForState(ModuleEvent.ModuleState.STARTED, 5_000));
        bucketEndpoint = httpServer.getPublicEndpointUrl(bucketConfig.endPoint);

        OSCARServiceConfig oscarConfig = (OSCARServiceConfig) reg.createModuleConfig(new Descriptor());
        oscarConfig.autoStart = true;
        oscarConfig.nodeId = "primary-gate-node";
        oscarConfig.databaseID = dbModule.getLocalID();
        oscarConfig.siteDiagramConfig = new SiteDiagramConfig();
        oscarModule = (OSCARServiceModule) reg.loadModule(oscarConfig);
        reg.startModule(oscarModule.getLocalID());
        assertTrue(oscarModule.waitForState(ModuleEvent.ModuleState.STARTED, 10_000));

        OSCARSystem system = oscarModule.getOSCARSystem();
        reportControl = (RequestReportControl) system.getCommandInputs().get(RequestReportControl.NAME);
        assertNotNull(reportControl);
    }

    @After
    public void tearDown() throws Exception {
        if (hub != null) {
            hub.stop();
        }
        if (dbFile != null) {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void correlatesMultiPassRescanAndRs350SpectroscopicSopPlusFederatedIncidentReport() throws Exception {
        var sml = new SMLHelper();
        RADHelper rad = new RADHelper();

        // Primary Gross-Counting Lane 1
        String lane1Uid = "urn:osh:system:lane:lane-1";
        BigId lane1SysId = db.getSystemDescStore().add(new SystemWrapper(
                sml.createPhysicalSystem().uniqueID(lane1Uid).name("Primary Lane 1").build())).getInternalID();

        @SuppressWarnings({"rawtypes", "unchecked"})
        DataComponent grossOccStruct = new OccupancyOutput(new SensorSystem()).getRecordDescription();
        BigId lane1OccDs = db.getObservationStore().getDataStreams().add(new DataStreamInfo.Builder()
                .withSystem(new FeatureId(lane1SysId, lane1Uid))
                .withName("occupancy")
                .withRecordDescription(grossOccStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .build()).getInternalID();

        DataComponent ocrStruct = rad.createVehicleOcrRecord();
        BigId lane1OcrDs = db.getObservationStore().getDataStreams().add(new DataStreamInfo.Builder()
                .withSystem(new FeatureId(lane1SysId, lane1Uid))
                .withName("vehicleOcr")
                .withRecordDescription(ocrStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .build()).getInternalID();

        // Secondary Spectroscopic RS350 Lane 2
        String lane2Uid = "urn:osh:system:lane:rs350-secondary";
        BigId lane2SysId = db.getSystemDescStore().add(new SystemWrapper(
                sml.createPhysicalSystem().uniqueID(lane2Uid).name("RS350 Secondary").build())).getInternalID();

        DataComponent extOccStruct = OccupancyExtended.createExtendedRecordStructure();
        BigId lane2OccDs = db.getObservationStore().getDataStreams().add(new DataStreamInfo.Builder()
                .withSystem(new FeatureId(lane2SysId, lane2Uid))
                .withName("occupancy")
                .withRecordDescription(extOccStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .build()).getInternalID();

        DataComponent webIdStruct = rad.createWebIdRecord();
        BigId lane2WebIdDs = db.getObservationStore().getDataStreams().add(new DataStreamInfo.Builder()
                .withSystem(new FeatureId(lane2SysId, lane2Uid))
                .withName("webIdAnalysis")
                .withRecordDescription(webIdStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .build()).getInternalID();

        LaneSystem dummyLane = new LaneSystem();
        dummyLane.setParentHub(hub);
        AdjudicationControl adjControl = new AdjudicationControl(dummyLane) {};
        Method createResultMethod = AdjudicationControl.class.getDeclaredMethod(
                "createResultData", Adjudication.class, String.class);
        createResultMethod.setAccessible(true);

        BigId lane1CmdStream = db.getCommandStore().getCommandStreams().add(new CommandStreamInfo.Builder()
                .withSystem(new FeatureId(lane1SysId, lane1Uid))
                .withName(AdjudicationControl.NAME)
                .withRecordDescription(adjControl.getCommandDescription())
                .withRecordEncoding(new TextEncodingImpl())
                .withResultDescription(adjControl.getResultDescription())
                .withResultEncoding(new TextEncodingImpl())
                .build()).getInternalID();

        BigId lane2CmdStream = db.getCommandStore().getCommandStreams().add(new CommandStreamInfo.Builder()
                .withSystem(new FeatureId(lane2SysId, lane2Uid))
                .withName(AdjudicationControl.NAME)
                .withRecordDescription(adjControl.getCommandDescription())
                .withRecordEncoding(new TextEncodingImpl())
                .withResultDescription(adjControl.getResultDescription())
                .withResultEncoding(new TextEncodingImpl())
                .build()).getInternalID();

        Instant baseTime = Instant.parse("2026-11-14T14:00:00Z");

        // Pass #1: Primary Lane 1 Gamma Alarm with automated camera OCR only (still open/unadjudicated!)
        Instant pass1Start = baseTime.plusSeconds(60);
        Instant pass1End = pass1Start.plusSeconds(6);
        Occupancy pass1Occ = new Occupancy.Builder()
                .samplingTime(pass1End.getEpochSecond())
                .occupancyCount(101)
                .startTime(pass1Start.getEpochSecond())
                .endTime(pass1End.getEpochSecond())
                .gammaAlarm(true)
                .neutronAlarm(false)
                .maxGammaCount(2480)
                .maxNeutronCount(14)
                .videoPaths(List.of("videos/lane1_occ101.mp4"))
                .build();
        BigId pass1BigId = db.getObservationStore().add(new ObsData.Builder()
                .withDataStream(lane1OccDs)
                .withPhenomenonTime(pass1Start)
                .withResultTime(pass1End)
                .withResult(Occupancy.fromOccupancy(pass1Occ))
                .build());
        String pass1ObsId = hub.getIdEncoders().getObsIdEncoder().encodeID(pass1BigId);

        VehicleOcrResult pass1Ocr = new VehicleOcrResult.Builder()
                .sampleTime(pass1End)
                .occupancyObsId(pass1ObsId)
                .idType(VehicleOcrResult.ID_TYPE_CONTAINER)
                .value("MSCU 123456-5")
                .normalizedValue("MSCU1234565")
                .checksumValid(true)
                .confidence(0.96f)
                .readCount(4)
                .cameraUid("urn:osh:sensor:cam1")
                .frameTime(pass1Start.plusSeconds(3))
                .evidenceImagePath("ocr/lane1_occ101.jpg")
                .build();
        db.getObservationStore().add(new ObsData.Builder()
                .withDataStream(lane1OcrDs)
                .withPhenomenonTime(pass1End)
                .withResultTime(pass1End)
                .withResult(VehicleOcrResult.createDataBlock(ocrStruct, pass1Ocr))
                .build());

        // Pass #2: Confirmation Re-Scan on Primary Lane 1 (+4m 30s later), adjudicated by operator with lowercase/hyphenated ID
        Instant pass2Start = baseTime.plusSeconds(330);
        Instant pass2End = pass2Start.plusSeconds(7);
        Occupancy pass2Occ = new Occupancy.Builder()
                .samplingTime(pass2End.getEpochSecond())
                .occupancyCount(104)
                .startTime(pass2Start.getEpochSecond())
                .endTime(pass2End.getEpochSecond())
                .gammaAlarm(true)
                .neutronAlarm(false)
                .maxGammaCount(2510)
                .maxNeutronCount(15)
                .adjudicatedIds(List.of("code:2"))
                .build();
        BigId pass2BigId = db.getObservationStore().add(new ObsData.Builder()
                .withDataStream(lane1OccDs)
                .withPhenomenonTime(pass2Start)
                .withResultTime(pass2End)
                .withResult(Occupancy.fromOccupancy(pass2Occ))
                .build());
        String pass2ObsId = hub.getIdEncoders().getObsIdEncoder().encodeID(pass2BigId);

        Adjudication pass2Adj = new Adjudication.Builder()
                .feedback("Confirmation re-scan repeated gamma alarm; routing to RS350 secondary")
                .adjudicationCode(2)
                .isotopes(List.of())
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.REQUESTED)
                .filePaths(List.of())
                .occupancyObsId(pass2ObsId)
                .vehicleId("mscu-1234565")
                .build();
        recordCompletedAdjudication(lane1CmdStream, adjControl, createResultMethod, pass2Adj, "gate-op1", pass2End.plusSeconds(30));

        // Pass #3: RS350 Spectroscopic Secondary Lane 2 (+16m later) with WebID Cs-137 identification
        Instant pass3Start = baseTime.plusSeconds(1020);
        Instant pass3End = pass3Start.plusSeconds(18);
        WebIdAnalysis webId = new WebIdAnalysis.Builder()
                .sampleTime(pass3End.plusSeconds(2))
                .isotopes(List.of(new IsotopeAnalysis.Builder()
                        .name("Cs-137")
                        .type("Industrial")
                        .confidence(0.98f)
                        .confidenceStr("High")
                        .countRate(412.5f)
                        .build()))
                .isotopeString("Cs-137")
                .drf("RS350-Portal")
                .estimatedDose(0.38)
                .chi2(1.12)
                .errorMessage("")
                .analysisWarnings(List.of())
                .occupancyObsId("pending")
                .build();
        BigId webIdBigId = db.getObservationStore().add(new ObsData.Builder()
                .withDataStream(lane2WebIdDs)
                .withPhenomenonTime(pass3End.plusSeconds(2))
                .withResultTime(pass3End.plusSeconds(2))
                .withResult(WebIdAnalysis.fromWebIdAnalysis(webId))
                .build());
        String webIdObsId = hub.getIdEncoders().getObsIdEncoder().encodeID(webIdBigId);

        OccupancyExtended pass3Occ = (OccupancyExtended) new OccupancyExtended.Builder()
                .alarmCategory("Gamma-Isotope")
                .samplingTime(pass3End.getEpochSecond())
                .occupancyCount(19)
                .startTime(pass3Start.getEpochSecond())
                .endTime(pass3End.getEpochSecond())
                .gammaAlarm(true)
                .neutronAlarm(false)
                .maxGammaCount(3190)
                .maxNeutronCount(13)
                .adjudicatedIds(List.of("code:5"))
                .webIdObsIds(List.of(webIdObsId))
                .build();
        BigId pass3BigId = db.getObservationStore().add(new ObsData.Builder()
                .withDataStream(lane2OccDs)
                .withPhenomenonTime(pass3Start)
                .withResultTime(pass3End)
                .withResult(OccupancyExtended.fromOccupancy(pass3Occ))
                .build());
        String pass3ObsId = hub.getIdEncoders().getObsIdEncoder().encodeID(pass3BigId);

        Adjudication pass3Adj = new Adjudication.Builder()
                .feedback("Manifest verified for Cs-137 industrial gauge")
                .adjudicationCode(5)
                .isotopes(List.of("Cesium137"))
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.COMPLETED)
                .filePaths(List.of("/buckets/adjudication/manifest-123456.pdf"))
                .occupancyObsId(pass3ObsId)
                .vehicleId("MSCU 123456 5")
                .build();
        recordCompletedAdjudication(lane2CmdStream, adjControl, createResultMethod, pass3Adj, "sec-inspector", pass3End.plusSeconds(45));

        // Unrelated vehicle pass to verify search isolation
        Instant otherStart = baseTime.plusSeconds(500);
        Occupancy otherOcc = new Occupancy.Builder()
                .samplingTime(otherStart.plusSeconds(5).getEpochSecond())
                .occupancyCount(105)
                .startTime(otherStart.getEpochSecond())
                .endTime(otherStart.plusSeconds(5).getEpochSecond())
                .gammaAlarm(true)
                .maxGammaCount(1800)
                .build();
        BigId otherBigId = db.getObservationStore().add(new ObsData.Builder()
                .withDataStream(lane1OccDs)
                .withPhenomenonTime(otherStart)
                .withResultTime(otherStart.plusSeconds(5))
                .withResult(Occupancy.fromOccupancy(otherOcc))
                .build());
        String otherObsId = hub.getIdEncoders().getObsIdEncoder().encodeID(otherBigId);
        Adjudication otherAdj = new Adjudication.Builder()
                .feedback("NORM tile shipment")
                .adjudicationCode(4)
                .isotopes(List.of("Potassium"))
                .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.NONE)
                .filePaths(List.of())
                .occupancyObsId(otherObsId)
                .vehicleId("TRK-8800")
                .build();
        recordCompletedAdjudication(lane1CmdStream, adjControl, createResultMethod, otherAdj, "gate-op1", otherStart.plusSeconds(20));

        HttpClient client = HttpClient.newHttpClient();

        // 1. Verify authentication & read-only enforcement on /buckets/adjudication/vehicle-search
        HttpResponse<String> unauth = client.send(HttpRequest.newBuilder()
                .uri(URI.create(bucketEndpoint + "/adjudication/vehicle-search?vehicleId=MSCU1234565"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauth.statusCode());

        HttpResponse<String> postResp = client.send(HttpRequest.newBuilder()
                .uri(URI.create(bucketEndpoint + "/adjudication/vehicle-search"))
                .header("Authorization", basicAuth())
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(405, postResp.statusCode());

        // 2. Verify normalized exact search correlates all 3 passes (OCR-only Pass #1, Re-Scan Pass #2, RS350 Pass #3)
        String encodedQuery = URLEncoder.encode("mscu 123456-5", StandardCharsets.UTF_8);
        HttpResponse<String> searchResp = client.send(HttpRequest.newBuilder()
                .uri(URI.create(bucketEndpoint + "/adjudication/vehicle-search?vehicleId=" + encodedQuery + "&matchMode=equals"))
                .header("Authorization", basicAuth())
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, searchResp.statusCode());
        assertEquals("nosniff", searchResp.headers().firstValue("X-Content-Type-Options").orElse(""));

        JsonObject searchJson = JsonParser.parseString(searchResp.body()).getAsJsonObject();
        assertEquals("MSCU1234565", searchJson.get("normalizedVehicleId").getAsString());
        assertEquals(3, searchJson.get("totalMatched").getAsInt());

        JsonObject summary = searchJson.getAsJsonObject("summary");
        assertEquals(3, summary.get("totalPasses").getAsInt());
        assertEquals(3, summary.get("alarmingPasses").getAsInt());
        assertEquals(1, summary.get("openAlarmPasses").getAsInt());
        assertEquals(1, summary.get("spectroscopicPasses").getAsInt());
        assertEquals(2, summary.get("distinctLanes").getAsInt());

        JsonArray passes = searchJson.getAsJsonArray("passes");
        assertEquals(3, passes.size());
        assertEquals(pass1ObsId, passes.get(0).getAsJsonObject().get("occupancyObsId").getAsString());
        assertEquals("ocr", passes.get(0).getAsJsonObject().get("vehicleIdSource").getAsString());
        assertEquals("Gross-Counting", passes.get(0).getAsJsonObject().get("systemType").getAsString());

        assertEquals(pass2ObsId, passes.get(1).getAsJsonObject().get("occupancyObsId").getAsString());

        // 2b. Verify bounded isNotEmpty search preserves totalMatched=4, sets truncated=true,
        // and returns the 2 newest passes in chronological ascending order without scanning cold occupancies beyond limit
        HttpResponse<String> boundedWildcardResp = client.send(HttpRequest.newBuilder()
                .uri(URI.create(bucketEndpoint + "/adjudication/vehicle-search?matchMode=isNotEmpty&limit=2"))
                .header("Authorization", basicAuth())
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, boundedWildcardResp.statusCode());
        JsonObject boundedWildcardJson = JsonParser.parseString(boundedWildcardResp.body()).getAsJsonObject();
        assertEquals(4, boundedWildcardJson.get("totalMatched").getAsInt());
        assertTrue(boundedWildcardJson.get("truncated").getAsBoolean());
        JsonArray boundedPasses = boundedWildcardJson.getAsJsonArray("passes");
        assertEquals(2, boundedPasses.size());
        assertEquals("TRK-8800", boundedPasses.get(0).getAsJsonObject().get("vehicleId").getAsString());
        assertEquals(otherObsId, boundedPasses.get(0).getAsJsonObject().get("occupancyObsId").getAsString());
        assertEquals("MSCU 123456 5", boundedPasses.get(1).getAsJsonObject().get("vehicleId").getAsString());
        assertEquals("MSCU1234565", boundedPasses.get(1).getAsJsonObject().get("normalizedVehicleId").getAsString());
        assertEquals(pass3ObsId, boundedPasses.get(1).getAsJsonObject().get("occupancyObsId").getAsString());
        assertEquals("adjudication", passes.get(1).getAsJsonObject().get("vehicleIdSource").getAsString());

        assertEquals(pass3ObsId, passes.get(2).getAsJsonObject().get("occupancyObsId").getAsString());
        assertEquals("RS350 Spectroscopic", passes.get(2).getAsJsonObject().get("systemType").getAsString());
        assertEquals("Gamma-Isotope", passes.get(2).getAsJsonObject().get("alarmCategoryCode").getAsString());
        assertTrue(passes.get(2).getAsJsonObject().getAsJsonArray("confirmedIsotopes").toString().contains("Cs-137"));

        // 3. Verify VEHICLE_INCIDENT report in all 4 formats INCLUDING a federated remote pass bundle!
        String envelopeJson = """
                {
                  "vehicleId": "MSCU 123456-5",
                  "matchMode": "equals",
                  "federatedPasses": [
                    {
                      "nodeId": "federated-exit-node",
                      "nodeName": "South Exit Portal",
                      "laneUid": "urn:osh:system:lane:south-exit-1",
                      "laneName": "South Exit Lane 1",
                      "occupancyObsId": "remote-obs-77",
                      "occupancyCount": 77,
                      "systemType": "Gross-Counting",
                      "startTime": "2026-11-14T14:28:00Z",
                      "endTime": "2026-11-14T14:28:06Z",
                      "samplingTime": "2026-11-14T14:28:06Z",
                      "durationSeconds": 6.0,
                      "gammaAlarm": true,
                      "neutronAlarm": false,
                      "maxGammaCount": 2390,
                      "maxNeutronCount": 12,
                      "vehicleId": "MSCU 123456-5",
                      "normalizedVehicleId": "MSCU1234565",
                      "vehicleIdSource": "both",
                      "adjudications": [
                        {
                          "timeUtc": "2026-11-14T14:28:35Z",
                          "username": "exit-supervisor",
                          "adjudicationCode": 5,
                          "dispositionLabel": "Code 5: Declared Shipment of Radioactive Material",
                          "dispositionGroup": "Innocent Alarm",
                          "secondaryInspectionStatus": "COMPLETED",
                          "vehicleId": "MSCU 123456-5",
                          "isotopes": ["Cesium137"],
                          "summary": "Cleared against primary gate RS350 inspection"
                        }
                      ]
                    }
                  ]
                }
                """;

        DataComponent cmdDesc = reportControl.getCommandDescription().copy();
        Instant winStart = baseTime;
        Instant winEnd = baseTime.plusSeconds(3600);

        // PDF
        String pdfPath = submitReport(cmdDesc, 401, ReportCmdType.VEHICLE_INCIDENT_PDF, winStart, winEnd, envelopeJson);
        assertTrue(pdfPath.endsWith(".pdf"));
        assertTrue(pdfPath.contains("veh-mscu1234565-"));
        try (InputStream in = readReportObject(pdfPath);
             PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            assertEquals(792f, doc.getPage(0).getMediaBox().getWidth(), 0.5f);
            String pdfText = new PDFTextStripper().getText(doc);
            assertTrue(pdfText.contains("Vehicle Incident Report"));
            assertTrue(pdfText.contains("Vehicle ID: MSCU 123456-5"));
            assertTrue(pdfText.contains("Primary Lane 1"));
            assertTrue(pdfText.contains("RS350 Secondary"));
            assertTrue(pdfText.contains("South Exit Lane 1"));
            assertTrue(pdfText.contains("Cs-137 (High)"));
        }

        // CSV
        String csvPath = submitReport(cmdDesc, 402, ReportCmdType.VEHICLE_INCIDENT_CSV, winStart, winEnd, envelopeJson);
        assertTrue(csvPath.endsWith(".csv"));
        try (InputStream in = readReportObject(csvPath)) {
            String csv = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(csv.contains("Pass #,Node ID,Lane Name,Lane UID,System Type"));
            assertTrue(csv.contains("primary-gate-node"));
            assertTrue(csv.contains("federated-exit-node"));
            assertTrue(csv.contains("RS350 Spectroscopic"));
        }

        // JSON
        String jsonPath = submitReport(cmdDesc, 403, ReportCmdType.VEHICLE_INCIDENT_JSON, winStart, winEnd, envelopeJson);
        assertTrue(jsonPath.endsWith(".json"));
        try (InputStream in = readReportObject(jsonPath)) {
            JsonObject reportJson = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("VEHICLE_INCIDENT", reportJson.get("reportType").getAsString());
            assertEquals(4, reportJson.getAsJsonObject("summary").get("totalPasses").getAsInt());
            assertEquals(2, reportJson.getAsJsonObject("summary").get("distinctNodes").getAsInt());
            assertEquals(4, reportJson.getAsJsonArray("passes").size());
        }

        // XLSX
        String xlsxPath = submitReport(cmdDesc, 404, ReportCmdType.VEHICLE_INCIDENT_XLSX, winStart, winEnd, envelopeJson);
        assertTrue(xlsxPath.endsWith(".xlsx"));
        try (InputStream in = readReportObject(xlsxPath);
             ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(in.readAllBytes()), StandardCharsets.UTF_8)) {
            Map<String, String> entries = new HashMap<>();
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                entries.put(ze.getName(), new String(zis.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertTrue(entries.get("xl/workbook.xml").contains("Vehicle Passes"));
            assertTrue(entries.get("xl/workbook.xml").contains("Adjudication &amp; OCR Chain"));
            assertTrue(entries.get("xl/workbook.xml").contains("Incident Summary"));
            assertTrue(entries.get("xl/worksheets/sheet1.xml").contains("federated-exit-node"));
        }
    }

    @Test
    public void denseMultiPassChartLabelsStayInsideCardAndResolveMemberSensorLanesAndNoisyOcr() throws Exception {
        var sml = new SMLHelper();
        RADHelper rad = new RADHelper();

        // Register parent LaneSystem (Lane 41) and child Rapiscan sensor (urn:osh:sensor:rapiscan:lane-41)
        String parentLaneUid = "urn:osh:system:lane:lane-41";
        BigId parentLaneSysId = db.getSystemDescStore().add(new SystemWrapper(
                sml.createPhysicalSystem().uniqueID(parentLaneUid).name("Lane 41").build())).getInternalID();

        String childRapiscanUid = "urn:osh:sensor:rapiscan:lane-41";
        BigId childRapiscanSysId = db.getSystemDescStore().add(parentLaneSysId, new SystemWrapper(
                sml.createPhysicalSystem().uniqueID(childRapiscanUid).name("Rapiscan RPM 41").build())).getInternalID();

        @SuppressWarnings({"rawtypes", "unchecked"})
        DataComponent grossOccStruct = new OccupancyOutput(new SensorSystem()).getRecordDescription();
        BigId rapiscanOccDs = db.getObservationStore().getDataStreams().add(new DataStreamInfo.Builder()
                .withSystem(new FeatureId(childRapiscanSysId, childRapiscanUid))
                .withName("occupancy")
                .withRecordDescription(grossOccStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .build()).getInternalID();

        DataComponent ocrStruct = rad.createVehicleOcrRecord();
        BigId laneOcrDs = db.getObservationStore().getDataStreams().add(new DataStreamInfo.Builder()
                .withSystem(new FeatureId(parentLaneSysId, parentLaneUid))
                .withName("vehicleOcr")
                .withRecordDescription(ocrStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .build()).getInternalID();

        Instant baseTime = Instant.parse("2026-11-14T18:00:00Z");
        for (int i = 0; i < 31; i++) {
            Instant pStart = baseTime.plusSeconds(i * 120L);
            Instant pEnd = pStart.plusSeconds(8L);
            Occupancy occ = new Occupancy.Builder()
                    .samplingTime(pEnd.getEpochSecond())
                    .occupancyCount(880 + i)
                    .startTime(pStart.getEpochSecond())
                    .endTime(pEnd.getEpochSecond())
                    .gammaAlarm(i != 1)
                    .neutronAlarm(i % 7 == 3)
                    .maxGammaCount(i == 1 ? 120 : 860 + (i % 6) * 10)
                    .maxNeutronCount(i % 7 == 3 ? 58 + i : 4)
                    .build();
            BigId occBigId = db.getObservationStore().add(new ObsData.Builder()
                    .withDataStream(rapiscanOccDs)
                    .withPhenomenonTime(pStart)
                    .withResultTime(pEnd)
                    .withResult(Occupancy.fromOccupancy(occ))
                    .build());
            String occObsId = hub.getIdEncoders().getObsIdEncoder().encodeID(occBigId);

            // Include noisy shipping-line logo text ("tex TGBU355853") on Pass #3
            String rawOcrText = (i == 2) ? "tex TGBU355853" : "TGBU 355853-7";
            VehicleOcrResult ocr = new VehicleOcrResult.Builder()
                    .sampleTime(pEnd)
                    .occupancyObsId(occObsId)
                    .idType(VehicleOcrResult.ID_TYPE_CONTAINER)
                    .value(rawOcrText)
                    .normalizedValue("TGBU3558537")
                    .checksumValid(true)
                    .confidence(0.95f)
                    .readCount(2)
                    .cameraUid("urn:osh:sensor:ffmpeg:lane:lane-41:0")
                    .frameTime(pStart.plusSeconds(2))
                    .build();
            db.getObservationStore().add(new ObsData.Builder()
                    .withDataStream(laneOcrDs)
                    .withPhenomenonTime(pEnd)
                    .withResultTime(pEnd)
                    .withResult(VehicleOcrResult.createDataBlock(ocrStruct, ocr))
                    .build());
        }

        // Scoped query by parent LaneSystem UID must resolve all 31 child-sensor occupancies
        String specJson = """
                {
                  "vehicleId": "TGBU3558537",
                  "matchMode": "equals",
                  "laneUID": "urn:osh:system:lane:lane-41"
                }
                """;
        DataComponent cmdDesc = reportControl.getCommandDescription().copy();
        String pdfPath = submitReport(
                cmdDesc, 501, ReportCmdType.VEHICLE_INCIDENT_PDF, baseTime.minusSeconds(60), baseTime.plusSeconds(7200), specJson);

        record PositionedSpan(int page, String text, float minX, float minY, float maxY) {}
        List<PositionedSpan> spans = new ArrayList<>();

        try (InputStream in = readReportObject(pdfPath);
             PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            String fullText = new PDFTextStripper().getText(doc);
            assertTrue(fullText.contains("Lane 41"));
            assertTrue(fullText.contains("TGBU 355853-7"));
            assertTrue(fullText.contains("Camera 0 (lane-41)"));
            assertFalse("Raw child Rapiscan URN should resolve to Lane 41", fullText.contains("urn:osh:sensor:rapiscan:lane-41"));
            assertFalse("Raw FFmpeg camera URN should be formatted cleanly", fullText.contains("urn:osh:sensor:ffmpeg:lane:lane-41:0"));
            assertFalse("Extraneous shipping-line OCR prefix 'tex ' should be stripped", fullText.contains("tex TGBU"));

            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                final int pageNum = p;
                PDFTextStripper posStripper = new PDFTextStripper() {
                    @Override
                    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                        if (text != null && !text.isBlank() && textPositions != null && !textPositions.isEmpty()) {
                            float minX = Float.MAX_VALUE;
                            float minY = Float.MAX_VALUE;
                            float maxY = -Float.MAX_VALUE;
                            for (TextPosition tp : textPositions) {
                                float x = tp.getTextMatrix().getTranslateX();
                                float y = tp.getTextMatrix().getTranslateY();
                                minX = Math.min(minX, x);
                                minY = Math.min(minY, y);
                                maxY = Math.max(maxY, y);
                            }
                            spans.add(new PositionedSpan(pageNum, text.trim(), minX, minY, maxY));
                        }
                        super.writeString(text, textPositions);
                    }
                };
                posStripper.setStartPage(p);
                posStripper.setEndPage(p);
                posStripper.getText(doc);
            }
        }

        PositionedSpan xAxisTitle = spans.stream()
                .filter(s -> s.text().contains("Chronological Vehicle Pass"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing 'Chronological Vehicle Pass' axis title"));

        List<PositionedSpan> rotatedPassTicks = spans.stream()
                .filter(s -> s.page() == xAxisTitle.page() && s.text().startsWith("Pass #") && s.text().contains("("))
                .toList();
        assertFalse("Expected rotated Pass # category ticks on chart page", rotatedPassTicks.isEmpty());

        for (PositionedSpan tick : rotatedPassTicks) {
            assertTrue("Rotated tick '" + tick.text() + "' crossed left card margin: minX=" + tick.minX(),
                    tick.minX() >= 38f);
            assertTrue("Rotated tick '" + tick.text() + "' overlapped X-axis title: tick.minY="
                            + tick.minY() + " vs xAxisTitle.maxY=" + xAxisTitle.maxY(),
                    tick.minY() >= xAxisTitle.maxY() + 6f);
            assertTrue("Rotated tick '" + tick.text() + "' sloped downward instead of upward-right: minY="
                            + tick.minY() + ", maxY=" + tick.maxY(),
                    tick.maxY() > tick.minY() + 8f);
        }

        spans.stream()
                .filter(s -> s.page() == xAxisTitle.page()
                        && s.text().contains("Adjudication, Camera OCR & Spectroscopic Chain"))
                .findFirst()
                .ifPresent(nextHeader -> assertTrue(
                        "Section 3 header overlapped chart card: nextHeader.maxY="
                                + nextHeader.maxY() + " vs xAxisTitle.minY=" + xAxisTitle.minY(),
                        nextHeader.maxY() <= xAxisTitle.minY() - 14f));
    }

    private void recordCompletedAdjudication(
            BigId cmdStreamId,
            AdjudicationControl adjControl,
            Method createResultMethod,
            Adjudication adj,
            String sender,
            Instant reportTime
    ) throws Exception {
        BigId cmdKey = db.getCommandStore().add(new CommandData.Builder()
                .withCommandStream(cmdStreamId)
                .withIssueTime(reportTime.minusSeconds(2))
                .withSender(sender)
                .withParams(Adjudication.fromAdjudication(adj))
                .build());
        DataBlock resBlock = (DataBlock) createResultMethod.invoke(adjControl, adj, sender);
        db.getCommandStatusStore().add(new CommandStatus.Builder()
                .withCommand(cmdKey)
                .withReportTime(reportTime)
                .withStatusCode(ICommandStatus.CommandStatusCode.COMPLETED)
                .withResult(CommandResult.withData(resBlock))
                .build());
    }

    private String submitReport(
            DataComponent cmdDesc,
            int cmdId,
            ReportCmdType type,
            Instant start,
            Instant end,
            String spec
    ) throws Exception {
        DataBlock block = cmdDesc.createDataBlock();
        block.setStringValue(0, type.name());
        block.setTimeStamp(1, start);
        block.setTimeStamp(2, end);
        block.setStringValue(3, spec);
        block.setStringValue(4, EventReportType.NONE.name());
        ICommandStatus res = reportControl.submitCommand(new CommandData(cmdId, block)).get();
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, res.getStatusCode());
        return res.getResult().getInlineRecords().stream().findFirst().orElseThrow().getStringValue();
    }

    private InputStream readReportObject(String resPath) throws Exception {
        String key = resPath.startsWith("reports/") ? resPath.substring("reports/".length()) : resPath;
        return oscarModule.getBucketService().getBucketStore().getObject("reports", key);
    }

    private static String basicAuth() {
        String raw = USER + ":" + PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
