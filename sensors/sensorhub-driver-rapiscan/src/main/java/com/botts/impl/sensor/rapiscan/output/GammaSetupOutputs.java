package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
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

public class GammaSetupOutputs extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "setupGamma";
    private static final String SENSOR_OUTPUT_LABEL = "Setup Gamma";

    private static final Logger logger = LoggerFactory.getLogger(GammaSetupOutputs.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public GammaSetupOutputs(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init(){
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var highBGFault = radHelper.createHighBackgroundFault();
        var lowBGFault = radHelper.createLowBackgroundFault();
        var intervals = radHelper.createIntervals();
        var holdin = radHelper.createOccupancyHoldin();
        var nSigma = radHelper.createNSigma();
        var placeholder = radHelper.createPlaceholder();

        var detectors = radHelper.createDetectors();
        var controlLLD = radHelper.createLowerControlDiscriminator();
        var controlULD = radHelper.createUpperControlDiscriminator();
        var relayOutput = radHelper.createRelayOutput();
        var algorithm = radHelper.createAlgorithm();
        var version = radHelper.createSoftwareVersion();

        var auxLLD = radHelper.createAuxiliaryLowerDiscriminator();
        var auxULD = radHelper.createAuxiliaryUpperDiscriminator();
        var backgroundTime = radHelper.createBackgroundTime();
        var backgroundNSigma = radHelper.createBackgroundNSigma();
        var firmwareVersion = radHelper.createFirmwareVersion();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("setup-gammas"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(highBGFault.getName(), highBGFault)
                .addField(lowBGFault.getName(), lowBGFault)
                .addField(intervals.getName(), intervals)
                .addField(holdin.getName(), holdin)
                .addField(nSigma.getName(), nSigma)
                .addField(placeholder.getName(), placeholder)

                .addField(detectors.getName(), detectors)
                .addField(controlLLD.getName(), controlLLD)
                .addField(controlULD.getName(), controlULD)
                .addField(relayOutput.getName(), relayOutput)
                .addField(algorithm.getName(), algorithm)
                .addField(version.getName(), version)

                .addField(auxLLD.getName(), auxLLD)
                .addField(auxULD.getName(), auxULD)
                .addField(backgroundTime.getName(), backgroundTime)
                .addField(backgroundNSigma.getName(), backgroundNSigma)
                .addField(firmwareVersion.getName(), firmwareVersion)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(String[] setup1, String [] setup2, String[] setup3){
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }
        int index =0;
        dataBlock.setLongValue(index++,System.currentTimeMillis()/1000);
        // set up 1
        dataBlock.setIntValue(index++, Integer.parseInt(setup1[1])); //high bg
        dataBlock.setIntValue(index++, Integer.parseInt(setup1[2])); // low bg
        dataBlock.setIntValue(index++, Integer.parseInt(setup1[3])); //intervals
        dataBlock.setIntValue(index++, Integer.parseInt(setup1[4])); //occupancy holding
        dataBlock.setDoubleValue(index++, Double.parseDouble(setup1[5])); //nsigma
        dataBlock.setStringValue(index++, setup1[6]); //placeholder

        //set up 2
        dataBlock.setStringValue(index++, setup2[1]); //detectors
        dataBlock.setDoubleValue(index++, Double.parseDouble(setup2[2])); //low discrim
        dataBlock.setDoubleValue(index++, Double.parseDouble(setup2[3])); //high discrim
        dataBlock.setIntValue(index++, Integer.parseInt(setup2[4])); //relay out
        dataBlock.setStringValue(index++, setup2[5]); //algorithm
        dataBlock.setStringValue(index++, setup2[6]); //version software

        //set up 3
        dataBlock.setDoubleValue(index++, Double.parseDouble(setup3[1])); //low discrim
        dataBlock.setDoubleValue(index++, Double.parseDouble(setup3[2])); //high discrim
        dataBlock.setIntValue(index++, Integer.parseInt(setup3[3])); //background time
        dataBlock.setIntValue(index++, Integer.parseInt(setup3[4])); //background nsgima
        dataBlock.setStringValue(index++, setup3[5]); //version of firmware

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), GammaSetupOutputs.this, dataBlock));

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
