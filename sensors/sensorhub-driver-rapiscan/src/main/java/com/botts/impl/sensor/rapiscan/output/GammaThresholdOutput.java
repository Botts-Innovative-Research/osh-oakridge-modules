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

public class GammaThresholdOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "gammaThreshold";
    private static final String SENSOR_OUTPUT_LABEL = "Gamma Threshold";
    private static final Logger logger = LoggerFactory.getLogger(GammaThresholdOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    protected GammaThresholdOutput(RapiscanSensor parentSensor) {
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
        float nSigma = messageHandler.getGammaSetUp1Output().getLatestRecord().getFloatValue(6);
        // Get Algorithm value from gamma setup 2 data record
        String algorithmData = messageHandler.getGammaSetUp2Output().getLatestRecord().getStringValue(6);
        // Algorithm EnumSet includes algorithm type (example: 1010 = SUM, VERTICAL)
        EnumSet<AlgorithmType> algorithmSet = EnumSet.noneOf(AlgorithmType.class);

        for (int i = 0; i < 4; i++) {
            if(algorithmData.charAt(i) == '1') {
                algorithmSet.add(AlgorithmType.values()[i]);
            }
        }

        // TODO: Only sum # of detectors based on algorithm AND whether all 4 detectors are on
        int[] gammaCounts = new int[4];

        // TODO: Somehow ensure that we get latest background counts
        // Be able to get this from RPM history or "daily file"
        if(messageHandler.getGammaOutput().getLatestRecord().getStringValue(2) == "Background") {
            for(int i = 0; i < 4; i++) {
                gammaCounts[i] = messageHandler.getGammaOutput().getLatestRecord().getIntValue(i+3);
            }
        }

//        double averageGB = Arrays.stream(gammaCounts).average().orElse(Double.NaN);
        int sumGB = Arrays.stream(gammaCounts).sum();
        double sqrtSumGB = Math.sqrt(sumGB);

        // Return equation for threshold calculation
        dataBlock.setDoubleValue(index, sumGB + (nSigma * sqrtSumGB));

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
