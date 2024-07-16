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

public class NeutronOutput extends AbstractSensorOutput<AspectSensor> {
    private static final String SENSOR_OUTPUT_NAME = "neutronScan";
    private static final String SENSOR_OUTPUT_LABEL = "Neutron Scan";
    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    protected DataRecord dataRecord;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    long lastSetTimeMillis = System.currentTimeMillis();

    public NeutronOutput(AspectSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();
        var neutronCount = radHelper.createNeutronGrossCount();
        var neutronBackground = radHelper.createNeutronBackground();
        var neutronVariance = radHelper.createNeutronVariance();
        var neutronVarianceBackground = radHelper.createNeutronVarianceBackground();
        var neutronAlarm = radHelper.createNeutronAlarmState();

        dataRecord = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("neutron-scan"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
                .addField(neutronCount.getName(), neutronCount)
                .addField(neutronBackground.getName(), neutronBackground)
                .addField(neutronVariance.getName(), neutronVariance)
                .addField(neutronVarianceBackground.getName(), neutronVarianceBackground)
                .addField(neutronAlarm.getName(), neutronAlarm)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void setData(MonitorRegisters monitorRegisters, double timeStamp) {
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
        dataBlock.setIntValue(1, parent.laneID);
        dataBlock.setIntValue(2, monitorRegisters.getNeutronChannelCount());
        dataBlock.setFloatValue(3, monitorRegisters.getNeutronChannelBackground());
        dataBlock.setFloatValue(4, monitorRegisters.getNeutronChannelVariance());
        dataBlock.setDoubleValue(5, monitorRegisters.getNeutronVarianceBackground());
        dataBlock.setStringValue(6, monitorRegisters.getNeutronAlarmState());

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent((long) timeStamp, NeutronOutput.this, dataBlock));
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
