package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
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
    private static final String SENSOR_OUTPUT_NAME = "location";
    private static final String SENSOR_OUTPUT_LABEL = "Location";

    private static final Logger logger = LoggerFactory.getLogger(LocationOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public LocationOutput(RapiscanSensor rapiscanSensor){
        super(SENSOR_OUTPUT_NAME, rapiscanSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("location-output"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
                .addField("sensorLocation", radHelper.createLocationVectorLLA()
                        .label("Sensor Location"))
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
            dataBlock.setStringValue(index++, parentSensor.laneName);

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
