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
import com.botts.impl.service.oscar.alarms.DashboardAlarmReadStateHandler;
import com.botts.impl.service.oscar.vehicle.VehicleSearchHandler;
import com.botts.impl.service.oscar.dailyfile.DailyFileSliceHandler;
import com.botts.impl.service.oscar.diagnostics.SupportBundleHandler;
import com.botts.impl.service.oscar.eml.EmlPluginHandler;
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
import org.sensorhub.impl.utils.rad.interfaces.IAdjudicationCodeProvider;
import org.sensorhub.impl.utils.rad.dailyfile.IDailyFileJournalService;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.OccupancyExtended;
import org.sensorhub.impl.utils.rad.webid.WebIdClient;
import com.botts.impl.service.oscar.webid.WebIdResourceHandler;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServer;
import net.opengis.swe.v20.DataComponent;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OSCARServiceModule extends AbstractModule<OSCARServiceConfig> implements IWebIdProvider, IAdjudicationCodeProvider {
    static final long STOP_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(8);

    SiteInfoOutput siteInfoOutput;
    AdjudicationCodesOutput adjudicationCodesOutput;
    RequestReportControl reportControl;
    StatisticsOutput statsOutput;
    StatisticsControl statsControl;
    OSCARSystem system;

    SitemapDiagramHandler sitemapDiagramHandler;
    IBucketService bucketService;
    IBucketStore bucketStore;

    SpreadsheetHandler spreadsheetHandler;
    EmlPluginHandler emlPluginHandler;
    VideoRetention videoRetention;
    DatabasePurger databasePurger;
    AdjudicationCodeMarkerBackfill adjudicationCodeBackfill;
    StoragePressureRetention storagePressureRetention;
    WebIdResourceHandler webIdResourceHandler;
    MediaResourceHandler mediaResourceHandler;
    DailyFileSliceHandler dailyFileSliceHandler;
    DashboardAlarmReadStateHandler dashboardAlarmReadStateHandler;
    VehicleSearchHandler vehicleSearchHandler;
    SupportBundleHandler supportBundleHandler;
    MediaSetStore retainedMediaStore;
    ApplicationReadiness applicationReadiness;
    volatile Flow.Subscription staticGuideMimeSubscription;
    volatile boolean staticGuideMimeStopped;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        try {
            config.getCustomAdjudicationCodeValues();
        } catch (IllegalArgumentException e) {
            throw new SensorHubException("Invalid custom disposition code configuration", e);
        }

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

        emlPluginHandler = new EmlPluginHandler(getParentHub(), getLogger(), config);
        emlPluginHandler.restoreConfiguredOrActivePlugin(config.emlPluginPath);

        spreadsheetHandler = new SpreadsheetHandler(getParentHub().getModuleRegistry(), bucketStore, getLogger(), config);
        if (config.spreadsheetConfigPath != null && !config.spreadsheetConfigPath.isEmpty())
            spreadsheetHandler.handleFile(config.spreadsheetConfigPath);

        system = new OSCARSystem(config.nodeId);

        createOutputs();
        createControls();

        sitemapDiagramHandler = new SitemapDiagramHandler(getBucketService(), siteInfoOutput, this);

        if (getConfiguration().videoRetentionConfig != null) {
            int frameCount = getConfiguration().videoRetentionConfig.enableFrameRetention ? getConfiguration().videoRetentionConfig.frameRetentionCount : 0;
            Duration secondaryFrameRetention = getConfiguration().videoRetentionConfig.enableSecondaryFramePruning
                    ? Duration.ofDays(getConfiguration().videoRetentionConfig.secondaryFrameRetentionDays)
                    : null;
            videoRetention = new VideoRetention(getParentHub(),
                    bucketStore,
                    Duration.ofMinutes(getConfiguration().videoRetentionConfig.videoQueryPeriod),
                    Duration.ofDays(getConfiguration().videoRetentionConfig.timeToRetention),
                    frameCount,
                    getConfiguration().videoRetentionConfig.jpegQuality,
                    Duration.ofHours(getConfiguration().videoRetentionConfig.sourceRollbackGraceHours),
                    secondaryFrameRetention,
                    Duration.ofHours(getConfiguration().videoRetentionConfig.maximumCatchUpHours),
                    getConfiguration().videoRetentionConfig.maintenanceWorkLimit,
                    Duration.ofHours(getConfiguration().videoRetentionConfig.lateArrivalOverlapHours),
                    Duration.ofMinutes(
                            getConfiguration().videoRetentionConfig.lateArrivalScanPeriodMinutes),
                    Duration.ofHours(
                            getConfiguration().videoRetentionConfig.lateArrivalCatchUpHours),
                    Duration.ofHours(
                            getConfiguration().videoRetentionConfig.reconciliationCatchUpHours),
                    Duration.ofHours(
                            getConfiguration().videoRetentionConfig.reconciliationPeriodHours),
                    Duration.ofMinutes(
                            getConfiguration().videoRetentionConfig.occupancyStreamRefreshMinutes),
                    getConfiguration().videoRetentionConfig.metadataReclamationWorkLimit);
        } else {
            videoRetention = null;
            logger.info("No video retention config set.");
        }
        retainedMediaStore = videoRetention != null
                ? videoRetention.getMediaStore() : createMediaStore();

        if (getConfiguration().storagePressureRetentionConfig != null) {
            storagePressureRetention = new StoragePressureRetention(bucketStore,
                    retainedMediaStore, getConfiguration().storagePressureRetentionConfig,
                    this::isOperatorAdjudicatedOccupancy, getLogger());
        } else {
            storagePressureRetention = null;
            logger.info("No storage pressure retention config set.");
        }

        system.updateSensorDescription();
    }

    public void createOutputs(){
        adjudicationCodesOutput = new AdjudicationCodesOutput(system);
        system.addOutput(adjudicationCodesOutput, false);

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

        registerSystemAndPublishCodes();

        if (config.databaseID != null && !config.databaseID.isBlank()) {
            var module = getParentHub().getModuleRegistry().getModuleById(config.databaseID);

            if (config.delegateObservationLifecycleToDatabase)
                getLogger().info("High-volume observation lifecycle is delegated to the configured database; "
                        + "OSCAR will retain manifest-gated daily-file and connection-status maintenance");
            if (databasePurger == null)
                databasePurger = new DatabasePurger((IObsSystemDatabase) module, bucketStore, 5,
                        getParentHub().getModuleRegistry().getModuleByType(IDailyFileJournalService.class),
                        config.delegateObservationLifecycleToDatabase,
                        config.dailyFilePurgeWorkLimit,
                        config.connectionStatusRetentionHours);
            databasePurger.start();

            if (adjudicationCodeBackfill == null) {
                adjudicationCodeBackfill = new AdjudicationCodeMarkerBackfill(
                        (IObsSystemDatabase) module,
                        getParentHub().getIdEncoders().getObsIdEncoder(),
                        getLogger());
            }
            adjudicationCodeBackfill.start();
        }

        String effectiveWebIdApiRoot = (config.webIdApiRoot != null && !config.webIdApiRoot.isBlank())
                ? config.webIdApiRoot.trim()
                : OSCARServiceConfig.DEFAULT_WEBID_API_ROOT;
        WebIdClient webIdClient = new WebIdClient(effectiveWebIdApiRoot);
        webIdResourceHandler = new WebIdResourceHandler(bucketStore, getParentHub(), webIdClient);
        bucketService.registerObjectHandler(webIdResourceHandler);
        mediaResourceHandler = new MediaResourceHandler(retainedMediaStore);
        bucketService.registerObjectHandler(mediaResourceHandler);
        try {
            bucketService.ensureBucketRegistered(DatabasePurger.DAILY_FILES_BUCKET);
            dailyFileSliceHandler = new DailyFileSliceHandler(
                    bucketStore,
                    this::resolveSliceDatabase,
                    () -> getParentHub().getModuleRegistry().getModuleByType(IDailyFileJournalService.class));
            bucketService.registerObjectHandler(dailyFileSliceHandler);
        } catch (org.sensorhub.api.datastore.DataStoreException e) {
            throw new SensorHubException("Unable to initialize daily-file slice handler", e);
        }
        try {
            bucketService.ensureBucketRegistered(Constants.ADJUDICATION_BUCKET);
            dashboardAlarmReadStateHandler = new DashboardAlarmReadStateHandler(bucketStore, getLogger());
            bucketService.registerObjectHandler(dashboardAlarmReadStateHandler);
            vehicleSearchHandler = new VehicleSearchHandler(bucketStore, this);
            bucketService.registerObjectHandler(vehicleSearchHandler);
        } catch (org.sensorhub.api.datastore.DataStoreException e) {
            throw new SensorHubException("Unable to initialize adjudication bucket object handlers", e);
        }
        try {
            bucketService.ensureBucketRegistered(Constants.REPORT_BUCKET);
            supportBundleHandler = new SupportBundleHandler(getParentHub(), config, getLogger());
            bucketService.registerObjectHandler(supportBundleHandler);
        } catch (org.sensorhub.api.datastore.DataStoreException e) {
            throw new SensorHubException("Unable to initialize support bundle handler", e);
        }

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

        startStaticGuideMimeRegistration();
        startApplicationReadiness();
    }

    @Override
    protected void doStop() throws SensorHubException {
        long deadline = System.nanoTime() + STOP_BUDGET_NANOS;
        SensorHubException failure = null;
        // Withdraw readiness before stopping any service. This prevents a second
        // administrative lifecycle operation from observing a partially stopping
        // application as ready.
        failure = runStopStep(failure, "application readiness", this::stopApplicationReadiness);
        failure = runStopStep(failure, "base OSCAR module", this::stopBaseModule);

        // Cancellation must be broadcast before waiting for any one worker. A JDBC call can
        // legitimately take the whole bounded stop interval to acknowledge cancellation. If the
        // workers were stopped serially, that first wait would leave every later service running
        // and give it an already-expired deadline. The request phase is deliberately non-blocking;
        // the await phase below remains truthful and shares this one module-registry-safe deadline.
        failure = runStopStep(failure, "report worker stop request",
                this::requestReportControlStop);
        failure = runStopStep(failure, "statistics output stop request",
                this::requestStatisticsOutputStop);
        failure = runStopStep(failure, "database purger stop request",
                this::requestDatabasePurgerStop);
        failure = runStopStep(failure, "adjudication code backfill stop request",
                this::requestAdjudicationCodeBackfillStop);
        failure = runStopStep(failure, "video retention stop request",
                this::requestVideoRetentionStop);
        failure = runStopStep(failure, "storage-pressure retention stop request",
                this::requestStoragePressureRetentionStop);

        failure = runStopStep(failure, "report worker",
                () -> awaitReportControlStop(deadline));
        failure = runStopStep(failure, "statistics output",
                () -> awaitStatisticsOutputStop(deadline));
        failure = runStopStep(failure, "database purger",
                () -> awaitDatabasePurgerStop(deadline));
        failure = runStopStep(failure, "adjudication code backfill",
                () -> awaitAdjudicationCodeBackfillStop(deadline));
        failure = runStopStep(failure, "video retention",
                () -> awaitVideoRetentionStop(deadline));
        failure = runStopStep(failure, "storage-pressure retention",
                () -> awaitStoragePressureRetentionStop(deadline));
        failure = runStopStep(failure, "Web ID object handler", this::unregisterWebIdHandler);
        failure = runStopStep(failure, "retained-media object handler", this::unregisterMediaHandler);
        failure = runStopStep(failure, "daily-file slice object handler", this::unregisterDailyFileSliceHandler);
        failure = runStopStep(failure, "dashboard alarm read-state object handler",
                this::unregisterDashboardAlarmReadStateHandler);
        failure = runStopStep(failure, "vehicle search object handler",
                this::unregisterVehicleSearchHandler);
        failure = runStopStep(failure, "support bundle object handler",
                this::unregisterSupportBundleHandler);
        failure = runStopStep(failure, "static guide MIME registration",
                this::stopStaticGuideMimeRegistration);
        failure = runStopStep(failure, "OSCAR system registration", this::unregisterSystem);
        if (failure != null)
            throw failure;
    }

    protected void registerSystemAndPublishCodes() throws SensorHubException {
        var registry = getParentHub().getSystemDriverRegistry();
        // Select the target database before registering any output listeners.
        if (config.databaseID != null && !config.databaseID.isBlank()) {
            var database = (IObsSystemDatabase) getParentHub().getModuleRegistry().getModuleById(config.databaseID);
            var current = registry.getDatabase(system.getUniqueIdentifier());
            if (current == null)
                registry.registerDatabase(system.getUniqueIdentifier(), database);
            else if (current != database)
                throw new SensorHubException("OSCAR system is already mapped to a different database");
        }

        awaitSystemRegistration(registry.register(system));
        purgePriorAdjudicationCodesObservations();
        adjudicationCodesOutput.publish(config.customAdjudicationCodes);
    }

    private void purgePriorAdjudicationCodesObservations() {
        try {
            IObsSystemDatabase db = resolveSliceDatabase();
            if (db != null && db.getObservationStore() != null && db.getDataStreamStore() != null) {
                var streamIds = db.getDataStreamStore()
                        .selectKeys(new DataStreamFilter.Builder()
                                .withOutputNames(AdjudicationCodesOutput.NAME)
                                .build())
                        .map(org.sensorhub.api.resource.ResourceKey::getInternalID)
                        .collect(java.util.stream.Collectors.toSet());
                if (!streamIds.isEmpty()) {
                    db.getObservationStore().removeEntries(new ObsFilter.Builder()
                            .withDataStreams(streamIds)
                            .build());
                }
            }
        } catch (Exception e) {
            getLogger().debug("Could not prune prior adjudicationCodes observations before publishing current codes", e);
        }
    }

    protected void unregisterSystem() throws SensorHubException {
        if (system != null)
            awaitSystemRegistration(getParentHub().getSystemDriverRegistry().unregister(system));
    }

    private void awaitSystemRegistration(CompletableFuture<?> operation) throws SensorHubException {
        try {
            operation.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SensorHubException("Interrupted while registering OSCAR system", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new SensorHubException("Could not complete OSCAR system registration", e);
        }
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

    void startApplicationReadiness() throws SensorHubException {
        applicationReadiness = new ApplicationReadiness(
                () -> getParentHub().getModuleRegistry().getLoadedModules(),
                ApplicationReadiness.DEFAULT_MARKER,
                ApplicationReadiness.DEFAULT_POLL_MILLIS,
                getLogger());
        try {
            applicationReadiness.start();
        } catch (java.io.IOException error) {
            applicationReadiness = null;
            throw new SensorHubException("Could not start OSCAR application readiness", error);
        }
    }

    void stopApplicationReadiness() throws java.io.IOException {
        if (applicationReadiness == null)
            return;
        try {
            applicationReadiness.close();
        } finally {
            applicationReadiness = null;
        }
    }

    void requestReportControlStop() {
        if (reportControl != null)
            reportControl.requestStop();
    }

    void requestStatisticsOutputStop() {
        if (statsOutput != null)
            statsOutput.requestStop();
    }

    void requestDatabasePurgerStop() {
        if (databasePurger != null)
            databasePurger.requestStop();
    }

    void requestAdjudicationCodeBackfillStop() {
        if (adjudicationCodeBackfill != null)
            adjudicationCodeBackfill.requestStop();
    }

    void requestVideoRetentionStop() {
        if (videoRetention != null)
            videoRetention.requestStop();
    }

    void requestStoragePressureRetentionStop() {
        if (storagePressureRetention != null)
            storagePressureRetention.requestStop();
    }

    void awaitReportControlStop(long deadline) {
        if (reportControl != null)
            reportControl.awaitStop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
    }

    void awaitStatisticsOutputStop(long deadline) {
        if (statsOutput != null)
            statsOutput.awaitStop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
    }

    void awaitDatabasePurgerStop(long deadline) throws SensorHubException {
        if (databasePurger == null)
            return;
        databasePurger.awaitStop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
        databasePurger = null;
    }

    void awaitAdjudicationCodeBackfillStop(long deadline) {
        if (adjudicationCodeBackfill == null)
            return;
        adjudicationCodeBackfill.awaitStop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
        adjudicationCodeBackfill = null;
    }

    void awaitVideoRetentionStop(long deadline) {
        if (videoRetention != null)
            videoRetention.awaitStop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
    }

    void awaitStoragePressureRetentionStop(long deadline) {
        if (storagePressureRetention != null)
            storagePressureRetention.awaitStop(remainingStopNanos(deadline), TimeUnit.NANOSECONDS);
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

    void unregisterDailyFileSliceHandler() {
        if (dailyFileSliceHandler == null)
            return;
        bucketService.unregisterObjectHandler(dailyFileSliceHandler);
        dailyFileSliceHandler = null;
    }

    void unregisterDashboardAlarmReadStateHandler() {
        if (dashboardAlarmReadStateHandler == null)
            return;
        bucketService.unregisterObjectHandler(dashboardAlarmReadStateHandler);
        dashboardAlarmReadStateHandler = null;
    }

    void unregisterVehicleSearchHandler() {
        if (vehicleSearchHandler == null)
            return;
        bucketService.unregisterObjectHandler(vehicleSearchHandler);
        vehicleSearchHandler = null;
    }

    void unregisterSupportBundleHandler() {
        if (supportBundleHandler == null)
            return;
        bucketService.unregisterObjectHandler(supportBundleHandler);
        supportBundleHandler = null;
    }

    private IObsSystemDatabase resolveSliceDatabase() {
        if (config != null && config.databaseID != null && !config.databaseID.isBlank()) {
            try {
                var module = getParentHub().getModuleRegistry().getModuleById(config.databaseID);
                if (module instanceof IObsSystemDatabase db)
                    return db;
            } catch (SensorHubException ignored) {
                // Fall back to federated database below.
            }
        }
        return getParentHub() != null && getParentHub().getDatabaseRegistry() != null
                ? getParentHub().getDatabaseRegistry().getFederatedDatabase() : null;
    }

    private boolean isOperatorAdjudicatedOccupancy(String encodedObsId) {
        if (encodedObsId == null || encodedObsId.isBlank() || getParentHub() == null)
            return false;
        try {
            var encoders = getParentHub().getIdEncoders();
            if (encoders == null || encoders.getObsIdEncoder() == null)
                return false;
            BigId obsBigId = encoders.getObsIdEncoder().decodeID(encodedObsId);
            IObsSystemDatabase db = resolveSliceDatabase();
            if (obsBigId == null || db == null || db.getObservationStore() == null)
                return false;
            IObsData obs = db.getObservationStore().get(obsBigId);
            if (obs == null || obs.getResult() == null)
                return false;
            var ds = db.getObservationStore().getDataStreams().get(new DataStreamKey(obs.getDataStreamID()));
            if (ds == null)
                return false;
            DataComponent struct = ds.getRecordStructure();
            if (struct == null || !java.util.Objects.equals(RADHelper.getRadUri("Occupancy"), struct.getDefinition()))
                return false;
            Occupancy occ = OccupancyExtended.readOccupancy(struct, obs.getResult());
            if (occ == null || occ.getAdjudicatedIds() == null || occ.getAdjudicatedIds().isEmpty())
                return false;
            for (String marker : occ.getAdjudicatedIds()) {
                if (marker == null || marker.isBlank())
                    continue;
                if (Adjudication.EML_NON_THREAT_MARKER.equals(marker)
                        || "code:9999".equals(marker)
                        || Adjudication.EXTENDED_OCCUPANCY_GAMMA_MARKER.equals(marker)
                        || "code:9998".equals(marker)) {
                    continue;
                }
                return true;
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    @FunctionalInterface
    private interface StopAction {
        void run() throws Exception;
    }

    private MediaSetStore createMediaStore() throws SensorHubException {
        try {
            Duration secondaryFrameRetention = config != null && config.videoRetentionConfig != null
                    && config.videoRetentionConfig.enableSecondaryFramePruning
                    ? Duration.ofDays(config.videoRetentionConfig.secondaryFrameRetentionDays)
                    : null;
            return new MediaSetStore(bucketStore, secondaryFrameRetention);
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

    public synchronized EmlPluginHandler getEmlPluginHandler() {
        if (emlPluginHandler == null && getParentHub() != null) {
            emlPluginHandler = new EmlPluginHandler(getParentHub(), getLogger(), config);
            if (config != null) {
                emlPluginHandler.restoreConfiguredOrActivePlugin(config.emlPluginPath);
            }
        }
        return emlPluginHandler;
    }

    public WebIdResourceHandler getWebIdResourceHandler() { return webIdResourceHandler; }

    @Override
    public WebIdClient getWebIdClient() { return webIdResourceHandler.getWebIdClient(); }

    static void registerStaticGuideMimeMappings(MimeTypes mimeTypes) {
        if (mimeTypes == null)
            return;
        mimeTypes.addMimeMapping("vtt", "text/vtt;charset=utf-8");
        mimeTypes.addMimeMapping("srt", "text/plain;charset=utf-8");
        mimeTypes.addMimeMapping("mp4", "video/mp4");
    }

    private void startStaticGuideMimeRegistration() {
        staticGuideMimeStopped = false;
        ensureStaticGuideMimeTypes();
        if (staticGuideMimeSubscription == null && getParentHub() != null && getParentHub().getEventBus() != null) {
            getParentHub().getEventBus().newSubscription(ModuleEvent.class)
                    .withTopicID(ModuleRegistry.EVENT_GROUP_ID)
                    .subscribe(moduleEvent -> {
                        if (moduleEvent.getType() == ModuleEvent.Type.STATE_CHANGED
                                && moduleEvent.getNewState() == ModuleEvent.ModuleState.STARTED) {
                            ensureStaticGuideMimeTypes();
                        }
                    })
                    .thenAccept(sub -> {
                        if (staticGuideMimeStopped) {
                            sub.cancel();
                        } else {
                            staticGuideMimeSubscription = sub;
                            sub.request(Long.MAX_VALUE);
                            ensureStaticGuideMimeTypes();
                        }
                    });
        }
    }

    private void stopStaticGuideMimeRegistration() {
        staticGuideMimeStopped = true;
        Flow.Subscription sub = staticGuideMimeSubscription;
        staticGuideMimeSubscription = null;
        if (sub != null) {
            sub.cancel();
        }
    }

    private void ensureStaticGuideMimeTypes() {
        if (getParentHub() == null || getParentHub().getModuleRegistry() == null)
            return;
        HttpServer httpServer = getParentHub().getModuleRegistry().getModuleByType(HttpServer.class);
        if (httpServer == null || httpServer.getJettyServer() == null)
            return;
        Handler[] contextHandlers = httpServer.getJettyServer().getChildHandlersByClass(ContextHandler.class);
        if (contextHandlers != null) {
            for (Handler handler : contextHandlers) {
                if (handler instanceof ContextHandler contextHandler) {
                    registerStaticGuideMimeMappings(contextHandler.getMimeTypes());
                }
            }
        }
        Handler[] resourceHandlers = httpServer.getJettyServer().getChildHandlersByClass(ResourceHandler.class);
        if (resourceHandlers != null) {
            for (Handler handler : resourceHandlers) {
                if (handler instanceof ResourceHandler resourceHandler && resourceHandler.getMimeTypes() != null) {
                    registerStaticGuideMimeMappings(resourceHandler.getMimeTypes());
                }
            }
        }
    }

    @Override
    public boolean isCustomAdjudicationCode(int code) {
        return config != null && config.getCustomAdjudicationCodeValues().contains(code);
    }
}
