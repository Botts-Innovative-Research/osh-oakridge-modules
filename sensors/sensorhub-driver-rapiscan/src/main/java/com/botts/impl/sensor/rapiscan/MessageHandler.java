package com.botts.impl.sensor.rapiscan;

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


    public MessageHandler(InputStream msgIn, GammaOutput gammaOutput, NeutronOutput neutronOutput, OccupancyOutput occupancyOutput, TamperOutput tamperOutput, SpeedOutput speedOutput) {
        this.msgIn = msgIn;
        this.gammaOutput = gammaOutput;
        this.neutronOutput = neutronOutput;
        this.occupancyOutput = occupancyOutput;
        this.tamperOutput = tamperOutput;
        this.speedOutput = speedOutput;

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
//                setupData1("Setup Gamma 1");
//                setSetup1();
                break;
            case "SG2":
//                setupData2("Setup Gamma 2");
//                setSetup2();
                break;
            case "SG3":
//                setupData3("Setup Gamma 3");
//                setSetup3();
                break;
            case "SN1":
//                setupData4("Setup Neutron 1");
//                setSetup4();
                break;
            case "SN2":
//                setupData5("Setup Neutron 2");
//                setSetup5();
                break;
        }
    }

}
