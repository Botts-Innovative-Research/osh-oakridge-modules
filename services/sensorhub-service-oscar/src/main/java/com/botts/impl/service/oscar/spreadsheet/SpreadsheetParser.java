package com.botts.impl.service.oscar.spreadsheet;

import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.*;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.module.ModuleRegistry;

import java.io.*;
import java.nio.file.Path;
import java.sql.Array;
import java.util.*;

public class SpreadsheetParser {

    SchemaV1 schema;
    String[] headers;
    String DELIMITER = ",";

    public SpreadsheetParser() {
        schema = new SchemaV1();
    }

    public void validateHeader(String headerLine) throws IllegalArgumentException {
        String[] values = headerLine.split(DELIMITER, -1);

        int baseLength = schema.getHeaders().size();
        int extraLength = values.length - baseLength;

        // Ensure extra headers in groups of 6
        if (extraLength < 0 || extraLength % SchemaV1.CAMERA_HEADERS.length != 0) {
            throw new IllegalArgumentException(
                    "Extra cameras must be configured as " +
                            "[CameraTypeX, CameraHostX, CameraPathX, CodecX, UsernameX, PasswordX] " +
                            "with X sequential starting at 0"
            );
        }

        // Validate fixed headers
        for (int i = 0; i < SchemaV1.MAIN_HEADERS.length; i++) {
            String current = values[i];
            String expected = SchemaV1.MAIN_HEADERS[i];
            if (!Objects.equals(current, expected)) {
                throw new IllegalArgumentException("Expected: " + expected + " Got: " + current);
            }
        }

        // Validate camera headers
        int numExtraGroups = extraLength / SchemaV1.CAMERA_HEADERS.length;
        for (int camIndex = 0; camIndex < numExtraGroups; camIndex++) {
            for (int j = 0; j < SchemaV1.CAMERA_HEADERS.length; j++) {
                String expected = SchemaV1.CAMERA_HEADERS[j].replace("0", String.valueOf(camIndex));
                String actual = values[SchemaV1.MAIN_HEADERS.length + camIndex * SchemaV1.CAMERA_HEADERS.length + j];
                if (!Objects.equals(expected, actual)) {
                    throw new IllegalArgumentException(
                            "Expected camera header: " + expected + " Got: " + actual
                    );
                }
            }
        }

        headers = values;
    }

    public Map<String, String> toMap(String line) {
        String[] values = line.split(DELIMITER, -1);
        Map<String, String> result = new LinkedHashMap<>();

        if (values.length != headers.length)
            throw new IllegalArgumentException("Values must match headers");

        for (int i = 0; i < values.length; i++)
            result.put(headers[i], values[i].trim());

        return result;
    }

    public List<LaneConfig> deserialize(String csv) throws IOException {
        List<LaneConfig> lanes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csv))) {
            String headerLine = reader.readLine();
            validateHeader(headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                Map<String, String> row = toMap(line);
                LaneConfig laneConfig = deserializeLane(row);
                lanes.add(laneConfig);
            }

        }
        return lanes;
    }

    public LaneConfig deserializeLane(Map<String, String> row) {

        LaneConfig laneConfig = new LaneConfig();
        laneConfig.moduleClass = LaneSystem.class.getCanonicalName();
        laneConfig.id = UUID.randomUUID().toString();
        laneConfig.name = row.get(SchemaV1.NAME);
        laneConfig.uniqueID = row.get(SchemaV1.UID);
        laneConfig.autoStart = Boolean.parseBoolean(row.get(SchemaV1.AUTO_START));

        if (!row.get(SchemaV1.LATITUDE).isBlank() && !row.get(SchemaV1.LONGITUDE).isBlank()) {
            PositionConfig.LLALocation location = new PositionConfig.LLALocation();
            location.lat = Double.parseDouble(row.get(SchemaV1.LATITUDE));
            location.lon = Double.parseDouble(row.get(SchemaV1.LONGITUDE));
        }

        laneConfig.laneOptionsConfig = new LaneOptionsConfig();

        var rpmType = row.get(SchemaV1.RPM_TYPE);
        RPMConfig rpmConfig = null;
        if (!rpmType.isBlank())
            rpmConfig = deserializeRPM(row);

        int camIndex = 0;
        ArrayList<FFMpegConfig> ffMpegConfigs = new ArrayList<>();
        while (row.containsKey("CameraType" + camIndex)) {
            String cameraType = row.get("CameraType" + camIndex);
            if (cameraType.isBlank()) {
                camIndex++;
                continue;
            }

            FFMpegConfig ffMpegConfig = deserializeCamera(row, camIndex++);
            if (ffMpegConfig != null)
                ffMpegConfigs.add(ffMpegConfig);
        }

        if (rpmConfig != null)
            laneConfig.laneOptionsConfig.rpmConfig = rpmConfig;
        if (!ffMpegConfigs.isEmpty())
            laneConfig.laneOptionsConfig.ffmpegConfig = ffMpegConfigs;
        return laneConfig;

    }

    public RPMConfig deserializeRPM(Map<String, String> row) {
        RPMConfig rpmConfig = null;

        var rpmType = row.get(SchemaV1.RPM_TYPE);
        if (rpmType.equalsIgnoreCase("Aspect")) {
            rpmConfig = new AspectRPMConfig();
            rpmConfig.remoteHost = row.get(SchemaV1.RPM_HOST);
            rpmConfig.remotePort = Integer.parseInt(row.get(SchemaV1.RPM_PORT));
            ((AspectRPMConfig) rpmConfig).addressRange.from = Integer.parseInt(row.get(SchemaV1.ASPECT_ADDRESS_START));
            ((AspectRPMConfig) rpmConfig).addressRange.to = Integer.parseInt(row.get(SchemaV1.ASPECT_ADDRESS_END));
        } else if (rpmType.equalsIgnoreCase("Rapiscan")) {
            rpmConfig  = new RapiscanRPMConfig();
            rpmConfig.remoteHost = row.get(SchemaV1.RPM_HOST);
            rpmConfig.remotePort = Integer.parseInt(row.get(SchemaV1.RPM_PORT));
            var eml = ((RapiscanRPMConfig) rpmConfig).emlConfig = new RapiscanRPMConfig.EMLConfig();
            eml.emlEnabled = Boolean.parseBoolean(row.get(SchemaV1.EML_ENABLED));
            eml.isCollimated = row.get(SchemaV1.EML_COLLIMATED) != null && Boolean.parseBoolean(row.get(SchemaV1.EML_COLLIMATED));
            eml.laneWidth = row.get(SchemaV1.LANE_WIDTH) == null ? 0.0f : Double.parseDouble(row.get(SchemaV1.LANE_WIDTH));
        }

        return rpmConfig;
    }

    public FFMpegConfig deserializeCamera(Map<String, String> row, int camIndex) {
        FFMpegConfig ffMpegConfig = null;

        String cameraType = row.get(SchemaV1.CAMERA_TYPE + camIndex);
        if (cameraType == null)
            return null;

        if (cameraType.equalsIgnoreCase("Custom")) {
            ffMpegConfig = new CustomCameraConfig();
            ffMpegConfig.password = row.get(SchemaV1.CAMERA_PASSWORD + camIndex) == null ? "" : row.get(SchemaV1.CAMERA_PASSWORD + camIndex);
            ffMpegConfig.username = row.get(SchemaV1.CAMERA_USERNAME + camIndex) == null ? "" :row.get(SchemaV1.CAMERA_USERNAME + camIndex);
            ((CustomCameraConfig)ffMpegConfig).streamPath = row.get(SchemaV1.CAMERA_PATH + camIndex);
        } else if (cameraType.equalsIgnoreCase("Axis")) {
            ffMpegConfig = new AxisCameraConfig();
            ffMpegConfig.username = row.get(SchemaV1.CAMERA_USERNAME + camIndex) == null ? "" :row.get(SchemaV1.CAMERA_USERNAME + camIndex);
            ffMpegConfig.password = row.get(SchemaV1.CAMERA_PASSWORD + camIndex) == null ? "" : row.get(SchemaV1.CAMERA_PASSWORD + camIndex);
            var codec = row.get(SchemaV1.CODEC + camIndex);
            if (codec == null || (codec.equalsIgnoreCase("h264") || codec.equalsIgnoreCase("h.264")))
                ((AxisCameraConfig) ffMpegConfig).streamPath = AxisCameraConfig.CodecEndpoint.H264;
            else if (codec.equalsIgnoreCase("mjpeg") || codec.equalsIgnoreCase("jpeg"))
                ((AxisCameraConfig) ffMpegConfig).streamPath = AxisCameraConfig.CodecEndpoint.MJPEG;
        } else if (cameraType.equalsIgnoreCase("Sony")) {
            ffMpegConfig = new SonyCameraConfig();
            ffMpegConfig.username = row.get(SchemaV1.CAMERA_USERNAME + camIndex);
            ffMpegConfig.password = row.get(SchemaV1.CAMERA_PASSWORD + camIndex);
        }

        if (ffMpegConfig == null)
            return null;

        String host = row.get(SchemaV1.CAMERA_HOST + camIndex);
        if (host != null)
            ffMpegConfig.remoteHost = host;
        else
            throw new IllegalArgumentException("Must specify camera host address");

        return ffMpegConfig;
    }


    public String serialize(Collection<LaneConfig> lanes) {
        int maxCameras = lanes.stream()
                .mapToInt(lane -> lane.laneOptionsConfig.ffmpegConfig == null ? 0 : lane.laneOptionsConfig.ffmpegConfig.size())
                .max()
                .orElse(0);

        StringBuilder builder = new StringBuilder()
                .append(String.join(DELIMITER, schema.getHeaders(maxCameras)))
                .append("\n");

        lanes.forEach(lane -> {
                builder.append(serializeLane(lane, maxCameras));
                builder.append("\n");
        });

        return builder.toString();
    }

    public String serializeLane(LaneConfig lane, int maxCameras) {
        List<String> r = new LinkedList<>();
        // Lane stuff
        addToRow(r, lane.name);
        addToRow(r, lane.uniqueID);
        addToRow(r, lane.autoStart);

        var loc = lane.location;
        if (loc != null) {
            addToRow(r, loc.lat);
            addToRow(r, loc.lon);
        } else
            addNullsToRow(r, 2);

        var opts = lane.laneOptionsConfig;
        var rpm = opts.rpmConfig;
        if (rpm != null) {
            if (rpm instanceof AspectRPMConfig) {
                var aspect = (AspectRPMConfig) rpm;
                addToRow(r, "Aspect");
                addToRow(r, aspect.remoteHost);
                addToRow(r, aspect.remotePort);
                addToRow(r, aspect.addressRange.from);
                addToRow(r, aspect.addressRange.to);
                // Add empty vals for EML
                addNullsToRow(r, 3);
            } else if (rpm instanceof RapiscanRPMConfig) {
                var rapiscan = (RapiscanRPMConfig) rpm;
                addToRow(r, "Rapiscan");
                addToRow(r, rapiscan.remoteHost);
                addToRow(r, rapiscan.remotePort);
                // Aspect start & end
                addNullsToRow(r, 2);
                var eml = rapiscan.emlConfig;
                if (eml != null) {
                    addToRow(r, eml.emlEnabled);
                    addToRow(r, eml.isCollimated);
                    addToRow(r, eml.laneWidth);
                } else
                    addNullsToRow(r, 3);
            }
        } else {
            addNullsToRow(r, 8);
        }

        // Add header and vals for default camera
        // TODO: DO THIS
        for (int i = 0; i < maxCameras; i++) {
            if (opts.ffmpegConfig.size() > i) {
                var cam = opts.ffmpegConfig.get(i);

                if (cam instanceof CustomCameraConfig custom) {
                    addToRow(r, "Custom");
                    addToRow(r, custom.remoteHost);
                    addToRow(r, custom.streamPath);
                    addToRow(r, null);
                    addToRow(r, custom.username);
                    addToRow(r, custom.password);
                } else if (cam instanceof AxisCameraConfig axis) {
                    addToRow(r, "Axis");
                    addToRow(r, axis.remoteHost);
                    addToRow(r, null);
                    if (axis.streamPath == AxisCameraConfig.CodecEndpoint.H264) {
                        addToRow(r, "H264");
                    } else if (axis.streamPath == AxisCameraConfig.CodecEndpoint.MJPEG) {
                        addToRow(r, "MJPEG");
                    }
                    addToRow(r, axis.username);
                    addToRow(r, axis.password);
                } else if (cam instanceof SonyCameraConfig sony) {
                    addToRow(r, "Sony");
                    addToRow(r, sony.remoteHost);
                    addNullsToRow(r, 2);
                    addToRow(r, sony.username);
                    addToRow(r, sony.password);
                }
            } else {
                addNullsToRow(r, SchemaV1.CAMERA_HEADERS.length);
            }
        }

        return String.join(DELIMITER, r);
    }

    private void addToRow(List<String> row, Object item) {
        if (item != null)
            row.add(String.valueOf(item));
        else
            row.add("");
    }

    private void addNullsToRow(List<String> row, int numVals) {
        for (int i = 0; i < numVals; i++)
            addToRow(row, null);
    }
}
