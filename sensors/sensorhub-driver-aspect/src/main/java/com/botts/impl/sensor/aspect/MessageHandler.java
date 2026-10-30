package com.botts.impl.sensor.aspect;

import com.botts.impl.sensor.aspect.output.DailyFileOutput;
import com.botts.impl.sensor.aspect.registers.DeviceDescriptionRegisters;
import com.botts.impl.sensor.aspect.registers.MonitorRegisters;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileJournalConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

/**
 * Handles the incoming messages from the sensor
 *
 * @author Michael Elmore
 * @since December 2023
 */
public class MessageHandler {
    private final AspectSensor parentSensor;
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    private static final long STOP_TIMEOUT_MILLIS = 2_000L;
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    Thread thread;

    int occupancyCount = -1;
    double startTime = 0;
    double endTime = 0;
    boolean gammaAlarm = false;
    boolean neutronAlarm = false;

    LinkedList<Integer> occupancyGammaBatch ;
    LinkedList<Integer> occupancyNeutronBatch;
    int maxNeutron = 0;
    int maxGamma = 0;
    int deviceAddress = -1;

    private static final int BACKGROUND_INTERVAL_MS = 5000;
    private static final int SCAN_INTERVAL_MS = 200;
    private long timeSinceLastMessage;
    private long timeOfLastMessage;
    private long timer = 0;

    public long getTimeSinceLastMessage() {
        return timeSinceLastMessage;
    }

    /** Testable initialization path that does not start the device reader thread. */
    MessageHandler(AspectSensor parentSensor) {
        this.parentSensor = parentSensor;
        occupancyGammaBatch = new LinkedList<>();
        occupancyNeutronBatch = new LinkedList<>();
    }

    public MessageHandler(AspectSensor parentSensor, int deviceAddress) {
        this(parentSensor);
        this.deviceAddress = deviceAddress;



//        thread = new Thread(this, "Message Handler");

        thread = new Thread(()->{

            try {
                DeviceDescriptionRegisters deviceDescriptionRegisters = new DeviceDescriptionRegisters(parentSensor.commProviderModule.getConnection());
                deviceDescriptionRegisters.readRegisters(deviceAddress);
                timeOfLastMessage = System.currentTimeMillis();
                timeSinceLastMessage = 0;
                timer = 0;

                MonitorRegisters monitorRegisters = new MonitorRegisters(parentSensor.commProviderModule.getConnection(), 100, 21);
                while (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                    timer += timeSinceLastMessage;
                    timeSinceLastMessage = System.currentTimeMillis() - timeOfLastMessage;
                    timeOfLastMessage = System.currentTimeMillis();

                    monitorRegisters.readRegisters(deviceAddress);

                    SampleClock sampleClock = sampleClock(Instant.now());
                    Instant sampleTime = sampleClock.observationTime();
                    double timestamp = sampleClock.epochSeconds();
                    int previousOccupancyCount = occupancyCount;
                    boolean occupancyRecordReady = checkOccupancyRecord(
                            monitorRegisters, timestamp);
                    boolean occupancyStartedThisSample = previousOccupancyCount >= 0
                            && occupancyCount != previousOccupancyCount;

                    if (checkScan(monitorRegisters)) {
                        if (shouldPublishDailyFile(true, timer, occupancyStartedThisSample)) {
                            timer = 0;

                            publishDailyFile(monitorRegisters, sampleTime);

                            parentSensor.gammaOutput.setData(monitorRegisters, timestamp);
                            parentSensor.neutronOutput.setData(monitorRegisters, timestamp);
                            parentSensor.speedOutput.setData(monitorRegisters, timestamp);
                        }

                    } else {
                        if (shouldPublishDailyFile(false, timer, occupancyStartedThisSample)) {
                            timer = 0;

                            publishDailyFile(monitorRegisters, sampleTime);

                            parentSensor.gammaOutput.setData(monitorRegisters, timestamp);
                            parentSensor.neutronOutput.setData(monitorRegisters, timestamp);
                        }
                    }

                    if (occupancyRecordReady) {
                        Occupancy occupancy = new Occupancy.Builder()
                                .occupancyCount(monitorRegisters.getObjectCounter())
                                .startTime(startTime)
                                .endTime(endTime)
                                .samplingTime(timestamp)
                                .neutronBackground(monitorRegisters.getNeutronChannelBackground())
                                .gammaAlarm(gammaAlarm)
                                .neutronAlarm(neutronAlarm)
                                .maxGammaCount(maxGamma)
                                .maxNeutronCount(maxNeutron)
                                .build();

                        parentSensor.occupancyOutput.setData(occupancy);
                    }

                }
            } catch (Exception e) {
                if (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                    StringWriter stringWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(stringWriter));
                    log.error("Error in worker thread: {} due to exception: {}",
                            Thread.currentThread().getName(), stringWriter);
                }
            }

        }, "Aspect-Reader-" + parentSensor.getUniqueIdentifier());
        thread.setDaemon(true);
        thread.start();
    }

    /** Builds, journals, and only then publishes one generated Aspect daily-file line. */
    private void publishDailyFile(MonitorRegisters monitorRegisters, Instant timestamp) throws IOException {
        publishDailyFile(monitorRegisters, timestamp, parentSensor.getDailyFileJournal(),
                parentSensor.dailyFileOutput);
    }

    /**
     * Synchronous journal/publication boundary kept package-visible so ordering and
     * fail-closed policy can be verified without starting a Modbus reader thread.
     */
    static void publishDailyFile(MonitorRegisters monitorRegisters, Instant timestamp,
                                 DailyFileJournalConnection journal,
                                 DailyFileOutput dailyFileOutput) throws IOException {
        String line = DailyFileOutput.buildDailyFileLine(monitorRegisters);
        if (journal != null) {
            try {
                journal.append(line, timestamp);
            } catch (IOException e) {
                log.error("Unable to append generated Aspect line to daily-file journal", e);
                if (journal.isRequired())
                    throw e;
            }
        }
        if (journal == null || journal.shouldPublishLegacyObservation())
            dailyFileOutput.onNewMessage(line, timestamp);
    }

    /** One captured clock value drives both the daily observation and occupancy start time. */
    static SampleClock sampleClock(Instant observationTime) {
        // Aspect and the other supported RPMs expose millisecond timestamps. Quantizing here makes
        // the double occupancy value and the Instant phenomenon time an exact correlation pair.
        Instant millisecondTime = Instant.ofEpochMilli(observationTime.toEpochMilli());
        return new SampleClock(millisecondTime, millisecondTime.toEpochMilli() / 1_000.0);
    }

    record SampleClock(Instant observationTime, double epochSeconds) {}

    /** Occupancy transitions bypass cadence throttling so correlation never uses a later sample. */
    static boolean shouldPublishDailyFile(boolean scan, long elapsedMillis,
                                          boolean occupancyStartedThisSample) {
        return occupancyStartedThisSample
                || elapsedMillis >= (scan ? SCAN_INTERVAL_MS : BACKGROUND_INTERVAL_MS);
    }

    public void requestStop() {
        stopRequested.set(true);
    }

    public void awaitStop() throws IOException {
        Thread current = thread;
        if (current == null)
            return;
        long deadlineNanos = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(STOP_TIMEOUT_MILLIS);
        try {
            // Give a source read or durable append a chance to finish after the
            // provider is closed. Interrupt only as the bounded fallback.
            current.join(STOP_TIMEOUT_MILLIS / 2);
            if (current.isAlive())
                current.interrupt();
            long remaining = deadlineNanos - System.nanoTime();
            if (current.isAlive() && remaining > 0)
                current.join(Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remaining)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while stopping Aspect reader", e);
        }
        if (current.isAlive())
            throw new IOException("Aspect reader did not stop within "
                    + STOP_TIMEOUT_MILLIS + " ms");
        thread = null;
    }

    public void stop() throws IOException {
        requestStop();
        awaitStop();
    }

    private boolean checkScan(MonitorRegisters monitorRegisters) {
        return monitorRegisters.isOccupied() || monitorRegisters.isGammaAlarm() || monitorRegisters.isNeutronAlarm();
    }

    /**
     * Checks whether to write the occupancy record and sets the start time, end time, and alarm flags
     *
     * @param monitorRegisters the monitor registers
     * @param timestamp        the timestamp
     * @return true if the record should be written, false otherwise
     */
    boolean checkOccupancyRecord(MonitorRegisters monitorRegisters, double timestamp) {
        // Initialize the occupancy count
        if (occupancyCount == -1) {
            occupancyCount = monitorRegisters.getObjectCounter();
            return false;
        }

        // If the occupancy count has changed, then we need to set the start time
        if (occupancyCount != monitorRegisters.getObjectCounter()) {
            startTime = timestamp;
            endTime = 0;
            occupancyCount = monitorRegisters.getObjectCounter();
            gammaAlarm = false;
            neutronAlarm = false;
            return false;
        }

        // If both start time and end time are set, the record has already been written
        if (startTime != 0 && endTime != 0){
            return false;
        }
        if (startTime == 0 && endTime == 0) {
            return false;
        }

        // If an alarm occurs during this time, set the alarm flag
        if (monitorRegisters.isGammaAlarm()){
            gammaAlarm = true;
        }
        if (monitorRegisters.isNeutronAlarm()){
            neutronAlarm = true;
        }

        occupancyGammaBatch.addLast(monitorRegisters.getGammaChannelCount());
        occupancyNeutronBatch.addLast(monitorRegisters.getNeutronChannelCount());

        // If the sensor is still occupied, wait for it to become unoccupied
        if (monitorRegisters.isOccupied()) {
            return false;
        }

        // If the sensor is now unoccupied, set the end time
        endTime = timestamp;
        System.out.println("occupancy ended");


        maxGamma = calcMax(occupancyGammaBatch);
        maxNeutron = calcMax(occupancyNeutronBatch);

        occupancyNeutronBatch.clear();
        occupancyGammaBatch.clear();
        return true;
    }

    public int calcMax(LinkedList<Integer> occBatch){

        int temp = 0;
        int position = 0;
        for(int i = 0; i < occBatch.size(); i++){
            if(occBatch.get(i) > temp){
                temp = occBatch.get(i);
                position = i;
            }
        }
        return occBatch.get(position);
    }
}
