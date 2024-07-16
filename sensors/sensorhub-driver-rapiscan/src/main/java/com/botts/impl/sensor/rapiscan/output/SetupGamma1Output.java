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

public class SetupGamma1Output extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "setupGamma1";
    private static final String SENSOR_OUTPUT_LABEL = "Setup Gamma 1";

    private static final Logger logger = LoggerFactory.getLogger(SetupGamma1Output.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public SetupGamma1Output(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init(){
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();
        var highBGFault = radHelper.createHighBackgroundFault();
        var lowBGFault = radHelper.createLowBackgroundFault();
        var intervals = radHelper.createIntervals();
        var holdin = radHelper.createOccupancyHoldin();
        var nSigma = radHelper.createNSigma();
        var placeholder = radHelper.createPlaceholder();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("setup-gamma-1"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
                .addField(highBGFault.getName(), highBGFault)
                .addField(lowBGFault.getName(), lowBGFault)
                .addField(intervals.getName(), intervals)
                .addField(holdin.getName(), holdin)
                .addField(nSigma.getName(), nSigma)
                .addField(placeholder.getName(), placeholder)
                .build();

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
        dataBlock.setStringValue(index++, parent.laneName);
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[1])); //high bg
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[2])); // low bg
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[3])); //intervals
        dataBlock.setIntValue(index++, Integer.parseInt(csvString[4])); //occupancy holding
        dataBlock.setDoubleValue(index++, Double.parseDouble(csvString[5])); //nsigma
        dataBlock.setStringValue(index++, csvString[6]); //placeholder

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), SetupGamma1Output.this, dataBlock));

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
