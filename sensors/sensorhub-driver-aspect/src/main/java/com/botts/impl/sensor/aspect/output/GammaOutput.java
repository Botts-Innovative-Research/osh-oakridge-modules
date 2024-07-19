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

public class GammaOutput extends AbstractSensorOutput<AspectSensor> {
    private static final String SENSOR_OUTPUT_NAME = "gammaScan";
    private static final String SENSOR_OUTPUT_LABEL = "Gamma Scan";
    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    protected DataRecord dataRecord;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    long lastSetTimeMillis = System.currentTimeMillis();

    public GammaOutput(AspectSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var gammaCount = radHelper.createGammaGrossCount();
        var gammaBackground = radHelper.createGammaBackground();
        var gammaVariance = radHelper.createGammaVariance();
        var gammaVarianceBackground = radHelper.createGammaVarianceBackground();
        var gammaAlarmState = radHelper.createGammaAlarmState();

        dataRecord = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("gamma-scan"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(gammaCount.getName(), gammaCount)
                .addField(gammaBackground.getName(), gammaBackground)
                .addField(gammaVariance.getName(), gammaVariance)
                .addField(gammaVarianceBackground.getName(), gammaVarianceBackground)
                .addField(gammaAlarmState.getName(), gammaAlarmState)
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
        dataBlock.setIntValue(1, monitorRegisters.getGammaChannelCount());
        dataBlock.setDoubleValue(2, monitorRegisters.getGammaChannelBackground());
        dataBlock.setDoubleValue(3, monitorRegisters.getGammaChannelVariance());
        dataBlock.setDoubleValue(4, monitorRegisters.getGammaVarianceBackground());
        dataBlock.setStringValue(5, monitorRegisters.getGammaAlarmState());

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent((long) timeStamp, GammaOutput.this, dataBlock));
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
