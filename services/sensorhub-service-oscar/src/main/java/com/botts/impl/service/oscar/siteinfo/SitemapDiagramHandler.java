
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
        SiteDiagramConfig siteDiagramConfig = module.getConfiguration().siteDiagramConfig;

        if (siteDiagramConfig == null) {
            module.getLogger().warn("No site diagram config found in OSCAR Module Service config");
        }

        if(!bucketStore.objectExists(SITE_MAP_BUCKET, filename)){
            module.getLogger().error("Sitemap file {} not found in bucket {}", filename, SITE_MAP_BUCKET);
            return false;
        }

        try {
            siteInfoOutput.setData(bucketStore.getRelativeResourceURI(SITE_MAP_BUCKET, filename), siteDiagramConfig.siteLowerLeftBound, siteDiagramConfig.siteUpperRightBound);

        } catch (DataStoreException e) {
            module.getLogger().error("Unable to read bucket for sitemap config", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean isValidFileType(String fileName, String mimeType) {
        if(fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
            return true;
        }
        return false;
    }

    @Override
    public OutputStream handleUpload(String filename) throws DataStoreException {
        return bucketStore.putObject(SITE_MAP_BUCKET, filename, Collections.emptyMap());
    }
}
