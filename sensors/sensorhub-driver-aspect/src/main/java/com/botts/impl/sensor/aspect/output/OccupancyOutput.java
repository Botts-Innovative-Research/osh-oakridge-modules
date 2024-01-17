package com.botts.impl.sensor.aspect.output;

import com.botts.impl.sensor.aspect.AspectSensor;
import com.botts.impl.sensor.aspect.registers.MonitorRegisters;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

public class OccupancyOutput extends AbstractSensorOutput<AspectSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Occupancy";
    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    protected DataRecord dataRecord;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    long lastSetTimeMillis = System.currentTimeMillis();

    public OccupancyOutput(AspectSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        dataRecord = radHelper.createRecord()
                .name(getName())
                .label(getName())
                .definition(RADHelper.getRadUri("occupancy"))
                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
                .addField("OccupancyCount", radHelper.createOccupancyCount())
                .addField("StartTime", radHelper.createOccupancyStartTime())
                .addField("EndTime", radHelper.createOccupancyEndTime())
                .addField("GammaAlarm", radHelper.createBoolean()
                        .name("gamma-alarm")
                        .label("Gamma Alarm")
                        .definition(RADHelper.getRadUri("gamma-alarm")))
                .addField("NeutronAlarm", radHelper.createBoolean()
                        .name("neutron-alarm")
                        .label("Neutron Alarm")
                        .definition(RADHelper.getRadUri("neutron-alarm")))
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void setData(MonitorRegisters monitorRegisters, double timeStamp, double startTime, double endTime, boolean gammaAlarm, boolean neutronAlarm) {


        if (latestRecord == null) {
            dataBlock = dataRecord.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        synchronized (histogramLock) {
            int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

            // Get a sampling time for the latest set based on previous set sampling time
            timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

            // Set the latest sampling time to now
            lastSetTimeMillis = System.currentTimeMillis();
        }

        ++setCount;

        dataBlock.setDoubleValue(0, timeStamp);
        dataBlock.setIntValue(1, monitorRegisters.getNumberOfObject());
        dataBlock.setDoubleValue(2, startTime);
        dataBlock.setDoubleValue(3, endTime);
        dataBlock.setBooleanValue(4, gammaAlarm);
        dataBlock.setBooleanValue(5, neutronAlarm);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(System.currentTimeMillis(), OccupancyOutput.this, dataBlock));
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
        long accumulator = 0;

        synchronized (histogramLock) {
            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {
                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }
}
