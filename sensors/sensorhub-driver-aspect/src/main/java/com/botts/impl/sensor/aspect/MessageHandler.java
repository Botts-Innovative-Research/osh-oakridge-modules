package com.botts.impl.sensor.aspect;

import com.botts.impl.sensor.aspect.output.DailyFileOutput;
import com.botts.impl.sensor.aspect.registers.DeviceDescriptionRegisters;
import com.botts.impl.sensor.aspect.registers.MonitorRegisters;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileAppender;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;

import static java.lang.Thread.sleep;

/**
 * Handles the incoming messages from the sensor
 *
 * @author Michael Elmore
 * @since December 2023
 */
public class MessageHandler {
    private final AspectSensor parentSensor;
    private final DailyFileAppender dailyFile;
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
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

    /**
     * @param parentSensor  owning sensor module
     * @param deviceAddress Modbus unit address
     * @param dailyFile     appender that records every daily file line before it is published (may be null in tests)
     */
    public MessageHandler(AspectSensor parentSensor, int deviceAddress, DailyFileAppender dailyFile) {
        this.parentSensor = parentSensor;
        this.deviceAddress = deviceAddress;
        this.dailyFile = dailyFile;

        occupancyGammaBatch = new LinkedList<>();
        occupancyNeutronBatch = new LinkedList<>();



        thread = new Thread(()->{

            try {
                DeviceDescriptionRegisters deviceDescriptionRegisters = new DeviceDescriptionRegisters(parentSensor.commProviderModule.getConnection());
                deviceDescriptionRegisters.readRegisters(deviceAddress);
                timeOfLastMessage = System.currentTimeMillis();
                timeSinceLastMessage = 0;
                timer = 0;

                MonitorRegisters monitorRegisters = new MonitorRegisters(parentSensor.commProviderModule.getConnection(), 100, 21);
                while (!Thread.currentThread().isInterrupted()) {
                    timer += timeSinceLastMessage;
                    timeSinceLastMessage = System.currentTimeMillis() - timeOfLastMessage;
                    timeOfLastMessage = System.currentTimeMillis();

                    monitorRegisters.readRegisters(deviceAddress);

                    double timestamp = System.currentTimeMillis() / 1000d;

                    try {
                        if (checkScan(monitorRegisters)) {
                            if (timer >= SCAN_INTERVAL_MS) {
                                timer = 0;

                                publishDailyFile(monitorRegisters);

                                parentSensor.gammaOutput.setData(monitorRegisters, timestamp);
                                parentSensor.neutronOutput.setData(monitorRegisters, timestamp);
                                parentSensor.speedOutput.setData(monitorRegisters, timestamp);
                            }

                        } else {
                            if (timer >= BACKGROUND_INTERVAL_MS) {
                                timer = 0;

                                publishDailyFile(monitorRegisters);

                                parentSensor.gammaOutput.setData(monitorRegisters, timestamp);
                                parentSensor.neutronOutput.setData(monitorRegisters, timestamp);
                            }
                        }

                        if (checkOccupancyRecord(monitorRegisters, timestamp)) {
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
                    } catch (Exception e) {
                        // A failure while publishing one poll must not kill the polling loop
                        log.warn("Error processing Aspect register poll: {}", e.toString());
                    }

                }
            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                log.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter);
            }

        }, "Aspect-Reader-" + parentSensor.getUniqueIdentifier());
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Builds the daily file line once, appends it to the daily file first, then publishes it.
     */
    private void publishDailyFile(MonitorRegisters monitorRegisters) {
        String line = DailyFileOutput.buildDailyFileLine(monitorRegisters);
        if (dailyFile != null)
            dailyFile.append(line);
        parentSensor.dailyFileOutput.onNewMessage(line);
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
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
    private boolean checkOccupancyRecord(MonitorRegisters monitorRegisters, double timestamp) {
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