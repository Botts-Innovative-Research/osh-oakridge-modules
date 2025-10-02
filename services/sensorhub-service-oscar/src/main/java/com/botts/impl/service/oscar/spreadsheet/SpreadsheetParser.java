package com.botts.impl.service.oscar.spreadsheet;

import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.config.LaneConfig;
import org.sensorhub.impl.module.ModuleRegistry;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class SpreadsheetParser {

    SchemaV1 schema;
    String[] headers;
    String DELIMITER = ",";

    public SpreadsheetParser() {
        schema = new SchemaV1();
    }

    public void validateHeader(String headerLine) throws IllegalArgumentException {
        String[] values = headerLine.split(DELIMITER);

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

    private boolean isValidCameraHeader(String validHeader, String header) {
        // TODO: This will only support 0-9 rn. Should not have more than 3 cameras per lane anyways. 10 would be crazy
        int validLastIndex = validHeader.length() - 1;
        String validBase = validHeader.substring(0, validLastIndex);
        int headerLastIndex = header.length() - 1;
        String headerBase = header.substring(0, headerLastIndex);
        return Objects.equals(validBase, headerBase);
    }

    public Map<String, String> toMap(String line) {
        String[] values = line.split(DELIMITER);
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

                LaneConfig laneConfig = new LaneConfig();
                laneConfig.moduleClass = LaneSystem.class.getCanonicalName();
                laneConfig.id = UUID.randomUUID().toString();
                laneConfig.
            }

        }
    }

    public String serialize(Collection<LaneConfig> laneSystems) {

        // objects to csv
        return "";
    }

    public void loadFile(Path filePath){

    }

    public void load(String data) {

    }

}
