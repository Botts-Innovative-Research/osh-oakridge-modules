package com.botts.impl.service.oscar;

import com.botts.api.service.bucket.IBucketService;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.botts.impl.service.oscar.siteinfo.SiteInfoOutput;
import com.botts.impl.service.oscar.siteinfo.SitemapDiagramHandler;
import net.opengis.swe.v20.DataComponent;
import org.junit.Test;

import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static com.botts.impl.service.oscar.Constants.SITE_MAP_BUCKET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void uploadedDiagramActivatesAgainstEditedConfiguration() throws Exception {
        Path root = Files.createTempDirectory("site-diagram-upload");
        try {
            var store = new FileSystemBucketStore(root);
            IBucketService bucketService = (IBucketService) Proxy.newProxyInstance(
                    IBucketService.class.getClassLoader(),
                    new Class<?>[]{IBucketService.class},
                    (proxy, method, args) -> {
                        if ("getBucketStore".equals(method.getName()))
                            return store;
                        return null;
                    });

            var output = new SiteInfoOutput(new OSCARSystem("site-diagram-upload-test"));
            var handler = new SitemapDiagramHandler(bucketService, output, new OSCARServiceModule());
            var config = siteDiagramConfig();
            String browserFilename = "C:\\fakepath\\Screenshot 2026-09-19 083137.PNG";
            String objectKey = "Screenshot 2026-09-19 083137.PNG";

            assertTrue(handler.isValidFileType(browserFilename, "image/png"));
            try (OutputStream upload = handler.handleUpload(browserFilename)) {
                upload.write("image data".getBytes(StandardCharsets.UTF_8));
            }

            assertTrue(handler.handleFile(browserFilename, config));
            assertTrue(store.objectExists(SITE_MAP_BUCKET, objectKey));
            assertEquals(objectKey, config.siteDiagramPath);
            assertEquals(Path.of(SITE_MAP_BUCKET, objectKey).toString(),
                    output.getLatestRecord().getStringValue(1));
        } finally {
            if (Files.exists(root)) {
                try (var paths = Files.walk(root)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
                }
            }
        }
    }

    private SiteDiagramConfig siteDiagramConfig() {
        var config = new SiteDiagramConfig();
        config.siteLowerLeftBound = new SiteDiagramConfig.LatLonLocation();
        config.siteLowerLeftBound.lat = 35.9;
        config.siteLowerLeftBound.lon = -84.4;
        config.siteUpperRightBound = new SiteDiagramConfig.LatLonLocation();
        config.siteUpperRightBound.lat = 36.1;
        config.siteUpperRightBound.lon = -84.1;
        return config;
    }
}
