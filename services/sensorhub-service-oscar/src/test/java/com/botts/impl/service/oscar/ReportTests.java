package com.botts.impl.service.oscar;

import com.botts.impl.service.bucket.BucketService;
import com.botts.impl.service.bucket.BucketServiceConfig;
import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.reports.helpers.*;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.database.system.SystemDriverDatabase;
import org.sensorhub.impl.database.system.SystemDriverDatabaseConfig;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabaseConfig;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.service.consys.ConSysApiService;
import org.sensorhub.impl.service.consys.ConSysApiServiceConfig;
import org.sensorhub.impl.utils.rad.RADHelper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import static org.junit.Assert.*;

public class ReportTests {
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
        httpServer = (IHttpServer) reg.loadModule(httpConfig);
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
        bucketServiceConfig.fileStoreRootDir = "oscar-test";

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

        assertFalse(resPath.isEmpty());
        assertTrue(oscarServiceModule.getBucketService().getBucketStore().objectExists(resPath));
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
        Predicate<IObsData> gammaPredicate = (obsData) -> obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);
        return Utils.countObservations(oscarServiceModule, gammaPredicate, begin, end, RADHelper.DEF_OCCUPANCY);
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
                "    \"params\": {\n" +
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


    @After
    public void cleanup() {
        if (hub != null)
            hub.stop();
        httpClient = null;
        httpServer = null;

    }
}