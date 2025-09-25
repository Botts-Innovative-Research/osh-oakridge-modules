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

import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.utils.NamedThreadFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BucketStorageService extends AbstractHttpServiceModule<BucketStorageServiceConfig> {

    private BucketStorageServlet servlet;
    private ScheduledExecutorService threadPool;
    private IBucketStore bucketStore;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        if (config.initialBuckets != null && !config.initialBuckets.isEmpty()) {
            for (String bucket : config.initialBuckets)
                if (!bucketStore.bucketExists(bucket))
                    bucketStore.createBucket(bucket);
        }

        List<String> buckets = bucketStore.listBuckets();

        this.securityHandler = new BucketStorageSecurity(this, buckets, config.security.enableAccessControl);

        this.servlet = new BucketStorageServlet(this, securityHandler);

        this.threadPool = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory("BucketStorageService-Pool"));

        deploy();
    }

    private void deploy() {
        var wildcardEndpoint = config.endPoint + "/*";

        httpServer.deployServlet(servlet, wildcardEndpoint);
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
        super.doStop();
    }

    public String getPublicEndpointUrl() {
        return config.endPoint;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

}
