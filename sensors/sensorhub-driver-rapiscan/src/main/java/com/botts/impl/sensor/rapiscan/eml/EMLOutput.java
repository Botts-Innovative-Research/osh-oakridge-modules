package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import gov.llnl.ernie.api.Results;
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
import org.vast.swe.SWEBuilders;

import static com.botts.impl.sensor.rapiscan.eml.EMLFieldFactory.*;


public class EMLOutput extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "ERNIEAnalysis";
    private static final String SENSOR_OUTPUT_LABEL = "EML ERNIE Analysis";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "EML ERNIE Analysis Data parsed from XML";

    private static final Logger logger = LoggerFactory.getLogger(EMLOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    //indices
    int resultIndex, releaseProbabilityIndex, investigateProbabilityIndex, gammaAlertIndex, neutronAlertIndex, overallSourceTypeIndex,
            overallClassifierUsedIndex, overallXLocation1Index, overallXLocation2Index, overallYLocationIndex, overallZLocationIndex,
            overallProbabilityNonEmittingIndex, overallProbabilityNORMIndex, overallProbabilityThreatIndex, sourceTypeIndex, classifierUsedIndex,
            xLocation1Index, xLocation2Index, yLocationIndex, zLocationIndex, probabilityNonEmittingIndex, probabilityNORMIndex,
            probabilityThreatIndex, vehicleClassIndex, vehicleLengthIndex, messageIndex, yellowLightMessageIndex, versionIdIndex,
            modelIdIndex, thresholdsIndex, portIdIndex, laneIdIndex, dateTimeIndex, segmentIdIndex, rpmResultIndex, rpmGammaAlertIndex,
            rpmNeutronAlertIndex, rpmScanErrorIndex, sourceCountIdIndex, sourceArrayIndex;
    public EMLOutput(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        dataStruct = getDataRecord();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }

//    DataRecord createDataRecord() {
//        RADHelper radHelper = new RADHelper();
//
//        var samplingTime = radHelper.createPrecisionTimeStamp();
//        var laneID = radHelper.createLaneID();
//
//        return radHelper.createRecord()
//                .name(getName())
//                .label(SENSOR_OUTPUT_LABEL)
//                .definition(RADHelper.getRadUri("ERNIEAnalysis"))
//                .addField(samplingTime.getName(), samplingTime)
//                .addField(laneID.getName(), laneID)
//                .addField("result", radHelper.createText()
//                        .name("result")
//                        .label("Result")
//                        .definition(RADHelper.getRadUri("result"))
//                        .build())
//                .addField("investigateProbability", radHelper.createQuantity()
//                        .name("investigate-probability")
//                        .label("Investigate Probability")
//                        .dataType(DataType.DOUBLE)
//                        .definition(RADHelper.getRadUri("investigate-probability"))
//                        .build())
//                .addField("releaseProbability", radHelper.createQuantity()
//                        .name("release-probability")
//                        .label("Release Probability")
//                        .dataType(DataType.DOUBLE)
//                        .definition(RADHelper.getRadUri("release-probability"))
//                        .build())
//                .addField("gammaAlert", radHelper.createBoolean()
//                        .name("gamma-alert")
//                        .label("Gamma Alert")
//                        .definition(RADHelper.getRadUri("gamma-alert"))
//                        .build())
//                .addField("neutronAlert", radHelper.createBoolean()
//                        .name("neutron-alert")
//                        .label("Neutron Alert")
//                        .definition(RADHelper.getRadUri("neutron-alert"))
//                        .build())
//                .addField("sourceCount", radHelper.createCount()
//                        .id("sourceCountId"))
//                .addField("sources", radHelper.createArray()
//                        .label("sources")
//                        .withVariableSize("sourceCountId")
//                        .withElement("source", radHelper.createRecord()
//                                .addField("sourceType", radHelper.createText())
//                                .addField("classifierUsed", radHelper.createText())
//                                .addField("xLocation1", radHelper.createQuantity())
//                                .addField("xLocation2", radHelper.createQuantity())
//                                .addField("yLocation", radHelper.createQuantity())
//                                .addField("zLocation", radHelper.createQuantity())
//                                .addField("probabilityNonEmitting", radHelper.createQuantity())
//                                .addField("probabilityNORM", radHelper.createQuantity())
//                                .addField("probabilityThreat", radHelper.createQuantity())
//                        )
//                        .build())
//                .addField("overallSource", radHelper.createRecord()
//                        .label("Overall Source")
//                        .addField("sourceType", radHelper.createText())
//                        .addField("classifierUsed", radHelper.createText())
//                        .addField("xLocation1", radHelper.createQuantity())
//                        .addField("xLocation2", radHelper.createQuantity())
//                        .addField("yLocation", radHelper.createQuantity())
//                        .addField("zLocation", radHelper.createQuantity())
//                        .addField("probabilityNonEmitting", radHelper.createQuantity())
//                        .addField("probabilityNORM", radHelper.createQuantity())
//                        .addField("probabilityThreat", radHelper.createQuantity())
//                        .build())
//                .addField("vehicleClass", radHelper.createCount()
//                        .name("vehicle-class")
//                        .label("Vehicle Class")
//                        .dataType(DataType.INT)
//                        .definition(RADHelper.getRadUri("vehicle-class"))
//                        .build())
//                .addField("vehicleLength", radHelper.createQuantity()
//                        .name("vehicle-length")
//                        .label("Vehicle Length")
//                        .dataType(DataType.DOUBLE)
//                        .definition(RADHelper.getRadUri("vehicle-length"))
//                        .build())
//                .addField("message", radHelper.createText()
//                        .name("message")
//                        .label("Message")
//                        .definition(RADHelper.getRadUri("message"))
//                        .build())
//                .addField("yellowMessageLight", radHelper.createText()
//                        .name("yellow-message-light")
//                        .label("Yellow Message Light")
//                        .definition(RADHelper.getRadUri("yellow-message-light"))
//                        .build())
//
//                .build();
//
//    }

    private DataRecord getDataRecord(){
        var emlFieldFactory = new EMLFieldFactory();

        SWEBuilders.DataRecordBuilder dataRecordBuilder = emlFieldFactory.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("eml-analyis"))
                .description(SENSOR_OUTPUT_DESCRIPTION);

        int fieldIndex = 0;

        dataRecordBuilder.addField(VERSION_ID_FIELD_NAME, emlFieldFactory.createVersionIdField());
        versionIdIndex= fieldIndex++;

        dataRecordBuilder.addField(MODEL_ID_FIELD_NAME, emlFieldFactory.createModelIdField());
        modelIdIndex= fieldIndex++;

        dataRecordBuilder.addField(THRESHOLDS_FIELD_NAME, emlFieldFactory.createThresholdsField());
        thresholdsIndex= fieldIndex++;

        dataRecordBuilder.addField(PORT_ID_FIELD_NAME, emlFieldFactory.createPortIdField());
        portIdIndex= fieldIndex++;

        dataRecordBuilder.addField(LANE_ID_FIELD_NAME, emlFieldFactory.createLaneIdField());
        laneIdIndex= fieldIndex++;

        dataRecordBuilder.addField(TIME_DATE_FIELD_NAME, emlFieldFactory.createDateTimeField());
        dateTimeIndex= fieldIndex++;

        dataRecordBuilder.addField(SEGMENT_ID_FIELD_NAME, emlFieldFactory.createSegmentIdField());
        segmentIdIndex= fieldIndex++;

        dataRecordBuilder.addField(RPM_RESULT_FIELD_NAME, emlFieldFactory.createRpmResultField());
        rpmResultIndex= fieldIndex++;

        dataRecordBuilder.addField(RPM_GAMMA_ALERT_FIELD_NAME, emlFieldFactory.createRpmGammaField());
        segmentIdIndex= fieldIndex++;

        dataRecordBuilder.addField(RPM_NEUTRON_ALERT_FIELD_NAME, emlFieldFactory.createRpmNeutronField());
        rpmResultIndex= fieldIndex++;

        dataRecordBuilder.addField(RPM_SCAN_ERROR_FIELD_NAME, emlFieldFactory.createRpmScanField());
        rpmScanErrorIndex= fieldIndex++;

        dataRecordBuilder.addField(RESULT_FIELD_NAME, emlFieldFactory.createResultsField());
        resultIndex= fieldIndex++;

        dataRecordBuilder.addField(INVESTIGATIVE_PROBABILITY_FIELD_NAME, emlFieldFactory.createInvestigativeProbabilityField());
        investigateProbabilityIndex= fieldIndex++;

        dataRecordBuilder.addField(RELEASE_PROBABILITY_FIELD_NAME, emlFieldFactory.createReleaseProbabilityField());
        releaseProbabilityIndex= fieldIndex++;

        dataRecordBuilder.addField(GAMMA_ALERT_FIELD_NAME, emlFieldFactory.createGammaAlertField());
        gammaAlertIndex= fieldIndex++;

        dataRecordBuilder.addField(NEUTRON_ALERT_FIELD_NAME, emlFieldFactory.createNeutronAlertField());
        neutronAlertIndex= fieldIndex++;

        dataRecordBuilder.addField("sourceCount", emlFieldFactory.createCount().id("sourceCountId"));
        sourceCountIdIndex = fieldIndex++;

        dataRecordBuilder.addField("sources", emlFieldFactory.createArray()
                .label("sources")
                .withVariableSize("sourceCountId")
                .withElement("source", emlFieldFactory.createRecord()
                        .addField(SOURCE_TYPE_FIELD_NAME, emlFieldFactory.createSourceTypeField())
                        .addField(CLASSIFIER_FIELD_NAME, emlFieldFactory.createClassifierUsedField())
                        .addField(X_LOCATION_1_FIELD_NAME, emlFieldFactory.createXLocation1Field())
                        .addField(X_LOCATION_2_FIELD_NAME, emlFieldFactory.createXLocation2Field())
                        .addField(Y_LOCATION_FIELD_NAME, emlFieldFactory.createYLocationField())
                        .addField(Z_LOCATION_FIELD_NAME, emlFieldFactory.createZLocationField())
                        .addField(PROBABILITY_NON_EMITTING_FIELD_NAME, emlFieldFactory.createProbabilityNonEmittingField())
                        .addField(PROBABILITY_NORM_FIELD_NAME, emlFieldFactory.createProbabilityNormField())
                        .addField(PROBABILITY_THREAT_FIELD_NAME, emlFieldFactory.createProbabilityThreatField())
                )
        );
        sourceArrayIndex = fieldIndex++;

//        dataRecordBuilder.addField(SOURCE_TYPE_FIELD_NAME, emlFieldFactory.createSourceTypeField());
//        sourceTypeIndex= fieldIndex++;
//
//        dataRecordBuilder.addField(CLASSIFIER_FIELD_NAME, emlFieldFactory.createClassifierUsedField());
//        classifierUsedIndex= fieldIndex++;
//
//        dataRecordBuilder.addField(X_LOCATION_1_FIELD_NAME, emlFieldFactory.createXLocation1Field());
//        xLocation1Index= fieldIndex++;
//
//        dataRecordBuilder.addField(X_LOCATION_2_FIELD_NAME, emlFieldFactory.createXLocation2Field());
//        xLocation2Index= fieldIndex++;
//
//        dataRecordBuilder.addField(Y_LOCATION_FIELD_NAME, emlFieldFactory.createYLocationField());
//        yLocationIndex= fieldIndex++;
//
//        dataRecordBuilder.addField(Z_LOCATION_FIELD_NAME, emlFieldFactory.createZLocationField());
//        zLocationIndex= fieldIndex++;
//
//        dataRecordBuilder.addField(PROBABILITY_NON_EMITTING_FIELD_NAME, emlFieldFactory.createProbabilityNonEmittingField());
//        probabilityNonEmittingIndex= fieldIndex++;
//
//        dataRecordBuilder.addField(PROBABILITY_NORM_FIELD_NAME, emlFieldFactory.createProbabilityNormField());
//        probabilityNORMIndex= fieldIndex++;
//
//        dataRecordBuilder.addField(PROBABILITY_THREAT_FIELD_NAME, emlFieldFactory.createProbabilityThreatField());
//        probabilityThreatIndex= fieldIndex++;

        dataRecordBuilder.addField(OVERALL_SOURCE_TYPE_FIELD_NAME, emlFieldFactory.createOverallSourceTypeField());
        overallSourceTypeIndex= fieldIndex++;

        dataRecordBuilder.addField(OVERALL_CLASSIFIER_FIELD_NAME, emlFieldFactory.createOverallClassifierUsedField());
        overallClassifierUsedIndex= fieldIndex++;

        dataRecordBuilder.addField(OVERALL_X_LOCATION_1_FIELD_NAME, emlFieldFactory.createOverallXLocation1Field());
        overallXLocation1Index= fieldIndex++;

        dataRecordBuilder.addField(OVERALL_X_LOCATION_2_FIELD_NAME, emlFieldFactory.createOverallXLocation2Field());
        overallXLocation2Index= fieldIndex++;

        dataRecordBuilder.addField(OVERALL_Y_LOCATION_FIELD_NAME, emlFieldFactory.createOverallYLocationField());
        overallYLocationIndex= fieldIndex++;

        dataRecordBuilder.addField(OVERALL_Z_LOCATION_FIELD_NAME, emlFieldFactory.createOverallZLocationField());
        overallZLocationIndex= fieldIndex++;

        dataRecordBuilder.addField(OVERALL_PROBABILITY_NON_EMITTING_FIELD_NAME, emlFieldFactory.createOverallProbabilityNonEmittingField());
        overallProbabilityNonEmittingIndex= fieldIndex++;

        dataRecordBuilder.addField(OVERALL_PROBABILITY_NORM_FIELD_NAME, emlFieldFactory.createOverallProbabilityNormField());
        overallProbabilityNORMIndex= fieldIndex++;

        dataRecordBuilder.addField(OVERALL_PROBABILITY_THREAT_FIELD_NAME, emlFieldFactory.createOverallProbabilityThreatField());
        overallProbabilityThreatIndex= fieldIndex++;

        dataRecordBuilder.addField(VEHICLE_CLASS_FIELD_NAME, emlFieldFactory.createVehicleClassField());
        vehicleClassIndex= fieldIndex++;

        dataRecordBuilder.addField(VEHICLE_LENGTH_FIELD_NAME, emlFieldFactory.createVehicleLengthField());
        vehicleLengthIndex= fieldIndex++;

        dataRecordBuilder.addField(MESSAGE_FIELD_NAME, emlFieldFactory.createMessageField());
        messageIndex= fieldIndex++;

        dataRecordBuilder.addField(YELLOW_LIGHT_MESSAGE_FIELD_NAME, emlFieldFactory.createYellowLightMessageField());
        yellowLightMessageIndex= fieldIndex++;

        return dataRecordBuilder.build();

    }
    public void handleMessage( Results results, long timeStamp)
    {

        DataBlock dataBlock;
        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        dataBlock.setStringValue(versionIdIndex, results.getVersionID());
        dataBlock.setStringValue(modelIdIndex, results.getModelID());
        dataBlock.setDoubleValue(thresholdsIndex, results.getThresholds().getPrimary());

        dataBlock.setStringValue(portIdIndex, results.getPortID());
        dataBlock.setLongValue(laneIdIndex, results.getLaneID());
        dataBlock.setLongValue(dateTimeIndex, timeStamp);
        dataBlock.setLongValue(segmentIdIndex, results.getSegmentId());
        dataBlock.setStringValue(rpmResultIndex, results.getRPMResult().toString());
        dataBlock.setBooleanValue(rpmGammaAlertIndex, results.getRPMGammaAlert());
        dataBlock.setBooleanValue(rpmNeutronAlertIndex, results.getRPMNeutronAlert());
        dataBlock.setBooleanValue(rpmScanErrorIndex, results.getRPMScanError());

        dataBlock.setStringValue(resultIndex, results.getResult().toString());
        dataBlock.setDoubleValue(investigateProbabilityIndex, results.getInvestigateProbability());
        dataBlock.setDoubleValue(releaseProbabilityIndex, results.getReleaseProbability());
        dataBlock.setBooleanValue(gammaAlertIndex, results.getERNIEGammaAlert());
        dataBlock.setBooleanValue(neutronAlertIndex, results.getERNIENeutronAlert());

        int sourceCount = results.getNumberOfSources();

        dataBlock.setIntValue(sourceCountIdIndex, sourceCount);
        if(sourceCount > 0){
            for(int i = 0; i < sourceCount; i++){
                dataBlock.setStringValue(sourceArrayIndex++, results.getSource(i).getSourceType());
                dataBlock.setStringValue(sourceArrayIndex++, results.getSource(i).getClassifierUsed());
                dataBlock.setDoubleValue(sourceArrayIndex++,results.getSource(i).getxLocation1());
                dataBlock.setDoubleValue(sourceArrayIndex++,results.getSource(i).getxLocation2());
                dataBlock.setDoubleValue(sourceArrayIndex++, results.getSource(i).getyLocation());
                dataBlock.setDoubleValue(sourceArrayIndex++,results.getSource(i).getzLocation());
                dataBlock.setDoubleValue(sourceArrayIndex++, results.getSource(i).getProbabilityNonEmitting());
                dataBlock.setDoubleValue(sourceArrayIndex++, results.getSource(i).getProbabilityNORM());
                dataBlock.setDoubleValue(sourceArrayIndex++, results.getSource(i).getProbabilityThreat());
            }

            //set overall stuff
            if(results.getOverallSource().toString() != null){
                dataBlock.setStringValue(overallSourceTypeIndex, results.getOverallSource().toString());
                dataBlock.setStringValue(overallClassifierUsedIndex, results.getOverallSource().getClassifierUsed());
                dataBlock.setDoubleValue(overallXLocation1Index, results.getOverallSource().getxLocation1());
                dataBlock.setDoubleValue(overallXLocation2Index, results.getOverallSource().getxLocation2());
                dataBlock.setDoubleValue(overallYLocationIndex, results.getOverallSource().getyLocation());
                dataBlock.setDoubleValue(overallZLocationIndex, results.getOverallSource().getzLocation());
                dataBlock.setDoubleValue(overallProbabilityNonEmittingIndex, results.getOverallSource().getProbabilityNonEmitting());
                dataBlock.setDoubleValue(overallProbabilityNORMIndex, results.getOverallSource().getProbabilityNORM());
                dataBlock.setDoubleValue(overallProbabilityThreatIndex, results.getOverallSource().getProbabilityThreat());
            }

        }

        dataBlock.setIntValue(vehicleClassIndex, results.getVehicleClass());
        dataBlock.setDoubleValue(vehicleLengthIndex, results.getVehicleLength());
        dataBlock.setStringValue(messageIndex, results.getMessage());
        dataBlock.setStringValue(yellowLightMessageIndex, results.getYellowLightMessage());

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), EMLOutput.this, dataBlock));
    }
//    public void onNewMessage(Results results, long timeStamp) {
//        if (latestRecord == null) {
//            dataBlock = dataStruct.createDataBlock();
//        } else {
//
//            dataBlock = latestRecord.renew();
//        }
//
//        int index = 0;
//        dataBlock.setLongValue(index++,timeStamp/1000);
//        dataBlock.setStringValue(index++, parent.laneName);
//
//        dataBlock.setStringValue(index++, results.getResult().name());
//        dataBlock.setDoubleValue(index++, results.getInvestigateProbability());
//        dataBlock.setDoubleValue(index++, results.getReleaseProbability());
//        dataBlock.setBooleanValue(index++, results.getERNIEGammaAlert());
//        dataBlock.setBooleanValue(index++, results.getERNIENeutronAlert());
//
//        dataBlock.setIntValue(index++, results.getNumberOfSources());
//
//        for(int srcIndex = 0; srcIndex < results.getNumberOfSources(); srcIndex++) {
//            var source = results.getSource(srcIndex);
//            dataBlock.setStringValue(index++, source.getSourceType());
//            dataBlock.setStringValue(index++, source.getClassifierUsed());
//            dataBlock.setDoubleValue(index++, source.getxLocation1());
//            dataBlock.setDoubleValue(index++, source.getxLocation2());
//            dataBlock.setDoubleValue(index++, source.getyLocation());
//            dataBlock.setDoubleValue(index++, source.getzLocation());
//            dataBlock.setDoubleValue(index++, source.getProbabilityNonEmitting());
//            dataBlock.setDoubleValue(index++, source.getProbabilityNORM());
//            dataBlock.setDoubleValue(index++, source.getProbabilityThreat());
//        }
//
//        if(results.getNumberOfSources() == 0) {
//            index++;
//        }
//
//        var overallSource = results.getOverallSource();
//        if(overallSource != null) {
//            dataBlock.setStringValue(index++, overallSource.getSourceType());
//            dataBlock.setStringValue(index++, overallSource.getClassifierUsed());
//            dataBlock.setDoubleValue(index++, overallSource.getxLocation1());
//            dataBlock.setDoubleValue(index++, overallSource.getxLocation2());
//            dataBlock.setDoubleValue(index++, overallSource.getyLocation());
//            dataBlock.setDoubleValue(index++, overallSource.getzLocation());
//            dataBlock.setDoubleValue(index++, overallSource.getProbabilityNonEmitting());
//            dataBlock.setDoubleValue(index++, overallSource.getProbabilityNORM());
//            dataBlock.setDoubleValue(index++, overallSource.getProbabilityThreat());
//        } else {
//            index += 7;
//        }
//
//        dataBlock.setIntValue(index++, results.getVehicleClass());
//        dataBlock.setDoubleValue(index++, results.getVehicleLength());
//        dataBlock.setStringValue(index++, results.getMessage());
//        dataBlock.setStringValue(index, results.getYellowLightMessage());
//
//        latestRecord = dataBlock;
//        eventHandler.publish(new DataEvent(timeStamp, EMLOutput.this, dataBlock));
//
//    }

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