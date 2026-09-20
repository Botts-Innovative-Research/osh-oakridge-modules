
/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.siteinfo;


import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.oscar.IFileHandler;
import com.botts.impl.service.oscar.OSCARServiceModule;
import org.sensorhub.api.datastore.DataStoreException;
import org.vast.util.Asserts;

import java.io.*;
import java.util.Collections;

import static com.botts.impl.service.oscar.Constants.SITE_MAP_BUCKET;

public class SitemapDiagramHandler implements IFileHandler {

    IBucketService bucketService;
    IBucketStore bucketStore;
    SiteInfoOutput siteInfoOutput;
    OSCARServiceModule module;


    public SitemapDiagramHandler(IBucketService bucketService, SiteInfoOutput siteInfoOutput, OSCARServiceModule module) {
        this.bucketService = bucketService;
        this.siteInfoOutput = siteInfoOutput;
        this.module = module;
        this.bucketStore = Asserts.checkNotNull(bucketService.getBucketStore());

        checkBucketExists();
    }

    private void checkBucketExists() {
        if (!bucketStore.bucketExists(SITE_MAP_BUCKET)) {
            try {
                bucketStore.createBucket(SITE_MAP_BUCKET);
            } catch (DataStoreException e) {
                module.getLogger().error("Unable to create bucket for sitemap config", e);
            }
        }
    }

    @Override
    public boolean handleFile(String filename) {
        return handleFile(filename, module.getConfiguration().siteDiagramConfig);
    }

    /**
     * Activates an uploaded site diagram using the configuration currently
     * being edited in the admin UI. This is intentionally separate from the
     * module's running configuration, which is not replaced until the user
     * clicks Apply Changes.
     */
    public boolean handleFile(String filename, SiteDiagramConfig siteDiagramConfig) {
        String objectKey = normalizeFileName(filename);

        if (!hasValidBounds(siteDiagramConfig)) {
            module.getLogger().warn("Site diagram bounds are missing or invalid");
            return false;
        }

        if (objectKey.isBlank() || !bucketStore.objectExists(SITE_MAP_BUCKET, objectKey)) {
            module.getLogger().error("Sitemap file {} not found in bucket {}", objectKey, SITE_MAP_BUCKET);
            return false;
        }

        try {
            siteDiagramConfig.siteDiagramPath = objectKey;
            siteInfoOutput.setData(bucketStore.getRelativeResourceURI(SITE_MAP_BUCKET, objectKey),
                    siteDiagramConfig.siteLowerLeftBound, siteDiagramConfig.siteUpperRightBound);
        } catch (DataStoreException e) {
            module.getLogger().error("Unable to read bucket for sitemap config", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean isValidFileType(String fileName, String mimeType) {
        String normalizedName = normalizeFileName(fileName).toLowerCase();
        return normalizedName.endsWith(".png") || normalizedName.endsWith(".jpg") ||
                normalizedName.endsWith(".jpeg");
    }

    @Override
    public OutputStream handleUpload(String filename) throws DataStoreException {
        return bucketStore.putObject(SITE_MAP_BUCKET, normalizeFileName(filename), Collections.emptyMap());
    }

    public static String normalizeFileName(String filename) {
        if (filename == null)
            return "";

        String normalized = filename.replace('\\', '/');
        int lastSeparator = normalized.lastIndexOf('/');
        return (lastSeparator >= 0 ? normalized.substring(lastSeparator + 1) : normalized).trim();
    }

    public static boolean hasValidBounds(SiteDiagramConfig config) {
        if (config == null || config.siteLowerLeftBound == null || config.siteUpperRightBound == null)
            return false;

        double lowerLat = config.siteLowerLeftBound.lat;
        double lowerLon = config.siteLowerLeftBound.lon;
        double upperLat = config.siteUpperRightBound.lat;
        double upperLon = config.siteUpperRightBound.lon;
        return Double.isFinite(lowerLat) && Double.isFinite(lowerLon) &&
                Double.isFinite(upperLat) && Double.isFinite(upperLon) &&
                Math.abs(lowerLat) <= 90 && Math.abs(upperLat) <= 90 &&
                Math.abs(lowerLon) <= 180 && Math.abs(upperLon) <= 180 &&
                lowerLat < upperLat && lowerLon < upperLon;
    }
}
