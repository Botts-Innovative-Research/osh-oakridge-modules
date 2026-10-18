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
import com.botts.impl.service.oscar.purge.DatabasePurger;
import com.botts.impl.service.oscar.retention.StoragePressureRetention;
import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.siteinfo.SiteInfoOutput;
import com.botts.impl.service.oscar.siteinfo.SitemapDiagramHandler;
import com.botts.impl.service.oscar.spreadsheet.SpreadsheetHandler;
import com.botts.impl.service.oscar.stats.StatisticsControl;
import com.botts.impl.service.oscar.stats.StatisticsOutput;
import com.botts.impl.service.oscar.video.VideoRetention;
import com.botts.impl.service.oscar.media.MediaResourceHandler;
import com.botts.impl.service.oscar.media.MediaSetStore;
import org.sensorhub.impl.utils.rad.interfaces.IWebIdProvider;
import org.sensorhub.impl.utils.rad.dailyfile.IDailyFileJournalService;
import org.sensorhub.impl.utils.rad.webid.WebIdClient;
import com.botts.impl.service.oscar.webid.WebIdResourceHandler;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.AbstractModule;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OSCARServiceModule extends AbstractModule<OSCARServiceConfig> implements IWebIdProvider {
    static final long STOP_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(8);

    SiteInfoOutput siteInfoOutput;
    RequestReportControl reportControl;
    StatisticsOutput statsOutput;
    StatisticsControl statsControl;
    OSCARSystem system;

    SitemapDiagramHandler sitemapDiagramHandler;
    IBucketService bucketService;
    IBucketStore bucketStore;

    SpreadsheetHandler spreadsheetHandler;
    VideoRetention videoRetention;
    DatabasePurger databasePurger;
    StoragePressureRetention storagePressureRetention;
    WebIdResourceHandler webIdResourceHandler;
    MediaResourceHandler mediaResourceHandler;
    MediaSetStore retainedMediaStore;

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
                    frameCount,
                    getConfiguration().videoRetentionConfig.jpegQuality,
                    Duration.ofHours(getConfiguration().videoRetentionConfig.sourceRollbackGraceHours),
                    Duration.ofHours(getConfiguration().videoRetentionConfig.maximumCatchUpHours),
                    getConfiguration().videoRetentionConfig.maintenanceWorkLimit,
                    Duration.ofHours(getConfiguration().videoRetentionConfig.lateArrivalOverlapHours),
                    Duration.ofHours(getConfiguration().videoRetentionConfig.reconciliationCatchUpHours),
                    getConfiguration().videoRetentionConfig.metadataReclamationWorkLimit);
        } else {
            videoRetention = null;
            logger.info("No video retention config set.");
        }
        retainedMediaStore = videoRetention != null
                ? videoRetention.getMediaStore() : createMediaStore();

        if (getConfiguration().storagePressureRetentionConfig != null) {
            storagePressureRetention = new StoragePressureRetention(bucketStore,
                    retainedMediaStore, getConfiguration().storagePressureRetentionConfig, getLogger());
        } else {
            storagePressureRetention = null;
            logger.info("No storage pressure retention config set.");
        }

        system.updateSensorDescription();
    }

    public void createOutputs(){
        siteInfoOutput = new SiteInfoOutput(system);
        system.addOutput(siteInfoOutput, false);

        IObsSystemDatabase database = null;
        if (config.databaseID != null && !config.databaseID.isBlank()) {
            try {
                database = (IObsSystemDatabase) getParentHub().getModuleRegistry().getModuleById(config.databaseID);
            } catch (SensorHubException e) {
                getLogger().warn("No database configured for OSCAR service");
                database = null;
            }
        }

        if (database == null)
            database = getParentHub().getDatabaseRegistry().getFederatedDatabase();

        statsOutput = new StatisticsOutput(system, database, config.statsFrequencyMinutes);
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


            if (config.delegateObservationLifecycleToDatabase)
                getLogger().info("High-volume observation lifecycle is delegated to the configured database; "
                        + "OSCAR will retain manifest-gated daily-file and connection-status maintenance");
            if (databasePurger == null)
                databasePurger = new DatabasePurger((IObsSystemDatabase) module, bucketStore, 5,
                        getParentHub().getModuleRegistry().getModuleByType(IDailyFileJournalService.class),
                        config.delegateObservationLifecycleToDatabase,
                        config.dailyFilePurgeWorkLimit);
            databasePurger.start();
        }

        WebIdClient webIdClient = (config.webIdApiRoot != null && !config.webIdApiRoot.isBlank())
                ? new WebIdClient(config.webIdApiRoot) : null;
        webIdResourceHandler = new WebIdResourceHandler(bucketStore, getParentHub(), webIdClient);
        bucketService.registerObjectHandler(webIdResourceHandler);
        mediaResourceHandler = new MediaResourceHandler(retainedMediaStore);
        bucketService.registerObjectHandler(mediaResourceHandler);

        statsOutput.start();

        if (config.siteDiagramConfig != null && config.siteDiagramConfig.siteDiagramPath != null &&
                !config.siteDiagramConfig.siteDiagramPath.isBlank()) {
            if (!sitemapDiagramHandler.handleFile(
                    config.siteDiagramConfig.siteDiagramPath, config.siteDiagramConfig))
                getLogger().warn("Unable to activate configured site diagram {}",
                        config.siteDiagramConfig.siteDiagramPath);
        } else {
            refreshSiteDiagram();
        }

        if (videoRetention != null)
            videoRetention.start();

        if (storagePressureRetention != null)
            storagePressureRetention.start();
    }

    @Override
    protected void doStop() throws SensorHubException {
        long deadline = System.nanoTime() + STOP_BUDGET_NANOS;
        SensorHubException failure = null;
        failure = runStopStep(failure, "base OSCAR module", this::stopBaseModule);
        failure = runStopStep(failure, "statistics output",
                () -> stopStatisticsOutput(deadline));
        failure = runStopStep(failure, "database purger",
                () -> stopDatabasePurger(deadline));
        failure = runStopStep(failure, "video retention",
                () -> stopVideoRetention(deadline));
        failure = runStopStep(failure, "storage-pressure retention",
                () -> stopStoragePressureRetention(deadline));
        failure = runStopStep(failure, "Web ID object handler", this::unregisterWebIdHandler);
        failure = runStopStep(failure, "retained-media object handler", this::unregisterMediaHandler);
        if (failure != null)
            throw failure;
    }

    private SensorHubException runStopStep(SensorHubException aggregate,
                                           String description,
                                           StopAction action) {
        try {
            action.run();
            return aggregate;
        } catch (Throwable error) {
            getLogger().error("Could not stop {}", description, error);
            SensorHubException stepFailure = new SensorHubException(
                    "Could not stop " + description, error);
            if (aggregate == null)
                return stepFailure;
            aggregate.addSuppressed(stepFailure);
            return aggregate;
        }
    }

    void stopBaseModule() throws SensorHubException {
        super.doStop();
    }

    void stopStatisticsOutput(long deadline) {
        if (statsOutput != null)
            statsOutput.stop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
    }

    void stopDatabasePurger(long deadline) throws SensorHubException {
        if (databasePurger == null)
            return;
        databasePurger.stop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
        databasePurger = null;
    }

    void stopVideoRetention(long deadline) {
        if (videoRetention != null)
            videoRetention.stop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
    }

    void stopStoragePressureRetention(long deadline) {
        if (storagePressureRetention != null)
            storagePressureRetention.stop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
    }

    private static long remainingStopNanos(long deadline) {
        return Math.max(1L, deadline - System.nanoTime());
    }

    void unregisterWebIdHandler() {
        if (webIdResourceHandler == null)
            return;
        bucketService.unregisterObjectHandler(webIdResourceHandler);
        webIdResourceHandler = null;
    }

    void unregisterMediaHandler() {
        if (mediaResourceHandler == null)
            return;
        bucketService.unregisterObjectHandler(mediaResourceHandler);
        mediaResourceHandler = null;
    }

    @FunctionalInterface
    private interface StopAction {
        void run() throws Exception;
    }

    private MediaSetStore createMediaStore() throws SensorHubException {
        try {
            return new MediaSetStore(bucketStore);
        } catch (org.sensorhub.api.datastore.DataStoreException e) {
            throw new SensorHubException("Unable to initialize retained-media storage", e);
        }
    }

    private void refreshSiteDiagram() {
        long currTime = System.currentTimeMillis();
        var query = getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withOutputNames(SiteInfoOutput.NAME)
                        .build())
                .withLatestResult()
                .build());

        var obsList = query.toList();
        if (obsList.isEmpty())
            return;

        var res = obsList.get(0).getResult();

        siteInfoOutput.setData(res);
        System.out.println("Site diagram refreshed in " +  (System.currentTimeMillis() - currTime) + "ms");
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

    public WebIdResourceHandler getWebIdResourceHandler() { return webIdResourceHandler; }

    @Override
    public WebIdClient getWebIdClient() { return webIdResourceHandler.getWebIdClient(); }
}
