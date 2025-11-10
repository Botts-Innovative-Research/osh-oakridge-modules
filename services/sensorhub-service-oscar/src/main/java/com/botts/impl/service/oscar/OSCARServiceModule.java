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

package com.botts.impl.service.oscar;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.siteinfo.SiteInfoOutput;
import com.botts.impl.service.oscar.siteinfo.SitemapDiagramHandler;
import com.botts.impl.service.oscar.spreadsheet.SpreadsheetHandler;
import com.botts.impl.service.oscar.stats.StatisticsControl;
import com.botts.impl.service.oscar.stats.StatisticsOutput;
import com.botts.impl.service.oscar.video.VideoRetention;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.AbstractModule;

import java.time.Duration;
import java.time.Period;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OSCARServiceModule extends AbstractModule<OSCARServiceConfig> {
    SiteInfoOutput siteInfoOutput;
    RequestReportControl reportControl;
    StatisticsOutput statsOutput;
    StatisticsControl statsControl;

    SitemapDiagramHandler sitemapDiagramHandler;
    IBucketService bucketService;

    SpreadsheetHandler spreadsheetHandler;
    OSCARSystem system;
    IBucketStore bucketStore;

    VideoRetention videoRetention;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // Block here for bucket service
        try {
            getLogger().info("Checking that a bucket service is loaded...");
            this.bucketService = getParentHub().getModuleRegistry()
                    .waitForModuleType(IBucketService.class, ModuleEvent.ModuleState.STARTED)
                    .get(10, TimeUnit.SECONDS);
            this.bucketStore = bucketService.getBucketStore();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            reportError("Could not find this OSH node's Bucket Service", new IllegalStateException(e));
        }

        spreadsheetHandler = new SpreadsheetHandler(getParentHub().getModuleRegistry(), bucketStore, getLogger());
        if (config.spreadsheetConfigPath != null && !config.spreadsheetConfigPath.isEmpty())
            spreadsheetHandler.handleFile(config.spreadsheetConfigPath);



        system = new OSCARSystem(config.nodeId);

        createOutputs();
        createControls();

        sitemapDiagramHandler = new SitemapDiagramHandler(getBucketService(), siteInfoOutput, this);

        if (getConfiguration().videoRetentionConfig != null) {
            int frameCount = getConfiguration().videoRetentionConfig.enableFrameRetention ? getConfiguration().videoRetentionConfig.frameRetentionCount : 0;
            videoRetention = new VideoRetention(getParentHub(),
                    bucketStore,
                    Duration.ofMinutes(getConfiguration().videoRetentionConfig.videoQueryPeriod),
                    Duration.ofDays(getConfiguration().videoRetentionConfig.timeToRetention),
                    frameCount);
        } else {
            videoRetention = null;
            logger.info("No video retention config set.");
        }

        system.updateSensorDescription();
    }

    public void createOutputs(){
        siteInfoOutput = new SiteInfoOutput(system);
        system.addOutput(siteInfoOutput, false);

        statsOutput = new StatisticsOutput(system, getParentHub().getDatabaseRegistry().getFederatedDatabase());
        system.addOutput(statsOutput, false);
    }

    public void createControls(){
        reportControl = new RequestReportControl(system, this);
        system.addControlInput(reportControl);

        statsControl = new StatisticsControl(system);
        system.addControlInput(statsControl);
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        getParentHub().getSystemDriverRegistry().register(system);

        if (config.databaseID != null && !config.databaseID.isBlank()) {
            var module = getParentHub().getModuleRegistry().getModuleById(config.databaseID);
            if (getParentHub().getSystemDriverRegistry().getDatabase(system.getUniqueIdentifier()) == null)
                getParentHub().getSystemDriverRegistry().registerDatabase(system.getUniqueIdentifier(), (IObsSystemDatabase) module);
        }

        statsOutput.start();

        if (videoRetention != null)
            videoRetention.start();
        //videoRetention.decimate(bucketStore.getResourceURI(Constants.VIDEO_BUCKET, "test.mp4"));
    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
        try {
            statsOutput.stop();
        } catch (Exception ex) {
            getLogger().error("Could not stop stats output", ex);
        }

        if (videoRetention != null)
            videoRetention.stop();
    }

    public SitemapDiagramHandler getSitemapDiagramHandler() {
        return sitemapDiagramHandler;
    }

    public OSCARSystem getOSCARSystem() {
        return system;
    }

    public IBucketService getBucketService() {
        return bucketService;
    }

    public SpreadsheetHandler getSpreadsheetHandler() {
        return spreadsheetHandler;
    }
}
