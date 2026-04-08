package com.botts.impl.sensor.rs350;

import com.botts.api.service.bucket.IBucketService;
import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.sensor.rs350.messages.RS350Message;
//import com.botts.impl.service.oscar.Constants;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.sensorhub.impl.utils.rad.webid.WebIdAnalyzer;
import org.sensorhub.impl.utils.rad.webid.WebIdRequestContext;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import com.botts.impl.utils.n42.RadInstrumentDataType;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    final LinkedList<String> messageQueue = new LinkedList<>();
    private final AbstractSensorModule<?> sensorModule;
    WebIdAnalyzer webIdAnalyzer;
    boolean hasAlarm = false;
    String laneUID;
    IBucketStore bucketStore;

    RADHelper radHelper = new RADHelper();

    private final InputStream msgIn;

    private final String messageDelimiter;

    private long timeSinceLastMessage;


    public interface StatusListener {
        void onNewMessage(RS350Message message);
    }

    public interface BackgroundListener {
        void onNewMessage(RS350Message message);
    }

    public interface ForegroundListener {
        void onNewMessage(RS350Message message);
    }

    public interface AlarmListener {
        void onNewMessage(RS350Message message);
    }

    private final ArrayList<StatusListener> statusListeners = new ArrayList<>();
    private final ArrayList<BackgroundListener> backgroundListeners = new ArrayList<>();
    private final ArrayList<ForegroundListener> foregroundListeners = new ArrayList<>();
    private final ArrayList<AlarmListener> alarmListeners = new ArrayList<>();
    //private final ArrayList<N42Output> n42Listeners = new ArrayList<>();

    private final AtomicBoolean isProcessing = new AtomicBoolean(true);

    // The RS350 occupancy is created by the Rs350OutputToOccupancy process module
    // ~10 seconds after an alarm fires (see OCCUPANCY_INTERVAL_MS in that class).
    // We defer WebID/N42 publishing slightly longer than that so the lookup of the
    // freshly-created occupancy obs id succeeds and N42/WebID observations get
    // tagged with the right occupancyObsId.
    private static final long WEBID_PUBLISH_DELAY_SECONDS = 12L;
    private final ScheduledExecutorService webIdScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rs350-webid-deferred");
        t.setDaemon(true);
        return t;
    });

    private final Thread messageReader = new Thread(new Runnable() {
        @Override
        public void run() {

            boolean continueProcessing = true;

            try {

                ArrayList<Character> buffer = new ArrayList<>();
                timeSinceLastMessage = 0;

                while (continueProcessing) {

                    int character = msgIn.read();

                    // Detected STX
                    if (character == 0x02) {
                        character = msgIn.read();
                        // Detect ETX
                        while (character != 0x03 && character != -1) {
                            buffer.add((char)character);
                            character = msgIn.read();
                            if (character == -1){
                                System.out.println("did not read complete message");
                            }
                        }
                        StringBuilder sb = new StringBuilder(buffer.size());

                        for (char c : buffer) {

                            sb.append(c);
                        }

                        String n42Message = sb.toString().replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();

                        synchronized (messageQueue) {

                            messageQueue.add(n42Message);

                            messageQueue.notifyAll();
                        }
                        buffer.clear();
                    }

                    synchronized (isProcessing) {

                        continueProcessing = isProcessing.get();
                    }
                }
            } catch (IOException exception) {

            }
        }
    });

    private final Thread messageNotifier = new Thread(() -> {

        boolean continueProcessing = true;

        while (continueProcessing) {

            final String currentMessage;

            synchronized (messageQueue) {

                try {

                    while (messageQueue.isEmpty()) {

                        messageQueue.wait();

                    }

                    currentMessage = messageQueue.removeFirst();

                    //n42Listeners.forEach(listener -> listener.onNewMessage(currentMessage));

                } catch (InterruptedException e) {

                    throw new RuntimeException(e);
                }
            }

            if (currentMessage != null && !currentMessage.isEmpty()) {

                try {
                    RadInstrumentDataType radInstrumentDataType = radHelper.getRadInstrumentData(currentMessage);
                    RS350Message message = new RS350Message(radInstrumentDataType);

                    // Add webid handler call here

                    if (message.getRs350InstrumentCharacteristics() != null && message.getRs350Item() != null && message.getRs350LinEnergyCalibration() != null && message.getRs350CmpEnergyCalibration() != null) {
                        statusListeners.forEach(listener -> listener.onNewMessage(message));
                    }

                    if (message.getRs350BackgroundMeasurement() != null) {
                        backgroundListeners.forEach(listener -> listener.onNewMessage(message));
                    }

                    if (message.getRs350ForegroundMeasurement() != null) {
                        foregroundListeners.forEach(listener -> listener.onNewMessage(message));
                    }

                    if (message.getRs350RadAlarm() != null && message.getRs350DerivedData() != null) {
                        if (!hasAlarm) {
                            hasAlarm = true;
                            publishWebIdRequest(currentMessage);
                        }
                        alarmListeners.forEach(listener -> listener.onNewMessage(message));
                    } else {
                        hasAlarm = false;
                    }
                }
                catch (Exception e){
                    log.error("Error reading message: " + e, e);
                }
            }

            synchronized (isProcessing) {

                continueProcessing = isProcessing.get();
            }
        }
    });

    public MessageHandler(WebIdAnalyzer webIdAnalyzer, InputStream msgIn, String messageDelimiter, AbstractSensorModule<?> parentSensor) {
        this.sensorModule = parentSensor;
        this.webIdAnalyzer = webIdAnalyzer;
        this.msgIn = msgIn;
        this.messageDelimiter = messageDelimiter;
        laneUID = parentSensor.getParentSystemUID();
        var bucketService = parentSensor.getParentHub().getModuleRegistry().getModuleByType(IBucketService.class);
        bucketStore = bucketService.getBucketStore();

        this.messageReader.start();
        this.messageNotifier.start();
    }

    public void addStatusListener(StatusListener listener) {
        statusListeners.add(listener);
    }

    public void addBackgroundListener(BackgroundListener listener) {
        backgroundListeners.add(listener);
    }

    public void addForegroundListener(ForegroundListener listener) {
        foregroundListeners.add(listener);
    }

    public void addAlarmListener(AlarmListener listener) {
        alarmListeners.add(listener);
    }

    public void stopProcessing() {

        synchronized (isProcessing) {

            isProcessing.set(false);
        }

        webIdScheduler.shutdown();
        try {
            if (!webIdScheduler.awaitTermination(WEBID_PUBLISH_DELAY_SECONDS + 5, TimeUnit.SECONDS)) {
                webIdScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            webIdScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public long getTimeSinceLastMessage() {
        return timeSinceLastMessage;
    }

    private void publishWebIdRequest(String n42) {
        if (webIdAnalyzer == null) {
            return;
        }

        if (laneUID == null) {
            log.warn("No lane UID found for sensor module {}, using sensor UID instead", sensorModule.getName());
            laneUID = sensorModule.getUniqueIdentifier();
        }

        // The RS350 occupancy is built by Rs350OutputToOccupancy when the alarm
        // arrives but isn't published until ~OCCUPANCY_INTERVAL_MS (10s) later.
        // Defer the WebID publish so the lookup can find the just-published
        // occupancy and tag the N42/WebID observations with its obs id.
        try {
            webIdScheduler.schedule(() -> {
                try {
                    doPublishWebIdRequest(n42);
                } catch (Exception e) {
                    log.error("Error publishing deferred WebID request", e);
                }
            }, WEBID_PUBLISH_DELAY_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to schedule deferred WebID request", e);
        }
    }

    private void doPublishWebIdRequest(String n42) {
        var requestBuilder = WebIdRequestContext.builder();
        String fileName = "rs350_" + laneUID + "_" + System.currentTimeMillis() + ".n42";

        // RS350 occupancies are produced by the Rs350OutputToOccupancy process
        // module (a sub-system of the lane), not by the RS350 sensor itself.
        // Look up the most recently published occupancy in any subsystem of the
        // lane so the WebID/N42 records can be correlated to it in the viewer.
        String occupancyObsId = lookupLatestOccupancyObsId();
        if (occupancyObsId == null) {
            log.warn("No RS350 occupancy obs id found for lane {} when publishing WebID request; the N42 will not be linked to an occupancy", laneUID);
        } else {
            log.debug("Linking RS350 WebID request to occupancy obs id {}", occupancyObsId);
        }

        requestBuilder.foregroundFile(fileName, n42.getBytes(StandardCharsets.UTF_8))
                .webIdEnabled(true)
                //.bucketName(Constants.REPORT_BUCKET) TODO Move constants to a module w/o dependencies?
                .bucketName("reports")
                .objectKey(fileName)
                .laneUid(laneUID)
                .synthesizeBackground(true)
                .occupancyObsId(occupancyObsId)
                .multipartRequest(false);

        var analysis = this.webIdAnalyzer.processWebIdRequest(requestBuilder.build());

        // Store analysis JSON in bucket
        if (analysis != null) {
            var analysisJson = analysis.toString();
            Map<String, String> jsonMetadata = new HashMap<>();
            jsonMetadata.put("Content-Type", "application/json");
            try {
                bucketStore.createObject("reports", new ByteArrayInputStream(analysisJson.getBytes()), jsonMetadata);
            } catch (DataStoreException e) {
                log.error("Failed to store WebId analysis JSON in bucket", e);
            }
        }
    }

    /**
     * UID prefix for the Rs350OccupancyProcessModule system. Matches
     * {@code OSHProcessInfo.URI_PREFIX + "rs350-occupancy:"} as assembled in
     * Rs350OccupancyProcessModule.doInit(). Hardcoded rather than imported to
     * avoid adding a compile-time dependency from the sensor driver onto the
     * process module package.
     */
    private static final String RS350_OCCUPANCY_PROCESS_UID_PREFIX = "urn:osh:process:rs350-occupancy:";

    /**
     * Returns the encoded id of the most recent occupancy observation produced
     * by a Rs350OutputToOccupancy process module that is a subsystem of this
     * lane, or null if none can be found.
     *
     * It is critical that we only match occupancies from the RS350 occupancy
     * process module: other sensors in the same lane (e.g. Rapiscan) also
     * publish an "occupancy" output, and a blanket query by output name would
     * incorrectly tag RS350 N42/WebID observations with a vehicle-portal
     * occupancy from a completely unrelated system.
     */
    private String lookupLatestOccupancyObsId() {
        if (laneUID == null)
            return null;
        try {
            var hub = sensorModule.getParentHub();
            var db = hub.getDatabaseRegistry().getFederatedDatabase();

            // Find any RS350 occupancy process sub-system of this lane. The
            // process module registers its output under a system whose UID
            // starts with RS350_OCCUPANCY_PROCESS_UID_PREFIX.
            var laneMemberFilter = new SystemFilter.Builder()
                    .withUniqueIDs(laneUID)
                    .includeMembers(true)
                    .build();

            String[] rs350ProcessUids = db.getSystemDescStore()
                    .select(laneMemberFilter)
                    .map(sys -> sys.getUniqueIdentifier())
                    .filter(uid -> uid != null && uid.startsWith(RS350_OCCUPANCY_PROCESS_UID_PREFIX))
                    .toArray(String[]::new);

            if (rs350ProcessUids.length == 0) {
                log.debug("No RS350 occupancy process system (prefix {}) found under lane {}",
                        RS350_OCCUPANCY_PROCESS_UID_PREFIX, laneUID);
                return null;
            }

            var rs350SysFilter = new SystemFilter.Builder()
                    .withUniqueIDs(rs350ProcessUids)
                    .build();

            var keys = db.getObservationStore().selectKeys(new ObsFilter.Builder()
                    .withLatestResult()
                    .withSystems(rs350SysFilter)
                    .withDataStreams().withOutputNames(OccupancyOutput.NAME).done()
                    .withLimit(1)
                    .build()).toList();

            if (keys.isEmpty()) {
                log.debug("No RS350 occupancy observations yet under lane {}; rs350 process systems scanned: {}",
                        laneUID, String.join(",", rs350ProcessUids));
                return null;
            }

            BigId obsId = keys.get(0);
            return hub.getIdEncoders().getObsIdEncoder().encodeID(obsId);
        } catch (Exception e) {
            log.warn("Failed to look up latest RS350 occupancy obs id for lane {}", laneUID, e);
            return null;
        }
    }

}
