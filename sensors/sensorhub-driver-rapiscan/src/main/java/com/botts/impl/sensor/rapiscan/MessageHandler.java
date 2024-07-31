package com.botts.impl.sensor.rapiscan;


import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler {

    private final InputStream msgIn;
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

    String [] setupGamma1;
    String [] setupGamma2;
    String [] setupNeutron1;

    LinkedList<String[]> gammaScanRunningSumBatch;

    boolean isSetup = false;
    String sampleSetups =
            "SG1,000450,000068,05,10,08.0,P\n" +
            "SG2,1111,0.069,0.455,01,1010,A\n" +
            "SG3,0.069,0.455,020,000,1.10.1\n" +
            "SN1,000050,05,0047,1200,02,120\n" +
            "SN2,0.504,5.040,0.504,5.040,PP\n" +
            "NS,000000,000000,000000,000000\n" +
            "GS,000052,000052,000052,000052\n" +
            "GS,000052,000052,000052,000052\n" +
            "SP,0.2249,03.032,004.88,000000\n" +
            "GS,000053,000053,000053,000053\n" +
            "GS,000054,000054,000054,000054\n" +
            "GS,000056,000056,000056,000056\n" +
            "NS,000000,000000,000000,000000\n" +
            "GS,000059,000059,000059,000059\n" +
            "GS,000064,000064,000064,000064\n" +
            "GS,000072,000072,000072,000072\n" +
            "SG1,000450,000068,06,08,14.0,P\n" +
            "SG2,1111,0.069,0.455,01,1111,A\n" +
            "SG3,0.069,0.455,020,000,1.10.1\n" +
            "SN1,000050,05,0047,1200,02,120\n" +
            "SN2,0.504,5.040,0.504,5.040,PP\n";


    FileWriter fw;
    CSVWriter writer;
    private final AtomicBoolean isProcessing = new AtomicBoolean(true);
    // Setup boolean
    private final Thread messageReader = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean continueProcessing = true;

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String dateStamp = dateFormat.format(new Date());
            String fileName = "dailyfile_"+dateStamp+"_"+System.currentTimeMillis()+".csv";

            File dailyFile = new File(fileName);

            try
            {
                fw = new FileWriter(dailyFile);
                writer = new CSVWriter(fw);


                //todo: read from config if no set up values are available
                //todo:
                while (continueProcessing){
                    BufferedReader bufferedReader;
                    InputStream setupInput = new ByteArrayInputStream(sampleSetups.getBytes());

//                    //this is just to simulate the setup values coming in!! for testing purposes :)
                    if(!isSetup) {
                        bufferedReader = new BufferedReader(new InputStreamReader(setupInput));
                        isSetup = true;

                    } else {
                        bufferedReader = new BufferedReader(new InputStreamReader(msgIn));
                    }

//                    bufferedReader = new BufferedReader(new InputStreamReader(msgIn));

                    String msgLine = bufferedReader.readLine();
                    while (msgLine != null){
                        reader = new CSVReader(new StringReader(msgLine));
                        csvList = reader.readAll();

                        //todo add time to csv files after each line!
                        writer.writeNext(new String[]{msgLine});

                        onNewMainChar(csvList.get(0)[0], csvList.get(0));

                        if(csvList.get(0)[0].contains("SG1")){
                            isSetup = true;
                        }
                        checkParameters(); //checks if set up values are given, if not then sets default values from config

                        System.out.println(msgLine);
                        msgLine = bufferedReader.readLine();

                        synchronized (isProcessing) {
                            continueProcessing = isProcessing.get();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    public MessageHandler(InputStream msgIn, RapiscanSensor parentSensor)
    {
        this.msgIn = msgIn;
        this.parentSensor = parentSensor;

        gammaScanRunningSumBatch = new LinkedList<>();

        this.messageReader.start();
    }

    public void stopProcessing() {
        synchronized (isProcessing) {
            isProcessing.set(false);
        }
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void onNewMainChar(String mainChar, String[] csvLine){

        // Add occupancy for eml service
        if(parentSensor.getConfiguration().emlConfig.emlEnabled) {
            parentSensor.getEmlService().addOccupancyLine(csvLine);
        }

        parentSensor.gammaThresholdOutput.publishThreshold(isSetup);

        switch (mainChar){
            // ------------------- NOT OCCUPIED
            case "GB":
                parentSensor.gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), BACKGROUND);

                // Send latest Gamma Background to threshold calculator
                parentSensor.gammaThresholdOutput.onNewGammaBackground(csvLine);

                break;
            case "GH":
                parentSensor.gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), FAULT_GH);
                break;
            case "GL":
                parentSensor.gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), FAULT_GL);
                break;
            case "NB":
                parentSensor.neutronOutput.onNewMessage(csvLine, System.currentTimeMillis(), BACKGROUND);
                break;
            case "NH":
                parentSensor.neutronOutput.onNewMessage(csvLine, System.currentTimeMillis(), FAULT_NH);
                break;

            // --------------- OCCUPIED
            case  "GA":
                if (!currentOccupancy){
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                parentSensor.gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), ALARM);

                gammaScanRunningSumBatch.addLast(csvLine);

                isGammaAlarm = true;
                break;

            case "GS":
                //usually the foreground value will switch to "GA" in an alarm state
                if (!currentOccupancy){
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                parentSensor.gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), SCAN);

                gammaScanRunningSumBatch.addLast(csvLine);

                break;

            case "NA":
                if (!currentOccupancy){
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                parentSensor.neutronOutput.onNewMessage(csvLine, System.currentTimeMillis(), ALARM);

                isNeutronAlarm = true;
                break;

            case "NS":
                if (!currentOccupancy){
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                parentSensor.neutronOutput.onNewMessage(csvLine, System.currentTimeMillis(), SCAN);

                break;

            case "GX":
                occupancyEndTime = System.currentTimeMillis();
                parentSensor.occupancyOutput.onNewMessage(occupancyStartTime, occupancyEndTime, isGammaAlarm, isNeutronAlarm, csvLine);
                currentOccupancy = false;
                isGammaAlarm = false;
                isNeutronAlarm = false;

                if(parentSensor.getConfiguration().emlConfig.emlEnabled){
                    var results = parentSensor.emlService.processCurrentOccupancy();
                    parentSensor.emlScanContextualOutput.handleScanContextualMessage(results, occupancyEndTime);
                    parentSensor.emlContextualOutput.handleContextualMessage(results, occupancyEndTime);
                    parentSensor.emlAnalysisOutput.handleAnalysisMessage(results, occupancyEndTime);
                }

                break;

            // -------------------- OTHER STATE
            case "TC":
                parentSensor.tamperOutput.onNewMessage(false);
                break;
            case "TT":
                parentSensor.tamperOutput.onNewMessage(true);
                break;
            case "SP":
                parentSensor.speedOutput.onNewMessage(csvLine);
                break;
            case "SG1":
                isSetup = true;
                setupGamma1 = csvLine;
                break;
            case "SG2":
                setupGamma2 = csvLine;
                break;
            case "SG3":
                parentSensor.setupGammaOutput.onNewMessage(setupGamma1, setupGamma2, csvLine);
                setupGamma1 = new String[]{""};
                setupGamma2 = new String[]{""};
                break;
            case "SN1":
                setupNeutron1 = csvLine;
                break;
            case "SN2":
                parentSensor.setupNeutronOutput.onNewMessage(setupNeutron1, csvLine);
                setupNeutron1 = new String[]{""};
                break;
        }

        parentSensor.gammaThresholdOutput.onNewForegroundBatch(currentBatch);

    }

    /**
     * this checks to see if setups are available if not sets the default values
     */
    public void checkParameters(){
        if(!isSetup){
            System.out.println("No setup information found");
            // use the setup values from the config
            useDefaultValues();
        }else{
            System.out.println("Setup information is found!");
        }
    }

    /**
     * this sets the set up values that are used in the gamma threshold outputs
     */
    public void useDefaultValues(){
        this.gammaSetup.setIntervals(emlConfig.gammaSetupConfig.intervals);
        this.gammaSetup.setOccupancyHoldin(emlConfig.gammaSetupConfig.occupancyHoldin);
        this.gammaSetup.setNsigma(emlConfig.gammaSetupConfig.nsigma);
        this.gammaSetup.setAlgorithm(emlConfig.gammaSetupConfig.algorithm);
    }

}
