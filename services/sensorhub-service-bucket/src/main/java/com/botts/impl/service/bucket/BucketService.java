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

package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.filesystem.FileSystemBucketStore;
import com.botts.impl.service.bucket.handler.BucketHandler;
import com.botts.impl.service.bucket.handler.ObjectHandler;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.utils.NamedThreadFactory;
import org.vast.util.Asserts;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BucketService extends AbstractHttpServiceModule<BucketServiceConfig> implements IBucketService {

    private BucketServlet servlet;
    private ScheduledExecutorService threadPool;
    private IBucketStore bucketStore;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        Asserts.checkNotNull(config.fileStoreRootDir);

        try {
            bucketStore = new FileSystemBucketStore(Path.of(config.fileStoreRootDir));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (config.initialBuckets != null && !config.initialBuckets.isEmpty())
            for (String bucket : config.initialBuckets)
                if (!bucketStore.bucketExists(bucket))
                    bucketStore.createBucket(bucket);
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        List<String> buckets = bucketStore.listBuckets();

        this.securityHandler = new BucketSecurity(this, buckets, config.security.enableAccessControl);

        this.threadPool = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory("BucketStorageService-Pool"));

        ObjectHandler objectHandler = new ObjectHandler(bucketStore);
        BucketHandler bucketHandler = new BucketHandler(bucketStore, objectHandler);

        this.servlet = new BucketServlet(this, securityHandler, bucketHandler, objectHandler);

        deploy();
    }

    private void deploy() {
        var wildcardEndpoint = config.endPoint + "/*";

        httpServer.deployServlet(servlet, wildcardEndpoint);
        httpServer.addServletSecurity(wildcardEndpoint, config.security.requireAuth);
    }

    private void undeploy() {
        // return silently if HTTP server missing on stop
        if (httpServer == null || !httpServer.isStarted())
            return;

        if (servlet != null)
        {
            httpServer.undeployServlet(servlet);
            servlet.destroy();
            servlet = null;
        }
    }

    @Override
    protected void doStop() throws SensorHubException {
        undeploy();

        if (threadPool != null)
            threadPool.shutdown();
    }

    @Override
    public void cleanup() throws SensorHubException {
        if (securityHandler != null)
            securityHandler.unregister();
    }

    @Override
    public String getPublicEndpointUrl() {
        return config.endPoint;
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    @Override
    public IBucketStore getBucketStore() {
        return bucketStore;
    }

}
