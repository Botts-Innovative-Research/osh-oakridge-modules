package com.botts.impl.service.oscar.siteinfo;

import org.sensorhub.api.config.DisplayInfo;

public class SiteDiagramConfig {

    @DisplayInfo(label = "Path to Site Diagram (.png/.jpg)", desc = "Site diagram to use for systems connected to this OSCAR node")
    public String siteDiagramPath;

    public static class LatLonLocation
    {
        @DisplayInfo(label="Latitude", desc="Geodetic latitude, in degrees")
        public double lat;

        @DisplayInfo(label="Longitude", desc="Longitude, in degrees")
        public double lon;
    }

    @DisplayInfo(desc = "Lower left point of site map (latitude, longitude)")
    public LatLonLocation siteLowerLeftBound;

    @DisplayInfo(desc = "Upper right point of site map (latitude, longitude)")
    public LatLonLocation siteUpperRightBound;

}
