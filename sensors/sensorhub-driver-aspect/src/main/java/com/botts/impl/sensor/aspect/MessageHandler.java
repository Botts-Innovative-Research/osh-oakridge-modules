package com.botts.impl.sensor.aspect;

import com.botts.impl.sensor.aspect.output.*;
import com.botts.impl.sensor.aspect.registers.DeviceDescriptionRegisters;
import com.botts.impl.sensor.aspect.registers.MonitorRegisters;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
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
public class MessageHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    private final TCPMasterConnection tcpMasterConnection;
    private final GammaOutput gammaOutput;
    private final NeutronOutput neutronOutput;
    private final OccupancyOutput occupancyOutput;
    private final SpeedOutput speedOutput;
    private final DailyFileOutput dailyFileOutput;
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

    public MessageHandler(TCPMasterConnection tcpMasterConnection, GammaOutput gammaOutput, NeutronOutput neutronOutput, OccupancyOutput occupancyOutput, SpeedOutput speedOutput, DailyFileOutput dailyFileOutput) {
        this.tcpMasterConnection = tcpMasterConnection;
        this.gammaOutput = gammaOutput;
        this.neutronOutput = neutronOutput;
        this.occupancyOutput = occupancyOutput;
        this.speedOutput = speedOutput;
        this.dailyFileOutput = dailyFileOutput;

        occupancyGammaBatch = new LinkedList<>();
        occupancyNeutronBatch = new LinkedList<>();

        thread = new Thread(this, "Message Handler");
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        thread.interrupt();
    }

    @Override
    public void run() {
        try {
            DeviceDescriptionRegisters deviceDescriptionRegisters = new DeviceDescriptionRegisters(tcpMasterConnection);
            deviceDescriptionRegisters.readRegisters(1);


            MonitorRegisters monitorRegisters = new MonitorRegisters(tcpMasterConnection, deviceDescriptionRegisters.getMonitorRegistersBaseAddress(), deviceDescriptionRegisters.getMonitorRegistersNumberOfRegisters());
            while (!Thread.currentThread().isInterrupted()) {
                monitorRegisters.readRegisters(1);

                double timestamp = System.currentTimeMillis() / 1000d;
                dailyFileOutput.getDailyFile(monitorRegisters);
                dailyFileOutput.onNewMessage();

                gammaOutput.setData(monitorRegisters, timestamp);
                neutronOutput.setData(monitorRegisters, timestamp);
                speedOutput.setData(monitorRegisters, timestamp);


                if (checkOccupancyRecord(monitorRegisters, timestamp)) {
                    occupancyOutput.setData(monitorRegisters, timestamp, startTime, endTime, gammaAlarm, neutronAlarm, maxGamma, maxNeutron);
                }
                System.out.println("monitor registers: "+ monitorRegisters);
                sleep(500);
            }
        } catch (Exception e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            log.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter);
        }
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
            System.out.println("occupancy started");
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
        System.out.println(occupancyGammaBatch);
        occupancyNeutronBatch.addLast(monitorRegisters.getNeutronChannelCount());
        System.out.println(occupancyNeutronBatch);

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