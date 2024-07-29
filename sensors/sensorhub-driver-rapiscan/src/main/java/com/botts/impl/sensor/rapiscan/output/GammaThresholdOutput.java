package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.MessageHandler;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.sensor.rapiscan.eml.types.AlgorithmType;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;

public class GammaThresholdOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "gammaThreshold";
    private static final String SENSOR_OUTPUT_LABEL = "Gamma Threshold";
    private static final Logger logger = LoggerFactory.getLogger(GammaThresholdOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    private int latestBackgroundSum = 0;
    double nVal = 0.0;
    double sigmaVal = 0.0;
    String algorithmData = null;

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
    }

    public void onNewGammaBackground(String[] csvLine) {
        int sum = 0;
        int index;
        for(index = 0; index < csvLine.length-1; index++) {
            int bg = Integer.parseInt(csvLine[index+1]);
            sum += bg;
        }
        latestBackgroundSum = sum;
        if(sum == 0) {
            getLogger().warn("No gamma background");
        }
    }

    public void onNewForegroundBatch(LinkedList<String[]> batch) {
        MessageHandler messageHandler = parentSensor.getMessageHandler();
        if(messageHandler.getGammaSetup().getLatestRecord() != null) {
            nVal = messageHandler.getGammaSetup().getNsigma();
        } else {
            nVal = parentSensor.getConfiguration().EMLConfig.gammaSetupConfig.nsigma;
        }

        int size = batch.size();

        // Only update threshold when foreground batch is at 1 second
        if(size == 5 && nVal != 0 && latestBackgroundSum != 0) {
            // Find the foreground counts for 1 second
            int[] foregroundCounts = new int[4];

            for(String[] line : batch) {
                for(int i = 0; i < 4; i++) {
                    foregroundCounts[i] += Integer.parseInt(line[i+1]);
                }
            }

            // Sum up the current foreground sum over 1 second
            int foregroundSum = 0;
            for(int i = 0; i < 4; i++) {
                foregroundSum += foregroundCounts[i];
            }

            // Sigma calculation = FG-BG / sqrt(BG)
            sigmaVal = (foregroundSum - latestBackgroundSum)
                    / Math.sqrt(latestBackgroundSum);

            getLogger().debug("Threshold and sigma set during occupancy");
            publishSigma(sigmaVal);
        }
    }

    public void publishSigma(double newSigma) {
        if(latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        int index = 0;

        dataBlock.setLongValue(index++, System.currentTimeMillis()/1000);
        dataBlock.setDoubleValue(index++, latestRecord != null ? latestRecord.getDoubleValue(1) : 0.0f);
        dataBlock.setDoubleValue(index, newSigma);

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), GammaThresholdOutput.this, dataBlock));
    }

    public void publishThreshold(boolean isSetup) {
        if(latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        int index = 0;

        dataBlock.setLongValue(index++, System.currentTimeMillis()/1000);

        MessageHandler messageHandler = parentSensor.getMessageHandler();

        // TODO: Have as occupancy output


        // Get NSigma value from gamma setup 1 data record
        // If none, default value = 6

        if(isSetup){
            //if setup is not available it uses the preset config values (these are updatable)
            nVal = messageHandler.getGammaSetup().getLatestRecord() != null ? messageHandler.getGammaSetup().getNsigma() : 6.0f;
            algorithmData = messageHandler.getGammaSetup().getLatestRecord() != null ? messageHandler.getGammaSetup().getLatestRecord().getStringValue(10) : "1010";

            // TODO: Sigma = sum(FG) - sum(BG) / sqrt(sum(BG))
            //  Threshold = BG(avg) + n*sigma
        }
        else{
            nVal = messageHandler.getGammaSetup().getLatestRecord() != null ? messageHandler.getGammaSetup().getLatestRecord().getFloatValue(5) : 6.0f;
            algorithmData = messageHandler.getGammaSetup().getLatestRecord() != null ? messageHandler.getGammaSetup().getLatestRecord().getStringValue(10) : "1010";
        }

//        float nSigma = messageHandler.getGammaSetup().getLatestRecord() != null ? messageHandler.getGammaSetup().getLatestRecord().getFloatValue(5) : 6.0f;
        // Get Algorithm value from gamma setup 2 data record
        // If none default value = 1010 or SUM & VERTICAL
//        String algorithmData = messageHandler.getGammaSetup().getLatestRecord() != null ? messageHandler.getGammaSetup().getLatestRecord().getStringValue(11) : "1010";
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
        dataBlock.setDoubleValue(index++, sumGB + (nVal * sqrtSumGB));
        dataBlock.setDoubleValue(index, sigmaVal);
        getLogger().debug("Threshold is {}", sumGB + (nVal * sqrtSumGB));

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
