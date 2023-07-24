/***************************** BEGIN LICENSE BLOCK ***************************
 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rs350;

import com.botts.impl.sensor.rs350.messages.RS350Message;
import com.botts.impl.utils.n42.RadInstrumentDataType;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

public class RS350MessageHandler implements Runnable {
    Thread worker;
    RS350Sensor sensor;
    RS350Config config;
    InputStream msgIn;

    LocationOutput locationOutput;
    StatusOutput statusOutput;
    BackgroundOutput backgroundOutput;
    ForegroundOutput foregroundOutput;
    DerivedDataOutput derivedDataOutput;
    AlarmOutput alarmOutput;

    RADHelper radHelper = new RADHelper();

    static final Logger log = LoggerFactory.getLogger(OutputBase.class);

    public RS350MessageHandler(RS350Sensor sensor, InputStream msgIn, LocationOutput locationOutput, StatusOutput statusOutput, BackgroundOutput backgroundOutput, ForegroundOutput foregroundOutput, AlarmOutput alarmOutput) {
        this.sensor = sensor;
        this.msgIn = msgIn;
        this.locationOutput = locationOutput;
        this.statusOutput = statusOutput;
        this.backgroundOutput = backgroundOutput;
        this.foregroundOutput = foregroundOutput;
        this.alarmOutput = alarmOutput;

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

        RS350Message rs350Message = new RS350Message(radInstrumentDataType);

        if (config.outputs.enableLocationOutput && rs350Message.getRs350ForegroundMeasurement() != null){
            locationOutput.parseData(rs350Message);
        }

        if (config.outputs.enableStatusOutput && rs350Message.getRs350InstrumentCharacteristics() != null){
            statusOutput.parseData(rs350Message);
        }

        if (config.outputs.enableBackgroundOutput && rs350Message.getRs350BackgroundMeasurement() != null){
            backgroundOutput.parseData(rs350Message);
        }

        if (config.outputs.enableForegroundOutput && rs350Message.getRs350ForegroundMeasurement() != null){
            foregroundOutput.parseData(rs350Message);
        }

        if (config.outputs.enableAlarmOutput && rs350Message.getRs350DerivedData() != null){
            alarmOutput.parseData(rs350Message);
        }


    }

}