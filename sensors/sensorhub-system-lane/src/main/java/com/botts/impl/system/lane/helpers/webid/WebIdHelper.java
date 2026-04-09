package com.botts.impl.system.lane.helpers.webid;

import com.botts.impl.system.lane.LaneSystem;
import com.botts.impl.system.lane.WebIdOutput;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.datastore.EmptyFilterIntersection;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.system.DataStreamTransactionHandler;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.OccupancyExtended;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.sensorhub.utils.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

public class WebIdHelper {
    private static final String WEBID_NAME = WebIdOutput.NAME;
    private static final String OCCUPANCY_NAME = OccupancyOutput.NAME;
    private static final Logger logger = LoggerFactory.getLogger(WebIdHelper.class);
    private final int MAX_OUTPUT_INTERVAL_MS = 10000;
    private final long MAX_OCCUPANCY_PRODUCER_WAIT_MS = 5000;
    private final long TIME_TIL_OBS_CHECK_MS = 300; // Wait before checking the obs store

    IdEncoder obsIdEncoder, sysEncoder;

    IObsStore webIdObsStore, occupancyObsStore;
    //DataStreamTransactionHandler webIdTransactionHandler, occupancyTransactionHandler;
    ObsFilter webIdFilter, occupancyFilter;
    LaneSystem laneSystem;
    ISensorHub hub;
    IDataProducerModule<?> occupancyProducer;
    Flow.Subscription webIdSub, occupancySub;
    Thread startupThread;

    volatile BigId lastWebIdBigId = null;
    volatile ObsData lastWebIdBlock = null;
    volatile WebIdAnalysis lastWebId = null;
    volatile long lastWebIdTime = 0;

    volatile BigId lastOccupancyBigId = null;
    volatile ObsData lastOccupancyBlock = null;
    volatile Occupancy lastOccupancy = null;
    volatile long lastOccupancyTime = 0;

    public WebIdHelper(LaneSystem laneSystem, IDataProducerModule<?> occupancyProducer) {
        this.laneSystem = laneSystem;
        this.hub = laneSystem.getParentHub();
        this.occupancyProducer = occupancyProducer;
        startupThread = new Thread(this::init);
        startupThread.start();
    }

    private void init() {
        try {
            occupancyProducer.waitForState(ModuleEvent.ModuleState.STARTED, MAX_OCCUPANCY_PRODUCER_WAIT_MS);
        } catch (SensorHubException e) {
            logger.error("Occupancy producer not started in time", e);
            return;
        }
        createSubscription();
    }

    private void createSubscription() {
        try {
            var eventBus = hub.getEventBus();
            obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();


            webIdObsStore = hub.getSystemDriverRegistry().getDatabase(laneSystem.getUniqueIdentifier()).getObservationStore();
            occupancyObsStore = hub.getSystemDriverRegistry().getDatabase(occupancyProducer.getUniqueIdentifier()).getObservationStore();

            /*
            var webIdSysTxHandler = new SystemDatabaseTransactionHandler(eventBus, laneSystem.getParentHub().getSystemDriverRegistry().getDatabase(laneSystem.getUniqueIdentifier()));
            webIdTransactionHandler = webIdSysTxHandler.getDataStreamHandler(laneSystem.getUniqueIdentifier(), WEBID_NAME);

            var occupancySysTxHandler = new SystemDatabaseTransactionHandler(eventBus, laneSystem.getParentHub().getSystemDriverRegistry().getDatabase(occupancyProducerUID));
            occupancyTransactionHandler = occupancySysTxHandler.getDataStreamHandler(occupancyProducerUID, OCCUPANCY_NAME);
             */

            var temp = new ObsFilter.Builder().withSystems().withUniqueIDs(laneSystem.getUniqueIdentifier()).build();

            eventBus.newSubscription(ObsEvent.class)
                    .withTopicID(EventUtils.getDataStreamDataTopicID(laneSystem.getUniqueIdentifier(), WEBID_NAME))
                    .subscribe(this::processWebIdEvent)
                    .thenAccept(subscription -> {
                        this.webIdSub = subscription;
                        subscription.request(Long.MAX_VALUE);
                        logger.info("Started subscription to " + WEBID_NAME);
                    });

            eventBus.newSubscription(ObsEvent.class)
                    .withTopicID(EventUtils.getDataStreamDataTopicID(occupancyProducer.getUniqueIdentifier(), OCCUPANCY_NAME))
                    .subscribe(this::processOccupancyEvent)
                    .thenAccept(subscription -> {
                        this.occupancySub = subscription;
                        subscription.request(Long.MAX_VALUE);
                        logger.info("Started subscription to " + OCCUPANCY_NAME);
                    });

        } catch (Exception e) {
            logger.error("Failed to create WebId/Occupancy subscription", e);
            this.stop();
        }
    }

    private void processWebIdEvent(ObsEvent event) {
        try {
            Thread.sleep(TIME_TIL_OBS_CHECK_MS);
        } catch (InterruptedException e) { return; }

        for (var obs : event.getObservations()) {
            ObsFilter filter = new ObsFilter.Builder()
                    .withSystems()
                        .withUniqueIDs(laneSystem.getUniqueIdentifier())
                        .done()
                    .withDataStreams()
                        .withOutputNames(WEBID_NAME)
                        .done()
                    .withPhenomenonTime()
                        .withSingleValue(obs.getPhenomenonTime())
                        .done()
                    .build();

            BigId obsId = null;
            try {
                obsId = webIdObsStore.selectKeys(filter).findFirst().get();
            } catch (Exception e) {
                logger.error("Empty filter intersection", e);
                return;
            }

            setWebId((ObsData) obs, obsId);

            if (lastOccupancy != null) {
                if (isTimeRoughlyEqual()) { // Success
                    correlateWebIdWithOccupancy();
                } else { // Fail
                    logger.warn("WebID output not received in time");
                    clearOccupancy();
                }
            }
        }
    }

    private void processOccupancyEvent(ObsEvent event) {
        try {
            Thread.sleep(TIME_TIL_OBS_CHECK_MS);
        } catch (InterruptedException e) { return; }

        for (var obs : event.getObservations()) {
            ObsFilter filter = new ObsFilter.Builder()
                    .withSystems()
                        .withUniqueIDs(occupancyProducer.getUniqueIdentifier())
                        .done()
                    .withDataStreams()
                        .withOutputNames(OCCUPANCY_NAME)
                        .done()
                    .withPhenomenonTime()
                        .withSingleValue(obs.getPhenomenonTime())
                        .done()
                    .build();

            BigId obsId = null;
            try {
                obsId = occupancyObsStore.selectKeys(filter).findFirst().get();
            } catch (Exception e) {
                logger.error("Empty filter intersection", e);
                return;
            }


            setOccupancy((ObsData) obs, obsId);

            if (lastWebId != null) {
                if (isTimeRoughlyEqual()) { // Success
                    correlateWebIdWithOccupancy();
                } else { // Fail
                    logger.warn("Occupancy output not received in time");
                    clearWebId();
                }
            }
        }
    }

    private synchronized boolean isTimeRoughlyEqual() {
        //return true;
        return Math.abs(lastWebId.getSampleTime().getEpochSecond() - (long)lastOccupancy.getStartTime()) < MAX_OUTPUT_INTERVAL_MS;
    }

    private void clearWebId() {
        synchronized (this) {
            lastWebId = null;
            lastWebIdTime = 0;
            lastWebIdBlock = null;
            lastWebIdBigId = null;
        }
    }

    private void clearOccupancy() {
        synchronized (this) {
            lastOccupancy = null;
            lastOccupancyTime = 0;
            lastOccupancyBlock = null;
            lastOccupancyBigId = null;
        }
    }

    private void setWebId(ObsData webIdObs, BigId obsId) {
        synchronized (this) {
            lastWebIdBlock = webIdObs;
            lastWebId = WebIdAnalysis.toWebIdAnalysis(lastWebIdBlock.getResult());
            lastWebIdTime = System.currentTimeMillis();
            lastWebIdBigId = obsId;
        }
    }

    private void setOccupancy(ObsData occupancyObs, BigId obsId) {
        synchronized (this) {
            lastOccupancyBlock = occupancyObs;
            // Schema-aware read: returns OccupancyExtended (carrying alarmCategory)
            // for RS350 datastreams, plain Occupancy for Rapiscan/Aspect.
            DataComponent recordStructure = getOccupancyRecordStructure(occupancyObs);
            lastOccupancy = OccupancyExtended.readOccupancy(recordStructure, lastOccupancyBlock.getResult());
            lastOccupancyTime = System.currentTimeMillis();
            lastOccupancyBigId = obsId;
        }
    }

    /**
     * Look up the record description for the datastream backing this observation
     * so we can dispatch to the correct (base vs. extended) occupancy serializer.
     * Falls back to {@code null} if the datastream is unknown — callers treat
     * that as the base schema.
     */
    private DataComponent getOccupancyRecordStructure(ObsData occupancyObs) {
        try {
            var dsInfo = occupancyObsStore.getDataStreams().get(new DataStreamKey(occupancyObs.getDataStreamID()));
            return dsInfo != null ? dsInfo.getRecordStructure() : null;
        } catch (Exception e) {
            logger.warn("Failed to look up datastream for occupancy obs", e);
            return null;
        }
    }

    private void correlateWebIdWithOccupancy() {
        synchronized (this) {
            if (lastOccupancy == null || lastWebId == null) { return; }
            lastOccupancy.addWebIdObsId(obsIdEncoder.encodeID(lastWebIdBigId));
            lastWebId.setOccupancyObsId(obsIdEncoder.encodeID(lastOccupancyBigId));

            // Schema-aware write: serializes a 16-field datablock for Rapiscan/Aspect
            // and a 17-field datablock (with trailing alarmCategoryCode) for RS350,
            // matching the datastream record so the JSON serializer in PostgisObsStoreImpl
            // doesn't fall off the end of the atom array.
            DataComponent occupancyRecord = getOccupancyRecordStructure(lastOccupancyBlock);
            DataBlock newOccupancyBlock = OccupancyExtended.writeOccupancy(occupancyRecord, lastOccupancy);
            lastOccupancyBlock.getResult().setUnderlyingObject(newOccupancyBlock.getUnderlyingObject());
            lastOccupancyBlock.getResult().updateAtomCount();

            lastWebIdBlock.getResult().setUnderlyingObject(WebIdAnalysis.fromWebIdAnalysis(lastWebId).getUnderlyingObject());
            lastWebIdBlock.getResult().updateAtomCount();
            logger.info("{}", lastOccupancyBlock);
            logger.info("{}", lastWebIdBlock);
            webIdObsStore.put(lastWebIdBigId, lastWebIdBlock);
            occupancyObsStore.put(lastOccupancyBigId, lastOccupancyBlock);
            clearWebId();
            clearOccupancy();
            logger.info("Correlated WebId and Occupancy");
        }
    }

    public void stop() {
        if (startupThread != null) {
            startupThread.interrupt();
            try {
                startupThread.join();
            } catch (InterruptedException ignored) {}
            startupThread = null;
        }

        if (webIdSub != null) {
            webIdSub.cancel();
            webIdSub = null;
        }
        if (occupancySub != null) {
            occupancySub.cancel();
            occupancySub = null;
        }
    }
}
