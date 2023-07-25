package com.botts.impl.sensor.rs350;

import com.botts.impl.sensor.rs350.messages.RS350Message;
import com.botts.impl.utils.n42.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.data.TextEncodingImpl;

import java.util.List;

public class StatusOutput  extends OutputBase{

    private static final String SENSOR_OUTPUT_NAME = "RS350 Status";

    private static final Logger logger = LoggerFactory.getLogger(StatusOutput.class);

    public StatusOutput(RS350Sensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    protected void init(){
        RADHelper radHelper = new RADHelper();


        // OUTPUT

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Status")
                .definition(radHelper.getRadUri("device-status"))
                .addField("Latest Record Time", radHelper.createPrecisionTimeStamp())
                .addField("Battery Charge", radHelper.createBatteryCharge())
                .addField("Scan Mode",
                        radHelper.createText()
                                .name("ScanMode")
                                .label("Scan Mode")
                                .definition(radHelper.getRadUri("scan-mode"))
                                .build())
                .addField("Scan Timeout",
                        radHelper.createQuantity()
                                .name("ScanTimeout")
                                .label("Scan Timeout")
                                .definition(radHelper.getRadUri("scan-timeout"))
                                .build())
                .addField("Analysis Enabled",
                        radHelper.createText()
                                .name("AnalysisEnabled")
                                .label("Analysis Enabled")
                                .definition(radHelper.getRadUri("analysis-enabled"))
                                .build())
                .addField("LinCalibration", radHelper.createLinCalibration())
                .addField("CmpCalibration", radHelper.createCmpCalibration())
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

        // Time

        // Battery Charge (%)
        // RSI Scan Mode (string)
        // RSI Scan Number (quantity)
        // RSI Scan Timeout Number (quantity)
        // RSI Analysis Enabled (quantity)
        // Lin Energy Calibration (matrix) (N42 Helper)
        // Cmp Energy Calibration (matrix) (N42 Helper)
        // Location (location vector)
    }

    public void parseData(RS350Message msg){
        if (latestRecord == null)
            dataBlock = dataStruct.createDataBlock();
        else
            dataBlock = latestRecord.renew();

        latestRecordTime = System.currentTimeMillis();

        dataBlock.setLongValue(0, latestRecordTime);
        dataBlock.setDoubleValue(1, msg.getRs350InstrumentCharacteristics().getBatteryCharge());
        dataBlock.setStringValue(2, msg.getRs350Item().getRsiScanMode());
        dataBlock.setDoubleValue(3, msg.getRs350Item().getRsiScanTimeoutNumber());
        dataBlock.setStringValue(4, msg.getRs350Item().getRsiAnalysisEnabled());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[5].setUnderlyingObject(msg.getRs350LinEnergyCalibration());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[6].setUnderlyingObject(msg.getRs350CmpEnergyCalibration());

        eventHandler.publish(new DataEvent(latestRecordTime, StatusOutput.this, dataBlock));
    }



}
