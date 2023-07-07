/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

public class RS350MessageHandler implements Runnable {
    Thread worker;
    RS350Sensor sensor;
    RS350Config config;
    InputStream msgIn;
    OutputBase radInstrumentInformation;

    static final Logger log = LoggerFactory.getLogger(OutputBase.class);

    public RS350MessageHandler(RS350Sensor sensor, InputStream msgIn, OutputBase radInstrumentInformation) {
        this.sensor = sensor;
        this.msgIn = msgIn;
        this.radInstrumentInformation = radInstrumentInformation;

        config = sensor.getConfiguration();
        worker = new Thread(this, "RS350 Message Handler");
    }

    public void parse() {
        worker.start();
    }

    public synchronized void run() {
        while (!sensor.processLock) {
            try {
                handleOutput("asd");
            } catch (Exception e) {
                log.error("context", e);
            }
        }
    }

    public void handleOutput(String dataString) {
        if (config.outputs.enableRadInstrumentInformation) {
//            radInstrumentInformation.parseData();
        }
    }
}