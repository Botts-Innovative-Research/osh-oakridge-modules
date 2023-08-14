package com.botts.impl.sensor.rs350;

import com.botts.impl.sensor.rs350.messages.RS350Message;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.data.TextEncodingImpl;

public class ForegroundOutput extends OutputBase {

    private static final String SENSOR_OUTPUT_NAME = "RS350 Foreground Report";

    private static final Logger logger = LoggerFactory.getLogger(ForegroundOutput.class);

    public ForegroundOutput(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    protected void init() {
        RADHelper radHelper = new RADHelper();
        final String LIN_SPEC_ID = "lin-spectrum";
        final String CMP_SPEC_ID = "cmp-spectrum";
        // OUTPUT

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Foreground Report")
                .definition(RADHelper.getRadUri("foreground-report"))
                .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
                .addField("Duration",
                        radHelper.createQuantity()
                                .name("Duration")
                                .label("Duration")
                                .definition(RADHelper.getRadUri("duration")))
                .addField("LinSpectrumSize", radHelper.createArraySize("Lin Spectrum Size", LIN_SPEC_ID))
                .addField("LinSpectrum", radHelper.createLinSpectrum(LIN_SPEC_ID))
                .addField("CmpSpectrumSize", radHelper.createArraySize("Cmp Spectrum Size", CMP_SPEC_ID))
                .addField("CmpSpectrum", radHelper.createCmpSpectrum(CMP_SPEC_ID))
                .addField("GammaGrossCount", radHelper.createGammaGrossCount())
                .addField("NeutronGrossCount", radHelper.createNeutronGrossCount())
                .addField("DoseRate", radHelper.createDoseUSVh())
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

    }

//    public void parseData(RS350Message msg) {
//        if (latestRecord == null)
//            dataBlock = dataStruct.createDataBlock();
//        else
//            dataBlock = latestRecord.renew();
//
//        latestRecordTime = System.currentTimeMillis() / 1000;
//
//        dataBlock.setLongValue(0, msg.getRs350ForegroundMeasurement().getStartDateTime() / 1000);
//        dataBlock.setDoubleValue(1, msg.getRs350ForegroundMeasurement().getRealTimeDuration());
//        dataBlock.setIntValue(2, msg.getRs350ForegroundMeasurement().getLinEnCalSpectrum().length);
//        ((DataBlockMixed) dataBlock).getUnderlyingObject()[3].setUnderlyingObject(msg.getRs350ForegroundMeasurement().getLinEnCalSpectrum());
//        dataBlock.setIntValue(4, msg.getRs350ForegroundMeasurement().getLinEnCalSpectrum().length);
//        ((DataBlockMixed) dataBlock).getUnderlyingObject()[5].setUnderlyingObject(msg.getRs350ForegroundMeasurement().getLinEnCalSpectrum());
//        dataBlock.setDoubleValue(6, msg.getRs350ForegroundMeasurement().getGammaGrossCount());
//        dataBlock.setDoubleValue(7, msg.getRs350ForegroundMeasurement().getNeutronGrossCount());
//        dataBlock.setDoubleValue(8, msg.getRs350ForegroundMeasurement().getDoseRate());
//
//        eventHandler.publish(new DataEvent(latestRecordTime, ForegroundOutput.this, dataBlock));
//    }


    @Override
    public void onNewMessage(RS350Message message) {

        if (message.getRs350ForegroundMeasurement() != null) {

            createOrRenewDataBlock();

            latestRecordTime = System.currentTimeMillis() / 1000;

            dataBlock.setLongValue(0, message.getRs350ForegroundMeasurement().getStartDateTime() / 1000);
            dataBlock.setDoubleValue(1, message.getRs350ForegroundMeasurement().getRealTimeDuration());
            dataBlock.setIntValue(2, message.getRs350ForegroundMeasurement().getLinEnCalSpectrum().length);
            ((DataBlockMixed) dataBlock).getUnderlyingObject()[3].setUnderlyingObject(message.getRs350ForegroundMeasurement().getLinEnCalSpectrum());
            dataBlock.setIntValue(4, message.getRs350ForegroundMeasurement().getLinEnCalSpectrum().length);
            ((DataBlockMixed) dataBlock).getUnderlyingObject()[5].setUnderlyingObject(message.getRs350ForegroundMeasurement().getLinEnCalSpectrum());
            dataBlock.setDoubleValue(6, message.getRs350ForegroundMeasurement().getGammaGrossCount());
            dataBlock.setDoubleValue(7, message.getRs350ForegroundMeasurement().getNeutronGrossCount());
            dataBlock.setDoubleValue(8, message.getRs350ForegroundMeasurement().getDoseRate());

            eventHandler.publish(new DataEvent(latestRecordTime, ForegroundOutput.this, dataBlock));
        }
    }
}
