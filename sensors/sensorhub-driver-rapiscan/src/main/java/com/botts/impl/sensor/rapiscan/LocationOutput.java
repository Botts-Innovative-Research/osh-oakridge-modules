package com.botts.impl.sensor.rapiscan;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;


public class LocationOutput  extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Location";

    private static final Logger logger = LoggerFactory.getLogger(LocationOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    LocationOutput(RapiscanSensor rapiscanSensor){
        super(SENSOR_OUTPUT_NAME, rapiscanSensor);
    }

    protected void init() {
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Location")
                .definition(RADHelper.getRadUri("location-output"))
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("Sensor Location", radHelper.createLocationVectorLLA())
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

    }

    public void setLocationOutput(LLALocation gpsLocation) {

            if (latestRecord == null) {

                dataBlock = dataStruct.createDataBlock();

            } else {

                dataBlock = latestRecord.renew();
            }


            latestRecordTime = System.currentTimeMillis() / 1000;

            int index = 0;
            dataBlock.setLongValue(index++, latestRecordTime);

            dataBlock.setDoubleValue(index++, gpsLocation.lat);
            dataBlock.setDoubleValue(index++, gpsLocation.lon);
            dataBlock.setDoubleValue(index++, gpsLocation.alt);

            eventHandler.publish(new DataEvent(latestRecordTime, LocationOutput.this, dataBlock));
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
