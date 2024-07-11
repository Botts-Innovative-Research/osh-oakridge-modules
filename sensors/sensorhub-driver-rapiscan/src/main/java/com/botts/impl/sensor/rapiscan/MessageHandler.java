package com.botts.impl.sensor.rapiscan;

//import com.botts.impl.sensor.rapiscan.helpers.RapiscanPreset;
import com.botts.impl.sensor.rapiscan.output.*;
import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler {

    private final InputStream msgIn;

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

    SetupGamma1Output setupGamma1Output;
    SetupGamma2Output setupGamma2Output;
    SetupGamma3Output setupGamma3Output;
    SetupNeutronOutput setupNeutronOutput;


    final static String ALARM = "Alarm";
    final static String BACKGROUND = "Background";
    final static String SCAN = "Scan";
    final static String FAULT_GH = "Fault - Gamma High";
    final static String FAULT_GL = "Fault - Gamma Low";
    final static String FAULT_NH = "Fault - Neutron High";

    private final AtomicBoolean isProcessing = new AtomicBoolean(true);

    private final Thread messageReader = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean continueProcessing = true;

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
//                        System.out.println(this);

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


    public MessageHandler(InputStream msgIn, GammaOutput gammaOutput, NeutronOutput neutronOutput, OccupancyOutput occupancyOutput, TamperOutput tamperOutput, SpeedOutput speedOutput, SetupGamma1Output setupGamma1Output, SetupGamma2Output setupGamma2Output, SetupGamma3Output setupGamma3Output, SetupNeutronOutput setupNeutronOutput) {
        this.msgIn = msgIn;
        this.gammaOutput = gammaOutput;
        this.neutronOutput = neutronOutput;
        this.occupancyOutput = occupancyOutput;
        this.tamperOutput = tamperOutput;
        this.speedOutput = speedOutput;
        this.setupGamma1Output = setupGamma1Output;
        this.setupGamma2Output = setupGamma2Output;
        this.setupGamma3Output = setupGamma3Output;
        this.setupNeutronOutput = setupNeutronOutput;

        this.messageReader.start();
    }

    public void stopProcessing() {

        synchronized (isProcessing) {

            isProcessing.set(false);
        }
    }


    void getMainCharDefinition(String mainChar, String[] csvLine){
        switch (mainChar){
            // ------------------- NOT OCCUPIED
            case "GB":
                gammaOutput.onNewMessage(csvLine, System.currentTimeMillis(), BACKGROUND);
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
                setupGamma1Output.onNewMessage(csvLine);
                break;
            case "SG2":
                setupGamma2Output.onNewMessage(csvLine);
                break;
            case "SG3":
                setupGamma3Output.onNewMessage(csvLine);
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
    public SetupGamma1Output getGammaSetUp1Output() { return this.setupGamma1Output; }
    public SetupGamma2Output getGammaSetUp2Output() { return this.setupGamma2Output; }
    public SetupGamma3Output getGammaSetUp3Output() { return this.setupGamma3Output; }
    public SetupNeutronOutput getNeutronSetupOutput() { return this.setupNeutronOutput; }

}
