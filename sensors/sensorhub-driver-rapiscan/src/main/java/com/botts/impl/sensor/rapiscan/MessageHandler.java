package com.botts.impl.sensor.rapiscan;


import com.botts.impl.sensor.rapiscan.eml.EMLService;
import com.botts.impl.sensor.rapiscan.eml.outputs.EMLAnalysisOutput;
import com.botts.impl.sensor.rapiscan.eml.outputs.EMLContextualOutputs;
import com.botts.impl.sensor.rapiscan.eml.outputs.EMLScanContextualOutput;
import com.botts.impl.sensor.rapiscan.output.*;
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
    private final EMLService emlService;

    List<String[]> csvList;
    String[] csvLine;
    CSVReader reader;
    Boolean currentOccupancy = false;
    Boolean isGammaAlarm = false;
    Boolean isNeutronAlarm = false;
    long occupancyStartTime;
    long occupancyEndTime;
    GammaOutput gammaOutput;
    NeutronOutput neutronOutput;
    OccupancyOutput occupancyOutput;
    TamperOutput tamperOutput;
    SpeedOutput speedOutput;
    EMLAnalysisOutput emlAnalysisOutput;
    EMLScanContextualOutput emlScanContextualOutput;
    EMLContextualOutputs emlContextualOutputs;

    GammaSetupOutputs gammaSetup;
    SetupNeutronOutput setupNeutronOutput;
    GammaThresholdOutput gammaThresholdOutput;

    EMLConfig emlConfig;
    final static String ALARM = "Alarm";
    final static String BACKGROUND = "Background";
    final static String SCAN = "Scan";
    final static String FAULT_GH = "Fault - Gamma High";
    final static String FAULT_GL = "Fault - Gamma Low";
    final static String FAULT_NH = "Fault - Neutron High";

    String [] setupGamma1;
    String [] setupGamma2;
    String [] setupNeutron1;

    LinkedList<String[]> currentBatch;

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
                        csvLine = csvList.get(0);

                        //todo add time to csv files after each line!
                        writer.writeNext(new String[]{msgLine});

                        getMainCharDefinition(csvLine[0], csvLine);

                        if(csvLine[0].contains("SG1")){
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

    public MessageHandler(EMLConfig emlConfig, InputStream msgIn,
                          GammaOutput gammaOutput,
                          NeutronOutput neutronOutput,
                          OccupancyOutput occupancyOutput,
                          TamperOutput tamperOutput,
                          SpeedOutput speedOutput,
                          GammaSetupOutputs gammaSetup,
                          SetupNeutronOutput setupNeutronOutput,
                          EMLAnalysisOutput emlAnalysisOutput,
                          EMLService emlService,
                          GammaThresholdOutput gammaThresholdOutput,
                          EMLScanContextualOutput emlScanContextualOutput,
                          EMLContextualOutputs emlContextualOutput)
    {
        this.msgIn = msgIn;
        this.gammaOutput = gammaOutput;
        this.neutronOutput = neutronOutput;
        this.occupancyOutput = occupancyOutput;
        this.tamperOutput = tamperOutput;
        this.speedOutput = speedOutput;
        this.gammaSetup = gammaSetup;
        this.setupNeutronOutput = setupNeutronOutput;
        this.gammaThresholdOutput = gammaThresholdOutput;

        this.emlConfig = emlConfig;
        this.emlAnalysisOutput = emlAnalysisOutput;
        this.emlService = emlService;
        this.emlContextualOutputs = emlContextualOutput;
        this.emlScanContextualOutput = emlScanContextualOutput;

        currentBatch = new LinkedList<>();

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

    void getMainCharDefinition(String mainChar, String[] csvLine){
        // Add occupancy for eml service
        String scanData = String.join(",", csvLine);

        if(emlConfig.isSupplementalAlgorithm){
            this.emlService.addOccupancyLine(scanData);
        }

        gammaThresholdOutput.publishThreshold(isSetup);

        if(currentBatch.size() == 5)
            currentBatch.removeFirst();

        switch (mainChar){
            // ------------------- NOT OCCUPIED
            case "GB":
                gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), BACKGROUND);

                // Send latest Gamma Background to threshold calculator
                gammaThresholdOutput.onNewGammaBackground(csvLine);

                break;
            case "GH":
                gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), FAULT_GH);
                break;
            case "GL":
                gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), FAULT_GL);
                break;
            case "NB":
                neutronOutput.onNewMessage(csvLine, System.currentTimeMillis(), BACKGROUND);
                break;
            case "NH":
                neutronOutput.onNewMessage(csvLine, System.currentTimeMillis(), FAULT_NH);
                break;

            // --------------- OCCUPIED
            case  "GA":
                if (!currentOccupancy){
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), ALARM);

                currentBatch.addLast(csvLine);

                isGammaAlarm = true;
                break;

            case "GS":
                //usually the foreground value will switch to "GA" in an alarm state
                if (!currentOccupancy){
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), SCAN);

                currentBatch.addLast(csvLine);

                break;

            case "NA":
                if (!currentOccupancy){
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                neutronOutput.onNewMessage(csvLine, System.currentTimeMillis(), ALARM);

                isNeutronAlarm = true;
                break;

            case "NS":
                if (!currentOccupancy){
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                neutronOutput.onNewMessage(csvLine, System.currentTimeMillis(), SCAN);

                break;

            case "GX":
                occupancyEndTime = System.currentTimeMillis();
                occupancyOutput.onNewMessage(occupancyStartTime, occupancyEndTime, isGammaAlarm, isNeutronAlarm, csvLine);
                currentOccupancy = false;
                isGammaAlarm = false;
                isNeutronAlarm = false;

                if(emlConfig.isSupplementalAlgorithm){
                    var results = emlService.processCurrentOccupancy();
                    emlScanContextualOutput.handleScanContextualMessage(results, occupancyEndTime);
                    emlContextualOutputs.handleContextualMessage(results, occupancyEndTime);
                    emlAnalysisOutput.handleAnalysisMessage(results, occupancyEndTime);
                }

                break;

            // -------------------- OTHER STATE
            case "TC":
                tamperOutput.onNewMessage(false);
                break;
            case "TT":
                tamperOutput.onNewMessage(true);
                break;
            case "SP":
                speedOutput.onNewMessage(csvLine);
                break;
            case "SG1":
                isSetup = true;
                setupGamma1 = csvLine;
                break;
            case "SG2":
                setupGamma2 = csvLine;
                break;
            case "SG3":
                gammaSetup.onNewMessage(setupGamma1, setupGamma2, csvLine);
                setupGamma1 = new String[]{""};
                setupGamma2 = new String[]{""};
                break;
            case "SN1":
                setupNeutron1 = csvLine;
                break;
            case "SN2":
                setupNeutronOutput.onNewMessage(setupNeutron1, csvLine);
                setupNeutron1 = new String[]{""};
                break;
        }

        gammaThresholdOutput.onNewForegroundBatch(currentBatch);

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



    public GammaOutput getGammaOutput() { return this.gammaOutput; }
    public NeutronOutput getNeutronOutput() { return this.neutronOutput; }
    public OccupancyOutput getOccupancyOutput() { return this.occupancyOutput; }
    public TamperOutput getTamperOutput() { return this.tamperOutput; }
    public SpeedOutput getSpeedOutput() { return this.speedOutput; }
    public GammaSetupOutputs getGammaSetup() { return this.gammaSetup; }
    public SetupNeutronOutput getNeutronSetupOutput() { return this.setupNeutronOutput; }

}
