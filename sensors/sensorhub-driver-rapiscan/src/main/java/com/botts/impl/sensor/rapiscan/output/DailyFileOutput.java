package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import gov.llnl.ernie.vm250.tools.DailyFileWriter;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

public class DailyFileOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "dailyFile";
    private static final String SENSOR_OUTPUT_LABEL = "Daily File";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public DailyFileOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var csvLine = radHelper.createCsvLine();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("gamma-count"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(csvLine.getName(), csvLine)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(String csvString) {
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }
        long timeStamp = System.currentTimeMillis()/1000;

        dataBlock.setLongValue(0, timeStamp);
        dataBlock.setStringValue(1, csvString);

        eventHandler.publish(new DataEvent(timeStamp, DailyFileOutput.this, dataBlock));
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
