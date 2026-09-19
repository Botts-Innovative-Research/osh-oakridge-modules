package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.botts.impl.service.oscar.siteinfo.SiteInfoOutput;
import net.opengis.swe.v20.DataComponent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SiteDiagramTests {

    @Test
    public void siteDiagramCoordinatesMatchThePublishedVectorSchema() {
        var output = new SiteInfoOutput(new OSCARSystem("site-diagram-test"));
        var lowerLeft = new SiteDiagramConfig.LatLonLocation();
        lowerLeft.lat = 35.9;
        lowerLeft.lon = -84.4;
        var upperRight = new SiteDiagramConfig.LatLonLocation();
        upperRight.lat = 36.1;
        upperRight.lon = -84.1;

        output.setData("sitemap/site-plan.png", lowerLeft, upperRight);

        var record = output.getLatestRecord();
        assertEquals("sitemap/site-plan.png", record.getStringValue(1));
        assertEquals(lowerLeft.lon, record.getDoubleValue(2), 0.0);
        assertEquals(lowerLeft.lat, record.getDoubleValue(3), 0.0);
        assertEquals(upperRight.lon, record.getDoubleValue(4), 0.0);
        assertEquals(upperRight.lat, record.getDoubleValue(5), 0.0);

        DataComponent boundingBox = output.getRecordDescription().getComponent(2);
        DataComponent lowerLeftVector = boundingBox.getComponent(0);
        DataComponent upperRightVector = boundingBox.getComponent(1);
        assertEquals("lon", lowerLeftVector.getComponent(0).getName());
        assertEquals("lat", lowerLeftVector.getComponent(1).getName());
        assertEquals("lon", upperRightVector.getComponent(0).getName());
        assertEquals("lat", upperRightVector.getComponent(1).getName());
    }
}
