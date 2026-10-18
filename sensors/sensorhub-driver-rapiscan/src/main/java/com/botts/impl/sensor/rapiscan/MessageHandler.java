package com.botts.impl.sensor.rapiscan;


import com.opencsv.CSVReader;
import org.sensorhub.impl.utils.rad.model.Occupancy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler {

    private final RapiscanSensor parentSensor;

    List<String[]> csvList;
    CSVReader reader;
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

    private volatile long timeSinceLastMessage;

    private final InputStream msgIn;
    private Future<?> messageReaderFuture;
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final CountDownLatch readerStopped = new CountDownLatch(1);
    private volatile BufferedReader bufferedReader;

    public long getTimeSinceLastMessage() {
        long now = System.currentTimeMillis();
        return (now - timeSinceLastMessage);
    }

    public MessageHandler(InputStream msgIn, RapiscanSensor parentSensor) {
        this.parentSensor = parentSensor;
        this.msgIn = msgIn;

        gammaScanRunningSumBatch = new LinkedList<>();
        occupancyGammaBatch = new LinkedList<>();
        occupancyNeutronBatch = new LinkedList<>();

        timeSinceLastMessage = System.currentTimeMillis();

        start();
    }

    public synchronized void start() {
        if (!hasStarted.compareAndSet(false, true)) {
            parentSensor.getLogger().warn("MessageHandler already running");
            return;
        }

        isRunning.set(true);
        bufferedReader = new BufferedReader(new InputStreamReader(msgIn));

        try {
            messageReaderFuture = RapiscanThreadPoolManager.getInstance().submitMessageReader(() -> {
                parentSensor.getLogger().debug("Message reader started for sensor {}", parentSensor.getUniqueIdentifier());

                try {
                    String msgLine;
                    while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                        try {
                            msgLine = bufferedReader.readLine();
                        } catch (IOException e) {
                            if (isRunning.get()) {
                                parentSensor.getLogger().error("Error reading RPM message", e);
                            }
                            break;
                        }

                        if (msgLine == null || !isRunning.get()) {
                            break;
                        }
                        if (msgLine.isEmpty())
                            continue;

                        // Daily-file phenomenon time and Occupancy.startTime share this exact
                        // millisecond correlation key.
                        Instant receivedAt = Instant.ofEpochMilli(System.currentTimeMillis());
                        var journal = parentSensor.getDailyFileJournal();
                        if (journal != null) {
                            try {
                                // Audit capture happens before CSV parsing or any output publication.
                                journal.append(msgLine, receivedAt);
                            } catch (IOException e) {
                                parentSensor.getLogger().error("Unable to append raw RPM line to daily-file journal", e);
                                if (journal.isRequired())
                                    throw e;
                            }
                        }

                        try {
                            reader = new CSVReader(new StringReader(msgLine));
                            csvList = reader.readAll();

                            if (!csvList.isEmpty() && csvList.get(0).length > 0 && isRunning.get()) {
                                if (journal == null || journal.shouldPublishLegacyObservation())
                                    parentSensor.getDailyFileOutput().onNewMessage(msgLine, receivedAt);
                                onNewMainChar(csvList.get(0)[0], csvList.get(0), receivedAt);
                                timeSinceLastMessage = System.currentTimeMillis();
                            }
                        } catch (Exception e) {
                            if (isRunning.get()) {
                                parentSensor.getLogger().error("Error processing RPM message: {}", msgLine, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (isRunning.get()) {
                        parentSensor.getLogger().error("Fatal error in message reader", e);
                    }
                } finally {
                    isRunning.set(false);
                    readerStopped.countDown();
                    parentSensor.getLogger().debug("Message reader exiting for sensor {}", parentSensor.getUniqueIdentifier());
                }
            });
        } catch (RuntimeException e) {
            isRunning.set(false);
            readerStopped.countDown();
            closeInput();
            throw e;
        }
    }

    private void closeInput() {
        try {
            // Close the raw stream first so a socket-backed readLine() is
            // unblocked before BufferedReader.close() waits on its lock.
            msgIn.close();
        } catch (IOException e) {
            parentSensor.getLogger().debug("Error closing RPM input stream", e);
        }

        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        } catch (IOException e) {
            parentSensor.getLogger().debug("Error closing RPM message reader", e);
        }
    }

    public synchronized void stop() {
        if (!hasStarted.get()) {
            return;
        }

        parentSensor.getLogger().debug("Stopping MessageHandler for sensor {}", parentSensor.getUniqueIdentifier());
        isRunning.set(false);
        closeInput();

        try {
            if (!readerStopped.await(5, TimeUnit.SECONDS)) {
                if (messageReaderFuture != null) {
                    messageReaderFuture.cancel(true);
                }
                parentSensor.getLogger().warn("Message reader did not stop within timeout");
            }
        } catch (InterruptedException e) {
            if (messageReaderFuture != null) {
                messageReaderFuture.cancel(true);
            }
            Thread.currentThread().interrupt();
            parentSensor.getLogger().warn("Interrupted while stopping message reader");
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
        onNewMainChar(mainChar, csvLine, Instant.now());
    }

    void onNewMainChar(String mainChar, String[] csvLine, Instant receivedAt) {
        long receivedAtMillis = receivedAt.toEpochMilli();

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
                    occupancyStartTime = receivedAtMillis;
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
                    occupancyStartTime = receivedAtMillis;
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
                    occupancyStartTime = receivedAtMillis;
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
                    occupancyStartTime = receivedAtMillis;
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
                occupancyEndTime = receivedAtMillis;
                gammaMax = getGammaMax(occupancyGammaBatch);
                neutronMax = getNeutronMax(occupancyNeutronBatch);

                Occupancy occupancy = new Occupancy.Builder()
                        .occupancyCount(Integer.parseInt(csvLine[1]))
                        .startTime(occupancyStartTime/1000d)
                        .endTime(occupancyEndTime/1000d)
                        .samplingTime(receivedAtMillis/1000d)
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

    static int getNeutronMax(List<Integer> neutronBatch) {
        if (neutronBatch == null || neutronBatch.isEmpty()) {
            return 0;
        }
        return Collections.max(neutronBatch);
    }

}
