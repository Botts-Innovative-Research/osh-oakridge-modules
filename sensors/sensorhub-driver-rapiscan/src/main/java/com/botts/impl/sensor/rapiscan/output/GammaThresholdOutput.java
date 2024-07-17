package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.MessageHandler;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.sensor.rapiscan.eml.types.AlgorithmType;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.utils.OshAsserts;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

import java.util.Arrays;
import java.util.EnumSet;

public class GammaThresholdOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "gammaThreshold";
    private static final String SENSOR_OUTPUT_LABEL = "Gamma Threshold";
    private static final Logger logger = LoggerFactory.getLogger(GammaThresholdOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    private int latestBackgroundSum = 0;

    public GammaThresholdOutput(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();
        var threshold = radHelper.createThreshold();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("gamma-threshold"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
                .addField(threshold.getName(), threshold)
                .build();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewGammaBackground(String[] csvLine) {
        int sum = 0;
        int index;
        for(index = 1; index < csvLine.length; index++) {
            int bg = Integer.parseInt(csvLine[index]);
            sum += bg;
        }
        if(sum == 0) {
            latestBackgroundSum = 0;
            getLogger().warn("No gamma background");
        }
        latestBackgroundSum = sum / index-1;
    }

    public void publishThreshold() {
        if(latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        int index = 0;

        dataBlock.setLongValue(index++, System.currentTimeMillis()/1000);
        dataBlock.setStringValue(index++, parent.laneName);

        MessageHandler messageHandler = parentSensor.getMessageHandler();

        // TODO: Have as occupancy output

        //TODO: Somehow ensure that we can get setup values even if unavailable

        // Get NSigma value from gamma setup 1 data record
        // If none, default value = 6
        float nSigma = messageHandler.getGammaSetUp1Output().getLatestRecord() != null ? messageHandler.getGammaSetUp1Output().getLatestRecord().getFloatValue(6) : 6.0f;
        // Get Algorithm value from gamma setup 2 data record
        // If none default value = 1010 or SUM & VERTICAL
        String algorithmData = messageHandler.getGammaSetUp2Output().getLatestRecord() != null ? messageHandler.getGammaSetUp2Output().getLatestRecord().getStringValue(6) : "1010";
        // Algorithm EnumSet includes algorithm type (example: 1010 = SUM, VERTICAL)
        EnumSet<AlgorithmType> algorithmSet = EnumSet.noneOf(AlgorithmType.class);

        for (int i = 0; i < 4; i++) {
            if(algorithmData.charAt(i) == '1') {
                algorithmSet.add(AlgorithmType.values()[i]);
            }
        }

        int sumGB = this.latestBackgroundSum;
        double sqrtSumGB = Math.sqrt(sumGB);

        // Return equation for threshold calculation
        dataBlock.setDoubleValue(index, sumGB + (nSigma * sqrtSumGB));
        getLogger().debug("Threshold is {}", sumGB + (nSigma * sqrtSumGB));

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

}
