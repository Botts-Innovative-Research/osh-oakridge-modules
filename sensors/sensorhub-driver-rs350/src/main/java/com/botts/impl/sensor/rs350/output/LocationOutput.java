package com.botts.impl.sensor.rs350.output;

import com.botts.impl.sensor.rs350.MessageHandler;
import com.botts.impl.sensor.rs350.RS350Sensor;
import com.botts.impl.sensor.rs350.messages.RS350Message;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

/**
 * Publishes the RS350's GPS fix as a standard sensorLocation datastream,
 * using the same record structure as the Rapiscan driver's LocationOutput
 * (which is what the OSCAR viewer's location-datastream detection matches).
 * Unlike a fixed portal, the RS350 is worn by a person: a fix arrives inside
 * every Foreground measurement, so this output updates continuously.
 */
public class LocationOutput extends AbstractSensorOutput<RS350Sensor> implements MessageHandler.ForegroundListener {
    private static final String SENSOR_OUTPUT_NAME = "location";
    private static final String SENSOR_OUTPUT_LABEL = "Location";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public LocationOutput(RS350Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("location-output"))
                .addField(samplingTime.getName(), samplingTime)
                .addField("sensorLocation", radHelper.createLocationVectorLLA()
                        .label("Sensor Location"))
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    @Override
    public void onNewMessage(RS350Message message) {
        var foreground = message.getRs350ForegroundMeasurement();
        if (foreground == null)
            return;

        Double lat = foreground.getLat();
        Double lon = foreground.getLon();
        Double alt = foreground.getAlt();

        // The N42 GeographicPoint is optional; the parser defaults missing
        // coordinates to 0.0, which is not a plausible fix for a deployed unit
        if (lat == null || lon == null || (lat == 0.0 && lon == 0.0))
            return;

        DataBlock dataBlock;
        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = foreground.getStartDateTime() != null
                ? foreground.getStartDateTime() / 1000
                : System.currentTimeMillis() / 1000;

        int index = 0;
        dataBlock.setLongValue(index++, latestRecordTime);
        dataBlock.setDoubleValue(index++, lat);
        dataBlock.setDoubleValue(index++, lon);
        dataBlock.setDoubleValue(index, alt != null ? alt : 0.0);

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
        return 1.0;
    }
}
