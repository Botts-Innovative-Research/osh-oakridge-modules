package com.botts.impl.service.oscar.spreadsheet;

import java.util.ArrayList;
import java.util.List;

public class SchemaV1 {

    // Lane stuff
    public static final String NAME = "Name";
    public static final String UID = "UniqueID";
    public static final String AUTO_START = "AutoStart";
    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";

    // RPM stuff
    public static final String RPM_TYPE = "RPMConfigType";
    public static final String RPM_HOST = "RPMHost";
    public static final String RPM_PORT = "RPMPort";
    public static final String ASPECT_ADDRESS_START = "AspectAddressStart";
    public static final String ASPECT_ADDRESS_END = "AspectAddressEnd";
    public static final String EML_ENABLED = "EMLEnabled";
    public static final String EML_COLLIMATED = "EMLCollimated";
    public static final String LANE_WIDTH = "LaneWidth";

    // Camera stuff
    public static final String CAMERA_TYPE = "CameraType0";
    public static final String CAMERA_HOST = "CameraHost0";
    public static final String CAMERA_PATH = "CameraPath0";
    public static final String CODEC = "Codec0";
    public static final String CAMERA_USERNAME = "Username0";
    public static final String CAMERA_PASSWORD = "Password0";

    public static final String[] MAIN_HEADERS = {
            "Name",
            "UniqueID",
            "AutoStart",
            "Latitude",
            "Longitude",
            "RPMConfigType",
            "RPMHost",
            "RPMPort",
            "AspectAddressStart",
            "AspectAddressEnd",
            "EMLEnabled",
            "EMLCollimated",
            "LaneWidth"
    };

    public static final String[] CAMERA_HEADERS = {
            "CameraType0",
            "CameraHost0",
            "CameraPath0",
            "Codec0",
            "Username0",
            "Password0"
    };

    private List<String> headers;

    public SchemaV1() {
        headers = new ArrayList<>();
        for (var mainHeader : MAIN_HEADERS)
            headers.add(mainHeader);
        for (var cameraHeader : CAMERA_HEADERS)
            headers.add(cameraHeader);
    }

    public List<String> getHeaders() {
        return headers;
    }


}
