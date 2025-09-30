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
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.DataStoreException;

import java.io.*;
import java.util.Collection;
import java.util.Collections;

import static com.botts.impl.service.oscar.Constants.SITE_MAP_BUCKET;

public class SitemapDiagramHandler {

    IBucketService bucketService;
    IBucketStore bucketStore;

    public SitemapDiagramHandler (IBucketService bucketService) {
        this.bucketService = bucketService;

    }

    public void handleFile(String filename) throws DataStoreException, FileNotFoundException {

        bucketStore = bucketService.getBucketStore();

        if (bucketStore == null) {
            return;
        }
        boolean siteMapBucketExists = bucketStore.bucketExists(SITE_MAP_BUCKET);

        if (!siteMapBucketExists) {
            bucketStore.createBucket(SITE_MAP_BUCKET);
        }

        bucketStore.putObject(SITE_MAP_BUCKET, filename, Collections.emptyMap());
    }

    public OutputStream handleUpload(String filename) throws DataStoreException {

        bucketStore = bucketService.getBucketStore();

        if (bucketStore == null) {
            return null;
        }
        boolean siteMapBucketExists = bucketStore.bucketExists(SITE_MAP_BUCKET);

        if (!siteMapBucketExists) {
            bucketStore.createBucket(SITE_MAP_BUCKET);
        }

        return bucketStore.putObject(SITE_MAP_BUCKET, filename, Collections.emptyMap());
    }
}
