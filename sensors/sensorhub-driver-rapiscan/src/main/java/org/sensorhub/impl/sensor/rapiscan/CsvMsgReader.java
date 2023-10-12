package org.sensorhub.impl.sensor.rapiscan;


import com.opencsv.CSVReader;

import java.io.*;
import java.util.List;

public class CsvMsgReader {

    String string;
    List<String[]> csvValues;

    List<String[]> gammaOccupancy;
    List<String[]> neutronOccupancy;
    Boolean hasGammaOccupancy = false;
    Boolean hasNeutronOccupancy = false;

    List<String[]> gammaBG;
    List<String[]> neutronBG;
    Boolean hasGammaBG = false;
    Boolean hasNeutronBG = false;

    List<String[]> setup1;
    List<String[]> setup2;
    List<String[]> setup3;
    List<String[]> setup4;
    List<String[]> setup5;
    Boolean hasSetup1 = false;
    Boolean hasSetup2 = false;
    Boolean hasSetup3 = false;
    Boolean hasSetup4 = false;
    Boolean hasSetup5 = false;

    String state;
    String speed;
//
//    void readMessages(InputStream inputStream) throws IOException {
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//        string = bufferedReader.readLine();
//        while (string != ""){
//            CSVReader reader = new CSVReader(new StringReader(string));
//            csvValues = reader.readAll();
//
//            getMainCharaterDefinition(csvValues.get(0).toString());
//
////            if(hasGammaOccupancy && hasNeutronOccupancy){
////                setOccupancyData(gammaOccupancy, neutronOccupancy);
////                clearOccupancy();
////            }
////
////            if(hasGammaBG && hasNeutronBG){
////                setBackgroundData(gammaBG, neutronBG);
////                clearBackground();
////            }
//
//            if (hasSetup1 && hasSetup2 && hasSetup3 && hasSetup4 && hasSetup5){
//                setSetupData(setup1, setup2, setup3, setup4, setup5);
//                clearSetup();
//            }
//        }
//    }
//
//    void getMainCharaterDefinition(String mainChar){
//        switch (mainChar){
//           case  "GA":
//               occupancyDataGamma("Gamma Alarm", true);
////               setGammaOccupancy();
//            break;
//            case "GB":
//                backgroundDataGamma("Gamma Background", false);
////                setGammaBG();
//            break;
//            case "GH":
//                backgroundDataGamma("Gamma High (Fault)", true);
////                setGammaBG();
//            break;
//            case "GL":
//                backgroundDataGamma("Gamma Low (Fault)", true);
////                setGammaBG();
//            break;
//            case "GS":
//                occupancyDataGamma("Gamma Scan", false);
////                setGammaOccupancy();
//            break;
//            case "GX":
//                stateData("Occupancy Cleared");
//            break;
//            case "NA":
//                occupancyDataNeutron("Neutron Alarm", true);
////                setNeutronOccupancy();
//            break;
//            case "NB":
//                backgroundDataNeutron("Neutron Background", false);
////                setNeutronBG();
//            break;
//            case "NH":
//                backgroundDataNeutron("Neutron High (Fault)", true);
////                setNeutronBG();
//            break;
//            case "NS":
//                occupancyDataNeutron("Neutron Scan");
////                setNeutronOccupancy();
//            break;
//            case "TC":
//                stateData("Tamper Cleared");
//            break;
//            case "TT":
//                stateData("Tamper Fault");
//            break;
//            case "SP":
//                speedMessage("Speed Message");
//            break;
//            case "SG1":
////                setupData1("Setup Gamma 1");
//                setSetup1();
//            break;
//            case "SG2":
////                setupData2("Setup Gamma 2");
//                setSetup2();
//            break;
//            case "SG3":
////                setupData3("Setup Gamma 3");
//                setSetup3();
//            break;
//            case "SN1":
////                setupData4("Setup Neutron 1");
//                setSetup4();
//            break;
//            case "SN2":
////                setupData5("Setup Neutron 2");
//                setSetup5();
//            break;
//
//            }
//
//    }


    void setGammaBG(){
        gammaBG = csvValues;
        hasGammaBG = true;
    }

    void setGammaOccupancy(){
        gammaOccupancy = csvValues;
        hasGammaOccupancy = true;
    }

    void setNeutronOccupancy(){
        neutronOccupancy = csvValues;
        hasNeutronOccupancy = true;
    }

    void setNeutronBG(){
        neutronBG = csvValues;
        hasNeutronBG = true;
    }

    void setSetup1(){
        setup1 = csvValues;
        hasSetup1 = true;
    }

    void setSetup2(){
        setup2 = csvValues;
        hasSetup2 = true;
    }

    void setSetup3(){
        setup3 = csvValues;
        hasSetup3 = true;
    }

    void setSetup4(){
        setup4 = csvValues;
        hasSetup4 = true;
    }

    void setSetup5(){
        setup5 = csvValues;
        hasSetup5 = true;
    }

    void clearOccupancy(){
        hasNeutronOccupancy = false;
        hasGammaOccupancy = false;
        gammaOccupancy.clear();
        neutronOccupancy.clear();
    }

    void clearBackground(){
        hasNeutronBG = false;
        hasGammaBG = false;
        gammaBG.clear();
        neutronBG.clear();
    }

    void clearSetup(){
        hasSetup1 = false;
        hasSetup2 = false;
        hasSetup3 = false;
        hasSetup4 = false;
        hasSetup5 = false;
        setup1.clear();
        setup2.clear();
        setup3.clear();
        setup4.clear();
        setup5.clear();
    }

}
