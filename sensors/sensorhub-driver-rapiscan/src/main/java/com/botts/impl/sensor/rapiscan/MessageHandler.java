package com.botts.impl.sensor.rapiscan;


import com.opencsv.CSVReader;
import org.sensorhub.impl.utils.rad.dailyfile.DailyFileAppender;
import org.sensorhub.impl.utils.rad.model.Occupancy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler {

    private final RapiscanSensor parentSensor;
    private final DailyFileAppender dailyFile;

    Boolean currentOccupancy = false;
    Boolean isGammaAlarm = false;
    Boolean isNeutronAlarm = false;
    long occupancyStartTime;
    long occupancyEndTime;

    final static String ALARM = "Alarm";
    final static String BACKGROUND = "Background";
    final static String SCAN = "Scan";
    final static String FAULT_GH = "Fault - Gamma High";
    final static String FAULT_GL = "Fault - Gamma Low";
    final static String FAULT_NH = "Fault - Neutron High";

    String[] setupGamma1;
    String[] setupGamma2;
    String[] setupNeutron1;

    LinkedList<String[]> gammaScanRunningSumBatch;

    LinkedList<Integer> occupancyGammaBatch;
    LinkedList<Integer> occupancyNeutronBatch;

    int neutronMax;
    int gammaMax;

    private final AtomicBoolean isProcessing = new AtomicBoolean(true);
    private volatile long timeSinceLastMessage;

    private final InputStream msgIn;
    private final Thread readerThread;

    public long getTimeSinceLastMessage() {
        long now = System.currentTimeMillis();
        return (now - timeSinceLastMessage);
    }

    /**
     * @param msgIn        stream of CRLF/LF delimited RPM messages
     * @param parentSensor owning sensor module
     * @param dailyFile    appender that records every raw message before it is parsed (may be null in tests)
     */
    public MessageHandler(InputStream msgIn, RapiscanSensor parentSensor, DailyFileAppender dailyFile) {
        this.parentSensor = parentSensor;
        this.msgIn = msgIn;
        this.dailyFile = dailyFile;

        gammaScanRunningSumBatch = new LinkedList<>();
        occupancyGammaBatch = new LinkedList<>();
        occupancyNeutronBatch = new LinkedList<>();

        timeSinceLastMessage = System.currentTimeMillis();

        readerThread = new Thread(this::readLoop, "Rapiscan-Reader-" + parentSensor.getUniqueIdentifier());
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(msgIn))) {
            String msgLine;
            while (isProcessing.get() && (msgLine = bufferedReader.readLine()) != null) {
                timeSinceLastMessage = System.currentTimeMillis();
                processLine(msgLine);
            }
            if (isProcessing.get())
                parentSensor.getLogger().info("End of RPM message stream reached");
        } catch (IOException e) {
            if (isProcessing.get())
                parentSensor.getLogger().error("Error reading RPM message stream: {}", e.getMessage());
        } catch (Exception e) {
            parentSensor.getLogger().error("Rapiscan message reader terminated unexpectedly", e);
        } finally {
            parentSensor.getLogger().debug("Message reader exiting for sensor {}", parentSensor.getUniqueIdentifier());
        }
    }

    /**
     * Handles one raw message line. The line is written to the daily file first, exactly as received,
     * then published on the dailyFile output, then parsed. A failure in any later step only affects
     * that line; the reader keeps going.
     */
    void processLine(String msgLine) {
        if (msgLine.isBlank())
            return;

        // 1. Daily file: raw message, before anything can fail
        if (dailyFile != null)
            dailyFile.append(msgLine);

        // 2. Live dailyFile output (lane system state tracking)
        try {
            parentSensor.getDailyFileOutput().onNewMessage(msgLine);
        } catch (Exception e) {
            parentSensor.getLogger().warn("Failed to publish dailyFile output for line '{}': {}", msgLine, e.toString());
        }

        // 3. Parse and dispatch
        try {
            List<String[]> csvList;
            try (CSVReader reader = new CSVReader(new StringReader(msgLine))) {
                csvList = reader.readAll();
            }
            if (!csvList.isEmpty() && csvList.get(0).length > 0 && !csvList.get(0)[0].isEmpty())
                onNewMainChar(csvList.get(0)[0], csvList.get(0));
        } catch (Exception e) {
            parentSensor.getLogger().warn("Failed to process RPM message '{}': {}", msgLine, e.toString());
        }
    }

    public synchronized void stop() {
        if (!isProcessing.getAndSet(false))
            return;

        parentSensor.getLogger().debug("Stopping MessageHandler for sensor {}", parentSensor.getUniqueIdentifier());

        // Closing the stream unblocks readLine()
        try {
            msgIn.close();
        } catch (IOException e) {
            parentSensor.getLogger().debug("Error closing RPM message stream: {}", e.getMessage());
        }

        readerThread.interrupt();
        try {
            readerThread.join(2000);
            if (readerThread.isAlive())
                parentSensor.getLogger().warn("Message reader did not stop within timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int[] getGammaForegroundCountsPerSecond() {
        int size = gammaScanRunningSumBatch.size();

        if (size != 5) {
            return null;
        }

        int[] gammaForegroundCountsPerSecond = new int[4];

        for (String[] line : gammaScanRunningSumBatch) {
            for (int i = 0; i < 4; i++) {
                gammaForegroundCountsPerSecond[i] += Integer.parseInt(line[i + 1]);
            }
        }

        return gammaForegroundCountsPerSecond;
    }

    void onNewMainChar(String mainChar, String[] csvLine) {

        // Add scan data for EML service. Background and other data gives EML context, so we must show EML everything until end of occupancy
        if (parentSensor.getConfiguration().emlConfig.emlEnabled && !mainChar.equals("GB") && !mainChar.equals("NB")) {
            parentSensor.getEmlService().addScanDataLine(csvLine);
        }

        switch (mainChar) {
            // ------------------- NOT OCCUPIED
            case "GB" -> {
                parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), BACKGROUND, null);

                // Send latest Gamma Background to threshold calculator and EML service
                parentSensor.getGammaThresholdOutput().onNewBackground(csvLine);

                if (parentSensor.getConfiguration().emlConfig.emlEnabled)
                    parentSensor.getEmlService().setLatestGammaBackground(csvLine);
                break;
            }
            case "GH" -> {
                parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), FAULT_GH, null);
            }
            case "GL" -> {
                parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), FAULT_GL, null);
            }
            case "NB" -> {
                parentSensor.getNeutronOutput().onNewMessage(csvLine, System.currentTimeMillis(), BACKGROUND);

                if (parentSensor.getConfiguration().emlConfig.emlEnabled)
                    parentSensor.getEmlService().setLatestNeutronBackground(csvLine);
            }
            case "NH" -> {
                parentSensor.getNeutronOutput().onNewMessage(csvLine, System.currentTimeMillis(), FAULT_NH);
            }

            // --------------- OCCUPIED
            case "GA" -> {
                gammaScanRunningSumBatch.addLast(csvLine);
                if (!currentOccupancy) {
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }

                int gamma = Integer.parseInt(csvLine[1]) +
                        Integer.parseInt(csvLine[2]) +
                        Integer.parseInt(csvLine[3]) +
                        Integer.parseInt(csvLine[4]);
                occupancyGammaBatch.addLast(gamma);

                var counts = getGammaForegroundCountsPerSecond();
                if (counts != null) {
                    parentSensor.getGammaThresholdOutput().onNewForeground(counts);
                    parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), ALARM, counts);
                }

                isGammaAlarm = true;
            }

            case "GS" -> {
                gammaScanRunningSumBatch.addLast(csvLine);

                //usually the foreground value will switch to "GA" in an alarm state
                if (!currentOccupancy) {
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                int gamma = Integer.parseInt(csvLine[1]) +
                        Integer.parseInt(csvLine[2]) +
                        Integer.parseInt(csvLine[3]) +
                        Integer.parseInt(csvLine[4]);
                occupancyGammaBatch.addLast(gamma);

                var counts = getGammaForegroundCountsPerSecond();
                if (counts != null) {
                    parentSensor.getGammaThresholdOutput().onNewForeground(counts);
                    parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), SCAN, counts);
                }
            }

            case "NA" -> {
                if (!currentOccupancy) {
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;

                } else {
                    int neutron = Integer.parseInt(csvLine[1]) +
                            Integer.parseInt(csvLine[2]) +
                            Integer.parseInt(csvLine[3]) +
                            Integer.parseInt(csvLine[4]);
                    occupancyNeutronBatch.addLast(neutron);
                }
                parentSensor.getNeutronOutput().onNewMessage(csvLine, System.currentTimeMillis(), ALARM);

                isNeutronAlarm = true;
            }

            case "NS" -> {
                if (!currentOccupancy) {
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                } else {
                    int neutron = Integer.parseInt(csvLine[1]) +
                            Integer.parseInt(csvLine[2]) +
                            Integer.parseInt(csvLine[3]) +
                            Integer.parseInt(csvLine[4]);
                    occupancyNeutronBatch.addLast(neutron);
                }
                parentSensor.getNeutronOutput().onNewMessage(csvLine, System.currentTimeMillis(), SCAN);

            }

            case "GX" -> {
                occupancyEndTime = System.currentTimeMillis();
                gammaMax = getGammaMax(occupancyGammaBatch);
                neutronMax = occupancyNeutronBatch.isEmpty() ? 0 : Collections.max(occupancyNeutronBatch);

                Occupancy occupancy = new Occupancy.Builder()
                        .occupancyCount(Integer.parseInt(csvLine[1]))
                        .startTime(occupancyStartTime/1000d)
                        .endTime(occupancyEndTime/1000d)
                        .samplingTime(System.currentTimeMillis()/1000d)
                        .neutronBackground(Double.parseDouble(csvLine[2])/1000)
                        .gammaAlarm(isGammaAlarm)
                        .neutronAlarm(isNeutronAlarm)
                        .maxGammaCount(gammaMax)
                        .maxNeutronCount(neutronMax)
                        .build();

                parentSensor.getOccupancyOutput().setData(occupancy);
                currentOccupancy = false;
                isGammaAlarm = false;
                isNeutronAlarm = false;

                gammaScanRunningSumBatch.clear();
                //clear max batches for next occupancy
                occupancyNeutronBatch.clear();
                occupancyGammaBatch.clear();

                if (parentSensor.getConfiguration().emlConfig.emlEnabled) {
                    // TODO: Make better determination of whether occupancy ended
                    var results = parentSensor.getEmlService().processCurrentOccupancy();
                    parentSensor.getEmlScanContextualOutput().handleScanContextualMessage(results);
                    parentSensor.getEmlContextualOutput().handleContextualMessage(results);
                    parentSensor.getEmlAnalysisOutput().handleAnalysisMessage(results);
                }
            }

            // -------------------- OTHER STATE
            case "TC" -> parentSensor.getTamperOutput().onNewMessage(false);

            case "TT" -> parentSensor.getTamperOutput().onNewMessage(true);

            case "SP" -> parentSensor.getSpeedOutput().onNewMessage(csvLine);

            case "SG1" -> setupGamma1 = csvLine;

            case "SG2"-> setupGamma2 = csvLine;

            case "SG3" -> {
                parentSensor.getSetupGammaOutput().onNewMessage(setupGamma1, setupGamma2, csvLine);
                setupGamma1 = new String[]{""};
                setupGamma2 = new String[]{""};
            }

            case "SN1" -> setupNeutron1 = csvLine;

            case "SN2" -> {
                parentSensor.getSetupNeutronOutput().onNewMessage(setupNeutron1, csvLine);
                setupNeutron1 = new String[]{""};
            }
        }

        if (gammaScanRunningSumBatch.size() > 4) {
            gammaScanRunningSumBatch.removeFirst();
        }
    }

    private int getGammaMax(List<Integer> gammaBatch) {
        if (gammaBatch == null || gammaBatch.size() < 5) {
            return 0;
        }

        int windowSize = 5;
        int currentSum = 0;
        int maxSum = 0;

        for (int i = 0; i < windowSize; i++) {
            currentSum += gammaBatch.get(i);
        }
        maxSum = currentSum;

        for (int i = windowSize; i < gammaBatch.size(); i++) {
            currentSum += gammaBatch.get(i) - gammaBatch.get(i - windowSize);
            if (currentSum > maxSum) {
                maxSum = currentSum;
            }
        }

        return maxSum;
    }

}