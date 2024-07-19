package com.botts.impl.sensor.rapiscan;

//import com.botts.impl.sensor.rapiscan.helpers.RapiscanPreset;
import com.botts.impl.sensor.rapiscan.eml.EMLAnalysisOutput;
import com.botts.impl.sensor.rapiscan.eml.EMLContextualOutputs;
import com.botts.impl.sensor.rapiscan.eml.EMLScanContextualOutput;
import com.botts.impl.sensor.rapiscan.eml.EMLService;
import com.botts.impl.sensor.rapiscan.output.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.sensorhub.utils.FileUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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


    FileWriter fw;
    CSVWriter writer;
    private final AtomicBoolean isProcessing = new AtomicBoolean(true);

    private final Thread messageReader = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean continueProcessing = true;

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String dateStamp = dateFormat.format(new Date());
            String fileName= "dailyfile_"+dateStamp+"_"+System.currentTimeMillis()+".csv";
            File dailyFile = new File(fileName);

            try{
                while (continueProcessing){
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(msgIn));
                    String msgLine = bufferedReader.readLine();
                    while (msgLine != null){
                        reader = new CSVReader(new StringReader(msgLine));
                        csvList = reader.readAll();
                        csvLine = csvList.get(0);
                        getMainCharDefinition(csvLine[0], csvLine);
                        System.out.println(msgLine);

                        fw = new FileWriter(dailyFile);
                        writer = new CSVWriter(fw);
                        writer.writeNext(new String[]{msgLine});
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

        //TODO: fix this.  bc it doesnt allways need this.
        if(emlConfig.isSupplementalAlgorithm){
            this.emlService.addOccupancyLine(scanData);

        }

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

                isGammaAlarm = true;
                break;

            case "GS":
                if (!currentOccupancy){
                    occupancyStartTime = System.currentTimeMillis();
                    currentOccupancy = true;
                }
                gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), SCAN);

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

                gammaThresholdOutput.publishThreshold();
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

                //TODO: combine set up gammas
            case "SG1":
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
                setupNeutronOutput.onNewMessage(csvLine);
                break;
            case "SN2":
                System.out.println("SN2 no output");
                break;
        }

    }

    public GammaOutput getGammaOutput() { return this.gammaOutput; }
    public NeutronOutput getNeutronOutput() { return this.neutronOutput; }
    public OccupancyOutput getOccupancyOutput() { return this.occupancyOutput; }
    public TamperOutput getTamperOutput() { return this.tamperOutput; }
    public SpeedOutput getSpeedOutput() { return this.speedOutput; }
    public GammaSetupOutputs getGammaSetup() { return this.gammaSetup; }
    public SetupNeutronOutput getNeutronSetupOutput() { return this.setupNeutronOutput; }

}
