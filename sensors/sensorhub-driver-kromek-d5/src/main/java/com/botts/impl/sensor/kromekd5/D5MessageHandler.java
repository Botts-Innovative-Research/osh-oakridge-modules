/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.kromekd5;

import com.botts.impl.sensor.kromekd5.messages.D5Message;
import com.botts.impl.utils.n42.RadInstrumentDataType;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

public class D5MessageHandler implements Runnable {
    Thread worker;
    D5Sensor sensor;
    D5Config config;
    InputStream msgIn;

    LocationOutput locationOutput;
    BackgroundOutput backgroundOutput;
    ForegroundOutput foregroundOutput;
    AnalysisOutput analysisOutput;

    RADHelper radHelper = new RADHelper();

    static final Logger log = LoggerFactory.getLogger(OutputBase.class);

    public D5MessageHandler(D5Sensor sensor, InputStream msgIn, LocationOutput locationOutput, ForegroundOutput foregroundOutput, BackgroundOutput backgroundOutput, AnalysisOutput analysisOutput) {
        this.sensor = sensor;
        this.msgIn = msgIn;
        this.locationOutput = locationOutput;
        this.backgroundOutput = backgroundOutput;
        this.foregroundOutput = foregroundOutput;
        this.analysisOutput = analysisOutput;

        config = sensor.getConfiguration();
        worker = new Thread(this, "RS350 Message Handler");
    }

    public void parse() {
        worker.start();
    }

    public synchronized void run() {


        int c;
        StringBuilder xmlDataBuffer = new StringBuilder();

        while (!sensor.processLock) {
            try {
                while ((c = msgIn.read()) != -1) {
                    xmlDataBuffer.append((char)c);
                    String dataBufferString = xmlDataBuffer.toString();
                    if(dataBufferString.endsWith(("</RadInstrumentData>")))
                    {
                        String[] n42Messages = dataBufferString.split("</RadInstrumentData>");
                        for (String n42Message : n42Messages) {
                            log.debug("xmlEvent: " + n42Message + "</RadInstrumentData>");
                            createRS350Message(n42Message + "</RadInstrumentData>");
                        }
                        xmlDataBuffer.setLength(0);
                    }
                }
            } catch(Exception e) {
                log.error("context", e);
            }
        }
    }

    public void createRS350Message(String msg){
        String  n42msg = msg;
        RadInstrumentDataType radInstrumentDataType = radHelper.getRadInstrumentData(n42msg);

        D5Message d5Message = new D5Message(radInstrumentDataType);


        if (config.outputs.enableLocationOutput && d5Message.getD5Location() != null){
            locationOutput.parseData(d5Message);
        }

        if (config.outputs.enableBackgroundOutput && d5Message.getD5BackgroundMeasurement() != null){
            backgroundOutput.parseData(d5Message);
        }

        if (config.outputs.enableForegroundOutput && d5Message.getD5ForegroundMeasurement() != null){
            foregroundOutput.parseData(d5Message);
        }

        if (config.outputs.enableAnalysisData && d5Message.getD5AnalysisResults() != null){
            
        }


    }

}