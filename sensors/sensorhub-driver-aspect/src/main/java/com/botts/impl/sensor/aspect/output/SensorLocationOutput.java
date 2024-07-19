package com.botts.impl.sensor.aspect.output;

import com.botts.impl.sensor.aspect.AspectSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

public class SensorLocationOutput extends AbstractSensorOutput<AspectSensor> {

    private static final String SENSOR_OUTPUT_NAME = "location";
    private static final String SENSOR_OUTPUT_LABEL = "Location";

    protected DataRecord dataRecord;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public SensorLocationOutput(AspectSensor aspectSensor) {
        super(SENSOR_OUTPUT_NAME, aspectSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();

        dataRecord = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("location-output"))
                .addField(samplingTime.getName(), samplingTime)
                .addField("sensorLocation", radHelper.createLocationVectorLLA()
                        .label("Sensor Location"))
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void setLocationOutput(LLALocation gpsLocation) {
        if (latestRecord == null) {
            dataBlock = dataRecord.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = System.currentTimeMillis() / 1000;

        dataBlock.setLongValue(0, latestRecordTime);
        dataBlock.setDoubleValue(1, gpsLocation.lat);
        dataBlock.setDoubleValue(2, gpsLocation.lon);
        dataBlock.setDoubleValue(3, gpsLocation.alt);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, SensorLocationOutput.this, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataRecord;
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
