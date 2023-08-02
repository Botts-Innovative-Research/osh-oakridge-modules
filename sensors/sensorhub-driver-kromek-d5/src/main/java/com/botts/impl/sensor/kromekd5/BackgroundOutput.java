package com.botts.impl.sensor.kromekd5;

import com.botts.impl.sensor.kromekd5.messages.D5Message;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.data.TextEncodingImpl;

public class BackgroundOutput extends OutputBase{

    private static final String SENSOR_OUTPUT_NAME = "RS350 Background Report";

    private static final Logger logger = LoggerFactory.getLogger(BackgroundOutput.class);

    public BackgroundOutput(D5Sensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    protected void init(){
        RADHelper radHelper = new RADHelper();
        final String SPEC_ID = "spectrum";


        // OUTPUT

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Background Report")
                .definition(RADHelper.getRadUri("background-report"))
                .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
                .addField("Duration",
                        radHelper.createQuantity()
                                .name("Duration")
                                .label("Duration")
                                .definition(RADHelper.getRadUri("duration")))
                .addField("LinSpectrumSize", radHelper.createArraySize("Spectrum Size", SPEC_ID))
                .addField("LinSpectrum", radHelper.createSpectrum(SPEC_ID))
                .addField("Neutron Gross Count", radHelper.createNeutronGrossCount())
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

    }

    public void parseData(D5Message msg){
        if (latestRecord == null)
            dataBlock = dataStruct.createDataBlock();
        else
            dataBlock = latestRecord.renew();

        latestRecordTime = System.currentTimeMillis();

        dataBlock.setLongValue(0, msg.getD5BackgroundMeasurement().getStartDateTime());
        dataBlock.setDoubleValue(1, msg.getD5BackgroundMeasurement().getDuration());
        dataBlock.setIntValue(2, msg.getD5BackgroundMeasurement().getSpectrum().size());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[3].setUnderlyingObject(msg.getD5BackgroundMeasurement().getSpectrum());
        dataBlock.setDoubleValue(4, msg.getD5BackgroundMeasurement().getNeutronGrossCounts());

        eventHandler.publish(new DataEvent(latestRecordTime, BackgroundOutput.this, dataBlock));
    }
}
