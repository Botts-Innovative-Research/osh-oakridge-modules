package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

public class GammaThresholdOutput extends AbstractSensorOutput<RapiscanSensor> implements IEventListener {

    private static final String SENSOR_OUTPUT_NAME = "gammaThreshold";
    private static final String SENSOR_OUTPUT_LABEL = "Gamma Threshold";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    int latestBackgroundSum = 0;
    double nVal = 0.0;
    double sigmaVal = 0.0;

    public GammaThresholdOutput(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var threshold = radHelper.createThreshold();
        var sigma = radHelper.createSigmaValue();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("gamma-threshold"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(threshold.getName(), threshold)
                .addField(sigma.getName(), sigma)
                .build();
        dataEncoding = new TextEncodingImpl(",", "\n");

        nVal = parentSensor.getConfiguration().setupGammaConfig.nsigma;
    }

    public void onNewBackground(String[] csvLine) {
        if(latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        latestBackgroundSum = 0;
        for(int i = 0; i < csvLine.length-1; i++) {
            int bg = Integer.parseInt(csvLine[i+1]);
            latestBackgroundSum += bg;
        }

        if(latestBackgroundSum == 0) {
            getLogger().warn("No gamma background");
        }

        // Publish threshold data
        int index = 0;

        dataBlock.setLongValue(index++, System.currentTimeMillis()/1000);

        double sqrtBackgroundSum = Math.sqrt(latestBackgroundSum);

        // Return equation for threshold calculation
        dataBlock.setDoubleValue(index++, latestBackgroundSum + (nVal * sqrtBackgroundSum));
        // Set sigma to 0 since we are not in an occupancy
        dataBlock.setDoubleValue(index, sqrtBackgroundSum);

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), GammaThresholdOutput.this, dataBlock));
    }

    // This will be called when there is an available 5 interval (1 second) sum of gamma foreground counts
    public void onNewForeground(int[] foregroundCounts) {
        if(foregroundCounts == null) {
            return;
        }

        if(latestBackgroundSum == 0) {
            getLogger().error("No available gamma background to calculate sigma!");
            return;
        }

        if(nVal == 0) {
            getLogger().error("No available n sigma value. Please set in module config or wait for RPM to update.");
        }

        if(latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        // Sum up the current foreground
        int foregroundSum = 0;
        for(int i = 0; i < 4; i++) {
            foregroundSum += foregroundCounts[i];
        }

        // Sigma calculation = FG-BG / sqrt(BG)
        sigmaVal = (foregroundSum - latestBackgroundSum)
                / Math.sqrt(latestBackgroundSum);

        dataBlock.setLongValue(0, System.currentTimeMillis()/1000);
//        dataBlock.setDoubleValue(1, latestBackgroundSum); Threshold should be set from before. If it's not, then this will never be called
        dataBlock.setDoubleValue(2, sigmaVal);

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), GammaThresholdOutput.this, dataBlock));
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

    // Update n sigma if setup message received
    @Override
    public void handleEvent(Event e) {
        // On setup data received, update n sigma
        if(e instanceof DataEvent) {
            DataEvent dataEvent = (DataEvent) e;
            // Set n value to the value received from Setup Gamma 1
            nVal = dataEvent.getSource().getLatestRecord().getIntValue(5);
        }
    }

}
