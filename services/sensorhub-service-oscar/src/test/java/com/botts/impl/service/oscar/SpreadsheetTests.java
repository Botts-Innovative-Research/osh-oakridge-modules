package com.botts.impl.service.oscar;

import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import com.botts.impl.service.oscar.spreadsheet.SpreadsheetHandler;
import com.botts.impl.service.oscar.spreadsheet.SpreadsheetParser;
import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.AspectRPMConfig;
import com.botts.impl.system.lane.config.LaneConfig;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class SpreadsheetTests {

    private static final Logger logger = LoggerFactory.getLogger(SpreadsheetTests.class);

    SpreadsheetParser parser;
    SpreadsheetHandler handler;
    SensorHub hub;
    ModuleRegistry reg;

    @Before
    public void setup() throws SensorHubException, IOException {
        parser = new SpreadsheetParser();
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();
        FileSystemBucketStore store = new FileSystemBucketStore(Path.of("test"));
        handler = new SpreadsheetHandler(reg, store, logger);
    }

    // Parser tests

    private String readFile(String filename) throws IOException {
        var s = SpreadsheetTests.class.getResourceAsStream("/spreadsheets/" + filename);
        return new String(s.readAllBytes());
    }

    @Test
    public void testParseWithOnlyHeader() throws IOException, SensorHubException {
        String contents = readFile("only-header.csv");
        var lanes = parser.deserialize(contents);
        assertEquals(lanes.size(), 0);
        handler.loadModules(lanes);
        assertNotNull(reg.getLoadedModules(LaneConfig.class));
    }

    @Test
    public void testParseWithAspect() throws IOException, SensorHubException {
        String contents = readFile("1-camera-aspect.csv");
        var lanes = parser.deserialize(contents);
        var lane = lanes.get(0);
        assertNotNull(lane.laneOptionsConfig.rpmConfig);
        assertTrue(lane.laneOptionsConfig.rpmConfig instanceof AspectRPMConfig);
        assertNotNull(lane.laneOptionsConfig.ffmpegConfig);
        assertTrue(lane.laneOptionsConfig.ffmpegConfig.size() == 1);
        handler.loadModules(lanes);
    }


    @Test
    public void testParseWith1Camera() throws IOException, SensorHubException {
        String contents = readFile("1-camera.csv");
        var lanes = parser.deserialize(contents);
        handler.loadModules(lanes);
    }

    @Test
    public void testParseWith2Camera() throws IOException, SensorHubException {
        String contents = readFile("2-camera.csv");
        var lanes = parser.deserialize(contents);
        handler.loadModules(lanes);
    }

    @Test
    public void testParseWith3Camera() throws IOException, SensorHubException {
        String contents = readFile("3-camera.csv");
        var lanes = parser.deserialize(contents);
        handler.loadModules(lanes);
    }

    @Test
    public void testParseMultipleLanes() throws IOException, SensorHubException {
        String contents = readFile("multiple-lanes.csv");
        var lanes = parser.deserialize(contents);
        handler.loadModules(lanes);
    }

    @Test
    public void testSerializeMultipleLanes() throws IOException, SensorHubException {
        testParseMultipleLanes();
        var lanes = reg.getLoadedModules(LaneSystem.class);
        var laneConfigs = lanes.stream().map(lane -> lane.getConfiguration()).toList();
        String serialized = parser.serialize(laneConfigs);
        assertNotNull(serialized);
        String[] resSplit = serialized.split("\n");
        String header = resSplit[0];
        String firstLine = resSplit[1];
        String[] headerSplit = header.split(",", -1);
        String[] firstLineSplit = firstLine.split(",", -1);
        assertEquals(headerSplit.length, firstLineSplit.length);
        System.out.println(serialized);
    }

    @Test
    public void testSerializeDeserialize() throws IOException, SensorHubException {
        for (int i = 0; i < 100; i++) {
            testParseMultipleLanes();
            testSerializeMultipleLanes();
        }
    }

    // Handler tests

    @Test
    public void testUploadAndLoad() throws DataStoreException, IOException {
        String file = "upload-this.csv";
        String data = readFile(file);

        // Simulate upload process
        var out = handler.handleUpload(file);
        out.write(data.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();

        // Should be done "uploading"
        boolean successful = handler.handleFile(file);
        assertTrue(successful);

        // See if "successful" is trustworthy
        var lanes = reg.getLoadedModules(LaneSystem.class);
        var laneConfigs = lanes.stream().map(lane -> lane.getConfiguration()).toList();
        assertFalse(laneConfigs.isEmpty());
    }

    @Test
    public void testUploadAndDownload() throws DataStoreException, IOException {
        testUploadAndLoad();
        var download = handler.getDownloadStream();
        assertNotNull(download);
        var data = new String(download.readAllBytes());
        assertFalse(data.isBlank());
        System.out.println(data);
    }

    @Test
    public void testUploadAndDownloadRepeated() throws DataStoreException, IOException {
        for (int i = 0; i < 100; i++)
            testUploadAndDownload();
    }

}
