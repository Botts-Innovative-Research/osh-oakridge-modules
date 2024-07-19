package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class SetupNeutronOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "setupNeutron1";
    private static final String SENSOR_OUTPUT_LABEL = "Setup Neutron 1";

    private static final Logger logger = LoggerFactory.getLogger(SetupNeutronOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public SetupNeutronOutput(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init(){
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var highBGFault = radHelper.createHighBackgroundFault();
        var maxIntervals = radHelper.createMaxIntervals();
        var alphaValue = radHelper.createAlphaValue();
        var zMaxValue = radHelper.createZMaxValue();
        var sequentialIntervals = radHelper.createSequentialIntervals();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("setup-neutron-1"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(highBGFault.getName(), highBGFault)
                .addField(maxIntervals.getName(), maxIntervals)
                .addField(alphaValue.getName(), alphaValue)
                .addField(zMaxValue.getName(), zMaxValue)
                .addField(sequentialIntervals.getName(), sequentialIntervals)
                .build();

        //TODO: add set up neutron 2
        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(String[] csvString){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }
        int index =0;
        dataBlock.setLongValue(index++,System.currentTimeMillis()/1000);
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[1])); //high bg
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[2])); //max intervals
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[3])); //alpha val
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[4])); //zmax value
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[5])); //sequential intervals

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), SetupNeutronOutput.this, dataBlock));

    }
    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
}

