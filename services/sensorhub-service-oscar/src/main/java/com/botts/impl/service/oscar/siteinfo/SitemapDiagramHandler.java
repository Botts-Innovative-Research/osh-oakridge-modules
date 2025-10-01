
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
import com.botts.impl.service.oscar.OSCARServiceModule;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Collections;

import static com.botts.impl.service.oscar.Constants.SITE_MAP_BUCKET;

public class SitemapDiagramHandler {

    IBucketService bucketService;
    IBucketStore bucketStore;
    SiteInfoOutput siteInfoOutput;
    OSCARServiceModule module;

    public SitemapDiagramHandler(IBucketService bucketService, SiteInfoOutput siteInfoOutput, OSCARServiceModule module) {
        this.bucketService = bucketService;
        this.siteInfoOutput = siteInfoOutput;
        this.bucketStore = bucketService.getBucketStore();
        this.module = module;
    }

    public void handleFile(String filename) throws DataStoreException, FileNotFoundException {
        if (bucketStore == null)
            return;

        if (!bucketStore.bucketExists(SITE_MAP_BUCKET))
            bucketStore.createBucket(SITE_MAP_BUCKET);


        bucketStore.putObject(SITE_MAP_BUCKET, filename, Collections.emptyMap());
    }

    public OutputStream handleUpload(String filename, SiteDiagramConfig.LatLonLocation siteLowerLeftBound, SiteDiagramConfig.LatLonLocation siteUpperRightBound) throws DataStoreException {
        if (bucketStore == null) {
            module.getLogger().warn("Bucket store is null");
            return null;
        }
        if (!bucketStore.bucketExists(SITE_MAP_BUCKET)){
            module.getLogger().warn("Bucket does not exist, creating new bucket: " + SITE_MAP_BUCKET);
            bucketStore.createBucket(SITE_MAP_BUCKET);
        }


        siteInfoOutput.setData(filename, siteLowerLeftBound, siteUpperRightBound);

        return bucketStore.putObject(SITE_MAP_BUCKET, filename, Collections.emptyMap());
    }
}
