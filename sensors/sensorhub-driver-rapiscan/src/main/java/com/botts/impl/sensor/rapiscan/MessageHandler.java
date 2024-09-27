package com.botts.impl.sensor.rapiscan;


import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
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

    //    FileWriter fw;
    //    CSVWriter writer;
    private final AtomicBoolean isProcessing = new AtomicBoolean(true);
    private long timeSinceLastMessage;

    public long getTimeSinceLastMessage() {
        long now = System.currentTimeMillis();
//        System.out.printf("\nTime since last message: %d seconds", (now - timeSinceLastMessage)/1000);
//        parentSensor.getLogger().debug("Time since last message: {} seconds", (now - timeSinceLastMessage)/1000);
        return (now - timeSinceLastMessage);
    }

    public MessageHandler(InputStream msgIn, RapiscanSensor parentSensor) {
        this.parentSensor = parentSensor;

        gammaScanRunningSumBatch = new LinkedList<>();
        occupancyGammaBatch = new LinkedList<>();
        occupancyNeutronBatch = new LinkedList<>();

        timeSinceLastMessage = System.currentTimeMillis();

        // Setup boolean
        Thread messageReader = new Thread(() -> {
            boolean continueProcessing = true;

            try {

                while (continueProcessing) {
                    BufferedReader bufferedReader;
                    bufferedReader = new BufferedReader(new InputStreamReader(msgIn));

                    String msgLine = bufferedReader.readLine();
                    while (msgLine != null) {
                        reader = new CSVReader(new StringReader(msgLine));
                        csvList = reader.readAll();

                        parentSensor.getDailyFileOutput().onNewMessage(msgLine);

                        onNewMainChar(csvList.get(0)[0], csvList.get(0));

                        timeSinceLastMessage = System.currentTimeMillis();

                        System.out.println(msgLine);
                        msgLine = bufferedReader.readLine();

                        synchronized (isProcessing) {
                            continueProcessing = isProcessing.get();
                        }
                    }
                }

            } catch (Exception e) {
                parentSensor.getLogger().error(e.getMessage());
            }
        });
        messageReader.start();
    }

    public void stopProcessing() {
        synchronized (isProcessing) {
            isProcessing.set(false);
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
            case "GB":
                parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), BACKGROUND, null);
                // Send latest Gamma Background to threshold calculator and EML service
                parentSensor.getGammaThresholdOutput().onNewBackground(csvLine);
                parentSensor.getEmlService().setLatestGammaBackground(csvLine);
                break;
            case "GH":
                parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), FAULT_GH, null);
                break;
            case "GL":
                parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), FAULT_GL, null);
                break;
            case "NB":
                parentSensor.getNeutronOutput().onNewMessage(csvLine, System.currentTimeMillis(), BACKGROUND);
                parentSensor.getEmlService().setLatestNeutronBackground(csvLine);
                break;
            case "NH":
                parentSensor.getNeutronOutput().onNewMessage(csvLine, System.currentTimeMillis(), FAULT_NH);
                break;

            // --------------- OCCUPIED
            case "GA":
                gammaScanRunningSumBatch.addLast(csvLine);
                if (!currentOccupancy) {
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                } else {
                    occupancyGammaBatch.addLast(Integer.parseInt(csvLine[1]));
                    occupancyGammaBatch.addLast(Integer.parseInt(csvLine[2]));
                    occupancyGammaBatch.addLast(Integer.parseInt(csvLine[3]));
                    occupancyGammaBatch.addLast(Integer.parseInt(csvLine[4]));
                }
                parentSensor.getGammaThresholdOutput().onNewForeground(getGammaForegroundCountsPerSecond());
                parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), ALARM, getGammaForegroundCountsPerSecond());

                isGammaAlarm = true;
                break;

            case "GS":
                gammaScanRunningSumBatch.addLast(csvLine);
                //usually the foreground value will switch to "GA" in an alarm state
                if (!currentOccupancy) {
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                } else {
                    occupancyGammaBatch.addLast(Integer.parseInt(csvLine[1]));
                    occupancyGammaBatch.addLast(Integer.parseInt(csvLine[2]));
                    occupancyGammaBatch.addLast(Integer.parseInt(csvLine[3]));
                    occupancyGammaBatch.addLast(Integer.parseInt(csvLine[4]));
                }
                parentSensor.getGammaThresholdOutput().onNewForeground(getGammaForegroundCountsPerSecond());
                parentSensor.getGammaOutput().onNewMessage(csvLine, System.currentTimeMillis(), SCAN, getGammaForegroundCountsPerSecond());

                break;

            case "NA":
                if (!currentOccupancy) {
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;

                } else {
                    occupancyNeutronBatch.addLast(Integer.parseInt(csvLine[1]));
                    occupancyNeutronBatch.addLast(Integer.parseInt(csvLine[2]));
                    occupancyNeutronBatch.addLast(Integer.parseInt(csvLine[3]));
                    occupancyNeutronBatch.addLast(Integer.parseInt(csvLine[4]));
                }
                parentSensor.getNeutronOutput().onNewMessage(csvLine, System.currentTimeMillis(), ALARM);

                isNeutronAlarm = true;
                break;

            case "NS":
                if (!currentOccupancy) {
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                } else {
                    occupancyNeutronBatch.addLast(Integer.parseInt(csvLine[1]));
                    occupancyNeutronBatch.addLast(Integer.parseInt(csvLine[2]));
                    occupancyNeutronBatch.addLast(Integer.parseInt(csvLine[3]));
                    occupancyNeutronBatch.addLast(Integer.parseInt(csvLine[4]));
                }
                parentSensor.getNeutronOutput().onNewMessage(csvLine, System.currentTimeMillis(), SCAN);

                break;

            case "GX":
                occupancyEndTime = System.currentTimeMillis();
                gammaMax = calcMax(occupancyGammaBatch);
                neutronMax = calcMax(occupancyNeutronBatch);

                parentSensor.getOccupancyOutput().onNewMessage(occupancyStartTime, occupancyEndTime, isGammaAlarm, isNeutronAlarm, csvLine, gammaMax, neutronMax);
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
                break;

            // -------------------- OTHER STATE
            case "TC":
                parentSensor.getTamperOutput().onNewMessage(false);
                break;
            case "TT":
                parentSensor.getTamperOutput().onNewMessage(true);
                break;
            case "SP":
                parentSensor.getSpeedOutput().onNewMessage(csvLine);
                break;
            case "SG1":
                setupGamma1 = csvLine;
                break;
            case "SG2":
                setupGamma2 = csvLine;
                break;
            case "SG3":
                parentSensor.getSetupGammaOutput().onNewMessage(setupGamma1, setupGamma2, csvLine);
                setupGamma1 = new String[]{""};
                setupGamma2 = new String[]{""};
                break;
            case "SN1":
                setupNeutron1 = csvLine;
                break;
            case "SN2":
                parentSensor.getSetupNeutronOutput().onNewMessage(setupNeutron1, csvLine);
                setupNeutron1 = new String[]{""};
                break;
        }

        if (gammaScanRunningSumBatch.size() > 4) {
            gammaScanRunningSumBatch.removeFirst();
        }
    }

    public int calcMax(LinkedList<Integer> occBatch) {

        int temp = 0;
        int position = 0;
        for (int i = 0; i < occBatch.size(); i++) {
            if (occBatch.get(i) > temp) {
                temp = occBatch.get(i);
                position = i;
            }
        }
        return occBatch.get(position);
    }
}